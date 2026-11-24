package com.carsen.moneymanager.ui.dialogs;

import com.carsen.moneymanager.core.AllocationPlan;
import com.carsen.moneymanager.core.Ledger;
import com.carsen.moneymanager.core.Money;
import com.carsen.moneymanager.data.DataStore;
import com.carsen.moneymanager.model.AppData;
import com.carsen.moneymanager.model.Goal;
import com.carsen.moneymanager.ui.Icons;
import com.carsen.moneymanager.ui.MainWindow;
import com.carsen.moneymanager.ui.Theme;
import com.carsen.moneymanager.ui.Ui;
import com.carsen.moneymanager.ui.components.AppButton;
import com.carsen.moneymanager.ui.components.RoundedPanel;

import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.SwingConstants;
import javax.swing.border.EmptyBorder;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.Window;
import java.math.BigDecimal;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.time.Month;
import java.time.Year;
import java.time.YearMonth;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.format.TextStyle;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/** Every dialog the app opens. */
public final class Dialogs {

    private Dialogs() {
    }

    private static final DateTimeFormatter BACKUP_TIME =
            DateTimeFormatter.ofPattern("MMM d, yyyy  h:mm:ss a", Locale.US);

    // --------------------------------------------------
    // ADD MONEY
    // --------------------------------------------------

    public static void addMoney(MainWindow win) {
        boolean auto = win.settings().autoAllocateDeposits;
        boolean hasGoals = !win.data().goals.isEmpty();

        FormDialog d = new FormDialog(win, "Add money",
                auto && hasGoals
                        ? "It's split across your goals by their deposit shares. The rest stays in Unallocated."
                        : auto ? "It goes to Unallocated until you create goals."
                        : "It goes to Unallocated. Turn on automatic split in Settings to divide deposits.");

        Ui.Field amount = d.add("Amount", Ui.field("", "0.00"), null);
        Ui.Field description = d.add("Description", Ui.field("", "Paycheck, gift, refund"), null);

        Preview preview = new Preview("Where it goes");
        d.addBlock(preview);

        Runnable update = () -> {
            preview.clear();
            BigDecimal value = tryParse(amount.getText());
            if (value == null || value.signum() <= 0) {
                preview.note("Enter an amount to see how it's split.");
            } else {
                Ledger.Split split = win.ledger().previewDeposit(value);
                for (Map.Entry<Goal, BigDecimal> e : split.toGoals.entrySet()) {
                    preview.row(Theme.goalColor(e.getKey()), e.getKey().name, Money.formatVisible(e.getValue()));
                }
                if (split.toUnallocated.signum() > 0 || split.toGoals.isEmpty()) {
                    preview.row(Theme.UNALLOCATED, Ledger.UNALLOCATED, Money.formatVisible(split.toUnallocated));
                }
            }
            d.clearError();
            d.relayout();
        };
        Ui.onChange(amount, update);
        update.run();

        d.setPrimary("Add money", AppButton.Kind.PRIMARY, () -> {
            BigDecimal value = Money.parse(amount.getText(), "an amount");
            win.perform("Added " + Money.formatVisible(value),
                    () -> win.ledger().deposit(value, description.getText()));
        });
        d.open();
    }

    // --------------------------------------------------
    // EXPENSE
    // --------------------------------------------------

    public static void recordExpense(MainWindow win, String presetGoalId) {
        FormDialog d = new FormDialog(win, "Record expense",
                "Take money out of Unallocated or one of your goals.");

        Ui.Field amount = d.add("Amount", Ui.field("", "0.00"), null);
        Ui.Field description = d.add("Description", Ui.field("", "Rent, groceries, car repair"), null);
        Ui.Combo<Ui.Option<String>> source = d.add("Pay from", bucketCombo(win, presetGoalId), null);

        JLabel status = Ui.muted(" ", 12.5f);
        d.addBlock(status);

        Runnable update = () -> {
            String id = source.selected().value;
            BigDecimal available = win.ledger().available(id);
            BigDecimal value = tryParse(amount.getText());
            String name = win.ledger().bucketName(id);
            if (value == null || value.signum() <= 0) {
                status.setForeground(Theme.MUTED);
                status.setText(name + " has " + Money.formatVisible(available) + ".");
            } else if (value.compareTo(available) > 0) {
                status.setForeground(Theme.NEGATIVE);
                status.setText(name + " only has " + Money.formatVisible(available) + ".");
            } else {
                status.setForeground(Theme.TEXT_2);
                status.setText("Leaves " + Money.formatVisible(available.subtract(value)) + " in " + name + ".");
            }
            d.clearError();
        };
        Ui.onChange(amount, update);
        source.addActionListener(e -> update.run());
        update.run();

        d.setPrimary("Record expense", AppButton.Kind.PRIMARY, () -> {
            BigDecimal value = Money.parse(amount.getText(), "an amount");
            String id = source.selected().value;
            win.perform("Recorded " + Money.formatVisible(value) + " from " + win.ledger().bucketName(id),
                    () -> win.ledger().expense(value, description.getText(), id));
        });
        d.open();
    }

