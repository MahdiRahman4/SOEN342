package taskmanager.domain;

public enum Recurrence {
    NONE, DAILY, WEEKLY, MONTHLY;

    public static Recurrence fromString(String s) {
        try { return valueOf(s.trim().toUpperCase()); }
        catch (IllegalArgumentException e) { return NONE; }
    }
}
