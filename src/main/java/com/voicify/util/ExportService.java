package com.voicify.util;

import com.voicify.model.TranslationHistory;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.font.PDType0Font;
import org.apache.pdfbox.pdmodel.font.PDType1Font;
import org.apache.pdfbox.pdmodel.font.PDFont;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.time.format.DateTimeFormatter;
import java.util.List;

public class ExportService {
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    // Enum định nghĩa các định dạng export
    public enum ExportFormat {
        TXT("txt", "Text File"),
        CSV("csv", "Comma Separated Values"),
        PDF("pdf", "Portable Document Format");

        private final String extension;
        private final String description;

        ExportFormat(String extension, String description) {
            this.extension = extension;
            this.description = description;
        }

        public String getExtension() {
            return extension;
        }

        public String getDescription() {
            return description;
        }
    }

    // Export lịch sử dịch sang định dạng được chỉ định
    public void exportHistory(List<TranslationHistory> histories, File outputFile, ExportFormat format) throws IOException {
        if (histories == null || histories.isEmpty()) {
            throw new IllegalArgumentException("No translation history to export");
        }

        if (outputFile == null) {
            throw new IllegalArgumentException("Output file cannot be null");
        }

        System.out.println("Exporting " + histories.size() + " translation records to " + format + " format: " + outputFile.getName());

        switch (format) {
            case TXT -> exportToTxt(histories, outputFile);
            case CSV -> exportToCsv(histories, outputFile);
            case PDF -> exportToPdf(histories, outputFile);
            default -> throw new IllegalArgumentException("Unsupported export format: " + format);
        }

        System.out.println("Export completed successfully: " + outputFile.getAbsolutePath());
    }

    // Export sang định dạng văn bản thuần
    private void exportToTxt(List<TranslationHistory> histories, File outputFile) throws IOException {
        try (FileWriter writer = new FileWriter(outputFile, java.nio.charset.StandardCharsets.UTF_8)) {
            writer.write("VOICIFY TRANSLATION HISTORY\n");
            writer.write("===========================\n\n");
            writer.write("Export Date: " + java.time.LocalDateTime.now().format(DATE_FORMATTER) + "\n");
            writer.write("Total Records: " + histories.size() + "\n\n");

            for (int i = 0; i < histories.size(); i++) {
                TranslationHistory history = histories.get(i);

                writer.write("Record " + (i + 1) + ":\n");
                writer.write("-----------\n");
                writer.write("Date: " + history.getCreatedAt().format(DATE_FORMATTER) + "\n");
                writer.write("Type: " + history.getTranslationType() + "\n");
                writer.write("Original Text: " + history.getSourceText() + "\n");
                writer.write("Translation: " + history.getTranslatedText() + "\n");

                if (history.isImprovedByAi() && history.getAiTranslationText() != null) {
                    writer.write("AI Improved: " + history.getAiTranslationText() + "\n");
                }

                if (history.getAudioFilePath() != null) {
                    writer.write("Audio File: " + history.getAudioFilePath() + "\n");
                }

                writer.write("\n");
            }
        }
    }

    // Export sang định dạng CSV
    private void exportToCsv(List<TranslationHistory> histories, File outputFile) throws IOException {
        try (java.io.OutputStreamWriter writer = new java.io.OutputStreamWriter(
                new java.io.FileOutputStream(outputFile), java.nio.charset.StandardCharsets.UTF_8)) {

            // Ghi BOM (Byte Order Mark) để Excel nhận diện UTF-8
            writer.write('\ufeff');

            // Ghi tiêu đề CSV
            writer.write("ID,Date,Type,Original Text,Translation,AI Improved,AI Translation,Audio File\n");

            // Ghi dữ liệu
            for (TranslationHistory history : histories) {
                writer.write(String.format("%d,\"%s\",\"%s\",\"%s\",\"%s\",%s,\"%s\",\"%s\"\n",
                        history.getId(),
                        history.getCreatedAt().format(DATE_FORMATTER),
                        escapeCsv(history.getTranslationType()),
                        escapeCsv(history.getSourceText()),
                        escapeCsv(history.getTranslatedText()),
                        history.isImprovedByAi() ? "Yes" : "No",
                        escapeCsv(history.getAiTranslationText() != null ? history.getAiTranslationText() : ""),
                        escapeCsv(history.getAudioFilePath() != null ? history.getAudioFilePath() : "")
                ));
            }
        }
    }

