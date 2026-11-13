package com.example.carebridge;

import android.app.Dialog;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;

import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;
import java.util.List;

public class AppointmentsDialog extends Dialog {

    private final String userId;
    private LinearLayout offersContainer;
    private TextView offersEmptyText;
    private LinearLayout scheduledAppointmentsContainer;
    private TextView scheduledEmptyText;

    public AppointmentsDialog(@NonNull Context context, String userId) {
        super(context);
        this.userId = userId;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.dialog_appointments);

        offersContainer = findViewById(R.id.offersContainer);
        offersEmptyText = findViewById(R.id.offersEmptyText);
        scheduledAppointmentsContainer = findViewById(R.id.scheduledAppointmentsContainer);
        scheduledEmptyText = findViewById(R.id.scheduledEmptyText);
        Button closeButton = findViewById(R.id.closeAppointmentsButton);

        if (closeButton != null) {
            closeButton.setOnClickListener(v -> dismiss());
        }

        loadOffersFromFirebase();
        loadAppointmentsFromFirebase();
    }

    // Removed TTS methods

    private void loadOffersFromFirebase() {
        FirebaseHelper.getOffersRef(userId).addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                List<AppointmentOffer> offersList = new ArrayList<>();
                for (DataSnapshot child : snapshot.getChildren()) {
                    AppointmentOffer offer = child.getValue(AppointmentOffer.class);
                    if (offer != null) {
                        // Firebase fallback: If the internal ID is null, use the database key
                        if (offer.getId() == null || offer.getId().isEmpty()) {
                            offer.setId(child.getKey());
                        }

                        if ("AVAILABLE".equalsIgnoreCase(offer.getStatus()) || "PENDING".equalsIgnoreCase(offer.getStatus())) {
                            offersList.add(offer);
                        }
                    }
                }
                renderOffers(offersList);
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Toast.makeText(getContext(), "Error loading offers: " + error.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }

    // Removed offersListContains as it is no longer needed

    private void renderOffers(List<AppointmentOffer> offersList) {
        offersContainer.removeAllViews();

        if (offersList.isEmpty()) {
            if (offersEmptyText != null) {
                offersEmptyText.setText("No new appointment offers at this time.");
                offersContainer.addView(offersEmptyText);
            }
            return;
        }

        for (AppointmentOffer offer : offersList) {
            View cardView = LayoutInflater.from(getContext()).inflate(R.layout.item_offer_card, offersContainer, false);

            TextView providerText = cardView.findViewById(R.id.offerProvider);
            TextView departmentText = cardView.findViewById(R.id.offerDepartment);
            TextView locationText = cardView.findViewById(R.id.offerLocation);
            Spinner slotsSpinner = cardView.findViewById(R.id.offerSlotsSpinner);
            Button confirmButton = cardView.findViewById(R.id.confirmOfferButton);

            if (providerText != null) providerText.setText(offer.getProviderName());
            if (departmentText != null) departmentText.setText(offer.getDepartment());
            if (locationText != null) locationText.setText("Location: " + offer.getLocation());

            if (slotsSpinner != null && offer.getAvailableSlots() != null) {
                ArrayAdapter<String> adapter = new ArrayAdapter<>(getContext(), android.R.layout.simple_spinner_item, offer.getAvailableSlots());
                adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
                slotsSpinner.setAdapter(adapter);
            }

            if (confirmButton != null) {
                confirmButton.setOnClickListener(v -> {
                    String selectedSlot = (slotsSpinner != null && slotsSpinner.getSelectedItem() != null)
                            ? slotsSpinner.getSelectedItem().toString()
                            : "Nov 15, 2026 at 10:00 AM";
                    bookAppointmentFromOffer(offer, selectedSlot);
                });
            }

            offersContainer.addView(cardView);
        }
    }

    private void bookAppointmentFromOffer(AppointmentOffer offer, String selectedSlot) {
        // Parse date and time from slot string e.g. "Nov 18, 2026 at 09:00 AM"
        String date = selectedSlot;
        String time = "";
        if (selectedSlot.contains(" at ")) {
            String[] parts = selectedSlot.split(" at ");
            date = parts[0];
            time = parts[1];
        }

        // 1. Create Appointment
        DatabaseReference apptRef = FirebaseHelper.getAppointmentsRef(userId).push();
        String apptId = apptRef.getKey();
        Appointment appt = new Appointment(
                apptId,
                userId,
                offer.getProviderName(),
                offer.getDepartment(),
                offer.getLocation(),
                date,
                time,
                "Confirmed",
                "Scheduled via CareBridge offer.",
                System.currentTimeMillis()
        );
        apptRef.setValue(appt);

        // 2. Update Offer Status to CONFIRMED for Admin view
        if (offer.getId() != null) {
            long now = System.currentTimeMillis();
            FirebaseHelper.getOffersRef(userId).child(offer.getId()).child("status").setValue("CONFIRMED");
            FirebaseHelper.getOffersRef(userId).child(offer.getId()).child("bookedSlot").setValue(selectedSlot);
            FirebaseHelper.getOffersRef(userId).child(offer.getId()).child("confirmedAt").setValue(now);

            // Mirror to root /appointment_offers for Admin real-time listener
            FirebaseHelper.getAllOffersRef().child(offer.getId()).child("status").setValue("CONFIRMED");
            FirebaseHelper.getAllOffersRef().child(offer.getId()).child("bookedSlot").setValue(selectedSlot);
            FirebaseHelper.getAllOffersRef().child(offer.getId()).child("confirmedAt").setValue(now);
        }

        // 3. Add to Journey Timeline
        DatabaseReference jRef = FirebaseHelper.getJourneyRef(userId).push();
        JourneyItem jItem = new JourneyItem(
                jRef.getKey(),
                userId,
                "0",
                "Appointment",
                "Confirmed: " + offer.getDepartment(),
                "Appointment with " + offer.getProviderName() + " at " + offer.getLocation(),
                selectedSlot,
                System.currentTimeMillis()
        );
        jRef.setValue(jItem);

        // 4. Trigger Notification (No Toast)
        Intent homeIntent = new Intent(getContext(), activity_home.class);
        NotificationHelper.showNotification(
                getContext(),
                "Appointment Confirmed!",
                "Your appointment with " + offer.getProviderName() + " is set for " + selectedSlot + ".",
                homeIntent
        );

        dismiss();
    }

    private void loadAppointmentsFromFirebase() {
        FirebaseHelper.getAppointmentsRef(userId).addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                List<Appointment> apptList = new ArrayList<>();
                for (DataSnapshot child : snapshot.getChildren()) {
                    Appointment appt = child.getValue(Appointment.class);
                    if (appt != null) {
                        apptList.add(appt);
                    }
                }
                renderScheduledAppointments(apptList);
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Toast.makeText(getContext(), "Error loading appointments: " + error.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void renderScheduledAppointments(List<Appointment> apptList) {
        scheduledAppointmentsContainer.removeAllViews();

        if (apptList.isEmpty()) {
            if (scheduledEmptyText != null) {
                scheduledEmptyText.setText("No scheduled appointments.");
                scheduledAppointmentsContainer.addView(scheduledEmptyText);
            }
            return;
        }

        for (Appointment appt : apptList) {
            View cardView = LayoutInflater.from(getContext()).inflate(R.layout.item_appointment_card, scheduledAppointmentsContainer, false);

            TextView providerText = cardView.findViewById(R.id.apptProvider);
            TextView deptText = cardView.findViewById(R.id.apptDepartment);
            TextView dateTimeText = cardView.findViewById(R.id.apptDateTime);
            TextView locationText = cardView.findViewById(R.id.apptLocation);
            TextView statusText = cardView.findViewById(R.id.apptStatus);

            if (providerText != null) providerText.setText(appt.getProviderName());
            if (deptText != null) deptText.setText(appt.getDepartment());

            String dt = appt.getDate() + (appt.getTime() != null && !appt.getTime().isEmpty() ? " at " + appt.getTime() : "");
            if (dateTimeText != null) dateTimeText.setText("📅 " + dt);
            if (locationText != null) locationText.setText("📍 " + appt.getLocation());
            if (statusText != null) statusText.setText(appt.getStatus() != null ? appt.getStatus() : "Confirmed");

            Button btnViewOnMap = cardView.findViewById(R.id.btnViewOnMap);
            if (btnViewOnMap != null) {
                btnViewOnMap.setOnClickListener(v -> {
                    Intent intent = new Intent(getContext(), AppointmentLocationActivity.class);
                    intent.putExtra("clinicName", appt.getProviderName() + " (" + appt.getDepartment() + ")");
                    intent.putExtra("clinicAddress", appt.getLocation());
                    getContext().startActivity(intent);
                });
            }

            Button btnGetDirections = cardView.findViewById(R.id.btnGetDirections);
            if (btnGetDirections != null) {
                btnGetDirections.setOnClickListener(v -> {
                    android.net.Uri gmmIntentUri = android.net.Uri.parse("google.navigation:q=" + android.net.Uri.encode(appt.getLocation()));
                    Intent mapIntent = new Intent(Intent.ACTION_VIEW, gmmIntentUri);
                    mapIntent.setPackage("com.google.android.apps.maps");
                    if (mapIntent.resolveActivity(getContext().getPackageManager()) != null) {
                        getContext().startActivity(mapIntent);
                    } else {
                        Intent webIntent = new Intent(Intent.ACTION_VIEW, android.net.Uri.parse("https://www.google.com/maps/dir/?api=1&destination=" + android.net.Uri.encode(appt.getLocation())));
                        getContext().startActivity(webIntent);
                    }
                });
            }

            scheduledAppointmentsContainer.addView(cardView);
        }
    }
}
