package com.example.carebridge;

import android.app.Activity;
import android.os.Bundle;
import android.content.Intent;
import android.util.Log;
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
import com.google.firebase.messaging.FirebaseMessaging;

public class MainActivity extends Activity {

    private static final String TAG = "MainActivity";
    private static final String DATABASE_URL = "https://carebridge-4607d-default-rtdb.firebaseio.com";

    private EditText loginIdInput;
    private EditText passwordInput;
    private Button loginButton;
    private ProgressBar loginProgressBar;
    private UserSession userSession;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        userSession = new UserSession(this);

        // Auto login routing based on role
        if (userSession.isLoggedIn()) {
            if (userSession.isAdmin()) {
                openScreen(AdminActivity.class);
            } else {
                openScreen(activity_home.class);
            }
            finish();
            return;
        }

        setContentView(R.layout.activity_main);

        loginIdInput = findViewById(R.id.loginIdInput);
        passwordInput = findViewById(R.id.passwordInput);
        loginButton = findViewById(R.id.loginButton);
        Button submitLoginButton = findViewById(R.id.submitLoginButton);
        Button createAccountButtonMain = findViewById(R.id.createAccountButtonMain);
        loginProgressBar = findViewById(R.id.loginProgressBar);
        View loginFormContainer = findViewById(R.id.loginFormContainer);
        View welcomeButtonsContainer = findViewById(R.id.welcomeButtonsContainer);
        TextView createAccount = findViewById(R.id.createAccount);
        TextView forgotPassword = findViewById(R.id.forgotPassword);

        loginButton.setOnClickListener(view -> {
            welcomeButtonsContainer.setVisibility(View.GONE);
            loginFormContainer.setVisibility(View.VISIBLE);
        });

        submitLoginButton.setOnClickListener(view -> performLogin());

        View.OnClickListener openCreateAccount = view -> openScreen(CreateAccountActivity.class);
        createAccount.setOnClickListener(openCreateAccount);
        createAccountButtonMain.setOnClickListener(openCreateAccount);

