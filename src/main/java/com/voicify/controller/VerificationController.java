package com.voicify.controller;

import com.voicify.model.UserDAO;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.stage.Stage;

import java.io.IOException;

public class VerificationController {
    @FXML private Label messageLabel;
    
    private UserDAO userDAO = new UserDAO();
    
    public void verifyAccount(String token) {
        boolean verified = userDAO.verifyAccount(token);
        
        if (verified) {
            messageLabel.setText("Your account has been successfully verified! You can now log in.");
            messageLabel.setStyle("-fx-text-fill: #00b300;");
        } else {
            messageLabel.setText("Invalid or expired verification token. Please try registering again or contact support.");
            messageLabel.setStyle("-fx-text-fill: #ed1c24;");
        }
    }
    
    @FXML
    private void handleGoToLogin() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/com/voicify/fxml/login-view.fxml"));
            Parent root = loader.load();
            
            Stage stage = (Stage) messageLabel.getScene().getWindow();
            Scene scene = new Scene(root);
            stage.setScene(scene);
            stage.setTitle("Voicify - Login");
            stage.show();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}