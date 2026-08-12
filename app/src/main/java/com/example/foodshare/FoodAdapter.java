package com.example.foodshare;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.bumptech.glide.Glide;
import com.google.firebase.auth.FirebaseAuth;
import java.util.List;

public class FoodAdapter extends RecyclerView.Adapter<FoodAdapter.ViewHolder> {

    private List<FoodItem> foodItems;
    private OnItemClickListener listener;
    private OnDeleteItemListener deleteListener;
    private OnEditItemListener editListener;
    private String currentUserId;

    public interface OnItemClickListener { void onItemClick(FoodItem item); }
    public interface OnDeleteItemListener { void onDeleteClick(FoodItem item); }
    public interface OnEditItemListener { void onEditClick(FoodItem item); }

    public FoodAdapter(List<FoodItem> foodItems, OnItemClickListener listener, OnDeleteItemListener deleteListener, OnEditItemListener editListener) {
        this.foodItems = foodItems;
        this.listener = listener;
        this.deleteListener = deleteListener;
        this.editListener = editListener;
        if (FirebaseAuth.getInstance().getCurrentUser() != null) {
            this.currentUserId = FirebaseAuth.getInstance().getCurrentUser().getUid();
        } else {
            this.currentUserId = ""; 
        }
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_food, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        FoodItem item = foodItems.get(position);
        
        if (holder.tvFoodTitle != null) {
            if (item.getTitle() != null) {
                holder.tvFoodTitle.setText(item.getTitle());
            } else {
                holder.tvFoodTitle.setText("Unknown Item");
            }
        }
        
        // Price Display logic
        String priceText = item.isFree() ? "FREE" : String.format("RM %.2f", item.getPrice());
        if (holder.tag1 != null) {
            holder.tag1.setText(priceText);
            holder.tag1.setBackgroundResource(item.isFree() ? R.drawable.tag_bg_green : R.drawable.tag_bg_orange);
            holder.tag1.setTextColor(item.isFree() ? 0xFF2E7D32 : 0xFFE65100);
        }

        String details = item.getLocation(); 
        if (details == null) details = "Location not available";
        if (holder.tvFoodDetails != null) {
            holder.tvFoodDetails.setText(details);
        }
        
        // Verification Badge Logic
        if (holder.imgVerifiedBadge != null) {
            if (item.isDonorVerified()) {
                holder.imgVerifiedBadge.setVisibility(View.VISIBLE);
            } else {
                holder.imgVerifiedBadge.setVisibility(View.GONE);
            }
        }

        // Delete Button & Edit Button Logic
        boolean isOwner = item.getDonorId() != null && item.getDonorId().equals(currentUserId);
        
        if (holder.btnDelete != null) {
            holder.btnDelete.setVisibility(isOwner ? View.VISIBLE : View.GONE);
            if (isOwner) holder.btnDelete.setOnClickListener(v -> deleteListener.onDeleteClick(item));
        }
        
        if (holder.btnEdit != null) {
            holder.btnEdit.setVisibility(isOwner ? View.VISIBLE : View.GONE);
            if (isOwner) holder.btnEdit.setOnClickListener(v -> editListener.onEditClick(item));
        }

        if (holder.imgFood != null) {
            if (item.getImageUri() != null) {
                Glide.with(holder.itemView.getContext())
                     .load(item.getImageUri())
                     .centerCrop()
                     .into(holder.imgFood);
            } else if (item.getImageResId() != 0) {
                try {
                    holder.imgFood.setImageResource(item.getImageResId());
                } catch (Exception e) {
                    holder.imgFood.setImageDrawable(null);
                    holder.imgFood.setBackgroundColor(0xFFEEEEEE);
                }
            } else {
                 holder.imgFood.setBackgroundColor(0xFFEEEEEE); // Fallback color
                 holder.imgFood.setImageDrawable(null);
                 //ddd
            }
        }

        holder.itemView.setOnClickListener(v -> listener.onItemClick(item));
    }

    @Override
    public int getItemCount() { return foodItems.size(); }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        ImageView imgFood, imgVerifiedBadge;
        TextView tvFoodTitle, tvFoodDetails, tag1;
        ImageButton btnDelete, btnEdit;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            imgFood = itemView.findViewById(R.id.imgFood);
            tvFoodTitle = itemView.findViewById(R.id.tvFoodTitle);
            tvFoodDetails = itemView.findViewById(R.id.tvFoodDetails);
            imgVerifiedBadge = itemView.findViewById(R.id.imgVerifiedBadge); 
            tag1 = itemView.findViewById(R.id.tag1);
            btnDelete = itemView.findViewById(R.id.btnDelete);
            btnEdit = itemView.findViewById(R.id.btnEdit);
        }
    }
}
