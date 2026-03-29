package com.voicify.model;

import com.voicify.util.SessionManager;

import java.sql.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

public class TranslationHistoryDAO {
    private final DatabaseManager databaseManager;

    public TranslationHistoryDAO() {
        this.databaseManager = DatabaseManager.getInstance();
    }

    public void save(TranslationHistory history) {
        String sql = "INSERT INTO TranslationHistory (user_id, source_text, translated_text, translation_type, audio_file_path, improved_by_ai, ai_translation_text) VALUES (?, ?, ?, ?, ?, ?, ?)";
        
        try (Connection conn = DatabaseManager.getInstance().getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            
            stmt.setInt(1, history.getUserId());
            stmt.setString(2, history.getSourceText());
            stmt.setString(3, history.getTranslatedText());
            stmt.setString(4, history.getTranslationType());
            stmt.setString(5, history.getAudioFilePath());
            stmt.setBoolean(6, history.isImprovedByAi());
            stmt.setString(7, history.getAiTranslationText());
            
            int affectedRows = stmt.executeUpdate();
            
            if (affectedRows > 0) {
                try (ResultSet rs = stmt.getGeneratedKeys()) {
                    if (rs.next()) {
                        history.setId(rs.getInt(1));
                    }
                }
            }
        } catch (SQLException e) {
            System.err.println("Error saving translation history: " + e.getMessage());
            throw new RuntimeException("Failed to save translation history", e);
        }
    }

    public List<TranslationHistory> findAll() {
        List<TranslationHistory> historyList = new ArrayList<>();
        Integer userId = resolveCurrentUserId();
        if (userId == null) {
            return historyList;
        }
        
        String sql = "SELECT * FROM TranslationHistory WHERE user_id = ? ORDER BY created_at DESC";
        
        try (Connection conn = DatabaseManager.getInstance().getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            
            stmt.setInt(1, userId);
            
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    TranslationHistory history = new TranslationHistory(
                        rs.getInt("id"),
                        rs.getInt("user_id"),
                        rs.getString("source_text"),
                        rs.getString("translated_text"),
                        rs.getString("translation_type"),
                        rs.getString("audio_file_path"),
                        rs.getTimestamp("created_at").toLocalDateTime(),
                        rs.getBoolean("improved_by_ai"),
                        rs.getString("ai_translation_text")
                    );
                    historyList.add(history);
                }
            }
        } catch (SQLException e) {
            System.err.println("Error retrieving translation history: " + e.getMessage());
            throw new RuntimeException("Failed to retrieve translation history", e);
        }
        
        return historyList;
    }

    public List<TranslationHistory> searchByKeyword(String keyword) {
        List<TranslationHistory> historyList = new ArrayList<>();
        Integer userId = resolveCurrentUserId();
        if (userId == null) {
            return historyList;
        }
        
        String sql = "SELECT * FROM TranslationHistory WHERE user_id = ? AND " +
                    "(source_text LIKE ? OR translated_text LIKE ? OR ai_translation_text LIKE ?) " +
                    "ORDER BY created_at DESC";
        
        try (Connection conn = DatabaseManager.getInstance().getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            
            String searchPattern = "%" + keyword + "%";
            stmt.setInt(1, userId);
            stmt.setString(2, searchPattern);
            stmt.setString(3, searchPattern);
            stmt.setString(4, searchPattern);
            
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    TranslationHistory history = new TranslationHistory(
                        rs.getInt("id"),
                        rs.getInt("user_id"),
                        rs.getString("source_text"),
                        rs.getString("translated_text"),
                        rs.getString("translation_type"),
                        rs.getString("audio_file_path"),
                        rs.getTimestamp("created_at").toLocalDateTime(),
                        rs.getBoolean("improved_by_ai"),
                        rs.getString("ai_translation_text")
                    );
                    historyList.add(history);
                }
            }
        } catch (SQLException e) {
            System.err.println("Error searching translation history: " + e.getMessage());
            throw new RuntimeException("Failed to search translation history", e);
        }
        
        return historyList;
    }

    public void delete(int id) {
        String sql = "DELETE FROM TranslationHistory WHERE id = ?";
        
        try (Connection conn = DatabaseManager.getInstance().getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            
            stmt.setInt(1, id);
            stmt.executeUpdate();
            
        } catch (SQLException e) {
            System.err.println("Error deleting translation history: " + e.getMessage());
            throw new RuntimeException("Failed to delete translation history", e);
        }
    }

    private Integer resolveCurrentUserId() {
        if (SessionManager.getInstance().getCurrentUser() == null) {
            return null;
        }
        return SessionManager.getInstance().getCurrentUser().getUserId();
    }
}
