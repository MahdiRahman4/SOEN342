package taskmanager.domain;

public class Collaborator {
    private int id;
    private String name;
    private CollaboratorCategory category;
    /** The project this collaborator belongs to (spec: "defined under a project"). */
    private int projectId;

    public Collaborator(int id, String name, CollaboratorCategory category, int projectId) {
        this.id = id;
        this.name = name;
        this.category = category;
        this.projectId = projectId;
    }

    public Collaborator(String name, CollaboratorCategory category, int projectId) {
        this(-1, name, category, projectId);
    }

    public int getId() { return id; }
    public void setId(int id) { this.id = id; }
    public String getName() { return name; }
    public CollaboratorCategory getCategory() { return category; }
    public int getProjectId() { return projectId; }

    @Override
    public String toString() {
        return name + " (" + category + ", limit=" + category.getOpenTaskLimit() + ")";
    }
}
