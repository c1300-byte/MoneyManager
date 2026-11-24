package com.carsen.moneymanager.data;

import com.carsen.moneymanager.model.AppData;
import com.carsen.moneymanager.model.Goal;
import com.carsen.moneymanager.model.Settings;
import com.carsen.moneymanager.model.Transaction;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import java.io.IOException;
import java.io.Writer;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.nio.file.StandardOpenOption;
import java.nio.file.attribute.FileTime;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;

/** Reads and writes ~/.money-manager/data.json and its backups. */
public final class DataStore {

    public static final Path APP_DIR = Paths.get(System.getProperty("user.home"), ".money-manager");
    public static final Path DATA_FILE = APP_DIR.resolve("data.json");
    public static final Path BACKUP_DIR = APP_DIR.resolve("backups");

    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

    private static final DateTimeFormatter STAMP = DateTimeFormatter.ofPattern("yyyyMMdd-HHmmss-SSS");

    private LocalDateTime lastSaved;

    public static Gson gson() {
        return GSON;
    }

    /** A problem that stops the file from loading at all. */
    public static class DataException extends Exception {
        public DataException(String message) {
            super(message);
        }

        public DataException(String message, Throwable cause) {
            super(message, cause);
        }
    }

    public static class LoadResult {
        public AppData data;
        public boolean createdNew;
        /** totalBalance minus what the buckets actually hold. Zero when fine. */
        public BigDecimal mismatch = BigDecimal.ZERO;
    }

    // --------------------------------------------------
    // LOADING
    // --------------------------------------------------

    public LoadResult load() throws DataException {
        LoadResult result = new LoadResult();

        try {
            Files.createDirectories(APP_DIR);
            Files.createDirectories(BACKUP_DIR);
        } catch (IOException e) {
            throw new DataException("Can't create the data folder at " + APP_DIR + ".", e);
        }

        if (!Files.exists(DATA_FILE)) {
            AppData fresh = new AppData();
            normalize(fresh);
            try {
                save(fresh, false, 0);
            } catch (IOException e) {
                throw new DataException("Can't create " + DATA_FILE + ".", e);
            }
            result.data = fresh;
            result.createdNew = true;
            return result;
        }

        result.data = readFile(DATA_FILE);
        result.mismatch = mismatch(result.data);
        try {
            lastSaved = LocalDateTime.ofInstant(
                    Files.getLastModifiedTime(DATA_FILE).toInstant(),
                    java.time.ZoneId.systemDefault());
        } catch (IOException ignored) {
        }
        return result;
    }