    // --------------------------------------------------
    // MOVE MONEY
    // --------------------------------------------------

    public static void transfer(MainWindow win, String fromId, String toId) {
        if (win.data().goals.isEmpty()) {
            message(win, "Create a goal first",
                    "Moving money needs at least one goal to move it into or out of.", false);
            return;
        }

        FormDialog d = new FormDialog(win, "Move money",
                "Shift money between goals and Unallocated. Your total balance doesn't change.");

        Ui.Combo<Ui.Option<String>> from = bucketCombo(win, fromId);
        String defaultTo = toId;
        if (defaultTo == null && fromId == null) defaultTo = win.data().goals.get(0).id;
        Ui.Combo<Ui.Option<String>> to = bucketCombo(win, defaultTo);
        if (toId == null && fromId != null) to.setSelectedIndex(0);

        d.addSideBySide(Ui.formRow("From", from, null), Ui.formRow("To", to, null));
        d.focusFirst(from);

        Ui.Field amount = Ui.field("", "0.00");
        JLabel all = Ui.label("Move all", Theme.semibold(12.5f), Theme.ACCENT_TEXT);
        Ui.onClick(all, e -> amount.setText(win.ledger().available(from.selected().value).toPlainString()));
        JPanel amountRow = Ui.transparent(new BorderLayout(0, 6));
        JPanel labelRow = Ui.transparent(new BorderLayout());
        labelRow.add(Ui.label("Amount", Theme.semibold(12.5f), Theme.TEXT_2), BorderLayout.WEST);
        labelRow.add(all, BorderLayout.EAST);
        amountRow.add(labelRow, BorderLayout.NORTH);
        amountRow.add(amount, BorderLayout.CENTER);
        d.addBlock(amountRow);

        Ui.Field note = d.add("Note", Ui.field("", "Optional"), null);

        Preview preview = new Preview("After the move");
        d.addBlock(preview);

        Runnable update = () -> {
            preview.clear();
            Ledger ledger = win.ledger();
            String f = from.selected().value;
            String t = to.selected().value;
            BigDecimal value = tryParse(amount.getText());
            BigDecimal fa = ledger.available(f);
            BigDecimal ta = ledger.available(t);
            boolean same = (f == null && t == null) || (f != null && f.equals(t));
            if (same) {
                preview.note("Pick two different places.");
            } else if (value == null || value.signum() <= 0) {
                preview.row(colorOf(win, f), ledger.bucketName(f), Money.formatVisible(fa));
                preview.row(colorOf(win, t), ledger.bucketName(t), Money.formatVisible(ta));
            } else if (value.compareTo(fa) > 0) {
                preview.warn(ledger.bucketName(f) + " only has " + Money.formatVisible(fa) + ".");
            } else {
                preview.change(colorOf(win, f), ledger.bucketName(f), fa, fa.subtract(value));
                preview.change(colorOf(win, t), ledger.bucketName(t), ta, ta.add(value));
            }
            d.clearError();
            d.relayout();
        };
        Ui.onChange(amount, update);
        from.addActionListener(e -> update.run());
        to.addActionListener(e -> update.run());
        update.run();

        d.setPrimary("Move money", AppButton.Kind.PRIMARY, () -> {
            BigDecimal value = Money.parse(amount.getText(), "an amount");
            String f = from.selected().value;
            String t = to.selected().value;
            Ledger ledger = win.ledger();
            win.perform("Moved " + Money.formatVisible(value) + " to " + ledger.bucketName(t),
                    () -> win.ledger().transfer(f, t, value, note.getText()));
        });
        d.open();
    }

