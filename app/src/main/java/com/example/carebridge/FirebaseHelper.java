package com.example.carebridge;

import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

public class FirebaseHelper {

    public static final String DATABASE_URL = "https://carebridge-4607d-default-rtdb.firebaseio.com";

    public static String sanitizeFirebaseKey(String key) {
        if (key == null || key.trim().isEmpty()) return "default_user";
        return key.trim().toLowerCase()
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

        // Ensure the doctor list exists for the admin screen, but don't seed fake patient data.
        seedDoctorsIfEmpty();
        
        // (Fake patient data generation has been removed)
    }
}
