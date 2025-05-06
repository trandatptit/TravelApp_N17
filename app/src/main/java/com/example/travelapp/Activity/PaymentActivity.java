package com.example.travelapp.Activity;

import android.content.Intent;
import android.os.Bundle;
import android.os.StrictMode;
import android.view.View;
import android.widget.Toast;

import com.example.travelapp.Api.CreateOrder;
import com.example.travelapp.Domain.ItemDomain;
import com.example.travelapp.Domain.Order;
import com.example.travelapp.Domain.OrderDetail;
import com.example.travelapp.databinding.ActivityPaymentBinding;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

import org.json.JSONObject;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Locale;

import vn.zalopay.sdk.Environment;
import vn.zalopay.sdk.ZaloPayError; // Import class xử lý lỗi ZaloPay
import vn.zalopay.sdk.ZaloPaySDK; // Import class chính của ZaloPay SDK
import vn.zalopay.sdk.listeners.PayOrderListener; // Import interface lắng nghe kết quả thanh toán


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

        StrictMode.ThreadPolicy policy = new
                StrictMode.ThreadPolicy.Builder().permitAll().build();
        StrictMode.setThreadPolicy(policy);

        // ZaloPay SDK Init
        ZaloPaySDK.init(2554, Environment.SANDBOX);

        // Khởi tạo Firebase
        database = FirebaseDatabase.getInstance();
        ordersRef = database.getReference("Orders"); // Tham chiếu đến node "Orders" trong Firebase
        currentUser = FirebaseAuth.getInstance().getCurrentUser();
        if (currentUser != null) {
            String userId = currentUser.getUid();
            // Thực hiện các hành động với userId
        } else {
            // Xử lý trường hợp chưa có người dùng đăng nhập
            Toast.makeText(this, "Bạn cần đăng nhập để thực hiện thanh toán", Toast.LENGTH_SHORT).show();
            // Có thể chuyển người dùng đến màn hình đăng nhập
        }

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
            binding.tvProductPrice.setText("Đơn giá: " + String.valueOf(object.getPrice()) + " VND");
            binding.tvTotalAmountDetail.setText("Tổng tiền: " + String.format("%.0f", totalAmount) + " VND");
        }
    }

    private void saveOrderToFirebase(String fullName, String phoneNumber, String productName, int quantity, double price, double totalAmount, String status) {
        if (currentUser != null) {
            String userId = currentUser.getUid();
            String orderId = ordersRef.push().getKey(); // Tạo ID duy nhất cho đơn hàng mới
            currentOrderId = orderId; // Lưu lại orderId

            // Lấy thời gian hiện tại theo định dạng ISO 8601
            SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.getDefault());
            String createdAt = sdf.format(Calendar.getInstance().getTime());

            // Tạo đối tượng Order
            Order order = new Order(createdAt, status, totalAmount, userId);

            ordersRef.child(orderId).setValue(order) // Lưu đơn hàng vào Firebase
                    .addOnSuccessListener(aVoid -> {
                        // Sau khi tạo đơn hàng thành công, lưu chi tiết đơn hàng
                        saveOrderDetail(orderId, price, quantity, object.getId(), totalAmount);
                    })
                    .addOnFailureListener(e -> {
                        Toast.makeText(PaymentActivity.this, "Lỗi khi tạo đơn hàng: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                    });
        } else {
            Toast.makeText(this, "Người dùng chưa đăng nhập", Toast.LENGTH_SHORT).show();
        }
    }

    private void saveOrderDetail(String orderId, double price, int quantity, String ticketId, double totalPrice) {
        orderDetailsRef = database.getReference("OrderDetails").child(orderId).child("0"); // Tham chiếu đến chi tiết đơn hàng (sử dụng "0" làm key con đầu tiên)
        OrderDetail orderDetail = new OrderDetail(price, quantity, ticketId, totalPrice);
        orderDetailsRef.setValue(orderDetail) // Lưu chi tiết đơn hàng vào Firebase
                .addOnSuccessListener(aVoid -> {
                    // Gọi API ZaloPay và tiến hành thanh toán sau khi lưu chi tiết thành công
                    // Chuyển đổi totalAmount thành int trước khi truyền vào createOrder
                    long amountToSend = (long) totalAmount;
                    callZaloPayAPIAndPay(String.valueOf(amountToSend));
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(PaymentActivity.this, "Lỗi khi thêm chi tiết đơn hàng: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                    // Rollback: Xóa đơn hàng chính nếu thêm chi tiết thất bại
                    ordersRef.child(orderId).removeValue()
                            .addOnSuccessListener(aVoid -> {
                                Toast.makeText(PaymentActivity.this, "Đã hủy đơn hàng do lỗi khi thêm chi tiết.", Toast.LENGTH_LONG).show();
                                // Có thể cần thực hiện thêm các hành động thông báo cho người dùng
                            })
                            .addOnFailureListener(error -> {
                                Toast.makeText(PaymentActivity.this, "Lỗi khi hủy đơn hàng: " + error.getMessage(), Toast.LENGTH_LONG).show();
                                // Xảy ra lỗi kép, cần log hoặc xử lý đặc biệt
                            });
                });
    }

    private void callZaloPayAPIAndPay(String amount) {
        CreateOrder createOrder = new CreateOrder();
        try {
            JSONObject response = createOrder.createOrder(amount); // Gọi API tạo đơn hàng ZaloPay
            if (response != null && response.getInt("return_code") == 1) {
                String zpTransToken = response.getString("zp_trans_token"); // Lấy zp_trans_token từ response của ZaloPay

                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        // Gọi ZaloPay SDK để tiến hành thanh toán
                        ZaloPaySDK.getInstance().payOrder(PaymentActivity.this, zpTransToken, "demozpdk://app", new PayOrderListener() {
                            @Override
                            public void onPaymentSucceeded(String transactionId, String transToken, String appTransID) {
                                runOnUiThread(() -> {
                                    Toast.makeText(PaymentActivity.this, "Thanh toán thành công: " + transactionId, Toast.LENGTH_SHORT).show();
                                    updateOrderStatusToPaid("paid"); // Cập nhật trạng thái đơn hàng thành "đã thanh toán"
                                    Intent intent = new Intent(PaymentActivity.this, TicketActivity.class);
                                    intent.putExtra("object", object);
                                    startActivity(intent);
                                    finish(); // Kết thúc Activity hiện tại
                                });
                            }

                            @Override
                            public void onPaymentCanceled(String zpTransToken, String appTransID) {
                                runOnUiThread(() -> {
                                    Toast.makeText(PaymentActivity.this, "Thanh toán bị hủy", Toast.LENGTH_SHORT).show();
                                    updateOrderStatusToPaid("canceled"); // Cập nhật trạng thái đơn hàng thành "đã hủy"
                                });
                            }

                            @Override
                            public void onPaymentError(ZaloPayError zaloPayError, String zpTransToken, String appTransID) {
                                runOnUiThread(() -> {
                                    Toast.makeText(PaymentActivity.this, "Lỗi thanh toán (" + zaloPayError.toString() + "): " + zpTransToken, Toast.LENGTH_LONG).show();
                                    updateOrderStatusToPaid("failed"); // Cập nhật trạng thái đơn hàng thành "thất bại"
                                });
                            }
                        });
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

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        ZaloPaySDK.getInstance().onResult(intent); // Xử lý kết quả trả về từ ZaloPay SB
    }

    private void updateOrderStatusToPaid(String status) {
        if (currentOrderId != null) {
            ordersRef.child(currentOrderId).child("status").setValue(status) // Cập nhật trạng thái đơn hàng trong Firebase
                    .addOnSuccessListener(aVoid -> {
                        runOnUiThread(() -> {
                            Toast.makeText(PaymentActivity.this, "Đã cập nhật trạng thái đơn hàng: " + status, Toast.LENGTH_SHORT).show();
                        });
                    })
                    .addOnFailureListener(e -> {
                        runOnUiThread(() -> {
                            Toast.makeText(PaymentActivity.this, "Lỗi khi cập nhật trạng thái đơn hàng: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                        });
                    });
        } else {
            runOnUiThread(() -> {
                Toast.makeText(PaymentActivity.this, "Không tìm thấy ID đơn hàng để cập nhật trạng thái", Toast.LENGTH_SHORT).show();
            });
        }
    }
}