package com.p17142.vroom.ui.application.trips;

import static com.p17142.vroom.utilities.Constants.KEY_ANYWHERE;
import static com.p17142.vroom.utilities.Constants.KEY_FILTER_END_DATE;
import static com.p17142.vroom.utilities.Constants.KEY_FILTER_SHOW_FULL;
import static com.p17142.vroom.utilities.Constants.KEY_FILTER_SHOW_OWNED;
import static com.p17142.vroom.utilities.Constants.KEY_FILTER_START_DATE;
import static com.p17142.vroom.utilities.Constants.KEY_FILTER_TRIP_END;
import static com.p17142.vroom.utilities.Constants.KEY_FILTER_TRIP_START;
import static com.p17142.vroom.utilities.Constants.KEY_TRIP;
import static com.p17142.vroom.utilities.Constants.KEY_USERNAME;
import static com.p17142.vroom.utilities.Constants.REQUEST_KEY_FILTER_UPDATE;

import android.os.Bundle;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.p17142.vroom.adapters.TripAdapter;
import com.p17142.vroom.data.dao.TripDao;
import com.p17142.vroom.data.dao.UserDao;
import com.p17142.vroom.databinding.FragmentTripsMainBinding;
import com.p17142.vroom.listeners.TripListener;
import com.p17142.vroom.models.Trip;
import com.p17142.vroom.models.User;
import com.p17142.vroom.utilities.FragUtils;
import com.p17142.vroom.utilities.Logger;
import com.p17142.vroom.utilities.PreferenceManager;
import java.util.ArrayList;
import java.util.List;

public class TripFinderFragment extends Fragment implements TripListener,TripFilterDialogFragment.OnDialogDismissListener {
    private PreferenceManager preferenceManager;
    private FragmentTripsMainBinding binding;

    private final UserDao userDao = UserDao.getInstance();
    private TripDao tripDao;

    private List<Trip> databaseTrips = new ArrayList<>();
    private List<Trip> filteredTrips = new ArrayList<>();

    private TripAdapter tripAdapter = null;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        preferenceManager = new PreferenceManager(requireContext());
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        binding = FragmentTripsMainBinding.inflate(inflater,container,false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        init();
        getTrips();
        setListeners();
    }

    private void init(){
        tripDao = TripDao.getInstance();
    }

    private void setListeners(){
        binding.filterButton.setOnClickListener( unused -> onFilterClick());
    }

    private void getTrips(){
        loading(true);
        tripDao.getAllNonCompleteTrips(preferenceManager.getString(KEY_USERNAME)).addOnSuccessListener( tripList -> {
            if(!tripList.isEmpty()){
                List<String> driverUsernames = new ArrayList<>();
                for (Trip trip: tripList
                     ) {
                    driverUsernames.add(trip.getDriverUsername());
                }
                userDao.getUsersByUsernameList(driverUsernames)
                        .addOnSuccessListener( usersList -> {
                            if(!usersList.isEmpty()){
                                for (Trip trip: tripList) {
                                    for (User user: usersList) {
                                        if(user.getUsername().equals(trip.getDriverUsername())){
                                            trip.setDriverImageUri(user.getImageUri());
                                            break;
                                        }
                                    }
                                }
                                // continue here
                                //filterList(tripList);
                                databaseTrips.clear();
                                databaseTrips.addAll(tripList);

                                filteredTrips.clear();
                                filteredTrips.addAll(filterTrips(tripList,false,false,KEY_ANYWHERE,KEY_ANYWHERE,-1,-1));

                                loading(false);
                                tripAdapter = new TripAdapter(filteredTrips,preferenceManager.getString(KEY_USERNAME),this);
                                binding.availableTripsRecyclerView.setAdapter(tripAdapter);
                                if(filteredTrips.isEmpty()){
                                    showErrorMessage("Found no available trips.");
                                }

                            } else{
                                loading(false);
                                Logger.printLogFatal(TripFinderFragment.class,"Found trips but usersList was returned empty, database inconsistency?");
                            }
                        })
                        .addOnFailureListener( e -> {
                            loading(false);
                            Logger.printLogFatal(TripFinderFragment.class,"Unexpected error on getUsersByUsernameList with error: "+e);
                        });
            }
            else{
                loading(false);
                showErrorMessage("No available trips found.");
            }
        }).addOnFailureListener( e -> {
            loading(false);
            Logger.printLogFatal(TripFinderFragment.class,"Unexpected error on getAllTrips with error: "+e);
        });
    }

