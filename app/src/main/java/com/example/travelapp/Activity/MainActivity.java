package com.example.travelapp.Activity;

import android.content.Intent;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.viewpager2.widget.CompositePageTransformer;
import androidx.viewpager2.widget.MarginPageTransformer;

import com.example.travelapp.Api.ChatApi;
import com.example.travelapp.Fragment.BookmarkFragment;
import com.example.travelapp.Fragment.HistoryFragment;
import com.example.travelapp.Fragment.ProfileFragment;
import com.example.travelapp.R;

import com.example.travelapp.Adapter.CategoryAdapter;
import com.example.travelapp.Adapter.PopularAdapter;
import com.example.travelapp.Adapter.RecommentdedAdapter;
import com.example.travelapp.Adapter.SearchResultsAdapter;
import com.example.travelapp.Adapter.SliderAdapter;
import com.example.travelapp.Domain.Category;
import com.example.travelapp.Domain.ItemDomain;
import com.example.travelapp.Domain.Location;
import com.example.travelapp.Domain.SliderItems;
import com.example.travelapp.databinding.ActivityMainBinding;
import com.google.android.gms.auth.api.signin.GoogleSignInClient;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.ValueEventListener;

import io.noties.markwon.Markwon;
import io.noties.markwon.html.HtmlPlugin;
import io.noties.markwon.image.ImagesPlugin;

import java.util.ArrayList;

public class MainActivity extends BaseActivity {

    ActivityMainBinding binding;
    private int selectedCategoryId = -1; // Lưu categoryId được chọn

    private FirebaseAuth mAuth;
    private GoogleSignInClient mGoogleSignInClient;
    private SearchResultsAdapter searchResultsAdapter;
    private ArrayList<ItemDomain> allItems = new ArrayList<>(); // Danh sách tất cả các item để tìm kiếm
    
    // Chat adapter and message list
    private ChatAdapter chatAdapter;
    private ArrayList<ChatMessage> chatMessages = new ArrayList<>();
    
    // Markdown renderer
    private Markwon markwon;

    // Phương thức công khai để điều hướng đến tab cụ thể dựa trên tabId.
    public void navigateToTab(int tabId) {
        binding.chipNavigation.setItemSelected(tabId, true);

        Fragment fragment = null;

        if (tabId == R.id.home) {
            // Hiển thị recyclerView và ẩn fragmentContainer
            binding.recyclerViewCategory.setVisibility(View.VISIBLE);
            binding.fragmentContainer.setVisibility(View.GONE);
            return;
        } else if (tabId == R.id.history) {
            // Điều hướng tới HistoryFragment
            fragment = new HistoryFragment();
        } else if (tabId == R.id.bookmark) {
            // Điều hướng tới BookmarkFragment
            fragment = new BookmarkFragment();  // Đảm bảo rằng bạn đã tạo lớp BookmarkFragment
        } else if (tabId == R.id.profile) {
            // Điều hướng tới ProfileFragment
            fragment = new ProfileFragment();  // Đảm bảo rằng bạn đã tạo lớp ProfileFragment
        }

        if (fragment != null) {
            getSupportFragmentManager()
                    .beginTransaction()
                    .replace(R.id.fragmentContainer, fragment)
                    .commit();
            binding.recyclerViewCategory.setVisibility(View.GONE);
            binding.fragmentContainer.setVisibility(View.VISIBLE);
        }
    }


    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityMainBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        
        // Initialize Markwon for markdown rendering
        markwon = Markwon.builder(this)
                .usePlugin(ImagesPlugin.create())
                .usePlugin(HtmlPlugin.create())
                .build();

        // Setup chat functionality
        setupChatBot();

        //Xử lý phần chuyển cu chipNavigation
        binding.chipNavigation.setOnItemSelectedListener(this::navigateToTab);

        // Nút see all cho phần recommend
        binding.textView6.setOnClickListener(v -> {
            Intent intent = new Intent(MainActivity.this, RecommendedSeeAllActivity.class);
            intent.putExtra("categoryId", selectedCategoryId); // Truyền categoryId
            startActivity(intent);
        });

