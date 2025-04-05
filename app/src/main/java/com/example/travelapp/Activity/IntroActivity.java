package com.example.travelapp.Activity;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;

import com.example.travelapp.R;
import com.example.travelapp.databinding.ActivityIntroBinding;

public class IntroActivity extends BaseActivity {

    ActivityIntroBinding binding;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        binding = ActivityIntroBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        binding.introBtn.setOnClickListener((View.OnClickListener) v -> startActivity(new Intent(IntroActivity.this, MainActivity.class)));
    }
}