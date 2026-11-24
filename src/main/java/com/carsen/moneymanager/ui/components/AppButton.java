package com.carsen.moneymanager.ui.components;

import com.carsen.moneymanager.ui.Icons;
import com.carsen.moneymanager.ui.Theme;
import com.carsen.moneymanager.ui.Ui;

import javax.swing.ButtonModel;
import javax.swing.JButton;
import javax.swing.border.EmptyBorder;
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.event.FocusAdapter;
import java.awt.event.FocusEvent;
import java.awt.geom.RoundRectangle2D;

/** Flat, rounded button in four styles. */
public class AppButton extends JButton {

    public enum Kind { PRIMARY, SECONDARY, GHOST, DANGER }

    private final Kind kind;
    private final String iconName;
    private boolean small;
    private boolean focusVisible;

    public AppButton(String text, Kind kind) {
        this(text, kind, null);
    }

    public AppButton(String text, Kind kind, String iconName) {
        super(text);
        this.kind = kind;
        this.iconName = iconName;

        setContentAreaFilled(false);
        setBorderPainted(false);
        setFocusPainted(false);
        setOpaque(false);
        setRolloverEnabled(true);
        setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        setFont(Theme.semibold(13f));
        setIconTextGap(7);
        applySize();
        updateColors();

        addFocusListener(new FocusAdapter() {
            @Override
            public void focusGained(FocusEvent e) {
                focusVisible = isKeyboard(e);
                repaint();
            }

            @Override
            public void focusLost(FocusEvent e) {
                focusVisible = false;
                repaint();
            }
        });
    }

    /** True when focus arrived by Tab, so the ring doesn't show after a mouse click. */
    public static boolean isKeyboard(FocusEvent e) {
        FocusEvent.Cause cause = e.getCause();
        return cause == FocusEvent.Cause.TRAVERSAL
                || cause == FocusEvent.Cause.TRAVERSAL_FORWARD
                || cause == FocusEvent.Cause.TRAVERSAL_BACKWARD
                || cause == FocusEvent.Cause.TRAVERSAL_UP
                || cause == FocusEvent.Cause.TRAVERSAL_DOWN;
    }

    /** Smaller padding for buttons inside cards. */
    public AppButton small() {
        small = true;
        setFont(Theme.semibold(12f));
        applySize();
        updateColors();
        return this;
    }

    private void applySize() {
        boolean iconOnly = getText() == null || getText().isEmpty();
        if (iconOnly) {
            int s = small ? 30 : 36;
            setBorder(new EmptyBorder(0, 0, 0, 0));
            setPreferredSize(new Dimension(s, s));
        } else if (small) {
            setBorder(new EmptyBorder(6, 11, 6, 12));
        } else {
            setBorder(new EmptyBorder(9, 15, 9, 16));
        }
    }

    private Color textColor() {
        switch (kind) {
            case PRIMARY:
                return Theme.ON_ACCENT;
            case DANGER:
                return Theme.NEGATIVE;
            case GHOST:
                return Theme.TEXT_2;
            default:
                return Theme.TEXT;
        }
    }

    private void updateColors() {
        Color color = isEnabled() ? textColor() : Theme.MUTED;
        setForeground(color);
        if (iconName != null) {
            setIcon(Icons.get(iconName, small ? 15 : 16, color));
        }
    }

    @Override
    public void setEnabled(boolean enabled) {
        super.setEnabled(enabled);
        updateColors();
        setCursor(Cursor.getPredefinedCursor(enabled ? Cursor.HAND_CURSOR : Cursor.DEFAULT_CURSOR));
    }

    @Override
    protected void paintComponent(Graphics graphics) {
        Graphics2D g = Ui.hq(graphics);
        ButtonModel m = getModel();
        boolean hover = m.isRollover() && isEnabled();
        boolean pressed = m.isPressed() && isEnabled();

        Color fill = null;
        Color border = null;

        switch (kind) {
            case PRIMARY:
                fill = !isEnabled() ? Theme.SURFACE_2
                        : pressed ? Theme.mix(Theme.ACCENT, Color.BLACK, 0.12)
                        : hover ? Theme.ACCENT_HOVER : Theme.ACCENT;
                break;
            case SECONDARY:
                fill = pressed ? Theme.mix(Theme.SURFACE_2, Theme.TEXT, 0.08)
                        : hover ? Theme.mix(Theme.SURFACE_2, Theme.TEXT, 0.04) : Theme.SURFACE_2;
                border = Theme.BORDER;
                break;
            case GHOST:
                if (hover || pressed) fill = Theme.SURFACE_2;
                break;
            case DANGER:
                fill = hover || pressed ? Theme.NEGATIVE_SOFT : null;
                border = Theme.alpha(Theme.NEGATIVE, 110);
                break;
        }

        double w = getWidth() - 1;
        double h = getHeight() - 1;
        int arc = small ? 8 : 10;

        if (fill != null) {
            g.setColor(fill);
            g.fill(new RoundRectangle2D.Double(0, 0, w, h, arc, arc));
        }
        if (border != null) {
            g.setColor(border);
            g.draw(new RoundRectangle2D.Double(0.5, 0.5, w - 0.5, h - 0.5, arc, arc));
        }
        if (focusVisible && isEnabled()) {
            g.setColor(Theme.alpha(Theme.ACCENT_HOVER, 170));
            g.setStroke(new BasicStroke(2f));
            g.draw(new RoundRectangle2D.Double(1, 1, w - 2, h - 2, arc, arc));
        }
        g.dispose();
        super.paintComponent(graphics);
    }
}