    private void onFilterClick(){
        binding.filterButton.setClickable(false);
        TripFilterDialogFragment tripFilterDialogFragment = new TripFilterDialogFragment();
        tripFilterDialogFragment.show(getChildFragmentManager(), "TripFilterDialog"); // dialogFragment will be a child of this fragment

        getChildFragmentManager().setFragmentResultListener(REQUEST_KEY_FILTER_UPDATE,this, (requestKey,result) -> {
            loading(true);
            String filterTripStart = result.getString(KEY_FILTER_TRIP_START);
            String filterTripEnd = result.getString(KEY_FILTER_TRIP_END);
            long filterDateStart = result.getLong(KEY_FILTER_START_DATE);
            long filterDateEnd = result.getLong(KEY_FILTER_END_DATE);
            boolean showOwnedTrips = result.getBoolean(KEY_FILTER_SHOW_OWNED);
            boolean showFullTrips = result.getBoolean(KEY_FILTER_SHOW_FULL);

            filteredTrips.clear();
            filteredTrips.addAll(filterTrips(databaseTrips,showOwnedTrips,showFullTrips,filterTripStart,filterTripEnd,filterDateStart,filterDateEnd));
            tripAdapter.notifyDataSetChanged();
            if(filteredTrips.isEmpty())
            {
                showErrorMessage("Found no trips matching your criteria.");
            }
            loading(false);
        });
    }

    private List<Trip> filterTrips(List<Trip> unfilteredTrips, boolean showOwnedTrips, boolean showFullTrips, String startLocation, String endLocation, long startTime, long endTime){
        List<Trip> filteredTrips = new ArrayList<>();
        filteredTrips.addAll(unfilteredTrips);
        for (Trip unfTrip: unfilteredTrips) { // need to be a different list or we get a concurrent modification error
            if(unfTrip.isParticipant(preferenceManager.getString(KEY_USERNAME))) // remove if is participant quickfx
            {
                filteredTrips.remove(unfTrip);
                continue;
            }
            if(unfTrip.getInProgress()) // remove if trip is already in progress
            {
                filteredTrips.remove(unfTrip);
                continue;
            }
            if(!showOwnedTrips)
            {
                if(unfTrip.getDriverUsername().equals(preferenceManager.getString(KEY_USERNAME)))
                {
                    filteredTrips.remove(unfTrip);
                    continue;
                }
            }
            if(!showFullTrips)
            {
                if(unfTrip.getMaxNumOfRiders() == unfTrip.getRiderUsernames().size())
                {
                    filteredTrips.remove(unfTrip);
                    continue;
                }
            }
            if(startTime != -1 && endTime != -1)
            {
                if(!unfTrip.isBetweenEpochDates(startTime,endTime))
                {
                   filteredTrips.remove(unfTrip);
                   continue;
                }
            }
            if(!startLocation.equals(KEY_ANYWHERE))
            {
                if(!unfTrip.getStartLocation().equals(startLocation))
                {
                    filteredTrips.remove(unfTrip);
                    continue;
                }
            }
            if(!endLocation.equals(KEY_ANYWHERE))
            {
                if(!unfTrip.getEndLocation().equals(endLocation))
                {
                    filteredTrips.remove(unfTrip);
                    continue;
                }
            }
        }
        return filteredTrips;
    }


    private void loading(Boolean isLoading){
        if (isLoading) {
            binding.availableTripsRecyclerView.setVisibility(View.INVISIBLE);
            binding.errorMessageText.setVisibility(View.INVISIBLE);
            binding.loadingTripsBar.setVisibility(View.VISIBLE);

        }else{
            binding.loadingTripsBar.setVisibility(View.INVISIBLE);
            binding.availableTripsRecyclerView.setVisibility(View.VISIBLE);
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
            fragment = new TripDetailsFragment();
        }
        fragment.setArguments(bundle);
        FragUtils.replaceFragment(getParentFragmentManager(), fragment, null);
    }

    @Override
    public void onDialogDismissed() {
        binding.filterButton.setClickable(true); // on filter dialog dismiss -> make button clickable again
    }
}