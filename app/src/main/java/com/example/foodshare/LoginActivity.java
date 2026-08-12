package com.example.foodshare;

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.android.gms.auth.api.signin.GoogleSignIn;
import com.google.android.gms.auth.api.signin.GoogleSignInAccount;
import com.google.android.gms.auth.api.signin.GoogleSignInClient;
import com.google.android.gms.auth.api.signin.GoogleSignInOptions;
import com.google.android.gms.common.api.ApiException;
import com.google.android.gms.tasks.Task;
import com.google.android.material.textfield.TextInputEditText;
import com.google.firebase.auth.AuthCredential;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.auth.GoogleAuthProvider;
import com.google.firebase.firestore.FirebaseFirestore;

public class LoginActivity extends AppCompatActivity {

    private FirebaseAuth mAuth;
    private GoogleSignInClient mGoogleSignInClient;
    private static final int RC_SIGN_IN = 9001;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login); // Load login screen layout

        if (getSupportActionBar() != null) {
            getSupportActionBar().hide(); // Hide action bar for cleaner full-screen login UI
        }

        mAuth = FirebaseAuth.getInstance(); // Initialize Firebase Authentication instance
        // Bind UI components for email/password login + navigation actions
        TextInputEditText etEmail = findViewById(R.id.etEmail);
        TextInputEditText etPassword = findViewById(R.id.etPassword);
        Button btnLogin = findViewById(R.id.btnLogin);
        TextView tvRegister = findViewById(R.id.tvRegister);
        View btnLoginGoogle = findViewById(R.id.btnLoginGoogle);

        btnLogin.setOnClickListener(v -> {
            // Read and sanitize user inputs
            String email = etEmail.getText().toString().trim();
            String password = etPassword.getText().toString().trim();

            // Basic validation to prevent empty login attempts
            if (TextUtils.isEmpty(email)) {
                etEmail.setError(getString(R.string.error_email_required));
                return;
            }
            if (TextUtils.isEmpty(password)) {
                etPassword.setError(getString(R.string.error_password_required));
                return;
            }
            // Firebase Email/Password authentication
            mAuth.signInWithEmailAndPassword(email, password)
                    .addOnCompleteListener(this, task -> {
                        if (task.isSuccessful()) {
                            Toast.makeText(LoginActivity.this, getString(R.string.msg_login_successful), Toast.LENGTH_SHORT).show();
                            // Route to MainActivity after successful login
                            Intent intent = new Intent(LoginActivity.this, MainActivity.class);
                            startActivity(intent);
                            // Prevent returning to login screen via back button
                            finish();
                        } else {
                            Toast.makeText(LoginActivity.this, getString(R.string.msg_auth_failed), Toast.LENGTH_SHORT).show();
                        }
                    });
        });
        // Navigate to Register screen
        tvRegister.setOnClickListener(v -> {
            Intent intent = new Intent(LoginActivity.this, RegisterActivity.class);
            startActivity(intent);
        });

        // Configure Google Sign-In request (ID token + email)
        GoogleSignInOptions gso = new GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                    .requestIdToken(getString(R.string.default_web_client_id))
                .requestEmail()
                .build();

        mGoogleSignInClient = GoogleSignIn.getClient(this, gso); // Create Google Sign-In client

        btnLoginGoogle.setOnClickListener(v -> signIn()); // Start Google sign-in flow

        // Navigate to Forgot Password screen
        TextView tvForgotPassword = findViewById(R.id.tvForgotPassword);
        tvForgotPassword.setOnClickListener(v -> {
            Intent intent = new Intent(LoginActivity.this, ForgotPasswordActivity.class);
            startActivity(intent);
        });
    }

    private void signIn() {
        // Launch Google sign-in intent
        Intent signInIntent = mGoogleSignInClient.getSignInIntent();
        startActivityForResult(signInIntent, RC_SIGN_IN);
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        // Result returned from launching the Intent from GoogleSignInApi.getSignInIntent(...);
        if (requestCode == RC_SIGN_IN) {
            // Handle result returned from Google Sign-In
            Task<GoogleSignInAccount> task = GoogleSignIn.getSignedInAccountFromIntent(data);
            try {
                // Google Sign In was successful, authenticate with Firebase
                GoogleSignInAccount account = task.getResult(ApiException.class);
                // Authenticate Google account with Firebase using the ID token
                firebaseAuthWithGoogle(account.getIdToken());
            } catch (ApiException e) {
                // Google Sign-In failed, update the UI appropriately
                Toast.makeText(this, "Google sign in failed: " + e.getMessage(), Toast.LENGTH_SHORT).show();
            }
        }
    }

    private void firebaseAuthWithGoogle(String idToken) {
        // Convert Google ID token to Firebase credential and sign in
        AuthCredential credential = GoogleAuthProvider.getCredential(idToken, null);
        mAuth.signInWithCredential(credential)
                .addOnCompleteListener(this, task -> {
                    if (task.isSuccessful()) {
                        // Signed in successfully with Google + Firebase
                        boolean isNewUser = task.getResult().getAdditionalUserInfo().isNewUser();
                        FirebaseUser user = mAuth.getCurrentUser();

                        // If new user, create a profile record in Firestore
                        // (store basic details like uid/email/name)
                        if (isNewUser && user != null) {
                            // Create user in Firestore
                            String uid = user.getUid();
                            String email = user.getEmail();
                            String displayName = user.getDisplayName();
                            String firstName = displayName != null ? displayName : "";
                            String lastName = "";
                            
                            if (displayName != null && displayName.contains(" ")) {
                                firstName = displayName.substring(0, displayName.indexOf(" "));
                                lastName = displayName.substring(displayName.indexOf(" ") + 1);
                            }

                            UserModel newUser = new UserModel(uid, email, firstName, lastName, "", "", "", "");
                            
                            FirebaseFirestore.getInstance().collection("users")
                                    .document(uid)
                                    .set(newUser)
                                    .addOnSuccessListener(aVoid -> {
                                        Toast.makeText(LoginActivity.this, getString(R.string.msg_login_successful), Toast.LENGTH_SHORT).show();
                                        updateUI();
                                    })
                                    .addOnFailureListener(e -> {
                                        Toast.makeText(LoginActivity.this, "Failed to create profile: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                                        updateUI(); // Proceed anyway
                                    });
                        } else {
                            Toast.makeText(LoginActivity.this, getString(R.string.msg_login_successful), Toast.LENGTH_SHORT).show();
                            updateUI();
                        }
                    } else {
                        // Firebase auth failed
                        // If sign in fails, display a message to the user.
                        // If sign in fails, display a message to the user.
                        String errorMsg = task.getException() != null ? task.getException().getMessage() : "Unknown Error";
                        Toast.makeText(LoginActivity.this, "Auth Failed: " + errorMsg, Toast.LENGTH_LONG).show();
                    }
                });
    }

    private void updateUI() {
        Intent intent = new Intent(LoginActivity.this, MainActivity.class);
        startActivity(intent);
        finish();
    }
}
