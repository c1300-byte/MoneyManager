package com.carsen.moneymanager.ui;

import com.carsen.moneymanager.model.Goal;

import javax.swing.BorderFactory;
import javax.swing.UIManager;
import javax.swing.plaf.ColorUIResource;
import javax.swing.plaf.FontUIResource;
import javax.swing.plaf.metal.DefaultMetalTheme;
import javax.swing.plaf.metal.MetalLookAndFeel;
import java.awt.Color;
import java.awt.Font;
import java.awt.GraphicsEnvironment;
import java.util.Arrays;
import java.util.Enumeration;
import java.util.HashSet;
import java.util.Set;

/**
 * Colors and fonts for the whole app. Call {@link #apply(String)} and then
 * rebuild the window to switch between light and dark.
 */
public final class Theme {

    private Theme() {
    }

    public static boolean dark = true;

    public static Color BG;
    public static Color SIDEBAR;
    public static Color SURFACE;
    public static Color SURFACE_2;
    public static Color FIELD;
    public static Color BORDER;
    public static Color BORDER_STRONG;
    public static Color TEXT;
    public static Color TEXT_2;
    public static Color MUTED;
    public static Color ACCENT;
    public static Color ACCENT_HOVER;
    public static Color ACCENT_SOFT;
    public static Color ACCENT_TEXT;
    public static Color ON_ACCENT;
    public static Color POSITIVE;
    public static Color NEGATIVE;
    public static Color NEGATIVE_SOFT;
    public static Color WARNING;
    public static Color GRID;
    public static Color SELECTION;
    public static Color UNALLOCATED;
    public static Color MONEY_IN;
    public static Color MONEY_OUT;
    public static Color SHADOW;

    /**
     * Goal colors, in a fixed order checked for color-blind separation
     * on both themes. A goal keeps its color for life (Goal.colorIndex).
     */
    public static Color[] SERIES;

    private static final Color[] SERIES_DARK = hex(
            "#3987e5", "#d95926", "#199e70", "#c98500",
            "#d55181", "#008300", "#9085e9", "#e66767");

    private static final Color[] SERIES_LIGHT = hex(
            "#2a78d6", "#eb6834", "#1baf7a", "#eda100",
            "#e87ba4", "#008300", "#6250d6", "#e34948");

    static {
        apply("dark");
    }

    public static void apply(String name) {
        dark = !"light".equalsIgnoreCase(name);

        if (dark) {
            BG = c("#0f141c");
            SIDEBAR = c("#121925");
            SURFACE = c("#18202c");
            SURFACE_2 = c("#202a38");
            FIELD = c("#111823");
            BORDER = c("#253041");
            BORDER_STRONG = c("#354258");
            TEXT = c("#e8edf5");
            TEXT_2 = c("#a9b3c4");
            MUTED = c("#76839a");
            ACCENT = c("#3d7ff0");
            ACCENT_HOVER = c("#5393ff");
            ACCENT_SOFT = c("#1b2c47");
            ACCENT_TEXT = c("#7fb0ff");
            ON_ACCENT = Color.WHITE;
            POSITIVE = c("#3fcd91");
            NEGATIVE = c("#f56970");
            NEGATIVE_SOFT = c("#3a1f27");
            WARNING = c("#f6b153");
            GRID = c("#222c3b");
            SELECTION = c("#24395a");
            UNALLOCATED = c("#4a566b");
            MONEY_IN = c("#3987e5");
            MONEY_OUT = c("#e66767");
            SHADOW = new Color(0, 0, 0, 90);
            SERIES = SERIES_DARK;
        } else {
            BG = c("#f3f5f8");
            SIDEBAR = c("#ffffff");
            SURFACE = c("#ffffff");
            SURFACE_2 = c("#f1f4f8");
            FIELD = c("#ffffff");
            BORDER = c("#e2e7ee");
            BORDER_STRONG = c("#cbd3df");
            TEXT = c("#131a26");
            TEXT_2 = c("#4b5565");
            MUTED = c("#7f8a9c");
            ACCENT = c("#2a6fdb");
            ACCENT_HOVER = c("#1f5fc4");
            ACCENT_SOFT = c("#e6eefb");
            ACCENT_TEXT = c("#1f5fc4");
            ON_ACCENT = Color.WHITE;
            POSITIVE = c("#0f8a4f");
            NEGATIVE = c("#cf3a3a");
            NEGATIVE_SOFT = c("#fdecec");
            WARNING = c("#a8690b");
            GRID = c("#edf0f4");
            SELECTION = c("#dce8fb");
            UNALLOCATED = c("#c3cad5");
            MONEY_IN = c("#2a78d6");
            MONEY_OUT = c("#e34948");
            SHADOW = new Color(15, 23, 42, 30);
            SERIES = SERIES_LIGHT;
        }
    }

