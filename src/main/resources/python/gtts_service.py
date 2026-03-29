#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""
Google Text-to-Speech Service for Voicify
Provides high-quality text-to-speech using gTTS library
"""

import sys
import os
import json
import argparse
import tempfile

# Suppress pygame welcome message
os.environ['PYGAME_HIDE_SUPPORT_PROMPT'] = '1'
import pygame

from gtts import gTTS
from io import BytesIO
import logging

# Setup logging - only for test mode
logger = logging.getLogger(__name__)

class GTTSService:
    def __init__(self, enable_logging=False):
        """Initialize gTTS service"""
        self.supported_languages = {
            'en': 'English',
            'vi': 'Vietnamese',
            'fr': 'French',
            'es': 'Spanish',
            'de': 'German',
            'ja': 'Japanese',
            'ko': 'Korean',
            'zh': 'Chinese'
        }

        # Setup logging only if enabled
        if enable_logging:
            logging.basicConfig(level=logging.INFO, format='%(asctime)s - %(levelname)s - %(message)s')
        else:
            logging.basicConfig(level=logging.CRITICAL)  # Suppress all logs except critical

        # Initialize pygame mixer for audio playback
        try:
            pygame.mixer.init()
            if enable_logging:
                logger.info("Pygame mixer initialized successfully")
        except Exception as e:
            if enable_logging:
                logger.error(f"Failed to initialize pygame mixer: {e}")
            raise

    def text_to_speech(self, text, language='en', slow=False, save_file=None):
        """
        Convert text to speech using gTTS
        
        Args:
            text (str): Text to convert to speech
            language (str): Language code (default: 'en')
            slow (bool): Speak slowly (default: False)
            save_file (str): Path to save audio file (optional)
            
        Returns:
            dict: Result with success status and message
        """
        try:
            if not text or text.strip() == "":
                return {"success": False, "message": "Empty text provided"}
            
            # Validate language
            if language not in self.supported_languages:
                language = 'en'
            
            # Create gTTS object
            tts = gTTS(text=text, lang=language, slow=slow)
            
            if save_file:
                # Save to specified file
                tts.save(save_file)
                return {"success": True, "message": f"Audio saved to {save_file}"}
            else:
                # Play directly using pygame
                return self._play_audio_from_gtts(tts, text)

        except Exception as e:
            error_msg = f"Text-to-speech failed: {str(e)}"
            return {"success": False, "message": error_msg}

    def _play_audio_from_gtts(self, tts, text):
        """Play audio directly from gTTS object using pygame"""
        try:
            # Create temporary file in memory
            audio_buffer = BytesIO()
            tts.write_to_fp(audio_buffer)
            audio_buffer.seek(0)

            # Create temporary file for pygame
            with tempfile.NamedTemporaryFile(suffix='.mp3', delete=False) as temp_file:
                temp_file.write(audio_buffer.getvalue())
                temp_file_path = temp_file.name

            try:
                # Play audio using pygame
                pygame.mixer.music.load(temp_file_path)
                pygame.mixer.music.play()

                # Wait for playback to complete
                while pygame.mixer.music.get_busy():
                    pygame.time.wait(100)

                # Stop music to release file handle
                pygame.mixer.music.stop()
                pygame.mixer.music.unload()

                return {"success": True, "message": "Audio played successfully"}

            finally:
                # Clean up temporary file with retry
                try:
                    # Wait a bit for file handle to be released
                    import time
                    time.sleep(0.1)
                    os.unlink(temp_file_path)
                except Exception as cleanup_error:
                    pass  # Ignore cleanup errors

        except Exception as e:
            error_msg = f"Audio playback failed: {str(e)}"
            return {"success": False, "message": error_msg}

    def get_supported_languages(self):
        """Get list of supported languages"""
        return self.supported_languages

    def test_service(self):
        """Test the gTTS service"""
        test_text = "Hello, Welcome to Voicify."
        result = self.text_to_speech(test_text, 'en')
        return result

def main():
    """Main function for command line usage"""
    parser = argparse.ArgumentParser(description='Google Text-to-Speech Service')
    parser.add_argument('--text', help='Text to convert to speech')
    parser.add_argument('--lang', default='en', help='Language code (default: en)')
    parser.add_argument('--slow', action='store_true', help='Speak slowly')
    parser.add_argument('--save', help='Save audio to file instead of playing')
    parser.add_argument('--test', action='store_true', help='Test the service')
    parser.add_argument('--languages', action='store_true', help='List supported languages')

    args = parser.parse_args()
    
    try:
        # Enable logging only for test mode
        enable_logging = args.test if hasattr(args, 'test') else False
        service = GTTSService(enable_logging=enable_logging)

        if args.languages:
            print(json.dumps(service.get_supported_languages(), indent=2))
            return

        if args.test:
            result = service.test_service()
            print(json.dumps(result))
            return

        if not args.text:
            print(json.dumps({"success": False, "message": "Text argument is required"}))
            return

        # Convert text to speech
        result = service.text_to_speech(
            text=args.text,
            language=args.lang,
            slow=args.slow,
            save_file=args.save
        )
        
        print(json.dumps(result))
        
    except Exception as e:
        error_result = {"success": False, "message": f"Service error: {str(e)}"}
        print(json.dumps(error_result))
        sys.exit(1)

if __name__ == "__main__":
    main()
