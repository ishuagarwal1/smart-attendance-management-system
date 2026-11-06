package com.sams.model;

import java.sql.Date;

/** Present / total counts for one calendar day (used by the dashboard chart). */
public class DailyStat {
    private final Date date;
    private final int present;
    private final int total;

    public DailyStat(Date date, int present, int total) {
        this.date = date;
        this.present = present;
        this.total = total;
    }
    public Date getDate() { return date; }
    public int getPresent() { return present; }
    public int getTotal() { return total; }
    public int getPercent() { return total == 0 ? 0 : (int) Math.round(100.0 * present / total); }
}
