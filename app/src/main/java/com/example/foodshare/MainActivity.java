package com.example.foodshare;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Toast;
import androidx.activity.result.ActivityResultLauncher;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.firebase.firestore.FirebaseFirestore;
import com.journeyapps.barcodescanner.ScanContract;
import com.journeyapps.barcodescanner.ScanOptions;

public class MainActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        if (getSupportActionBar() != null) getSupportActionBar().hide();

        // Start Notification Service (For background alerts)
        startService(new Intent(this, NotificationService.class));
        
        checkNotificationPermission();

        BottomNavigationView bottomNav = findViewById(R.id.bottomNavigationView);

        // Default Fragment Logic
        if (savedInstanceState == null) {
            String navigateTo = getIntent().getStringExtra("NAVIGATE_TO");
            if ("RESERVATIONS".equals(navigateTo)) {
                getSupportFragmentManager().beginTransaction().replace(R.id.fragment_container, new ReservationsFragment()).commit();
                bottomNav.setSelectedItemId(R.id.nav_reservations);
            } else {
                getSupportFragmentManager().beginTransaction().replace(R.id.fragment_container, new HomeFragment()).commit();
            }
        }

        // --- SCANNER LAUNCHER ---
        ActivityResultLauncher<ScanOptions> scanLauncher = registerForActivityResult(new ScanContract(), result -> {
            if (result.getContents() == null) {
                Toast.makeText(this, "Cancelled", Toast.LENGTH_SHORT).show();
            } else {
                String documentId = result.getContents();
                processCollection(documentId);
            }
        });

        // Navigation Logic
        bottomNav.setOnItemSelectedListener(item -> {
            Fragment selectedFragment = null;
            int id = item.getItemId();

            if (id == R.id.nav_home) selectedFragment = new HomeFragment();
            else if (id == R.id.nav_search) selectedFragment = new MapFragment();
            else if (id == R.id.nav_reservations) selectedFragment = new ReservationsFragment();
            else if (id == R.id.nav_profile) selectedFragment = new ProfileFragment();
            else if (id == R.id.nav_scan) {
                // Launch Custom Box Scanner
                ScanOptions options = new ScanOptions();
                options.setCaptureActivity(CustomScannerActivity.class);
                options.setDesiredBarcodeFormats(ScanOptions.QR_CODE);
                options.setPrompt("Scan QR Code");
                options.setCameraId(0);
                options.setBeepEnabled(true);
                options.setOrientationLocked(true);
                scanLauncher.launch(options);
                return false;
            }

            if (selectedFragment != null) {
                getSupportFragmentManager().beginTransaction().replace(R.id.fragment_container, selectedFragment).commit();
            }
            return true;
        });
    }

    private void processCollection(String reservationId) {
        if (reservationId == null || reservationId.isEmpty()) return;

        FirebaseFirestore db = FirebaseFirestore.getInstance();
        
        db.collection("reservations").document(reservationId).get().addOnSuccessListener(resSnap -> {
            if (!resSnap.exists()) return;

            String foodId = resSnap.getString("foodId");
            String donorId = resSnap.getString("donorId"); // The one who posted
            String userId = resSnap.getString("userId");   // The receiver

            // 1. Mark Reservation as Collected
            db.collection("reservations").document(reservationId).update("status", "Collected");

            // 2. Reduce Food Quantity
            if (foodId != null) {
                db.collection("foods").document(foodId).get().addOnSuccessListener(foodSnap -> {
                    if (foodSnap.exists()) {
                        Long currentQty = foodSnap.getLong("quantity");
                        if (currentQty == null) currentQty = 1L;

                        long newQty = currentQty - 1;

                        if (newQty <= 0) {
                            // DELETE if 0 left
                            db.collection("foods").document(foodId).delete();
                            NotificationHelper.send(donorId, "Stock Empty", "Your listing is out of stock and removed.", "system", foodId);
                        } else {
                            // UPDATE quantity
                            db.collection("foods").document(foodId).update("quantity", newQty);
                        }
                    }
                });
            }
            
            // 3. Send Notifications
            NotificationHelper.send(userId, "Collected", "Pickup confirmed!", "collection", reservationId);
            NotificationHelper.send(donorId, "Food Taken", "Your item has been collected.", "collection", reservationId);

            Toast.makeText(this, "Pickup Verified!", Toast.LENGTH_LONG).show();
        });
    }

    public void navigateToHome() {
        ((BottomNavigationView) findViewById(R.id.bottomNavigationView)).setSelectedItemId(R.id.nav_home);
    }

    private void checkNotificationPermission() {
        if (android.os.Build.VERSION.SDK_INT >= 33) {
            if (androidx.core.content.ContextCompat.checkSelfPermission(this, android.Manifest.permission.POST_NOTIFICATIONS) != android.content.pm.PackageManager.PERMISSION_GRANTED) {
                androidx.core.app.ActivityCompat.requestPermissions(this, new String[]{android.Manifest.permission.POST_NOTIFICATIONS}, 101);
            }
        }
    }
}
