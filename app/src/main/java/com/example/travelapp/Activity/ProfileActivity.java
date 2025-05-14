package com.example.travelapp.Activity;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.text.TextUtils;
import android.util.Patterns;
import android.view.View;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;

import com.bumptech.glide.Glide;
import com.example.travelapp.Domain.User;
import com.example.travelapp.R;
import com.example.travelapp.databinding.ProfileUserBinding;
import com.google.android.gms.auth.api.signin.GoogleSignIn;
import com.google.android.gms.auth.api.signin.GoogleSignInClient;
import com.google.android.gms.auth.api.signin.GoogleSignInOptions;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.ValueEventListener;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.UploadTask;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

public class ProfileActivity extends BaseActivity {

    private ProfileUserBinding binding;
    private FirebaseAuth mAuth;
    private FirebaseUser currentUser;
    private DatabaseReference userRef;
    private GoogleSignInClient mGoogleSignInClient;
    private Uri selectedImageUri;
    private User userProfile;

    // Activity result launcher for image picker
    private final ActivityResultLauncher<Intent> imagePickerLauncher = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(),
            result -> {
                if (result.getResultCode() == RESULT_OK && result.getData() != null) {
                    selectedImageUri = result.getData().getData();
                    
                    // Load and display the selected image
                    Glide.with(this)
                            .load(selectedImageUri)
                            .placeholder(R.drawable.default_profile)
                            .into(binding.profileImage);
                }
            }
    );

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ProfileUserBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        // Initialize Firebase Auth
        mAuth = FirebaseAuth.getInstance();
        currentUser = mAuth.getCurrentUser();
        
        // Configure Google Sign In for later sign out
        GoogleSignInOptions gso = new GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                .requestIdToken(getString(R.string.default_web_client_id))
                .requestEmail()
                .build();
        mGoogleSignInClient = GoogleSignIn.getClient(this, gso);

        // Initialize Firebase Database reference for the current user
        if (currentUser != null) {
            userRef = database.getReference("Users").child(currentUser.getUid());
            loadUserProfile();
        } else {
            // Redirect to login if not logged in
            startActivity(new Intent(ProfileActivity.this, LoginActivity.class));
            finish();
            return;
        }

        setupClickListeners();
    }

    private void setupClickListeners() {
        // Back button handler
        binding.backBtn.setOnClickListener(v -> finish());
        
        // Change photo button handler
        binding.changePhotoBtn.setOnClickListener(v -> openImagePicker());
        
        // Save profile button handler
        binding.saveProfileBtn.setOnClickListener(v -> validateAndSaveProfile());
        
        // Sign Out button handler
        binding.signOutBtn.setOnClickListener(v -> showSignOutDialog());
    }

    private void openImagePicker() {
        Intent intent = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
        imagePickerLauncher.launch(intent);
    }

    private void loadUserProfile() {
        userRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (snapshot.exists()) {
                    userProfile = snapshot.getValue(User.class);
                    
                    if (userProfile != null) {
                        // Populate the UI with user data
                        binding.emailEditText.setText(userProfile.getEmail());
                        binding.fullNameEditText.setText(userProfile.getFullName());
                        binding.phoneEditText.setText(userProfile.getPhone());
                        
                        // Load profile photo if available
                        if (!TextUtils.isEmpty(userProfile.getPhotoUrl())) {
                            Glide.with(ProfileActivity.this)
                                    .load(userProfile.getPhotoUrl())
                                    .placeholder(R.drawable.default_profile)
                                    .into(binding.profileImage);
                        }
                    }
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Toast.makeText(ProfileActivity.this, "Không thể tải thông tin: " + error.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void validateAndSaveProfile() {
        String email = binding.emailEditText.getText().toString().trim();
        String fullName = binding.fullNameEditText.getText().toString().trim();
        String phone = binding.phoneEditText.getText().toString().trim();
        
        // Validate email
        if (TextUtils.isEmpty(email)) {
            binding.emailErrorText.setVisibility(View.VISIBLE);
            binding.emailErrorText.setText("Email không được để trống");
            return;
        } else if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            binding.emailErrorText.setVisibility(View.VISIBLE);
            binding.emailErrorText.setText("Email không đúng định dạng");
            return;
        } else {
            binding.emailErrorText.setVisibility(View.GONE);
        }
        
        // Validate fullName
        if (TextUtils.isEmpty(fullName)) {
            Toast.makeText(this, "Họ và tên không được để trống", Toast.LENGTH_SHORT).show();
            return;
        }
        
        // If we have a new image, upload it first, then save profile
        if (selectedImageUri != null) {
            uploadImageAndSaveProfile(email, fullName, phone);
        } else {
            // No new image, just save the profile
            saveUserProfile(email, fullName, phone, userProfile != null ? userProfile.getPhotoUrl() : null);
        }
    }

    private void uploadImageAndSaveProfile(String email, String fullName, String phone) {
        binding.progressBarPhoto.setVisibility(View.VISIBLE);
        
        // Create a storage reference
        StorageReference storageRef = FirebaseStorage.getInstance().getReference()
                .child("profile_images")
                .child(Objects.requireNonNull(currentUser.getUid()))
                .child("profile.jpg");
        
        // Upload file to Firebase Storage
        UploadTask uploadTask = storageRef.putFile(selectedImageUri);
        uploadTask.continueWithTask(task -> {
            if (!task.isSuccessful()) {
                throw Objects.requireNonNull(task.getException());
            }
            
            // Get the download URL
            return storageRef.getDownloadUrl();
        }).addOnCompleteListener(task -> {
            binding.progressBarPhoto.setVisibility(View.GONE);
            
            if (task.isSuccessful()) {
                // Get the download URL
                Uri downloadUri = task.getResult();
                // Save profile with the new image URL
                saveUserProfile(email, fullName, phone, downloadUri.toString());
            } else {
                Toast.makeText(ProfileActivity.this, "Không thể tải ảnh lên", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void saveUserProfile(String email, String fullName, String phone, String photoUrl) {
        // Create a map of updates
        Map<String, Object> updates = new HashMap<>();
        updates.put("email", email);
        updates.put("fullName", fullName);
        updates.put("phone", phone);
        
        if (photoUrl != null) {
            updates.put("photoUrl", photoUrl);
        }
        
        // If this is an update to an existing profile, preserve the auth provider
        if (userProfile != null && userProfile.getAuthProvider() != null) {
            updates.put("authProvider", userProfile.getAuthProvider());
        }
        
        // Update the user profile in the database
        userRef.updateChildren(updates)
                .addOnSuccessListener(aVoid -> {
                    Toast.makeText(ProfileActivity.this, "Cập nhật thông tin thành công", Toast.LENGTH_SHORT).show();
                    
                    // Clear focus from EditText fields and hide keyboard
                    clearFocusAndHideKeyboard();
                    
                    // Also update the email in Firebase Auth if it changed
                    if (userProfile != null && !email.equals(userProfile.getEmail())) {
                        updateEmailInAuth(email);
                    }
                })
                .addOnFailureListener(e -> 
                        Toast.makeText(ProfileActivity.this, "Lỗi: " + e.getMessage(), Toast.LENGTH_SHORT).show());
    }

    private void clearFocusAndHideKeyboard() {
        // Clear focus from all EditText fields
        binding.emailEditText.clearFocus();
        binding.fullNameEditText.clearFocus();
        binding.phoneEditText.clearFocus();
        
        // Hide keyboard
        View currentFocus = getCurrentFocus();
        if (currentFocus != null) {
            android.view.inputmethod.InputMethodManager imm = (android.view.inputmethod.InputMethodManager)
                    getSystemService(INPUT_METHOD_SERVICE);
            imm.hideSoftInputFromWindow(currentFocus.getWindowToken(), 0);
        }
    }

    private void updateEmailInAuth(String newEmail) {
        // Only attempt to update email in Firebase Auth if user is not using a federated provider
        if (currentUser != null && 
                userProfile != null && 
                !"google.com".equals(userProfile.getAuthProvider())) {
            
            currentUser.updateEmail(newEmail)
                    .addOnSuccessListener(aVoid -> {
                        // Email updated successfully
                    })
                    .addOnFailureListener(e -> {
                        // If email update fails, we don't want to show error to user
                        // as the database update already succeeded
                    });
        }
    }

    private void showSignOutDialog() {
        new AlertDialog.Builder(this)
                .setTitle("Xác nhận đăng xuất")
                .setMessage("Bạn có chắc chắn muốn đăng xuất không?")
                .setPositiveButton("Đồng ý", (dialog, which) -> signOut())
                .setNegativeButton("Hủy", null)
                .setCancelable(true)
                .show();
    }

    private void signOut() {
        mGoogleSignInClient.signOut().addOnCompleteListener(this, task -> {
            FirebaseAuth.getInstance().signOut();  // Sign out from Firebase
            Toast.makeText(this, "Đăng xuất thành công", Toast.LENGTH_SHORT).show();
            
            // Navigate back to Login screen
            Intent intent = new Intent(ProfileActivity.this, LoginActivity.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            startActivity(intent);
            finish();
        });
    }
} 