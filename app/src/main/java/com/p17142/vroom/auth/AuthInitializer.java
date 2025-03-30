package com.p17142.vroom.auth;

import com.google.firebase.auth.FirebaseAuth;

public class AuthInitializer {
    private static final FirebaseAuth INSTANCE = FirebaseAuth.getInstance();

    private  AuthInitializer() {} // private prevents instantiation // can remove

    public static FirebaseAuth getInstance() {
        return INSTANCE;
    }
}
