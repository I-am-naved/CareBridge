package com.example.carebridge;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;

import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.ValueEventListener;

public class activity_home extends Activity {

    private UserSession userSession;
    private TextView upcomingTitle;
    private TextView upcomingSubtitle;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_home);

        userSession = new UserSession(this);

        TextView welcomeTitle = findViewById(R.id.welcomeTitle);
        TextView profileIcon = findViewById(R.id.profileIcon);
        upcomingTitle = findViewById(R.id.upcomingTitle);
        upcomingSubtitle = findViewById(R.id.upcomingSubtitle);

        if (userSession.isLoggedIn()) {
            String name = userSession.getFullName();
            if (name.isEmpty()) {
                name = userSession.getUserId();
            }

            if (!name.isEmpty()) {
                if (welcomeTitle != null) {
                    welcomeTitle.setText("Hello, " + name + "!");
                }

                if (profileIcon != null) {
                    profileIcon.setText(getInitials(name));
                }
            }

            String userId = userSession.getUserId();

            // Seed sample data in Firebase if new user
            FirebaseHelper.seedDefaultDataIfEmpty(userId);

            // Listen to real-time appointments from Firebase
            listenToAppointments(userId);
        }

        Button viewJourneyButton = findViewById(R.id.viewJourneyButton);

        LinearLayout cardAppointments = findViewById(R.id.cardAppointments);
        LinearLayout cardCareTasks = findViewById(R.id.cardCareTasks);
        LinearLayout cardAddActivity = findViewById(R.id.cardAddActivity);
        LinearLayout cardCareProgress = findViewById(R.id.cardCareProgress);
        LinearLayout upcomingContainer = findViewById(R.id.upcomingAppointmentsContainer);

        // 1. View My Journey
        if (viewJourneyButton != null) {
            viewJourneyButton.setOnClickListener(view -> openJourney());
        }
        if (cardCareProgress != null) {
            cardCareProgress.setOnClickListener(view -> openJourney());
        }

        // 2. View & Manage Appointments
        if (cardAppointments != null) {
            cardAppointments.setOnClickListener(view -> openAppointmentsDialog());
        }
        if (upcomingContainer != null) {
            upcomingContainer.setOnClickListener(view -> openAppointmentsDialog());
        }

        // 3. View & Manage Care Tasks
        if (cardCareTasks != null) {
            cardCareTasks.setOnClickListener(view -> openTasksDialog());
        }

        // 4. Add Care Activity
        if (cardAddActivity != null) {
            cardAddActivity.setOnClickListener(view -> openAddActivityDialog());
        }

        // 5. User Profile
        if (profileIcon != null) {
            profileIcon.setOnClickListener(view -> {
                Intent intent = new Intent(this, activity_profile.class);
                startActivity(intent);
            });
        }

        // Check if opened from notification with an offer
        if (getIntent() != null && getIntent().hasExtra("offerId")) {
            openAppointmentsDialog();
        }
    }

    private void listenToAppointments(String userId) {
        FirebaseHelper.getAppointmentsRef(userId).addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                Appointment nextAppt = null;
                for (DataSnapshot child : snapshot.getChildren()) {
                    Appointment appt = child.getValue(Appointment.class);
                    if (appt != null) {
                        nextAppt = appt; // Get latest
                    }
                }

                if (nextAppt != null) {
                    if (upcomingTitle != null) {
                        upcomingTitle.setText(nextAppt.getProviderName() + " (" + nextAppt.getDepartment() + ")");
                    }
                    if (upcomingSubtitle != null) {
                        String dt = nextAppt.getDate() + (nextAppt.getTime() != null && !nextAppt.getTime().isEmpty() ? " at " + nextAppt.getTime() : "");
                        upcomingSubtitle.setText("📅 " + dt + "\n📍 " + nextAppt.getLocation());
                    }
                } else {
                    if (upcomingTitle != null) {
                        upcomingTitle.setText("No appointments to display");
                    }
                    if (upcomingSubtitle != null) {
                        upcomingSubtitle.setText("Your scheduled appointments will appear here.");
                    }
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                // Silently handle
            }
        });
    }

    private String getInitials(String name) {
        if (name == null || name.trim().isEmpty()) {
            return "ME";
        }
        String[] parts = name.trim().split("\\s+");
        if (parts.length >= 2) {
            return (parts[0].substring(0, 1) + parts[1].substring(0, 1)).toUpperCase();
        } else if (parts[0].length() >= 2) {
            return parts[0].substring(0, 2).toUpperCase();
        } else {
            return parts[0].toUpperCase();
        }
    }

    private void openJourney() {
        Intent intent = new Intent(this, activity_journey.class);
        startActivity(intent);
    }

    private void openAppointmentsDialog() {
        if (!userSession.isLoggedIn()) {
            Toast.makeText(this, "Please log in first.", Toast.LENGTH_SHORT).show();
            return;
        }
        AppointmentsDialog dialog = new AppointmentsDialog(this, userSession.getUserId());
        dialog.show();
    }

    private void openTasksDialog() {
        if (!userSession.isLoggedIn()) {
            Toast.makeText(this, "Please log in first.", Toast.LENGTH_SHORT).show();
            return;
        }
        TasksDialog dialog = new TasksDialog(this, userSession.getUserId());
        dialog.show();
    }

    private void openAddActivityDialog() {
        if (!userSession.isLoggedIn()) {
            Toast.makeText(this, "Please log in first.", Toast.LENGTH_SHORT).show();
            return;
        }
        AddActivityDialog dialog = new AddActivityDialog(this, userSession.getUserId());
        dialog.show();
    }
}
