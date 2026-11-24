package com.carsen.moneymanager.core;

import com.carsen.moneymanager.model.AppData;
import com.carsen.moneymanager.model.Transaction;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.List;

/** Read-only numbers for charts and summaries. */
public final class Stats {

    private Stats() {
    }

    public static class MonthFlow {
        public final YearMonth month;
        public BigDecimal in = BigDecimal.ZERO;
        public BigDecimal out = BigDecimal.ZERO;

        MonthFlow(YearMonth month) {
            this.month = month;
        }

        public BigDecimal net() {
            return in.subtract(out);
        }
    }

    /** Money in and out for the last {@code count} months, oldest first. */
    public static List<MonthFlow> cashFlow(AppData data, int count) {
        YearMonth now = YearMonth.now();
        List<MonthFlow> months = new ArrayList<>();
        for (int i = count - 1; i >= 0; i--) months.add(new MonthFlow(now.minusMonths(i)));

        YearMonth first = months.get(0).month;

        for (Transaction t : data.transactions) {
            LocalDateTime time = t.time();
            if (time == null || t.amount == null) continue;

            YearMonth ym = YearMonth.from(time);
            if (ym.isBefore(first) || ym.isAfter(now)) continue;

            MonthFlow flow = months.get((int) (monthsBetween(first, ym)));
            int dir = t.direction();
            if (dir > 0) flow.in = flow.in.add(t.amount.abs());
            if (dir < 0) flow.out = flow.out.add(t.amount.abs());
        }
        return months;
    }

    public static MonthFlow thisMonth(AppData data) {
        List<MonthFlow> flows = cashFlow(data, 1);
        return flows.get(0);
    }

    private static long monthsBetween(YearMonth a, YearMonth b) {
        return (b.getYear() * 12L + b.getMonthValue()) - (a.getYear() * 12L + a.getMonthValue());
    }

    public static boolean hasDatedActivity(List<MonthFlow> flows) {
        for (MonthFlow f : flows) {
            if (f.in.signum() != 0 || f.out.signum() != 0) return true;
        }
        return false;
    }
}
