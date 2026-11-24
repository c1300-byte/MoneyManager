package com.carsen.moneymanager.ui.components;

import com.carsen.moneymanager.ui.Theme;
import com.carsen.moneymanager.ui.Ui;

import javax.swing.JPanel;
import javax.swing.border.EmptyBorder;
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.LayoutManager;
import java.awt.geom.RoundRectangle2D;

/** A panel with rounded corners and a hairline border. */
public class RoundedPanel extends JPanel {

    private Color fill;
    private Color stroke;
    private final int radius;

    public RoundedPanel(LayoutManager layout) {
        this(layout, 14, Theme.SURFACE, Theme.BORDER);
    }

    public RoundedPanel(LayoutManager layout, int radius, Color fill, Color stroke) {
        super(layout);
        this.radius = radius;
        this.fill = fill;
        this.stroke = stroke;
        setOpaque(false);
        setBorder(new EmptyBorder(18, 20, 18, 20));
    }

    public RoundedPanel padding(int top, int left, int bottom, int right) {
        setBorder(new EmptyBorder(top, left, bottom, right));
        return this;
    }

    public void setFill(Color fill) {
        this.fill = fill;
        repaint();
    }

    public void setStroke(Color stroke) {
        this.stroke = stroke;
        repaint();
    }

    @Override
    protected void paintComponent(Graphics graphics) {
        Graphics2D g = Ui.hq(graphics);
        double w = getWidth() - 1;
        double h = getHeight() - 1;
        if (fill != null) {
            g.setColor(fill);
            g.fill(new RoundRectangle2D.Double(0, 0, w, h, radius, radius));
        }
        if (stroke != null) {
            g.setColor(stroke);
            g.setStroke(new BasicStroke(1f));
            g.draw(new RoundRectangle2D.Double(0.5, 0.5, w - 0.5, h - 0.5, radius, radius));
        }
        g.dispose();
        super.paintComponent(graphics);
    }
}
