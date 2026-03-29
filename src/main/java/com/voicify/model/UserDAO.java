package com.voicify.model;

import java.sql.*;
import java.time.LocalDateTime;
import com.voicify.util.PasswordHasher;
import java.util.UUID;

public class UserDAO {
    
    public User authenticateUser(String username, String password) {
        String sql = "SELECT * FROM Users WHERE username = ?";
        
        try (Connection conn = DatabaseManager.getInstance().getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            
            stmt.setString(1, username);
            
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    String storedPasswordHash = rs.getString("password_hash");
                    
                    // Verify the password using secure verification
                    if (PasswordHasher.verifyPassword(password, storedPasswordHash)) {
                        return new User(
                            rs.getInt("user_id"),
                            rs.getString("username"),
                            rs.getString("email"),
                            rs.getString("password_hash"),
                            rs.getTimestamp("created_at").toLocalDateTime(),
                            rs.getString("verification_token"),
                            false  // Default to false since column doesn't exist
                        );
                    }
                }
            }
        } catch (SQLException e) {
            System.err.println("Authentication error: " + e.getMessage());
            throw new RuntimeException("Failed to authenticate user", e);
        }
        
        return null;
    }
    
    public boolean registerUser(User user) {
        // First check if the verification_token and is_verified columns exist
        try (Connection conn = DatabaseManager.getInstance().getConnection()) {
            DatabaseMetaData meta = conn.getMetaData();
            ResultSet rs = meta.getColumns(null, null, "Users", "verification_token");
            boolean hasVerificationToken = rs.next();
            rs.close();
            
            rs = meta.getColumns(null, null, "Users", "is_verified");
            boolean hasIsVerified = rs.next();
            rs.close();
            
            // Construct SQL based on available columns
            StringBuilder sql = new StringBuilder("INSERT INTO Users (username, email, password_hash");
            if (hasVerificationToken) sql.append(", verification_token");
            if (hasIsVerified) sql.append(", is_verified");
            sql.append(") VALUES (?, ?, ?");
            if (hasVerificationToken) sql.append(", ?");
            if (hasIsVerified) sql.append(", ?");
            sql.append(")");
            
            try (PreparedStatement stmt = conn.prepareStatement(sql.toString(), Statement.RETURN_GENERATED_KEYS)) {
                // Generate verification token
                String verificationToken = UUID.randomUUID().toString();
                
                // Hash the password
                String hashedPassword = PasswordHasher.hashPassword(user.getPasswordHash());
                
                stmt.setString(1, user.getUsername());
                stmt.setString(2, user.getEmail());
                stmt.setString(3, hashedPassword);
                
                int paramIndex = 4;
                if (hasVerificationToken) {
                    stmt.setString(paramIndex++, verificationToken);
                    user.setVerificationToken(verificationToken);
                }
                
                if (hasIsVerified) {
                    stmt.setBoolean(paramIndex, false); // Not verified initially
                }
                
                int affectedRows = stmt.executeUpdate();
                
                if (affectedRows > 0) {
                    try (ResultSet generatedKeys = stmt.getGeneratedKeys()) {
                        if (generatedKeys.next()) {
                            user.setUserId(generatedKeys.getInt(1));
                            return true;
                        }
                    }
                }
            }
        } catch (SQLException e) {
            System.err.println("Registration error: " + e.getMessage());
            e.printStackTrace();
        }
        return false;
    }
    
    public boolean isUsernameExists(String username) {
        String sql = "SELECT COUNT(*) FROM Users WHERE username = ?";
        
        try (Connection conn = DatabaseManager.getInstance().getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            
            stmt.setString(1, username);
            
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return rs.getInt(1) > 0;
                }
            }
        } catch (SQLException e) {
            System.err.println("Error checking username: " + e.getMessage());
        }
        
        return false;
    }
    
    public boolean isEmailExists(String email) {
        String sql = "SELECT COUNT(*) FROM Users WHERE email = ?";
        
        try (Connection conn = DatabaseManager.getInstance().getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            
            stmt.setString(1, email);
            
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return rs.getInt(1) > 0;
                }
            }
        } catch (SQLException e) {
            System.err.println("Error checking email: " + e.getMessage());
        }
        
        return false;
    }
    
    /**
     * Updates a user's password
     * @param email The user's email
     * @param newPassword The new password (will be hashed before storing)
     * @return true if the password was updated successfully, false otherwise
     */
    public boolean updatePassword(String email, String newPassword) {
        try (Connection conn = DatabaseManager.getInstance().getConnection()) {
            // Hash the password before storing it
            String hashedPassword = PasswordHasher.hashPassword(newPassword);
            
            String sql = "UPDATE Users SET password_hash = ? WHERE email = ?";
            try (PreparedStatement stmt = conn.prepareStatement(sql)) {
                stmt.setString(1, hashedPassword);
                stmt.setString(2, email);
                int rowsAffected = stmt.executeUpdate();
                return rowsAffected > 0;
            }
        } catch (SQLException e) {
            System.err.println("Error updating password: " + e.getMessage());
            e.printStackTrace();
            return false;
        }
    }

    public boolean verifyAccount(String token) {
        try (Connection conn = DatabaseManager.getInstance().getConnection()) {
            // Check if is_verified column exists
            DatabaseMetaData meta = conn.getMetaData();
            ResultSet rs = meta.getColumns(null, null, "Users", "is_verified");
            boolean hasIsVerified = rs.next();
            rs.close();
            
            if (!hasIsVerified) {
                // If column doesn't exist, we can't verify accounts
                System.err.println("Cannot verify account: is_verified column does not exist");
                return false;
            }
            
            String sql = "UPDATE Users SET is_verified = 1 WHERE verification_token = ?";
            
            try (PreparedStatement stmt = conn.prepareStatement(sql)) {
                stmt.setString(1, token);
                int rowsAffected = stmt.executeUpdate();
                return rowsAffected > 0;
            }
        } catch (SQLException e) {
            System.err.println("Verification error: " + e.getMessage());
            e.printStackTrace();
        }
        
        return false;
    }

    /**
     * Updates a user's verification status based on the verification code
     * @param email The user's email
     * @param verificationCode The verification code to check
     * @return true if verification was successful, false otherwise
     */
    public boolean verifyEmailWithCode(String email, String verificationCode) {
        try (Connection conn = DatabaseManager.getInstance().getConnection()) {
            // First check if the verification code matches
            String checkSql = "SELECT verification_token FROM Users WHERE email = ?";
            
            try (PreparedStatement checkStmt = conn.prepareStatement(checkSql)) {
                checkStmt.setString(1, email);
                
                try (ResultSet rs = checkStmt.executeQuery()) {
                    if (rs.next()) {
                        String storedCode = rs.getString("verification_token");
                        
                        if (storedCode != null && storedCode.equals(verificationCode)) {
                            // Code matches, update is_verified
                            String updateSql = "UPDATE Users SET is_verified = 1 WHERE email = ?";
                            
                            try (PreparedStatement updateStmt = conn.prepareStatement(updateSql)) {
                                updateStmt.setString(1, email);
                                int rowsAffected = updateStmt.executeUpdate();
                                return rowsAffected > 0;
                            }
                        }
                    }
                }
            }
        } catch (SQLException e) {
            System.err.println("Error verifying email: " + e.getMessage());
            e.printStackTrace();
        }
        
        return false;
    }

    /**
     * Stores a verification code for a user
     * @param email The user's email
     * @param verificationCode The verification code to store
     * @return true if the code was stored successfully, false otherwise
     */
    public boolean storeVerificationCode(String email, String verificationCode) {
        try (Connection conn = DatabaseManager.getInstance().getConnection()) {
            String sql = "UPDATE Users SET verification_token = ? WHERE email = ?";
            
            try (PreparedStatement stmt = conn.prepareStatement(sql)) {
                stmt.setString(1, verificationCode);
                stmt.setString(2, email);
                
                int rowsAffected = stmt.executeUpdate();
                return rowsAffected > 0;
            }
        } catch (SQLException e) {
            System.err.println("Error storing verification code: " + e.getMessage());
            e.printStackTrace();
        }
        
        return false;
    }
}
