package com.voicify.controller;

import com.voicify.model.DatabaseManager;
import com.voicify.model.TranslationHistory;
import com.voicify.model.TranslationHistoryDAO;
import com.voicify.model.User;
import com.voicify.service.*;
import com.voicify.util.ExportService;
import com.voicify.util.SessionManager;
import com.voicify.util.ThemeManager;
import javafx.animation.FadeTransition;
import javafx.animation.Timeline;
import javafx.animation.KeyFrame;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.concurrent.Task;
import javafx.util.Duration;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.net.URL;
import java.nio.file.Files;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.ResourceBundle;

public class MainController implements Initializable {
    // FXML Components
    @FXML private StackPane mainContentPane;
    @FXML private VBox regularTranslationContent;
    @FXML private VBox cabinTranslationContent;
    @FXML private VBox aiTranslationContent;
    @FXML private VBox historyContent;
    @FXML private VBox fileProcessingContent;
    @FXML private Label fileProcessingTitle;
    @FXML private TextArea fileInputTextArea;
    @FXML private TextArea fileOutputTextArea;
    @FXML private Button processFileButton;
    @FXML private Button loadFileForProcessingButton;
    @FXML private Button saveOutputButton;
    @FXML private Label fileProcessingStatusLabel;
    @FXML private Button sidebarFileTranslation;
    @FXML private Button sidebarFileSummary;
    @FXML private RadioButton translateRadio;
    @FXML private RadioButton summarizeRadio;
    @FXML private ToggleGroup processingType;

    // Regular Translation Tab
    @FXML private TextArea sourceTextArea;
    @FXML private TextArea translationTextArea;
    @FXML private Button recordButton;
    @FXML private Button loadFileButton;
    @FXML private Button sidebarFileProcessing;
    @FXML private Button translateButton;
    @FXML private Button speakEnglishButton;
    @FXML private Button speakVietnameseButton;
    @FXML private Label statusLabel;
    @FXML private Button regularRefreshButton;

    // AI Translation Tab
    @FXML private TextArea aiSourceTextArea;
    @FXML private TextArea aiCurrentTranslationArea;
    @FXML private TextArea aiImprovedTranslationArea;
    @FXML private Button loadTranslationButton;
    @FXML private Button improveWithAIButton;
    @FXML private Button speakAIButton;
    @FXML private Button saveAITranslationButton;
    @FXML private ChoiceBox<String> aiModelChoice;
    @FXML private Slider aiTemperatureSlider;
    @FXML private Label temperatureLabel;
    @FXML private Label aiStatusLabel;
    @FXML private Button aiRefreshButton;

    // Cabin Translation Tab
    @FXML private TextArea cabinSourceTextArea;
    @FXML private TextArea cabinTranslationTextArea;
    @FXML private Button cabinMicButton;
    @FXML private Button cabinSpeakButton;
    @FXML private Label cabinStatusLabel;
    @FXML private Button cabinRefreshButton;

    // History Tab
    @FXML private TableView<TranslationHistory> historyTableView;
    @FXML private TableColumn<TranslationHistory, Integer> idColumn;
    @FXML private TableColumn<TranslationHistory, String> sourceColumn;
    @FXML private TableColumn<TranslationHistory, String> translationColumn;
    @FXML private TableColumn<TranslationHistory, String> typeColumn;
    @FXML private TableColumn<TranslationHistory, LocalDateTime> dateColumn;
    @FXML private TextField searchField;
    @FXML private Button searchButton;
    @FXML private Button exportButton;
    @FXML private Button deleteButton;
    @FXML private Button refreshButton;

    // Sidebar buttons
    @FXML private Button sidebarRegularTab;
    @FXML private Button sidebarCabinTab;
    @FXML private Button sidebarAITab;
    @FXML private Button sidebarHistoryTab;
    @FXML private Button sidebarExportHistory;
    @FXML private Button sidebarDarkMode;
    @FXML private Button sidebarAbout;
    @FXML private Button sidebarExit;
    @FXML private Button sidebarRefreshAll;
    @FXML private Button sidebarLogout;

    // Services
    private TranslationService translationService;
    private SpeechRecognitionService speechRecognitionService;
    private TextToSpeechService textToSpeechService;
    private AITranslationService aiTranslationService;
    private AudioService audioService;
    private ExportService exportService;
    private ThemeManager themeManager;
    private FileProcessingService fileProcessingService;

    // Data
    private TranslationHistoryDAO historyDAO;
    private ObservableList<TranslationHistory> historyData;
    private boolean isRecording = false;
    private boolean isCabinMode = false;
    private boolean isTranslateMode = true; // true for translate, false for summarize
    private File currentFile;

    // Current view tracking
    private enum CurrentView {
        REGULAR_TRANSLATION,
        CABIN_TRANSLATION,
        AI_TRANSLATION,
        HISTORY,
        FILE_PROCESSING
    }

    private CurrentView currentView = CurrentView.REGULAR_TRANSLATION;

    // Timeline for updating button states
    private Timeline buttonUpdateTimeline;

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        System.out.println("Initializing MainController");

        initializeServices();
        initializeUI();
        initializeDatabase();
        loadTranslationHistory();

        // Hiển thị thông tin người dùng đã đăng nhập
        User currentUser = SessionManager.getInstance().getCurrentUser();
        if (currentUser != null) {
            System.out.println("Logged in as: " + currentUser.getUsername());
        }

        sidebarRegularTab.setOnAction(e -> {
            handleSidebarRegularTab();
        });

        sidebarCabinTab.setOnAction(e -> {
            handleSidebarCabinTab();
        });

        sidebarAITab.setOnAction(e -> {
            handleSidebarAITab();
        });

        sidebarHistoryTab.setOnAction(e -> {
            handleSidebarHistoryTab();
        });

        handleSidebarRegularTab();

        switchToView(CurrentView.REGULAR_TRANSLATION);

        // Initialize button state update timeline
        initializeButtonUpdateTimeline();

