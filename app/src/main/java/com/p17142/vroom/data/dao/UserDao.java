package com.p17142.vroom.data.dao;

import static com.p17142.vroom.utilities.Constants.KEY_COLLECTION_CONVERSATIONS;
import static com.p17142.vroom.utilities.Constants.KEY_COLLECTION_USERS;
import static com.p17142.vroom.utilities.Constants.KEY_EMAIL;
import static com.p17142.vroom.utilities.Constants.KEY_FCM_TOKEN;
import static com.p17142.vroom.utilities.Constants.KEY_IMG_URI;
import static com.p17142.vroom.utilities.Constants.KEY_IS_IN_CHAT_WITH;
import static com.p17142.vroom.utilities.Constants.KEY_IS_ONLINE;
import static com.p17142.vroom.utilities.Constants.KEY_RECEIVER_ENCODED_IMG;
import static com.p17142.vroom.utilities.Constants.KEY_RECEIVER_USERNAME;
import static com.p17142.vroom.utilities.Constants.KEY_SENDER_ENCODED_IMG;
import static com.p17142.vroom.utilities.Constants.KEY_SENDER_USERNAME;
import static com.p17142.vroom.utilities.Constants.KEY_UID;
import static com.p17142.vroom.utilities.Constants.KEY_USERNAME;
import static com.p17142.vroom.utilities.Constants.KEY_USER_NUM_OF_RATINGS;
import static com.p17142.vroom.utilities.Constants.KEY_USER_RATING_SUM;
import static com.p17142.vroom.utilities.Constants.KEY_USER_TRIPS_COMPLETED_AS_DRIVER;
import static com.p17142.vroom.utilities.Constants.KEY_USER_TRIPS_COMPLETED_AS_RIDER;
import static com.p17142.vroom.utilities.Constants.KEY_USER_UNIQUE_PEOPLE_MET;
import static com.p17142.vroom.utilities.Constants.NO_IMG_URI;

import com.google.android.gms.tasks.Task;
import com.google.android.gms.tasks.Tasks;

import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QuerySnapshot;
import com.p17142.vroom.data.database.FirestoreDatabase;
import com.p17142.vroom.models.User;
import com.p17142.vroom.utilities.Constants;
import com.p17142.vroom.utilities.Logger;

import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

public class UserDao
{
    private final FirebaseFirestore database;

    private static final UserDao INSTANCE = new UserDao();

    public static UserDao getInstance(){
        return INSTANCE;
    }

    private UserDao(){
        database = FirestoreDatabase.getInstance();
    }

    public Task<User> getUserByUid(String userUid){

        if(userUid==null){
            return Tasks.forException(new Exception("Uid is null"));
        }
        Query query = database.collection(KEY_COLLECTION_USERS)
                .whereEqualTo(KEY_UID, userUid);

        return query.get().continueWith( task -> {
            if (task.isSuccessful() && !task.getResult().isEmpty()) {
                DocumentSnapshot document = task.getResult().getDocuments().get(0);
                return new User(
                        document.getString(Constants.KEY_UID),
                        document.getString(Constants.KEY_USERNAME),
                        document.getString(Constants.KEY_EMAIL),
                        document.getString(Constants.KEY_IMG_URI),
                        Instant.parse(document.getString(Constants.KEY_USER_CREATED_TIMESTAMP)),
                        document.getDouble(KEY_USER_RATING_SUM).intValue(),
                        document.getDouble(KEY_USER_NUM_OF_RATINGS).intValue(),
                        document.getDouble(KEY_USER_TRIPS_COMPLETED_AS_DRIVER).intValue(),
                        document.getDouble(KEY_USER_TRIPS_COMPLETED_AS_RIDER).intValue(),
                        document.getDouble(KEY_USER_UNIQUE_PEOPLE_MET).intValue()
                );
            } else {
                throw new Exception("User not found in Firestore, database inconsistency?");
            }
        });
    }

