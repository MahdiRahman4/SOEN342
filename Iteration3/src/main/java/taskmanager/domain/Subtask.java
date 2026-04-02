package taskmanager.domain;

public class Subtask {
    private int id;
    private String title;
    private Status status;
    private int parentTaskId;

    public Subtask(int id, String title, Status status, int parentTaskId) {
        this.id = id;
        this.title = title;
        this.status = status;
        this.parentTaskId = parentTaskId;
    }

    public Subtask(String title, int parentTaskId) {
        this(-1, title, Status.OPEN, parentTaskId);
    }

    public int getId() { return id; }
    public void setId(int id) { this.id = id; }
    public String getTitle() { return title; }
    public Status getStatus() { return status; }
    public void setStatus(Status status) { this.status = status; }
    public int getParentTaskId() { return parentTaskId; }

    @Override
    public String toString() { return "[" + status + "] " + title; }
}
