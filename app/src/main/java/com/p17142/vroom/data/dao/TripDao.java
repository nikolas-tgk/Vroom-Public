package com.p17142.vroom.data.dao;

import static com.p17142.vroom.utilities.Constants.KEY_COLLECTION_TRIPS;

import com.google.android.gms.tasks.Task;
import com.google.android.gms.tasks.Tasks;
import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QuerySnapshot;
import com.p17142.vroom.data.database.FirestoreDatabase;
import com.p17142.vroom.models.Trip;
import com.p17142.vroom.utilities.Constants;
import com.p17142.vroom.utilities.Logger;

import static com.p17142.vroom.utilities.Constants.*;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Objects;

public class TripDao {

    private final FirebaseFirestore database;

    private static final TripDao INSTANCE = new TripDao();

    public static TripDao getInstance(){
        return INSTANCE;
    }

    private TripDao(){
        database = FirestoreDatabase.getInstance();
    }


    public Task<List<Trip>> getAllTrips(String loggedUsername){
        Query query = database.collection(KEY_COLLECTION_TRIPS);

        return query.get().continueWith( task -> {
            if(task.isSuccessful() && !task.getResult().isEmpty()) {
                List<Trip> trips = new ArrayList<>();
                for (DocumentSnapshot document: task.getResult()) {
                    Trip trip = new Trip();
                    String tripId = document.getId();
                    trip.setTripUid(tripId);
                    trip.setStartTime(document.getString(Constants.KEY_TRIP_START_TIME));
                    trip.setStartLocation(document.getString(Constants.KEY_TRIP_START_LOCATION));
                    trip.setEndLocation(document.getString(Constants.KEY_TRIP_END_LOCATION));
                    trip.setDateCreated(document.getDate(Constants.KEY_TRIP_DATE_CREATED));
                    trip.setTripDate(document.getDate(Constants.KEY_TRIP_DATE)); // will this throw an error?
                    trip.setStartTime(document.getString(Constants.KEY_TRIP_START_TIME));
                    trip.setMaxNumOfRiders(Objects.requireNonNull(document.getDouble(KEY_TRIP_MAX_NUM_OF_RIDERS)).intValue());
                    trip.setDriverUsername(document.getString(KEY_TRIP_DRIVER_USERNAME));
                    trip.setRiderUsernames((List<String>) document.get(KEY_TRIP_RIDER_USERNAMES));
                    trip.setInvitedUsernames( (List<String>) document.get(KEY_TRIP_INVITED_TO_JOIN_USERNAMES));
                    trip.determineSetCurrentUserStatus(loggedUsername); // determine status using logged username
                    trip.setCompleted(document.getBoolean(KEY_TRIP_IS_COMPLETED));
                    trip.determineSetProgress();
                    trip.setRatingCompletedByUsernames((List<String>) document.get(KEY_TRIP_RATING_COMPLETED_BY_ARRAY));
                    // each document
                    trips.add(trip);
                }
                return trips;
            } else if(task.getResult().isEmpty()){
                // no trips in database
                return new ArrayList<>();
            }
            else{
                throw Objects.requireNonNull(task.getException()); // task fail
            }
        });
    }