    public Task<User> getUserByUsername(String username){
        if(username == null)
        {
            return Tasks.forException(new Exception("Username is null"));
        }
        Query query = database.collection(KEY_COLLECTION_USERS)
                .whereEqualTo(KEY_USERNAME,username);
        return query.get().continueWith( task -> {
            if(task.isSuccessful() && !task.getResult().isEmpty()){
                DocumentSnapshot document = task.getResult().getDocuments().get(0);
                return new User(
                        document.getString(Constants.KEY_UID),
                        document.getString(Constants.KEY_USERNAME),
                        document.getString(Constants.KEY_EMAIL),
                        document.getString(Constants.KEY_IMG_URI),
                        Instant.parse(document.getString(Constants.KEY_USER_CREATED_TIMESTAMP)),
                        document.getDouble(KEY_USER_RATING_SUM).intValue(),
                        document.getDouble(KEY_USER_NUM_OF_RATINGS).intValue(),
                        document.getDouble(KEY_USER_TRIPS_COMPLETED_AS_DRIVER).intValue(),
                        document.getDouble(KEY_USER_TRIPS_COMPLETED_AS_RIDER).intValue(),
                        document.getDouble(KEY_USER_UNIQUE_PEOPLE_MET).intValue()
                );
            }
            else if(task.isSuccessful() && task.getResult().isEmpty())
            {
                Logger.printLogError(UserDao.class,"User not found, did user get deleted?");
                return null;
            }
            else{
                throw Objects.requireNonNull(task.getException());
            }
        });
    }

    public Task<List<User>> getUsersByUsernameList(List<String> usernameList) {

        List<Task<QuerySnapshot>> tasks = new ArrayList<>(); // list that holds all tasks to take place
        List<User> users = new ArrayList<>();

        // loop through all usernames and create a query for each one
        for (String username : usernameList) {
            Query query = database.collection(KEY_COLLECTION_USERS).whereEqualTo(KEY_USERNAME, username);
            Task<QuerySnapshot> task = query.get();
            tasks.add(task);
        }

        // wait for all tasks to complete
        return Tasks.whenAllSuccess(tasks).continueWith( task -> {
            if ( task.isSuccessful() ) {
                for (Task<QuerySnapshot> queryTask : tasks) {
                    if (queryTask.isSuccessful() && !queryTask.getResult().isEmpty()) {
                        DocumentSnapshot document = queryTask.getResult().getDocuments().get(0);
                        User user = new User(
                                document.getString(Constants.KEY_UID),
                                document.getString(Constants.KEY_USERNAME),
                                document.getString(Constants.KEY_EMAIL),
                                document.getString(Constants.KEY_IMG_URI),
                                Instant.parse(document.getString(Constants.KEY_USER_CREATED_TIMESTAMP)),
                                document.getDouble(KEY_USER_RATING_SUM).intValue(),
                                document.getDouble(KEY_USER_NUM_OF_RATINGS).intValue(),
                                document.getDouble(KEY_USER_TRIPS_COMPLETED_AS_DRIVER).intValue(),
                                document.getDouble(KEY_USER_TRIPS_COMPLETED_AS_RIDER).intValue(),
                                document.getDouble(KEY_USER_UNIQUE_PEOPLE_MET).intValue()
                        );
                        users.add(user);
                    }
                }
                return users;
            } else {
                throw Objects.requireNonNull(task.getException()); // error throw
            }
        });
    }

    public Task<Boolean> checkIfUsernameExists(String username) {
        return database.collection(KEY_COLLECTION_USERS).document(username).get()
                .continueWith( task -> {
                    if (!task.isSuccessful()) {
                        throw Objects.requireNonNull(task.getException());  // will activate on failure listener attached on method call
                    }
                    else{
                        return task.getResult().exists(); // true or false depending if document exists
                    }
                });
    }

    public Task<Boolean> checkIfEmailExists(String email) {
        return database.collection(KEY_COLLECTION_USERS).whereEqualTo(KEY_EMAIL,email).get()
                .continueWith( task -> {
                    if(!task.isSuccessful()){
                        throw Objects.requireNonNull(task.getException());
                    }
                    else{
                        return task.getResult().isEmpty(); // true = email does not exist, false = email already taken
                    }
                });
    }

