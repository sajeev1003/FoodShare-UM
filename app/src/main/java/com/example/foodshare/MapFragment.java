package com.example.foodshare;

import android.Manifest;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.app.ActivityCompat;
import androidx.fragment.app.Fragment;

import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import org.osmdroid.config.Configuration;
import org.osmdroid.tileprovider.tilesource.TileSourceFactory;
import org.osmdroid.util.GeoPoint;
import org.osmdroid.views.MapView;
import org.osmdroid.views.overlay.Marker;
import org.osmdroid.views.overlay.mylocation.GpsMyLocationProvider;
import org.osmdroid.views.overlay.mylocation.MyLocationNewOverlay;

public class MapFragment extends Fragment {

    private MapView map;
    private MyLocationNewOverlay locationOverlay;
    private static final int LOCATION_REQUEST_CODE = 100;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        // Initialize OSMDroid configuration
        Configuration.getInstance().load(getContext(), PreferenceManager.getDefaultSharedPreferences(getContext()));

        View view = inflater.inflate(R.layout.fragment_map, container, false);

        map = view.findViewById(R.id.map);
        map.setTileSource(TileSourceFactory.MAPNIK);
        map.setMultiTouchControls(true);
        map.getController().setZoom(15.0);

        // 1. Initial Check (Optional: Loads location if already granted)
        if (hasLocationPermission()) {
            setupLocationOverlay();
        }

        // Default start point (UM)
        GeoPoint startPoint = new GeoPoint(3.1209, 101.6538);
        map.getController().setCenter(startPoint);

        loadFoodMarkers();

        // 2. LOCATE BUTTON CLICK LISTENER (Updated)
        view.findViewById(R.id.btnLocateMe).setOnClickListener(v -> {
            if (ActivityCompat.checkSelfPermission(getContext(), Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                // 1. Permission is MISSING. Force request.
                requestPermissions(new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, LOCATION_REQUEST_CODE);
            } else {
                // 2. Permission IS GRANTED.
                if (locationOverlay != null && locationOverlay.getMyLocation() != null) {
                    // GPS has a fix -> Zoom
                    map.getController().animateTo(locationOverlay.getMyLocation());
                    map.getController().setZoom(18.0);
                } else {
                    // Permission granted, but GPS hardware hasn't found you yet.
                    // Try enabling overlay again just in case
                    setupLocationOverlay();
                    Toast.makeText(getContext(), "Permission granted. Waiting for GPS signal...", Toast.LENGTH_SHORT).show();
                }
            }
        });
        return view;
    }

    // Helper to check permission status
    private boolean hasLocationPermission() {
        return ActivityCompat.checkSelfPermission(getContext(), Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED;
    }

    private void setupLocationOverlay() {
        if (locationOverlay == null && getContext() != null) {
            locationOverlay = new MyLocationNewOverlay(new GpsMyLocationProvider(getContext()), map);
            locationOverlay.enableMyLocation();
            map.getOverlays().add(locationOverlay);
        }
    }

    private void zoomToMyLocation() {
        if (locationOverlay != null && locationOverlay.getMyLocation() != null) {
            map.getController().animateTo(locationOverlay.getMyLocation());
            map.getController().setZoom(18.0);
        } else {
            // Even if permission granted, sometimes GPS isn't ready instantly
            if(hasLocationPermission()) {
                setupLocationOverlay(); // Try setting up again
                Toast.makeText(getContext(), "Locating... tap again in a moment", Toast.LENGTH_SHORT).show();
            }
        }
    }

    // 3. HANDLE PERMISSION RESULT
    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == LOCATION_REQUEST_CODE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                // Permission was just granted!
                setupLocationOverlay();
                // Give the overlay a split second to start, then try zooming
                map.postDelayed(this::zoomToMyLocation, 500);
            } else {
                Toast.makeText(getContext(), "Permission denied. Cannot show your location.", Toast.LENGTH_SHORT).show();
            }
        }
    }

    private void loadFoodMarkers() {
        FirebaseFirestore.getInstance().collection("foods").get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    for (QueryDocumentSnapshot doc : queryDocumentSnapshots) {
                        try {
                            FoodItem item = doc.toObject(FoodItem.class);
                            if (item.getLatitude() != 0 && item.getLongitude() != 0) {
                                Marker marker = new Marker(map);
                                marker.setPosition(new GeoPoint(item.getLatitude(), item.getLongitude()));
                                marker.setTitle(item.getTitle());
                                marker.setSnippet(item.isFree() ? "FREE" : String.format("RM %.2f", item.getPrice()));
                                marker.setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM);
                                map.getOverlays().add(marker);
                            }
                        } catch (Exception e) {
                            // Skip invalid items
                        }
                    }
                    map.invalidate(); // Refresh map
                });
    }

    @Override
    public void onResume() {
        super.onResume();
        map.onResume();
        if (locationOverlay != null) locationOverlay.enableMyLocation();
    }

    @Override
    public void onPause() {
        super.onPause();
        map.onPause();
        if (locationOverlay != null) locationOverlay.disableMyLocation();
    }
}