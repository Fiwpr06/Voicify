package com.voicify.model;

import java.time.LocalDateTime;

public class TranslationHistory {
    private int id;
    private int userId;
    private String sourceText;
    private String translatedText;
    private String translationType;
    private String audioFilePath;
    private LocalDateTime createdAt;
    private boolean improvedByAi;
    private String aiTranslationText;
    
    public TranslationHistory(int id, String sourceText, String translatedText, String translationType, String audioFilePath, LocalDateTime createdAt, boolean improvedByAi, String aiTranslationText) {
        this.createdAt = LocalDateTime.now();
        this.improvedByAi = false;
    }
    
    public TranslationHistory(int userId, String sourceText, String translatedText, String translationType, String audioFilePath) {
        this.userId = userId;
        this.sourceText = sourceText;
        this.translatedText = translatedText;
        this.translationType = translationType;
        this.audioFilePath = audioFilePath;
        this.createdAt = LocalDateTime.now();
        this.improvedByAi = false;
    }
    
    public TranslationHistory(int id, int userId, String sourceText, String translatedText, 
                             String translationType, String audioFilePath, 
                             LocalDateTime createdAt, boolean improvedByAi, String aiTranslationText) {
        this.id = id;
        this.userId = userId;
        this.sourceText = sourceText;
        this.translatedText = translatedText;
        this.translationType = translationType;
        this.audioFilePath = audioFilePath;
        this.createdAt = createdAt;
        this.improvedByAi = improvedByAi;
        this.aiTranslationText = aiTranslationText;
    }
    
    // Getters and setters
    public int getId() {
        return id;
    }
    
    public void setId(int id) {
        this.id = id;
    }
    
    public int getUserId() {
        return userId;
    }
    
    public void setUserId(int userId) {
        this.userId = userId;
    }
    
    public String getSourceText() {
        return sourceText;
    }
    
    public void setSourceText(String sourceText) {
        this.sourceText = sourceText;
    }
    
    public String getTranslatedText() {
        return translatedText;
    }
    
    public void setTranslatedText(String translatedText) {
        this.translatedText = translatedText;
    }
    
    public String getTranslationType() {
        return translationType;
    }
    
    public void setTranslationType(String translationType) {
        this.translationType = translationType;
    }
    
    public String getAudioFilePath() {
        return audioFilePath;
    }
    
    public void setAudioFilePath(String audioFilePath) {
        this.audioFilePath = audioFilePath;
    }
    
    public LocalDateTime getCreatedAt() {
        return createdAt;
    }
    
    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }
    
    public boolean isImprovedByAi() {
        return improvedByAi;
    }
    
    public void setImprovedByAi(boolean improvedByAi) {
        this.improvedByAi = improvedByAi;
    }
    
    public String getAiTranslationText() {
        return aiTranslationText;
    }
    
    public void setAiTranslationText(String aiTranslationText) {
        this.aiTranslationText = aiTranslationText;
    }
    
    @Override
    public String toString() {
        return "TranslationHistory{" +
                "id=" + id +
                ", userId=" + userId +
                ", sourceText='" + sourceText + '\'' +
                ", translatedText='" + translatedText + '\'' +
                ", translationType='" + translationType + '\'' +
                ", createdAt=" + createdAt +
                '}';
    }
}