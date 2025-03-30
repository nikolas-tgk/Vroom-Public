package com.p17142.vroom.notifications;

import static com.p17142.vroom.utilities.Keys.FCM_API_KEY;

import android.util.Log;

import com.google.auth.oauth2.GoogleCredentials;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;



public class AccessToken {
    private static final String firebaseMessagingScope = "https://www.googleapis.com/auth/firebase.messaging";

    public String getAccessToken(){
        try{
            String jsonString = FCM_API_KEY;
            InputStream stream = new ByteArrayInputStream(jsonString.getBytes(StandardCharsets.UTF_8));
            List<String> list = new ArrayList<>();
            list.add(firebaseMessagingScope);
            GoogleCredentials googleCredentials = GoogleCredentials.fromStream(stream).createScoped(list);
            googleCredentials.refresh();

            return googleCredentials.getAccessToken().getTokenValue();
        } catch (IOException e)
        {
            Log.e("error",e.getMessage());
            return null;
        }
    }
}
