package com.example.travelapp.Activity;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.ArrayAdapter;

import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.viewpager2.widget.CompositePageTransformer;
import androidx.viewpager2.widget.MarginPageTransformer;

import com.example.travelapp.R;

import com.example.travelapp.Adapter.CategoryAdapter;
import com.example.travelapp.Adapter.PopularAdapter;
import com.example.travelapp.Adapter.RecommentdedAdapter;
import com.example.travelapp.Adapter.SliderAdapter;
import com.example.travelapp.Domain.Category;
import com.example.travelapp.Domain.ItemDomain;
import com.example.travelapp.Domain.Location;
import com.example.travelapp.Domain.SliderItems;
import com.example.travelapp.R;
import com.example.travelapp.databinding.ActivityMainBinding;
import com.google.android.gms.auth.api.signin.GoogleSignIn;
import com.google.android.gms.auth.api.signin.GoogleSignInClient;
import com.google.android.gms.auth.api.signin.GoogleSignInOptions;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.ValueEventListener;
import java.util.ArrayList;
import com.example.travelapp.databinding.ViewholderCategoryBinding;
public class MainActivity extends BaseActivity {

    ActivityMainBinding binding;
    private int selectedCategoryId = -1; // Lưu categoryId được chọn

    private FirebaseAuth mAuth;
    private GoogleSignInClient mGoogleSignInClient;


    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityMainBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        // Nút see all cho phần recommend
        binding.textView6.setOnClickListener(v -> {
            Intent intent = new Intent(MainActivity.this, RecommendedSeeAllActivity.class);
            intent.putExtra("categoryId", selectedCategoryId); // Truyền categoryId
            startActivity(intent);
        });

        // Nút see all cho phần popular
        binding.textView8.setOnClickListener(v -> {
            Intent intent = new Intent(MainActivity.this, PopularSeeAllActivity.class);
            intent.putExtra("categoryId", selectedCategoryId); // Truyền categoryId
            startActivity(intent);
        });

        mAuth = FirebaseAuth.getInstance();

//        // Cấu hình Google Sign-In (giống như trong LoginActivity)
//        GoogleSignInOptions gso = new GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
//                .requestIdToken(getString(R.string.default_web_client_id)) // Lấy từ strings.xml
//                .requestEmail()
//                .build();
//
//        mGoogleSignInClient = GoogleSignIn.getClient(this, gso);
//
//        // Xử lý sự kiện nút "Đăng xuất"
//        binding.signOutBtn.setOnClickListener(v -> showSignOutDialog());

        // Các hàm khởi tạo khác
        initLocation();
        initBanner();
        initCategory();
        initPopular();
        initRecommentded();
    }


