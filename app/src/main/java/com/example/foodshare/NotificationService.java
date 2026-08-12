package com.example.foodshare;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.Service;
import android.content.Intent;
import android.content.pm.ServiceInfo;
import android.os.Build;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.widget.Toast;
import androidx.annotation.Nullable;
import androidx.core.app.NotificationCompat;
import android.content.SharedPreferences; // Import added

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

public class NotificationService extends Service {

    private static final String CHANNEL_ID = "urgent_channel_v2";
    private static final String FG_CHANNEL_ID = "service_channel_v2";
    private boolean isRunning = false;
    private Handler mainHandler;

    @Override
    public void onCreate() {
        super.onCreate();
        mainHandler = new Handler(Looper.getMainLooper());
        createNotificationChannels();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        // promote to foreground to prevent kill
        try {
            if (Build.VERSION.SDK_INT >= 34) {
                // Use integer 1 for FOREGROUND_SERVICE_TYPE_DATA_SYNC to avoid compilation
                // issues on older SDKs
                // Ensure android.permission.FOREGROUND_SERVICE_DATA_SYNC is in manifest
                startForeground(1, createForegroundNotification(), 1);
            } else {
                startForeground(1, createForegroundNotification());
            }
        } catch (Exception e) {
            e.printStackTrace();
            // If startForeground fails, the service might be killed by the system, but we
            // shouldn't crash.
        }

        if (!isRunning) {
            startRealTimeSync();
        }
        return START_STICKY;
    }

    private Notification createForegroundNotification() {
        NotificationCompat.Builder builder = new NotificationCompat.Builder(this, FG_CHANNEL_ID)
                .setContentTitle("FoodShare Monitor")
                .setContentText("Checking for expirations...")
                .setSmallIcon(R.drawable.ic_notifications)
                .setPriority(NotificationCompat.PRIORITY_LOW);
        return builder.build();
    }

