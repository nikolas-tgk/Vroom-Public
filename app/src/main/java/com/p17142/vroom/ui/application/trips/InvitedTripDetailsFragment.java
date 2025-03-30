package com.p17142.vroom.ui.application.trips;

import static com.p17142.vroom.utilities.Constants.KEY_ACCEPTED_TRIP_INVITE_NOTIFICATION;
import static com.p17142.vroom.utilities.Constants.KEY_DENIED_TRIP_INVITE_NOTIFICATION;
import static com.p17142.vroom.utilities.Constants.KEY_TRIP;
import static com.p17142.vroom.utilities.Constants.KEY_USER;
import static com.p17142.vroom.utilities.Constants.KEY_USERNAME;

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
import com.p17142.vroom.databinding.FragmentInvitedTripDetailsBinding;
import com.p17142.vroom.models.Trip;
import com.p17142.vroom.models.User;
import com.p17142.vroom.notifications.FcmNotificationSender;
import com.p17142.vroom.utilities.FragUtils;
import com.p17142.vroom.utilities.Logger;
import com.p17142.vroom.utilities.PreferenceManager;
import com.p17142.vroom.ui.application.profiles.OtherUserProfileFragment;

public class InvitedTripDetailsFragment extends Fragment {

    private FragmentInvitedTripDetailsBinding binding;
    private PreferenceManager preferenceManager;

