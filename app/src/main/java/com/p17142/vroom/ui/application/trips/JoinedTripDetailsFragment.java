package com.p17142.vroom.ui.application.trips;

import static com.p17142.vroom.utilities.Constants.KEY_ACTIVITY_SWAP;
import static com.p17142.vroom.utilities.Constants.KEY_TRIP;
import static com.p17142.vroom.utilities.Constants.KEY_USERNAME;

import android.content.Intent;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import com.p17142.vroom.data.dao.TripDao;
import com.p17142.vroom.data.dao.UserDao;
import com.p17142.vroom.databinding.FragmentJoinedTripDetailsBinding;
import com.p17142.vroom.models.Trip;
import com.p17142.vroom.models.User;
import com.p17142.vroom.utilities.Constants;
import com.p17142.vroom.utilities.FragUtils;
import com.p17142.vroom.utilities.Logger;
import com.p17142.vroom.utilities.PreferenceManager;
import com.p17142.vroom.ui.application.chat.ChatActivity;

public class JoinedTripDetailsFragment extends Fragment {
    private FragmentJoinedTripDetailsBinding binding;
    private Trip trip = null;
    private boolean validBundle = false;
    private String loggedUsername = "";
    private TripDao tripDao;
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
            Logger.printLogError(JoinedTripDetailsFragment.class,"Invalid bundle.");}
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        binding = FragmentJoinedTripDetailsBinding.inflate(inflater, container, false);
        return  binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        init();
        setListeners();
    }

    private void init(){
        if( !validBundle )
        {
            Logger.printLogFatal(JoinedTripDetailsFragment.class,"Invalid bundle, force back.");
            requireActivity().getOnBackPressedDispatcher().onBackPressed();
        }

        preferenceManager = new PreferenceManager(requireContext());
        loggedUsername = preferenceManager.getString(KEY_USERNAME);
        tripDao = TripDao.getInstance();


        binding.driverAutoCompleteTextView.setText(trip.getDriverUsername());
        binding.tripStartAutoCompleteTextView.setText(trip.getStartLocation());
        binding.tripEndAutoCompleteTextView.setText(trip.getEndLocation());
        binding.dateInputText.setText(trip.getTripDateDisplayValueddMMM());
        binding.timeInputText.setText(trip.getStartTimeDisplayValue());
        binding.ridersAutoCompleteTextView.setText(trip.getRidersToMaxRidersDisplayValue());
    }

    private void setListeners(){
        binding.backButton.setOnClickListener( unused -> requireActivity().getOnBackPressedDispatcher().onBackPressed());
        binding.messageDriverButton.setOnClickListener( unused -> getDriverDetailsAndContinue() );
        binding.leaveTripButton.setOnClickListener( unused -> tryLeaveTrip() );
    }

    private void tryLeaveTrip(){
        isLoading(true);
        tripDao.getTripByUid(trip.getTripUid(),loggedUsername)
                .addOnSuccessListener( retrievedTrip -> {
                    if(retrievedTrip == null){
                        Toast.makeText(requireContext(), "Trip no longed exists.", Toast.LENGTH_SHORT).show();
                        popBackStackAndGoHome();
                    }else{
                        if(retrievedTrip.hasTripStartTimeDateElapsed())
                        {
                            Logger.printLog(JoinedTripDetailsFragment.class, "Trip has already started!");
                            Toast.makeText(requireContext(), "Too late to leave trip.", Toast.LENGTH_SHORT).show();
                            goToHomeFragment();
                        }
                         else if (trip.isRider(loggedUsername)) {
                             tripDao.removeRiderFromTrip(loggedUsername,trip.getTripUid())
                                             .addOnSuccessListener( task -> {
                                                // continue
                                                 Toast.makeText(requireContext(),"Successfully left trip.",Toast.LENGTH_SHORT).show();
                                                 Logger.printLog(JoinedTripDetailsFragment.class,"Rider left trip, database updated successfully!");
                                                 trip.removeRider(loggedUsername);
                                                 popBackStackAndGoHome();
                                             })
                                             .addOnFailureListener( e -> {
                                                 Logger.printLogFatal(JoinedTripDetailsFragment.class, "Unexpected error: "+e);
                                                 //Toast.makeText(requireContext(),"Unexpected error.",Toast.LENGTH_SHORT).show();
                                                isLoading(false);
                                             });
                        } else {
                            Logger.printLogError(JoinedTripDetailsFragment.class, "User is not a rider on this trip");
                            Toast.makeText(requireContext(), "You are not a rider on this trip.", Toast.LENGTH_SHORT).show();
                            popBackStackAndGoHome();
                        }
                    }
                })
                .addOnFailureListener( e -> {
                    Toast.makeText(requireContext(), "Unexpected Error.", Toast.LENGTH_SHORT).show();
                    Logger.printLogFatal(JoinedTripDetailsFragment.class, "Unexpected error :"+e);
                    isLoading(false); // retrieval unexpected error
                });
    }

    private void getDriverDetailsAndContinue(){
        if(trip != null)
        {
            UserDao userDao = UserDao.getInstance();
            userDao.getUserByUsername(trip.getDriverUsername())
                    .addOnSuccessListener( driver -> {
                        if(driver != null) {
                            goToDriverChat(driver);
                        } else{
                            Logger.printLogFatal(JoinedTripDetailsFragment.class,"Unexpected Error on getUserByUsername, driver was not found, did user get deleted?");
                            popBackStackAndGoHome();
                        }
                    })
                    .addOnFailureListener( e -> {
                        Logger.printLogFatal(JoinedTripDetailsFragment.class,"Error on driver data retrieval. Error Message: "+e);
                        isLoading(false);
                    });
        }
    }

    private void goToDriverChat(User driver){
        preferenceManager.putBoolean(KEY_ACTIVITY_SWAP,true);

        Intent intent = new Intent(getActivity(), ChatActivity.class);
        intent.putExtra(Constants.KEY_USER, driver);
        startActivity(intent);
        isLoading(false);
    }

    private void isLoading(boolean loading){
        if(loading){
            binding.messageDriverButton.setClickable(false);
            binding.leaveTripButton.setClickable(false);
            binding.backButton.setClickable(false);

            binding.loadingBar.setVisibility(View.VISIBLE);
        }else{
            binding.loadingBar.setVisibility(View.INVISIBLE);

            binding.messageDriverButton.setClickable(true);
            binding.leaveTripButton.setClickable(true);
            binding.backButton.setClickable(true);
        }
    }

    private void goToHomeFragment(){
        TripHomeFragment fragment = new TripHomeFragment();
        FragUtils.replaceFragment(getParentFragmentManager(),fragment,null);
        isLoading(false);
    }

    private void popBackStackAndGoHome(){
        requireActivity().getSupportFragmentManager().popBackStack();
        TripHomeFragment fragment = new TripHomeFragment();
        FragUtils.replaceFragment(getParentFragmentManager(), fragment, null);
        isLoading(false);
    }
}