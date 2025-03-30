package com.p17142.vroom.ui.application.trips;

import static com.p17142.vroom.utilities.Constants.CHAT_ACTIVITY;
import static com.p17142.vroom.utilities.Constants.KEY_ACTIVITY_SWAP;
import static com.p17142.vroom.utilities.Constants.KEY_IMG_URI;
import static com.p17142.vroom.utilities.Constants.KEY_NEW_TRIP_INVITE_NOTIFICATION;
import static com.p17142.vroom.utilities.Constants.KEY_SOURCE;
import static com.p17142.vroom.utilities.Constants.KEY_TRIP_LIST;
import static com.p17142.vroom.utilities.Constants.KEY_USER;
import static com.p17142.vroom.utilities.Constants.KEY_USERNAME;

import android.content.Intent;
import android.os.Bundle;

import androidx.activity.OnBackPressedCallback;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import com.p17142.vroom.adapters.InvitableTripAdapter;
import com.p17142.vroom.data.dao.TripDao;
import com.p17142.vroom.data.dao.UserDao;
import com.p17142.vroom.databinding.FragmentSelectTripForInviteBinding;
import com.p17142.vroom.listeners.TripListener;
import com.p17142.vroom.models.Trip;
import com.p17142.vroom.models.User;
import com.p17142.vroom.notifications.FcmNotificationSender;
import com.p17142.vroom.utilities.FragUtils;
import com.p17142.vroom.utilities.Logger;
import com.p17142.vroom.utilities.PreferenceManager;
import com.p17142.vroom.ui.application.chat.ChatActivity;

import java.util.ArrayList;
import java.util.List;


public class SelectTripForInviteFragment extends Fragment implements TripListener {
    private FragmentSelectTripForInviteBinding binding;
    private PreferenceManager preferenceManager;
    private List<Trip> invitableTrips = new ArrayList<>();
    private User userToInvite = null;
    private String usernameToInvite = "";
    private String source = "";
    private final TripDao tripDao = TripDao.getInstance();
    private final UserDao userDao = UserDao.getInstance();

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        binding = FragmentSelectTripForInviteBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        setListeners();
        Bundle bundle = getArguments();
        try{
            invitableTrips = (List<Trip>) bundle.getSerializable(KEY_TRIP_LIST);
            userToInvite = (User) bundle.getSerializable(KEY_USER);
            usernameToInvite = userToInvite.getUsername();
            source = bundle.getString(KEY_SOURCE);
        }
        catch (Exception e){
            showErrorMessage("Error received null bundle, or null trip list.");
            Logger.printLogError(SelectTripForInviteFragment.class,"Error received null bundle, or null trip list."+e);
            // go back
        }
        init();
    }

    private void init(){
        preferenceManager = new PreferenceManager(requireContext());
        for (Trip invitableTrip: invitableTrips) {
            invitableTrip.setDriverImageUri(preferenceManager.getString(KEY_IMG_URI));
        }
        InvitableTripAdapter minimalTripAdapter = new InvitableTripAdapter(invitableTrips,this);
        binding.tripsRecyclerView.setAdapter(minimalTripAdapter);

        loading(false);
        binding.tripsRecyclerView.setVisibility(View.VISIBLE);
    }

    private void setListeners()
    {
        binding.backArrowImage.setOnClickListener( unused ->{
            requireActivity().getOnBackPressedDispatcher().onBackPressed();
        });

        // custom back press behavior
        OnBackPressedCallback callback = new OnBackPressedCallback(true) {
            @Override
            public void handleOnBackPressed() {
                // custom back press behavior
                if(source.equals(CHAT_ACTIVITY)){ // if we were prompted here from chat activity
                    preferenceManager.putBoolean(KEY_ACTIVITY_SWAP,true);
                    Intent intent = new Intent(getActivity(), ChatActivity.class);
                    intent.putExtra(KEY_USER, userToInvite);
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

    private void loading(Boolean isLoading){
        if (isLoading) {
            binding.loadingTripsBar.setVisibility(View.VISIBLE);
        }else{
            binding.loadingTripsBar.setVisibility(View.INVISIBLE);
        }
    }

    private void showErrorMessage(String message){
        loading(false);
        binding.errorMessageText.setText(String.format("%s",message));
        binding.errorMessageText.setVisibility(View.VISIBLE);
    }

    @Override
    public void onTripClicked(Trip trip) {
        Logger.printLog(SelectTripForInviteFragment.class, "click.");

        tripDao.getTripByUid(trip.getTripUid(),preferenceManager.getString(KEY_USERNAME))
                .addOnSuccessListener( updatedTrip -> {
                    if( updatedTrip == null ){
                        // error trip has been deleted / not found in db
                        Logger.printLogError(SelectTripForInviteFragment.class, "error trip has been deleted / not found in db.");
                        popBackStackAndGoToHome();

                    }
                    if(updatedTrip.hasTripStartTimeDateElapsed())
                    {
                        // too late to invite
                        Logger.printLogError(SelectTripForInviteFragment.class, "too late to invite");
                        popBackStackAndGoBack();
                    } else if (updatedTrip.areMaxRiders()) {
                        // already maxed riders
                        Logger.printLogError(SelectTripForInviteFragment.class, "already maxed riders");
                        popBackStackAndGoBack();

                    }
                    else if(updatedTrip.isRider(usernameToInvite)){
                        Toast.makeText(requireContext(),"User is already a rider!",Toast.LENGTH_SHORT).show();
                        Logger.printLogError(SelectTripForInviteFragment.class, "Already a rider on this trip.");
                        popBackStackAndGoBack();
                    }
                    else if(!updatedTrip.isInvited(usernameToInvite))
                    {
                        // ok, to send invite
                        tripDao.sendNewInvite(trip.getTripUid(),usernameToInvite)
                                .addOnCompleteListener( task -> {
                                    if( task.isSuccessful() )
                                    {
                                        Logger.printLog(SelectTripForInviteFragment.class, "User invited successfully.");
                                        Toast.makeText(requireContext(),"User invited!",Toast.LENGTH_SHORT).show();
                                        userDao.provideFcm(usernameToInvite).addOnSuccessListener( targetFcm -> {
                                            if(targetFcm != null)
                                            {
                                                FcmNotificationSender fcmNotificationSender = new FcmNotificationSender(targetFcm,
                                                        "Vroom Trip Invite","User "+trip.getDriverUsername()+" has invited you to their trip!",getContext(),trip.getDriverUsername(),trip.getTripUid(),KEY_NEW_TRIP_INVITE_NOTIFICATION);
                                                fcmNotificationSender.sendNotification();
                                            }
                                            popBackStackAndGoBack();

                                        });
                                    }
                                    else{
                                        // error inviting user
                                        Logger.printLogFatal(SelectTripForInviteFragment.class, "// error inviting user"+task.getException());
                                        popBackStackAndGoToHome();
                                    }
                                });
                    }
                    else if(updatedTrip.isInvited(usernameToInvite)){
                        // user already invited
                        Toast.makeText(requireContext(),"User is already invited!",Toast.LENGTH_SHORT).show();
                        popBackStackAndGoBack();
                    }
                });
    }

    private void popBackStackAndGoToHome(){
        requireActivity().getSupportFragmentManager().popBackStack();
        TripHomeFragment fragment = new TripHomeFragment();
        FragUtils.replaceFragment(getParentFragmentManager(), fragment, null);
        loading(false);
    }

    private void popBackStackAndGoBack(){
        requireActivity().getSupportFragmentManager().popBackStack();
        requireActivity().getOnBackPressedDispatcher().onBackPressed();
        loading(false);
    }
}