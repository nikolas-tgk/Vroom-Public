package com.p17142.vroom.ui.application.chat;

import static com.p17142.vroom.utilities.Constants.KEY_ACTIVITY_SWAP;
import static com.p17142.vroom.utilities.Constants.KEY_FCM_TOKEN;
import static com.p17142.vroom.utilities.Constants.KEY_EMAIL;
import static com.p17142.vroom.utilities.Constants.KEY_IMG_URI;
import static com.p17142.vroom.utilities.Constants.KEY_USERNAME;
import static com.p17142.vroom.utilities.Constants.KEY_COLLECTION_USERS;

import android.content.Intent;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.p17142.vroom.adapters.UserAdapter;
import com.p17142.vroom.databinding.FragmentChatsUsersListBinding;
import com.p17142.vroom.listeners.UserListener;
import com.p17142.vroom.models.User;
import com.p17142.vroom.utilities.Constants;
import com.p17142.vroom.utilities.Logger;
import com.p17142.vroom.utilities.PreferenceManager;

import java.util.ArrayList;
import java.util.List;


public class SelectUserToChatFragment extends Fragment implements UserListener {
    // Select user from all users list, to chat, screen

    private FragmentChatsUsersListBinding binding;
    private PreferenceManager preferenceManager;
    private String currentUserUsername;
    private String loggedUsername;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        preferenceManager = new PreferenceManager(requireContext());
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        binding = FragmentChatsUsersListBinding.inflate(inflater,container,false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        loggedUsername = preferenceManager.getString(KEY_USERNAME);
        Logger.printLog(SelectUserToChatFragment.class,"PREF.MNGR. RETRIEVED: "+Constants.KEY_USERNAME+" == "+loggedUsername);
        setListeners();
        getUsers();
    }

    private void setListeners(){
        binding.backArrowImage.setOnClickListener( click -> {
            Logger.printLog(SelectUserToChatFragment.class,"Back icon press.");
            requireActivity().getOnBackPressedDispatcher().onBackPressed();
        });
    }

    private void getUsers(){
        loading(true);
        FirebaseFirestore database = FirebaseFirestore.getInstance();
        database.collection(KEY_COLLECTION_USERS)
                .get()
                .addOnCompleteListener(task -> {
                    loading(false);
                    if(task.isSuccessful() && task.getResult() != null)
                    {
                        List<User> users = new ArrayList<>();
                        for(QueryDocumentSnapshot queryDocumentSnapshot : task.getResult()){
                            //if(loggedUsername.equals(queryDocumentSnapshot.get("username"))) // works the same with current app firestore document structure
                            if(loggedUsername.equals(queryDocumentSnapshot.getId()))
                            {
                                Logger.printLog(SelectUserToChatFragment.class,"Username found! Skipping RecycleView entry");
                                continue;
                            }
                            else{
                                Logger.printLog(SelectUserToChatFragment.class,"New user added.. "+queryDocumentSnapshot.getString(KEY_USERNAME));
                                User user = new User(queryDocumentSnapshot.getString(Constants.KEY_UID),queryDocumentSnapshot.getString(KEY_USERNAME),queryDocumentSnapshot.getString(KEY_EMAIL),queryDocumentSnapshot.getString(KEY_IMG_URI),queryDocumentSnapshot.getString(KEY_FCM_TOKEN));
                                users.add(user);
                            }
                        }
                        if(!users.isEmpty()){
                            UserAdapter userAdapter = new UserAdapter(users,this);
                            binding.usersRecyclerView.setAdapter(userAdapter);
                            binding.usersRecyclerView.setVisibility(View.VISIBLE);
                        } else{
                            Logger.printLog(SelectUserToChatFragment.class,"Found no users to show.");
                            showErrorMessage();
                        }
                    }
                    else{
                        Logger.printLog(SelectUserToChatFragment.class," !task.isSuccessful() || task.getResult() == null . Found no users to show.");
                        showErrorMessage();
                    }
                });
    }

    private void loading(Boolean isLoading){
        if (isLoading) {
            binding.loadingUsersBar.setVisibility(View.VISIBLE);
        }else{
            binding.loadingUsersBar.setVisibility(View.INVISIBLE);
        }
    }

    private void showErrorMessage(){
        binding.errorMessageUsersText.setText(String.format("%s","No users found!"));
        binding.errorMessageUsersText.setVisibility(View.VISIBLE);
    }

    @Override
    public void onUserClicked(User user) {

        preferenceManager.putBoolean(KEY_ACTIVITY_SWAP,true);
        Intent intent = new Intent(getActivity(), ChatActivity.class);
        intent.putExtra(Constants.KEY_USER, user);
        startActivity(intent);
    }
}