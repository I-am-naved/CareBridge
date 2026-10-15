package com.example.carebridge;

import android.app.Activity;
import android.os.Bundle;
import android.content.Intent;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;

public class activity_home extends Activity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_home);

        UserSession userSession = new UserSession(this);

        TextView welcomeTitle = findViewById(R.id.welcomeTitle);
        TextView profileIcon = findViewById(R.id.profileIcon);

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
        }

        Button viewJourneyButton = findViewById(R.id.viewJourneyButton);

        LinearLayout cardAppointments = findViewById(R.id.cardAppointments);
        LinearLayout cardCareTasks = findViewById(R.id.cardCareTasks);
        LinearLayout cardAddActivity = findViewById(R.id.cardAddActivity);
        LinearLayout cardCareProgress = findViewById(R.id.cardCareProgress);

        if (viewJourneyButton != null) {
            viewJourneyButton.setOnClickListener(view -> openJourney());
        }

        if (cardAppointments != null) cardAppointments.setOnClickListener(view -> openJourney());
        if (cardCareTasks != null) cardCareTasks.setOnClickListener(view -> openJourney());
        if (cardAddActivity != null) cardAddActivity.setOnClickListener(view -> openJourney());
        if (cardCareProgress != null) cardCareProgress.setOnClickListener(view -> openJourney());

        if (profileIcon != null) {
            profileIcon.setOnClickListener(view -> {
                Intent intent = new Intent(this, activity_profile.class);
                startActivity(intent);
            });
        }
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
}
