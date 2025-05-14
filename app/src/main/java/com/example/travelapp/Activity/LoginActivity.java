package com.example.travelapp.Activity;

import android.content.Intent;
import android.os.Bundle;
import android.view.Window;
import android.view.WindowManager;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.example.travelapp.Domain.User;
import com.example.travelapp.R;
import com.example.travelapp.databinding.ActivityLoginBinding;
import com.google.android.gms.auth.api.signin.GoogleSignIn;
import com.google.android.gms.auth.api.signin.GoogleSignInAccount;
import com.google.android.gms.auth.api.signin.GoogleSignInClient;
import com.google.android.gms.auth.api.signin.GoogleSignInOptions;
import com.google.android.gms.common.api.ApiException;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.AuthCredential;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.GoogleAuthProvider;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;


public class LoginActivity extends AppCompatActivity {

    // ========== KHAI BÁO BIẾN ==========
    private ActivityLoginBinding binding;
    private FirebaseAuth mAuth;
    private GoogleSignInClient mGoogleSignInClient;
    private static final int RC_SIGN_IN = 9001;
    private DatabaseReference mDatabase;  // Firebase Database Reference

    // ========== onCreate: KHỞI TẠO HOẠT ĐỘNG ==========
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityLoginBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());


        // Khởi tạo Firebase Auth và Firebase Database
        mAuth = FirebaseAuth.getInstance();
        mDatabase = FirebaseDatabase.getInstance().getReference("Users");

        // Cấu hình Google Sign-In
        GoogleSignInOptions gso = new GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                .requestIdToken(getString(R.string.default_web_client_id)) // Lấy từ strings.xml
                .requestEmail()
                .build();

        mGoogleSignInClient = GoogleSignIn.getClient(this, gso);

        // Xử lý sự kiện nút "Đăng nhập bằng Email"
        binding.signInBtn.setOnClickListener(v -> handleEmailSignIn());

        // Xử lý sự kiện nút "Chưa có tài khoản? Đăng ký"
        binding.signUpBtn.setOnClickListener(v -> {
            Intent intent = new Intent(LoginActivity.this, RegisterActivity.class);
            startActivity(intent);
        });

        // Xử lý sự kiện nút "Đăng nhập bằng Google"
        binding.signInGoogleBtn.setOnClickListener(v -> {
            Intent signInIntent = mGoogleSignInClient.getSignInIntent();
            startActivityForResult(signInIntent, RC_SIGN_IN);
        });
    }

    // ========== XỬ LÝ ĐĂNG NHẬP EMAIL/PASSWORD ==========
    private void handleEmailSignIn() {
        String email = binding.emailTxt.getText().toString().trim();
        String password = binding.passwordTxt.getText().toString().trim();

        if (email.isEmpty() || password.isEmpty()) {
            Toast.makeText(this, "Vui lòng nhập đầy đủ email và mật khẩu", Toast.LENGTH_SHORT).show();
            return;
        }

        if (password.length() < 6) {
            Toast.makeText(this, "Mật khẩu phải từ 6 ký tự trở lên", Toast.LENGTH_SHORT).show();
            return;
        }

        mAuth.signInWithEmailAndPassword(email, password)
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        Toast.makeText(this, "Đăng nhập thành công!", Toast.LENGTH_SHORT).show();
                        
                        // Kiểm tra và đảm bảo dữ liệu người dùng tồn tại trong Firebase Database
                        String userId = mAuth.getCurrentUser().getUid();
                        checkUserExistsInDatabase(userId, email);
                    } else {
                        Toast.makeText(this, "Đăng nhập thất bại: " + task.getException().getMessage(), Toast.LENGTH_SHORT).show();
                    }
                });
    }
    
    // ========== KIỂM TRA USER TRONG DATABASE ==========
    private void checkUserExistsInDatabase(String userId, String email) {
        mDatabase.child(userId).addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                if (!dataSnapshot.exists()) {
                    // Nếu user không tồn tại trong Database, tạo mới
                    String displayName = mAuth.getCurrentUser().getDisplayName();
                    if (displayName == null || displayName.isEmpty()) {
                        displayName = email.substring(0, email.indexOf('@')); // Lấy phần trước @ làm tên
                    }
                    
                    // Tạo đối tượng User mới
                    User user = new User(
                            email,
                            displayName,
                            "",  // phone
                            "password",  // authProvider
                            "https://png.pngtree.com/png-clipart/20210608/ourlarge/pngtree-dark-gray-simple-avatar-png-image_3418404.jpg"  // default avatar
                    );
                    
                    // Lưu vào database
                    mDatabase.child(userId).setValue(user)
                            .addOnCompleteListener(task -> {
                                if (task.isSuccessful()) {
                                    goToMainActivity();
                                } else {
                                    Toast.makeText(LoginActivity.this, 
                                            "Không thể lưu thông tin người dùng: " + task.getException().getMessage(), 
                                            Toast.LENGTH_SHORT).show();
                                }
                            });
                } else {
                    // User đã tồn tại, chuyển đến Main Activity
                    goToMainActivity();
                }
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {
                Toast.makeText(LoginActivity.this, 
                        "Lỗi kết nối database: " + databaseError.getMessage(), 
                        Toast.LENGTH_SHORT).show();
                goToMainActivity(); // Vẫn chuyển sang Main mặc dù có lỗi
            }
        });
    }

    // ========== KẾT QUẢ ĐĂNG NHẬP GOOGLE ==========
    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == RC_SIGN_IN) {
            Task<GoogleSignInAccount> task = GoogleSignIn.getSignedInAccountFromIntent(data);
            try {
                GoogleSignInAccount account = task.getResult(ApiException.class);
                firebaseAuthWithGoogle(account);
            } catch (ApiException e) {
                Toast.makeText(this, "Đăng nhập Google thất bại", Toast.LENGTH_SHORT).show();
            }
        }
    }

    // ========== XÁC THỰC VỚI FIREBASE BẰNG GOOGLE ==========
    private void firebaseAuthWithGoogle(GoogleSignInAccount account) {
        AuthCredential credential = GoogleAuthProvider.getCredential(account.getIdToken(), null);
        mAuth.signInWithCredential(credential)
                .addOnCompleteListener(this, task -> {
                    if (task.isSuccessful()) {
                        Toast.makeText(this, "Đăng nhập Google thành công!", Toast.LENGTH_SHORT).show();
                        saveUserToDatabase(account); // Lưu user vào Firebase Realtime Database
                    } else {
                        Toast.makeText(this, "Đăng nhập Google thất bại: " + task.getException().getMessage(), Toast.LENGTH_SHORT).show();
                    }
                });
    }

    // ========== LƯU USER VÀO DATABASE ==========
    private void saveUserToDatabase(GoogleSignInAccount account) {
        String userId = mAuth.getCurrentUser().getUid();
        String email = account.getEmail();
        String fullName = account.getDisplayName();
        String photoUrl = account.getPhotoUrl() != null ? account.getPhotoUrl().toString() : "";

        // Kiểm tra nếu user đã tồn tại
        mDatabase.child(userId).addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                if (!dataSnapshot.exists()) {
                    // Người dùng chưa tồn tại, tạo mới
                    User user = new User(email, fullName, "", "google", photoUrl);
                    
                    mDatabase.child(userId).setValue(user)
                            .addOnCompleteListener(task -> {
                                if (task.isSuccessful()) {
                                    Toast.makeText(LoginActivity.this, "Thông tin người dùng đã được lưu!", Toast.LENGTH_SHORT).show();
                                    goToMainActivity();
                                } else {
                                    Toast.makeText(LoginActivity.this, "Lỗi lưu thông tin: " + task.getException().getMessage(), Toast.LENGTH_SHORT).show();
                                    goToMainActivity(); // Vẫn chuyển sang Main mặc dù có lỗi
                                }
                            });
                } else {
                    // Người dùng đã tồn tại, có thể cập nhật thông tin nếu cần
                    goToMainActivity();
                }
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {
                Toast.makeText(LoginActivity.this, "Lỗi database: " + databaseError.getMessage(), Toast.LENGTH_SHORT).show();
                goToMainActivity();
            }
        });
    }

    // ========== CHUYỂN VỀ MÀN HÌNH CHÍNH ==========
    private void goToMainActivity() {
        Intent intent = new Intent(LoginActivity.this, MainActivity.class); // thay bằng màn chính của bạn
        startActivity(intent);
        finish(); // kết thúc login
    }
}



