package com.p17142.vroom.data.database;

import com.google.firebase.firestore.FirebaseFirestore;

public class FirestoreDatabase {
    private static final FirebaseFirestore INSTANCE = FirebaseFirestore.getInstance();

    private FirestoreDatabase(){}

    public static FirebaseFirestore getInstance(){
        return INSTANCE;
    }
}
