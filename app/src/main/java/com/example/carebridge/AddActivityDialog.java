package com.example.carebridge;

import android.Manifest;
import android.app.Activity;
import android.app.Dialog;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.speech.RecognitionListener;
import android.speech.RecognizerIntent;
import android.speech.SpeechRecognizer;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.google.firebase.database.DatabaseReference;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.Locale;

public class AddActivityDialog extends Dialog {

    private final String userId;
    private Spinner categorySpinner;
    private EditText titleInput;
    private EditText descInput;
    private EditText dateInput;
    private SpeechRecognizer speechRecognizer;

    public AddActivityDialog(@NonNull Context context, String userId) {
        super(context);
        this.userId = userId;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.dialog_add_activity);

        categorySpinner = findViewById(R.id.categorySpinner);
        titleInput = findViewById(R.id.titleInput);
        descInput = findViewById(R.id.descInput);
        dateInput = findViewById(R.id.dateInput);
        Button cancelButton = findViewById(R.id.cancelButton);
        Button saveButton = findViewById(R.id.saveButton);
        Button btnMic = findViewById(R.id.btnMic);

        speechRecognizer = SpeechRecognizer.createSpeechRecognizer(getContext());
        speechRecognizer.setRecognitionListener(new RecognitionListener() {
            @Override
            public void onReadyForSpeech(Bundle params) {}
            @Override
            public void onBeginningOfSpeech() {
                descInput.setHint("Listening...");
            }
            @Override
            public void onRmsChanged(float rmsdB) {}
            @Override
            public void onBufferReceived(byte[] buffer) {}
            @Override
            public void onEndOfSpeech() {
                descInput.setHint("Details about instructions...");
            }
            @Override
            public void onError(int error) {
                Toast.makeText(getContext(), "Speech recognition error: " + error, Toast.LENGTH_SHORT).show();
                descInput.setHint("Details about instructions...");
            }
            @Override
            public void onResults(Bundle results) {
                ArrayList<String> matches = results.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION);
                if (matches != null && !matches.isEmpty()) {
                    String current = descInput.getText().toString();
                    if (!current.isEmpty()) current += " ";
                    descInput.setText(current + matches.get(0));
                }
            }
            @Override
            public void onPartialResults(Bundle partialResults) {}
            @Override
            public void onEvent(int eventType, Bundle params) {}
        });

        if (btnMic != null) {
            btnMic.setOnClickListener(v -> {
                if (ContextCompat.checkSelfPermission(getContext(), Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
                    ActivityCompat.requestPermissions((Activity) getContext(), new String[]{Manifest.permission.RECORD_AUDIO}, 1);
                } else {
                    Intent intent = new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH);
                    intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM);
                    intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE, Locale.getDefault());
                    speechRecognizer.startListening(intent);
                }
            });
        }

        String[] categories = new String[]{
                "Doctor Visit",
                "Referral",
                "Follow-up",
                "Appointment",
                "Care Task"
        };

        ArrayAdapter<String> adapter = new ArrayAdapter<>(getContext(), android.R.layout.simple_spinner_item, categories);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        if (categorySpinner != null) {
            categorySpinner.setAdapter(adapter);
        }

        // Set default date
        if (dateInput != null) {
            SimpleDateFormat sdf = new SimpleDateFormat("MMM dd, yyyy", Locale.US);
            dateInput.setText(sdf.format(new Date()));
        }

        if (cancelButton != null) {
            cancelButton.setOnClickListener(v -> dismiss());
        }

        if (saveButton != null) {
            saveButton.setOnClickListener(v -> saveActivityToFirebase());
        }
    }

    @Override
    public void dismiss() {
        if (speechRecognizer != null) {
            speechRecognizer.destroy();
        }
        super.dismiss();
    }

    private void saveActivityToFirebase() {
        String category = categorySpinner.getSelectedItem() != null ? categorySpinner.getSelectedItem().toString() : "Care Event";
        String title = titleInput.getText().toString().trim();
        String desc = descInput.getText().toString().trim();
        String dateStr = dateInput.getText().toString().trim();

        if (title.isEmpty()) {
            Toast.makeText(getContext(), "Please enter a title for the activity.", Toast.LENGTH_SHORT).show();
            return;
        }

        DatabaseReference jRef = FirebaseHelper.getJourneyRef(userId).push();
        String itemId = jRef.getKey();
        long now = System.currentTimeMillis();

        JourneyItem item = new JourneyItem(
                itemId,
                userId,
                "0",
                category,
                title,
                desc,
                dateStr,
                now
        );

        jRef.setValue(item).addOnCompleteListener(task -> {
            if (task.isSuccessful()) {
                Toast.makeText(getContext(), "Activity added to your journey!", Toast.LENGTH_SHORT).show();

                // If Category is Care Task, also add to tasks node
                if ("Care Task".equals(category)) {
                    DatabaseReference tRef = FirebaseHelper.getTasksRef(userId).push();
                    CareTask cTask = new CareTask(
                            tRef.getKey(),
                            userId,
                            title,
                            desc,
                            dateStr,
                            false,
                            now
                    );
                    tRef.setValue(cTask);
                }

                // Post notification
                Intent homeIntent = new Intent(getContext(), activity_home.class);
                NotificationHelper.showNotification(
                        getContext(),
                        "Care Activity Logged",
                        "New " + category + ": " + title,
                        homeIntent
                );

                dismiss();
            } else {
                Toast.makeText(getContext(), "Failed to save activity.", Toast.LENGTH_SHORT).show();
            }
        });
    }
}
