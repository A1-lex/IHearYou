package com.example.ihearyou;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.os.Build;
import android.os.Bundle;
import android.speech.RecognitionListener;
import android.speech.RecognizerIntent;
import android.speech.SpeechRecognizer;
import android.speech.tts.TextToSpeech;
import android.widget.ImageButton;
import android.widget.RelativeLayout;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import java.util.ArrayList;
import java.util.Locale;

public class MainActivity extends AppCompatActivity {

    private static final int REQUEST_RECORD_AUDIO_PERMISSION_CODE = 1;
    private TextToSpeech tts;
    private SpeechRecognizer speechRecognizer;
    private RelativeLayout rootLayout;
    private ImageButton btnSpeak;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // Initialize views
        rootLayout = findViewById(R.id.rootLayout);
        btnSpeak = findViewById(R.id.btnSpeak);

        // Initialize TextToSpeech
        tts = new TextToSpeech(this, status -> {
            if (status == TextToSpeech.SUCCESS) {
                tts.setLanguage(Locale.US);
                speakOut("IHearYou is ready. Tap the microphone to speak.");
            } else {
                Toast.makeText(MainActivity.this, "TTS initialization failed", Toast.LENGTH_SHORT).show();
            }
        });

        // Check for microphone permission and initialize SpeechRecognizer if granted
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO)
                != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.RECORD_AUDIO},
                    REQUEST_RECORD_AUDIO_PERMISSION_CODE);
        } else {
            initializeSpeechRecognizer();
        }

        // Set button click listener to start voice recognition
        btnSpeak.setOnClickListener(v -> startVoiceRecognition());
    }

    // Initialize SpeechRecognizer, using on-device if available (API 31+)
    private void initializeSpeechRecognizer() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S &&
                SpeechRecognizer.isOnDeviceRecognitionAvailable(this)) {
            speechRecognizer = SpeechRecognizer.createOnDeviceSpeechRecognizer(this);
        } else if (SpeechRecognizer.isRecognitionAvailable(this)) {
            speechRecognizer = SpeechRecognizer.createSpeechRecognizer(this);
        } else {
            Toast.makeText(this, "Speech recognition not available", Toast.LENGTH_SHORT).show();
            return;
        }
        speechRecognizer.setRecognitionListener(new RecognitionListener() {
            @Override
            public void onReadyForSpeech(Bundle params) {
                // Optional: update UI to indicate readiness
                Toast.makeText(MainActivity.this, "Ready for speech", Toast.LENGTH_SHORT).show();
            }

            @Override
            public void onBeginningOfSpeech() {
                // Optional: update UI
            }

            @Override
            public void onRmsChanged(float rmsdB) {
                // No action needed
            }

            @Override
            public void onBufferReceived(byte[] buffer) {
                // No action needed
            }

            @Override
            public void onEndOfSpeech() {
                // Optional: update UI
            }

            @Override
            public void onError(int error) {
                Toast.makeText(MainActivity.this, "Speech recognition error: " + error, Toast.LENGTH_SHORT).show();
            }

            @Override
            public void onResults(Bundle results) {
                ArrayList<String> matches = results.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION);
                if (matches != null && !matches.isEmpty()) {
                    String recognizedText = matches.get(0);
                    processVoiceCommand(recognizedText);
                } else {
                    Toast.makeText(MainActivity.this, "No match found.", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onPartialResults(Bundle partialResults) {
                // No action needed
            }

            @Override
            public void onEvent(int eventType, Bundle params) {
                // No action needed
            }
        });
    }

    // Handle runtime permission result
    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == REQUEST_RECORD_AUDIO_PERMISSION_CODE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                Toast.makeText(this, "Permission Granted", Toast.LENGTH_SHORT).show();
                initializeSpeechRecognizer();
            } else {
                Toast.makeText(this, "Permission Denied", Toast.LENGTH_SHORT).show();
            }
        }
    }

    // Start voice recognition if SpeechRecognizer is initialized
    private void startVoiceRecognition() {
        if (speechRecognizer != null) {
            Intent intent = new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH);
            intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM);
            intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE, Locale.getDefault());
            intent.putExtra(RecognizerIntent.EXTRA_PROMPT, "Say 'Blue' or 'Red'");
            // For devices with API 31+, prefer offline recognition if available
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                intent.putExtra(RecognizerIntent.EXTRA_PREFER_OFFLINE, true);
            }
            speechRecognizer.startListening(intent);
        } else {
            Toast.makeText(this, "Speech recognizer not initialized", Toast.LENGTH_SHORT).show();
        }
    }

    // Process the recognized voice command and update the UI
    private void processVoiceCommand(String command) {
        command = command.toLowerCase();
        if (command.contains("blue")) {
            rootLayout.setBackgroundColor(Color.BLUE);
            speakOut("Here is the blue screen");
        } else if (command.contains("red")) {
            rootLayout.setBackgroundColor(Color.RED);
            speakOut("Here is the red screen");
        } else {
            speakOut("I did not understand the command. Please say blue or red.");
        }
    }

    // Helper method to use TTS to speak a message
    private void speakOut(String message) {
        tts.speak(message, TextToSpeech.QUEUE_FLUSH, null, null);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (tts != null) {
            tts.stop();
            tts.shutdown();
        }
        if (speechRecognizer != null) {
            speechRecognizer.destroy();
        }
    }
}
