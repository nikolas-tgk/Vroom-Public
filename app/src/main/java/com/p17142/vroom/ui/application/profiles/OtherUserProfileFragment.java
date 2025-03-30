package com.p17142.vroom.ui.application.profiles;

import static com.p17142.vroom.utilities.Constants.CHAT_ACTIVITY;
import static com.p17142.vroom.utilities.Constants.KEY_ACTIVITY_SWAP;
import static com.p17142.vroom.utilities.Constants.KEY_USER;
import static com.p17142.vroom.utilities.Constants.KEY_USERNAME;
import static com.p17142.vroom.utilities.Constants.KEY_SOURCE;

import android.content.Intent;
import android.os.Bundle;

import androidx.activity.OnBackPressedCallback;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.p17142.vroom.data.dao.UserDao;
import com.p17142.vroom.databinding.FragmentThirdUserProfileBinding;
import com.p17142.vroom.models.User;
import com.p17142.vroom.utilities.Constants;
import com.p17142.vroom.utilities.FragUtils;
import com.p17142.vroom.utilities.Logger;
import com.p17142.vroom.utilities.ImageUtils;
import com.p17142.vroom.ui.application.chat.ChatActivity;
import com.p17142.vroom.ui.application.trips.TripHomeFragment;
import com.p17142.vroom.utilities.PreferenceManager;

import java.text.SimpleDateFormat;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

public class OtherUserProfileFragment extends Fragment {

