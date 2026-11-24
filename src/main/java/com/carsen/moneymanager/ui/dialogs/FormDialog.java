package com.carsen.moneymanager.ui.dialogs;

import com.carsen.moneymanager.ui.Icons;
import com.carsen.moneymanager.ui.Theme;
import com.carsen.moneymanager.ui.Ui;
import com.carsen.moneymanager.ui.components.AppButton;

import javax.swing.AbstractAction;
import javax.swing.BorderFactory;
import javax.swing.JComponent;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JRootPane;
import javax.swing.KeyStroke;
import javax.swing.SwingUtilities;
import javax.swing.border.EmptyBorder;
import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.GridLayout;
import java.awt.Point;
import java.awt.Window;
import java.awt.event.ActionEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;

/**
 * Themed modal dialog with a title, form rows, an inline error line and
 * Cancel / primary buttons. Enter submits, Esc cancels.
 */
public class FormDialog extends JDialog {

    /** Runs when the primary button is pressed. Throw to keep the dialog open. */
    public interface Submit {
        void run() throws Exception;
    }

    private final JPanel body;
    private final JLabel error;
    private final JPanel footerButtons;
    private AppButton primary;
    private final AppButton cancelButton;
    private Submit submit;
    private boolean confirmed;
    private JComponent firstFocus;
    private Point dragStart;

    public FormDialog(Window owner, String title, String subtitle) {
        this(owner, title, subtitle, 460);
    }

