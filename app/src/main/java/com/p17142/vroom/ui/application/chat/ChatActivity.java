package com.p17142.vroom.ui.application.chat;

import static com.p17142.vroom.utilities.Constants.CHAT_ACTIVITY;
import static com.p17142.vroom.utilities.Constants.INVITE_USER_FRAGMENT;
import static com.p17142.vroom.utilities.Constants.KEY_ACTIVITY_SWAP;
import static com.p17142.vroom.utilities.Constants.KEY_COLLECTION_USERS;
import static com.p17142.vroom.utilities.Constants.KEY_DESTINATION_FRAGMENT;
import static com.p17142.vroom.utilities.Constants.KEY_IS_ONLINE;
import static com.p17142.vroom.utilities.Constants.KEY_MOST_RECENT_RECEIVER;
import static com.p17142.vroom.utilities.Constants.KEY_NEW_MSG_NOTIFICATION;
import static com.p17142.vroom.utilities.Constants.KEY_RECEIVER_USERNAME;
import static com.p17142.vroom.utilities.Constants.KEY_TRIP_LIST;
import static com.p17142.vroom.utilities.Constants.KEY_USER;
import static com.p17142.vroom.utilities.Constants.KEY_USERNAME;
import static com.p17142.vroom.utilities.Constants.KEY_SOURCE;
import static com.p17142.vroom.utilities.Constants.THIRD_USER_PROFILE_FRAGMENT;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.activity.OnBackPressedCallback;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.firebase.firestore.DocumentChange;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.EventListener;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QuerySnapshot;
import com.p17142.vroom.R;
import com.p17142.vroom.adapters.DirectMessageAdapter;
import com.p17142.vroom.auth.AuthManager;
import com.p17142.vroom.data.dao.TripDao;
import com.p17142.vroom.data.dao.UserDao;
import com.p17142.vroom.databinding.ActivityChatBinding;
import com.p17142.vroom.models.Message;
import com.p17142.vroom.models.Trip;
import com.p17142.vroom.models.User;
import com.p17142.vroom.notifications.FcmNotificationSender;
import com.p17142.vroom.ui.authentication.LoginActivity;
import com.p17142.vroom.utilities.Constants;
import com.p17142.vroom.utilities.Logger;
import com.p17142.vroom.utilities.ImageUtils;
import com.p17142.vroom.utilities.PreferenceManager;
import com.p17142.vroom.ui.application.MainActivity;
import com.p17142.vroom.ui.RootActivity;

import java.io.Serializable;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Objects;

import kotlin.collections.ArrayDeque;

public class ChatActivity extends RootActivity {
    // 1 to 1 Chatting screen

