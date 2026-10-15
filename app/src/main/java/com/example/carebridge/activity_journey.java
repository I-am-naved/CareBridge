package com.example.carebridge;

import android.app.Activity;
import android.os.Bundle;
import android.widget.Button;
import android.widget.Toast;

public class activity_journey extends Activity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_journey);

        Button addActivityButton = findViewById(R.id.addActivityButton);
        Button backButton = findViewById(R.id.backButton);

        if (backButton != null) {
            backButton.setOnClickListener(view -> finish());
        }

        addActivityButton.setOnClickListener(view ->
                Toast.makeText(
                        this,
                        "Adding journey activities will be implemented later.",
                        Toast.LENGTH_SHORT
                ).show()
        );
    }
}