    public FormDialog(Window owner, String title, String subtitle, int width) {
        super(owner, title, ModalityType.APPLICATION_MODAL);
        setUndecorated(true);
        setDefaultCloseOperation(DISPOSE_ON_CLOSE);

        JPanel root = new JPanel(new BorderLayout());
        root.setBackground(Theme.SURFACE);
        root.setBorder(BorderFactory.createLineBorder(Theme.BORDER_STRONG));
        setContentPane(root);

        // Header (also the drag handle)
        JPanel header = Ui.transparent(new BorderLayout(12, 0));
        header.setBorder(new EmptyBorder(20, 24, 6, 16));
        JPanel titles = Ui.column();
        titles.add(Ui.left(Ui.title(title, 18f)));
        if (subtitle != null) {
            titles.add(Ui.gap(5));
            titles.add(Ui.left(Ui.paragraph(subtitle, width - 110, Theme.TEXT_2)));
        }
        header.add(titles, BorderLayout.CENTER);

        AppButton close = new AppButton("", AppButton.Kind.GHOST, "close").small();
        close.setToolTipText("Close (Esc)");
        close.addActionListener(e -> cancel());
        JPanel closeWrap = Ui.transparent(new BorderLayout());
        closeWrap.add(close, BorderLayout.NORTH);
        header.add(closeWrap, BorderLayout.EAST);

        MouseAdapter drag = new MouseAdapter() {
            @Override
            public void mousePressed(MouseEvent e) {
                dragStart = e.getPoint();
            }

            @Override
            public void mouseDragged(MouseEvent e) {
                if (dragStart == null) return;
                Point p = getLocation();
                setLocation(p.x + e.getX() - dragStart.x, p.y + e.getY() - dragStart.y);
            }
        };
        header.addMouseListener(drag);
        header.addMouseMotionListener(drag);
        root.add(header, BorderLayout.NORTH);

        body = Ui.column();
        body.setBorder(new EmptyBorder(14, 24, 4, 24));
        JPanel bodyHolder = new com.carsen.moneymanager.ui.components.ScrollablePanel(new BorderLayout());
        bodyHolder.setOpaque(false);
        bodyHolder.add(body, BorderLayout.NORTH);
        javax.swing.JScrollPane scroll = Ui.scroll(bodyHolder);
        scroll.setOpaque(false);
        scroll.getViewport().setOpaque(false);
        scroll.getViewport().setBackground(Theme.SURFACE);
        root.add(scroll, BorderLayout.CENTER);

        JPanel footer = Ui.transparent(new BorderLayout(0, 10));
        footer.setBorder(new EmptyBorder(8, 24, 20, 24));

        error = new JLabel();
        error.setFont(Theme.font(12.5f));
        error.setForeground(Theme.NEGATIVE);
        error.setIcon(Icons.get("alert", 15, Theme.NEGATIVE));
        error.setIconTextGap(8);
        error.setVisible(false);
        footer.add(error, BorderLayout.NORTH);

        footerButtons = Ui.transparent(new FlowLayout(FlowLayout.RIGHT, 8, 0));
        cancelButton = new AppButton("Cancel", AppButton.Kind.SECONDARY);
        cancelButton.addActionListener(e -> cancel());
        footerButtons.add(cancelButton);
        footer.add(footerButtons, BorderLayout.SOUTH);
        root.add(footer, BorderLayout.SOUTH);

        JRootPane rp = getRootPane();
        rp.getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW).put(KeyStroke.getKeyStroke("ESCAPE"), "cancel");
        rp.getActionMap().put("cancel", new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                cancel();
            }
        });

        addWindowListener(new WindowAdapter() {
            @Override
            public void windowOpened(WindowEvent e) {
                if (firstFocus != null) firstFocus.requestFocusInWindow();
            }
        });

        setMinimumSize(new Dimension(width, 10));
        setPreferredSize(null);
        this.width = width;
    }

    private final int width;

    // --------------------------------------------------

    /** Adds "label / control / hint" and returns the control. */
    public <T extends JComponent> T add(String label, T control, String hint) {
        JPanel row = Ui.formRow(label, control, hint);
        addBlock(row);
        if (firstFocus == null && control.isFocusable()) firstFocus = control;
        return control;
    }

    /** Several form rows next to each other. */
    public void addSideBySide(JComponent... rows) {
        JPanel line = Ui.transparent(new GridLayout(1, rows.length, 12, 0));
        for (JComponent r : rows) line.add(r);
        addBlock(line);
    }

    public void addBlock(JComponent component) {
        if (body.getComponentCount() > 0) body.add(Ui.gap(14));
        component.setAlignmentX(Component.LEFT_ALIGNMENT);
        body.add(component);
    }

    public void focusFirst(JComponent component) {
        firstFocus = component;
    }

    public AppButton setPrimary(String label, AppButton.Kind kind, Submit submit) {
        this.submit = submit;
        primary = new AppButton(label, kind);
        primary.addActionListener(e -> submit());
        footerButtons.add(primary);
        getRootPane().setDefaultButton(primary);
        return primary;
    }

    public void setCancelText(String text) {
        cancelButton.setText(text);
    }

    public void showError(String message) {
        error.setText("<html><div style='width:" + (width - 90) + "px'>" + Ui.escape(message) + "</div></html>");
        error.setVisible(true);
        relayout();
    }

    public void clearError() {
        if (error.isVisible()) {
            error.setVisible(false);
            relayout();
        }
    }

    private void submit() {
        if (submit == null) {
            confirmed = true;
            dispose();
            return;
        }
        try {
            submit.run();
            confirmed = true;
            dispose();
        } catch (IllegalArgumentException | IllegalStateException e) {
            showError(e.getMessage());
        } catch (Exception e) {
            showError(e.getMessage() == null ? e.toString() : e.getMessage());
        }
    }

    private void cancel() {
        confirmed = false;
        dispose();
    }

    /** Keeps the dialog on screen; the body scrolls if it gets taller than the screen. */
    private void fitToScreen() {
        java.awt.Rectangle screen = java.awt.GraphicsEnvironment.getLocalGraphicsEnvironment()
                .getMaximumWindowBounds();
        int h = Math.min(getHeight(), screen.height - 40);
        setSize(Math.max(width, getWidth()), h);
        int y = getY();
        if (y + h > screen.y + screen.height - 20) y = Math.max(screen.y + 20, screen.y + screen.height - 20 - h);
        if (y < screen.y) y = screen.y + 20;
        setLocation(getX(), y);
    }

    /** Shows the dialog and waits. Returns true if the primary action succeeded. */
    public boolean open() {
        pack();
        setSize(Math.max(width, getWidth()), getHeight());
        setLocationRelativeTo(getOwner());
        fitToScreen();
        SwingUtilities.invokeLater(() -> {
            if (firstFocus != null) firstFocus.requestFocusInWindow();
        });
        setVisible(true);
        return confirmed;
    }

    /** Repack after the body changed size (e.g. a preview grew). */
    public void relayout() {
        int w = getWidth();
        pack();
        setSize(Math.max(w, getWidth()), getHeight());
        if (isVisible()) fitToScreen();
        getContentPane().repaint();
    }
}
