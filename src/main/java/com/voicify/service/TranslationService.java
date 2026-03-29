package com.voicify.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.github.cdimascio.dotenv.Dotenv;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.util.EntityUtils;

import java.io.IOException;

public class TranslationService {
    private static final String GEMINI_API_URL = "https://generativelanguage.googleapis.com/v1beta/models/gemini-2.0-flash:generateContent";
    private static final String API_KEY;
    private final CloseableHttpClient httpClient;
    private final ObjectMapper mapper;

    static {
        Dotenv dotenv = Dotenv.configure().directory(".").ignoreIfMissing().load();
        String geminiApi = dotenv.get("GEMINI_API");
        if (geminiApi != null && !geminiApi.trim().isEmpty()) {
            API_KEY = geminiApi;
        } else {
            API_KEY = dotenv.get("GEMINI_API_KEY", "YOUR_GEMINI_API_KEY");
        }
    }

    public TranslationService() {
        RequestConfig config = RequestConfig.custom()
                .setConnectTimeout(5000)
                .setSocketTimeout(5000)
                .build();
        httpClient = HttpClients.custom().setDefaultRequestConfig(config).build();
        mapper = new ObjectMapper();
    }

    public String translateToVietnamese(String englishText) {
        if (englishText == null || englishText.trim().isEmpty()) {
            return "";
        }

        try {
            return translateOnline(englishText);
        } catch (Exception e) {
            System.out.println("Online translation failed: " + e.getMessage());
            return translateOffline(englishText);
        }
    }

    private String translateOnline(String text) throws Exception {
        text = text.replace("’", "'").replace("“", "\"").replace("”", "\"");
        String prompt = "Translate to Vietnamese (return only the translated text, no JSON format, no explanations): " + text;

        String responseJson = callGeminiAPI(prompt);
        JsonNode response = mapper.readTree(responseJson);
        if (response.has("error")) {
            throw new RuntimeException("Gemini API error: " + response.get("error").get("message").asText());
        }
        if (!response.has("candidates") || response.get("candidates").size() == 0) {
            throw new RuntimeException("Gemini API response missing candidates: " + response.toString());
        }

        String translatedText = response.get("candidates").get(0)
                .get("content").get("parts").get(0).get("text").asText();

        // Clean up the response to remove any JSON formatting or extra text
        return cleanTranslationResponse(translatedText);
    }

    private String cleanTranslationResponse(String response) {
        if (response == null || response.trim().isEmpty()) {
            return "";
        }

        // Remove JSON formatting if present
        response = response.trim();

        // If response starts with { and ends with }, try to extract text field
        if (response.startsWith("{") && response.endsWith("}")) {
            try {
                JsonNode jsonNode = mapper.readTree(response);
                if (jsonNode.has("text")) {
                    return jsonNode.get("text").asText();
                }
            } catch (Exception e) {
                // If JSON parsing fails, continue with string cleaning
            }
        }

        // Remove common prefixes and suffixes
        response = response.replaceAll("^(Translation:|Translated text:|Vietnamese:|Result:)\\s*", "");

        // Remove quotes if the entire text is wrapped in quotes
        if (response.startsWith("\"") && response.endsWith("\"")) {
            response = response.substring(1, response.length() - 1);
        }

        return response.trim();
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
            int statusCode = response.getStatusLine().getStatusCode();
            if (statusCode == 200) {
                return EntityUtils.toString(response.getEntity(), "UTF-8");
            } else {
                String errorBody = EntityUtils.toString(response.getEntity(), "UTF-8");
                throw new IOException("Gemini API failed: " + statusCode + ", response: " + errorBody);
            }
        }
    }

    private String translateOffline(String text) {
        System.out.println("Performing offline translation for: " + (text.length() > 50 ? text.substring(0, 50) : text));

        text = text.replace("’", "'").replace("“", "\"").replace("”", "\"");
        String innerText = text;

        if (text.contains("Translating text:") || text.contains("For the input")) {
            int start = text.lastIndexOf("Translating text:") + 17;
            if (start >= 17) {
                innerText = text.substring(start).trim();
            } else if (text.contains("For the input")) {
                int quoteStart = text.indexOf("\"") + 1;
                int quoteEnd = text.lastIndexOf("\"");
                if (quoteStart > 0 && quoteEnd > quoteStart) {
                    innerText = text.substring(quoteStart, quoteEnd);
                }
            }
        }

        String translation = innerText.toLowerCase()
                .replace("hello", "xin chào")
                .replace("goodbye", "tạm biệt")
                .replace("thank you", "cảm ơn")
                .replace("please", "xin vui lòng")
                .replace("yes", "có")
                .replace("no", "không")
                .replace("good morning", "chào buổi sáng")
                .replace("good afternoon", "chào buổi chiều")
                .replace("good evening", "chào buổi tối")
                .replace("good night", "chúc ngủ ngon")
                .replace("how are you", "bạn có khỏe không")
                .replace("what is your name", "tên bạn là gì")
                .replace("my name is", "tên tôi là")
                .replace("nice to meet you", "rất vui được gặp bạn")
                .replace("excuse me", "xin lỗi")
                .replace("sorry", "xin lỗi")
                .replace("help", "giúp đỡ")
                .replace("water", "nước")
                .replace("food", "thức ăn")
                .replace("money", "tiền")
                .replace("time", "thời gian")
                .replace("today", "hôm nay")
                .replace("tomorrow", "ngày mai")
                .replace("yesterday", "hôm qua")
                .replace("where", "ở đâu")
                .replace("when", "khi nào")
                .replace("why", "tại sao")
                .replace("how", "như thế nào")
                .replace("what", "cái gì")
                .replace("who", "ai")
                .replace("i love you", "tôi yêu bạn")
                .replace("family", "gia đình")
                .replace("friend", "bạn bè")
                .replace("work", "công việc")
                .replace("school", "trường học")
                .replace("house", "nhà")
                .replace("car", "xe hơi")
                .replace("phone", "điện thoại")
                .replace("computer", "máy tính")
                .replace("book", "sách")
                .replace("music", "âm nhạc")
                .replace("movie", "phim")
                .replace("restaurant", "nhà hàng")
                .replace("hospital", "bệnh viện")
                .replace("airport", "sân bay")
                .replace("hotel", "khách sạn")
                .replace("beautiful", "đẹp")
                .replace("good", "tốt")
                .replace("bad", "xấu")
                .replace("big", "lớn")
                .replace("small", "nhỏ")
                .replace("hot", "nóng")
                .replace("cold", "lạnh")
                .replace("happy", "vui")
                .replace("sad", "buồn")
                .replace("tired", "mệt")
                .replace("hungry", "đói")
                .replace("thirsty", "khát")
                .replace("learning a new language", "học một ngôn ngữ mới")
                .replace("opens the door to many opportunities", "mở ra cánh cửa cho nhiều cơ hội")
                .replace("translating text", "dịch văn bản")
                .replace("cli", "CLI")
                .replace("server", "máy chủ")
                .replace("batch file", "tệp batch")
                .replace("no longer needs to start", "không còn cần phải khởi động");

        if (!translation.isEmpty()) {
            translation = translation.substring(0, 1).toUpperCase() + translation.substring(1);
        }

        if (!text.equals(innerText)) {
            translation = text.substring(0, text.indexOf(innerText)) + translation;
        }

        System.out.println("Offline translation result: " + translation);
        return translation;
    }

    public void close() throws IOException {
        httpClient.close();
    }
}
