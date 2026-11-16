package com.example.carebridge;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class AdminActivity extends Activity {
    private LinearLayout doctorsContainer, adminOffersTrackerContainer;
    private TextView doctorsEmptyText, adminOffersEmptyText;
    private Spinner adminDoctorSpinner;
    private EditText adminPatientIdInput, adminDeptInput, adminSlot1Input, adminSlot2Input;
    private final List<Doctor> availableDoctors = new ArrayList<>();
    private final List<Doctor> spinnerDoctors = new ArrayList<>();
    private ValueEventListener doctorsListener, offersListener, notificationsListener;

    @Override protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_admin);

        UserSession session = new UserSession(this);
        if (!session.isLoggedIn() || !session.isAdmin()) {
            Toast.makeText(this, "Administrator access required.", Toast.LENGTH_LONG).show();
            FirebaseAuth.getInstance().signOut();
            session.logoutUser();
            startActivity(new Intent(this, MainActivity.class));
            finish();
            return;
        }
        
        // Simulating the backend push notification delivery for testing purposes:
        // We listen to /notifications/ where recipientId == "admin" and status == "pending"
        notificationsListener = new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (isFinishing()) return;
                for (DataSnapshot child : snapshot.getChildren()) {
                    String recipientId = child.child("recipientId").getValue(String.class);
                    String status = child.child("status").getValue(String.class);
                    if ("admin".equals(recipientId) && "pending".equals(status)) {
                        String title = child.child("title").getValue(String.class);
                        String body = child.child("body").getValue(String.class);
                        
                        Intent intent = new Intent(AdminActivity.this, AdminActivity.class);
                        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);

                        NotificationHelper.showNotification(
                                AdminActivity.this,
                                title != null ? title : "CareBridge Admin",
                                body != null ? body : "You have a new update.",
                                intent
                        );
                        
                        // Mark as delivered
                        child.getRef().child("status").setValue("delivered");
                    }
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {}
        };
        FirebaseHelper.getRootRef().child("notifications").addValueEventListener(notificationsListener);

        doctorsContainer = findViewById(R.id.doctorsContainer);
        doctorsEmptyText = findViewById(R.id.doctorsEmptyText);
        adminDoctorSpinner = findViewById(R.id.adminDoctorSpinner);
        adminPatientIdInput = findViewById(R.id.adminPatientIdInput);
        adminDeptInput = findViewById(R.id.adminDeptInput);
        adminSlot1Input = findViewById(R.id.adminSlot1Input);
        adminSlot2Input = findViewById(R.id.adminSlot2Input);
        adminOffersTrackerContainer = findViewById(R.id.adminOffersTrackerContainer);
        adminOffersEmptyText = findViewById(R.id.adminOffersEmptyText);

        Button backButton = findViewById(R.id.adminBackButton);
        Button logoutButton = findViewById(R.id.adminLogoutButton);
        Button sendOfferButton = findViewById(R.id.sendOfferButton);

        if (backButton != null) backButton.setOnClickListener(v -> finish());
        if (logoutButton != null) logoutButton.setOnClickListener(v -> logout());
        if (sendOfferButton != null) sendOfferButton.setOnClickListener(v -> sendOfferToPatient());

        FirebaseHelper.seedDoctorsIfEmpty();
        listenForDoctors();
        listenForOffers();
    }

    private void logout() {
        FirebaseAuth.getInstance().signOut();
        new UserSession(this).logoutUser();
        Intent intent = new Intent(this, MainActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }

    private void listenForDoctors() {
        doctorsListener = new ValueEventListener() {
            @Override public void onDataChange(@NonNull DataSnapshot snapshot) {
                availableDoctors.clear();
                for (DataSnapshot child : snapshot.getChildren()) {
                    Doctor doctor = child.getValue(Doctor.class);
                    if (doctor != null) {
                        if (doctor.getId() == null) doctor.setId(child.getKey());
                        availableDoctors.add(doctor);
                    }
                }
                renderDoctors();
                populateDoctorSpinner();
            }
            @Override public void onCancelled(@NonNull DatabaseError error) {
                toast("Unable to load doctors: " + error.getMessage());
            }
        };
        FirebaseHelper.getDoctorsRef().addValueEventListener(doctorsListener);
    }

    private void renderDoctors() {
        doctorsContainer.removeAllViews();
        if (availableDoctors.isEmpty()) {
            if (doctorsEmptyText != null) {
                doctorsEmptyText.setText("No doctors are available yet.");
                doctorsContainer.addView(doctorsEmptyText);
            }
            return;
        }
        for (Doctor doctor : availableDoctors) {
            View row = LayoutInflater.from(this).inflate(R.layout.item_doctor_row, doctorsContainer, false);
            TextView name = row.findViewById(R.id.docName);
            TextView specialty = row.findViewById(R.id.docSpecialty);
            TextView schedule = row.findViewById(R.id.docSchedule);
            TextView status = row.findViewById(R.id.docStatusBadge);
            if (name != null) name.setText(doctor.getName());
            if (specialty != null) specialty.setText(doctor.getSpecialty() + " • " + doctor.getLocation());
            if (schedule != null) schedule.setText(doctor.getScheduleSummary());
            if (status != null) status.setText(doctor.isAvailable() ? "AVAILABLE" : "UNAVAILABLE");
            doctorsContainer.addView(row);
        }
    }

    private void populateDoctorSpinner() {
        spinnerDoctors.clear();
        List<String> names = new ArrayList<>();
        for (Doctor doctor : availableDoctors) {
            if (doctor.isAvailable()) {
                spinnerDoctors.add(doctor);
                names.add(doctor.getName() + " (" + doctor.getSpecialty() + ")");
            }
        }
        if (names.isEmpty()) names.add("No available doctors");
        ArrayAdapter<String> adapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, names);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        adminDoctorSpinner.setAdapter(adapter);
    }

    private void sendOfferToPatient() {
        String patientId = adminPatientIdInput.getText().toString().trim().toLowerCase();
        String department = adminDeptInput.getText().toString().trim();
        String slot1 = adminSlot1Input.getText().toString().trim();
        String slot2 = adminSlot2Input.getText().toString().trim();

        if (patientId.isEmpty()) { toast("Enter the patient's User ID."); return; }
        if (spinnerDoctors.isEmpty() || adminDoctorSpinner.getSelectedItemPosition() < 0) {
            toast("There are no available doctors to select."); return;
        }
        List<String> slots = new ArrayList<>();
        if (!slot1.isEmpty()) slots.add(slot1);
        if (!slot2.isEmpty()) slots.add(slot2);
        if (slots.isEmpty()) { toast("Enter at least one available appointment date and time."); return; }
        if (slots.size() != new java.util.HashSet<>(slots).size()) { toast("Remove duplicate time slots."); return; }

        Doctor doctor = spinnerDoctors.get(adminDoctorSpinner.getSelectedItemPosition());
        if (department.isEmpty()) department = doctor.getSpecialty() + " consultation";

        String safePatientId = FirebaseHelper.sanitizeFirebaseKey(patientId);
        DatabaseReference patientRef = FirebaseHelper.getUserRef(safePatientId);
        findPatientAndSaveOffer(patientRef, safePatientId, doctor, department, slots);
    }

    private void findPatientAndSaveOffer(DatabaseReference patientRef, String patientId,
                                         Doctor doctor, String department, List<String> slots) {
        patientRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (!snapshot.exists()) {
                    toast("Patient not found. Check the User ID.");
                    return;
                }
                User patient = snapshot.getValue(User.class);
                if (patient == null) {
                    toast("Patient profile could not be read.");
                    return;
                }
                String patientName = patient.getFullName() == null ? patientId : patient.getFullName();
                DatabaseReference rootOfferRef = FirebaseHelper.getAllOffersRef().push();
                String offerId = rootOfferRef.getKey();
                if (offerId == null) { toast("Could not create appointment offer."); return; }

                AppointmentOffer offer = new AppointmentOffer(
                        offerId, patientId, patientName, doctor.getId(), doctor.getName(),
                        department, doctor.getLocation(), "PENDING", slots, System.currentTimeMillis());

                Map<String, Object> updates = new HashMap<>();
                updates.put("/appointment_offers/" + offerId, offer);
                updates.put("/users/" + patientId + "/appointment_offers/" + offerId, offer);

                // Create a notification request in the database for Cloud Functions/backend to process
                String notificationId = FirebaseHelper.getRootRef().child("notifications").push().getKey();
                if (notificationId != null) {
                    Map<String, Object> notifReq = new HashMap<>();
                    notifReq.put("recipientId", patientId);
                    notifReq.put("title", "New Appointment Offer");
                    notifReq.put("body", "You have a new appointment offer from " + doctor.getName());
                    notifReq.put("offerId", offerId);
                    notifReq.put("status", "pending");
                    notifReq.put("timestamp", System.currentTimeMillis());
                    updates.put("/notifications/" + notificationId, notifReq);
                }

                FirebaseHelper.getRootRef().updateChildren(updates).addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        toast("Appointment offer sent to " + patientName + ". It is waiting for the patient's selection.");
                        adminDeptInput.setText("");
                        adminSlot1Input.setText("");
                        adminSlot2Input.setText("");
                    } else {
                        String message = task.getException() == null ? "Unknown error" : task.getException().getMessage();
                        toast("Could not send offer: " + message);
                    }
                });
            }
            @Override public void onCancelled(@NonNull DatabaseError error) {
                toast("Could not verify patient: " + error.getMessage());
            }
        });
    }

    private void listenForOffers() {
        offersListener = new ValueEventListener() {
            @Override public void onDataChange(@NonNull DataSnapshot snapshot) {
                List<AppointmentOffer> offers = new ArrayList<>();
                for (DataSnapshot child : snapshot.getChildren()) {
                    AppointmentOffer offer = child.getValue(AppointmentOffer.class);
                    if (offer != null) {
                        if (offer.getId() == null) offer.setId(child.getKey());
                        offers.add(offer);
                    }
                }
                renderOffers(offers);
            }
            @Override public void onCancelled(@NonNull DatabaseError error) {
                toast("Unable to load offer updates: " + error.getMessage());
            }
        };
        FirebaseHelper.getAllOffersRef().addValueEventListener(offersListener);
    }

    private void renderOffers(List<AppointmentOffer> offers) {
        adminOffersTrackerContainer.removeAllViews();
        if (offers.isEmpty()) {
            if (adminOffersEmptyText != null) {
                adminOffersEmptyText.setText("No appointment offers have been sent.");
                adminOffersTrackerContainer.addView(adminOffersEmptyText);
            }
            return;
        }
        for (AppointmentOffer offer : offers) {
            View card = LayoutInflater.from(this).inflate(R.layout.item_admin_offer_card, adminOffersTrackerContainer, false);
            TextView patient = card.findViewById(R.id.adminOfferPatient);
            TextView provider = card.findViewById(R.id.adminOfferProviderDept);
            TextView details = card.findViewById(R.id.adminOfferDetails);
            TextView status = card.findViewById(R.id.adminOfferStatusBadge);
            TextView checkinStatus = card.findViewById(R.id.adminOfferCheckinStatus);
            
            if (patient != null) patient.setText("Patient: " + safe(offer.getPatientName(), offer.getUserId()));
            if (provider != null) provider.setText(safe(offer.getProviderName(), "Doctor") + " • " + safe(offer.getDepartment(), "Consultation"));
            String state = safe(offer.getStatus(), "PENDING").toUpperCase();
            if (status != null) status.setText(state);
            
            if (checkinStatus != null) {
                if (offer.isPatientCheckedIn()) {
                    checkinStatus.setText("Patient has checked in.");
                    checkinStatus.setVisibility(View.VISIBLE);
                } else {
                    checkinStatus.setVisibility(View.GONE);
                }
            }
            if (details != null) {
                if ("CONFIRMED".equals(state)) {
                    details.setText("Patient selected: " + safe(offer.getBookedSlot(), "Confirmed slot"));
                    Button btnAssignTask = card.findViewById(R.id.btnAssignTask);
                    Button btnViewTasks = card.findViewById(R.id.btnViewTasks);
                    if (btnAssignTask != null) {
                        btnAssignTask.setVisibility(View.VISIBLE);
                        btnAssignTask.setOnClickListener(v -> showAssignTaskDialog(offer));
                    }
                    if (btnViewTasks != null) {
                        btnViewTasks.setVisibility(View.VISIBLE);
                        btnViewTasks.setOnClickListener(v -> {
                            TasksDialog dialog = new TasksDialog(this, offer.getUserId());
                            dialog.show();
                        });
                    }
                }
                else if ("DECLINED".equals(state)) details.setText("Patient declined this offer.");
                else if ("EXPIRED".equals(state)) details.setText("This offer has expired.");
                else details.setText("Available times: " + (offer.getAvailableSlots() == null ? "Not provided" : android.text.TextUtils.join(", ", offer.getAvailableSlots())));
            }
            adminOffersTrackerContainer.addView(card);
        }
    }

    private void showAssignTaskDialog(AppointmentOffer offer) {
        LinearLayout form = new LinearLayout(this);
        form.setOrientation(LinearLayout.VERTICAL);
        int pad = 40;
        form.setPadding(pad, 20, pad, 0);

        EditText titleInput = new EditText(this);
        titleInput.setHint("Task Title (e.g. Bring Medical Records)");
        form.addView(titleInput);

        EditText descInput = new EditText(this);
        descInput.setHint("Instructions");
        form.addView(descInput);

        EditText reasonInput = new EditText(this);
        reasonInput.setHint("Reason (Why is this needed?)");
        form.addView(reasonInput);

        TextView dueInput = new TextView(this);
        String dateOnly = safe(offer.getBookedSlot(), "").split(" at ")[0].trim();
        dueInput.setText(dateOnly.isEmpty() ? "Select Due Date" : dateOnly);
        dueInput.setTextSize(16);
        dueInput.setPadding(0, 20, 0, 20);
        dueInput.setOnClickListener(v -> {
            java.util.Calendar cal = java.util.Calendar.getInstance();
            new android.app.DatePickerDialog(this, (view, year, month, dayOfMonth) -> {
                cal.set(year, month, dayOfMonth);
                dueInput.setText(new java.text.SimpleDateFormat("MMM dd, yyyy", java.util.Locale.US).format(cal.getTime()));
            }, cal.get(java.util.Calendar.YEAR), cal.get(java.util.Calendar.MONTH), cal.get(java.util.Calendar.DAY_OF_MONTH)).show();
        });
        form.addView(dueInput);

        new android.app.AlertDialog.Builder(this)
                .setTitle("Assign Prep Task")
                .setView(form)
                .setPositiveButton("Assign", (dialog, which) -> {
                    String title = titleInput.getText().toString().trim();
                    if (title.isEmpty()) {
                        toast("Task title is required.");
                        return;
                    }
                    String dueDate = dueInput.getText().toString().trim();
                    if ("Select Due Date".equals(dueDate)) dueDate = "";
                    assignTaskToPatient(offer, title, descInput.getText().toString().trim(), 
                                        reasonInput.getText().toString().trim(), dueDate);
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void assignTaskToPatient(AppointmentOffer offer, String title, String desc, String reason, String dueDate) {
        String patientId = offer.getUserId();
        DatabaseReference tasksRef = FirebaseHelper.getTasksRef(patientId);
        String taskId = tasksRef.push().getKey();
        if (taskId == null) return;

        UserSession session = new UserSession(this);
        String adminName = session.getFullName().isEmpty() ? "Care Coordinator" : session.getFullName();

        CareTask task = new CareTask(taskId, patientId, title, desc, reason, adminName, offer.getId(), dueDate, "PENDING", System.currentTimeMillis());
        
        Map<String, Object> updates = new HashMap<>();
        updates.put("/users/" + patientId + "/tasks/" + taskId, task);

        // Notify patient
        String notificationId = FirebaseHelper.getRootRef().child("notifications").push().getKey();
        if (notificationId != null) {
            Map<String, Object> notifReq = new HashMap<>();
            notifReq.put("recipientId", patientId);
            notifReq.put("title", "New Care Task");
            notifReq.put("body", "You have a new task assigned for your upcoming appointment with " + offer.getProviderName());
            notifReq.put("offerId", offer.getId());
            notifReq.put("taskId", taskId);
            notifReq.put("status", "pending");
            notifReq.put("timestamp", System.currentTimeMillis());
            updates.put("/notifications/" + notificationId, notifReq);
        }

        FirebaseHelper.getRootRef().updateChildren(updates).addOnCompleteListener(t -> {
            if (t.isSuccessful()) {
                toast("Task assigned successfully.");
            } else {
                toast("Failed to assign task.");
            }
        });
    }

    private String safe(String value, String fallback) {
        return value == null || value.trim().isEmpty() ? fallback : value;
    }

    private void toast(String message) {
        Toast.makeText(this, message, Toast.LENGTH_LONG).show();
    }

    @Override protected void onDestroy() {
        super.onDestroy();
        if (doctorsListener != null) FirebaseHelper.getDoctorsRef().removeEventListener(doctorsListener);
        if (offersListener != null) FirebaseHelper.getAllOffersRef().removeEventListener(offersListener);
        if (notificationsListener != null) FirebaseHelper.getRootRef().child("notifications").removeEventListener(notificationsListener);
    }
}