    public Task<List<Trip>> getAllNonCompleteTrips(String callerUsername){
        Query query = database.collection(KEY_COLLECTION_TRIPS).whereEqualTo(KEY_TRIP_IS_COMPLETED,false).orderBy(KEY_TRIP_DATE,Query.Direction.ASCENDING);

        return query.get().continueWith( task -> {
           if(task.isSuccessful() && !task.getResult().isEmpty())
            {
                List<Trip> trips = new ArrayList<>();
                for(DocumentSnapshot document : task.getResult().getDocuments())
                {
                    Trip trip = new Trip();

                    String tripId = document.getId();
                    trip.setTripUid(tripId);
                    trip.setStartTime(document.getString(Constants.KEY_TRIP_START_TIME));
                    trip.setStartLocation(document.getString(Constants.KEY_TRIP_START_LOCATION));
                    trip.setEndLocation(document.getString(Constants.KEY_TRIP_END_LOCATION));
                    trip.setDateCreated(document.getDate(Constants.KEY_TRIP_DATE_CREATED));
                    trip.setTripDate(document.getDate(Constants.KEY_TRIP_DATE));
                    trip.setStartTime(document.getString(Constants.KEY_TRIP_START_TIME));
                    trip.setMaxNumOfRiders(Objects.requireNonNull(document.getDouble(KEY_TRIP_MAX_NUM_OF_RIDERS)).intValue());
                    trip.setDriverUsername(document.getString(KEY_TRIP_DRIVER_USERNAME));
                    trip.setRiderUsernames((List<String>) document.get(KEY_TRIP_RIDER_USERNAMES));
                    trip.setInvitedUsernames( (List<String>) document.get(KEY_TRIP_INVITED_TO_JOIN_USERNAMES));
                    trip.determineSetCurrentUserStatus(callerUsername); // determine status using logged username
                    trip.setCompleted(document.getBoolean(KEY_TRIP_IS_COMPLETED));
                    trip.determineSetProgress();
                    trip.setRatingCompletedByUsernames((List<String>) document.get(KEY_TRIP_RATING_COMPLETED_BY_ARRAY));


                    trips.add(trip);
                }
                return trips;
            }
           return new ArrayList<>();
        });
    }

    public Task<List<Trip>> getAllUserRelatedTrips(String targetUser, String loggedUsername){
        List<Task<QuerySnapshot>> tasks = new ArrayList<>();
        List<Trip> trips = new ArrayList<>();

        Query driverQuery = database.collection(KEY_COLLECTION_TRIPS)
                .whereEqualTo(KEY_TRIP_DRIVER_USERNAME,targetUser).orderBy(KEY_TRIP_DATE,Query.Direction.ASCENDING);
        Task<QuerySnapshot> task1 = driverQuery.get();
        tasks.add(task1);

        Query invitedQuery = database.collection(KEY_COLLECTION_TRIPS)
                .whereArrayContains(KEY_TRIP_INVITED_TO_JOIN_USERNAMES, targetUser).orderBy(KEY_TRIP_DATE,Query.Direction.ASCENDING);
        Task<QuerySnapshot> task2 = invitedQuery.get();
        tasks.add(task2);

        Query riderQuery = database.collection(KEY_COLLECTION_TRIPS)
                .whereArrayContains(KEY_TRIP_RIDER_USERNAMES,targetUser).orderBy(KEY_TRIP_DATE,Query.Direction.ASCENDING);
        Task<QuerySnapshot> task3 = riderQuery.get();
        tasks.add(task3);

        return Tasks.whenAllSuccess(tasks).continueWith( task -> {
           if(task.isSuccessful()){
               for (Task<QuerySnapshot> queryTask : tasks) {
                   if (queryTask.isSuccessful() && !queryTask.getResult().isEmpty()) {
                       List<DocumentSnapshot> documents = queryTask.getResult().getDocuments();
                       for (DocumentSnapshot document: documents
                            ) {
                           Trip trip = new Trip();
                           String tripId = document.getId();
                           trip.setTripUid(tripId);
                           trip.setStartTime(document.getString(Constants.KEY_TRIP_START_TIME));
                           trip.setStartLocation(document.getString(Constants.KEY_TRIP_START_LOCATION));
                           trip.setEndLocation(document.getString(Constants.KEY_TRIP_END_LOCATION));
                           trip.setDateCreated(document.getDate(Constants.KEY_TRIP_DATE_CREATED));
                           trip.setTripDate(document.getDate(Constants.KEY_TRIP_DATE)); // will this throw an error?
                           trip.setStartTime(document.getString(Constants.KEY_TRIP_START_TIME));
                           trip.setMaxNumOfRiders(Objects.requireNonNull(document.getDouble(KEY_TRIP_MAX_NUM_OF_RIDERS)).intValue()); // on null, sets to default value
                           trip.setDriverUsername(document.getString(KEY_TRIP_DRIVER_USERNAME));
                           trip.setRiderUsernames((List<String>) document.get(KEY_TRIP_RIDER_USERNAMES));
                           trip.setInvitedUsernames( (List<String>) document.get(KEY_TRIP_INVITED_TO_JOIN_USERNAMES));
                           trip.determineSetCurrentUserStatus(loggedUsername); // determine status using logged username
                           trip.setCompleted(document.getBoolean(KEY_TRIP_IS_COMPLETED));
                           trip.determineSetProgress();
                           trip.setRatingCompletedByUsernames((List<String>) document.get(KEY_TRIP_RATING_COMPLETED_BY_ARRAY));

                           trips.add(trip);
                       }
                   }
               }
               return trips;
           }
            throw Objects.requireNonNull(task.getException()); // error throw
        });
    }

