package com.example.travelapp.Activity;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.bumptech.glide.Glide;
import com.example.travelapp.Domain.User;
import com.example.travelapp.R;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import com.cloudinary.android.MediaManager;
import com.cloudinary.android.callback.ErrorInfo;
import com.cloudinary.android.callback.UploadCallback;
import com.cloudinary.android.policy.TimeWindow;

import java.util.HashMap;
import java.util.Map;

import de.hdodenhof.circleimageview.CircleImageView;

public class EditProfileActivity extends AppCompatActivity {

    private ImageView backButton;
    private CircleImageView profileImage;
    private ImageView cameraIcon;
    private EditText editName, editEmail, editPhone, editPassword, editBirthDate;
    private Button saveButton;

    private FirebaseAuth mAuth;
    private FirebaseUser currentUser;
    private FirebaseDatabase database;
    private DatabaseReference userRef;

    private Uri selectedImageUri;
    private User userData;
    private boolean isCloudinaryInitialized = false;

    // Activity result launcher for image selection
    private final ActivityResultLauncher<String> pickImage = registerForActivityResult(
            new ActivityResultContracts.GetContent(),
            uri -> {
                if (uri != null) {
                    selectedImageUri = uri;
                    profileImage.setImageURI(uri);
                }
            }
    );

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.edit_profile);

        mAuth = FirebaseAuth.getInstance();
        currentUser = mAuth.getCurrentUser();
        database = FirebaseDatabase.getInstance();

        if (currentUser == null) {
            startActivity(new Intent(this, LoginActivity.class));
            finish();
            return;
        }

        userRef = database.getReference("Users").child(currentUser.getUid());

        // Initialize Cloudinary
        initCloudinary();

        // Initialize views
        initViews();
        
        // Set click listeners
        setupClickListeners();
        
        // Load user data
        loadUserData();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        // Clean up MediaManager when activity is destroyed
        if (isCloudinaryInitialized) {
            MediaManager.get().cancelAllRequests();
        }
    }

    private void initCloudinary() {
        if (!isCloudinaryInitialized) {
            try {
                Map<String, String> config = new HashMap<>();
                config.put("cloud_name", "dcwumv8cx");
                config.put("api_key", "645165865312542");
                config.put("api_secret", "SlE0JlELpfhljFfNkKRYqYUjzVw");

                try {
                    MediaManager.init(this, config);
                    isCloudinaryInitialized = true;
                } catch (IllegalStateException e) {
                    isCloudinaryInitialized = true;
                }
            } catch (Exception e) {
                Toast.makeText(this, "Lỗi khởi tạo Cloudinary: " + e.getMessage(), Toast.LENGTH_SHORT).show();
            }
        }
    }

    private void initViews() {
        backButton = findViewById(R.id.backButton);
        profileImage = findViewById(R.id.editProfileImage);
        cameraIcon = findViewById(R.id.cameraIcon);
        editName = findViewById(R.id.editName);
        editEmail = findViewById(R.id.editEmail);
        editPhone = findViewById(R.id.editPhone);
        editPassword = findViewById(R.id.editPassword);
        editBirthDate = findViewById(R.id.editBirthDate);
        saveButton = findViewById(R.id.saveButton);
    }

    private void setupClickListeners() {
        backButton.setOnClickListener(v -> finish());

        cameraIcon.setOnClickListener(v -> {
            // Launch image picker
            pickImage.launch("image/*");
        });

        saveButton.setOnClickListener(v -> saveUserProfile());
    }

    private void loadUserData() {
        userRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (snapshot.exists()) {
                    userData = snapshot.getValue(User.class);
                    if (userData != null) {
                        populateFields(userData);
                    }
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Toast.makeText(EditProfileActivity.this, 
                        "Lỗi tải dữ liệu: " + error.getMessage(), 
                        Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void populateFields(User user) {
        // Fill in the form fields with user data
        if (user.getFullName() != null) {
            editName.setText(user.getFullName());
        }
        
        if (user.getEmail() != null) {
            editEmail.setText(user.getEmail());
        } else if (currentUser.getEmail() != null) {
            editEmail.setText(currentUser.getEmail());
        }
        
        if (user.getPhone() != null) {
            editPhone.setText(user.getPhone());
        }
        
        if (user.getBirthDate() != null && !user.getBirthDate().isEmpty()) {
            editBirthDate.setText(user.getBirthDate());
        }
        
        // Password field is left empty for security reasons
        
        // Load profile image
        if (user.getPhotoUrl() != null && !user.getPhotoUrl().isEmpty()) {
            Glide.with(this)
                    .load(user.getPhotoUrl())
                    .placeholder(R.drawable.default_profile_image)
                    .error(R.drawable.default_profile_image)
                    .into(profileImage);
        } else if (currentUser.getPhotoUrl() != null) {
            Glide.with(this)
                    .load(currentUser.getPhotoUrl())
                    .placeholder(R.drawable.default_profile_image)
                    .error(R.drawable.default_profile_image)
                    .into(profileImage);
        }

        // Disable email editing if using OAuth provider
        if (user.getAuthProvider() != null && !user.getAuthProvider().equals("password")) {
            editEmail.setEnabled(false);
            editPassword.setEnabled(false);
            editEmail.setAlpha(0.5f);
            editPassword.setAlpha(0.5f);
        }
    }

    private void saveUserProfile() {
        // Show progress
        saveButton.setEnabled(false);
        saveButton.setText("Đang lưu...");

        // Get values from form
        String name = editName.getText().toString().trim();
        String email = editEmail.getText().toString().trim();
        String phone = editPhone.getText().toString().trim();
        String password = editPassword.getText().toString().trim();
        String birthDate = editBirthDate.getText().toString().trim();

        // Validate input
        if (name.isEmpty()) {
            editName.setError("Vui lòng nhập họ tên");
            saveButton.setEnabled(true);
            saveButton.setText("Lưu");
            return;
        }

        // Update user object
        Map<String, Object> updates = new HashMap<>();
        updates.put("fullName", name);
        updates.put("phone", phone);
        updates.put("birthDate", birthDate);

        if (userData.getAuthProvider() != null && userData.getAuthProvider().equals("password")) {
            if (!email.equals(userData.getEmail()) && !email.isEmpty()) {
                updates.put("email", email);
                // Update Firebase Auth email
                currentUser.updateEmail(email)
                        .addOnCompleteListener(task -> {
                            if (!task.isSuccessful()) {
                                Toast.makeText(EditProfileActivity.this, 
                                        "Lỗi cập nhật email: " + task.getException().getMessage(), 
                                        Toast.LENGTH_SHORT).show();
                            }
                        });
            }

            if (!password.isEmpty()) {
                currentUser.updatePassword(password)
                        .addOnCompleteListener(task -> {
                            if (!task.isSuccessful()) {
                                Toast.makeText(EditProfileActivity.this, 
                                        "Lỗi cập nhật mật khẩu: " + task.getException().getMessage(), 
                                        Toast.LENGTH_SHORT).show();
                            }
                        });
            }
        }

        // If image was selected, upload it to Cloudinary
        if (selectedImageUri != null) {
            uploadImageToCloudinary(updates);
        } else {
            // Just update the database
            updateUserData(updates);
        }
    }

    private void uploadImageToCloudinary(Map<String, Object> updates) {
        Toast.makeText(this, "Đang tải ảnh lên...", Toast.LENGTH_SHORT).show();

        String fileName = "profile_" + currentUser.getUid();
        
        try {
            MediaManager.get().upload(selectedImageUri)
                    .option("public_id", fileName)
                    .option("folder", "travel_app_profiles")
                    .callback(new UploadCallback() {
                        @Override
                        public void onStart(String requestId) {}

                        @Override
                        public void onProgress(String requestId, long bytes, long totalBytes) {
                            int progress = (int) ((bytes * 100) / totalBytes);
                        }

                        @Override
                        public void onSuccess(String requestId, Map resultData) {
                            String imageUrl = resultData.get("secure_url").toString();
                            updates.put("photoUrl", imageUrl);

                            updateUserData(updates);
                        }

                        @Override
                        public void onError(String requestId, ErrorInfo error) {
                            Toast.makeText(EditProfileActivity.this,
                                    "Lỗi tải ảnh lên: " + error.getDescription(),
                                    Toast.LENGTH_SHORT).show();
                            saveButton.setEnabled(true);
                            saveButton.setText("Lưu");
                        }

                        @Override
                        public void onReschedule(String requestId, ErrorInfo error) {
                            Toast.makeText(EditProfileActivity.this,
                                    "Tải ảnh bị trì hoãn, đang thử lại...",
                                    Toast.LENGTH_SHORT).show();
                        }
                    })
                    .dispatch();
        } catch (Exception e) {
            Toast.makeText(this, "Lỗi: " + e.getMessage(), Toast.LENGTH_SHORT).show();
            saveButton.setEnabled(true);
            saveButton.setText("Lưu");
        }
    }

    private void updateUserData(Map<String, Object> updates) {
        userRef.updateChildren(updates)
                .addOnCompleteListener(task -> {
                    saveButton.setEnabled(true);
                    saveButton.setText("Lưu");
                    
                    if (task.isSuccessful()) {
                        Toast.makeText(EditProfileActivity.this, 
                                "Cập nhật hồ sơ thành công", 
                                Toast.LENGTH_SHORT).show();
                        finish();
                    } else {
                        Toast.makeText(EditProfileActivity.this, 
                                "Lỗi cập nhật hồ sơ: " + task.getException().getMessage(), 
                                Toast.LENGTH_SHORT).show();
                    }
                });
    }
} 