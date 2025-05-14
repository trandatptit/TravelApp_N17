package com.example.travelapp.Domain;

public class BookingHistoryItem {
    private String orderId;
    private Integer tourId;
    private String tourName;
    private String tourImage;
    private String bookingDate;
    private String travelDate;
    private double totalPrice;
    private String status;

    public BookingHistoryItem() {
        // Required empty constructor for Firebase
    }

    public BookingHistoryItem(String orderId, Integer tourId, String tourName, String tourImage,
                              String bookingDate, String travelDate, double totalPrice, String status) {
        this.orderId = orderId;
        this.tourId = tourId;
        this.tourName = tourName;
        this.tourImage = tourImage;
        this.bookingDate = bookingDate;
        this.travelDate = travelDate;
        this.totalPrice = totalPrice;
        this.status = status;
    }

    public String getOrderId() {
        return orderId;
    }

    public void setOrderId(String orderId) {
        this.orderId = orderId;
    }

    public Integer getTourId() {
        return tourId;
    }

    public void setTourId(Integer tourId) {
        this.tourId = tourId;
    }

    public String getTourName() {
        return tourName;
    }

    public void setTourName(String tourName) {
        this.tourName = tourName;
    }

    public String getTourImage() {
        return tourImage;
    }

    public void setTourImage(String tourImage) {
        this.tourImage = tourImage;
    }

    public String getBookingDate() {
        return bookingDate;
    }

    public void setBookingDate(String bookingDate) {
        this.bookingDate = bookingDate;
    }

    public String getTravelDate() {
        return travelDate;
    }

    public void setTravelDate(String travelDate) {
        this.travelDate = travelDate;
    }

    public double getTotalPrice() {
        return totalPrice;
    }

    public void setTotalPrice(double totalPrice) {
        this.totalPrice = totalPrice;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }
}