    // --------------------------------------------------
    // GOAL EDITOR
    // --------------------------------------------------

    public static void goalEditor(MainWindow win, Goal existing) {
        boolean editing = existing != null;
        Ledger ledger = win.ledger();

        FormDialog d = new FormDialog(win,
                editing ? "Edit goal" : "New goal",
                editing ? null : "Set a target and how much of each deposit it should get.",
                500);

        Ui.Field name = d.add("Name", Ui.field(editing ? existing.name : "", "Emergency fund, new car, trip"), null);

        Ui.Field category = Ui.field(editing ? existing.category : "", "Savings, travel, home");
        Ui.Combo<String> type = Ui.combo(Goal.TYPES);
        if (editing) type.setSelectedItem(existing.goalType);
        d.addSideBySide(Ui.formRow("Category", category, null), Ui.formRow("Type", type, null));

        Ui.Field target = Ui.field(editing ? existing.targetAmount.toPlainString() : "", "0.00");
        Ui.Field percent = Ui.field(editing ? existing.allocationPercent.stripTrailingZeros().toPlainString() : "",
                "0");

        BigDecimal free = ledger.freePercent();
        if (editing) free = free.add(existing.allocationPercent);
        JPanel targetRow = Ui.formRow("Target amount", target, " ");
        JPanel percentRow = Ui.formRow("Share of each deposit (%)", percent,
                Money.percent(free) + " available");
        d.addSideBySide(targetRow, percentRow);

        // Deadline: month + year
        List<Ui.Option<Integer>> months = new ArrayList<>();
        months.add(new Ui.Option<>(0, "No deadline"));
        for (Month m : Month.values()) {
            months.add(new Ui.Option<>(m.getValue(), m.getDisplayName(TextStyle.FULL, Locale.US)));
        }
        @SuppressWarnings("unchecked")
        Ui.Combo<Ui.Option<Integer>> month = Ui.combo(months.toArray(new Ui.Option[0]));
        int thisYear = Year.now().getValue();
        Integer[] years = new Integer[21];
        for (int i = 0; i < years.length; i++) years[i] = thisYear + i;
        Ui.Combo<Integer> year = Ui.combo(years);

        YearMonth deadline = editing ? existing.deadline() : null;
        if (deadline != null) {
            month.setSelectedIndex(deadline.getMonthValue());
            year.setSelectedItem(deadline.getYear());
        } else {
            year.setSelectedItem(thisYear + 1);
        }

        JLabel deadlineHint = Ui.muted(" ", 12f);
        JPanel deadlineRow = Ui.transparent(new BorderLayout(0, 6));
        deadlineRow.add(Ui.label("Deadline", Theme.semibold(12.5f), Theme.TEXT_2), BorderLayout.NORTH);
        JPanel pickers = Ui.transparent(new java.awt.GridLayout(1, 2, 12, 0));
        pickers.add(month);
        pickers.add(year);
        deadlineRow.add(pickers, BorderLayout.CENTER);
        deadlineRow.add(deadlineHint, BorderLayout.SOUTH);
        d.addBlock(deadlineRow);

        Ui.Field purpose = d.add("Notes", Ui.field(editing ? existing.purposeLabel : "", "Optional: why this goal matters"), null);

        Preview preview = new Preview(editing ? "Good to know" : "When you create it");
        d.addBlock(preview);

        Runnable update = () -> {
            year.setEnabled(month.getSelectedIndex() > 0);

            BigDecimal t = tryParse(target.getText());
            BigDecimal p = tryPercent(percent.getText());

            // Monthly savings hint
            String hint = " ";
            if (month.getSelectedIndex() > 0 && t != null && t.signum() > 0) {
                Goal probe = new Goal();
                probe.targetAmount = t;
                probe.savedAmount = editing ? existing.savedAmount : BigDecimal.ZERO;
                probe.targetMonth = YearMonth.of(year.selected(), month.selected().value).toString();
                BigDecimal monthly = Ledger.monthlyNeeded(probe, YearMonth.now());
                if (monthly != null) hint = "Save about " + Money.formatVisible(monthly) + " a month to make it.";
                else if (probe.isReached()) hint = "Already reached.";
            }
            deadlineHint.setText(hint);

            preview.clear();
            if (editing) {
                boolean changed = p != null && p.compareTo(existing.allocationPercent) != 0;
                if (changed) {
                    preview.note("The new share applies to future deposits. To re-split money you "
                            + "already have, use Rebalance on the Goals page.");
                } else {
                    preview.note("This goal holds " + Money.formatVisible(existing.savedAmount)
                            + ". Use Move money to add or take out funds by hand.");
                }
            } else {
                describeNewGoalPlan(win, preview, p, t, name.getText().isBlank() ? "New goal" : name.getText().trim());
            }
            d.clearError();
            d.relayout();
        };
        Ui.onChange(target, update);
        Ui.onChange(percent, update);
        Ui.onChange(name, update);
        month.addActionListener(e -> update.run());
        year.addActionListener(e -> update.run());
        update.run();

        d.setPrimary(editing ? "Save changes" : "Create goal", AppButton.Kind.PRIMARY, () -> {
            Ledger.GoalDraft draft = new Ledger.GoalDraft();
            draft.name = name.getText();
            draft.category = category.getText();
            draft.purpose = purpose.getText();
            draft.type = type.selected();
            draft.target = Money.parse(target.getText(), "a target amount");
            draft.percent = Money.parsePercent(percent.getText());
            draft.targetMonth = month.getSelectedIndex() > 0
                    ? YearMonth.of(year.selected(), month.selected().value).toString() : null;

            if (editing) {
                win.perform("Saved " + draft.name.trim(), () -> win.ledger().updateGoal(existing.id, draft));
            } else {
                win.perform("Created " + draft.name.trim(), () -> {
                    Goal g = win.ledger().createGoal(draft);
                    if (g.savedAmount.signum() > 0) {
                        win.setPendingToast("Created " + g.name + " with " + Money.formatVisible(g.savedAmount));
                    }
                });
            }
        });
        d.open();
    }

