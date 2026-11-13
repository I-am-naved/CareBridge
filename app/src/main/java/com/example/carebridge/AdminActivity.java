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
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;
import java.util.List;

public class AdminActivity extends Activity {

    private LinearLayout doctorsContainer;
    private TextView doctorsEmptyText;
    private Spinner adminDoctorSpinner;
    private EditText adminPatientIdInput;
    private EditText adminDeptInput;
    private EditText adminSlot1Input;
    private EditText adminSlot2Input;
    private LinearLayout adminOffersTrackerContainer;
    private TextView adminOffersEmptyText;

    private List<Doctor> availableDoctorList = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_admin);

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

        UserSession session = new UserSession(this);

        if (backButton != null) {
            backButton.setOnClickListener(v -> finish());
        }

        if (logoutButton != null) {
            logoutButton.setOnClickListener(v -> {
                // Remove FCM token (if admin accidentally got one)
                String userId = session.getUserId();
                if (userId != null && !userId.isEmpty()) {
                    DatabaseReference tokenRef = FirebaseDatabase.getInstance(FirebaseHelper.DATABASE_URL)
                            .getReference("users")
                            .child(userId)
                            .child("fcmToken");
                    tokenRef.removeValue();
                }

                FirebaseAuth.getInstance().signOut();
                session.logoutUser();
                Intent intent = new Intent(this, MainActivity.class);
                intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                startActivity(intent);
                finish();
            });
        }

        if (session.isLoggedIn() && adminPatientIdInput != null && !"admin".equalsIgnoreCase(session.getUserId())) {
            adminPatientIdInput.setText(session.getUserId());
        }

        // Seed doctors if empty
        FirebaseHelper.seedDoctorsIfEmpty();

        loadDoctorsFromFirebase();
        loadAllOffersFromFirebase();

        if (sendOfferButton != null) {
            sendOfferButton.setOnClickListener(v -> sendOfferToPatient());
        }
    }

    private void loadDoctorsFromFirebase() {
        FirebaseHelper.getDoctorsRef().addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                availableDoctorList.clear();
                for (DataSnapshot child : snapshot.getChildren()) {
                    Doctor doc = child.getValue(Doctor.class);
                    if (doc != null) {
                        availableDoctorList.add(doc);
                    }
                }
                renderDoctors(availableDoctorList);
                populateDoctorSpinner(availableDoctorList);
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Toast.makeText(AdminActivity.this, "Error loading doctors: " + error.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void renderDoctors(List<Doctor> doctorList) {
        doctorsContainer.removeAllViews();

        if (doctorList.isEmpty()) {
            if (doctorsEmptyText != null) {
                doctorsEmptyText.setText("No doctors found in system.");
                doctorsContainer.addView(doctorsEmptyText);
            }
            return;
        }

        for (Doctor doc : doctorList) {
            View rowView = LayoutInflater.from(this).inflate(R.layout.item_doctor_row, doctorsContainer, false);

            TextView nameText = rowView.findViewById(R.id.docName);
            TextView specText = rowView.findViewById(R.id.docSpecialty);
            TextView schedText = rowView.findViewById(R.id.docSchedule);
            TextView badgeText = rowView.findViewById(R.id.docStatusBadge);

            if (nameText != null) nameText.setText(doc.getName());
            if (specText != null) specText.setText(doc.getSpecialty() + "  •  " + doc.getLocation());
            if (schedText != null) schedText.setText("🕒 " + doc.getScheduleSummary());

            if (badgeText != null) {
                if (doc.isAvailable()) {
                    badgeText.setText("AVAILABLE");
                    badgeText.setBackgroundColor(0xFFE8F5F2);
                    badgeText.setTextColor(0xFF2D8C88);
                } else {
                    badgeText.setText("UNAVAILABLE");
                    badgeText.setBackgroundColor(0xFFFEF3F2);
                    badgeText.setTextColor(0xFFB42318);
                }
            }

            doctorsContainer.addView(rowView);
        }
    }

    private void populateDoctorSpinner(List<Doctor> doctorList) {
        List<String> docNames = new ArrayList<>();
        for (Doctor d : doctorList) {
            if (d.isAvailable()) {
                docNames.add(d.getName() + " (" + d.getSpecialty() + ")");
            }
        }
        if (docNames.isEmpty()) {
            docNames.add("No available doctors");
        }

        ArrayAdapter<String> adapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, docNames);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        if (adminDoctorSpinner != null) {
            adminDoctorSpinner.setAdapter(adapter);
        }
    }

    private void sendOfferToPatient() {
        String patientId = adminPatientIdInput.getText().toString().trim().toLowerCase();
        String dept = adminDeptInput.getText().toString().trim();
        String slot1 = adminSlot1Input.getText().toString().trim();
        String slot2 = adminSlot2Input.getText().toString().trim();

        if (patientId.isEmpty()) {
            Toast.makeText(this, "Please enter a target patient User ID.", Toast.LENGTH_SHORT).show();
            return;
        }
        
        String safePatientId = FirebaseHelper.sanitizeFirebaseKey(patientId);

        if (availableDoctorList.isEmpty()) {
            Toast.makeText(this, "No doctors available.", Toast.LENGTH_SHORT).show();
            return;
        }

        int selectedIndex = adminDoctorSpinner.getSelectedItemPosition();
        Doctor selectedDoctor = (selectedIndex >= 0 && selectedIndex < availableDoctorList.size())
                ? availableDoctorList.get(selectedIndex)
                : availableDoctorList.get(0);

        if (dept.isEmpty()) {
            dept = selectedDoctor.getSpecialty() + " Consultation";
        }

        List<String> slots = new ArrayList<>();
        if (!slot1.isEmpty()) slots.add(slot1);
        if (!slot2.isEmpty()) slots.add(slot2);
        if (slots.isEmpty()) {
            slots.add("Nov 22, 2026 at 10:00 AM");
            slots.add("Nov 22, 2026 at 02:00 PM");
        }

        DatabaseReference offerRef = FirebaseHelper.getRootRef().child("appointment_offers").push();
        String offerId = offerRef.getKey();
        long now = System.currentTimeMillis();

        AppointmentOffer offer = new AppointmentOffer(
                offerId,
                safePatientId,
                "Patient (" + patientId + ")",
                selectedDoctor.getId(),
                selectedDoctor.getName(),
                dept,
                selectedDoctor.getLocation(),
                "PENDING",
                slots,
                now
        );

        // 1. Save to root /appointment_offers
        offerRef.setValue(offer);

        // 2. Save under patient node /users/{safePatientId}/appointment_offers/{offerId}
        if (offerId != null) {
            FirebaseHelper.getOffersRef(safePatientId).child(offerId).setValue(offer);
        }

        Toast.makeText(this, "Appointment offer sent to " + patientId + " successfully!", Toast.LENGTH_LONG).show();

        // Clear inputs
        if (adminDeptInput != null) adminDeptInput.setText("");
        if (adminSlot1Input != null) adminSlot1Input.setText("");
        if (adminSlot2Input != null) adminSlot2Input.setText("");
    }

    private void loadAllOffersFromFirebase() {
        FirebaseHelper.getAllOffersRef().addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                List<AppointmentOffer> allOffers = new ArrayList<>();
                for (DataSnapshot child : snapshot.getChildren()) {
                    AppointmentOffer offer = child.getValue(AppointmentOffer.class);
                    if (offer != null) {
                        allOffers.add(offer);
                    }
                }
                renderAdminOffersTracker(allOffers);
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Toast.makeText(AdminActivity.this, "Error loading offers tracker: " + error.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void renderAdminOffersTracker(List<AppointmentOffer> allOffers) {
        adminOffersTrackerContainer.removeAllViews();

        if (allOffers.isEmpty()) {
            if (adminOffersEmptyText != null) {
                adminOffersEmptyText.setText("No appointment offers sent yet.");
                adminOffersTrackerContainer.addView(adminOffersEmptyText);
            }
            return;
        }

        for (AppointmentOffer offer : allOffers) {
            View cardView = LayoutInflater.from(this).inflate(R.layout.item_admin_offer_card, adminOffersTrackerContainer, false);

            TextView patientText = cardView.findViewById(R.id.adminOfferPatient);
            TextView providerText = cardView.findViewById(R.id.adminOfferProviderDept);
            TextView detailsText = cardView.findViewById(R.id.adminOfferDetails);
            TextView statusBadge = cardView.findViewById(R.id.adminOfferStatusBadge);

            if (patientText != null) {
                patientText.setText("Patient: " + (offer.getPatientName() != null ? offer.getPatientName() : offer.getUserId()));
            }

            if (providerText != null) {
                providerText.setText(offer.getProviderName() + "  •  " + offer.getDepartment());
            }

            if (statusBadge != null) {
                if ("CONFIRMED".equalsIgnoreCase(offer.getStatus())) {
                    statusBadge.setText("CONFIRMED");
                    statusBadge.setBackgroundColor(0xFFE8F5F2);
                    statusBadge.setTextColor(0xFF2D8C88);
                } else {
                    statusBadge.setText("PENDING RESPONSE");
                    statusBadge.setBackgroundColor(0xFFFEF3F2);
                    statusBadge.setTextColor(0xFFB42318);
                }
            }

            if (detailsText != null) {
                if ("CONFIRMED".equalsIgnoreCase(offer.getStatus())) {
                    detailsText.setText("✓ Patient Selected: " + (offer.getBookedSlot() != null ? offer.getBookedSlot() : "Slot Confirmed"));
                    detailsText.setTextColor(0xFF2D8C88);
                } else {
                    detailsText.setText("⏳ Awaiting patient selection from offered slots.");
                    detailsText.setTextColor(0xFF667085);
                }
            }

            adminOffersTrackerContainer.addView(cardView);
        }
    }
}