    private void startRealTimeSync() {
        isRunning = true;
        new Thread(() -> {
            while (isRunning) {
                try {
                    if (FirebaseAuth.getInstance().getCurrentUser() != null) {
                        checkUrgentEvents();
                        checkNewFoodListings(); // Added check for new foods
                    }
                    Thread.sleep(60000); // Check every 60 seconds
                } catch (InterruptedException e) {
                    break;
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }).start();
    }

    private void checkUrgentEvents() {
        FirebaseFirestore db = FirebaseFirestore.getInstance();
        long now = System.currentTimeMillis();

        // --- 1. FOOD EXPIRY CHECK ---
        db.collection("foods")
                .whereEqualTo("status", "Available")
                .whereLessThan("expiryTimestamp", now)
                .get()
                .addOnSuccessListener(snaps -> {
                    if (!snaps.isEmpty()) {
                        for (DocumentSnapshot doc : snaps) {
                            try {
                                doc.getReference().delete(); // Remove expired item
                                String donorId = doc.getString("donorId");
                                String title = doc.getString("title");
                                if (donorId != null) {
                                    checkAndSendPopup(donorId, "Food Expired", "Item expired: " + title, "expiry",
                                            doc.getId());
                                }
                            } catch (Exception e) {
                                e.printStackTrace();
                            }
                        }
                    }
                })
                .addOnFailureListener(e -> {
                    android.util.Log.e("NotificationService", "Expiry Check Failed: " + e.getMessage());
                });

        // --- 2. PICKUP REMINDER CHECK (10 Mins) ---
        if (FirebaseAuth.getInstance().getCurrentUser() == null)
            return;
        String currentUid = FirebaseAuth.getInstance().getCurrentUser().getUid();

        db.collection("reservations")
                .whereEqualTo("userId", currentUid)
                .whereEqualTo("status", "Ready")
                .get()
                .addOnSuccessListener(snaps -> {
                    if (snaps == null || snaps.isEmpty()) {
                        return;
                    }

                    for (DocumentSnapshot doc : snaps) {
                        try {
                            Reservation res = doc.toObject(Reservation.class);
                            if (res != null && res.getFoodId() != null) {
                                String reservationId = doc.getId();

                                db.collection("foods").document(res.getFoodId()).get()
                                        .addOnSuccessListener(foodSnap -> {
                                            if (foodSnap.exists()) {
                                                Long expiry = foodSnap.getLong("expiryTimestamp");
                                                if (expiry != null) {
                                                    long timeDiff = expiry - now;

                                                    // 0 < timeDiff <= 10 mins (600,000 ms)
                                                    if (timeDiff > 0 && timeDiff <= 600000) { // 10 minutes
                                                        checkAndSendPopup(currentUid, "Urgent Pickup",
                                                                "Your reservation for " + res.getItemName()
                                                                        + " expires in less than 10 mins!",
                                                                "reminder", reservationId);
                                                    }
                                                }
                                            }
                                        });
                            }
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                    }
                })
                .addOnFailureListener(e -> {
                    android.util.Log.e("NotificationService", "Reminder Check Failed: " + e.getMessage());
                });
    }

    private void checkNewFoodListings() {
        SharedPreferences prefs = getSharedPreferences("foodshare_prefs", MODE_PRIVATE);
        // Default to now if first run
        long lastCheck = prefs.getLong("last_food_check", System.currentTimeMillis());

        FirebaseFirestore.getInstance().collection("foods")
                .whereGreaterThan("pickupStartTimestamp", lastCheck)
                .get()
                .addOnSuccessListener(snaps -> {
                    if (snaps != null && !snaps.isEmpty()) {
                        long maxTimestamp = lastCheck;
                        String currentUid = FirebaseAuth.getInstance().getCurrentUser().getUid();

                        for (DocumentSnapshot doc : snaps) {
                            try {
                                FoodItem item = doc.toObject(FoodItem.class);
                                if (item == null)
                                    continue;

                                if (item.getPickupStartTimestamp() > maxTimestamp) {
                                    maxTimestamp = item.getPickupStartTimestamp();
                                }

                                // Filter: Available items only + Not my own items
                                if ("Available".equals(item.getStatus()) &&
                                        item.getDonorId() != null &&
                                        !item.getDonorId().equals(currentUid)) {

                                    checkAndSendPopup(currentUid, "New Food Available!",
                                            "New listing: " + item.getTitle(), "new_listing", item.getId());
                                }
                            } catch (Exception e) {
                                e.printStackTrace();
                            }
                        }

                        // Update last check to the latest timestamp found
                        prefs.edit().putLong("last_food_check", maxTimestamp).apply();
                    }
                })
                .addOnFailureListener(e -> {
                    android.util.Log.e("NotificationService", "New Food Check Failed: " + e.getMessage());
                });
    }

    private void checkAndSendPopup(String userId, String title, String msg, String type, String targetId) {
        // Prevent duplicate spam check using Firestore lookup
        FirebaseFirestore.getInstance().collection("users").document(userId)
                .collection("notifications")
                .whereEqualTo("targetId", targetId)
                .whereEqualTo("type", type)
                .get()
                .addOnSuccessListener(snaps -> {
                    if (snaps.isEmpty()) {
                        NotificationHelper.send(userId, title, msg, type, targetId);
                        showSystemNotification(title, msg);
                    }
                });
    }

    private void showSystemNotification(String title, String message) {
        try {
            NotificationCompat.Builder builder = new NotificationCompat.Builder(this, CHANNEL_ID)
                    .setSmallIcon(R.drawable.ic_notifications)
                    .setContentTitle(title)
                    .setContentText(message)
                    .setPriority(NotificationCompat.PRIORITY_HIGH)
                    .setDefaults(Notification.DEFAULT_ALL)
                    .setAutoCancel(true);

            NotificationManager manager = getSystemService(NotificationManager.class);
            if (manager != null)
                manager.notify((int) System.currentTimeMillis(), builder.build());
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void createNotificationChannels() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            try {
                NotificationManager manager = getSystemService(NotificationManager.class);

                // 1. High Priority Channel (Popups)
                NotificationChannel channel = new NotificationChannel(CHANNEL_ID, "Urgent Notifications",
                        NotificationManager.IMPORTANCE_HIGH);
                channel.enableVibration(true);
                manager.createNotificationChannel(channel);

                // 2. Service Channel (Silent)
                NotificationChannel serviceChannel = new NotificationChannel(FG_CHANNEL_ID, "Background Service",
                        NotificationManager.IMPORTANCE_LOW);
                manager.createNotificationChannel(serviceChannel);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        isRunning = false;
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }
}
