package com.carsen.moneymanager.ui;

import com.carsen.moneymanager.core.Ledger;
import com.carsen.moneymanager.core.Money;
import com.carsen.moneymanager.data.DataStore;
import com.carsen.moneymanager.model.AppData;
import com.carsen.moneymanager.model.Settings;
import com.carsen.moneymanager.ui.components.AppButton;
import com.carsen.moneymanager.ui.components.Toast;
import com.carsen.moneymanager.ui.dialogs.Dialogs;
import com.carsen.moneymanager.ui.pages.BasePage;
import com.carsen.moneymanager.ui.pages.DashboardPage;
import com.carsen.moneymanager.ui.pages.GoalsPage;
import com.carsen.moneymanager.ui.pages.SettingsPage;
import com.carsen.moneymanager.ui.pages.TransactionsPage;

import javax.swing.AbstractAction;
import javax.swing.BorderFactory;
import javax.swing.ImageIcon;
import javax.swing.InputMap;
import javax.swing.JComponent;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.KeyStroke;
import javax.swing.border.EmptyBorder;
import java.awt.BorderLayout;
import java.awt.CardLayout;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Frame;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.GraphicsEnvironment;
import java.awt.Rectangle;
import java.awt.event.ActionEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.awt.geom.RoundRectangle2D;
import java.io.IOException;
import java.time.format.DateTimeFormatter;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;

/** The app window: sidebar, page header and the current page. */
public class MainWindow extends JFrame {

    public static final String VERSION = "2.0";

    private static final int UNDO_LIMIT = 30;
    private static final DateTimeFormatter SAVED_TIME = DateTimeFormatter.ofPattern("h:mm a", Locale.US);

    private final DataStore store;
    private AppData data;

    /** Each entry: {description, JSON before the change}. */
    private final Deque<String[]> undoStack = new ArrayDeque<>();

    private final Map<String, BasePage> pages = new LinkedHashMap<>();
    private final Map<String, NavItem> nav = new LinkedHashMap<>();

    private CardLayout cardLayout;
    private JPanel cards;
    private JLabel pageTitle;
    private JLabel pageSubtitle;
    private JPanel actionsPanel;
    private AppButton undoButton;
    private JLabel saveStatus;
    private String current;
    private boolean saveFailed;
    private String pendingToast;

    public MainWindow(DataStore store, AppData data) {
        super("Money Manager");
        this.store = store;
        this.data = data;
        this.current = data.settings.lastPage;
    }

    // --------------------------------------------------
    // ACCESS
    // --------------------------------------------------

    public AppData data() {
        return data;
    }

    public Settings settings() {
        return data.settings;
    }

    public Ledger ledger() {
        return new Ledger(data);
    }

    public DataStore store() {
        return store;
    }

    // --------------------------------------------------
    // OPEN / BUILD
    // --------------------------------------------------