    public Task<List<Trip>> getAllTripsByDriverUsername(String driverUsername, String loggedUsername){
        if(driverUsername == null || driverUsername.isEmpty())
        {
            Tasks.forException(new Exception("Error ,provided null/empty driverUsername argument."));
        }
        Query query = database.collection(KEY_COLLECTION_TRIPS).whereEqualTo(KEY_TRIP_DRIVER_USERNAME,driverUsername);

        return query.get().continueWith( task -> {
            if(task.isSuccessful() && !task.getResult().isEmpty())
            {
                List<Trip> trips = new ArrayList<>();
                for( DocumentSnapshot document : task.getResult())
                {
                    Trip trip = new Trip();
                    String tripId = document.getId();
                    trip.setTripUid(tripId);
                    trip.setStartTime(document.getString(Constants.KEY_TRIP_START_TIME));
                    trip.setStartLocation(document.getString(Constants.KEY_TRIP_START_LOCATION));
                    trip.setEndLocation(document.getString(Constants.KEY_TRIP_END_LOCATION));
                    trip.setDateCreated(document.getDate(Constants.KEY_TRIP_DATE_CREATED));
                    trip.setTripDate(document.getDate(Constants.KEY_TRIP_DATE)); // will this throw an error?
                    trip.setStartTime(document.getString(Constants.KEY_TRIP_START_TIME));
                    trip.setMaxNumOfRiders(Objects.requireNonNull(document.getDouble(KEY_TRIP_MAX_NUM_OF_RIDERS)).intValue()); // on null, sets to default value
                    trip.setDriverUsername(document.getString(KEY_TRIP_DRIVER_USERNAME));
                    trip.setRiderUsernames((List<String>) document.get(KEY_TRIP_RIDER_USERNAMES));
                    trip.setInvitedUsernames( (List<String>) document.get(KEY_TRIP_INVITED_TO_JOIN_USERNAMES));
                    trip.determineSetCurrentUserStatus(loggedUsername); // determine status using logged username
                    trip.setCompleted(document.getBoolean(KEY_TRIP_IS_COMPLETED));
                    trip.determineSetProgress();
                    trip.setRatingCompletedByUsernames((List<String>) document.get(KEY_TRIP_RATING_COMPLETED_BY_ARRAY));

                    // each document
                    trips.add(trip);
                }
                return trips;
            }
            else {
                return new ArrayList<>();
            }
        });
    }

    public Task<Trip> getTripByUid(String tripUid, String loggedUsername){
        if(tripUid == null || tripUid.isEmpty())
        {
            return Tasks.forException(new Exception("Error, provided null/empty  tripUd"));
        }
        DocumentReference documentReference = database.collection(KEY_COLLECTION_TRIPS).document(tripUid);
        return documentReference.get().continueWith( task -> {
            if(task.isSuccessful() && task.getResult().exists())
            {
                DocumentSnapshot document = task.getResult();
                Trip trip = new Trip();
                String tripId = document.getId();
                trip.setTripUid(tripId);
                trip.setStartTime(document.getString(Constants.KEY_TRIP_START_TIME));
                trip.setStartLocation(document.getString(Constants.KEY_TRIP_START_LOCATION));
                trip.setEndLocation(document.getString(Constants.KEY_TRIP_END_LOCATION));
                trip.setDateCreated(document.getDate(Constants.KEY_TRIP_DATE_CREATED));
                trip.setTripDate(document.getDate(Constants.KEY_TRIP_DATE)); // will this throw an error?
                trip.setStartTime(document.getString(Constants.KEY_TRIP_START_TIME));
                trip.setMaxNumOfRiders(Objects.requireNonNull(document.getDouble(KEY_TRIP_MAX_NUM_OF_RIDERS)).intValue()); // on null, sets to default value
                trip.setDriverUsername(document.getString(KEY_TRIP_DRIVER_USERNAME));
                trip.setRiderUsernames((List<String>) document.get(KEY_TRIP_RIDER_USERNAMES));
                trip.setInvitedUsernames( (List<String>) document.get(KEY_TRIP_INVITED_TO_JOIN_USERNAMES));
                trip.determineSetCurrentUserStatus(loggedUsername); // determine status using logged username
                trip.setCompleted(document.getBoolean(KEY_TRIP_IS_COMPLETED));
                trip.determineSetProgress();
                trip.setRatingCompletedByUsernames((List<String>) document.get(KEY_TRIP_RATING_COMPLETED_BY_ARRAY));

                return trip;
            }
            else if(task.isSuccessful() && !task.getResult().exists())
            {
                Logger.printLogError(TripDao.class,"Trip not found, did it get deleted?");
                return null;
            }
            else{
                throw new Exception("Error on retrieve trip using uid");
            }
        });
    }

