package com.p17142.vroom.ui;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.p17142.vroom.R;
import com.p17142.vroom.auth.AuthManager;
import com.p17142.vroom.databinding.ActivitySplashBinding;
import com.p17142.vroom.ui.application.MainActivity;
import com.p17142.vroom.ui.authentication.LoginActivity;
import com.p17142.vroom.utilities.Constants;
import com.p17142.vroom.utilities.Logger;
import com.p17142.vroom.utilities.PreferenceManager;

public class SplashActivity extends AppCompatActivity {

    private PreferenceManager preferenceManager;

    private ActivitySplashBinding binding;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);

        binding = ActivitySplashBinding.inflate(getLayoutInflater()); //binding
        setContentView(binding.getRoot()); //binding

        binding.versionTextView.setText("v."+Constants.APP_VERSION);

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        new Handler().postDelayed(new Runnable() { // delays the code to run after x delaysMillis // so this loading splash actually shows up in fast scenarios
            @Override
            public void run() {
                if(Constants.DEBUG_MODE) Toast.makeText(getApplicationContext(),"Debug On",Toast.LENGTH_SHORT).show();
                preferenceManager = new PreferenceManager(getApplicationContext());

                showPreferenceManger();

                if(AuthManager.isUserSignedIn() && preferenceManager.getBoolean(Constants.KEY_IS_SIGNED)){
                    Logger.printLog(SplashActivity.class,"User already logged in. Skipping Login Screen. Moving to MainActivity...");
                    startActivity(new Intent(SplashActivity.this, MainActivity.class));
                    finish();
                }
                else{
                    Logger.printLog(SplashActivity.class,"Auto-Login Fail. Clearing Preference Manager. Moving to Login...");
                    preferenceManager.clear();
                    startActivity(new Intent(SplashActivity.this, LoginActivity.class));
                    finish();
                }
            }
        },1000); //
    }
    public void showPreferenceManger(){
        Logger.printLog(SplashActivity.class,Constants.KEY_USERNAME+" -> "+ preferenceManager.getString(Constants.KEY_USERNAME));
        Logger.printLog(SplashActivity.class,Constants.KEY_UID+" -> "+ preferenceManager.getString(Constants.KEY_UID));
    }
}