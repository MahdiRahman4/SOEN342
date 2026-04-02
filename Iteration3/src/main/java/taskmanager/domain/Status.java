package taskmanager.domain;

public enum Status {
    OPEN, COMPLETED, CANCELLED;

    public static Status fromString(String s) {
        try { return valueOf(s.trim().toUpperCase()); }
        catch (IllegalArgumentException e) { return OPEN; }
    }
}
