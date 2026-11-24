package com.carsen.moneymanager.model;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

/**
 * Everything that is saved to data.json.
 *
 * Field names must stay the same as in v1.x so existing data files keep
 * loading. New fields are optional: older files simply leave them out.
 */
public class AppData {

    /** Format version of the file. 1 = original app, 2 = this version. */
    public Integer version = 2;

    public BigDecimal totalBalance = BigDecimal.ZERO;
    public BigDecimal unallocatedBalance = BigDecimal.ZERO;

    public List<Goal> goals = new ArrayList<>();
    public List<Transaction> transactions = new ArrayList<>();

    /**
     * Setting from v1.1. Still written (mirrors settings.autoAllocateDeposits)
     * so an older copy of the app can open the file without surprises.
     */
    public Boolean automaticAllocation;

    /** Added in v2. Missing in older files. */
    public Settings settings;
}
