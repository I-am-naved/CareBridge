package com.example.carebridge;

import android.Manifest;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.location.Location;
import android.os.Bundle;
import android.view.View;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.location.Priority;
import com.google.android.gms.tasks.CancellationTokenSource;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.libraries.places.api.Places;
import com.google.android.libraries.places.api.model.CircularBounds;
import com.google.android.libraries.places.api.model.Place;
import com.google.android.libraries.places.api.net.PlacesClient;
import com.google.android.libraries.places.api.net.SearchNearbyRequest;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;

public class NearbyServicesActivity extends AppCompatActivity {

    private static final int LOCATION_PERMISSION_REQUEST = 410;
    private static final double SEARCH_RADIUS_METERS = 5000;

    private RecyclerView rvNearbyPlaces;
    private NearbyServicesAdapter adapter;
    private final List<NearbyPlace> allPlaces = new ArrayList<>();
    private TextView chipAll, chipHospitals, chipClinics, chipPharmacies;
    private TextView locationMessage, emptyMessage;
    private ProgressBar loading;
    private FusedLocationProviderClient fusedLocationClient;
    private PlacesClient placesClient;
    private LatLng currentLatLng;
    private String activeCategory = "all";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        WindowCompat.setDecorFitsSystemWindows(getWindow(), false);
        getWindow().setStatusBarColor(Color.WHITE);
        WindowCompat.getInsetsController(getWindow(), getWindow().getDecorView())
                .setAppearanceLightStatusBars(true);
        setContentView(R.layout.activity_nearby_services);
        applySystemInsets();

        findViewById(R.id.btnBackNearby).setOnClickListener(v -> finish());
        chipAll = findViewById(R.id.chipAll);
        chipHospitals = findViewById(R.id.chipHospitals);
        chipClinics = findViewById(R.id.chipClinics);
        chipPharmacies = findViewById(R.id.chipPharmacies);
        locationMessage = findViewById(R.id.locationMessage);
        emptyMessage = findViewById(R.id.emptyMessage);
        loading = findViewById(R.id.nearbyLoading);
        rvNearbyPlaces = findViewById(R.id.rvNearbyPlaces);

        adapter = new NearbyServicesAdapter(this, new ArrayList<>());
        rvNearbyPlaces.setLayoutManager(new LinearLayoutManager(this));
        rvNearbyPlaces.setAdapter(adapter);

