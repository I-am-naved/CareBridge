package com.example.carebridge;

import android.app.Activity;
import android.os.Bundle;
import android.content.Intent;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

public class activity_profile extends Activity {

    private UserSession userSession;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_profile);

        userSession = new UserSession(this);

        TextView profileAvatar = findViewById(R.id.profileAvatar);
        TextView profileName = findViewById(R.id.profileName);
        TextView profileEmail = findViewById(R.id.profileEmail);
        Button editProfileButton = findViewById(R.id.editProfileButton);
        Button logoutButton = findViewById(R.id.logoutButton);
        Button backButton = findViewById(R.id.backButton);

        if (userSession.isLoggedIn()) {
            String fullName = userSession.getFullName();
            if (fullName.isEmpty()) {
                fullName = userSession.getUserId();
            }

            if (profileName != null) {
                profileName.setText(fullName);
            }

            if (profileAvatar != null && !fullName.isEmpty()) {
                profileAvatar.setText(getInitials(fullName));
            }

            if (profileEmail != null) {
                String emailAndId = userSession.getEmail() + " (@" + userSession.getUserId() + ")";
                profileEmail.setText(emailAndId);
            }
        }

        if (backButton != null) {
            backButton.setOnClickListener(view -> finish());
        }

        editProfileButton.setOnClickListener(view ->
                Toast.makeText(
                        this,
                        "Profile editing will be added later.",
                        Toast.LENGTH_SHORT
                ).show()
        );

        logoutButton.setOnClickListener(view -> {
            userSession.logoutUser();
            Intent intent = new Intent(this, MainActivity.class);
            intent.setFlags(
                    Intent.FLAG_ACTIVITY_CLEAR_TOP |
                            Intent.FLAG_ACTIVITY_NEW_TASK |
                            Intent.FLAG_ACTIVITY_CLEAR_TASK
            );
            startActivity(intent);
            finish();
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
}
