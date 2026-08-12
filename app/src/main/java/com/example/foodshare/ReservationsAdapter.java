package com.example.foodshare;

import android.graphics.Bitmap;
import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.google.zxing.BarcodeFormat;
import com.google.zxing.WriterException;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.qrcode.QRCodeWriter;

import java.util.List;

public class ReservationsAdapter extends RecyclerView.Adapter<ReservationsAdapter.ViewHolder> {

    private List<Reservation> reservations;

    public ReservationsAdapter(List<Reservation> reservations) {
        this.reservations = reservations;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_reservation, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        Reservation reservation = reservations.get(position);
        
        // Safety checks for null views
        if (holder.tvLocationName != null) holder.tvLocationName.setText(reservation.getLocationName());
        if (holder.tvItemName != null) holder.tvItemName.setText(reservation.getItemName());
        if (holder.tvTime != null) holder.tvTime.setText(reservation.getPickupTime());
        if (holder.tvPortion != null) holder.tvPortion.setText(reservation.getPortion());
        if (holder.tvPickupId != null) holder.tvPickupId.setText(reservation.getPickupId());
        
        String status = reservation.getStatus();
        if (holder.tvStatus != null) holder.tvStatus.setText(status);

        if ("Collected".equals(status) || "Expired".equals(status) || "Cancelled".equals(status)) {
            // 1. Hide the Cancel Button
            if (holder.btnCancel != null) holder.btnCancel.setVisibility(View.GONE);
            
            // 2. Disable Detail Button (View Only)
            if (holder.btnDetail != null) {
                holder.btnDetail.setEnabled(false);
                holder.btnDetail.setAlpha(0.5f); // Make it look grayed out
                holder.btnDetail.setText("View Only");
            }
    
            // 3. Hide QR Code (No black box)
            if (holder.imgQrCode != null) holder.imgQrCode.setVisibility(View.GONE);
            if (holder.tvQrPlaceholder != null) holder.tvQrPlaceholder.setVisibility(View.GONE); // Hide placeholder too if any
            
            // 4. Show Status Badge
            if (holder.tvStatus != null) {
                holder.tvStatus.setVisibility(View.VISIBLE);
                holder.tvStatus.setText(status);
                holder.tvStatus.setTextColor(Color.GRAY);
                holder.tvStatus.setBackgroundColor(Color.LTGRAY);
            }
            
            // Hide pickup section if needed or just QR
            if (holder.layoutPickup != null) holder.layoutPickup.setVisibility(View.GONE);

        } else if ("Ready".equalsIgnoreCase(status)) {
            // ACTIVE ITEMS (Ready)
            if (holder.btnCancel != null) holder.btnCancel.setVisibility(View.VISIBLE);
            if (holder.btnDetail != null) {
                holder.btnDetail.setVisibility(View.VISIBLE);
                holder.btnDetail.setEnabled(true);
                holder.btnDetail.setAlpha(1.0f);
                holder.btnDetail.setText("Detail");
            }
            
            if (holder.tvStatus != null) {
                holder.tvStatus.setTextColor(Color.parseColor("#2E7D32"));
                holder.tvStatus.setBackgroundResource(R.drawable.tag_bg_green);
            }
            
            if (holder.layoutPickup != null) holder.layoutPickup.setVisibility(View.VISIBLE);
            if (holder.tvQrPlaceholder != null) holder.tvQrPlaceholder.setVisibility(View.GONE);
            
            if (holder.imgQrCode != null) {
                holder.imgQrCode.setVisibility(View.VISIBLE);
                // Secure Display: Show Black Box initially
                holder.imgQrCode.setImageDrawable(new android.graphics.drawable.ColorDrawable(Color.BLACK));
                
                // Generate QR Code content
                String qrContent = reservation.getDocumentId();
                if (qrContent == null || qrContent.isEmpty()) {
                    qrContent = reservation.getPickupId(); // Fallback
                }
                
                if (qrContent != null) {
                    final String finalQrContent = qrContent;
                    holder.imgQrCode.setOnClickListener(v -> {
                        // Generate QR Code on click
                        Bitmap qrCode = generateQRCode(finalQrContent);
                        if (qrCode != null) {
                            android.app.AlertDialog.Builder builder = new android.app.AlertDialog.Builder(v.getContext());
                            ImageView imageView = new ImageView(v.getContext());
                            imageView.setImageBitmap(qrCode);
                            imageView.setPadding(32, 32, 32, 32);
                            imageView.setBackgroundColor(Color.WHITE);
                            builder.setView(imageView);
                            builder.setPositiveButton("Close", (dialog, which) -> dialog.dismiss());
                            builder.create().show();
                        }
                    });
                } else {
                    holder.imgQrCode.setOnClickListener(null);
                }
            }
        } else {
             // Other status e.g. Pending
             if (holder.btnCancel != null) holder.btnCancel.setVisibility(View.VISIBLE);
             if (holder.btnDetail != null) {
                 holder.btnDetail.setVisibility(View.VISIBLE);
                 holder.btnDetail.setEnabled(true);
                 holder.btnDetail.setAlpha(1.0f);
                 holder.btnDetail.setText("Detail");
             }
             
             // For other statuses, maybe hide QR or show placeholder
             if (holder.imgQrCode != null) holder.imgQrCode.setVisibility(View.GONE);
             if (holder.tvQrPlaceholder != null) holder.tvQrPlaceholder.setVisibility(View.VISIBLE);
        }

        holder.itemView.setOnClickListener(v -> {
            android.content.Context context = v.getContext();
            android.content.Intent intent = new android.content.Intent(context, ReservationDetailsActivity.class);
            intent.putExtra("ITEM_NAME", reservation.getItemName());
            intent.putExtra("LOCATION_NAME", reservation.getLocationName());
            intent.putExtra("PICKUP_ID", reservation.getPickupId());
            intent.putExtra("PICKUP_TIME", reservation.getPickupTime());
            intent.putExtra("PORTION", reservation.getPortion());
            intent.putExtra("DONOR_ID", reservation.getDonorId());
            intent.putExtra("FOOD_ID", reservation.getFoodId());
            intent.putExtra("RESERVATION_ID", reservation.getDocumentId());
            context.startActivity(intent);
        });

        if (holder.btnDetail != null) {
            holder.btnDetail.setOnClickListener(v -> {
                android.content.Context context = v.getContext();
                android.content.Intent intent = new android.content.Intent(context, ReservationDetailsActivity.class);
                intent.putExtra("ITEM_NAME", reservation.getItemName());
                intent.putExtra("LOCATION_NAME", reservation.getLocationName());
                intent.putExtra("PICKUP_ID", reservation.getPickupId());
                intent.putExtra("PICKUP_TIME", reservation.getPickupTime());
                intent.putExtra("PORTION", reservation.getPortion());
                intent.putExtra("DONOR_ID", reservation.getDonorId());
                intent.putExtra("FOOD_ID", reservation.getFoodId());
                intent.putExtra("RESERVATION_ID", reservation.getDocumentId());
                context.startActivity(intent);
            });
        }

        if (holder.btnCancel != null) {
            holder.btnCancel.setOnClickListener(v -> {
                new android.app.AlertDialog.Builder(v.getContext())
                        .setTitle("Cancel Reservation")
                        .setMessage("Do you really want to cancel this reservation?")
                        .setPositiveButton("Yes", (dialog, which) -> {
                            String docId = reservation.getDocumentId();
                            String foodId = reservation.getFoodId();
                            String portionStr = reservation.getPortion();

                            if (docId != null && !docId.isEmpty()) {
                                com.google.firebase.firestore.FirebaseFirestore db = com.google.firebase.firestore.FirebaseFirestore.getInstance();
                                
                                // Parse quantity to return
                                int qtyToReturn = 1;
                                if (portionStr != null) {
                                    try {
                                        String[] parts = portionStr.split(" ");
                                        if (parts.length > 0) {
                                            qtyToReturn = Integer.parseInt(parts[0]);
                                        }
                                    } catch (NumberFormatException e) {
                                        qtyToReturn = 1;
                                    }
                                }
                                final int finalQty = qtyToReturn;

                                if (foodId != null && !foodId.isEmpty()) {
                                    com.google.firebase.firestore.DocumentReference foodRef = db.collection("foods").document(foodId);
                                    com.google.firebase.firestore.DocumentReference resRef = db.collection("reservations").document(docId);

                                    db.runTransaction(transaction -> {
                                        com.google.firebase.firestore.DocumentSnapshot foodSnap = transaction.get(foodRef);
                                        
                                        // Delete reservation or mark cancelled
                                        transaction.update(resRef, "status", "Cancelled");
                                        
                                        // Update stock if item exists
                                        if (foodSnap.exists()) {
                                            Long currentQty = foodSnap.getLong("quantity");
                                            if (currentQty != null) {
                                                transaction.update(foodRef, "quantity", currentQty + finalQty);
                                            }
                                        }
                                        return null;
                                    }).addOnSuccessListener(aVoid -> {
                                        android.widget.Toast.makeText(v.getContext(), "Reservation Cancelled & Stock Returned", android.widget.Toast.LENGTH_SHORT).show();
                                        // Update locally or wait for listener
                                        reservation.setStatus("Cancelled");
                                        notifyItemChanged(holder.getBindingAdapterPosition());
                                    }).addOnFailureListener(e -> {
                                        android.widget.Toast.makeText(v.getContext(), "Failed to cancel: " + e.getMessage(), android.widget.Toast.LENGTH_SHORT).show();
                                    });
                                } else {
                                    // Fallback: Just update status
                                    db.collection("reservations").document(docId)
                                        .update("status", "Cancelled")
                                        .addOnSuccessListener(aVoid -> {
                                            android.widget.Toast.makeText(v.getContext(), "Reservation Cancelled", android.widget.Toast.LENGTH_SHORT).show();
                                            reservation.setStatus("Cancelled");
                                            notifyItemChanged(holder.getBindingAdapterPosition());
                                        })
                                        .addOnFailureListener(e -> {
                                            android.widget.Toast.makeText(v.getContext(), "Failed to cancel: " + e.getMessage(), android.widget.Toast.LENGTH_SHORT).show();
                                        });
                                }
                            } else {
                                // Handle dummy data or missing ID
                                 android.widget.Toast.makeText(v.getContext(), "Cannot cancel this item (Invalid ID)", android.widget.Toast.LENGTH_SHORT).show();
                            }
                        })
                        .setNegativeButton("No", null)
                        .show();
            });
        }
    }

