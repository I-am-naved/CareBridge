package com.example.carebridge;

import android.app.Activity;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;

import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.ValueEventListener;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

public class activity_journey extends Activity {

    private UserSession userSession;
    private LinearLayout journeyTimelineContainer;
    private TextView journeyEmptyText;
    private Spinner sortOrderSpinner;
    private ValueEventListener journeyListener;
    private ValueEventListener tasksListener;
    private com.google.firebase.database.DatabaseReference journeyRef;
    private com.google.firebase.database.DatabaseReference tasksRef;
    private final List<JourneyItem> cachedJourney = new ArrayList<>();
    private final List<CareTask> cachedTasks = new ArrayList<>();
    private boolean isNewestFirst = true;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_journey);

        userSession = new UserSession(this);

        Button addActivityButton = findViewById(R.id.addActivityButton);
        View backButton = findViewById(R.id.backButton);
        journeyTimelineContainer = findViewById(R.id.journeyTimelineContainer);
        journeyEmptyText = findViewById(R.id.journeyEmptyText);
        sortOrderSpinner = findViewById(R.id.sortOrderSpinner);

        if (backButton != null) {
            backButton.setOnClickListener(view -> finish());
        }

        if (addActivityButton != null) {
            addActivityButton.setOnClickListener(view -> openAddActivityDialog());
        }

        setupSortSpinner();

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

    private void setupSortSpinner() {
        if (sortOrderSpinner == null) return;
        String[] options = new String[]{"Newest First", "Oldest First"};
        ArrayAdapter<String> adapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, options);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        sortOrderSpinner.setAdapter(adapter);
        sortOrderSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                isNewestFirst = (position == 0);
                combineAndRender();
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {}
        });
    }

    private void loadTasksFromFirebase(String userId) {
        tasksRef = FirebaseHelper.getTasksRef(userId);
        tasksListener = new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (isFinishing()) return;
                cachedTasks.clear();
                for (DataSnapshot child : snapshot.getChildren()) {
                    CareTask task = child.getValue(CareTask.class);
                    if (task != null) {
                        if (task.getId() == null) task.setId(child.getKey());
                        cachedTasks.add(task);
                    }
                }
                combineAndRender();
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Toast.makeText(activity_journey.this, "Failed to load tasks: " + error.getMessage(), Toast.LENGTH_SHORT).show();
            }
        };
        tasksRef.addValueEventListener(tasksListener);
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
                    if (item != null) {
                        if (item.getId() == null) item.setId(child.getKey());
                        cachedJourney.add(item);
                    }
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
        List<JourneyItem> combined = new ArrayList<>();
        Set<String> seenIds = new HashSet<>();
        Set<String> seenKeys = new HashSet<>();

        // Add journey items first
        for (JourneyItem ji : cachedJourney) {
            if (ji.getId() != null && !ji.getId().trim().isEmpty()) {
                seenIds.add(ji.getId());
            }
            String key = makeKey(ji.getTitle(), ji.getTimestamp(), ji.getDate());
            seenKeys.add(key);
            combined.add(ji);
        }

        // Add care tasks without duplicating existing journey entries
        for (CareTask t : cachedTasks) {
            if (t.getId() != null && seenIds.contains(t.getId())) {
                // If already present as a journey item, enrich it with task details
                for (JourneyItem existing : combined) {
                    if (t.getId().equals(existing.getId())) {
                        if (existing.getAssignedBy() == null || existing.getAssignedBy().isEmpty()) {
                            existing.setAssignedBy(t.getAssignedBy());
                        }
                        if (existing.getReason() == null || existing.getReason().isEmpty()) {
                            existing.setReason(t.getReason());
                        }
                        if (existing.getStatus() == null || existing.getStatus().isEmpty()) {
                            existing.setStatus(t.getStatus());
                        }
                    }
                }
                continue;
            }

            String key = makeKey(t.getTitle(), t.getTimestamp(), t.getDueDate());
            if (seenKeys.contains(key)) {
                continue;
            }
            seenKeys.add(key);
            if (t.getId() != null) seenIds.add(t.getId());

            String status = t.getStatus() != null ? t.getStatus() : "PENDING";
            String title = (t.getTitle() != null && !t.getTitle().startsWith("Task:"))
                    ? "Task: " + t.getTitle()
                    : (t.getTitle() != null ? t.getTitle() : "Care Task");

            String desc = t.getDescription() != null ? t.getDescription() : "";
            String assignedBy = t.getAssignedBy() != null ? t.getAssignedBy() : "Care Team";

            JourneyItem ji = new JourneyItem(
                    t.getId(), t.getUserId(), "", "Care Task", title, desc,
                    t.getDueDate(), t.getTimestamp(), assignedBy, t.getReason(), status
            );
            combined.add(ji);
        }

        // Sort events by timestamp based on user preference
        Collections.sort(combined, (a, b) -> {
            long tsA = a.getTimestamp();
            long tsB = b.getTimestamp();
            return isNewestFirst ? Long.compare(tsB, tsA) : Long.compare(tsA, tsB);
        });

        renderJourneyTimeline(combined);
    }

    private String makeKey(String title, long timestamp, String dateStr) {
        String safeTitle = title == null ? "" : title.trim().toLowerCase();
        return safeTitle + "_" + timestamp + "_" + (dateStr == null ? "" : dateStr.trim());
    }

    @Override
    protected void onDestroy() {
        if (journeyRef != null && journeyListener != null) {
            journeyRef.removeEventListener(journeyListener);
        }
        if (tasksRef != null && tasksListener != null) {
            tasksRef.removeEventListener(tasksListener);
        }
        super.onDestroy();
    }

    private String formatDateTime(String dateStr, long timestamp) {
        if (timestamp > 0) {
            SimpleDateFormat sdf = new SimpleDateFormat("MMM dd, yyyy • hh:mm a", Locale.US);
            return "📅 " + sdf.format(new Date(timestamp));
        }
        if (dateStr != null && !dateStr.trim().isEmpty()) {
            return "📅 " + dateStr;
        }
        return "";
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
            TextView statusBadge = cardView.findViewById(R.id.itemStatusBadge);
            TextView titleText = cardView.findViewById(R.id.itemTitle);
            TextView assignedByText = cardView.findViewById(R.id.itemAssignedBy);
            TextView reasonText = cardView.findViewById(R.id.itemReason);
            TextView descText = cardView.findViewById(R.id.itemDescription);
            TextView dateText = cardView.findViewById(R.id.itemDate);

            String stepStr = String.format(Locale.US, "%02d", step++);
            String categoryStr = (item.getCategory() != null) ? item.getCategory() : "Care Event";

            if (categoryText != null) {
                categoryText.setText(stepStr + "  •  " + categoryStr);
            }

            // Distinguish patient-reported completion from provider-verified completion
            if (statusBadge != null) {
                String status = item.getStatus();
                if ("COMPLETED".equalsIgnoreCase(status)) {
                    statusBadge.setText("✓ Provider-Verified");
                    statusBadge.setTextColor(Color.parseColor("#065F46"));
                    statusBadge.setBackgroundTintList(ColorStateList.valueOf(Color.parseColor("#D1FAE5")));
                    statusBadge.setVisibility(View.VISIBLE);
                } else if ("AWAITING_REVIEW".equalsIgnoreCase(status)) {
                    statusBadge.setText("⏳ Patient-Reported");
                    statusBadge.setTextColor(Color.parseColor("#1E40AF"));
                    statusBadge.setBackgroundTintList(ColorStateList.valueOf(Color.parseColor("#DBEAFE")));
                    statusBadge.setVisibility(View.VISIBLE);
                } else if ("PENDING".equalsIgnoreCase(status)) {
                    statusBadge.setText("Pending Task");
                    statusBadge.setTextColor(Color.parseColor("#9A3412"));
                    statusBadge.setBackgroundTintList(ColorStateList.valueOf(Color.parseColor("#FFEDD5")));
                    statusBadge.setVisibility(View.VISIBLE);
                } else {
                    statusBadge.setVisibility(View.GONE);
                }
            }

            if (titleText != null) {
                titleText.setText(item.getTitle() != null ? item.getTitle() : "Activity");
            }

            // Display who created or assigned each task
            if (assignedByText != null) {
                String assignedBy = item.getAssignedBy();
                if (assignedBy != null && !assignedBy.trim().isEmpty()) {
                    assignedByText.setText("Assigned/Created by: " + assignedBy);
                    assignedByText.setVisibility(View.VISIBLE);
                } else {
                    assignedByText.setVisibility(View.GONE);
                }
            }

            // Display the reason for the task when available
            if (reasonText != null) {
                String reason = item.getReason();
                if (reason != null && !reason.trim().isEmpty()) {
                    reasonText.setText("Reason: " + reason);
                    reasonText.setVisibility(View.VISIBLE);
                } else {
                    reasonText.setVisibility(View.GONE);
                }
            }

            if (descText != null) {
                String desc = item.getDescription() != null ? item.getDescription() : "";
                if (!desc.isEmpty()) {
                    descText.setText(desc);
                    descText.setVisibility(View.VISIBLE);
                } else {
                    descText.setVisibility(View.GONE);
                }
            }

            // Display activity date and time consistently
            if (dateText != null) {
                String formattedDate = formatDateTime(item.getDate(), item.getTimestamp());
                if (!formattedDate.isEmpty()) {
                    dateText.setText(formattedDate);
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
