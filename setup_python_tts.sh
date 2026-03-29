#!/bin/bash

echo "========================================"
echo "Voicify Python gTTS Setup"
echo "========================================"

echo
echo "Checking Python installation..."
if ! command -v python3 &> /dev/null; then
    echo "ERROR: Python 3 not found"
    echo "Please install Python 3.8+ first"
    echo "Ubuntu/Debian: sudo apt install python3 python3-pip"
    echo "macOS: brew install python3"
    exit 1
fi

python3 --version

echo
echo "Installing Python dependencies..."
cd "$(dirname "$0")"
python3 -m pip install --upgrade pip
python3 -m pip install -r src/main/resources/python/requirements.txt

if [ $? -ne 0 ]; then
    echo "ERROR: Failed to install dependencies"
    exit 1
fi

echo
echo "Testing gTTS service..."
python3 src/main/resources/python/gtts_service.py --test

if [ $? -ne 0 ]; then
    echo "ERROR: gTTS service test failed"
    exit 1
fi

echo
echo "========================================"
echo "Setup completed successfully!"
echo "========================================"
echo
echo "gTTS (Google Text-to-Speech) is now ready to use."
echo "The application will automatically use gTTS for high-quality speech synthesis."
echo
