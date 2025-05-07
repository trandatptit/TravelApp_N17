package com.example.travelapp.Activity;

import android.os.Bundle;
import androidx.appcompat.app.AppCompatActivity;
import com.example.travelapp.databinding.ActivityRecommendedSeeAllBinding;
import com.example.travelapp.Adapter.RecommentdedAdapter;
import com.example.travelapp.Domain.ItemDomain;
import com.google.firebase.database.*;

import java.util.ArrayList;

public class RecommendedSeeAllActivity extends BaseActivity {
    ActivityRecommendedSeeAllBinding binding;
    private int selectedCategoryId = -1;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityRecommendedSeeAllBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        // Nhận categoryId từ Intent
        selectedCategoryId = getIntent().getIntExtra("categoryId", -1);

        loadRecommendedItems();
        binding.backBtn.setOnClickListener(v -> finish());
    }

    private void loadRecommendedItems() {
        DatabaseReference myRef = FirebaseDatabase.getInstance().getReference("Item");
        ArrayList<ItemDomain> list = new ArrayList<>();

        myRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot snapshot) {
                for (DataSnapshot data : snapshot.getChildren()) {
                    ItemDomain item = data.getValue(ItemDomain.class);
                    // Chỉ thêm item nếu categoryId khớp hoặc không có bộ lọc
                    if (selectedCategoryId == -1 || item.getCategoryId() == selectedCategoryId) {
                        list.add(item);
                    }
                }
                RecommentdedAdapter adapter = new RecommentdedAdapter(list, false);
                binding.recyclerView.setAdapter(adapter);
            }

            @Override
            public void onCancelled(DatabaseError error) {}
        });
    }
}