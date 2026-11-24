package com.carsen.moneymanager.ui.pages;

import com.carsen.moneymanager.core.Ledger;
import com.carsen.moneymanager.core.Money;
import com.carsen.moneymanager.core.Stats;
import com.carsen.moneymanager.model.AppData;
import com.carsen.moneymanager.model.Goal;
import com.carsen.moneymanager.model.Transaction;
import com.carsen.moneymanager.ui.MainWindow;
import com.carsen.moneymanager.ui.Theme;
import com.carsen.moneymanager.ui.Ui;
import com.carsen.moneymanager.ui.components.AllocationBar;
import com.carsen.moneymanager.ui.components.AppButton;
import com.carsen.moneymanager.ui.components.CashFlowChart;
import com.carsen.moneymanager.ui.components.ProgressBar;
import com.carsen.moneymanager.ui.components.RoundedPanel;
import com.carsen.moneymanager.ui.components.ScrollablePanel;
import com.carsen.moneymanager.ui.components.WrapLayout;
import com.carsen.moneymanager.ui.dialogs.Dialogs;

import javax.swing.BoxLayout;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.SwingConstants;
import javax.swing.border.EmptyBorder;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.GridLayout;
import java.awt.Insets;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;

public class DashboardPage extends BasePage {

    private static final DateTimeFormatter TODAY = DateTimeFormatter.ofPattern("EEEE, MMMM d", Locale.US);
    private static final DateTimeFormatter WHEN = DateTimeFormatter.ofPattern("MMM d, h:mm a", Locale.US);

    private final ScrollablePanel content;
    private final List<JComponent> actions = new ArrayList<>();

    public DashboardPage(MainWindow window) {
        super(window);

        content = new ScrollablePanel(new GridBagLayout());
        content.setBackground(Theme.BG);
        content.setBorder(new EmptyBorder(2, 34, 30, 34));
        add(Ui.scroll(content), BorderLayout.CENTER);

        AppButton move = new AppButton("Move money", AppButton.Kind.SECONDARY, "transfer");
        move.setToolTipText("Move money between goals (Ctrl+M)");
        move.addActionListener(e -> Dialogs.transfer(window, null, null));

        AppButton expense = new AppButton("Record expense", AppButton.Kind.SECONDARY, "minus");
        expense.setToolTipText("Record an expense (Ctrl+E)");
        expense.addActionListener(e -> Dialogs.recordExpense(window, null));

        AppButton add = new AppButton("Add money", AppButton.Kind.PRIMARY, "plus");
        add.setToolTipText("Add a deposit (Ctrl+D)");
        add.addActionListener(e -> Dialogs.addMoney(window));

        actions.add(move);
        actions.add(expense);
        actions.add(add);
    }

    @Override
    public String title() {
        return "Dashboard";
    }

    @Override
    public String subtitle() {
        return LocalDate.now().format(TODAY);
    }

    @Override
    public List<JComponent> actions() {
        return actions;
    }

    @Override
    public void refresh() {
        content.removeAll();
        AppData data = window.data();

        GridBagConstraints c = new GridBagConstraints();
        c.gridx = 0;
        c.gridy = 0;
        c.gridwidth = 2;
        c.weightx = 1;
        c.fill = GridBagConstraints.HORIZONTAL;
        c.anchor = GridBagConstraints.NORTH;
        content.add(hero(data), c);

        c.gridy = 1;
        c.gridwidth = 1;
        c.weightx = 0.58;
        c.fill = GridBagConstraints.BOTH;
        c.insets = new Insets(18, 0, 0, 9);
        content.add(cashFlow(data), c);

        c.gridx = 1;
        c.weightx = 0.42;
        c.insets = new Insets(18, 9, 0, 0);
        content.add(goalProgress(data), c);

        c.gridx = 0;
        c.gridy = 2;
        c.gridwidth = 2;
        c.weightx = 1;
        c.insets = new Insets(18, 0, 0, 0);
        content.add(recent(data), c);

        c.gridy = 3;
        c.weighty = 1;
        content.add(Ui.transparent(new BorderLayout()), c);

        content.revalidate();
        content.repaint();
    }

    // --------------------------------------------------
    // HERO: total + where every dollar sits
    // --------------------------------------------------

