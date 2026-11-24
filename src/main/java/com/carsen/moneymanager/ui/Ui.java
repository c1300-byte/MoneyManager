package com.carsen.moneymanager.ui;

import com.carsen.moneymanager.ui.components.AppButton;
import com.carsen.moneymanager.ui.components.ModernScrollBarUI;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.Icon;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextField;
import javax.swing.ListCellRenderer;
import javax.swing.SwingConstants;
import javax.swing.border.EmptyBorder;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.plaf.basic.BasicComboBoxUI;
import javax.swing.plaf.basic.BasicComboPopup;
import javax.swing.plaf.basic.ComboPopup;
import java.awt.BasicStroke;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Insets;
import java.awt.Rectangle;
import java.awt.RenderingHints;
import java.awt.Toolkit;
import java.awt.event.FocusAdapter;
import java.awt.event.FocusEvent;
import java.awt.geom.Ellipse2D;
import java.awt.geom.RoundRectangle2D;
import java.util.Map;
import java.util.function.Consumer;

/** Small factory methods so every screen builds controls the same way. */
public final class Ui {

    private Ui() {
    }

    private static final Map<?, ?> DESKTOP_HINTS =
            (Map<?, ?>) Toolkit.getDefaultToolkit().getDesktopProperty("awt.font.desktophints");

    /** Graphics with anti-aliasing turned on. Remember to dispose() it. */
    public static Graphics2D hq(Graphics graphics) {
        Graphics2D g = (Graphics2D) graphics.create();
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g.setRenderingHint(RenderingHints.KEY_STROKE_CONTROL, RenderingHints.VALUE_STROKE_PURE);
        if (DESKTOP_HINTS != null) {
            g.addRenderingHints(DESKTOP_HINTS);
        } else {
            g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
        }
        return g;
    }

    // --------------------------------------------------
    // TEXT
    // --------------------------------------------------

    public static JLabel label(String text, Font font, Color color) {
        JLabel label = new JLabel(text);
        label.setFont(font);
        label.setForeground(color);
        return label;
    }

    public static JLabel title(String text, float size) {
        return label(text, Theme.semibold(size), Theme.TEXT);
    }

    public static JLabel muted(String text, float size) {
        return label(text, Theme.font(size), Theme.MUTED);
    }

    public static JLabel secondary(String text, float size) {
        return label(text, Theme.font(size), Theme.TEXT_2);
    }

    /** Multi-line text that wraps at the given width. */
    public static JLabel paragraph(String text, int width, Color color) {
        JLabel label = new JLabel("<html><div style='width:" + width + "px'>" + escape(text) + "</div></html>");
        label.setFont(Theme.font(12.5f));
        label.setForeground(color);
        return label;
    }

    public static String escape(String text) {
        if (text == null) return "";
        return text.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;")
                .replace("\n", "<br>");
    }

    // --------------------------------------------------
    // LAYOUT
    // --------------------------------------------------

    public static JPanel transparent(java.awt.LayoutManager layout) {
        JPanel p = new JPanel(layout);
        p.setOpaque(false);
        return p;
    }

    public static JPanel column() {
        JPanel p = new JPanel();
        p.setLayout(new BoxLayout(p, BoxLayout.Y_AXIS));
        p.setOpaque(false);
        return p;
    }

    public static JPanel row() {
        JPanel p = new JPanel();
        p.setLayout(new BoxLayout(p, BoxLayout.X_AXIS));
        p.setOpaque(false);
        return p;
    }

    /** Left-align a component inside a vertical BoxLayout. */
    public static <T extends JComponent> T left(T component) {
        component.setAlignmentX(Component.LEFT_ALIGNMENT);
        return component;
    }

    public static Component gap(int height) {
        return Box.createVerticalStrut(height);
    }

    public static Component hgap(int width) {
        return Box.createHorizontalStrut(width);
    }

