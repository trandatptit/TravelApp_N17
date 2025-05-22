package com.example.travelapp.Activity;

import android.os.Bundle;
import com.example.travelapp.Adapter.PopularAdapter;
import com.example.travelapp.Domain.ItemDomain;
import com.example.travelapp.databinding.ActivityPopularSeeAllBinding;
import com.google.firebase.database.*;

import java.util.ArrayList;

public class PopularSeeAllActivity extends BaseActivity {
    ActivityPopularSeeAllBinding binding;
    private int selectedCategoryId = -1;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityPopularSeeAllBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        // Nhận categoryId từ Intent
        selectedCategoryId = getIntent().getIntExtra("categoryId", -1);

        loadPopularItems();
        binding.backBtn.setOnClickListener(v -> finish());
    }

    private void loadPopularItems() {
        DatabaseReference myRef = FirebaseDatabase.getInstance().getReference("Popular");
        ArrayList<ItemDomain> list = new ArrayList<>();

        myRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot snapshot) {
                for (DataSnapshot data : snapshot.getChildren()) {
                    ItemDomain item = data.getValue(ItemDomain.class);
                    if (selectedCategoryId == -1 || item.getCategoryId() == selectedCategoryId) {
                        list.add(item);
                    }
                }
                PopularAdapter adapter = new PopularAdapter(list, false);
                binding.recyclerView.setAdapter(adapter);
            }

            @Override
            public void onCancelled(DatabaseError error) {}
        });
    }
}