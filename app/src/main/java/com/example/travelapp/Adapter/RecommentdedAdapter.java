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
import com.example.travelapp.R;
import com.example.travelapp.databinding.ViewholderRecommendedBinding;
import com.example.travelapp.databinding.ViewholderRecommendedCompactBinding;
import java.util.ArrayList;

public class RecommentdedAdapter extends RecyclerView.Adapter<RecommentdedAdapter.Viewholder> {
    ArrayList<ItemDomain> items;
    Context context;
<<<<<<< HEAD
=======
    boolean isCompactLayout; // Biến xác định layout
>>>>>>> seeAll

    public RecommentdedAdapter(ArrayList<ItemDomain> items, boolean isCompactLayout) {
        this.items = items;
        this.isCompactLayout = isCompactLayout;
    }

    @NonNull
    @Override
    public Viewholder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        context = parent.getContext();
<<<<<<< HEAD
        ViewholderRecommendedBinding binding = ViewholderRecommendedBinding.inflate(LayoutInflater.from(context), parent, false);
        return new Viewholder(binding);
=======
        LayoutInflater inflater = LayoutInflater.from(context);
        if (isCompactLayout) {
            ViewholderRecommendedCompactBinding binding = ViewholderRecommendedCompactBinding.inflate(inflater, parent, false);
            return new Viewholder(binding.getRoot());
        } else {
            ViewholderRecommendedBinding binding = ViewholderRecommendedBinding.inflate(inflater, parent, false);
            return new Viewholder(binding.getRoot());
        }
>>>>>>> seeAll
    }

    @Override
    public void onBindViewHolder(@NonNull Viewholder holder, int position) {
        ItemDomain item = items.get(position);
<<<<<<< HEAD

        holder.binding.titleTxt.setText(item.getTitle());
        holder.binding.priceTxt.setText(" $" + item.getPrice());
        holder.binding.addressTxt.setText(item.getAddress());
        holder.binding.scoreTxt.setText("" + item.getScore());

        Glide.with(context)
                .load(item.getPic())
                .into(holder.binding.pic);

=======
        if (isCompactLayout) {
            ViewholderRecommendedCompactBinding binding = ViewholderRecommendedCompactBinding.bind(holder.itemView);
            binding.titleTxt.setText(item.getTitle());
            binding.priceTxt.setText("$" + item.getPrice());
            binding.addressTxt.setText(item.getAddress());
            binding.scoreTxt.setText(String.valueOf(item.getScore()));
            Glide.with(context).load(item.getPic()).into(binding.pic);
        } else {
            ViewholderRecommendedBinding binding = ViewholderRecommendedBinding.bind(holder.itemView);
            binding.titleTxt.setText(item.getTitle());
            binding.priceTxt.setText("$" + item.getPrice());
            binding.addressTxt.setText(item.getAddress());
            binding.scoreTxt.setText(String.valueOf(item.getScore()));
            Glide.with(context).load(item.getPic()).into(binding.pic);
        }

>>>>>>> seeAll
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
<<<<<<< HEAD
        ViewholderRecommendedBinding binding;

        public Viewholder(ViewholderRecommendedBinding binding) {
            super(binding.getRoot());
            this.binding = binding;
=======
        public Viewholder(View itemView) {
            super(itemView);
>>>>>>> seeAll
        }
    }
}