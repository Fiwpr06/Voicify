package com.voicify.controller;

import com.voicify.model.User;
import com.voicify.model.UserDAO;
import com.voicify.util.EmailService;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.stage.Stage;

import java.util.Random;

public class VerificationDialogController {
    @FXML private TextField codeField;
    @FXML private Button sendCodeButton;
    @FXML private Button verifyButton;
    @FXML private Button cancelButton;
    @FXML private Label messageLabel;
    
    private UserDAO userDAO = new UserDAO();
    private User user;
    private Stage dialogStage;
    private boolean verified = false;
    private String generatedCode;
    
    public void setUser(User user) {
        this.user = user;
    }
    
    public void setDialogStage(Stage dialogStage) {
        this.dialogStage = dialogStage;
    }
    
    public boolean isVerified() {
        return verified;
    }
    
    @FXML
    private void handleSendCode(ActionEvent event) {
        if (user == null) {
            showMessage("User information not available", true);
            return;
        }
        
        // Generate a random 6-digit verification code
        generatedCode = String.format("%06d", new Random().nextInt(1000000));
        
        // Store the verification code in the database
        boolean stored = userDAO.storeVerificationCode(user.getEmail(), generatedCode);
        
        if (!stored) {
            showMessage("Failed to generate verification code. Please try again.", true);
            return;
        }
        
        // Send the verification code via email
        boolean emailSent = EmailService.sendVerificationCode(user.getEmail(), generatedCode);
        
        if (emailSent) {
            showMessage("Verification code sent to " + user.getEmail(), false);
        } else {
            showMessage("Failed to send email. Your verification code is: " + generatedCode, false);
        }
    }
    
    @FXML
    private void handleVerify(ActionEvent event) {
        String code = codeField.getText().trim();
        
        if (code.isEmpty()) {
            showMessage("Please enter the verification code", true);
            return;
        }
        
        // Verify the code
        boolean success = userDAO.verifyEmailWithCode(user.getEmail(), code);
        
        if (success) {
            showMessage("Email verified successfully!", false);
            verified = true;
            user.setVerified(true);
            
            // Close the dialog after a short delay
            new Thread(() -> {
                try {
                    Thread.sleep(2000);
                    javafx.application.Platform.runLater(() -> dialogStage.close());
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }).start();
        } else {
            showMessage("Invalid verification code. Please try again.", true);
        }
    }
    
    @FXML
    private void handleCancel(ActionEvent event) {
        dialogStage.close();
    }
    
    private void showMessage(String message, boolean isError) {
        messageLabel.setText(message);
        messageLabel.setStyle("-fx-text-fill: " + (isError ? "#ed1c24" : "#00b300") + ";");
        messageLabel.setVisible(true);
    }
}