    // Export sang định dạng PDF
    private void exportToPdf(List<TranslationHistory> histories, File outputFile) throws IOException {
        try (PDDocument document = new PDDocument()) {
            PDPage page = new PDPage();
            document.addPage(page);

            // Sử dụng font built-in của PDF (hỗ trợ Unicode cơ bản)
            PDFont titleFont = PDType1Font.HELVETICA_BOLD;
            PDFont headerFont = PDType1Font.HELVETICA_BOLD;
            PDFont bodyFont = PDType1Font.HELVETICA;

            float margin = 50;
            float yPosition = page.getMediaBox().getHeight() - margin;
            float pageWidth = page.getMediaBox().getWidth() - 2 * margin;
            float lineSpacing = 15;

            PDPageContentStream contentStream = new PDPageContentStream(document, page);

            try {

            // Ghi tiêu đề và thông tin export
            yPosition = writeHeader(document, page, contentStream, titleFont, bodyFont, margin, yPosition, histories);

            // Ghi các bản ghi lịch sử
            for (int i = 0; i < histories.size(); i++) {
                TranslationHistory history = histories.get(i);

                // Kiểm tra nếu không đủ chỗ cho bản ghi, tạo trang mới
                float requiredSpace = calculateRecordHeight(history, lineSpacing);
                if (yPosition - requiredSpace < margin) {
                    contentStream.close();
                    page = new PDPage();
                    document.addPage(page);
                    contentStream = new PDPageContentStream(document, page);
                    yPosition = page.getMediaBox().getHeight() - margin;
                }

                // Ghi bản ghi
                yPosition = writeRecord(contentStream, headerFont, bodyFont, margin, yPosition, lineSpacing, i + 1, history);
            }

                contentStream.close();
                document.save(outputFile);

            } catch (Exception e) {
                System.out.println("PDF export error: " + e.getMessage());
                e.printStackTrace();

                // Fallback: Create simple text-based PDF
                createSimplePdf(histories, outputFile, document);
            }
        } catch (Exception e) {
            System.out.println("PDF export failed: " + e.getMessage());
            throw new IOException("Failed to export PDF: " + e.getMessage(), e);
        }
    }

    // Fallback method for simple PDF creation
    private void createSimplePdf(List<TranslationHistory> histories, File outputFile, PDDocument document) throws IOException {
        try {
            // Clear any existing pages
            while (document.getNumberOfPages() > 0) {
                document.removePage(0);
            }

            PDPage page = new PDPage();
            document.addPage(page);

            PDPageContentStream contentStream = new PDPageContentStream(document, page);
            PDFont font = PDType1Font.HELVETICA;

            float margin = 50;
            float yPosition = page.getMediaBox().getHeight() - margin;

            contentStream.beginText();
            contentStream.setFont(font, 12);
            contentStream.newLineAtOffset(margin, yPosition);
            contentStream.showText("Translation History Export");
            contentStream.endText();

            yPosition -= 30;

            contentStream.beginText();
            contentStream.setFont(font, 10);
            contentStream.newLineAtOffset(margin, yPosition);
            contentStream.showText("Total Records: " + histories.size());
            contentStream.endText();

            contentStream.close();
            document.save(outputFile);

        } catch (Exception e) {
            System.out.println("Fallback PDF creation failed: " + e.getMessage());
            throw new IOException("Failed to create simple PDF", e);
        }
    }

    // Tính chiều cao cần thiết cho một bản ghi
    private float calculateRecordHeight(TranslationHistory history, float lineSpacing) {
        int lines = 3; // Date, Original, Translation
        if (history.isImprovedByAi() && history.getAiTranslationText() != null) {
            lines++; // AI Improved
        }
        return (lines * lineSpacing) + 20; // Thêm khoảng cách giữa các bản ghi
    }

