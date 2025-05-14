package com.example.travelapp.Fragment;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.ImageView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentTransaction;

import com.bumptech.glide.Glide;
import com.example.travelapp.Activity.EditProfileActivity;
import com.example.travelapp.Activity.LoginActivity;
import com.example.travelapp.Activity.MainActivity;
import com.example.travelapp.Domain.User;
import com.example.travelapp.R;
import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import de.hdodenhof.circleimageview.CircleImageView;

public class ProfileFragment extends Fragment {

    private CircleImageView profileImage;
    private TextView tvUserName, tvProfileHeader, tvUserEmail;
    private Button btnEditProfile;
    private LinearLayout personalInfoLayout, emailLayout, billingLayout, 
                        tourManagementLayout, passwordLayout, logoutLayout;
    
    private FirebaseAuth mAuth;
    private FirebaseDatabase database;
    private DatabaseReference userRef;
    private FirebaseUser currentUser;
    private User userData;

    public ProfileFragment() {
        // Bắt buộc phải có constructor rỗng
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        // Inflate profile_user.xml layout
        View view = inflater.inflate(R.layout.profile_user, container, false);
        
        // Initialize Firebase
        mAuth = FirebaseAuth.getInstance();
        database = FirebaseDatabase.getInstance();
        currentUser = mAuth.getCurrentUser();
        
        // Initialize views
        initViews(view);
        
        // Load user data
        loadUserData();
        
        // Set up click listeners
        setupClickListeners();
        
        return view;
    }
    
    @Override
    public void onResume() {
        super.onResume();
        // Reload user data when returning from EditProfileActivity
        if (currentUser != null) {
            loadUserData();
        }
    }
    
    private void initViews(View view) {
        profileImage = view.findViewById(R.id.profileImage);
        tvUserName = view.findViewById(R.id.tvUserName);
        tvUserEmail = view.findViewById(R.id.tvUserEmail);
        tvProfileHeader = view.findViewById(R.id.tvProfileHeader);
        btnEditProfile = view.findViewById(R.id.btnEditProfile);
        
        personalInfoLayout = view.findViewById(R.id.personalInfoLayout);
        emailLayout = view.findViewById(R.id.emailLayout);
        billingLayout = view.findViewById(R.id.billingLayout);
        tourManagementLayout = view.findViewById(R.id.tourManagementLayout);
        passwordLayout = view.findViewById(R.id.passwordLayout);
        logoutLayout = view.findViewById(R.id.logoutLayout);
    }
    
    private void loadUserData() {
        if (currentUser != null) {
            // Get user data from Firebase
            userRef = database.getReference("Users").child(currentUser.getUid());
            userRef.addValueEventListener(new ValueEventListener() {
                @Override
                public void onDataChange(@NonNull DataSnapshot snapshot) {
                    if (snapshot.exists()) {
                        userData = snapshot.getValue(User.class);
                        if (userData != null) {
                            updateUI(userData);
                        }
                    }
                }

                @Override
                public void onCancelled(@NonNull DatabaseError error) {
                    Toast.makeText(getContext(), "Error loading user data: " + error.getMessage(), 
                                   Toast.LENGTH_SHORT).show();
                }
            });
        } else {
            // User not signed in, redirect to login
            navigateToLogin();
        }
    }
    
    private void updateUI(User user) {
        // Update username
        if (user.getFullName() != null && !user.getFullName().isEmpty()) {
            tvUserName.setText(user.getFullName());
        } else {
            tvUserName.setText("User");
        }
        
        // Update email
        if (user.getEmail() != null && !user.getEmail().isEmpty()) {
            tvUserEmail.setText(user.getEmail());
        } else if (currentUser != null && currentUser.getEmail() != null) {
            // Fallback to FirebaseUser email if User object doesn't have it
            tvUserEmail.setText(currentUser.getEmail());
        } else {
            tvUserEmail.setText("No email available");
        }
        
        // Update profile image
        if (user.getPhotoUrl() != null && !user.getPhotoUrl().isEmpty()) {
            Glide.with(this)
                 .load(user.getPhotoUrl())
                 .placeholder(R.drawable.default_profile_image)
                 .error(R.drawable.default_profile_image)
                 .into(profileImage);
        } else if (currentUser != null && currentUser.getPhotoUrl() != null) {
            // Fallback to FirebaseUser photo if User object doesn't have it
            Glide.with(this)
                 .load(currentUser.getPhotoUrl().toString())
                 .placeholder(R.drawable.default_profile_image)
                 .error(R.drawable.default_profile_image)
                 .into(profileImage);
        } else {
            profileImage.setImageResource(R.drawable.default_profile_image);
        }
    }
    
