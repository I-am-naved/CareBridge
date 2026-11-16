package com.example.carebridge;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Address;
import android.location.Geocoder;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.core.app.ActivityCompat;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.fragment.app.FragmentActivity;

import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MarkerOptions;

import java.io.IOException;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class AppointmentLocationActivity extends FragmentActivity implements OnMapReadyCallback {
    private static final int LOCATION_PERMISSION_REQUEST_CODE = 1001;
    private GoogleMap map;
    private String clinicName;
    private String clinicAddress;
    private LatLng clinicLatLng;
    private final ExecutorService geocoderExecutor = Executors.newSingleThreadExecutor();

    @Override protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        WindowCompat.setDecorFitsSystemWindows(getWindow(), false);
        getWindow().setStatusBarColor(android.graphics.Color.WHITE);
        WindowCompat.getInsetsController(getWindow(), getWindow().getDecorView()).setAppearanceLightStatusBars(true);
        setContentView(R.layout.activity_appointment_location);
        applyInsets();

        clinicName = getIntent().getStringExtra("clinicName");
        clinicAddress = getIntent().getStringExtra("clinicAddress");
        if (clinicName == null) clinicName = "Healthcare location";
        if (clinicAddress == null) clinicAddress = "Address unavailable";
        double lat = getIntent().getDoubleExtra("clinicLatitude", Double.NaN);
        double lng = getIntent().getDoubleExtra("clinicLongitude", Double.NaN);
        if (!Double.isNaN(lat) && !Double.isNaN(lng)) clinicLatLng = new LatLng(lat, lng);

        TextView back = findViewById(R.id.mapBackButton);
        TextView name = findViewById(R.id.cardClinicName);
        TextView address = findViewById(R.id.cardClinicAddress);
        Button directions = findViewById(R.id.btnCardDirections);
        Button openMaps = findViewById(R.id.btnOpenInGoogleMaps);
        back.setOnClickListener(v -> finish());
        name.setText(clinicName);
        address.setText(clinicAddress);
        directions.setOnClickListener(v -> openNavigation());
        openMaps.setOnClickListener(v -> openNavigation());

        SupportMapFragment fragment = (SupportMapFragment) getSupportFragmentManager().findFragmentById(R.id.mapFragment);
        if (fragment != null) fragment.getMapAsync(this);
    }

    private void applyInsets() {
        View root = findViewById(R.id.mapRoot);
        View toolbar = findViewById(R.id.topBar);
        View card = findViewById(R.id.locationCard);
        int left = root.getPaddingLeft(), right = root.getPaddingRight();
        ViewCompat.setOnApplyWindowInsetsListener(root, (v, insets) -> {
            Insets bars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            toolbar.setPadding(toolbar.getPaddingLeft(), dp(8) + bars.top, toolbar.getPaddingRight(), dp(8));
            card.setPadding(card.getPaddingLeft(), card.getPaddingTop(), card.getPaddingRight(), dp(16) + bars.bottom);
            v.setPadding(left + bars.left, 0, right + bars.right, 0);
            return insets;
        });
    }
    private int dp(int value) { return Math.round(value * getResources().getDisplayMetrics().density); }

    @Override public void onMapReady(@NonNull GoogleMap googleMap) {
        map = googleMap;
        enableMyLocation();
        if (clinicLatLng != null) showClinicOnMap();
        else geocodeClinicAddress();
    }

    private void showClinicOnMap() {
        if (map == null || clinicLatLng == null) return;
        map.clear();
        map.addMarker(new MarkerOptions().position(clinicLatLng).title(clinicName).snippet(clinicAddress));
        map.animateCamera(CameraUpdateFactory.newLatLngZoom(clinicLatLng, 15f));
    }

    private void geocodeClinicAddress() {
        if (!Geocoder.isPresent()) {
            Toast.makeText(this, "Map coordinates are not available for this address.", Toast.LENGTH_LONG).show();
            return;
        }
        geocoderExecutor.execute(() -> {
            try {
                Geocoder geocoder = new Geocoder(this, Locale.getDefault());
                List<Address> results = geocoder.getFromLocationName(clinicAddress, 1);
                if (results != null && !results.isEmpty()) {
                    Address result = results.get(0);
                    clinicLatLng = new LatLng(result.getLatitude(), result.getLongitude());
                    runOnUiThread(this::showClinicOnMap);
                } else {
                    runOnUiThread(() -> Toast.makeText(this, "We couldn't locate this address. Check the clinic address.", Toast.LENGTH_LONG).show());
                }
            } catch (IOException e) {
                runOnUiThread(() -> Toast.makeText(this, "Unable to look up this address. Check your connection.", Toast.LENGTH_LONG).show());
            }
        });
    }

    private void enableMyLocation() {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED
                || ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            if (map != null) map.setMyLocationEnabled(true);
        } else {
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION},
                    LOCATION_PERMISSION_REQUEST_CODE);
        }
    }

    @Override public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == LOCATION_PERMISSION_REQUEST_CODE && map != null) {
            boolean granted = false;
            for (int result : grantResults) if (result == PackageManager.PERMISSION_GRANTED) granted = true;
            if (granted) enableMyLocation();
            else Toast.makeText(this, "You can still view the clinic location without sharing your location.", Toast.LENGTH_LONG).show();
        }
    }

    private void openNavigation() {
        if (clinicLatLng == null) {
            Toast.makeText(this, "The clinic coordinates are not available yet.", Toast.LENGTH_SHORT).show();
            return;
        }
        Uri uri = Uri.parse("google.navigation:q=" + clinicLatLng.latitude + "," + clinicLatLng.longitude);
        Intent intent = new Intent(Intent.ACTION_VIEW, uri);
        intent.setPackage("com.google.android.apps.maps");
        if (intent.resolveActivity(getPackageManager()) != null) startActivity(intent);
        else startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse("https://www.google.com/maps/dir/?api=1&destination="
                + clinicLatLng.latitude + "," + clinicLatLng.longitude)));
    }

    @Override protected void onDestroy() {
        super.onDestroy();
        geocoderExecutor.shutdown();
    }
}
