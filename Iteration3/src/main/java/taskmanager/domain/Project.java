package taskmanager.domain;

public class Project {
    private int id;
    private String name;
    private String description;

    public Project(int id, String name, String description) {
        this.id = id;
        this.name = name;
        this.description = description;
    }

    public Project(String name, String description) {
        this(-1, name, description);
    }

    public int getId() { return id; }
    public void setId(int id) { this.id = id; }
    public String getName() { return name; }
    public String getDescription() { return description == null ? "" : description; }

    @Override
    public String toString() { return name; }
}
