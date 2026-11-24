package com.carsen.moneymanager.ui;

import javax.swing.Icon;
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Component;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.geom.AffineTransform;
import java.awt.geom.Arc2D;
import java.awt.geom.Area;
import java.awt.geom.Ellipse2D;
import java.awt.geom.Line2D;
import java.awt.geom.Path2D;
import java.awt.geom.Rectangle2D;
import java.awt.geom.RoundRectangle2D;

/**
 * Small line icons drawn with Java2D, so they look sharp at any size and
 * never depend on which symbol fonts are installed.
 */
public final class Icons {

    private Icons() {
    }

    public static Icon get(String name, int size, Color color) {
        return new LineIcon(name, size, color);
    }

    private static final class LineIcon implements Icon {
        private final String name;
        private final int size;
        private final Color color;

        LineIcon(String name, int size, Color color) {
            this.name = name;
            this.size = size;
            this.color = color;
        }

        @Override
        public int getIconWidth() {
            return size;
        }

        @Override
        public int getIconHeight() {
            return size;
        }

        @Override
        public void paintIcon(Component c, Graphics graphics, int x, int y) {
            Graphics2D g = (Graphics2D) graphics.create();
            g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g.setRenderingHint(RenderingHints.KEY_STROKE_CONTROL, RenderingHints.VALUE_STROKE_PURE);
            g.translate(x, y);
            g.scale(size / 24.0, size / 24.0);
            g.setColor(color);
            g.setStroke(new BasicStroke(1.9f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
            draw(g, name);
            g.dispose();
        }
    }

    private static void draw(Graphics2D g, String name) {
        switch (name) {
            case "dashboard":
                g.draw(rr(3.5, 3.5, 7, 8.5));
                g.draw(rr(13.5, 3.5, 7, 5));
                g.draw(rr(13.5, 11.5, 7, 9));
                g.draw(rr(3.5, 15, 7, 5.5));
                break;
            case "goals":
                g.draw(circle(12, 12, 8.5));
                g.draw(circle(12, 12, 4.5));
                g.fill(circle(12, 12, 1.4));
                break;
            case "transactions":
                g.draw(line(8, 4.5, 8, 19.5));
                g.draw(poly(4.5, 8, 8, 4.5, 11.5, 8));
                g.draw(line(16, 4.5, 16, 19.5));
                g.draw(poly(12.5, 16, 16, 19.5, 19.5, 16));
                break;
            case "settings": {
                Area gear = new Area(circle(12, 12, 6.6));
                for (int i = 0; i < 8; i++) {
                    Rectangle2D tooth = new Rectangle2D.Double(10.4, 2.6, 3.2, 4);
                    gear.add(new Area(AffineTransform.getRotateInstance(Math.toRadians(i * 45), 12, 12)
                            .createTransformedShape(tooth)));
                }
                g.draw(gear);
                g.draw(circle(12, 12, 2.6));
                break;
            }
            case "plus":
                g.draw(line(12, 5, 12, 19));
                g.draw(line(5, 12, 19, 12));
                break;
            case "minus":
                g.draw(line(5, 12, 19, 12));
                break;
            case "transfer":
                g.draw(line(4, 8, 19, 8));
                g.draw(poly(15.5, 4.5, 19, 8, 15.5, 11.5));
                g.draw(line(20, 16, 5, 16));
                g.draw(poly(8.5, 12.5, 5, 16, 8.5, 19.5));
                break;
            case "edit":
                g.draw(poly(4.5, 19.5, 4.5, 15.8, 15, 5.3, 18.7, 9, 8.2, 19.5, 4.5, 19.5));
                g.draw(line(12.8, 7.5, 16.5, 11.2));
                break;
            case "trash":
                g.draw(line(4, 7, 20, 7));
                g.draw(poly(9, 7, 9, 4, 15, 4, 15, 7));
                g.draw(poly(6, 7, 7, 20, 17, 20, 18, 7));
                g.draw(line(10, 11, 10, 16));
                g.draw(line(14, 11, 14, 16));
                break;
            case "search":
                g.draw(circle(10.5, 10.5, 6.5));
                g.draw(line(15.5, 15.5, 20, 20));
                break;
            case "undo": {
                g.draw(poly(9, 4, 4, 9, 9, 14));
                Path2D p = new Path2D.Double();
                p.moveTo(4, 9);
                p.lineTo(13.5, 9);
                p.append(new Arc2D.Double(8, 9, 11, 11, 90, -180, Arc2D.OPEN), true);
                p.lineTo(10, 20);
                g.draw(p);
                break;
            }
            case "eye":
                g.draw(eye());
                g.draw(circle(12, 12, 3));
                break;
            case "eye-off":
                g.draw(eye());
                g.draw(circle(12, 12, 3));
                g.draw(line(4, 4, 20, 20));
                break;
            case "export":
                g.draw(line(12, 4, 12, 15));
                g.draw(poly(7.5, 10.5, 12, 15, 16.5, 10.5));
                g.draw(poly(4, 16, 4, 20, 20, 20, 20, 16));
                break;
            case "import":
                g.draw(line(12, 15, 12, 4));
                g.draw(poly(7.5, 8.5, 12, 4, 16.5, 8.5));
                g.draw(poly(4, 16, 4, 20, 20, 20, 20, 16));
                break;
            case "folder":
                g.draw(poly(3.5, 6.5, 9.5, 6.5, 11.5, 9, 20.5, 9, 20.5, 19, 3.5, 19, 3.5, 6.5));
                break;
            case "rebalance":
                g.draw(new Arc2D.Double(4, 4, 16, 16, 20, 150, Arc2D.OPEN));
                g.draw(poly(3.5, 8.2, 4.6, 12.4, 8.6, 11));
                g.draw(new Arc2D.Double(4, 4, 16, 16, 200, 150, Arc2D.OPEN));
                g.draw(poly(20.5, 15.8, 19.4, 11.6, 15.4, 13));
                break;
            case "sun":
                g.draw(circle(12, 12, 4));
                for (int i = 0; i < 8; i++) {
                    double a = Math.toRadians(i * 45);
                    g.draw(line(12 + Math.cos(a) * 7, 12 + Math.sin(a) * 7,
                            12 + Math.cos(a) * 9.5, 12 + Math.sin(a) * 9.5));
                }
                break;
            case "moon": {
                Area moon = new Area(circle(12, 12, 8));
                moon.subtract(new Area(circle(16.5, 8, 7)));
                g.draw(moon);
                break;
            }
            case "check":
                g.draw(poly(5, 12.5, 10, 17.5, 19, 7));
                break;
            case "in":
                g.draw(line(12, 5, 12, 19));
                g.draw(poly(6.5, 13.5, 12, 19, 17.5, 13.5));
                break;
            case "out":
                g.draw(line(12, 19, 12, 5));
                g.draw(poly(6.5, 10.5, 12, 5, 17.5, 10.5));
                break;
            case "more":
                g.fill(circle(6, 12, 1.7));
                g.fill(circle(12, 12, 1.7));
                g.fill(circle(18, 12, 1.7));
                break;
            case "close":
                g.draw(line(6.5, 6.5, 17.5, 17.5));
                g.draw(line(17.5, 6.5, 6.5, 17.5));
                break;
            case "calendar":
                g.draw(rr(4, 5.5, 16, 14.5));
                g.draw(line(4, 10, 20, 10));
                g.draw(line(8.5, 3.5, 8.5, 7));
                g.draw(line(15.5, 3.5, 15.5, 7));
                break;
            case "archive":
                g.draw(rr(3.5, 4, 17, 4.5));
                g.draw(poly(5, 8.5, 5, 20, 19, 20, 19, 8.5));
                g.draw(line(10, 12.5, 14, 12.5));
                break;
            case "alert":
                g.draw(poly(12, 4, 21, 19.5, 3, 19.5, 12, 4));
                g.draw(line(12, 10, 12, 14));
                g.fill(circle(12, 17, 1.1));
                break;
            case "chevron-down":
                g.draw(poly(6.5, 9.5, 12, 15, 17.5, 9.5));
                break;
            case "chevron-right":
                g.draw(poly(9.5, 6.5, 15, 12, 9.5, 17.5));
                break;
            case "wallet":
                g.draw(rr(3.5, 6.5, 17, 13));
                g.draw(poly(6, 6.5, 15, 3.5, 16.5, 6.5));
                g.fill(circle(16, 13, 1.4));
                break;
            case "keyboard":
                g.draw(rr(3, 6.5, 18, 11));
                g.draw(line(7, 14, 17, 14));
                g.fill(circle(7, 10.3, 0.9));
                g.fill(circle(10.3, 10.3, 0.9));
                g.fill(circle(13.7, 10.3, 0.9));
                g.fill(circle(17, 10.3, 0.9));
                break;
            default:
                g.draw(circle(12, 12, 8));
        }
    }

    private static Ellipse2D circle(double cx, double cy, double r) {
        return new Ellipse2D.Double(cx - r, cy - r, r * 2, r * 2);
    }

    private static RoundRectangle2D rr(double x, double y, double w, double h) {
        return new RoundRectangle2D.Double(x, y, w, h, 3.5, 3.5);
    }

    private static Line2D line(double x1, double y1, double x2, double y2) {
        return new Line2D.Double(x1, y1, x2, y2);
    }

    private static Path2D poly(double... points) {
        Path2D path = new Path2D.Double();
        path.moveTo(points[0], points[1]);
        for (int i = 2; i < points.length; i += 2) path.lineTo(points[i], points[i + 1]);
        return path;
    }

    private static Path2D eye() {
        Path2D p = new Path2D.Double();
        p.moveTo(2.5, 12);
        p.quadTo(12, 2.5, 21.5, 12);
        p.quadTo(12, 21.5, 2.5, 12);
        p.closePath();
        return p;
    }
}