    public static Color goalColor(Goal goal) {
        int index = goal.colorIndex == null ? 0 : Math.floorMod(goal.colorIndex, SERIES.length);
        return SERIES[index];
    }

    /** Same color with an alpha value from 0 to 255. */
    public static Color alpha(Color color, int alpha) {
        return new Color(color.getRed(), color.getGreen(), color.getBlue(), alpha);
    }

    /** Blend two colors; amount 0 = a, 1 = b. */
    public static Color mix(Color a, Color b, double amount) {
        double t = Math.max(0, Math.min(1, amount));
        return new Color(
                (int) Math.round(a.getRed() + (b.getRed() - a.getRed()) * t),
                (int) Math.round(a.getGreen() + (b.getGreen() - a.getGreen()) * t),
                (int) Math.round(a.getBlue() + (b.getBlue() - a.getBlue()) * t));
    }

    // --------------------------------------------------
    // FONTS
    // --------------------------------------------------

    private static String family;
    private static String semiboldFamily;

    private static void loadFonts() {
        if (family != null) return;
        Set<String> available = new HashSet<>(Arrays.asList(
                GraphicsEnvironment.getLocalGraphicsEnvironment().getAvailableFontFamilyNames()));

        family = pick(available, "Segoe UI", "Inter", "Noto Sans", "SansSerif");
        semiboldFamily = pick(available, "Segoe UI Semibold", "Inter SemiBold", "Inter Semi Bold");
    }

    private static String pick(Set<String> available, String... names) {
        for (String name : names) {
            if (available.contains(name)) return name;
        }
        return null;
    }

    public static Font font(float size) {
        loadFonts();
        return new Font(family, Font.PLAIN, 12).deriveFont(size);
    }

    public static Font semibold(float size) {
        loadFonts();
        if (semiboldFamily != null) return new Font(semiboldFamily, Font.PLAIN, 12).deriveFont(size);
        return new Font(family, Font.BOLD, 12).deriveFont(size);
    }

    public static Font bold(float size) {
        loadFonts();
        return new Font(family, Font.BOLD, 12).deriveFont(size);
    }

    // --------------------------------------------------
    // LOOK AND FEEL
    // --------------------------------------------------

