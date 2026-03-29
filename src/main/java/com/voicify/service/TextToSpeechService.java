package com.voicify.service;

import com.voicify.config.ConfigurationManager;
import com.voicify.util.Dialog;
import com.voicify.util.ErrorHandler;
import javax.sound.sampled.*;
import java.io.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.ExecutionException;
import javafx.scene.control.Alert.AlertType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class TextToSpeechService {
    private static final Logger logger = LoggerFactory.getLogger(TextToSpeechService.class);
    private boolean isInitialized = false;
    private final ConfigurationManager config;

    // gTTS integration - only TTS engine we use now
    private PythonGTTSService gttsService;

    // Speech state management
    private final AtomicBoolean isSpeaking = new AtomicBoolean(false);
    private final AtomicBoolean isEnglishSpeaking = new AtomicBoolean(false);
    private final AtomicBoolean isVietnameseSpeaking = new AtomicBoolean(false);
    private final ExecutorService speechExecutor = Executors.newSingleThreadExecutor();
    private Future<?> currentSpeechTask = null;

    // Speech control
    private volatile boolean shouldStop = false;

    public TextToSpeechService() {
        this.config = ConfigurationManager.getInstance();
        initializeGTTS();
    }

    private void initializeGTTS() {
        try {
            logger.info("Initializing gTTS (Google Text-to-Speech) service...");
            gttsService = new PythonGTTSService();
            if (gttsService.isInitialized()) {
                logger.info("gTTS service initialized successfully - high-quality TTS ready");
                isInitialized = true;
            } else {
                logger.error("gTTS service failed to initialize");
                isInitialized = false;
            }
        } catch (Exception e) {
            logger.error("Failed to initialize gTTS service: {}", e.getMessage(), e);
            isInitialized = false;
        }
    }


    /**
     * Check if any speech is currently active
     */
    public boolean isSpeaking() {
        return isSpeaking.get();
    }

    /**
     * Check if English speech is currently active
     */
    public boolean isEnglishSpeaking() {
        return isEnglishSpeaking.get();
    }

    /**
     * Check if Vietnamese speech is currently active
     */
    public boolean isVietnameseSpeaking() {
        return isVietnameseSpeaking.get();
    }

    /**
     * Stop current speech
     */
    public void stopSpeaking() {
        logger.info("Stopping current speech...");
        shouldStop = true;

        if (currentSpeechTask != null && !currentSpeechTask.isDone()) {
            currentSpeechTask.cancel(true);
        }

        // Reset all states
        isSpeaking.set(false);
        isEnglishSpeaking.set(false);
        isVietnameseSpeaking.set(false);

        // Stop gTTS if it's speaking
        if (gttsService != null) {
            gttsService.stopSpeaking();
        }
    }

    /**
     * Speak text using default language (Vietnamese)
     */
    public void speak(String text) {
        speak(text, "vi");
    }

    /**
     * Speak text in specified language using gTTS with queue management
     */
    public void speak(String text, String language) {
        if (!isInitialized) {
            logger.warn("gTTS service not initialized");
            ErrorHandler.showWarning("TTS Error",
                "Hệ thống Text-to-Speech chưa sẵn sàng.\n" +
                "Vui lòng chạy setup_python_tts.bat để cài đặt gTTS.");
            return;
        }

        if (text == null || text.trim().isEmpty()) {
            logger.warn("Empty text provided for speech synthesis");
            ErrorHandler.showWarning("TTS Error", "Vui lòng nhập văn bản để phát!");
            return;
        }

        // If already speaking, queue this request
        if (isSpeaking.get()) {
            logger.info("Speech in progress, queuing new request...");
            currentSpeechTask = speechExecutor.submit(() -> {
                try {
                    // Wait for current speech to finish
                    while (isSpeaking.get() && !Thread.currentThread().isInterrupted()) {
                        Thread.sleep(100);
                    }

                    if (!Thread.currentThread().isInterrupted()) {
                        speakInternal(text, language);
                    }
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    logger.info("Speech task interrupted");
                }
            });
        } else {
            // Speak immediately
            currentSpeechTask = speechExecutor.submit(() -> speakInternal(text, language));
        }
    }

    /**
     * Internal method to handle actual speech synthesis
     */
    private void speakInternal(String text, String language) {
        try {
            // Set speaking state
            isSpeaking.set(true);
            if ("en".equals(language)) {
                isEnglishSpeaking.set(true);
            } else {
                isVietnameseSpeaking.set(true);
            }

            shouldStop = false;

            logger.info("Speaking text with gTTS: '{}' in language: {}",
                       text.substring(0, Math.min(text.length(), 50)), language);

            CompletableFuture<Boolean> speechFuture = gttsService.speak(text, language);

            // Add timeout and cancellation support
            try {
                Boolean success = speechFuture.get(60, java.util.concurrent.TimeUnit.SECONDS); // Wait for completion with timeout

                if (success && !shouldStop) {
                    logger.info("gTTS speech synthesis completed successfully");
                } else if (!shouldStop) {
                    logger.error("gTTS speech synthesis failed");
                    ErrorHandler.showWarning("TTS Error",
                        "Không thể phát giọng nói.\n" +
                        "Vui lòng kiểm tra kết nối internet và cài đặt gTTS.");
                }
            } catch (java.util.concurrent.TimeoutException e) {
                logger.error("Speech synthesis timed out: {}", e.getMessage());
                if (!shouldStop) {
                    ErrorHandler.showWarning("TTS Error", "Quá thời gian chờ phát giọng nói.");
                }
            } catch (java.util.concurrent.ExecutionException e) {
                logger.error("Speech synthesis execution error: {}", e.getMessage(), e);
                if (!shouldStop) {
                    ErrorHandler.showWarning("TTS Error", "Lỗi thực thi Text-to-Speech: " + e.getMessage());
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                logger.info("Speech synthesis interrupted");
            }

        } catch (Exception e) {
            logger.error("Speech synthesis error: {}", e.getMessage(), e);
            if (!shouldStop) {
                ErrorHandler.showWarning("TTS Error", "Lỗi Text-to-Speech: " + e.getMessage());
            }
        } finally {
            // Reset speaking states
            isSpeaking.set(false);
            isEnglishSpeaking.set(false);
            isVietnameseSpeaking.set(false);
            logger.info("Speech synthesis completed, states reset");
        }
    }

    /**
     * Toggle English speech - start if not speaking, stop if speaking
     */
    public void toggleEnglishSpeech(String text) {
        if (isEnglishSpeaking.get()) {
            logger.info("Stopping English speech");
            stopSpeaking();
        } else {
            logger.info("Starting English speech");
            speak(text, "en");
        }
    }

    /**
     * Toggle Vietnamese speech - start if not speaking, stop if speaking
     */
    public void toggleVietnameseSpeech(String text) {
        if (isVietnameseSpeaking.get()) {
            logger.info("Stopping Vietnamese speech");
            stopSpeaking();
        } else {
            logger.info("Starting Vietnamese speech");
            speak(text, "vi");
        }
    }

    /**
     * Legacy method - speak English (for backward compatibility)
     */
    public void speakEnglish(String text) {
        speak(text, "en");
    }

    /**
     * Legacy method - speak Vietnamese (for backward compatibility)
     */
    public void speakVietnamese(String text) {
        speak(text, "vi");
    }


    public void speakToFile(String text, File outputFile) {
        speakToFile(text, outputFile, "vi");
    }

    public void speakToFile(String text, File outputFile, String language) {
        if (!isInitialized) {
            logger.warn("gTTS service not initialized");
            ErrorHandler.showWarning("TTS Error",
                "Hệ thống Text-to-Speech chưa sẵn sàng.\n" +
                "Vui lòng chạy setup_python_tts.bat để cài đặt gTTS.");
            return;
        }

        if (text == null || text.trim().isEmpty()) {
            logger.warn("Empty text provided for speech synthesis");
            ErrorHandler.showWarning("TTS Error", "Vui lòng nhập văn bản để lưu!");
            return;
        }

        logger.info("Saving speech to file: {} with language: {}", outputFile.getName(), language);

        gttsService.saveToFile(text, language, outputFile.getAbsolutePath()).thenAccept(success -> {
            if (success) {
                logger.info("Speech saved to file successfully: {}", outputFile.getAbsolutePath());
                ErrorHandler.showInfo("TTS Success", "Đã lưu file âm thanh: " + outputFile.getName());
            } else {
                logger.error("Failed to save speech to file");
                ErrorHandler.showWarning("TTS Error", "Không thể lưu file âm thanh.");
            }
        }).exceptionally(throwable -> {
            logger.error("Error saving speech to file: {}", throwable.getMessage());
            ErrorHandler.handleError("TTS Error", "Lỗi khi lưu file âm thanh", (Exception) throwable);
            return null;
        });
    }

    public boolean isAvailable() {
        return isInitialized && gttsService != null && gttsService.isInitialized();
    }

    public String[] getSupportedLanguages() {
        if (gttsService != null) {
            return gttsService.getSupportedLanguages().keySet().toArray(new String[0]);
        }
        return new String[]{"en", "vi"};
    }

    public java.util.Map<String, String> getSupportedLanguagesMap() {
        if (gttsService != null) {
            return gttsService.getSupportedLanguages();
        }
        return java.util.Map.of("en", "English", "vi", "Vietnamese");
    }

    /**
     * Cleanup resources when service is no longer needed
     */
    public void cleanup() {
        logger.info("Cleaning up TextToSpeechService...");
        stopSpeaking();

        if (speechExecutor != null && !speechExecutor.isShutdown()) {
            speechExecutor.shutdown();
        }

        // Note: gTTS service doesn't need explicit cleanup
    }
}
