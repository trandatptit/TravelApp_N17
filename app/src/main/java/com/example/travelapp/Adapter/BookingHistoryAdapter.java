package com.example.travelapp.Adapter;

import android.view.LayoutInflater;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.example.travelapp.Domain.BookingHistoryItem;
import com.example.travelapp.R;
import com.example.travelapp.databinding.ItemBookingHistoryBinding;

import java.text.NumberFormat;
import java.util.List;
import java.util.Locale;

public class BookingHistoryAdapter extends RecyclerView.Adapter<BookingHistoryAdapter.BookingViewHolder> {

    private final List<BookingHistoryItem> bookingList;
    private final OnBookingItemClickListener listener;

    public interface OnBookingItemClickListener {
        void onBookingItemClick(BookingHistoryItem item);
    }

    public BookingHistoryAdapter(List<BookingHistoryItem> bookingList, OnBookingItemClickListener listener) {
        this.bookingList = bookingList;
        this.listener = listener;
    }

    @NonNull
    @Override
    public BookingViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        ItemBookingHistoryBinding binding = ItemBookingHistoryBinding.inflate(
                LayoutInflater.from(parent.getContext()), parent, false);
        return new BookingViewHolder(binding);
    }

    @Override
    public void onBindViewHolder(@NonNull BookingViewHolder holder, int position) {
        holder.bind(bookingList.get(position), listener);
    }

    @Override
    public int getItemCount() {
        return bookingList.size();
    }

    static class BookingViewHolder extends RecyclerView.ViewHolder {
        private final ItemBookingHistoryBinding binding;

        public BookingViewHolder(@NonNull ItemBookingHistoryBinding binding) {
            super(binding.getRoot());
            this.binding = binding;
        }

        public void bind(BookingHistoryItem item, OnBookingItemClickListener listener) {
            binding.tourNameTxt.setText(item.getTourName());
            binding.bookingDateTxt.setText(item.getBookingDate());
            binding.travelDateTxt.setText(item.getTravelDate());

            // Format price with currency symbol
            NumberFormat formatter = NumberFormat.getCurrencyInstance(new Locale("vi", "VN"));
            binding.priceTxt.setText(formatter.format(item.getTotalPrice()));

            // Load tour image
            if (item.getTourImage() != null && !item.getTourImage().isEmpty()) {
                Glide.with(binding.getRoot().getContext())
                        .load(item.getTourImage())
                        .placeholder(R.drawable.placeholder_image)
                        .error(R.drawable.error_image)
                        .centerCrop()
                        .into(binding.tourImage);
            } else {
                binding.tourImage.setImageResource(R.drawable.placeholder_image);
            }

            // Set status text and background
            String status = item.getStatus();
            if ("paid".equalsIgnoreCase(status)) {
                binding.statusTxt.setText("Đã thanh toán");
                binding.statusTxt.setBackgroundResource(R.drawable.bg_status_paid);
            } else if ("canceled".equalsIgnoreCase(status) || "cancelled".equalsIgnoreCase(status)) {
                binding.statusTxt.setText("Đã hủy");
                binding.statusTxt.setBackgroundResource(R.drawable.bg_status_cancelled);
            } else {
                binding.statusTxt.setText("Chờ xử lý");
                binding.statusTxt.setBackgroundResource(R.drawable.bg_status_pending);
            }


            // Set click listener
            binding.viewDetailsTxt.setOnClickListener(v -> {
                if (listener != null) {
                    listener.onBookingItemClick(item);
                }
            });

            // Also make the whole item clickable
            binding.getRoot().setOnClickListener(v -> {
                if (listener != null) {
                    listener.onBookingItemClick(item);
                }
            });
        }
    }
}