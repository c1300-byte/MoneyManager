package com.carsen.moneymanager.ui.components;

import com.carsen.moneymanager.ui.Theme;
import com.carsen.moneymanager.ui.Ui;

import javax.swing.JComponent;
import javax.swing.ToolTipManager;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Shape;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.geom.Rectangle2D;
import java.awt.geom.RoundRectangle2D;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;

/**
 * One horizontal bar split into colored parts, e.g. where every dollar
 * sits right now. Hover a part to see its name and value.
 */
public class AllocationBar extends JComponent {

    public static class Segment {
        public final String label;
        public final double value;
        public final Color color;
        public final String tooltip;

        public Segment(String label, double value, Color color, String tooltip) {
            this.label = label;
            this.value = value;
            this.color = color;
            this.tooltip = tooltip;
        }
    }

    private List<Segment> segments = new ArrayList<>();
    private final int thickness;
    private final List<double[]> bounds = new ArrayList<>();
    private int hover = -1;

    public AllocationBar(int thickness) {
        this.thickness = thickness;
        setPreferredSize(new Dimension(200, thickness + 4));
        ToolTipManager.sharedInstance().registerComponent(this);
        ToolTipManager.sharedInstance().setInitialDelay(150);

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
        };
        addMouseListener(mouse);
        addMouseMotionListener(mouse);
    }

    public void setSegments(List<Segment> segments) {
        this.segments = segments;
        repaint();
    }

    @Override
    public Dimension getMaximumSize() {
        return new Dimension(Integer.MAX_VALUE, thickness + 4);
    }

    private int indexAt(int x) {
        for (int i = 0; i < bounds.size(); i++) {
            double[] b = bounds.get(i);
            if (x >= b[0] - 1 && x <= b[0] + b[1] + 1) return i;
        }
        return -1;
    }

    @Override
    public String getToolTipText(MouseEvent e) {
        int i = indexAt(e.getX());
        if (i < 0 || i >= segments.size()) return null;
        return segments.get(i).tooltip;
    }

    @Override
    protected void paintComponent(Graphics graphics) {
        Graphics2D g = Ui.hq(graphics);
        int w = getWidth();
        int h = thickness;
        int y = (getHeight() - h) / 2;
        double radius = Math.min(h, 10);

        Shape outline = new RoundRectangle2D.Double(0, y, w, h, radius, radius);

        double total = 0;
        for (Segment s : segments) total += Math.max(0, s.value);

        bounds.clear();

        if (total <= 0) {
            g.setColor(Theme.SURFACE_2);
            g.fill(outline);
            g.dispose();
            return;
        }

        g.clip(outline);

        int visible = 0;
        for (Segment s : segments) if (s.value > 0) visible++;
        double gap = 2;
        double usable = w - gap * Math.max(0, visible - 1);

        double x = 0;
        int drawn = 0;
        Function<Double, Double> width = v -> Math.max(3, usable * v);

        for (int i = 0; i < segments.size(); i++) {
            Segment s = segments.get(i);
            if (s.value <= 0) {
                bounds.add(new double[]{-10, 0});
                continue;
            }
            drawn++;
            double sw = drawn == visible ? w - x : width.apply(s.value / total);
            Color c = s.color;
            if (hover >= 0 && hover != i) c = Theme.mix(c, Theme.SURFACE, 0.45);
            g.setColor(c);
            g.fill(new Rectangle2D.Double(x, y, sw, h));
            bounds.add(new double[]{x, sw});
            x += sw + gap;
        }
        g.dispose();
    }
}
