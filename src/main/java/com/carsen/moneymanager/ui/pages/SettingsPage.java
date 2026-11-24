package com.carsen.moneymanager.ui.pages;

import com.carsen.moneymanager.core.Money;
import com.carsen.moneymanager.data.DataStore;
import com.carsen.moneymanager.model.AppData;
import com.carsen.moneymanager.model.Settings;
import com.carsen.moneymanager.ui.MainWindow;
import com.carsen.moneymanager.ui.Theme;
import com.carsen.moneymanager.ui.Ui;
import com.carsen.moneymanager.ui.components.AppButton;
import com.carsen.moneymanager.ui.components.RoundedPanel;
import com.carsen.moneymanager.ui.components.ScrollablePanel;
import com.carsen.moneymanager.ui.components.SegmentedControl;
import com.carsen.moneymanager.ui.components.Toast;
import com.carsen.moneymanager.ui.components.ToggleSwitch;
import com.carsen.moneymanager.ui.components.WrapLayout;
import com.carsen.moneymanager.ui.dialogs.Dialogs;

import javax.swing.BorderFactory;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.border.EmptyBorder;
import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Desktop;
import java.awt.Dimension;
import java.awt.FileDialog;
import java.awt.FlowLayout;
import java.awt.GridBagLayout;
import java.awt.GridLayout;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class SettingsPage extends BasePage {

    private final ToggleSwitch autoDeposits;
    private final ToggleSwitch fundNew;
    private final ToggleSwitch redistribute;
    private final ToggleSwitch stopAtTarget;
    private final ToggleSwitch hideAmounts;
    private final JPanel redistributeRow;
    private final JLabel backupInfo = Ui.muted("", 12.5f);

    public SettingsPage(MainWindow window) {
        super(window);
        Settings s = window.settings();

        ScrollablePanel content = new ScrollablePanel(null);
        content.setLayout(new javax.swing.BoxLayout(content, javax.swing.BoxLayout.Y_AXIS));
        content.setBackground(Theme.BG);
        content.setBorder(new EmptyBorder(2, 34, 34, 34));
        add(Ui.scroll(content), BorderLayout.CENTER);

        // ---------------- Allocation ----------------
        autoDeposits = new ToggleSwitch(s.autoAllocateDeposits);
        autoDeposits.onChange(on -> {
            s.autoAllocateDeposits = on;
            saved(on ? "Deposits will be split across your goals" : "Deposits will go to Unallocated");
        });

        fundNew = new ToggleSwitch(s.fundNewGoals);
        redistribute = new ToggleSwitch(s.redistributeOnNewGoal);
        fundNew.onChange(on -> {
            s.fundNewGoals = on;
            updateEnabled();
            saved(on ? "New goals will get their share right away" : "New goals will start at "
                    + Money.formatVisible(java.math.BigDecimal.ZERO));
        });
        redistribute.onChange(on -> {
            s.redistributeOnNewGoal = on;
            saved(on ? "New goals will re-split your whole balance"
                    : "New goals will only take from Unallocated");
        });

        stopAtTarget = new ToggleSwitch(s.stopAtTarget);
        stopAtTarget.onChange(on -> {
            s.stopAtTarget = on;
            saved(on ? "Goals stop filling at their target" : "Goals keep filling past their target");
        });

        redistributeRow = row("Redistribute the total balance for new goals",
                "On: your whole balance is re-split by percentage when you add a goal, so other goals "
                        + "may hand some money to the new one.\nOff: the new goal's share comes only from "
                        + "Unallocated money, if there is any. Other goals aren't touched.\n"
                        + "Either way, goals set to 0% keep what they have.",
                redistribute);
        redistributeRow.setBorder(new EmptyBorder(0, 26, 0, 0));

        JPanel allocation = section("Allocation", "How money is divided between your goals.",
                row("Split deposits automatically",
                        "Each deposit is divided across your goals by their share. Off: deposits go to Unallocated.",
                        autoDeposits),
                row("Fund new goals right away",
                        "A new goal gets its share as soon as you create it. Off: new goals start empty.",
                        fundNew),
                redistributeRow,
                row("Stop filling a goal at its target",
                        "Once a goal reaches its target, deposits and rebalancing skip it. "
                                + "The extra stays in Unallocated.",
                        stopAtTarget));
        content.add(Ui.left(allocation));
        content.add(Ui.gap(18));

        // ---------------- Appearance ----------------
        SegmentedControl theme = new SegmentedControl(new String[]{"Dark", "Light"},
                new String[]{"moon", "sun"}, Theme.dark ? 0 : 1);
        theme.onChange(i -> window.setTheme(i == 0 ? "dark" : "light"));

        List<Ui.Option<String>> currencies = new ArrayList<>();
        int selectedCurrency = 0;
        for (Map.Entry<String, Locale> e : Money.CURRENCIES.entrySet()) {
            if (e.getKey().equals(s.currency)) selectedCurrency = currencies.size();
            currencies.add(new Ui.Option<>(e.getKey(), Money.label(e.getKey())));
        }
        @SuppressWarnings("unchecked")
        Ui.Combo<Ui.Option<String>> currency = Ui.combo(currencies.toArray(new Ui.Option[0]));
        currency.setSelectedIndex(selectedCurrency);
        currency.setPreferredSize(new Dimension(260, 38));
        currency.addActionListener(e -> {
            window.setCurrency(currency.selected().value);
            Toast.show(window.getRootPane(), "Showing amounts in " + currency.selected().value, null, null);
        });

        hideAmounts = new ToggleSwitch(s.hideAmounts);
        hideAmounts.onChange(on -> window.toggleHideAmounts());

        Integer[] counts = {4, 6, 8, 10, 15};
        Ui.Combo<Integer> recent = Ui.combo(counts);
        recent.setSelectedItem(s.recentCount);
        recent.setPreferredSize(new Dimension(100, 38));
        recent.addActionListener(e -> {
            s.recentCount = recent.selected();
            window.saveSettings();
            window.refreshAll();
        });

        JPanel appearance = section("Appearance", null,
                row("Theme", "Switch any time from the sun and moon button in the sidebar.", theme),
                row("Currency", "Changes the symbol and number format. Amounts aren't converted.", currency),
                row("Hide amounts", "Masks balances on screen, handy when sharing your screen. "
                        + "Forms still show numbers while you type. Shortcut: Ctrl+H.", hideAmounts),
                row("Recent activity on the dashboard", "How many of your latest transactions to list.", recent));
        content.add(Ui.left(appearance));
        content.add(Ui.gap(18));

        // ---------------- Data ----------------
        List<Ui.Option<Integer>> keepOptions = new ArrayList<>();
        int selectedKeep = 1;
        int[] keepValues = {20, 50, 100, 250, 0};
        for (int v : keepValues) {
            if (v == s.backupsToKeep) selectedKeep = keepOptions.size();
            keepOptions.add(new Ui.Option<>(v, v == 0 ? "Keep all" : "Keep last " + v));
        }
        @SuppressWarnings("unchecked")
        Ui.Combo<Ui.Option<Integer>> keep = Ui.combo(keepOptions.toArray(new Ui.Option[0]));
        keep.setSelectedIndex(selectedKeep);
        keep.setPreferredSize(new Dimension(160, 38));
        keep.addActionListener(e -> {
            s.backupsToKeep = keep.selected().value;
            window.store().prune(s.backupsToKeep);
            window.saveSettings();
            updateBackupInfo();
        });

        AppButton backupNow = new AppButton("Back up now", AppButton.Kind.SECONDARY, "archive");
        backupNow.addActionListener(e -> backupNow());
        AppButton restore = new AppButton("Restore a backup", AppButton.Kind.SECONDARY, "undo");
        restore.addActionListener(e -> {
            Dialogs.restoreBackup(window);
            updateBackupInfo();
        });
        AppButton folder = new AppButton("Open data folder", AppButton.Kind.SECONDARY, "folder");
        folder.addActionListener(e -> openFolder());

        AppButton exportCsv = new AppButton("Export transactions (CSV)", AppButton.Kind.SECONDARY, "export");
        exportCsv.addActionListener(e -> exportCsv());
        AppButton exportJson = new AppButton("Export all data (JSON)", AppButton.Kind.SECONDARY, "export");
        exportJson.addActionListener(e -> exportJson());
        AppButton importJson = new AppButton("Import data (JSON)", AppButton.Kind.SECONDARY, "import");
        importJson.addActionListener(e -> importJson());

        JPanel data = section("Data and backups",
                "Everything is stored on this computer in " + DataStore.DATA_FILE + ". Nothing is sent online.",
                row("Automatic backups", "A copy is saved before every change. The oldest copies are "
                        + "removed once you pass the limit. Manual backups are never removed.", keep),
                buttonRow(backupInfo, backupNow, restore, folder),
                buttonRow(Ui.muted("Move your data to another computer or open it in a spreadsheet.", 12.5f),
                        exportCsv, exportJson, importJson));
        content.add(Ui.left(data));
        content.add(Ui.gap(18));

        // ---------------- Reset ----------------
        AppButton reset = new AppButton("Reset everything", AppButton.Kind.DANGER, "trash");
        reset.addActionListener(e -> resetAll());
        JPanel danger = section("Start over", null,
                row("Reset all data", "Deletes every goal, transaction and balance. A backup is made first "
                        + "and you can undo right after.", reset));
        content.add(Ui.left(danger));
        content.add(Ui.gap(18));

        // ---------------- Shortcuts ----------------
        content.add(Ui.left(shortcuts()));
        content.add(Ui.gap(18));

        JLabel about = Ui.muted("Money Manager " + MainWindow.VERSION
                + ". Works offline. No account, no tracking.", 12.5f);
        about.setBorder(new EmptyBorder(0, 4, 0, 0));
        content.add(Ui.left(about));

        updateEnabled();
        updateBackupInfo();
    }

    @Override
    public String title() {
        return "Settings";
    }

    @Override
    public String subtitle() {
        return "Allocation rules, appearance and your data";
    }

    @Override
    public void refresh() {
        Settings s = window.settings();
        autoDeposits.setOn(s.autoAllocateDeposits);
        fundNew.setOn(s.fundNewGoals);
        redistribute.setOn(s.redistributeOnNewGoal);
        stopAtTarget.setOn(s.stopAtTarget);
        hideAmounts.setOn(s.hideAmounts);
        updateEnabled();
        updateBackupInfo();
    }

    private void updateEnabled() {
        boolean enabled = fundNew.isOn();
        redistribute.setEnabled(enabled);
        for (Component c : redistributeRow.getComponents()) setTreeEnabledLook(c, enabled);
    }

    private static void setTreeEnabledLook(Component c, boolean enabled) {
        if (c instanceof JLabel) {
            JLabel l = (JLabel) c;
            Object role = l.getClientProperty("role");
            if ("title".equals(role)) l.setForeground(enabled ? Theme.TEXT : Theme.MUTED);
            if ("desc".equals(role)) l.setForeground(enabled ? Theme.TEXT_2 : Theme.MUTED);
        }
        if (c instanceof java.awt.Container) {
            for (Component child : ((java.awt.Container) c).getComponents()) setTreeEnabledLook(child, enabled);
        }
    }

    private void saved(String message) {
        window.saveSettings();
        window.refreshAll();
        Toast.show(window.getRootPane(), message, null, null);
    }

    private void updateBackupInfo() {
        List<Path> backups = window.store().listBackups();
        backupInfo.setText(backups.isEmpty() ? "No backups yet."
                : backups.size() + (backups.size() == 1 ? " backup" : " backups")
                + ". Latest: " + Dialogs.describeBackupTime(backups.get(0)) + ".");
    }

    // --------------------------------------------------
    // LAYOUT HELPERS
    // --------------------------------------------------

    private static JPanel section(String title, String description, JComponent... rows) {
        RoundedPanel card = new RoundedPanel(new BorderLayout(0, 6), 16, Theme.SURFACE, Theme.BORDER);
        card.padding(20, 24, 10, 24);
        card.setMaximumSize(new Dimension(980, Integer.MAX_VALUE));

        JPanel head = Ui.column();
        head.add(Ui.left(Ui.title(title, 16f)));
        if (description != null) {
            head.add(Ui.gap(4));
            head.add(Ui.left(Ui.paragraph(description, 640, Theme.TEXT_2)));
        }
        head.setBorder(new EmptyBorder(0, 0, 6, 0));
        card.add(head, BorderLayout.NORTH);

        JPanel list = Ui.column();
        for (int i = 0; i < rows.length; i++) {
            JComponent r = rows[i];
            JPanel holder = Ui.transparent(new BorderLayout());
            holder.setBorder(BorderFactory.createCompoundBorder(
                    i == 0 ? BorderFactory.createEmptyBorder() : BorderFactory.createMatteBorder(1, 0, 0, 0, Theme.BORDER),
                    new EmptyBorder(14, 0, 14, 0)));
            holder.add(r, BorderLayout.CENTER);
            list.add(Ui.left(holder));
        }
        card.add(list, BorderLayout.CENTER);
        return card;
    }

    private static JPanel row(String title, String description, JComponent control) {
        JPanel row = Ui.transparent(new BorderLayout(28, 0));
        JPanel text = Ui.column();
        JLabel t = Ui.label(title, Theme.semibold(13.5f), Theme.TEXT);
        t.putClientProperty("role", "title");
        text.add(Ui.left(t));
        if (description != null) {
            text.add(Ui.gap(4));
            JLabel d = Ui.paragraph(description, 520, Theme.TEXT_2);
            d.putClientProperty("role", "desc");
            text.add(Ui.left(d));
        }
        row.add(text, BorderLayout.CENTER);
        JPanel controlWrap = Ui.transparent(new GridBagLayout());
        controlWrap.add(control);
        row.add(controlWrap, BorderLayout.EAST);
        return row;
    }

    private static JPanel buttonRow(JLabel caption, AppButton... buttons) {
        JPanel row = Ui.transparent(new BorderLayout(0, 10));
        row.add(caption, BorderLayout.NORTH);
        JPanel flow = Ui.transparent(new WrapLayout(FlowLayout.LEFT, 0, 0));
        flow.setBorder(new EmptyBorder(0, 0, 0, 0));
        for (AppButton b : buttons) {
            JPanel spaced = Ui.transparent(new BorderLayout());
            spaced.setBorder(new EmptyBorder(0, 0, 6, 8));
            spaced.add(b);
            flow.add(spaced);
        }
        row.add(flow, BorderLayout.CENTER);
        return row;
    }

    private static JPanel shortcuts() {
        String[][] keys = {
                {"Ctrl+D", "Add money"}, {"Ctrl+E", "Record expense"},
                {"Ctrl+M", "Move money"}, {"Ctrl+N", "New goal"},
                {"Ctrl+Z", "Undo last change"}, {"Ctrl+F", "Search this page"},
                {"Ctrl+H", "Hide or show amounts"}, {"Ctrl+1 to 4", "Switch pages"},
        };
        JPanel grid = Ui.transparent(new GridLayout(0, 2, 24, 10));
        for (String[] k : keys) {
            JPanel item = Ui.transparent(new FlowLayout(FlowLayout.LEFT, 0, 0));
            JLabel key = new JLabel(k[0]) {
                @Override
                protected void paintComponent(java.awt.Graphics graphics) {
                    java.awt.Graphics2D g = Ui.hq(graphics);
                    g.setColor(Theme.SURFACE_2);
                    g.fill(new java.awt.geom.RoundRectangle2D.Double(0, 0, getWidth() - 1, getHeight() - 1, 8, 8));
                    g.setColor(Theme.BORDER_STRONG);
                    g.draw(new java.awt.geom.RoundRectangle2D.Double(0.5, 0.5, getWidth() - 2, getHeight() - 2, 8, 8));
                    g.dispose();
                    super.paintComponent(graphics);
                }
            };
            key.setFont(Theme.semibold(12f));
            key.setForeground(Theme.TEXT);
            key.setBorder(new EmptyBorder(4, 9, 4, 9));
            item.add(key);
            item.add(Ui.hgap(12));
            item.add(Ui.secondary(k[1], 13f));
            grid.add(item);
        }
        return section("Keyboard shortcuts", null, grid);
    }

    // --------------------------------------------------
    // ACTIONS
    // --------------------------------------------------

    private void backupNow() {
        try {
            Path p = window.store().backupNow(window.data(), window.settings().backupsToKeep);
            updateBackupInfo();
            Toast.show(window.getRootPane(), "Backup saved: " + p.getFileName(), null, null);
        } catch (Exception e) {
            Dialogs.message(window, "Backup failed", e.getMessage(), true);
        }
    }

    private void openFolder() {
        try {
            if (Desktop.isDesktopSupported() && Desktop.getDesktop().isSupported(Desktop.Action.OPEN)) {
                Desktop.getDesktop().open(DataStore.APP_DIR.toFile());
            } else {
                Dialogs.message(window, "Data folder", DataStore.APP_DIR.toString(), false);
            }
        } catch (Exception e) {
            Dialogs.message(window, "Data folder", "Your data is in:\n" + DataStore.APP_DIR, false);
        }
    }

    private Path chooseSave(String title, String fileName, String extension) {
        FileDialog fd = new FileDialog(window, title, FileDialog.SAVE);
        fd.setFile(fileName);
        fd.setVisible(true);
        if (fd.getFile() == null) return null;
        String name = fd.getFile();
        if (!name.toLowerCase(Locale.ROOT).endsWith(extension)) name += extension;
        return Paths.get(fd.getDirectory(), name);
    }

    private void exportCsv() {
        AppData data = window.data();
        if (data.transactions.isEmpty()) {
            Dialogs.message(window, "Nothing to export", "You don't have any transactions yet.", false);
            return;
        }
        Path path = chooseSave("Export transactions", "money-manager-transactions-" + LocalDate.now() + ".csv", ".csv");
        if (path == null) return;
        try {
            List<com.carsen.moneymanager.model.Transaction> newestFirst = new ArrayList<>(data.transactions);
            java.util.Collections.reverse(newestFirst);
            window.store().exportCsv(newestFirst, path);
            Toast.show(window.getRootPane(), "Exported " + data.transactions.size() + " transactions", null, null);
        } catch (Exception e) {
            Dialogs.message(window, "Export failed", e.getMessage(), true);
        }
    }

    private void exportJson() {
        Path path = chooseSave("Export all data", "money-manager-" + LocalDate.now() + ".json", ".json");
        if (path == null) return;
        try {
            window.store().exportJson(window.data(), path);
            Toast.show(window.getRootPane(), "Exported your data to " + path.getFileName(), null, null);
        } catch (Exception e) {
            Dialogs.message(window, "Export failed", e.getMessage(), true);
        }
    }

    private void importJson() {
        FileDialog fd = new FileDialog(window, "Import data", FileDialog.LOAD);
        fd.setFile("*.json");
        fd.setVisible(true);
        if (fd.getFile() == null) return;
        Path path = Paths.get(fd.getDirectory(), fd.getFile());
        try {
            AppData imported = window.store().readFile(path);
            boolean repaired = DataStore.mismatch(imported).signum() != 0;
            DataStore.repairTotal(imported);
            String body = "This file has a total of " + Money.formatVisible(imported.totalBalance) + ", "
                    + imported.goals.size() + " goals and " + imported.transactions.size() + " transactions.\n\n"
                    + "It replaces everything currently in the app. Your current data is backed up first, "
                    + "and you can undo right after."
                    + (repaired ? "\n\nThe file's total didn't match its goals, so the total was recalculated." : "");
            if (Dialogs.confirm(window, "Replace your data?", body, "Import and replace", true)) {
                window.replaceData(imported, "Imported " + path.getFileName());
            }
        } catch (DataStore.DataException e) {
            Dialogs.message(window, "Can't import this file", e.getMessage(), true);
        }
    }

    private void resetAll() {
        boolean ok = Dialogs.typeToConfirm(window, "Reset all data?",
                "Every goal, transaction and balance is deleted. Your settings stay. "
                        + "A backup is saved first, and you can undo right after.",
                "RESET", "Reset everything");
        if (ok) {
            AppData fresh = new AppData();
            window.replaceData(fresh, "Reset all data");
            window.showPage("dashboard");
        }
    }
}
