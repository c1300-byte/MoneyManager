package com.carsen.moneymanager.ui.components;

import com.carsen.moneymanager.ui.Icons;
import com.carsen.moneymanager.ui.Theme;
import com.carsen.moneymanager.ui.Ui;

import javax.swing.JComponent;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.geom.RoundRectangle2D;
import java.util.function.Consumer;

/** A row of mutually exclusive options, e.g. Dark | Light. */
public class SegmentedControl extends JComponent {

    private final String[] labels;
    private final String[] icons;
    private int selected;
    private int hover = -1;
    private Consumer<Integer> listener;

    public SegmentedControl(String[] labels, String[] icons, int selected) {
        this.labels = labels;
        this.icons = icons;
        this.selected = selected;
        setFont(Theme.semibold(12.5f));
        setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));

        MouseAdapter mouse = new MouseAdapter() {
            @Override
            public void mouseMoved(MouseEvent e) {
                int i = indexAt(e.getX());
                if (i != hover) {
                    hover = i;
                    repaint();
                }
            }

            @Override
            public void mouseExited(MouseEvent e) {
                hover = -1;
                repaint();
            }

            @Override
            public void mousePressed(MouseEvent e) {
                int i = indexAt(e.getX());
                if (i >= 0 && i != SegmentedControl.this.selected) {
                    SegmentedControl.this.selected = i;
                    repaint();
                    if (listener != null) listener.accept(i);
                }
            }
        };
        addMouseListener(mouse);
        addMouseMotionListener(mouse);
    }

    public void onChange(Consumer<Integer> listener) {
        this.listener = listener;
    }

    private int segmentWidth() {
        FontMetrics fm = getFontMetrics(getFont());
        int max = 0;
        for (String l : labels) max = Math.max(max, fm.stringWidth(l));
        return max + 22 + (icons != null ? 22 : 0);
    }

    private int indexAt(int x) {
        int i = (x - 3) / segmentWidth();
        return i >= 0 && i < labels.length ? i : -1;
    }

    @Override
    public Dimension getPreferredSize() {
        return new Dimension(segmentWidth() * labels.length + 6, 36);
    }

    @Override
    public Dimension getMaximumSize() {
        return getPreferredSize();
    }

    @Override
    protected void paintComponent(Graphics graphics) {
        Graphics2D g = Ui.hq(graphics);
        int w = getWidth();
        int h = getHeight();
        g.setColor(Theme.SURFACE_2);
        g.fill(new RoundRectangle2D.Double(0, 0, w - 1, h - 1, 10, 10));
        g.setColor(Theme.BORDER);
        g.draw(new RoundRectangle2D.Double(0.5, 0.5, w - 2, h - 2, 10, 10));

        int sw = segmentWidth();
        FontMetrics fm = g.getFontMetrics(getFont());
        g.setFont(getFont());

        for (int i = 0; i < labels.length; i++) {
            int x = 3 + i * sw;
            if (i == selected) {
                g.setColor(Theme.dark ? Theme.BORDER_STRONG : Theme.SURFACE);
                g.fill(new RoundRectangle2D.Double(x, 3, sw, h - 7, 8, 8));
                if (!Theme.dark) {
                    g.setColor(Theme.BORDER_STRONG);
                    g.draw(new RoundRectangle2D.Double(x + 0.5, 3.5, sw - 1, h - 8, 8, 8));
                }
            } else if (i == hover) {
                g.setColor(Theme.alpha(Theme.TEXT, 14));
                g.fill(new RoundRectangle2D.Double(x, 3, sw, h - 7, 8, 8));
            }
            java.awt.Color color = i == selected ? Theme.TEXT : Theme.TEXT_2;
            int textW = fm.stringWidth(labels[i]);
            int contentW = textW + (icons != null ? 22 : 0);
            int cx = x + (sw - contentW) / 2;
            if (icons != null) {
                Icons.get(icons[i], 15, color).paintIcon(this, g, cx, (h - 15) / 2 - 1);
                cx += 22;
            }
            g.setColor(color);
            g.drawString(labels[i], cx, (h - fm.getHeight()) / 2 + fm.getAscent() - 1);
        }
        g.dispose();
    }
}
