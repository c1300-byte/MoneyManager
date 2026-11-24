package com.carsen.moneymanager.core;

import com.carsen.moneymanager.model.AppData;
import com.carsen.moneymanager.model.Goal;
import com.carsen.moneymanager.model.Settings;
import com.carsen.moneymanager.model.Transaction;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * All the rules for moving money around.
 *
 * Every method checks its inputs BEFORE changing anything, so a failed
 * operation never leaves the data half-updated. The one rule that must
 * always hold:
 *
 *     totalBalance == unallocatedBalance + sum of every goal's savedAmount
 */
public final class Ledger {

    public static final String UNALLOCATED = "Unallocated";
    private static final BigDecimal MAX_AMOUNT = new BigDecimal("1000000000");

    private final AppData data;

    public Ledger(AppData data) {
        this.data = data;
    }

    public AppData data() {
        return data;
    }

    public Settings settings() {
        return data.settings;
    }

    // --------------------------------------------------
    // LOOKUPS
    // --------------------------------------------------

    public BigDecimal goalsTotal() {
        BigDecimal total = BigDecimal.ZERO;
        for (Goal goal : data.goals) total = total.add(goal.savedAmount);
        return total;
    }

    /** Sum of every goal's allocation percentage. */
    public BigDecimal allocatedPercent() {
        BigDecimal total = BigDecimal.ZERO;
        for (Goal goal : data.goals) total = total.add(goal.allocationPercent);
        return total;
    }

    public BigDecimal freePercent() {
        return Money.HUNDRED.subtract(allocatedPercent()).max(BigDecimal.ZERO);
    }

    public Goal goal(String id) {
        if (id == null) return null;
        for (Goal goal : data.goals) {
            if (goal.id.equals(id)) return goal;
        }
        return null;
    }

    /** null means the Unallocated bucket. */
    public String bucketName(String goalId) {
        Goal goal = goal(goalId);
        return goal == null ? UNALLOCATED : goal.name;
    }

    /** Money available in a bucket. null means Unallocated. */
    public BigDecimal available(String goalId) {
        if (goalId == null) return data.unallocatedBalance;
        Goal goal = requireGoal(goalId);
        return goal.savedAmount;
    }

    private Goal requireGoal(String id) {
        Goal goal = goal(id);
        if (goal == null) {
            throw new IllegalArgumentException("That goal no longer exists.");
        }
        return goal;
    }

    // --------------------------------------------------
    // DEPOSITS
    // --------------------------------------------------

    /** How a deposit would be split. Nothing is changed. */
    public static class Split {
        public final Map<Goal, BigDecimal> toGoals = new LinkedHashMap<>();
        public BigDecimal toUnallocated = BigDecimal.ZERO;
    }

    public Split previewDeposit(BigDecimal amount) {
        Split split = new Split();
        BigDecimal distributed = BigDecimal.ZERO;

        if (settings().autoAllocateDeposits) {
            for (Goal goal : data.goals) {
                if (!goal.participates()) continue;

                BigDecimal share = Money.share(amount, goal.allocationPercent);

                if (settings().stopAtTarget) {
                    share = Money.min(share, goal.remaining());
                }
                if (share.signum() > 0) {
                    split.toGoals.put(goal, share);
                    distributed = distributed.add(share);
                }
            }
        }

        split.toUnallocated = amount.subtract(distributed);
        return split;
    }

    public Split deposit(BigDecimal amount, String description) {
        checkAmount(amount);

        Split split = previewDeposit(amount);

        for (Map.Entry<Goal, BigDecimal> entry : split.toGoals.entrySet()) {
            Goal goal = entry.getKey();
            goal.savedAmount = goal.savedAmount.add(entry.getValue());
        }

        data.unallocatedBalance = data.unallocatedBalance.add(split.toUnallocated);
        data.totalBalance = data.totalBalance.add(amount);

        String bucket = split.toGoals.isEmpty()
                ? UNALLOCATED
                : split.toGoals.size() + (split.toGoals.size() == 1 ? " goal" : " goals")
                + (split.toUnallocated.signum() > 0 ? " + Unallocated" : "");

        addTransaction(
                Transaction.DEPOSIT,
                orDefault(description, "Money deposited"),
                bucket,
                amount,
                describeSplit(split)
        );

        checkConsistency();
        return split;
    }

