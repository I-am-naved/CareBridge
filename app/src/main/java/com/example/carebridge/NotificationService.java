package com.example.carebridge;

import android.app.Notification;
import android.app.Service;
import android.content.Intent;
import android.content.pm.ServiceInfo;
import android.os.Build;
import android.os.IBinder;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.app.NotificationCompat;

import com.google.firebase.database.ChildEventListener;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;

public class NotificationService extends Service {

    private DatabaseReference offersRef;
    private ChildEventListener offersListener;
    private boolean isListening = false;
    private long serviceStartTime;

    @Override
    public void onCreate() {
        super.onCreate();
        NotificationHelper.createNotificationChannel(this);
        serviceStartTime = System.currentTimeMillis();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        // Start as foreground service to keep it alive
        Notification notification = new NotificationCompat.Builder(this, NotificationHelper.CHANNEL_ID)
                .setContentTitle("CareBridge Active")
                .setContentText("Listening for updates...")
                .setSmallIcon(R.drawable.ic_launcher_foreground)
                .setPriority(NotificationCompat.PRIORITY_MIN)
                .build();

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            startForeground(1001, notification, ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC);
        } else {
            startForeground(1001, notification);
        }

        UserSession session = new UserSession(this);
        if (session.isLoggedIn() && !session.isAdmin() && !isListening) {
            String userId = session.getUserId();
            startListeningForOffers(userId);
        }

        return START_STICKY; // Restart if killed by system
    }

    private void startListeningForOffers(String userId) {
        offersRef = FirebaseHelper.getOffersRef(userId);
        isListening = true;

        offersListener = new ChildEventListener() {
            @Override
            public void onChildAdded(@NonNull DataSnapshot snapshot, @Nullable String previousChildName) {
                AppointmentOffer offer = snapshot.getValue(AppointmentOffer.class);
                if (offer != null) {
                    // Only notify for offers created AFTER the service started to prevent spam on login
                    if (offer.getTimestamp() >= serviceStartTime) {
                        if ("PENDING".equalsIgnoreCase(offer.getStatus()) || "AVAILABLE".equalsIgnoreCase(offer.getStatus())) {
                            triggerOfferNotification(offer);
                        }
                    }
                }
            }

            @Override
            public void onChildChanged(@NonNull DataSnapshot snapshot, @Nullable String previousChildName) {}

            @Override
            public void onChildRemoved(@NonNull DataSnapshot snapshot) {}

            @Override
            public void onChildMoved(@NonNull DataSnapshot snapshot, @Nullable String previousChildName) {}

            @Override
            public void onCancelled(@NonNull DatabaseError error) {}
        };

        offersRef.addChildEventListener(offersListener);
    }

    private void triggerOfferNotification(AppointmentOffer offer) {
        Intent homeIntent = new Intent(this, activity_home.class);
        if (offer.getId() != null) {
            homeIntent.putExtra("offerId", offer.getId());
        }
        homeIntent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);

        String provider = offer.getProviderName() != null ? offer.getProviderName() : "Doctor";
        NotificationHelper.showNotification(
                this,
                "New Appointment Offer!",
                "A follow-up appointment with " + provider + " is available. Tap to pick a time slot.",
                homeIntent
        );
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (offersRef != null && offersListener != null) {
            offersRef.removeEventListener(offersListener);
        }
        isListening = false;
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }
}
