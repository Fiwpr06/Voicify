package com.voicify.service;

import com.voicify.config.ConfigurationManager;
import com.voicify.util.AudioOptimizer;
import com.voicify.util.ErrorHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.vosk.LibVosk;
import org.vosk.LogLevel;
import org.vosk.Model;
import org.vosk.Recognizer;
import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;
import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.Map;

public class SpeechRecognitionService {
    private static final Logger logger = LoggerFactory.getLogger(SpeechRecognitionService.class);
    private boolean isInitialized = false;
    private boolean isRecording = false;
    private final ConfigurationManager config;
    private final Map<String, Model> voskModels;
    private String currentLanguage = "en"; // Default to English
    private AudioService activeAudioService;

    public SpeechRecognitionService() {
        this.config = ConfigurationManager.getInstance();
        this.voskModels = new HashMap<>();
        initializeVosk();
    }

    private void initializeVosk() {
        try {
            LibVosk.setLogLevel(LogLevel.WARNINGS);

            // Try to load small models first for faster processing
            // Load English model - try small model first
            String[] englishModelPaths = {
                "./library/vosk-model-small-en-us-0.15",  // Small model (faster)
                "./library/vosk-en-us-0.15",              // Full model (fallback)
                "./library/vosk-model-en-us-0.22-lgraph", // Alternative
                "./library/vosk-model-en-us-0.22"         // Alternative
            };

            boolean englishLoaded = false;
            for (String modelPath : englishModelPaths) {
                if (loadModel("en", modelPath)) {
                    logger.info("English Vosk model loaded successfully from: {}", modelPath);
                    englishLoaded = true;
                    break;
                }
            }
            if (!englishLoaded) {
                logger.warn("No English Vosk model could be loaded");
            }

            // Load Vietnamese model - try small model first
            String[] vietnameseModelPaths = {
                "./library/vosk-model-small-vn-0.4",      // Small model (faster)
                "./library/vosk-model-vn-0.4",            // Full model (fallback)
                "./library/vosk-model-vn-0.3"             // Alternative
            };

            boolean vietnameseLoaded = false;
            for (String modelPath : vietnameseModelPaths) {
                if (loadModel("vi", modelPath)) {
                    logger.info("Vietnamese Vosk model loaded successfully from: {}", modelPath);
                    vietnameseLoaded = true;
                    break;
                }
            }
            if (!vietnameseLoaded) {
                logger.warn("No Vietnamese Vosk model could be loaded");
            }

            // Check if at least one model is loaded
            if (!voskModels.isEmpty()) {
                isInitialized = true;
                logger.info("Vosk speech recognition initialized with {} language(s)", voskModels.size());
            } else {
                logger.error("No Vosk models could be loaded");
                ErrorHandler.handleConfigurationError("Vosk Models",
                    "Không tìm thấy model Vosk nào. Vui lòng kiểm tra thư mục library/");
            }

        } catch (Exception e) {
            isInitialized = false;
            logger.error("Failed to initialize Vosk: {}", e.getMessage(), e);
            ErrorHandler.handleError("Vosk Error", "Không thể khởi tạo Vosk", e);
        }
    }

    private boolean loadModel(String language, String modelPath) {
        try {
            if (!Files.exists(Paths.get(modelPath))) {
                logger.warn("Vosk model not found at: {}", modelPath);
                return false;
            }

            Model model = new Model(modelPath);
            voskModels.put(language, model);
            logger.info("Loaded {} Vosk model from: {}", language, modelPath);
            return true;
        } catch (Exception e) {
            logger.error("Failed to load {} model from {}: {}", language, modelPath, e.getMessage());
            return false;
        }
    }

//    public void startRealTimeRecognition(SpeechRecognitionCallback callback) {
//        if (!isInitialized) {
//            callback.onError("Speech recognition not initialized");
//            Dialog.showAlert(AlertType.ERROR, "Recognition Error", "Hệ thống nhận diện giọng nói chưa được khởi tạo.");
//            return;
//        }
//
//        if (isRecording) {
//            callback.onError("Recording already in progress");
//            Dialog.showAlert(AlertType.WARNING, "Recognition Error", "Đang nhận diện, vui lòng dừng trước khi bắt đầu lại.");
//            return;
//        }
//
//        CompletableFuture.runAsync(() -> {
//            try {
//                isRecording = true;
//                System.out.println("Starting real-time speech recognition");
//                callback.onRecognitionStarted();
//                Dialog.showAlert(AlertType.INFORMATION, "Recognition Started", "Bắt đầu nhận diện giọng nói...");
//
//                Recognizer recognizer = new Recognizer(voskModel, 16000);
//                AudioService audioService = new AudioService();
//                audioService.startRecording(new AudioService.AudioRecordingCallback() {
//                    @Override
//                    public void onRecordingStarted() {}
//
//                    @Override
//                    public void onAudioData(byte[] data, int length) {
//                        if (recognizer.acceptWaveForm(data, length)) {
//                            String result = recognizer.getResult();
//                            callback.onPartialResult(result);
//                        }
//                    }
//
//                    @Override
//                    public void onRecordingCompleted(byte[] audioData) {
//                        String finalResult = recognizer.getFinalResult();
//                        callback.onFinalResult(finalResult);
//                        System.out.println("Real-time recognition completed: " + finalResult);
//                        Dialog.showAlert(AlertType.INFORMATION, "Recognition Completed", "Nhận diện hoàn tất: " + finalResult);
//                        recognizer.close();
//                    }
//
//                    @Override
//                    public void onError(String error) {
//                        callback.onError(error);
//                        Dialog.showAlert(AlertType.ERROR, "Recording Error", error);
//                        recognizer.close();
//                    }
//                });
//
//            } catch (Exception e) {
//                callback.onError("Recognition failed: " + e.getMessage());
//                System.out.println("Real-time recognition failed: " + e.getMessage());
//                Dialog.showAlert(AlertType.ERROR, "Recognition Error", "Nhận diện thất bại: " + e.getMessage());
//            } finally {
//                isRecording = false;
//            }
//        });
//    }

