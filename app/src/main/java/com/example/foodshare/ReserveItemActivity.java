package com.example.foodshare;

import android.graphics.Color;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.FirebaseFirestoreException;

import java.util.HashMap;
import java.util.Map;

public class ReserveItemActivity extends AppCompatActivity {

    private String foodId, donorId, foodName, locationName;
    private int maxQuantity = 1; // Default
    private int selectedQuantity = 1;

    // Views
    private TextView tvName, tvLocation, tvTimeframe, tvDescription, tvStatus, tvMaxLimit, tvQuantityDisplay, tvPostedTime;
    private Button btnConfirm;
    private ImageView btnBack;
    
    // Changing field types to View to support CardView from XML
    private View btnMinus, btnPlus;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_reserve_item);

        if (getSupportActionBar() != null) getSupportActionBar().hide();

        // 1. Get Intent Data
        foodId = getIntent().getStringExtra("FOOD_ID");
        if (foodId == null) {
            Toast.makeText(this, "Error: Item not found", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        // 2. Initialize Views
        initViews();

        // 3. Load Food Details
        loadFoodDetails();

        // 4. Setup Listeners
        setupListeners();
    }

    private void initViews() {
        tvName = findViewById(R.id.tvItemName); 
        tvLocation = findViewById(R.id.tvLocationName); 
        tvTimeframe = findViewById(R.id.tvPickupTime); 
        tvDescription = findViewById(R.id.tvDescription);
        tvStatus = findViewById(R.id.tvStatus); 

        
        // Quantity Controls
        tvMaxLimit = findViewById(R.id.tvMaxQuantity);
        tvQuantityDisplay = findViewById(R.id.tvQuantity);
        
        // These are CardViews in XML
        btnMinus = findViewById(R.id.btnMinus);
        btnPlus = findViewById(R.id.btnPlus);
        
        btnConfirm = findViewById(R.id.btnReserve);
        btnBack = findViewById(R.id.btnBack);
    }

    private void setupListeners() {
        if (btnBack != null) {
            btnBack.setOnClickListener(v -> finish());
        }

        // Minus Button
        if (btnMinus != null) {
            btnMinus.setOnClickListener(v -> {
                if (selectedQuantity > 1) {
                    selectedQuantity--;
                    if (tvQuantityDisplay != null) {
                        tvQuantityDisplay.setText(String.valueOf(selectedQuantity));
                    }
                }
            });
        }

        // Plus Button
        if (btnPlus != null) {
            btnPlus.setOnClickListener(v -> {
                if (selectedQuantity < maxQuantity) {
                    selectedQuantity++;
                    if (tvQuantityDisplay != null) {
                        tvQuantityDisplay.setText(String.valueOf(selectedQuantity));
                    }
                } else {
                    Toast.makeText(this, "Maximum quantity reached", Toast.LENGTH_SHORT).show();
                }
            });
        }

        if (btnConfirm != null) {
            btnConfirm.setOnClickListener(v -> confirmReservation());
        }
    }

    private void loadFoodDetails() {
        FirebaseFirestore.getInstance().collection("foods").document(foodId).get()
                .addOnSuccessListener(snap -> {
                    if (snap.exists()) {
                        try {
                            FoodItem item = snap.toObject(FoodItem.class);
                            if (item != null) {
                                updateUI(item);
                            } else {
                                Toast.makeText(ReserveItemActivity.this, "Failed to parse item data", Toast.LENGTH_SHORT).show();
                            }
                        } catch (Exception e) {
                            Toast.makeText(ReserveItemActivity.this, "Error loading data: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                            e.printStackTrace();
                        }
                    } else {
                        Toast.makeText(ReserveItemActivity.this, "Item no longer exists", Toast.LENGTH_SHORT).show();
                        finish();
                    }
                })
                .addOnFailureListener(e -> Toast.makeText(ReserveItemActivity.this, "Network error: " + e.getMessage(), Toast.LENGTH_SHORT).show());
    }

    private void updateUI(FoodItem item) {
        // --- REQUIREMENT 1: Remove "Posted 2 mins ago" ---
        if (tvPostedTime != null) {
            tvPostedTime.setVisibility(View.GONE); 
        }

        // Basic Info
        foodName = item.getTitle();
        locationName = item.getLocation();
        donorId = item.getDonorId();

        if (tvName != null) tvName.setText(foodName);
        if (tvLocation != null) tvLocation.setText(locationName);
        if (tvDescription != null) tvDescription.setText(item.getDescription());
        if (tvTimeframe != null) tvTimeframe.setText("Pickup: " + item.getPickupTime());

        // --- REQUIREMENT 2 & 3: Quantity & Status Logic ---
        maxQuantity = item.getQuantity();
        
        // Check stock status
        if (maxQuantity <= 0) {
            // CASE: Out of Stock / Reserved
            if (tvStatus != null) {
                tvStatus.setText("Reserved");
                tvStatus.setTextColor(Color.WHITE);
                tvStatus.setBackgroundColor(Color.GRAY);
            }
            
            if (tvMaxLimit != null) tvMaxLimit.setText("No portions available");
            
            // DISABLE BUTTON
            if (btnConfirm != null) {
                btnConfirm.setVisibility(View.GONE);
                btnConfirm.setEnabled(false);
            }
            
            // Disable +/- buttons
            if (btnPlus != null) btnPlus.setEnabled(false);
            if (btnMinus != null) btnMinus.setEnabled(false);
            if (tvQuantityDisplay != null) tvQuantityDisplay.setText("0");

        } else {
            // CASE: Available
            if (tvStatus != null) tvStatus.setText("Available");
            
            if (tvMaxLimit != null) tvMaxLimit.setText("Maximum " + maxQuantity + " portions available");
            
            // Reset selection to 1
            selectedQuantity = 1;
            if (tvQuantityDisplay != null) tvQuantityDisplay.setText("1");
            
            if (btnConfirm != null) {
                btnConfirm.setVisibility(View.VISIBLE);
                btnConfirm.setEnabled(true);
            }
        }
    }

    private void confirmReservation() {
        if (FirebaseAuth.getInstance().getCurrentUser() == null) return;

        String currentUserId = FirebaseAuth.getInstance().getCurrentUser().getUid();

        // 1. Prepare Reservation Data
        Map<String, Object> reservationMap = new HashMap<>();
        reservationMap.put("userId", currentUserId); // Critical for Orders Page
        reservationMap.put("foodId", foodId);
        reservationMap.put("donorId", donorId);
        reservationMap.put("itemName", foodName);
        reservationMap.put("locationName", locationName);
        
        if (tvTimeframe != null) {
            reservationMap.put("pickupTime", tvTimeframe.getText().toString());
        } else {
            reservationMap.put("pickupTime", "Arranged");
        }
        
        reservationMap.put("portion", selectedQuantity + " portion(s)");
        reservationMap.put("status", "Ready");
        reservationMap.put("pickupId", generatePickupId());
        reservationMap.put("timestamp", System.currentTimeMillis());

        // 2. Transaction: Save Reservation AND Reduce Stock safely
        FirebaseFirestore db = FirebaseFirestore.getInstance();
        
        // Generate Reference beforehand to capture ID
        com.google.firebase.firestore.DocumentReference resRef = db.collection("reservations").document();
        String newResId = resRef.getId();

        db.runTransaction(transaction -> {
            // Re-read food document to prevent race conditions (two people booking at once)
            DocumentSnapshot foodSnap = transaction.get(db.collection("foods").document(foodId));
            Long currentStock = foodSnap.getLong("quantity");
            if (currentStock == null) currentStock = 0L;

            if (currentStock < selectedQuantity) {
                throw new FirebaseFirestoreException(
                        "Not enough stock", FirebaseFirestoreException.Code.ABORTED);
            }

            // A. Update Stock
            long newStock = currentStock - selectedQuantity;
            transaction.update(db.collection("foods").document(foodId), "quantity", newStock);
            
            // B. Create Reservation
            transaction.set(resRef, reservationMap);

            return null;
        }).addOnSuccessListener(aVoid -> {
            Toast.makeText(this, "Reservation Confirmed!", Toast.LENGTH_SHORT).show();
            
            // Notifications
            NotificationHelper.send(currentUserId, "Reservation Confirmed", "You reserved " + foodName, "reservation", newResId);
            if (donorId != null) {
                // For donor, link to the new reservation
                NotificationHelper.send(donorId, "New Order", "Someone reserved " + selectedQuantity + "x " + foodName, "order", newResId);
            }

            finish();
        }).addOnFailureListener(e -> {
            Toast.makeText(this, "Reservation Failed: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        });
    }

    private String generatePickupId() {
        int random = (int)(Math.random() * 9000) + 1000;
        return "#FS" + random;
    }
}