        chipAll.setOnClickListener(v -> filterCategory("all", chipAll));
        chipHospitals.setOnClickListener(v -> filterCategory("hospital", chipHospitals));
        chipClinics.setOnClickListener(v -> filterCategory("clinic", chipClinics));
        chipPharmacies.setOnClickListener(v -> filterCategory("pharmacy", chipPharmacies));

        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this);
        String apiKey = readMapsApiKey();
        if (apiKey == null || apiKey.trim().isEmpty()) {
            showEmpty("Google Maps API key is missing. Add your restricted API key to AndroidManifest.xml.");
            return;
        }
        if (!Places.isInitialized()) {
            Places.initializeWithNewPlacesApiEnabled(getApplicationContext(), apiKey);
        }
        placesClient = Places.createClient(this);
        checkLocationPermissionAndSearch();
    }

    private void applySystemInsets() {
        View root = findViewById(R.id.nearbyRoot);
        View toolbar = findViewById(R.id.nearbyToolbar);
        final int left = root.getPaddingLeft();
        final int right = root.getPaddingRight();
        final int bottom = root.getPaddingBottom();
        ViewCompat.setOnApplyWindowInsetsListener(root, (view, insets) -> {
            Insets bars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            toolbar.setPadding(toolbar.getPaddingLeft(), dp(12) + bars.top,
                    toolbar.getPaddingRight(), toolbar.getPaddingBottom());
            view.setPadding(left + bars.left, 0, right + bars.right, bottom + bars.bottom);
            return insets;
        });
    }

    private int dp(int value) {
        return Math.round(value * getResources().getDisplayMetrics().density);
    }

    private String readMapsApiKey() {
        try {
            ApplicationInfo info = getPackageManager()
                    .getApplicationInfo(getPackageName(), PackageManager.GET_META_DATA);
            return info.metaData.getString("com.google.android.geo.API_KEY");
        } catch (Exception e) {
            return null;
        }
    }

    private void checkLocationPermissionAndSearch() {
        boolean fine = ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
                == PackageManager.PERMISSION_GRANTED;
        boolean coarse = ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION)
                == PackageManager.PERMISSION_GRANTED;
        if (!fine && !coarse) {
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION},
                    LOCATION_PERMISSION_REQUEST);
            locationMessage.setText("Allow location access to find healthcare services near you.");
            return;
        }
        getCurrentLocation();
    }

    private void getCurrentLocation() {
        loading.setVisibility(View.VISIBLE);
        emptyMessage.setVisibility(View.GONE);
        locationMessage.setText("Finding your location…");
        CancellationTokenSource token = new CancellationTokenSource();
        try {
            fusedLocationClient.getCurrentLocation(Priority.PRIORITY_BALANCED_POWER_ACCURACY, token.getToken())
                    .addOnSuccessListener(location -> {
                        if (location == null) {
                            loading.setVisibility(View.GONE);
                            showEmpty("We couldn't get your location. Turn on device location and try again.");
                            return;
                        }
                        currentLatLng = new LatLng(location.getLatitude(), location.getLongitude());
                        locationMessage.setText("Showing healthcare services within 5 km of your location");
                        searchNearbyPlaces();
                    })
                    .addOnFailureListener(error -> {
                        loading.setVisibility(View.GONE);
                        showEmpty("Unable to get your location. Please try again.");
                    });
        } catch (SecurityException e) {
            loading.setVisibility(View.GONE);
            showEmpty("Location permission is required to find nearby services.");
        }
    }

    private void searchNearbyPlaces() {
        List<Place.Field> fields = Arrays.asList(
                Place.Field.ID, Place.Field.DISPLAY_NAME, Place.Field.FORMATTED_ADDRESS,
                Place.Field.LOCATION, Place.Field.TYPES);
        CircularBounds area = CircularBounds.newInstance(currentLatLng, SEARCH_RADIUS_METERS);
        SearchNearbyRequest request = SearchNearbyRequest.builder(area, fields)
                .setIncludedTypes(Arrays.asList("hospital", "medical_clinic", "pharmacy"))
                .setMaxResultCount(20)
                .setRankPreference(SearchNearbyRequest.RankPreference.DISTANCE)
                .build();

        placesClient.searchNearby(request)
                .addOnSuccessListener(response -> {
                    allPlaces.clear();
                    for (Place place : response.getPlaces()) {
                        if (place.getLocation() == null || place.getDisplayName() == null) continue;
                        String category = classify(place.getPlaceTypes());
                        if (category == null) continue;
                        float[] distance = new float[1];
                        Location.distanceBetween(currentLatLng.latitude, currentLatLng.longitude,
                                place.getLocation().latitude, place.getLocation().longitude, distance);
                        allPlaces.add(new NearbyPlace(
                                place.getId() == null ? "" : place.getId(),
                                place.getDisplayName(),
                                place.getFormattedAddress() == null ? "Address unavailable" : place.getFormattedAddress(),
                                category,
                                place.getLocation().latitude,
                                place.getLocation().longitude,
                                distance[0] / 1000.0));
                    }
                    Collections.sort(allPlaces, Comparator.comparingDouble(NearbyPlace::getDistanceKm));
                    loading.setVisibility(View.GONE);
                    filterCategory(activeCategory, selectedChip(activeCategory));
                    if (allPlaces.isEmpty()) showEmpty("No hospitals, clinics, or pharmacies were found within 5 km.");
                })
                .addOnFailureListener(error -> {
                    loading.setVisibility(View.GONE);
                    showEmpty("Nearby search couldn't load. Check your internet connection and make sure Places API (New) is enabled for your Google API key.");
                });
    }

    private String classify(List<String> types) {
        if (types == null) return null;
        for (String type : types) {
            String value = type.toLowerCase(Locale.US);
            if (value.equals("hospital")) return "hospital";
            if (value.equals("medical_clinic") || value.equals("doctor")) return "clinic";
            if (value.equals("pharmacy")) return "pharmacy";
        }
        return null;
    }

    private TextView selectedChip(String category) {
        if ("hospital".equals(category)) return chipHospitals;
        if ("clinic".equals(category)) return chipClinics;
        if ("pharmacy".equals(category)) return chipPharmacies;
        return chipAll;
    }

    private void filterCategory(String category, TextView selected) {
        activeCategory = category;
        resetChipStyles();
        if (selected != null) {
            selected.setBackgroundTintList(ColorStateList.valueOf(0xFF173B57));
            selected.setTextColor(0xFFFFFFFF);
        }
        List<NearbyPlace> filtered = new ArrayList<>();
        for (NearbyPlace place : allPlaces) {
            if ("all".equals(category) || category.equals(place.getCategory())) filtered.add(place);
        }
        adapter.updateList(filtered);
        emptyMessage.setVisibility(filtered.isEmpty() && loading.getVisibility() != View.VISIBLE ? View.VISIBLE : View.GONE);
        if (!filtered.isEmpty()) emptyMessage.setText("");
    }

    private void resetChipStyles() {
        TextView[] chips = {chipAll, chipHospitals, chipClinics, chipPharmacies};
        for (TextView chip : chips) {
            chip.setBackgroundTintList(ColorStateList.valueOf(0xFFFFFFFF));
            chip.setTextColor(0xFF173B57);
        }
    }

    private void showEmpty(String message) {
        emptyMessage.setText(message);
        emptyMessage.setVisibility(View.VISIBLE);
        rvNearbyPlaces.setVisibility(View.GONE);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == LOCATION_PERMISSION_REQUEST) {
            boolean granted = false;
            for (int result : grantResults) if (result == PackageManager.PERMISSION_GRANTED) granted = true;
            if (granted) getCurrentLocation();
            else showEmpty("Location permission was not granted. You can enable it in Settings to find nearby services.");
        }
    }
}