    public void setLanguage(String language) {
        if (voskModels.containsKey(language)) {
            this.currentLanguage = language;
            logger.info("Speech recognition language set to: {}", language);
        } else {
            logger.warn("Language {} not available, keeping current language: {}", language, currentLanguage);
        }
    }

    public String getCurrentLanguage() {
        return currentLanguage;
    }

    public String[] getAvailableLanguages() {
        return voskModels.keySet().toArray(new String[0]);
    }

    public void startRealTimeRecognition(SpeechRecognitionCallback callback) {
        startRealTimeRecognition(callback, currentLanguage);
    }

    public void startRealTimeRecognition(SpeechRecognitionCallback callback, String language) {
        if (!isInitialized) {
            logger.error("Speech recognition not initialized");
            callback.onError("Speech recognition not initialized");
            return;
        }

        if (isRecording) {
            logger.warn("Recording already in progress");
            callback.onError("Recording already in progress");
            return;
        }

        Model model = voskModels.get(language);
        if (model == null) {
            logger.error("Language model not available: {}", language);
            callback.onError("Language model not available: " + language);
            return;
        }

        Recognizer recognizer = null;
        try {
            isRecording = true;
            logger.info("Starting real-time speech recognition for language: {}", language);
            callback.onRecognitionStarted();

            recognizer = new Recognizer(model, 16000);
            activeAudioService = new AudioService();
            Recognizer finalRecognizer = recognizer;

            activeAudioService.startRecording(new AudioService.AudioRecordingCallback() {
                @Override
                public void onRecordingStarted() {
                    System.out.println("Audio recording started");
                }

                @Override
                public void onAudioData(byte[] data, int length) {
                    if (data == null || length <= 0 || length > data.length || !isRecording) {
                        return;
                    }
                    try {
                        if (finalRecognizer.acceptWaveForm(data, length)) {
                            String result = finalRecognizer.getResult();
                            String extractedText = parseTextFromVoskResult(result);
                            callback.onPartialResult(extractedText);
                        }
                    } catch (Exception e) {
                        callback.onError("Error processing audio data: " + e.getMessage());
                    }
                }

                @Override
                public void onRecordingCompleted(byte[] audioData) {
                    try {
                        String finalResult = finalRecognizer.getFinalResult();
                        String extractedText = parseTextFromVoskResult(finalResult);
                        callback.onFinalResult(extractedText);

                        if (audioData != null && audioData.length > 0) {
                            File outputFile = new File("test_recognition.wav");
                            activeAudioService.saveAudioToFile(audioData, outputFile);
                        }
                    } finally {
                        isRecording = false;
                        activeAudioService = null;
                        finalRecognizer.close();
                    }
                }

                @Override
                public void onError(String error) {
                    try {
                        callback.onError(error);
                    } finally {
                        isRecording = false;
                        activeAudioService = null;
                        finalRecognizer.close();
                    }
                }
            });
        } catch (Exception e) {
            isRecording = false;
            activeAudioService = null;
            callback.onError("Recognition failed: " + e.getMessage());
            if (recognizer != null) {
                recognizer.close();
            }
        }
    }

    public void stopRealTimeRecognition() {
        if (isRecording) {
            isRecording = false;
            if (activeAudioService != null) {
                activeAudioService.stopRecording();
            }
            System.out.println("Stopping real-time speech recognition");
        }
    }

    public String recognizeFromFile(File audioFile) {
        return recognizeFromFile(audioFile, currentLanguage);
    }