    private static void describeNewGoalPlan(MainWindow win, Preview preview, BigDecimal percent,
                                            BigDecimal target, String newName) {
        Ledger ledger = win.ledger();
        Color newColor = Theme.SERIES[ledger.nextColorIndex() % Theme.SERIES.length];
        boolean fund = win.settings().fundNewGoals;
        boolean redistribute = win.settings().redistributeOnNewGoal;

        if (!fund) {
            preview.note("It starts at " + Money.formatVisible(BigDecimal.ZERO)
                    + ". New goals aren't funded right away (Settings > Allocation).");
            return;
        }
        if (percent == null || percent.signum() <= 0) {
            preview.note("It starts at " + Money.formatVisible(BigDecimal.ZERO)
                    + ". Give it a share of deposits to fund it automatically.");
            return;
        }

        AllocationPlan plan;
        try {
            plan = ledger.planNewGoal(percent, target);
        } catch (IllegalStateException e) {
            preview.warn(e.getMessage());
            return;
        }

        if (plan.mode == AllocationPlan.Mode.FROM_UNALLOCATED) {
            if (plan.newGoalAmount.signum() > 0) {
                preview.change(newColor, newName, BigDecimal.ZERO, plan.newGoalAmount);
                preview.change(Theme.UNALLOCATED, Ledger.UNALLOCATED, plan.unallocatedBefore, plan.unallocatedAfter);
            }
            if (plan.newGoalAmount.compareTo(plan.newGoalWanted) < 0) {
                preview.note(plan.unallocatedBefore.signum() == 0
                        ? "Unallocated is empty, so it starts at " + Money.formatVisible(BigDecimal.ZERO)
                        + ". Its full share would be " + Money.formatVisible(plan.newGoalWanted) + "."
                        : "Its full share would be " + Money.formatVisible(plan.newGoalWanted)
                        + ", but Unallocated only has " + Money.formatVisible(plan.unallocatedBefore) + ".");
            }
            preview.note("Takes from Unallocated only. Your other goals aren't touched.");
        } else if (plan.mode == AllocationPlan.Mode.REDISTRIBUTE) {
            preview.change(newColor, newName, BigDecimal.ZERO, plan.newGoalAmount);
            for (Goal g : win.data().goals) {
                BigDecimal b = plan.before.get(g.id);
                BigDecimal a = plan.after.get(g.id);
                if (b.compareTo(a) != 0) preview.change(Theme.goalColor(g), g.name, b, a);
            }
            if (plan.unallocatedBefore.compareTo(plan.unallocatedAfter) != 0) {
                preview.change(Theme.UNALLOCATED, Ledger.UNALLOCATED, plan.unallocatedBefore, plan.unallocatedAfter);
            }
            preview.note("Your whole balance is re-split by percentage. Goals at 0% keep what they have.");
        }
        preview.footnote("Change this in Settings > Allocation.");
    }

