package com.voicify.controller;

import com.voicify.model.User;
import com.voicify.model.UserDAO;
import com.voicify.util.SessionManager;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.input.MouseEvent;
import javafx.stage.Stage;

import java.io.IOException;

public class LoginController {
    @FXML private TextField usernameField;
    @FXML private PasswordField passwordField;
    @FXML private Button loginButton;
    @FXML private Label registerLabel;
    @FXML private Label errorLabel;

    private UserDAO userDAO = new UserDAO();

    @FXML
    private void handleLogin(ActionEvent event) {
        String username = usernameField.getText().trim();
        String password = passwordField.getText().trim();

        // Validate input
        if (username.isEmpty() || password.isEmpty()) {
            showError("Username and password are required");
            return;
        }

        try {
            User user = userDAO.authenticateUser(username, password);
            if (user != null) {
                // Login successful
                SessionManager.getInstance().setCurrentUser(user);
                loadMainView();
            } else {
                showError("Invalid username or password");
            }
        } catch (Exception e) {
            showError("Login error: " + e.getMessage());
            e.printStackTrace();
        }
    }

    @FXML
    private void handleRegisterLink(MouseEvent event) {
        loadRegisterView();
    }

    private void loadMainView() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/com/voicify/fxml/main-view.fxml"));
            Parent root = loader.load();

            Stage stage = (Stage) loginButton.getScene().getWindow();
            Scene scene = new Scene(root);
            stage.setScene(scene);
            stage.setTitle("Voicify - Translation App");

            // Reset window properties from login screen
            stage.setResizable(true);
            stage.setMaxWidth(Double.MAX_VALUE);
            stage.setMaxHeight(Double.MAX_VALUE);
            stage.setMinWidth(1000);
            stage.setMinHeight(700);

            // Center and then maximize
            stage.centerOnScreen();
            stage.setMaximized(true);
            stage.show();
        } catch (IOException e) {
            showError("Failed to load main view: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private void loadRegisterView() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/com/voicify/fxml/register-view.fxml"));
            Parent root = loader.load();

            Stage stage = (Stage) loginButton.getScene().getWindow();
            Scene scene = new Scene(root, 1280, 720);

            // Clear existing stylesheets and add register styling
            scene.getStylesheets().clear();
            scene.getStylesheets().add(getClass().getResource("/com/voicify/css/login-register.css").toExternalForm());

            stage.setScene(scene);
            stage.setTitle("Voicify - Register");

            // Maintain 16:9 dimensions
            stage.setWidth(1280);
            stage.setHeight(720);
            stage.setResizable(false);
            stage.centerOnScreen();
            stage.show();
        } catch (IOException e) {
            showError("Failed to load register view: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private void showError(String message) {
        errorLabel.setText(message);
        errorLabel.setVisible(true);
        errorLabel.setManaged(true);
    }
}