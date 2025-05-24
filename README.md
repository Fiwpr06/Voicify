# Voicify 🎙️

## Project Overview
**Voicify** is a JavaFX app for translating English to Vietnamese with voice output. Built with MVC on IntelliJ IDEA, it offers standard translation, real-time cabin interpretation, and AI-enhanced translation. Audio/file history is stored in SQL Server, with offline support and dark mode.

⏳ **Timeline**: 10–15 days

🎯 **Objectives**:
- Translate English to Vietnamese with voice output.
- Support real-time cabin translation.
- Enhance translations with Gemini API.
- Store audio/file history; support search/export.
- Provide offline mode, dark mode UI.

🛠️ **Technologies**:
- **JavaFX**: GUI, dark mode (CSS).
- **Vosk API**: Offline speech recognition.
- **LibreTranslate**: Translation (offline).
- **Gemini API**: AI translation.
- **RHVoice**: Vietnamese text-to-speech.
- **Java Sound API**: Audio handling.
- **SQL Server + JDBC**: History storage.
- **Apache PDFBox**: PDF export.
- **IntelliJ IDEA + MVC**: Development.

📊 **Database**:
```sql
CREATE TABLE TranslationHistory (
    id INT IDENTITY(1,1) PRIMARY KEY,
    source_text NVARCHAR(MAX) NOT NULL,
    translated_text NVARCHAR(MAX) NOT NULL,
    translation_type VARCHAR(20) NOT NULL,
    audio_file_path NVARCHAR(255) NULL,
    created_at DATETIME NOT NULL DEFAULT GETDATE(),
    improved_by_ai BIT NOT NULL DEFAULT 0,
    ai_translation_text NVARCHAR(MAX) NULL
);