package com.example.foodshare;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.google.android.material.textfield.TextInputEditText;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;

public class ProfileFragment extends Fragment {

    private ImageView imgProfile;
    private FirebaseAuth mAuth;
    private TextView tvMyReviews, tvLogout, btnBecomeVerified;
    private View layoutVerifiedBadge; // Changed to clear generic View for layout
    private TextInputEditText etFirstName, etLastName, etEmail, etPhone, etBio, etCity, etCountry;
    private Button btnSaveChanges;

    // Image Picker Logic
    private final ActivityResultLauncher<String> pickImage = registerForActivityResult(
            new ActivityResultContracts.GetContent(),
            uri -> {
                if (uri != null && imgProfile != null) {
                    imgProfile.setImageURI(uri);
                    // TODO: Upload 'uri' to Firebase Storage here to save it permanently
                }
            }
    );

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_profile, container, false);

        try {
            mAuth = FirebaseAuth.getInstance();
            FirebaseUser currentUser = mAuth.getCurrentUser();

            // 1. Initialize Views
            imgProfile = view.findViewById(R.id.imgProfile);
            tvLogout = view.findViewById(R.id.tvLogout);
            tvMyReviews = view.findViewById(R.id.tvMyReviews);
            btnSaveChanges = view.findViewById(R.id.btnSaveChanges);
            
            // Verification Views
            layoutVerifiedBadge = view.findViewById(R.id.layoutVerifiedBadge);
            btnBecomeVerified = view.findViewById(R.id.btnBecomeVerified); 

            // Inputs
            etFirstName = view.findViewById(R.id.etFirstName);
            etLastName = view.findViewById(R.id.etLastName);
            etEmail = view.findViewById(R.id.etProfileEmail);
            etPhone = view.findViewById(R.id.etPhone);
            etBio = view.findViewById(R.id.etBio);
            etCity = view.findViewById(R.id.etCity);
            etCountry = view.findViewById(R.id.etCountry);

            // 2. Load User Data
            if (currentUser != null) {
                if (etEmail != null) etEmail.setText(currentUser.getEmail());
                loadUserProfile(currentUser.getUid());
            }

            // 3. Setup Listeners
            if (imgProfile != null) {
                imgProfile.setOnClickListener(v -> pickImage.launch("image/*"));
            }

            if (tvMyReviews != null) {
                tvMyReviews.setOnClickListener(v -> {
                    Intent intent = new Intent(getActivity(), MyReviewsActivity.class);
                    startActivity(intent);
                });
            }

            if (btnSaveChanges != null) {
                btnSaveChanges.setOnClickListener(v -> saveProfileChanges());
            }

            if (tvLogout != null) {
                tvLogout.setOnClickListener(v -> {
                    mAuth.signOut();
                    Intent intent = new Intent(getActivity(), LoginActivity.class);
                    intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                    startActivity(intent);
                    if (getActivity() != null) getActivity().finish();
                });
            }
            
            // Navigate to Verification with SAFEGUARDS
            if (btnBecomeVerified != null) {
                btnBecomeVerified.setOnClickListener(v -> {
                     try {
                         if (getActivity() != null) {
                             Intent intent = new Intent(getContext(), VerificationActivity.class);
                             startActivity(intent);
                         } else {
                             Toast.makeText(getContext(), "Error: Activity Context is null", Toast.LENGTH_SHORT).show();
                         }
                     } catch (Exception e) {
                         e.printStackTrace();
                         Toast.makeText(getContext(), "Crash Error: " + e.getMessage(), Toast.LENGTH_LONG).show();
                     }
                });
            }

        } catch (Exception e) {
            e.printStackTrace();
            Toast.makeText(getContext(), "Fragment Error: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        }

        return view;
    }
    
    @Override
    public void onResume() {
        super.onResume();
        if (mAuth.getCurrentUser() != null) {
            loadUserProfile(mAuth.getCurrentUser().getUid());
        }
    }

    private void loadUserProfile(String uid) {
        FirebaseFirestore.getInstance().collection("users").document(uid).get()
                .addOnSuccessListener(snap -> {
                    if (snap.exists()) {
                        if (etFirstName != null) etFirstName.setText(snap.getString("firstName"));
                        if (etLastName != null) etLastName.setText(snap.getString("lastName"));
                        if (etPhone != null) etPhone.setText(snap.getString("phone"));
                        if (etBio != null) etBio.setText(snap.getString("bio"));
                        if (etCity != null) etCity.setText(snap.getString("city"));
                        if (etCountry != null) etCountry.setText(snap.getString("country"));
                        
                        // Verification Status
                        boolean isVerified = snap.contains("isVerified") && Boolean.TRUE.equals(snap.getBoolean("isVerified"));
                        if (isVerified) {
                             if (layoutVerifiedBadge != null) layoutVerifiedBadge.setVisibility(View.VISIBLE);
                             if (btnBecomeVerified != null) btnBecomeVerified.setVisibility(View.GONE);
                        } else {
                             if (layoutVerifiedBadge != null) layoutVerifiedBadge.setVisibility(View.GONE);
                             if (btnBecomeVerified != null) btnBecomeVerified.setVisibility(View.VISIBLE);
                        }
                    }
                });
    }

    private void saveProfileChanges() {
        if (mAuth.getCurrentUser() == null) return;
        
        java.util.Map<String, Object> updates = new java.util.HashMap<>();
        updates.put("firstName", etFirstName.getText().toString());
        updates.put("lastName", etLastName.getText().toString());
        updates.put("phone", etPhone.getText().toString());
        updates.put("bio", etBio.getText().toString());
        updates.put("city", etCity.getText().toString());
        updates.put("country", etCountry.getText().toString());

        FirebaseFirestore.getInstance().collection("users").document(mAuth.getCurrentUser().getUid())
                .update(updates)
                .addOnSuccessListener(v -> Toast.makeText(getContext(), "Profile Updated", Toast.LENGTH_SHORT).show())
                .addOnFailureListener(e -> Toast.makeText(getContext(), "Update Failed", Toast.LENGTH_SHORT).show());
    }
}