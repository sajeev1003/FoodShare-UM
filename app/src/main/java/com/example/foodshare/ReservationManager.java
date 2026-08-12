package com.example.foodshare;

import java.util.ArrayList;
import java.util.List;

public class ReservationManager {
    private static ReservationManager instance;
    private List<Reservation> reservations;

    private ReservationManager() {
        reservations = new ArrayList<>();
        // Add some dummy data initially if needed, or keep empty
        Reservation r1 = new Reservation("1", "Faculty of Engineering", "Fresh Vegetable Mix", "12:30 PM", "1 portion", "Ready", "#FS2024001", "dummy_user", "dummy_donor", "dummy_food_1");
        r1.setDocumentId("dummy_doc_1");
        reservations.add(r1);
        
        Reservation r2 = new Reservation("2", "Student Center", "Chicken Curry & Rice", "1:15 PM", "1 portion", "Completed", "#FS2024002", "dummy_user", "dummy_donor", "dummy_food_2");
        r2.setDocumentId("dummy_doc_2");
        reservations.add(r2);
    }

    public static synchronized ReservationManager getInstance() {
        if (instance == null) {
            instance = new ReservationManager();
        }
        return instance;
    }

    public List<Reservation> getReservations() {
        return reservations;
    }

    public void addReservation(Reservation reservation) {
        reservations.add(0, reservation); // Add to top
    }

    public boolean completeReservation(String pickupId) {
        for (Reservation res : reservations) {
            if (res.getPickupId().equals(pickupId)) {
                res.setStatus("Completed");
                return true;
            }
        }
        return false;
    }
}
