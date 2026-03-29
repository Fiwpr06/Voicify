package com.voicify.util;

import javafx.application.Platform;
import javafx.scene.control.Alert;
import javafx.scene.control.ButtonType;
import javafx.scene.control.TextArea;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.Optional;

/**
 * Centralized error handling utility for Voicify application
 * Provides consistent error logging and user notification
 */
public class ErrorHandler {
    private static final Logger logger = LoggerFactory.getLogger(ErrorHandler.class);

    /**
     * Handle an exception with user notification
     */
    public static void handleError(String title, String message, Throwable throwable) {
        logger.error("{}: {}", title, message, throwable);
        
        Platform.runLater(() -> {
            Alert alert = new Alert(Alert.AlertType.ERROR);
            alert.setTitle(title);
            alert.setHeaderText(message);
            alert.setContentText(throwable.getMessage());
            
            // Add expandable exception details
            StringWriter sw = new StringWriter();
            PrintWriter pw = new PrintWriter(sw);
            throwable.printStackTrace(pw);
            String exceptionText = sw.toString();
            
            TextArea textArea = new TextArea(exceptionText);
            textArea.setEditable(false);
            textArea.setWrapText(true);
            textArea.setMaxWidth(Double.MAX_VALUE);
            textArea.setMaxHeight(Double.MAX_VALUE);
            
            alert.getDialogPane().setExpandableContent(textArea);
            alert.showAndWait();
        });
    }

    /**
     * Handle an error with simple message
     */
    public static void handleError(String title, String message) {
        logger.error("{}: {}", title, message);
        
        Platform.runLater(() -> {
            Alert alert = new Alert(Alert.AlertType.ERROR);
            alert.setTitle(title);
            alert.setHeaderText(null);
            alert.setContentText(message);
            alert.showAndWait();
        });
    }

    /**
     * Show warning message
     */
    public static void showWarning(String title, String message) {
        logger.warn("{}: {}", title, message);
        
        Platform.runLater(() -> {
            Alert alert = new Alert(Alert.AlertType.WARNING);
            alert.setTitle(title);
            alert.setHeaderText(null);
            alert.setContentText(message);
            alert.showAndWait();
        });
    }

    /**
     * Show information message
     */
    public static void showInfo(String title, String message) {
        logger.info("{}: {}", title, message);
        
        Platform.runLater(() -> {
            Alert alert = new Alert(Alert.AlertType.INFORMATION);
            alert.setTitle(title);
            alert.setHeaderText(null);
            alert.setContentText(message);
            alert.showAndWait();
        });
    }

    /**
     * Show confirmation dialog
     */
    public static boolean showConfirmation(String title, String message) {
        logger.info("Confirmation dialog: {}: {}", title, message);
        
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        
        Optional<ButtonType> result = alert.showAndWait();
        return result.isPresent() && result.get() == ButtonType.OK;
    }

    /**
     * Handle database connection errors specifically
     */
    public static void handleDatabaseError(String operation, Throwable throwable) {
        String message = "Database operation failed: " + operation;
        logger.error(message, throwable);
        
        Platform.runLater(() -> {
            Alert alert = new Alert(Alert.AlertType.ERROR);
            alert.setTitle("Database Error");
            alert.setHeaderText("Database Connection Problem");
            alert.setContentText(
                "Failed to " + operation + ".\n\n" +
                "Please check:\n" +
                "• Database server is running\n" +
                "• Connection settings are correct\n" +
                "• Network connectivity\n\n" +
                "Error: " + throwable.getMessage()
            );
            alert.showAndWait();
        });
    }

    /**
     * Handle file operation errors
     */
    public static void handleFileError(String operation, String filePath, Throwable throwable) {
        String message = "File operation failed: " + operation + " for " + filePath;
        logger.error(message, throwable);
        
        Platform.runLater(() -> {
            Alert alert = new Alert(Alert.AlertType.ERROR);
            alert.setTitle("File Error");
            alert.setHeaderText("File Operation Failed");
            alert.setContentText(
                "Failed to " + operation + " file: " + filePath + "\n\n" +
                "Please check:\n" +
                "• File exists and is accessible\n" +
                "• You have proper permissions\n" +
                "• Disk space is available\n\n" +
                "Error: " + throwable.getMessage()
            );
            alert.showAndWait();
        });
    }

    /**
     * Handle network/API errors
     */
    public static void handleNetworkError(String service, Throwable throwable) {
        String message = "Network error for service: " + service;
        logger.error(message, throwable);
        
        Platform.runLater(() -> {
            Alert alert = new Alert(Alert.AlertType.ERROR);
            alert.setTitle("Network Error");
            alert.setHeaderText("Service Connection Failed");
            alert.setContentText(
                "Failed to connect to " + service + ".\n\n" +
                "Please check:\n" +
                "• Internet connection\n" +
                "• Service availability\n" +
                "• Firewall settings\n\n" +
                "Error: " + throwable.getMessage()
            );
            alert.showAndWait();
        });
    }

    /**
     * Handle configuration errors
     */
    public static void handleConfigurationError(String configItem, String issue) {
        String message = "Configuration error for " + configItem + ": " + issue;
        logger.error(message);
        
        Platform.runLater(() -> {
            Alert alert = new Alert(Alert.AlertType.ERROR);
            alert.setTitle("Configuration Error");
            alert.setHeaderText("Invalid Configuration");
            alert.setContentText(
                "Configuration problem with " + configItem + ":\n\n" +
                issue + "\n\n" +
                "Please check your configuration files:\n" +
                "• application.properties\n" +
                "• .env file\n" +
                "• Environment variables"
            );
            alert.showAndWait();
        });
    }

    /**
     * Log error without showing dialog (for background operations)
     */
    public static void logError(String message, Throwable throwable) {
        logger.error(message, throwable);
    }

    /**
     * Log warning without showing dialog
     */
    public static void logWarning(String message) {
        logger.warn(message);
    }

    /**
     * Log info message
     */
    public static void logInfo(String message) {
        logger.info(message);
    }
}
