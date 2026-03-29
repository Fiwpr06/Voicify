@echo off
echo ========================================
echo Voicify Python gTTS Setup
echo ========================================

echo.
echo Checking Python installation...
python --version
if %errorlevel% neq 0 (
    echo ERROR: Python not found in PATH
    echo Please install Python 3.8+ and add it to PATH
    echo Download from: https://www.python.org/downloads/
    pause
    exit /b 1
)

echo.
echo Installing Python dependencies...
cd /d "%~dp0"
python -m pip install --upgrade pip
python -m pip install -r src/main/resources/python/requirements.txt

if %errorlevel% neq 0 (
    echo ERROR: Failed to install dependencies
    pause
    exit /b 1
)

echo.
echo Testing gTTS service...
python src/main/resources/python/gtts_service.py --test

if %errorlevel% neq 0 (
    echo ERROR: gTTS service test failed
    pause
    exit /b 1
)

echo.
echo ========================================
echo Setup completed successfully!
echo ========================================
echo.
echo gTTS (Google Text-to-Speech) is now ready to use.
echo The application will automatically use gTTS for high-quality speech synthesis.
echo.
pause
