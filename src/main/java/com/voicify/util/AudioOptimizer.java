package com.voicify.util;

import javax.sound.sampled.*;
import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * Utility class for optimizing audio files for faster speech recognition
 */
public class AudioOptimizer {
    
    private static final AudioFormat OPTIMAL_FORMAT = new AudioFormat(16000, 16, 1, true, false);
    
    /**
     * Optimize audio file for faster speech recognition
     * Converts to 16kHz, 16-bit, mono WAV format
     */
    public static File optimizeAudioFile(File inputFile) throws Exception {
        String fileName = inputFile.getName();
        String baseName = fileName.substring(0, fileName.lastIndexOf('.'));
        
        // Create temporary optimized file
        File optimizedFile = File.createTempFile("voicify_optimized_" + baseName, ".wav");
        optimizedFile.deleteOnExit();
        
        try (AudioInputStream originalStream = AudioSystem.getAudioInputStream(inputFile)) {
            AudioFormat originalFormat = originalStream.getFormat();
            
            // Check if already in optimal format
            if (isOptimalFormat(originalFormat)) {
                // Just copy the file
                Files.copy(inputFile.toPath(), optimizedFile.toPath(), 
                          java.nio.file.StandardCopyOption.REPLACE_EXISTING);
                return optimizedFile;
            }
            
            // Convert to optimal format
            try (AudioInputStream convertedStream = AudioSystem.getAudioInputStream(OPTIMAL_FORMAT, originalStream)) {
                AudioSystem.write(convertedStream, AudioFileFormat.Type.WAVE, optimizedFile);
            }
            
            System.out.println("Audio optimized: " + originalFormat + " -> " + OPTIMAL_FORMAT);
            return optimizedFile;
            
        } catch (Exception e) {
            // If optimization fails, return original file
            System.err.println("Audio optimization failed, using original file: " + e.getMessage());
            return inputFile;
        }
    }
    
    /**
     * Check if audio format is already optimal for speech recognition
     */
    private static boolean isOptimalFormat(AudioFormat format) {
        return format.getSampleRate() == 16000 &&
               format.getSampleSizeInBits() == 16 &&
               format.getChannels() == 1 &&
               format.isBigEndian() == false;
    }
    
    /**
     * Get file size in a human-readable format
     */
    public static String getFileSizeString(File file) {
        long bytes = file.length();
        if (bytes < 1024) return bytes + " B";
        if (bytes < 1024 * 1024) return (bytes / 1024) + " KB";
        return (bytes / (1024 * 1024)) + " MB";
    }
    
    /**
     * Estimate processing time based on file size
     */
    public static String estimateProcessingTime(File file) {
        long sizeInMB = file.length() / (1024 * 1024);
        if (sizeInMB < 1) return "< 5 seconds";
        if (sizeInMB < 5) return "5-15 seconds";
        if (sizeInMB < 10) return "15-30 seconds";
        return "30+ seconds";
    }
}
