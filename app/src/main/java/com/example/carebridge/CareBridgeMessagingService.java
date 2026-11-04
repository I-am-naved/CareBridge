package com.example.carebridge;

import android.content.Intent;
import android.util.Log;

import androidx.annotation.NonNull;

import com.google.firebase.messaging.FirebaseMessagingService;
import com.google.firebase.messaging.RemoteMessage;

public class CareBridgeMessagingService extends FirebaseMessagingService {

    private static final String TAG = "CareBridgeFCM";

    @Override
    public void onNewToken(@NonNull String token) {
        super.onNewToken(token);
        Log.d(TAG, "Refreshed FCM Token: " + token);
    }

    @Override
    public void onMessageReceived(@NonNull RemoteMessage remoteMessage) {
        super.onMessageReceived(remoteMessage);

        String title = "CareBridge Notification";
        String body = "You have a new update in your CareBridge account.";

        if (remoteMessage.getNotification() != null) {
            if (remoteMessage.getNotification().getTitle() != null) {
                title = remoteMessage.getNotification().getTitle();
            }
            if (remoteMessage.getNotification().getBody() != null) {
                body = remoteMessage.getNotification().getBody();
            }
        } else if (!remoteMessage.getData().isEmpty()) {
            if (remoteMessage.getData().containsKey("title")) {
                title = remoteMessage.getData().get("title");
            }
            if (remoteMessage.getData().containsKey("body")) {
                body = remoteMessage.getData().get("body");
            }
        }

        Intent intent = new Intent(this, activity_home.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);

        if (remoteMessage.getData().containsKey("offerId")) {
            intent.putExtra("offerId", remoteMessage.getData().get("offerId"));
        }

        NotificationHelper.showNotification(this, title, body, intent);
    }
}