    // --------------------------------------------------
    // DELETE GOAL
    // --------------------------------------------------

    public static void deleteGoal(MainWindow win, Goal goal) {
        FormDialog d = new FormDialog(win, "Delete “" + goal.name + "”?",
                "The goal is removed. Its past transactions stay in your history.");

        Ui.Combo<Ui.Option<String>> moveTo = null;
        if (goal.savedAmount.signum() > 0) {
            List<Ui.Option<String>> options = new ArrayList<>();
            options.add(new Ui.Option<>(null, Ledger.UNALLOCATED));
            for (Goal g : win.data().goals) {
                if (!g.id.equals(goal.id)) options.add(new Ui.Option<>(g.id, g.name));
            }
            @SuppressWarnings("unchecked")
            Ui.Combo<Ui.Option<String>> combo = Ui.combo(options.toArray(new Ui.Option[0]));
            moveTo = d.add("Move its " + Money.formatVisible(goal.savedAmount) + " to", combo, null);
        }

        final Ui.Combo<Ui.Option<String>> target = moveTo;
        d.setPrimary("Delete goal", AppButton.Kind.DANGER, () -> {
            String to = target == null ? null : target.selected().value;
            win.perform("Deleted " + goal.name, () -> win.ledger().deleteGoal(goal.id, to));
        });
        d.open();
    }

    // --------------------------------------------------
    // REBALANCE
    // --------------------------------------------------

    public static void rebalance(MainWindow win) {
        Ledger ledger = win.ledger();
        boolean anyShare = false;
        for (Goal g : win.data().goals) anyShare |= g.participates();
        if (!anyShare) {
            message(win, "Nothing to rebalance",
                    "None of your goals has a deposit share yet. Edit a goal and give it a percentage first.", false);
            return;
        }

        AllocationPlan plan;
        try {
            plan = ledger.planRebalance();
        } catch (IllegalStateException e) {
            message(win, "Can't rebalance", e.getMessage(), true);
            return;
        }
        if (!plan.changesAnything()) {
            message(win, "Already balanced", "Every goal already holds its share of your money.", false);
            return;
        }

        FormDialog d = new FormDialog(win, "Rebalance goals",
                "Re-split your money so each goal holds its percentage of the total. "
                        + "Goals set to 0% keep what they have.", 500);

        Preview preview = new Preview("Changes");
        for (Goal g : win.data().goals) {
            BigDecimal b = plan.before.get(g.id);
            BigDecimal a = plan.after.get(g.id);
            if (b.compareTo(a) != 0) preview.change(Theme.goalColor(g), g.name, b, a);
        }
        if (plan.unallocatedBefore.compareTo(plan.unallocatedAfter) != 0) {
            preview.change(Theme.UNALLOCATED, Ledger.UNALLOCATED, plan.unallocatedBefore, plan.unallocatedAfter);
        }
        d.addBlock(preview);

        d.setPrimary("Rebalance", AppButton.Kind.PRIMARY,
                () -> win.perform("Rebalanced your goals", () -> win.ledger().rebalance()));
        d.open();
    }

    // --------------------------------------------------
    // BACKUPS AND RESET
    // --------------------------------------------------

    public static void restoreBackup(MainWindow win) {
        List<Path> backups = win.store().listBackups();
        if (backups.isEmpty()) {
            message(win, "No backups yet", "Backups are made automatically each time you change something.", false);
            return;
        }
        Path chosen = pickBackup(win, backups, win.store(), "Restore a backup",
                "Your current data is replaced. You can undo this right after.");
        if (chosen == null) return;
        try {
            AppData restored = win.store().readFile(chosen);
            DataStore.repairTotal(restored);
            win.replaceData(restored, "Restored backup from " + describeBackupTime(chosen));
        } catch (DataStore.DataException e) {
            message(win, "Can't restore", e.getMessage(), true);
        }
    }