    public String recognizeFromFile(File audioFile, String language) {
        if (!isInitialized) {
            logger.error("Speech recognition not initialized");
            return "";
        }

        if (audioFile == null || !audioFile.exists()) {
            logger.error("Audio file does not exist: {}", audioFile);
            return "";
        }

        Model model = voskModels.get(language);
        if (model == null) {
            logger.error("Language model not available: {}", language);
            return "";
        }

        try {
            logger.info("Recognizing speech from file: {} ({}) using language: {}",
                       audioFile.getName(), AudioOptimizer.getFileSizeString(audioFile), language);
            logger.info("Estimated processing time: {}", AudioOptimizer.estimateProcessingTime(audioFile));

            long startTime = System.currentTimeMillis();

            // Optimize audio file for faster processing
            File optimizedFile = AudioOptimizer.optimizeAudioFile(audioFile);
            if (optimizedFile != audioFile) {
                logger.info("Audio file optimized for faster processing");
            }

            // Use optimized audio file
            AudioInputStream ais = AudioSystem.getAudioInputStream(new BufferedInputStream(new FileInputStream(optimizedFile), 32768)); // Larger buffer

            // Convert to optimal format for Vosk (16kHz, 16-bit, mono)
            AudioFormat targetFormat = new AudioFormat(16000, 16, 1, true, false);
            if (!ais.getFormat().matches(targetFormat)) {
                logger.info("Converting audio format for optimal processing");
                ais = AudioSystem.getAudioInputStream(targetFormat, ais);
            }

            Recognizer recognizer = new Recognizer(model, 16000);

            // Use larger buffer for faster processing
            byte[] buffer = new byte[16384]; // Increased from 4096 to 16384
            int bytesRead;
            long totalBytesRead = 0;
            long fileSize = audioFile.length();

            while ((bytesRead = ais.read(buffer)) > 0) {
                recognizer.acceptWaveForm(buffer, bytesRead);
                totalBytesRead += bytesRead;

                // Optional: Log progress for large files
                if (fileSize > 1024 * 1024) { // Files larger than 1MB
                    int progress = (int) ((totalBytesRead * 100) / fileSize);
                    if (progress % 20 == 0) { // Log every 20%
                        logger.info("Processing progress: {}%", progress);
                    }
                }
            }

            String result = recognizer.getFinalResult();
            ais.close();
            recognizer.close();

            long endTime = System.currentTimeMillis();
            logger.info("File recognition completed in {}ms: {}", (endTime - startTime), result);

            // Parse JSON to extract only the text content
            return parseTextFromVoskResult(result);

        } catch (Exception e) {
            logger.error("File recognition failed: {}", e.getMessage(), e);
            ErrorHandler.handleFileError("recognize speech from", audioFile.getAbsolutePath(), e);
            return "";
        }
    }

    /**
     * Parse text from Vosk JSON result
     * Converts {"text": "what's your name"} to "what's your name"
     */
    private String parseTextFromVoskResult(String voskResult) {
        if (voskResult == null || voskResult.trim().isEmpty()) {
            return "";
        }

        try {
            // Try to parse as JSON
            if (voskResult.trim().startsWith("{") && voskResult.trim().endsWith("}")) {
                // Use simple string parsing to avoid adding JSON dependency
                String trimmed = voskResult.trim();

                // Look for "text" field in JSON
                int textIndex = trimmed.indexOf("\"text\"");
                if (textIndex != -1) {
                    // Find the value after "text":
                    int colonIndex = trimmed.indexOf(":", textIndex);
                    if (colonIndex != -1) {
                        // Find the opening quote
                        int startQuote = trimmed.indexOf("\"", colonIndex);
                        if (startQuote != -1) {
                            // Find the closing quote
                            int endQuote = trimmed.indexOf("\"", startQuote + 1);
                            if (endQuote != -1) {
                                String extractedText = trimmed.substring(startQuote + 1, endQuote);
                                logger.info("Extracted text from Vosk result: '{}'", extractedText);
                                return extractedText;
                            }
                        }
                    }
                }
            }

            // If JSON parsing fails, return the original result
            logger.warn("Could not parse Vosk result as JSON, returning original: {}", voskResult);
            return voskResult;

        } catch (Exception e) {
            logger.error("Error parsing Vosk result: {}", e.getMessage());
            return voskResult;
        }
    }

    public boolean isAvailable() {
        return isInitialized;
    }

    public boolean isRecording() {
        return isRecording;
    }

    public String[] getSupportedFormats() {
        return new String[]{"wav"};
    }

    public interface SpeechRecognitionCallback {
        void onRecognitionStarted();
        void onPartialResult(String partialText);
        void onFinalResult(String finalText);
        void onError(String error);
    }
}