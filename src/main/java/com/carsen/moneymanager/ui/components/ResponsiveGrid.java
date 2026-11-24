package com.carsen.moneymanager.ui.components;

import java.awt.Component;
import java.awt.Container;
import java.awt.Dimension;
import java.awt.Insets;
import java.awt.LayoutManager;

/**
 * Lays children out in equal-width columns. The number of columns grows
 * as the window gets wider (each column is at least minColumnWidth).
 */
public class ResponsiveGrid implements LayoutManager {

    private final int minColumnWidth;
    private final int maxColumns;
    private final int gap;

    public ResponsiveGrid(int minColumnWidth, int maxColumns, int gap) {
        this.minColumnWidth = minColumnWidth;
        this.maxColumns = maxColumns;
        this.gap = gap;
    }

    private int columns(int width) {
        int cols = Math.max(1, (width + gap) / (minColumnWidth + gap));
        return Math.min(cols, maxColumns);
    }

    private int availableWidth(Container parent) {
        Insets in = parent.getInsets();
        int width = parent.getWidth();
        if (width <= 0 && parent.getParent() != null) width = parent.getParent().getWidth();
        if (width <= 0) width = minColumnWidth * 2 + gap;
        return width - in.left - in.right;
    }

    @Override
    public Dimension preferredLayoutSize(Container parent) {
        synchronized (parent.getTreeLock()) {
            Insets in = parent.getInsets();
            int width = availableWidth(parent);
            int cols = columns(width);
            int colWidth = (width - gap * (cols - 1)) / cols;
            int height = 0;
            int n = parent.getComponentCount();
            for (int row = 0; row * cols < n; row++) {
                int rowHeight = 0;
                for (int c = 0; c < cols && row * cols + c < n; c++) {
                    Component comp = parent.getComponent(row * cols + c);
                    comp.setSize(colWidth, Short.MAX_VALUE);
                    rowHeight = Math.max(rowHeight, comp.getPreferredSize().height);
                }
                height += rowHeight + (row > 0 ? gap : 0);
            }
            return new Dimension(width + in.left + in.right, height + in.top + in.bottom);
        }
    }

    @Override
    public Dimension minimumLayoutSize(Container parent) {
        return new Dimension(minColumnWidth, 0);
    }

    @Override
    public void layoutContainer(Container parent) {
        synchronized (parent.getTreeLock()) {
            Insets in = parent.getInsets();
            int width = parent.getWidth() - in.left - in.right;
            int cols = columns(width);
            int colWidth = (width - gap * (cols - 1)) / cols;
            int n = parent.getComponentCount();
            int y = in.top;
            for (int row = 0; row * cols < n; row++) {
                int rowHeight = 0;
                for (int c = 0; c < cols && row * cols + c < n; c++) {
                    Component comp = parent.getComponent(row * cols + c);
                    comp.setSize(colWidth, Short.MAX_VALUE);
                    rowHeight = Math.max(rowHeight, comp.getPreferredSize().height);
                }
                for (int c = 0; c < cols && row * cols + c < n; c++) {
                    Component comp = parent.getComponent(row * cols + c);
                    comp.setBounds(in.left + c * (colWidth + gap), y, colWidth, rowHeight);
                }
                y += rowHeight + gap;
            }
        }
    }

    @Override
    public void addLayoutComponent(String name, Component comp) {
    }

    @Override
    public void removeLayoutComponent(Component comp) {
    }
}
