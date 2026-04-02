package taskmanager.persistence;

import taskmanager.domain.Project;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class ProjectRepository {

    public Project save(Project project) throws SQLException {
        if (project.getId() == -1) {
            String sql = "INSERT INTO projects (name, description) VALUES (?, ?)";
            try (PreparedStatement ps = Database.getConnection()
                    .prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
                ps.setString(1, project.getName());
                ps.setString(2, project.getDescription());
                ps.executeUpdate();
                try (ResultSet rs = ps.getGeneratedKeys()) {
                    if (rs.next()) project.setId(rs.getInt(1));
                }
            }
        } else {
            String sql = "UPDATE projects SET name=?, description=? WHERE id=?";
            try (PreparedStatement ps = Database.getConnection().prepareStatement(sql)) {
                ps.setString(1, project.getName());
                ps.setString(2, project.getDescription());
                ps.setInt(3, project.getId());
                ps.executeUpdate();
            }
        }
        return project;
    }

    public Optional<Project> findByName(String name) throws SQLException {
        String sql = "SELECT id, name, description FROM projects WHERE name = ?";
        try (PreparedStatement ps = Database.getConnection().prepareStatement(sql)) {
            ps.setString(1, name);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return Optional.of(mapRow(rs));
            }
        }
        return Optional.empty();
    }

    public Optional<Project> findById(int id) throws SQLException {
        String sql = "SELECT id, name, description FROM projects WHERE id = ?";
        try (PreparedStatement ps = Database.getConnection().prepareStatement(sql)) {
            ps.setInt(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return Optional.of(mapRow(rs));
            }
        }
        return Optional.empty();
    }

    public List<Project> findAll() throws SQLException {
        List<Project> list = new ArrayList<>();
        String sql = "SELECT id, name, description FROM projects ORDER BY name";
        try (Statement st = Database.getConnection().createStatement();
             ResultSet rs = st.executeQuery(sql)) {
            while (rs.next()) list.add(mapRow(rs));
        }
        return list;
    }

    private Project mapRow(ResultSet rs) throws SQLException {
        return new Project(rs.getInt("id"), rs.getString("name"), rs.getString("description"));
    }
}
