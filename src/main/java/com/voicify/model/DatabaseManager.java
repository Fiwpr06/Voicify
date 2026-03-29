package com.voicify.model;

import com.voicify.config.ConfigurationManager;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

import com.voicify.util.Dialog;
import javafx.scene.control.Alert.AlertType;

public class DatabaseManager {
    private static final String DEFAULT_SERVER = "DESKTOP-4ET29IJ\\SQLEXPRESS";
    private static final String DEFAULT_PORT = "1433";
    private static final String DEFAULT_DATABASE = "Voicify";
    private static final String DEFAULT_USERNAME = "sa";
    private static final String DEFAULT_PASSWORD = "";

    private static DatabaseManager instance;
    private String connectionUrl;
    private String dbUsername;
    private String dbPassword;
    private String dbName;

    private DatabaseManager() {
        try {
            Class.forName("com.microsoft.sqlserver.jdbc.SQLServerDriver");
        } catch (ClassNotFoundException e) {
            Dialog.showAlert(AlertType.ERROR, "Driver Error", "JDBC Driver not found: " + e.getMessage());
            throw new RuntimeException("JDBC Driver not found", e);
        }
        loadConnectionSettings();
    }

    private void loadConnectionSettings() {
        String server = getSetting("DB_SERVER", ConfigurationManager.DB_SERVER, DEFAULT_SERVER);
        String port = getSetting("DB_PORT", ConfigurationManager.DB_PORT, DEFAULT_PORT);
        this.dbName = getSetting("DB_NAME", ConfigurationManager.DB_NAME, DEFAULT_DATABASE);
        this.dbUsername = getSetting("DB_USER", ConfigurationManager.DB_USERNAME, DEFAULT_USERNAME);
        this.dbPassword = getSetting("DB_PASSWORD", ConfigurationManager.DB_PASSWORD, DEFAULT_PASSWORD);

        StringBuilder urlBuilder = new StringBuilder();
        urlBuilder.append("jdbc:sqlserver://")
                .append(server)
                .append(":")
                .append(port)
                .append(";databaseName=")
                .append(dbName)
                .append(";encrypt=true")
                .append(";trustServerCertificate=true")
                .append(";integratedSecurity=false");
        this.connectionUrl = urlBuilder.toString();
    }

    private String getSetting(String envKey, String configKey, String defaultValue) {
        String systemProperty = System.getProperty(envKey);
        if (systemProperty != null && !systemProperty.trim().isEmpty()) {
            return systemProperty;
        }

        String envValue = System.getenv(envKey);
        if (envValue != null && !envValue.trim().isEmpty()) {
            return envValue;
        }

        String configValue = ConfigurationManager.getInstance().getProperty(configKey, defaultValue);
        if (configValue != null && !configValue.trim().isEmpty()) {
            return configValue;
        }

        return defaultValue;
    }

    public static synchronized DatabaseManager getInstance() {
        if (instance == null) {
            instance = new DatabaseManager();
        }
        return instance;
    }

    public Connection getConnection() {
        try {
            Properties props = new Properties();
            props.setProperty("user", dbUsername);
            props.setProperty("password", dbPassword);
            props.setProperty("loginTimeout", "30");
            Connection connection = DriverManager.getConnection(connectionUrl, props);
            System.out.println("Kết nối thành công với database: " + dbName);
            return connection;
        } catch (SQLException e) {
            Dialog.showAlert(AlertType.ERROR, "Connection Error", "Failed to connect to database: " + e.getMessage());
            return null;
        }
    }

    public boolean testConnection() {
        try (Connection connection = getConnection()) {
            return connection != null && !connection.isClosed();
        } catch (SQLException e) {
            Dialog.showAlert(AlertType.ERROR, "Connection Error", "Database connection test failed: " + e.getMessage());
            return false;
        }
    }

