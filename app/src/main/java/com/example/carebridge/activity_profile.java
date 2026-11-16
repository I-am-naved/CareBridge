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

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.provider.MediaStore;
import android.util.Base64;
import android.widget.ImageView;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.ValueEventListener;
import androidx.annotation.NonNull;

public class activity_profile extends Activity {

    private UserSession userSession;
    private static final int PICK_IMAGE = 1;
    private ImageView profileImage;
    private TextView profileAvatar;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_profile);

        userSession = new UserSession(this);

        profileAvatar = findViewById(R.id.profileAvatar);
        profileImage = findViewById(R.id.profileImage);
        TextView profileName = findViewById(R.id.profileName);
        TextView profileEmail = findViewById(R.id.profileEmail);
        Button editProfileButton = findViewById(R.id.editProfileButton);
        android.view.View avatarContainer = findViewById(R.id.avatarContainer);
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
            
            loadProfileImage();
        }

        if (backButton != null) {
            backButton.setOnClickListener(view -> finish());
        }

        if (avatarContainer != null) {
            avatarContainer.setOnClickListener(v -> selectImage());
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
    
    private void selectImage() {
        Intent intent = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
        startActivityForResult(intent, PICK_IMAGE);
    }
    
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == PICK_IMAGE && resultCode == RESULT_OK && data != null && data.getData() != null) {
            Uri imageUri = data.getData();
            try {
                InputStream inputStream = getContentResolver().openInputStream(imageUri);
                Bitmap bitmap = BitmapFactory.decodeStream(inputStream);
                
                // Scale down to prevent Firebase max payload issues
                Bitmap scaled = Bitmap.createScaledBitmap(bitmap, 300, 300, true);
                
                profileImage.setImageBitmap(scaled);
                profileImage.setVisibility(android.view.View.VISIBLE);
                profileAvatar.setVisibility(android.view.View.GONE);
                
                ByteArrayOutputStream baos = new ByteArrayOutputStream();
                scaled.compress(Bitmap.CompressFormat.JPEG, 70, baos);
                byte[] bytes = baos.toByteArray();
                String base64 = Base64.encodeToString(bytes, Base64.DEFAULT);
                
                FirebaseHelper.getUserRef(userSession.getUserId()).child("profilePicture").setValue(base64)
                    .addOnFailureListener(e -> Toast.makeText(this, "Failed to save picture", Toast.LENGTH_SHORT).show());
                    
            } catch (Exception e) {
                Toast.makeText(this, "Failed to load image", Toast.LENGTH_SHORT).show();
            }
        }
    }
    
    private void loadProfileImage() {
        FirebaseHelper.getUserRef(userSession.getUserId()).child("profilePicture")
            .addListenerForSingleValueEvent(new ValueEventListener() {
                @Override
                public void onDataChange(@NonNull DataSnapshot snapshot) {
                    if (snapshot.exists()) {
                        String base64 = snapshot.getValue(String.class);
                        if (base64 != null) {
                            try {
                                byte[] decoded = Base64.decode(base64, Base64.DEFAULT);
                                Bitmap bitmap = BitmapFactory.decodeByteArray(decoded, 0, decoded.length);
                                profileImage.setImageBitmap(bitmap);
                                profileImage.setVisibility(android.view.View.VISIBLE);
                                profileAvatar.setVisibility(android.view.View.GONE);
                            } catch (Exception e) {
                                // Fallback to initials on error
                            }
                        }
                    }
                }
                @Override
                public void onCancelled(@NonNull DatabaseError error) {}
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