    private ActivityChatBinding binding;
    private User receiver = null;
    private PreferenceManager preferenceManager;
    private FirebaseFirestore db;
    private DirectMessageAdapter messageAdapter;
    private String conversationUid;
    private final List<Trip> invitableTrips = new ArrayList<>();
    private List<Message> messages;
    private final UserDao userDao = UserDao.getInstance();
    private final TripDao tripDao = TripDao.getInstance();
    private String loggedUsername = "";
    private boolean isReceiverOnline = false;
    private String isInChatWith = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);

        binding = ActivityChatBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        loading(true);
        init();
        setListeners();

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
                    Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
                    v.setPadding(systemBars.left, 0, systemBars.right, systemBars.bottom);
                    return insets;});
    }

    private void init(){
        preferenceManager = new PreferenceManager(getApplicationContext());
        if(!checkAuthorization()) // needs preference manager to be initialized first
        {
            return;
        }
        loggedUsername = preferenceManager.getString(KEY_USERNAME);
        loadReceiverDetails();
    }

    private boolean checkAuthorization(){ // possibility of user being logged off but then clicked on new message notification, force log out.
        if(!AuthManager.isUserSignedIn() || !preferenceManager.getBoolean(Constants.KEY_IS_SIGNED)){
            Logger.printLogError(ChatActivity.class,"Illegal authorization, force logout.");
            preferenceManager.clear();
            AuthManager.signOut();
            Toast.makeText(this,"You have been logged out.",Toast.LENGTH_SHORT).show();
            startActivity(new Intent(this, LoginActivity.class)); // this will throw null loggedUsername <- preferenceManager on RootActivity, handling this specifically onResume-onCreate-onPause overrides with if condition.
            this.finish();
            return false;
        }
        else{
            Logger.printLog(ChatActivity.class,"User authorized");
            return true;
        }
    }

    private void getInvitableTrips(){
        tripDao.getAllTripsByDriverUsername(loggedUsername,loggedUsername)
                .addOnSuccessListener( ownedTrips -> {
                    if(ownedTrips.isEmpty())
                    {
                        // no owned trips found
                    }
                    invitableTrips.clear();
                    for (Trip ownedTrip : ownedTrips) {
                        if(ownedTrip.hasTripStartTimeDateElapsed())
                        {
                            // trip has already started, can't invite anymore
                        } else if (ownedTrip.areMaxRiders()) {
                            // trip already full
                        } else if(ownedTrip.isInvited(receiver.getUsername()))
                        {
                            // user has a pending invite on this trip already
                        } else if (ownedTrip.isRider(receiver.getUsername())) {
                            
                        } else{
                            //ownedTrip.setDriverImageUri(loggedUserImageUri); // local fetch, less load on database
                            invitableTrips.add(ownedTrip);
                            //invitableTrips.get(invitableTrips.indexOf(ownedTrip)).setDriverImageUri(loggedUserImageUri);
                        }
                    }
                    if(!invitableTrips.isEmpty())
                    {
                        binding.inviteButton.setVisibility(View.VISIBLE);
                    }
                });
    }

    private void loadReceiverDetails(){
        //receiver = MainActivity.getClickedUser();
        Bundle bundle = getIntent().getExtras();
        if (bundle != null) {
            db = FirebaseFirestore.getInstance();
            if(bundle.get(KEY_USER) != null)
            {
                receiver = (User) bundle.get(Constants.KEY_USER);
                binding.usernameTextView.setText(receiver.getUsername());
                binding.profileImageView.setImageBitmap(ImageUtils.decodeImage(receiver.getImageUri()));
                if(receiver.getUsername() != null && !loggedUsername.isEmpty()){
                    Logger.printLog(ChatActivity.class, "Loaded receiver using serialized user object.");
                    isInChatWith = receiver.getUsername();
                    userDao.updateIamInChatWith(loggedUsername,receiver.getUsername());
                    messages = new ArrayDeque<>();
                    messageAdapter = new DirectMessageAdapter(ImageUtils.decodeImage(receiver.getImageUri()), messages,preferenceManager.getString(Constants.KEY_UID));
                    binding.chatRecyclerView.setAdapter(messageAdapter);

                    getInvitableTrips();
                    listenMessages();
                    loading(false);
                }

            }
            else if(bundle.getString(KEY_RECEIVER_USERNAME) != null)
            {
                String receiverUsername = bundle.getString(KEY_RECEIVER_USERNAME);
                userDao.getUserByUsername(receiverUsername)
                        .addOnSuccessListener( result -> {
                            if(result!=null)
                            {
                                receiver = result;
                                binding.usernameTextView.setText(receiver.getUsername());
                                binding.profileImageView.setImageBitmap(ImageUtils.decodeImage(receiver.getImageUri()));
                                if(receiver.getUsername() != null && !loggedUsername.isEmpty()){
                                    Logger.printLog(ChatActivity.class, "Loaded receiver from database, using receiver username. (Notification source)");
                                    isInChatWith = receiver.getUsername();
                                    userDao.updateIamInChatWith(loggedUsername,receiver.getUsername());
                                    messages = new ArrayDeque<>();
                                    messageAdapter = new DirectMessageAdapter(ImageUtils.decodeImage(receiver.getImageUri()), messages,preferenceManager.getString(Constants.KEY_UID));
                                    binding.chatRecyclerView.setAdapter(messageAdapter);
                                    getInvitableTrips();
                                    listenMessages();
                                    listenForReceiverOnlineStatus(); // needs this extra on first onResume wont execute
                                    loading(false);
                                }
                            }
                        });
            }
            else{
                Logger.printLogFatal(ChatActivity.class,"Error, missing both receiver object and receiver username");
            }

        } else {
            Logger.printLogFatal(ChatActivity.class,"Error, no bundle received");
        }
    }

    private void listenForReceiverOnlineStatus(){
        db.collection(KEY_COLLECTION_USERS).document(receiver.getUsername()).addSnapshotListener(ChatActivity.this, ((value, error) -> {
            if(error != null)
            {
                return;
            }
            if(value != null)
            {
                if( value.getBoolean(KEY_IS_ONLINE)  != null )
                {
                    isReceiverOnline = Objects.requireNonNull(value.getBoolean(KEY_IS_ONLINE));
                    if(isReceiverOnline)
                    {
                        binding.isOnlineHeaderText.setVisibility(View.VISIBLE);
                    }
                    else{
                        binding.isOnlineHeaderText.setVisibility(View.GONE);
                    }
                }
            }
        }));
    }

    private void sendMessage(String messageText){
        HashMap<String,Object> message = new HashMap<>(); // object cause we use type String and also type Date as second argument.
        message.put(Constants.KEY_SENDER_UID,preferenceManager.getString(Constants.KEY_UID));
        message.put(Constants.KEY_RECEIVER_UID,receiver.getUid());
        message.put(Constants.KEY_MESSAGE,messageText);
        message.put(Constants.KEY_DATETIME, new Date());
        if(conversationUid != null)
        {
            updateConversation(binding.inputMessageEditText.getText().toString());
        } else {
            HashMap<String, Object> conversation = new HashMap<>();
            conversation.put(Constants.KEY_SENDER_UID, preferenceManager.getString(Constants.KEY_UID)); // sendMessage is always used by the user writing on screen.
            conversation.put(Constants.KEY_SENDER_USERNAME, preferenceManager.getString(Constants.KEY_USERNAME));
            conversation.put(Constants.KEY_SENDER_ENCODED_IMG, preferenceManager.getString(Constants.KEY_IMG_URI));
            conversation.put(Constants.KEY_RECEIVER_UID,receiver.getUid());
            conversation.put(Constants.KEY_RECEIVER_USERNAME,receiver.getUsername());
            conversation.put(KEY_MOST_RECENT_RECEIVER,receiver.getUid());
            conversation.put(Constants.KEY_RECEIVER_ENCODED_IMG,receiver.getImageUri());
            conversation.put(Constants.KEY_LAST_MESSAGE,binding.inputMessageEditText.getText().toString());
            conversation.put(Constants.KEY_DATETIME,new Date());
            addConversation(conversation);
        }
        db.collection(Constants.KEY_COLLECTION_MESSAGES).add(message);
    }

    private void checkInputValidity()
    {
        String messageText = binding.inputMessageEditText.getText().toString().trim();
        if(!messageText.isEmpty())
        {
            sendMessage(messageText);
            userDao.isInChatWithMe(loggedUsername,receiver.getUsername()).addOnSuccessListener( targetFcmToken -> {
                if(targetFcmToken != null)
                {
                    FcmNotificationSender fcmNotificationSender = new FcmNotificationSender(targetFcmToken,
                            "Vroom Messaging",loggedUsername+": "+messageText,this,loggedUsername,"",KEY_NEW_MSG_NOTIFICATION);
                    fcmNotificationSender.sendNotification();
                    Logger.printLog(ChatActivity.class,"Notification sent from "+loggedUsername);
                }
                else{
                    Logger.printLog(ChatActivity.class,"Notification from "+loggedUsername+" did not fire.");
                }
            });
        }
        binding.inputMessageEditText.setText(null);
        closeKeyboard();
    }

    private String simplifyDate(Date date)
    {
        try{
            return new SimpleDateFormat("MMMM dd, yyyy - hh:mm a", Locale.getDefault()).format(date);
        }catch (Exception e){
            Logger.printLogFatal(this.getClass(),"Error null date retrieved");
            return "null";
        }
    }

    private void listenMessages(){
        db.collection(Constants.KEY_COLLECTION_MESSAGES)
                .whereEqualTo(Constants.KEY_SENDER_UID,preferenceManager.getString(Constants.KEY_UID))
                .whereEqualTo(Constants.KEY_RECEIVER_UID,receiver.getUid())
                .addSnapshotListener(eventListener);
        db.collection(Constants.KEY_COLLECTION_MESSAGES)
                .whereEqualTo(Constants.KEY_SENDER_UID,receiver.getUid())
                .whereEqualTo(Constants.KEY_RECEIVER_UID,preferenceManager.getString(Constants.KEY_UID))
                .addSnapshotListener(eventListener);
    }

    private final EventListener<QuerySnapshot> eventListener = ((value, error) -> {
       if(error != null)
       {
           return;
       }
       if(value != null)
       {
           int count = messages.size();
           for(DocumentChange documentChange : value.getDocumentChanges()){
               if(documentChange.getType()== DocumentChange.Type.ADDED){
                   Message message = new Message();
                   message.setSenderUid(documentChange.getDocument().getString(Constants.KEY_SENDER_UID));
                   message.setReceiverUid(documentChange.getDocument().getString(Constants.KEY_RECEIVER_UID));
                   message.setMessage(documentChange.getDocument().getString(Constants.KEY_MESSAGE));
                   message.setDateTime(simplifyDate(documentChange.getDocument().getDate(Constants.KEY_DATETIME)));
                   message.setDate(documentChange.getDocument().getDate(Constants.KEY_DATETIME));
                   messages.add(message);
               }
           }
           Collections.sort(messages, (msg1,msg2) -> msg1.getDate().compareTo(msg2.getDate()));
           if(count == 0)
           {
               messageAdapter.notifyDataSetChanged();
           } else{
               messageAdapter.notifyItemRangeInserted(messages.size(),messages.size());
               binding.chatRecyclerView.smoothScrollToPosition(messages.size()-1);
           }
           binding.chatRecyclerView.setVisibility(View.VISIBLE);
       }
       binding.chatProgressBar.setVisibility(View.GONE);
       if(conversationUid == null){
           checkForConversation();
       }
    });

    private final OnCompleteListener<QuerySnapshot> conversationOnCompleteListener = task -> {
        // if document found retrieve documentId, this documentId is the conversationUid
        if(task.isSuccessful() && task.getResult() != null  && !task.getResult().isEmpty()) {
            DocumentSnapshot documentSnapshot = task.getResult().getDocuments().get(0);
            conversationUid = documentSnapshot.getId(); // get the conversation id by getting the last document's Id. (returns document's Id)
        }
    };

    private void addConversation(HashMap<String, Object> conversation){
        db.collection(Constants.KEY_COLLECTION_CONVERSATIONS)
                .add(conversation)
                .addOnSuccessListener(documentReference -> conversationUid = documentReference.getId());
    }

    private void updateConversation(String directMessage){
        DocumentReference dr =
                db.collection(Constants.KEY_COLLECTION_CONVERSATIONS).document(conversationUid);
        dr.update(Constants.KEY_LAST_MESSAGE,directMessage, Constants.KEY_DATETIME, new Date(), KEY_MOST_RECENT_RECEIVER, receiver.getUid()); //  updated!
    }

    private void checkForConversation()
    {
        if(!messages.isEmpty())
        {
            checkForConversationOnFirestore(preferenceManager.getString(Constants.KEY_UID),receiver.getUid());
            checkForConversationOnFirestore(receiver.getUid(),preferenceManager.getString(Constants.KEY_UID));
        }
    }

    private void checkForConversationOnFirestore(String senderUid, String receiverUid){
        // look into convertations collection for the document with matching sender and receiver uid, continue with conversationOnCompleteListener
        db.collection(Constants.KEY_COLLECTION_CONVERSATIONS)
                .whereEqualTo(Constants.KEY_SENDER_UID,senderUid)
                .whereEqualTo(Constants.KEY_RECEIVER_UID,receiverUid)
                .get()
                .addOnCompleteListener(conversationOnCompleteListener);
    }

    private void closeKeyboard(){
        // code to close keyboard
        InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
        View view = getWindow().getDecorView().getRootView();
        if (imm != null && view != null
        ) {
            imm.hideSoftInputFromWindow(view.getWindowToken(), 0);
        }
    }

    @Override
    protected void onResume() {
        super.onResume();

        if(receiver != null && receiver.getUsername() != null && !loggedUsername.isEmpty()){
            listenForReceiverOnlineStatus();
            isInChatWith = receiver.getUsername();
            userDao.updateIamInChatWith(loggedUsername,receiver.getUsername());
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        if(receiver.getUsername() != null && !loggedUsername.isEmpty()){
            isInChatWith = null;
            userDao.updateIamInChatWith(loggedUsername,null);
        }
    }

    private void setListeners(){
        binding.sendFrameLayout.setOnClickListener( v -> checkInputValidity());
        binding.profileImageView.setOnClickListener( unused -> binding.usernameTextView.performClick());
        binding.usernameTextView.setOnClickListener( unused -> onUsernameTextClick());
        binding.inviteButton.setOnClickListener( unused -> onInviteButtonClick());

        binding.backButtonImageView.setOnClickListener( v -> {
            preferenceManager.putBoolean(KEY_ACTIVITY_SWAP,true);
            getOnBackPressedDispatcher().onBackPressed();
        });

        // custom on back press behavior
        OnBackPressedCallback callback = new OnBackPressedCallback(true) {
            @Override
            public void handleOnBackPressed() {
                preferenceManager.putBoolean(KEY_ACTIVITY_SWAP,true);
                setEnabled(false); // disable to allow back-press after custom logic exec
                getOnBackPressedDispatcher().onBackPressed();
            }
        };
        getOnBackPressedDispatcher().addCallback(this, callback);
    }

    private void onUsernameTextClick(){
        // go to main activity -> third user profile fragment
        preferenceManager.putBoolean(KEY_ACTIVITY_SWAP,true);
        Intent intent = new Intent(ChatActivity.this, MainActivity.class);
        intent.putExtra(KEY_DESTINATION_FRAGMENT, THIRD_USER_PROFILE_FRAGMENT);  // specify which fragment to load
        intent.putExtra(KEY_SOURCE, CHAT_ACTIVITY); // specify source
        intent.putExtra(KEY_USERNAME,receiver.getUsername()); // username to load
        startActivity(intent);
        finish();
    }

    private void onInviteButtonClick(){
        // go to main activity -> select trip for invite fragment
        preferenceManager.putBoolean(KEY_ACTIVITY_SWAP,true);
        Intent intent = new Intent(ChatActivity.this, MainActivity.class);
        intent.putExtra(KEY_DESTINATION_FRAGMENT, INVITE_USER_FRAGMENT); // specify which fragment to load
        intent.putExtra(KEY_SOURCE, CHAT_ACTIVITY); // specify source
        intent.putExtra(KEY_TRIP_LIST, (Serializable) invitableTrips); // invitable trips list
        intent.putExtra(KEY_USER,receiver); // user to invite
        startActivity(intent);
        finish();
    }

    private void loading(boolean isLoading){
        if(isLoading){
            binding.chatRecyclerView.setVisibility(View.INVISIBLE);
            binding.chatProgressBar.setVisibility(View.VISIBLE);
            binding.errorMessageText.setVisibility(View.INVISIBLE);
        }else{
            binding.chatProgressBar.setVisibility(View.INVISIBLE);
            binding.chatRecyclerView.setVisibility(View.VISIBLE);
        }
    }
}