package com.example.foodshare;

import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.widget.RatingBar;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.HashMap;
import java.util.Map;

public class ReviewActivity extends AppCompatActivity {

    private RatingBar ratingBar;
    private EditText etFeedback;
    private Button btnSubmit;
    private String donorId, foodId;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_review);

        if (getSupportActionBar() != null) getSupportActionBar().hide();

        // 1. CAPTURE DATA (Crucial Step)
        donorId = getIntent().getStringExtra("DONOR_ID");
        foodId = getIntent().getStringExtra("FOOD_ID");

        // 2. Bind Views
        ratingBar = findViewById(R.id.ratingBar);
        etFeedback = findViewById(R.id.etReview);
        btnSubmit = findViewById(R.id.btnSubmitReview);

        ratingBar.setIsIndicator(false);

        btnSubmit.setOnClickListener(v -> {
            float ratingValue = ratingBar.getRating();
            String feedbackText = etFeedback.getText().toString();

            if (ratingValue == 0) {
                Toast.makeText(this, "Please select a star rating!", Toast.LENGTH_SHORT).show();
                return;
            }

            if (donorId == null) {
                Toast.makeText(this, "Error: Donor not identified", Toast.LENGTH_SHORT).show();
                return;
            }

            // 3. SAVE DATA (Now includes Donor ID)
            Map<String, Object> review = new HashMap<>();
            review.put("donorId", donorId);      // <--- FIXED: Now MyReviewsActivity can find it
            review.put("foodId", foodId);
            review.put("reviewerId", FirebaseAuth.getInstance().getCurrentUser().getUid());
            review.put("rating", ratingValue);
            review.put("feedback", feedbackText); // Note: We use "feedback" here
            review.put("timestamp", System.currentTimeMillis());

            FirebaseFirestore.getInstance().collection("reviews").add(review)
                    .addOnSuccessListener(doc -> {
                        Toast.makeText(this, "Review Submitted!", Toast.LENGTH_SHORT).show();
                        finish();
                    })
                    .addOnFailureListener(e -> Toast.makeText(this, "Failed: " + e.getMessage(), Toast.LENGTH_SHORT).show());
        });
    }
}