        // Nút see all cho phần popular
        binding.textView8.setOnClickListener(v -> {
            Intent intent = new Intent(MainActivity.this, PopularSeeAllActivity.class);
            intent.putExtra("categoryId", selectedCategoryId); // Truyền categoryId
            startActivity(intent);
        });

        mAuth = FirebaseAuth.getInstance();

//        // Cấu hình Google Sign-In (giống như trong LoginActivity)
//        GoogleSignInOptions gso = new GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
//                .requestIdToken(getString(R.string.default_web_client_id)) // Lấy từ strings.xml
//                .requestEmail()
//                .build();
//
//        mGoogleSignInClient = GoogleSignIn.getClient(this, gso);
//
//        // Xử lý sự kiện nút "Đăng xuất"
//        binding.signOutBtn.setOnClickListener(v -> showSignOutDialog());

        // Khởi tạo RecyclerView cho kết quả tìm kiếm
        setupSearchResults();
        
        // Khởi tạo tìm kiếm
        setupSearch();
        
        // Nạp dữ liệu cho tìm kiếm
        loadAllItems();
        
        // Các hàm khởi tạo khác
        initLocation();
        initBanner();
        initCategory();
        initPopular();
        initRecommentded();
    }

    private void setupSearchResults() {
        searchResultsAdapter = new SearchResultsAdapter(new ArrayList<>());
        binding.recyclerViewSearchResults.setLayoutManager(new LinearLayoutManager(this));
        binding.recyclerViewSearchResults.setAdapter(searchResultsAdapter);
    }

    private void setupSearch() {
        // Đặt sự kiện click cho nút tìm kiếm
        binding.textView4.setOnClickListener(v -> {
            String query = binding.editTextText.getText().toString().trim();
            if (!query.isEmpty()) {
                performSearch(query);
            }
        });

        // Theo dõi thay đổi văn bản để tìm kiếm tự động
        binding.editTextText.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                String query = s.toString().trim();
                if (query.isEmpty()) {
                    // Ẩn kết quả tìm kiếm nếu trống
                    binding.searchResultsLayout.setVisibility(View.GONE);
                } else {
                    // Thực hiện tìm kiếm khi người dùng nhập
                    performSearch(query);
                }
            }

            @Override
            public void afterTextChanged(Editable s) {}
        });
    }

    private void loadAllItems() {
        // Lấy tất cả các mục từ cơ sở dữ liệu để tìm kiếm
        DatabaseReference itemsRef = database.getReference("Item");
        
        itemsRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                allItems.clear();
                if (snapshot.exists()) {
                    for (DataSnapshot issue : snapshot.getChildren()) {
                        ItemDomain item = issue.getValue(ItemDomain.class);
                        if (item != null) {
                            item.setId(issue.getKey());
                            allItems.add(item);
                        }
                    }
                }
                
                // Thêm các mục từ danh sách Popular
                loadPopularItems();
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {}
        });
    }
    
    private void loadPopularItems() {
        DatabaseReference popularRef = database.getReference("Popular");
        
        popularRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (snapshot.exists()) {
                    for (DataSnapshot issue : snapshot.getChildren()) {
                        ItemDomain item = issue.getValue(ItemDomain.class);
                        if (item != null) {
                            item.setId(issue.getKey());
                            
                            // Kiểm tra xem item đã tồn tại chưa (tránh trùng lặp)
                            boolean exists = false;
                            for (ItemDomain existingItem : allItems) {
                                if (existingItem.getId().equals(item.getId())) {
                                    exists = true;
                                    break;
                                }
                            }
                            
                            if (!exists) {
                                allItems.add(item);
                            }
                        }
                    }
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {}
        });
    }

    private void performSearch(String query) {
        // Hiển thị khu vực kết quả tìm kiếm
        binding.searchResultsLayout.setVisibility(View.VISIBLE);
        binding.progressBarSearch.setVisibility(View.VISIBLE);
        
        // Chuyển chuỗi tìm kiếm sang chữ thường
        String lowercaseQuery = query.toLowerCase();
        
        // Lọc kết quả
        ArrayList<ItemDomain> searchResults = new ArrayList<>();
        
        for (ItemDomain item : allItems) {
            // Tìm kiếm theo title, address hoặc description
            boolean matchesTitle = item.getTitle() != null && item.getTitle().toLowerCase().contains(lowercaseQuery);
            boolean matchesAddress = item.getAddress() != null && item.getAddress().toLowerCase().contains(lowercaseQuery);
            boolean matchesDescription = item.getDescription() != null && item.getDescription().toLowerCase().contains(lowercaseQuery);
            
            if (matchesTitle || matchesAddress || matchesDescription) {
                searchResults.add(item);
            }
        }
        
        // Cập nhật UI dựa trên kết quả
        if (searchResults.isEmpty()) {
            binding.noResultsText.setVisibility(View.VISIBLE);
            binding.recyclerViewSearchResults.setVisibility(View.GONE);
        } else {
            binding.noResultsText.setVisibility(View.GONE);
            binding.recyclerViewSearchResults.setVisibility(View.VISIBLE);
            searchResultsAdapter.updateList(searchResults);
        }
        
        binding.progressBarSearch.setVisibility(View.GONE);
    }

