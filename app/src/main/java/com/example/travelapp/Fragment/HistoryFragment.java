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
import androidx.recyclerview.widget.LinearLayoutManager;

import com.example.travelapp.Activity.MainActivity;
import com.example.travelapp.Adapter.BookingHistoryAdapter;
import com.example.travelapp.Domain.BookingHistoryItem;
import com.example.travelapp.R;
import com.example.travelapp.databinding.FragmentBookingHistoryBinding;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class HistoryFragment extends Fragment implements BookingHistoryAdapter.OnBookingItemClickListener {

    private FragmentBookingHistoryBinding binding;
    private BookingHistoryAdapter adapter;
    private List<BookingHistoryItem> bookingList;
    private FirebaseDatabase database;
    private DatabaseReference ordersRef, orderDetailsRef, itemsRef;
    private static final String TAG = "HistoryFragment";

    // For testing purposes, hardcode the userId
//        private String userId = "4YWE4EwLXbRsgSnz2fDUlZZ2uu12";

    private String userId = FirebaseAuth.getInstance().getCurrentUser().getUid();


    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        binding = FragmentBookingHistoryBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        // Initialize Firebase references
        database = FirebaseDatabase.getInstance();
        ordersRef = database.getReference("Orders");
        orderDetailsRef = database.getReference("OrderDetails");
        itemsRef = database.getReference("Item");

        // Initialize RecyclerView
        bookingList = new ArrayList<>();
        adapter = new BookingHistoryAdapter(bookingList, this);
        binding.bookingRecyclerView.setLayoutManager(new LinearLayoutManager(requireContext()));
        binding.bookingRecyclerView.setAdapter(adapter);

        // Setup swipe refresh
        binding.swipeRefreshLayout.setOnRefreshListener(this::loadBookingHistory);

        //Setup explore button in empty view
        binding.emptyView.exploreTourBtn.setOnClickListener(v -> {
            if (getActivity() instanceof MainActivity) {
                ((MainActivity) getActivity()).navigateToTab(R.id.home);
            }
        });

        // Load booking history
        loadBookingHistory();
    }

    private void loadBookingHistory() {
        binding.progressBar.setVisibility(View.VISIBLE);
        binding.emptyView.getRoot().setVisibility(View.GONE);

        // Query orders for the current user
        ordersRef.orderByChild("userId").equalTo(userId).addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                bookingList.clear();

                if (!dataSnapshot.exists()) {
                    showEmptyView();
                    return;
                }

                final int[] pendingItems = {(int) dataSnapshot.getChildrenCount()};

                for (DataSnapshot orderSnapshot : dataSnapshot.getChildren()) {
                    String orderId = orderSnapshot.getKey();
                    String status = orderSnapshot.child("status").getValue(String.class);

                    // Handle potential type issues gracefully
                    Double totalPriceObj = orderSnapshot.child("totalPrice").getValue(Double.class);
                    double totalPrice = totalPriceObj != null ? totalPriceObj : 0.0;

                    String createdAt = orderSnapshot.child("createdAt").getValue(String.class);

                    Log.d(TAG, "Processing order: " + orderId + " with status: " + status);

                    // Now fetch order details to get the tourId
                    orderDetailsRef.child(orderId).addListenerForSingleValueEvent(new ValueEventListener() {
                        @Override
                        public void onDataChange(@NonNull DataSnapshot orderDetailsSnapshot) {
                            if (orderDetailsSnapshot.exists() && orderDetailsSnapshot.getChildrenCount() > 0) {
                                // Get the first item in order details
                                DataSnapshot firstItem = orderDetailsSnapshot.getChildren().iterator().next();

                                // Get ticketId which refers to the tour
                                String ticketIdStr = firstItem.child("ticketId").getValue(String.class);
                                Log.d(TAG, "Order " + orderId + " has ticketId: " + ticketIdStr);

                                if (ticketIdStr != null && !ticketIdStr.isEmpty()) {
                                    try {
                                        // Convert ticketId to integer index
                                        final int tourIndex = Integer.parseInt(ticketIdStr);

                                        // Directly get the Item array from root reference
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
                                                            String tourName = tourSnapshot.child("title").getValue(String.class);
                                                            String tourImage = tourSnapshot.child("pic").getValue(String.class);
                                                            String travelDate = tourSnapshot.child("dateTour").getValue(String.class);

                                                            Log.d(TAG, "Found tour: " + tourName);

                                                            // Create BookingHistoryItem
                                                            BookingHistoryItem item = new BookingHistoryItem(
                                                                    orderId,
                                                                    tourIndex,
                                                                    tourName != null ? tourName : "Tour Package",
                                                                    tourImage != null ? tourImage : "",
                                                                    formatDate(createdAt),
                                                                    travelDate != null ? travelDate : "N/A",
                                                                    totalPrice,
                                                                    status
                                                            );

                                                            bookingList.add(item);
                                                        } else {
                                                            Log.d(TAG, "Tour at index " + tourIndex + " not found");
                                                            createBasicHistoryItem(orderId, createdAt, totalPrice, status);
                                                        }
                                                    } else {
                                                        Log.d(TAG, "Tour index " + tourIndex + " out of bounds");
                                                        createBasicHistoryItem(orderId, createdAt, totalPrice, status);
                                                    }
                                                } else {
                                                    Log.d(TAG, "No items found in database");
                                                    createBasicHistoryItem(orderId, createdAt, totalPrice, status);
                                                }

                                                checkAndFinishLoading(pendingItems);
                                            }

                                            @Override
                                            public void onCancelled(@NonNull DatabaseError error) {
                                                Log.e(TAG, "Error loading tour items: " + error.getMessage());
                                                createBasicHistoryItem(orderId, createdAt, totalPrice, status);
                                                checkAndFinishLoading(pendingItems);
                                            }
                                        });
                                    } catch (NumberFormatException e) {
                                        Log.e(TAG, "Error parsing ticketId: " + e.getMessage());
                                        createBasicHistoryItem(orderId, createdAt, totalPrice, status);
                                        checkAndFinishLoading(pendingItems);
                                    }
                                } else {
                                    // No ticket ID found
                                    createBasicHistoryItem(orderId, createdAt, totalPrice, status);
                                    checkAndFinishLoading(pendingItems);
                                }
                            } else {
                                // No order details found
                                createBasicHistoryItem(orderId, createdAt, totalPrice, status);
                                checkAndFinishLoading(pendingItems);
                            }
                        }

                        @Override
                        public void onCancelled(@NonNull DatabaseError error) {
                            Log.e(TAG, "Order details query cancelled: " + error.getMessage());
                            pendingItems[0]--;
                            if (pendingItems[0] == 0) {
                                finishLoading();
                            }
                        }
                    });
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Log.e(TAG, "Orders query cancelled: " + error.getMessage());
                binding.progressBar.setVisibility(View.GONE);
                binding.swipeRefreshLayout.setRefreshing(false);
                showEmptyView();
            }
        });
    }

    // Helper method to create a basic history item when tour details aren't available
    private void createBasicHistoryItem(String orderId, String createdAt, double totalPrice, String status) {
        BookingHistoryItem item = new BookingHistoryItem(
                orderId,
                null,
                "Tour Package",
                "",
                formatDate(createdAt),
                "N/A",
                totalPrice,
                status
        );
        bookingList.add(item);
    }

    // Helper method to check if all items are loaded and finish
    private void checkAndFinishLoading(int[] pendingItems) {
        pendingItems[0]--;
        if (pendingItems[0] == 0) {
            finishLoading();
        }
    }

    private void finishLoading() {
        // Sort by date (most recent first)
        Collections.sort(bookingList, (item1, item2) ->
                item2.getBookingDate().compareTo(item1.getBookingDate()));

        adapter.notifyDataSetChanged();
        binding.progressBar.setVisibility(View.GONE);
        binding.swipeRefreshLayout.setRefreshing(false);

        if (bookingList.isEmpty()) {
            showEmptyView();
        } else {
            binding.emptyView.getRoot().setVisibility(View.GONE);
            binding.bookingRecyclerView.setVisibility(View.VISIBLE);
        }
    }

    private void showEmptyView() {
        binding.progressBar.setVisibility(View.GONE);
        binding.swipeRefreshLayout.setRefreshing(false);
        binding.bookingRecyclerView.setVisibility(View.GONE);
        binding.emptyView.getRoot().setVisibility(View.VISIBLE);
    }

    private String formatDate(String dateStr) {
        try {
            SimpleDateFormat inputFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.getDefault());
            SimpleDateFormat outputFormat = new SimpleDateFormat("dd/MM/yyyy", Locale.getDefault());
            Date date = inputFormat.parse(dateStr);
            return outputFormat.format(date);
        } catch (Exception e) {
            Log.e(TAG, "Date format error: " + e.getMessage());
            return dateStr;
        }
    }

    @Override
    public void onBookingItemClick(BookingHistoryItem item) {
        // Navigate to booking details fragment
        BookingDetailsFragment detailsFragment = BookingDetailsFragment.newInstance(item.getOrderId());
        requireActivity().getSupportFragmentManager().beginTransaction()
                .replace(R.id.fragmentContainer, detailsFragment)
                .addToBackStack(null)
                .commit();
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}