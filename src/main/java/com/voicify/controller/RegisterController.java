package com.voicify.controller;

import com.voicify.model.User;
import com.voicify.model.UserDAO;
import com.voicify.util.Dialog;
import com.voicify.util.EmailService;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.input.MouseEvent;
import javafx.stage.Stage;

import java.io.IOException;
import java.util.Random;

public class RegisterController {
    @FXML private TextField emailField;
    @FXML private TextField usernameField;
    @FXML private PasswordField passwordField;
    @FXML private PasswordField confirmPasswordField;
    @FXML private Button registerButton;
    @FXML private Label loginLabel;
    @FXML private Label errorLabel;
    @FXML private Button backButton;

    // Email verification components
    @FXML private TextField verificationCodeField;
    @FXML private Button sendCodeButton;
    @FXML private Button verifyEmailButton;
    @FXML private Label verificationStatusLabel;

    private UserDAO userDAO = new UserDAO();
    private String generatedVerificationCode;
    private boolean emailVerified = false;
    private long codeGenerationTime;

    @FXML
    private void handleRegister(ActionEvent event) {
        String email = emailField.getText().trim();
        String username = usernameField.getText().trim();
        String password = passwordField.getText().trim();
        String confirmPassword = confirmPasswordField.getText().trim();

        // Validate input
        if (email.isEmpty() || username.isEmpty() ||
            password.isEmpty() || confirmPassword.isEmpty()) {
            showError("All fields are required");
            return;
        }

        if (!password.equals(confirmPassword)) {
            showError("Passwords do not match");
            return;
        }

        if (password.length() < 6) {
            showError("Password must be at least 6 characters");
            return;
        }

        // Check email format
        if (!isValidEmail(email)) {
            showError("Please enter a valid email address");
            return;
        }

        // Check if email verification is required and completed
        if (!emailVerified) {
            showError("Please verify your email address first");
            return;
        }

        // Check if username already exists
        if (userDAO.isUsernameExists(username)) {
            showError("Username already exists");
            return;
        }

        // Check if email already exists
        if (userDAO.isEmailExists(email)) {
            showError("Email already exists");
            return;
        }

        // Create new user
        User user = new User();
        user.setUsername(username);
        user.setEmail(email);
        user.setPasswordHash(password); // Will be hashed in DAO
        user.setVerified(true); // Already verified via email

        // Register user
        boolean success = userDAO.registerUser(user);
        if (success) {
            showSuccess("Registration successful! You can now log in.");

            // Redirect to login after a short delay
            new Thread(() -> {
                try {
                    Thread.sleep(2000);
                    javafx.application.Platform.runLater(this::loadLoginView);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }).start();
        } else {
            showError("Failed to register. Please try again.");
        }
    }

    @FXML
    private void handleLoginLink(MouseEvent event) {
        loadLoginView();
    }

    private void loadLoginView() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/com/voicify/fxml/login-view.fxml"));
            Parent root = loader.load();

            Stage stage = (Stage) registerButton.getScene().getWindow();
            Scene scene = new Scene(root, 1280, 720); // 16:9 aspect ratio

            // Clear existing stylesheets and add login styling
            scene.getStylesheets().clear();
            scene.getStylesheets().add(getClass().getResource("/com/voicify/css/login-register.css").toExternalForm());

            stage.setScene(scene);
            stage.setTitle("Voicify - Login");

            // Maintain 16:9 dimensions
            stage.setWidth(1280);
            stage.setHeight(720);
            stage.setResizable(false);
            stage.centerOnScreen();
            stage.show();
        } catch (IOException e) {
            showError("Failed to load login view: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private void showError(String message) {
        errorLabel.setText(message);
        errorLabel.setVisible(true);
        errorLabel.setManaged(true);
    }

    private void showSuccess(String message) {
        errorLabel.setText(message);
        errorLabel.setStyle("-fx-text-fill: #00b300;");
        errorLabel.setVisible(true);
    }

    @FXML
    private void handleSendVerificationCode() {
        String email = emailField.getText().trim();

        if (email.isEmpty()) {
            showError("Please enter your email address first");
            return;
        }

        if (!isValidEmail(email)) {
            showError("Please enter a valid email address");
            return;
        }

        // Check if email already exists
        if (userDAO.isEmailExists(email)) {
            showError("Email already exists");
            return;
        }

        // Generate verification code
        generatedVerificationCode = generateVerificationCode();
        codeGenerationTime = System.currentTimeMillis();

        // Send verification email
        boolean emailSent = EmailService.sendVerificationCode(email, generatedVerificationCode);

        if (emailSent) {
            showVerificationStatus("Verification code sent to " + email, false);
            sendCodeButton.setDisable(true);
            sendCodeButton.setText("Code Sent");

            // Enable code verification after 30 seconds
            new Thread(() -> {
                try {
                    Thread.sleep(30000); // 30 seconds
                    javafx.application.Platform.runLater(() -> {
                        sendCodeButton.setDisable(false);
                        sendCodeButton.setText("Resend Code");
                    });
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }).start();
        } else {
            showError("Failed to send verification code. Please try again.");
        }
    }

    @FXML
    private void handleVerifyEmail() {
        String enteredCode = verificationCodeField.getText().trim();

        if (enteredCode.isEmpty()) {
            showError("Please enter the verification code");
            return;
        }

        if (generatedVerificationCode == null) {
            showError("Please request a verification code first");
            return;
        }

        // Check if code is expired (15 minutes)
        long currentTime = System.currentTimeMillis();
        long timeDifference = currentTime - codeGenerationTime;
        if (timeDifference > 15 * 60 * 1000) { // 15 minutes in milliseconds
            showError("Verification code has expired. Please request a new one.");
            generatedVerificationCode = null;
            return;
        }

        if (enteredCode.equals(generatedVerificationCode)) {
            emailVerified = true;
            showVerificationStatus("Email verified successfully!", false);
            verificationCodeField.setDisable(true);
            verifyEmailButton.setDisable(true);
            sendCodeButton.setDisable(true);
            verifyEmailButton.setText("✓ Verified");
            verifyEmailButton.setStyle("-fx-background-color: #28a745; -fx-text-fill: white;");
        } else {
            showError("Invalid verification code. Please try again.");
        }
    }

    private String generateVerificationCode() {
        Random random = new Random();
        return String.format("%06d", random.nextInt(1000000));
    }

    private boolean isValidEmail(String email) {
        return email.matches("^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$");
    }

    private void showVerificationStatus(String message, boolean isError) {
        if (verificationStatusLabel != null) {
            verificationStatusLabel.setText(message);
            verificationStatusLabel.setStyle(isError ? "-fx-text-fill: #e53e3e;" : "-fx-text-fill: #28a745;");
            verificationStatusLabel.setVisible(true);
            verificationStatusLabel.setManaged(true);
        }
    }

    @FXML
    private void handleBack() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/com/voicify/fxml/login-view.fxml"));
            Parent root = loader.load();

            Stage stage = (Stage) backButton.getScene().getWindow();
            Scene scene = new Scene(root, 1280, 720); // 16:9 aspect ratio

            // Clear existing stylesheets and add login styling
            scene.getStylesheets().clear();
            scene.getStylesheets().add(getClass().getResource("/com/voicify/css/login-register.css").toExternalForm());

            stage.setScene(scene);
            stage.setTitle("Voicify - Login");

            // Maintain 16:9 dimensions
            stage.setWidth(1280);
            stage.setHeight(720);
            stage.setResizable(false);
            stage.centerOnScreen();
            stage.show();
        } catch (IOException e) {
            e.printStackTrace();
            showError("Error loading login view: " + e.getMessage());
        }
    }
}
