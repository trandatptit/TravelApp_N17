package com.example.travelapp.Activity;

import android.os.Bundle;
import android.view.View;
import android.widget.Toast;

import com.example.travelapp.Api.CreateOrder;
import com.example.travelapp.Domain.ItemDomain;
import com.example.travelapp.databinding.ActivityPaymentBinding;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

import org.json.JSONObject;

public class PaymentActivity extends BaseActivity {
    private ActivityPaymentBinding binding;
    private ItemDomain object; // Nhận object sản phẩm từ DetailActivity
    private int productQuantity = 1; // Mặc định số lượng là 1
    private double totalAmount;

    // Firebase
    private FirebaseDatabase database;
    private DatabaseReference ordersRef;
    private DatabaseReference orderDetailsRef;
    private FirebaseUser currentUser;

    // Order ID (sẽ được tạo khi lưu đơn hàng ban đầu)
    private String currentOrderId;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityPaymentBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        // Khởi tạo Firebase
        database = FirebaseDatabase.getInstance();
        ordersRef = database.getReference("Orders"); // Tên node Orders (số nhiều)
        currentUser = FirebaseAuth.getInstance().getCurrentUser();

        // Nhận object sản phẩm từ Intent
        getIntentExtra();

        // Hiển thị thông tin sản phẩm
        displayProductInfo();

        // Xử lý sự kiện nút Thanh toán
        binding.btnPay.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String fullName = binding.etFullName.getText().toString().trim();
                String phoneNumber = binding.etPhoneNumber.getText().toString().trim();

                // Kiểm tra thông tin người dùng
                if (fullName.isEmpty()) {
                    binding.etFullName.setError("Vui lòng nhập họ và tên");
                    return;
                }
                if (phoneNumber.isEmpty()) {
                    binding.etPhoneNumber.setError("Vui lòng nhập số điện thoại");
                    return;
                }

                // Lưu đơn hàng với trạng thái "pending"
                saveOrderToFirebase(fullName, phoneNumber, object.getTitle(), productQuantity, object.getPrice(), totalAmount, "pending");
            }
        });
    }

    private void getIntentExtra() {
        if (getIntent().hasExtra("object")) {
            object = (ItemDomain) getIntent().getSerializableExtra("object");
            if (object != null) {
                totalAmount = productQuantity * object.getPrice();
            }
        }
    }

    private void displayProductInfo() {
        if (object != null) {
            binding.tvProductName.setText("Tên sản phẩm: " + object.getTitle());
            binding.tvProductQuantity.setText("Số lượng: " + productQuantity);
            binding.tvProductPrice.setText("Đơn giá: " + String.format("%.0f", object.getPrice()) + " VND");
            binding.tvTotalAmountDetail.setText("Tổng tiền: " + String.format("%.0f", totalAmount) + " VND");
        }
    }

    private void saveOrderToFirebase(String fullName, String phoneNumber, String productName, int quantity, double price, double totalAmount, String status) {
        if (currentUser != null) {
            String userId = currentUser.getUid();
            String orderId = ordersRef.push().getKey();
            currentOrderId = orderId; // Lưu lại orderId

            // Lấy thời gian hiện tại
            SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.getDefault());
            String createdAt = sdf.format(Calendar.getInstance().getTime());

            // Tạo đối tượng Order
            Order order = new Order(createdAt, status, totalAmount, userId);

            ordersRef.child(orderId).setValue(order)
                    .addOnSuccessListener(aVoid -> {
                        Toast.makeText(PaymentActivity.this, "Đã tạo đơn hàng (chờ thanh toán)", Toast.LENGTH_SHORT).show();
                        // Sau khi tạo đơn hàng, lưu chi tiết đơn hàng
                        saveOrderDetail(orderId, price, quantity, object.getId(), totalAmount);
                        // Gọi API ZaloPay
                        callZaloPayAPI(String.valueOf(totalAmount));
                    })
                    .addOnFailureListener(e -> {
                        Toast.makeText(PaymentActivity.this, "Lỗi khi tạo đơn hàng: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                    });
        } else {
            Toast.makeText(this, "Người dùng chưa đăng nhập", Toast.LENGTH_SHORT).show();
        }
    }

    private void saveOrderDetail(String orderId, double price, int quantity, String ticketId, double totalPrice) {
        orderDetailsRef = database.getReference("OrderDetails").child(orderId).child("0"); // Sử dụng "0" làm key con đầu tiên
        OrderDetail orderDetail = new OrderDetail(price, quantity, ticketId, totalPrice);
        orderDetailsRef.setValue(orderDetail)
                .addOnFailureListener(e -> {
                    Toast.makeText(PaymentActivity.this, "Lỗi khi thêm chi tiết đơn hàng: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                    // Có thể cần rollback việc tạo đơn hàng chính nếu thêm chi tiết thất bại
                });
    }

    private void callZaloPayAPI(String amount) {
        CreateOrder createOrder = new CreateOrder();
        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    JSONObject response = createOrder.createOrder(amount);
                    if (response != null && response.getInt("return_code") == 1) {
                        String orderToken = response.getString("order_token");
                        // TODO: Sử dụng orderToken để gọi ZaloPay SDK thanh toán
                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                Toast.makeText(PaymentActivity.this, "Gọi API ZaloPay thành công, orderToken: " + orderToken, Toast.LENGTH_LONG).show();
                                // Sau khi có orderToken và giả sử quá trình thanh toán ZaloPay thành công,
                                // gọi phương thức để cập nhật trạng thái đơn hàng
                                updateOrderStatusToPaid();
                                // Chuyển sang TicketActivity và truyền object (nếu cần)
                                Intent intent = new Intent(PaymentActivity.this, TicketActivity.class);
                                intent.putExtra("object", object);
                                startActivity(intent);
                            }
                        });
                    } else {
                        final String errorMessage = (response != null && response.has("return_message")) ? response.getString("return_message") : "Lỗi gọi API ZaloPay";
                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                Toast.makeText(PaymentActivity.this, errorMessage, Toast.LENGTH_LONG).show();
                            }
                        });
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            Toast.makeText(PaymentActivity.this, "Lỗi khi gọi API ZaloPay: " + e.getMessage(), Toast.LENGTH_LONG).show();
                        }
                    });
                }
            }
        }).start();
    }

    private void updateOrderStatusToPaid() {
        if (currentOrderId != null) {
            ordersRef.child(currentOrderId).child("status").setValue("paid")
                    .addOnSuccessListener(aVoid -> {
                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                Toast.makeText(PaymentActivity.this, "Đã cập nhật trạng thái đơn hàng: Đã thanh toán", Toast.LENGTH_SHORT).show();
                            }
                        });
                    })
                    .addOnFailureListener(e -> {
                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                Toast.makeText(PaymentActivity.this, "Lỗi khi cập nhật trạng thái đơn hàng: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                            }
                        });
                    });
        } else {
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    Toast.makeText(PaymentActivity.this, "Không tìm thấy ID đơn hàng để cập nhật trạng thái", Toast.LENGTH_SHORT).show();
                }
            });
        }
    }
}
