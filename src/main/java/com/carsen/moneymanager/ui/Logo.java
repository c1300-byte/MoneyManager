package com.carsen.moneymanager.ui;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.RenderingHints;
import java.awt.geom.Path2D;
import java.awt.geom.RoundRectangle2D;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.List;

/** The app's chart-arrow logo, drawn in code at any size. */
public final class Logo {

    private Logo() {
    }

    public static BufferedImage image(int size) {
        BufferedImage image = new BufferedImage(size, size, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = image.createGraphics();
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g.setRenderingHint(RenderingHints.KEY_STROKE_CONTROL, RenderingHints.VALUE_STROKE_PURE);

        float s = size;
        g.setColor(new Color(31, 51, 83));
        g.fill(new RoundRectangle2D.Float(0, 0, s, s, s * 0.28f, s * 0.28f));

        g.setColor(new Color(83, 147, 255));
        g.setStroke(new BasicStroke(Math.max(1.5f, s * 0.075f), BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));

        Path2D line = new Path2D.Float();
        line.moveTo(s * 0.22, s * 0.68);
        line.lineTo(s * 0.40, s * 0.49);
        line.lineTo(s * 0.53, s * 0.58);
        line.lineTo(s * 0.76, s * 0.30);
        g.draw(line);

        Path2D head = new Path2D.Float();
        head.moveTo(s * 0.60, s * 0.30);
        head.lineTo(s * 0.77, s * 0.29);
        head.lineTo(s * 0.765, s * 0.46);
        g.draw(head);

        g.setColor(new Color(63, 205, 145));
        float dot = Math.max(3, s * 0.13f);
        g.fill(new java.awt.geom.Ellipse2D.Float(s * 0.22f - dot / 2, s * 0.68f - dot / 2, dot, dot));

        g.dispose();
        return image;
    }

    public static List<Image> windowIcons() {
        List<Image> icons = new ArrayList<>();
        for (int size : new int[]{16, 20, 24, 32, 40, 48, 64, 128, 256}) icons.add(image(size));
        return icons;
    }
}
