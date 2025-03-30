package com.p17142.vroom.ui.authentication;

import static com.p17142.vroom.utilities.Constants.DEBUG_MODE;

import android.content.Intent;
import android.os.Bundle;
import android.text.Editable;
import android.text.InputType;
import android.text.TextWatcher;
import android.util.Patterns;
import android.view.View;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.p17142.vroom.R;
import com.p17142.vroom.auth.AuthManager;
import com.p17142.vroom.data.dao.UserDao;
import com.p17142.vroom.databinding.ActivityLoginBinding;
import com.p17142.vroom.models.User;
import com.p17142.vroom.ui.application.MainActivity;
import com.p17142.vroom.utilities.Logger;
import com.p17142.vroom.utilities.PreferenceManager;

public class LoginActivity extends AppCompatActivity {
    private ActivityLoginBinding binding;
    private PreferenceManager preferenceManager;
    private AuthManager authManager;
    private final UserDao userDao = UserDao.getInstance();
    private boolean isPassBeingShown = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityLoginBinding.inflate(getLayoutInflater()); //binding
        setContentView(binding.getRoot()); //binding
        EdgeToEdge.enable(this);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> { // set system borders
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        init();
        setListeners();
    }

    private void init(){
        preferenceManager = new PreferenceManager(getApplicationContext()); // initialize preference manager for this activity
        authManager = new AuthManager(this); // set-up authManager, provide context to initialize preference manager use inside authManager

        //!DEBUG VERSION ONLY
        if(DEBUG_MODE) {
            binding.emailEditText.setText("@debug.com");
            binding.passwordEditText.setText("123123");

            binding.adminButton.setOnClickListener(v -> {
                binding.emailEditText.setText("nikolast@debug.com");
                binding.passwordEditText.setText("123123");
                binding.signInButton.callOnClick();
            });
            binding.debugButton.setOnClickListener(v -> {
                binding.emailEditText.setText("arisg@debug.com");
                binding.passwordEditText.setText("123123");
                binding.signInButton.callOnClick();
            });
        }
        else{
            binding.debugButton.setVisibility(View.INVISIBLE);
            binding.adminButton.setVisibility(View.INVISIBLE);
            binding.emailEditText.setText("");
            binding.passwordEditText.setText("");
        }
        //!END-DEBUG
    }

    private boolean isUserInputValid(String email, String password){
        if(email.isEmpty() && password.isEmpty()){
            Toast.makeText(LoginActivity.this,"Credentials not provided.",Toast.LENGTH_SHORT).show();
            return false;
        }
        else if(email.isEmpty()){
            Toast.makeText(LoginActivity.this,"No email provided.",Toast.LENGTH_SHORT).show();
            return false;
        }
        else if(password.isEmpty()){
            Toast.makeText(LoginActivity.this,"No password provided.",Toast.LENGTH_SHORT).show();
            return false;
        }
        else if(!Patterns.EMAIL_ADDRESS.matcher(email).matches())
        {
            Toast.makeText(LoginActivity.this,"Invalid email address!",Toast.LENGTH_SHORT).show();
            return false;
        }
        else{
            return  true; // continue login process
        }
    }

    private void setListeners(){
        binding.passwordIconImageView.setOnClickListener( unused -> passwordIconClicked());
        binding.signUpRedirectText.setOnClickListener( unused -> startActivity(new Intent(LoginActivity.this, RegisterActivity.class))); // go to register activity
        binding.forgotPassRedirect.setOnClickListener( unused -> startActivity(new Intent(LoginActivity.this, ForgotPasswordActivity.class))); // go to forgot password activity
        binding.signInButton.setOnClickListener( unused -> signInProcedure());
        binding.passwordEditText.addTextChangedListener(new TextWatcher() { // restrict space usage
            @Override
            public void beforeTextChanged(CharSequence charSequence, int i, int i1, int i2) {
                // not needed
            }

            @Override
            public void onTextChanged(CharSequence charSequence, int i, int i1, int i2) {
                // not needed
            }

            @Override
            public void afterTextChanged(Editable c) {
                String input = c.toString();
                if (input.contains(" ")) {
                    String newText = input.replace(" ", ""); // remove space
                    binding.passwordEditText.setText(newText);
                    binding.passwordEditText.setSelection(newText.length()); // Move cursor to the end
                }
            }
        });
    }

    private void passwordIconClicked(){
        if(isPassBeingShown)
        {
            isPassBeingShown = false;
            binding.passwordEditText.setInputType( InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD);
            binding.passwordIconImageView.setImageResource(R.drawable.baseline_remove_red_eye_24); // change icon image
        } else{
            isPassBeingShown = true;
            binding.passwordEditText.setInputType(InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD);
            binding.passwordIconImageView.setImageResource(R.drawable.baseline_password_24); // change icon image
        }
        binding.passwordEditText.setSelection(binding.passwordEditText.length()); //
    }

    private void signInProcedure(){
        loading(true);
        String email = binding.emailEditText.getText().toString().trim();
        String password = binding.passwordEditText.getText().toString().trim();

        if(isUserInputValid(email,password)){
            authManager.signInUser(email, password, new AuthManager.AuthCallback() {
                @Override
                public void onSuccess() {
                    getUsernameFromUID();
                }
                @Override
                public void onFailure(String errorMessage) {
                    loading(false);
                    Toast.makeText(LoginActivity.this,errorMessage,Toast.LENGTH_SHORT).show();
                }
            });
        }
        else{
            loading(false); // invalid input case
        }
    }

    private void getUsernameFromUID(){
        userDao.getUserByUid(authManager.getSignedUid()).addOnCompleteListener(task -> {
            if (task.isSuccessful() && task.getResult() != null) {
                User signedUser = task.getResult();
                preferenceManager.storeLogin(); // KEY_IS_SIGNED == true
                preferenceManager.storeUser(signedUser); // store user details in storage for in app later use

                // proceed to MainActivity
                Toast.makeText(LoginActivity.this, "Authentication Success!", Toast.LENGTH_SHORT).show();
                startActivity(new Intent(LoginActivity.this, MainActivity.class));
                finish();
            } else {
                Logger.printLogFatal(LoginActivity.class, "Failed to fetch user data: " + task.getException());
                Toast.makeText(LoginActivity.this, "Login Error", Toast.LENGTH_SHORT).show();
            }
            loading(false);
        });
    }

    private void loading(Boolean isLoading){
        if(isLoading){
            binding.signInButton.setVisibility(View.INVISIBLE);
            binding.loginProgessBar.setVisibility(View.VISIBLE);
        }else{
            binding.loginProgessBar.setVisibility(View.INVISIBLE);
            binding.signInButton.setVisibility(View.VISIBLE);
        }
    }
}