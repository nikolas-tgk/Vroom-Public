package com.p17142.vroom.ui.application.trips;

import static com.p17142.vroom.utilities.Constants.KEY_TRIP;
import static com.p17142.vroom.utilities.Constants.KEY_USERNAME;

import android.annotation.SuppressLint;
import android.app.Dialog;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.Toast;

import com.p17142.vroom.R;
import com.p17142.vroom.data.dao.TripDao;
import com.p17142.vroom.data.dao.UserDao;
import com.p17142.vroom.databinding.FragmentManageOwnedTripBinding;
import com.p17142.vroom.models.Trip;
import com.p17142.vroom.utilities.FragUtils;
import com.p17142.vroom.utilities.Logger;
import com.p17142.vroom.utilities.PreferenceManager;

import java.util.Objects;

public class ManageOwnedTripFragment extends Fragment {

    private FragmentManageOwnedTripBinding binding;
    private PreferenceManager preferenceManager;
    private Trip trip = null;
    private final TripDao tripDao = TripDao.getInstance();
    private final UserDao userDao = UserDao.getInstance();
    private boolean validBundle = false;
    private String action = "";
    private final String ACTION_COMPLETE = "COMPLETE";
    private final String ACTION_DELETE = "DELETE";

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
            Logger.printLogError(FragmentManageOwnedTripBinding.class,"Invalid bundle.");
        }
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        binding = FragmentManageOwnedTripBinding.inflate(inflater, container, false);
        return  binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        init();
        setListeners();
    }

    @SuppressLint("SetTextI18n")
    private void init(){

        if( !validBundle )
        {
            Logger.printLogFatal(ManageOwnedTripFragment.class,"Invalid bundle, force back.");
            requireActivity().getOnBackPressedDispatcher().onBackPressed();
        }
        preferenceManager = new PreferenceManager(requireContext());

        if(trip != null)
        {
            determineAction();

            binding.driverAutoCompleteTextView.setText("You");
            binding.tripStartAutoCompleteTextView.setText(trip.getStartLocation());
            binding.tripEndAutoCompleteTextView.setText(trip.getEndLocation());
            binding.dateInputText.setText(trip.getTripDateDisplayValueddMMM());
            binding.timeInputText.setText(trip.getStartTimeDisplayValue());
            binding.ridersAutoCompleteTextView.setText(trip.getRidersToMaxRidersDisplayValue());
            isLoading(false);
        }
    }

    private void setListeners(){
        binding.backButton.setOnClickListener( unused -> requireActivity().getOnBackPressedDispatcher().onBackPressed());

        binding.actionButton.setOnClickListener( unused -> actionClick());
    }

    @SuppressLint("SetTextI18n")
    private void determineAction(){
        isLoading(true);
        if(this.trip.getInProgress())
        {
            binding.actionText.setText("Mark as Complete");
            action = ACTION_COMPLETE;
        }
        else{
            binding.actionText.setText("Delete Trip");
            action = ACTION_DELETE;
        }
    }

    private void actionClick(){
        if(action.equals(ACTION_DELETE))
        {
            showDeleteDialog();
        }
        else if(action.equals(ACTION_COMPLETE)){
            tryCompleteTrip();
        }
    }

    private void tryCompleteTrip(){
        tripDao.setTripComplete(trip.getTripUid())
                .addOnSuccessListener( success -> {
                    if(success)
                    {
                        userDao.updateUsersOnTripComplete(trip.getRiderUsernames(),preferenceManager.getString(KEY_USERNAME))
                                        .addOnSuccessListener( isSuccessful -> {
                                            if(isSuccessful)
                                            {
                                                Toast.makeText(requireContext(),"Trip completed!",Toast.LENGTH_SHORT).show();
                                                popBackStackAndGoHome();
                                            }
                                            else{
                                                Toast.makeText(requireContext(),"Unexpected error",Toast.LENGTH_SHORT).show();
                                                popBackStackAndGoHome();
                                            }
                                        });
                    }
                    else{
                        Toast.makeText(requireContext(),"Unexpected error",Toast.LENGTH_SHORT).show();
                        popBackStackAndGoHome();
                    }
                });
    }

    private void showDeleteDialog(){
        Dialog dialog = new Dialog(requireContext());
        dialog.setContentView(R.layout.delete_trip_dialog_box);
        Objects.requireNonNull(dialog.getWindow()).setLayout(ViewGroup.LayoutParams.WRAP_CONTENT,ViewGroup.LayoutParams.WRAP_CONTENT);
        dialog.getWindow().setBackgroundDrawableResource(R.drawable.custom_background_dialog_box);
        dialog.setCancelable(true);
        Button cancelButton = dialog.findViewById(R.id.cancelAction);
        Button okButton = dialog.findViewById(R.id.okAction);

        cancelButton.setOnClickListener((unused2) -> dialog.cancel());
        okButton.setOnClickListener((unused2) -> {
            tryDeleteTrip();
            dialog.dismiss();
        });

        dialog.setOnCancelListener( (unused2) -> isLoading(false));

        dialog.show();
    }

    private void tryDeleteTrip(){
        isLoading(true);
        tripDao.getTripByUid(trip.getTripUid(),preferenceManager.getString(KEY_USERNAME))
                .addOnSuccessListener( retrievedTrip -> {
                    if(retrievedTrip != null)
                    {
                        if(retrievedTrip.hasTripStartTimeDateElapsed()){ // too late to delete trip
                            isLoading(false);
                            Toast.makeText(requireContext(),"You can't delete this trip anymore.",Toast.LENGTH_SHORT).show();
                        }
                        else{
                            Logger.printLog(ManageOwnedTripFragment.class,"Ok to delete trip!");
                            deleteTrip();
                        }
                    }
                    else{
                        Logger.printLogFatal(ManageOwnedTripFragment.class,"Trip was not found!");
                        Toast.makeText(requireContext(),"Trip has already been deleted!",Toast.LENGTH_SHORT).show();
                        popBackStackAndGoHome();
                    }

                }).addOnFailureListener( e -> {
                    Logger.printLogFatal(ManageOwnedTripFragment.class,"Unexpected Error!"+e);
                    Toast.makeText(requireContext(),"Unexpected error!",Toast.LENGTH_SHORT).show();
                    isLoading(false);
                });
    }

    private void deleteTrip(){
        tripDao.deleteTrip(trip.getTripUid())
                .addOnCompleteListener( task -> {
                    if (task.isSuccessful())
                    {
                        if(task.getResult())
                        {
                            Toast.makeText(requireContext(),"Trip deleted successfully",Toast.LENGTH_SHORT).show();
                            Logger.printLog(ManageOwnedTripFragment.class,"Trip deleted successfully, trip uid: "+trip.getTripUid());
                            popBackStackAndGoHome();
                        }
                        else{
                            Logger.printLogFatal(ManageOwnedTripFragment.class,"deleteTrip: trip not found");
                            Toast.makeText(requireContext(),"Trip not found.",Toast.LENGTH_SHORT).show();
                            popBackStackAndGoHome();
                            isLoading(false);
                        }
                    }
                    else{
                        Toast.makeText(requireContext(),"Unexpected error!",Toast.LENGTH_SHORT).show();
                        Logger.printLogFatal(ManageOwnedTripFragment.class,"Unexpected error on deleteTrip: "+task.getException());
                        isLoading(false);
                    }
                });
    }

    private void popBackStackAndGoHome(){
        requireActivity().getSupportFragmentManager().popBackStack();
        TripHomeFragment fragment = new TripHomeFragment();
        FragUtils.replaceFragment(getParentFragmentManager(), fragment, null);
    }

    private void isLoading(Boolean loading)
    {
        if(loading)
        {
            binding.backButton.setClickable(false);
            binding.actionButton.setClickable(false);
            binding.loadingBar.setVisibility(View.VISIBLE);

        } else {
            binding.backButton.setClickable(true);
            binding.actionButton.setClickable(true);
            binding.loadingBar.setVisibility(View.INVISIBLE);
        }
    }
}