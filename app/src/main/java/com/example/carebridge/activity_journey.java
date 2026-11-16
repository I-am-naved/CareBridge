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
    private ValueEventListener journeyListener;
    private com.google.firebase.database.DatabaseReference journeyRef;
    private com.google.firebase.database.DatabaseReference tasksRef;
    private List<JourneyItem> cachedJourney = new ArrayList<>();
    private List<CareTask> cachedTasks = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_journey);

        userSession = new UserSession(this);

        Button addActivityButton = findViewById(R.id.addActivityButton);
        android.view.View backButton = findViewById(R.id.backButton);
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
            loadTasksFromFirebase(userId);
        } else {
            if (journeyEmptyText != null) {
                journeyEmptyText.setText("Please log in to view your journey.");
            }
        }
    }
    
    private void loadTasksFromFirebase(String userId) {
        tasksRef = FirebaseHelper.getTasksRef(userId);
        tasksRef.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (isFinishing()) return;
                cachedTasks.clear();
                for (DataSnapshot child : snapshot.getChildren()) {
                    CareTask task = child.getValue(CareTask.class);
                    if (task != null) cachedTasks.add(task);
                }
                combineAndRender();
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Toast.makeText(activity_journey.this, "Failed to load tasks: " + error.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void loadJourneyFromFirebase(String userId) {
        journeyRef = FirebaseHelper.getJourneyRef(userId);
        journeyListener = new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (isFinishing()) return;
                cachedJourney.clear();
                for (DataSnapshot child : snapshot.getChildren()) {
                    JourneyItem item = child.getValue(JourneyItem.class);
                    if (item != null) cachedJourney.add(item);
                }
                combineAndRender();
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Toast.makeText(activity_journey.this, "Failed to load journey: " + error.getMessage(), Toast.LENGTH_SHORT).show();
            }
        };
        journeyRef.addValueEventListener(journeyListener);
    }
    
    private void combineAndRender() {
        List<JourneyItem> combined = new ArrayList<>(cachedJourney);
        for (CareTask t : cachedTasks) {
            String status = t.getStatus() != null ? t.getStatus() : "PENDING";
            String title = "Task: " + (t.getTitle() != null ? t.getTitle() : "Care Task");
            String desc = "Status: " + status;
            if (t.getDescription() != null && !t.getDescription().trim().isEmpty()) {
                desc += "\n" + t.getDescription();
            }
            JourneyItem ji = new JourneyItem(
                t.getId(), t.getUserId(), "", "Care Task", title, desc, t.getDueDate(), t.getTimestamp()
            );
            combined.add(ji);
        }
        
        Collections.sort(combined, (a, b) -> Long.compare(b.getTimestamp(), a.getTimestamp()));
        renderJourneyTimeline(combined);
    }

    @Override
    protected void onDestroy() {
        if (journeyRef != null && journeyListener != null) {
            journeyRef.removeEventListener(journeyListener);
        }
        super.onDestroy();
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
