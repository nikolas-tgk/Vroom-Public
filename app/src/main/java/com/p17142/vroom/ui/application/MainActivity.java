package com.p17142.vroom.ui.application;

import static com.p17142.vroom.utilities.Constants.*;
import static com.p17142.vroom.utilities.Logger.printLog;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentTransaction;

import com.google.firebase.messaging.FirebaseMessaging;
import com.p17142.vroom.R;
import com.p17142.vroom.auth.AuthManager;
import com.p17142.vroom.data.dao.UserDao;
import com.p17142.vroom.models.Trip;
import com.p17142.vroom.ui.authentication.LoginActivity;
import com.p17142.vroom.utilities.Constants;
import com.p17142.vroom.utilities.FragUtils;
import com.p17142.vroom.ui.application.profiles.ProfileFragment;
import com.p17142.vroom.ui.application.chat.RecentConversationsFragment;
import com.p17142.vroom.ui.application.trips.TripHomeFragment;
import com.p17142.vroom.ui.application.profiles.OtherUserProfileFragment;
import com.p17142.vroom.ui.application.trips.SelectTripForInviteFragment;
import com.p17142.vroom.ui.application.trips.TripFinderFragment;
import com.p17142.vroom.databinding.ActivityMainBinding;
import com.p17142.vroom.models.User;
import com.p17142.vroom.utilities.Logger;
import com.p17142.vroom.utilities.PreferenceManager;
import com.p17142.vroom.ui.RootActivity;

import java.io.Serializable;
import java.util.List;

public class MainActivity extends RootActivity {

    private ActivityMainBinding binding;
    private PreferenceManager preferenceManager;
    private final UserDao userDao = UserDao.getInstance();

    private String ownToken = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        preferenceManager = new PreferenceManager(getApplicationContext());

        if(!checkAuthorization())
        {
            return;
        }

        String fragmentToLoad = getIntent().getStringExtra(KEY_DESTINATION_FRAGMENT);
        // load the appropriate fragment if any
        if (fragmentToLoad != null) {
            Fragment fragment = null;
            String source = "";
            Bundle bundle = new Bundle();
            switch (fragmentToLoad) {
                case THIRD_USER_PROFILE_FRAGMENT:
                    fragment = new OtherUserProfileFragment();
                    String username =  getIntent().getStringExtra(KEY_USERNAME);
                    source = getIntent().getStringExtra(KEY_SOURCE);
                    bundle.putString(KEY_USERNAME,username);
                    bundle.putString(KEY_SOURCE,source);
                    fragment.setArguments(bundle);
                    FragUtils.replaceFragment(getSupportFragmentManager(), fragment, null);
                    break;
                case INVITE_USER_FRAGMENT:
                    fragment = new SelectTripForInviteFragment();
                    source = getIntent().getStringExtra(KEY_SOURCE);
                    User receiver = (User) getIntent().getSerializableExtra(KEY_USER);
                    List<Trip> trips = (List<Trip>) getIntent().getSerializableExtra(KEY_TRIP_LIST);
                    bundle.putSerializable(KEY_USER,receiver);
                    bundle.putSerializable(KEY_TRIP_LIST,(Serializable) trips);
                    bundle.putString(KEY_SOURCE,source);
                    fragment.setArguments(bundle);
                    FragUtils.replaceFragment(getSupportFragmentManager(), fragment, null);
                    break;
            }
        }
        else{
            replaceFragment(new TripHomeFragment());
        }

        binding = ActivityMainBinding.inflate(getLayoutInflater()); //binding
        setContentView(binding.getRoot()); //binding

        setListeners();
        getFcmToken();
    }

    private boolean checkAuthorization(){ // possibility of user being logged off but then clicked on new message notification, force log out.
        if(!AuthManager.isUserSignedIn() || !preferenceManager.getBoolean(Constants.KEY_IS_SIGNED)){
            Logger.printLogError(MainActivity.class,"Illegal authorization, force logout.");
            preferenceManager.clear();
            AuthManager.signOut();
            Toast.makeText(this,"You have been logged out.",Toast.LENGTH_SHORT).show();
            startActivity(new Intent(this, LoginActivity.class)); // this will throw null/empty loggedUsername <- (sourced by) preferenceManager on RootActivity, handling this specifically onResume-onCreate-onPause overrides with if condition.
            finish();
            return false;
        }
        else{
            Logger.printLog(MainActivity.class,"User authorized");
            return true;
        }
    }

    private void updateFcmToken(String token){
        userDao.updateFcmToken(preferenceManager.getString(KEY_USERNAME),token)
                .addOnSuccessListener( successful ->{
                    if(successful)
                    {
                        ownToken = token;
                        Logger.printLog(MainActivity.class, "Fcm token updated successfully.");
                    }
                    else{
                        Logger.printLogFatal(MainActivity.class, "Fcm update error!, check firestore.");
                    }
                });
    }

    private void getFcmToken(){
        FirebaseMessaging.getInstance().getToken().addOnSuccessListener(this::updateFcmToken).addOnFailureListener( e ->
        {
            Logger.printLogFatal(MainActivity.class, "POSSIBLE EMULATOR ERROR! FCM TOKEN WAS NOT RETRIEVED. Try uninstalling and re-installing the app. Wipe all emulator data, rebuild project. Restart Android Studio."+e.getMessage());
            Toast.makeText(getApplicationContext(),"FCM emulator-specific token error. Follow log instructions. Wipe data, re-install application.",Toast.LENGTH_LONG).show();
        });
    }

    private void setListeners(){
        binding.bottomNavigationView.setOnItemSelectedListener(item -> { // binding
            int itemClicked = item.getItemId();
            if(itemClicked== R.id.home)
            {
                replaceFragment(new TripHomeFragment());
            } else if (itemClicked== R.id.search) {
                replaceFragment(new TripFinderFragment());
            }
            else if (itemClicked== R.id.chats) {
                replaceFragment(new RecentConversationsFragment());
            }
            else if (itemClicked== R.id.profile) {
                replaceFragment(new ProfileFragment());
            }
            return true;
        });
    }

    private void replaceFragment(Fragment fragment){
        FragmentManager fragmentManager = getSupportFragmentManager();
        FragmentTransaction fragmentTransaction = fragmentManager.beginTransaction();
        fragmentTransaction.replace(R.id.frameLayout,fragment);
        fragmentTransaction.commit();
    }
}