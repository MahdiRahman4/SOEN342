package taskmanager.domain;

/**
 * Collaborator category, each with a fixed open-task limit.
 *
 * OCL: context CollaboratorCategory inv CategoryLimitPositive:
 *   self.openTaskLimit > 0
 */
public enum CollaboratorCategory {
    JUNIOR(10),
    INTERMEDIATE(5),
    SENIOR(2);

    private final int openTaskLimit;

    CollaboratorCategory(int openTaskLimit) {
        this.openTaskLimit = openTaskLimit;
    }

    public int getOpenTaskLimit() {
        return openTaskLimit;
    }

    public static CollaboratorCategory fromString(String s) {
        try { return valueOf(s.trim().toUpperCase()); }
        catch (IllegalArgumentException e) { return JUNIOR; }
    }
}