    public Task<Boolean> acceptInvitationForTrip(String acceptorUsername, String tripUid)
    {
        if(tripUid == null || tripUid.isEmpty() )
        {
            return Tasks.forException(new Exception("Error, provided null/empty tripUd"));
        }
        if(acceptorUsername == null || acceptorUsername.isEmpty())
        {
            return Tasks.forException(new Exception("Error, provided null/empty acceptorUsername"));
        }
        DocumentReference documentReference = database.collection(KEY_COLLECTION_TRIPS).document(tripUid);

        return documentReference
                .update(KEY_TRIP_INVITED_TO_JOIN_USERNAMES,FieldValue.arrayRemove(acceptorUsername),KEY_TRIP_RIDER_USERNAMES,FieldValue.arrayUnion(acceptorUsername))
                .continueWith( task -> {
                    if (task.isSuccessful()) {
                        Logger.printLog(TripDao.class,"Invitation accepted successfully.");
                        return true;
                    }
                    else{
                        Logger.printLogFatal(TripDao.class,"Could accept invitation for trip, trip does not exist or user was not invited.");
                        return false;
                    }
                });

    }

    public Task<Boolean> removeRiderFromTrip(String riderUsernameToRemove, String tripUid)
    {
        if(tripUid == null || tripUid.isEmpty() )
        {
            return Tasks.forException(new Exception("Error, provided null/empty tripUd"));
        }
        if(riderUsernameToRemove == null || riderUsernameToRemove.isEmpty())
        {
            return Tasks.forException(new Exception("Error, provided null/empty riderUsernameToRemove"));
        }
        DocumentReference documentReference = database.collection(KEY_COLLECTION_TRIPS).document(tripUid);

        return documentReference
                .update(KEY_TRIP_RIDER_USERNAMES, FieldValue.arrayRemove(riderUsernameToRemove))
                .continueWith( task -> {
                            if (task.isSuccessful()) {
                                Logger.printLog(TripDao.class,"Rider "+riderUsernameToRemove+" removed successfully.");
                                return true;
                            } else {
                                Logger.printLogFatal(TripDao.class,"Could not remove rider from trip, trip does not exist, or user is no longed a rider.");
                                return false;
                            }
                        }
                );
    }

    public Task<Boolean> denyInvitationForTrip(String denierUsername, String tripUid)
    {
        if(tripUid == null || tripUid.isEmpty() )
        {
            return Tasks.forException(new Exception("Error, provided null/empty tripUd"));
        }
        if(denierUsername == null || denierUsername.isEmpty())
        {
            return Tasks.forException(new Exception("Error, provided null/empty denierUsername"));
        }
        DocumentReference documentReference = database.collection(KEY_COLLECTION_TRIPS).document(tripUid);

        return documentReference
                .update(KEY_TRIP_INVITED_TO_JOIN_USERNAMES, FieldValue.arrayRemove(denierUsername))
                .continueWith( task -> {
                            if (task.isSuccessful()) {
                                Logger.printLog(TripDao.class,"Invitation regarding "+denierUsername+" denied successfully.");
                                return true;
                            } else {
                                Logger.printLogFatal(TripDao.class,"Could not remove invitation from trip, trip does not exist, or user is no longed invited.");
                                return false;
                            }
                        }
                );
    }

