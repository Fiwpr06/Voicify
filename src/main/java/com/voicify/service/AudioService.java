package com.voicify.service;

import com.voicify.util.ErrorHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.sound.sampled.*;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.util.concurrent.CompletableFuture;

public class AudioService {
    private static final Logger logger = LoggerFactory.getLogger(AudioService.class);
    private static final AudioFormat AUDIO_FORMAT = new AudioFormat(
            AudioFormat.Encoding.PCM_SIGNED,
            16000,
            16,
            1, 2,
            16000,
            false);

    private TargetDataLine microphone;
    private boolean isRecording = false;
    private ByteArrayOutputStream recordedAudio;
    private Clip playbackClip;

    public AudioService() {
        initializeAudioSystem();
    }

    private void initializeAudioSystem() {
        try {
            DataLine.Info micInfo = new DataLine.Info(TargetDataLine.class, AUDIO_FORMAT);
            if (!AudioSystem.isLineSupported(micInfo)) {
                //            Dialog.showAlert(AlertType.ERROR, "Audio Error", "Microphone not supported with the specified audio format.");
            } else {
                System.out.println("Audio system initialized successfully");
            }
        } catch (Exception e) {
//            Dialog.showAlert(AlertType.ERROR, "Audio Error", "Failed to initialize audio system: " + e.getMessage());
        }
    }

//    public void startRecording(AudioRecordingCallback callback) {
//        if (isRecording) {
//            callback.onError("Recording already in progress");
    ////            Dialog.showAlert(AlertType.WARNING, "Recording Error", "Đang ghi âm, vui lòng dừng trước khi bắt đầu lại.");
//            return;
//        }
//
//        CompletableFuture.runAsync(() -> {
//            try {
//                DataLine.Info micInfo = new DataLine.Info(TargetDataLine.class, AUDIO_FORMAT);
//                microphone = (TargetDataLine) AudioSystem.getLine(micInfo);
//                microphone.open(AUDIO_FORMAT);
//
//                recordedAudio = new ByteArrayOutputStream();
//                isRecording = true;
//
//                microphone.start();
//                callback.onRecordingStarted();
//                System.out.println("Audio recording started");
//    //            Dialog.showAlert(AlertType.INFORMATION, "Recording Started", "Bắt đầu ghi âm...");
//
//                byte[] buffer = new byte[4096];
//                while (isRecording) {
//                    int bytesRead = microphone.read(buffer, 0, buffer.length);
//                    if (bytesRead > 0) {
//                        recordedAudio.write(buffer, 0, bytesRead);
//                        callback.onAudioData(buffer, bytesRead);
//                    }
//                }
//
//                microphone.stop();
//                microphone.close();
//
//                byte[] audioData = recordedAudio.toByteArray();
//                callback.onRecordingCompleted(audioData);
//                System.out.println("Audio recording completed. Recorded " + audioData.length + " bytes");
//    //            Dialog.showAlert(AlertType.INFORMATION, "Recording Completed", "Ghi âm hoàn tất!");
//
//            } catch (LineUnavailableException e) {
//                callback.onError("Microphone not available: " + e.getMessage());
//    //            Dialog.showAlert(AlertType.ERROR, "Recording Error", "Microphone không khả dụng: " + e.getMessage());
//            } catch (Exception e) {
//                callback.onError("Recording failed: " + e.getMessage());
//    //            Dialog.showAlert(AlertType.ERROR, "Recording Error", "Ghi âm thất bại: " + e.getMessage());
//            } finally {
//                isRecording = false;
//                if (microphone != null && microphone.isOpen()) {
//                    microphone.close();
//                }
//            }
//        });
//    }

    public void startRecording(AudioRecordingCallback callback) {
        if (isRecording) {
            System.err.println("Recording already in progress");
            callback.onError("Recording already in progress");
            return;
        }

        CompletableFuture.runAsync(() -> {
            try {
                DataLine.Info micInfo = new DataLine.Info(TargetDataLine.class, AUDIO_FORMAT);
                microphone = (TargetDataLine) AudioSystem.getLine(micInfo);
                microphone.open(AUDIO_FORMAT);

                recordedAudio = new ByteArrayOutputStream();
                isRecording = true;

                microphone.start();
                callback.onRecordingStarted();
                System.out.println("Audio recording started");

                byte[] buffer = new byte[2048];
                while (isRecording) {
                    int bytesRead = microphone.read(buffer, 0, buffer.length);
                    if (bytesRead > 0) {
                        recordedAudio.write(buffer, 0, bytesRead);
                        // Log để kiểm tra dữ liệu
                        System.out.println("Read " + bytesRead + " bytes from microphone");
                        callback.onAudioData(buffer, bytesRead);
                    }
                }

                microphone.stop();
                microphone.close();

                byte[] audioData = recordedAudio.toByteArray();
                callback.onRecordingCompleted(audioData);
                System.out.println("Audio recording completed. Recorded " + audioData.length + " bytes");

            } catch (LineUnavailableException e) {
                System.err.println("Microphone not available: " + e.getMessage());
                e.printStackTrace();
                callback.onError("Microphone not available: " + e.getMessage());
            } catch (Exception e) {
                System.err.println("Recording failed: " + e.getMessage());
                e.printStackTrace();
                callback.onError("Recording failed: " + e.getMessage());
            } finally {
                isRecording = false;
                if (microphone != null && microphone.isOpen()) {
                    microphone.close();
                }
                if (recordedAudio != null) {
                    try {
                        recordedAudio.close();
                    } catch (IOException e) {
                        System.err.println("Error closing recordedAudio: " + e.getMessage());
                    }
                }
            }
        });
    }

