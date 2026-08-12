package com.example.foodshare;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;

import androidx.appcompat.app.AppCompatActivity;
/**
 * Entry screen (Launcher Activity) for FoodShare.
 * Responsibilities:
 * - Display the welcome UI (activity_welcome).
 * - Hide the action bar for a cleaner full-screen welcome experience.
 * - Navigate the user to LoginActivity when "Start" is pressed.
 * - Finish this activity so users cannot return to the welcome screen via back button.
 */

public class WelcomeActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_welcome); // Load welcome screen layout

        // Hide action bar to keep the welcome screen clean and full-screen
        if (getSupportActionBar() != null) {
            getSupportActionBar().hide();
        }

        // Navigate to Login screen when user taps "Start"
        View btnStart = findViewById(R.id.btnStart);
        btnStart.setOnClickListener(v -> {
            Intent intent = new Intent(WelcomeActivity.this, LoginActivity.class);
            startActivity(intent);
            // Prevent returning to welcome screen when pressing back from LoginActivity
            finish();
        });
    }
}