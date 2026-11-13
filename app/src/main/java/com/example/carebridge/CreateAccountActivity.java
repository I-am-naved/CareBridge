package com.example.carebridge;

import android.app.Activity;
import android.os.Bundle;
import android.util.Log;
import android.util.Patterns;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

public class CreateAccountActivity extends Activity {

    private static final String TAG = "CreateAccountActivity";
    private static final String DATABASE_URL = "https://carebridge-4607d-default-rtdb.firebaseio.com";

    private EditText fullNameInput;
    private EditText userIdInput;
    private EditText emailInput;
    private EditText passwordInput;
    private EditText confirmPasswordInput;
    private Button createAccountButton;
    private ProgressBar progressBar;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_create_account);

        fullNameInput = findViewById(R.id.fullNameInput);
        userIdInput = findViewById(R.id.userIdInput);
        emailInput = findViewById(R.id.emailInput);
        passwordInput = findViewById(R.id.passwordInput);
        confirmPasswordInput = findViewById(R.id.confirmPasswordInput);
        createAccountButton = findViewById(R.id.createAccountButton);
        progressBar = findViewById(R.id.progressBar);
        TextView backToLogin = findViewById(R.id.backToLogin);

        createAccountButton.setOnClickListener(v -> validateAndRegisterAccount());

        if (backToLogin != null) {
            backToLogin.setOnClickListener(v -> finish());
        }
    }

    private DatabaseReference getUsersRef() {
        return FirebaseDatabase.getInstance(DATABASE_URL).getReference("users");
    }

    private void validateAndRegisterAccount() {
        String fullName = fullNameInput.getText().toString().trim();
        String userId = userIdInput.getText().toString().trim().toLowerCase();
        String email = emailInput.getText().toString().trim().toLowerCase();
        String password = passwordInput.getText().toString();
        String confirmPassword = confirmPasswordInput.getText().toString();

        // Rule 1: All fields required
        if (fullName.isEmpty() || userId.isEmpty() || email.isEmpty() || password.isEmpty() || confirmPassword.isEmpty()) {
            Toast.makeText(this, getString(R.string.error_all_fields_required), Toast.LENGTH_SHORT).show();
            return;
        }

        // Rule 2: User ID must be at least 3 characters
        if (userId.length() < 3) {
            Toast.makeText(this, getString(R.string.error_user_id_short), Toast.LENGTH_SHORT).show();
            return;
        }

        // Rule 2b: User ID cannot contain special path characters (. # $ [ ] @)
        if (userId.contains(".") || userId.contains("#") || userId.contains("$") ||
                userId.contains("[") || userId.contains("]") || userId.contains("@")) {
            Toast.makeText(this, "User ID cannot contain special characters (. # $ [ ] @)", Toast.LENGTH_SHORT).show();
            return;
        }

        // Rule 3: Valid email address format
        if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            Toast.makeText(this, getString(R.string.error_invalid_email), Toast.LENGTH_SHORT).show();
            return;
        }

        // Rule 4: Password must be at least 8 characters
        if (password.length() < 8) {
            Toast.makeText(this, getString(R.string.error_password_length), Toast.LENGTH_SHORT).show();
            return;
        }

        // Rule 5: Passwords must match
        if (!password.equals(confirmPassword)) {
            Toast.makeText(this, getString(R.string.error_password_mismatch), Toast.LENGTH_SHORT).show();
            return;
        }

        setLoading(true);

        DatabaseReference usersRef = getUsersRef();

        // Rule 6: Check uniqueness of User ID in Firebase
        usersRef.child(userId).addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (snapshot.exists()) {
                    setLoading(false);
                    Toast.makeText(CreateAccountActivity.this, getString(R.string.error_user_id_exists), Toast.LENGTH_SHORT).show();
                } else {
                    saveUserToFirebase(usersRef, fullName, userId, email, password);
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                setLoading(false);
                Log.e(TAG, "Database error: " + error.getMessage(), error.toException());
                Toast.makeText(CreateAccountActivity.this, "Database error: " + error.getMessage(), Toast.LENGTH_LONG).show();
            }
        });
    }

    private void saveUserToFirebase(DatabaseReference usersRef, String fullName, String userId, String email, String password) {
        FirebaseAuth.getInstance().createUserWithEmailAndPassword(email, password)
                .addOnCompleteListener(this, authTask -> {
                    if (authTask.isSuccessful()) {
                        User user = new User(fullName, userId, email, "patient", System.currentTimeMillis());

                        usersRef.child(userId).setValue(user).addOnCompleteListener(dbTask -> {
                            setLoading(false);
                            if (dbTask.isSuccessful()) {
                                Toast.makeText(
                                        CreateAccountActivity.this,
                                        "Account created successfully! Please log in.",
                                        Toast.LENGTH_LONG
                                ).show();
                                finish();
                            } else {
                                String errorMsg = dbTask.getException() != null ? dbTask.getException().getMessage() : "Unknown error";
                                Log.e(TAG, "Failed to save user details: " + errorMsg, dbTask.getException());
                                Toast.makeText(
                                        CreateAccountActivity.this,
                                        "Failed to save account details: " + errorMsg,
                                        Toast.LENGTH_LONG
                                ).show();
                            }
                        });
                    } else {
                        setLoading(false);
                        String errorMsg = authTask.getException() != null ? authTask.getException().getMessage() : "Unknown authentication error";
                        Log.e(TAG, "Failed to create Auth account: " + errorMsg, authTask.getException());
                        Toast.makeText(
                                CreateAccountActivity.this,
                                "Failed to create account: " + errorMsg,
                                Toast.LENGTH_LONG
                        ).show();
                    }
                });
    }

    private void setLoading(boolean isLoading) {
        if (progressBar != null) {
            progressBar.setVisibility(isLoading ? View.VISIBLE : View.GONE);
        }
        createAccountButton.setEnabled(!isLoading);
        fullNameInput.setEnabled(!isLoading);
        userIdInput.setEnabled(!isLoading);
        emailInput.setEnabled(!isLoading);
        passwordInput.setEnabled(!isLoading);
        confirmPasswordInput.setEnabled(!isLoading);
    }
}
