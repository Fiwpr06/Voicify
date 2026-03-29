package com.voicify.util;

import com.voicify.config.ConfigurationManager;
import com.voicify.model.User;
import javax.mail.*;
import javax.mail.internet.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Properties;

public class EmailService {
    private static final Logger logger = LoggerFactory.getLogger(EmailService.class);
    private static final ConfigurationManager config = ConfigurationManager.getInstance();

    private static boolean isEmailEnabled() {
        return config.getBooleanProperty(ConfigurationManager.EMAIL_ENABLED, false);
    }

    public static boolean sendVerificationEmail(User user, String baseUrl) {
        if (!isEmailEnabled()) {
            // If email is disabled, just log the verification link and return success
            String verificationLink = baseUrl + "/verify?token=" + user.getVerificationToken();
            logger.info("Email sending is disabled. Verification link: {}", verificationLink);
            return true;
        }

        String verificationLink = baseUrl + "/verify?token=" + user.getVerificationToken();
        String htmlContent = buildVerificationEmailContent(verificationLink);

        return sendEmailWithAppPassword(user.getEmail(), "Verify Your Voicify Account", htmlContent);
    }

    /**
     * Send email using Gmail App Password for better security
     */
    public static boolean sendEmailWithAppPassword(String recipientEmail, String subject, String htmlContent) {
        final String username = config.getProperty(ConfigurationManager.EMAIL_USERNAME);
        final String appPassword = config.getProperty(ConfigurationManager.EMAIL_PASSWORD);

        if (username == null || username.isEmpty() || appPassword == null || appPassword.isEmpty()) {
            logger.error("Email credentials not configured properly");
            return false;
        }

        Properties props = new Properties();
        props.put("mail.smtp.auth", "true");
        props.put("mail.smtp.starttls.enable", "true");
        props.put("mail.smtp.starttls.required", "true");
        props.put("mail.smtp.host", config.getProperty(ConfigurationManager.EMAIL_HOST, "smtp.gmail.com"));
        props.put("mail.smtp.port", config.getProperty(ConfigurationManager.EMAIL_PORT, "587"));
        props.put("mail.smtp.ssl.trust", config.getProperty(ConfigurationManager.EMAIL_HOST, "smtp.gmail.com"));
        props.put("mail.smtp.ssl.protocols", "TLSv1.2");
        props.put("mail.debug", "true"); // Enable debug for troubleshooting

        Session session = Session.getInstance(props, new javax.mail.Authenticator() {
            protected PasswordAuthentication getPasswordAuthentication() {
                return new PasswordAuthentication(username, appPassword);
            }
        });

        try {
            // Test connection first
            logger.info("Testing SMTP connection...");
            Transport transport = session.getTransport("smtp");
            transport.connect(
                config.getProperty(ConfigurationManager.EMAIL_HOST, "smtp.gmail.com"),
                Integer.parseInt(config.getProperty(ConfigurationManager.EMAIL_PORT, "587")),
                username,
                appPassword
            );
            transport.close();
            logger.info("SMTP connection test successful");

            Message message = new MimeMessage(session);
            message.setFrom(new InternetAddress(config.getProperty(ConfigurationManager.EMAIL_FROM, username)));
            message.setRecipients(Message.RecipientType.TO, InternetAddress.parse(recipientEmail));
            message.setSubject(subject);
            message.setContent(htmlContent, "text/html; charset=utf-8");

            Transport.send(message);
            logger.info("Email sent successfully to: {}", recipientEmail);
            return true;
        } catch (MessagingException e) {
            logger.error("Error sending email to {}: {}", recipientEmail, e.getMessage(), e);
            // Log more detailed error information
            if (e.getMessage().contains("Authentication")) {
                logger.error("Authentication failed. Please check email credentials.");
            } else if (e.getMessage().contains("Connection")) {
                logger.error("Connection failed. Please check SMTP settings.");
            }
            return false;
        }
    }

    private static String buildVerificationEmailContent(String verificationLink) {
        return "<html>" +
                "<body style='font-family: Arial, sans-serif;'>" +
                "<div style='max-width: 600px; margin: 0 auto; padding: 20px; border: 1px solid #e0e0e0;'>" +
                "<h2 style='color: #e89300;'>Welcome to Voicify!</h2>" +
                "<p>Thank you for registering. Please verify your account by clicking the link below:</p>" +
                "<p><a href='" + verificationLink + "' style='display: inline-block; padding: 10px 20px; background-color: #e89300; color: white; text-decoration: none; border-radius: 5px;'>Verify Account</a></p>" +
                "<p>If the button doesn't work, copy and paste this link into your browser:</p>" +
                "<p>" + verificationLink + "</p>" +
                "<p>This link will expire in 24 hours.</p>" +
                "<p>Best regards,<br>The Voicify Team</p>" +
                "</div>" +
                "</body>" +
                "</html>";
    }

