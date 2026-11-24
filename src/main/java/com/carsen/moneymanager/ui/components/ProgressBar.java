package com.carsen.moneymanager.ui.components;

import com.carsen.moneymanager.ui.Theme;
import com.carsen.moneymanager.ui.Ui;

import javax.swing.JComponent;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.geom.RoundRectangle2D;

/** Thin rounded progress bar. */
public class ProgressBar extends JComponent {

    private double value;
    private Color color;
    private final int thickness;

    public ProgressBar(double value, Color color, int thickness) {
        this.value = value;
        this.color = color;
        this.thickness = thickness;
        setPreferredSize(new Dimension(100, thickness));
        setMinimumSize(new Dimension(20, thickness));
    }

    public void setValue(double value, Color color) {
        this.value = value;
        this.color = color;
        repaint();
    }

    @Override
    public Dimension getMaximumSize() {
        return new Dimension(Integer.MAX_VALUE, thickness);
    }

    @Override
    protected void paintComponent(Graphics graphics) {
        Graphics2D g = Ui.hq(graphics);
        int h = thickness;
        int y = (getHeight() - h) / 2;
        int w = getWidth();
        g.setColor(Theme.dark ? Theme.SURFACE_2 : Theme.mix(Theme.SURFACE_2, Theme.BORDER_STRONG, 0.35));
        g.fill(new RoundRectangle2D.Double(0, y, w, h, h, h));

        double v = Math.max(0, Math.min(1, value));
        if (v > 0) {
            double fw = Math.max(h, w * v);
            g.setColor(color);
            g.fill(new RoundRectangle2D.Double(0, y, fw, h, h, h));
        }
        g.dispose();
    }
}
