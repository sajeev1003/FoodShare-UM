package com.example.foodshare;

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import com.google.android.material.textfield.TextInputEditText;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;

public class RegisterActivity extends AppCompatActivity {

    private FirebaseAuth mAuth;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // Register screen UI setup
        setContentView(R.layout.activity_register);

        // Hide ActionBar to match full-screen auth flow UI
        if (getSupportActionBar() != null) getSupportActionBar().hide();

        // Firebase Auth entry point for email/password registration
        mAuth = FirebaseAuth.getInstance();

        // Bind UI components used for registration and navigation
        View btnBack = findViewById(R.id.btnBack);
        Button btnCreateAccount = findViewById(R.id.btnCreateAccount);
        TextView tvLogin = findViewById(R.id.tvLogin);
        
        TextInputEditText etEmail = findViewById(R.id.etEmail);
        TextInputEditText etPassword = findViewById(R.id.etPassword);
        TextInputEditText etConfirmPassword = findViewById(R.id.etConfirmPassword);

        // Back button: return to previous screen
        btnBack.setOnClickListener(v -> finish());

        // Create account flow: validate inputs → create Firebase Auth user → store profile in Firestore
        btnCreateAccount.setOnClickListener(v -> {
            String email = etEmail.getText().toString().trim();
            String password = etPassword.getText().toString().trim();
            String confirmPassword = etConfirmPassword.getText().toString().trim();

            // Basic client-side validation to prevent avoidable Firebase failures
            if (TextUtils.isEmpty(email)) { etEmail.setError("Email required"); return; }
            if (TextUtils.isEmpty(password)) { etPassword.setError("Password required"); return; }
            if (!password.equals(confirmPassword)) { etConfirmPassword.setError("Passwords mismatch"); return; }

            // Create account in Firebase Authentication (email/password)
            mAuth.createUserWithEmailAndPassword(email, password)
                    .addOnCompleteListener(this, task -> {
                        // Get the newly created user's UID (used as Firestore document id)
                        if (task.isSuccessful()) {
                            // Collect profile fields to persist in Firestore
                            String uid = mAuth.getCurrentUser().getUid();
                            String fullName = ((TextInputEditText) findViewById(R.id.etFullName)).getText().toString().trim();
                            String phone = ((TextInputEditText) findViewById(R.id.etPhone)).getText().toString().trim();
                            String bio = ((TextInputEditText) findViewById(R.id.etBio)).getText().toString().trim();
                            String city = ((TextInputEditText) findViewById(R.id.etCity)).getText().toString().trim();
                            String country = ((TextInputEditText) findViewById(R.id.etCountry)).getText().toString().trim();

                            String firstName = fullName;
                            String lastName = "";
                            if (fullName.contains(" ")) {
                                firstName = fullName.substring(0, fullName.indexOf(" "));
                                lastName = fullName.substring(fullName.indexOf(" ") + 1);
                            }

                            UserModel user = new UserModel(uid, email, firstName, lastName, phone, bio, city, country);

                            // Persist user profile in Firestore under: users/{uid}
                            FirebaseFirestore.getInstance().collection("users").document(uid).set(user)
                                    .addOnSuccessListener(aVoid -> {
                                        Toast.makeText(RegisterActivity.this, "Account Created", Toast.LENGTH_SHORT).show();
                                        Intent intent = new Intent(RegisterActivity.this, LoginActivity.class);
                                        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                                        startActivity(intent);
                                    });
                        } else {
                            // Only show generic failure or handle specific exceptions if needed
                            // Registration failed: show a user-friendly message (and surface Firebase error when available)
                            if (task.getException() != null) {
                                Toast.makeText(RegisterActivity.this, "Failed: " + task.getException().getMessage(), Toast.LENGTH_SHORT).show();
                            } else {
                                Toast.makeText(RegisterActivity.this, "Registration Failed", Toast.LENGTH_SHORT).show();
                            }
                        }
                    });
        });

        tvLogin.setOnClickListener(v -> finish());
    }
}