    // Ghi tiêu đề và thông tin export
    private float writeHeader(PDDocument document, PDPage page, PDPageContentStream contentStream,
                              PDFont titleFont, PDFont bodyFont, float margin, float yPosition,
                              List<TranslationHistory> histories) throws IOException {
        contentStream.beginText();
        contentStream.setFont(titleFont, 16);
        contentStream.newLineAtOffset(margin, yPosition);
        contentStream.showText("Voicify Translation History");
        contentStream.endText();

        yPosition -= 30;

        contentStream.beginText();
        contentStream.setFont(bodyFont, 10);
        contentStream.newLineAtOffset(margin, yPosition);
        contentStream.showText("Export Date: " + java.time.LocalDateTime.now().format(DATE_FORMATTER));
        contentStream.endText();

        yPosition -= 15;

        contentStream.beginText();
        contentStream.setFont(bodyFont, 10);
        contentStream.newLineAtOffset(margin, yPosition);
        contentStream.showText("Total Records: " + histories.size());
        contentStream.endText();

        yPosition -= 30;
        return yPosition;
    }

    // Ghi một bản ghi lịch sử vào PDF
    private float writeRecord(PDPageContentStream contentStream, PDFont headerFont, PDFont bodyFont,
                              float margin, float yPosition, float lineSpacing, int recordNumber,
                              TranslationHistory history) throws IOException {
        // Record header
        contentStream.beginText();
        contentStream.setFont(headerFont, 12);
        contentStream.newLineAtOffset(margin, yPosition);
        contentStream.showText("Record " + recordNumber + " - " + history.getCreatedAt().format(DATE_FORMATTER));
        contentStream.endText();

        yPosition -= lineSpacing;

        // Original text
        String originalText = truncateText(history.getSourceText(), 80);
        contentStream.beginText();
        contentStream.setFont(bodyFont, 10);
        contentStream.newLineAtOffset(margin, yPosition);
        contentStream.showText("Original: " + originalText);
        contentStream.endText();

        yPosition -= lineSpacing;

        // Translation
        String translation = truncateText(history.getTranslatedText(), 80);
        contentStream.beginText();
        contentStream.setFont(bodyFont, 10);
        contentStream.newLineAtOffset(margin, yPosition);
        contentStream.showText("Translation: " + translation);
        contentStream.endText();

        yPosition -= lineSpacing;

        // AI improvement if available
        if (history.isImprovedByAi() && history.getAiTranslationText() != null) {
            String aiTranslation = truncateText(history.getAiTranslationText(), 80);
            contentStream.beginText();
            contentStream.setFont(bodyFont, 10);
            contentStream.newLineAtOffset(margin, yPosition);
            contentStream.showText("AI Improved: " + aiTranslation);
            contentStream.endText();

            yPosition -= lineSpacing;
        }

        yPosition -= 10; // Khoảng cách giữa các bản ghi
        return yPosition;
    }

    // Thoát ký tự đặc biệt cho CSV
    private String escapeCsv(String text) {
        if (text == null) {
            return "";
        }

        String escaped = text.replace("\"", "\"\"");
        if (escaped.contains(",") || escaped.contains("\"") || escaped.contains("\n")) {
            escaped = "\"" + escaped + "\"";
        }

        return escaped;
    }

    // Cắt ngắn văn bản nếu quá dài
    private String truncateText(String text, int maxLength) {
        if (text == null) {
            return "";
        }

        if (text.length() <= maxLength) {
            return text;
        }

        return text.substring(0, maxLength - 3) + "...";
    }

    // Lấy bộ lọc phần mở rộng cho FileChooser
    public static javafx.stage.FileChooser.ExtensionFilter getExtensionFilter(ExportFormat format) {
        return new javafx.stage.FileChooser.ExtensionFilter(
                format.getDescription() + " (*." + format.getExtension() + ")",
                "*." + format.getExtension()
        );
    }
}