//    // ========== HIỂN THỊ DIALOG XÁC NHẬN ĐĂNG XUẤT ==========
//    private void showSignOutDialog() {
//        new AlertDialog.Builder(this)
//                .setTitle("Xác nhận đăng xuất")
//                .setMessage("Bạn có chắc chắn muốn đăng xuất không?")
//                .setPositiveButton("Đồng ý", (dialog, which) -> signOut()) // Xác nhận đăng xuất
//                .setNegativeButton("Hủy", null) // Hủy bỏ đăng xuất
//                .setCancelable(true)
//                .show();
//    }
//
//    // ========== ĐĂNG XUẤT ==========
//    private void signOut() {
//        mGoogleSignInClient.signOut().addOnCompleteListener(this, task -> {
//            FirebaseAuth.getInstance().signOut();  // Đăng xuất Firebase
//            Toast.makeText(this, "Đăng xuất thành công", Toast.LENGTH_SHORT).show();
//
//            // Chuyển hướng về màn hình đăng nhập
//            Intent intent = new Intent(MainActivity.this, LoginActivity.class);
//            startActivity(intent);
//            finish(); // Kết thúc màn hình hiện tại
//        });
//    }

    private void initCategory() {
        DatabaseReference myRef = database.getReference("Category");
        binding.progressBarCategory.setVisibility(View.VISIBLE);
        ArrayList<Category> list = new ArrayList<>();

        myRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (snapshot.exists()) {
                    for (DataSnapshot issue : snapshot.getChildren()) {
                        list.add(issue.getValue(Category.class));
                    }
                    if (!list.isEmpty()) {
                        binding.recyclerViewCategory.setLayoutManager(new LinearLayoutManager(MainActivity.this, LinearLayoutManager.HORIZONTAL, false));
                        CategoryAdapter adapter = new CategoryAdapter(list, new CategoryAdapter.OnCategorySelectedListener() {
                            @Override
                            public void onCategorySelected(int categoryId) {
                                selectedCategoryId = categoryId;
                                initRecommentded(); // Tải lại danh sách Recommended
                                initPopular(); // Tải lại danh sách Popular
                            }

                            @Override
                            public void onCategoryDeselected() {
                                selectedCategoryId = -1;
                                initRecommentded(); // Tải lại tất cả Recommended
                                initPopular(); // Tải lại tất cả Popular
                            }
                        });
                        binding.recyclerViewCategory

                                .setAdapter(adapter);
                    }
                    binding.progressBarCategory.setVisibility(View.GONE);
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {}
        });
    }

    private void initRecommentded() {
        DatabaseReference myRef = database.getReference("Item");
        binding.progressBarRecommended.setVisibility(View.VISIBLE);
        ArrayList<ItemDomain> list = new ArrayList<>();

        myRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (snapshot.exists()) {
                    for (DataSnapshot issue : snapshot.getChildren()) {
                        ItemDomain item = issue.getValue(ItemDomain.class);
                        // Chỉ thêm item nếu categoryId khớp hoặc không có bộ lọc

                        if (item != null) {
                            item.setId(issue.getKey()); // ✅ GÁN ID ở đây

                            // Lọc theo category nếu có
                            if (selectedCategoryId == -1 || item.getCategoryId() == selectedCategoryId) {
                                list.add(item);
                            }
                        }

                    }
                    if (!list.isEmpty()) {
                        binding.recyclerViewRecommended.setLayoutManager(new LinearLayoutManager(MainActivity.this, LinearLayoutManager.HORIZONTAL, false));
                        RecyclerView.Adapter adapter = new RecommentdedAdapter(list, true);
                        binding.recyclerViewRecommended.setAdapter(adapter);
                    } else {
                        binding.recyclerViewRecommended.setAdapter(null); // Xóa danh sách nếu không có item
                    }
                    binding.progressBarRecommended.setVisibility(View.GONE);
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {}
        });
    }

    private void initPopular() {
        DatabaseReference myRef = database.getReference("Popular");
        binding.progressBarPopular.setVisibility(View.VISIBLE);
        ArrayList<ItemDomain> list = new ArrayList<>();

        myRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (snapshot.exists()) {
                    for (DataSnapshot issue : snapshot.getChildren()) {
                        ItemDomain item = issue.getValue(ItemDomain.class);
                        // Chỉ thêm item nếu categoryId khớp hoặc không có bộ lọc

                        if (item != null) {
                            item.setId(issue.getKey()); // ✅ GÁN ID ở đây

                            // Lọc theo category nếu có
                            if (selectedCategoryId == -1 || item.getCategoryId() == selectedCategoryId) {
                                list.add(item);
                            }
                        }

                    }
                    if (!list.isEmpty()) {
                        binding.recyclerViewPopular.setLayoutManager(new LinearLayoutManager(MainActivity.this, LinearLayoutManager.HORIZONTAL, false));
                        RecyclerView.Adapter adapter = new PopularAdapter(list, true);
                        binding.recyclerViewPopular.setAdapter(adapter);
                    } else {
                        binding.recyclerViewPopular.setAdapter(null); // Xóa danh sách nếu không có item
                    }
                    binding.progressBarPopular.setVisibility(View.GONE);
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {}
        });
    }

    private void initLocation() {
        DatabaseReference myRef = database.getReference("Location");
        ArrayList<Location> list = new ArrayList<>();
        myRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (snapshot.exists()) {
                    for (DataSnapshot issue : snapshot.getChildren()) {
                        list.add(issue.getValue(Location.class));
                    }
                    ArrayAdapter<Location> adapter = new ArrayAdapter<>(MainActivity.this, R.layout.sp_item, list);
                    adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
                    binding.locationSp.setAdapter(adapter);
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {}
        });
    }


    private void banners(ArrayList<SliderItems> items) {

        binding.viewPagerSlider.setAdapter(new SliderAdapter(items, binding.viewPagerSlider));
        binding.viewPagerSlider.setClipToPadding(false);
        binding.viewPagerSlider.setClipChildren(false);
        binding.viewPagerSlider.setOffscreenPageLimit(3);
        binding.viewPagerSlider.getChildAt(0).setOverScrollMode(RecyclerView.OVER_SCROLL_NEVER);

        CompositePageTransformer compositePageTransformer = new CompositePageTransformer();
        compositePageTransformer.addTransformer(new MarginPageTransformer(40));
        binding.viewPagerSlider.setPageTransformer(compositePageTransformer);
    }

    private void initBanner() {
        DatabaseReference myRef = database.getReference("Banner");
        binding.progressBarBaner.setVisibility(View.VISIBLE);
        ArrayList<SliderItems> items = new ArrayList<>();
        myRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (snapshot.exists()) {
                    for (DataSnapshot issue : snapshot.getChildren()) {
                        items.add(issue.getValue(SliderItems.class));
                    }
                    banners(items);
                    binding.progressBarBaner.setVisibility(View.GONE);
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {}
        });
    }
}