package com.carsen.moneymanager.model;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.YearMonth;

public class Goal {

    public String id;
    public String name;
    public String category;
    public String purposeLabel;
    public String goalType;

    public BigDecimal targetAmount;
    public BigDecimal savedAmount;
    public BigDecimal allocationPercent;

    /** Added in v2: which chart color this goal keeps (0-7). */
    public Integer colorIndex;

    /** Added in v2: optional deadline, stored as "yyyy-MM". */
    public String targetMonth;

    /** Added in v2: ISO date-time the goal was created. */
    public String createdAt;

    public static final String[] TYPES = {"Need", "Want", "Emergency", "Long-term"};

    /** Progress from 0 to 100+ (can pass 100 if over-funded). */
    public BigDecimal progressPercent() {
        if (targetAmount == null || targetAmount.signum() <= 0) {
            return BigDecimal.ZERO;
        }
        return savedAmount.multiply(BigDecimal.valueOf(100))
                .divide(targetAmount, 1, RoundingMode.HALF_UP);
    }

    public boolean isReached() {
        return targetAmount != null
                && targetAmount.signum() > 0
                && savedAmount.compareTo(targetAmount) >= 0;
    }

    public BigDecimal remaining() {
        BigDecimal left = targetAmount.subtract(savedAmount);
        return left.signum() < 0 ? BigDecimal.ZERO : left;
    }

    public YearMonth deadline() {
        if (targetMonth == null || targetMonth.isBlank()) return null;
        try {
            return YearMonth.parse(targetMonth);
        } catch (Exception e) {
            return null;
        }
    }

    public boolean participates() {
        return allocationPercent != null && allocationPercent.signum() > 0;
    }
}