    private JComponent hero(AppData data) {
        Ledger ledger = new Ledger(data);
        RoundedPanel card = new RoundedPanel(new BorderLayout(0, 20), 16, Theme.SURFACE, Theme.BORDER);
        card.padding(24, 26, 22, 26);

        Stats.MonthFlow month = Stats.thisMonth(data);
        BigDecimal net = month.net();

        // Top row
        JPanel top = Ui.transparent(new BorderLayout(30, 0));

        JPanel total = Ui.column();
        total.add(Ui.left(Ui.secondary("Total balance", 13.5f)));
        total.add(Ui.gap(4));
        total.add(Ui.left(Ui.label(Money.format(data.totalBalance), Theme.semibold(38f), Theme.TEXT)));
        total.add(Ui.gap(4));
        JLabel delta;
        if (month.in.signum() == 0 && month.out.signum() == 0) {
            delta = Ui.muted("No money in or out this month yet", 12.5f);
        } else {
            delta = Ui.label(Money.signed(net, net.signum()) + " this month",
                    Theme.semibold(12.5f), net.signum() >= 0 ? Theme.POSITIVE : Theme.NEGATIVE);
        }
        total.add(Ui.left(delta));
        top.add(total, BorderLayout.WEST);

        JPanel stats = Ui.transparent(new GridLayout(1, 3, 34, 0));
        BigDecimal inGoals = ledger.goalsTotal();
        String share = data.totalBalance.signum() > 0
                ? Money.percent(inGoals.multiply(Money.HUNDRED).divide(data.totalBalance, 1, RoundingMode.HALF_UP))
                + " of your money"
                : "No money yet";
        stats.add(stat("In goals", Money.format(inGoals), share));
        stats.add(stat("Unallocated", Money.format(data.unallocatedBalance),
                data.unallocatedBalance.signum() > 0 ? "Ready to assign" : "Nothing waiting"));
        int expenseCount = countThisMonth(data, Transaction.EXPENSE);
        stats.add(stat("Spent this month", Money.format(month.out),
                expenseCount == 0 ? "No expenses" : expenseCount + (expenseCount == 1 ? " expense" : " expenses")));
        JPanel statsWrap = Ui.transparent(new BorderLayout());
        statsWrap.add(stats, BorderLayout.SOUTH);
        top.add(statsWrap, BorderLayout.EAST);
        card.add(top, BorderLayout.NORTH);

        // Where the money sits
        AllocationBar bar = new AllocationBar(16);
        List<AllocationBar.Segment> segments = new ArrayList<>();
        JPanel legend = Ui.transparent(new WrapLayout(FlowLayout.LEFT, 0, 4));
        
        double totalValue = data.totalBalance.doubleValue();
        List<Goal> goals = new ArrayList<>(data.goals);
        goals.sort(Comparator.comparing((Goal g) -> g.savedAmount).reversed());

        int shown = 0;
        BigDecimal other = BigDecimal.ZERO;
        for (Goal g : goals) {
            if (g.savedAmount.signum() <= 0) continue;
            if (shown < 7) {
                segments.add(new AllocationBar.Segment(g.name, g.savedAmount.doubleValue(), Theme.goalColor(g),
                        tip(g.name, g.savedAmount, totalValue)));
                legend.add(legendItem(Theme.goalColor(g), g.name, g.savedAmount));
                shown++;
            } else {
                other = other.add(g.savedAmount);
            }
        }
        if (other.signum() > 0) {
            Color oc = Theme.mix(Theme.UNALLOCATED, Theme.TEXT, 0.25);
            segments.add(new AllocationBar.Segment("Other goals", other.doubleValue(), oc,
                    tip("Other goals", other, totalValue)));
            legend.add(legendItem(oc, "Other goals", other));
        }
        if (data.unallocatedBalance.signum() > 0) {
            segments.add(new AllocationBar.Segment(Ledger.UNALLOCATED, data.unallocatedBalance.doubleValue(),
                    Theme.UNALLOCATED, tip(Ledger.UNALLOCATED, data.unallocatedBalance, totalValue)));
            legend.add(legendItem(Theme.UNALLOCATED, Ledger.UNALLOCATED, data.unallocatedBalance));
        }
        bar.setSegments(segments);

        JPanel where = Ui.transparent(new BorderLayout(0, 10));
        where.add(bar, BorderLayout.NORTH);
        if (segments.isEmpty()) {
            where.add(Ui.muted("Add money to see where it sits.", 12.5f), BorderLayout.CENTER);
        } else {
            where.add(legend, BorderLayout.CENTER);
        }
        card.add(where, BorderLayout.CENTER);
        return card;
    }

    private static String tip(String name, BigDecimal value, double total) {
        String pct = total > 0 ? Money.percent(BigDecimal.valueOf(value.doubleValue() * 100 / total)) : "0%";
        return "<html><b>" + Ui.escape(name) + "</b><br>" + Money.format(value) + " (" + pct + ")</html>";
    }

    private static JComponent legendItem(Color color, String name, BigDecimal value) {
        JPanel item = Ui.transparent(new FlowLayout(FlowLayout.LEFT, 0, 0));
        item.setBorder(new EmptyBorder(0, 0, 0, 22));
        JLabel n = Ui.legend(color, name);
        item.add(n);
        item.add(Ui.hgap(7));
        item.add(Ui.label(Money.format(value), Theme.semibold(12.5f), Theme.TEXT));
        return item;
    }

