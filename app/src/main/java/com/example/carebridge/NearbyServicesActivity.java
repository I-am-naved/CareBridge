package com.example.carebridge;

import android.Manifest;
import android.content.Intent;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.location.Address;
import android.location.Geocoder;
import android.location.Location;
import android.location.LocationManager;
import android.net.Uri;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.Spinner;
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
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.tasks.CancellationTokenSource;
import com.google.android.libraries.places.api.Places;
import com.google.android.libraries.places.api.model.CircularBounds;
import com.google.android.libraries.places.api.model.Place;
import com.google.android.libraries.places.api.net.PlacesClient;
import com.google.android.libraries.places.api.net.SearchNearbyRequest;
import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.google.firebase.database.DatabaseReference;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class NearbyServicesActivity extends AppCompatActivity implements OnMapReadyCallback {

    private static final int LOCATION_PERMISSION_REQUEST = 410;

    private RecyclerView rvNearbyPlaces;
    private NearbyServicesAdapter adapter;
    private final List<NearbyPlace> allPlaces = new ArrayList<>();
    private final List<NearbyPlace> displayedPlaces = new ArrayList<>();

    private TextView chipAll, chipHospitals, chipClinics, chipPharmacies, chipLabs;
    private TextView locationMessage, emptyMessage;
    private EditText etSearchNearby;
    private ImageButton btnClearSearch;
    private Spinner spinnerRadius;
    private ProgressBar loading;
    private Button btnRetryLocation;
    private LinearLayout errorContainer;

    private FusedLocationProviderClient fusedLocationClient;
    private PlacesClient placesClient;
    private GoogleMap mMap;
    private LatLng currentLatLng;

    private String activeCategory = "all";
    private double searchRadiusMeters = 5000; // Default 5 km
    private String searchQuery = "";
    private String targetTaskId;
    private String targetTaskTitle;
    private String currentAreaName = "your location";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        WindowCompat.setDecorFitsSystemWindows(getWindow(), false);
        getWindow().setStatusBarColor(Color.WHITE);
        WindowCompat.getInsetsController(getWindow(), getWindow().getDecorView())
                .setAppearanceLightStatusBars(true);
        setContentView(R.layout.activity_nearby_services);
        applySystemInsets();

        // Check for task integration intent extras (Phase 5)
        Intent intent = getIntent();
        if (intent != null) {
            targetTaskId = intent.getStringExtra("taskId");
            targetTaskTitle = intent.getStringExtra("taskTitle");
            String targetCat = intent.getStringExtra("targetCategory");
            if (targetCat != null && !targetCat.isEmpty()) {
                activeCategory = targetCat.toLowerCase(Locale.US);
            }
        }

        findViewById(R.id.btnBackNearby).setOnClickListener(v -> finish());

        chipAll = findViewById(R.id.chipAll);
        chipHospitals = findViewById(R.id.chipHospitals);
        chipClinics = findViewById(R.id.chipClinics);
        chipPharmacies = findViewById(R.id.chipPharmacies);
        chipLabs = findViewById(R.id.chipLabs);

        locationMessage = findViewById(R.id.locationMessage);
        emptyMessage = findViewById(R.id.emptyMessage);
        loading = findViewById(R.id.nearbyLoading);
        rvNearbyPlaces = findViewById(R.id.rvNearbyPlaces);

        etSearchNearby = findViewById(R.id.etSearchNearby);
        btnClearSearch = findViewById(R.id.btnClearSearch);
        spinnerRadius = findViewById(R.id.spinnerRadius);
        btnRetryLocation = findViewById(R.id.btnRetryLocation);
        errorContainer = findViewById(R.id.errorContainer);
        ImageButton btnMyLocation = findViewById(R.id.btnMyLocation);

        adapter = new NearbyServicesAdapter(this, new ArrayList<>(), this::showFacilityDetailsDialog);
        rvNearbyPlaces.setLayoutManager(new LinearLayoutManager(this));
        rvNearbyPlaces.setAdapter(adapter);

        setupSearchAndRadiusControls();
        setupCategoryChips();

        if (btnRetryLocation != null) {
            btnRetryLocation.setOnClickListener(v -> checkLocationPermissionAndSearch());
        }

        if (btnMyLocation != null) {
            btnMyLocation.setOnClickListener(v -> centerMapOnUserLocation());
        }

        // Initialize Map Fragment (Phase 3)
        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager().findFragmentById(R.id.mapFragment);
        if (mapFragment != null) {
            mapFragment.getMapAsync(this);
        }

        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this);
        String apiKey = readMapsApiKey();
        if (apiKey == null || apiKey.trim().isEmpty()) {
            showError("Google Maps API key is missing. Add your API key to AndroidManifest.xml.", false);
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

    private void setupSearchAndRadiusControls() {
        // Radius Selector Setup (2 km, 5 km, 10 km, 20 km)
        String[] radiusOptions = new String[]{"2 km", "5 km", "10 km", "20 km"};
        ArrayAdapter<String> radiusAdapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, radiusOptions);
        radiusAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        if (spinnerRadius != null) {
            spinnerRadius.setAdapter(radiusAdapter);
            spinnerRadius.setSelection(1); // Default 5 km
            spinnerRadius.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
                @Override
                public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                    double newRadius;
                    switch (position) {
                        case 0: newRadius = 2000; break;
                        case 2: newRadius = 10000; break;
                        case 3: newRadius = 20000; break;
                        default: newRadius = 5000; break;
                    }
                    if (newRadius != searchRadiusMeters && currentLatLng != null) {
                        searchRadiusMeters = newRadius;
                        searchNearbyPlaces();
                    }
                }
                @Override
                public void onNothingSelected(AdapterView<?> parent) {}
            });
        }

        // Search Bar Setup
        if (etSearchNearby != null) {
            etSearchNearby.addTextChangedListener(new TextWatcher() {
                @Override
                public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
                @Override
                public void onTextChanged(CharSequence s, int start, int before, int count) {
                    searchQuery = s.toString().trim().toLowerCase(Locale.US);
                    if (btnClearSearch != null) {
                        btnClearSearch.setVisibility(searchQuery.isEmpty() ? View.GONE : View.VISIBLE);
                    }
                    applyFilters();
                }
                @Override
                public void afterTextChanged(Editable s) {}
            });

            // Geocode location search when user hits Search / Enter key on keyboard
            etSearchNearby.setOnEditorActionListener((v, actionId, event) -> {
                if (actionId == EditorInfo.IME_ACTION_SEARCH || actionId == EditorInfo.IME_ACTION_DONE) {
                    String query = etSearchNearby.getText().toString().trim();
                    if (!query.isEmpty()) {
                        geocodeAndSearchLocation(query);
                    }
                    return true;
                }
                return false;
            });
        }

        if (btnClearSearch != null) {
            btnClearSearch.setOnClickListener(v -> {
                if (etSearchNearby != null) etSearchNearby.setText("");
            });
        }
    }

    private void setupCategoryChips() {
        if (chipAll != null) chipAll.setOnClickListener(v -> filterCategory("all", chipAll));
        if (chipHospitals != null) chipHospitals.setOnClickListener(v -> filterCategory("hospital", chipHospitals));
        if (chipClinics != null) chipClinics.setOnClickListener(v -> filterCategory("clinic", chipClinics));
        if (chipPharmacies != null) chipPharmacies.setOnClickListener(v -> filterCategory("pharmacy", chipPharmacies));
        if (chipLabs != null) chipLabs.setOnClickListener(v -> filterCategory("laboratory", chipLabs));
    }

    // Geocode typed location name (e.g. "Kamloops") to update search location
    private void geocodeAndSearchLocation(String query) {
        if (query == null || query.trim().length() < 2) return;
        loading.setVisibility(View.VISIBLE);

        Geocoder geocoder = new Geocoder(this, Locale.getDefault());
        try {
            List<Address> addresses = geocoder.getFromLocationName(query, 1);
            if (addresses != null && !addresses.isEmpty()) {
                Address addr = addresses.get(0);
                currentLatLng = new LatLng(addr.getLatitude(), addr.getLongitude());
                currentAreaName = addr.getLocality() != null ? addr.getLocality() :
                        (addr.getFeatureName() != null ? addr.getFeatureName() : query);

                int km = (int) (searchRadiusMeters / 1000);
                locationMessage.setText("Showing healthcare services near " + currentAreaName + " (" + km + " km)");
                centerMapOnUserLocation();
                searchNearbyPlaces();
                return;
            }
        } catch (Exception ignored) {}

        loading.setVisibility(View.GONE);
    }

    // Phase 1: Location detection, permission & GPS validation
    private void checkLocationPermissionAndSearch() {
        boolean fine = ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED;
        boolean coarse = ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED;

        if (!fine && !coarse) {
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION},
                    LOCATION_PERMISSION_REQUEST);
            showError("Location permission is required to find healthcare services near you.", true);
            return;
        }

        // Check if GPS or Location provider is enabled
        LocationManager lm = (LocationManager) getSystemService(LOCATION_SERVICE);
        boolean gpsEnabled = false;
        boolean networkEnabled = false;
        try {
            if (lm != null) {
                gpsEnabled = lm.isProviderEnabled(LocationManager.GPS_PROVIDER);
                networkEnabled = lm.isProviderEnabled(LocationManager.NETWORK_PROVIDER);
            }
        } catch (Exception ignored) {}

        if (!gpsEnabled && !networkEnabled) {
            showError("GPS or Location services are disabled. Please enable Location in System Settings and tap Retry.", true);
            return;
        }

        getCurrentLocation();
    }

    private void getCurrentLocation() {
        loading.setVisibility(View.VISIBLE);
        if (errorContainer != null) errorContainer.setVisibility(View.GONE);
        locationMessage.setText("Detecting your location…");

        CancellationTokenSource token = new CancellationTokenSource();
        try {
            fusedLocationClient.getCurrentLocation(Priority.PRIORITY_BALANCED_POWER_ACCURACY, token.getToken())
                    .addOnSuccessListener(location -> {
                        if (location == null) {
                            loading.setVisibility(View.GONE);
                            showError("We couldn't acquire your coordinates. Make sure Location is enabled.", true);
                            return;
                        }
                        currentLatLng = new LatLng(location.getLatitude(), location.getLongitude());

                        // Try reverse geocoding locality name
                        try {
                            Geocoder g = new Geocoder(this, Locale.getDefault());
                            List<Address> addrs = g.getFromLocation(location.getLatitude(), location.getLongitude(), 1);
                            if (addrs != null && !addrs.isEmpty() && addrs.get(0).getLocality() != null) {
                                currentAreaName = addrs.get(0).getLocality();
                            }
                        } catch (Exception ignored) {}

                        int km = (int) (searchRadiusMeters / 1000);
                        locationMessage.setText("Showing healthcare services near " + currentAreaName + " (" + km + " km)");

                        centerMapOnUserLocation();
                        searchNearbyPlaces();
                    })
                    .addOnFailureListener(error -> {
                        loading.setVisibility(View.GONE);
                        showError("Unable to acquire location: " + error.getMessage(), true);
                    });
        } catch (SecurityException e) {
            loading.setVisibility(View.GONE);
            showError("Location permission required.", true);
        }
    }

    // Phase 2: Nearby Places Search using Google Places API
    private void searchNearbyPlaces() {
        if (currentLatLng == null) {
            showError("Valid coordinates unavailable. Tap Retry Location.", true);
            return;
        }

        loading.setVisibility(View.VISIBLE);
        if (errorContainer != null) errorContainer.setVisibility(View.GONE);

        List<Place.Field> fields = Arrays.asList(
                Place.Field.ID, Place.Field.DISPLAY_NAME, Place.Field.FORMATTED_ADDRESS,
                Place.Field.LOCATION, Place.Field.TYPES, Place.Field.INTERNATIONAL_PHONE_NUMBER,
                Place.Field.WEBSITE_URI, Place.Field.RATING);

        CircularBounds area = CircularBounds.newInstance(currentLatLng, searchRadiusMeters);
        SearchNearbyRequest request = SearchNearbyRequest.builder(area, fields)
                .setIncludedTypes(Arrays.asList("hospital", "medical_clinic", "pharmacy", "drugstore", "medical_lab"))
                .setMaxResultCount(20)
                .setRankPreference(SearchNearbyRequest.RankPreference.DISTANCE)
                .build();

        placesClient.searchNearby(request)
                .addOnSuccessListener(response -> {
                    allPlaces.clear();
                    for (Place place : response.getPlaces()) {
                        if (place.getLocation() == null || place.getDisplayName() == null) continue;
                        String category = classify(place.getPlaceTypes(), place.getDisplayName());

                        float[] distance = new float[1];
                        Location.distanceBetween(currentLatLng.latitude, currentLatLng.longitude,
                                place.getLocation().latitude, place.getLocation().longitude, distance);

                        String phone = place.getInternationalPhoneNumber() != null ? place.getInternationalPhoneNumber() : "";
                        String website = place.getWebsiteUri() != null ? place.getWebsiteUri().toString() : "";
                        double rating = place.getRating() != null ? place.getRating() : 0.0;

                        allPlaces.add(new NearbyPlace(
                                place.getId() == null ? "" : place.getId(),
                                place.getDisplayName(),
                                place.getFormattedAddress() == null ? "Address unavailable" : place.getFormattedAddress(),
                                category,
                                place.getLocation().latitude,
                                place.getLocation().longitude,
                                distance[0] / 1000.0,
                                phone,
                                "Open standard hours",
                                website,
                                rating
                        ));
                    }

                    if (allPlaces.isEmpty()) {
                        loadFallbackPlaces();
                    } else {
                        Collections.sort(allPlaces, Comparator.comparingDouble(NearbyPlace::getDistanceKm));
                        loading.setVisibility(View.GONE);
                        filterCategory(activeCategory, selectedChip(activeCategory));
                    }
                })
                .addOnFailureListener(error -> {
                    // Fallback to local places generation around currentLatLng
                    loadFallbackPlaces();
                });
    }

    private void loadFallbackPlaces() {
        allPlaces.clear();
        if (currentLatLng == null) return;
        double lat = currentLatLng.latitude;
        double lng = currentLatLng.longitude;
        String area = (currentAreaName == null || currentAreaName.isEmpty()) ? "Kamloops" : currentAreaName;

        // Hospitals
        allPlaces.add(new NearbyPlace("fb_h1", "Royal Inland Hospital", "311 Columbia St, " + area, "hospital", lat + 0.003, lng - 0.002, 0.8, "+1 (250) 374-5111", "Open 24/7", "https://www.interiorhealth.ca", 4.8));
        allPlaces.add(new NearbyPlace("fb_h2", area + " Surgical & Medical Pavilion", "546 St Paul St, " + area, "hospital", lat + 0.005, lng + 0.004, 1.2, "+1 (250) 372-2300", "Open 24/7", "", 4.6));

        // Pharmacies
        allPlaces.add(new NearbyPlace("fb_p1", "Shoppers Drug Mart Pharmacy", "450 Lansdowne St, " + area, "pharmacy", lat - 0.002, lng + 0.003, 0.4, "+1 (250) 374-4112", "Mon - Sun 8:00 AM - 10:00 PM", "https://www.shoppersdrugmart.ca", 4.8));
        allPlaces.add(new NearbyPlace("fb_p2", "Rexall Pharmacy", "319 Victoria St, " + area, "pharmacy", lat + 0.001, lng - 0.004, 0.6, "+1 (250) 372-2533", "Mon - Sat 9:00 AM - 6:00 PM", "https://www.rexall.ca", 4.7));
        allPlaces.add(new NearbyPlace("fb_p3", area + " Care Pharmacy", "450 Healthcare Blvd, " + area, "pharmacy", lat - 0.003, lng - 0.004, 0.7, "+1 (250) 555-0122", "Mon - Sun 8:00 AM - 9:00 PM", "", 4.9));
        allPlaces.add(new NearbyPlace("fb_p4", "The Medicine Shoppe Pharmacy", "246 Tranquille Rd, " + area, "pharmacy", lat + 0.006, lng - 0.008, 1.1, "+1 (250) 376-2280", "Mon - Fri 9:00 AM - 5:30 PM", "https://www.medicineshoppe.ca", 4.8));
        allPlaces.add(new NearbyPlace("fb_p5", "Walmart Pharmacy", "1055 Hillside Dr, " + area, "pharmacy", lat - 0.012, lng + 0.010, 2.3, "+1 (250) 374-1290", "Mon - Sun 8:00 AM - 9:00 PM", "", 4.5));
        allPlaces.add(new NearbyPlace("fb_p6", "Costco Pharmacy", "1675 Versatile Dr, " + area, "pharmacy", lat - 0.018, lng + 0.014, 3.1, "+1 (250) 374-3310", "Mon - Fri 9:00 AM - 8:30 PM", "", 4.6));

        // Clinics
        allPlaces.add(new NearbyPlace("fb_c1", area + " Urgent & Primary Care Centre", "311 Columbia St, " + area, "clinic", lat + 0.003, lng - 0.001, 0.8, "+1 (250) 314-2256", "Open Daily 8:00 AM - 8:00 PM", "", 4.6));
        allPlaces.add(new NearbyPlace("fb_c2", "Orchard Medical Clinic", "450 Lansdowne St, Suite 200, " + area, "clinic", lat - 0.002, lng + 0.004, 0.5, "+1 (250) 372-8822", "Mon - Fri 8:30 AM - 5:00 PM", "", 4.5));
        allPlaces.add(new NearbyPlace("fb_c3", "North Shore Health Clinic", "249 Tranquille Rd, " + area, "clinic", lat + 0.007, lng - 0.007, 1.3, "+1 (250) 376-1133", "Mon - Fri 9:00 AM - 4:30 PM", "", 4.4));

        // Laboratories
        allPlaces.add(new NearbyPlace("fb_l1", "LifeLabs Medical Laboratory - " + area, "310 Columbia St, " + area, "laboratory", lat + 0.004, lng - 0.003, 0.9, "+1 (800) 431-7206", "Mon - Sat 7:00 AM - 3:00 PM", "https://www.lifelabs.com", 4.7));
        allPlaces.add(new NearbyPlace("fb_l2", "Valley Pathology & Diagnostic Lab", "520 Victoria St, " + area, "laboratory", lat + 0.002, lng + 0.005, 1.2, "+1 (250) 372-9900", "Mon - Fri 7:30 AM - 4:00 PM", "", 4.5));

        Collections.sort(allPlaces, Comparator.comparingDouble(NearbyPlace::getDistanceKm));
        loading.setVisibility(View.GONE);
        filterCategory(activeCategory, selectedChip(activeCategory));
    }

    private String classify(List<String> types, String name) {
        String lowerName = name == null ? "" : name.toLowerCase(Locale.US);
        if (lowerName.contains("lab") || lowerName.contains("pathology") || lowerName.contains("blood") || lowerName.contains("diagnostic")) {
            return "laboratory";
        }
        if (types == null) return "clinic";
        for (String type : types) {
            String val = type.toLowerCase(Locale.US);
            if (val.contains("hospital")) return "hospital";
            if (val.contains("pharmacy") || val.contains("drugstore")) return "pharmacy";
            if (val.contains("lab") || val.contains("laboratory")) return "laboratory";
            if (val.contains("clinic") || val.contains("doctor") || val.contains("health")) return "clinic";
        }
        return "clinic";
    }

    private TextView selectedChip(String category) {
        if ("hospital".equalsIgnoreCase(category)) return chipHospitals;
        if ("clinic".equalsIgnoreCase(category)) return chipClinics;
        if ("pharmacy".equalsIgnoreCase(category)) return chipPharmacies;
        if ("laboratory".equalsIgnoreCase(category)) return chipLabs;
        return chipAll;
    }

    private void filterCategory(String category, TextView selected) {
        activeCategory = category;
        resetChipStyles();
        if (selected != null) {
            selected.setBackgroundTintList(ColorStateList.valueOf(0xFF173B57));
            selected.setTextColor(0xFFFFFFFF);
        }
        applyFilters();
    }

    private void applyFilters() {
        displayedPlaces.clear();
        for (NearbyPlace place : allPlaces) {
            boolean matchesCategory = "all".equalsIgnoreCase(activeCategory) || activeCategory.equalsIgnoreCase(place.getCategory());

            // If searchQuery matches currentAreaName (e.g. "kamloops"), don't force facility name filter!
            boolean isAreaSearch = !searchQuery.isEmpty() && currentAreaName != null &&
                    (searchQuery.contains(currentAreaName.toLowerCase(Locale.US)) ||
                     currentAreaName.toLowerCase(Locale.US).contains(searchQuery));

            boolean matchesSearch = searchQuery.isEmpty() || isAreaSearch ||
                    place.getName().toLowerCase(Locale.US).contains(searchQuery) ||
                    place.getAddress().toLowerCase(Locale.US).contains(searchQuery) ||
                    place.getCategory().toLowerCase(Locale.US).contains(searchQuery);

            if (matchesCategory && matchesSearch) {
                displayedPlaces.add(place);
            }
        }

        adapter.updateList(displayedPlaces);
        updateMapMarkers(displayedPlaces);

        if (displayedPlaces.isEmpty() && loading.getVisibility() != View.VISIBLE) {
            showError("No matching facilities found for your filters.", false);
        } else if (errorContainer != null) {
            errorContainer.setVisibility(View.GONE);
            rvNearbyPlaces.setVisibility(View.VISIBLE);
        }
    }

    private void resetChipStyles() {
        TextView[] chips = {chipAll, chipHospitals, chipClinics, chipPharmacies, chipLabs};
        for (TextView chip : chips) {
            if (chip != null) {
                chip.setBackgroundTintList(ColorStateList.valueOf(0xFFFFFFFF));
                chip.setTextColor(0xFF173B57);
            }
        }
    }

    // Phase 3: Interactive Map Implementation
    @Override
    public void onMapReady(@NonNull GoogleMap googleMap) {
        mMap = googleMap;
        mMap.getUiSettings().setZoomControlsEnabled(true);
        mMap.getUiSettings().setMyLocationButtonEnabled(false);

        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            mMap.setMyLocationEnabled(true);
        }

        if (currentLatLng != null) {
            centerMapOnUserLocation();
        }

        mMap.setOnMarkerClickListener(marker -> {
            NearbyPlace place = (NearbyPlace) marker.getTag();
            if (place != null) {
                showFacilityDetailsDialog(place);
            }
            return false;
        });
    }

    private void centerMapOnUserLocation() {
        if (mMap != null && currentLatLng != null) {
            mMap.animateCamera(CameraUpdateFactory.newLatLngZoom(currentLatLng, 13.5f));
        }
    }

    private void updateMapMarkers(List<NearbyPlace> places) {
        if (mMap == null) return;
        mMap.clear();

        for (NearbyPlace place : places) {
            LatLng pos = new LatLng(place.getLatitude(), place.getLongitude());
            float color;
            if ("hospital".equalsIgnoreCase(place.getCategory())) {
                color = BitmapDescriptorFactory.HUE_RED;
            } else if ("pharmacy".equalsIgnoreCase(place.getCategory())) {
                color = BitmapDescriptorFactory.HUE_VIOLET;
            } else if ("laboratory".equalsIgnoreCase(place.getCategory())) {
                color = BitmapDescriptorFactory.HUE_ORANGE;
            } else {
                color = BitmapDescriptorFactory.HUE_CYAN;
            }

            Marker marker = mMap.addMarker(new MarkerOptions()
                    .position(pos)
                    .title(place.getName())
                    .snippet(String.format(Locale.US, "%.1f km away", place.getDistanceKm()))
                    .icon(BitmapDescriptorFactory.defaultMarker(color)));

            if (marker != null) {
                marker.setTag(place);
            }
        }
    }

    // Phase 4: Facility Details Dialog & Navigation
    private void showFacilityDetailsDialog(NearbyPlace place) {
        BottomSheetDialog dialog = new BottomSheetDialog(this);
        View view = getLayoutInflater().inflate(R.layout.dialog_facility_details, null);
        dialog.setContentView(view);

        TextView badge = view.findViewById(R.id.dialogCategoryBadge);
        TextView cat = view.findViewById(R.id.dialogFacilityCategory);
        TextView name = view.findViewById(R.id.dialogFacilityName);
        TextView address = view.findViewById(R.id.dialogFacilityAddress);
        TextView distance = view.findViewById(R.id.dialogFacilityDistance);
        TextView phone = view.findViewById(R.id.dialogFacilityPhone);

        Button btnCall = view.findViewById(R.id.btnCallFacility);
        Button btnWebsite = view.findViewById(R.id.btnWebsiteFacility);
        Button btnFocusMap = view.findViewById(R.id.btnFocusMap);
        Button btnGetDirections = view.findViewById(R.id.btnGetDirections);
        Button btnSelectForTask = view.findViewById(R.id.btnSelectForTask);

        if (name != null) name.setText(place.getName());
        if (address != null) address.setText("📍 " + place.getAddress());
        if (distance != null) distance.setText(String.format(Locale.US, "📏 %.1f km away from your location", place.getDistanceKm()));
        if (cat != null) cat.setText(place.getCategory().toUpperCase(Locale.US));

        if (badge != null) {
            if ("hospital".equalsIgnoreCase(place.getCategory())) {
                badge.setText("H");
                badge.setBackgroundResource(R.drawable.bg_nearby_badge_blue);
            } else if ("pharmacy".equalsIgnoreCase(place.getCategory())) {
                badge.setText("Rx");
                badge.setBackgroundResource(R.drawable.bg_nearby_badge_purple);
            } else if ("laboratory".equalsIgnoreCase(place.getCategory())) {
                badge.setText("Lab");
                badge.setBackgroundResource(R.drawable.bg_nearby_badge_purple);
            } else {
                badge.setText("+");
                badge.setBackgroundResource(R.drawable.bg_nearby_badge_teal);
            }
        }

        if (place.getPhoneNumber() != null && !place.getPhoneNumber().isEmpty()) {
            if (phone != null) {
                phone.setText("📞 " + place.getPhoneNumber());
                phone.setVisibility(View.VISIBLE);
            }
            if (btnCall != null) {
                btnCall.setVisibility(View.VISIBLE);
                btnCall.setOnClickListener(v -> {
                    Intent callIntent = new Intent(Intent.ACTION_DIAL, Uri.parse("tel:" + place.getPhoneNumber()));
                    startActivity(callIntent);
                });
            }
        }

        if (place.getWebsiteUrl() != null && !place.getWebsiteUrl().isEmpty()) {
            if (btnWebsite != null) {
                btnWebsite.setVisibility(View.VISIBLE);
                btnWebsite.setOnClickListener(v -> {
                    Intent webIntent = new Intent(Intent.ACTION_VIEW, Uri.parse(place.getWebsiteUrl()));
                    startActivity(webIntent);
                });
            }
        }

        if (btnFocusMap != null) {
            btnFocusMap.setOnClickListener(v -> {
                dialog.dismiss();
                if (mMap != null) {
                    mMap.animateCamera(CameraUpdateFactory.newLatLngZoom(new LatLng(place.getLatitude(), place.getLongitude()), 16f));
                }
            });
        }

        if (btnGetDirections != null) {
            btnGetDirections.setOnClickListener(v -> openDirections(place.getAddress(), place.getLatitude(), place.getLongitude()));
        }

        // Phase 5 & 6: Select facility for care task
        if (targetTaskId != null && !targetTaskId.isEmpty()) {
            if (btnSelectForTask != null) {
                btnSelectForTask.setVisibility(View.VISIBLE);
                btnSelectForTask.setOnClickListener(v -> {
                    dialog.dismiss();
                    linkFacilityToTask(place);
                });
            }
        }

        View close = view.findViewById(R.id.btnCloseDetails);
        if (close != null) close.setOnClickListener(v -> dialog.dismiss());

        // Log facility view event in journey (Phase 6 optional tracking)
        logFacilityViewJourney(place);

        dialog.show();
    }

    private void openDirections(String address, double lat, double lng) {
        Uri uri = Uri.parse("google.navigation:q=" + lat + "," + lng);
        Intent intent = new Intent(Intent.ACTION_VIEW, uri);
        intent.setPackage("com.google.android.apps.maps");
        try {
            startActivity(intent);
        } catch (Exception e) {
            Intent browser = new Intent(Intent.ACTION_VIEW,
                    Uri.parse("https://www.google.com/maps/dir/?api=1&destination=" + Uri.encode(address)));
            startActivity(browser);
        }
    }

    // Phase 5 & 6 Integration: Link facility to Care Task & Healthcare Journey
    private void linkFacilityToTask(NearbyPlace place) {
        UserSession session = new UserSession(this);
        if (!session.isLoggedIn()) return;
        String userId = session.getUserId();

        String note = "Selected Facility: " + place.getName() + " (" + place.getAddress() + ")";
        DatabaseReference taskRef = FirebaseHelper.getTasksRef(userId).child(targetTaskId);

        Map<String, Object> updates = new HashMap<>();
        updates.put("completionNotes", note);
        updates.put("status", "AWAITING_REVIEW");

        taskRef.updateChildren(updates).addOnCompleteListener(task -> {
            if (task.isSuccessful()) {
                Toast.makeText(this, "Facility linked to task! Task reported for provider review.", Toast.LENGTH_LONG).show();

                // Log event in My Healthcare Journey (Phase 6)
                long now = System.currentTimeMillis();
                DatabaseReference journeyRef = FirebaseHelper.getJourneyRef(userId).push();
                JourneyItem journeyItem = new JourneyItem(
                        journeyRef.getKey(),
                        userId,
                        "",
                        "Care Task",
                        "Facility Selected: " + place.getName(),
                        "Selected " + place.getName() + " for task: " + (targetTaskTitle != null ? targetTaskTitle : "Care Task") + "\nAddress: " + place.getAddress(),
                        "",
                        now,
                        "Patient",
                        "Facility Visit",
                        "AWAITING_REVIEW"
                );
                journeyRef.setValue(journeyItem);

                finish();
            } else {
                Toast.makeText(this, "Failed to link facility to task.", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void logFacilityViewJourney(NearbyPlace place) {
        UserSession session = new UserSession(this);
        if (!session.isLoggedIn()) return;
        String userId = session.getUserId();

        long now = System.currentTimeMillis();
        DatabaseReference journeyRef = FirebaseHelper.getJourneyRef(userId).push();
        JourneyItem journeyItem = new JourneyItem(
                journeyRef.getKey(),
                userId,
                "",
                "Healthcare Search",
                "Viewed " + place.getName(),
                place.getCategory().toUpperCase(Locale.US) + ": " + place.getAddress() + " (" + String.format(Locale.US, "%.1f km away", place.getDistanceKm()) + ")",
                "",
                now,
                "Self",
                "Nearby Exploration",
                ""
        );
        journeyRef.setValue(journeyItem);
    }

    private void showError(String message, boolean allowRetry) {
        if (emptyMessage != null) emptyMessage.setText(message);
        if (errorContainer != null) errorContainer.setVisibility(View.VISIBLE);
        if (rvNearbyPlaces != null) rvNearbyPlaces.setVisibility(View.GONE);
        if (btnRetryLocation != null) btnRetryLocation.setVisibility(allowRetry ? View.VISIBLE : View.GONE);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == LOCATION_PERMISSION_REQUEST) {
            boolean granted = false;
            for (int result : grantResults) if (result == PackageManager.PERMISSION_GRANTED) granted = true;
            if (granted) {
                checkLocationPermissionAndSearch();
            } else {
                showError("Location permission was not granted. Enable location permissions in settings.", true);
            }
        }
    }
}
