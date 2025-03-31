package com.p17142.vroom.ui.application.chat;

import static com.p17142.vroom.utilities.Constants.KEY_ACTIVITY_SWAP;
import static com.p17142.vroom.utilities.Constants.KEY_MOST_RECENT_RECEIVER;
import static com.p17142.vroom.utilities.Constants.KEY_UID;

import android.content.Intent;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentTransaction;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.google.firebase.firestore.DocumentChange;
import com.google.firebase.firestore.EventListener;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QuerySnapshot;
import com.p17142.vroom.R;
import com.p17142.vroom.adapters.RecentConversationAdapter;
import com.p17142.vroom.databinding.FragmentChatsMainBinding;
import com.p17142.vroom.listeners.ConversationListener;
import com.p17142.vroom.models.Message;
import com.p17142.vroom.models.User;
import com.p17142.vroom.utilities.Constants;
import com.p17142.vroom.utilities.PreferenceManager;

import java.util.ArrayList;
import java.util.List;


public class RecentConversationsFragment extends Fragment implements ConversationListener {
    // First Chat Screen, recent conversations screen

    private FragmentChatsMainBinding binding; // old name
    private PreferenceManager preferenceManager;
    private List<Message> conversations;
    private RecentConversationAdapter recentConversationAdapter;
    private FirebaseFirestore database;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, // set binding
                             Bundle savedInstanceState) {
        binding = FragmentChatsMainBinding.inflate(inflater,container,false);
        return  binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        setListeners(); // set listeners
        init();
        conversationsListener();
    }

    private void init(){
        preferenceManager = new PreferenceManager(requireContext());
        conversations = new ArrayList<>();
        recentConversationAdapter = new RecentConversationAdapter(conversations,this, preferenceManager.getString(KEY_UID)); // initialize adapter
        binding.recentConvertationsRecyclerView.setAdapter(recentConversationAdapter);
        database = FirebaseFirestore.getInstance();
    }

    private final EventListener<QuerySnapshot> eventListener = ((value, error) -> {
        if(error != null){
            return;
        }
        if(value != null){
            binding.errorMessageText.setVisibility(View.INVISIBLE);
            for(DocumentChange documentChange : value.getDocumentChanges()){
                if(documentChange.getType() == DocumentChange.Type.ADDED){
                    String senderUid = documentChange.getDocument().getString(Constants.KEY_SENDER_UID);
                    String receiverUid = documentChange.getDocument().getString(Constants.KEY_RECEIVER_UID);
                    Message message = new Message();
                    message.setSenderUid(senderUid);
                    message.setReceiverUid(receiverUid);
                    if(preferenceManager.getString(KEY_UID).equals(senderUid)) // can create an object here and use getDocument once..
                    {
                        message.setConversationImage(documentChange.getDocument().getString(Constants.KEY_RECEIVER_ENCODED_IMG));
                        message.setConversationName(documentChange.getDocument().getString(Constants.KEY_RECEIVER_USERNAME));
                        message.setConversationUid(documentChange.getDocument().getString(Constants.KEY_RECEIVER_UID));
                    } else {
                        message.setConversationImage(documentChange.getDocument().getString(Constants.KEY_SENDER_ENCODED_IMG));
                        message.setConversationName(documentChange.getDocument().getString(Constants.KEY_SENDER_USERNAME));
                        message.setConversationUid(documentChange.getDocument().getString(Constants.KEY_SENDER_UID));
                    }
                    message.setMessage(documentChange.getDocument().getString(Constants.KEY_LAST_MESSAGE));
                    message.setDate(documentChange.getDocument().getDate(Constants.KEY_DATETIME));
                    message.setMostRecentReceiver(documentChange.getDocument().getString(Constants.KEY_MOST_RECENT_RECEIVER));
                    conversations.add(message);
                } else if(documentChange.getType() == DocumentChange.Type.MODIFIED){
                    for(int i = 0; i < conversations.size(); i++) // new profile pic refresh logic TO-DO
                    {
                        String senderUid = documentChange.getDocument().getString(Constants.KEY_SENDER_UID);
                        String receiverUid = documentChange.getDocument().getString(Constants.KEY_RECEIVER_UID);
                        if(conversations.get(i).getSenderUid().equals(senderUid) && conversations.get(i).getReceiverUid().equals(receiverUid)){
                            conversations.get(i).setMessage(documentChange.getDocument().getString(Constants.KEY_LAST_MESSAGE));
                            conversations.get(i).setDate(documentChange.getDocument().getDate(Constants.KEY_DATETIME));
                            conversations.get(i).setMostRecentReceiver(documentChange.getDocument().getString(KEY_MOST_RECENT_RECEIVER));
                            break;
                        }
                    }
                }
            }
            conversations.sort((obj1, obj2) -> obj2.getDate().compareTo(obj1.getDate()));
            recentConversationAdapter.notifyDataSetChanged();
            binding.recentConvertationsRecyclerView.smoothScrollToPosition(0);
            binding.recentConvertationsRecyclerView.setVisibility(View.VISIBLE);
            binding.loadingMsgsBar.setVisibility(View.GONE);
            if(conversations.isEmpty())
            {
                showErrorMessage("No active conversations yet.");
            }
        }
    });

    private void conversationsListener(){ //listenConversations
        database.collection(Constants.KEY_COLLECTION_CONVERSATIONS)
                .whereEqualTo(Constants.KEY_SENDER_UID,preferenceManager.getString(KEY_UID))
                .addSnapshotListener(eventListener);
        database.collection(Constants.KEY_COLLECTION_CONVERSATIONS)
                .whereEqualTo(Constants.KEY_RECEIVER_UID,preferenceManager.getString(KEY_UID))
                .addSnapshotListener(eventListener);
    }

    private void setListeners(){
        binding.composeNewMsgButton.setOnClickListener( v ->
                replaceFragment(new SelectUserToChatFragment()));
    }

    private void showErrorMessage(String message){
        binding.errorMessageText.setVisibility(View.VISIBLE);

        binding.errorMessageText.setText(message);
    }

    private void replaceFragment(Fragment fragment){
        FragmentManager fragmentManager = getParentFragmentManager();
        FragmentTransaction fragmentTransaction = fragmentManager.beginTransaction();
        fragmentTransaction.addToBackStack("mainChatFragment");
        fragmentTransaction.replace(R.id.frameLayout,fragment);
        fragmentTransaction.commit();
    }

    @Override
    public void onConversationClicked(User user) { // HERE CHECK FOR CHANGES PUT FRAGMENT ON HOLD TO-DO
        preferenceManager.putBoolean(KEY_ACTIVITY_SWAP,true);
        Intent intent = new Intent(requireContext(),ChatActivity.class);
        intent.putExtra(Constants.KEY_USER,user);
        startActivity(intent);
    }
}