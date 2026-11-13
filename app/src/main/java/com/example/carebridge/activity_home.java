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

import java.util.HashSet;
import java.util.Set;

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

            // Request notification permission on Android 13+ (API 33+)
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
                if (checkSelfPermission(android.Manifest.permission.POST_NOTIFICATIONS) != android.content.pm.PackageManager.PERMISSION_GRANTED) {
                    requestPermissions(new String[]{android.Manifest.permission.POST_NOTIFICATIONS}, 101);
                }
            }

            // Seed sample data in Firebase if new user
            FirebaseHelper.seedDefaultDataIfEmpty(userId);

            // Listen to real-time appointments and offers from Firebase
            listenToAppointments(userId);
            listenToOffers(userId);
        }

        Button viewJourneyButton = findViewById(R.id.viewJourneyButton);

        LinearLayout cardAppointments = findViewById(R.id.cardAppointments);
        LinearLayout cardCareTasks = findViewById(R.id.cardCareTasks);
        LinearLayout cardCareProgress = findViewById(R.id.cardCareProgress);
        LinearLayout cardHealthcareMap = findViewById(R.id.cardHealthcareMap);
        LinearLayout cardVoiceAssistant = findViewById(R.id.cardVoiceAssistant);
        LinearLayout cardProfile = findViewById(R.id.cardProfile);
        LinearLayout upcomingContainer = findViewById(R.id.upcomingAppointmentsContainer);

        // 1. View My Journey (Row 2, Left)
        if (viewJourneyButton != null) {
            viewJourneyButton.setOnClickListener(view -> openJourney());
        }
        if (cardCareProgress != null) {
            cardCareProgress.setOnClickListener(view -> openJourney());
        }

        // 2. View & Manage Appointments (Row 1, Left)
        if (cardAppointments != null) {
            cardAppointments.setOnClickListener(view -> openAppointmentsDialog());
        }
        if (upcomingContainer != null) {
            upcomingContainer.setOnClickListener(view -> openAppointmentsDialog());
        }

        // 3. View & Manage Care Tasks (Row 1, Right)
        if (cardCareTasks != null) {
            cardCareTasks.setOnClickListener(view -> openTasksDialog());
        }

        // 4. Healthcare Map (Row 2, Right)
        if (cardHealthcareMap != null) {
            cardHealthcareMap.setOnClickListener(view -> {
                Intent intent = new Intent(this, NearbyServicesActivity.class);
                startActivity(intent);
            });
        }
        
        // 5. Voice Assistant (Row 3, Left)
        if (cardVoiceAssistant != null) {
            cardVoiceAssistant.setOnClickListener(view -> {
                Intent intent = new Intent(this, VoiceAssistantActivity.class);
                startActivity(intent);
            });
        }

        // 6. User Profile (Row 3, Right)
        if (profileIcon != null) {
            profileIcon.setOnClickListener(view -> openProfile());
        }
        if (cardProfile != null) {
            cardProfile.setOnClickListener(view -> openProfile());
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
                long maxTimestamp = -1;
                for (DataSnapshot child : snapshot.getChildren()) {
                    Appointment appt = child.getValue(Appointment.class);
                    if (appt != null) {
                        if (appt.getTimestamp() >= maxTimestamp) {
                            maxTimestamp = appt.getTimestamp();
                            nextAppt = appt; // Get most recent appointment
                        }
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

    private void openProfile() {
        Intent intent = new Intent(this, activity_profile.class);
        startActivity(intent);
    }

    private void openAddActivityDialog() {
        if (!userSession.isLoggedIn()) {
            Toast.makeText(this, "Please log in first.", Toast.LENGTH_SHORT).show();
            return;
        }
        AddActivityDialog dialog = new AddActivityDialog(this, userSession.getUserId());
        dialog.show();
    }

    private final Set<String> notifiedOfferIds = new HashSet<>();

    private void listenToOffers(String userId) {
        ValueEventListener offerListener = new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                FirebaseHelper.getAllOffersRef().addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot globalSnapshot) {
                        checkOffersSnapshot(snapshot, userId);
                        checkOffersSnapshot(globalSnapshot, userId);
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {
                        checkOffersSnapshot(snapshot, userId);
                    }
                });
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {}
        };

        FirebaseHelper.getOffersRef(userId).addValueEventListener(offerListener);
        FirebaseHelper.getAllOffersRef().addValueEventListener(offerListener);
    }

    private void checkOffersSnapshot(DataSnapshot snapshot, String userId) {
        for (DataSnapshot child : snapshot.getChildren()) {
            AppointmentOffer offer = child.getValue(AppointmentOffer.class);
            if (offer != null) {
                String offerId = offer.getId() != null ? offer.getId() : child.getKey();
                boolean isForUser = offer.getUserId() != null && offer.getUserId().equalsIgnoreCase(userId);
                if (isForUser && ("PENDING".equalsIgnoreCase(offer.getStatus()) || "AVAILABLE".equalsIgnoreCase(offer.getStatus()))) {
                    if (offerId != null && !notifiedOfferIds.contains(offerId)) {
                        notifiedOfferIds.add(offerId);
                        triggerOfferNotification(offer);
                    }
                }
            }
        }
    }

    private void triggerOfferNotification(AppointmentOffer offer) {
        Intent homeIntent = new Intent(this, activity_home.class);
        if (offer.getId() != null) {
            homeIntent.putExtra("offerId", offer.getId());
        }
        homeIntent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);

        String provider = offer.getProviderName() != null ? offer.getProviderName() : "Doctor";
        NotificationHelper.showNotification(
                this,
                "Appointment Offer Available!",
                "A follow-up appointment with " + provider + " is available. Tap to pick a time slot.",
                homeIntent
        );
    }
}
