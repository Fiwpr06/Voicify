package com.voicify.config;

import io.github.cdimascio.dotenv.Dotenv;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Properties;

/**
 * Centralized configuration management for Voicify application
 * Handles loading configuration from multiple sources with priority:
 * 1. Environment variables
 * 2. .env file
 * 3. application.properties
 * 4. Default values
 */
public class ConfigurationManager {
    private static final Logger logger = LoggerFactory.getLogger(ConfigurationManager.class);
    private static ConfigurationManager instance;
    private final Properties properties;
    private final Dotenv dotenv;

    // Configuration keys
    public static final String VOSK_MODEL_PATH = "vosk.model.path";
    public static final String DB_SERVER = "database.server";
    public static final String DB_PORT = "database.port";
    public static final String DB_NAME = "database.name";
    public static final String DB_USERNAME = "database.username";
    public static final String DB_PASSWORD = "database.password";
    public static final String EMAIL_HOST = "email.smtp.host";
    public static final String EMAIL_PORT = "email.smtp.port";
    public static final String EMAIL_USERNAME = "email.username";
    public static final String EMAIL_PASSWORD = "email.password";
    public static final String EMAIL_FROM = "email.from";
    public static final String EMAIL_ENABLED = "email.enabled";
    public static final String APP_BASE_URL = "app.base.url";
    public static final String LOG_LEVEL = "logging.level";

    private ConfigurationManager() {
        this.properties = new Properties();
        this.dotenv = loadDotenv();
        loadConfiguration();
    }

    public static synchronized ConfigurationManager getInstance() {
        if (instance == null) {
            instance = new ConfigurationManager();
        }
        return instance;
    }

    private Dotenv loadDotenv() {
        try {
            return Dotenv.configure()
                    .directory(".")
                    .ignoreIfMalformed()
                    .ignoreIfMissing()
                    .load();
        } catch (Exception e) {
            logger.warn("Could not load .env file: {}", e.getMessage());
            return null;
        }
    }

    private void loadConfiguration() {
        // Load default properties
        loadDefaultProperties();

        // Load from application.properties
        loadApplicationProperties();

        // Load from email.properties
        loadEmailProperties();

        logger.info("Configuration loaded successfully");
    }

    private void loadDefaultProperties() {
        // Default configuration values
        properties.setProperty(VOSK_MODEL_PATH, "./library/vosk-en-us-0.15");
        properties.setProperty(DB_SERVER, "localhost");
        properties.setProperty(DB_PORT, "1433");
        properties.setProperty(DB_NAME, "VoicifyDB");
        properties.setProperty(DB_USERNAME, "sa");
        properties.setProperty(DB_PASSWORD, "");
        properties.setProperty(EMAIL_HOST, "smtp.gmail.com");
        properties.setProperty(EMAIL_PORT, "587");
        properties.setProperty(EMAIL_USERNAME, "");
        properties.setProperty(EMAIL_PASSWORD, "");
        properties.setProperty(EMAIL_FROM, "noreply@voicify.com");
        properties.setProperty(EMAIL_ENABLED, "false");
        properties.setProperty(APP_BASE_URL, "http://localhost:8080");
        properties.setProperty(LOG_LEVEL, "INFO");
    }

    private void loadApplicationProperties() {
        try (InputStream input = getClass().getClassLoader().getResourceAsStream("application.properties")) {
            if (input != null) {
                properties.load(input);
                logger.info("Loaded configuration from application.properties");
            }
        } catch (IOException e) {
            logger.warn("Could not load application.properties: {}", e.getMessage());
        }
    }

