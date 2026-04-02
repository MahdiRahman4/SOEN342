package taskmanager.domain;

public enum Priority {
    LOW, MEDIUM, HIGH;

    public static Priority fromString(String s) {
        try { return valueOf(s.trim().toUpperCase()); }
        catch (IllegalArgumentException e) { return MEDIUM; }
    }
}
