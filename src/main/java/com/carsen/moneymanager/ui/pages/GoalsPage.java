package com.carsen.moneymanager.ui.pages;

import com.carsen.moneymanager.core.Ledger;
import com.carsen.moneymanager.core.Money;
import com.carsen.moneymanager.model.AppData;
import com.carsen.moneymanager.model.Goal;
import com.carsen.moneymanager.ui.Icons;
import com.carsen.moneymanager.ui.MainWindow;
import com.carsen.moneymanager.ui.Theme;
import com.carsen.moneymanager.ui.Ui;
import com.carsen.moneymanager.ui.components.AllocationBar;
import com.carsen.moneymanager.ui.components.AppButton;
import com.carsen.moneymanager.ui.components.ProgressBar;
import com.carsen.moneymanager.ui.components.ResponsiveGrid;
import com.carsen.moneymanager.ui.components.RoundedPanel;
import com.carsen.moneymanager.ui.components.ScrollablePanel;
import com.carsen.moneymanager.ui.components.WrapLayout;
import com.carsen.moneymanager.ui.dialogs.Dialogs;

import javax.swing.BorderFactory;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.SwingConstants;
import javax.swing.border.EmptyBorder;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.math.BigDecimal;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;

public class GoalsPage extends BasePage {

    private static final DateTimeFormatter DEADLINE = DateTimeFormatter.ofPattern("MMM yyyy", Locale.US);

    private final ScrollablePanel content;
    private final Ui.Field search;
    private final Ui.Combo<String> sort;
    private final List<JComponent> actions = new ArrayList<>();
    private final JPanel top;
    private final JPanel splitHolder = Ui.transparent(new BorderLayout());
    private final JPanel gridHolder = Ui.transparent(new BorderLayout());
    private final JLabel countLabel = Ui.muted("", 12.5f);

    private static final String[] SORTS = {"Your order", "Progress", "Amount saved", "Deposit share", "Name"};

    public GoalsPage(MainWindow window) {
        super(window);

        content = new ScrollablePanel(new BorderLayout(0, 18));
        content.setBackground(Theme.BG);
        content.setBorder(new EmptyBorder(2, 34, 30, 34));
        add(Ui.scroll(content), BorderLayout.CENTER);

        search = Ui.searchField("Search goals");
        search.setPreferredSize(new Dimension(260, 38));
        Ui.onChange(search, this::refresh);

        sort = Ui.combo(SORTS);
        sort.setPreferredSize(new Dimension(190, 38));
        sort.addActionListener(e -> refresh());

        top = Ui.column();
        top.add(Ui.left(splitHolder));
        top.add(Ui.gap(22));
        top.add(Ui.left(toolbar()));
        content.add(top, BorderLayout.NORTH);
        content.add(gridHolder, BorderLayout.CENTER);

        AppButton rebalance = new AppButton("Rebalance", AppButton.Kind.SECONDARY, "rebalance");
        rebalance.setToolTipText("Re-split existing money by each goal's percentage");
        rebalance.addActionListener(e -> Dialogs.rebalance(window));

        AppButton add = new AppButton("New goal", AppButton.Kind.PRIMARY, "plus");
        add.setToolTipText("Create a goal (Ctrl+N)");
        add.addActionListener(e -> Dialogs.goalEditor(window, null));

        actions.add(rebalance);
        actions.add(add);
    }

    @Override
    public String title() {
        return "Goals";
    }

    @Override
    public String subtitle() {
        return "Buckets for the money you're saving";
    }

    @Override
    public List<JComponent> actions() {
        return actions;
    }

    @Override
    public void focusSearch() {
        search.requestFocusInWindow();
    }

