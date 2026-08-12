package com.example.foodshare;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Address;
import android.location.Geocoder;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.view.View;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.ImageView;
import android.widget.RadioGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.core.content.FileProvider;

import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;

import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

public class ShareFoodActivity extends AppCompatActivity {

    private ImageView imgUpload;
    private View layoutUploadPlaceholder;
    private TextInputEditText etTitle, etDescription, etLocation, etQuantity, etStartTime, etEndTime, etPrice;
    private TextInputLayout layoutPrice;
    private RadioGroup rgType;
    private CheckBox cbHalal, cbVegetarian, cbVegan, cbNutFree, cbDairyFree;
    
    private Uri selectedImageUri;
    private Uri photoUri; // For Camera
    
    private double currentLat = 0.0;
    private double currentLng = 0.0;
    private Calendar expiryCalendar;
    private FusedLocationProviderClient fusedLocationClient;

    private boolean isEditMode = false;
    private String editingFoodId = null;
    private String existingImageUri = null;

    // --- ACTIVITY LAUNCHERS ---
    
    // 1. Gallery
    private final ActivityResultLauncher<String> pickImage = registerForActivityResult(
            new ActivityResultContracts.GetContent(),
            uri -> {
                if (uri != null) {
                    selectedImageUri = uri;
                    imgUpload.setImageURI(uri);
                    layoutUploadPlaceholder.setVisibility(View.GONE);
                }
            }
    );