    public static JScrollPane scroll(Component view) {
        JScrollPane scroll = new JScrollPane(view);
        scroll.setBorder(BorderFactory.createEmptyBorder());
        scroll.setViewportBorder(null);
        scroll.getViewport().setBackground(Theme.BG);
        scroll.setBackground(Theme.BG);
        scroll.getVerticalScrollBar().setUI(new ModernScrollBarUI());
        scroll.getHorizontalScrollBar().setUI(new ModernScrollBarUI());
        scroll.getVerticalScrollBar().setUnitIncrement(24);
        scroll.getVerticalScrollBar().setOpaque(false);
        scroll.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
        return scroll;
    }

    // --------------------------------------------------
    // ICONS
    // --------------------------------------------------

    public static Icon dot(Color color, int size) {
        return new Icon() {
            @Override
            public void paintIcon(Component c, Graphics graphics, int x, int y) {
                Graphics2D g = hq(graphics);
                g.setColor(color);
                g.fill(new Ellipse2D.Double(x, y, size, size));
                g.dispose();
            }

            @Override
            public int getIconWidth() {
                return size;
            }

            @Override
            public int getIconHeight() {
                return size;
            }
        };
    }

    /** Round tinted badge with a line icon inside (used in activity lists). */
    public static Icon badge(String icon, Color color, int size) {
        return new Icon() {
            @Override
            public void paintIcon(Component c, Graphics graphics, int x, int y) {
                Graphics2D g = hq(graphics);
                g.setColor(Theme.alpha(color, Theme.dark ? 40 : 30));
                g.fill(new Ellipse2D.Double(x, y, size, size));
                int inner = (int) (size * 0.5);
                Icons.get(icon, inner, color).paintIcon(c, g, x + (size - inner) / 2, y + (size - inner) / 2);
                g.dispose();
            }

            @Override
            public int getIconWidth() {
                return size;
            }

            @Override
            public int getIconHeight() {
                return size;
            }
        };
    }

    /** Colored dot + name, used in chart legends. */
    public static JLabel legend(Color color, String text) {
        JLabel label = new JLabel(text, dot(color, 9), SwingConstants.LEFT);
        label.setIconTextGap(7);
        label.setFont(Theme.font(12.5f));
        label.setForeground(Theme.TEXT_2);
        return label;
    }

    // --------------------------------------------------
    // TEXT FIELDS
    // --------------------------------------------------

    /** Text field with rounded border, focus ring and placeholder. */
    public static class Field extends JTextField {
        private String placeholder;

        public Field(String text, String placeholder) {
            super(text);
            this.placeholder = placeholder;
            setOpaque(false);
            setFont(Theme.font(13.5f));
            setForeground(Theme.TEXT);
            setCaretColor(Theme.TEXT);
            setSelectionColor(Theme.SELECTION);
            setSelectedTextColor(Theme.TEXT);
            setBorder(new EmptyBorder(9, 12, 9, 12));
            addFocusListener(new FocusAdapter() {
                @Override
                public void focusGained(FocusEvent e) {
                    repaint();
                }

                @Override
                public void focusLost(FocusEvent e) {
                    repaint();
                }
            });
        }

        public void setPlaceholder(String placeholder) {
            this.placeholder = placeholder;
            repaint();
        }

        @Override
        protected void paintComponent(Graphics graphics) {
            Graphics2D g = hq(graphics);
            g.setColor(isEnabled() ? Theme.FIELD : Theme.SURFACE_2);
            g.fill(new RoundRectangle2D.Double(0, 0, getWidth() - 1, getHeight() - 1, 10, 10));
            g.dispose();

            super.paintComponent(graphics);

            if (getText().isEmpty() && placeholder != null) {
                Graphics2D p = hq(graphics);
                p.setFont(getFont());
                p.setColor(Theme.MUTED);
                Insets in = getInsets();
                int y = in.top + p.getFontMetrics().getAscent()
                        + (getHeight() - in.top - in.bottom - p.getFontMetrics().getHeight()) / 2;
                p.drawString(placeholder, in.left, y);
                p.dispose();
            }
        }

