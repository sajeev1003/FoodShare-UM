package com.example.foodshare;

public class UserModel {
    private String uid;
    private String email;
    private String firstName;
    private String lastName;
    private String phone;
    private String bio;
    private String city;
    private String country;
    // New Field for Module 1.3
    private boolean isVerified; 

    public UserModel() {}

    public UserModel(String uid, String email, String firstName, String lastName, String phone, String bio, String city, String country) {
        this.uid = uid;
        this.email = email;
        this.firstName = firstName;
        this.lastName = lastName;
        this.phone = phone;
        this.bio = bio;
        this.city = city;
        this.country = country;
        this.isVerified = false; // Default false
    }

    // Getters and Setters
    public String getUid() { return uid; }
    public void setUid(String uid) { this.uid = uid; }
    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }
    public String getFirstName() { return firstName; }
    public void setFirstName(String firstName) { this.firstName = firstName; }
    public String getLastName() { return lastName; }
    public void setLastName(String lastName) { this.lastName = lastName; }
    public String getPhone() { return phone; }
    public void setPhone(String phone) { this.phone = phone; }
    public String getBio() { return bio; }
    public void setBio(String bio) { this.bio = bio; }
    public String getCity() { return city; }
    public void setCity(String city) { this.city = city; }
    public String getCountry() { return country; }
    public void setCountry(String country) { this.country = country; }
    
    // Verification Getter/Setter
    public boolean isVerified() { return isVerified; }
    public void setVerified(boolean verified) { isVerified = verified; }
}