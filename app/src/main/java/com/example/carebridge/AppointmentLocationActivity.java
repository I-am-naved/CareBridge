package com.example.carebridge;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Address;
import android.location.Geocoder;
import android.net.Uri;
import android.os.Bundle;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.core.app.ActivityCompat;
import androidx.fragment.app.FragmentActivity;

import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MarkerOptions;

import java.io.IOException;
import java.util.List;
import java.util.Locale;

public class AppointmentLocationActivity extends FragmentActivity implements OnMapReadyCallback {

    private static final int LOCATION_PERMISSION_REQUEST_CODE = 1001;

    private GoogleMap mMap;
    private String clinicName;
    private String clinicAddress;
    private LatLng clinicLatLng;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_appointment_location);

        clinicName = getIntent().getStringExtra("clinicName");
        clinicAddress = getIntent().getStringExtra("clinicAddress");

        if (clinicName == null) clinicName = "Healthcare Clinic";
        if (clinicAddress == null) clinicAddress = "Medical Center";

        TextView backButton = findViewById(R.id.mapBackButton);
        TextView nameText = findViewById(R.id.cardClinicName);
        TextView addressText = findViewById(R.id.cardClinicAddress);
        Button btnDirections = findViewById(R.id.btnCardDirections);
        Button btnOpenInMaps = findViewById(R.id.btnOpenInGoogleMaps);

        if (backButton != null) {
            backButton.setOnClickListener(v -> finish());
        }

        if (nameText != null) nameText.setText(clinicName);
        if (addressText != null) addressText.setText(clinicAddress);

        if (btnDirections != null) {
            btnDirections.setOnClickListener(v -> openNavigationIntent());
        }

        if (btnOpenInMaps != null) {
            btnOpenInMaps.setOnClickListener(v -> openNavigationIntent());
        }

        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.mapFragment);
        if (mapFragment != null) {
            mapFragment.getMapAsync(this);
        }
    }

    @Override
    public void onMapReady(@NonNull GoogleMap googleMap) {
        mMap = googleMap;

        // Enable My Location layer if permission granted
        enableMyLocation();

        // Geocode clinic address
        clinicLatLng = geocodeAddress(clinicAddress);
        if (clinicLatLng == null) {
            // Default fallback (e.g. Kamloops / General location)
            clinicLatLng = new LatLng(50.6745, -120.3273);
        }

        mMap.addMarker(new MarkerOptions()
                .position(clinicLatLng)
                .title(clinicName)
                .snippet(clinicAddress)
                .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_RED)));

        mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(clinicLatLng, 15f));
    }

    private LatLng geocodeAddress(String addressStr) {
        Geocoder geocoder = new Geocoder(this, Locale.getDefault());
        try {
            List<Address> addresses = geocoder.getFromLocationName(addressStr, 1);
            if (addresses != null && !addresses.isEmpty()) {
                Address loc = addresses.get(0);
                return new LatLng(loc.getLatitude(), loc.getLongitude());
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        return null;
    }

    private void enableMyLocation() {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED
                || ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            if (mMap != null) {
                mMap.setMyLocationEnabled(true);
            }
        } else {
            ActivityCompat.requestPermissions(
                    this,
                    new String[]{Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION},
                    LOCATION_PERMISSION_REQUEST_CODE
            );
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == LOCATION_PERMISSION_REQUEST_CODE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                enableMyLocation();
            } else {
                Toast.makeText(this, "Location permission denied. Map will display clinic location only.", Toast.LENGTH_SHORT).show();
            }
        }
    }

    private void openNavigationIntent() {
        Uri gmmIntentUri = Uri.parse("google.navigation:q=" + Uri.encode(clinicAddress));
        Intent mapIntent = new Intent(Intent.ACTION_VIEW, gmmIntentUri);
        mapIntent.setPackage("com.google.android.apps.maps");
        if (mapIntent.resolveActivity(getPackageManager()) != null) {
            startActivity(mapIntent);
        } else {
            // Fallback to browser if Maps app isn't installed
            Intent webIntent = new Intent(Intent.ACTION_VIEW, Uri.parse("https://www.google.com/maps/dir/?api=1&destination=" + Uri.encode(clinicAddress)));
            startActivity(webIntent);
        }
    }
}