    public static String describeSplit(Split split) {
        if (split.toGoals.isEmpty()) return null;
        StringBuilder text = new StringBuilder();
        for (Map.Entry<Goal, BigDecimal> entry : split.toGoals.entrySet()) {
            if (text.length() > 0) text.append(", ");
            text.append(entry.getKey().name)
                    .append(' ')
                    .append(Money.formatVisible(entry.getValue()));
        }
        if (split.toUnallocated.signum() > 0) {
            text.append(", Unallocated ").append(Money.formatVisible(split.toUnallocated));
        }
        return text.toString();
    }

    // --------------------------------------------------
    // EXPENSES
    // --------------------------------------------------

    /** goalId null = pay from Unallocated. */
    public void expense(BigDecimal amount, String description, String goalId) {
        checkAmount(amount);

        BigDecimal available = available(goalId);
        if (available.compareTo(amount) < 0) {
            throw new IllegalArgumentException(
                    bucketName(goalId) + " only has " + Money.formatVisible(available) + "."
            );
        }

        if (goalId == null) {
            data.unallocatedBalance = data.unallocatedBalance.subtract(amount);
        } else {
            Goal goal = requireGoal(goalId);
            goal.savedAmount = goal.savedAmount.subtract(amount);
        }

        data.totalBalance = data.totalBalance.subtract(amount);

        addTransaction(
                Transaction.EXPENSE,
                orDefault(description, "Expense"),
                bucketName(goalId),
                amount,
                null
        );

        checkConsistency();
    }

    // --------------------------------------------------
    // TRANSFERS
    // --------------------------------------------------

    /** Move money between goals and/or Unallocated. null = Unallocated. */
    public void transfer(String fromId, String toId, BigDecimal amount, String note) {
        checkAmount(amount);

        boolean same = (fromId == null && toId == null)
                || (fromId != null && fromId.equals(toId));
        if (same) {
            throw new IllegalArgumentException("Choose two different places to move money between.");
        }

        if (toId != null) requireGoal(toId);

        BigDecimal available = available(fromId);
        if (available.compareTo(amount) < 0) {
            throw new IllegalArgumentException(
                    bucketName(fromId) + " only has " + Money.formatVisible(available) + "."
            );
        }

        if (fromId == null) {
            data.unallocatedBalance = data.unallocatedBalance.subtract(amount);
        } else {
            Goal from = requireGoal(fromId);
            from.savedAmount = from.savedAmount.subtract(amount);
        }

        if (toId == null) {
            data.unallocatedBalance = data.unallocatedBalance.add(amount);
        } else {
            Goal to = requireGoal(toId);
            to.savedAmount = to.savedAmount.add(amount);
        }

        addTransaction(
                Transaction.TRANSFER,
                orDefault(note, "Moved money"),
                bucketName(fromId) + " → " + bucketName(toId),
                amount,
                null
        );

        checkConsistency();
    }

    // --------------------------------------------------
    // GOALS
    // --------------------------------------------------

    /** What the user typed into the goal form. */
    public static class GoalDraft {
        public String name = "";
        public String category = "";
        public String purpose = "";
        public String type = "Need";
        public BigDecimal target = BigDecimal.ZERO;
        public BigDecimal percent = BigDecimal.ZERO;
        public String targetMonth;
    }