    /**
     * Uses Swing's cross-platform look so every control can follow the
     * app's colors (the Windows look ignores most custom colors).
     */
    public static void installLookAndFeel() {
        try {
            MetalLookAndFeel.setCurrentTheme(new AppMetalTheme());
            UIManager.setLookAndFeel(new MetalLookAndFeel());
        } catch (Exception ignored) {
        }

        FontUIResource base = new FontUIResource(font(13f));
        Enumeration<Object> keys = UIManager.getDefaults().keys();
        while (keys.hasMoreElements()) {
            Object key = keys.nextElement();
            if (UIManager.get(key) instanceof FontUIResource) UIManager.put(key, base);
        }

        put("Panel.background", BG);
        put("Label.foreground", TEXT);
        put("OptionPane.background", SURFACE);
        put("OptionPane.messageForeground", TEXT);

        for (String p : new String[]{"TextField", "FormattedTextField", "PasswordField", "TextArea"}) {
            put(p + ".background", FIELD);
            put(p + ".foreground", TEXT);
            put(p + ".caretForeground", TEXT);
            put(p + ".selectionBackground", SELECTION);
            put(p + ".selectionForeground", TEXT);
            put(p + ".inactiveForeground", MUTED);
            put(p + ".inactiveBackground", SURFACE_2);
        }

        put("ComboBox.background", FIELD);
        put("ComboBox.foreground", TEXT);
        put("ComboBox.selectionBackground", SELECTION);
        put("ComboBox.selectionForeground", TEXT);
        put("ComboBox.disabledBackground", SURFACE_2);
        put("ComboBox.disabledForeground", MUTED);

        put("List.background", SURFACE);
        put("List.foreground", TEXT);
        put("List.selectionBackground", SELECTION);
        put("List.selectionForeground", TEXT);

        put("ScrollPane.background", BG);
        put("Viewport.background", BG);
        put("ScrollBar.background", BG);
        put("ScrollBar.track", BG);
        put("ScrollBar.thumb", BORDER_STRONG);

        put("Table.background", SURFACE);
        put("Table.foreground", TEXT);
        put("Table.gridColor", BORDER);
        put("Table.selectionBackground", SELECTION);
        put("Table.selectionForeground", TEXT);
        put("TableHeader.background", SURFACE);
        put("TableHeader.foreground", MUTED);

        put("ToolTip.background", dark ? c("#273244") : c("#1f2937"));
        put("ToolTip.foreground", c("#f3f6fb"));
        UIManager.put("ToolTip.border", BorderFactory.createEmptyBorder(6, 9, 6, 9));
        UIManager.put("ToolTip.font", new FontUIResource(font(12f)));

        put("PopupMenu.background", SURFACE);
        UIManager.put("PopupMenu.border", BorderFactory.createLineBorder(BORDER_STRONG));
        put("MenuItem.background", SURFACE);
        put("MenuItem.foreground", TEXT);
        put("MenuItem.selectionBackground", SELECTION);
        put("MenuItem.selectionForeground", TEXT);

        put("CheckBox.background", SURFACE);
        put("CheckBox.foreground", TEXT);
        put("Button.background", SURFACE_2);
        put("Button.foreground", TEXT);
        put("Separator.foreground", BORDER);
        put("SplitPane.background", BG);
        UIManager.put("Button.focus", new ColorUIResource(alpha(ACCENT, 0)));
    }

    private static void put(String key, Color color) {
        UIManager.put(key, new ColorUIResource(color));
    }

    private static class AppMetalTheme extends DefaultMetalTheme {
        @Override
        public String getName() {
            return "Money Manager";
        }

        @Override
        protected ColorUIResource getPrimary1() {
            return new ColorUIResource(ACCENT);
        }

        @Override
        protected ColorUIResource getPrimary2() {
            return new ColorUIResource(BORDER_STRONG);
        }

        @Override
        protected ColorUIResource getPrimary3() {
            return new ColorUIResource(SELECTION);
        }

        @Override
        protected ColorUIResource getSecondary1() {
            return new ColorUIResource(BORDER_STRONG);
        }

        @Override
        protected ColorUIResource getSecondary2() {
            return new ColorUIResource(BORDER);
        }

        @Override
        protected ColorUIResource getSecondary3() {
            return new ColorUIResource(BG);
        }

        @Override
        protected ColorUIResource getBlack() {
            return new ColorUIResource(TEXT);
        }

        @Override
        protected ColorUIResource getWhite() {
            return new ColorUIResource(SURFACE);
        }

        @Override
        public FontUIResource getControlTextFont() {
            return new FontUIResource(font(13f));
        }

        @Override
        public FontUIResource getSystemTextFont() {
            return new FontUIResource(font(13f));
        }

        @Override
        public FontUIResource getUserTextFont() {
            return new FontUIResource(font(13f));
        }

        @Override
        public FontUIResource getMenuTextFont() {
            return new FontUIResource(font(13f));
        }

        @Override
        public FontUIResource getWindowTitleFont() {
            return new FontUIResource(semibold(13f));
        }

        @Override
        public FontUIResource getSubTextFont() {
            return new FontUIResource(font(11f));
        }
    }

    // --------------------------------------------------

    private static Color c(String hex) {
        return Color.decode(hex);
    }

    private static Color[] hex(String... values) {
        Color[] colors = new Color[values.length];
        for (int i = 0; i < values.length; i++) colors[i] = Color.decode(values[i]);
        return colors;
    }
}
