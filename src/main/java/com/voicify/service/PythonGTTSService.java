package com.voicify.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

/**
 * Java service to integrate with Python gTTS (Google Text-to-Speech)
 * Provides high-quality text-to-speech functionality
 */
public class PythonGTTSService {
    private static final Logger logger = LoggerFactory.getLogger(PythonGTTSService.class);
    private static final ObjectMapper objectMapper = new ObjectMapper();
    
    private String pythonExecutable;
    private String gttsScriptPath;
    private boolean isInitialized = false;

    // Process control for stopping speech
    private volatile Process currentSpeechProcess = null;
    private volatile boolean shouldStop = false;
    
    public PythonGTTSService() {
        initialize();
    }
    
    private void initialize() {
        try {
            // Find Python executable
            pythonExecutable = findPythonExecutable();
            if (pythonExecutable == null) {
                logger.error("Python executable not found");
                return;
            }
            
            // Set gTTS script path
            gttsScriptPath = getGTTSScriptPath();
            if (!Files.exists(Paths.get(gttsScriptPath))) {
                logger.error("gTTS script not found at: {}", gttsScriptPath);
                return;
            }
            
            // Test Python and install dependencies if needed
            if (testPythonEnvironment()) {
                isInitialized = true;
                logger.info("Python gTTS service initialized successfully");
            }
            
        } catch (Exception e) {
            logger.error("Failed to initialize Python gTTS service: {}", e.getMessage(), e);
        }
    }
    
    private String findPythonExecutable() {
        String[] possiblePaths = {
            "python3",
            "python",
            "C:\\Python39\\python.exe",
            "C:\\Python310\\python.exe",
            "C:\\Python311\\python.exe",
            "C:\\Python312\\python.exe",
            "C:\\Users\\%USERNAME%\\AppData\\Local\\Programs\\Python\\Python39\\python.exe",
            "C:\\Users\\%USERNAME%\\AppData\\Local\\Programs\\Python\\Python310\\python.exe",
            "C:\\Users\\%USERNAME%\\AppData\\Local\\Programs\\Python\\Python311\\python.exe",
            "C:\\Users\\%USERNAME%\\AppData\\Local\\Programs\\Python\\Python312\\python.exe"
        };
        
        for (String path : possiblePaths) {
            if (testPythonPath(path)) {
                logger.info("Found Python executable: {}", path);
                return path;
            }
        }
        
        return null;
    }
    
