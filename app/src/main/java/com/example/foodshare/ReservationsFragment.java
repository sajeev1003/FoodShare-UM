package com.example.foodshare;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.tabs.TabLayout;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentChange;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.ListenerRegistration;
import com.google.firebase.firestore.Query;

import java.util.ArrayList;
import java.util.List;

public class ReservationsFragment extends Fragment {

    private RecyclerView recyclerView;
    private ReservationsAdapter adapter;
    private List<Reservation> list = new ArrayList<>();
    private TabLayout tabLayout;
    private ListenerRegistration firestoreListener;
    private TextView tvEmpty;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        // SAFETY CHECK: Handle crash if layout is missing
        View view = inflater.inflate(R.layout.fragment_reservations, container, false);

        try {
            tabLayout = view.findViewById(R.id.tabLayoutReservations);
            recyclerView = view.findViewById(R.id.recyclerReservations);
            tvEmpty = view.findViewById(R.id.tvEmpty); // It's okay if this is null in XML

            recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
            adapter = new ReservationsAdapter(list);
            // Note: If your Adapter constructor requires Context, change above line to:
            // adapter = new ReservationsAdapter(list, getContext());
            recyclerView.setAdapter(adapter);

            // Tab Selection Logic
            tabLayout.addOnTabSelectedListener(new TabLayout.OnTabSelectedListener() {
                @Override public void onTabSelected(TabLayout.Tab tab) { loadData(tab.getPosition()); }
                @Override public void onTabUnselected(TabLayout.Tab tab) {}
                @Override public void onTabReselected(TabLayout.Tab tab) {}
            });

            // Load Active tabs by default
            loadData(0);

        } catch (Exception e) {
            e.printStackTrace();
            Toast.makeText(getContext(), "Error init Orders: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        }

        return view;
    }

    private void loadData(int tabIndex) {
        if (FirebaseAuth.getInstance().getCurrentUser() == null) return;
        String uid = FirebaseAuth.getInstance().getCurrentUser().getUid();

        // 1. Clear previous listeners
        if (firestoreListener != null) firestoreListener.remove();

        // 2. Query ALL reservations for this user
        Query query = FirebaseFirestore.getInstance().collection("reservations")
                .whereEqualTo("userId", uid);

        firestoreListener = query.addSnapshotListener((value, error) -> {
            if (error != null) {
                // Log error but don't crash
                return;
            }
            if (value == null) return;

            // --- PART A: CHECK FOR FEEDBACK TRIGGER (Safe Mode) ---
            try {
                for (DocumentChange dc : value.getDocumentChanges()) {
                    if (dc.getType() == DocumentChange.Type.MODIFIED) {
                        Reservation res = dc.getDocument().toObject(Reservation.class);
                        if ("Collected".equalsIgnoreCase(res.getStatus())) {
                            Intent intent = new Intent(getContext(), ReviewActivity.class);
                            intent.putExtra("DONOR_ID", res.getDonorId());
                            intent.putExtra("FOOD_ID", res.getFoodId());
                            startActivity(intent);
                        }
                    }
                }
            } catch (Exception e) {
                // Ignore feedback errors to keep list running
            }

            // --- PART B: REBUILD THE LIST ---
            list.clear();
            for (com.google.firebase.firestore.DocumentSnapshot doc : value.getDocuments()) {
                try {
                    Reservation res = doc.toObject(Reservation.class);
                    if (res == null) continue;

                    // Ensure ID is set safely
                    if (res.getDocumentId() == null) res.setDocumentId(doc.getId());

                    String status = res.getStatus();
                    if (status == null) status = "Ready";

                    boolean isCompleted = "Collected".equalsIgnoreCase(status)
                            || "Expired".equalsIgnoreCase(status)
                            || "Cancelled".equalsIgnoreCase(status);

                    if (tabIndex == 0 && !isCompleted) {
                        list.add(res);
                    } else if (tabIndex == 1 && isCompleted) {
                        list.add(res);
                    }
                } catch (Exception e) {
                    // Skip one bad item instead of crashing app
                    e.printStackTrace();
                }
            }

            adapter.notifyDataSetChanged();

            if (tvEmpty != null) {
                tvEmpty.setVisibility(list.isEmpty() ? View.VISIBLE : View.GONE);
            }
        });
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (firestoreListener != null) firestoreListener.remove();
    }
}