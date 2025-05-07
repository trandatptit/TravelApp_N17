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
import com.example.travelapp.databinding.ViewholderPopularCompactBinding;
import com.example.travelapp.databinding.ViewholderRecommendedBinding;

import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.Locale;

public class PopularAdapter extends RecyclerView.Adapter<PopularAdapter.Viewholder> {
    ArrayList<ItemDomain> items;
    Context context;
    boolean isCompactLayout; // Biến xác định layout

    public PopularAdapter(ArrayList<ItemDomain> items, boolean isCompactLayout) {
        this.items = items;
        this.isCompactLayout = isCompactLayout;
    }

    @NonNull
    @Override
    public Viewholder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        context = parent.getContext();
        LayoutInflater inflater = LayoutInflater.from(context);
        if (isCompactLayout) {
            ViewholderPopularCompactBinding binding = ViewholderPopularCompactBinding.inflate(inflater, parent, false);
            return new Viewholder(binding.getRoot());
        } else {
            ViewholderRecommendedBinding binding = ViewholderRecommendedBinding.inflate(inflater, parent, false);
            return new Viewholder(binding.getRoot());
        }
    }

    @Override
    public void onBindViewHolder(@NonNull Viewholder holder, int position) {
        ItemDomain item = items.get(position);
        if (isCompactLayout) {
            ViewholderPopularCompactBinding binding = ViewholderPopularCompactBinding.bind(holder.itemView);
            binding.titleTxt.setText(item.getTitle());
            // Định dạng số theo định dạng tiền tệ Việt Nam
            NumberFormat currencyFormatter = NumberFormat.getNumberInstance(new Locale("vi", "VN"));
            String formattedPrice = currencyFormatter.format(item.getPrice()) + " VND";
            binding.priceTxt.setText(formattedPrice);
            binding.addressTxt.setText(item.getAddress());
            binding.scoreTxt.setText(String.valueOf(item.getScore()));
            Glide.with(context).load(item.getPic()).into(binding.pic);
        } else {
            ViewholderRecommendedBinding binding = ViewholderRecommendedBinding.bind(holder.itemView);
            binding.titleTxt.setText(item.getTitle());
            // Định dạng số theo định dạng tiền tệ Việt Nam
            NumberFormat currencyFormatter = NumberFormat.getNumberInstance(new Locale("vi", "VN"));
            String formattedPrice = currencyFormatter.format(item.getPrice()) + " VND";
            binding.priceTxt.setText(formattedPrice);
            binding.addressTxt.setText(item.getAddress());
            binding.scoreTxt.setText(String.valueOf(item.getScore()));
            Glide.with(context).load(item.getPic()).into(binding.pic);
        }

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
        public Viewholder(View itemView) {
            super(itemView);
        }
    }
}