    /** existingId = the goal being edited, or null for a new goal. */
    public void validateDraft(GoalDraft draft, String existingId) {
        String name = draft.name == null ? "" : draft.name.trim();

        if (name.isEmpty()) {
            throw new IllegalArgumentException("Give the goal a name.");
        }
        if (name.equalsIgnoreCase(UNALLOCATED)) {
            throw new IllegalArgumentException("\"Unallocated\" is reserved. Pick another name.");
        }
        for (Goal goal : data.goals) {
            if (goal.name.trim().equalsIgnoreCase(name) && !goal.id.equals(existingId)) {
                throw new IllegalArgumentException("You already have a goal called \"" + goal.name + "\".");
            }
        }
        if (draft.target.signum() <= 0) {
            throw new IllegalArgumentException("The target amount must be more than zero.");
        }
        if (draft.target.compareTo(MAX_AMOUNT) > 0) {
            throw new IllegalArgumentException("That target is too large.");
        }
        if (draft.percent.signum() < 0 || draft.percent.compareTo(Money.HUNDRED) > 0) {
            throw new IllegalArgumentException("The deposit share must be between 0% and 100%.");
        }

        BigDecimal others = allocatedPercent();
        Goal existing = goal(existingId);
        if (existing != null) others = others.subtract(existing.allocationPercent);

        if (others.add(draft.percent).compareTo(Money.HUNDRED) > 0) {
            BigDecimal left = Money.HUNDRED.subtract(others).max(BigDecimal.ZERO);
            throw new IllegalArgumentException(
                    "Your goals would add up to more than 100%. You have "
                            + Money.percent(left) + " left to assign."
            );
        }
    }

    /**
     * What happens to the money when a new goal with this percentage is
     * created, based on the current settings. Nothing is changed.
     */
    public AllocationPlan planNewGoal(BigDecimal percent, BigDecimal target) {
        Settings s = settings();

        if (!s.fundNewGoals || percent == null || percent.signum() <= 0) {
            return plan(null, null, false);
        }
        return plan(percent, target, s.redistributeOnNewGoal);
    }

    /** Re-split everything across goals by their current percentages. */
    public AllocationPlan planRebalance() {
        return plan(null, null, true);
    }

    /**
     * The shared calculation.
     *
     * "Pool" = Unallocated + money in every goal that has a percentage.
     * Goals set to 0% are never touched, so money you parked there by
     * hand stays put.
     */
    private AllocationPlan plan(BigDecimal newPercent, BigDecimal newTarget, boolean redistribute) {
        AllocationPlan plan = new AllocationPlan();
        boolean capped = settings().stopAtTarget;

        plan.unallocatedBefore = data.unallocatedBalance;
        plan.unallocatedAfter = data.unallocatedBalance;

        BigDecimal pool = data.unallocatedBalance;

        for (Goal goal : data.goals) {
            plan.before.put(goal.id, goal.savedAmount);
            plan.after.put(goal.id, goal.savedAmount);
            if (goal.participates()) pool = pool.add(goal.savedAmount);
        }

        if (newPercent != null) {
            BigDecimal wanted = Money.share(pool, newPercent);
            if (capped && newTarget != null && newTarget.signum() > 0) {
                wanted = Money.min(wanted, newTarget);
            }
            plan.newGoalWanted = wanted;
        }

        if (redistribute) {
            plan.mode = AllocationPlan.Mode.REDISTRIBUTE;

            BigDecimal assigned = BigDecimal.ZERO;

            for (Goal goal : data.goals) {
                if (!goal.participates()) continue;

                BigDecimal share = Money.share(pool, goal.allocationPercent);
                if (capped) share = Money.min(share, goal.targetAmount);

                plan.after.put(goal.id, share);
                assigned = assigned.add(share);
            }

            if (newPercent != null) {
                plan.newGoalAmount = plan.newGoalWanted;
                assigned = assigned.add(plan.newGoalAmount);
            }

            if (assigned.compareTo(pool) > 0) {
                throw new IllegalStateException(
                        "Your goal percentages add up to more than 100%. Lower one and try again."
                );
            }

            plan.unallocatedAfter = pool.subtract(assigned);

        } else if (newPercent != null) {
            plan.mode = AllocationPlan.Mode.FROM_UNALLOCATED;
            plan.newGoalAmount = Money.min(plan.newGoalWanted, data.unallocatedBalance);
            plan.unallocatedAfter = data.unallocatedBalance.subtract(plan.newGoalAmount);
        }

        return plan;
    }

