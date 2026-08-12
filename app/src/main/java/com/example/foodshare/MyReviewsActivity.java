package com.example.foodshare;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.RatingBar;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import java.util.ArrayList;
import java.util.List;

public class MyReviewsActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_my_reviews);

        if(getSupportActionBar() != null) getSupportActionBar().hide();

        RecyclerView rv = findViewById(R.id.rvReviews);
        TextView tvNoReviews = findViewById(R.id.tvNoReviews);

        rv.setLayoutManager(new LinearLayoutManager(this));

        String uid = FirebaseAuth.getInstance().getCurrentUser().getUid();
        List<ReviewModel> reviews = new ArrayList<>();

        RecyclerView.Adapter adapter = new RecyclerView.Adapter<ReviewHolder>() {
            @NonNull @Override public ReviewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
                // Ensure you have item_review.xml created (see step 3 below)
                View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_review, parent, false);
                return new ReviewHolder(v);
            }
            @Override public void onBindViewHolder(@NonNull ReviewHolder holder, int position) {
                ReviewModel r = reviews.get(position);
                holder.rating.setRating(r.rating);
                holder.comment.setText(r.comment);
            }
            @Override public int getItemCount() { return reviews.size(); }
        };
        rv.setAdapter(adapter);

        FirebaseFirestore.getInstance().collection("reviews")
                .whereEqualTo("donorId", uid) // Filters correctly now
                .get()
                .addOnSuccessListener(snaps -> {
                    reviews.clear();
                    for(QueryDocumentSnapshot doc : snaps) {
                        ReviewModel r = new ReviewModel();
                        r.rating = doc.getDouble("rating").floatValue();

                        // --- FIX: Read "feedback", NOT "comment" ---
                        r.comment = doc.getString("feedback");
                        if (r.comment == null) r.comment = "";

                        reviews.add(r);
                    }

                    if (reviews.isEmpty()) {
                        tvNoReviews.setVisibility(View.VISIBLE);
                        rv.setVisibility(View.GONE);
                    } else {
                        tvNoReviews.setVisibility(View.GONE);
                        rv.setVisibility(View.VISIBLE);
                    }

                    adapter.notifyDataSetChanged();
                });
    }

    static class ReviewHolder extends RecyclerView.ViewHolder {
        RatingBar rating; TextView comment;
        public ReviewHolder(View v) {
            super(v);
            // Matches IDs in item_review.xml
            rating = v.findViewById(R.id.rbItemRating);
            comment = v.findViewById(R.id.tvItemFeedback);
        }
    }

    static class ReviewModel { float rating; String comment; }
}