    // 2. Camera
    private final ActivityResultLauncher<Uri> takePicture = registerForActivityResult(
            new ActivityResultContracts.TakePicture(),
            isSuccess -> {
                if (isSuccess && photoUri != null) {
                    selectedImageUri = photoUri;
                    imgUpload.setImageURI(photoUri);
                    layoutUploadPlaceholder.setVisibility(View.GONE);
                }
            }
    );

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_share_food);
        if (getSupportActionBar() != null) getSupportActionBar().hide();

        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this);
        initViews();
        setupListeners();

        if (getIntent().getBooleanExtra("EDIT_MODE", false)) {
            isEditMode = true;
            editingFoodId = getIntent().getStringExtra("FOOD_ID");
            loadDataForEdit();
            View header = findViewById(R.id.header);
            if (header != null) {
                TextView title = header.findViewById(R.id.tvAppTitle);
                if (title != null) title.setText("Edit Listing");
            }
            Button btnConfirm = findViewById(R.id.btnConfirmShare);
            if (btnConfirm != null) btnConfirm.setText("Update Listing");
        }
    }

    private void initViews() {
        imgUpload = findViewById(R.id.imgUpload);
        layoutUploadPlaceholder = findViewById(R.id.layoutUploadPlaceholder);
        etTitle = findViewById(R.id.etTitle);
        etDescription = findViewById(R.id.etDescription);
        etLocation = findViewById(R.id.etLocation);
        etQuantity = findViewById(R.id.etQuantity);
        etStartTime = findViewById(R.id.etStartTime);
        etEndTime = findViewById(R.id.etEndTime);
        etPrice = findViewById(R.id.etPrice);
        layoutPrice = findViewById(R.id.layoutPrice);
        rgType = findViewById(R.id.rgType);

        cbHalal = findViewById(R.id.cbHalal);
        cbVegetarian = findViewById(R.id.cbVegetarian);
        cbVegan = findViewById(R.id.cbVegan);
        cbNutFree = findViewById(R.id.cbNutFree);
        cbDairyFree = findViewById(R.id.cbDairyFree);
    }

    private void setupListeners() {
        findViewById(R.id.btnBack).setOnClickListener(v -> finish());
        
        // CLICK TO ADD IMAGE -> SHOW DIALOG
        imgUpload.setOnClickListener(v -> showImageSourceDialog());

        rgType.setOnCheckedChangeListener((group, checkedId) -> {
            if (checkedId == R.id.rbDiscounted) layoutPrice.setVisibility(View.VISIBLE);
            else layoutPrice.setVisibility(View.GONE);
        });

        findViewById(R.id.btnDetectLocation).setOnClickListener(v -> detectLocation());

        findViewById(R.id.btnConfirmShare).setOnClickListener(v -> {
            // Calling saveFoodItem directly; validation is inside
            saveFoodItem();
        });

        etStartTime.setOnClickListener(v -> showTimePicker(etStartTime));
        etEndTime.setOnClickListener(v -> showTimePicker(etEndTime));
    }

    // --- DIALOG FOR CAMERA vs GALLERY ---
    private void showImageSourceDialog() {
        String[] options = {"Take Photo", "Choose from Gallery"};
        new AlertDialog.Builder(this)
                .setTitle("Select Image")
                .setItems(options, (dialog, which) -> {
                    if (which == 0) {
                        checkCameraPermission();
                    } else {
                        checkStoragePermission();
                    }
                })
                .show();
    }

    private void checkCameraPermission() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED) {
            launchCamera();
        } else {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.CAMERA}, 200);
        }
    }

    private void checkStoragePermission() {
        String perm;
        if (Build.VERSION.SDK_INT >= 33) {
            perm = Manifest.permission.READ_MEDIA_IMAGES;
        } else {
            perm = Manifest.permission.READ_EXTERNAL_STORAGE;
        }

        if (ContextCompat.checkSelfPermission(this, perm) == PackageManager.PERMISSION_GRANTED) {
            pickImage.launch("image/*");
        } else {
            ActivityCompat.requestPermissions(this, new String[]{perm}, 300);
        }
    }

    private void launchCamera() {
        try {
            File photoFile = createImageFile();
            if (photoFile != null) {
                photoUri = FileProvider.getUriForFile(this,
                        getApplicationContext().getPackageName() + ".fileprovider",
                        photoFile);
                takePicture.launch(photoUri);
            }
        } catch (IOException ex) {
            Toast.makeText(this, "Error creating file", Toast.LENGTH_SHORT).show();
        }
    }

    private File createImageFile() throws IOException {
        String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(new Date());
        String imageFileName = "JPEG_" + timeStamp + "_";
        File storageDir = getExternalFilesDir(Environment.DIRECTORY_PICTURES);
        return File.createTempFile(imageFileName, ".jpg", storageDir);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == 200) { // Camera
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                launchCamera();
            } else {
                Toast.makeText(this, "Camera permission needed", Toast.LENGTH_SHORT).show();
            }
        } else if (requestCode == 300) { // Storage
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                pickImage.launch("image/*");
            } else {
                Toast.makeText(this, "Storage permission needed", Toast.LENGTH_SHORT).show();
            }
        } else if (requestCode == 100) { // Location
             if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                fetchLocation();
            } else {
                Toast.makeText(this, "Permission denied. Location needed for this feature.", Toast.LENGTH_SHORT).show();
            }
        }
    }

    // --- LOCATION LOGIC ---
    private void detectLocation() {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, 100);
        } else {
            fetchLocation();
        }
    }

    private void fetchLocation() {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            fusedLocationClient.getLastLocation().addOnSuccessListener(this, location -> {
                if (location != null) {
                    currentLat = location.getLatitude();
                    currentLng = location.getLongitude();
                    Geocoder geocoder = new Geocoder(this, Locale.getDefault());
                    try {
                        List<Address> addresses = geocoder.getFromLocation(currentLat, currentLng, 1);
                        if (addresses != null && !addresses.isEmpty()) {
                            etLocation.setText(addresses.get(0).getAddressLine(0));
                        } else {
                            etLocation.setText(currentLat + ", " + currentLng);
                        }
                    } catch (IOException e) {
                        etLocation.setText(currentLat + ", " + currentLng);
                    }
                } else {
                    Toast.makeText(this, "Please enable GPS", Toast.LENGTH_SHORT).show();
                }
            });
        }
    }
    
    private void showTimePicker(TextInputEditText editText) {
        Calendar c = Calendar.getInstance();
        new android.app.TimePickerDialog(this, (view, h, m) -> {
            String time = String.format(Locale.getDefault(), "%02d:%02d", h, m);
            editText.setText(time);
            if (editText.getId() == R.id.etEndTime) {
                expiryCalendar = Calendar.getInstance();
                expiryCalendar.set(Calendar.HOUR_OF_DAY, h);
                expiryCalendar.set(Calendar.MINUTE, m);
                if (expiryCalendar.before(Calendar.getInstance())) expiryCalendar.add(Calendar.DAY_OF_YEAR, 1);
            }
        }, c.get(Calendar.HOUR_OF_DAY), c.get(Calendar.MINUTE), true).show();
    }

    private boolean validateInput() {
        if (etTitle.getText().toString().isEmpty()) {
            Toast.makeText(this, "Please enter a title", Toast.LENGTH_SHORT).show();
            return false;
        }
        if (rgType.getCheckedRadioButtonId() == R.id.rbDiscounted && etPrice.getText().toString().isEmpty()) {
             Toast.makeText(this, "Please enter a price", Toast.LENGTH_SHORT).show();
             return false;
        }
        if (etQuantity.getText().toString().isEmpty()) {
            Toast.makeText(this, "Please enter quantity", Toast.LENGTH_SHORT).show();
            return false;
        }
        return true;
    }

    // --- REFACTORED SAVE LOGIC: EXPLICIT BUCKET + STREAM + VERIFICATION ---
    private void saveFoodItem() {
        if (!validateInput()) return;
        
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user == null) {
            Toast.makeText(this, "You are not logged in! Please login first.", Toast.LENGTH_LONG).show();
            return; 
        }

        findViewById(R.id.btnConfirmShare).setEnabled(false);
        Toast.makeText(this, "Starting upload...", Toast.LENGTH_SHORT).show();
        
        String id = isEditMode ? editingFoodId : UUID.randomUUID().toString();
        boolean isFree = rgType.getCheckedRadioButtonId() == R.id.rbFree;
        double price = isFree ? 0.0 : Double.parseDouble(etPrice.getText().toString());

        if (selectedImageUri != null) {
             // 1. Explicitly use the bucket URL from google-services.json
            FirebaseStorage storage = FirebaseStorage.getInstance("gs://foodshare-50e96.firebasestorage.app");
            StorageReference ref = storage.getReference().child("food_images/" + id + ".jpg");
            
            try {
                // 2. Open Stream to verify local file access
                java.io.InputStream stream = getContentResolver().openInputStream(selectedImageUri);
                
                if (stream == null) {
                    Toast.makeText(this, "Error: Could not open image file locally", Toast.LENGTH_SHORT).show();
                    findViewById(R.id.btnConfirmShare).setEnabled(true);
                    return;
                }

                // 3. Upload Stream
                ref.putStream(stream).addOnSuccessListener(task -> {
                    // Upload Succeeded
                     ref.getDownloadUrl().addOnSuccessListener(uri -> {
                        saveFirestore(user, id, uri, isFree, price);
                    }).addOnFailureListener(e -> {
                        findViewById(R.id.btnConfirmShare).setEnabled(true);
                        Toast.makeText(this, "Get URL Error: " + e.getMessage(), Toast.LENGTH_LONG).show();
                    });
                }).addOnFailureListener(e -> {
                    findViewById(R.id.btnConfirmShare).setEnabled(true);
                    Toast.makeText(this, "Upload Error: " + e.getMessage(), Toast.LENGTH_LONG).show();
                    e.printStackTrace();
                });

            } catch (IOException e) {
                 findViewById(R.id.btnConfirmShare).setEnabled(true);
                 Toast.makeText(this, "Local File Error: " + e.getMessage(), Toast.LENGTH_SHORT).show();
            }
        } else {
            Uri finalUri = (existingImageUri != null) ? Uri.parse(existingImageUri) : null;
            saveFirestore(user, id, finalUri, isFree, price);
        }
    }

    private void saveFirestore(FirebaseUser user, String id, Uri imageUri, boolean isFree, double price) {
        // --- PRECISE TIME CALCULATION ---
        long pickupStart = System.currentTimeMillis();
        long expiry = pickupStart + 3600000; // Default 1 hour later
        
        try {
            String sTime = etStartTime.getText().toString(); // HH:mm
            String eTime = etEndTime.getText().toString();   // HH:mm
            
            SimpleDateFormat sdf = new SimpleDateFormat("HH:mm", Locale.getDefault());
            Date dateStart = sdf.parse(sTime);
            Date dateEnd = sdf.parse(eTime);
            
            Calendar nowCal = Calendar.getInstance();
            Calendar startCal = Calendar.getInstance();
            Calendar endCal = Calendar.getInstance();
            
            if (dateStart != null) {
                startCal.setTime(dateStart);
                // Set Year/Month/Day to Today
                startCal.set(Calendar.YEAR, nowCal.get(Calendar.YEAR));
                startCal.set(Calendar.MONTH, nowCal.get(Calendar.MONTH));
                startCal.set(Calendar.DAY_OF_MONTH, nowCal.get(Calendar.DAY_OF_MONTH));
            }
            
            if (dateEnd != null) {
                endCal.setTime(dateEnd);
                endCal.set(Calendar.YEAR, nowCal.get(Calendar.YEAR));
                endCal.set(Calendar.MONTH, nowCal.get(Calendar.MONTH));
                endCal.set(Calendar.DAY_OF_MONTH, nowCal.get(Calendar.DAY_OF_MONTH));
            }
            
            // Logic: If End < Start, it means next day (e.g. 23:00 to 02:00)
            if (endCal.before(startCal)) {
                endCal.add(Calendar.DAY_OF_YEAR, 1);
            }
            
            /* 
               Optional: If Start is way in the past (e.g. > 12h ago), maybe user meant tomorrow? 
               But for simplicity, assume User enters Today's times unless it's a "night owl" slot.
            */
            
            pickupStart = startCal.getTimeInMillis();
            expiry = endCal.getTimeInMillis();
            
        } catch (Exception e) {
            e.printStackTrace();
            // Fallback used if parsing fails
        }

        final long finalPickupStart = pickupStart;
        final long finalExpiry = expiry;

        FirebaseFirestore.getInstance().collection("users").document(user.getUid()).get()
                .addOnSuccessListener(snap -> {
                    String donorName = "User";
                    boolean isVerified = false; // Default
                    if (snap.exists()) {
                        if (snap.getString("firstName") != null) {
                            donorName = snap.getString("firstName");
                        }
                        // Check for verified status
                        if (snap.contains("isVerified")) {
                            isVerified = Boolean.TRUE.equals(snap.getBoolean("isVerified"));
                        }
                    }
                    saveToDb(id, etTitle.getText().toString(), etDescription.getText().toString(), etLocation.getText().toString(),
                            currentLat, currentLng, imageUri, donorName, user.getUid(), isFree, price, finalPickupStart, finalExpiry, isVerified);
                })
                .addOnFailureListener(e -> {
                    saveToDb(id, etTitle.getText().toString(), etDescription.getText().toString(), etLocation.getText().toString(),
                            currentLat, currentLng, imageUri, "User", user.getUid(), isFree, price, finalPickupStart, finalExpiry, false);
                });
    }

    private void saveToDb(String id, String title, String desc, String loc, double lat, double lng, Uri uri, String donor, String uid, boolean isFree, double price, long pickupStart, long expiry, boolean isVerified) {
          FoodItem item = new FoodItem(
                id, title, desc, loc, lat, lng, uri,
                "Just now", donor,
                Integer.parseInt(etQuantity.getText().toString()),
                etStartTime.getText() + " - " + etEndTime.getText(),
                cbHalal.isChecked(), cbVegetarian.isChecked(), cbVegan.isChecked(), cbNutFree.isChecked(), cbDairyFree.isChecked(),
                uid, isFree, price, pickupStart, expiry, isVerified
        );

        FirebaseFirestore.getInstance().collection("foods").document(id).set(item)
                .addOnSuccessListener(v -> {
                    Toast.makeText(this, isEditMode ? "Listing Updated!" : "Food Shared!", Toast.LENGTH_SHORT).show();
                    finish();
                })
                .addOnFailureListener(e -> {
                    findViewById(R.id.btnConfirmShare).setEnabled(true);
                    Toast.makeText(this, "Database Error: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
    }

    private void loadDataForEdit() {
        FirebaseFirestore.getInstance().collection("foods").document(editingFoodId).get()
                .addOnSuccessListener(snap -> {
                    FoodItem item = snap.toObject(FoodItem.class);
                    if (item != null) {
                        etTitle.setText(item.getTitle());
                        etDescription.setText(item.getDescription());
                        etLocation.setText(item.getLocation());
                        etQuantity.setText(String.valueOf(item.getQuantity()));

                        if (item.getPickupTime().contains("-")) {
                            String[] times = item.getPickupTime().split("-");
                            if (times.length > 0) etStartTime.setText(times[0].trim());
                            if (times.length > 1) etEndTime.setText(times[1].trim());
                        }

                        if (item.isFree()) rgType.check(R.id.rbFree);
                        else {
                            rgType.check(R.id.rbDiscounted);
                            etPrice.setText(String.valueOf(item.getPrice()));
                        }

                        cbHalal.setChecked(item.isHalal());
                        cbVegetarian.setChecked(item.isVegetarian());
                        cbVegan.setChecked(item.isVegan());
                        cbNutFree.setChecked(item.isNutFree());
                        cbDairyFree.setChecked(item.isDairyFree());

                        if (item.getImageUri() != null) {
                            existingImageUri = item.getImageUri().toString();
                            layoutUploadPlaceholder.setVisibility(View.GONE);
                        }
                        currentLat = item.getLatitude();
                        currentLng = item.getLongitude();
                    }
                });
    }
}