    public Goal createGoal(GoalDraft draft) {
        validateDraft(draft, null);

        AllocationPlan plan = planNewGoal(draft.percent, draft.target);

        Goal goal = new Goal();
        goal.id = UUID.randomUUID().toString();
        goal.name = draft.name.trim();
        goal.category = safe(draft.category);
        goal.purposeLabel = safe(draft.purpose);
        goal.goalType = draft.type == null ? "Need" : draft.type;
        goal.targetAmount = draft.target;
        goal.allocationPercent = draft.percent;
        goal.savedAmount = BigDecimal.ZERO;
        goal.targetMonth = blankToNull(draft.targetMonth);
        goal.colorIndex = nextColorIndex();
        goal.createdAt = LocalDateTime.now().withNano(0).toString();

        // Describe the change before applying it (uses the "before" values).
        String details = describePlan(plan, goal.name);

        applyPlan(plan, goal);
        data.goals.add(goal);

        addTransaction(
                Transaction.GOAL_CREATED,
                "Created goal: " + goal.name,
                goal.name,
                goal.savedAmount,
                details
        );

        checkConsistency();
        return goal;
    }

    public void updateGoal(String id, GoalDraft draft) {
        Goal goal = requireGoal(id);
        validateDraft(draft, id);

        String oldName = goal.name;

        goal.name = draft.name.trim();
        goal.category = safe(draft.category);
        goal.purposeLabel = safe(draft.purpose);
        goal.goalType = draft.type == null ? goal.goalType : draft.type;
        goal.targetAmount = draft.target;
        goal.allocationPercent = draft.percent;
        goal.targetMonth = blankToNull(draft.targetMonth);

        // Keep history readable if the goal was renamed.
        if (!oldName.equals(goal.name)) {
            for (Transaction t : data.transactions) {
                if (oldName.equals(t.bucket)) t.bucket = goal.name;
            }
        }

        checkConsistency();
    }

    /** moveToId null = return the goal's money to Unallocated. */
    public void deleteGoal(String id, String moveToId) {
        Goal goal = requireGoal(id);

        if (id.equals(moveToId)) {
            throw new IllegalArgumentException("Choose somewhere else to move the money.");
        }
        Goal target = moveToId == null ? null : requireGoal(moveToId);

        BigDecimal money = goal.savedAmount;

        if (target == null) {
            data.unallocatedBalance = data.unallocatedBalance.add(money);
        } else {
            target.savedAmount = target.savedAmount.add(money);
        }

        data.goals.remove(goal);

        addTransaction(
                Transaction.GOAL_DELETED,
                "Deleted goal: " + goal.name,
                goal.name,
                money,
                money.signum() > 0
                        ? "Moved " + Money.formatVisible(money) + " to "
                        + (target == null ? UNALLOCATED : target.name)
                        : null
        );

        checkConsistency();
    }

    public AllocationPlan rebalance() {
        AllocationPlan plan = planRebalance();
        if (!plan.changesAnything()) {
            throw new IllegalArgumentException("Everything is already split by your percentages.");
        }

        String details = describePlan(plan, null);
        applyPlan(plan, null);

        addTransaction(
                Transaction.REBALANCE,
                "Rebalanced goals by percentage",
                "All goals",
                BigDecimal.ZERO,
                details
        );

        checkConsistency();
        return plan;
    }

    private void applyPlan(AllocationPlan plan, Goal newGoal) {
        for (Map.Entry<String, BigDecimal> entry : plan.after.entrySet()) {
            Goal goal = goal(entry.getKey());
            if (goal != null) goal.savedAmount = entry.getValue();
        }
        if (newGoal != null) newGoal.savedAmount = plan.newGoalAmount;
        data.unallocatedBalance = plan.unallocatedAfter;
    }

    /** Human-readable list of what a plan moves. */
    public List<String> planLines(AllocationPlan plan, String newGoalName) {
        List<String> lines = new ArrayList<>();
        for (Goal goal : data.goals) {
            BigDecimal b = plan.before.get(goal.id);
            BigDecimal a = plan.after.get(goal.id);
            if (b != null && a != null && b.compareTo(a) != 0) {
                lines.add(goal.name + ": " + Money.formatVisible(b) + " → " + Money.formatVisible(a));
            }
        }
        if (newGoalName != null && plan.newGoalAmount.signum() > 0) {
            lines.add(newGoalName + ": " + Money.formatVisible(BigDecimal.ZERO)
                    + " → " + Money.formatVisible(plan.newGoalAmount));
        }
        if (plan.unallocatedBefore.compareTo(plan.unallocatedAfter) != 0) {
            lines.add(UNALLOCATED + ": " + Money.formatVisible(plan.unallocatedBefore)
                    + " → " + Money.formatVisible(plan.unallocatedAfter));
        }
        return lines;
    }

