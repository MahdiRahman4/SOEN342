package taskmanager.persistence;

import taskmanager.domain.*;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class TaskRepository {

    private final ProjectRepository projectRepo;
    private final CollaboratorRepository collaboratorRepo;
    private final SubtaskRepository subtaskRepo;

    public TaskRepository(ProjectRepository projectRepo,
                          CollaboratorRepository collaboratorRepo,
                          SubtaskRepository subtaskRepo) {
        this.projectRepo      = projectRepo;
        this.collaboratorRepo = collaboratorRepo;
        this.subtaskRepo      = subtaskRepo;
    }

    /** Insert or update a task and its tags. */
    public Task save(Task task) throws SQLException {
        if (task.getId() == -1) {
            String sql = """
                INSERT INTO tasks
                  (title, description, creation_date, due_date,
                   priority, status, recurrence, project_id, collaborator_id)
                VALUES (?,?,?,?,?,?,?,?,?)
                """;
            try (PreparedStatement ps = Database.getConnection()
                    .prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
                bindTask(ps, task);
                ps.executeUpdate();
                try (ResultSet rs = ps.getGeneratedKeys()) {
                    if (rs.next()) task.setId(rs.getInt(1));
                }
            }
        } else {
            String sql = """
                UPDATE tasks
                SET title=?, description=?, creation_date=?, due_date=?,
                    priority=?, status=?, recurrence=?, project_id=?, collaborator_id=?
                WHERE id=?
                """;
            try (PreparedStatement ps = Database.getConnection().prepareStatement(sql)) {
                bindTask(ps, task);
                ps.setInt(10, task.getId());
                ps.executeUpdate();
            }
        }
        persistTags(task);
        return task;
    }

    private void bindTask(PreparedStatement ps, Task task) throws SQLException {
        ps.setString(1, task.getTitle());
        ps.setString(2, task.getDescription());
        ps.setString(3, task.getCreationDate());
        ps.setString(4, task.getDueDate());
        ps.setString(5, task.getPriority().name());
        ps.setString(6, task.getStatus().name());
        ps.setString(7, task.getRecurrence().name());
        if (task.getProject() != null) ps.setInt(8, task.getProject().getId());
        else ps.setNull(8, Types.INTEGER);
        if (task.getCollaborator() != null) ps.setInt(9, task.getCollaborator().getId());
        else ps.setNull(9, Types.INTEGER);
    }

    private void persistTags(Task task) throws SQLException {
        if (task.getId() == -1) return;
        try (PreparedStatement del = Database.getConnection()
                .prepareStatement("DELETE FROM task_tags WHERE task_id=?")) {
            del.setInt(1, task.getId());
            del.executeUpdate();
        }
        for (String tag : task.getTags()) {
            try (PreparedStatement ins = Database.getConnection()
                    .prepareStatement("INSERT OR IGNORE INTO task_tags(task_id,tag) VALUES(?,?)")) {
                ins.setInt(1, task.getId());
                ins.setString(2, tag);
                ins.executeUpdate();
            }
        }
    }

    public List<Task> findAll() throws SQLException {
        String sql = """
            SELECT * FROM tasks
            ORDER BY CASE WHEN due_date='' THEN 1 ELSE 0 END, due_date ASC
            """;
        List<Task> list = new ArrayList<>();
        try (Statement st = Database.getConnection().createStatement();
             ResultSet rs = st.executeQuery(sql)) {
            while (rs.next()) list.add(mapRow(rs));
        }
        return hydrate(list);
    }

    public List<Task> findByProject(int projectId) throws SQLException {
        String sql = "SELECT * FROM tasks WHERE project_id=? ORDER BY due_date ASC";
        List<Task> list = new ArrayList<>();
        try (PreparedStatement ps = Database.getConnection().prepareStatement(sql)) {
            ps.setInt(1, projectId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) list.add(mapRow(rs));
            }
        }
        return hydrate(list);
    }

    public List<Task> findByCollaborator(int collaboratorId) throws SQLException {
        String sql = "SELECT * FROM tasks WHERE collaborator_id=?";
        List<Task> list = new ArrayList<>();
        try (PreparedStatement ps = Database.getConnection().prepareStatement(sql)) {
            ps.setInt(1, collaboratorId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) list.add(mapRow(rs));
            }
        }
        return hydrate(list);
    }

    public Optional<Task> findById(int id) throws SQLException {
        String sql = "SELECT * FROM tasks WHERE id=?";
        try (PreparedStatement ps = Database.getConnection().prepareStatement(sql)) {
            ps.setInt(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    Task t = mapRow(rs);
                    hydrate(List.of(t));
                    return Optional.of(t);
                }
            }
        }
        return Optional.empty();
    }

    /**
     * Full-text search across title and description, returning results sorted
     * by due date ascending (tasks without a due date appear last).
     */
    public List<Task> search(String keyword) throws SQLException {
        String kw = "%" + keyword.trim().toLowerCase() + "%";
        String sql = """
            SELECT * FROM tasks
            WHERE lower(title) LIKE ? OR lower(description) LIKE ?
            ORDER BY CASE WHEN due_date='' THEN 1 ELSE 0 END, due_date ASC
            """;
        List<Task> list = new ArrayList<>();
        try (PreparedStatement ps = Database.getConnection().prepareStatement(sql)) {
            ps.setString(1, kw);
            ps.setString(2, kw);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) list.add(mapRow(rs));
            }
        }
        return hydrate(list);
    }

    /** Loads subtasks and tags for each task (avoids N+1 for small datasets). */
    private List<Task> hydrate(List<Task> tasks) throws SQLException {
        for (Task t : tasks) {
            t.setSubtasks(subtaskRepo.findByTaskId(t.getId()));
            List<String> tags = new ArrayList<>();
            try (PreparedStatement ps = Database.getConnection()
                    .prepareStatement("SELECT tag FROM task_tags WHERE task_id=?")) {
                ps.setInt(1, t.getId());
                try (ResultSet rs = ps.executeQuery()) {
                    while (rs.next()) tags.add(rs.getString("tag"));
                }
            }
            t.setTags(tags);
        }
        return tasks;
    }

    private Task mapRow(ResultSet rs) throws SQLException {
        int projectId = rs.getInt("project_id");
        Project project = rs.wasNull() ? null : projectRepo.findById(projectId).orElse(null);

        int collabId = rs.getInt("collaborator_id");
        Collaborator collaborator = rs.wasNull() ? null
            : collaboratorRepo.findById(collabId).orElse(null);

        return new Task(
            rs.getInt("id"),
            rs.getString("title"),
            rs.getString("description"),
            rs.getString("creation_date"),
            rs.getString("due_date"),
            Priority.fromString(rs.getString("priority")),
            Status.fromString(rs.getString("status")),
            Recurrence.fromString(rs.getString("recurrence")),
            project,
            collaborator
        );
    }
}
