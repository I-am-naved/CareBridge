package com.example.carebridge;

import android.app.Activity;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;

import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class activity_journey extends Activity {

    private UserSession userSession;
    private LinearLayout journeyTimelineContainer;
    private TextView journeyEmptyText;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_journey);

        userSession = new UserSession(this);

        Button addActivityButton = findViewById(R.id.addActivityButton);
        Button backButton = findViewById(R.id.backButton);
        journeyTimelineContainer = findViewById(R.id.journeyTimelineContainer);
        journeyEmptyText = findViewById(R.id.journeyEmptyText);

        if (backButton != null) {
            backButton.setOnClickListener(view -> finish());
        }

        if (addActivityButton != null) {
            addActivityButton.setOnClickListener(view -> openAddActivityDialog());
        }

        if (userSession.isLoggedIn()) {
            String userId = userSession.getUserId();
            FirebaseHelper.seedDefaultDataIfEmpty(userId);
            loadJourneyFromFirebase(userId);
        } else {
            if (journeyEmptyText != null) {
                journeyEmptyText.setText("Please log in to view your journey.");
            }
        }
    }

    private void loadJourneyFromFirebase(String userId) {
        FirebaseHelper.getJourneyRef(userId).addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                List<JourneyItem> journeyList = new ArrayList<>();
                for (DataSnapshot child : snapshot.getChildren()) {
                    JourneyItem item = child.getValue(JourneyItem.class);
                    if (item != null) {
                        journeyList.add(item);
                    }
                }

                // Sort by timestamp
                Collections.sort(journeyList, (a, b) -> Long.compare(a.getTimestamp(), b.getTimestamp()));

                renderJourneyTimeline(journeyList);
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Toast.makeText(activity_journey.this, "Failed to load journey: " + error.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void renderJourneyTimeline(List<JourneyItem> journeyList) {
        journeyTimelineContainer.removeAllViews();

        if (journeyList.isEmpty()) {
            if (journeyEmptyText != null) {
                journeyEmptyText.setText("No journey activities yet. Click '+ Add Activity' below to add one.");
                journeyTimelineContainer.addView(journeyEmptyText);
            }
            return;
        }

        int step = 1;
        for (JourneyItem item : journeyList) {
            View cardView = LayoutInflater.from(this).inflate(R.layout.item_journey_card, journeyTimelineContainer, false);

            TextView categoryText = cardView.findViewById(R.id.itemCategory);
            TextView titleText = cardView.findViewById(R.id.itemTitle);
            TextView descText = cardView.findViewById(R.id.itemDescription);
            TextView dateText = cardView.findViewById(R.id.itemDate);

            String stepStr = String.format("%02d", step++);
            String categoryStr = (item.getCategory() != null) ? item.getCategory() : "Care Event";

            if (categoryText != null) {
                categoryText.setText(stepStr + "  •  " + categoryStr);
            }

            if (titleText != null) {
                titleText.setText(item.getTitle() != null ? item.getTitle() : "Activity");
            }

            if (descText != null) {
                descText.setText(item.getDescription() != null ? item.getDescription() : "");
            }

            if (dateText != null) {
                if (item.getDate() != null && !item.getDate().isEmpty()) {
                    dateText.setText("Date: " + item.getDate());
                    dateText.setVisibility(View.VISIBLE);
                } else {
                    dateText.setVisibility(View.GONE);
                }
            }

            journeyTimelineContainer.addView(cardView);
        }
    }

    private void openAddActivityDialog() {
        AddActivityDialog dialog = new AddActivityDialog(this, userSession.getUserId());
        dialog.show();
    }
}