    private String describePlan(AllocationPlan plan, String newGoalName) {
        if (plan.mode == AllocationPlan.Mode.FROM_UNALLOCATED) {
            if (plan.newGoalAmount.signum() > 0) {
                return "Funded with " + Money.formatVisible(plan.newGoalAmount) + " from Unallocated";
            }
            return "No Unallocated money was available";
        }
        List<String> lines = planLines(plan, newGoalName);
        return lines.isEmpty() ? null : String.join("; ", lines);
    }

    /** Color slot (0-7) the next new goal will get: the least used one. */
    public int nextColorIndex() {
        int[] used = new int[8];
        for (Goal goal : data.goals) {
            if (goal.colorIndex != null) used[Math.floorMod(goal.colorIndex, 8)]++;
        }
        int best = 0;
        for (int i = 1; i < 8; i++) {
            if (used[i] < used[best]) best = i;
        }
        return best;
    }

    // --------------------------------------------------
    // INSIGHTS
    // --------------------------------------------------

    /** Monthly amount needed to hit the target by its deadline, or null. */
    public static BigDecimal monthlyNeeded(Goal goal, YearMonth now) {
        YearMonth deadline = goal.deadline();
        if (deadline == null || goal.isReached()) return null;

        long months = (deadline.getYear() * 12L + deadline.getMonthValue())
                - (now.getYear() * 12L + now.getMonthValue()) + 1;

        if (months <= 0) return goal.remaining();
        return goal.remaining().divide(BigDecimal.valueOf(months), 2, RoundingMode.UP);
    }

    // --------------------------------------------------
    // SAFETY
    // --------------------------------------------------

    /** Throws if the books don't balance. Called after every change. */
    public void checkConsistency() {
        BigDecimal sum = data.unallocatedBalance.add(goalsTotal());
        if (sum.compareTo(data.totalBalance) != 0) {
            throw new IllegalStateException(
                    "Internal error: balances no longer add up ("
                            + sum.toPlainString() + " vs " + data.totalBalance.toPlainString() + ")."
            );
        }
        if (data.unallocatedBalance.signum() < 0) {
            throw new IllegalStateException("Internal error: Unallocated went below zero.");
        }
        for (Goal goal : data.goals) {
            if (goal.savedAmount.signum() < 0) {
                throw new IllegalStateException("Internal error: " + goal.name + " went below zero.");
            }
        }
    }

    // --------------------------------------------------
    // HELPERS
    // --------------------------------------------------

    private void checkAmount(BigDecimal amount) {
        if (amount == null || amount.signum() <= 0) {
            throw new IllegalArgumentException("The amount must be more than zero.");
        }
        if (amount.compareTo(MAX_AMOUNT) > 0) {
            throw new IllegalArgumentException("That amount is too large.");
        }
    }

    private void addTransaction(String type, String description, String bucket,
                                BigDecimal amount, String details) {
        LocalDateTime now = LocalDateTime.now().withNano(0);

        Transaction t = new Transaction();
        t.id = UUID.randomUUID().toString();
        t.timestamp = now.toString();
        t.date = now.format(Transaction.LEGACY_FORMAT);
        t.type = type;
        t.description = description;
        t.bucket = bucket;
        t.amount = amount;
        t.details = details;

        data.transactions.add(t);
    }

    private static String orDefault(String text, String fallback) {
        return text == null || text.isBlank() ? fallback : text.trim();
    }

    private static String safe(String text) {
        return text == null ? "" : text.trim();
    }

    private static String blankToNull(String text) {
        return text == null || text.isBlank() ? null : text.trim();
    }
}
