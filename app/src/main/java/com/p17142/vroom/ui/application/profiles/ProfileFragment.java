package com.p17142.vroom.ui.application.profiles;

import static android.app.Activity.RESULT_CANCELED;
import static android.app.Activity.RESULT_OK;

import static com.p17142.vroom.utilities.Constants.DEBUG_MODE;
import static com.p17142.vroom.utilities.Constants.DEFAULT_ENCODED_IMAGE;
import static com.p17142.vroom.utilities.Constants.KEY_IMG_URI;
import static com.p17142.vroom.utilities.Constants.KEY_USERNAME;
import static com.p17142.vroom.utilities.Constants.APP_VERSION;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Bundle;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import android.provider.MediaStore;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import com.google.firebase.auth.FirebaseAuth;
import com.p17142.vroom.auth.AuthManager;
import com.p17142.vroom.data.dao.UserDao;
import com.p17142.vroom.utilities.ImageUtils;
import com.p17142.vroom.ui.authentication.LoginActivity;
import com.p17142.vroom.databinding.FragmentProfileBinding;
import com.p17142.vroom.utilities.Logger;
import com.p17142.vroom.utilities.PreferenceManager;

import java.io.FileNotFoundException;
import java.io.InputStream;
import java.text.SimpleDateFormat;
import java.util.Date;

