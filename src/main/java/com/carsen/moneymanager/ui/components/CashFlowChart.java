package com.carsen.moneymanager.ui.components;

import com.carsen.moneymanager.core.Money;
import com.carsen.moneymanager.core.Stats;
import com.carsen.moneymanager.ui.Theme;
import com.carsen.moneymanager.ui.Ui;

import javax.swing.JComponent;
import javax.swing.ToolTipManager;
import java.awt.Dimension;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.geom.Line2D;
import java.awt.geom.Path2D;
import java.awt.geom.RoundRectangle2D;
import java.math.BigDecimal;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/** Grouped bars: money in vs money out for each recent month. */
public class CashFlowChart extends JComponent {

    private static final DateTimeFormatter MONTH = DateTimeFormatter.ofPattern("MMM", Locale.US);
    private static final DateTimeFormatter MONTH_YEAR = DateTimeFormatter.ofPattern("MMMM yyyy", Locale.US);

    private List<Stats.MonthFlow> flows = new ArrayList<>();
    private int hover = -1;

    private static final int LEFT = 52;
    private static final int BOTTOM = 26;
    private static final int TOP = 10;

    public CashFlowChart() {
        setPreferredSize(new Dimension(400, 220));
        setFont(Theme.font(11.5f));
        ToolTipManager.sharedInstance().registerComponent(this);

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

    public void setData(List<Stats.MonthFlow> flows) {
        this.flows = flows;
        repaint();
    }

    private double groupWidth() {
        return flows.isEmpty() ? 0 : (getWidth() - LEFT - 4) / (double) flows.size();
    }

    private int indexAt(int x) {
        if (flows.isEmpty() || x < LEFT) return -1;
        int i = (int) ((x - LEFT) / groupWidth());
        return i >= 0 && i < flows.size() ? i : -1;
    }

    @Override
    public String getToolTipText(MouseEvent e) {
        int i = indexAt(e.getX());
        if (i < 0) return null;
        Stats.MonthFlow f = flows.get(i);
        return "<html><b>" + f.month.format(MONTH_YEAR) + "</b><br>"
                + "Money in: " + Money.format(f.in) + "<br>"
                + "Money out: " + Money.format(f.out) + "<br>"
                + "Net: " + Money.signed(f.net(), f.net().signum()) + "</html>";
    }

    /** Smallest round step (1, 2, 2.5, 5 x 10^n) that covers max in at most 5 ticks. */
    private static double niceStep(double max) {
        if (max <= 0) return 25;
        double exp = Math.pow(10, Math.floor(Math.log10(max / 5)));
        for (double m : new double[]{1, 2, 2.5, 5, 10, 20}) {
            double step = m * exp;
            if (Math.ceil(max / step) <= 5) return step;
        }
        return 20 * exp;
    }

    @Override
    protected void paintComponent(Graphics graphics) {
        Graphics2D g = Ui.hq(graphics);
        g.setFont(getFont());
        FontMetrics fm = g.getFontMetrics();

        int w = getWidth();
        int h = getHeight();
        int plotH = h - TOP - BOTTOM;
        int baseline = TOP + plotH;

        double max = 0;
        for (Stats.MonthFlow f : flows) {
            max = Math.max(max, f.in.doubleValue());
            max = Math.max(max, f.out.doubleValue());
        }
        double step = niceStep(max);
        int ticks = (int) Math.max(1, Math.ceil(max / step));
        double top = step * ticks;

        // Hover band
        double gw = groupWidth();
        if (hover >= 0) {
            g.setColor(Theme.alpha(Theme.TEXT, Theme.dark ? 10 : 12));
            g.fill(new RoundRectangle2D.Double(LEFT + hover * gw + 4, TOP - 4, gw - 8, plotH + 8, 10, 10));
        }

        // Gridlines and axis labels
        for (int i = 0; i <= ticks; i++) {
            double v = step * i;
            double y = baseline - plotH * (v / top);
            g.setColor(i == 0 ? Theme.BORDER_STRONG : Theme.GRID);
            g.draw(new Line2D.Double(LEFT, y, w - 4, y));
            String label = Money.compact(BigDecimal.valueOf(v));
            g.setColor(Theme.MUTED);
            g.drawString(label, LEFT - 10 - fm.stringWidth(label), (int) y + fm.getAscent() / 2 - 1);
        }

        // Bars
        double bw = Math.max(6, Math.min(20, gw * 0.26));
        for (int i = 0; i < flows.size(); i++) {
            Stats.MonthFlow f = flows.get(i);
            double cx = LEFT + i * gw + gw / 2;

            drawBar(g, cx - bw - 1, bw, f.in.doubleValue() / top * plotH, baseline, Theme.MONEY_IN);
            drawBar(g, cx + 1, bw, f.out.doubleValue() / top * plotH, baseline, Theme.MONEY_OUT);

            String label = f.month.format(MONTH);
            g.setColor(i == hover ? Theme.TEXT : Theme.MUTED);
            g.drawString(label, (int) (cx - fm.stringWidth(label) / 2.0), baseline + 18);
        }
        g.dispose();
    }

    /** Bar with a 4px rounded top and a square bottom on the baseline. */
    private void drawBar(Graphics2D g, double x, double width, double height, double baseline,
                         java.awt.Color color) {
        if (height <= 0.5) return;
        double r = Math.min(4, Math.min(width / 2, height));
        double top = baseline - height;
        Path2D bar = new Path2D.Double();
        bar.moveTo(x, baseline);
        bar.lineTo(x, top + r);
        bar.quadTo(x, top, x + r, top);
        bar.lineTo(x + width - r, top);
        bar.quadTo(x + width, top, x + width, top + r);
        bar.lineTo(x + width, baseline);
        bar.closePath();
        g.setColor(color);
        g.fill(bar);
    }
}
