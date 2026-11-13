package com.example.carebridge;

import android.Manifest;
import android.app.Activity;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.speech.RecognitionListener;
import android.speech.RecognizerIntent;
import android.speech.SpeechRecognizer;
import android.view.View;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.core.app.ActivityCompat;

import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class VoiceAssistantActivity extends Activity {

    private static final int RECORD_AUDIO_REQUEST_CODE = 1002;

    private UserSession userSession;
    private SpeechRecognizer speechRecognizer;

    private TextView tvVoiceStatus;
    private LinearLayout cardResponse;
    private TextView tvTranscript;
    private TextView tvAnswer;

    private String lastAnswerText = "";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_voice_assistant);

        userSession = new UserSession(this);

        TextView btnBack = findViewById(R.id.btnBackVoice);
        ImageButton btnMicAssistant = findViewById(R.id.btnMicAssistant);
        tvVoiceStatus = findViewById(R.id.tvVoiceStatus);
        cardResponse = findViewById(R.id.cardResponse);
        tvTranscript = findViewById(R.id.tvTranscript);
        tvAnswer = findViewById(R.id.tvAnswer);

        LinearLayout promptNextAppt = findViewById(R.id.promptNextAppt);
        LinearLayout promptTasks = findViewById(R.id.promptTasks);
        LinearLayout promptWhereAppt = findViewById(R.id.promptWhereAppt);
        LinearLayout promptJourney = findViewById(R.id.promptJourney);

        if (btnBack != null) {
            btnBack.setOnClickListener(v -> finish());
        }

        btnMicAssistant.setOnClickListener(v -> startListening());

        if (promptNextAppt != null) {
            promptNextAppt.setOnClickListener(v -> processCommand("what is my next appointment"));
        }

        if (promptTasks != null) {
            promptTasks.setOnClickListener(v -> processCommand("what tasks do i still need to complete"));
        }

        if (promptWhereAppt != null) {
            promptWhereAppt.setOnClickListener(v -> processCommand("where is my next appointment"));
        }

        if (promptJourney != null) {
            promptJourney.setOnClickListener(v -> processCommand("show my healthcare journey"));
        }
    }

    private void startListening() {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.RECORD_AUDIO}, RECORD_AUDIO_REQUEST_CODE);
            return;
        }

        if (SpeechRecognizer.isRecognitionAvailable(this)) {
            if (speechRecognizer != null) {
                speechRecognizer.destroy();
            }
            speechRecognizer = SpeechRecognizer.createSpeechRecognizer(this);
            Intent intent = new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH);
            intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM);
            intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE, Locale.getDefault());

            speechRecognizer.setRecognitionListener(new RecognitionListener() {
                @Override
                public void onReadyForSpeech(Bundle params) {
                    if (tvVoiceStatus != null) tvVoiceStatus.setText("Listening...");
                }

                @Override
                public void onBeginningOfSpeech() {}

                @Override
                public void onRmsChanged(float rmsdB) {}

                @Override
                public void onBufferReceived(byte[] buffer) {}

                @Override
                public void onEndOfSpeech() {
                    if (tvVoiceStatus != null) tvVoiceStatus.setText("Processing request...");
                }

                @Override
                public void onError(int error) {
                    if (tvVoiceStatus != null) tvVoiceStatus.setText("Tap to speak");
                    Toast.makeText(VoiceAssistantActivity.this, "Speech recognition error: " + error, Toast.LENGTH_SHORT).show();
                }

                @Override
                public void onResults(Bundle results) {
                    if (tvVoiceStatus != null) tvVoiceStatus.setText("Tap to speak");
                    ArrayList<String> matches = results.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION);
                    if (matches != null && !matches.isEmpty()) {
                        String speechText = matches.get(0);
                        processCommand(speechText);
                    }
                }

                @Override
                public void onPartialResults(Bundle partialResults) {}

                @Override
                public void onEvent(int eventType, Bundle params) {}
            });

            speechRecognizer.startListening(intent);
        } else {
            Toast.makeText(this, "Speech recognition is not available on this device.", Toast.LENGTH_SHORT).show();
        }
    }

    private void processCommand(String text) {
        if (text == null) return;
        String lower = text.toLowerCase().trim();

        if (tvTranscript != null) tvTranscript.setText("You asked: \"" + text + "\"");
        if (cardResponse != null) cardResponse.setVisibility(View.VISIBLE);

        String userId = userSession.getUserId();

        if (lower.contains("next appointment") || lower.contains("upcoming appointment") || lower.contains("when is my appointment")) {
            queryNextAppointment(userId, false);
        } else if (lower.contains("where") || lower.contains("location")) {
            queryNextAppointment(userId, true);
        } else if (lower.contains("task") || lower.contains("todo") || lower.contains("complete")) {
            queryCareTasks(userId);
        } else if (lower.contains("journey") || lower.contains("progress")) {
            lastAnswerText = "Opening your Healthcare Journey history now.";
            if (tvAnswer != null) tvAnswer.setText(lastAnswerText);
            Intent intent = new Intent(this, activity_journey.class);
            startActivity(intent);
        } else {
            lastAnswerText = "I didn't quite understand that command. Try asking 'What is my next appointment?' or 'What tasks do I have?'";
            if (tvAnswer != null) tvAnswer.setText(lastAnswerText);
        }
    }

    private void queryNextAppointment(String userId, boolean locationOnly) {
        FirebaseHelper.getAppointmentsRef(userId).addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                Appointment nextAppt = null;
                long maxTimestamp = -1;
                for (DataSnapshot child : snapshot.getChildren()) {
                    Appointment appt = child.getValue(Appointment.class);
                    if (appt != null && appt.getTimestamp() >= maxTimestamp) {
                        maxTimestamp = appt.getTimestamp();
                        nextAppt = appt;
                    }
                }

                if (nextAppt != null) {
                    if (locationOnly) {
                        lastAnswerText = "Your next appointment is at " + nextAppt.getLocation() + " with " + nextAppt.getProviderName() + ".";
                    } else {
                        String dt = nextAppt.getDate() + (nextAppt.getTime() != null ? " at " + nextAppt.getTime() : "");
                        lastAnswerText = "Your next appointment is a " + nextAppt.getDepartment() + " with " + nextAppt.getProviderName() + " on " + dt + " at " + nextAppt.getLocation() + ".";
                    }
                } else {
                    lastAnswerText = "You currently have no scheduled appointments in CareBridge.";
                }

                if (tvAnswer != null) tvAnswer.setText(lastAnswerText);
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                lastAnswerText = "Failed to retrieve appointment information.";
                if (tvAnswer != null) tvAnswer.setText(lastAnswerText);
            }
        });
    }

    private void queryCareTasks(String userId) {
        FirebaseHelper.getTasksRef(userId).addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                List<CareTask> taskList = new ArrayList<>();
                for (DataSnapshot child : snapshot.getChildren()) {
                    CareTask task = child.getValue(CareTask.class);
                    if (task != null && !task.isCompleted()) {
                        taskList.add(task);
                    }
                }

                if (!taskList.isEmpty()) {
                    StringBuilder sb = new StringBuilder("You have " + taskList.size() + " pending care tasks: ");
                    for (int i = 0; i < taskList.size(); i++) {
                        sb.append(taskList.get(i).getTitle());
                        if (i < taskList.size() - 1) sb.append(", ");
                    }
                    lastAnswerText = sb.toString();
                } else {
                    lastAnswerText = "Great job! You have no pending care tasks to complete.";
                }

                if (tvAnswer != null) tvAnswer.setText(lastAnswerText);
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                lastAnswerText = "Failed to retrieve care tasks.";
                if (tvAnswer != null) tvAnswer.setText(lastAnswerText);
            }
        });
    }

    // Removed speakAnswer and onInit methods

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == RECORD_AUDIO_REQUEST_CODE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                startListening();
            } else {
                Toast.makeText(this, "Microphone permission is required for voice commands.", Toast.LENGTH_SHORT).show();
            }
        }
    }

    @Override
    protected void onDestroy() {
        if (speechRecognizer != null) {
            speechRecognizer.destroy();
        }
        super.onDestroy();
    }
}