public class ProfileFragment extends Fragment {
    private PreferenceManager preferenceManager;
    private FragmentProfileBinding binding;
    @SuppressLint("SimpleDateFormat")
    private SimpleDateFormat dateFormat = new SimpleDateFormat("dd MMM yyyy"); // to print type Instant, in member since
    private final UserDao userDao = UserDao.getInstance();
    private String loggedUsername;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        preferenceManager = new PreferenceManager(requireContext());
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        binding = FragmentProfileBinding.inflate(inflater,container,false);
        loading(true);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        init();
        setListeners();
    }

    @SuppressLint("DefaultLocale")
    private void init(){
        loggedUsername = preferenceManager.getString(KEY_USERNAME);
        binding.versionTextView.setText(String.format("v.%s", APP_VERSION));
        userDao.getUserByUsername(loggedUsername)
                        .addOnSuccessListener( signedUser -> {
                            binding.usernameTextView.setText(String.format("@%s", signedUser.getUsername()));
                            binding.emailTextView.setText(signedUser.getEmail());

                            Date memberSinceDate = Date.from(signedUser.getCreatedDate());
                            binding.memberSinceTextView.setText(String.format("Member since %s", dateFormat.format(memberSinceDate)));

                            binding.profileImageView.setImageBitmap(ImageUtils.decodeImage(signedUser.getImageUri()));
                            binding.peopleMetTextView.setText(String.valueOf(signedUser.getNumOfUniquePeopleMet()));

                            int ridesSharedNum = signedUser.getNumOfTripsCompletedAsRider()+signedUser.getNumOfTripsCompletedAsDriver();
                            binding.ridesSharedTextView.setText(String.valueOf(ridesSharedNum));

                            float userRating = signedUser.getUserRating();
                            if(userRating !=0 )
                            {
                                binding.userRatingTextView.setText(String.format("%.1f",userRating / signedUser.getNumOfRatings()));
                            } else
                            {
                                binding.userRatingTextView.setText("-");
                            }
                            loading(false);
                        });
    }

    private void setListeners(){
        binding.logoutImageView.setOnClickListener( unused -> onLogoutClick());

        binding.profileImageView.setOnClickListener( unused -> onProfileImageClick());

        binding.emailTextView.setOnClickListener( unused -> onDebugClick());
    }

    private void onDebugClick()
    {
        if(DEBUG_MODE)
        {
            binding.profileImageView.setImageBitmap(ImageUtils.decodeImage(DEFAULT_ENCODED_IMAGE)); // show new img locally on view
            Toast.makeText(requireContext(),"Image changed, not saved.",Toast.LENGTH_SHORT).show();
        }
    }

    private void onLogoutClick(){
        binding.logoutImageView.setClickable(false);
        Logger.printLog(ProfileFragment.this.getClass(),"Logging out user. Clearing Preference Manager and signing out of Firebase Auth. Moving to Login...");
        PreferenceManager preferenceManager = new PreferenceManager(requireActivity().getApplicationContext());
        Logger.printLog(ProfileFragment.this.getClass(),"Retrieving signed username: "+loggedUsername);
        userDao.deleteFCMToken(loggedUsername)
                .addOnSuccessListener( success -> {
                    if(!success)
                    {
                        Logger.printLogError(ProfileFragment.this.getClass(),"Error clearing fcm token of: "+loggedUsername);
                    }
                });
        preferenceManager.clear();
        AuthManager.signOut();
        Toast.makeText(requireContext(),"Signed out successfully.",Toast.LENGTH_SHORT).show();

        startActivity(new Intent(getActivity(), LoginActivity.class));
        requireActivity().finish();
    }

    private void onProfileImageClick(){
        // change profile image
        binding.profileImageView.setClickable(false);
        Intent intent =  new Intent( Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI); // system auto-handles media storage permission
        intent.addFlags(Intent.FLAG_GRANT_PERSISTABLE_URI_PERMISSION);
        pickImage.launch(intent);
    }

    // uploads image string to database
    private void uploadImgToFirestore(String encodedImage)
    {
        userDao.updateProfileImage(loggedUsername,encodedImage)
                .addOnSuccessListener( success -> {
                   if(success){
                       Logger.printLog(ProfileFragment.class,"New image uploaded to Firestore!");
                   } else{
                       Logger.printLogError(ProfileFragment.class,"Error updating image on Firestore!");
                   }
                });
    }

    private void loading(boolean isLoading){
        if(isLoading){
            binding.profileImageView.setVisibility(View.INVISIBLE);
            binding.logoutImageView.setVisibility(View.INVISIBLE);
            binding.infoLayout.setVisibility(View.INVISIBLE);
            binding.mailImageView.setVisibility(View.INVISIBLE);
            binding.calendarImageView.setVisibility(View.INVISIBLE);
            binding.emailTextView.setVisibility(View.INVISIBLE);
            binding.memberSinceTextView.setVisibility(View.INVISIBLE);
            binding.usernameTextView.setVisibility(View.INVISIBLE);
            binding.plusBack.setVisibility(View.INVISIBLE);
            binding.plusFront.setVisibility(View.INVISIBLE);

            binding.loadingBar.setVisibility(ViewGroup.VISIBLE);
        }else{
            binding.loadingBar.setVisibility(ViewGroup.INVISIBLE);

            binding.profileImageView.setVisibility(View.VISIBLE);
            binding.logoutImageView.setVisibility(View.VISIBLE);
            binding.infoLayout.setVisibility(View.VISIBLE);
            binding.mailImageView.setVisibility(View.VISIBLE);
            binding.calendarImageView.setVisibility(View.VISIBLE);
            binding.emailTextView.setVisibility(View.VISIBLE);
            binding.memberSinceTextView.setVisibility(View.VISIBLE);
            binding.usernameTextView.setVisibility(View.VISIBLE);
            binding.plusBack.setVisibility(View.VISIBLE);
            binding.plusFront.setVisibility(View.VISIBLE);
        }
    }

    // to open the system window to pick a new profile image.
    private final ActivityResultLauncher<Intent> pickImage = registerForActivityResult( new ActivityResultContracts.StartActivityForResult(),
            result -> {
        if (result.getResultCode() == RESULT_OK) {
            if(result.getData() !=null) {
                Uri imageUri = result.getData().getData();
                try{
                    assert imageUri != null;
                    InputStream inputStream = requireActivity().getApplicationContext().getContentResolver().openInputStream(imageUri);
                    Bitmap bitmap = BitmapFactory.decodeStream(inputStream);
                    binding.profileImageView.setImageBitmap(bitmap); // show new img locally on view
                    // now upload image
                    String encodedImage = ImageUtils.encodeImage(bitmap);
                    preferenceManager.putString(KEY_IMG_URI,encodedImage); // save new img locally on preference manager
                    uploadImgToFirestore(encodedImage);  // save new img on database
                } catch (FileNotFoundException e){
                    e.printStackTrace();
                }
            }
        } else if (result.getResultCode() == RESULT_CANCELED) {
            Logger.printLog(ProfileFragment.class,"Cancelled by user.");
        }
        binding.profileImageView.setClickable(true);
            });
}