    /** Shared by the Settings page and start-up recovery. */
    public static Path pickBackup(Window owner, List<Path> backups, DataStore store, String title, String subtitle) {
        FormDialog d = new FormDialog(owner, title, subtitle, 500);

        List<Ui.Option<Path>> options = new ArrayList<>();
        for (Path p : backups) {
            String kind = p.getFileName().toString().startsWith("manual") ? "manual" : "automatic";
            options.add(new Ui.Option<>(p, describeBackupTime(p) + "   •   " + kind));
        }
        @SuppressWarnings("unchecked")
        Ui.Combo<Ui.Option<Path>> combo = Ui.combo(options.toArray(new Ui.Option[0]));
        d.add("Backup", combo, null);

        JLabel summary = Ui.secondary(" ", 12.5f);
        d.addBlock(summary);
        Runnable update = () -> {
            try {
                AppData data = store.readFile(combo.selected().value);
                summary.setForeground(Theme.TEXT_2);
                summary.setText("Total " + Money.formatVisible(data.totalBalance) + ",  "
                        + data.goals.size() + (data.goals.size() == 1 ? " goal, " : " goals, ")
                        + data.transactions.size() + " transactions");
            } catch (DataStore.DataException e) {
                summary.setForeground(Theme.NEGATIVE);
                summary.setText("This backup can't be read: " + e.getMessage());
            }
        };
        combo.addActionListener(e -> update.run());
        update.run();

        final Path[] result = new Path[1];
        d.setPrimary("Restore", AppButton.Kind.PRIMARY, () -> {
            store.readFile(combo.selected().value);
            result[0] = combo.selected().value;
        });
        return d.open() ? result[0] : null;
    }

    public static String describeBackupTime(Path p) {
        LocalDateTime time = LocalDateTime.ofInstant(DataStore.modified(p).toInstant(), ZoneId.systemDefault());
        return time.format(BACKUP_TIME);
    }

    public static boolean typeToConfirm(Window owner, String title, String body, String word, String button) {
        FormDialog d = new FormDialog(owner, title, body);
        Ui.Field field = d.add("Type " + word + " to confirm", Ui.field("", word), null);
        d.setPrimary(button, AppButton.Kind.DANGER, () -> {
            if (!word.equals(field.getText().trim())) {
                throw new IllegalArgumentException("Type " + word + " in capital letters to continue.");
            }
        });
        return d.open();
    }

    // --------------------------------------------------
    // GENERIC
    // --------------------------------------------------

    public static boolean confirm(Window owner, String title, String body, String button, boolean danger) {
        return confirm(owner, title, body, button, danger, "Cancel");
    }

    public static boolean confirm(Window owner, String title, String body, String button, boolean danger,
                                  String cancelText) {
        FormDialog d = new FormDialog(owner, title, body);
        d.setCancelText(cancelText);
        d.setPrimary(button, danger ? AppButton.Kind.DANGER : AppButton.Kind.PRIMARY, null);
        return d.open();
    }

    public static void message(Window owner, String title, String body, boolean error) {
        FormDialog d = new FormDialog(owner, title, null);
        JLabel text = Ui.paragraph(body, 390, Theme.TEXT_2);
        if (error) {
            text.setIcon(Icons.get("alert", 18, Theme.NEGATIVE));
            text.setIconTextGap(10);
            text.setVerticalTextPosition(SwingConstants.TOP);
        }
        d.addBlock(text);
        d.setPrimary("OK", AppButton.Kind.PRIMARY, null);
        d.open();
    }

    // --------------------------------------------------
    // HELPERS
    // --------------------------------------------------

    @SuppressWarnings("unchecked")
    private static Ui.Combo<Ui.Option<String>> bucketCombo(MainWindow win, String selectedId) {
        List<Ui.Option<String>> options = new ArrayList<>();
        options.add(new Ui.Option<>(null, Ledger.UNALLOCATED + "   " + Money.formatVisible(win.data().unallocatedBalance)));
        int selected = 0;
        for (Goal g : win.data().goals) {
            if (g.id.equals(selectedId)) selected = options.size();
            options.add(new Ui.Option<>(g.id, g.name + "   " + Money.formatVisible(g.savedAmount)));
        }
        Ui.Combo<Ui.Option<String>> combo = Ui.combo(options.toArray(new Ui.Option[0]));
        combo.setSelectedIndex(selected);
        return combo;
    }

