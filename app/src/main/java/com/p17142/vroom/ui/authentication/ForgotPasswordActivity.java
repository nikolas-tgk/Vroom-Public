package com.p17142.vroom.ui.authentication;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
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
import com.p17142.vroom.databinding.ActivityForgotPasswordBinding;

public class ForgotPasswordActivity extends AppCompatActivity {
    private AuthManager authManager;
    private ActivityForgotPasswordBinding binding;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityForgotPasswordBinding.inflate(getLayoutInflater()); //binding
        setContentView(binding.getRoot()); //binding
        EdgeToEdge.enable(this);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        init();
        setListeners();
    }

    private void init(){
        authManager = new AuthManager(this);
    }

    private void setListeners(){
        binding.resetPasswordButton.setOnClickListener( unused -> {
            String email = binding.emailEditText.getText().toString().trim();
            loading(true);
            if(email.isEmpty()){
                //do nothing
                loading(false);
            }
            else if(!Patterns.EMAIL_ADDRESS.matcher(email).matches())
            {
                Toast.makeText(ForgotPasswordActivity.this,"Invalid email address!",Toast.LENGTH_SHORT).show();
                loading(false);
            }
            else{
                authManager.sendPasswordResetEmail(email, new AuthManager.AuthCallback() {
                    @Override
                    public void onSuccess() {
                        Toast.makeText(ForgotPasswordActivity.this,"If the address is registered, a reset link will be sent shortly..",Toast.LENGTH_LONG).show();
                        Handler handler = new Handler();
                        handler.postDelayed( () -> {
                            loading(false);
                            finish();
                        }, 2500);
                    }

                    @Override
                    public void onFailure(String errorMessage) {
                        Toast.makeText(ForgotPasswordActivity.this, errorMessage,Toast.LENGTH_LONG).show();
                    }
                });
            }
        });

        binding.signInRedirectText.setOnClickListener(view -> {
            startActivity(new Intent(ForgotPasswordActivity.this, LoginActivity.class));
            finish();
        });
    }

    private void loading(Boolean isLoading){
        if(isLoading){
            binding.resetPasswordButton.setVisibility(View.INVISIBLE);
            binding.forgotPassLoadingBar.setVisibility(View.VISIBLE);
        }else{
            binding.forgotPassLoadingBar.setVisibility(View.INVISIBLE);
            binding.resetPasswordButton.setVisibility(View.VISIBLE);
        }
    }
}