        forgotPassword.setOnClickListener(view ->
                Toast.makeText(
                        this,
                        getString(R.string.password_reset_not_available),
                        Toast.LENGTH_SHORT
                ).show()
        );
    }

    private DatabaseReference getUsersRef() {
        return FirebaseDatabase.getInstance(DATABASE_URL).getReference("users");
    }

    private void performLogin() {
        String loginId = loginIdInput.getText().toString().trim().toLowerCase();
        String password = passwordInput.getText().toString();

        if (loginId.isEmpty() || password.isEmpty()) {
            Toast.makeText(
                    this,
                    getString(R.string.error_required_credentials),
                    Toast.LENGTH_SHORT
            ).show();
            return;
        }

        setLoading(true);

        // Special check for default Admin account (admin / admin123)
        if ("admin".equals(loginId)) {
            FirebaseAuth.getInstance().signInWithEmailAndPassword("admin@carebridge.com", password)
                    .addOnCompleteListener(this, task -> {
                        if (task.isSuccessful()) {
                            User adminUser = new User("Healthcare Admin", "admin", "admin@carebridge.com", "admin", System.currentTimeMillis());
                            onLoginSuccess(adminUser);
                        } else {
                            setLoading(false);
                            Toast.makeText(
                                    MainActivity.this,
                                    getString(R.string.error_login_failed),
                                    Toast.LENGTH_SHORT
                            ).show();
                        }
                    });
            return;
        }

        DatabaseReference usersRef = getUsersRef();

        // If loginId contains @ or . or invalid key chars, it's an email address
        if (loginId.contains("@") || loginId.contains(".")) {
            loginByEmail(usersRef, loginId, password);
        } else {
            // It's a User ID key
            usersRef.child(loginId).addListenerForSingleValueEvent(new ValueEventListener() {
                @Override
                public void onDataChange(@NonNull DataSnapshot snapshot) {
                    if (snapshot.exists()) {
                        User user = snapshot.getValue(User.class);
                        if (user != null && user.getEmail() != null) {
                            loginByEmail(usersRef, user.getEmail(), password);
                        } else {
                            setLoading(false);
                            Toast.makeText(
                                    MainActivity.this,
                                    getString(R.string.error_login_failed),
                                    Toast.LENGTH_SHORT
                            ).show();
                        }
                    } else {
                        // Fallback check by email just in case
                        loginByEmail(usersRef, loginId, password);
                    }
                }

                @Override
                public void onCancelled(@NonNull DatabaseError error) {
                    setLoading(false);
                    Log.e(TAG, "Database error: " + error.getMessage(), error.toException());
                    Toast.makeText(
                            MainActivity.this,
                            "Database error: " + error.getMessage(),
                            Toast.LENGTH_LONG
                    ).show();
                }
            });
        }
    }

    private void loginByEmail(DatabaseReference usersRef, String email, String password) {
        FirebaseAuth.getInstance().signInWithEmailAndPassword(email, password)
                .addOnCompleteListener(this, task -> {
                    if (task.isSuccessful()) {
                        // Successfully logged in via Auth. Now fetch user data from DB based on email.
                        usersRef.orderByChild("email").equalTo(email).addListenerForSingleValueEvent(new ValueEventListener() {
                            @Override
                            public void onDataChange(@NonNull DataSnapshot snapshot) {
                                if (snapshot.exists()) {
                                    for (DataSnapshot userSnap : snapshot.getChildren()) {
                                        User user = userSnap.getValue(User.class);
                                        if (user != null) {
                                            onLoginSuccess(user);
                                            return;
                                        }
                                    }
                                }
                                setLoading(false);
                                Toast.makeText(
                                        MainActivity.this,
                                        "User data not found.",
                                        Toast.LENGTH_SHORT
                                ).show();
                            }

                            @Override
                            public void onCancelled(@NonNull DatabaseError error) {
                                setLoading(false);
                                Log.e(TAG, "Database error: " + error.getMessage(), error.toException());
                                Toast.makeText(
                                        MainActivity.this,
                                        "Database error: " + error.getMessage(),
                                        Toast.LENGTH_LONG
                                ).show();
                            }
                        });
                    } else {
                        setLoading(false);
                        Toast.makeText(
                                MainActivity.this,
                                getString(R.string.error_login_failed),
                                Toast.LENGTH_SHORT
                        ).show();
                    }
                });
    }

    private void onLoginSuccess(User user) {
        setLoading(false);

        String role = user.getRole() != null ? user.getRole() : ("admin".equalsIgnoreCase(user.getUserId()) ? "admin" : "patient");

        userSession.createLoginSession(
                user.getUserId() != null ? user.getUserId() : "",
                user.getFullName() != null ? user.getFullName() : "",
                user.getEmail() != null ? user.getEmail() : "",
                role
        );

        String nameToShow = user.getFullName() != null && !user.getFullName().isEmpty()
                ? user.getFullName()
                : user.getUserId();

        if ("admin".equalsIgnoreCase(role)) {
            Toast.makeText(
                    MainActivity.this,
                    "Logged in as Admin (" + nameToShow + ")",
                    Toast.LENGTH_SHORT
            ).show();
            openScreen(AdminActivity.class);
        } else {
            // Start the background notification listener
            Intent serviceIntent = new Intent(this, NotificationService.class);
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
                startForegroundService(serviceIntent);
            } else {
                startService(serviceIntent);
            }

            Toast.makeText(
                    MainActivity.this,
                    "Welcome back, " + nameToShow + "!",
                    Toast.LENGTH_SHORT
            ).show();
            openScreen(activity_home.class);
        }

        finish();
    }

    private void setLoading(boolean isLoading) {
        if (loginProgressBar != null) {
            loginProgressBar.setVisibility(isLoading ? View.VISIBLE : View.GONE);
        }
        loginButton.setEnabled(!isLoading);
        loginIdInput.setEnabled(!isLoading);
        passwordInput.setEnabled(!isLoading);
    }

    private void openScreen(Class<?> screen) {
        Intent intent = new Intent(this, screen);
        startActivity(intent);
    }
}
