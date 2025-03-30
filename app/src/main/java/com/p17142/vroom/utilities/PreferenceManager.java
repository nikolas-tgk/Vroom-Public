package com.p17142.vroom.utilities;

import static com.p17142.vroom.utilities.Constants.KEY_EMAIL;
import static com.p17142.vroom.utilities.Constants.KEY_IMG_URI;
import static com.p17142.vroom.utilities.Constants.KEY_IS_SIGNED;
import static com.p17142.vroom.utilities.Constants.KEY_UID;
import static com.p17142.vroom.utilities.Constants.KEY_USERNAME;
import static com.p17142.vroom.utilities.Constants.KEY_USER_CREATED_TIMESTAMP;
import static com.p17142.vroom.utilities.Constants.KEY_USER_NUM_OF_RATINGS;
import static com.p17142.vroom.utilities.Constants.KEY_USER_RATING_SUM;
import static com.p17142.vroom.utilities.Constants.KEY_USER_TRIPS_COMPLETED_AS_DRIVER;
import static com.p17142.vroom.utilities.Constants.KEY_USER_TRIPS_COMPLETED_AS_RIDER;
import static com.p17142.vroom.utilities.Constants.KEY_USER_UNIQUE_PEOPLE_MET;

import android.content.Context;
import android.content.SharedPreferences;

import com.p17142.vroom.models.User;

public class PreferenceManager {
    private final SharedPreferences sharedPreferences;

    public PreferenceManager(Context context){
        sharedPreferences = context.getSharedPreferences(Constants.KEY_PREFERENCE_NAME,Context.MODE_PRIVATE);
    }

    public void putBoolean(String key, Boolean value){
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.putBoolean(key,value);
        editor.apply();
    }
    public Boolean getBoolean(String key){
        return  sharedPreferences.getBoolean(key,false);
    }

    public void putString(String key,String value){
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.putString(key, value);
        editor.apply();
    }

    public void putInt(String key,int value){
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.putInt(key, value);
        editor.apply();
    }

    public int getInt(String key){
        return sharedPreferences.getInt(key,-1);
    }

    public void putFloat(String key, float value){
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.putFloat(key, value);
        editor.apply();
    }

    public float getFloat(String key){
        return  sharedPreferences.getFloat(key,-1);
    }

    public String getString(String key){
        return sharedPreferences.getString(key,null);
    }


    public void clear(){
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.clear();
        editor.apply();

        Logger.printLog(PreferenceManager.class, "Cleared all values");
    }

    public void storeLogin(){
        putBoolean(KEY_IS_SIGNED, true);

        Logger.printLog(PreferenceManager.class, "Stored: " + KEY_IS_SIGNED + " value: " + true);
    }

    public void storeUser(User user){
        // does not store all information
        putString(KEY_UID, user.getUid());
        putString(KEY_EMAIL, user.getEmail());
        putString(KEY_USERNAME,user.getUsername());
        putString(KEY_IMG_URI,user.getImageUri());
        putString(KEY_USER_CREATED_TIMESTAMP, user.getCreatedDateAsString());
        putFloat(KEY_USER_RATING_SUM,user.getUserRating());
        putInt(KEY_USER_NUM_OF_RATINGS, user.getNumOfRatings());
        putInt(KEY_USER_TRIPS_COMPLETED_AS_RIDER, user.getNumOfTripsCompletedAsRider());
        putInt(KEY_USER_TRIPS_COMPLETED_AS_DRIVER, user.getNumOfTripsCompletedAsDriver());
        putInt(KEY_USER_UNIQUE_PEOPLE_MET,user.getNumOfUniquePeopleMet());

        Logger.printLog(PreferenceManager.class, "Stored: " + Constants.KEY_UID + " value: " + user.getUid());
        Logger.printLog(PreferenceManager.class, "Stored: " + Constants.KEY_EMAIL + " value: " + user.getEmail());
        Logger.printLog(PreferenceManager.class, "Stored: " + Constants.KEY_USERNAME + " value: " + user.getUsername());
        Logger.printLog(PreferenceManager.class, "Stored: " + Constants.KEY_IMG_URI + " value: " + user.getImageUri());
        Logger.printLog(PreferenceManager.class, "Stored: " + KEY_USER_CREATED_TIMESTAMP + " value: " + user.getCreatedDateAsString());
        Logger.printLog(PreferenceManager.class, "Stored: " + Constants.KEY_USER_RATING_SUM + " value: " + user.getUserRating());
        Logger.printLog(PreferenceManager.class, "Stored: " + Constants.KEY_USER_NUM_OF_RATINGS + " value: " + user.getNumOfRatings());
        Logger.printLog(PreferenceManager.class, "Stored: " + Constants.KEY_USER_TRIPS_COMPLETED_AS_RIDER + " value: " + user.getNumOfTripsCompletedAsRider());
        Logger.printLog(PreferenceManager.class, "Stored: " + Constants.KEY_USER_TRIPS_COMPLETED_AS_DRIVER + " value: " + user.getNumOfTripsCompletedAsDriver());
        Logger.printLog(PreferenceManager.class, "Stored: " + Constants.KEY_USER_UNIQUE_PEOPLE_MET + " value: " + user.getNumOfUniquePeopleMet());
    }



}
