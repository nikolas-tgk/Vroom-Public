package com.p17142.vroom.notifications;

import static com.p17142.vroom.utilities.Constants.KEY_NOTIFICATION_SENDER;
import static com.p17142.vroom.utilities.Constants.KEY_NOTIFICATION_TRIP_UID;
import static com.p17142.vroom.utilities.Constants.KEY_NOTIFICATION_TYPE;
import static com.p17142.vroom.utilities.Keys.POST_URL;

import android.content.Context;

import com.android.volley.AuthFailureError;
import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.toolbox.JsonObjectRequest;
import com.android.volley.toolbox.Volley;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.HashMap;
import java.util.Map;

public class FcmNotificationSender {

    private final String userFcmToken;
    private final String title;
    private final String body;
    private final Context context;
    private final String postUrl = POST_URL;

    private final String fromUsername;
    private final String tripUid;
    private final String notifType;

    public FcmNotificationSender(String userFcmToken, String title, String body, Context context, String fromUsername, String tripUid, String notifType) {
        this.userFcmToken = userFcmToken;
        this.title = title;
        this.body = body;
        this.context = context;
        this.fromUsername = fromUsername;
        this.tripUid = tripUid;
        this.notifType = notifType;
    }

    public void sendNotification(){
        RequestQueue requestQueue = Volley.newRequestQueue(context);
        JSONObject mainObj = new JSONObject();

        try{
            JSONObject messageObject = new JSONObject();

            JSONObject notificationObject = new JSONObject();
            notificationObject.put("title",title);
            notificationObject.put("body",body);

            // data object for custom key-value pairs
            JSONObject dataObject = new JSONObject();
            dataObject.put(KEY_NOTIFICATION_SENDER, fromUsername);
            dataObject.put(KEY_NOTIFICATION_TRIP_UID, tripUid);
            dataObject.put(KEY_NOTIFICATION_TYPE, notifType);

            messageObject.put("token",userFcmToken);
            messageObject.put("notification",notificationObject);
            messageObject.put("data", dataObject);

            mainObj.put("message",messageObject);

            JsonObjectRequest request = new JsonObjectRequest(Request.Method.POST, postUrl, mainObj, response -> {

            }, volleyError -> {

            } ) {
                @Override
                public Map<String, String> getHeaders() throws AuthFailureError {
                    AccessToken accessToken = new AccessToken();
                    String accessKey = accessToken.getAccessToken();
                    Map<String,String> header = new HashMap<>();
                    header.put("content-type","application/json");
                    header.put("authorization","Bearer "+accessKey);
                    return header;
                }
            };
            requestQueue.add(request);

        } catch (JSONException e) {
            e.printStackTrace();
        }

    }
}
