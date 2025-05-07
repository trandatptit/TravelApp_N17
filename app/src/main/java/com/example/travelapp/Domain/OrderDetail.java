package com.example.travelapp.Domain;

public class OrderDetail {
    private double price;
    private int quantity;
    private String ticketId;
    private double totalPrice;

    // Bắt buộc phải có constructor không tham số cho Firebase
    public OrderDetail() {
    }

    public OrderDetail(double price, int quantity, String ticketId, double totalPrice) {
        this.price = price;
        this.quantity = quantity;
        this.ticketId = ticketId;
        this.totalPrice = totalPrice;
    }

    public double getPrice() {
        return price;
    }

    public void setPrice(double price) {
        this.price = price;
    }

    public int getQuantity() {
        return quantity;
    }

    public void setQuantity(int quantity) {
        this.quantity = quantity;
    }

    public String getTicketId() {
        return ticketId;
    }

    public void setTicketId(String ticketId) {
        this.ticketId = ticketId;
    }

    public double getTotalPrice() {
        return totalPrice;
    }

    public void setTotalPrice(double totalPrice) {
        this.totalPrice = totalPrice;
    }
}