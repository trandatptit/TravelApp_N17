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
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityRecommendedSeeAllBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

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
                    list.add(data.getValue(ItemDomain.class));
                }
                RecommentdedAdapter adapter = new RecommentdedAdapter(list, false); // Sử dụng layout gốc
                binding.recyclerView.setAdapter(adapter);
            }

            @Override
            public void onCancelled(DatabaseError error) {}
        });
    }
}