    public void stopRecording() {
        if (isRecording) {
            isRecording = false;
            System.out.println("Stopping audio recording");
        }
    }

//    public void saveAudioToFile(byte[] audioData, File outputFile) {
//        try {
//            AudioInputStream audioInputStream = new AudioInputStream(
//                    new java.io.ByteArrayInputStream(audioData),
//                    AUDIO_FORMAT,
//                    audioData.length / AUDIO_FORMAT.getFrameSize()
//            );
//
//            AudioSystem.write(audioInputStream, AudioFileFormat.Type.WAVE, outputFile);
//            System.out.println("Audio saved to file: " + outputFile.getAbsolutePath());
////            Dialog.showAlert(AlertType.INFORMATION, "Save Success", "Đã lưu file âm thanh: " + outputFile.getName());
//        } catch (IOException e) {
    ////            Dialog.showAlert(AlertType.ERROR, "Save Error", "Failed to save audio to file: " + e.getMessage());
//        }
//    }

    public void saveAudioToFile(byte[] audioData, File outputFile) {
        if (audioData == null || audioData.length == 0) {
            System.err.println("No audio data to save");
            return;
        }
        try {
            AudioInputStream audioInputStream = new AudioInputStream(
                    new java.io.ByteArrayInputStream(audioData),
                    AUDIO_FORMAT,
                    audioData.length / AUDIO_FORMAT.getFrameSize()
            );
            AudioSystem.write(audioInputStream, AudioFileFormat.Type.WAVE, outputFile);
            System.out.println("Audio saved to file: " + outputFile.getAbsolutePath());
            audioInputStream.close();
        } catch (IOException e) {
            System.err.println("Failed to save audio to file: " + e.getMessage());
            e.printStackTrace();
        } catch (Exception e) {
            System.err.println("Unexpected error saving audio: " + e.getMessage());
            e.printStackTrace();
        }
    }

    public void playAudioFile(File audioFile) {
        if (!audioFile.exists()) {
//            Dialog.showAlert(AlertType.ERROR, "Playback Error", "File âm thanh không tồn tại: " + audioFile.getAbsolutePath());
            return;
        }

        CompletableFuture.runAsync(() -> {
            try {
                AudioInputStream audioInputStream = AudioSystem.getAudioInputStream(audioFile);
                AudioFormat format = audioInputStream.getFormat();

                DataLine.Info info = new DataLine.Info(Clip.class, format);
                playbackClip = (Clip) AudioSystem.getLine(info);

                playbackClip.open(audioInputStream);
                playbackClip.start();

                System.out.println("Playing audio file: " + audioFile.getName());
                //            Dialog.showAlert(AlertType.INFORMATION, "Playback Started", "Đang phát file âm thanh: " + audioFile.getName());

                while (playbackClip.isRunning()) {
                    try {
                        Thread.sleep(100);
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                        break;
                    }
                }

                playbackClip.close();
                System.out.println("Audio playback completed");

            } catch (UnsupportedAudioFileException e) {
                //            Dialog.showAlert(AlertType.ERROR, "Playback Error", "Định dạng file âm thanh không được hỗ trợ: " + e.getMessage());
            } catch (IOException e) {
                //            Dialog.showAlert(AlertType.ERROR, "Playback Error", "Không thể đọc file âm thanh: " + e.getMessage());
            } catch (LineUnavailableException e) {
                //            Dialog.showAlert(AlertType.ERROR, "Playback Error", "Không thể phát âm thanh: " + e.getMessage());
            }
        });
    }

    public void playAudio(byte[] audioData) {
        CompletableFuture.runAsync(() -> {
            try {
                AudioInputStream audioInputStream = new AudioInputStream(
                        new java.io.ByteArrayInputStream(audioData),
                        AUDIO_FORMAT,
                        audioData.length / AUDIO_FORMAT.getFrameSize()
                );

                DataLine.Info info = new DataLine.Info(Clip.class, AUDIO_FORMAT);
                playbackClip = (Clip) AudioSystem.getLine(info);

                playbackClip.open(audioInputStream);
                playbackClip.start();

                System.out.println("Playing audio data (" + audioData.length + " bytes)");
                //            Dialog.showAlert(AlertType.INFORMATION, "Playback Started", "Đang phát âm thanh...");

                while (playbackClip.isRunning()) {
                    try {
                        Thread.sleep(100);
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                        break;
                    }
                }

                playbackClip.close();
                System.out.println("Audio playback completed");

            } catch (LineUnavailableException e) {
                //            Dialog.showAlert(AlertType.ERROR, "Playback Error", "Không thể phát âm thanh: " + e.getMessage());
            } catch (IOException e) {
                //            Dialog.showAlert(AlertType.ERROR, "Playback Error", "Lỗi khi phát âm thanh: " + e.getMessage());
            }
        });
    }

    public void stopPlayback() {
        if (playbackClip != null && playbackClip.isRunning()) {
            playbackClip.stop();
            playbackClip.close();
            System.out.println("Audio playback stopped");
//            Dialog.showAlert(AlertType.INFORMATION, "Playback Stopped", "Đã dừng phát âm thanh.");
        }
    }

    public boolean isRecording() {
        return isRecording;
    }

    public boolean isPlaying() {
        return playbackClip != null && playbackClip.isRunning();
    }

    public String[] getSupportedFormats() {
        return new String[]{"wav", "au", "aiff"};
    }

    public AudioFormat getAudioFormat() {
        return AUDIO_FORMAT;
    }

    public interface AudioRecordingCallback {
        void onRecordingStarted();
        void onAudioData(byte[] data, int length);
        void onRecordingCompleted(byte[] audioData);
        void onError(String error);
    }
}