        System.out.println("MainController initialization completed");
    }

    // Initialize timeline for updating button states
    private void initializeButtonUpdateTimeline() {
        buttonUpdateTimeline = new Timeline(new KeyFrame(Duration.millis(500), e -> {
            updateSpeakButtonStates();
        }));
        buttonUpdateTimeline.setCycleCount(Timeline.INDEFINITE);
        buttonUpdateTimeline.play();
    }

    // Initialize all services
    private void initializeServices() {
        translationService = new TranslationService();
        speechRecognitionService = new SpeechRecognitionService();
        textToSpeechService = new TextToSpeechService();
        aiTranslationService = new AITranslationService();
        audioService = new AudioService();
        exportService = new ExportService();
        themeManager = ThemeManager.getInstance();
        historyDAO = new TranslationHistoryDAO();
        fileProcessingService = new FileProcessingService();

        System.out.println("All services initialized");
    }

    // Initialize user interface
    private void initializeUI() {
        // Initialize history table
        if (historyTableView != null) {
            setupHistoryTable();
        }

        // Initialize status labels
        if (statusLabel != null) {
            statusLabel.setText("Ready");
        }
        if (cabinStatusLabel != null) {
            cabinStatusLabel.setText("Ready");
        }
        if (aiStatusLabel != null) {
            aiStatusLabel.setText("Ready for AI translation improvement");
        }

        // Initialize AI components
        initializeAIComponents();

        // Initialize button states
        updateButtonStates();

        // Register scene with theme manager
        Platform.runLater(() -> {
            if (mainContentPane != null && mainContentPane.getScene() != null) {
                themeManager.registerScene(mainContentPane.getScene());
            }
        });
    }

    // Configure columns for history table
    private void setupHistoryTable() {
        historyData = FXCollections.observableArrayList();
        historyTableView.setItems(historyData);

        idColumn.setCellValueFactory(new PropertyValueFactory<>("id"));
        sourceColumn.setCellValueFactory(new PropertyValueFactory<>("sourceText"));
        translationColumn.setCellValueFactory(new PropertyValueFactory<>("translatedText"));
        typeColumn.setCellValueFactory(new PropertyValueFactory<>("translationType"));
        dateColumn.setCellValueFactory(new PropertyValueFactory<>("createdAt"));

        // Set column widths
        idColumn.setPrefWidth(50);
        sourceColumn.setPrefWidth(200);
        translationColumn.setPrefWidth(200);
        typeColumn.setPrefWidth(80);
        dateColumn.setPrefWidth(150);

        System.out.println("History table setup completed");
    }

    // Initialize AI components
    private void initializeAIComponents() {
        if (aiModelChoice != null) {
            aiModelChoice.getItems().addAll("Gemini Pro", "Gemini Pro Vision", "GPT-3.5 Turbo", "GPT-4");
            aiModelChoice.setValue("Gemini Pro");
        }

        if (aiTemperatureSlider != null && temperatureLabel != null) {
            aiTemperatureSlider.valueProperty().addListener((observable, oldValue, newValue) ->
                    temperatureLabel.setText(String.format("%.1f", newValue.doubleValue())));
        }
    }

    // Initialize database
    private void initializeDatabase() {
        try {
            DatabaseManager.getInstance().initializeDatabase();
            System.out.println("Database initialized successfully");
        } catch (Exception e) {
            System.out.println("Failed to initialize database: " + e.getMessage());
            showErrorAlert("Database Error", "Failed to initialize database: " + e.getMessage());
        }
    }

    // Load translation history from database
    private void loadTranslationHistory() {
        try {
            List<TranslationHistory> histories = historyDAO.findAll();
            historyData.clear();
            historyData.addAll(histories);
            System.out.println("Loaded " + histories.size() + " translation history records");
        } catch (Exception e) {
            System.out.println("Failed to load translation history: " + e.getMessage());
            showErrorAlert("Database Error", "Failed to load translation history: " + e.getMessage());
        }
    }

    // Update button states based on application state
    private void updateButtonStates() {
        if (recordButton != null) {
            recordButton.setText(isRecording ? "Stop Recording" : "Start Recording");
        }
        if (cabinMicButton != null) {
            cabinMicButton.setText(isCabinMode ? "Stop Cabin" : "Start Cabin");
        }
        updateDarkModeButtonText();
        updateSpeakButtonStates();
    }

    // Update speak button states based on TTS service state
    private void updateSpeakButtonStates() {
        if (speakEnglishButton != null) {
            speakEnglishButton.setText(textToSpeechService.isEnglishSpeaking() ? "Stop EN" : "Speak EN");
        }
        if (speakVietnameseButton != null) {
            speakVietnameseButton.setText(textToSpeechService.isVietnameseSpeaking() ? "Stop VN" : "Speak VN");
        }
        if (cabinSpeakButton != null) {
            cabinSpeakButton.setText(textToSpeechService.isSpeaking() ? "Stop" : "Speak");
        }
        if (speakAIButton != null) {
            speakAIButton.setText(textToSpeechService.isSpeaking() ? "Stop" : "Speak");
        }
    }

    // Update dark mode button text
    private void updateDarkModeButtonText() {
        if (sidebarDarkMode != null) {
            String text = themeManager.isDarkTheme() ? "Light Mode" : "Dark Mode";
            String iconPath = themeManager.isDarkTheme()
                    ? "/com/voicify/icons/light.png"
                    : "/com/voicify/icons/dark.png";

            Image icon = new Image(getClass().getResourceAsStream(iconPath));
            ImageView iconView = new ImageView(icon);
            iconView.setFitWidth(24);
            iconView.setFitHeight(24);

            sidebarDarkMode.setGraphic(iconView);
            sidebarDarkMode.setText(text);
            sidebarDarkMode.setContentDisplay(ContentDisplay.LEFT);
        }
    }


    // Switch to the specified view with fade transition
    private void switchToView(CurrentView view) {
        // Fade out all content panes
        for (Node node : mainContentPane.getChildren()) {
            if (node instanceof VBox && node != getContentForView(view)) {
                FadeTransition fadeOut = new FadeTransition(javafx.util.Duration.millis(200), node);
                fadeOut.setToValue(0.0);
                fadeOut.setOnFinished(e -> {
                    node.setVisible(false);
                    node.setManaged(false);
                });
                fadeOut.play();
            }
        }

        // Fade in the new content
        VBox newContent = getContentForView(view);
        if (newContent != null) {
            FadeTransition fadeIn = new FadeTransition(javafx.util.Duration.millis(200), newContent);
            newContent.setVisible(true);
            newContent.setManaged(true);
            fadeIn.setToValue(1.0);
            fadeIn.play();
        }

        currentView = view;
    }

    // Get the content VBox for a given view
    private VBox getContentForView(CurrentView view) {
        switch (view) {
            case REGULAR_TRANSLATION:
                return regularTranslationContent;
            case CABIN_TRANSLATION:
                return cabinTranslationContent;
            case AI_TRANSLATION:
                return aiTranslationContent;
            case HISTORY:
                return historyContent;
            case FILE_PROCESSING:
                return fileProcessingContent;
            default:
                return null;
        }
    }

    // Handle Record button event
    @FXML
    private void handleRecordButton() {
        if (isRecording) {
            stopRecording();
        } else {
            startRecording();
        }
    }

    // Handle Load File button event
    @FXML
    private void handleLoadFileButton() {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Select File");
        fileChooser.getExtensionFilters().addAll(
                new FileChooser.ExtensionFilter("All Supported Files",
                    "*.wav", "*.mp3", "*.flac", "*.m4a", // Audio files
                    "*.txt", "*.pdf", "*.docx", "*.xlsx", "*.xls", "*.csv", "*.json", "*.xml"), // Text files
                new FileChooser.ExtensionFilter("Audio Files", "*.wav", "*.mp3", "*.flac", "*.m4a"),
                new FileChooser.ExtensionFilter("Text Files", "*.txt"),
                new FileChooser.ExtensionFilter("PDF Files", "*.pdf"),
                new FileChooser.ExtensionFilter("Word Documents", "*.docx"),
                new FileChooser.ExtensionFilter("Excel Spreadsheets", "*.xlsx", "*.xls"),
                new FileChooser.ExtensionFilter("CSV Files", "*.csv"),
                new FileChooser.ExtensionFilter("JSON Files", "*.json"),
                new FileChooser.ExtensionFilter("XML Files", "*.xml"),
                new FileChooser.ExtensionFilter("All Files", "*.*")
        );

        Stage stage = (Stage) loadFileButton.getScene().getWindow();
        File selectedFile = fileChooser.showOpenDialog(stage);

        if (selectedFile != null) {
            processSelectedFile(selectedFile);
        }
    }

    // Handle Translate button event
    @FXML
    private void handleTranslateButton() {
        String sourceText = sourceTextArea != null ? sourceTextArea.getText().trim() : "";
        if (sourceText.isEmpty()) {
            showWarningAlert("Input Required", "Please enter text to translate or record audio.");
            return;
        }

        translateText(sourceText, false);
    }

    // Handle Speak English button event (Toggle functionality)
    @FXML
    private void handleSpeakEnglishButton() {
        // Check if currently speaking English
        if (textToSpeechService.isEnglishSpeaking()) {
            // Stop speaking
            textToSpeechService.stopSpeaking();
            if (statusLabel != null) statusLabel.setText("English speech stopped");
            updateSpeakButtonStates();
            return;
        }

        // Start speaking - get SOURCE text (English)
        String sourceText = sourceTextArea != null ? sourceTextArea.getText().trim() : "";
        if (sourceText.isEmpty()) {
            showWarningAlert("No Source Text", "Please enter text first.");
            return;
        }

        // Optional: Check if text contains Vietnamese characters (more reliable check)
        if (sourceText.matches(".*[àáạảãâầấậẩẫăằắặẳẵèéẹẻẽêềếệểễìíịỉĩòóọỏõôồốộổỗơờớợởỡùúụủũưừứựửữỳýỵỷỹđ].*")) {
            Alert confirmDialog = new Alert(Alert.AlertType.CONFIRMATION);
            confirmDialog.setTitle("Language Confirmation");
            confirmDialog.setHeaderText("Vietnamese characters detected");
            confirmDialog.setContentText("The text contains Vietnamese characters. Do you want to speak it in English anyway?");

            Optional<ButtonType> result = confirmDialog.showAndWait();
            if (result.isEmpty() || result.get() != ButtonType.OK) {
                return;
            }
        }

        textToSpeechService.toggleEnglishSpeech(sourceText);
        if (statusLabel != null) statusLabel.setText("Speaking text in English...");
        updateSpeakButtonStates();
    }

    // Handle Speak Vietnamese button event (Toggle functionality)
    @FXML
    private void handleSpeakVietnameseButton() {
        // Check if currently speaking Vietnamese
        if (textToSpeechService.isVietnameseSpeaking()) {
            // Stop speaking
            textToSpeechService.stopSpeaking();
            if (statusLabel != null) statusLabel.setText("Vietnamese speech stopped");
            updateSpeakButtonStates();
            return;
        }

        // Start speaking - get TRANSLATION text (Vietnamese)
        String translationText = translationTextArea != null ? translationTextArea.getText().trim() : "";
        if (translationText.isEmpty()) {
            showWarningAlert("No Translation", "Please translate text first.");
            return;
        }

        textToSpeechService.toggleVietnameseSpeech(translationText);
        if (statusLabel != null) statusLabel.setText("Speaking in Vietnamese...");
        updateSpeakButtonStates();
    }

    // Handle Cabin Speak button event (Toggle functionality)
    @FXML
    private void handleCabinSpeakButton() {
        // Check if currently speaking
        if (textToSpeechService.isSpeaking()) {
            // Stop speaking
            textToSpeechService.stopSpeaking();
            if (cabinStatusLabel != null) cabinStatusLabel.setText("Cabin speech stopped");
            updateSpeakButtonStates();
            return;
        }

        // Start speaking - get cabin translation text
        String cabinTranslation = cabinTranslationTextArea != null ? cabinTranslationTextArea.getText().trim() : "";
        if (cabinTranslation.isEmpty()) {
            showWarningAlert("No Translation", "Please translate text first.");
            return;
        }

        textToSpeechService.speak(cabinTranslation, "vi");
        if (cabinStatusLabel != null) cabinStatusLabel.setText("Speaking cabin translation...");
        updateSpeakButtonStates();
    }

    // Handle Improve with AI button event
    @FXML
    private void handleImproveWithAIButton() {
        if (currentView == CurrentView.AI_TRANSLATION) {
            String sourceText = aiSourceTextArea != null ? aiSourceTextArea.getText().trim() : "";
            String currentTranslation = aiCurrentTranslationArea != null ? aiCurrentTranslationArea.getText().trim() : "";

            if (sourceText.isEmpty() || currentTranslation.isEmpty()) {
                showWarningAlert("Translation Required", "Please enter both source text and current translation.");
                return;
            }

            improveTranslationWithAI(sourceText, currentTranslation);
        } else {
            String sourceText = sourceTextArea != null ? sourceTextArea.getText().trim() : "";
            String currentTranslation = translationTextArea != null ? translationTextArea.getText().trim() : "";

            if (sourceText.isEmpty() || currentTranslation.isEmpty()) {
                showWarningAlert("Translation Required", "Please translate text first before AI improvement.");
                return;
            }

            improveTranslationWithAI(sourceText, currentTranslation);
        }
    }

    // Handle Cabin Mic button event
    @FXML
    private void handleCabinMicButton() {
        if (isCabinMode) {
            stopCabinMode();
        } else {
            startCabinMode();
        }
    }

    // Handle Search button event
    @FXML
    private void handleSearchButton() {
        String keyword = searchField != null ? searchField.getText().trim() : "";
        if (keyword.isEmpty()) {
            loadTranslationHistory();
        } else {
            searchTranslationHistory(keyword);
        }
    }

    // Handle Export button event
    @FXML
    private void handleExportButton() {
        exportTranslationHistory();
    }

    // Handle Delete button event
    @FXML
    private void handleDeleteButton() {
        TranslationHistory selected = historyTableView != null ? historyTableView.getSelectionModel().getSelectedItem() : null;
        if (selected != null) {
            deleteTranslationHistory(selected);
        } else {
            showWarningAlert("No Selection", "Please select a translation record to delete.");
        }
    }

    // Handle Refresh button event
    @FXML
    private void handleRefreshButton() {
        loadTranslationHistory();
    }

    // Handle Dark Mode button event
    @FXML
    private void handleDarkModeMenuItem() {
        themeManager.toggleTheme();
        updateDarkModeButtonText();
    }

    // Handle About button event
    @FXML
    private void handleAboutMenuItem() {
        showInfoAlert("About Voicify",
                "Voicify v1.0\n\n" +
                        "A JavaFX application for English to Vietnamese translation\n" +
                        "with voice input/output and AI enhancement.\n\n" +
                        "Developed with JavaFX, Vosk API, and Gemini AI.");
    }

    // Handle sidebar tab events
    @FXML
    private void handleSidebarRegularTab() {
        switchToView(CurrentView.REGULAR_TRANSLATION);
    }

    @FXML
    private void handleSidebarCabinTab() {
        switchToView(CurrentView.CABIN_TRANSLATION);
    }

    @FXML
    private void handleSidebarAITab() {
        switchToView(CurrentView.AI_TRANSLATION);
    }

    @FXML
    private void handleSidebarHistoryTab() {
        switchToView(CurrentView.HISTORY);
    }

    // Handle AI Translation Tab events
    @FXML
    private void handleLoadTranslationButton() {
        String sourceText = sourceTextArea != null ? sourceTextArea.getText().trim() : "";
        String translation = translationTextArea != null ? translationTextArea.getText().trim() : "";

        if (!sourceText.isEmpty() && !translation.isEmpty()) {
            if (aiSourceTextArea != null) aiSourceTextArea.setText(sourceText);
            if (aiCurrentTranslationArea != null) aiCurrentTranslationArea.setText(translation);
            if (aiStatusLabel != null) aiStatusLabel.setText("Translation loaded. Ready for AI improvement.");
        } else {
            showWarningAlert("No Translation", "Please create a translation in the Regular Translation tab first.");
        }
    }

    // Handle AI Speak button event (Toggle functionality)
    @FXML
    private void handleSpeakAIButton() {
        // Check if currently speaking
        if (textToSpeechService.isSpeaking()) {
            // Stop speaking
            textToSpeechService.stopSpeaking();
            if (aiStatusLabel != null) aiStatusLabel.setText("AI speech stopped");
            updateSpeakButtonStates();
            return;
        }

        // Start speaking - get AI improved translation text
        String aiTranslation = aiImprovedTranslationArea != null ? aiImprovedTranslationArea.getText().trim() : "";
        if (aiTranslation.isEmpty()) {
            showWarningAlert("No AI Translation", "Please improve translation with AI first.");
            return;
        }

        textToSpeechService.speak(aiTranslation, "vi");
        if (aiStatusLabel != null) aiStatusLabel.setText("Speaking AI improved translation...");
        updateSpeakButtonStates();
    }

    @FXML
    private void handleSaveAITranslationButton() {
        String sourceText = aiSourceTextArea != null ? aiSourceTextArea.getText().trim() : "";
        String aiTranslation = aiImprovedTranslationArea != null ? aiImprovedTranslationArea.getText().trim() : "";

        if (sourceText.isEmpty() || aiTranslation.isEmpty()) {
            showWarningAlert("Incomplete Data", "Please ensure both source text and AI translation are available.");
            return;
        }

        saveTranslationToHistory(sourceText, aiTranslation, null, "ai");
        if (aiStatusLabel != null) aiStatusLabel.setText("AI translation saved to history.");
    }

    @FXML
    private void handleExit() {
        Platform.exit();
    }

    // Start recording
    private void startRecording() {
        System.out.println("Starting audio recording");
        isRecording = true;
        updateButtonStates();
        if (statusLabel != null) statusLabel.setText("Recording...");

        audioService.startRecording(new AudioService.AudioRecordingCallback() {
            @Override
            public void onRecordingStarted() {
                Platform.runLater(() -> {
                    if (statusLabel != null) statusLabel.setText("Recording... (Click Stop to finish)");
                });
            }

            @Override
            public void onAudioData(byte[] data, int length) {
                // Process real-time audio data if needed
            }

            @Override
            public void onRecordingCompleted(byte[] audioData) {
                Platform.runLater(() -> {
                    if (statusLabel != null) statusLabel.setText("Processing audio...");
                    processRecordedAudio(audioData);
                });
            }

            @Override
            public void onError(String error) {
                Platform.runLater(() -> {
                    if (statusLabel != null) statusLabel.setText("Recording failed");
                    showErrorAlert("Recording Error", error);
                    isRecording = false;
                    updateButtonStates();
                });
            }
        });
    }

    // Stop recording
    private void stopRecording() {
        System.out.println("Stopping audio recording");
        audioService.stopRecording();
        isRecording = false;
        updateButtonStates();
        if (statusLabel != null) statusLabel.setText("Processing...");
    }

    // Process recorded audio
    private void processRecordedAudio(byte[] audioData) {
        try {
            File tempAudioFile = File.createTempFile("voicify_recording_", ".wav");
            audioService.saveAudioToFile(audioData, tempAudioFile);

            String recognizedText = speechRecognitionService.recognizeFromFile(tempAudioFile);

            Platform.runLater(() -> {
                if (sourceTextArea != null) sourceTextArea.setText(recognizedText);
                if (statusLabel != null) {
                    statusLabel.setText("Speech recognized. Click Translate to continue.");
                    updateStatusLabelStyle(statusLabel, "completed");
                }

                if (!recognizedText.trim().isEmpty()) {
                    translateText(recognizedText, true, tempAudioFile.getAbsolutePath());
                }
            });

            // Delete temp file after processing
            tempAudioFile.deleteOnExit();

        } catch (Exception e) {
            System.out.println("Failed to process recorded audio: " + e.getMessage());
            Platform.runLater(() -> {
                if (statusLabel != null) statusLabel.setText("Audio processing failed");
                showErrorAlert("Processing Error", "Failed to process recorded audio: " + e.getMessage());
            });
        }
    }

    // Process selected file based on its type
    private void processSelectedFile(File selectedFile) {
        String fileName = selectedFile.getName().toLowerCase();
        System.out.println("MainController.processSelectedFile called for: " + selectedFile.getName());

        if (statusLabel != null) statusLabel.setText("Processing file: " + selectedFile.getName());

        // Check file type and process accordingly
        if (fileName.endsWith(".wav") || fileName.endsWith(".mp3") ||
            fileName.endsWith(".flac") || fileName.endsWith(".m4a")) {
            // Process as audio file for speech recognition
            System.out.println("Processing as AUDIO file: " + fileName);
            processAudioFile(selectedFile);
        } else if (fileName.endsWith(".txt") || fileName.endsWith(".csv") ||
                   fileName.endsWith(".json") || fileName.endsWith(".xml")) {
            // Process as text file
            System.out.println("Processing as TEXT file: " + fileName);
            processTextFile(selectedFile);
        } else if (fileName.endsWith(".pdf")) {
            // Process as PDF file
            System.out.println("Processing as PDF file: " + fileName);
            processPdfFile(selectedFile);
        } else if (fileName.endsWith(".docx")) {
            // Process as Word document
            System.out.println("Processing as DOCX file: " + fileName);
            processDocxFile(selectedFile);
        } else if (fileName.endsWith(".xlsx") || fileName.endsWith(".xls")) {
            // Process as Excel file
            System.out.println("Processing as EXCEL file: " + fileName);
            processExcelFile(selectedFile);
        } else {
            System.out.println("UNSUPPORTED file format: " + fileName);
            if (statusLabel != null) statusLabel.setText("Unsupported file format: " + fileName);
            showWarningAlert("Unsupported Format",
                "Supported formats: Audio (wav, mp3, flac, m4a), Text (txt, csv, json, xml), PDF, Word (docx), Excel (xlsx, xls)");
        }
    }

    // Process audio file
    private void processAudioFile(File audioFile) {
        System.out.println("Processing audio file: " + audioFile.getName());
        if (statusLabel != null) statusLabel.setText("Processing audio file...");

        try {
            String recognizedText = speechRecognitionService.recognizeFromFile(audioFile);

            Platform.runLater(() -> {
                if (sourceTextArea != null) sourceTextArea.setText(recognizedText);
                if (statusLabel != null) statusLabel.setText("Speech recognized from file. Click Translate to continue.");

                if (!recognizedText.trim().isEmpty()) {
                    translateText(recognizedText, true, audioFile.getAbsolutePath());
                }
            });

        } catch (Exception e) {
            System.out.println("Failed to process audio file: " + e.getMessage());
            Platform.runLater(() -> {
                if (statusLabel != null) statusLabel.setText("File processing failed");
                showErrorAlert("Processing Error", "Failed to process audio file: " + e.getMessage());
            });
        }
    }

    // Process text file
    private void processTextFile(File textFile) {
        System.out.println("MainController.processTextFile called for: " + textFile.getName());
        if (statusLabel != null) statusLabel.setText("Reading text file...");

        try {
            // Read text file content
            StringBuilder content = new StringBuilder();
            try (java.io.BufferedReader reader = new java.io.BufferedReader(
                    new java.io.FileReader(textFile, java.nio.charset.StandardCharsets.UTF_8))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    content.append(line).append("\n");
                }
            }

            String text = content.toString().trim();
            Platform.runLater(() -> {
                if (sourceTextArea != null) sourceTextArea.setText(text);
                if (statusLabel != null) statusLabel.setText("Text file loaded successfully. Click Translate to continue.");

                if (!text.isEmpty()) {
                    // Auto-translate if text is not empty
                    translateText(text, true, textFile.getAbsolutePath());
                }
            });

        } catch (Exception e) {
            System.out.println("Failed to process text file: " + e.getMessage());
            Platform.runLater(() -> {
                if (statusLabel != null) statusLabel.setText("Text file processing failed");
                showErrorAlert("Processing Error", "Failed to process text file: " + e.getMessage());
            });
        }
    }

    // Process PDF file
    private void processPdfFile(File pdfFile) {
        System.out.println("MainController.processPdfFile called for: " + pdfFile.getName());
        if (statusLabel != null) statusLabel.setText("Processing PDF file...");

        Task<String> task = new Task<String>() {
            @Override
            protected String call() throws Exception {
                return fileProcessingService.extractTextFromFile(pdfFile);
            }
        };

        task.setOnSucceeded(e -> {
            String extractedText = task.getValue();
            Platform.runLater(() -> {
                if (sourceTextArea != null) sourceTextArea.setText(extractedText);
                if (statusLabel != null) statusLabel.setText("PDF file processed successfully. Text extracted and ready for translation.");

                // Auto-translate if text is not empty
                if (!extractedText.trim().isEmpty()) {
                    translateText(extractedText, true, pdfFile.getAbsolutePath());
                }
            });
        });

        task.setOnFailed(e -> {
            Throwable exception = task.getException();
            Platform.runLater(() -> {
                if (statusLabel != null) statusLabel.setText("Failed to process PDF file: " + exception.getMessage());
                showErrorAlert("PDF Processing Error", "Failed to process PDF file: " + exception.getMessage());
            });
        });

        new Thread(task).start();
    }

    // Process Word document
    private void processDocxFile(File docxFile) {
        System.out.println("MainController.processDocxFile called for: " + docxFile.getName());
        if (statusLabel != null) statusLabel.setText("Processing Word document...");

        Task<String> task = new Task<String>() {
            @Override
            protected String call() throws Exception {
                return fileProcessingService.extractTextFromFile(docxFile);
            }
        };

        task.setOnSucceeded(e -> {
            String extractedText = task.getValue();
            Platform.runLater(() -> {
                if (sourceTextArea != null) sourceTextArea.setText(extractedText);
                if (statusLabel != null) statusLabel.setText("Word document processed successfully. Text extracted and ready for translation.");

                // Auto-translate if text is not empty
                if (!extractedText.trim().isEmpty()) {
                    translateText(extractedText, true, docxFile.getAbsolutePath());
                }
            });
        });

        task.setOnFailed(e -> {
            Throwable exception = task.getException();
            Platform.runLater(() -> {
                if (statusLabel != null) statusLabel.setText("Failed to process Word document: " + exception.getMessage());
                showErrorAlert("Word Processing Error", "Failed to process Word document: " + exception.getMessage());
            });
        });

        new Thread(task).start();
    }

    // Process Excel file
    private void processExcelFile(File excelFile) {
        System.out.println("MainController.processExcelFile called for: " + excelFile.getName());
        if (statusLabel != null) statusLabel.setText("Processing Excel file...");

        Task<String> task = new Task<String>() {
            @Override
            protected String call() throws Exception {
                return fileProcessingService.extractTextFromFile(excelFile);
            }
        };

        task.setOnSucceeded(e -> {
            String extractedText = task.getValue();
            Platform.runLater(() -> {
                if (sourceTextArea != null) sourceTextArea.setText(extractedText);
                if (statusLabel != null) statusLabel.setText("Excel file processed successfully. Text extracted and ready for translation.");

                // Auto-translate if text is not empty
                if (!extractedText.trim().isEmpty()) {
                    translateText(extractedText, true, excelFile.getAbsolutePath());
                }
            });
        });

        task.setOnFailed(e -> {
            Throwable exception = task.getException();
            Platform.runLater(() -> {
                if (statusLabel != null) statusLabel.setText("Failed to process Excel file: " + exception.getMessage());
                showErrorAlert("Excel Processing Error", "Failed to process Excel file: " + exception.getMessage());
            });
        });

        new Thread(task).start();
    }

    // Translate text
    private void translateText(String text, boolean saveToHistory) {
        translateText(text, saveToHistory, null);
    }

    private void translateText(String text, boolean saveToHistory, String audioFilePath) {
        System.out.println("Translating text: " + text.substring(0, Math.min(text.length(), 50)));
        if (statusLabel != null) {
            statusLabel.setText("Translating...");
            updateStatusLabelStyle(statusLabel, "processing");
        }

        try {
            String translation = translationService.translateToVietnamese(text);

            Platform.runLater(() -> {
                if (translationTextArea != null) translationTextArea.setText(translation);
                if (statusLabel != null) {
                    statusLabel.setText("Translation completed successfully");
                    updateStatusLabelStyle(statusLabel, "completed");
                }

                if (saveToHistory && audioFilePath != null) {
                    saveTranslationToHistory(text, translation, audioFilePath);
                }
            });

        } catch (Exception e) {
            System.out.println("Translation failed: " + e.getMessage());
            Platform.runLater(() -> {
                if (statusLabel != null) {
                    statusLabel.setText("Translation failed - " + e.getMessage());
                    updateStatusLabelStyle(statusLabel, "error");
                }
                showErrorAlert("Translation Error", "Failed to translate text: " + e.getMessage());
            });
        }
    }

    // Improve translation with AI
    private void improveTranslationWithAI(String sourceText, String currentTranslation) {
        if (currentView == CurrentView.AI_TRANSLATION) {
            if (aiStatusLabel != null) aiStatusLabel.setText("Improving with AI...");

            try {
                String improvedTranslation = aiTranslationService.improveTranslation(sourceText, currentTranslation);

                Platform.runLater(() -> {
                    if (aiImprovedTranslationArea != null) aiImprovedTranslationArea.setText(improvedTranslation);
                    if (aiStatusLabel != null) aiStatusLabel.setText("AI improvement completed");
                });

            } catch (Exception e) {
                Platform.runLater(() -> {
                    if (aiStatusLabel != null) aiStatusLabel.setText("AI improvement failed");
                    showErrorAlert("AI Error", "Failed to improve translation: " + e.getMessage());
                });
            }
        } else {
            showInfoAlert("Use AI Translation View",
                    "For AI translation improvements, please use the dedicated AI Translation view.\n\n" +
                            "Click '🤖 AI Translation' in the sidebar to access AI features.");
        }
    }

    // Start cabin mode
    private void startCabinMode() {
        System.out.println("Starting cabin mode");
        isCabinMode = true;
        updateButtonStates();
        if (cabinStatusLabel != null) cabinStatusLabel.setText("Cabin mode active - Listening...");

        speechRecognitionService.startRealTimeRecognition(new SpeechRecognitionService.SpeechRecognitionCallback() {
            @Override
            public void onRecognitionStarted() {
                Platform.runLater(() -> {
                    if (cabinStatusLabel != null) cabinStatusLabel.setText("Listening for speech...");
                });
            }

            @Override
            public void onPartialResult(String partialText) {
                Platform.runLater(() -> {
                    if (cabinSourceTextArea != null) cabinSourceTextArea.setText(partialText);
                });
            }

            @Override
            public void onFinalResult(String finalText) {
                Platform.runLater(() -> {
                    if (cabinSourceTextArea != null) cabinSourceTextArea.setText(finalText);

                    String translation = translationService.translateToVietnamese(finalText);
                    if (cabinTranslationTextArea != null) cabinTranslationTextArea.setText(translation);

                    textToSpeechService.speak(translation);
                    saveTranslationToHistory(finalText, translation, null, "audio");

                    if (cabinStatusLabel != null) cabinStatusLabel.setText("Translation completed - Listening...");
                });
            }

            @Override
            public void onError(String error) {
                Platform.runLater(() -> {
                    if (cabinStatusLabel != null) cabinStatusLabel.setText("Recognition error: " + error);
                    isCabinMode = false;
                    updateButtonStates();
                });
            }
        });
    }

    // Stop cabin mode
    private void stopCabinMode() {
        System.out.println("Stopping cabin mode");
        speechRecognitionService.stopRealTimeRecognition();
        isCabinMode = false;
        updateButtonStates();
        if (cabinStatusLabel != null) cabinStatusLabel.setText("Cabin mode stopped");
    }

    // Search translation history
    private void searchTranslationHistory(String keyword) {
        System.out.println("Searching translation history for: " + keyword);

        try {
            List<TranslationHistory> searchResults = historyDAO.searchByKeyword(keyword);
            historyData.clear();
            historyData.addAll(searchResults);
            System.out.println("Found " + searchResults.size() + " translation records for keyword: " + keyword);
        } catch (Exception e) {
            System.out.println("Failed to search translation history: " + e.getMessage());
            showErrorAlert("Search Error", "Failed to search translation history: " + e.getMessage());
        }
    }

    // Export translation history
    private void exportTranslationHistory() {
        System.out.println("Exporting translation history");

        if (historyData == null || historyData.isEmpty()) {
            showWarningAlert("No Data", "No translation history to export.");
            return;
        }

        ChoiceDialog<ExportService.ExportFormat> formatDialog = new ChoiceDialog<>(
                ExportService.ExportFormat.PDF, ExportService.ExportFormat.values());
        formatDialog.setTitle("Export Format");
        formatDialog.setHeaderText("Select Export Format");
        formatDialog.setContentText("Choose format:");

        Optional<ExportService.ExportFormat> formatResult = formatDialog.showAndWait();
        if (formatResult.isEmpty()) {
            return;
        }

        ExportService.ExportFormat selectedFormat = formatResult.get();

        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Export Translation History");
        fileChooser.getExtensionFilters().add(ExportService.getExtensionFilter(selectedFormat));
        fileChooser.setInitialFileName("voicify_history." + selectedFormat.getExtension());

        Stage stage = (Stage) exportButton.getScene().getWindow();
        File saveFile = fileChooser.showSaveDialog(stage);

        if (saveFile != null) {
            try {
                exportService.exportHistory(historyData, saveFile, selectedFormat);
                showInfoAlert("Export Successful", "Translation history exported to: " + saveFile.getName());
            } catch (Exception e) {
                System.out.println("Export failed: " + e.getMessage());
                showErrorAlert("Export Error", "Failed to export translation history: " + e.getMessage());
            }
        }
    }

    // Delete translation history
    private void deleteTranslationHistory(TranslationHistory history) {
        System.out.println("Deleting translation history: " + history.getId());

        Alert confirmDialog = new Alert(Alert.AlertType.CONFIRMATION);
        confirmDialog.setTitle("Confirm Delete");
        confirmDialog.setHeaderText("Delete Translation Record");
        confirmDialog.setContentText("Are you sure you want to delete this translation record?");

        Optional<ButtonType> result = confirmDialog.showAndWait();
        if (result.isPresent() && result.get() == ButtonType.OK) {
            try {
                historyDAO.delete(history.getId());
                historyData.remove(history);
                System.out.println("Translation history deleted successfully");
            } catch (Exception e) {
                System.out.println("Failed to delete translation history: " + e.getMessage());
                showErrorAlert("Delete Error", "Failed to delete translation history: " + e.getMessage());
            }
        }
    }

    // Save translation to history
    private void saveTranslationToHistory(String sourceText, String translatedText, String audioFilePath) {
        saveTranslationToHistory(sourceText, translatedText, audioFilePath,
                audioFilePath != null ? "file" : "audio");
    }

    private void saveTranslationToHistory(String sourceText, String translatedText, String audioFilePath, String type) {
        try {
            // Lấy user_id của người dùng hiện tại
            int userId = SessionManager.getInstance().getCurrentUser().getUserId();

            TranslationHistory history = new TranslationHistory(
                    userId, sourceText, translatedText, type, audioFilePath);

            historyDAO.save(history);
            System.out.println("Translation saved to history");
        } catch (Exception e) {
            System.out.println("Failed to save translation to history: " + e.getMessage());
            showErrorAlert("Save Error", "Failed to save translation to history: " + e.getMessage());
        }
    }

    // Show error alert
    private void showErrorAlert(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }

    // Show warning alert
    private void showWarningAlert(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.WARNING);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }

    // Show info alert
    private void showInfoAlert(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }

    // Handle File Translation button event
    @FXML
    private void handleFileTranslationButton() {
        switchToView(CurrentView.FILE_PROCESSING);
        isTranslateMode = true;
        fileProcessingTitle.setText("File Translation");
        processFileButton.setText("Translate File");
        if (fileProcessingStatusLabel != null) fileProcessingStatusLabel.setText("Ready for file translation");
    }

    // Handle File Summary button event
    @FXML
    private void handleFileSummaryButton() {
        switchToView(CurrentView.FILE_PROCESSING);
        isTranslateMode = false;
        fileProcessingTitle.setText("File Summary");
        processFileButton.setText("Summarize File");
        if (fileProcessingStatusLabel != null) fileProcessingStatusLabel.setText("Ready for file summarization");
    }

    // Handle Load File for Processing button event
    /**
     * Xử lý sự kiện khi người dùng nhấp vào nút "Load File"
     */
    @FXML
    private void handleLoadFileForProcessing() {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Select File");
        fileChooser.getExtensionFilters().addAll(
            new FileChooser.ExtensionFilter("All Supported Files", "*.txt", "*.pdf", "*.docx", "*.xlsx", "*.xls", "*.csv", "*.json", "*.xml"),
            new FileChooser.ExtensionFilter("Text Files", "*.txt"),
            new FileChooser.ExtensionFilter("PDF Files", "*.pdf"),
            new FileChooser.ExtensionFilter("Word Documents", "*.docx"),
            new FileChooser.ExtensionFilter("Excel Spreadsheets", "*.xlsx", "*.xls"),
            new FileChooser.ExtensionFilter("CSV Files", "*.csv"),
            new FileChooser.ExtensionFilter("JSON Files", "*.json"),
            new FileChooser.ExtensionFilter("XML Files", "*.xml"),
            new FileChooser.ExtensionFilter("All Files", "*.*")
        );

        Stage stage = (Stage) loadFileForProcessingButton.getScene().getWindow();
        File selectedFile = fileChooser.showOpenDialog(stage);

        if (selectedFile != null) {
            try {
                currentFile = selectedFile;

                // Hiển thị thông báo đang tải
                if (fileProcessingStatusLabel != null) {
                    fileProcessingStatusLabel.setText("Loading file: " + selectedFile.getName() + "...");
                }

                // Tạo task riêng biệt để tải file lớn
                Task<String> loadTask = new Task<>() {
                    @Override
                    protected String call() throws Exception {
                        return fileProcessingService.extractTextFromFile(selectedFile);
                    }
                };

                loadTask.setOnSucceeded(event -> {
                    String content = loadTask.getValue();
                    if (fileInputTextArea != null) {
                        fileInputTextArea.setText(content);
                    }
                    if (fileProcessingStatusLabel != null) {
                        fileProcessingStatusLabel.setText("File loaded: " + selectedFile.getName());
                    }
                });

                loadTask.setOnFailed(event -> {
                    Throwable exception = loadTask.getException();
                    showErrorAlert("File Error", "Could not read file: " + exception.getMessage());
                    if (fileProcessingStatusLabel != null) {
                        fileProcessingStatusLabel.setText("Failed to load file");
                    }
                });

                // Chạy task trong thread riêng biệt
                new Thread(loadTask).start();

            } catch (Exception e) {
                showErrorAlert("File Error", "Could not read file: " + e.getMessage());
            }
        }
    }

    // Handle Process File button event
    /**
     * Xử lý sự kiện khi người dùng nhấp vào nút "Process File" (Translate/Summarize)
     */
    @FXML
    private void handleProcessFile() {
        if (fileInputTextArea == null || fileInputTextArea.getText().isEmpty()) {
            showWarningAlert("Input Error", "Please load or enter text to process.");
            return;
        }

        if (fileProcessingStatusLabel != null) {
            fileProcessingStatusLabel.setText("Processing...");
        }

        // Vô hiệu hóa nút xử lý trong quá trình xử lý
        if (processFileButton != null) {
            processFileButton.setDisable(true);
        }

        Task<String> processTask = new Task<>() {
            @Override
            protected String call() throws Exception {
                // Luôn xử lý snapshot nội dung hiện tại để tránh dùng file cũ.
                File processingFile = File.createTempFile("voicify_", ".txt");
                processingFile.deleteOnExit();
                try (FileWriter writer = new FileWriter(processingFile)) {
                    writer.write(fileInputTextArea.getText());
                }

                // Xử lý file dựa trên chế độ hiện tại
                if (isTranslateMode) {
                    return fileProcessingService.translateFile(processingFile);
                } else {
                    return fileProcessingService.summarizeFile(processingFile);
                }
            }
        };

        processTask.setOnSucceeded(event -> {
            String result = processTask.getValue();
            if (fileOutputTextArea != null) {
                fileOutputTextArea.setText(result);
            }
            if (fileProcessingStatusLabel != null) {
                fileProcessingStatusLabel.setText("Processing completed");
            }
            if (processFileButton != null) {
                processFileButton.setDisable(false);
            }
        });

        processTask.setOnFailed(event -> {
            Throwable exception = processTask.getException();
            if (fileProcessingStatusLabel != null) {
                fileProcessingStatusLabel.setText("Processing failed");
            }
            showErrorAlert("Processing Error", exception.getMessage());
            if (processFileButton != null) {
                processFileButton.setDisable(false);
            }
        });

        // Chạy task trong thread riêng biệt
        new Thread(processTask).start();
    }

    // Handle Save Output button event
    @FXML
    private void handleSaveOutput() {
        // Kiểm tra xem có nội dung để lưu không
        if (fileOutputTextArea == null || fileOutputTextArea.getText().isEmpty()) {
            showWarningAlert("Output Error", "No output to save.");
            return;
        }

        // Tạo hộp thoại lưu file
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Save Output");
        fileChooser.getExtensionFilters().add(
            new FileChooser.ExtensionFilter("Text Files", "*.txt")
        );

        // Hiển thị hộp thoại lưu file
        Stage stage = (Stage) saveOutputButton.getScene().getWindow();
        File file = fileChooser.showSaveDialog(stage);

        // Lưu nội dung vào file
        if (file != null) {
            try (FileWriter writer = new FileWriter(file)) {
                writer.write(fileOutputTextArea.getText());

                // Cập nhật nhãn trạng thái
                if (fileProcessingStatusLabel != null) {
                    fileProcessingStatusLabel.setText("Output saved to: " + file.getName());
                }
            } catch (Exception e) {
                showErrorAlert("Save Error", "Could not save file: " + e.getMessage());
            }
        }
    }

    private void updateProcessButtonText() {
        if (processFileButton != null) {
            processFileButton.setText(isTranslateMode ? "Translate File" : "Summarize File");
        }
    }

    @FXML
    private void handleFileProcessingButton() {
        switchToView(CurrentView.FILE_PROCESSING);

        updateProcessButtonText();

        if (fileProcessingStatusLabel != null) {
            fileProcessingStatusLabel.setText("Ready for file processing");
        }
    }

    /**
     * Xử lý sự kiện khi người dùng thay đổi loại xử lý (Translate/Summarize)
     */
    @FXML
    private void handleProcessingTypeChange() {
        // Cập nhật chế độ xử lý dựa trên RadioButton được chọn
        isTranslateMode = translateRadio.isSelected();

        // Cập nhật văn bản trên nút xử lý
        updateProcessButtonText();

        // Cập nhật nhãn trạng thái
        if (fileProcessingStatusLabel != null) {
            fileProcessingStatusLabel.setText(isTranslateMode ?
                "Ready for file translation" : "Ready for file summarization");
        }
    }

    /**
     * Update status label styling based on state
     */
    private void updateStatusLabelStyle(Label label, String state) {
        if (label == null) return;

        // Remove all existing state classes
        label.getStyleClass().removeAll("ready", "processing", "completed", "error", "warning");

        // Add status-label class if not present
        if (!label.getStyleClass().contains("status-label")) {
            label.getStyleClass().add("status-label");
        }

        // Add the new state class
        label.getStyleClass().add(state);
    }

    // Thêm phương thức xử lý cho nút refresh ở mỗi tab
    @FXML
    private void handleRegularRefreshButton() {
        // Xóa nội dung các trường văn bản
        if (sourceTextArea != null) sourceTextArea.clear();
        if (translationTextArea != null) translationTextArea.clear();
        if (statusLabel != null) {
            statusLabel.setText("Ready for new translation");
            updateStatusLabelStyle(statusLabel, "ready");
        }
    }

    @FXML
    private void handleCabinRefreshButton() {
        // Xóa nội dung các trường văn bản và dừng cabin mode nếu đang hoạt động
        if (cabinSourceTextArea != null) cabinSourceTextArea.clear();
        if (cabinTranslationTextArea != null) cabinTranslationTextArea.clear();
        if (isCabinMode) {
            stopCabinMode();
        }
        if (cabinStatusLabel != null) cabinStatusLabel.setText("Ready for cabin mode");
    }

    @FXML
    private void handleAIRefreshButton() {
        // Xóa nội dung các trường văn bản
        if (aiSourceTextArea != null) aiSourceTextArea.clear();
        if (aiCurrentTranslationArea != null) aiCurrentTranslationArea.clear();
        if (aiImprovedTranslationArea != null) aiImprovedTranslationArea.clear();
        if (aiStatusLabel != null) aiStatusLabel.setText("Ready for AI translation improvement");
    }

    @FXML
    private void handleFileProcessingRefreshButton() {
        // Xóa nội dung các trường văn bản và đặt lại trạng thái
        if (fileInputTextArea != null) fileInputTextArea.clear();
        if (fileOutputTextArea != null) fileOutputTextArea.clear();
        currentFile = null;
        if (fileProcessingStatusLabel != null) fileProcessingStatusLabel.setText("Ready for file processing");
    }

    /**
     * Làm mới tất cả các tab
     */
    private void refreshAllTabs() {
        // Làm mới tab Regular Translation
        if (sourceTextArea != null) sourceTextArea.clear();
        if (translationTextArea != null) translationTextArea.clear();

        // Làm mới tab Cabin Translation
        if (cabinSourceTextArea != null) cabinSourceTextArea.clear();
        if (cabinTranslationTextArea != null) cabinTranslationTextArea.clear();
        if (isCabinMode) {
            stopCabinMode();
        }

        // Làm mới tab AI Translation
        if (aiSourceTextArea != null) aiSourceTextArea.clear();
        if (aiCurrentTranslationArea != null) aiCurrentTranslationArea.clear();
        if (aiImprovedTranslationArea != null) aiImprovedTranslationArea.clear();

        // Làm mới tab File Processing
        if (fileInputTextArea != null) fileInputTextArea.clear();
        if (fileOutputTextArea != null) fileOutputTextArea.clear();
        currentFile = null;

        // Làm mới tab History
        loadTranslationHistory();

        // Cập nhật các nhãn trạng thái với styling
        if (statusLabel != null) {
            statusLabel.setText("Ready for new translation");
            updateStatusLabelStyle(statusLabel, "ready");
        }
        if (cabinStatusLabel != null) {
            cabinStatusLabel.setText("Ready for cabin mode");
            updateStatusLabelStyle(cabinStatusLabel, "ready");
        }
        if (aiStatusLabel != null) {
            aiStatusLabel.setText("Ready for AI translation improvement");
            updateStatusLabelStyle(aiStatusLabel, "ready");
        }
        if (fileProcessingStatusLabel != null) {
            fileProcessingStatusLabel.setText("Ready for file processing");
            updateStatusLabelStyle(fileProcessingStatusLabel, "ready");
        }
    }

    // Thêm phương thức xử lý cho nút làm mới tất cả
    @FXML
    private void handleRefreshAllButton() {
        refreshAllTabs();
    }

    /**
     * Handle logout button click
     * This method logs out the current user and returns to the login screen
     */
    @FXML
    private void handleLogout() {
        // Clear the current user session
        SessionManager.getInstance().setCurrentUser(null);

        try {
            // Load the login view
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/com/voicify/fxml/login-view.fxml"));
            Parent root = loader.load();

            // Get the current stage
            Stage stage = (Stage) sidebarLogout.getScene().getWindow();

            // Create a new scene with the login view in 16:9 aspect ratio
            Scene scene = new Scene(root, 1280, 720);

            // Clear existing stylesheets and add login styling
            scene.getStylesheets().clear();
            scene.getStylesheets().add(getClass().getResource("/com/voicify/css/login-register.css").toExternalForm());

            // Set the scene on the stage
            stage.setScene(scene);
            stage.setTitle("Voicify - Login");
            stage.setResizable(false);

            // Set exact 16:9 dimensions
            stage.setWidth(1280);
            stage.setHeight(720);
            stage.setMinWidth(1280);
            stage.setMinHeight(720);
            stage.setMaxWidth(1280);
            stage.setMaxHeight(720);
            stage.centerOnScreen();
            stage.show();

        } catch (IOException e) {
            System.err.println("Failed to load login view: " + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * Improved heuristic to check if text is likely English
     */
    private boolean isLikelyEnglish(String text) {
        if (text == null || text.trim().isEmpty()) {
            return false;
        }

        // Remove punctuation and convert to lowercase
        String cleanText = text.replaceAll("[^a-zA-Z\\s]", "").toLowerCase();
        String[] words = cleanText.split("\\s+");

        if (words.length == 0) {
            return false;
        }

        // Check for Vietnamese characters (if present, likely not English)
        if (text.matches(".*[àáạảãâầấậẩẫăằắặẳẵèéẹẻẽêềếệểễìíịỉĩòóọỏõôồốộổỗơờớợởỡùúụủũưừứựửữỳýỵỷỹđ].*")) {
            return false;
        }

        // Common English words (expanded list)
        String[] commonEnglishWords = {
            "the", "be", "to", "of", "and", "a", "in", "that", "have", "i", "it", "for", "not", "on", "with", "he", "as", "you", "do", "at",
            "this", "but", "his", "by", "from", "they", "she", "or", "an", "will", "my", "one", "all", "would", "there", "their",
            "what", "so", "up", "out", "if", "about", "who", "get", "which", "go", "me", "when", "make", "can", "like", "time",
            "no", "just", "him", "know", "take", "people", "into", "year", "your", "good", "some", "could", "them", "see", "other",
            "than", "then", "now", "look", "only", "come", "its", "over", "think", "also", "back", "after", "use", "two", "how",
            "our", "work", "first", "well", "way", "even", "new", "want", "because", "any", "these", "give", "day", "most", "us",
            // Technical/common words
            "voice", "translation", "platform", "advanced", "system", "application", "technology", "service", "english", "language",
            "hello", "world", "name", "what", "where", "when", "why", "how", "please", "thank", "welcome", "help", "support"
        };

        int englishWordCount = 0;
        int totalWords = words.length;

        for (String word : words) {
            // Check against common English words
            for (String englishWord : commonEnglishWords) {
                if (word.equals(englishWord)) {
                    englishWordCount++;
                    break;
                }
            }

            // Additional checks for English-like patterns
            if (word.length() > 0) {
                // Check if word contains only English letters
                if (word.matches("[a-z]+")) {
                    // Common English word patterns
                    if (word.endsWith("ing") || word.endsWith("ed") || word.endsWith("er") ||
                        word.endsWith("ly") || word.endsWith("tion") || word.endsWith("ness") ||
                        word.startsWith("un") || word.startsWith("re") || word.startsWith("pre")) {
                        englishWordCount++;
                    }
                }
            }
        }

        // More lenient threshold: if at least 20% of words are English-like, consider it English
        // OR if it's a short text (< 5 words) and contains any English words, consider it English
        double englishRatio = (double) englishWordCount / totalWords;

        if (totalWords <= 4) {
            // For short texts, be more lenient
            return englishWordCount > 0 || cleanText.matches(".*[a-z].*");
        } else {
            // For longer texts, use ratio
            return englishRatio >= 0.2;
        }
    }
}
