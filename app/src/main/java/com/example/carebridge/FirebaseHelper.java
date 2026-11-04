package com.example.carebridge;

import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

import java.util.ArrayList;
import java.util.List;

public class FirebaseHelper {

    public static final String DATABASE_URL = "https://carebridge-4607d-default-rtdb.firebaseio.com";

    public static String sanitizeFirebaseKey(String key) {
        if (key == null || key.trim().isEmpty()) return "default_user";
        return key.trim()
                  .replace(".", "_")
                  .replace("#", "_")
                  .replace("$", "_")
                  .replace("[", "_")
                  .replace("]", "_");
    }

    public static DatabaseReference getRootRef() {
        return FirebaseDatabase.getInstance(DATABASE_URL).getReference();
    }

    public static DatabaseReference getUsersRef() {
        return getRootRef().child("users");
    }

    public static DatabaseReference getUserRef(String userId) {
        return getUsersRef().child(sanitizeFirebaseKey(userId));
    }

    public static DatabaseReference getAppointmentsRef(String userId) {
        return getUserRef(userId).child("appointments");
    }

    public static DatabaseReference getTasksRef(String userId) {
        return getUserRef(userId).child("tasks");
    }

    public static DatabaseReference getJourneyRef(String userId) {
        return getUserRef(userId).child("journey");
    }

    public static DatabaseReference getOffersRef(String userId) {
        return getUserRef(userId).child("appointment_offers");
    }

    public static DatabaseReference getAllOffersRef() {
        return getRootRef().child("appointment_offers");
    }

    public static DatabaseReference getDoctorsRef() {
        return getRootRef().child("doctors");
    }

    public static void seedDoctorsIfEmpty() {
        DatabaseReference doctorsRef = getDoctorsRef();
        doctorsRef.get().addOnCompleteListener(task -> {
            if (task.isSuccessful() && (task.getResult() == null || !task.getResult().exists())) {
                Doctor d1 = new Doctor("doc_1", "Dr. Sarah Jenkins", "Cardiology", "Medical Center, Suite 304", true, "Available Mon - Thu (09:00 AM - 04:00 PM)");
                Doctor d2 = new Doctor("doc_2", "Dr. Michael Chen", "General Medicine / Wellness", "Building B, Suite 102", true, "Available Mon - Fri (08:30 AM - 05:00 PM)");
                Doctor d3 = new Doctor("doc_3", "Dr. Emily Vance", "Neurology Specialist", "East Wing, Room 210", true, "Available Tue & Fri (10:00 AM - 03:00 PM)");
                Doctor d4 = new Doctor("doc_4", "Dr. Robert Taylor", "Orthopedics", "Surgical Pavilion, Suite 105", false, "Currently on leave");

                doctorsRef.child(d1.getId()).setValue(d1);
                doctorsRef.child(d2.getId()).setValue(d2);
                doctorsRef.child(d3.getId()).setValue(d3);
                doctorsRef.child(d4.getId()).setValue(d4);
            }
        });
    }

    public static void seedDefaultDataIfEmpty(String userId) {
        if (userId == null || userId.isEmpty()) return;

        seedDoctorsIfEmpty();

        String safeUserId = sanitizeFirebaseKey(userId);
        DatabaseReference userRef = getUserRef(safeUserId);

        userRef.child("seeded").get().addOnCompleteListener(task -> {
            if (task.isSuccessful() && task.getResult() != null && task.getResult().exists()) {
                return;
            }

            userRef.child("seeded").setValue(true);

            // 1. Initial Appointment
            DatabaseReference apptRef = getAppointmentsRef(safeUserId).push();
            String apptId = apptRef.getKey();
            Appointment appt = new Appointment(
                    apptId,
                    safeUserId,
                    "Dr. Sarah Jenkins",
                    "Cardiology Follow-Up",
                    "Medical Center, Room 304",
                    "Nov 15, 2026",
                    "10:30 AM",
                    "Confirmed",
                    "Bring recent blood pressure logs and medication list.",
                    System.currentTimeMillis() + 864000000L
            );
            apptRef.setValue(appt);

            // 2. Initial Care Tasks
            DatabaseReference taskRef1 = getTasksRef(safeUserId).push();
            CareTask task1 = new CareTask(
                    taskRef1.getKey(),
                    safeUserId,
                    "Blood pressure log",
                    "Record morning and evening readings in health log",
                    "Daily",
                    false,
                    System.currentTimeMillis()
            );
            taskRef1.setValue(task1);

            DatabaseReference taskRef2 = getTasksRef(safeUserId).push();
            CareTask task2 = new CareTask(
                    taskRef2.getKey(),
                    safeUserId,
                    "Pick up prescription refills",
                    "Collect Lisinopril refill from pharmacy",
                    "Nov 10, 2026",
                    false,
                    System.currentTimeMillis() + 1000L
            );
            taskRef2.setValue(task2);

            // 3. Initial Journey Items
            DatabaseReference jRef1 = getJourneyRef(safeUserId).push();
            JourneyItem j1 = new JourneyItem(
                    jRef1.getKey(),
                    safeUserId,
                    "01",
                    "Doctor Visit",
                    "Initial Consultation",
                    "Met with primary doctor. Reviewed medical history and ordered lab tests.",
                    "Oct 20, 2026",
                    System.currentTimeMillis() - 800000000L
            );
            jRef1.setValue(j1);

            DatabaseReference jRef2 = getJourneyRef(safeUserId).push();
            JourneyItem j2 = new JourneyItem(
                    jRef2.getKey(),
                    safeUserId,
                    "02",
                    "Referral",
                    "Cardiology Specialist Referral",
                    "Referred to Cardiology for comprehensive follow-up assessment.",
                    "Oct 28, 2026",
                    System.currentTimeMillis() - 400000000L
            );
            jRef2.setValue(j2);

            DatabaseReference jRef3 = getJourneyRef(safeUserId).push();
            JourneyItem j3 = new JourneyItem(
                    jRef3.getKey(),
                    safeUserId,
                    "03",
                    "Follow-up",
                    "Upcoming Cardiology Follow-up",
                    "Scheduled appointment with Dr. Sarah Jenkins.",
                    "Nov 15, 2026",
                    System.currentTimeMillis() + 864000000L
            );
            jRef3.setValue(j3);

            // 4. Sample Appointment Offer for Demonstration Flow
            DatabaseReference offerRef = getOffersRef(safeUserId).push();
            List<String> slots = new ArrayList<>();
            slots.add("Nov 18, 2026 at 09:00 AM");
            slots.add("Nov 18, 2026 at 02:00 PM");
            slots.add("Nov 19, 2026 at 11:30 AM");

            AppointmentOffer offer = new AppointmentOffer(
                    offerRef.getKey(),
                    safeUserId,
                    "Current User",
                    "doc_2",
                    "Dr. Michael Chen",
                    "Routine Wellness Exam",
                    "Building B, Suite 102",
                    "PENDING",
                    slots,
                    System.currentTimeMillis()
            );
            offerRef.setValue(offer);

            // Also mirror under root /appointment_offers so Admin can see it
            if (offerRef.getKey() != null) {
                getAllOffersRef().child(offerRef.getKey()).setValue(offer);
            }
        });
    }
}