    public void initializeDatabase() {
        // First check if Users table exists
        String createUsersTable = """
            IF NOT EXISTS (SELECT * FROM sysobjects WHERE name='Users' AND xtype='U')
            CREATE TABLE Users (
                user_id INT IDENTITY(1,1) PRIMARY KEY,
                username NVARCHAR(50) NOT NULL UNIQUE,
                email NVARCHAR(100) NOT NULL UNIQUE,
                password_hash NVARCHAR(255) NOT NULL,
                created_at DATETIME NOT NULL DEFAULT GETDATE(),
                is_admin BIT NOT NULL DEFAULT 0,
                verification_token NVARCHAR(255) NULL,
                is_verified BIT NOT NULL DEFAULT 0
            )
            """;
        
        // Add missing columns if table exists but columns don't
        String addVerificationColumns = """
            IF EXISTS (SELECT * FROM sysobjects WHERE name='Users' AND xtype='U')
            AND NOT EXISTS (SELECT * FROM syscolumns WHERE name='verification_token' AND id=OBJECT_ID('Users'))
            ALTER TABLE Users ADD verification_token NVARCHAR(255) NULL
            """;
        
        String addIsVerifiedColumn = """
            IF EXISTS (SELECT * FROM sysobjects WHERE name='Users' AND xtype='U')
            AND NOT EXISTS (SELECT * FROM syscolumns WHERE name='is_verified' AND id=OBJECT_ID('Users'))
            ALTER TABLE Users ADD is_verified BIT NOT NULL DEFAULT 0
            """;
        
        // Execute the SQL to create or update the table
        try (Connection conn = getConnection();
             Statement stmt = conn.createStatement()) {
            stmt.execute(createUsersTable);
            stmt.execute(addVerificationColumns);
            stmt.execute(addIsVerifiedColumn);
            System.out.println("Database schema initialized successfully");
        } catch (SQLException e) {
            System.err.println("Error initializing database: " + e.getMessage());
        }
        
        // Create TranslationHistory table if it doesn't exist
        String createTranslationHistoryTable = """
            IF NOT EXISTS (SELECT * FROM sysobjects WHERE name='TranslationHistory' AND xtype='U')
            CREATE TABLE TranslationHistory (
                id INT IDENTITY(1,1) PRIMARY KEY,
                user_id INT NULL,
                source_text NVARCHAR(MAX) NOT NULL,
                translated_text NVARCHAR(MAX) NOT NULL,
                translation_type NVARCHAR(50) NOT NULL,
                audio_file_path NVARCHAR(255) NULL,
                created_at DATETIME NOT NULL DEFAULT GETDATE(),
                improved_by_ai BIT NOT NULL DEFAULT 0,
                ai_translation_text NVARCHAR(MAX) NULL,
                FOREIGN KEY (user_id) REFERENCES Users(user_id) ON DELETE SET NULL
            )
            """;
        
        try (Connection conn = getConnection();
             Statement stmt = conn.createStatement()) {
            stmt.execute(createTranslationHistoryTable);
        } catch (SQLException e) {
            System.err.println("Error creating TranslationHistory table: " + e.getMessage());
        }
    }

    public static void closeResources(Connection connection, PreparedStatement statement, ResultSet resultSet) {
        try {
            if (resultSet != null) {
                resultSet.close();
            }
        } catch (SQLException e) {
            Dialog.showAlert(AlertType.ERROR, "Resource Error", "Failed to close ResultSet: " + e.getMessage());
        }
        try {
            if (statement != null) {
                statement.close();
            }
        } catch (SQLException e) {
            Dialog.showAlert(AlertType.ERROR, "Resource Error", "Failed to close PreparedStatement: " + e.getMessage());
        }
        try {
            if (connection != null) {
                connection.close();
            }
        } catch (SQLException e) {
            Dialog.showAlert(AlertType.ERROR, "Resource Error", "Failed to close Connection: " + e.getMessage());
        }
    }


    public void updateConfiguration(String server, String port, String database, String username, String password) {
        this.dbName = database;
        this.dbUsername = username;
        this.dbPassword = password;
        this.connectionUrl = "jdbc:sqlserver://" + server + ":" + port +
                ";databaseName=" + database +
                ";encrypt=true;trustServerCertificate=true;integratedSecurity=false";
        System.setProperty("DB_SERVER", server);
        System.setProperty("DB_PORT", port);
        System.setProperty("DB_NAME", database);
        System.setProperty("DB_USER", username);
        System.setProperty("DB_PASSWORD", password);
        System.out.println("Database configuration updated successfully");
    }