    private boolean testPythonPath(String pythonPath) {
        try {
            ProcessBuilder pb = new ProcessBuilder(pythonPath, "--version");
            Process process = pb.start();
            boolean finished = process.waitFor(5, TimeUnit.SECONDS);
            
            if (finished && process.exitValue() == 0) {
                try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
                    String version = reader.readLine();
                    logger.debug("Python version: {}", version);
                    return true;
                }
            }
        } catch (Exception e) {
            logger.debug("Python path test failed for: {}", pythonPath);
        }
        return false;
    }
    
    private String getGTTSScriptPath() {
        // Try to find the script in resources
        try {
            Path resourcePath = Paths.get("src/main/resources/python/gtts_service.py");
            if (Files.exists(resourcePath)) {
                return resourcePath.toAbsolutePath().toString();
            }
            
            // Fallback to classpath resource
            return "./src/main/resources/python/gtts_service.py";
        } catch (Exception e) {
            logger.error("Error finding gTTS script path: {}", e.getMessage());
            return "./src/main/resources/python/gtts_service.py";
        }
    }
    
    private boolean testPythonEnvironment() {
        try {
            // Test if required packages are installed
            ProcessBuilder pb = new ProcessBuilder(pythonExecutable, gttsScriptPath, "--test");
            Process process = pb.start();
            
            boolean finished = process.waitFor(30, TimeUnit.SECONDS);
            if (!finished) {
                process.destroyForcibly();
                logger.error("Python test timed out");
                return false;
            }
            
            if (process.exitValue() != 0) {
                // Try to install dependencies
                logger.info("Installing Python dependencies...");
                return installDependencies();
            }
            
            return true;
            
        } catch (Exception e) {
            logger.error("Python environment test failed: {}", e.getMessage());
            return false;
        }
    }
    
    private boolean installDependencies() {
        try {
            String requirementsPath = "./src/main/resources/python/requirements.txt";
            
            logger.info("Installing Python dependencies from: {}", requirementsPath);
            ProcessBuilder pb = new ProcessBuilder(pythonExecutable, "-m", "pip", "install", "-r", requirementsPath);
            Process process = pb.start();
            
            // Log installation output
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    logger.info("pip: {}", line);
                }
            }
            
            boolean finished = process.waitFor(120, TimeUnit.SECONDS);
            if (!finished) {
                process.destroyForcibly();
                logger.error("Dependency installation timed out");
                return false;
            }
            
            if (process.exitValue() == 0) {
                logger.info("Python dependencies installed successfully");
                return true;
            } else {
                logger.error("Dependency installation failed with exit code: {}", process.exitValue());
                return false;
            }
            
        } catch (Exception e) {
            logger.error("Failed to install dependencies: {}", e.getMessage());
            return false;
        }
    }
    
    /**
     * Convert text to speech using gTTS
     */
    public CompletableFuture<Boolean> speak(String text, String language) {
        return CompletableFuture.supplyAsync(() -> {
            if (!isInitialized) {
                logger.error("gTTS service not initialized");
                return false;
            }
            
            if (text == null || text.trim().isEmpty()) {
                logger.warn("Empty text provided for TTS");
                return false;
            }
            
            try {
                // Reset stop flag
                shouldStop = false;

                logger.info("Converting text to speech: '{}' in language: {}", text.substring(0, Math.min(text.length(), 50)), language);

                List<String> command = new ArrayList<>();
                command.add(pythonExecutable);
                command.add(gttsScriptPath);
                command.add("--text");
                command.add(text);
                command.add("--lang");
                command.add(language != null ? language : "en");

                ProcessBuilder pb = new ProcessBuilder(command);
                Process process = pb.start();

                // Store current process for potential stopping
                currentSpeechProcess = process;
                
                // Read output
                StringBuilder output = new StringBuilder();
                try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
                    String line;
                    while ((line = reader.readLine()) != null && !shouldStop) {
                        output.append(line);
                    }
                }

                // Check if stopped by user
                if (shouldStop) {
                    logger.info("Speech stopped by user");
                    process.destroyForcibly();
                    currentSpeechProcess = null;
                    return false;
                }

                boolean finished = process.waitFor(30, TimeUnit.SECONDS);
                if (!finished) {
                    process.destroyForcibly();
                    logger.error("TTS process timed out");
                    currentSpeechProcess = null;
                    return false;
                }
                
                if (process.exitValue() == 0) {
                    // Parse JSON response
                    try {
                        JsonNode result = objectMapper.readTree(output.toString());
                        boolean success = result.get("success").asBoolean();
                        String message = result.get("message").asText();
                        
                        if (success) {
                            logger.info("TTS completed successfully: {}", message);
                        } else {
                            logger.error("TTS failed: {}", message);
                        }

                        // Reset current process
                        currentSpeechProcess = null;
                        return success;
                    } catch (Exception e) {
                        logger.error("Failed to parse TTS response: {}", e.getMessage());
                        return false;
                    }
                } else {
                    logger.error("TTS process failed with exit code: {}", process.exitValue());
                    currentSpeechProcess = null;
                    return false;
                }

            } catch (Exception e) {
                logger.error("TTS execution failed: {}", e.getMessage(), e);
                currentSpeechProcess = null;
                return false;
            }
        });
    }
    
    /**
     * Convert text to speech and save to file
     */
    public CompletableFuture<Boolean> saveToFile(String text, String language, String filePath) {
        return CompletableFuture.supplyAsync(() -> {
            if (!isInitialized) {
                logger.error("gTTS service not initialized");
                return false;
            }
            
            try {
                List<String> command = new ArrayList<>();
                command.add(pythonExecutable);
                command.add(gttsScriptPath);
                command.add("--text");
                command.add(text);
                command.add("--lang");
                command.add(language != null ? language : "en");
                command.add("--save");
                command.add(filePath);
                
                ProcessBuilder pb = new ProcessBuilder(command);
                Process process = pb.start();
                
                boolean finished = process.waitFor(30, TimeUnit.SECONDS);
                if (!finished) {
                    process.destroyForcibly();
                    return false;
                }
                
                return process.exitValue() == 0;
                
            } catch (Exception e) {
                logger.error("Save to file failed: {}", e.getMessage(), e);
                return false;
            }
        });
    }
    
    /**
     * Get supported languages
     */
    public Map<String, String> getSupportedLanguages() {
        if (!isInitialized) {
            return Map.of("en", "English");
        }
        
        try {
            ProcessBuilder pb = new ProcessBuilder(pythonExecutable, gttsScriptPath, "--languages");
            Process process = pb.start();
            
            StringBuilder output = new StringBuilder();
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    output.append(line);
                }
            }
            
            boolean finished = process.waitFor(10, TimeUnit.SECONDS);
            if (finished && process.exitValue() == 0) {
                JsonNode languages = objectMapper.readTree(output.toString());
                return objectMapper.convertValue(languages, Map.class);
            }
            
        } catch (Exception e) {
            logger.error("Failed to get supported languages: {}", e.getMessage());
        }
        
        // Fallback
        return Map.of(
            "en", "English",
            "vi", "Vietnamese",
            "fr", "French",
            "es", "Spanish",
            "de", "German"
        );
    }
    
    public boolean isInitialized() {
        return isInitialized;
    }

    /**
     * Stop current speech process
     */
    public void stopSpeaking() {
        logger.info("Stopping current speech...");
        shouldStop = true;

        if (currentSpeechProcess != null && currentSpeechProcess.isAlive()) {
            logger.info("Destroying speech process...");
            currentSpeechProcess.destroyForcibly();
            currentSpeechProcess = null;
        }
    }
}