    private static JComponent stat(String label, String value, String sub) {
        JPanel p = Ui.column();
        p.add(Ui.left(Ui.secondary(label, 12.5f)));
        p.add(Ui.gap(4));
        p.add(Ui.left(Ui.label(value, Theme.semibold(19f), Theme.TEXT)));
        p.add(Ui.gap(3));
        p.add(Ui.left(Ui.muted(sub, 12f)));
        return p;
    }

    private static int countThisMonth(AppData data, String type) {
        YearMonth now = YearMonth.now();
        int n = 0;
        for (Transaction t : data.transactions) {
            LocalDateTime time = t.time();
            if (type.equals(t.type) && time != null && YearMonth.from(time).equals(now)) n++;
        }
        return n;
    }

    // --------------------------------------------------
    // CASH FLOW
    // --------------------------------------------------

    private JComponent cashFlow(AppData data) {
        RoundedPanel card = new RoundedPanel(new BorderLayout(0, 14), 16, Theme.SURFACE, Theme.BORDER);
        card.padding(20, 22, 18, 22);

        JPanel header = Ui.transparent(new BorderLayout());
        JPanel titles = Ui.column();
        titles.add(Ui.left(Ui.title("Cash flow", 15.5f)));
        titles.add(Ui.gap(2));
        titles.add(Ui.left(Ui.muted("Money in and out, last 6 months", 12.5f)));
        header.add(titles, BorderLayout.WEST);

        JPanel legend = Ui.transparent(new FlowLayout(FlowLayout.RIGHT, 14, 0));
        legend.add(Ui.legend(Theme.MONEY_IN, "Money in"));
        legend.add(Ui.legend(Theme.MONEY_OUT, "Money out"));
        JPanel legendWrap = Ui.transparent(new BorderLayout());
        legendWrap.add(legend, BorderLayout.NORTH);
        header.add(legendWrap, BorderLayout.EAST);
        card.add(header, BorderLayout.NORTH);

        List<Stats.MonthFlow> flows = Stats.cashFlow(data, 6);
        if (!Stats.hasDatedActivity(flows)) {
            JPanel empty = Ui.transparent(new GridBagLayout());
            empty.setPreferredSize(new Dimension(300, 220));
            empty.add(Ui.muted("Deposits and expenses will show up here.", 13f));
            card.add(empty, BorderLayout.CENTER);
        } else {
            CashFlowChart chart = new CashFlowChart();
            chart.setData(flows);
            card.add(chart, BorderLayout.CENTER);
        }
        return card;
    }

    // --------------------------------------------------
    // GOAL PROGRESS
    // --------------------------------------------------

    private JComponent goalProgress(AppData data) {
        RoundedPanel card = new RoundedPanel(new BorderLayout(0, 14), 16, Theme.SURFACE, Theme.BORDER);
        card.padding(20, 22, 18, 22);
        card.add(cardHeader("Goal progress", data.goals.isEmpty() ? null : "View all", "goals"), BorderLayout.NORTH);

        if (data.goals.isEmpty()) {
            AppButton create = new AppButton("New goal", AppButton.Kind.PRIMARY, "plus").small();
            create.addActionListener(e -> Dialogs.goalEditor(window, null));
            card.add(Ui.emptyState("goals", "No goals yet",
                    "Goals are buckets for money you're saving, like an emergency fund or a trip.", create),
                    BorderLayout.CENTER);
            return card;
        }

        List<Goal> goals = new ArrayList<>(data.goals);
        goals.sort(Comparator.comparing(Goal::isReached)
                .thenComparing(Comparator.comparing(Goal::progressPercent).reversed()));

        JPanel list = Ui.column();
        int count = Math.min(5, goals.size());
        for (int i = 0; i < count; i++) {
            Goal g = goals.get(i);
            if (i > 0) list.add(Ui.gap(15));

            JPanel line = Ui.transparent(new BorderLayout(8, 0));
            JLabel name = new JLabel(g.name, Ui.dot(Theme.goalColor(g), 8), SwingConstants.LEFT);
            name.setIconTextGap(8);
            name.setFont(Theme.semibold(13f));
            name.setForeground(Theme.TEXT);
            line.add(name, BorderLayout.CENTER);
            JLabel pct = g.isReached()
                    ? Ui.label("Reached", Theme.semibold(12.5f), Theme.POSITIVE)
                    : Ui.label(Money.percent(g.progressPercent()), Theme.semibold(12.5f), Theme.TEXT_2);
            line.add(pct, BorderLayout.EAST);
            line.setMaximumSize(new Dimension(Integer.MAX_VALUE, 22));
            list.add(Ui.left(line));
            list.add(Ui.gap(7));

            ProgressBar bar = new ProgressBar(g.progressPercent().doubleValue() / 100, Theme.goalColor(g), 7);
            list.add(Ui.left(bar));
            list.add(Ui.gap(6));
            list.add(Ui.left(Ui.muted(Money.format(g.savedAmount) + " of " + Money.format(g.targetAmount), 12f)));
        }
        if (goals.size() > count) {
            list.add(Ui.gap(12));
            list.add(Ui.left(Ui.muted("+ " + (goals.size() - count) + " more", 12f)));
        }

        JPanel wrap = Ui.transparent(new BorderLayout());
        wrap.add(list, BorderLayout.NORTH);
        card.add(wrap, BorderLayout.CENTER);
        return card;
    }

