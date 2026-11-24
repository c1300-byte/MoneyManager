package com.carsen.moneymanager.ui.components;

import com.carsen.moneymanager.ui.Theme;
import com.carsen.moneymanager.ui.Ui;

import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.plaf.basic.BasicScrollBarUI;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.geom.RoundRectangle2D;

/** Slim rounded scrollbar without arrow buttons. */
public class ModernScrollBarUI extends BasicScrollBarUI {

    @Override
    protected void configureScrollBarColors() {
        trackColor = Theme.BG;
        thumbColor = Theme.BORDER_STRONG;
    }

    @Override
    protected JButton createDecreaseButton(int orientation) {
        return zero();
    }

    @Override
    protected JButton createIncreaseButton(int orientation) {
        return zero();
    }

    private static JButton zero() {
        JButton b = new JButton();
        b.setPreferredSize(new Dimension(0, 0));
        b.setMinimumSize(new Dimension(0, 0));
        b.setMaximumSize(new Dimension(0, 0));
        return b;
    }

    @Override
    public Dimension getPreferredSize(JComponent c) {
        return scrollbar.getOrientation() == VERTICAL ? new Dimension(12, 48) : new Dimension(48, 12);
    }

    @Override
    protected void paintTrack(Graphics g, JComponent c, Rectangle r) {
        // Transparent track: the thumb floats over the content's background.
    }

    @Override
    protected void paintThumb(Graphics graphics, JComponent c, Rectangle r) {
        if (r.isEmpty() || !scrollbar.isEnabled()) return;
        Graphics2D g = Ui.hq(graphics);
        boolean active = isThumbRollover() || isDragging;
        g.setColor(active ? Theme.MUTED : Theme.BORDER_STRONG);
        if (scrollbar.getOrientation() == VERTICAL) {
            int w = active ? 8 : 6;
            g.fill(new RoundRectangle2D.Double(r.x + (r.width - w) / 2.0, r.y + 3, w, r.height - 6, w, w));
        } else {
            int h = active ? 8 : 6;
            g.fill(new RoundRectangle2D.Double(r.x + 3, r.y + (r.height - h) / 2.0, r.width - 6, h, h, h));
        }
        g.dispose();
    }
}
