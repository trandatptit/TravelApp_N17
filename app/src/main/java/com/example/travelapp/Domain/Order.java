package com.example.travelapp.Domain;

public class Order {
    private String createdAt;
    private String status;
    private double totalPrice;
    private String userId;

    // Bắt buộc phải có constructor không tham số cho Firebase
    public Order() {
    }

    public Order(String createdAt, String status, double totalPrice, String userId) {
        this.createdAt = createdAt;
        this.status = status;
        this.totalPrice = totalPrice;
        this.userId = userId;
    }

    public String getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(String createdAt) {
        this.createdAt = createdAt;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public double getTotalPrice() {
        return totalPrice;
    }

    public void setTotalPrice(double totalPrice) {
        this.totalPrice = totalPrice;
    }

    public String getUserId() {
        return userId;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }
}