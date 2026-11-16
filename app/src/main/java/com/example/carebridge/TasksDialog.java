package com.example.carebridge;

import android.app.AlertDialog;
import android.app.Dialog;
import android.content.Context;
import android.graphics.Paint;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;

import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.ValueEventListener;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class TasksDialog extends Dialog {
    private final String userId;
    private LinearLayout tasksContainer;
    private TextView tasksEmptyText;
    private DatabaseReference tasksRef;
    private ValueEventListener tasksListener;

    public TasksDialog(@NonNull Context context, String userId) {
        super(context);
        this.userId = FirebaseHelper.sanitizeFirebaseKey(userId);
    }

    private boolean showCompleted = false;
    private TextView tabToDo, tabCompleted;
    private List<CareTask> allTasks = new ArrayList<>();

    @Override protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.dialog_tasks);
        tasksContainer = findViewById(R.id.tasksContainer);
        tasksEmptyText = findViewById(R.id.tasksEmptyText);
        tabToDo = findViewById(R.id.tabToDo);
        tabCompleted = findViewById(R.id.tabCompleted);
        
        View close = findViewById(R.id.closeTasksButton);
        Button add = findViewById(R.id.addTaskButton);
        if (close != null) close.setOnClickListener(v -> dismiss());
        if (add != null) add.setOnClickListener(v -> showAddTaskDialog());
        
        if (tabToDo != null && tabCompleted != null) {
            tabToDo.setOnClickListener(v -> { showCompleted = false; updateTabs(); renderTasks(allTasks); });
            tabCompleted.setOnClickListener(v -> { showCompleted = true; updateTabs(); renderTasks(allTasks); });
        }
        
        tasksRef = FirebaseHelper.getTasksRef(userId);
        loadTasks();
    }
    
    private void updateTabs() {
        if (tabToDo == null || tabCompleted == null) return;
        if (showCompleted) {
            tabCompleted.setBackgroundResource(R.drawable.bg_card);
            tabCompleted.setBackgroundTintList(android.content.res.ColorStateList.valueOf(android.graphics.Color.WHITE));
            tabCompleted.setTextColor(android.graphics.Color.parseColor("#173B57"));
            tabCompleted.setTypeface(null, android.graphics.Typeface.BOLD);
            
            tabToDo.setBackground(null);
            tabToDo.setTextColor(android.graphics.Color.parseColor("#667085"));
            tabToDo.setTypeface(null, android.graphics.Typeface.NORMAL);
        } else {
            tabToDo.setBackgroundResource(R.drawable.bg_card);
            tabToDo.setBackgroundTintList(android.content.res.ColorStateList.valueOf(android.graphics.Color.WHITE));
            tabToDo.setTextColor(android.graphics.Color.parseColor("#173B57"));
            tabToDo.setTypeface(null, android.graphics.Typeface.BOLD);
            
            tabCompleted.setBackground(null);
            tabCompleted.setTextColor(android.graphics.Color.parseColor("#667085"));
            tabCompleted.setTypeface(null, android.graphics.Typeface.NORMAL);
        }
    }

    private void loadTasks() {
        tasksListener = new ValueEventListener() {
            @Override public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (!isShowing()) return;
                allTasks.clear();
                int todoCount = 0;
                int doneCount = 0;
                for (DataSnapshot child : snapshot.getChildren()) {
                    CareTask task = child.getValue(CareTask.class);
                    if (task != null) {
                        if (task.getId() == null) task.setId(child.getKey());
                        allTasks.add(task);
                        if ("COMPLETED".equalsIgnoreCase(task.getStatus()) || "AWAITING_REVIEW".equalsIgnoreCase(task.getStatus())) doneCount++;
                        else todoCount++;
                    }
                }
                if (tabToDo != null) tabToDo.setText("To Do (" + todoCount + ")");
                if (tabCompleted != null) tabCompleted.setText("Completed (" + doneCount + ")");
                renderTasks(allTasks);
            }
            @Override public void onCancelled(@NonNull DatabaseError error) {
                Toast.makeText(getContext(), "Unable to load care tasks: " + error.getMessage(), Toast.LENGTH_SHORT).show();
            }
        };
        tasksRef.addValueEventListener(tasksListener);
    }

    private void renderTasks(List<CareTask> taskList) {
        tasksContainer.removeAllViews();
        
        List<CareTask> filtered = new ArrayList<>();
        for (CareTask t : taskList) {
            boolean isCompleted = "COMPLETED".equalsIgnoreCase(t.getStatus()) || "AWAITING_REVIEW".equalsIgnoreCase(t.getStatus());
            if (isCompleted == showCompleted) filtered.add(t);
        }

        if (filtered.isEmpty()) {
            if (tasksEmptyText != null) {
                detachEmptyView();
                tasksEmptyText.setText(showCompleted ? "No completed tasks yet." : "No pending care tasks. Add a task to keep track of your next steps.");
                tasksEmptyText.setVisibility(View.VISIBLE);
                tasksContainer.addView(tasksEmptyText);
            }
            return;
        }
        detachEmptyView();
        
        // Group by appointmentId
        java.util.Map<String, List<CareTask>> groups = new java.util.HashMap<>();
        for (CareTask task : filtered) {
            String apptId = task.getAppointmentId();
            if (apptId == null || apptId.trim().isEmpty()) apptId = "general";
            if (!groups.containsKey(apptId)) groups.put(apptId, new ArrayList<>());
            groups.get(apptId).add(task);
        }
        
        // Render groups
        for (java.util.Map.Entry<String, List<CareTask>> entry : groups.entrySet()) {
            String apptId = entry.getKey();
            List<CareTask> tasksInGroup = entry.getValue();
            
            // Draw group header
            View header = LayoutInflater.from(getContext()).inflate(R.layout.item_task_header, tasksContainer, false);
            TextView details = header.findViewById(R.id.taskHeaderDetails);
            TextView countBadge = header.findViewById(R.id.taskCountBadge);
            
            if (countBadge != null) countBadge.setText(tasksInGroup.size() + " tasks assigned");
            
            if ("general".equals(apptId)) {
                if (details != null) details.setText("General care tasks");
            } else {
                CareTask sample = tasksInGroup.get(0);
                if (details != null) {
                    details.setText("Assigned by " + (sample.getAssignedBy() != null ? sample.getAssignedBy() : "Care team") 
                                  + " • Due " + (sample.getDueDate() != null ? sample.getDueDate() : ""));
                }
            }
            tasksContainer.addView(header);
            
            // Draw items
            for (CareTask task : tasksInGroup) {
                View row = LayoutInflater.from(getContext()).inflate(R.layout.item_task_row, tasksContainer, false);
                TextView title = row.findViewById(R.id.taskTitle);
                TextView reason = row.findViewById(R.id.taskReason);
                TextView description = row.findViewById(R.id.taskDescription);
                TextView due = row.findViewById(R.id.taskDueDate);
                TextView statusBadge = row.findViewById(R.id.taskStatusBadge);

                if (title != null) {
                    title.setText(task.getTitle() == null ? "Care task" : task.getTitle());
                }
                if (reason != null) {
                    String r = task.getReason();
                    if (r != null && !r.trim().isEmpty()) {
                        reason.setText("Reason: " + r);
                        reason.setVisibility(View.VISIBLE);
                    } else {
                        reason.setVisibility(View.GONE);
                    }
                }
                if (description != null) {
                    String value = task.getDescription();
                    if (value != null && !value.trim().isEmpty()) {
                        description.setText(value);
                        description.setVisibility(View.VISIBLE);
                    } else {
                        description.setVisibility(View.GONE);
                    }
                }
                if (due != null) {
                    String value = task.getDueDate();
                    if (value != null && !value.trim().isEmpty()) {
                        due.setText("Due: " + value);
                        due.setVisibility(View.VISIBLE);
                    } else {
                        due.setVisibility(View.GONE);
                    }
                }
                if (statusBadge != null) {
                    String currentStatus = task.getStatus() == null ? "PENDING" : task.getStatus().toUpperCase();
                    
                    if ("COMPLETED".equals(currentStatus)) {
                        statusBadge.setText("Completed");
                        statusBadge.setTextColor(android.graphics.Color.parseColor("#065F46")); // Dark green
                        statusBadge.setBackgroundTintList(android.content.res.ColorStateList.valueOf(android.graphics.Color.parseColor("#D1FAE5"))); // Light green
                    } else if ("AWAITING_REVIEW".equals(currentStatus)) {
                        statusBadge.setText("Awaiting Review");
                        statusBadge.setTextColor(android.graphics.Color.parseColor("#1E40AF")); // Dark blue
                        statusBadge.setBackgroundTintList(android.content.res.ColorStateList.valueOf(android.graphics.Color.parseColor("#DBEAFE"))); // Light blue
                    } else {
                        statusBadge.setText("Pending");
                        statusBadge.setTextColor(android.graphics.Color.parseColor("#9A3412")); // Dark orange
                        statusBadge.setBackgroundTintList(android.content.res.ColorStateList.valueOf(android.graphics.Color.parseColor("#FFEDD5"))); // Light orange
                    }
                }
                
                row.setOnClickListener(v -> {
                    String currentStatus = task.getStatus() == null ? "PENDING" : task.getStatus().toUpperCase();
                    
                    LinearLayout form = new LinearLayout(getContext());
                    form.setOrientation(LinearLayout.VERTICAL);
                    int pad = dp(18);
                    form.setPadding(pad, dp(8), pad, 0);

                    EditText noteInput = new EditText(getContext());
                    noteInput.setHint("Add an optional completion note");
                    if (task.getCompletionNotes() != null) {
                        noteInput.setText(task.getCompletionNotes());
                    }
                    form.addView(noteInput, new LinearLayout.LayoutParams(-1, -2));

                    AlertDialog.Builder builder = new AlertDialog.Builder(getContext())
                            .setTitle(task.getTitle())
                            .setView(form);
                    
                    UserSession session = new UserSession(getContext());
                    boolean isAdmin = session.isAdmin();
                    
                    if ("COMPLETED".equals(currentStatus)) {
                        builder.setMessage("This task is completed.\n" + (task.getDescription() != null ? task.getDescription() : ""));
                        builder.setPositiveButton("Save Note", (d, w) -> {
                            tasksRef.child(task.getId()).child("completionNotes").setValue(noteInput.getText().toString().trim());
                        });
                        builder.setNeutralButton("Mark Pending", (d, w) -> {
                            tasksRef.child(task.getId()).child("status").setValue("PENDING");
                        });
                    } else if ("AWAITING_REVIEW".equals(currentStatus)) {
                        builder.setMessage("Patient submitted this for review.\n" + (task.getDescription() != null ? task.getDescription() : ""));
                        if (isAdmin) {
                            builder.setPositiveButton("Verify & Complete", (d, w) -> {
                                tasksRef.child(task.getId()).child("status").setValue("COMPLETED");
                            });
                        }
                        builder.setNeutralButton("Mark Pending", (d, w) -> {
                            tasksRef.child(task.getId()).child("status").setValue("PENDING");
                        });
                    } else {
                        builder.setMessage("Instructions: " + (task.getDescription() != null ? task.getDescription() : ""));
                        builder.setPositiveButton("Submit for Review", (d, w) -> {
                            java.util.Map<String, Object> updates = new java.util.HashMap<>();
                            updates.put("status", "AWAITING_REVIEW");
                            updates.put("completionNotes", noteInput.getText().toString().trim());
                            tasksRef.child(task.getId()).updateChildren(updates);
                            
                            // Send notification to admin/provider
                            String notificationId = FirebaseHelper.getRootRef().child("notifications").push().getKey();
                            if (notificationId != null) {
                                java.util.Map<String, Object> notifReq = new java.util.HashMap<>();
                                notifReq.put("recipientId", "admin"); // Notify provider/admin
                                notifReq.put("title", "Patient Completed Task");
                                notifReq.put("body", "Patient completed task: " + task.getTitle());
                                notifReq.put("status", "pending");
                                notifReq.put("timestamp", System.currentTimeMillis());
                                FirebaseHelper.getRootRef().child("notifications").child(notificationId).setValue(notifReq);
                            }
                        });
                        if (isAdmin) {
                            builder.setNeutralButton("Delete Task", (d, w) -> {
                                tasksRef.child(task.getId()).removeValue();
                            });
                        }
                    }
                    builder.setNegativeButton("Cancel", null);
                    builder.show();
                });
                
                tasksContainer.addView(row);
            }
            
            // Add separator
            View separator = new View(getContext());
            separator.setLayoutParams(new LinearLayout.LayoutParams(-1, dp(1)));
            separator.setBackgroundColor(android.graphics.Color.parseColor("#E5E7EB"));
            ((LinearLayout.LayoutParams) separator.getLayoutParams()).setMargins(dp(16), dp(8), dp(16), dp(8));
            tasksContainer.addView(separator);
        }
    }

    private void detachEmptyView() {
        if (tasksEmptyText != null && tasksEmptyText.getParent() instanceof android.view.ViewGroup) {
            ((android.view.ViewGroup) tasksEmptyText.getParent()).removeView(tasksEmptyText);
        }
    }

    private void showAddTaskDialog() {
        LinearLayout form = new LinearLayout(getContext());
        form.setOrientation(LinearLayout.VERTICAL);
        int pad = dp(18);
        form.setPadding(pad, dp(8), pad, 0);

        EditText title = new EditText(getContext());
        title.setHint("Task title");
        title.setSingleLine(true);
        form.addView(title, new LinearLayout.LayoutParams(-1, -2));

        EditText description = new EditText(getContext());
        description.setHint("Description (optional)");
        description.setMinLines(2);
        description.setGravity(android.view.Gravity.TOP);
        form.addView(description, new LinearLayout.LayoutParams(-1, -2));

        TextView due = new TextView(getContext());
        due.setText("Select Due Date");
        due.setTextSize(16);
        due.setPadding(dp(4), dp(16), dp(4), dp(16));
        due.setOnClickListener(v -> {
            java.util.Calendar cal = java.util.Calendar.getInstance();
            new android.app.DatePickerDialog(getContext(), (view, year, month, dayOfMonth) -> {
                cal.set(year, month, dayOfMonth);
                due.setText(new SimpleDateFormat("MMM dd, yyyy", Locale.US).format(cal.getTime()));
            }, cal.get(java.util.Calendar.YEAR), cal.get(java.util.Calendar.MONTH), cal.get(java.util.Calendar.DAY_OF_MONTH)).show();
        });
        form.addView(due, new LinearLayout.LayoutParams(-1, -2));

        AlertDialog dialog = new AlertDialog.Builder(getContext())
                .setTitle("Add care task")
                .setView(form)
                .setNegativeButton("Cancel", null)
                .setPositiveButton("Save task", null)
                .create();
        dialog.setOnShowListener(unused -> dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(v -> {
            String taskTitle = title.getText().toString().trim();
            if (taskTitle.isEmpty()) {
                title.setError("Enter a task title");
                return;
            }
            DatabaseReference ref = tasksRef.push();
            long now = System.currentTimeMillis();
            String dueDate = due.getText().toString().trim();
            if ("Select Due Date".equals(dueDate)) dueDate = "";
            CareTask task = new CareTask(ref.getKey(), userId, taskTitle,
                    description.getText().toString().trim(), "", "Patient", "", dueDate, "PENDING", now);
            ref.setValue(task).addOnCompleteListener(result -> {
                if (result.isSuccessful()) {
                    Toast.makeText(getContext(), "Care task added.", Toast.LENGTH_SHORT).show();
                    dialog.dismiss();
                } else {
                    Toast.makeText(getContext(), "Could not save task: " +
                            (result.getException() == null ? "Unknown error" : result.getException().getMessage()), Toast.LENGTH_LONG).show();
                }
            });
        }));
        dialog.show();
    }

    private int dp(int value) {
        return Math.round(value * getContext().getResources().getDisplayMetrics().density);
    }

    @Override public void dismiss() {
        if (tasksRef != null && tasksListener != null) tasksRef.removeEventListener(tasksListener);
        super.dismiss();
    }
}
