package com.p17142.vroom.ui.application.trips;


import static com.p17142.vroom.utilities.Constants.KEY_ACTIVITY_SWAP;
import static com.p17142.vroom.utilities.Constants.KEY_TRIP;

import static com.p17142.vroom.utilities.Constants.KEY_USER;


import android.content.Intent;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;


import com.p17142.vroom.data.dao.UserDao;
import com.p17142.vroom.databinding.FragmentTripDetailsBinding;
import com.p17142.vroom.models.Trip;
import com.p17142.vroom.models.User;
import com.p17142.vroom.utilities.Constants;
import com.p17142.vroom.utilities.FragUtils;
import com.p17142.vroom.utilities.Logger;
import com.p17142.vroom.ui.application.chat.ChatActivity;
import com.p17142.vroom.ui.application.profiles.OtherUserProfileFragment;
import com.p17142.vroom.utilities.PreferenceManager;

import java.util.ArrayList;
import java.util.List;

public class TripDetailsFragment extends Fragment {

    private FragmentTripDetailsBinding binding;

    private Trip trip = null; // trip object, information coming from fragment bundle
    private final List<User> singleDriver = new ArrayList<>(); // driver store as final single item list to be accessible by listener
    private boolean validBundle = false;
    private PreferenceManager preferenceManager;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Bundle bundle = getArguments();
        if (bundle != null) {
            Trip trip = (Trip) bundle.getSerializable(KEY_TRIP);
            if (trip != null && trip.getDriverUsername() != null) {
                this.trip = trip;
                validBundle = true;
            }
        }
        else{
            validBundle = false;
            Logger.printLogError(TripDetailsFragment.class,"Error received null bundle or null bundle argument.");}
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        binding = FragmentTripDetailsBinding.inflate(inflater,container,false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        init();
        setListeners();
    }

    private void init()
    {
        if( !validBundle )
        {
            Logger.printLogFatal(TripDetailsFragment.class,"Invalid bundle, force back.");
            requireActivity().getOnBackPressedDispatcher().onBackPressed();
        }
        if(trip != null)
        {
            binding.driverAutoCompleteTextView.setText(trip.getDriverUsername());
            binding.tripStartAutoCompleteTextView.setText(trip.getStartLocation());
            binding.tripEndAutoCompleteTextView.setText(trip.getEndLocation());
            binding.dateInputText.setText(trip.getTripDateDisplayValueddMMM());
            binding.timeInputText.setText(trip.getStartTimeDisplayValue());
            binding.ridersAutoCompleteTextView.setText(trip.getRidersToMaxRidersDisplayValue());
        }
        preferenceManager = new PreferenceManager(requireContext());
        getDriverDetails();

    }

    private void getDriverDetails(){
        this.singleDriver.clear();
        if(trip!=null)
        {
            UserDao userDao = UserDao.getInstance();
            userDao.getUserByUsername(trip.getDriverUsername())
                    .addOnSuccessListener( driver -> {
                        if(driver != null) {
                            //continue
                            this.singleDriver.add(driver); // save driver object for future use
                        } else{
                            Logger.printLogFatal(TripDetailsFragment.class,"Unexpected Error on getUserByUsername, driver was not found, did user get deleted?");
                        }
                    })
                    .addOnFailureListener( e -> {
                        Logger.printLogFatal(TripDetailsFragment.class,"Error on driver data retrieval. Error Message: "+e);
                    });
        }
    }

    private void setListeners() {
        binding.backButton.setOnClickListener(click -> requireActivity().getOnBackPressedDispatcher().onBackPressed());

        binding.goToProfileButton.setOnClickListener( click -> {
            // check third user profile fragment ( driver profile )
            if(!singleDriver.isEmpty())
            {
                OtherUserProfileFragment fragment = new OtherUserProfileFragment();
                Bundle bundle = new Bundle();
                bundle.putSerializable(KEY_USER, singleDriver.get(0));
                fragment.setArguments(bundle);
                FragUtils.replaceFragment(getParentFragmentManager(), fragment,null);
            }
        });

        binding.askToJoinButton.setOnClickListener( unused -> {
            // go to chat activity ( chat with driver )
            if(!singleDriver.isEmpty())
            {
                preferenceManager.putBoolean(KEY_ACTIVITY_SWAP,true);
                Intent intent = new Intent(getActivity(), ChatActivity.class);
                intent.putExtra(Constants.KEY_USER, singleDriver.get(0));
                startActivity(intent);
            }
        });
    }
}