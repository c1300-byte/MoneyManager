package com.carsen.moneymanager.model;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Locale;

public class Transaction {

    public static final String DEPOSIT = "Deposit";
    public static final String EXPENSE = "Expense";
    public static final String TRANSFER = "Transfer";
    public static final String GOAL_CREATED = "Goal Created";
    public static final String GOAL_DELETED = "Goal Deleted";
    public static final String REBALANCE = "Rebalance";
    public static final String ADJUSTMENT = "Adjustment";

    /** Format the original app used for the "date" field. */
    public static final DateTimeFormatter LEGACY_FORMAT =
            DateTimeFormatter.ofPattern("MMM dd, yyyy HH:mm", Locale.US);

    public String id;
    public String date;
    public String type;
    public String description;
    public String bucket;

    public BigDecimal amount;

    /** Added in v2: ISO date-time, used for sorting and charts. */
    public String timestamp;

    /** Added in v2: extra detail, e.g. how a deposit was split. */
    public String details;

    private transient LocalDateTime cachedTime;

    public LocalDateTime time() {
        if (cachedTime != null) return cachedTime;
        if (timestamp != null && !timestamp.isBlank()) {
            try {
                cachedTime = LocalDateTime.parse(timestamp);
                return cachedTime;
            } catch (Exception ignored) {
            }
        }
        cachedTime = parseLegacy(date);
        return cachedTime;
    }

    public static LocalDateTime parseLegacy(String text) {
        if (text == null || text.isBlank()) return null;
        for (Locale locale : new Locale[]{Locale.US, Locale.getDefault()}) {
            try {
                return LocalDateTime.parse(
                        text.trim(),
                        DateTimeFormatter.ofPattern("MMM dd, yyyy HH:mm", locale)
                );
            } catch (Exception ignored) {
            }
        }
        return null;
    }

    /** +1 for money coming in, -1 for money going out, 0 for moves. */
    public int direction() {
        if (DEPOSIT.equals(type)) return 1;
        if (EXPENSE.equals(type)) return -1;
        if (ADJUSTMENT.equals(type) && amount != null) return amount.signum();
        return 0;
    }
}
