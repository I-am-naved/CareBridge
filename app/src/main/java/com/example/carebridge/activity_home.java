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
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class activity_home extends Activity {

    private UserSession userSession;
    private TextView upcomingTitle;
    private TextView upcomingSubtitle;
    private com.google.firebase.database.DatabaseReference appointmentsRef;
    private ValueEventListener appointmentsListener;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_home);

        userSession = new UserSession(this);

        TextView welcomeTitle = findViewById(R.id.welcomeTitle);
        TextView greetingText = findViewById(R.id.greetingText);
        android.view.View profileIcon = findViewById(R.id.profileIcon);
        upcomingTitle = findViewById(R.id.upcomingTitle);
        upcomingSubtitle = findViewById(R.id.upcomingSubtitle);
        
        if (greetingText != null) {
            java.util.Calendar c = java.util.Calendar.getInstance();
            int timeOfDay = c.get(java.util.Calendar.HOUR_OF_DAY);

            if (timeOfDay >= 0 && timeOfDay < 12) {
                greetingText.setText("Good Morning,");
            } else if (timeOfDay >= 12 && timeOfDay < 16) {
                greetingText.setText("Good Afternoon,");
            } else if (timeOfDay >= 16 && timeOfDay < 21) {
                greetingText.setText("Good Evening,");
            } else {
                greetingText.setText("Good Night,");
            }
        }

        if (userSession.isLoggedIn()) {
            String name = userSession.getFullName();
            if (name.isEmpty()) {
                name = userSession.getUserId();
            }

            if (!name.isEmpty()) {
                if (welcomeTitle != null) {
                    welcomeTitle.setText("Hello, " + name + "!");
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

        // Bottom Navigation Bar
        LinearLayout navHome = findViewById(R.id.navHome);
        LinearLayout navAppointments = findViewById(R.id.navAppointments);
        LinearLayout navMap = findViewById(R.id.navMap);
        LinearLayout navProfile = findViewById(R.id.navProfile);

        if (navHome != null) navHome.setOnClickListener(v -> {}); // Already on Home
        if (navAppointments != null) navAppointments.setOnClickListener(v -> openAppointmentsDialog());
        if (navMap != null) navMap.setOnClickListener(v -> {
            Intent intent = new Intent(this, NearbyServicesActivity.class);
            startActivity(intent);
        });
        if (navProfile != null) navProfile.setOnClickListener(v -> openProfile());

        // If opened from a local notification, take the patient to appointment offers.
        handleNotificationIntent(getIntent());
    }

    private void listenToAppointments(String userId) {
        appointmentsRef = FirebaseHelper.getAppointmentsRef(userId);
        appointmentsListener = new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                Appointment nextAppointment = null;
                long bestTime = Long.MAX_VALUE;
                long now = System.currentTimeMillis();

                for (DataSnapshot child : snapshot.getChildren()) {
                    Appointment appointment = child.getValue(Appointment.class);
                    if (appointment == null) continue;
                    String status = appointment.getStatus() == null ? "CONFIRMED" : appointment.getStatus();
                    if (!"CONFIRMED".equalsIgnoreCase(status) && !"SCHEDULED".equalsIgnoreCase(status)) continue;

                    long appointmentTime = parseAppointmentDateTime(appointment);
                    if (appointmentTime > 0) {
                        if (appointmentTime >= now && appointmentTime < bestTime) {
                            bestTime = appointmentTime;
                            nextAppointment = appointment;
                        }
                    } else if (nextAppointment == null || appointment.getTimestamp() > nextAppointment.getTimestamp()) {
                        // Fallback for legacy records whose date format cannot be parsed.
                        nextAppointment = appointment;
                    }
                }

                if (nextAppointment != null) {
                    if (upcomingTitle != null) {
                        upcomingTitle.setText(safe(nextAppointment.getProviderName(), "Upcoming appointment"));
                    }
                    if (upcomingSubtitle != null) {
                        String dt = safe(nextAppointment.getDate(), "Date not set") +
                                (nextAppointment.getTime() == null || nextAppointment.getTime().isEmpty()
                                        ? "" : " at " + nextAppointment.getTime());
                        upcomingSubtitle.setText("📅 " + dt + "\n📍 " + safe(nextAppointment.getLocation(), "Location not provided"));
                    }
                } else {
                    if (upcomingTitle != null) upcomingTitle.setText("No upcoming appointments");
                    if (upcomingSubtitle != null) upcomingSubtitle.setText("Appointment offers and confirmed visits will appear here.");
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                if (upcomingTitle != null) upcomingTitle.setText("Unable to load appointments");
                if (upcomingSubtitle != null) upcomingSubtitle.setText(error.getMessage());
            }
        };
        appointmentsRef.addValueEventListener(appointmentsListener);
    }

    private long parseAppointmentDateTime(Appointment appointment) {
        String date = appointment.getDate();
        if (date == null || date.trim().isEmpty()) return -1L;
        String time = appointment.getTime() == null ? "" : appointment.getTime().trim();
        String value = date.trim() + (time.isEmpty() ? "" : " " + time);
        String[] patterns = {
                "MMM dd, yyyy h:mm a", "MMM d, yyyy h:mm a",
                "MMMM dd, yyyy h:mm a", "MMMM d, yyyy h:mm a",
                "yyyy-MM-dd HH:mm", "yyyy-MM-dd h:mm a",
                "MMM dd, yyyy", "MMM d, yyyy",
                "MMMM dd, yyyy", "MMMM d, yyyy"
        };
        for (String pattern : patterns) {
            try {
                SimpleDateFormat format = new SimpleDateFormat(pattern, Locale.US);
                format.setLenient(false);
                Date parsed = format.parse(value);
                if (parsed != null) return parsed.getTime();
            } catch (ParseException ignored) { }
        }
        return -1L;
    }

    private String safe(String value, String fallback) {
        return value == null || value.trim().isEmpty() ? fallback : value;
    }


    private void openJourney() {
        Intent intent = new Intent(this, activity_journey.class);
        startActivity(intent);
    }

    private void openAppointmentsDialog() {
        android.util.Log.d("CareBridge", "openAppointmentsDialog called");
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

    private final Set<String> knownOfferIds = new HashSet<>();
    private boolean initialOffersLoaded = false;
    private ValueEventListener offersListener;
    private ValueEventListener notificationsListener;

    private void listenToOffers(String userId) {
        offersListener = new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (isFinishing()) return;

                Set<String> currentIds = new HashSet<>();
                for (DataSnapshot child : snapshot.getChildren()) {
                    AppointmentOffer offer = child.getValue(AppointmentOffer.class);
                    if (offer == null) continue;
                    String offerId = offer.getId() != null ? offer.getId() : child.getKey();
                    if (offerId == null) continue;
                    currentIds.add(offerId);

                    String status = offer.getStatus() == null ? "PENDING" : offer.getStatus();
                    boolean pending = "PENDING".equalsIgnoreCase(status) || "AVAILABLE".equalsIgnoreCase(status);
                    if (initialOffersLoaded && pending && !knownOfferIds.contains(offerId)) {
                        showOfferNotification(offerId, offer.getProviderName());
                    }
                }

                knownOfferIds.clear();
                knownOfferIds.addAll(currentIds);
                initialOffersLoaded = true;
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Toast.makeText(activity_home.this,
                        "Unable to check appointment offers: " + error.getMessage(),
                        Toast.LENGTH_SHORT).show();
            }
        };
        FirebaseHelper.getOffersRef(userId).addValueEventListener(offersListener);
        
        // Simulating the backend push notification delivery for testing purposes:
        // We listen to /notifications/ where recipientId == userId and status == "pending"
        notificationsListener = new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (isFinishing()) return;
                for (DataSnapshot child : snapshot.getChildren()) {
                    String recipientId = child.child("recipientId").getValue(String.class);
                    String status = child.child("status").getValue(String.class);
                    if (userId.equals(recipientId) && "pending".equals(status)) {
                        // We found a pending notification for this user
                        String title = child.child("title").getValue(String.class);
                        String body = child.child("body").getValue(String.class);
                        String offerId = child.child("offerId").getValue(String.class);
                        String taskId = child.child("taskId").getValue(String.class);
                        
                        Intent intent = new Intent(activity_home.this, activity_home.class);
                        if (offerId != null) intent.putExtra("offerId", offerId);
                        if (taskId != null) intent.putExtra("taskId", taskId);
                        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);

                        NotificationHelper.showNotification(
                                activity_home.this,
                                title != null ? title : "CareBridge",
                                body != null ? body : "You have a new update.",
                                intent
                        );
                        
                        // Mark as delivered so it doesn't pop again
                        child.getRef().child("status").setValue("delivered");
                    }
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {}
        };
        // In a real app with a backend, the backend would read this node and send the FCM push.
        // For testing without a backend, we read it locally.
        FirebaseHelper.getRootRef().child("notifications").addValueEventListener(notificationsListener);
    }

    private void showOfferNotification(String offerId, String providerName) {
        Intent intent = new Intent(this, activity_home.class);
        intent.putExtra("offerId", offerId);
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);

        String provider = providerName == null || providerName.trim().isEmpty()
                ? "your healthcare provider" : providerName;
        NotificationHelper.showNotification(
                this,
                "New appointment offer",
                "An appointment offer from " + provider + " is ready to review.",
                intent
        );
    }

    private void handleNotificationIntent(Intent intent) {
        if (intent != null) {
            if (intent.hasExtra("taskId")) {
                openTasksDialog();
                intent.removeExtra("taskId");
            } else if (intent.hasExtra("offerId")) {
                openAppointmentsDialog();
                intent.removeExtra("offerId");
            }
        }
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        setIntent(intent);
        handleNotificationIntent(intent);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (offersListener != null && userSession != null && userSession.getUserId() != null) {
            FirebaseHelper.getOffersRef(userSession.getUserId()).removeEventListener(offersListener);
        }
        if (notificationsListener != null) {
            FirebaseHelper.getRootRef().child("notifications").removeEventListener(notificationsListener);
        }
        if (appointmentsRef != null && appointmentsListener != null) {
            appointmentsRef.removeEventListener(appointmentsListener);
        }
    }

}
