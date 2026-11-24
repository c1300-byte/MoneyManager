package com.carsen.moneymanager.ui.components;

import com.carsen.moneymanager.ui.Icons;
import com.carsen.moneymanager.ui.Theme;
import com.carsen.moneymanager.ui.Ui;

import javax.swing.BorderFactory;
import javax.swing.JLabel;
import javax.swing.JLayeredPane;
import javax.swing.JPanel;
import javax.swing.JRootPane;
import javax.swing.Timer;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.geom.RoundRectangle2D;

/** Short confirmation message at the bottom of the window, with an optional action. */
public final class Toast {

    private static JPanel current;
    private static Timer timer;

    private Toast() {
    }

    public static void show(JRootPane root, String message, String actionLabel, Runnable action) {
        hide(root);

        JLayeredPane layers = root.getLayeredPane();

        JPanel toast = new JPanel(new BorderLayout(14, 0)) {
            @Override
            protected void paintComponent(Graphics graphics) {
                Graphics2D g = Ui.hq(graphics);
                g.setColor(Theme.SHADOW);
                g.fill(new RoundRectangle2D.Double(0, 3, getWidth(), getHeight() - 3, 14, 14));
                g.setColor(Theme.dark ? Theme.mix(Theme.SURFACE_2, Color.WHITE, 0.04) : new Color(0x1f2937));
                g.fill(new RoundRectangle2D.Double(0, 0, getWidth(), getHeight() - 3, 14, 14));
                g.dispose();
            }
        };
        toast.setOpaque(false);
        toast.setBorder(BorderFactory.createEmptyBorder(11, 14, 14, 14));

        JLabel text = new JLabel(message, Icons.get("check", 16, Theme.dark ? Theme.POSITIVE : new Color(0x6ee7b7)), JLabel.LEFT);
        text.setIconTextGap(9);
        text.setForeground(new Color(0xf3f6fb));
        text.setFont(Theme.font(13f));
        toast.add(text, BorderLayout.CENTER);

        JPanel right = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 0));
        right.setOpaque(false);

        if (actionLabel != null && action != null) {
            JLabel link = new JLabel(actionLabel);
            link.setFont(Theme.semibold(13f));
            link.setForeground(new Color(0x9cc3ff));
            link.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
            link.addMouseListener(new MouseAdapter() {
                @Override
                public void mouseClicked(MouseEvent e) {
                    hide(root);
                    action.run();
                }
            });
            right.add(link);
        }

        JLabel close = new JLabel(Icons.get("close", 14, new Color(0x9aa5b8)));
        close.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        close.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                hide(root);
            }
        });
        right.add(close);
        toast.add(right, BorderLayout.EAST);

        Dimension size = toast.getPreferredSize();
        int width = Math.min(Math.max(size.width, 320), root.getWidth() - 80);
        toast.setBounds((root.getWidth() - width) / 2 + 110, root.getHeight() - size.height - 28, width, size.height);
        if (toast.getX() + width > root.getWidth() - 20) toast.setLocation(root.getWidth() - width - 20, toast.getY());

        layers.add(toast, JLayeredPane.POPUP_LAYER);
        layers.revalidate();
        layers.repaint();
        current = toast;

        timer = new Timer(6000, e -> hide(root));
        timer.setRepeats(false);
        timer.start();
    }

    public static void hide(JRootPane root) {
        if (timer != null) timer.stop();
        if (current != null) {
            JLayeredPane layers = root.getLayeredPane();
            layers.remove(current);
            layers.repaint();
            current = null;
        }
    }
}
