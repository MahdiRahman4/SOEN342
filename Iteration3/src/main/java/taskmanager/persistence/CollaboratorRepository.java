package taskmanager.persistence;

import taskmanager.domain.Collaborator;
import taskmanager.domain.CollaboratorCategory;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class CollaboratorRepository {

    public Collaborator save(Collaborator c) throws SQLException {
        if (c.getId() == -1) {
            String sql = "INSERT INTO collaborators (name, category, project_id) VALUES (?, ?, ?)";
            try (PreparedStatement ps = Database.getConnection()
                    .prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
                ps.setString(1, c.getName());
                ps.setString(2, c.getCategory().name());
                ps.setInt(3, c.getProjectId());
                ps.executeUpdate();
                try (ResultSet rs = ps.getGeneratedKeys()) {
                    if (rs.next()) c.setId(rs.getInt(1));
                }
            }
        } else {
            String sql = "UPDATE collaborators SET name=?, category=?, project_id=? WHERE id=?";
            try (PreparedStatement ps = Database.getConnection().prepareStatement(sql)) {
                ps.setString(1, c.getName());
                ps.setString(2, c.getCategory().name());
                ps.setInt(3, c.getProjectId());
                ps.setInt(4, c.getId());
                ps.executeUpdate();
            }
        }
        return c;
    }

    public Optional<Collaborator> findById(int id) throws SQLException {
        String sql = "SELECT id, name, category, project_id FROM collaborators WHERE id = ?";
        try (PreparedStatement ps = Database.getConnection().prepareStatement(sql)) {
            ps.setInt(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return Optional.of(mapRow(rs));
            }
        }
        return Optional.empty();
    }

    public List<Collaborator> findAll() throws SQLException {
        List<Collaborator> list = new ArrayList<>();
        String sql = "SELECT id, name, category, project_id FROM collaborators ORDER BY name";
        try (Statement st = Database.getConnection().createStatement();
             ResultSet rs = st.executeQuery(sql)) {
            while (rs.next()) list.add(mapRow(rs));
        }
        return list;
    }

    /** Returns all collaborators belonging to the given project. */
    public List<Collaborator> findByProjectId(int projectId) throws SQLException {
        List<Collaborator> list = new ArrayList<>();
        String sql = "SELECT id, name, category, project_id FROM collaborators " +
                     "WHERE project_id = ? ORDER BY name";
        try (PreparedStatement ps = Database.getConnection().prepareStatement(sql)) {
            ps.setInt(1, projectId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) list.add(mapRow(rs));
            }
        }
        return list;
    }

    private Collaborator mapRow(ResultSet rs) throws SQLException {
        CollaboratorCategory cat = CollaboratorCategory.valueOf(rs.getString("category"));
        int projectId = rs.getInt("project_id");
        if (rs.wasNull()) projectId = -1;
        return new Collaborator(rs.getInt("id"), rs.getString("name"), cat, projectId);
    }
}
