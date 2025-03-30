package com.p17142.vroom.ui.application.trips;

import static com.p17142.vroom.utilities.Constants.KEY_TRIP;
import static com.p17142.vroom.utilities.Constants.KEY_USERNAME;

import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.p17142.vroom.R;
import com.p17142.vroom.adapters.TripAdapter;
import com.p17142.vroom.data.dao.TripDao;
import com.p17142.vroom.data.dao.UserDao;
import com.p17142.vroom.databinding.FragmentHomeBinding;
import com.p17142.vroom.listeners.TripListener;
import com.p17142.vroom.models.Trip;
import com.p17142.vroom.models.User;
import com.p17142.vroom.ui.application.MapsFragment;
import com.p17142.vroom.utilities.FragUtils;
import com.p17142.vroom.utilities.Logger;
import com.p17142.vroom.utilities.PreferenceManager;

import java.util.ArrayList;
import java.util.List;

public class TripHomeFragment extends Fragment implements TripListener {

    private PreferenceManager preferenceManager;
    private FragmentHomeBinding binding;
    private String loggedUsername;
    private List<Trip> filteredTrips = new ArrayList<>();
    private List<Trip> retrievedTrips = new ArrayList<>();
    private TripAdapter tripAdapter = new TripAdapter(filteredTrips,"",this);
    private boolean showOld = false;
    private final TripDao tripDao = TripDao.getInstance();
    private final UserDao userDao = UserDao.getInstance();

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        preferenceManager = new PreferenceManager(requireContext());
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        binding = FragmentHomeBinding.inflate(inflater,container,false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        init();
        setListeners();
    }

    private void init(){
        binding.mapsButton.setVisibility(View.INVISIBLE); // FUTURE
        binding.toggleOldButton.setVisibility(View.INVISIBLE);
        loggedUsername = preferenceManager.getString(KEY_USERNAME);

        getUserRelatedTrips();
    }

    private void setListeners(){
        binding.newTripButton.setOnClickListener( v -> FragUtils.replaceFragment(getParentFragmentManager(),new NewTripFragment(),null));

        binding.toggleOldButton.setOnClickListener( v -> onToggleClickRatedTripsVisibility());

        binding.mapsButton.setOnClickListener( v -> onMapsButtonClick());
    }

    private void onMapsButtonClick()
    {
        FragUtils.replaceFragment(getParentFragmentManager(),new MapsFragment(),null);
    }

    /**
     * toggles the visibility of past completed but also rated trips
     */
    private void onToggleClickRatedTripsVisibility(){
        if(!showOld)
        {
            showOld = true;
            binding.toggleOldButton.setImageResource(R.drawable.baseline_update_disabled_24);
        }
        else{
            showOld = false;
            binding.toggleOldButton.setImageResource(R.drawable.baseline_update_24);

        }
        loading(true);
        filteredTrips.clear();
        filteredTrips.addAll(filterTrips(retrievedTrips));
        tripAdapter.notifyDataSetChanged();
        loading(false);
    }

    /**
     * queries firestore for all of logged on user's related trips , passes result through the filter and then shows result
     */
    private void getUserRelatedTrips(){
        loading(true);
        tripDao.getAllUserRelatedTrips(preferenceManager.getString(KEY_USERNAME),preferenceManager.getString(KEY_USERNAME))
                .addOnSuccessListener( trips -> {
                    //continue
                    List<String> driverUsernames = new ArrayList<>();
                    if(!trips.isEmpty()){
                        for (Trip trip: trips) {
                            driverUsernames.add(trip.getDriverUsername());
                        }
                        userDao.getUsersByUsernameList(driverUsernames)
                                .addOnSuccessListener( usersList -> {
                                    if(!usersList.isEmpty()){
                                        for (Trip trip: trips) {
                                            for (User user: usersList) {
                                                if(user.getUsername().equals(trip.getDriverUsername())){
                                                    trip.setDriverImageUri(user.getImageUri());
                                                    break;
                                                }
                                            }
                                        }
                                        retrievedTrips.clear();
                                        retrievedTrips.addAll(trips);
                                        filteredTrips.clear();
                                        filteredTrips.addAll(filterTrips(trips));
                                        // continue here
                                        loading(false);
                                        tripAdapter = new TripAdapter(filteredTrips,loggedUsername,this);
                                        binding.myTripsRecyclerView.setAdapter(tripAdapter);

                                    } else{
                                        loading(false);
                                        Logger.printLogFatal(TripHomeFragment.class,"Found trips but usersList was returned empty, database inconsistency?");
                                    }
                                })
                                .addOnFailureListener( e -> {
                                    loading(false);
                                    showErrorMessage("Could not load trips.");
                                    Logger.printLogFatal(TripHomeFragment.class,"Unexpected error on getUsersByUsernameList with error: "+e);
                                });
                    }
                    else{
                        loading(false);
                        showErrorMessage("No trips yet! Create your own or find one in the search tab.");
                        Logger.printLog(TripHomeFragment.class,"Found no participating trips to show.");
                    }

                })
                .addOnFailureListener( e -> {
                   loading(false);
                    Logger.printLogFatal(TripHomeFragment.class,"Unexpected Error at getAllUserRelatedTrips with error: "+e);
                });

    }