    @Override
    public void refresh() {
        AppData data = window.data();
        gridHolder.removeAll();
        splitHolder.removeAll();

        if (data.goals.isEmpty()) {
            top.setVisible(false);
            AppButton create = new AppButton("Create your first goal", AppButton.Kind.PRIMARY, "plus");
            create.addActionListener(e -> Dialogs.goalEditor(window, null));
            RoundedPanel empty = new RoundedPanel(new BorderLayout());
            empty.add(Ui.emptyState("goals", "No goals yet",
                    "Give each goal a target and a share of every deposit, and it fills up on its own.",
                    create), BorderLayout.CENTER);
            gridHolder.add(empty, BorderLayout.NORTH);
        } else {
            top.setVisible(true);
            splitHolder.add(depositSplit(data), BorderLayout.CENTER);

            int reached = 0;
            for (Goal g : data.goals) if (g.isReached()) reached++;
            countLabel.setText(data.goals.size() + (data.goals.size() == 1 ? " goal" : " goals")
                    + (reached > 0 ? ", " + reached + " reached" : ""));

            List<Goal> goals = filteredAndSorted(data);
            if (goals.isEmpty()) {
                gridHolder.add(Ui.emptyState("search", "No matches",
                        "No goal matches \u201c" + search.getText().trim() + "\u201d.", null), BorderLayout.NORTH);
            } else {
                JPanel grid = Ui.transparent(new ResponsiveGrid(300, 3, 16));
                for (Goal g : goals) grid.add(card(g));
                gridHolder.add(grid, BorderLayout.NORTH);
            }
        }

        content.revalidate();
        content.repaint();
    }

    private List<Goal> filteredAndSorted(AppData data) {
        String q = search.getText().trim().toLowerCase(Locale.ROOT);
        List<Goal> list = new ArrayList<>();
        for (Goal g : data.goals) {
            if (q.isEmpty()
                    || g.name.toLowerCase(Locale.ROOT).contains(q)
                    || g.category.toLowerCase(Locale.ROOT).contains(q)
                    || g.goalType.toLowerCase(Locale.ROOT).contains(q)
                    || g.purposeLabel.toLowerCase(Locale.ROOT).contains(q)) {
                list.add(g);
            }
        }
        switch (sort.getSelectedIndex()) {
            case 1:
                list.sort(Comparator.comparing(Goal::progressPercent).reversed());
                break;
            case 2:
                list.sort(Comparator.comparing((Goal g) -> g.savedAmount).reversed());
                break;
            case 3:
                list.sort(Comparator.comparing((Goal g) -> g.allocationPercent).reversed());
                break;
            case 4:
                list.sort(Comparator.comparing(g -> g.name.toLowerCase(Locale.ROOT)));
                break;
            default:
                break;
        }
        return list;
    }

    // --------------------------------------------------
    // DEPOSIT SPLIT SUMMARY
    // --------------------------------------------------

    private JComponent depositSplit(AppData data) {
        Ledger ledger = new Ledger(data);
        RoundedPanel card = new RoundedPanel(new BorderLayout(0, 14), 16, Theme.SURFACE, Theme.BORDER);
        card.padding(20, 22, 18, 22);
        card.setMaximumSize(new Dimension(Integer.MAX_VALUE, 400));

        BigDecimal used = ledger.allocatedPercent();
        BigDecimal free = ledger.freePercent();

        JPanel header = Ui.transparent(new BorderLayout(20, 0));
        JPanel titles = Ui.column();
        titles.add(Ui.left(Ui.title("How each deposit is split", 15.5f)));
        titles.add(Ui.gap(3));
        String text;
        if (!window.settings().autoAllocateDeposits) {
            text = "Automatic split is off, so deposits go to Unallocated. Turn it on in Settings.";
        } else if (used.signum() == 0) {
            text = "None of your goals has a share yet, so deposits go to Unallocated.";
        } else {
            text = Money.percent(used) + " of every deposit goes to your goals"
                    + (free.signum() > 0 ? " and " + Money.percent(free) + " stays in Unallocated." : ".");
        }
        titles.add(Ui.left(Ui.secondary(text, 13f)));
        header.add(titles, BorderLayout.CENTER);
        card.add(header, BorderLayout.NORTH);

        AllocationBar bar = new AllocationBar(12);
        List<AllocationBar.Segment> segments = new ArrayList<>();
        JPanel legend = Ui.transparent(new WrapLayout(FlowLayout.LEFT, 0, 4));
        
        for (Goal g : data.goals) {
            if (!g.participates()) continue;
            Color color = window.settings().autoAllocateDeposits ? Theme.goalColor(g) : Theme.mix(Theme.goalColor(g), Theme.SURFACE, 0.5);
            segments.add(new AllocationBar.Segment(g.name, g.allocationPercent.doubleValue(), color,
                    "<html><b>" + Ui.escape(g.name) + "</b><br>" + Money.percent(g.allocationPercent) + " of each deposit</html>"));
            JPanel item = Ui.transparent(new FlowLayout(FlowLayout.LEFT, 0, 0));
            item.setBorder(new EmptyBorder(0, 0, 0, 20));
            item.add(Ui.legend(color, g.name));
            item.add(Ui.hgap(6));
            item.add(Ui.label(Money.percent(g.allocationPercent), Theme.semibold(12.5f), Theme.TEXT));
            legend.add(item);
        }
        if (free.signum() > 0) {
            segments.add(new AllocationBar.Segment(Ledger.UNALLOCATED, free.doubleValue(), Theme.UNALLOCATED,
                    "<html><b>Unallocated</b><br>" + Money.percent(free) + " of each deposit</html>"));
            JPanel item = Ui.transparent(new FlowLayout(FlowLayout.LEFT, 0, 0));
            item.add(Ui.legend(Theme.UNALLOCATED, "Unassigned"));
            item.add(Ui.hgap(6));
            item.add(Ui.label(Money.percent(free), Theme.semibold(12.5f), Theme.TEXT));
            legend.add(item);
        }
        bar.setSegments(segments);

        JPanel body = Ui.transparent(new BorderLayout(0, 10));
        body.add(bar, BorderLayout.NORTH);
        body.add(legend, BorderLayout.CENTER);
        card.add(body, BorderLayout.CENTER);
        return card;
    }

