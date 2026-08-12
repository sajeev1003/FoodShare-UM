package com.example.foodshare;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.location.Location;
import android.os.Bundle;
import android.util.DisplayMetrics;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.PopupWindow;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.widget.SearchView;
import androidx.core.app.ActivityCompat;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.gms.location.LocationServices;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.android.material.tabs.TabLayout;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.ListenerRegistration;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

public class HomeFragment extends Fragment {

    private RecyclerView recyclerView;
    private FoodAdapter adapter;
    private List<FoodItem> fullList = new ArrayList<>();
    private List<FoodItem> displayList = new ArrayList<>();
    private TabLayout tabLayout;
    private String currentUserId;
    private Location userLocation;

    // Notification UI
    private TextView tvNotificationBadge;
    private ImageView btnNotification;
    private ListenerRegistration badgeListener;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_home, container, false);

        if (FirebaseAuth.getInstance().getCurrentUser() != null) {
            currentUserId = FirebaseAuth.getInstance().getCurrentUser().getUid();
        } else {
            currentUserId = "";
        }

        // --- Notification UI Setup ---
        btnNotification = view.findViewById(R.id.btnNotification);
        tvNotificationBadge = view.findViewById(R.id.tvNotificationBadge); 

        if (btnNotification != null) {
            btnNotification.setOnClickListener(this::showNotificationDrawer);
            // Start listening for unread messages
            setupNotificationBadge();
        }

        // Fetch location
        if (ActivityCompat.checkSelfPermission(getContext(), Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            LocationServices.getFusedLocationProviderClient(getActivity())
                    .getLastLocation()
                    .addOnSuccessListener(loc -> userLocation = loc);
        }

        // Setup Recycler
        recyclerView = view.findViewById(R.id.recyclerViewFood);
        recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));

        adapter = new FoodAdapter(displayList, item -> {
            if (item.getDonorId() != null && item.getDonorId().equals(currentUserId)) {
                Toast.makeText(getContext(), "You cannot reserve your own listing.", Toast.LENGTH_SHORT).show();
            } else {
                Intent intent = new Intent(getActivity(), ReserveItemActivity.class);
                intent.putExtra("FOOD_ID", item.getId());
                startActivity(intent);
            }
        }, this::deleteFoodItem, this::editFoodItem);

        recyclerView.setAdapter(adapter);

        // Dashboard Click Listeners
        view.findViewById(R.id.cardShare).setOnClickListener(v ->
                startActivity(new Intent(getActivity(), ShareFoodActivity.class))
        );

        view.findViewById(R.id.cardMap).setOnClickListener(v -> {
            if (getActivity() instanceof MainActivity) {
                BottomNavigationView nav = getActivity().findViewById(R.id.bottomNavigationView);
                if (nav != null) {
                    nav.setSelectedItemId(R.id.nav_search);
                }
            }
        });

        view.findViewById(R.id.btnFilter).setOnClickListener(v -> showSortDialog());

        // Search Logic
        SearchView searchView = view.findViewById(R.id.searchView);
        searchView.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
            @Override
            public boolean onQueryTextSubmit(String query) { filter(query); return true; }
            @Override
            public boolean onQueryTextChange(String newText) { filter(newText); return true; }
        });

        // Tab Logic
        tabLayout = view.findViewById(R.id.tabLayout);
        tabLayout.addOnTabSelectedListener(new TabLayout.OnTabSelectedListener() {
            @Override public void onTabSelected(TabLayout.Tab tab) { loadFoodItems(tab.getPosition()); }
            @Override public void onTabUnselected(TabLayout.Tab tab) {}
            @Override public void onTabReselected(TabLayout.Tab tab) {}
        });

        return view;
    }

    // 1. Red Dot Listener (No Numbers, just Visible/Gone)
    private void setupNotificationBadge() {
        if (currentUserId.isEmpty()) return;

        if (badgeListener != null) {
            badgeListener.remove();
        }

        badgeListener = FirebaseFirestore.getInstance().collection("users").document(currentUserId)
                .collection("notifications")
                .whereEqualTo("isRead", false) // Only get unread
                .addSnapshotListener((value, error) -> {
                    // If error or value is null, hide immediately
                    if (error != null || value == null) {
                        tvNotificationBadge.setVisibility(View.GONE);
                        return;
                    }

                    // STRICT CHECK: Only show if count > 0
                    if (!value.isEmpty()) {
                        tvNotificationBadge.setText(""); // No numbers
                        tvNotificationBadge.setVisibility(View.VISIBLE); // Show Dot
                    } else {
                        tvNotificationBadge.setVisibility(View.GONE);    // Hide Dot
                    }
                });
    }

    // 2. Notification Inbox (Click to Mark Read)
    private void showNotificationDrawer(View anchor) {
        if (getContext() == null || currentUserId == null) return;

        try {
            // 0. Container for Reference
            final android.widget.PopupWindow[] popupWindowRef = new android.widget.PopupWindow[1];

            // 1. Inflate the layout
            View popupView = LayoutInflater.from(getContext()).inflate(R.layout.popup_notifications, null);
            LinearLayout listContainer = popupView.findViewById(R.id.notificationList);
            TextView btnMarkAll = popupView.findViewById(R.id.btnMarkAllRead);

            // MARK ALL READ LOGIC
            btnMarkAll.setOnClickListener(v -> {
                FirebaseFirestore db = FirebaseFirestore.getInstance();
                db.collection("users").document(currentUserId).collection("notifications")
                    .whereEqualTo("isRead", false)
                    .get()
                    .addOnSuccessListener(unreadSnaps -> {
                        if (unreadSnaps.isEmpty()) {
                            Toast.makeText(getContext(), "All caught up!", Toast.LENGTH_SHORT).show();
                            return;
                        }
                        
                        // Batch Write for performance
                        com.google.firebase.firestore.WriteBatch batch = db.batch();
                        for (DocumentSnapshot doc : unreadSnaps) {
                            batch.update(doc.getReference(), "isRead", true, "read", true);
                        }
                        
                        batch.commit().addOnSuccessListener(aVoid -> {
                            Toast.makeText(getContext(), "All marked as read", Toast.LENGTH_SHORT).show();
                            // Close popup to force refresh
                            if (popupWindowRef[0] != null && popupWindowRef[0].isShowing()) {
                                popupWindowRef[0].dismiss();
                            }
                        });
                    });
            });

            if (listContainer == null) {
                Toast.makeText(getContext(), "Error: notificationList ID missing in XML", Toast.LENGTH_SHORT).show();
                return;
            }

            // 2. Load Notifications (Standard List, No Dynamic Buttons)
            FirebaseFirestore.getInstance().collection("users").document(currentUserId)
                    .collection("notifications")
                    .orderBy("timestamp", Query.Direction.DESCENDING)
                    .limit(100)
                    .get()
                    .addOnSuccessListener(snaps -> {
                        // Check if fragment is still attached
                        if (!isAdded() || getContext() == null) return;

                        listContainer.removeAllViews(); // Clear any dummy views

                        if (snaps.isEmpty()) {
                            TextView empty = new TextView(getContext());
                            empty.setText("No notifications");
                            empty.setPadding(30, 30, 30, 30);
                            empty.setTextColor(Color.GRAY);
                            listContainer.addView(empty);
                        } else {
                            for (DocumentSnapshot doc : snaps) {
                                try {
                                    NotificationModel notif = doc.toObject(NotificationModel.class);
                                    if (notif == null) continue;

                                    // ROBUST CHECK: Check both known fields
                                    boolean isRead = false;
                                    if (doc.contains("isRead")) {
                                        Boolean val = doc.getBoolean("isRead");
                                        if (val != null) isRead = val;
                                    } else if (doc.contains("read")) {
                                        Boolean val = doc.getBoolean("read");
                                        if (val != null) isRead = val;
                                    }

                                    View itemView = LayoutInflater.from(getContext()).inflate(R.layout.item_notification, listContainer, false);
                                    TextView title = itemView.findViewById(R.id.tvNotifTitle);
                                    TextView msg = itemView.findViewById(R.id.tvNotifMessage);

                                    title.setText(notif.getTitle());
                                    msg.setText(notif.getMessage());

                                    // COLOR LOGIC: Blue = Unread
                                    if (!isRead) {
                                        itemView.setBackgroundColor(Color.parseColor("#E3F2FD")); // Blue tint
                                        title.setTextColor(Color.BLACK);
                                        title.setTypeface(null, android.graphics.Typeface.BOLD);
                                        msg.setTextColor(Color.parseColor("#424242")); // Darker Gray
                                    } else {
                                        itemView.setBackgroundColor(Color.WHITE);
                                        title.setTextColor(Color.LTGRAY); // Light Gray (Faded)
                                        title.setTypeface(null, android.graphics.Typeface.NORMAL);
                                        msg.setTextColor(Color.parseColor("#E0E0E0")); // Very Light Gray
                                    }

                                    // CLICK LISTENER: Mark Read
                                    final boolean finalIsRead = isRead;
                                    itemView.setOnClickListener(v -> {
                                        if (!finalIsRead) {
                                            // Update UI immediately
                                            itemView.setBackgroundColor(Color.WHITE);
                                            title.setTextColor(Color.LTGRAY);
                                            title.setTypeface(null, android.graphics.Typeface.NORMAL);
                                            msg.setTextColor(Color.parseColor("#E0E0E0"));
                                            
                                            // Update Firestore - UPDATE BOTH FIELDS to be safe
                                            doc.getReference().update("isRead", true, "read", true)
                                                .addOnSuccessListener(aVoid -> {
                                                     // Success
                                                })
                                                .addOnFailureListener(e -> {
                                                    Toast.makeText(getContext(), "Failed to update: " + e.getMessage(), Toast.LENGTH_LONG).show();
                                                });
                                        }
                                    });

                                    listContainer.addView(itemView);
                                } catch (Exception e) {
                                    e.printStackTrace();
                                }
                            }
                        }
                    })
                    .addOnFailureListener(e -> {
                        if (getContext() != null) {
                            Toast.makeText(getContext(), "Failed to load: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                        }
                    });

            // 3. Show Popup
            DisplayMetrics displayMetrics = getResources().getDisplayMetrics();
            int width = (int) (displayMetrics.widthPixels * 0.9);
            int height = ViewGroup.LayoutParams.WRAP_CONTENT;

            popupWindowRef[0] = new PopupWindow(popupView, width, height, true);
            popupWindowRef[0].setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
            popupWindowRef[0].setElevation(20);
            popupWindowRef[0].setOutsideTouchable(true);
            
            // Calculate proper offset
            int xOff = -(width - anchor.getWidth()); 
            
            // Check if anchor is still valid
            if (anchor.getWindowToken() != null) {
                popupWindowRef[0].showAsDropDown(anchor, xOff, 10);
            }

        } catch (Exception e) {
            // THIS PREVENTS THE APP FROM CLOSING
            e.printStackTrace();
            if (getContext() != null) {
                Toast.makeText(getContext(), "Crash blocked: " + e.getMessage(), Toast.LENGTH_LONG).show();
            }
        }
    }

    private void showSortDialog() {
        String[] options = {"Freshness (Newest First)", "Price (Low to High)", "Distance (Nearest First)"};
        new AlertDialog.Builder(getContext())
                .setTitle("Sort By")
                .setItems(options, (dialog, which) -> {
                    switch (which) {
                        case 0: 
                            // Freshness: Sort by Pickup Start Time (Newest/Latest First)
                            Collections.sort(displayList, (o1, o2) -> Long.compare(o2.getPickupStartTimestamp(), o1.getPickupStartTimestamp()));
                            break;
                        case 1: Collections.sort(displayList, Comparator.comparingDouble(FoodItem::getPrice)); break;
                        case 2:
                            if (userLocation == null) {
                                Toast.makeText(getContext(), "Location not available", Toast.LENGTH_SHORT).show();
                                return;
                            }
                            Collections.sort(displayList, (o1, o2) -> {
                                float[] result1 = new float[1];
                                float[] result2 = new float[1];
                                Location.distanceBetween(userLocation.getLatitude(), userLocation.getLongitude(), o1.getLatitude(), o1.getLongitude(), result1);
                                Location.distanceBetween(userLocation.getLatitude(), userLocation.getLongitude(), o2.getLatitude(), o2.getLongitude(), result2);
                                return Float.compare(result1[0], result2[0]);
                            });
                            break;
                    }
                    adapter.notifyDataSetChanged();
                })
                .show();
    }

    private void editFoodItem(FoodItem item) {
        Intent intent = new Intent(getActivity(), ShareFoodActivity.class);
        intent.putExtra("EDIT_MODE", true);
        intent.putExtra("FOOD_ID", item.getId());
        startActivity(intent);
    }

    private void deleteFoodItem(FoodItem item) {
        new androidx.appcompat.app.AlertDialog.Builder(getContext())
                .setTitle("Delete Listing")
                .setMessage("Are you sure?")
                .setPositiveButton("Delete", (dialog, which) -> {
                    FirebaseFirestore.getInstance().collection("foods").document(item.getId()).delete()
                            .addOnSuccessListener(v -> {
                                Toast.makeText(getContext(), "Deleted", Toast.LENGTH_SHORT).show();
                                loadFoodItems(tabLayout.getSelectedTabPosition());
                            });
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    @Override
    public void onResume() {
        super.onResume();
        if (tabLayout != null) loadFoodItems(tabLayout.getSelectedTabPosition());
        else loadFoodItems(0);
    }
    
    @Override
    public void onDestroyView() {
        super.onDestroyView();
        // Clean up Firestore listener to prevent leaks and crashes
        if (badgeListener != null) {
            badgeListener.remove();
        }
    }

    private void loadFoodItems(int tabIndex) {
        FirebaseFirestore.getInstance().collection("foods").get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    fullList.clear();
                    for (QueryDocumentSnapshot doc : queryDocumentSnapshots) {
                        try {
                            FoodItem item = doc.toObject(FoodItem.class);
                            if (item.getDonorId() == null) continue;
                            if (tabIndex == 0) {
                                if (!item.getDonorId().equals(currentUserId)) fullList.add(item);
                            } else {
                                if (item.getDonorId().equals(currentUserId)) fullList.add(item);
                            }
                        } catch (Exception e) {}
                    }
                    SearchView sv = getView() != null ? getView().findViewById(R.id.searchView) : null;
                    if (sv != null && sv.getQuery().length() > 0) {
                        filter(sv.getQuery().toString());
                    } else {
                        displayList.clear();
                        displayList.addAll(fullList);
                        adapter.notifyDataSetChanged();
                    }
                });
    }

    private void filter(String text) {
        displayList.clear();
        if (text == null || text.isEmpty()) {
            displayList.addAll(fullList);
        } else {
            for (FoodItem item : fullList) {
                if (item.getTitle() != null && item.getTitle().toLowerCase().contains(text.toLowerCase())) {
                    displayList.add(item);
                }
            }
        }
        adapter.notifyDataSetChanged();
    }
}