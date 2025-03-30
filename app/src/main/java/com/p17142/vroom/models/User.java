package com.p17142.vroom.models;

import java.io.Serializable;
import java.time.Instant;

public class User implements Serializable {
    private String uid;
    private String username;
    private String email;
    private String imageUri;
    private String token;
    private Instant createdDate;
    private int userRating;
    private int numOfRatings;
    private int numOfTripsCompletedAsDriver;
    private int numOfTripsCompletedAsRider;
    private int numOfUniquePeopleMet;

    public User(String uid, String username, String email, String imageUri, Instant createdDate, int userRating, int numOfRatings, int numOfTripsCompletedAsDriver, int numOfTripsCompletedAsRider, int numOfUniquePeopleMet) {
        this.uid = uid;
        this.username = username;
        this.email = email;
        this.imageUri = imageUri;
        this.createdDate = createdDate;
        this.userRating = userRating;
        this.numOfRatings = numOfRatings;
        this.numOfTripsCompletedAsDriver = numOfTripsCompletedAsDriver;
        this.numOfTripsCompletedAsRider = numOfTripsCompletedAsRider;
        this.numOfUniquePeopleMet = numOfUniquePeopleMet;
    }

    public User(String username, String email, String imageUri, String token) {
        this.username = username;
        this.email = email;
        this.imageUri = imageUri;
        this.token = token;
    }

    public User(String uid, String username, String email, String imageUri, String token) {
        this.uid = uid;
        this.username = username;
        this.email = email;
        this.imageUri = imageUri;
        this.token = token;
    }

    public User(String uid, String username, String imageUri){
        this.uid = uid;
        this.username = username;
        this.imageUri = imageUri;
    }

    public String getUid() {
        return uid;
    }

    public void setUid(String uid) {
        this.uid = uid;
    }

    public String getToken() {
        return token;
    }

    public void setToken(String token) {
        this.token = token;
    }

    public String getImageUri() {
        return imageUri;
    }

    public void setImageUri(String imageUri) {
        this.imageUri = imageUri;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public int getUserRating() {
        return userRating;
    }

    public void setUserRating(int userRating) {
        this.userRating = userRating;
    }

    public int getNumOfRatings() {
        return numOfRatings;
    }

    public void setNumOfRatings(int numOfRatings) {
        this.numOfRatings = numOfRatings;
    }

    public int getNumOfTripsCompletedAsDriver() {
        return numOfTripsCompletedAsDriver;
    }

    public void setNumOfTripsCompletedAsDriver(int numOfTripsCompletedAsDriver) {
        this.numOfTripsCompletedAsDriver = numOfTripsCompletedAsDriver;
    }

    public int getNumOfTripsCompletedAsRider() {
        return numOfTripsCompletedAsRider;
    }

    public void setNumOfTripsCompletedAsRider(int numOfTripsCompletedAsRider) {
        this.numOfTripsCompletedAsRider = numOfTripsCompletedAsRider;
    }

    public int getNumOfUniquePeopleMet() {
        return numOfUniquePeopleMet;
    }

    public void setNumOfUniquePeopleMet(int numOfUniquePeopleMet) {
        this.numOfUniquePeopleMet = numOfUniquePeopleMet;
    }

    public Instant getCreatedDate() {
        return createdDate;
    }

    public void setCreatedDate(Instant createdDate) {
        this.createdDate = createdDate;
    }

    public String getCreatedDateAsString(){
        return createdDate.toString();
    }
}