    private Trip trip = null;
    private boolean validBundle = false;
    private String loggedUsername = "";
    private final TripDao tripDao = TripDao.getInstance();
    private final UserDao userDao = UserDao.getInstance();

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
            Logger.printLogError(InvitedTripDetailsFragment.class,"Invalid bundle.");}
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        binding = FragmentInvitedTripDetailsBinding.inflate( inflater, container, false);
        return binding.getRoot();
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
            Logger.printLogFatal(InvitedTripDetailsFragment.class,"Invalid bundle, force back.");
            requireActivity().getOnBackPressedDispatcher().onBackPressed();
        }

        preferenceManager = new PreferenceManager(requireContext());
        loggedUsername = preferenceManager.getString(KEY_USERNAME);

        binding.driverAutoCompleteTextView.setText(trip.getDriverUsername());
        binding.tripStartAutoCompleteTextView.setText(trip.getStartLocation());
        binding.tripEndAutoCompleteTextView.setText(trip.getEndLocation());
        binding.dateInputText.setText(trip.getTripDateDisplayValueddMMM());
        binding.timeInputText.setText(trip.getStartTimeDisplayValue());
        binding.ridersAutoCompleteTextView.setText(trip.getRidersToMaxRidersDisplayValue());

        isLoading(false);
    }

    private void setListeners(){
        binding.backButton.setOnClickListener( unused -> requireActivity().getOnBackPressedDispatcher().onBackPressed());
        binding.acceptButton.setOnClickListener( unused -> respondToInvite(true));
        binding.denyButton.setOnClickListener( unused -> respondToInvite(false));
        binding.goToProfileButton.setOnClickListener( unused -> getDriverDetails());
    }

    private void respondToInvite(Boolean response){
        isLoading(true);
        tripDao.getTripByUid(trip.getTripUid(),loggedUsername)
                .addOnSuccessListener( retrievedTrip -> {
                    if(retrievedTrip == null )
                    {
                        isLoading(false);
                        Logger.printLogError(InvitedTripDetailsFragment.class, "Trip does not exist anymore.");
                        Toast.makeText(requireContext(), "Trip does not exist anymore", Toast.LENGTH_SHORT).show();
                        popBackStackAndGoHome();
                    }
                    else{
                        if(response) // if user tried to accept invite
                        {
                            if (retrievedTrip.hasTripStartTimeDateElapsed()) {
                                Logger.printLog(InvitedTripDetailsFragment.class, "Trip has already started!");
                                Toast.makeText(requireContext(), "Too late to accept invite.", Toast.LENGTH_SHORT).show();
                                popBackStackAndGoHome();
                            } else if (retrievedTrip.areMaxRiders()) {
                                Logger.printLogError(InvitedTripDetailsFragment.class, "Trip riders already full.");
                                Toast.makeText(requireContext(), "Trip is already full!", Toast.LENGTH_SHORT).show();
                                isLoading(false);
                                // don't go anywhere
                            } else if (retrievedTrip.isInvited(loggedUsername)) {
                                tripDao.acceptInvitationForTrip(loggedUsername,retrievedTrip.getTripUid())
                                                .addOnSuccessListener( success -> {
                                                    if(success){
                                                        retrievedTrip.removeInvite(preferenceManager.getString(KEY_USERNAME));
                                                        retrievedTrip.addRider(preferenceManager.getString(KEY_USERNAME));
                                                        Toast.makeText(requireContext(),"Invitation accepted.",Toast.LENGTH_SHORT).show();
                                                        Logger.printLog(InvitedTripDetailsFragment.class,"Invitation accepted, database updated successfully!");
                                                        userDao.provideFcm(retrievedTrip.getDriverUsername()).addOnSuccessListener( targetFcm -> {
                                                            if(targetFcm != null)
                                                            {
                                                                FcmNotificationSender fcmNotificationSender = new FcmNotificationSender(targetFcm,
                                                                        "Vroom: Invitation Accepted","User "+loggedUsername+" has accepted your trip invite!",getContext(),loggedUsername,retrievedTrip.getTripUid(),KEY_ACCEPTED_TRIP_INVITE_NOTIFICATION);
                                                                fcmNotificationSender.sendNotification();
                                                            }
                                                            popBackStackAndGoHome(); // TO-DO change this to new fragment
                                                        });
                                                    }else{
                                                        // user deny fail, user does not exist or invitation no longer valid
                                                        isLoading(false);
                                                    }
                                                }).addOnFailureListener( e -> {
                                                    //unexpected error
                                                    Logger.printLogFatal(InvitedTripDetailsFragment.class,"Unexpected error: "+e);
                                                    isLoading(false);
                                        });
                            } else {
                                // user is no longed invited at the time of button click
                                Logger.printLogError(InvitedTripDetailsFragment.class, "Invitation has expired.");
                                Toast.makeText(requireContext(), "Invitation has expired.", Toast.LENGTH_SHORT).show();
                                popBackStackAndGoHome();
                            }
                        }
                        else{ // if user tried to deny invite
                            if(retrievedTrip.isInvited(loggedUsername))
                            {
                                retrievedTrip.removeInvite(loggedUsername);
                                tripDao.denyInvitationForTrip(loggedUsername,retrievedTrip.getTripUid())
                                        .addOnSuccessListener( success -> {
                                            if(success)
                                            {
                                                retrievedTrip.removeInvite(preferenceManager.getString(KEY_USERNAME));
                                                retrievedTrip.addRider(preferenceManager.getString(KEY_USERNAME));
                                                Toast.makeText(requireContext(), "Invitation denied.", Toast.LENGTH_SHORT).show();
                                                userDao.provideFcm(retrievedTrip.getDriverUsername()).addOnSuccessListener( targetFcm -> {
                                                    if(targetFcm != null)
                                                    {
                                                        FcmNotificationSender fcmNotificationSender = new FcmNotificationSender(targetFcm,
                                                                "Vroom: Invitation Denied","User "+loggedUsername+" has denied your trip invite!",getContext(),loggedUsername,retrievedTrip.getTripUid(),KEY_DENIED_TRIP_INVITE_NOTIFICATION);
                                                        fcmNotificationSender.sendNotification();
                                                    }
                                                    popBackStackAndGoHome();

                                                });
                                            }else{
                                                Logger.printLogError(InvitedTripDetailsFragment.class, "Invitation has expired or trip does not exist.");
                                                isLoading(false);
                                            }
                                        })
                                        .addOnFailureListener( e -> {
                                            Logger.printLogFatal(InvitedTripDetailsFragment.class, "Unexpected error: "+e);
                                            isLoading(false);
                                        });
                            }
                            else{
                                Toast.makeText(requireContext(), "Invitation has already expired.", Toast.LENGTH_SHORT).show();
                                Logger.printLogError(InvitedTripDetailsFragment.class, "User was no longed invited.");
                                popBackStackAndGoHome();
                            }
                        }
                    }
                })
                .addOnFailureListener( e -> {
                    Logger.printLogError(InvitedTripDetailsFragment.class, "Unexpected error: "+e);
                    isLoading(false);
                });
    }

    private void getDriverDetails(){
        // getDriverDetails , on success go to thirdUserProfile fragment
        isLoading(true);
        if(trip != null)
        {
            UserDao userDao = UserDao.getInstance();
            userDao.getUserByUsername(trip.getDriverUsername())
                    .addOnSuccessListener( driver -> {
                        if(driver != null) {
                            goToDriverProfile(driver);
                        } else{
                            Logger.printLogFatal(InvitedTripDetailsFragment.class,"Unexpected Error on getUserByUsername, driver was not found, did user get deleted?");
                            goToHomeFragment();
                        }
                    })
                    .addOnFailureListener( e -> {
                        Logger.printLogFatal(InvitedTripDetailsFragment.class,"Error on driver data retrieval. Error Message: "+e);
                        goToHomeFragment();
                    });
        }
    }

    private void goToDriverProfile(User driver){
        OtherUserProfileFragment fragment = new OtherUserProfileFragment();
        Bundle bundle = new Bundle();
        bundle.putSerializable(KEY_USER, driver);
        fragment.setArguments(bundle);
        FragUtils.replaceFragment(getParentFragmentManager(), fragment,null);
        isLoading(false);
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

    private void isLoading(boolean loading){
        if(loading){
            binding.goToProfileButton.setClickable(false);
            binding.acceptButton.setClickable(false);
            binding.denyButton.setClickable(false);
            binding.backButton.setClickable(false);

            binding.loadingBar.setVisibility(View.VISIBLE);
        }else{
            binding.loadingBar.setVisibility(View.INVISIBLE);

            binding.goToProfileButton.setClickable(true);
            binding.acceptButton.setClickable(true);
            binding.denyButton.setClickable(true);
            binding.backButton.setClickable(true);
        }
    }
}