    @Override
    public int getItemCount() {
        return reservations.size();
    }

    private Bitmap generateQRCode(String text) {
        QRCodeWriter writer = new QRCodeWriter();
        try {
            BitMatrix bitMatrix = writer.encode(text, BarcodeFormat.QR_CODE, 512, 512);
            int width = bitMatrix.getWidth();
            int height = bitMatrix.getHeight();
            Bitmap bmp = Bitmap.createBitmap(width, height, Bitmap.Config.RGB_565);
            for (int x = 0; x < width; x++) {
                for (int y = 0; y < height; y++) {
                    bmp.setPixel(x, y, bitMatrix.get(x, y) ? Color.BLACK : Color.WHITE); // White background
                }
            }
            return bmp;
        } catch (WriterException e) {
            e.printStackTrace();
            return null;
        }
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        TextView tvLocationName, tvItemName, tvTime, tvPortion, tvStatus, tvPickupId, tvQrPlaceholder;
        ImageView imgQrCode;
        android.widget.Button btnDetail, btnCancel;
        View layoutPickup;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            tvLocationName = itemView.findViewById(R.id.tvLocationName);
            tvItemName = itemView.findViewById(R.id.tvItemName);
            tvTime = itemView.findViewById(R.id.tvTime);
            tvPortion = itemView.findViewById(R.id.tvPortion);
            tvStatus = itemView.findViewById(R.id.tvStatus);
            tvPickupId = itemView.findViewById(R.id.tvPickupId);
            imgQrCode = itemView.findViewById(R.id.imgQrCode);
            layoutPickup = itemView.findViewById(R.id.layoutPickup);
            tvQrPlaceholder = itemView.findViewById(R.id.tvQrPlaceholder);
            btnDetail = itemView.findViewById(R.id.btnDetail);
            btnCancel = itemView.findViewById(R.id.btnCancel);
        }
    }
}