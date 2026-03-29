package com.voicify.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.voicify.util.Dialog;
import io.github.cdimascio.dotenv.Dotenv;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.util.EntityUtils;
import java.io.IOException;
import javafx.scene.control.Alert.AlertType;

public class AITranslationService {
    private static final String GEMINI_API_URL = "https://generativelanguage.googleapis.com/v1beta/models/gemini-2.0-flash:generateContent";
    private static final String API_KEY;

    static {
        Dotenv dotenv = Dotenv.configure().directory(".").ignoreIfMissing().load();
        String geminiApi = dotenv.get("GEMINI_API");
        if (geminiApi != null && !geminiApi.trim().isEmpty()) {
            API_KEY = geminiApi;
        } else {
            API_KEY = dotenv.get("GEMINI_API_KEY", "YOUR_GEMINI_API_KEY");
        }
    }

    private final ObjectMapper objectMapper;
    private final CloseableHttpClient httpClient;

    public AITranslationService() {
        this.objectMapper = new ObjectMapper();
        this.httpClient = HttpClients.createDefault();
    }

    public String improveTranslation(String originalText, String currentTranslation) {
        if (API_KEY.equals("YOUR_GEMINI_API_KEY")) {
            Dialog.showAlert(AlertType.WARNING, "API Error", "Gemini API key not configured. Using mock translation.");
            return getMockImprovedTranslation(currentTranslation);
        }

        try {
            String prompt = buildImprovementPrompt(originalText, currentTranslation);
            String response = callGeminiAPI(prompt);
            String improvedTranslation = extractTranslationFromResponse(response);
            System.out.println("Translation improved successfully using Gemini AI");
            Dialog.showAlert(AlertType.INFORMATION, "Improvement Success", "Bản dịch đã được cải thiện bởi AI.");
            return improvedTranslation;
        } catch (Exception e) {
            Dialog.showAlert(AlertType.ERROR, "Improvement Error", "Failed to improve translation: " + e.getMessage());
            return getMockImprovedTranslation(currentTranslation);
        }
    }

    private String buildImprovementPrompt(String originalText, String currentTranslation) {
        return String.format("""
            You are a professional English-Vietnamese translator. Please improve the following Vietnamese translation to make it more natural, accurate, and culturally appropriate.

            Original English text: "%s"
            Current Vietnamese translation: "%s"

            Please provide an improved Vietnamese translation that:
            1. Maintains the original meaning accurately
            2. Uses natural Vietnamese expressions
            3. Is culturally appropriate for Vietnamese speakers
            4. Has proper grammar and flow

            Respond with only the improved Vietnamese translation, no additional text or explanations.
            """, originalText, currentTranslation);
    }

    private String callGeminiAPI(String prompt) throws IOException {
        HttpPost request = new HttpPost(GEMINI_API_URL + "?key=" + API_KEY);
        String requestBody = String.format("""
            {
                "contents": [{
                    "parts": [{
                        "text": "%s"
                    }]
                }],
                "generationConfig": {
                    "temperature": 0.3,
                    "topK": 40,
                    "topP": 0.95,
                    "maxOutputTokens": 1024
                }
            }
            """, prompt.replace("\"", "\\\"").replace("\n", "\\n"));

        request.setEntity(new StringEntity(requestBody, "UTF-8"));
        request.setHeader("Content-Type", "application/json");

        try (CloseableHttpResponse response = httpClient.execute(request)) {
            if (response.getStatusLine().getStatusCode() == 200) {
                return EntityUtils.toString(response.getEntity(), "UTF-8");
            } else {
                throw new IOException("Gemini API call failed with status: " + response.getStatusLine().getStatusCode());
            }
        }
    }

    private String extractTranslationFromResponse(String response) throws IOException {
        try {
            JsonNode root = objectMapper.readTree(response);
            JsonNode candidates = root.get("candidates");

            if (candidates != null && candidates.isArray() && candidates.size() > 0) {
                JsonNode firstCandidate = candidates.get(0);
                JsonNode content = firstCandidate.get("content");

                if (content != null) {
                    JsonNode parts = content.get("parts");
                    if (parts != null && parts.isArray() && parts.size() > 0) {
                        JsonNode firstPart = parts.get(0);
                        JsonNode text = firstPart.get("text");

                        if (text != null) {
                            return text.asText().trim();
                        }
                    }
                }
            }

            throw new IOException("Unable to extract translation from Gemini API response");
        } catch (Exception e) {
            Dialog.showAlert(AlertType.ERROR, "Parse Error", "Failed to parse Gemini API response: " + e.getMessage());
            throw new IOException("Failed to parse Gemini API response", e);
        }
    }

    private String getMockImprovedTranslation(String currentTranslation) {
        String improved = currentTranslation;
        improved = improved.replace("tôi", "tôi");
        improved = improved.replace("bạn", "bạn");
        if (!improved.contains("[Cải thiện bởi AI]")) {
            improved = improved + " [Cải thiện bởi AI - Demo]";
        }
        System.out.println("Returning mock improved translation");
        return improved;
    }

    public boolean isServiceAvailable() {
        return !API_KEY.equals("YOUR_GEMINI_API_KEY");
    }

    public void updateApiKey(String apiKey) {
        System.out.println("API key update requested - feature to be implemented");
    }

    public void close() {
        try {
            if (httpClient != null) {
                httpClient.close();
            }
        } catch (IOException e) {
            Dialog.showAlert(AlertType.ERROR, "Resource Error", "Failed to close HTTP client: " + e.getMessage());
        }
    }
}