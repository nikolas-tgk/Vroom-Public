package com.p17142.vroom.auth;

import android.content.Context;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseAuthInvalidCredentialsException;
import com.google.firebase.auth.FirebaseAuthInvalidUserException;
import com.p17142.vroom.utilities.Logger;
import com.p17142.vroom.utilities.PreferenceManager;

public class AuthManager
{
    private static final FirebaseAuth auth = AuthInitializer.getInstance();
    //private PreferenceManager preferenceManager;

    public AuthManager(Context context) {
        //this.preferenceManager = new PreferenceManager(context);
    }

    public static boolean isUserSignedIn(){
        return  auth.getCurrentUser() != null; // returns true if user is currently logged with Firebase Auth.
    }

    public static void signOut(){
        auth.signOut();
    }

    public String getSignedUid(){
        return auth.getUid();
    }

    public void signInUser(String email, String password, AuthCallback callback){
        auth.signInWithEmailAndPassword(email,password)
                .addOnSuccessListener( authResult -> {
                    callback.onSuccess();
                }).addOnFailureListener( e -> {
                    if (e instanceof FirebaseAuthInvalidCredentialsException) {
                        Logger.printLogError(AuthManager.class, "Invalid credentials.");
                        callback.onFailure("Invalid credentials.");
                    } else if (e instanceof FirebaseAuthInvalidUserException) {
                        Logger.printLogError(AuthManager.class, "User email does not exist.");
                        callback.onFailure("User does not exist.");
                    } else {
                        Logger.printLogError(AuthManager.class, "FirebaseAuth fail. Error: "+e.getMessage());
                        callback.onFailure("Authentication fail.");
                    }
                });
    }

    public void sendPasswordResetEmail(String email, AuthCallback callback){
        auth.sendPasswordResetEmail(email)
                .addOnCompleteListener( task -> {
                if(task.isSuccessful()){
                    Logger.printLog(AuthManager.class, "Reset password email sent successfully!");
                    callback.onSuccess();
                }else{
                    callback.onFailure("Invalid email address.");
                }
        });
    }

    public void registerUser(String email, String password, AuthCallback callback){
        auth.createUserWithEmailAndPassword(email,password).addOnCompleteListener( task -> {
            if(task.isSuccessful()){
                Logger.printLog(AuthManager.class, "User registered on FirebaseAuth");
                callback.onSuccess();
            }else{
                // this should not happen, means there is still a user stored in Auth while not in Firestore
                Logger.printLogFatal(AuthManager.class, "Database inconsistency? Error registering user on Auth:"+task.getException());
                callback.onFailure("Could not register user.");
            }
        }).addOnFailureListener( e -> {
            Logger.printLogFatal(AuthManager.class, "Error registering user on Auth with error: "+e);
            callback.onFailure("Check connection.");
        });
    }

    public interface AuthCallback {
        void onSuccess();
        void onFailure(String errorMessage);
    }

}
