package com.carsen.moneymanager.ui.pages;

import com.carsen.moneymanager.core.Money;
import com.carsen.moneymanager.model.AppData;
import com.carsen.moneymanager.model.Transaction;
import com.carsen.moneymanager.ui.MainWindow;
import com.carsen.moneymanager.ui.Theme;
import com.carsen.moneymanager.ui.Ui;
import com.carsen.moneymanager.ui.components.AppButton;
import com.carsen.moneymanager.ui.components.RoundedPanel;
import com.carsen.moneymanager.ui.dialogs.Dialogs;

import javax.swing.BorderFactory;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.ListSelectionModel;
import javax.swing.SwingConstants;
import javax.swing.border.EmptyBorder;
import javax.swing.table.AbstractTableModel;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.TableCellRenderer;
import javax.swing.table.TableColumnModel;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.FileDialog;
import java.awt.FlowLayout;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.geom.RoundRectangle2D;
import java.math.BigDecimal;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class TransactionsPage extends BasePage {

    private static final DateTimeFormatter DATE = DateTimeFormatter.ofPattern("MMM d, yyyy", Locale.US);
    private static final DateTimeFormatter TIME = DateTimeFormatter.ofPattern("h:mm a", Locale.US);

    private static final String[] TYPES = {"All types", "Deposits", "Expenses", "Moves", "Goal changes"};
    private static final String[] PERIODS = {"All time", "This month", "Last month", "Last 3 months", "This year"};

    private final Ui.Field search;
    private final Ui.Combo<String> type;
    private final Ui.Combo<String> period;
    private final JLabel inLabel = new JLabel();
    private final JLabel outLabel = new JLabel();
    private final JLabel netLabel = new JLabel();
    private final Model model = new Model();
    private final JTable table;
    private final JPanel body = Ui.transparent(new BorderLayout());
    private final JScrollPane tableScroll;
    private final List<JComponent> actions = new ArrayList<>();
    private int hoverRow = -1;

    public TransactionsPage(MainWindow window) {
        super(window);

        JPanel page = Ui.transparent(new BorderLayout(0, 16));
        page.setBorder(new EmptyBorder(2, 34, 30, 34));
        add(page, BorderLayout.CENTER);

        // Filters
        search = Ui.searchField("Search description or goal");
        search.setPreferredSize(new Dimension(270, 38));
        Ui.onChange(search, this::refresh);
        type = Ui.combo(TYPES);
        type.setPreferredSize(new Dimension(160, 38));
        type.addActionListener(e -> refresh());
        period = Ui.combo(PERIODS);
        period.setPreferredSize(new Dimension(160, 38));
        period.addActionListener(e -> refresh());

        JPanel filters = Ui.transparent(new BorderLayout());
        JPanel left = Ui.transparent(new FlowLayout(FlowLayout.LEFT, 0, 0));
        left.add(search);
        left.add(Ui.hgap(10));
        left.add(type);
        left.add(Ui.hgap(10));
        left.add(period);
        filters.add(left, BorderLayout.WEST);

        JPanel totals = Ui.transparent(new FlowLayout(FlowLayout.RIGHT, 18, 9));
        for (JLabel l : new JLabel[]{inLabel, outLabel, netLabel}) {
            l.setFont(Theme.font(13f));
            totals.add(l);
        }
        filters.add(totals, BorderLayout.EAST);
        page.add(filters, BorderLayout.NORTH);

        // Table
        table = new JTable(model);
        table.setRowHeight(52);
        table.setShowVerticalLines(false);
        table.setShowHorizontalLines(false);
        table.setIntercellSpacing(new Dimension(0, 0));
        table.setFillsViewportHeight(true);
        table.setBackground(Theme.SURFACE);
        table.setForeground(Theme.TEXT);
        table.setSelectionBackground(Theme.SELECTION);
        table.setSelectionForeground(Theme.TEXT);
        table.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        table.setFocusable(false);
        table.getTableHeader().setReorderingAllowed(false);
        table.getTableHeader().setResizingAllowed(true);
        table.getTableHeader().setDefaultRenderer(new HeaderRenderer());
        table.getTableHeader().setPreferredSize(new Dimension(0, 40));
        table.getTableHeader().setBackground(Theme.SURFACE);

        TableColumnModel cols = table.getColumnModel();
        int[] widths = {150, 130, 330, 220, 130};
        for (int i = 0; i < widths.length; i++) cols.getColumn(i).setPreferredWidth(widths[i]);
        Renderer renderer = new Renderer();
        for (int i = 0; i < widths.length; i++) cols.getColumn(i).setCellRenderer(renderer);

        MouseAdapter hover = new MouseAdapter() {
            @Override
            public void mouseMoved(MouseEvent e) {
                int row = table.rowAtPoint(e.getPoint());
                if (row != hoverRow) {
                    hoverRow = row;
                    table.repaint();
                }
            }

            @Override
            public void mouseExited(MouseEvent e) {
                hoverRow = -1;
                table.repaint();
            }
        };
        table.addMouseListener(hover);
        table.addMouseMotionListener(hover);

        tableScroll = Ui.scroll(table);
        tableScroll.getViewport().setBackground(Theme.SURFACE);
        tableScroll.setColumnHeaderView(table.getTableHeader());
        tableScroll.getColumnHeader().setBackground(Theme.SURFACE);

        RoundedPanel card = new RoundedPanel(new BorderLayout(), 16, Theme.SURFACE, Theme.BORDER);
        card.padding(6, 10, 10, 10);
        card.add(body, BorderLayout.CENTER);
        page.add(card, BorderLayout.CENTER);

        AppButton export = new AppButton("Export CSV", AppButton.Kind.SECONDARY, "export");
        export.setToolTipText("Save the transactions shown below as a spreadsheet file");
        export.addActionListener(e -> exportCsv());
        actions.add(export);
    }

    @Override
    public String title() {
        return "Transactions";
    }

    @Override
    public String subtitle() {
        return "Every deposit, expense and move";
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
        List<Transaction> rows = filter(data);
        model.setRows(rows);

        BigDecimal in = BigDecimal.ZERO;
        BigDecimal out = BigDecimal.ZERO;
        for (Transaction t : rows) {
            if (t.direction() > 0) in = in.add(t.amount.abs());
            if (t.direction() < 0) out = out.add(t.amount.abs());
        }
        BigDecimal net = in.subtract(out);
        inLabel.setText(html("In", Money.format(in), Theme.POSITIVE));
        outLabel.setText(html("Out", Money.format(out), Theme.NEGATIVE));
        netLabel.setText(html("Net", Money.signed(net, net.signum()), Theme.TEXT));

        body.removeAll();
        if (data.transactions.isEmpty()) {
            AppButton add = new AppButton("Add money", AppButton.Kind.PRIMARY, "plus");
            add.addActionListener(e -> Dialogs.addMoney(window));
            body.add(Ui.emptyState("transactions", "No transactions yet",
                    "Everything you add, spend or move shows up here, newest first.", add), BorderLayout.CENTER);
        } else if (rows.isEmpty()) {
            body.add(Ui.emptyState("search", "Nothing matches these filters",
                    "Try a different search, type or time period.", null), BorderLayout.CENTER);
        } else {
            body.add(tableScroll, BorderLayout.CENTER);
        }
        body.revalidate();
        body.repaint();
    }

    private static String html(String label, String value, Color color) {
        return "<html><span style='color:" + hex(Theme.MUTED) + "'>" + label + "</span>&nbsp;&nbsp;"
                + "<b style='color:" + hex(color) + "'>" + Ui.escape(value) + "</b></html>";
    }

    private static String hex(Color c) {
        return String.format("#%02x%02x%02x", c.getRed(), c.getGreen(), c.getBlue());
    }

    private List<Transaction> filter(AppData data) {
        String q = search.getText().trim().toLowerCase(Locale.ROOT);
        int typeIndex = type.getSelectedIndex();
        int periodIndex = period.getSelectedIndex();
        YearMonth now = YearMonth.now();

        List<Transaction> result = new ArrayList<>();
        for (int i = data.transactions.size() - 1; i >= 0; i--) {
            Transaction t = data.transactions.get(i);

            if (!q.isEmpty()) {
                String hay = (t.description + " " + t.bucket + " " + t.type + " "
                        + (t.details == null ? "" : t.details)).toLowerCase(Locale.ROOT);
                if (!hay.contains(q)) continue;
            }

            boolean typeOk;
            switch (typeIndex) {
                case 1:
                    typeOk = Transaction.DEPOSIT.equals(t.type);
                    break;
                case 2:
                    typeOk = Transaction.EXPENSE.equals(t.type);
                    break;
                case 3:
                    typeOk = Transaction.TRANSFER.equals(t.type) || Transaction.REBALANCE.equals(t.type);
                    break;
                case 4:
                    typeOk = t.type.startsWith("Goal");
                    break;
                default:
                    typeOk = true;
            }
            if (!typeOk) continue;

            if (periodIndex > 0) {
                LocalDateTime time = t.time();
                if (time == null) continue;
                YearMonth ym = YearMonth.from(time);
                boolean ok;
                switch (periodIndex) {
                    case 1:
                        ok = ym.equals(now);
                        break;
                    case 2:
                        ok = ym.equals(now.minusMonths(1));
                        break;
                    case 3:
                        ok = !ym.isBefore(now.minusMonths(2));
                        break;
                    default:
                        ok = ym.getYear() == now.getYear();
                }
                if (!ok) continue;
            }
            result.add(t);
        }
        return result;
    }

    private void exportCsv() {
        List<Transaction> rows = model.rows;
        if (rows.isEmpty()) {
            Dialogs.message(window, "Nothing to export", "There are no transactions in the current view.", false);
            return;
        }
        FileDialog fd = new FileDialog(window, "Export transactions", FileDialog.SAVE);
        fd.setFile("money-manager-transactions-" + LocalDate.now() + ".csv");
        fd.setVisible(true);
        if (fd.getFile() == null) return;
        String name = fd.getFile().toLowerCase(Locale.ROOT).endsWith(".csv") ? fd.getFile() : fd.getFile() + ".csv";
        Path path = Paths.get(fd.getDirectory(), name);
        try {
            window.store().exportCsv(rows, path);
            com.carsen.moneymanager.ui.components.Toast.show(window.getRootPane(),
                    "Exported " + rows.size() + " transactions to " + path.getFileName(), null, null);
        } catch (Exception ex) {
            Dialogs.message(window, "Export failed", ex.getMessage(), true);
        }
    }

    // --------------------------------------------------
    // TABLE
    // --------------------------------------------------

    private static class Model extends AbstractTableModel {
        private static final String[] COLUMNS = {"Date", "Type", "Description", "From / to", "Amount"};
        List<Transaction> rows = new ArrayList<>();

        void setRows(List<Transaction> rows) {
            this.rows = rows;
            fireTableDataChanged();
        }

        @Override
        public int getRowCount() {
            return rows.size();
        }

        @Override
        public int getColumnCount() {
            return COLUMNS.length;
        }

        @Override
        public String getColumnName(int column) {
            return COLUMNS[column];
        }

        @Override
        public Object getValueAt(int row, int column) {
            return rows.get(row);
        }
    }

    private static class HeaderRenderer extends DefaultTableCellRenderer {
        @Override
        public Component getTableCellRendererComponent(JTable table, Object value, boolean selected,
                                                       boolean focus, int row, int column) {
            JLabel l = (JLabel) super.getTableCellRendererComponent(table, value, false, false, row, column);
            l.setFont(Theme.semibold(12f));
            l.setForeground(Theme.MUTED);
            l.setBackground(Theme.SURFACE);
            l.setOpaque(true);
            l.setHorizontalAlignment(column == 4 ? SwingConstants.RIGHT : SwingConstants.LEFT);
            l.setBorder(BorderFactory.createCompoundBorder(
                    BorderFactory.createMatteBorder(0, 0, 1, 0, Theme.BORDER),
                    new EmptyBorder(0, 14, 0, 14)));
            return l;
        }
    }

    private class Renderer implements TableCellRenderer {
        private final JLabel label = new JLabel();
        private final Pill pill = new Pill();
        private final JPanel dateCell = new JPanel(new BorderLayout());
        private final JLabel dateTop = new JLabel();
        private final JLabel dateBottom = new JLabel();
        private final JPanel descCell = new JPanel(new BorderLayout());
        private final JLabel descTop = new JLabel();
        private final JLabel descBottom = new JLabel();

        Renderer() {
            label.setOpaque(true);
            dateCell.setOpaque(true);
            dateTop.setFont(Theme.font(13f));
            dateBottom.setFont(Theme.font(11.5f));
            dateCell.add(dateTop, BorderLayout.CENTER);
            dateCell.add(dateBottom, BorderLayout.SOUTH);
            dateCell.setBorder(new EmptyBorder(8, 14, 8, 14));
            descCell.setOpaque(true);
            descTop.setFont(Theme.font(13f));
            descBottom.setFont(Theme.font(11.5f));
            descCell.add(descTop, BorderLayout.CENTER);
            descCell.add(descBottom, BorderLayout.SOUTH);
        }

        @Override
        public Component getTableCellRendererComponent(JTable table, Object value, boolean selected,
                                                       boolean focus, int row, int column) {
            Transaction t = (Transaction) value;
            Color bg = selected ? Theme.SELECTION
                    : row == hoverRow ? Theme.mix(Theme.SURFACE, Theme.TEXT, Theme.dark ? 0.035 : 0.025)
                    : Theme.SURFACE;
            javax.swing.border.Border line = BorderFactory.createMatteBorder(0, 0, 1, 0, Theme.BORDER);

            if (column == 0) {
                LocalDateTime time = t.time();
                dateTop.setText(time == null ? t.date : time.format(DATE));
                dateTop.setForeground(Theme.TEXT);
                dateBottom.setText(time == null ? "" : time.format(TIME));
                dateBottom.setForeground(Theme.MUTED);
                dateCell.setBackground(bg);
                dateCell.setBorder(BorderFactory.createCompoundBorder(line, new EmptyBorder(8, 14, 7, 14)));
                return dateCell;
            }
            if (column == 1) {
                pill.set(t, bg, line);
                return pill;
            }

            label.setBackground(bg);
            label.setBorder(BorderFactory.createCompoundBorder(line, new EmptyBorder(0, 14, 0, 14)));
            label.setToolTipText(t.details);
            label.setIcon(null);

            if (column == 2) {
                descTop.setText(t.description);
                descTop.setForeground(Theme.TEXT);
                descBottom.setText(t.details == null ? "" : t.details);
                descBottom.setForeground(Theme.MUTED);
                descBottom.setVisible(t.details != null);
                descCell.setBackground(bg);
                descCell.setToolTipText(t.details);
                descCell.setBorder(BorderFactory.createCompoundBorder(line,
                        t.details == null ? new EmptyBorder(0, 14, 0, 14) : new EmptyBorder(8, 14, 7, 14)));
                return descCell;
            }
            if (column == 3) {
                label.setText(t.bucket);
                label.setFont(Theme.font(13f));
                label.setForeground(Theme.TEXT_2);
                label.setHorizontalAlignment(SwingConstants.LEFT);
            } else {
                int dir = t.direction();
                label.setText(Money.signed(t.amount, dir));
                label.setFont(Theme.semibold(13.5f));
                label.setForeground(dir > 0 ? Theme.POSITIVE : dir < 0 ? Theme.NEGATIVE : Theme.TEXT_2);
                label.setHorizontalAlignment(SwingConstants.RIGHT);
            }
            return label;
        }
    }

    /** Colored rounded tag for the transaction type. */
    private static class Pill extends JComponent {
        private String text = "";
        private Color color = Color.GRAY;
        private Color bg;

        void set(Transaction t, Color bg, javax.swing.border.Border border) {
            this.bg = bg;
            setBorder(border);
            switch (t.type) {
                case Transaction.DEPOSIT:
                    text = "Deposit";
                    color = Theme.MONEY_IN;
                    break;
                case Transaction.EXPENSE:
                    text = "Expense";
                    color = Theme.MONEY_OUT;
                    break;
                case Transaction.TRANSFER:
                    text = "Move";
                    color = Theme.SERIES[6];
                    break;
                case Transaction.REBALANCE:
                    text = "Rebalance";
                    color = Theme.SERIES[6];
                    break;
                case Transaction.GOAL_CREATED:
                    text = "New goal";
                    color = Theme.TEXT_2;
                    break;
                case Transaction.GOAL_DELETED:
                    text = "Goal deleted";
                    color = Theme.TEXT_2;
                    break;
                default:
                    text = t.type;
                    color = Theme.TEXT_2;
            }
        }

        @Override
        protected void paintComponent(Graphics graphics) {
            Graphics2D g = Ui.hq(graphics);
            g.setColor(bg);
            g.fillRect(0, 0, getWidth(), getHeight());
            g.setFont(Theme.semibold(11.5f));
            java.awt.FontMetrics fm = g.getFontMetrics();
            int w = fm.stringWidth(text) + 20;
            int h = 24;
            int x = 14;
            int y = (getHeight() - h) / 2;
            g.setColor(Theme.alpha(color, Theme.dark ? 36 : 26));
            g.fill(new RoundRectangle2D.Double(x, y, w, h, h, h));
            g.setColor(Theme.dark ? Theme.mix(color, Color.WHITE, 0.25) : Theme.mix(color, Color.BLACK, 0.15));
            g.drawString(text, x + 10, y + (h - fm.getHeight()) / 2 + fm.getAscent());
            g.dispose();
        }
    }
}
