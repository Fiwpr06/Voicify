# Voicify 🎙️

## Project Overview
**Voicify** is a comprehensive JavaFX application for English to Vietnamese translation with advanced voice input/output capabilities. Built using the MVC architecture pattern, it provides standard translation, real-time cabin interpretation, and AI-enhanced translation features. All audio/file translation history is stored in SQL Server with support for search, export, offline functionality, and dark mode UI.

⏳ **Timeline**: 10–15 days

🎯 **Objectives**:
- Translate English to Vietnamese with voice output
- Support real-time cabin translation mode
- Enhance translations using Gemini AI
- Store and manage audio/file translation history
- Provide comprehensive search and export functionality
- Support offline mode and dark/light theme switching

🛠️ **Technologies**:
- **JavaFX 21**: Modern GUI framework with CSS theming
- **Vosk API**: Offline speech recognition for English/Vietnamese
- **LibreTranslate**: Open-source translation service (offline capable)
- **Gemini API**: AI-powered translation enhancement
- **gTTS (Google Text-to-Speech)**: High-quality speech synthesis via Python integration
- **RHVoice**: Vietnamese text-to-speech synthesis (fallback)
- **Python 3.8+**: gTTS integration with pygame for audio playback
- **Java Sound API**: Audio recording and playback
- **SQL Server + JDBC**: Robust database storage
- **Apache PDFBox**: PDF export functionality
- **SLF4J + Logback**: Comprehensive logging
- **Maven**: Dependency management and build automation

## 🏗️ Architecture

### MVC Pattern Implementation:
- **Model**: Database entities, DAOs, and business logic
  - `TranslationHistory.java` - Entity class
  - `TranslationHistoryDAO.java` - Data access layer
  - `DatabaseManager.java` - Connection management

- **View**: FXML layouts and CSS themes
  - `main-view.fxml` - Main application layout
  - `light-theme.css` / `dark-theme.css` - Theme styling

- **Controller**: Event handling and UI coordination
  - `MainController.java` - Primary application controller

- **Services**: Core business functionality
  - `TranslationService.java` - Text translation
  - `SpeechRecognitionService.java` - Voice recognition
  - `TextToSpeechService.java` - Voice synthesis
  - `AITranslationService.java` - AI enhancement
  - `AudioService.java` - Audio recording/playback

### 📊 Database Schema:
```sql
CREATE TABLE TranslationHistory (
    id INT IDENTITY(1,1) PRIMARY KEY,
    source_text NVARCHAR(MAX) NOT NULL,
    translated_text NVARCHAR(MAX) NOT NULL,
    translation_type VARCHAR(20) NOT NULL, -- 'audio' or 'file'
    audio_file_path NVARCHAR(255) NULL,
    created_at DATETIME NOT NULL DEFAULT GETDATE(),
    improved_by_ai BIT NOT NULL DEFAULT 0,
    ai_translation_text NVARCHAR(MAX) NULL
);
```

## 🚀 Getting Started

### Prerequisites
- **Java 21** or higher
- **Maven 3.8+**
- **Python 3.8+** (for gTTS high-quality speech synthesis)
- **SQL Server** (Express or higher)
- **IntelliJ IDEA** (recommended) or any Java IDE
- **JavaFX SDK 21** (if not using Maven)

### Installation Steps

1. **Clone the Repository**
   ```bash
   git clone <repository-url>
   cd Voicify
   ```

2. **Python gTTS Setup** (Recommended for high-quality speech)
   ```bash
   # Windows
   setup_python_tts.bat

   # Linux/Mac
   chmod +x setup_python_tts.sh
   ./setup_python_tts.sh
   ```

   **Manual Setup:**
   ```bash
   # Install Python dependencies
   pip install gTTS pygame requests

   # Test gTTS service
   python src/main/resources/python/gtts_service.py --test
   ```

3. **Database Setup**
   - Install SQL Server (Express edition is sufficient)
   - Create a database named `VoicifyDB`
   - Update database credentials in `src/main/resources/application.properties`
   - The application will automatically create the required tables on first run

4. **Configure API Keys** (Optional)
   - For Gemini AI integration, update `gemini.api.key` in `application.properties`
   - For LibreTranslate, ensure the service is running or keep offline mode enabled

4. **Build and Run**
   ```bash
   # Using Maven
   mvn clean javafx:run

   # Or compile and run
   mvn clean compile
   mvn javafx:run
   ```

5. **Alternative: Run from IDE**
   - Open project in IntelliJ IDEA
   - Ensure JavaFX is properly configured
   - Run `VoicifyApplication.main()`

## 🎯 Features

### 1. Regular Translation Tab
- **Text Input**: Manual text entry for translation
- **Voice Recording**: Record audio for speech-to-text conversion
- **File Upload**: Load audio files (WAV, MP3, FLAC) for processing
- **Translation**: Convert English text to Vietnamese
- **Text-to-Speech**: Play Vietnamese translation as audio
- **AI Enhancement**: Improve translations using Gemini AI

### 2. Cabin Translation Tab
- **Real-time Mode**: Continuous speech recognition and translation
- **Live Translation**: Instant Vietnamese output as you speak
- **Auto-Speech**: Automatic playback of translated text
- **Cabin Interface**: Simplified UI for interpretation scenarios

### 3. Translation History Tab
- **History Management**: View all saved translations
- **Search Functionality**: Find translations by keyword
- **Export Options**: Export history to TXT, CSV, or PDF
- **Delete Records**: Remove unwanted translation entries
- **Filter by Type**: View audio or file-based translations

### 4. Additional Features
- **Dark/Light Theme**: Toggle between UI themes
- **Offline Support**: Work without internet connection
- **Audio Playback**: Replay original and translated audio
- **Database Storage**: Persistent storage of translation history
- **Error Handling**: Comprehensive error management and logging

## 🔧 Configuration

### Database Configuration
Update `src/main/resources/application.properties`:
```properties
database.server=localhost
database.port=1433
database.name=VoicifyDB
database.username=your_username
database.password=your_password
```

### API Configuration
```properties
# Gemini AI (optional)
gemini.api.key=YOUR_ACTUAL_API_KEY

# LibreTranslate (optional)
libretranslate.api.url=http://localhost:5000
libretranslate.offline.mode=true
```

## 🧪 Testing

Run the test suite:
```bash
mvn test
```

The tests cover:
- Translation service functionality
- Database entity creation
- Basic application components
- Service initialization and configuration

## 📁 Project Structure
```
Voicify/
├── src/
│   ├── main/
│   │   ├── java/com/voicify/
│   │   │   ├── controller/          # UI Controllers
│   │   │   ├── model/              # Data models and DAOs
│   │   │   ├── service/            # Business logic services
│   │   │   ├── util/               # Utility classes
│   │   │   └── VoicifyApplication.java
│   │   └── resources/
│   │       ├── com/voicify/
│   │       │   ├── css/            # Theme stylesheets
│   │       │   └── fxml/           # UI layouts
│   │       └── application.properties
│   └── test/                       # Unit tests
├── pom.xml                         # Maven configuration
└── README.md
```