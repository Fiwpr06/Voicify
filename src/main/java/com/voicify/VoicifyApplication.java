package com.voicify;

import com.voicify.config.ConfigurationManager;
import com.voicify.model.DatabaseManager;
import com.voicify.util.ThemeManager;
import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.image.Image;
import javafx.stage.Stage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.Arrays;

public class VoicifyApplication extends Application {
    private static final Logger logger = LoggerFactory.getLogger(VoicifyApplication.class);
    private static final String APP_TITLE = "Voicify - English to Vietnamese Translator";
    private ConfigurationManager config;

    @Override
    public void start(Stage primaryStage) {
        logger.info("Starting Voicify application");

        try {
            // Initialize configuration
            config = ConfigurationManager.getInstance();
            config.validateConfiguration();

            // Initialize database
            DatabaseManager.getInstance().initializeDatabase();

            // Load login screen with 16:9 aspect ratio
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/com/voicify/fxml/login-view.fxml"));
            Parent root = loader.load();

            Scene scene = new Scene(root, 1280, 720);

            // Clear any existing stylesheets and add login styling
            scene.getStylesheets().clear();
            scene.getStylesheets().add(getClass().getResource("/com/voicify/css/login-register.css").toExternalForm());

            // Configure stage for 16:9 login
            primaryStage.setTitle("Voicify - Login");
            primaryStage.setScene(scene);
            primaryStage.setResizable(false);

            // Set exact 16:9 dimensions for login
            primaryStage.setWidth(1280);
            primaryStage.setHeight(720);
            primaryStage.setMinWidth(1280);
            primaryStage.setMinHeight(720);
            primaryStage.setMaxWidth(1280);
            primaryStage.setMaxHeight(720);

            // Center the window on screen
            primaryStage.centerOnScreen();
            primaryStage.show();

            logger.info("Voicify application started successfully");

        } catch (IOException e) {
            logger.error("Failed to start Voicify application: {}", e.getMessage(), e);
            showErrorAndExit("Failed to load application: " + e.getMessage());
        } catch (Exception e) {
            logger.error("Unexpected error during application startup: {}", e.getMessage(), e);
            showErrorAndExit("Unexpected error: " + e.getMessage());
        }
    }

    private Scene initializeStage(Stage primaryStage) throws IOException {
        FXMLLoader fxmlLoader = new FXMLLoader(getClass().getResource("/com/voicify/fxml/main-view.fxml"));

        int prefWidth = config.getIntProperty("ui.window.width", 1200);
        int prefHeight = config.getIntProperty("ui.window.height", 800);
        Scene scene = new Scene(fxmlLoader.load(), prefWidth, prefHeight);

        primaryStage.setTitle(APP_TITLE);

        int minWidth = config.getIntProperty("ui.window.min.width", 1000);
        int minHeight = config.getIntProperty("ui.window.min.height", 700);
        primaryStage.setMinWidth(minWidth);
        primaryStage.setMinHeight(minHeight);
        primaryStage.setScene(scene);

        try {
            Image icon = new Image(getClass().getResourceAsStream("/com/voicify/icons/voicify.png"));
            primaryStage.getIcons().add(icon);
        } catch (Exception e) {
            logger.warn("Application icon not found: {}", e.getMessage());
        }

        return scene;
    }

    @Override
    public void stop() throws Exception {
        logger.info("Stopping Voicify application");
        ThemeManager.getInstance().saveThemePreference();
        super.stop();
        logger.info("Voicify application stopped");
    }

    private void showErrorAndExit(String message) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle("Application Error");
        alert.setHeaderText("Failed to start Voicify");
        alert.setContentText(message);
        alert.showAndWait();

        System.exit(1);
    }

    public static void main(String[] args) {
        System.setProperty("prism.lcdtext", "false");
        System.setProperty("prism.text", "t2k");

        System.out.println("Launching Voicify application with args: " + Arrays.toString(args));
        launch(args);
    }
}