    private static Color colorOf(MainWindow win, String goalId) {
        Goal g = win.ledger().goal(goalId);
        return g == null ? Theme.UNALLOCATED : Theme.goalColor(g);
    }

    private static BigDecimal tryParse(String text) {
        try {
            return Money.parse(text, "an amount");
        } catch (IllegalArgumentException e) {
            return null;
        }
    }

    private static BigDecimal tryPercent(String text) {
        try {
            return Money.parsePercent(text);
        } catch (IllegalArgumentException e) {
            return null;
        }
    }

    /** Tinted box listing what an action will do. */
    static class Preview extends RoundedPanel {
        private final JPanel rows;
        private int line;

        Preview(String heading) {
            super(new BorderLayout(0, 10), 12, Theme.dark ? Theme.BG : Theme.SURFACE_2, Theme.BORDER);
            padding(14, 16, 14, 16);
            add(Ui.label(heading, Theme.semibold(12.5f), Theme.TEXT_2), BorderLayout.NORTH);
            rows = Ui.transparent(new GridBagLayout());
            add(rows, BorderLayout.CENTER);
        }

        void clear() {
            rows.removeAll();
            line = 0;
        }

        private GridBagConstraints at(int x, double weight, int anchor) {
            GridBagConstraints c = new GridBagConstraints();
            c.gridx = x;
            c.gridy = line;
            c.weightx = weight;
            c.anchor = anchor;
            c.fill = weight > 0 ? GridBagConstraints.HORIZONTAL : GridBagConstraints.NONE;
            c.insets = new Insets(line == 0 ? 0 : 7, x == 0 ? 0 : 12, 0, 0);
            return c;
        }

        void row(Color color, String name, String value) {
            JLabel n = new JLabel(name, Ui.dot(color, 9), SwingConstants.LEFT);
            n.setIconTextGap(8);
            n.setFont(Theme.font(13f));
            n.setForeground(Theme.TEXT);
            rows.add(n, at(0, 1, GridBagConstraints.WEST));
            JLabel v = Ui.label(value, Theme.semibold(13f), Theme.TEXT);
            rows.add(v, at(3, 0, GridBagConstraints.EAST));
            line++;
        }

        void change(Color color, String name, BigDecimal before, BigDecimal after) {
            JLabel n = new JLabel(name, Ui.dot(color, 9), SwingConstants.LEFT);
            n.setIconTextGap(8);
            n.setFont(Theme.font(13f));
            n.setForeground(Theme.TEXT);
            rows.add(n, at(0, 1, GridBagConstraints.WEST));
            rows.add(Ui.label(Money.formatVisible(before), Theme.font(13f), Theme.MUTED), at(1, 0, GridBagConstraints.EAST));
            rows.add(new JLabel(Icons.get("chevron-right", 13, Theme.MUTED)), at(2, 0, GridBagConstraints.CENTER));
            int cmp = after.compareTo(before);
            JLabel a = Ui.label(Money.formatVisible(after), Theme.semibold(13f),
                    cmp > 0 ? Theme.POSITIVE : cmp < 0 ? Theme.NEGATIVE : Theme.TEXT);
            rows.add(a, at(3, 0, GridBagConstraints.EAST));
            line++;
        }

        void note(String text) {
            addText(text, Theme.TEXT_2, 12.5f);
        }

        void footnote(String text) {
            addText(text, Theme.MUTED, 12f);
        }

        void warn(String text) {
            addText(text, Theme.NEGATIVE, 12.5f);
        }

        private void addText(String text, Color color, float size) {
            JLabel l = Ui.label("<html><div style='width:360px'>" + Ui.escape(text) + "</div></html>",
                    Theme.font(size), color);
            GridBagConstraints c = at(0, 1, GridBagConstraints.WEST);
            c.gridwidth = 4;
            c.insets = new Insets(line == 0 ? 0 : 9, 0, 0, 0);
            rows.add(l, c);
            line++;
        }
    }

    static JComponent spacer() {
        JPanel p = Ui.transparent(new BorderLayout());
        p.setBorder(new EmptyBorder(0, 0, 0, 0));
        p.setAlignmentX(Component.LEFT_ALIGNMENT);
        return p;
    }
}