    private void loadEmailProperties() {
        try (InputStream input = getClass().getClassLoader().getResourceAsStream("email.properties")) {
            if (input != null) {
                Properties emailProps = new Properties();
                emailProps.load(input);

                // Map email.properties keys to our configuration keys
                if (emailProps.containsKey("mail.smtp.host")) {
                    properties.setProperty(EMAIL_HOST, emailProps.getProperty("mail.smtp.host"));
                }
                if (emailProps.containsKey("mail.smtp.port")) {
                    properties.setProperty(EMAIL_PORT, emailProps.getProperty("mail.smtp.port"));
                }
                if (emailProps.containsKey("mail.username")) {
                    properties.setProperty(EMAIL_USERNAME, emailProps.getProperty("mail.username"));
                }
                if (emailProps.containsKey("mail.password")) {
                    properties.setProperty(EMAIL_PASSWORD, emailProps.getProperty("mail.password"));
                }
                if (emailProps.containsKey("mail.from")) {
                    properties.setProperty(EMAIL_FROM, emailProps.getProperty("mail.from"));
                }
                if (emailProps.containsKey("mail.enabled")) {
                    properties.setProperty(EMAIL_ENABLED, emailProps.getProperty("mail.enabled"));
                }

                logger.info("Loaded email configuration from email.properties");
            } else {
                logger.warn("email.properties file not found, using default email settings");
            }
        } catch (IOException e) {
            logger.warn("Could not load email.properties: {}", e.getMessage());
        }
    }

    /**
     * Get configuration value with priority:
     * 1. Environment variable
     * 2. .env file
     * 3. Properties file
     * 4. Default value
     */
    public String getProperty(String key) {
        // Check environment variables first
        String envValue = System.getenv(key.toUpperCase().replace(".", "_"));
        if (envValue != null && !envValue.trim().isEmpty()) {
            return envValue;
        }

        // Check .env file
        if (dotenv != null) {
            String dotenvValue = dotenv.get(key.toUpperCase().replace(".", "_"));
            if (dotenvValue != null && !dotenvValue.trim().isEmpty()) {
                return dotenvValue;
            }
        }

        // Check properties file
        return properties.getProperty(key);
    }

    public String getProperty(String key, String defaultValue) {
        String value = getProperty(key);
        return value != null ? value : defaultValue;
    }

    public int getIntProperty(String key, int defaultValue) {
        try {
            String value = getProperty(key);
            return value != null ? Integer.parseInt(value) : defaultValue;
        } catch (NumberFormatException e) {
            logger.warn("Invalid integer value for key {}: {}", key, getProperty(key));
            return defaultValue;
        }
    }

    public boolean getBooleanProperty(String key, boolean defaultValue) {
        String value = getProperty(key);
        return value != null ? Boolean.parseBoolean(value) : defaultValue;
    }

    /**
     * Get absolute path for Vosk model, resolving relative paths
     */
    public String getVoskModelPath() {
        String modelPath = getProperty(VOSK_MODEL_PATH);
        Path path = Paths.get(modelPath);

        if (!path.isAbsolute()) {
            // Convert relative path to absolute
            path = Paths.get(System.getProperty("user.dir")).resolve(path);
        }

        if (!Files.exists(path)) {
            logger.warn("Vosk model path does not exist: {}", path);
        }

        return path.toString();
    }

    /**
     * Validate configuration and log warnings for missing required values
     */
    public void validateConfiguration() {
        logger.info("Validating configuration...");

        // Check Vosk model path
        String voskPath = getVoskModelPath();
        if (!Files.exists(Paths.get(voskPath))) {
            logger.error("Vosk model not found at: {}", voskPath);
        } else {
            logger.info("Vosk model found at: {}", voskPath);
        }

        // Check database configuration
        if (getProperty(DB_PASSWORD).isEmpty()) {
            logger.warn("Database password is empty - this may cause connection issues");
        }

        // Check email configuration if enabled
        if (getBooleanProperty(EMAIL_ENABLED, false)) {
            if (getProperty(EMAIL_USERNAME).isEmpty() || getProperty(EMAIL_PASSWORD).isEmpty()) {
                logger.warn("Email is enabled but credentials are missing");
            }
        }

        logger.info("Configuration validation completed");
    }

    /**
     * Get database connection URL
     */
    public String getDatabaseUrl() {
        return String.format("jdbc:sqlserver://%s:%s;databaseName=%s;encrypt=true;trustServerCertificate=true;integratedSecurity=false",
                getProperty(DB_SERVER),
                getProperty(DB_PORT),
                getProperty(DB_NAME));
    }

    /**
     * Reload configuration (useful for runtime updates)
     */
    public void reload() {
        properties.clear();
        loadConfiguration();
        logger.info("Configuration reloaded");
    }
}
