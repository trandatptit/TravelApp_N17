package com.example.travelapp.Activity;

import android.os.Bundle;
import com.example.travelapp.Adapter.PopularAdapter;
import com.example.travelapp.Domain.ItemDomain;
import com.example.travelapp.databinding.ActivityPopularSeeAllBinding;
import com.google.firebase.database.*;

import java.util.ArrayList;

public class PopularSeeAllActivity extends BaseActivity {
    ActivityPopularSeeAllBinding binding;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityPopularSeeAllBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

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
                    list.add(data.getValue(ItemDomain.class));
                }
                PopularAdapter adapter = new PopularAdapter(list, false); // Sử dụng viewholder_recommended.xml
                binding.recyclerView.setAdapter(adapter);
            }

            @Override
            public void onCancelled(DatabaseError error) {}
        });
    }
}