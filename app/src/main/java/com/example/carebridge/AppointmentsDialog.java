package com.example.carebridge;

import android.app.Dialog;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
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
import com.google.firebase.database.MutableData;
import com.google.firebase.database.Transaction;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class AppointmentsDialog extends Dialog {
    private final String userId;
    private LinearLayout offersContainer, scheduledAppointmentsContainer;
    private TextView offersEmptyText, scheduledEmptyText;
    private DatabaseReference offersRef, appointmentsRef;
    private ValueEventListener offersListener, appointmentsListener;
    private boolean bookingInProgress = false;

    public AppointmentsDialog(@NonNull Context context, String userId) {
        super(context);
        this.userId = FirebaseHelper.sanitizeFirebaseKey(userId);
    }

    @Override protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.dialog_appointments);
        setCanceledOnTouchOutside(true);

        offersContainer = findViewById(R.id.offersContainer);
        offersEmptyText = findViewById(R.id.offersEmptyText);
        scheduledAppointmentsContainer = findViewById(R.id.scheduledAppointmentsContainer);
        scheduledEmptyText = findViewById(R.id.scheduledEmptyText);

        View close = findViewById(R.id.closeAppointmentsButton);
        if (close != null) close.setOnClickListener(v -> dismiss());

        Button addButton = findViewById(R.id.addAppointmentButton);
        if (addButton != null) {
            addButton.setOnClickListener(v ->
                    Toast.makeText(getContext(), "Appointment offers are sent by your care team.", Toast.LENGTH_SHORT).show());
        }

        offersRef = FirebaseHelper.getOffersRef(userId);
        appointmentsRef = FirebaseHelper.getAppointmentsRef(userId);
        attachListeners();
    }

    private void attachListeners() {
        offersListener = new ValueEventListener() {
            @Override public void onDataChange(@NonNull DataSnapshot snapshot) {
                List<AppointmentOffer> pending = new ArrayList<>();
                for (DataSnapshot child : snapshot.getChildren()) {
                    AppointmentOffer offer = child.getValue(AppointmentOffer.class);
                    if (offer == null) continue;
                    if (offer.getId() == null) offer.setId(child.getKey());
                    String status = safe(offer.getStatus(), "PENDING");
                    if ("PENDING".equalsIgnoreCase(status) || "AVAILABLE".equalsIgnoreCase(status)) {
                        pending.add(offer);
                    }
                }
                renderOffers(pending);
            }
            @Override public void onCancelled(@NonNull DatabaseError error) {
                showError(offersContainer, "Could not load offers: " + error.getMessage());
            }
        };
        appointmentsListener = new ValueEventListener() {
            @Override public void onDataChange(@NonNull DataSnapshot snapshot) {
                List<Appointment> appointments = new ArrayList<>();
                for (DataSnapshot child : snapshot.getChildren()) {
                    Appointment appointment = child.getValue(Appointment.class);
                    if (appointment != null) {
                        if (appointment.getId() == null) appointment.setId(child.getKey());
                        appointments.add(appointment);
                    }
                }
                renderAppointments(appointments);
            }
            @Override public void onCancelled(@NonNull DatabaseError error) {
                showError(scheduledAppointmentsContainer, "Could not load appointments: " + error.getMessage());
            }
        };
        offersRef.addValueEventListener(offersListener);
        appointmentsRef.addValueEventListener(appointmentsListener);
    }

    private void renderOffers(List<AppointmentOffer> offers) {
        offersContainer.removeAllViews();
        if (offers.isEmpty()) {
            showEmpty(offersContainer, offersEmptyText, "No new appointment offers. New offers from your care team will appear here.");
            return;
        }
        detachEmpty(offersEmptyText);
        for (AppointmentOffer offer : offers) {
            View card = LayoutInflater.from(getContext()).inflate(R.layout.item_offer_card, offersContainer, false);
            TextView provider = card.findViewById(R.id.offerProvider);
            TextView department = card.findViewById(R.id.offerDepartment);
            TextView location = card.findViewById(R.id.offerLocation);
            Spinner slots = card.findViewById(R.id.offerSlotsSpinner);
            Button confirm = card.findViewById(R.id.confirmOfferButton);

            if (provider != null) provider.setText(safe(offer.getProviderName(), "Healthcare provider"));
            if (department != null) department.setText(safe(offer.getDepartment(), "Appointment"));
            if (location != null) location.setText("Location: " + safe(offer.getLocation(), "Not provided"));

            List<String> available = offer.getAvailableSlots() == null ? new ArrayList<>() : offer.getAvailableSlots();
            ArrayAdapter<String> adapter = new ArrayAdapter<>(getContext(), android.R.layout.simple_spinner_item, available);
            adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
            if (slots != null) slots.setAdapter(adapter);

            if (confirm != null) {
                confirm.setEnabled(!available.isEmpty());
                confirm.setOnClickListener(v -> {
                    if (bookingInProgress) return;
                    Object selected = slots == null ? null : slots.getSelectedItem();
                    if (selected == null) {
                        Toast.makeText(getContext(), "No available time slots for this offer.", Toast.LENGTH_SHORT).show();
                        return;
                    }
                    confirm.setEnabled(false);
                    bookAppointmentFromOffer(offer, selected.toString(), confirm);
                });
            }
            offersContainer.addView(card);
        }
    }

    private void bookAppointmentFromOffer(AppointmentOffer offer, String selectedSlot, Button button) {
        if (offer.getId() == null || offer.getId().trim().isEmpty()) {
            button.setEnabled(true);
            Toast.makeText(getContext(), "This offer is missing its ID. Please contact the administrator.", Toast.LENGTH_LONG).show();
            return;
        }
        bookingInProgress = true;
        String offerId = offer.getId();
        String[] parts = selectedSlot.split("\\s+at\\s+", 2);
        String date = parts[0].trim();
        String time = parts.length > 1 ? parts[1].trim() : "";
        long now = System.currentTimeMillis();

        Appointment appointment = new Appointment(
                offerId, userId, offer.getProviderName(), offer.getDepartment(),
                offer.getLocation(), date, time, "CONFIRMED",
                "Confirmed by patient from an appointment offer.", now);

        String journeyId = offerId + "_confirmed";
        JourneyItem journey = new JourneyItem(
                journeyId, userId, "",
                "Appointment", "Appointment confirmed",
                "Appointment with " + safe(offer.getProviderName(), "your provider"),
                selectedSlot, now);

        DatabaseReference patientRef = FirebaseHelper.getUserRef(userId);
        patientRef.runTransaction(new Transaction.Handler() {
            @NonNull @Override public Transaction.Result doTransaction(@NonNull MutableData currentData) {
                MutableData offerNode = currentData.child("appointment_offers").child(offerId);
                String status = offerNode.child("status").getValue(String.class);
                if (status == null || !("PENDING".equalsIgnoreCase(status) || "AVAILABLE".equalsIgnoreCase(status))) {
                    return Transaction.abort();
                }

                List<String> slots = new ArrayList<>();
                for (MutableData child : offerNode.child("availableSlots").getChildren()) {
                    String slot = child.getValue(String.class);
                    if (slot != null) slots.add(slot);
                }
                if (!slots.contains(selectedSlot)) return Transaction.abort();

                // The patient offer, appointment and journey record are committed atomically.
                offerNode.child("status").setValue("CONFIRMED");
                offerNode.child("bookedSlot").setValue(selectedSlot);
                offerNode.child("confirmedAt").setValue(now);
                offerNode.child("appointmentId").setValue(offerId);
                currentData.child("appointments").child(offerId).setValue(appointment);
                currentData.child("journey").child(journeyId).setValue(journey);
                return Transaction.success(currentData);
            }

            @Override public void onComplete(DatabaseError error, boolean committed, DataSnapshot snapshot) {
                if (error != null || !committed) {
                    bookingInProgress = false;
                    button.setEnabled(true);
                    String message = error != null ? error.getMessage()
                            : "This offer is no longer available. Refresh and try another offer.";
                    Toast.makeText(getContext(), message, Toast.LENGTH_LONG).show();
                    return;
                }
                
                // Note: We use root ref here to update both offers and notifications atomically across the DB
                String notificationId = FirebaseHelper.getRootRef().child("notifications").push().getKey();
                Map<String, Object> rootUpdates = new HashMap<>();
                rootUpdates.put("/appointment_offers/" + offerId + "/status", "CONFIRMED");
                rootUpdates.put("/appointment_offers/" + offerId + "/bookedSlot", selectedSlot);
                rootUpdates.put("/appointment_offers/" + offerId + "/confirmedAt", now);
                rootUpdates.put("/appointment_offers/" + offerId + "/appointmentId", offerId);
                
                if (notificationId != null) {
                    Map<String, Object> notifReq = new HashMap<>();
                    notifReq.put("recipientId", "admin");
                    notifReq.put("title", "Appointment Confirmed");
                    notifReq.put("body", "Patient confirmed appointment with " + offer.getProviderName() + " for " + selectedSlot);
                    notifReq.put("offerId", offerId);
                    notifReq.put("status", "pending");
                    notifReq.put("timestamp", now);
                    rootUpdates.put("/notifications/" + notificationId, notifReq);
                }

                FirebaseHelper.getRootRef().updateChildren(rootUpdates)
                        .addOnCompleteListener(mirrorTask -> {
                            bookingInProgress = false;
                            if (!mirrorTask.isSuccessful()) {
                                Toast.makeText(getContext(),
                                        "Appointment confirmed. Admin status will refresh when database access is restored.",
                                        Toast.LENGTH_LONG).show();
                            } else {
                                Toast.makeText(getContext(), "Appointment confirmed successfully.", Toast.LENGTH_LONG).show();
                            }
                            dismiss();
                        });
            }
        });
    }

    private void renderAppointments(List<Appointment> appointments) {
        scheduledAppointmentsContainer.removeAllViews();
        if (appointments.isEmpty()) {
            showEmpty(scheduledAppointmentsContainer, scheduledEmptyText, "No confirmed appointments yet.");
            return;
        }
        detachEmpty(scheduledEmptyText);
        appointments.sort((a, b) -> Long.compare(b.getTimestamp(), a.getTimestamp()));
        for (Appointment appointment : appointments) {
            View card = LayoutInflater.from(getContext()).inflate(R.layout.item_appointment_card, scheduledAppointmentsContainer, false);
            TextView provider = card.findViewById(R.id.apptProvider);
            TextView department = card.findViewById(R.id.apptDepartment);
            TextView dateTime = card.findViewById(R.id.apptDateTime);
            TextView location = card.findViewById(R.id.apptLocation);
            TextView status = card.findViewById(R.id.apptStatus);
            if (provider != null) provider.setText(safe(appointment.getProviderName(), "Appointment"));
            if (department != null) department.setText(safe(appointment.getDepartment(), ""));
            if (dateTime != null) dateTime.setText("Date: " + safe(appointment.getDate(), "Not set") +
                    (appointment.getTime() == null || appointment.getTime().isEmpty() ? "" : " • " + appointment.getTime()));
            if (location != null) location.setText("Location: " + safe(appointment.getLocation(), "Not provided"));
            if (status != null) status.setText(safe(appointment.getStatus(), "Confirmed"));

            Button checkInButton = card.findViewById(R.id.btnCheckIn);
            Button map = card.findViewById(R.id.btnViewOnMap);
            Button directions = card.findViewById(R.id.btnGetDirections);
            
            // Listen to the offer to see if they've checked in yet (assuming we're rendering from an Appointment list, we have offerId)
            if (checkInButton != null && appointment.getId() != null) {
                FirebaseHelper.getAllOffersRef().child(appointment.getId()).child("patientCheckedIn")
                    .addListenerForSingleValueEvent(new ValueEventListener() {
                        @Override public void onDataChange(@NonNull DataSnapshot snapshot) {
                            if (Boolean.TRUE.equals(snapshot.getValue(Boolean.class))) {
                                checkInButton.setText("Checked In ✓");
                                checkInButton.setEnabled(false);
                                checkInButton.setBackgroundTintList(android.content.res.ColorStateList.valueOf(android.graphics.Color.parseColor("#065F46")));
                            } else {
                                checkInButton.setOnClickListener(v -> {
                                    // Mark checked in for admin viewing
                                    FirebaseHelper.getRootRef().child("appointment_offers").child(appointment.getId()).child("patientCheckedIn").setValue(true)
                                        .addOnCompleteListener(t -> {
                                            if (t.isSuccessful()) {
                                                checkInButton.setText("Checked In ✓");
                                                checkInButton.setEnabled(false);
                                                checkInButton.setBackgroundTintList(android.content.res.ColorStateList.valueOf(android.graphics.Color.parseColor("#065F46")));
                                                Toast.makeText(getContext(), "You are checked in! The doctor has been notified.", Toast.LENGTH_SHORT).show();
                                                
                                                // Send notification to admin/provider
                                                String notificationId = FirebaseHelper.getRootRef().child("notifications").push().getKey();
                                                if (notificationId != null) {
                                                    java.util.Map<String, Object> notifReq = new java.util.HashMap<>();
                                                    notifReq.put("recipientId", "admin"); // Notify provider/admin
                                                    notifReq.put("title", "Patient Checked In");
                                                    notifReq.put("body", "Patient has checked in for appointment at " + appointment.getLocation());
                                                    notifReq.put("status", "pending");
                                                    notifReq.put("timestamp", System.currentTimeMillis());
                                                    FirebaseHelper.getRootRef().child("notifications").child(notificationId).setValue(notifReq);
                                                }
                                            }
                                        });
                                });
                            }
                        }
                        @Override public void onCancelled(@NonNull DatabaseError error) {}
                    });
            }

            if (map != null) map.setOnClickListener(v -> {
                Intent intent = new Intent(getContext(), AppointmentLocationActivity.class);
                intent.putExtra("clinicName", safe(appointment.getProviderName(), "Appointment location"));
                intent.putExtra("clinicAddress", safe(appointment.getLocation(), ""));
                getContext().startActivity(intent);
            });
            if (directions != null) directions.setOnClickListener(v -> openDirections(appointment.getLocation()));
            scheduledAppointmentsContainer.addView(card);
        }
    }

    private void openDirections(String address) {
        if (address == null || address.trim().isEmpty()) {
            Toast.makeText(getContext(), "This appointment does not have a location yet.", Toast.LENGTH_SHORT).show();
            return;
        }
        Uri uri = Uri.parse("google.navigation:q=" + Uri.encode(address));
        Intent intent = new Intent(Intent.ACTION_VIEW, uri);
        intent.setPackage("com.google.android.apps.maps");
        try {
            getContext().startActivity(intent);
        } catch (Exception e) {
            Intent browser = new Intent(Intent.ACTION_VIEW,
                    Uri.parse("https://www.google.com/maps/dir/?api=1&destination=" + Uri.encode(address)));
            getContext().startActivity(browser);
        }
    }

    private void detachEmpty(TextView emptyView) {
        if (emptyView == null) return;
        if (emptyView.getParent() instanceof android.view.ViewGroup) {
            ((android.view.ViewGroup) emptyView.getParent()).removeView(emptyView);
        }
        emptyView.setVisibility(View.GONE);
    }

    private void showEmpty(LinearLayout container, TextView emptyView, String message) {
        if (emptyView != null) {
            emptyView.setText(message);
            emptyView.setVisibility(View.VISIBLE);
            if (emptyView.getParent() instanceof android.view.ViewGroup) {
                ((android.view.ViewGroup) emptyView.getParent()).removeView(emptyView);
            }
            container.addView(emptyView);
        }
    }

    private void showError(LinearLayout container, String message) {
        container.removeAllViews();
        TextView error = new TextView(getContext());
        error.setText(message);
        error.setPadding(24, 24, 24, 24);
        container.addView(error);
    }

    private String safe(String value, String fallback) {
        return value == null || value.trim().isEmpty() ? fallback : value;
    }

    @Override public void dismiss() {
        if (offersRef != null && offersListener != null) offersRef.removeEventListener(offersListener);
        if (appointmentsRef != null && appointmentsListener != null) appointmentsRef.removeEventListener(appointmentsListener);
        super.dismiss();
    }
}
