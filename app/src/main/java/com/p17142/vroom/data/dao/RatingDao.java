package com.p17142.vroom.data.dao;

import static com.p17142.vroom.utilities.Constants.KEY_COLLECTION_RATINGS;
import static com.p17142.vroom.utilities.Constants.KEY_COLLECTION_TRIPS;
import static com.p17142.vroom.utilities.Constants.KEY_COLLECTION_USERS;
import static com.p17142.vroom.utilities.Constants.KEY_RATING_COMMENT;
import static com.p17142.vroom.utilities.Constants.KEY_RATING_FROM_USERNAME;
import static com.p17142.vroom.utilities.Constants.KEY_RATING_REGARDING_USERNAME;
import static com.p17142.vroom.utilities.Constants.KEY_RATING_TRIP_UID;
import static com.p17142.vroom.utilities.Constants.KEY_RATING_VALUE;
import static com.p17142.vroom.utilities.Constants.KEY_TRIP_RATING_COMPLETED_BY_ARRAY;
import static com.p17142.vroom.utilities.Constants.KEY_USER_NUM_OF_RATINGS;
import static com.p17142.vroom.utilities.Constants.KEY_USER_RATING_SUM;

import com.google.android.gms.tasks.Task;
import com.google.android.gms.tasks.Tasks;
import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;
import com.p17142.vroom.data.database.FirestoreDatabase;
import com.p17142.vroom.utilities.Logger;

import java.util.HashMap;

public class RatingDao
{
    private final FirebaseFirestore database;

    private static final RatingDao INSTANCE = new RatingDao();// get final instance

    private RatingDao()
    {
        database = FirestoreDatabase.getInstance();
    }

    public static RatingDao getInstance(){
        return INSTANCE;
    }

    public Task<Boolean> isRated(String tripUid, String fromUsername, String forUsername){
        if(tripUid == null || fromUsername == null || forUsername == null || tripUid.isEmpty() || fromUsername.isEmpty() || forUsername.isEmpty())
        {
            return Tasks.forException(new Exception("isRated method: invalid input"));
        }

        Query query = database.collection(KEY_COLLECTION_RATINGS).whereEqualTo(KEY_RATING_TRIP_UID,tripUid).whereEqualTo(KEY_RATING_FROM_USERNAME,fromUsername).whereEqualTo(KEY_RATING_REGARDING_USERNAME,forUsername);

        return query.get().continueWith( task -> {
           if(task.isSuccessful() && !task.getResult().isEmpty())
           {
               return true;
           }
           else{
               return false;
           }
        });
    }

    public Task<Boolean> putRating(String tripUid, String fromUsername, String forUsername, int ratingValue, String review)
    {
        if(tripUid == null || fromUsername == null || forUsername == null || review == null || tripUid.isEmpty() || fromUsername.isEmpty() || forUsername.isEmpty())
        {
            Logger.printLogFatal(RatingDao.class,"Invalid input on putRating");
            return Tasks.forException(new Exception("putRating method: invalid input")); // allow empty review string for now
        }

        return isRated(tripUid,fromUsername,forUsername).continueWithTask( task -> {
            if(task.isSuccessful() && task.getResult())
            {
                return Tasks.forResult(false);
            }
            else{
                HashMap<String, Object> rating = new HashMap<>();

                rating.put(KEY_RATING_TRIP_UID,tripUid);
                rating.put(KEY_RATING_FROM_USERNAME,fromUsername);
                rating.put(KEY_RATING_REGARDING_USERNAME,forUsername);
                rating.put(KEY_RATING_VALUE,ratingValue);
                rating.put(KEY_RATING_COMMENT,review);

                CollectionReference colRef = database.collection(KEY_COLLECTION_RATINGS);

                return colRef.add(rating).continueWithTask( task2 -> {
                    if(task2.isSuccessful()){
                        DocumentReference dcoRef = database.collection(KEY_COLLECTION_USERS).document(forUsername);
                        return dcoRef
                                .update(KEY_USER_RATING_SUM, FieldValue.increment(ratingValue),KEY_USER_NUM_OF_RATINGS,FieldValue.increment(1))
                                .continueWith( task3 -> {
                                    return task3.isSuccessful();
                                });
                    }else{
                        return Tasks.forResult(false);
                    }
                });
            }
        });
    }

    public Task<Boolean> markRatingComplete(String tripUid, String raterUsername)
    {
        if(tripUid == null || raterUsername == null || tripUid.isEmpty() || raterUsername.isEmpty() )
        {
            Logger.printLogFatal(RatingDao.class,"Invalid input on markRatingComplete");
            return Tasks.forException(new Exception("markRatingComplete method: invalid input"));
        }

        DocumentReference docRef = database.collection(KEY_COLLECTION_TRIPS).document(tripUid);

        return docRef.update(KEY_TRIP_RATING_COMPLETED_BY_ARRAY,FieldValue.arrayUnion(raterUsername))
                .continueWith( task -> {
                    return task.isSuccessful();
                });
    }
}
