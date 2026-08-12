package com.example.foodshare;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;

public class ReservationDetailsActivity extends AppCompatActivity {

    private String reservationId;
    private double destLat = 0.0;
    private double destLng = 0.0;
    private String foodName, donorId; // Added for notifications

    // UI Components
    private TextView tvFoodName, tvDescription, tvExpiry, tvPickupId, tvReservedOn, tvPickupBy, tvQuantity;
    private TextView tvDonorName, tvDonorDesc, tvLocationName, tvAddress;
    private ImageView btnBack, ivItemImage;
    private Button btnDirections, btnCancel, btnChat;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_reservation_details);

        // 1. Hide default Action Bar
        if (getSupportActionBar() != null) getSupportActionBar().hide();

        // 2. Get Intent Data
        reservationId = getIntent().getStringExtra("RESERVATION_ID");
        if (reservationId == null) {
            Toast.makeText(this, "Error: No Reservation ID", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        // 3. Initialize Views
        initViews();

        // 4. Setup Listeners
        setupListeners();

        // 5. Load Data
        loadReservationData();
    }

    private void initViews() {
        btnBack = findViewById(R.id.btnBack);
        ivItemImage = findViewById(R.id.ivItemImage);

        tvFoodName = findViewById(R.id.tvFoodName);
        tvDescription = findViewById(R.id.tvDetailDescription);
        tvExpiry = findViewById(R.id.tvDetailExpiry);

        tvPickupId = findViewById(R.id.tvDetailReservationId);
        tvReservedOn = findViewById(R.id.tvDetailReservedOn);
        tvPickupBy = findViewById(R.id.tvDetailPickupBy);
        tvQuantity = findViewById(R.id.tvDetailQuantity);

        tvDonorName = findViewById(R.id.tvDonorName);
        tvDonorDesc = findViewById(R.id.tvDonorDescription);

        tvLocationName = findViewById(R.id.tvDetailLocationName);
        tvAddress = findViewById(R.id.tvDetailAddress);

        btnDirections = findViewById(R.id.btnDirections);
        btnCancel = findViewById(R.id.btnCancel);
        btnChat = findViewById(R.id.btnChatWithDonor);
    }

    private void setupListeners() {
        btnBack.setOnClickListener(v -> finish());

        // Directions Logic (Google Maps)
        btnDirections.setOnClickListener(v -> {
            if (destLat != 0.0 && destLng != 0.0) {
                Uri gmmIntentUri = Uri.parse("google.navigation:q=" + destLat + "," + destLng);
                Intent mapIntent = new Intent(Intent.ACTION_VIEW, gmmIntentUri);
                mapIntent.setPackage("com.google.android.apps.maps");

                if (mapIntent.resolveActivity(getPackageManager()) != null) {
                    startActivity(mapIntent);
                } else {
                    // Fallback to browser
                    Uri browserUri = Uri.parse("https://www.google.com/maps/search/?api=1&query=" + destLat + "," + destLng);
                    Intent browserIntent = new Intent(Intent.ACTION_VIEW, browserUri);
                    startActivity(browserIntent);
                }
            } else {
                Toast.makeText(this, "Coordinates not available", Toast.LENGTH_SHORT).show();
            }
        });

        btnCancel.setOnClickListener(v -> {
            // Cancel Logic
            if (reservationId != null) {
                FirebaseFirestore.getInstance().collection("reservations").document(reservationId)
                    .update("status", "Cancelled")
                    .addOnSuccessListener(aVoid -> {
                        Toast.makeText(this, "Reservation Cancelled", Toast.LENGTH_SHORT).show();
                        
                        // Notify Donor about Cancellation
                        if (donorId != null) {
                            NotificationHelper.send(donorId, "Reservation Cancelled", "Reservation for " + foodName + " was cancelled.", "cancel", reservationId);
                        }
                        
                        finish();
                    })
                    .addOnFailureListener(e -> Toast.makeText(this, "Error: " + e.getMessage(), Toast.LENGTH_SHORT).show());
            }
        });
        
        // btnChat listener is now set in loadFoodDetails after fetching phone number
    }

    private void loadReservationData() {
        FirebaseFirestore.getInstance().collection("reservations").document(reservationId).get()
                .addOnSuccessListener(resSnap -> {
                    if (resSnap.exists()) {
                        Reservation res = resSnap.toObject(Reservation.class);
                        if (res != null) {
                            // --- 1. POPULATE DATA ---
                            tvPickupId.setText(res.getPickupId());
                            tvPickupBy.setText(res.getPickupTime());
                            tvQuantity.setText(res.getPortion());
                            tvReservedOn.setText("Today"); // Or fetch date if available
    
                            // --- 2. CHECK STATUS (THE FIX) ---
                            String status = res.getStatus();
                            
                            // If the reservation is "Done" (Collected, Expired, or Cancelled)
                            if ("Collected".equals(status) || "Expired".equals(status) || "Cancelled".equals(status)) {
                                
                                // HIDE ALL INTERACTIVE BUTTONS
                                btnCancel.setVisibility(View.GONE);      // No canceling
                                btnDirections.setVisibility(View.GONE);  // No directions
                                btnChat.setVisibility(View.GONE);        // No chatting
                                
                            } else {
                                // ACTIVE (Ready / Pending)
                                btnCancel.setVisibility(View.VISIBLE);
                                btnDirections.setVisibility(View.VISIBLE);
                                btnChat.setVisibility(View.VISIBLE);
                            }
    
                            // Now load the associated FoodItem details
                            loadFoodDetails(res.getFoodId());
                        }
                    } else {
                        Toast.makeText(this, "Reservation not found", Toast.LENGTH_SHORT).show();
                    }
                })
                .addOnFailureListener(e -> Toast.makeText(this, "Error: " + e.getMessage(), Toast.LENGTH_SHORT).show());
    }

    private void loadFoodDetails(String foodId) {
        if (foodId == null) return;

        FirebaseFirestore.getInstance().collection("foods").document(foodId).get()
                .addOnSuccessListener(foodSnap -> {
                    if (foodSnap.exists()) {
                        FoodItem item = foodSnap.toObject(FoodItem.class);
                        if (item != null) {
                            // --- Map FoodItem Data ---
                            foodName = item.getTitle(); // Store food name for notification
                            donorId = item.getDonorId(); // Store donor ID for notification

                            // 1. Title/Name
                            tvFoodName.setText(item.getTitle());

                            // 2. Description
                            tvDescription.setText(item.getDescription());

                            // 3. Expiry (Convert timestamp to readable text)
                            long hoursLeft = (item.getExpiryTimestamp() - System.currentTimeMillis()) / (1000 * 60 * 60);
                            if (hoursLeft > 0) {
                                tvExpiry.setText("Expires in " + hoursLeft + " hours");
                            } else {
                                tvExpiry.setText("Expired");
                            }

                            // 4. Location
                            tvLocationName.setText(item.getLocation());
                            tvAddress.setText(item.getLocation());

                            // 5. Donor Info
                            tvDonorName.setText(item.getDonorName());
                            tvDonorDesc.setText("Community Donor");

                            // 6. Map Coordinates
                            destLat = item.getLatitude();
                            destLng = item.getLongitude();

                            // 7. FETCH DONOR PHONE (For WhatsApp)
                            if (donorId != null) {
                                FirebaseFirestore.getInstance().collection("users").document(donorId).get()
                                    .addOnSuccessListener(userSnap -> {
                                        if (userSnap.exists()) {
                                            UserModel donor = userSnap.toObject(UserModel.class);
                                            if (donor != null && donor.getPhone() != null && !donor.getPhone().isEmpty()) {
                                                final String phone = donor.getPhone();
                                                
                                                // UDPATE CHAT BUTTON
                                                btnChat.setOnClickListener(v -> {
                                                    try {
                                                        // Sanitize Phone Number: Remove all non-digits
                                                        String cleanPhone = phone.replaceAll("[^\\d]", "");
                                                        
                                                        // Add Country Code (Malaysia 60) if missing
                                                        if (cleanPhone.startsWith("0")) {
                                                            cleanPhone = "60" + cleanPhone.substring(1);
                                                        }
                                                        
                                                        String url = "https://api.whatsapp.com/send?phone=" + cleanPhone;
                                                        Intent i = new Intent(Intent.ACTION_VIEW);
                                                        i.setData(Uri.parse(url));
                                                        startActivity(i);
                                                    } catch (Exception e) {
                                                        Toast.makeText(ReservationDetailsActivity.this, "WhatsApp not installed", Toast.LENGTH_SHORT).show();
                                                    }
                                                });
                                            } else {
                                                btnChat.setOnClickListener(v -> Toast.makeText(ReservationDetailsActivity.this, "Donor phone number not available", Toast.LENGTH_SHORT).show());
                                            }
                                        }
                                    });
                            }
                        }
                    }
                });
    }
}