//    // ========== HIỂN THỊ DIALOG XÁC NHẬN ĐĂNG XUẤT ==========
//    private void showSignOutDialog() {
//        new AlertDialog.Builder(this)
//                .setTitle("Xác nhận đăng xuất")
//                .setMessage("Bạn có chắc chắn muốn đăng xuất không?")
//                .setPositiveButton("Đồng ý", (dialog, which) -> signOut()) // Xác nhận đăng xuất
//                .setNegativeButton("Hủy", null) // Hủy bỏ đăng xuất
//                .setCancelable(true)
//                .show();
//    }
//
//    // ========== ĐĂNG XUẤT ==========
//    private void signOut() {
//        mGoogleSignInClient.signOut().addOnCompleteListener(this, task -> {
//            FirebaseAuth.getInstance().signOut();  // Đăng xuất Firebase
//            Toast.makeText(this, "Đăng xuất thành công", Toast.LENGTH_SHORT).show();
//
//            // Chuyển hướng về màn hình đăng nhập
//            Intent intent = new Intent(MainActivity.this, LoginActivity.class);
//            startActivity(intent);
//            finish(); // Kết thúc màn hình hiện tại
//        });
//    }

    private void initCategory() {
        DatabaseReference myRef = database.getReference("Category");
        binding.progressBarCategory.setVisibility(View.VISIBLE);
        ArrayList<Category> list = new ArrayList<>();

        myRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (snapshot.exists()) {
                    for (DataSnapshot issue : snapshot.getChildren()) {
                        list.add(issue.getValue(Category.class));
                    }
                    if (!list.isEmpty()) {
                        binding.recyclerViewCategory.setLayoutManager(new LinearLayoutManager(MainActivity.this, LinearLayoutManager.HORIZONTAL, false));
                        CategoryAdapter adapter = new CategoryAdapter(list, new CategoryAdapter.OnCategorySelectedListener() {
                            @Override
                            public void onCategorySelected(int categoryId) {
                                selectedCategoryId = categoryId;
                                initRecommentded(); // Tải lại danh sách Recommended
                                initPopular(); // Tải lại danh sách Popular
                            }

                            @Override
                            public void onCategoryDeselected() {
                                selectedCategoryId = -1;
                                initRecommentded(); // Tải lại tất cả Recommended
                                initPopular(); // Tải lại tất cả Popular
                            }
                        });
                        binding.recyclerViewCategory

                                .setAdapter(adapter);
                    }
                    binding.progressBarCategory.setVisibility(View.GONE);
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {}
        });
    }

    private void initRecommentded() {
        DatabaseReference myRef = database.getReference("Item");
        binding.progressBarRecommended.setVisibility(View.VISIBLE);
        ArrayList<ItemDomain> list = new ArrayList<>();

        myRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (snapshot.exists()) {
                    for (DataSnapshot issue : snapshot.getChildren()) {
                        ItemDomain item = issue.getValue(ItemDomain.class);
                        // Chỉ thêm item nếu categoryId khớp hoặc không có bộ lọc

                        if (item != null) {
                            item.setId(issue.getKey()); // ✅ GÁN ID ở đây

                            // Lọc theo category nếu có
                            if (selectedCategoryId == -1 || item.getCategoryId() == selectedCategoryId) {
                                list.add(item);
                            }
                        }

                    }
                    if (!list.isEmpty()) {
                        binding.recyclerViewRecommended.setLayoutManager(new LinearLayoutManager(MainActivity.this, LinearLayoutManager.HORIZONTAL, false));
                        RecyclerView.Adapter adapter = new RecommentdedAdapter(list, true);
                        binding.recyclerViewRecommended.setAdapter(adapter);
                    } else {
                        binding.recyclerViewRecommended.setAdapter(null); // Xóa danh sách nếu không có item
                    }
                    binding.progressBarRecommended.setVisibility(View.GONE);
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {}
        });
    }

    private void initPopular() {
        DatabaseReference myRef = database.getReference("Popular");
        binding.progressBarPopular.setVisibility(View.VISIBLE);
        ArrayList<ItemDomain> list = new ArrayList<>();

        myRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (snapshot.exists()) {
                    for (DataSnapshot issue : snapshot.getChildren()) {
                        ItemDomain item = issue.getValue(ItemDomain.class);
                        // Chỉ thêm item nếu categoryId khớp hoặc không có bộ lọc

                        if (item != null) {
                            item.setId(issue.getKey()); // ✅ GÁN ID ở đây

                            // Lọc theo category nếu có
                            if (selectedCategoryId == -1 || item.getCategoryId() == selectedCategoryId) {
                                list.add(item);
                            }
                        }

                    }
                    if (!list.isEmpty()) {
                        binding.recyclerViewPopular.setLayoutManager(new LinearLayoutManager(MainActivity.this, LinearLayoutManager.HORIZONTAL, false));
                        RecyclerView.Adapter adapter = new PopularAdapter(list, true);
                        binding.recyclerViewPopular.setAdapter(adapter);
                    } else {
                        binding.recyclerViewPopular.setAdapter(null); // Xóa danh sách nếu không có item
                    }
                    binding.progressBarPopular.setVisibility(View.GONE);
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {}
        });
    }

    private void initLocation() {
        DatabaseReference myRef = database.getReference("Location");
        ArrayList<Location> list = new ArrayList<>();
        myRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (snapshot.exists()) {
                    for (DataSnapshot issue : snapshot.getChildren()) {
                        list.add(issue.getValue(Location.class));
                    }
                    ArrayAdapter<Location> adapter = new ArrayAdapter<>(MainActivity.this, R.layout.sp_item, list);
                    adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
                    binding.locationSp.setAdapter(adapter);
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {}
        });
    }


    private void banners(ArrayList<SliderItems> items) {

        binding.viewPagerSlider.setAdapter(new SliderAdapter(items, binding.viewPagerSlider));
        binding.viewPagerSlider.setClipToPadding(false);
        binding.viewPagerSlider.setClipChildren(false);
        binding.viewPagerSlider.setOffscreenPageLimit(3);
        binding.viewPagerSlider.getChildAt(0).setOverScrollMode(RecyclerView.OVER_SCROLL_NEVER);

        CompositePageTransformer compositePageTransformer = new CompositePageTransformer();
        compositePageTransformer.addTransformer(new MarginPageTransformer(40));
        binding.viewPagerSlider.setPageTransformer(compositePageTransformer);
    }

    private void initBanner() {
        DatabaseReference myRef = database.getReference("Banner");
        binding.progressBarBaner.setVisibility(View.VISIBLE);
        ArrayList<SliderItems> items = new ArrayList<>();
        myRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (snapshot.exists()) {
                    for (DataSnapshot issue : snapshot.getChildren()) {
                        items.add(issue.getValue(SliderItems.class));
                    }
                    banners(items);
                    binding.progressBarBaner.setVisibility(View.GONE);
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {}
        });
    }

    // Setup ChatBot Functionality
    private void setupChatBot() {
        // Initialize chat RecyclerView
        chatAdapter = new ChatAdapter(chatMessages, markwon);
        binding.chatRecyclerView.setLayoutManager(new LinearLayoutManager(this));
        binding.chatRecyclerView.setAdapter(chatAdapter);
        
        // Set click listener for chatbot button
        binding.chatbotButton.setOnClickListener(v -> {
            binding.chatOverlay.setVisibility(View.VISIBLE);
        });
        
        // Set click listener for close button
        binding.closeChatButton.setOnClickListener(v -> {
            binding.chatOverlay.setVisibility(View.GONE);
        });
        
        // Set click listener for send button
        binding.sendMessageButton.setOnClickListener(v -> {
            String message = binding.messageInput.getText().toString().trim();
            if (!message.isEmpty()) {
                // Add user message
                addMessage(message, true);
                
                // Clear input field
                binding.messageInput.setText("");
                
                // Show loading indicator
                showLoading(true);
                
                // Send message to API
                ChatApi.sendChatMessage(message, new ChatApi.ChatCallback() {
                    @Override
                    public void onSuccess(String response) {
                        // Add bot response with markdown
                        runOnUiThread(() -> {
                            addMessage(response, false);
                            showLoading(false);
                        });
                    }

                    @Override
                    public void onError(String errorMessage) {
                        // Show error message
                        runOnUiThread(() -> {
                            Toast.makeText(MainActivity.this, errorMessage, Toast.LENGTH_SHORT).show();
                            addMessage("Sorry, I couldn't process your request. Please try again later.", false);
                            showLoading(false);
                        });
                    }
                });
            }
        });
    }
    
    // Show/hide loading indicator
    private void showLoading(boolean isLoading) {
        binding.sendMessageButton.setEnabled(!isLoading);
        if (isLoading) {
            binding.sendMessageButton.setAlpha(0.5f);
        } else {
            binding.sendMessageButton.setAlpha(1.0f);
        }
    }
    
    // Add a message to the chat
    private void addMessage(String message, boolean isUser) {
        ChatMessage chatMessage = new ChatMessage(message, isUser);
        chatMessages.add(chatMessage);
        chatAdapter.notifyItemInserted(chatMessages.size() - 1);
        binding.chatRecyclerView.smoothScrollToPosition(chatMessages.size() - 1);
    }
}

