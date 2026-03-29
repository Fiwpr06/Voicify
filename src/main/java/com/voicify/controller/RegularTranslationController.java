package com.voicify.controller;

import com.voicify.model.TranslationHistory;
import com.voicify.model.TranslationHistoryDAO;
import com.voicify.service.*;
import com.voicify.util.ErrorHandler;
import javafx.application.Platform;
import javafx.concurrent.Task;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.layout.VBox;
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;

public class RegularTranslationController extends BaseController {
    private static final Logger logger = LoggerFactory.getLogger(RegularTranslationController.class);

    @FXML private VBox root;
    @FXML private TextArea sourceTextArea;
    @FXML private TextArea translationTextArea;
    @FXML private Button recordButton;
    @FXML private Button loadFileButton;
    @FXML private Button translateButton;
    @FXML private Button speakButton;
    @FXML private Label statusLabel;

    private boolean isRecording = false;
    private TranslationHistoryDAO historyDAO = new TranslationHistoryDAO();

    // Additional services (base services are inherited from BaseController)
    private FileProcessingService fileProcessingService;

    @FXML
    private void initialize() {
        // Initialize services
        initializeServices();

        // Set initial status
        if (statusLabel != null) {
            statusLabel.setText("Ready");
        }
    }

    @Override
    protected void initializeServices() {
        try {
            super.initializeServices(); // Call parent initialization
            fileProcessingService = new FileProcessingService();

            logger.info("RegularTranslationController services initialized successfully");
        } catch (Exception e) {
            logger.error("Failed to initialize services", e);
            ErrorHandler.handleError("Initialization Error", "Failed to initialize translation services", e);
        }
    }

    @FXML
    private void handleRecordButton() {
        if (isRecording) {
            stopRecording();
        } else {
            startRecording();
        }
    }

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

    @FXML
    private void handleTranslateButton() {
        String sourceText = sourceTextArea.getText().trim();
        if (sourceText.isEmpty()) {
            updateStatus("Please enter text to translate");
            return;
        }

        translateText(sourceText);
    }

    @FXML
    private void handleSpeakButton() {
        String textToSpeak = translationTextArea.getText().trim();
        if (textToSpeak.isEmpty()) {
            updateStatus("No translation text to speak");
            return;
        }

        speakText(textToSpeak);
    }

    private void processSelectedFile(File selectedFile) {
        String fileName = selectedFile.getName().toLowerCase();
        updateStatus("Processing file: " + selectedFile.getName());

        // Debug logging
        System.out.println("RegularTranslationController.processSelectedFile: " + fileName);

        // Check file type and process accordingly
        if (fileName.endsWith(".wav") || fileName.endsWith(".mp3") || fileName.endsWith(".flac") || fileName.endsWith(".m4a")) {
            // Process as audio file for speech recognition
            System.out.println("Processing as AUDIO file: " + fileName);
            processAudioFile(selectedFile);
        } else if (fileName.endsWith(".txt") || fileName.endsWith(".csv") || fileName.endsWith(".json") || fileName.endsWith(".xml")) {
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
            updateStatus("Unsupported file format. Supported formats: Audio (wav, mp3, flac, m4a), Text (txt, csv, json, xml), PDF, Word (docx), Excel (xlsx, xls)");
        }
    }

    private void processAudioFile(File audioFile) {
        System.out.println("RegularTranslationController.processAudioFile called for: " + audioFile.getName());

        if (speechRecognitionService == null || !speechRecognitionService.isAvailable()) {
            updateStatus("Speech recognition service not available");
            return;
        }

        updateStatus("Recognizing speech from audio file... (This may take a moment)");

        Task<String> recognitionTask = new Task<String>() {
            @Override
            protected String call() throws Exception {
                // Update progress periodically
                updateMessage("Processing audio file: " + audioFile.getName());
                updateProgress(0, 100);

                long startTime = System.currentTimeMillis();
                String result = speechRecognitionService.recognizeFromFile(audioFile);
                long endTime = System.currentTimeMillis();

                updateProgress(100, 100);
                updateMessage("Recognition completed in " + (endTime - startTime) + "ms");

                return result;
            }
        };

        // Update UI with progress
        recognitionTask.messageProperty().addListener((obs, oldMessage, newMessage) -> {
            if (newMessage != null) {
                Platform.runLater(() -> updateStatus(newMessage));
            }
        });

        recognitionTask.setOnSucceeded(event -> {
            String recognizedText = recognitionTask.getValue();
            Platform.runLater(() -> {
                if (sourceTextArea != null) {
                    sourceTextArea.setText(recognizedText);
                }
                if (recognizedText.trim().isEmpty()) {
                    updateStatus("No speech recognized from audio file");
                } else {
                    updateStatus("Speech recognized successfully. Click Translate to continue.");
                }
            });
        });

        recognitionTask.setOnFailed(event -> {
            Throwable exception = recognitionTask.getException();
            Platform.runLater(() -> {
                updateStatus("Failed to recognize speech from audio file");
                ErrorHandler.handleFileError("recognize speech from", audioFile.getAbsolutePath(), exception);
            });
        });

        new Thread(recognitionTask).start();
    }

