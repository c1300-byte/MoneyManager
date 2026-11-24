package com.carsen.moneymanager.core;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.text.NumberFormat;
import java.util.Currency;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;

/** Parsing and formatting money and percentages in one place. */
public final class Money {

    public static final BigDecimal HUNDRED = BigDecimal.valueOf(100);

    /** Currencies offered in Settings, with the locale used to format them. */
    public static final Map<String, Locale> CURRENCIES = new LinkedHashMap<>();

    static {
        CURRENCIES.put("USD", Locale.US);
        CURRENCIES.put("CAD", Locale.CANADA);
        CURRENCIES.put("EUR", Locale.GERMANY);
        CURRENCIES.put("GBP", Locale.UK);
        CURRENCIES.put("AUD", new Locale("en", "AU"));
        CURRENCIES.put("MXN", new Locale("es", "MX"));
        CURRENCIES.put("INR", new Locale("en", "IN"));
        CURRENCIES.put("CHF", new Locale("de", "CH"));
    }

    private static NumberFormat format = NumberFormat.getCurrencyInstance(Locale.US);
    private static String symbol = "$";
    private static boolean hidden = false;

    private Money() {
    }

    public static void configure(String currencyCode, boolean hideAmounts) {
        Locale locale = CURRENCIES.getOrDefault(currencyCode, Locale.US);
        NumberFormat f = NumberFormat.getCurrencyInstance(locale);
        try {
            f.setCurrency(Currency.getInstance(currencyCode));
        } catch (Exception ignored) {
        }
        f.setMinimumFractionDigits(2);
        f.setMaximumFractionDigits(2);
        format = f;
        symbol = f.getCurrency().getSymbol(locale);
        hidden = hideAmounts;
    }

    public static String label(String code) {
        try {
            Currency c = Currency.getInstance(code);
            Locale l = CURRENCIES.getOrDefault(code, Locale.US);
            return code + "  (" + c.getSymbol(l) + ")  " + c.getDisplayName(Locale.US);
        } catch (Exception e) {
            return code;
        }
    }

    public static boolean isHidden() {
        return hidden;
    }

    public static String symbol() {
        return symbol;
    }

    /** "$1,234.56", or a mask when amounts are hidden. */
    public static String format(BigDecimal value) {
        if (hidden) return symbol + "••••";
        return format.format(value == null ? BigDecimal.ZERO : value);
    }

    /** Always shows the number, even in privacy mode (used inside forms). */
    public static String formatVisible(BigDecimal value) {
        return format.format(value == null ? BigDecimal.ZERO : value);
    }

    /** "+$50.00" / "-$12.00" / "$0.00". */
    public static String signed(BigDecimal value, int direction) {
        String body = format(value == null ? BigDecimal.ZERO : value.abs());
        if (direction > 0) return "+" + body;
        if (direction < 0) return "−" + body;
        return body;
    }

    /** Short axis labels: $950, $1.2k, $34k, $1.1M. */
    public static String compact(BigDecimal value) {
        if (hidden) return "••";
        double v = value.doubleValue();
        double a = Math.abs(v);
        String s;
        if (a >= 1_000_000) s = trim(v / 1_000_000) + "M";
        else if (a >= 1_000) s = trim(v / 1_000) + "k";
        else s = String.valueOf(Math.round(v));
        return symbol + s;
    }

    private static String trim(double v) {
        String s = String.format(Locale.US, "%.1f", v);
        return s.endsWith(".0") ? s.substring(0, s.length() - 2) : s;
    }

    public static String percent(BigDecimal value) {
        if (value == null) return "0%";
        BigDecimal v = value.setScale(1, RoundingMode.HALF_UP).stripTrailingZeros();
        return v.toPlainString() + "%";
    }

    /**
     * Reads what the user typed. Accepts "1,250", "$1250.5", " 40 ".
     * Throws IllegalArgumentException with a readable message.
     */
    public static BigDecimal parse(String text, String what) {
        String cleaned = text == null ? "" : text.trim()
                .replace(symbol, "")
                .replace("$", "")
                .replace("€", "")
                .replace("£", "")
                .replace(",", "")
                .replace(" ", "")
                .replace(" ", "");
        if (cleaned.isEmpty()) {
            throw new IllegalArgumentException("Enter " + what + ".");
        }
        try {
            return new BigDecimal(cleaned).setScale(2, RoundingMode.HALF_UP);
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException(
                    "\"" + text.trim() + "\" isn't a number. Enter " + what + " like 125.50."
            );
        }
    }

    public static BigDecimal parsePercent(String text) {
        String cleaned = text == null ? "" : text.replace("%", "").trim();
        if (cleaned.isEmpty()) return BigDecimal.ZERO.setScale(2);
        try {
            return new BigDecimal(cleaned).setScale(2, RoundingMode.HALF_UP);
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException(
                    "\"" + text.trim() + "\" isn't a percentage. Enter a number from 0 to 100."
            );
        }
    }

    /** amount x percent / 100, rounded down to the cent. */
    public static BigDecimal share(BigDecimal amount, BigDecimal percent) {
        return amount.multiply(percent)
                .divide(HUNDRED, 2, RoundingMode.DOWN);
    }

    public static BigDecimal min(BigDecimal a, BigDecimal b) {
        return a.compareTo(b) <= 0 ? a : b;
    }

    public static BigDecimal max(BigDecimal a, BigDecimal b) {
        return a.compareTo(b) >= 0 ? a : b;
    }
}
