package com.carsen.moneymanager.core;

import java.math.BigDecimal;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * A calculated "who ends up with how much" before anything is changed.
 * The same plan is shown as a preview and then applied, so what the user
 * sees is exactly what happens.
 */
public class AllocationPlan {

    public enum Mode {
        /** Nothing moves. */
        NONE,
        /** New goal is funded only from Unallocated. */
        FROM_UNALLOCATED,
        /** Every goal with a percentage is re-split from the shared pool. */
        REDISTRIBUTE
    }

    public Mode mode = Mode.NONE;

    /** Saved amount per goal id, before and after. */
    public final Map<String, BigDecimal> before = new LinkedHashMap<>();
    public final Map<String, BigDecimal> after = new LinkedHashMap<>();

    public BigDecimal unallocatedBefore = BigDecimal.ZERO;
    public BigDecimal unallocatedAfter = BigDecimal.ZERO;

    /** Amount a new goal starts with (only for new-goal plans). */
    public BigDecimal newGoalAmount = BigDecimal.ZERO;

    /** The share a new goal would get if Unallocated had enough. */
    public BigDecimal newGoalWanted = BigDecimal.ZERO;

    /** True when the plan changes money that is already in other goals. */
    public boolean changesExistingGoals() {
        for (String id : after.keySet()) {
            if (before.get(id).compareTo(after.get(id)) != 0) return true;
        }
        return false;
    }

    public boolean changesAnything() {
        return changesExistingGoals()
                || unallocatedBefore.compareTo(unallocatedAfter) != 0
                || newGoalAmount.signum() != 0;
    }
}
