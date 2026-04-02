package taskmanager.domain;

import java.util.ArrayList;
import java.util.List;

public class Task {
    private int id;
    private String title;
    private String description;
    private String creationDate;   // yyyy-MM-dd
    private String dueDate;        // yyyy-MM-dd, or empty string if none
    private Priority priority;
    private Status status;
    private Recurrence recurrence;
    private Project project;       // nullable
    private Collaborator collaborator; // nullable
    private List<Subtask> subtasks;
    private List<String> tags;

    public Task(int id, String title, String description, String creationDate,
                String dueDate, Priority priority, Status status,
                Recurrence recurrence, Project project, Collaborator collaborator) {
        this.id = id;
        this.title = title;
        this.description = description == null ? "" : description;
        this.creationDate = creationDate;
        this.dueDate = dueDate == null ? "" : dueDate;
        this.priority = priority;
        this.status = status;
        this.recurrence = recurrence;
        this.project = project;
        this.collaborator = collaborator;
        this.subtasks = new ArrayList<>();
        this.tags = new ArrayList<>();
    }

    public int getId() { return id; }
    public void setId(int id) { this.id = id; }
    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }
    public String getDescription() { return description; }
    public void setDescription(String desc) { this.description = desc == null ? "" : desc; }
    public String getCreationDate() { return creationDate; }
    public String getDueDate() { return dueDate; }
    public void setDueDate(String dueDate) { this.dueDate = dueDate == null ? "" : dueDate; }
    public Priority getPriority() { return priority; }
    public void setPriority(Priority p) { this.priority = p; }
    public Status getStatus() { return status; }
    public void setStatus(Status s) { this.status = s; }
    public Recurrence getRecurrence() { return recurrence; }
    public Project getProject() { return project; }
    public void setProject(Project project) { this.project = project; }
    public Collaborator getCollaborator() { return collaborator; }
    public void setCollaborator(Collaborator c) { this.collaborator = c; }
    public List<Subtask> getSubtasks() { return subtasks; }
    public void setSubtasks(List<Subtask> subtasks) { this.subtasks = subtasks; }
    public List<String> getTags() { return tags; }
    public void setTags(List<String> tags) { this.tags = tags; }

    /** True only when a non-empty due date is present. */
    public boolean hasDueDate() {
        return dueDate != null && !dueDate.isEmpty();
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("[").append(status).append("] ").append(title)
          .append(" (").append(priority).append(")");
        if (hasDueDate()) sb.append(" due: ").append(dueDate);
        if (project != null) sb.append(" [").append(project.getName()).append("]");
        if (collaborator != null) sb.append(" @").append(collaborator.getName());
        return sb.toString();
    }
}