    private FragmentThirdUserProfileBinding binding; // thirdUserProfile is OtherUserProfile after refactoring
    private SimpleDateFormat dateFormat = new SimpleDateFormat("dd MMM yyyy");
    private final List<User> user = new ArrayList<>(); // user store as final single item list to be accessible by listener
    private String from = "none"; // source activity
    private UserDao userDao;
    private boolean setMessageButtonInvisible = false;
    private PreferenceManager preferenceManager;


    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        binding = FragmentThirdUserProfileBinding.inflate(inflater, container, false);
        loading(true);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        init();
        setListeners();
    }

    private void setUserDetails(User user){
        binding.usernameTextView.setText(user.getUsername());
        binding.emailTextView.setText(user.getEmail());
        Instant memberSinceInstant  = user.getCreatedDate();
        Date memberSinceDate = Date.from(memberSinceInstant);
        binding.memberSinceTextView.setText(String.format("Member since %s", dateFormat.format(memberSinceDate)));

        binding.profileImageView.setImageBitmap(ImageUtils.decodeImage(user.getImageUri()));
        binding.peopleMetTextView.setText(String.valueOf(user.getNumOfUniquePeopleMet()));
        binding.ridesSharedTextView.setText(String.valueOf(user.getNumOfTripsCompletedAsDriver()+user.getNumOfTripsCompletedAsRider()));

        float userRating = user.getUserRating();
        if(userRating!=0)
        {
            binding.userRatingTextView.setText(String.format("%.1f",userRating / user.getNumOfRatings()));
        } else
        {
            binding.userRatingTextView.setText("-");
        }
        loading(false);
    }

    private void init(){
        userDao = UserDao.getInstance();
        preferenceManager = new PreferenceManager(requireContext());
        Bundle bundle = getArguments();
        if (bundle != null) {
            User user = (User) bundle.getSerializable(KEY_USER);
            String username = bundle.getString(KEY_USERNAME);
            if(bundle.getString(KEY_SOURCE) != null)
            {
                from = bundle.getString(KEY_SOURCE);
                if( from != null && from.equals(CHAT_ACTIVITY)) // hide message button if we just came from chat, is not right
                {
                    setMessageButtonInvisible = true;
                }
                else{
                    setMessageButtonInvisible = false;
                }
            }
            else{
                from = "none";
            }
            this.user.clear();
            if(user == null && username != null && !username.isEmpty()){
                // came from chat activity, load user object from db
                userDao.getUserByUsername(username)
                        .addOnSuccessListener( retrievedUser -> {
                            if(retrievedUser != null )
                            {
                                this.user.clear();
                                this.user.add(retrievedUser);
                                setUserDetails(retrievedUser);
                            }else{
                                Logger.printLogError(OtherUserProfileFragment.class,"Error user not found in db");
                            }
                        }).addOnFailureListener( e -> {
                            Logger.printLogError(OtherUserProfileFragment.class,"Null user parameter used in userDao");
                        });
            }
            if (user != null) {
                this.user.add(user);
                setUserDetails(user);
            }
        }
        else{
            //null bundle error
            Logger.printLogError(OtherUserProfileFragment.class,"Invalid bundle argument user == null, default to home.");
            popBackStackAndGoHome();
        }
    }

    private void setListeners(){
        binding.backButton.setOnClickListener( unused -> requireActivity().getOnBackPressedDispatcher().onBackPressed());

        binding.messageButton.setOnClickListener( unused -> {
            // pass (selected) user object and transfer to chat activity
            if(!user.isEmpty())
            {
                preferenceManager.putBoolean(KEY_ACTIVITY_SWAP,true);
                Intent intent = new Intent(getActivity(), ChatActivity.class);
                intent.putExtra(Constants.KEY_USER, user.get(0));
                startActivity(intent);
            }
        });
        // custom back press behavior
        OnBackPressedCallback callback = new OnBackPressedCallback(true) {
            @Override
            public void handleOnBackPressed() {
                // Custom back press behavior
                if(from.equals(CHAT_ACTIVITY)){ // if we were prompted here from chat activity
                    preferenceManager.putBoolean(KEY_ACTIVITY_SWAP,true);
                    Intent intent = new Intent(getActivity(), ChatActivity.class);
                    intent.putExtra(KEY_USER, user.get(0));
                    startActivity(intent);
                    requireActivity().finish();
                } else{
                    setEnabled(false); // temporarily disable callback to continue with default back execution
                    requireActivity().getOnBackPressedDispatcher().onBackPressed();
                }
            }
        };
        requireActivity().getOnBackPressedDispatcher().addCallback(getViewLifecycleOwner(), callback); // add the callback to handle back press in this fragment only
    }

    private void loading(boolean isLoading){
        if(isLoading){
            binding.profileImageView.setVisibility(View.INVISIBLE);
            binding.infoLayout.setVisibility(View.INVISIBLE);
            binding.mailImageView.setVisibility(View.INVISIBLE);
            binding.calendarImageView.setVisibility(View.INVISIBLE);
            binding.emailTextView.setVisibility(View.INVISIBLE);
            binding.memberSinceTextView.setVisibility(View.INVISIBLE);
            binding.usernameTextView.setVisibility(View.INVISIBLE);
            binding.messageButton.setVisibility(View.INVISIBLE);
            binding.loadingBar.setVisibility(ViewGroup.VISIBLE);
        }else{
            binding.loadingBar.setVisibility(ViewGroup.INVISIBLE);

            binding.profileImageView.setVisibility(View.VISIBLE);
            binding.infoLayout.setVisibility(View.VISIBLE);
            binding.mailImageView.setVisibility(View.VISIBLE);
            binding.calendarImageView.setVisibility(View.VISIBLE);
            binding.emailTextView.setVisibility(View.VISIBLE);
            binding.memberSinceTextView.setVisibility(View.VISIBLE);
            binding.usernameTextView.setVisibility(View.VISIBLE);
            if(!setMessageButtonInvisible)
            {
                binding.messageButton.setVisibility(View.VISIBLE);
            }
        }
    }

    private void popBackStackAndGoHome(){
        requireActivity().getSupportFragmentManager().popBackStack();
        TripHomeFragment fragment = new TripHomeFragment();
        FragUtils.replaceFragment(getParentFragmentManager(), fragment, null);
        loading(false);
    }
}