    // first checks if username document does not already exists on firestore, if not, uploads the new user document
    public Task<Boolean> uploadUserIfNotExists(String uid, String username, String email) {

        DocumentReference docRef = database.collection(Constants.KEY_COLLECTION_USERS).document(username);

        return docRef.get()
                .continueWithTask( task -> {
                    if (!task.isSuccessful()) {
                        throw Objects.requireNonNull(task.getException()); // task fail, send error
                    }
                    // check if username already document exists
                    if (task.getResult().exists()) {
                        // Username already exists
                        return Tasks.forResult(false);
                    } else {
                        // prepare user object for upload
                        Map<String, Object> user = new HashMap<>();
                        user.put(Constants.KEY_USERNAME, username);
                        user.put(Constants.KEY_EMAIL, email);
                        user.put(Constants.KEY_IMG_URI, NO_IMG_URI);
                        user.put(Constants.KEY_UID, uid);
                        user.put(Constants.KEY_USER_CREATED_TIMESTAMP, Instant.now().toString());
                        user.put(Constants.KEY_USER_TRIPS_COMPLETED_AS_DRIVER, 0); // TO-DO set default values inside user class, maybe convert this to constructor creation
                        user.put(Constants.KEY_USER_TRIPS_COMPLETED_AS_RIDER, 0);
                        user.put(Constants.KEY_USER_UNIQUE_PEOPLE_MET, 0);
                        user.put(Constants.KEY_USER_RATING_SUM, 0); // default value
                        user.put(Constants.KEY_USER_NUM_OF_RATINGS, 0);
                        // upload
                        return docRef.set(user)
                                .continueWith( uploadTask -> {
                                    if (!uploadTask.isSuccessful()) {
                                        throw Objects.requireNonNull(uploadTask.getException()); // task fail, send/throw error
                                    }
                                    Logger.printLog(UserDao.class,"User successfully uploaded to firestore.");
                                    return true; // successfully uploaded new user to firestore
                                });
                    }
                });
    }

    public Task<Boolean> updateUsersOnTripComplete(List<String> riderUsernames, String driverUsername){
        List<Task<Void>> tasks = new ArrayList<>();
        int peopleMetThisTrip = riderUsernames.size(); // ( +1 driver -1 (self) ) // THIS WAY IT IS NOT ACTUALLY UNIQUE - TO-DO

        DocumentReference docRefDriver = database.collection(KEY_COLLECTION_USERS).document(driverUsername);
        Task<Void> task1 = docRefDriver.update(KEY_USER_UNIQUE_PEOPLE_MET, FieldValue.increment(peopleMetThisTrip), KEY_USER_TRIPS_COMPLETED_AS_DRIVER,FieldValue.increment(1));
        tasks.add(task1);

        for (String riderUsername: riderUsernames
             ) {
            DocumentReference docRefRider = database.collection(KEY_COLLECTION_USERS).document(riderUsername);
            Task<Void> taskRiderX = docRefRider.update(KEY_USER_UNIQUE_PEOPLE_MET,FieldValue.increment(peopleMetThisTrip), KEY_USER_TRIPS_COMPLETED_AS_RIDER,FieldValue.increment(1));
            tasks.add(taskRiderX);
        }

        return Tasks.whenAllComplete(tasks).continueWith( aVoid -> true);
    }

    public Task<Boolean> deleteFCMToken(String username){
        if(username == null)
        {
            return Tasks.forException(new Exception("Username is null"));
        }
        DocumentReference docRef = database.collection(Constants.KEY_COLLECTION_USERS).document(username);
        HashMap<String, Object> fcmPair = new HashMap<>();
        fcmPair.put(Constants.KEY_FCM_TOKEN,FieldValue.delete());
        return docRef.update(fcmPair).continueWith( task -> {
            return task.isSuccessful();
        });
    }

    public Task<Boolean> updateProfileImageOnConversations(String username, String encodedImage)
    {
        // should change database structure in the future.

        // two different queries are needed depending on who started the conversation first.
        Query query = database.collection(KEY_COLLECTION_CONVERSATIONS).whereEqualTo(KEY_RECEIVER_USERNAME,username);

        Query query2 = database.collection(KEY_COLLECTION_CONVERSATIONS).whereEqualTo(KEY_SENDER_USERNAME,username);

        List<Task<QuerySnapshot>> retrieveTasks = new ArrayList<>();

        Task<QuerySnapshot> retrieve1 = query.get();
        Task<QuerySnapshot> retrieve2 = query2.get();
        retrieveTasks.add(retrieve1);
        retrieveTasks.add(retrieve2);
        // when all complete returns List<Task<?>> instead of List<Task<QuerySnapshot>>, need to cast ? to QuerySnapshot on task success individually.
        return Tasks.whenAllComplete(retrieveTasks).continueWithTask(result -> {
            List<Task<QuerySnapshot>> querySnapshotTasks = new ArrayList<>();
            for (Task<?> task : result.getResult()) {
                if (task.isSuccessful()) {
                    querySnapshotTasks.add((Task<QuerySnapshot>) task);
                }
            }

            for (Task<QuerySnapshot> querySnapshotTask : querySnapshotTasks) {
                QuerySnapshot querySnapshot = querySnapshotTask.getResult();
                if (querySnapshot != null) {
                    for (DocumentSnapshot document : querySnapshot.getDocuments()) {
                        if(document.getString(KEY_SENDER_USERNAME).equals(username))
                        {
                            document.getReference().update(KEY_SENDER_ENCODED_IMG, encodedImage);
                        }
                        else if (document.getString(KEY_RECEIVER_USERNAME).equals(username)){
                            document.getReference().update(KEY_RECEIVER_ENCODED_IMG, encodedImage);
                        }
                    }
                    }
                }
            return Tasks.forResult(true);
            });
    }