    public void open() {
        Money.configure(settings().currency, settings().hideAmounts);
        Theme.apply(settings().theme);
        Theme.installLookAndFeel();

        setIconImages(Logo.windowIcons());
        setMinimumSize(new Dimension(1020, 680));
        setDefaultCloseOperation(DO_NOTHING_ON_CLOSE);
        addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
                close();
            }
        });

        build();
        restoreBounds();
        setVisible(true);
    }

    /** Builds (or rebuilds, after a theme change) everything inside the window. */
    private void build() {
        Toast.hide(getRootPane());
        pages.clear();
        nav.clear();

        JPanel root = new JPanel(new BorderLayout());
        root.setBackground(Theme.BG);
        root.add(buildSidebar(), BorderLayout.WEST);
        root.add(buildMain(), BorderLayout.CENTER);
        setContentPane(root);

        registerShortcuts();
        if (!pages.containsKey(current)) current = "dashboard";
        showPage(current);
        updateSaveStatus();
        root.revalidate();
        root.repaint();
    }

    private JPanel buildSidebar() {
        JPanel side = new JPanel(new BorderLayout());
        side.setBackground(Theme.SIDEBAR);
        side.setPreferredSize(new Dimension(224, 0));
        side.setBorder(BorderFactory.createMatteBorder(0, 0, 0, 1, Theme.BORDER));

        // Brand
        JPanel brand = Ui.transparent(new BorderLayout(11, 0));
        brand.setBorder(new EmptyBorder(24, 20, 26, 16));
        brand.add(new JLabel(new ImageIcon(Logo.image(36))), BorderLayout.WEST);
        JPanel names = Ui.column();
        names.add(Ui.left(Ui.title("Money Manager", 15f)));
        names.add(Ui.gap(2));
        names.add(Ui.left(Ui.muted("Personal finance", 12f)));
        brand.add(names, BorderLayout.CENTER);
        side.add(brand, BorderLayout.NORTH);

        // Main navigation
        JPanel links = Ui.column();
        links.setBorder(new EmptyBorder(0, 12, 0, 12));
        addNav(links, "dashboard", "Dashboard", "dashboard", "Ctrl+1");
        addNav(links, "goals", "Goals", "goals", "Ctrl+2");
        addNav(links, "transactions", "Transactions", "transactions", "Ctrl+3");
        side.add(links, BorderLayout.CENTER);

        // Bottom: settings, quick toggles, save status
        JPanel bottom = Ui.column();
        bottom.setBorder(new EmptyBorder(0, 12, 18, 12));
        addNav(bottom, "settings", "Settings", "settings", "Ctrl+4");

        JPanel divider = new JPanel();
        divider.setBackground(Theme.BORDER);
        divider.setMaximumSize(new Dimension(Integer.MAX_VALUE, 1));
        divider.setPreferredSize(new Dimension(10, 1));
        bottom.add(Ui.gap(12));
        bottom.add(Ui.left(divider));
        bottom.add(Ui.gap(12));

        JPanel status = Ui.transparent(new BorderLayout(6, 0));
        status.setBorder(new EmptyBorder(0, 8, 0, 0));
        status.setMaximumSize(new Dimension(Integer.MAX_VALUE, 32));
        saveStatus = Ui.muted("", 12f);
        status.add(saveStatus, BorderLayout.CENTER);

        JPanel toggles = Ui.transparent(new FlowLayout(FlowLayout.RIGHT, 2, 0));
        AppButton privacy = new AppButton("", AppButton.Kind.GHOST,
                settings().hideAmounts ? "eye-off" : "eye").small();
        privacy.setToolTipText(settings().hideAmounts ? "Show amounts (Ctrl+H)" : "Hide amounts (Ctrl+H)");
        privacy.addActionListener(e -> toggleHideAmounts());
        AppButton theme = new AppButton("", AppButton.Kind.GHOST, Theme.dark ? "sun" : "moon").small();
        theme.setToolTipText(Theme.dark ? "Switch to light theme" : "Switch to dark theme");
        theme.addActionListener(e -> setTheme(Theme.dark ? "light" : "dark"));
        toggles.add(privacy);
        toggles.add(theme);
        status.add(toggles, BorderLayout.EAST);
        bottom.add(Ui.left(status));

        side.add(bottom, BorderLayout.SOUTH);
        return side;
    }

    private void addNav(JPanel parent, String id, String label, String icon, String shortcut) {
        NavItem item = new NavItem(label, icon);
        item.setToolTipText(label + " (" + shortcut + ")");
        item.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                showPage(id);
            }
        });
        nav.put(id, item);
        if (parent.getComponentCount() > 0) parent.add(Ui.gap(4));
        parent.add(Ui.left(item));
    }

    private JPanel buildMain() {
        JPanel main = new JPanel(new BorderLayout());
        main.setBackground(Theme.BG);

        JPanel header = Ui.transparent(new BorderLayout(20, 0));
        header.setBorder(new EmptyBorder(26, 34, 18, 34));

        JPanel titles = Ui.column();
        pageTitle = Ui.title("", 25f);
        pageSubtitle = Ui.secondary("", 13.5f);
        titles.add(Ui.left(pageTitle));
        titles.add(Ui.gap(4));
        titles.add(Ui.left(pageSubtitle));
        header.add(titles, BorderLayout.CENTER);

        actionsPanel = Ui.transparent(new FlowLayout(FlowLayout.RIGHT, 8, 0));
        JPanel actionsWrap = Ui.transparent(new java.awt.GridBagLayout());
        actionsWrap.add(actionsPanel);
        header.add(actionsWrap, BorderLayout.EAST);

        undoButton = new AppButton("", AppButton.Kind.GHOST, "undo");
        undoButton.addActionListener(e -> undo());

        main.add(header, BorderLayout.NORTH);

        cardLayout = new CardLayout();
        cards = new JPanel(cardLayout);
        cards.setBackground(Theme.BG);

        addPage("dashboard", new DashboardPage(this));
        addPage("goals", new GoalsPage(this));
        addPage("transactions", new TransactionsPage(this));
        addPage("settings", new SettingsPage(this));

        main.add(cards, BorderLayout.CENTER);
        return main;
    }

    private void addPage(String id, BasePage page) {
        pages.put(id, page);
        cards.add(page, id);
    }

    public void showPage(String id) {
        BasePage page = pages.get(id);
        if (page == null) return;
        current = id;
        settings().lastPage = id;

        cardLayout.show(cards, id);
        pageTitle.setText(page.title());
        pageSubtitle.setText(page.subtitle());

        actionsPanel.removeAll();
        actionsPanel.add(undoButton);
        for (JComponent action : page.actions()) actionsPanel.add(action);
        actionsPanel.revalidate();
        actionsPanel.repaint();
        updateUndoButton();

        for (Map.Entry<String, NavItem> e : nav.entrySet()) e.getValue().setSelected(e.getKey().equals(id));

        page.refresh();
    }

    public String currentPage() {
        return current;
    }

    // --------------------------------------------------
    // CHANGES, UNDO, SAVING
    // --------------------------------------------------

    /**
     * Runs a change to the money data. If it throws, nothing is kept and the
     * error goes back to the caller (dialogs show it inline). If it works,
     * the change is saved, the screen refreshes and an Undo toast appears.
     */
    public void perform(String message, Runnable change) {
        String before = DataStore.snapshot(data);
        pendingToast = null;
        try {
            change.run();
            ledger().checkConsistency();
        } catch (RuntimeException e) {
            data = DataStore.restore(before);
            throw e;
        }

        undoStack.push(new String[]{message, before});
        while (undoStack.size() > UNDO_LIMIT) undoStack.removeLast();

        persist(true);
        refreshAll();

        String text = pendingToast != null ? pendingToast : message;
        pendingToast = null;
        if (text != null) Toast.show(getRootPane(), text, "Undo", this::undo);
    }

    /** Lets an operation replace the toast text perform() will show. */
    public void setPendingToast(String text) {
        pendingToast = text;
    }

    /** Swap in completely new data (restore, import, reset). Undoable. */
    public void replaceData(AppData replacement, String message) {
        String before = DataStore.snapshot(data);
        Settings keep = data.settings;
        replacement.settings = keep;
        DataStore.normalize(replacement);
        data = replacement;

        undoStack.push(new String[]{message, before});
        persist(true);
        refreshAll();
        Toast.show(getRootPane(), message, "Undo", this::undo);
    }

    public void undo() {
        if (undoStack.isEmpty()) return;
        String[] entry = undoStack.pop();
        Settings keep = data.settings;
        data = DataStore.restore(entry[1]);
        data.settings = keep;
        persist(true);
        refreshAll();
        Toast.show(getRootPane(), "Undid: " + entry[0], null, null);
    }

    /** Save a settings change (no backup, not undoable). */
    public void saveSettings() {
        persist(false);
    }

    private void persist(boolean backup) {
        try {
            store.save(data, backup, settings().backupsToKeep);
            saveFailed = false;
        } catch (IOException e) {
            saveFailed = true;
            Dialogs.message(this, "Couldn't save",
                    "Your changes are still on screen but weren't written to disk.\n\n" + e.getMessage(), true);
        }
        updateSaveStatus();
    }

    private void updateSaveStatus() {
        if (saveStatus == null) return;
        if (saveFailed) {
            saveStatus.setIcon(Ui.dot(Theme.NEGATIVE, 7));
            saveStatus.setText("Not saved");
            saveStatus.setForeground(Theme.NEGATIVE);
        } else {
            saveStatus.setIcon(Ui.dot(Theme.POSITIVE, 7));
            saveStatus.setIconTextGap(7);
            saveStatus.setForeground(Theme.MUTED);
            saveStatus.setText(store.lastSaved() == null ? "Saved"
                    : "Saved " + store.lastSaved().format(SAVED_TIME));
        }
        saveStatus.setToolTipText("Stored on this computer: " + DataStore.DATA_FILE);
    }

    private void updateUndoButton() {
        if (undoButton == null) return;
        boolean can = !undoStack.isEmpty();
        undoButton.setEnabled(can);
        undoButton.setToolTipText(can ? "Undo: " + undoStack.peek()[0] + " (Ctrl+Z)" : "Nothing to undo");
    }

    public void refreshAll() {
        BasePage page = pages.get(current);
        if (page != null) page.refresh();
        for (Map.Entry<String, BasePage> e : pages.entrySet()) {
            if (!e.getKey().equals(current)) e.getValue().refresh();
        }
        updateUndoButton();
    }

    // --------------------------------------------------
    // APPEARANCE
    // --------------------------------------------------

    public void setTheme(String theme) {
        settings().theme = theme;
        saveSettings();
        Theme.apply(theme);
        Theme.installLookAndFeel();
        build();
    }

    public void toggleHideAmounts() {
        settings().hideAmounts = !settings().hideAmounts;
        Money.configure(settings().currency, settings().hideAmounts);
        saveSettings();
        build();
    }

    public void setCurrency(String code) {
        settings().currency = code;
        Money.configure(code, settings().hideAmounts);
        saveSettings();
        refreshAll();
    }

    // --------------------------------------------------
    // SHORTCUTS
    // --------------------------------------------------

    private void registerShortcuts() {
        JComponent root = getRootPane();
        InputMap im = root.getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW);
        im.clear();
        root.getActionMap().clear();

        shortcut(im, "ctrl D", "deposit", () -> Dialogs.addMoney(this));
        shortcut(im, "ctrl E", "expense", () -> Dialogs.recordExpense(this, null));
        shortcut(im, "ctrl M", "move", () -> Dialogs.transfer(this, null, null));
        shortcut(im, "ctrl N", "goal", () -> Dialogs.goalEditor(this, null));
        shortcut(im, "ctrl Z", "undo", this::undo);
        shortcut(im, "ctrl H", "hide", this::toggleHideAmounts);
        shortcut(im, "ctrl F", "search", () -> {
            BasePage page = pages.get(current);
            if (page != null) page.focusSearch();
        });
        shortcut(im, "ctrl 1", "page1", () -> showPage("dashboard"));
        shortcut(im, "ctrl 2", "page2", () -> showPage("goals"));
        shortcut(im, "ctrl 3", "page3", () -> showPage("transactions"));
        shortcut(im, "ctrl 4", "page4", () -> showPage("settings"));
    }

    private void shortcut(InputMap im, String keys, String name, Runnable action) {
        im.put(KeyStroke.getKeyStroke(keys), name);
        getRootPane().getActionMap().put(name, new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                action.run();
            }
        });
    }

    // --------------------------------------------------
    // WINDOW STATE
    // --------------------------------------------------

    private void restoreBounds() {
        Settings s = settings();
        Rectangle saved = null;
        if (s.windowX != null && s.windowY != null && s.windowWidth != null && s.windowHeight != null) {
            saved = new Rectangle(s.windowX, s.windowY, s.windowWidth, s.windowHeight);
        }
        Rectangle screen = GraphicsEnvironment.getLocalGraphicsEnvironment().getMaximumWindowBounds();
        if (saved != null && screen.intersects(saved) && saved.width >= 800 && saved.height >= 500) {
            setBounds(saved);
        } else {
            setSize(Math.min(1320, screen.width - 40), Math.min(860, screen.height - 40));
            setLocationRelativeTo(null);
        }
        if (s.windowMaximized) setExtendedState(Frame.MAXIMIZED_BOTH);
    }

    private void close() {
        Settings s = settings();
        s.windowMaximized = (getExtendedState() & Frame.MAXIMIZED_BOTH) == Frame.MAXIMIZED_BOTH;
        if (!s.windowMaximized) {
            Rectangle b = getBounds();
            s.windowX = b.x;
            s.windowY = b.y;
            s.windowWidth = b.width;
            s.windowHeight = b.height;
        }
        try {
            store.save(data, false, s.backupsToKeep);
        } catch (IOException ignored) {
        }
        dispose();
        System.exit(0);
    }

    // --------------------------------------------------
    // SIDEBAR ITEM
    // --------------------------------------------------

    private static class NavItem extends JComponent {
        private final String text;
        private final String icon;
        private boolean selected;
        private boolean hover;

        NavItem(String text, String icon) {
            this.text = text;
            this.icon = icon;
            setFont(Theme.semibold(13.5f));
            setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
            setPreferredSize(new Dimension(200, 40));
            setMaximumSize(new Dimension(Integer.MAX_VALUE, 40));
            addMouseListener(new MouseAdapter() {
                @Override
                public void mouseEntered(MouseEvent e) {
                    hover = true;
                    repaint();
                }

                @Override
                public void mouseExited(MouseEvent e) {
                    hover = false;
                    repaint();
                }
            });
        }

        void setSelected(boolean value) {
            selected = value;
            repaint();
        }

        @Override
        protected void paintComponent(Graphics graphics) {
            Graphics2D g = Ui.hq(graphics);
            if (selected || hover) {
                g.setColor(selected ? Theme.ACCENT_SOFT : Theme.alpha(Theme.TEXT, Theme.dark ? 10 : 9));
                g.fill(new RoundRectangle2D.Double(0, 0, getWidth(), getHeight(), 10, 10));
            }
            if (selected) {
                g.setColor(Theme.ACCENT);
                g.fill(new RoundRectangle2D.Double(0, 10, 3, getHeight() - 20, 3, 3));
            }
            java.awt.Color color = selected ? Theme.ACCENT_TEXT : hover ? Theme.TEXT : Theme.TEXT_2;
            Icons.get(icon, 18, color).paintIcon(this, g, 14, (getHeight() - 18) / 2);
            g.setFont(getFont());
            g.setColor(color);
            java.awt.FontMetrics fm = g.getFontMetrics();
            g.drawString(text, 44, (getHeight() - fm.getHeight()) / 2 + fm.getAscent());
            g.dispose();
        }
    }
}
