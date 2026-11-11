package com.bac.banking.dao;

import com.bac.banking.exception.DataAccessException;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;

/**
 * Owns the JDBC connection settings and creates the schema on first use.
 * The application uses an H2 file database (./data/bankdb); tests use an in-memory database.
 */
public class DatabaseManager {
    public static final String DEFAULT_URL = "jdbc:h2:./data/bankdb;AUTO_SERVER=TRUE";

    private final String url;
    private final String user;
    private final String password;

    public DatabaseManager(String url, String user, String password) {
        this.url = url;
        this.user = user;
        this.password = password;
    }

    /** The file-based database used by the running application. */
    public static DatabaseManager fileDatabase() {
        return new DatabaseManager(DEFAULT_URL, "sa", "");
    }

    /** A private in-memory database, used by the tests. */
    public static DatabaseManager inMemory(String name) {
        return new DatabaseManager("jdbc:h2:mem:" + name + ";DB_CLOSE_DELAY=-1", "sa", "");
    }

    public Connection getConnection() throws SQLException {
        return DriverManager.getConnection(url, user, password);
    }

    /** Runs schema.sql from the classpath. Safe to call every start-up (IF NOT EXISTS). */
    public void initialize() {
        String script = readSchema();
        try (Connection connection = getConnection(); Statement statement = connection.createStatement()) {
            for (String sql : script.split(";")) {
                String cleaned = stripComments(sql);
                if (!cleaned.isBlank()) {
                    statement.execute(cleaned);
                }
            }
        } catch (SQLException e) {
            throw new DataAccessException("Could not create the database schema", e);
        }
    }

    private String readSchema() {
        try (InputStream in = DatabaseManager.class.getResourceAsStream("/schema.sql")) {
            if (in == null) {
                throw new IllegalStateException("schema.sql not found on the classpath");
            }
            return new String(in.readAllBytes(), StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new IllegalStateException("Could not read schema.sql", e);
        }
    }

    private static String stripComments(String sql) {
        StringBuilder sb = new StringBuilder();
        for (String line : sql.split("\\R")) {
            if (!line.trim().startsWith("--")) {
                sb.append(line).append('\n');
            }
        }
        return sb.toString().trim();
    }

    public String getUrl() {
        return url;
    }
}
