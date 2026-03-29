package com.voicify.controller;

import com.voicify.model.User;
import com.voicify.model.UserDAO;
import com.voicify.util.EmailService;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import javafx.stage.Stage;

import java.io.IOException;
import java.util.Random;

public class ForgotPasswordController {
    @FXML private TextField emailField;
    @FXML private PasswordField verificationCodeField;
    @FXML private PasswordField newPasswordField;
    @FXML private Button sendCodeButton;
    @FXML private Button resetPasswordButton;
    @FXML private Button backButton;
    @FXML private Label errorLabel;

    private UserDAO userDAO = new UserDAO();
    private String generatedCode;
    private String userEmail;

    @FXML
    private void handleSendCode() {
        userEmail = emailField.getText().trim();
        
        if (userEmail.isEmpty()) {
            showError("Please enter your email address");
            return;
        }
        
        // Check if email exists in the database
        if (!userDAO.isEmailExists(userEmail)) {
            showError("Email not found in our records");
            return;
        }
        
        // Generate a random 6-digit verification code
        generatedCode = generateVerificationCode();
        
        // Send verification code via email
        boolean emailSent = EmailService.sendPasswordResetEmail(userEmail, generatedCode);
        
        if (emailSent) {
            showSuccess("Verification code sent to your email");
        } else {
            showError("Failed to send verification code. Please try again.");
        }
    }

    @FXML
    private void handleResetPassword() {
        String verificationCode = verificationCodeField.getText().trim();
        String newPassword = newPasswordField.getText().trim();
        
        if (verificationCode.isEmpty() || newPassword.isEmpty()) {
            showError("Please enter verification code and new password");
            return;
        }
        
        if (generatedCode == null || !generatedCode.equals(verificationCode)) {
            showError("Invalid verification code");
            return;
        }
        
        if (newPassword.length() < 6) {
            showError("Password must be at least 6 characters long");
            return;
        }
        
        // Update the user's password in the database
        boolean success = userDAO.updatePassword(userEmail, newPassword);
        
        if (success) {
            showSuccess("Password has been reset successfully");
            // Redirect to login page after a short delay
            new Thread(() -> {
                try {
                    Thread.sleep(2000);
                    javafx.application.Platform.runLater(this::loadLoginView);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }).start();
        } else {
            showError("Failed to reset password. Please try again.");
        }
    }

    @FXML
    private void handleBack() {
        loadLoginView();
    }

    private void loadLoginView() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/com/voicify/fxml/login-view.fxml"));
            Parent root = loader.load();
            
            Stage stage = (Stage) backButton.getScene().getWindow();
            Scene scene = new Scene(root);
            stage.setScene(scene);
            stage.setTitle("Voicify - Login");
            stage.show();
        } catch (IOException e) {
            showError("Failed to load login view: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private String generateVerificationCode() {
        Random random = new Random();
        int code = 100000 + random.nextInt(900000); // Generates a random 6-digit number
        return String.valueOf(code);
    }

    private boolean sendVerificationEmail(String email, String code) {
        // In a real application, you would implement email sending logic here
        // For this example, we'll just return true to simulate success
        return true;
    }

    private void showError(String message) {
        errorLabel.setText(message);
        errorLabel.setStyle("-fx-text-fill: #ed1c24;");
        errorLabel.setVisible(true);
    }

    private void showSuccess(String message) {
        errorLabel.setText(message);
        errorLabel.setStyle("-fx-text-fill: #00b300;");
        errorLabel.setVisible(true);
    }
}