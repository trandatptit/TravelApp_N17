package com.example.travelapp.Adapter;

import android.content.Context;
import android.content.Intent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.example.travelapp.Activity.DetailActivity;
import com.example.travelapp.Domain.ItemDomain;
import com.example.travelapp.databinding.ViewholderRecommendedBinding;

import java.util.ArrayList;

public class RecommentdedAdapter extends RecyclerView.Adapter<RecommentdedAdapter.Viewholder> {
    ArrayList<ItemDomain> items;
    Context context;

    public RecommentdedAdapter(ArrayList<ItemDomain> items) {
        this.items = items;
    }

    @NonNull
    @Override
    public Viewholder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        context = parent.getContext();
        ViewholderRecommendedBinding binding = ViewholderRecommendedBinding.inflate(LayoutInflater.from(context), parent, false);
        return new Viewholder(binding);
    }

    @Override
    public void onBindViewHolder(@NonNull Viewholder holder, int position) {
        ItemDomain item = items.get(position);

        holder.binding.titleTxt.setText(item.getTitle());
        holder.binding.priceTxt.setText(" $" + item.getPrice());
        holder.binding.addressTxt.setText(item.getAddress());
        holder.binding.scoreTxt.setText("" + item.getScore());

        Glide.with(context)
                .load(item.getPic())
                .into(holder.binding.pic);

        holder.itemView.setOnClickListener(v -> {
            Intent intent = new Intent(context, DetailActivity.class);
            intent.putExtra("object", item);
            context.startActivity(intent);
        });
    }

    @Override
    public int getItemCount() {
        return items.size();
    }

    public class Viewholder extends RecyclerView.ViewHolder {
        ViewholderRecommendedBinding binding;

        public Viewholder(ViewholderRecommendedBinding binding) {
            super(binding.getRoot());
            this.binding = binding;
        }
    }
}