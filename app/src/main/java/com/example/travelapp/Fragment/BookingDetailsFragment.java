package com.example.travelapp.Fragment;

import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.bumptech.glide.Glide;
import com.example.travelapp.R;
import com.example.travelapp.databinding.FragmentBookingDetailsBinding;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.text.NumberFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class BookingDetailsFragment extends Fragment {

    private FragmentBookingDetailsBinding binding;
    private String orderId;
    private FirebaseDatabase database;
    private DatabaseReference ordersRef, orderDetailsRef;
    private static final String TAG = "BookingDetailsFragment";

    public static BookingDetailsFragment newInstance(String orderId) {
        BookingDetailsFragment fragment = new BookingDetailsFragment();
        Bundle args = new Bundle();
        args.putString("order_id", orderId);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
            orderId = getArguments().getString("order_id");
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        binding = FragmentBookingDetailsBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        // Initialize Firebase references
        database = FirebaseDatabase.getInstance();
        ordersRef = database.getReference("Orders");
        orderDetailsRef = database.getReference("OrderDetails");

        // Set up back button
        binding.backButton.setOnClickListener(v -> requireActivity().onBackPressed());

        // Load booking details
        loadBookingDetails();
    }

    private void loadBookingDetails() {
        binding.progressBar.setVisibility(View.VISIBLE);
        binding.contentLayout.setVisibility(View.GONE);

        if (orderId == null || orderId.isEmpty()) {
            showError("Invalid order ID");
            return;
        }

        // First, get order information
        ordersRef.child(orderId).addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot orderSnapshot) {
                if (!orderSnapshot.exists()) {
                    showError("Order not found");
                    return;
                }

                // Get order details
                String status = orderSnapshot.child("status").getValue(String.class);
                // Use Double instead of double to handle null values
                Double totalPriceObj = orderSnapshot.child("totalPrice").getValue(Double.class);
                double totalPrice = totalPriceObj != null ? totalPriceObj : 0.0;
                String createdAt = orderSnapshot.child("createdAt").getValue(String.class);
                String userId = orderSnapshot.child("userId").getValue(String.class);

                // Format and set basic order details
                binding.orderIdValue.setText(orderId);
                binding.bookingDateValue.setText(formatDate(createdAt));
                binding.statusValue.setText(status);

                // Format price
                NumberFormat formatter = NumberFormat.getCurrencyInstance(new Locale("vi", "VN"));
                binding.totalPriceValue.setText(formatter.format(totalPrice));

                // Set status indicator
                updateStatusIndicator(status);

                // Now get order item details
                orderDetailsRef.child(orderId).addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot orderDetailsSnapshot) {
                        if (!orderDetailsSnapshot.exists() || orderDetailsSnapshot.getChildrenCount() == 0) {
                            showBasicOrderDetails(status, totalPrice, createdAt);
                            return;
                        }

                        // Get the first item in order details
                        DataSnapshot firstItem = orderDetailsSnapshot.getChildren().iterator().next();

                        // Get quantity and unit price if available
                        Integer quantityObj = firstItem.child("quantity").getValue(Integer.class);
                        int quantity = quantityObj != null ? quantityObj : 1;

                        Double unitPriceObj = firstItem.child("price").getValue(Double.class);
                        double unitPrice = unitPriceObj != null ? unitPriceObj : totalPrice;

                        binding.quantityValue.setText(String.valueOf(quantity));
                        binding.unitPriceValue.setText(formatter.format(unitPrice));

                        // Get ticketId from order details
                        String ticketIdStr = firstItem.child("ticketId").getValue(String.class);
                        Log.d(TAG, "Order " + orderId + " has ticketId: " + ticketIdStr);

                        if (ticketIdStr != null && !ticketIdStr.isEmpty()) {
                            try {
                                // Convert ticketId string to integer index
                                final int tourIndex = Integer.parseInt(ticketIdStr);

                                // Directly get the Item array from root reference like in HistoryFragment
                                database.getReference().child("Item").addListenerForSingleValueEvent(new ValueEventListener() {
                                    @Override
                                    public void onDataChange(@NonNull DataSnapshot itemsSnapshot) {
                                        Log.d(TAG, "Got items snapshot, children count: " + itemsSnapshot.getChildrenCount());

                                        if (itemsSnapshot.exists()) {
                                            // Try to get item directly using tourIndex as array index
                                            if (tourIndex >= 0 && tourIndex < itemsSnapshot.getChildrenCount()) {
                                                DataSnapshot tourSnapshot = null;

                                                // Iterate to find the item at specified index
                                                int currentIndex = 0;
                                                for (DataSnapshot itemSnapshot : itemsSnapshot.getChildren()) {
                                                    if (currentIndex == tourIndex) {
                                                        tourSnapshot = itemSnapshot;
                                                        break;
                                                    }
                                                    currentIndex++;
                                                }

                                                if (tourSnapshot != null && tourSnapshot.exists()) {
                                                    processTourData(tourSnapshot);
                                                } else {
                                                    Log.d(TAG, "Tour at index " + tourIndex + " not found");
                                                    showBasicOrderDetails(status, totalPrice, createdAt);
                                                }
                                            } else {
                                                Log.d(TAG, "Tour index " + tourIndex + " out of bounds");
                                                showBasicOrderDetails(status, totalPrice, createdAt);
                                            }
                                        } else {
                                            Log.d(TAG, "No items found in database");
                                            showBasicOrderDetails(status, totalPrice, createdAt);
                                        }
                                    }

                                    @Override
                                    public void onCancelled(@NonNull DatabaseError error) {
                                        Log.e(TAG, "Error loading tour items: " + error.getMessage());
                                        showBasicOrderDetails(status, totalPrice, createdAt);
                                    }
                                });
                            } catch (NumberFormatException e) {
                                Log.e(TAG, "Error parsing ticketId: " + e.getMessage());
                                showBasicOrderDetails(status, totalPrice, createdAt);
                            }
                        } else {
                            // No ticket ID found
                            Log.d(TAG, "No ticketId found in order details");
                            showBasicOrderDetails(status, totalPrice, createdAt);
                        }
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {
                        showError("Failed to load order details: " + error.getMessage());
                    }
                });
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                showError("Failed to load order: " + error.getMessage());
            }
        });
    }

    // Helper method to process tour data
    private void processTourData(DataSnapshot tourSnapshot) {
        String tourName = tourSnapshot.child("title").getValue(String.class);
        String tourImage = tourSnapshot.child("pic").getValue(String.class);
        String tourDesc = tourSnapshot.child("description").getValue(String.class);
        String travelDate = tourSnapshot.child("dateTour").getValue(String.class);
        String tourAddress = tourSnapshot.child("address").getValue(String.class);
        String tourTime = tourSnapshot.child("timeTour").getValue(String.class);
        String tourDuration = tourSnapshot.child("duration").getValue(String.class);

        // Set tour details
        binding.tourNameValue.setText(tourName != null ? tourName : "Unknown Tour");
        binding.tourLocationValue.setText(tourAddress != null ? tourAddress : "N/A");
        binding.travelDateValue.setText(travelDate != null ? travelDate : "N/A");
        binding.departTimeValue.setText(tourTime != null ? tourTime : "N/A");
        binding.durationValue.setText(tourDuration != null ? tourDuration : "N/A");

        // Set tour description
        binding.tourDescValue.setText(tourDesc != null ? tourDesc : "Tour description not available");

        // Load tour image
        if (tourImage != null && !tourImage.isEmpty()) {
            Glide.with(requireContext())
                    .load(tourImage)
                    .placeholder(R.drawable.placeholder_image)
                    .error(R.drawable.error_image)
                    .centerCrop()
                    .into(binding.tourImage);
        } else {
            binding.tourImage.setImageResource(R.drawable.placeholder_image);
        }

        // Show content
        binding.progressBar.setVisibility(View.GONE);
        binding.contentLayout.setVisibility(View.VISIBLE);
    }

    private void showBasicOrderDetails(String status, double totalPrice, String createdAt) {
        // Use default values for missing information
        binding.tourNameValue.setText("Tour Package");
        binding.tourLocationValue.setText("N/A");
        binding.travelDateValue.setText("N/A");
        binding.departTimeValue.setText("N/A");
        binding.durationValue.setText("N/A");
        binding.tourDescValue.setText("Tour description not available");
        binding.tourImage.setImageResource(R.drawable.placeholder_image);

        // Show content
        binding.progressBar.setVisibility(View.GONE);
        binding.contentLayout.setVisibility(View.VISIBLE);
    }

    private void updateStatusIndicator(String status) {
        if ("paid".equalsIgnoreCase(status)) {
            binding.statusValue.setText("Đã thanh toán");
            binding.statusValue.setTextColor(getResources().getColor(R.color.status_paid, null));
            binding.statusValue.setBackgroundResource(R.drawable.bg_status_paid);
        } else if ("canceled".equalsIgnoreCase(status) || "cancelled".equalsIgnoreCase(status)) {
            binding.statusValue.setText("Đã hủy");
            binding.statusValue.setTextColor(getResources().getColor(R.color.status_cancelled, null));
            binding.statusValue.setBackgroundResource(R.drawable.bg_status_cancelled);
        } else {
            binding.statusValue.setText("Chờ xử lý");
            binding.statusValue.setTextColor(getResources().getColor(R.color.status_pending, null));
            binding.statusValue.setBackgroundResource(R.drawable.bg_status_pending);
        }
    }


    private String formatDate(String dateStr) {
        try {
            SimpleDateFormat inputFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.getDefault());
            SimpleDateFormat outputFormat = new SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault());
            Date date = inputFormat.parse(dateStr);
            return outputFormat.format(date);
        } catch (Exception e) {
            return dateStr;
        }
    }

    private void showError(String message) {
        binding.progressBar.setVisibility(View.GONE);
        Toast.makeText(requireContext(), message, Toast.LENGTH_SHORT).show();
        requireActivity().onBackPressed();
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}