    private JComponent toolbar() {
        JPanel bar = Ui.transparent(new BorderLayout(10, 0));
        bar.setMaximumSize(new Dimension(Integer.MAX_VALUE, 40));
        JPanel left = Ui.transparent(new FlowLayout(FlowLayout.LEFT, 0, 0));
        left.add(search);
        left.add(Ui.hgap(10));
        left.add(sort);
        bar.add(left, BorderLayout.WEST);

        JPanel right = Ui.transparent(new java.awt.GridBagLayout());
        right.add(countLabel);
        bar.add(right, BorderLayout.EAST);
        return bar;
    }

    // --------------------------------------------------
    // GOAL CARD
    // --------------------------------------------------

    private JComponent card(Goal g) {
        Color color = Theme.goalColor(g);
        RoundedPanel card = new RoundedPanel(new BorderLayout(0, 14), 16, Theme.SURFACE, Theme.BORDER);
        card.padding(18, 20, 12, 16);

        // Title
        JPanel head = Ui.transparent(new BorderLayout(10, 0));
        JPanel names = Ui.column();
        JLabel name = new JLabel(g.name, Ui.dot(color, 10), SwingConstants.LEFT);
        name.setIconTextGap(9);
        name.setFont(Theme.semibold(15.5f));
        name.setForeground(Theme.TEXT);
        if (!g.purposeLabel.isBlank()) name.setToolTipText(g.purposeLabel);
        names.add(Ui.left(name));
        names.add(Ui.gap(3));
        String meta = g.category.isBlank() ? g.goalType : g.category + ",  " + g.goalType;
        JLabel metaLabel = Ui.muted(meta, 12f);
        metaLabel.setBorder(new EmptyBorder(0, 19, 0, 0));
        names.add(Ui.left(metaLabel));
        head.add(names, BorderLayout.CENTER);

        JLabel badge = g.isReached()
                ? pill("Reached", Theme.POSITIVE, "check")
                : pill(Money.percent(g.allocationPercent) + " of deposits", g.participates() ? Theme.TEXT_2 : Theme.MUTED, null);
        badge.setToolTipText(g.isReached() ? "This goal hit its target" : "Share of each deposit this goal receives");
        JPanel badgeWrap = Ui.transparent(new BorderLayout());
        badgeWrap.add(badge, BorderLayout.NORTH);
        head.add(badgeWrap, BorderLayout.EAST);
        card.add(head, BorderLayout.NORTH);

        // Amounts + progress
        JPanel middle = Ui.column();
        JPanel amounts = Ui.transparent(new BorderLayout());
        JPanel saved = Ui.transparent(new FlowLayout(FlowLayout.LEFT, 0, 0));
        saved.add(Ui.label(Money.format(g.savedAmount), Theme.semibold(22f), Theme.TEXT));
        saved.add(Ui.hgap(8));
        JLabel of = Ui.muted("of " + Money.format(g.targetAmount), 12.5f);
        of.setBorder(new EmptyBorder(8, 0, 0, 0));
        saved.add(of);
        amounts.add(saved, BorderLayout.WEST);
        JPanel pctWrap = Ui.transparent(new java.awt.GridBagLayout());
        pctWrap.add(Ui.label(Money.percent(g.progressPercent()), Theme.semibold(13f), Theme.TEXT_2));
        amounts.add(pctWrap, BorderLayout.EAST);
        middle.add(Ui.left(amounts));
        middle.add(Ui.gap(10));
        middle.add(Ui.left(new ProgressBar(g.progressPercent().doubleValue() / 100, color, 8)));
        middle.add(Ui.gap(10));

        // Deadline / remaining line
        JPanel facts = Ui.transparent(new BorderLayout(10, 0));
        String left;
        Color leftColor = Theme.TEXT_2;
        if (g.isReached()) {
            left = "Target reached";
            leftColor = Theme.POSITIVE;
        } else {
            left = Money.format(g.remaining()) + " to go";
        }
        facts.add(Ui.label(left, Theme.font(12.5f), leftColor), BorderLayout.WEST);

        YearMonth deadline = g.deadline();
        if (deadline != null && !g.isReached()) {
            BigDecimal monthly = Ledger.monthlyNeeded(g, YearMonth.now());
            boolean late = deadline.isBefore(YearMonth.now());
            String text = late
                    ? "Deadline passed (" + deadline.format(DEADLINE) + ")"
                    : deadline.format(DEADLINE) + ",  " + Money.format(monthly) + "/mo";
            JLabel due = Ui.label(text, Theme.font(12.5f), late ? Theme.WARNING : Theme.TEXT_2);
            due.setIcon(Icons.get(late ? "alert" : "calendar", 14, late ? Theme.WARNING : Theme.MUTED));
            due.setIconTextGap(6);
            due.setToolTipText(late ? "Edit the goal to set a new deadline"
                    : "Save this much each month to reach the target by " + deadline.format(DEADLINE));
            facts.add(due, BorderLayout.EAST);
        }
        middle.add(Ui.left(facts));
        card.add(middle, BorderLayout.CENTER);

        // Actions
        JPanel buttons = Ui.transparent(new BorderLayout());
        buttons.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createMatteBorder(1, 0, 0, 0, Theme.BORDER),
                new EmptyBorder(9, -6, 0, 0)));
        JPanel leftButtons = Ui.transparent(new FlowLayout(FlowLayout.LEFT, 2, 0));
        AppButton move = new AppButton("Move", AppButton.Kind.GHOST, "transfer").small();
        move.setToolTipText("Move money into or out of this goal");
        move.addActionListener(e -> Dialogs.transfer(window, null, g.id));
        AppButton spend = new AppButton("Spend", AppButton.Kind.GHOST, "minus").small();
        spend.setToolTipText("Record an expense paid from this goal");
        spend.addActionListener(e -> Dialogs.recordExpense(window, g.id));
        AppButton edit = new AppButton("Edit", AppButton.Kind.GHOST, "edit").small();
        edit.addActionListener(e -> Dialogs.goalEditor(window, g));
        leftButtons.add(move);
        leftButtons.add(spend);
        leftButtons.add(edit);
        buttons.add(leftButtons, BorderLayout.WEST);

        AppButton delete = new AppButton("", AppButton.Kind.GHOST, "trash").small();
        delete.setToolTipText("Delete goal");
        delete.addActionListener(e -> Dialogs.deleteGoal(window, g));
        buttons.add(delete, BorderLayout.EAST);
        card.add(buttons, BorderLayout.SOUTH);

        card.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                if (e.getClickCount() == 2) Dialogs.goalEditor(window, g);
            }
        });
        return card;
    }

    private static JLabel pill(String text, Color color, String icon) {
        JLabel l = new JLabel(text) {
            @Override
            protected void paintComponent(java.awt.Graphics graphics) {
                java.awt.Graphics2D g = Ui.hq(graphics);
                g.setColor(Theme.alpha(color, Theme.dark ? 30 : 24));
                g.fill(new java.awt.geom.RoundRectangle2D.Double(0, 0, getWidth(), getHeight(), getHeight(), getHeight()));
                g.dispose();
                super.paintComponent(graphics);
            }
        };
        if (icon != null) {
            l.setIcon(Icons.get(icon, 13, color));
            l.setIconTextGap(5);
        }
        l.setFont(Theme.semibold(11.5f));
        l.setForeground(color);
        l.setBorder(new EmptyBorder(4, 10, 4, 10));
        return l;
    }
}