    // --------------------------------------------------
    // RECENT ACTIVITY
    // --------------------------------------------------

    private JComponent recent(AppData data) {
        RoundedPanel card = new RoundedPanel(new BorderLayout(0, 8), 16, Theme.SURFACE, Theme.BORDER);
        card.padding(20, 22, 12, 22);
        card.add(cardHeader("Recent activity", data.transactions.isEmpty() ? null : "View all", "transactions"),
                BorderLayout.NORTH);

        if (data.transactions.isEmpty()) {
            AppButton add = new AppButton("Add money", AppButton.Kind.PRIMARY, "plus").small();
            add.addActionListener(e -> Dialogs.addMoney(window));
            card.add(Ui.emptyState("transactions", "No activity yet",
                    "Deposits, expenses and moves between goals will be listed here.", add), BorderLayout.CENTER);
            return card;
        }

        JPanel list = Ui.column();
        int count = Math.min(window.settings().recentCount, data.transactions.size());
        for (int i = 0; i < count; i++) {
            Transaction t = data.transactions.get(data.transactions.size() - 1 - i);
            list.add(Ui.left(activityRow(t, i > 0)));
        }
        card.add(list, BorderLayout.CENTER);
        return card;
    }

    static JComponent activityRow(Transaction t, boolean divider) {
        JPanel row = Ui.transparent(new BorderLayout(13, 0));
        row.setBorder(divider
                ? javax.swing.BorderFactory.createCompoundBorder(
                javax.swing.BorderFactory.createMatteBorder(1, 0, 0, 0, Theme.BORDER),
                new EmptyBorder(11, 0, 11, 0))
                : new EmptyBorder(8, 0, 11, 0));
        row.setMaximumSize(new Dimension(Integer.MAX_VALUE, 70));

        row.add(new JLabel(Ui.badge(iconFor(t), colorFor(t), 34)), BorderLayout.WEST);

        JPanel text = Ui.transparent(null);
        text.setLayout(new BoxLayout(text, BoxLayout.Y_AXIS));
        text.add(Ui.left(Ui.label(t.description, Theme.font(13.5f), Theme.TEXT)));
        text.add(Ui.gap(3));
        LocalDateTime time = t.time();
        String when = time == null ? t.date : time.format(WHEN);
        text.add(Ui.left(Ui.muted(when + "   " + t.bucket, 12f)));
        if (t.details != null) row.setToolTipText(t.details);
        row.add(text, BorderLayout.CENTER);

        int dir = t.direction();
        JLabel amount = Ui.label(Money.signed(t.amount, dir), Theme.semibold(13.5f),
                dir > 0 ? Theme.POSITIVE : dir < 0 ? Theme.NEGATIVE : Theme.TEXT_2);
        JPanel amountWrap = Ui.transparent(new GridBagLayout());
        amountWrap.add(amount);
        row.add(amountWrap, BorderLayout.EAST);
        return row;
    }

    static String iconFor(Transaction t) {
        switch (t.type) {
            case Transaction.DEPOSIT:
                return "in";
            case Transaction.EXPENSE:
                return "out";
            case Transaction.TRANSFER:
                return "transfer";
            case Transaction.REBALANCE:
                return "rebalance";
            case Transaction.GOAL_DELETED:
                return "trash";
            default:
                return "goals";
        }
    }

    static Color colorFor(Transaction t) {
        switch (t.type) {
            case Transaction.DEPOSIT:
                return Theme.MONEY_IN;
            case Transaction.EXPENSE:
                return Theme.MONEY_OUT;
            case Transaction.TRANSFER:
            case Transaction.REBALANCE:
                return Theme.SERIES[6];
            default:
                return Theme.TEXT_2;
        }
    }

    private JComponent cardHeader(String title, String link, String page) {
        JPanel header = Ui.transparent(new BorderLayout());
        header.add(Ui.title(title, 15.5f), BorderLayout.WEST);
        if (link != null) {
            JLabel l = Ui.label(link, Theme.semibold(12.5f), Theme.ACCENT_TEXT);
            Ui.onClick(l, e -> window.showPage(page));
            header.add(l, BorderLayout.EAST);
        }
        return header;
    }
}