    private void navigateToLogin() {
        Intent intent = new Intent(getActivity(), LoginActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
    }
    
    private void setupClickListeners() {
        btnEditProfile.setOnClickListener(v -> {
            // Navigate to EditProfileActivity
            Intent intent = new Intent(getActivity(), EditProfileActivity.class);
            startActivity(intent);
        });
        
        personalInfoLayout.setOnClickListener(v -> {
            // Show personal info in modal bottom sheet
            showProfileInfoBottomSheet("Thông tin cá nhân");
        });
        
        emailLayout.setOnClickListener(v -> {
            // Show email info in modal bottom sheet
            showProfileInfoBottomSheet("Địa chỉ email");
        });
        
        billingLayout.setOnClickListener(v -> {
            // Navigate to History fragment for billing details
            navigateToHistoryTab();
        });
        
        tourManagementLayout.setOnClickListener(v -> {
            // Navigate to History fragment for tour management
            navigateToHistoryTab();
        });
        
        passwordLayout.setOnClickListener(v -> {
            Toast.makeText(getContext(), "Password clicked", Toast.LENGTH_SHORT).show();
            // Navigate to password change screen
            if (userData != null && userData.getAuthProvider() != null) {
                if (!userData.getAuthProvider().equals("password")) {
                    Toast.makeText(getContext(), 
                        "You're signed in with " + userData.getAuthProvider() + 
                        ". Password change not available.", Toast.LENGTH_LONG).show();
                }
            }
        });
        
        logoutLayout.setOnClickListener(v -> {
            // Confirm logout
            Toast.makeText(getContext(), "Logging out...", Toast.LENGTH_SHORT).show();
            
            // Sign out from Firebase
            mAuth.signOut();
            
            // Navigate to login screen
            navigateToLogin();
        });
    }
    
    // Method to show user profile information in a bottom sheet
    private void showProfileInfoBottomSheet(String title) {
        if (getContext() == null || userData == null) return;
        
        // Create bottom sheet dialog
        BottomSheetDialog bottomSheetDialog = new BottomSheetDialog(getContext());
        View bottomSheetView = LayoutInflater.from(getContext()).inflate(R.layout.bottom_sheet_profile_info, null);
        bottomSheetDialog.setContentView(bottomSheetView);
        
        // Get references to views in the bottom sheet
        View modalContent = bottomSheetView.findViewById(R.id.profileInfoContent);
        TextView modalTitle = modalContent.findViewById(R.id.modalTitle);
        CircleImageView modalProfileImage = modalContent.findViewById(R.id.modalProfileImage);
        TextView viewName = modalContent.findViewById(R.id.viewName);
        TextView viewEmail = modalContent.findViewById(R.id.viewEmail);
        TextView viewPhone = modalContent.findViewById(R.id.viewPhone);
        TextView viewBirthDate = modalContent.findViewById(R.id.viewBirthDate);
        ImageView closeButton = modalContent.findViewById(R.id.closeButton);
        
        // Set title
        modalTitle.setText(title);
        
        // Set user information
        if (userData.getFullName() != null) {
            viewName.setText(userData.getFullName());
        } else {
            viewName.setText("Không có thông tin");
        }
        
        if (userData.getEmail() != null) {
            viewEmail.setText(userData.getEmail());
        } else if (currentUser != null && currentUser.getEmail() != null) {
            viewEmail.setText(currentUser.getEmail());
        } else {
            viewEmail.setText("Không có thông tin");
        }
        
        if (userData.getPhone() != null) {
            viewPhone.setText(userData.getPhone());
        } else {
            viewPhone.setText("Không có thông tin");
        }
        
        if (userData.getBirthDate() != null) {
            viewBirthDate.setText(userData.getBirthDate());
        } else {
            viewBirthDate.setText("Không có thông tin");
        }
        
        // Load profile image
        if (userData.getPhotoUrl() != null && !userData.getPhotoUrl().isEmpty()) {
            Glide.with(getContext())
                 .load(userData.getPhotoUrl())
                 .placeholder(R.drawable.default_profile_image)
                 .error(R.drawable.default_profile_image)
                 .into(modalProfileImage);
        } else if (currentUser != null && currentUser.getPhotoUrl() != null) {
            Glide.with(getContext())
                 .load(currentUser.getPhotoUrl().toString())
                 .placeholder(R.drawable.default_profile_image)
                 .error(R.drawable.default_profile_image)
                 .into(modalProfileImage);
        } else {
            modalProfileImage.setImageResource(R.drawable.default_profile_image);
        }
        
        // Set close button click listener
        closeButton.setOnClickListener(v -> bottomSheetDialog.dismiss());
        
        // Show the bottom sheet
        bottomSheetDialog.show();
    }
    
    // Method to navigate to History tab
    private void navigateToHistoryTab() {
        if (getActivity() instanceof MainActivity) {
            ((MainActivity) getActivity()).navigateToTab(R.id.history);
        }
    }
}
