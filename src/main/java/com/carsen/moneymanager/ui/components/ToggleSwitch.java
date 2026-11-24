package com.carsen.moneymanager.ui.components;

import com.carsen.moneymanager.ui.Theme;
import com.carsen.moneymanager.ui.Ui;

import javax.swing.AbstractAction;
import javax.swing.JComponent;
import javax.swing.KeyStroke;
import javax.swing.Timer;
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.event.ActionEvent;
import java.awt.event.FocusAdapter;
import java.awt.event.FocusEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.geom.Ellipse2D;
import java.awt.geom.RoundRectangle2D;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

/** An on/off switch. Click it or press Space when focused. */
public class ToggleSwitch extends JComponent {

    private boolean on;
    private float position;
    private final List<Consumer<Boolean>> listeners = new ArrayList<>();
    private Timer animation;
    private boolean focused;

    public ToggleSwitch(boolean on) {
        this.on = on;
        this.position = on ? 1f : 0f;
        setPreferredSize(new Dimension(42, 24));
        setMinimumSize(new Dimension(42, 24));
        setMaximumSize(new Dimension(42, 24));
        setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        setFocusable(true);

        addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                if (isEnabled()) {
                    requestFocusInWindow();
                    toggle();
                }
            }
        });

        getInputMap().put(KeyStroke.getKeyStroke("SPACE"), "toggle");
        getActionMap().put("toggle", new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                if (isEnabled()) toggle();
            }
        });

        addFocusListener(new FocusAdapter() {
            @Override
            public void focusGained(FocusEvent e) {
                focused = AppButton.isKeyboard(e);
                repaint();
            }

            @Override
            public void focusLost(FocusEvent e) {
                focused = false;
                repaint();
            }
        });
    }

    public boolean isOn() {
        return on;
    }

    /** Change state without notifying listeners. */
    public void setOn(boolean value) {
        on = value;
        position = value ? 1f : 0f;
        repaint();
    }

    public void onChange(Consumer<Boolean> listener) {
        listeners.add(listener);
    }

    private void toggle() {
        on = !on;
        animate();
        for (Consumer<Boolean> l : listeners) l.accept(on);
    }

    private void animate() {
        if (animation != null) animation.stop();
        final float target = on ? 1f : 0f;
        animation = new Timer(12, e -> {
            float step = 0.2f;
            if (Math.abs(position - target) <= step) {
                position = target;
                ((Timer) e.getSource()).stop();
            } else {
                position += position < target ? step : -step;
            }
            repaint();
        });
        animation.start();
    }

    @Override
    public void setEnabled(boolean enabled) {
        super.setEnabled(enabled);
        setCursor(Cursor.getPredefinedCursor(enabled ? Cursor.HAND_CURSOR : Cursor.DEFAULT_CURSOR));
        repaint();
    }

    @Override
    protected void paintComponent(Graphics graphics) {
        Graphics2D g = Ui.hq(graphics);
        int w = 40;
        int h = 22;
        int x = (getWidth() - w) / 2;
        int y = (getHeight() - h) / 2;

        Color offTrack = Theme.dark ? Theme.BORDER_STRONG : Theme.mix(Theme.BORDER_STRONG, Theme.MUTED, 0.2);
        Color track = Theme.mix(offTrack, Theme.ACCENT, position);
        if (!isEnabled()) track = Theme.alpha(track, 90);

        g.setColor(track);
        g.fill(new RoundRectangle2D.Double(x, y, w, h, h, h));

        if (focused) {
            g.setColor(Theme.alpha(Theme.ACCENT_HOVER, 150));
            g.setStroke(new BasicStroke(2f));
            g.draw(new RoundRectangle2D.Double(x - 2, y - 2, w + 4, h + 4, h + 4, h + 4));
        }

        double knob = h - 6;
        double kx = x + 3 + (w - 6 - knob) * position;
        g.setColor(isEnabled() ? Color.WHITE : Theme.alpha(Color.WHITE, 150));
        g.fill(new Ellipse2D.Double(kx, y + 3, knob, knob));
        g.dispose();
    }
}
