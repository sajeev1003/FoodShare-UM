package com.example.foodshare;

import android.os.Bundle;
import android.widget.Button;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.textfield.TextInputEditText;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.util.HashMap;
import java.util.Map;

public class VerificationActivity extends AppCompatActivity {

    private TextInputEditText etShopName, etFullName, etPhone, etRegNo;
    private Button btnSubmit;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_verification);
        if (getSupportActionBar() != null) getSupportActionBar().hide();

        initViews();
        setupListeners();
    }

    private void initViews() {
        etShopName = findViewById(R.id.etShopName);
        etFullName = findViewById(R.id.etFullName);
        etPhone = findViewById(R.id.etPhone);
        etRegNo = findViewById(R.id.etRegNo);
        btnSubmit = findViewById(R.id.btnSubmitVerification);
        
        // Back Button
        findViewById(R.id.btnBack).setOnClickListener(v -> finish());
    }

    private void setupListeners() {
        btnSubmit.setOnClickListener(v -> submitVerification());
    }

    private void submitVerification() {
        String shopName = etShopName.getText().toString().trim();
        String fullName = etFullName.getText().toString().trim();
        String phone = etPhone.getText().toString().trim();
        String regNo = etRegNo.getText().toString().trim();

        if (shopName.isEmpty()) {
            etShopName.setError("Shop Name is required");
            return;
        }
        if (fullName.isEmpty()) {
            etFullName.setError("Full Name is required");
            return;
        }
        if (phone.isEmpty()) {
            etPhone.setError("Phone Number is required");
            return;
        }

        btnSubmit.setEnabled(false);
        Toast.makeText(this, "Submitting...", Toast.LENGTH_SHORT).show();

        Map<String, Object> verificationData = new HashMap<>();
        verificationData.put("shopName", shopName);
        verificationData.put("verificationName", fullName);
        verificationData.put("verificationPhone", phone);
        verificationData.put("businessRegNo", regNo);
        verificationData.put("isVerified", true); // Auto-approve for demo/VM purpose

        String uid = FirebaseAuth.getInstance().getCurrentUser().getUid();

        FirebaseFirestore.getInstance().collection("users").document(uid)
                .update(verificationData)
                .addOnSuccessListener(aVoid -> {
                    // Update all existing food listings to be verified
                    updateExistingFoodListings(uid);
                    
                    Toast.makeText(this, "Verification Successful! You are now a Verified Vendor.", Toast.LENGTH_LONG).show();
                    finish(); // Go back to profile
                })
                .addOnFailureListener(e -> {
                    btnSubmit.setEnabled(true);
                    Toast.makeText(this, "Error: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
    }

    // Retroactive Update: Finds all food items by this donor and marks them as verified
    private void updateExistingFoodListings(String uid) {
        FirebaseFirestore.getInstance().collection("foods")
                .whereEqualTo("donorId", uid)
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    if (!queryDocumentSnapshots.isEmpty()) {
                        for (QueryDocumentSnapshot document : queryDocumentSnapshots) {
                            document.getReference().update("isDonorVerified", true);
                        }
                    }
                })
                .addOnFailureListener(e -> e.printStackTrace()); // Silent failure is acceptable here, main goal achieved
    }
}
