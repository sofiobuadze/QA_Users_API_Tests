package ge.qa.db;

import ge.qa.config.Config;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/** Stores test results in SQLite: UPDATE when test_name exists, otherwise INSERT. */
public class ResultRepository {

    private static final DateTimeFormatter FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    private static Connection connect() throws SQLException {
        return DriverManager.getConnection("jdbc:sqlite:" + Config.dbPath());
    }

    public void init() {
        String ddl = "CREATE TABLE IF NOT EXISTS test_results ("
                + "id INTEGER PRIMARY KEY AUTOINCREMENT, "
                + "test_name TEXT, "
                + "status TEXT, "
                + "execution_time DATETIME)";
        try (Connection c = connect(); Statement s = c.createStatement()) {
            s.execute(ddl);
        } catch (SQLException e) {
            throw new IllegalStateException("Cannot initialise SQLite database", e);
        }
    }

    public void save(String testName, String status) {
        String now = LocalDateTime.now().format(FMT);
        try (Connection c = connect()) {
            boolean exists;
            try (PreparedStatement ps = c.prepareStatement("SELECT id FROM test_results WHERE test_name = ?")) {
                ps.setString(1, testName);
                try (ResultSet rs = ps.executeQuery()) {
                    exists = rs.next();
                }
            }
            String sql = exists
                    ? "UPDATE test_results SET status = ?, execution_time = ? WHERE test_name = ?"
                    : "INSERT INTO test_results (status, execution_time, test_name) VALUES (?, ?, ?)";
            try (PreparedStatement ps = c.prepareStatement(sql)) {
                ps.setString(1, status);
                ps.setString(2, now);
                ps.setString(3, testName);
                ps.executeUpdate();
            }
        } catch (SQLException e) {
            throw new IllegalStateException("Cannot save result for " + testName, e);
        }
    }
}
