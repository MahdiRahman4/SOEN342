package taskmanager.persistence;

import taskmanager.domain.Status;
import taskmanager.domain.Subtask;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class SubtaskRepository {

    public Subtask save(Subtask s) throws SQLException {
        if (s.getId() == -1) {
            String sql = "INSERT INTO subtasks (title, status, parent_task_id) VALUES (?, ?, ?)";
            try (PreparedStatement ps = Database.getConnection()
                    .prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
                ps.setString(1, s.getTitle());
                ps.setString(2, s.getStatus().name());
                ps.setInt(3, s.getParentTaskId());
                ps.executeUpdate();
                try (ResultSet rs = ps.getGeneratedKeys()) {
                    if (rs.next()) s.setId(rs.getInt(1));
                }
            }
        } else {
            String sql = "UPDATE subtasks SET title=?, status=? WHERE id=?";
            try (PreparedStatement ps = Database.getConnection().prepareStatement(sql)) {
                ps.setString(1, s.getTitle());
                ps.setString(2, s.getStatus().name());
                ps.setInt(3, s.getId());
                ps.executeUpdate();
            }
        }
        return s;
    }

    public List<Subtask> findByTaskId(int taskId) throws SQLException {
        List<Subtask> list = new ArrayList<>();
        String sql = "SELECT id, title, status, parent_task_id FROM subtasks WHERE parent_task_id = ?";
        try (PreparedStatement ps = Database.getConnection().prepareStatement(sql)) {
            ps.setInt(1, taskId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) list.add(mapRow(rs));
            }
        }
        return list;
    }

    private Subtask mapRow(ResultSet rs) throws SQLException {
        return new Subtask(
            rs.getInt("id"),
            rs.getString("title"),
            Status.fromString(rs.getString("status")),
            rs.getInt("parent_task_id")
        );
    }
}
