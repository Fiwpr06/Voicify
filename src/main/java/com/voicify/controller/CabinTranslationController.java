package com.voicify.controller;

import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.layout.VBox;

public class CabinTranslationController extends BaseController {
    @FXML private VBox root;
    @FXML private TextArea cabinSourceTextArea;
    @FXML private TextArea cabinTranslationTextArea;
    @FXML private Button cabinMicButton;
    @FXML private Button cabinSpeakButton;
    @FXML private Label cabinStatusLabel;

    private boolean isCabinMode = false;

    @FXML
    private void handleCabinMicButton() {
        if (isCabinMode) {
            stopCabinMode();
        } else {
            startCabinMode();
        }
    }

    @FXML
    private void handleSpeakButton() {
        // Speak implementation
    }

    private void startCabinMode() {
        // Cabin mode implementation
    }

    private void stopCabinMode() {
        // Stop cabin mode implementation
    }

    @Override
    protected Object getRootNode() {
        return root;
    }
}