    private void processTextFile(File textFile) {
        if (fileProcessingService == null) {
            updateStatus("File processing service not available");
            return;
        }

        updateStatus("Reading text file...");

        Task<String> readTask = new Task<String>() {
            @Override
            protected String call() throws Exception {
                return fileProcessingService.extractTextFromFile(textFile);
            }
        };

        readTask.setOnSucceeded(event -> {
            String fileContent = readTask.getValue();
            Platform.runLater(() -> {
                if (sourceTextArea != null) {
                    sourceTextArea.setText(fileContent);
                }
                updateStatus("Text file loaded successfully. Click Translate to continue.");
            });
        });

        readTask.setOnFailed(event -> {
            Throwable exception = readTask.getException();
            Platform.runLater(() -> {
                updateStatus("Failed to read text file");
                ErrorHandler.handleFileError("read text from", textFile.getAbsolutePath(), exception);
            });
        });

        new Thread(readTask).start();
    }

    private void startRecording() {
        if (speechRecognitionService == null || !speechRecognitionService.isAvailable()) {
            updateStatus("Speech recognition service not available");
            return;
        }

        if (audioService == null) {
            updateStatus("Audio service not available");
            return;
        }

        isRecording = true;
        updateRecordButtonText();
        updateStatus("Recording... Click Stop to finish");

        speechRecognitionService.startRealTimeRecognition(new SpeechRecognitionService.SpeechRecognitionCallback() {
            @Override
            public void onRecognitionStarted() {
                Platform.runLater(() -> updateStatus("Recording started..."));
            }

            @Override
            public void onPartialResult(String partialText) {
                Platform.runLater(() -> {
                    if (sourceTextArea != null) {
                        sourceTextArea.setText(partialText);
                    }
                });
            }

            @Override
            public void onFinalResult(String finalText) {
                Platform.runLater(() -> {
                    if (sourceTextArea != null) {
                        sourceTextArea.setText(finalText);
                    }
                    isRecording = false;
                    updateRecordButtonText();
                    updateStatus("Recording completed. Click Translate to continue.");
                });
            }

            @Override
            public void onError(String error) {
                Platform.runLater(() -> {
                    isRecording = false;
                    updateRecordButtonText();
                    updateStatus("Recording error: " + error);
                });
            }
        }, "en"); // Default to English, could be made configurable
    }

    private void stopRecording() {
        if (speechRecognitionService != null) {
            speechRecognitionService.stopRealTimeRecognition();
        }
        isRecording = false;
        updateRecordButtonText();
        updateStatus("Recording stopped");
    }

    private void translateText(String sourceText) {
        if (translationService == null) {
            updateStatus("Translation service not available");
            return;
        }

        updateStatus("Translating...");

        Task<String> translationTask = new Task<String>() {
            @Override
            protected String call() throws Exception {
                return translationService.translateToVietnamese(sourceText);
            }
        };

        translationTask.setOnSucceeded(event -> {
            String translation = translationTask.getValue();
            Platform.runLater(() -> {
                if (translationTextArea != null) {
                    translationTextArea.setText(translation);
                }
                updateStatus("Translation completed");

                // Save to history
                saveTranslationToHistory(sourceText, translation);
            });
        });

        translationTask.setOnFailed(event -> {
            Throwable exception = translationTask.getException();
            Platform.runLater(() -> {
                updateStatus("Translation failed");
                ErrorHandler.handleError("Translation Error", "Failed to translate text", exception);
            });
        });

        new Thread(translationTask).start();
    }

