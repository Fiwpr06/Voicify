package com.voicify.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.github.cdimascio.dotenv.Dotenv;
import javafx.scene.control.Alert;
import javafx.scene.control.Alert.AlertType;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.apache.poi.hwpf.HWPFDocument;
import org.apache.poi.hwpf.extractor.WordExtractor;
import org.apache.poi.xwpf.extractor.XWPFWordExtractor;
import org.apache.poi.xwpf.usermodel.XWPFDocument;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.apache.poi.hssf.usermodel.HSSFWorkbook;

import java.io.*;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.Files;
import java.time.Duration;
import java.util.Properties;
import java.util.Iterator;

public class FileProcessingService {
    private static final String GEMINI_API_URL = "https://generativelanguage.googleapis.com/v1beta/models/gemini-2.0-flash:generateContent";
    private static final String API_KEY;
    private final HttpClient httpClient;
    private final ObjectMapper mapper;
    
    // Giới hạn ký tự cho mỗi loại file
    private static final int TEXT_CHAR_LIMIT = 30000;
    private static final int PDF_CHAR_LIMIT = 25000;
    private static final int DOCX_CHAR_LIMIT = 25000;
    private static final int EXCEL_CHAR_LIMIT = 20000;

    static {
        Dotenv dotenv = Dotenv.configure()
                .directory(".")
                .ignoreIfMissing()
                .load();
        API_KEY = dotenv.get("GEMINI_API", System.getenv("GEMINI_API_KEY") != null
                ? System.getenv("GEMINI_API_KEY")
                : "YOUR_GEMINI_API_KEY");
    }

