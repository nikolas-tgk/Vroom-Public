package com.p17142.vroom.ui.authentication;

import static com.p17142.vroom.utilities.Constants.DEBUG_MODE;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.os.Bundle;
import android.text.InputType;
import android.util.Patterns;
import android.view.View;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.google.firebase.auth.FirebaseAuth;
import com.p17142.vroom.R;
import com.p17142.vroom.auth.AuthManager;
import com.p17142.vroom.data.dao.UserDao;
import com.p17142.vroom.databinding.ActivityRegisterBinding;
import com.p17142.vroom.utilities.Logger;

public class RegisterActivity extends AppCompatActivity {
    private FirebaseAuth auth;
    private final UserDao userDao = UserDao.getInstance();
    private AuthManager authManager;
    private ActivityRegisterBinding binding;

    private boolean isPasswordBeingShown = false;
    private boolean  isConfirmPasswordBeingShown = false;

    @SuppressLint("SetTextI18n")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        binding = ActivityRegisterBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.mainReg), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        if(DEBUG_MODE) {
            //!DEBUG VERSION ONLY
            binding.usernameEditText.setText("");
            binding.emailEditText.setText("@debug.com");
            binding.passwordEditText.setText("123123");
            binding.confirmPasswordEditText.setText("123123");
            //!END-DEBUG
        }
        init();
        setListeners();
    }

    private void init(){
        auth = FirebaseAuth.getInstance();
        authManager = new AuthManager(this);
    }

    // check if sign up credentials are valid
    private boolean  isValid(String username,String email, String password, String confirmPassword){
        if(username.isEmpty())
        {
            Toast.makeText(getApplicationContext(),"Username can't be empty!",Toast.LENGTH_SHORT).show();
            return false;
        }
        else if(!username.matches("[a-zA-Z0-9-_.]+")){
            Toast.makeText(getApplicationContext(),"Invalid username!",Toast.LENGTH_SHORT).show();
            return false;
        }
        else if(username.length()<4){
            Toast.makeText(getApplicationContext(),"Please enter a longer username!",Toast.LENGTH_SHORT).show();
            return false;
        }
        else if(username.length()>14){
            Toast.makeText(getApplicationContext(),"Please enter a shorter username!",Toast.LENGTH_SHORT).show();
            return false;
        }
        else if(email.isEmpty())
        {
            Toast.makeText(getApplicationContext(),"Email can't be empty!",Toast.LENGTH_SHORT).show();
            return false;
        }
        else if(email.length()>40)
        {
            Toast.makeText(getApplicationContext(),"Email is too long!",Toast.LENGTH_SHORT).show();
            return false;
        }
        else if(!Patterns.EMAIL_ADDRESS.matcher(email).matches())
        {
            Toast.makeText(getApplicationContext(),"Invalid email address!",Toast.LENGTH_SHORT).show();
            return false;
        }
        else if(password.isEmpty()){
            Toast.makeText(getApplicationContext(),"Password can't be empty!",Toast.LENGTH_SHORT).show();
            return false;
        }
        else if(!password.equals(confirmPassword))
        {
            Toast.makeText(getApplicationContext(),"Password mismatch!",Toast.LENGTH_SHORT).show();
            return false;
        } else if (password.length()<6) {
            Toast.makeText(getApplicationContext(),"Please enter a stronger password!",Toast.LENGTH_SHORT).show();
            return false;
        }
        else if (password.length()>128) {
            Toast.makeText(getApplicationContext(),"Password is too long!",Toast.LENGTH_SHORT).show();
            return false;
        }
        return true;
    }

    private void setListeners(){
        binding.passwordIconImageView.setOnClickListener( unused -> {
            if(isPasswordBeingShown)
            {
                isPasswordBeingShown = false;
                binding.passwordEditText.setInputType( InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD);
                binding.passwordIconImageView.setImageResource(R.drawable.baseline_remove_red_eye_24);
            } else{
                isPasswordBeingShown = true;
                binding.passwordEditText.setInputType(InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD);
                binding.passwordIconImageView.setImageResource(R.drawable.baseline_password_24);
            }
            binding.passwordEditText.setSelection(binding.passwordEditText.length());
        });

        binding.confirmPasswordIconImageView.setOnClickListener( unused -> {
            if(isConfirmPasswordBeingShown)
            {
                isConfirmPasswordBeingShown = false;
                binding.confirmPasswordEditText.setInputType( InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD);
                binding.confirmPasswordIconImageView.setImageResource(R.drawable.baseline_remove_red_eye_24);
            } else{
                isConfirmPasswordBeingShown = true;
                binding.confirmPasswordEditText.setInputType(InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD);
                binding.confirmPasswordIconImageView.setImageResource(R.drawable.baseline_password_24);
            }
            binding.confirmPasswordEditText.setSelection(binding.confirmPasswordEditText.length());
        });

        binding.signInRedirectText.setOnClickListener( unused -> {
            startActivity(new Intent(getApplicationContext(), LoginActivity.class));
            finish(); // good
        });

        binding.signUpButton.setOnClickListener( unused -> {
            loading(true);
            String username = binding.usernameEditText.getText().toString().trim();
            String email = binding.emailEditText.getText().toString().trim();
            String password = binding.passwordEditText.getText().toString().trim();
            String confirmPassword = binding.confirmPasswordEditText.getText().toString().trim();
            
            if(!isValid(username,email,password,confirmPassword)){
                loading(false);
            }
            else{
                checkIfUsernameIsTaken(username,email,password);
            }
        });
    }
    // STEP 1
    private void checkIfUsernameIsTaken(String username, String email, String password){
        userDao.checkIfUsernameExists(username)
                .addOnSuccessListener(usernameFound -> {
                    if (!usernameFound) {
                        // continue
                        checkIfEmailIsTaken(username,email,password);
                    } else { // username already in firestore database
                        Toast.makeText(getApplicationContext(), "Username already exists!", Toast.LENGTH_SHORT).show();
                        loading(false);
                    }
                })
                .addOnFailureListener(e -> { // event unexpected error
                    Logger.printLogFatal(RegisterActivity.class,"Unexpected error on checkIfUsernameIsTaken: "+e);
                    loading(false);
                });
    }

    // STEP 2
    private void checkIfEmailIsTaken(String username, String email, String password){
        userDao.checkIfEmailExists(email)
                .addOnSuccessListener(success -> {
                    if (success) {
                        // continue
                        registerUserToAuthContinueWithFirestore(username,email,password); // checking FirebaseAuth for user is considered bad practice, we will check only our firestore database.
                    } else { // email already in firestore database
                        Toast.makeText(getApplicationContext(), "Email already exists!", Toast.LENGTH_SHORT).show();
                        loading(false);
                    }
                })
                .addOnFailureListener(e -> { // event unexpected error
                    Logger.printLogFatal(RegisterActivity.class,"Unexpected error on checkIfEmailIsTaken: "+e);
                    loading(false);
                });
    }

    // STEP 3
    private void registerUserToAuthContinueWithFirestore(String username, String email, String password)
    {
        authManager.registerUser(email, password, new AuthManager.AuthCallback() {
            @Override
            public void onSuccess() {
                // continue
                registerUserToFirestore(username,email);
            }

            @Override
            public void onFailure(String errorMessage) {
                loading(false);
                Toast.makeText(getApplicationContext(),errorMessage,Toast.LENGTH_SHORT).show();
            }
        });
    }

    // STEP 4
    private void registerUserToFirestore(String username,String email){
        userDao.uploadUserIfNotExists(authManager.getSignedUid(),username, email)
                .addOnSuccessListener(success -> {
                    if (success) {
                        // continue
                        finalizeRegistration();
                    } else { // event executed but found username document already stored, new user was not uploaded
                        Toast.makeText(getApplicationContext(), "Username already exists!", Toast.LENGTH_SHORT).show();
                        loading(false);
                    }
                })
                .addOnFailureListener(e -> { // event unexpected error
                    Logger.printLogFatal(RegisterActivity.class,"Unexpected error on registerUserToFirestore: "+e);
                    loading(false);
                });
    }

    // FINAL STEP 5
    // called only after registering user to both Auth && Firestore
    private void finalizeRegistration(){
        loading(false);
        Toast.makeText(getApplicationContext(),"Account created!",Toast.LENGTH_SHORT).show();
        Logger.printLog(RegisterActivity.class,"User registered successfully");
        auth.signOut(); // required since we are redirecting to login activity. Firebase Auth thinks we logged on otherwise.
        startActivity(new Intent(getApplicationContext(), LoginActivity.class));
        finish(); // finish, we don't want to be able to back-return here.
    }

    private void loading(Boolean isLoading){
        if(isLoading){
            binding.signUpButton.setVisibility(View.INVISIBLE);
            binding.signUpLoadingBar.setVisibility(View.VISIBLE);
        }else{
            binding.signUpLoadingBar.setVisibility(View.INVISIBLE);
            binding.signUpButton.setVisibility(View.VISIBLE);
        }
    }
}