        @Override
        protected void paintBorder(Graphics graphics) {
            Graphics2D g = hq(graphics);
            boolean focus = isFocusOwner();
            g.setColor(focus ? Theme.ACCENT : Theme.BORDER_STRONG);
            g.setStroke(new BasicStroke(focus ? 1.6f : 1f));
            g.draw(new RoundRectangle2D.Double(0.8, 0.8, getWidth() - 2.6, getHeight() - 2.6, 10, 10));
            g.dispose();
        }
    }

    public static Field field(String text, String placeholder) {
        return new Field(text, placeholder);
    }

    public static void onChange(JTextField field, Runnable action) {
        field.getDocument().addDocumentListener(new DocumentListener() {
            @Override
            public void insertUpdate(DocumentEvent e) {
                action.run();
            }

            @Override
            public void removeUpdate(DocumentEvent e) {
                action.run();
            }

            @Override
            public void changedUpdate(DocumentEvent e) {
                action.run();
            }
        });
    }

    /** Search box with a magnifier icon. */
    public static Field searchField(String placeholder) {
        Field field = new Field("", placeholder) {
            @Override
            protected void paintComponent(Graphics graphics) {
                super.paintComponent(graphics);
                Icons.get("search", 15, Theme.MUTED).paintIcon(this, graphics, 11, (getHeight() - 15) / 2);
            }
        };
        field.setBorder(new EmptyBorder(8, 34, 8, 12));
        return field;
    }

    // --------------------------------------------------
    // DROPDOWNS
    // --------------------------------------------------

    /** Value + display text for dropdowns. */
    public static class Option<T> {
        public final T value;
        public final String label;

        public Option(T value, String label) {
            this.value = value;
            this.label = label;
        }

        @Override
        public String toString() {
            return label;
        }
    }

    public static class Combo<T> extends JComboBox<T> {
        public Combo(T[] items) {
            super(items);
            setOpaque(false);
            setFont(Theme.font(13.5f));
            setForeground(Theme.TEXT);
            setBackground(Theme.FIELD);
            setUI(new FlatComboUI());
            setRenderer(new PaddedRenderer());
            setBorder(new EmptyBorder(1, 2, 1, 4));
            setMaximumRowCount(12);
            setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
            setFocusable(true);
            addFocusListener(new FocusAdapter() {
                @Override
                public void focusGained(FocusEvent e) {
                    repaint();
                }

                @Override
                public void focusLost(FocusEvent e) {
                    repaint();
                }
            });
        }

        @SuppressWarnings("unchecked")
        public T selected() {
            return (T) getSelectedItem();
        }

        @Override
        public Dimension getPreferredSize() {
            Dimension d = super.getPreferredSize();
            return new Dimension(d.width + 6, Math.max(d.height, 38));
        }

        @Override
        protected void paintComponent(Graphics graphics) {
            Graphics2D g = hq(graphics);
            g.setColor(isEnabled() ? Theme.FIELD : Theme.SURFACE_2);
            g.fill(new RoundRectangle2D.Double(0, 0, getWidth() - 1, getHeight() - 1, 10, 10));
            boolean focus = isFocusOwner() || isPopupVisible();
            g.setColor(focus ? Theme.ACCENT : Theme.BORDER_STRONG);
            g.setStroke(new BasicStroke(focus ? 1.6f : 1f));
            g.draw(new RoundRectangle2D.Double(0.8, 0.8, getWidth() - 2.6, getHeight() - 2.6, 10, 10));
            g.dispose();
            super.paintComponent(graphics);
        }
    }

    public static <T> Combo<T> combo(T[] items) {
        return new Combo<>(items);
    }

    private static class PaddedRenderer extends JLabel implements ListCellRenderer<Object> {
        PaddedRenderer() {
            setBorder(new EmptyBorder(7, 10, 7, 10));
        }

        @Override
        public Component getListCellRendererComponent(JList<?> list, Object value, int index,
                                                      boolean selected, boolean focus) {
            setText(value == null ? "" : value.toString());
            setFont(Theme.font(13.5f));
            setOpaque(index >= 0);
            setBackground(selected ? Theme.SELECTION : Theme.SURFACE);
            setForeground(Theme.TEXT);
            setBorder(index >= 0 ? new EmptyBorder(7, 12, 7, 12) : new EmptyBorder(0, 8, 0, 0));
            return this;
        }
    }

