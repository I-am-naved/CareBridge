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

import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

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

        // Auto login if session already exists
        if (userSession.isLoggedIn()) {
            openScreen(activity_home.class);
            finish();
            return;
        }

        setContentView(R.layout.activity_main);

        loginIdInput = findViewById(R.id.loginIdInput);
        passwordInput = findViewById(R.id.passwordInput);
        loginButton = findViewById(R.id.loginButton);
        loginProgressBar = findViewById(R.id.loginProgressBar);
        TextView createAccount = findViewById(R.id.createAccount);
        TextView forgotPassword = findViewById(R.id.forgotPassword);

        loginButton.setOnClickListener(view -> performLogin());

        createAccount.setOnClickListener(view ->
                openScreen(CreateAccountActivity.class)
        );

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
                        if (user != null && password.equals(user.getPassword())) {
                            onLoginSuccess(user);
                        } else {
                            setLoading(false);
                            Toast.makeText(
                                    MainActivity.this,
                                    getString(R.string.error_login_failed),
                                    Toast.LENGTH_SHORT
                            ).show();
                        }
                    } else {
                        // Fallback check by email
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
        usersRef.orderByChild("email").equalTo(email).addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                setLoading(false);
                boolean userFoundAndAuthenticated = false;

                if (snapshot.exists()) {
                    for (DataSnapshot userSnap : snapshot.getChildren()) {
                        User user = userSnap.getValue(User.class);
                        if (user != null && password.equals(user.getPassword())) {
                            onLoginSuccess(user);
                            userFoundAndAuthenticated = true;
                            break;
                        }
                    }
                }

                if (!userFoundAndAuthenticated) {
                    Toast.makeText(
                            MainActivity.this,
                            getString(R.string.error_login_failed),
                            Toast.LENGTH_SHORT
                    ).show();
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

    private void onLoginSuccess(User user) {
        setLoading(false);
        userSession.createLoginSession(
                user.getUserId() != null ? user.getUserId() : "",
                user.getFullName() != null ? user.getFullName() : "",
                user.getEmail() != null ? user.getEmail() : ""
        );

        String nameToShow = user.getFullName() != null && !user.getFullName().isEmpty()
                ? user.getFullName()
                : user.getUserId();

        Toast.makeText(
                MainActivity.this,
                "Welcome back, " + nameToShow + "!",
                Toast.LENGTH_SHORT
        ).show();

        openScreen(activity_home.class);
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