    public void saveTranslation(TranslationHistory translation) {
        String sql = "INSERT INTO TranslationHistory (source_text, translated_text, translation_type, audio_file_path, created_at, improved_by_ai, ai_translation_text) VALUES (?, ?, ?, ?, ?, ?, ?)";
        try (Connection conn = getConnection(); PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, translation.getSourceText());
            stmt.setString(2, translation.getTranslatedText());
            stmt.setString(3, translation.getTranslationType());
            stmt.setString(4, translation.getAudioFilePath());
            stmt.setTimestamp(5, Timestamp.valueOf(translation.getCreatedAt()));
            stmt.setBoolean(6, translation.isImprovedByAi());
            stmt.setString(7, translation.getAiTranslationText());
            stmt.executeUpdate();
            System.out.println("Translation saved successfully");
        } catch (SQLException e) {
            Dialog.showAlert(AlertType.ERROR, "Save Error", "Failed to save translation: " + e.getMessage());
        }
    }

    public List<TranslationHistory> getTranslationHistory(int userId) {
        List<TranslationHistory> historyList = new ArrayList<>();
        String sql = "SELECT * FROM TranslationHistory WHERE user_id = ? ORDER BY created_at DESC";

        try (Connection conn = getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setInt(1, userId);

            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    TranslationHistory history = new TranslationHistory(
                        rs.getInt("id"),
                        rs.getInt("user_id"),
                        rs.getString("source_text"),
                        rs.getString("translated_text"),
                        rs.getString("translation_type"),
                        rs.getString("audio_file_path"),
                        rs.getTimestamp("created_at").toLocalDateTime(),
                        rs.getBoolean("improved_by_ai"),
                        rs.getString("ai_translation_text")
                    );
                    historyList.add(history);
                }
            }
        } catch (SQLException e) {
            System.err.println("Error retrieving translation history: " + e.getMessage());
        }

        return historyList;
    }

    public TranslationHistory getTranslationById(int id) {
        String sql = "SELECT * FROM TranslationHistory WHERE id = ?";

        try (Connection conn = getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setInt(1, id);

            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return new TranslationHistory(
                        rs.getInt("id"),
                        rs.getInt("user_id"),
                        rs.getString("source_text"),
                        rs.getString("translated_text"),
                        rs.getString("translation_type"),
                        rs.getString("audio_file_path"),
                        rs.getTimestamp("created_at").toLocalDateTime(),
                        rs.getBoolean("improved_by_ai"),
                        rs.getString("ai_translation_text")
                    );
                }
            }
        } catch (SQLException e) {
            System.err.println("Error retrieving translation: " + e.getMessage());
        }

        return null;
    }

    public List<TranslationHistory> searchTranslationHistory(String keyword) {
        List<TranslationHistory> history = new ArrayList<>();
        String sql = "SELECT * FROM TranslationHistory WHERE source_text LIKE ? OR translated_text LIKE ? OR ai_translation_text LIKE ? ORDER BY created_at DESC";
        try (Connection conn = getConnection(); PreparedStatement stmt = conn.prepareStatement(sql)) {
            String searchPattern = "%" + keyword + "%";
            stmt.setString(1, searchPattern);
            stmt.setString(2, searchPattern);
            stmt.setString(3, searchPattern);
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    TranslationHistory translation = new TranslationHistory(
                            rs.getInt("id"),
                            rs.getString("source_text"),
                            rs.getString("translated_text"),
                            rs.getString("translation_type"),
                            rs.getString("audio_file_path"),
                            rs.getTimestamp("created_at").toLocalDateTime(),
                            rs.getBoolean("improved_by_ai"),
                            rs.getString("ai_translation_text")
                    );
                    history.add(translation);
                }
            }
        } catch (SQLException e) {
            Dialog.showAlert(AlertType.ERROR, "Search Error", "Failed to search translation history: " + e.getMessage());
        }
        return history;
    }

    public void updateTranslation(TranslationHistory translation) {
        String sql = "UPDATE TranslationHistory SET source_text = ?, translated_text = ?, translation_type = ?, audio_file_path = ?, improved_by_ai = ?, ai_translation_text = ? WHERE id = ?";
        try (Connection conn = getConnection(); PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, translation.getSourceText());
            stmt.setString(2, translation.getTranslatedText());
            stmt.setString(3, translation.getTranslationType());
            stmt.setString(4, translation.getAudioFilePath());
            stmt.setBoolean(5, translation.isImprovedByAi());
            stmt.setString(6, translation.getAiTranslationText());
            stmt.setInt(7, translation.getId());
            int rowsAffected = stmt.executeUpdate();
            if (rowsAffected > 0) {
                System.out.println("Translation updated successfully");
            } else {
                Dialog.showAlert(AlertType.WARNING, "Update Warning", "No translation found with ID: " + translation.getId());
            }
        } catch (SQLException e) {
            Dialog.showAlert(AlertType.ERROR, "Update Error", "Failed to update translation: " + e.getMessage());
        }
    }

    public void deleteTranslation(int id) {
        String sql = "DELETE FROM TranslationHistory WHERE id = ?";
        try (Connection conn = getConnection(); PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, id);
            int rowsAffected = stmt.executeUpdate();
            if (rowsAffected > 0) {
                System.out.println("Translation deleted successfully");
            } else {
                Dialog.showAlert(AlertType.WARNING, "Delete Warning", "No translation found with ID: " + id);
            }
        } catch (SQLException e) {
            Dialog.showAlert(AlertType.ERROR, "Delete Error", "Failed to delete translation: " + e.getMessage());
        }
    }

    public void exportTranslationHistoryToCSV(String filePath) {
        String sql = "SELECT * FROM TranslationHistory ORDER BY created_at DESC";
        try (Connection conn = getConnection(); PreparedStatement stmt = conn.prepareStatement(sql); ResultSet rs = stmt.executeQuery();
             java.io.PrintWriter writer = new java.io.PrintWriter(filePath, "UTF-8")) {
            writer.println("id,source_text,translated_text,translation_type,audio_file_path,created_at,improved_by_ai,ai_translation_text");
            while (rs.next()) {
                writer.println(String.format("%d,\"%s\",\"%s\",\"%s\",\"%s\",\"%s\",%b,\"%s\"",
                        rs.getInt("id"),
                        rs.getString("source_text").replace("\"", "\"\""),
                        rs.getString("translated_text").replace("\"", "\"\""),
                        rs.getString("translation_type"),
                        rs.getString("audio_file_path") != null ? rs.getString("audio_file_path").replace("\"", "\"\"") : "",
                        rs.getTimestamp("created_at").toLocalDateTime(),
                        rs.getBoolean("improved_by_ai"),
                        rs.getString("ai_translation_text") != null ? rs.getString("ai_translation_text").replace("\"", "\"\"") : ""));
            }
            System.out.println("Translation history exported to " + filePath);
        } catch (Exception e) {
            Dialog.showAlert(AlertType.ERROR, "Export Error", "Failed to export translation history: " + e.getMessage());
        }
    }
}
