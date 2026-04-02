package taskmanager.persistence;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;

/**
 * Manages the single SQLite connection and schema initialisation.
 * The database file is created in the working directory on first run.
 */
public class Database {

    private static final String DB_FILE = "taskmanager.db";
    private static Connection connection;

    public static Connection getConnection() throws SQLException {
        if (connection == null || connection.isClosed()) {
            connection = DriverManager.getConnection("jdbc:sqlite:" + DB_FILE);
            connection.setAutoCommit(true);
            initSchema(connection);
        }
        return connection;
    }

    private static void initSchema(Connection conn) throws SQLException {
        try (Statement st = conn.createStatement()) {
            st.executeUpdate("""
                CREATE TABLE IF NOT EXISTS projects (
                    id          INTEGER PRIMARY KEY AUTOINCREMENT,
                    name        TEXT    UNIQUE NOT NULL,
                    description TEXT    NOT NULL DEFAULT ''
                )
                """);

            st.executeUpdate("""
                CREATE TABLE IF NOT EXISTS collaborators (
                    id       INTEGER PRIMARY KEY AUTOINCREMENT,
                    name     TEXT NOT NULL,
                    category TEXT NOT NULL
                )
                """);

            // Migration: add project_id to collaborators if it doesn't exist yet.
            // SQLite does not support IF NOT EXISTS on ALTER TABLE, so we catch the error.
            try {
                st.executeUpdate(
                    "ALTER TABLE collaborators ADD COLUMN project_id INTEGER REFERENCES projects(id)");
            } catch (SQLException ignored) { /* column already present */ }

            st.executeUpdate("""
                CREATE TABLE IF NOT EXISTS tasks (
                    id              INTEGER PRIMARY KEY AUTOINCREMENT,
                    title           TEXT    NOT NULL,
                    description     TEXT    NOT NULL DEFAULT '',
                    creation_date   TEXT    NOT NULL,
                    due_date        TEXT    NOT NULL DEFAULT '',
                    priority        TEXT    NOT NULL DEFAULT 'MEDIUM',
                    status          TEXT    NOT NULL DEFAULT 'OPEN',
                    recurrence      TEXT    NOT NULL DEFAULT 'NONE',
                    project_id      INTEGER REFERENCES projects(id),
                    collaborator_id INTEGER REFERENCES collaborators(id)
                )
                """);

            st.executeUpdate("""
                CREATE TABLE IF NOT EXISTS subtasks (
                    id             INTEGER PRIMARY KEY AUTOINCREMENT,
                    title          TEXT    NOT NULL,
                    status         TEXT    NOT NULL DEFAULT 'OPEN',
                    parent_task_id INTEGER NOT NULL REFERENCES tasks(id) ON DELETE CASCADE
                )
                """);

            st.executeUpdate("""
                CREATE TABLE IF NOT EXISTS task_tags (
                    task_id INTEGER NOT NULL REFERENCES tasks(id) ON DELETE CASCADE,
                    tag     TEXT    NOT NULL,
                    PRIMARY KEY (task_id, tag)
                )
                """);
        }
    }

    public static void close() {
        if (connection != null) {
            try { connection.close(); } catch (SQLException ignored) {}
            connection = null;
        }
    }
}