    private void speakText(String text) {
        if (textToSpeechService == null) {
            updateStatus("Text-to-speech service not available");
            return;
        }

        updateStatus("Speaking...");

        Task<Void> speakTask = new Task<Void>() {
            @Override
            protected Void call() throws Exception {
                textToSpeechService.speak(text);
                return null;
            }
        };

        speakTask.setOnSucceeded(event -> {
            Platform.runLater(() -> updateStatus("Speech completed"));
        });

        speakTask.setOnFailed(event -> {
            Throwable exception = speakTask.getException();
            Platform.runLater(() -> {
                updateStatus("Speech failed");
                ErrorHandler.handleError("Speech Error", "Failed to speak text", exception);
            });
        });

        new Thread(speakTask).start();
    }

    private void saveTranslationToHistory(String sourceText, String translation) {
        try {
            // Get current user ID (default to 1 if no user session)
            int userId = 1; // Could be improved to get from SessionManager

            TranslationHistory history = new TranslationHistory(
                userId,
                sourceText,
                translation,
                "text",
                null // no audio file path for text translations
            );

            historyDAO.save(history);
            logger.info("Translation saved to history");
        } catch (Exception e) {
            logger.error("Failed to save translation to history", e);
            // Don't show error to user as this is not critical
        }
    }

    private void processPdfFile(File pdfFile) {
        updateStatus("Processing PDF file...");

        Task<String> task = new Task<String>() {
            @Override
            protected String call() throws Exception {
                return fileProcessingService.extractTextFromFile(pdfFile);
            }
        };

        task.setOnSucceeded(e -> {
            String extractedText = task.getValue();
            Platform.runLater(() -> {
                sourceTextArea.setText(extractedText);
                updateStatus("PDF file processed successfully. Text extracted and ready for translation.");
            });
        });

        task.setOnFailed(e -> {
            Throwable exception = task.getException();
            Platform.runLater(() -> {
                updateStatus("Failed to process PDF file: " + exception.getMessage());
                ErrorHandler.handleFileError("process PDF", pdfFile.getAbsolutePath(), (Exception) exception);
            });
        });

        new Thread(task).start();
    }

    private void processDocxFile(File docxFile) {
        updateStatus("Processing Word document...");

        Task<String> task = new Task<String>() {
            @Override
            protected String call() throws Exception {
                return fileProcessingService.extractTextFromFile(docxFile);
            }
        };

        task.setOnSucceeded(e -> {
            String extractedText = task.getValue();
            Platform.runLater(() -> {
                sourceTextArea.setText(extractedText);
                updateStatus("Word document processed successfully. Text extracted and ready for translation.");
            });
        });

        task.setOnFailed(e -> {
            Throwable exception = task.getException();
            Platform.runLater(() -> {
                updateStatus("Failed to process Word document: " + exception.getMessage());
                ErrorHandler.handleFileError("process Word document", docxFile.getAbsolutePath(), (Exception) exception);
            });
        });

        new Thread(task).start();
    }

    private void processExcelFile(File excelFile) {
        updateStatus("Processing Excel file...");

        Task<String> task = new Task<String>() {
            @Override
            protected String call() throws Exception {
                return fileProcessingService.extractTextFromFile(excelFile);
            }
        };

        task.setOnSucceeded(e -> {
            String extractedText = task.getValue();
            Platform.runLater(() -> {
                sourceTextArea.setText(extractedText);
                updateStatus("Excel file processed successfully. Text extracted and ready for translation.");
            });
        });

        task.setOnFailed(e -> {
            Throwable exception = task.getException();
            Platform.runLater(() -> {
                updateStatus("Failed to process Excel file: " + exception.getMessage());
                ErrorHandler.handleFileError("process Excel file", excelFile.getAbsolutePath(), (Exception) exception);
            });
        });

        new Thread(task).start();
    }

    private void updateStatus(String message) {
        if (statusLabel != null) {
            statusLabel.setText(message);
        }
        logger.info("Status: {}", message);
    }

    private void updateRecordButtonText() {
        if (recordButton != null) {
            recordButton.setText(isRecording ? "Stop Recording" : "Start Recording");
        }
    }

    @Override
    protected Object getRootNode() {
        return root;
    }
}