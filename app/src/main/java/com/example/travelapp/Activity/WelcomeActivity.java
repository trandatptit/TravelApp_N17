package com.example.travelapp.Activity;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;

import androidx.appcompat.app.AppCompatActivity;

import com.example.travelapp.databinding.ActivityWelcomeBinding;

public class WelcomeActivity extends AppCompatActivity {

    ActivityWelcomeBinding binding;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        binding = ActivityWelcomeBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        binding.loginBtn.setOnClickListener((View.OnClickListener) v -> startActivity(new Intent(WelcomeActivity.this, LoginActivity.class)));
        binding.registerBtn.setOnClickListener((View.OnClickListener) v -> startActivity(new Intent(WelcomeActivity.this, RegisterActivity.class)));

    }
}