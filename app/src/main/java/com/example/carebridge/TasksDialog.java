package com.example.carebridge;

import android.app.Dialog;
import android.content.Context;
import android.graphics.Paint;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;

import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;
import java.util.List;

public class TasksDialog extends Dialog {

    private final String userId;
    private LinearLayout tasksContainer;
    private TextView tasksEmptyText;

    public TasksDialog(@NonNull Context context, String userId) {
        super(context);
        this.userId = userId;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.dialog_tasks);

        tasksContainer = findViewById(R.id.tasksContainer);
        tasksEmptyText = findViewById(R.id.tasksEmptyText);
        Button closeButton = findViewById(R.id.closeTasksButton);
        Button addTaskButton = findViewById(R.id.addTaskButton);

        if (closeButton != null) {
            closeButton.setOnClickListener(v -> dismiss());
        }

        if (addTaskButton != null) {
            addTaskButton.setOnClickListener(v -> {
                dismiss();
                AddActivityDialog dialog = new AddActivityDialog(getContext(), userId);
                dialog.show();
            });
        }

        loadTasksFromFirebase();
    }

    private void loadTasksFromFirebase() {
        FirebaseHelper.getTasksRef(userId).addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                List<CareTask> taskList = new ArrayList<>();
                for (DataSnapshot child : snapshot.getChildren()) {
                    CareTask task = child.getValue(CareTask.class);
                    if (task != null) {
                        taskList.add(task);
                    }
                }
                renderTasks(taskList);
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Toast.makeText(getContext(), "Error loading tasks: " + error.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void renderTasks(List<CareTask> taskList) {
        tasksContainer.removeAllViews();

        if (taskList.isEmpty()) {
            if (tasksEmptyText != null) {
                tasksEmptyText.setText("No care tasks recorded yet.");
                tasksContainer.addView(tasksEmptyText);
            }
            return;
        }

        for (CareTask task : taskList) {
            View rowView = LayoutInflater.from(getContext()).inflate(R.layout.item_task_row, tasksContainer, false);

            CheckBox checkBox = rowView.findViewById(R.id.taskCheckBox);
            TextView titleText = rowView.findViewById(R.id.taskTitle);
            TextView descText = rowView.findViewById(R.id.taskDescription);
            TextView dueText = rowView.findViewById(R.id.taskDueDate);

            if (titleText != null) {
                titleText.setText(task.getTitle());
                if (task.isCompleted()) {
                    titleText.setPaintFlags(titleText.getPaintFlags() | Paint.STRIKE_THRU_TEXT_FLAG);
                } else {
                    titleText.setPaintFlags(titleText.getPaintFlags() & (~Paint.STRIKE_THRU_TEXT_FLAG));
                }
            }

            if (descText != null) {
                descText.setText(task.getDescription());
            }

            if (dueText != null) {
                if (task.getDueDate() != null && !task.getDueDate().isEmpty()) {
                    dueText.setText("Due: " + task.getDueDate());
                    dueText.setVisibility(View.VISIBLE);
                } else {
                    dueText.setVisibility(View.GONE);
                }
            }

            if (checkBox != null) {
                checkBox.setChecked(task.isCompleted());
                checkBox.setOnCheckedChangeListener((buttonView, isChecked) -> {
                    task.setCompleted(isChecked);
                    if (task.getId() != null) {
                        FirebaseHelper.getTasksRef(userId).child(task.getId()).child("completed").setValue(isChecked);
                    }
                });
            }

            tasksContainer.addView(rowView);
        }
    }
}