    public Task<Boolean> updateProfileImage(String username, String encodedImage){

        DocumentReference docRef = database.collection(KEY_COLLECTION_USERS).document(username);

        return docRef.update(KEY_IMG_URI,encodedImage).continueWithTask( task -> {
           if(task.isSuccessful()) {
               return updateProfileImageOnConversations(username,encodedImage); // need to also update that because of storing encodedImage also in convos
           }
               else{
                  return Tasks.forResult(false);
               }
        });}

    public Task<Boolean> updateFcmToken(String username, String fcmToken)
    {
        DocumentReference documentReference = database.collection(KEY_COLLECTION_USERS).document(username);

        return documentReference.update(KEY_FCM_TOKEN,fcmToken).continueWith( task-> {
           return task.isSuccessful();
        });
    }

    public Task<String> getFcmToken(String username){
        if(username == null || username.isEmpty())
        {
            return null;
        }
        DocumentReference documentReference = database.collection(KEY_COLLECTION_USERS).document(username);

        return documentReference.get().continueWith( task -> {
            if(task.isSuccessful() && task.getResult() != null)
            {
                return task.getResult().getString(KEY_FCM_TOKEN);

            }
            return null;
        });

    }

    public Task<Void> updateOnline(String username)
    {
        if(username == null || username.isEmpty())
        {
            return Tasks.forResult(null);
        }

        DocumentReference docRef = database.collection(KEY_COLLECTION_USERS).document(username);

        return docRef.update(KEY_IS_ONLINE,true);
    }

    public Task<Void> updateOffline(String username)
    {
        if(username == null || username.isEmpty())
        {
            return Tasks.forResult(null);
        }

        DocumentReference docRef = database.collection(KEY_COLLECTION_USERS).document(username);

        return  docRef.update(KEY_IS_ONLINE,false);
    }

    public Task<Void> updateIamInChatWith(String username, String receiverUsername)
    {
        if(username == null || username.isEmpty())
        {
            return Tasks.forResult(null);
        }

        DocumentReference docRef = database.collection(KEY_COLLECTION_USERS).document(username);

        return docRef.update(KEY_IS_IN_CHAT_WITH,receiverUsername);
    }

    // returns fcm token of target user on negative scenario, else null
    public Task<String> isInChatWithMe(String me, String targetUsername)
    {
        if(me == null || me.isEmpty() || targetUsername == null || targetUsername.isEmpty())
        {
            return Tasks.forResult(null);
        }
        DocumentReference docRef = database.collection(KEY_COLLECTION_USERS).document(targetUsername);

        return docRef.get().continueWith( task -> {
            if(task.isSuccessful() && task.getResult().exists())
            {
                DocumentSnapshot document = task.getResult();
                String chatter = document.getString(KEY_IS_IN_CHAT_WITH); // if this is == me, target is in chat screen with user
                String targetFcm = document.getString(KEY_FCM_TOKEN); // if this is null, target has signed out of app
                if(targetFcm!= null  && ( chatter == null || !chatter.equals(me)))
                {
                    return targetFcm;
                }
                else{
                    return null;
                }
            }
            return null;
        });
    }

    // returns fcm token if available, else is null
    public Task<String> provideFcm(String targetUsername){

        if(targetUsername == null || targetUsername.isEmpty())
        {
            return Tasks.forResult(null);
        }

        DocumentReference docRef = database.collection(KEY_COLLECTION_USERS).document(targetUsername);

        return docRef.get().continueWith( task -> {
            if(task.isSuccessful() && task.getResult().exists())
            {
                DocumentSnapshot document = task.getResult();

                return document.getString(KEY_FCM_TOKEN); // if this is null, target has signed out of app
            }
            return null;
        });
    }

}

