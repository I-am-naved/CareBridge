package com.example.carebridge;

import android.app.Activity;
import android.app.AlertDialog;
import android.text.InputType;
import android.widget.EditText;
import android.os.Bundle;
import android.content.Intent;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import com.google.firebase.auth.FirebaseAuth;

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
        android.view.View logoutButton = findViewById(R.id.logoutButton);
        android.view.View backButton = findViewById(R.id.backButton);

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

        if (editProfileButton != null) {
            editProfileButton.setOnClickListener(view -> showEditNameDialog(profileName, profileAvatar));
        }

        logoutButton.setOnClickListener(view -> {
            FirebaseAuth.getInstance().signOut();
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

    private void showEditNameDialog(TextView profileName, TextView profileAvatar) {
        EditText input = new EditText(this);
        input.setSingleLine(true);
        input.setHint("Full name");
        input.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_FLAG_CAP_WORDS);
        input.setText(userSession.getFullName());

        new AlertDialog.Builder(this)
                .setTitle("Update profile")
                .setMessage("Change the name displayed in CareBridge.")
                .setView(input)
                .setNegativeButton("Cancel", null)
                .setPositiveButton("Save", (dialog, which) -> {
                    String newName = input.getText().toString().trim();
                    if (newName.isEmpty()) {
                        Toast.makeText(this, "Name cannot be empty.", Toast.LENGTH_SHORT).show();
                        return;
                    }
                    FirebaseHelper.getUserRef(userSession.getUserId()).child("fullName").setValue(newName)
                            .addOnCompleteListener(task -> {
                                if (task.isSuccessful()) {
                                    userSession.createLoginSession(userSession.getUserId(), newName,
                                            userSession.getEmail(), userSession.getRole());
                                    if (profileName != null) profileName.setText(newName);
                                    if (profileAvatar != null) profileAvatar.setText(getInitials(newName));
                                    Toast.makeText(this, "Profile updated.", Toast.LENGTH_SHORT).show();
                                } else {
                                    Toast.makeText(this, "Could not update profile.", Toast.LENGTH_LONG).show();
                                }
                            });
                })
                .show();
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