    public Task<Boolean> sendNewInvite(String tripUid, String usernameToInvite)
    {
        if( tripUid == null || tripUid.isEmpty()) {
            return Tasks.forException(new Exception("Error provided null/empty tripUid argument"));
        }
        else if( usernameToInvite == null|| usernameToInvite.isEmpty() ){
            return Tasks.forException(new Exception("Error provided null/empty usernameToInvite argument"));
        }
        DocumentReference docRef = database.collection(KEY_COLLECTION_TRIPS).document(tripUid);

        return docRef.update(KEY_TRIP_INVITED_TO_JOIN_USERNAMES, FieldValue.arrayUnion(usernameToInvite))
                .continueWith( task -> {
                    if(task.isSuccessful())
                    {
                        return true;
                    }
                    else{
                        return false;
                    }
                });
    }

    public Task<Boolean> setTripComplete(String tripUid)
    {
        if( tripUid == null || tripUid.isEmpty()) {
            return Tasks.forException(new Exception("Error provided null/empty tripUid argument"));
        }
        DocumentReference docRef = database.collection(KEY_COLLECTION_TRIPS).document(tripUid);

        return  docRef.update(KEY_TRIP_IS_COMPLETED,true)
                .continueWith( task -> {
                    if(task.isSuccessful())
                    {
                        return true;
                    }
                    else{
                        return false;
                    }
                });
    }

    public Task<Boolean> putNewTrip(Trip trip)
    {
        CollectionReference collectionReference = database.collection(KEY_COLLECTION_TRIPS);

        HashMap<String,Object> tripHashMap = new HashMap<>();

        tripHashMap.put(KEY_TRIP_DRIVER_USERNAME,trip.getDriverUsername());
        tripHashMap.put(KEY_TRIP_START_LOCATION,trip.getStartLocation());
        tripHashMap.put(KEY_TRIP_END_LOCATION,trip.getEndLocation());
        tripHashMap.put(KEY_TRIP_START_TIME,trip.getStartTime());
        tripHashMap.put(KEY_TRIP_DATE,trip.getTripDate());
        tripHashMap.put(KEY_TRIP_DATE_CREATED,new Date()); // date now
        tripHashMap.put(KEY_TRIP_MAX_NUM_OF_RIDERS, trip.getMaxNumOfRiders());
        tripHashMap.put(KEY_TRIP_RIDER_USERNAMES, new ArrayList<>()); // no riders at time of creation
        tripHashMap.put(KEY_TRIP_INVITED_TO_JOIN_USERNAMES,new ArrayList<>()); // no invited riders at time of creation
        tripHashMap.put(KEY_TRIP_IS_COMPLETED, false); // new trip can't be already completed
        tripHashMap.put(KEY_TRIP_RATING_COMPLETED_BY_ARRAY, new ArrayList<>()); // no ratings completed at time of creation

        return collectionReference
                .add(tripHashMap).continueWith( uploadTask -> {
                    if (!uploadTask.isSuccessful()) {
                        throw Objects.requireNonNull(uploadTask.getException()); // task fail, send/throw error
                    }
                    Logger.printLog(UserDao.class,"Trip successfully uploaded to firestore.");
                    return true; // successfully uploaded new trip to firestore
                });
    }
    public Task<Boolean> deleteTrip(String tripUid)
    {
        if(tripUid == null || tripUid.isEmpty() )
        {
            return Tasks.forException(new Exception("Error, provided null/empty tripUd"));
        }

        DocumentReference documentReference = database.collection(KEY_COLLECTION_TRIPS).document(tripUid);

        return documentReference.get().continueWithTask( task -> {
            if (task.isSuccessful()) {
                DocumentSnapshot document = task.getResult();
                if (document.exists()) {
                    return documentReference.delete().continueWith( deleteTask -> {
                        if (deleteTask.isSuccessful()) {
                            return true;
                        } else {
                            throw Objects.requireNonNull(deleteTask.getException()); // delete fail
                        }
                    });
                } else {
                    return Tasks.forResult(false); // document does not exist
                }
            } else {
                throw Objects.requireNonNull(task.getException()); // unexpected error
            }
        });
    }
}
