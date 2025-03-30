package com.p17142.vroom.ui;

import static com.p17142.vroom.utilities.Constants.KEY_ACTIVITY_SWAP;
import static com.p17142.vroom.utilities.Constants.KEY_USERNAME;

import android.os.Bundle;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.p17142.vroom.data.dao.UserDao;
import com.p17142.vroom.utilities.Logger;
import com.p17142.vroom.utilities.PreferenceManager;

public class RootActivity extends AppCompatActivity {

    private PreferenceManager preferenceManager;
    private String loggedUsername;
    private final UserDao userDao = UserDao.getInstance();
    private boolean doNotDoubleExecute = false;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        preferenceManager = new PreferenceManager(getApplicationContext());
        loggedUsername = preferenceManager.getString(KEY_USERNAME);
        if(preferenceManager.getBoolean(KEY_ACTIVITY_SWAP) != null && preferenceManager.getBoolean(KEY_ACTIVITY_SWAP))
        {
            preferenceManager.putBoolean(KEY_ACTIVITY_SWAP,false);
            doNotDoubleExecute = true;
        }
        else{
            if(loggedUsername != null && !loggedUsername.isEmpty())
            {
                userDao.updateOnline(loggedUsername);
                Logger.printLog(RootActivity.class,"User "+loggedUsername+" is now online.");
                doNotDoubleExecute = true;
            }
            else{
                Logger.printLogError(RootActivity.class, "Illegal loggedUsername value, ignore on illegal authorization case");
            }

        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        if(preferenceManager.getBoolean(KEY_ACTIVITY_SWAP) != null && preferenceManager.getBoolean(KEY_ACTIVITY_SWAP))
        {
            Logger.printLog(RootActivity.class,"User "+loggedUsername+" retaining online user status.");
            preferenceManager.putBoolean(KEY_ACTIVITY_SWAP,false);
        }
        else{
            if(loggedUsername != null && !loggedUsername.isEmpty())
            {
                userDao.updateOffline(loggedUsername);
                Logger.printLog(RootActivity.class,"User "+loggedUsername+" went offline.");
            }
            else{
                Logger.printLogError(RootActivity.class, "Illegal loggedUsername value, ignore on illegal authorization case");
            }

        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        if(!doNotDoubleExecute && loggedUsername != null && !loggedUsername.isEmpty())
        {
            userDao.updateOnline(loggedUsername);
            Logger.printLog(RootActivity.class,"User "+loggedUsername+" is back online.");
        }
        else if(!doNotDoubleExecute){
            Logger.printLogError(RootActivity.class, "Illegal loggedUsername value, ignore on illegal authorization case");
        }
    }
}