    public FileProcessingService() {
        httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(10))
                .build();
        mapper = new ObjectMapper();
    }

    public String translateFile(File file) throws Exception {
        String content = extractTextFromFile(file);
        String prompt = "Translate the following text to Vietnamese. Maintain the original formatting as much as possible:\n\n" + content;
        return processWithGemini(prompt);
    }

    public String summarizeFile(File file) throws Exception {
        String content = extractTextFromFile(file);
        String prompt = "Summarize the following text in Vietnamese. Provide a comprehensive summary that captures the main points:\n\n" + content;
        return processWithGemini(prompt);
    }

    /**
     * Phương thức công khai để trích xuất văn bản từ file
     */
    public String extractTextFromFile(File file) throws Exception {
        String fileName = file.getName().toLowerCase();
        String content;

        // Check if file exists and is readable
        if (!file.exists()) {
            throw new Exception("File does not exist: " + file.getAbsolutePath());
        }
        if (!file.canRead()) {
            throw new Exception("Cannot read file: " + file.getAbsolutePath());
        }

        System.out.println("Processing file: " + file.getAbsolutePath());
        System.out.println("File size: " + file.length() + " bytes");

        // Handle audio files separately
        if (fileName.endsWith(".mp3") || fileName.endsWith(".wav") || fileName.endsWith(".m4a") || fileName.endsWith(".flac")) {
            throw new Exception("Audio files cannot be read as text. Please use the speech recognition feature to convert audio to text first.");
        }

        if (fileName.endsWith(".txt") || fileName.endsWith(".csv") || fileName.endsWith(".json") || fileName.endsWith(".xml")) {
            try {
                content = readTextFile(file);
                System.out.println("Successfully read text file, content length: " + content.length());
            } catch (Exception e) {
                System.out.println("Error reading text file: " + e.getMessage());
                e.printStackTrace();
                throw new Exception("Failed to read text file: " + e.getMessage(), e);
            }
            if (content.length() > TEXT_CHAR_LIMIT) {
                showAlert(AlertType.WARNING, "File Too Large",
                    "Text file is too large. Only the first " + TEXT_CHAR_LIMIT + " characters will be processed.");
                content = content.substring(0, TEXT_CHAR_LIMIT);
            }
        } else if (fileName.endsWith(".pdf")) {
            content = readPdfFile(file);
            if (content.length() > PDF_CHAR_LIMIT) {
                showAlert(AlertType.WARNING, "File Too Large",
                    "PDF file is too large. Only the first " + PDF_CHAR_LIMIT + " characters will be processed.");
                content = content.substring(0, PDF_CHAR_LIMIT);
            }
        } else if (fileName.endsWith(".docx")) {
            content = readDocxFile(file);
            if (content.length() > DOCX_CHAR_LIMIT) {
                showAlert(AlertType.WARNING, "File Too Large",
                    "DOCX file is too large. Only the first " + DOCX_CHAR_LIMIT + " characters will be processed.");
                content = content.substring(0, DOCX_CHAR_LIMIT);
            }
        } else if (fileName.endsWith(".doc")) {
            // Thông báo không hỗ trợ định dạng DOC cũ
            showAlert(AlertType.WARNING, "Unsupported Format",
                "The old DOC format is not supported. Please convert to DOCX format.");
            throw new UnsupportedOperationException("Old DOC format is not supported");
        } else if (fileName.endsWith(".xlsx") || fileName.endsWith(".xls")) {
            content = readExcelFile(file);
            if (content.length() > EXCEL_CHAR_LIMIT) {
                showAlert(AlertType.WARNING, "File Too Large",
                    "Excel file is too large. Only the first " + EXCEL_CHAR_LIMIT + " characters will be processed.");
                content = content.substring(0, EXCEL_CHAR_LIMIT);
            }
        } else {
            throw new UnsupportedOperationException("Unsupported file format: " + fileName);
        }
        
        return content;
    }

    /**
     * Đọc file văn bản thông thường
     */
    private String readTextFile(File file) throws IOException {
        try {
            // Try UTF-8 first
            return Files.readString(file.toPath(), java.nio.charset.StandardCharsets.UTF_8);
        } catch (Exception e) {
            System.out.println("UTF-8 reading failed, trying default charset: " + e.getMessage());
            try {
                // Fallback to system default charset
                return Files.readString(file.toPath());
            } catch (Exception e2) {
                System.out.println("Default charset reading failed, trying manual reading: " + e2.getMessage());
                // Manual reading as last resort
                StringBuilder content = new StringBuilder();
                try (BufferedReader reader = new BufferedReader(new FileReader(file))) {
                    String line;
                    while ((line = reader.readLine()) != null) {
                        content.append(line).append("\n");
                    }
                }
                return content.toString();
            }
        }
    }

    /**
     * Đọc file PDF
     */
    private String readPdfFile(File file) throws IOException {
        try (PDDocument document = PDDocument.load(file)) {
            PDFTextStripper stripper = new PDFTextStripper();
            return stripper.getText(document);
        }
    }

    /**
     * Đọc file DOCX (Word 2007+)
     */
    private String readDocxFile(File file) throws IOException {
        try (FileInputStream fis = new FileInputStream(file);
             XWPFDocument document = new XWPFDocument(fis);
             XWPFWordExtractor extractor = new XWPFWordExtractor(document)) {
            return extractor.getText();
        }
    }

    /**
     * Đọc file DOC (Word 97-2003)
     */
    private String readDocFile(File file) throws IOException {
        try (FileInputStream fis = new FileInputStream(file);
             HWPFDocument document = new HWPFDocument(fis);
             WordExtractor extractor = new WordExtractor(document)) {
            return extractor.getText();
        }
    }

    /**
     * Đọc file Excel (cả .xls và .xlsx)
     */
    private String readExcelFile(File file) throws IOException {
        StringBuilder content = new StringBuilder();
        
        try (FileInputStream fis = new FileInputStream(file);
             Workbook workbook = file.getName().toLowerCase().endsWith(".xlsx") ? 
                                new XSSFWorkbook(fis) : new HSSFWorkbook(fis)) {
            
            for (int i = 0; i < workbook.getNumberOfSheets(); i++) {
                Sheet sheet = workbook.getSheetAt(i);
                content.append("Sheet: ").append(sheet.getSheetName()).append("\n\n");
                
                Iterator<Row> rowIterator = sheet.iterator();
                while (rowIterator.hasNext()) {
                    Row row = rowIterator.next();
                    Iterator<Cell> cellIterator = row.cellIterator();
                    
                    while (cellIterator.hasNext()) {
                        Cell cell = cellIterator.next();
                        switch (cell.getCellType()) {
                            case STRING:
                                content.append(cell.getStringCellValue());
                                break;
                            case NUMERIC:
                                content.append(cell.getNumericCellValue());
                                break;
                            case BOOLEAN:
                                content.append(cell.getBooleanCellValue());
                                break;
                            case FORMULA:
                                content.append(cell.getCellFormula());
                                break;
                            default:
                                content.append(" ");
                        }
                        content.append("\t");
                    }
                    content.append("\n");
                }
                content.append("\n\n");
            }
        }
        
        return content.toString();
    }

    private String processWithGemini(String prompt) throws Exception {
        if (API_KEY == null || API_KEY.isEmpty() || "YOUR_GEMINI_API_KEY".equals(API_KEY)) {
            showAlert(AlertType.ERROR, "API Error", "Gemini API key not configured.");
            return "API key not configured. Please add your Gemini API key to the application.properties file or environment variables.";
        }

        String responseJson = callGeminiAPI(prompt);
        JsonNode response = mapper.readTree(responseJson);
        
        if (response.has("error")) {
            throw new RuntimeException("Gemini API error: " + response.get("error").get("message").asText());
        }
        
        if (!response.has("candidates") || response.get("candidates").size() == 0) {
            throw new RuntimeException("Gemini API response missing candidates");
        }

        return response.get("candidates").get(0)
                .get("content").get("parts").get(0).get("text").asText();
    }

    private String callGeminiAPI(String prompt) throws IOException, InterruptedException {
        String requestBody = String.format("""
            {
                "contents": [{
                    "parts": [{
                        "text": "%s"
                    }]
                }],
                "generationConfig": {
                    "temperature": 0.2,
                    "topK": 40,
                    "topP": 0.95,
                    "maxOutputTokens": 8192
                }
            }
            """, prompt.replace("\"", "\\\"").replace("\n", "\\n"));
        
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(GEMINI_API_URL + "?key=" + API_KEY))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(requestBody))
                .build();
        
        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
        return response.body();
    }
    
    private void showAlert(AlertType type, String title, String content) {
        Alert alert = new Alert(type);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(content);
        alert.showAndWait();
    }
}