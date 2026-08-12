package com.example.foodshare;

import java.util.ArrayList;
import java.util.List;

public class FoodManager {
    private static FoodManager instance;
    private List<FoodItem> foodItems;

    private FoodManager() {
        foodItems = new ArrayList<>();
        // Add dummy data
        foodItems.add(new FoodItem("1", "Fresh Vegetable Mix", "A mix of fresh vegetables from the garden.", "Faculty of Engineering", R.drawable.food_veg_mix, "2 hours ago", "John Doe", true, true, true, true, true, 5, "Today, 5:00 PM - 7:00 PM", "dummy_donor"));
        foodItems.add(new FoodItem("2", "Chicken Curry & Rice", "Delicious homemade chicken curry.", "Student Center", R.drawable.food_nasi_lemak, "4 hours ago", "Jane Smith", true, false, false, false, true, 3, "Today, 12:00 PM - 2:00 PM", "dummy_donor"));
        foodItems.add(new FoodItem("3", "Caesar Salad", "Healthy salad with dressing on the side.", "Library Cafe", R.drawable.food_caesar_salad, "5 hours ago", "Mike Ross", false, true, false, true, false, 2, "Today, 4:00 PM - 6:00 PM", "dummy_donor"));
    }

    public static synchronized FoodManager getInstance() {
        if (instance == null) {
            instance = new FoodManager();
        }
        return instance;
    }

    public List<FoodItem> getFoodItems() {
        return foodItems;
    }

    public void addFoodItem(FoodItem item) {
        foodItems.add(0, item); // Add to top
    }

    public FoodItem getFoodItem(String id) {
        for (FoodItem item : foodItems) {
            if (item.getId().equals(id)) {
                return item;
            }
        }
        return null;
    }
    
    public boolean decrementQuantity(String foodId, int amount) {
        FoodItem item = getFoodItem(foodId);
        if (item != null && item.getQuantity() >= amount) {
            item.setQuantity(item.getQuantity() - amount);
            return true;
        }
        return false;
    }
}