    private static class FlatComboUI extends BasicComboBoxUI {
        @Override
        protected JButton createArrowButton() {
            JButton b = new JButton(Icons.get("chevron-down", 15, Theme.MUTED));
            b.setContentAreaFilled(false);
            b.setBorderPainted(false);
            b.setFocusPainted(false);
            b.setOpaque(false);
            b.setBorder(new EmptyBorder(0, 0, 0, 8));
            return b;
        }

        @Override
        public void configureArrowButton() {
            super.configureArrowButton();
            if (arrowButton != null) arrowButton.setFocusable(false); // one Tab stop per dropdown
        }

        @Override
        public void paintCurrentValueBackground(Graphics g, Rectangle bounds, boolean hasFocus) {
        }

        @Override
        @SuppressWarnings({"unchecked", "rawtypes"})
        public void paintCurrentValue(Graphics g, Rectangle bounds, boolean hasFocus) {
            ListCellRenderer renderer = comboBox.getRenderer();
            Component c = renderer.getListCellRendererComponent(
                    listBox, comboBox.getSelectedItem(), -1, false, false);
            c.setFont(comboBox.getFont());
            c.setForeground(comboBox.isEnabled() ? Theme.TEXT : Theme.MUTED);
            if (c instanceof JComponent) ((JComponent) c).setOpaque(false);
            currentValuePane.paintComponent(g, c, comboBox,
                    bounds.x, bounds.y, bounds.width, bounds.height, false);
        }

        @Override
        protected ComboPopup createPopup() {
            BasicComboPopup popup = new BasicComboPopup(comboBox) {
                {
                    setBorder(BorderFactory.createLineBorder(Theme.BORDER_STRONG));
                    scroller.getVerticalScrollBar().setUI(new ModernScrollBarUI());
                    scroller.setBorder(BorderFactory.createEmptyBorder());
                    list.setBackground(Theme.SURFACE);
                }
            };
            return popup;
        }
    }

    // --------------------------------------------------
    // EMPTY STATES
    // --------------------------------------------------

    /** Centered icon, title, hint and an optional button. */
    public static JPanel emptyState(String icon, String title, String body, AppButton action) {
        JPanel box = column();
        box.setBorder(new EmptyBorder(36, 24, 36, 24));

        JLabel iconLabel = new JLabel(badge(icon, Theme.ACCENT_TEXT, 46));
        iconLabel.setAlignmentX(Component.CENTER_ALIGNMENT);
        box.add(iconLabel);
        box.add(gap(14));

        JLabel t = title(title, 16f);
        t.setAlignmentX(Component.CENTER_ALIGNMENT);
        box.add(t);
        box.add(gap(6));

        JLabel b = label("<html><div style='text-align:center;width:330px'>" + escape(body) + "</div></html>",
                Theme.font(13f), Theme.TEXT_2);
        b.setAlignmentX(Component.CENTER_ALIGNMENT);
        b.setHorizontalAlignment(SwingConstants.CENTER);
        box.add(b);

        if (action != null) {
            box.add(gap(18));
            action.setAlignmentX(Component.CENTER_ALIGNMENT);
            box.add(action);
        }

        JPanel wrap = transparent(new java.awt.GridBagLayout());
        wrap.add(box);
        return wrap;
    }

    /** Label + control stacked vertically for forms. */
    public static JPanel formRow(String label, JComponent control, String hint) {
        JPanel row = transparent(new BorderLayout(0, 6));
        row.add(label(label, Theme.semibold(12.5f), Theme.TEXT_2), BorderLayout.NORTH);
        row.add(control, BorderLayout.CENTER);
        if (hint != null) {
            JLabel h = muted(hint, 12f);
            row.add(h, BorderLayout.SOUTH);
        }
        return row;
    }

    public static void onClick(JComponent c, Consumer<java.awt.event.MouseEvent> action) {
        c.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        c.addMouseListener(new java.awt.event.MouseAdapter() {
            @Override
            public void mouseClicked(java.awt.event.MouseEvent e) {
                action.accept(e);
            }
        });
    }
}
