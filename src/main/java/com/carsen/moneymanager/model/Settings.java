package com.carsen.moneymanager.model;

/**
 * User preferences, saved inside data.json under "settings".
 * Gson fills missing fields with the defaults below.
 */
public class Settings {

    // ---- Allocation ----

    /** Split new deposits across goals by their percentages. */
    public boolean autoAllocateDeposits = true;

    /** Give a brand-new goal its share of money right away. */
    public boolean fundNewGoals = true;

    /**
     * When a new goal is added:
     *  true  = re-split the whole balance across every goal by percentage
     *  false = only take the new goal's share from Unallocated (if there is any)
     */
    public boolean redistributeOnNewGoal = false;

    /** Stop putting money into a goal once it reaches its target. */
    public boolean stopAtTarget = false;

    // ---- Appearance ----

    public String theme = "dark";
    public String currency = "USD";
    public int recentCount = 6;
    public boolean hideAmounts = false;

    // ---- Data ----

    /** How many automatic backups to keep. 0 = keep all. */
    public int backupsToKeep = 50;

    // ---- Window ----

    public Integer windowX;
    public Integer windowY;
    public Integer windowWidth;
    public Integer windowHeight;
    public boolean windowMaximized = false;
    public String lastPage = "dashboard";
}