    public static boolean sendPasswordResetEmail(String email, String verificationCode) {
        if (!isEmailEnabled()) {
            // If email is disabled, just log the verification code and return success
            logger.info("Email sending is disabled. Password reset code for {}: {}", email, verificationCode);
            return true;
        }

        String htmlContent = buildPasswordResetEmailContent(verificationCode);
        return sendEmailWithAppPassword(email, "Voicify Password Reset", htmlContent);
    }

    private static String buildPasswordResetEmailContent(String verificationCode) {
        return "<html>" +
                "<body style='font-family: Arial, sans-serif;'>" +
                "<div style='max-width: 600px; margin: 0 auto; padding: 20px; border: 1px solid #e0e0e0;'>" +
                "<h2 style='color: #e89300;'>Voicify Password Reset</h2>" +
                "<p>You requested a password reset. Use the verification code below:</p>" +
                "<div style='background-color: #f5f5f5; padding: 15px; text-align: center; font-size: 24px; letter-spacing: 5px;'>" +
                verificationCode +
                "</div>" +
                "<p>This code will expire in 15 minutes.</p>" +
                "<p>If you didn't request this password reset, please ignore this email.</p>" +
                "<p>Best regards,<br>The Voicify Team</p>" +
                "</div>" +
                "</body>" +
                "</html>";
    }

    /**
     * Sends a verification code to the user's email address
     *
     * @param email The recipient's email address
     * @param verificationCode The verification code to send
     * @return true if the email was sent successfully, false otherwise
     */
    public static boolean sendVerificationCode(String email, String verificationCode) {
        if (!isEmailEnabled()) {
            // If email is disabled, just log the verification code and return success
            logger.info("Email sending is disabled. Verification code for {}: {}", email, verificationCode);
            return true;
        }

        String htmlContent = buildVerificationCodeEmailContent(verificationCode);
        return sendEmailWithAppPassword(email, "Voicify Account Verification Code", htmlContent);
    }

    private static String buildVerificationCodeEmailContent(String verificationCode) {
        return "<html>" +
                "<body style='font-family: Arial, sans-serif;'>" +
                "<div style='max-width: 600px; margin: 0 auto; padding: 20px; border: 1px solid #e0e0e0;'>" +
                "<h2 style='color: #e89300;'>Voicify Account Verification</h2>" +
                "<p>Thank you for registering. Please use the verification code below to verify your account:</p>" +
                "<div style='background-color: #f5f5f5; padding: 15px; text-align: center; font-size: 24px; letter-spacing: 5px;'>" +
                verificationCode +
                "</div>" +
                "<p>This code will expire in 24 hours.</p>" +
                "<p>If you didn't request this verification code, please ignore this email.</p>" +
                "<p>Best regards,<br>The Voicify Team</p>" +
                "</div>" +
                "</body>" +
                "</html>";
    }

    /**
     * Test email configuration and connection
     */
    public static boolean testEmailConfiguration() {
        if (!isEmailEnabled()) {
            logger.info("Email is disabled in configuration");
            return true; // Consider this as "working" since it's intentionally disabled
        }

        final String username = config.getProperty(ConfigurationManager.EMAIL_USERNAME);
        final String appPassword = config.getProperty(ConfigurationManager.EMAIL_PASSWORD);

        if (username == null || username.isEmpty() || appPassword == null || appPassword.isEmpty()) {
            logger.error("Email credentials not configured properly");
            return false;
        }

        Properties props = new Properties();
        props.put("mail.smtp.auth", "true");
        props.put("mail.smtp.starttls.enable", "true");
        props.put("mail.smtp.starttls.required", "true");
        props.put("mail.smtp.host", config.getProperty(ConfigurationManager.EMAIL_HOST, "smtp.gmail.com"));
        props.put("mail.smtp.port", config.getProperty(ConfigurationManager.EMAIL_PORT, "587"));
        props.put("mail.smtp.ssl.trust", config.getProperty(ConfigurationManager.EMAIL_HOST, "smtp.gmail.com"));
        props.put("mail.smtp.ssl.protocols", "TLSv1.2");

        Session session = Session.getInstance(props, new javax.mail.Authenticator() {
            protected PasswordAuthentication getPasswordAuthentication() {
                return new PasswordAuthentication(username, appPassword);
            }
        });

        try {
            logger.info("Testing email configuration...");
            logger.info("SMTP Host: {}", config.getProperty(ConfigurationManager.EMAIL_HOST, "smtp.gmail.com"));
            logger.info("SMTP Port: {}", config.getProperty(ConfigurationManager.EMAIL_PORT, "587"));
            logger.info("Username: {}", username);

            Transport transport = session.getTransport("smtp");
            transport.connect(
                config.getProperty(ConfigurationManager.EMAIL_HOST, "smtp.gmail.com"),
                Integer.parseInt(config.getProperty(ConfigurationManager.EMAIL_PORT, "587")),
                username,
                appPassword
            );
            transport.close();
            logger.info("Email configuration test successful");
            return true;
        } catch (MessagingException e) {
            logger.error("Email configuration test failed: {}", e.getMessage(), e);
            return false;
        }
    }
}
