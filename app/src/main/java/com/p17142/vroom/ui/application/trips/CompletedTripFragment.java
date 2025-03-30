package com.p17142.vroom.ui.application.trips;

import static com.p17142.vroom.utilities.Constants.IS_SUCCESSFUL;
import static com.p17142.vroom.utilities.Constants.KEY_TRIP;
import static com.p17142.vroom.utilities.Constants.KEY_TRIP_UID;
import static com.p17142.vroom.utilities.Constants.KEY_USERNAME;
import static com.p17142.vroom.utilities.Constants.KEY_USERS;
import static com.p17142.vroom.utilities.Constants.REQUEST_KEY_RATING_COMPLETE;

import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import com.google.android.gms.tasks.Task;
import com.google.android.gms.tasks.Tasks;
import com.p17142.vroom.data.dao.RatingDao;
import com.p17142.vroom.data.dao.TripDao;
import com.p17142.vroom.databinding.FragmentCompletedTripBinding;
import com.p17142.vroom.models.Trip;
import com.p17142.vroom.utilities.Logger;
import com.p17142.vroom.utilities.PreferenceManager;
import com.p17142.vroom.ui.application.ratings.RatingUserListDialogFragment;

import java.util.ArrayList;
import java.util.List;

public class CompletedTripFragment extends Fragment {

    private FragmentCompletedTripBinding binding;

    private Trip trip = null;
    private boolean validBundle = false;
    private String loggedUsername = "";
    private final RatingDao ratingDao = RatingDao.getInstance();

    private final static String ACTION_INIT ="INIT";
    private final static String ACTION_RATE_BUTTON_CLICK ="RATE_BTN_CLICK";

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
            Logger.printLogError(CompletedTripFragment.class,"Invalid bundle.");
        }
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        binding = FragmentCompletedTripBinding.inflate(inflater, container, false);
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
            Logger.printLogFatal(CompletedTripFragment.class,"Invalid bundle, force back.");
            requireActivity().getOnBackPressedDispatcher().onBackPressed();
        }
        loading(true);
        PreferenceManager preferenceManager = new PreferenceManager(requireContext());
        loggedUsername = preferenceManager.getString(KEY_USERNAME);

        binding.driverAutoCompleteTextView.setText(trip.getDriverUsername());
        binding.tripStartAutoCompleteTextView.setText(trip.getStartLocation());
        binding.tripEndAutoCompleteTextView.setText(trip.getEndLocation());
        binding.dateInputText.setText(trip.getTripDateDisplayValueddMMM());
        binding.timeInputText.setText(trip.getStartTimeDisplayValue());
        binding.ridersAutoCompleteTextView.setText(trip.getRidersToMaxRidersDisplayValue());
        checkRatings(ACTION_INIT);
    }

    private void setListeners(){
        binding.backButton.setOnClickListener( unused -> requireActivity().getOnBackPressedDispatcher().onBackPressed());
        binding.rateUsersButton.setOnClickListener( unused ->  onRateUserButtonClick());
    }

    private void checkRatings(String action){
        ArrayList<String> usernamesToRate = new ArrayList<>(trip.getAllParticipantUsernames()); // getter returns a list, need ArrayList for bundle
        usernamesToRate.remove(loggedUsername);

        List<Task<Boolean>> tasks = new ArrayList<>();
        for (String usernameToRate: usernamesToRate) {
            Task<Boolean> task = ratingDao.isRated(trip.getTripUid(),loggedUsername,usernameToRate)
                    .addOnSuccessListener(isAlreadyRated -> {
                        if(isAlreadyRated)
                        {
                            usernamesToRate.remove(usernameToRate);
                        }
                    });
            tasks.add(task);
        }
        Tasks.whenAllComplete(tasks).addOnCompleteListener( completedTasks -> {
            if(action.equals(ACTION_INIT)){
                if(usernamesToRate.isEmpty())
                {
                    binding.rateUsersButton.setEnabled(false);
                    binding.rateUsersButton.setClickable(false);
                    binding.rateUsersText.setText("All riders rated");
                }

                loading(false);
            }
            else if(action.equals(ACTION_RATE_BUTTON_CLICK)){
                if(!usernamesToRate.isEmpty()) {
                    Bundle bundle = new Bundle();
                    bundle.putStringArrayList(KEY_USERS, usernamesToRate);
                    bundle.putString(KEY_TRIP_UID, trip.getTripUid());
                    RatingUserListDialogFragment ratingUserListDialogFragment = new RatingUserListDialogFragment();
                    ratingUserListDialogFragment.setArguments(bundle);
                    ratingUserListDialogFragment.show(getActivity().getSupportFragmentManager(), null);

                    getParentFragmentManager().setFragmentResultListener(REQUEST_KEY_RATING_COMPLETE,this, (requestKey,result2) -> {
                        if(result2.getBoolean(IS_SUCCESSFUL)){
                            usernamesToRate.clear();
                            binding.rateUsersButton.setEnabled(false);
                            binding.rateUsersButton.setClickable(false);
                            binding.rateUsersText.setText("All riders rated");
                        }
                    });
                }
                else{
                    Toast.makeText(requireContext(),"All riders already rated!",Toast.LENGTH_SHORT).show();
                    binding.rateUsersButton.setEnabled(false); // weird bug
                    binding.rateUsersButton.setClickable(false);

                    binding.rateUsersText.setText("All riders rated");
                }
            }
        });
    }

    private void onRateUserButtonClick(){
        checkRatings(ACTION_RATE_BUTTON_CLICK);
    }

    private void loading(boolean isLoading){
        if(isLoading){
            binding.loadingBar.setVisibility(View.VISIBLE);
            binding.rateUsersButton.setClickable(false);
            binding.backButton.setClickable(false);
        }
        else{
            binding.loadingBar.setVisibility(View.INVISIBLE);
            binding.rateUsersButton.setClickable(true);
            binding.backButton.setClickable(true);
        }
    }
}