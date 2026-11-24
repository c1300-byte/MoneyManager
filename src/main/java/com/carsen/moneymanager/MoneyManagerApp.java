package com.carsen.moneymanager;

import com.carsen.moneymanager.core.Money;
import com.carsen.moneymanager.data.DataStore;
import com.carsen.moneymanager.model.AppData;
import com.carsen.moneymanager.ui.MainWindow;
import com.carsen.moneymanager.ui.Theme;
import com.carsen.moneymanager.ui.dialogs.Dialogs;

import javax.swing.SwingUtilities;
import java.math.BigDecimal;
import java.nio.file.Path;
import java.util.List;
import java.util.Locale;

/**
 * Money Manager: a local, offline personal finance app.
 *
 * Code layout:
 *   model/    what gets saved (AppData, Goal, Transaction, Settings)
 *   data/     reading and writing data.json, backups, export
 *   core/     the money rules (Ledger) and number formatting (Money)
 *   ui/       the window, pages, dialogs and drawing components
 */
public final class MoneyManagerApp {

    private MoneyManagerApp() {
    }

    public static void main(String[] args) {
        String os = System.getProperty("os.name", "").toLowerCase(Locale.ROOT);
        if (!os.contains("win") && System.getProperty("awt.useSystemAAFontSettings") == null) {
            System.setProperty("awt.useSystemAAFontSettings", "lcd");
        }
        System.setProperty("swing.aatext", "true");

        SwingUtilities.invokeLater(MoneyManagerApp::start);
    }

    private static void start() {
        Theme.apply("dark");
        Theme.installLookAndFeel();

        DataStore store = new DataStore();
        DataStore.LoadResult result;

        try {
            result = store.load();
        } catch (DataStore.DataException e) {
            result = recover(store, e);
            if (result == null) {
                System.exit(1);
                return;
            }
        }

        AppData data = result.data;
        Money.configure(data.settings.currency, false);

        if (result.mismatch.signum() != 0) {
            BigDecimal recorded = data.totalBalance;
            DataStore.repairTotal(data);
            boolean fix = Dialogs.confirm(null, "Your totals don't add up",
                    "The saved total is " + Money.formatVisible(recorded) + ", but your goals and "
                            + "Unallocated hold " + Money.formatVisible(data.totalBalance) + ".\n\n"
                            + "Fix it by setting the total to " + Money.formatVisible(data.totalBalance)
                            + "? A backup of the current file is made first.",
                    "Fix and continue", false, "Quit");
            if (!fix) {
                System.exit(0);
                return;
            }
            try {
                store.save(data, true, data.settings.backupsToKeep);
            } catch (Exception e) {
                Dialogs.message(null, "Couldn't save", e.getMessage(), true);
            }
        }

        new MainWindow(store, data).open();
    }

    /** The data file can't be read. Offer a backup instead of giving up. */
    private static DataStore.LoadResult recover(DataStore store, DataStore.DataException error) {
        String reason = error.getMessage();
        if (error.getCause() != null && error.getCause().getMessage() != null) {
            String detail = error.getCause().getMessage().split("\n")[0];
            int colon = detail.indexOf(": ");
            if (detail.startsWith("java.") && colon > 0) detail = detail.substring(colon + 2);
            int path = detail.indexOf(" path $");
            if (path > 0) detail = detail.substring(0, path);
            reason += "\nDetails: " + detail + ".";
        }

        List<Path> backups = store.listBackups();
        boolean hasBackups = !backups.isEmpty();

        boolean proceed = Dialogs.confirm(null, "Money Manager can't open your data",
                reason + "\n\nYour file at " + DataStore.DATA_FILE + " hasn't been changed.\n\n"
                        + (hasBackups
                        ? (backups.size() == 1 ? "You can restore your backup." : "You can restore one of your " + backups.size() + " backups.")
                        + " The unreadable file is kept as a copy."
                        : "There are no backups yet. You can start with empty data. The unreadable file is kept as a copy."),
                hasBackups ? "Choose a backup" : "Start with empty data", false, "Quit");
        if (!proceed) return null;

        try {
            AppData data;
            if (hasBackups) {
                Path chosen = Dialogs.pickBackup(null, backups, store, "Restore a backup",
                        "Pick the copy to continue with.");
                if (chosen == null) return null;
                data = store.readFile(chosen);
                DataStore.repairTotal(data);
            } else {
                data = new AppData();
                DataStore.normalize(data);
            }
            store.setAsideBrokenFile();
            store.save(data, false, 0);

            DataStore.LoadResult result = new DataStore.LoadResult();
            result.data = data;
            return result;
        } catch (Exception e) {
            Dialogs.message(null, "Recovery failed", e.getMessage(), true);
            return null;
        }
    }
}
