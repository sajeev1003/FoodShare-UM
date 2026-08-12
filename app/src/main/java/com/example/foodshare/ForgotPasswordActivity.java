package com.example.foodshare;

import android.os.Bundle;
import android.text.TextUtils;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.textfield.TextInputEditText;
import com.google.firebase.auth.FirebaseAuth;

public class ForgotPasswordActivity extends AppCompatActivity {
    // FirebaseAuth instance used to trigger password reset emails via Firebase Authentication
    private FirebaseAuth mAuth;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_forgot_password);

        // Hide ActionBar for a cleaner full-screen auth UI (avoids inconsistent styling across screens)
        if (getSupportActionBar() != null) {
            getSupportActionBar().hide();
        }
        // Initialize Firebase Auth to enable password reset functionality
        mAuth = FirebaseAuth.getInstance();

        // Bind UI components for user input (email) + reset action + navigation back to login
        TextInputEditText etEmail = findViewById(R.id.etEmail);
        Button btnResetPassword = findViewById(R.id.btnResetPassword);
        TextView tvBackToLogin = findViewById(R.id.tvBackToLogin);

        // Handle reset request: validate email input and send reset link through Firebase
        btnResetPassword.setOnClickListener(v -> {
            // Read and sanitize the email input (trim removes accidental leading/trailing spaces)
            String email = etEmail.getText().toString().trim();

            if (TextUtils.isEmpty(email)) {
                etEmail.setError(getString(R.string.error_email_required));
                return;
            }

            // Trigger Firebase to send a password reset email to the provided address
            mAuth.sendPasswordResetEmail(email)
                    .addOnCompleteListener(task -> {
                        // Show user feedback and return to login if the reset email was successfully sent
                        if (task.isSuccessful()) {
                            Toast.makeText(ForgotPasswordActivity.this, getString(R.string.msg_reset_link_sent), Toast.LENGTH_LONG).show();
                            finish(); // Go back to login
                        } else {
                            // If Firebase fails (invalid email / network / auth config), show the error message for debugging
                            Toast.makeText(ForgotPasswordActivity.this, getString(R.string.error_failed_send_reset) + task.getException().getMessage(), Toast.LENGTH_LONG).show();
                        }
                    });
        });

        tvBackToLogin.setOnClickListener(v -> finish());
    }
}