// Chat Message Class
class ChatMessage {
    private String message;
    private boolean isUser;
    
    public ChatMessage(String message, boolean isUser) {
        this.message = message;
        this.isUser = isUser;
    }
    
    public String getMessage() {
        return message;
    }
    
    public boolean isUser() {
        return isUser;
    }
}

// Chat Adapter
class ChatAdapter extends RecyclerView.Adapter<ChatAdapter.ViewHolder> {
    private ArrayList<ChatMessage> chatMessages;
    private Markwon markwon;
    
    public ChatAdapter(ArrayList<ChatMessage> chatMessages, Markwon markwon) {
        this.chatMessages = chatMessages;
        this.markwon = markwon;
    }
    
    @Override
    public int getItemViewType(int position) {
        return chatMessages.get(position).isUser() ? 0 : 1;
    }
    
    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull android.view.ViewGroup parent, int viewType) {
        View view;
        if (viewType == 0) { // User message
            view = android.view.LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.item_chat_user, parent, false);
        } else { // Bot message
            view = android.view.LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.item_chat_bot, parent, false);
        }
        return new ViewHolder(view);
    }
    
    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        ChatMessage message = chatMessages.get(position);
        
        if (message.isUser()) {
            // Regular text for user messages
            holder.messageText.setText(message.getMessage());
        } else {
            // Markdown rendering for bot messages
            markwon.setMarkdown(holder.messageText, message.getMessage());
        }
    }
    
    @Override
    public int getItemCount() {
        return chatMessages.size();
    }
    
    public static class ViewHolder extends RecyclerView.ViewHolder {
        android.widget.TextView messageText;
        
        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            messageText = itemView.findViewById(R.id.messageTextView);
        }
    }
}