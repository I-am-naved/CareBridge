package com.example.carebridge;

import android.app.Activity;
import android.content.res.ColorStateList;
import android.os.Bundle;
import android.widget.Button;
import android.widget.TextView;

import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;
import java.util.List;

public class NearbyServicesActivity extends Activity {

    private RecyclerView rvNearbyPlaces;
    private NearbyServicesAdapter adapter;
    private List<NearbyPlace> allPlacesList = new ArrayList<>();
    private Button chipAll, chipHospitals, chipClinics, chipPharmacies;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_nearby_services);

        TextView btnBack = findViewById(R.id.btnBackNearby);
        chipAll = findViewById(R.id.chipAll);
        chipHospitals = findViewById(R.id.chipHospitals);
        chipClinics = findViewById(R.id.chipClinics);
        chipPharmacies = findViewById(R.id.chipPharmacies);
        rvNearbyPlaces = findViewById(R.id.rvNearbyPlaces);

        if (btnBack != null) {
            btnBack.setOnClickListener(v -> finish());
        }

        rvNearbyPlaces.setLayoutManager(new LinearLayoutManager(this));
        
        loadPlacesData();

        adapter = new NearbyServicesAdapter(this, new ArrayList<>(allPlacesList));
        rvNearbyPlaces.setAdapter(adapter);

        chipAll.setOnClickListener(v -> filterCategory("all", chipAll));
        chipHospitals.setOnClickListener(v -> filterCategory("hospital", chipHospitals));
        chipClinics.setOnClickListener(v -> filterCategory("clinic", chipClinics));
        chipPharmacies.setOnClickListener(v -> filterCategory("pharmacy", chipPharmacies));
    }

    private void loadPlacesData() {
        allPlacesList.clear();
        allPlacesList.add(new NearbyPlace("1", "Royal Inland Hospital", "311 Columbia St, Kamloops, BC", "hospital", 50.6710, -120.3320, 1.8));
        allPlacesList.add(new NearbyPlace("2", "Kamloops Medical Clinic", "123 Columbia St, Kamloops, BC", "clinic", 50.6745, -120.3273, 2.1));
        allPlacesList.add(new NearbyPlace("3", "Shoppers Drug Mart Pharmacy", "755 Tranquille Rd, Kamloops, BC", "pharmacy", 50.6930, -120.3550, 2.5));
        allPlacesList.add(new NearbyPlace("4", "Overlander Extended Care Health Centre", "560 Tranquille Rd, Kamloops, BC", "hospital", 50.6880, -120.3500, 3.2));
        allPlacesList.add(new NearbyPlace("5", "Rexall Care Pharmacy", "1180 Columbia St W, Kamloops, BC", "pharmacy", 50.6690, -120.3610, 4.0));
        allPlacesList.add(new NearbyPlace("6", "North Shore Medical Clinic", "556 Tranquille Rd, Kamloops, BC", "clinic", 50.6875, -120.3495, 4.5));
    }

    private void filterCategory(String category, Button selectedButton) {
        // Reset button colors
        resetChipStyles();
        
        // Highlight active button
        selectedButton.setBackgroundTintList(ColorStateList.valueOf(0xFF173B57));
        selectedButton.setTextColor(0xFFFFFFFF);

        if ("all".equalsIgnoreCase(category)) {
            adapter.updateList(new ArrayList<>(allPlacesList));
            return;
        }

        List<NearbyPlace> filtered = new ArrayList<>();
        for (NearbyPlace place : allPlacesList) {
            if (category.equalsIgnoreCase(place.getCategory())) {
                filtered.add(place);
            }
        }
        adapter.updateList(filtered);
    }

    private void resetChipStyles() {
        int whiteColor = 0xFFFFFFFF;
        int darkBlueTextColor = 0xFF173B57;

        chipAll.setBackgroundTintList(ColorStateList.valueOf(whiteColor));
        chipAll.setTextColor(darkBlueTextColor);

        chipHospitals.setBackgroundTintList(ColorStateList.valueOf(whiteColor));
        chipHospitals.setTextColor(darkBlueTextColor);

        chipClinics.setBackgroundTintList(ColorStateList.valueOf(whiteColor));
        chipClinics.setTextColor(darkBlueTextColor);

        chipPharmacies.setBackgroundTintList(ColorStateList.valueOf(whiteColor));
        chipPharmacies.setTextColor(darkBlueTextColor);
    }
}