    /** Parse, fill in missing fields and check a data file. */
    public AppData readFile(Path file) throws DataException {
        String json;
        try {
            json = Files.readString(file, StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new DataException("Can't read " + file.getFileName() + ".", e);
        }
        return parse(json);
    }

    public AppData parse(String json) throws DataException {
        if (json == null || json.isBlank()) {
            throw new DataException("The data file is empty.");
        }

        AppData data;
        try {
            data = GSON.fromJson(json, AppData.class);
        } catch (Exception e) {
            throw new DataException("The data file isn't valid JSON.", e);
        }
        if (data == null) {
            throw new DataException("The data file doesn't contain any data.");
        }

        normalize(data);
        validate(data);
        return data;
    }

    /** Fill in anything an older or hand-edited file left out. */
    public static void normalize(AppData data) {
        if (data.goals == null) data.goals = new ArrayList<>();
        if (data.transactions == null) data.transactions = new ArrayList<>();
        if (data.totalBalance == null) data.totalBalance = BigDecimal.ZERO;
        if (data.unallocatedBalance == null) data.unallocatedBalance = BigDecimal.ZERO;

        if (data.settings == null) {
            data.settings = new Settings();
            // v1.1 had a single switch. Keep what it meant: when it was off,
            // deposits stayed unallocated AND new goals started at zero.
            if (Boolean.FALSE.equals(data.automaticAllocation)) {
                data.settings.autoAllocateDeposits = false;
                data.settings.fundNewGoals = false;
            }
        }
        Settings s = data.settings;
        if (s.theme == null) s.theme = "dark";
        if (s.currency == null) s.currency = "USD";
        if (s.lastPage == null) s.lastPage = "dashboard";
        if (s.recentCount <= 0) s.recentCount = 6;
        if (s.backupsToKeep < 0) s.backupsToKeep = 50;

        int nextColor = 0;
        for (Goal goal : data.goals) {
            if (goal.id == null) goal.id = UUID.randomUUID().toString();
            if (goal.name == null) goal.name = "";
            if (goal.category == null) goal.category = "";
            if (goal.purposeLabel == null) goal.purposeLabel = "";
            if (goal.goalType == null) goal.goalType = "Need";
            if (goal.targetAmount == null) goal.targetAmount = BigDecimal.ZERO;
            if (goal.savedAmount == null) goal.savedAmount = BigDecimal.ZERO;
            if (goal.allocationPercent == null) goal.allocationPercent = BigDecimal.ZERO;
            if (goal.colorIndex == null) goal.colorIndex = nextColor % 8;
            nextColor = goal.colorIndex + 1;
        }

        for (Transaction t : data.transactions) {
            if (t.id == null) t.id = UUID.randomUUID().toString();
            if (t.date == null) t.date = "";
            if (t.type == null) t.type = "";
            if (t.description == null) t.description = "";
            if (t.bucket == null) t.bucket = "";
            if (t.amount == null) t.amount = BigDecimal.ZERO;
            if (t.timestamp == null) {
                LocalDateTime parsed = Transaction.parseLegacy(t.date);
                if (parsed != null) t.timestamp = parsed.toString();
            }
        }

        data.version = 2;
        data.automaticAllocation = s.autoAllocateDeposits;
    }

    private static void validate(AppData data) throws DataException {
        if (data.totalBalance.signum() < 0 || data.unallocatedBalance.signum() < 0) {
            throw new DataException("A balance in the file is negative.");
        }
        for (Goal goal : data.goals) {
            if (goal.savedAmount.signum() < 0
                    || goal.targetAmount.signum() < 0
                    || goal.allocationPercent.signum() < 0) {
                throw new DataException("The goal \"" + goal.name + "\" has a negative amount.");
            }
        }
    }

    public static BigDecimal mismatch(AppData data) {
        BigDecimal held = data.unallocatedBalance;
        for (Goal goal : data.goals) held = held.add(goal.savedAmount);
        return data.totalBalance.subtract(held);
    }

    /** Trust the individual buckets and recompute the total from them. */
    public static void repairTotal(AppData data) {
        BigDecimal held = data.unallocatedBalance;
        for (Goal goal : data.goals) held = held.add(goal.savedAmount);
        data.totalBalance = held;
    }

    // --------------------------------------------------
    // SAVING
    // --------------------------------------------------

    /**
     * Writes data.json safely (temp file, then swap).
     * When {@code backup} is true the previous file is copied to backups/ first.
     */
    public void save(AppData data, boolean backup, int keepBackups) throws IOException {
        Files.createDirectories(APP_DIR);
        Files.createDirectories(BACKUP_DIR);

        data.automaticAllocation = data.settings == null || data.settings.autoAllocateDeposits;

        if (backup && Files.exists(DATA_FILE)) {
            copyToBackups(DATA_FILE, "data");
            prune(keepBackups);
        }

        Path temporary = APP_DIR.resolve("data.tmp");
        Files.writeString(
                temporary,
                GSON.toJson(data),
                StandardCharsets.UTF_8,
                StandardOpenOption.CREATE,
                StandardOpenOption.TRUNCATE_EXISTING
        );

        try {
            Files.move(temporary, DATA_FILE,
                    StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
        } catch (AtomicMoveNotSupportedException e) {
            Files.move(temporary, DATA_FILE, StandardCopyOption.REPLACE_EXISTING);
        }

        lastSaved = LocalDateTime.now();
    }

    public LocalDateTime lastSaved() {
        return lastSaved;
    }

    // --------------------------------------------------
    // BACKUPS
    // --------------------------------------------------

    private Path copyToBackups(Path source, String prefix) throws IOException {
        Path target = BACKUP_DIR.resolve(prefix + "-" + LocalDateTime.now().format(STAMP) + ".json");
        Files.copy(source, target, StandardCopyOption.REPLACE_EXISTING);
        return target;
    }

    /** Manual "Back up now" from Settings. */
    public Path backupNow(AppData data, int keepBackups) throws IOException {
        save(data, false, keepBackups);
        Path path = copyToBackups(DATA_FILE, "manual");
        prune(keepBackups);
        return path;
    }

    /** Keeps the newest {@code keep} automatic backups. Manual backups are never removed. */
    public void prune(int keep) {
        if (keep <= 0) return;
        List<Path> auto = new ArrayList<>();
        for (Path p : listBackups()) {
            if (p.getFileName().toString().startsWith("data-")) auto.add(p);
        }
        for (int i = keep; i < auto.size(); i++) {
            try {
                Files.deleteIfExists(auto.get(i));
            } catch (IOException ignored) {
            }
        }
    }

    /** All backups, newest first. */
    public List<Path> listBackups() {
        List<Path> files = new ArrayList<>();
        if (!Files.isDirectory(BACKUP_DIR)) return files;
        try (DirectoryStream<Path> stream = Files.newDirectoryStream(BACKUP_DIR, "*.json")) {
            for (Path p : stream) files.add(p);
        } catch (IOException ignored) {
        }
        files.sort(Comparator.comparing(DataStore::modified).reversed());
        return files;
    }

    public static FileTime modified(Path p) {
        try {
            return Files.getLastModifiedTime(p);
        } catch (IOException e) {
            return FileTime.fromMillis(0);
        }
    }

    /** Keep a copy of a broken data file before it is replaced. */
    public Path setAsideBrokenFile() throws IOException {
        if (!Files.exists(DATA_FILE)) return null;
        Path target = APP_DIR.resolve("data-unreadable-" + LocalDateTime.now().format(STAMP) + ".json");
        Files.copy(DATA_FILE, target, StandardCopyOption.REPLACE_EXISTING);
        return target;
    }

    // --------------------------------------------------
    // EXPORT
    // --------------------------------------------------

    public void exportJson(AppData data, Path file) throws IOException {
        Files.writeString(file, GSON.toJson(data), StandardCharsets.UTF_8);
    }

    public void exportCsv(List<Transaction> transactions, Path file) throws IOException {
        try (Writer out = Files.newBufferedWriter(file, StandardCharsets.UTF_8)) {
            out.write('﻿'); // lets Excel detect UTF-8
            out.write("Date,Type,Description,From / To,Amount,Details\r\n");
            for (Transaction t : transactions) {
                LocalDateTime time = t.time();
                out.write(csv(time == null ? t.date : time.toString().replace('T', ' ')));
                out.write(',');
                out.write(csv(t.type));
                out.write(',');
                out.write(csv(t.description));
                out.write(',');
                out.write(csv(t.bucket));
                out.write(',');
                BigDecimal signed = t.direction() < 0 ? t.amount.abs().negate() : t.amount;
                out.write(signed.toPlainString());
                out.write(',');
                out.write(csv(t.details));
                out.write("\r\n");
            }
        }
    }

    private static String csv(String value) {
        if (value == null) return "";
        String v = value;
        // Stop spreadsheet apps from treating text as a formula.
        if (!v.isEmpty() && "=+-@".indexOf(v.charAt(0)) >= 0) v = "'" + v;
        if (v.contains(",") || v.contains("\"") || v.contains("\n")) {
            v = "\"" + v.replace("\"", "\"\"") + "\"";
        }
        return v;
    }

    /** Deep copy through JSON, used for undo snapshots. */
    public static String snapshot(AppData data) {
        return GSON.toJson(data);
    }

    public static AppData restore(String snapshot) {
        AppData data = GSON.fromJson(snapshot, AppData.class);
        normalize(data);
        return data;
    }
}
