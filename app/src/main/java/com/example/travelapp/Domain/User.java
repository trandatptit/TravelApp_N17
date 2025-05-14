package com.example.travelapp.Domain;

public class User {
    public String email;
    public String fullName;
    public String phone;
    public String birthDate;
    public String authProvider;
    public String photoUrl;

    public User(String email, String fullName, String phone, String authProvider, String photoUrl) {
        this.email = email;
        this.fullName = fullName;
        this.phone = phone;
        this.authProvider = authProvider;
        this.photoUrl = photoUrl;
        this.birthDate = "";
    }

    public User(String email, String fullName, String phone, String birthDate, String authProvider, String photoUrl) {
        this.email = email;
        this.fullName = fullName;
        this.phone = phone;
        this.birthDate = birthDate;
        this.authProvider = authProvider;
        this.photoUrl = photoUrl;
    }

    public User() {} // Firebase cần constructor rỗng

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getFullName() {
        return fullName;
    }

    public void setFullName(String fullName) {
        this.fullName = fullName;
    }

    public String getPhone() {
        return phone;
    }

    public void setPhone(String phone) {
        this.phone = phone;
    }

    public String getBirthDate() {
        return birthDate;
    }

    public void setBirthDate(String birthDate) {
        this.birthDate = birthDate;
    }

    public String getAuthProvider() {
        return authProvider;
    }

    public void setAuthProvider(String authProvider) {
        this.authProvider = authProvider;
    }

    public String getPhotoUrl() {
        return photoUrl;
    }

    public void setPhotoUrl(String photoUrl) {
        this.photoUrl = photoUrl;
    }
}
