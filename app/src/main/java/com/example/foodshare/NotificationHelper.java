package com.example.foodshare;

import com.google.firebase.firestore.FirebaseFirestore;
import java.util.UUID;

public class NotificationHelper {

    public static void send(String userId, String title, String message, String type, String targetId) {
        if (userId == null || userId.isEmpty()) return;

        String id = UUID.randomUUID().toString();
        // Ensure your NotificationModel matches this structure
        NotificationModel notif = new NotificationModel(
            id, title, message, System.currentTimeMillis(), false, type, targetId
        );

        FirebaseFirestore.getInstance()
            .collection("users").document(userId)
            .collection("notifications").document(id)
            .set(notif);
    }
}
