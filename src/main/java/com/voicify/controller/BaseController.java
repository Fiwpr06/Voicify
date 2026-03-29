package com.voicify.controller;

import com.voicify.model.TranslationHistory;
import com.voicify.service.*;
import com.voicify.util.ExportService;
import com.voicify.util.ThemeManager;
import javafx.fxml.FXML;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.stage.Stage;

import java.util.List;

public abstract class BaseController {
    protected TranslationService translationService;
    protected SpeechRecognitionService speechRecognitionService;
    protected TextToSpeechService textToSpeechService;
    protected AITranslationService aiTranslationService;
    protected AudioService audioService;
    protected ExportService exportService;
    protected ThemeManager themeManager;

    protected void initializeServices() {
        translationService = new TranslationService();
        speechRecognitionService = new SpeechRecognitionService();
        textToSpeechService = new TextToSpeechService();
        aiTranslationService = new AITranslationService();
        audioService = new AudioService();
        exportService = new ExportService();
        themeManager = ThemeManager.getInstance();
    }

    protected void showErrorAlert(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }

    protected void showWarningAlert(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.WARNING);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }

    protected void showInfoAlert(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }

    protected Stage getCurrentStage() {
        return (Stage) ((Node) getRootNode()).getScene().getWindow();
    }

    protected abstract Object getRootNode();
}