    /**
     * categorizes, sorts and filters a given list of trips
     * @param unfilteredTrips a list containing all user's related trips
     * @return a list of trips sorted primarily by category and secondly by trip starting date
     */
    private List<Trip> filterTrips(List<Trip> unfilteredTrips){
        List<Trip> unratedTrips = new ArrayList<>();
        List<Trip> invitedToTrips = new ArrayList<>();
        List<Trip> participatingTrips = new ArrayList<>();
        List<Trip> oldTrips = new ArrayList<>();
        List<Trip> filteredTrips = new ArrayList<>();
        List<Trip> hide = new ArrayList<>(); // this could be useful for less intensive databases reads
        for (Trip trip: unfilteredTrips) {
            if(trip.isCompleted() && (trip.hasUserFinishedRating(loggedUsername) || trip.getRiderUsernames().isEmpty())) // if there were no riders, there were no users to rate in the first place
            {
                if(showOld)
                {
                    oldTrips.add(trip);
                }
                else{
                    hide.add(trip);
                }
            }
            else if(trip.isCompleted() && !trip.hasUserFinishedRating(loggedUsername))
            {
                unratedTrips.add(trip);
            }
            else if(trip.isInvited(loggedUsername))
            {
                invitedToTrips.add(trip);
            } else if (trip.isParticipant(loggedUsername))
            {
                participatingTrips.add(trip);
            }
            else if(trip.getDriverUsername().equals(loggedUsername))
            {
                participatingTrips.add(trip);
            }
            else{
                Logger.printLogFatal(TripHomeFragment.class,"Error, Unable to filter trip, partial trip retrieved? TripUid: "+trip.getTripUid());
            }
        }
        participatingTrips.sort( (trip1, trip2) -> trip1.getTripDate().compareTo(trip2.getTripDate()));
        unratedTrips.sort((trip1, trip2) -> trip1.getTripDate().compareTo(trip2.getTripDate()));
        // this order is the final ui order
        filteredTrips.addAll(unratedTrips); // adds at the end, which is the logic we need
        filteredTrips.addAll(invitedToTrips);
        filteredTrips.addAll(participatingTrips);
        filteredTrips.addAll(oldTrips);
        return filteredTrips;
    }

    private void loading(Boolean isLoading){
        if (isLoading) {
            binding.myTripsRecyclerView.setVisibility(View.INVISIBLE);
            binding.loadingTripsBar.setVisibility(View.VISIBLE);

        }else{
            binding.loadingTripsBar.setVisibility(View.INVISIBLE);
            binding.myTripsRecyclerView.setVisibility(View.VISIBLE);
            binding.toggleOldButton.setVisibility(View.VISIBLE);
        }
    }

    private void showErrorMessage(String message){
        binding.errorMessageText.setText(message);
        binding.errorMessageText.setVisibility(View.VISIBLE);
    }

    @Override
    public void onTripClicked(Trip trip) {
        String loggedUsername = preferenceManager.getString(KEY_USERNAME);
        Bundle bundle = new Bundle();
        bundle.putSerializable(KEY_TRIP, trip);
        Fragment fragment;
        if(trip.getDriverUsername().equals(loggedUsername) && !trip.isCompleted() ) // clicks on a trip where he is the driver and trip is not finished
        {
            fragment = new ManageOwnedTripFragment();
        } else if(trip.isRider(loggedUsername) && !trip.isCompleted()){ // clicks on a trip where he is a participant and trip is not finished
            fragment = new JoinedTripDetailsFragment();
        }
        else if(trip.isInvited(loggedUsername)){ // invited trip fragment to join
            fragment = new InvitedTripDetailsFragment();
        }
        else if(trip.isCompleted() && (trip.isRider(loggedUsername) || trip.getDriverUsername().equals(loggedUsername))){ // participating trip that has finished
            fragment = new CompletedTripFragment();
        }
        else{ // clicks on a random trip without anything related
            fragment = new TripDetailsFragment(); // this should not be possible here (home fragment, as only relatable trips are shown)
        }
        fragment.setArguments(bundle);
        FragUtils.replaceFragment(getParentFragmentManager(), fragment, null);
    }
}