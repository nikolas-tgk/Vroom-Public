package com.p17142.vroom.ui.application.ratings;

import static com.p17142.vroom.utilities.Constants.IS_SUCCESSFUL;
import static com.p17142.vroom.utilities.Constants.KEY_TRIP_UID;
import static com.p17142.vroom.utilities.Constants.KEY_USER;
import static com.p17142.vroom.utilities.Constants.KEY_USERNAME;
import static com.p17142.vroom.utilities.Constants.KEY_USERS;
import static com.p17142.vroom.utilities.Constants.REQUEST_KEY_RATE_SUBMIT;

import android.content.DialogInterface;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.DialogFragment;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import com.p17142.vroom.R;
import com.p17142.vroom.data.dao.RatingDao;
import com.p17142.vroom.databinding.FragmentSetRatingBinding;
import com.p17142.vroom.models.User;
import com.p17142.vroom.utilities.Logger;
import com.p17142.vroom.utilities.ImageUtils;
import com.p17142.vroom.utilities.PreferenceManager;


public class SetRatingDialogFragment extends DialogFragment {

    private FragmentSetRatingBinding binding;
    private PreferenceManager preferenceManager;
    private boolean validBundle = false;
    private User userToRate = null;
    private String tripUid = "";
    private RatingDao ratingDao;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setStyle(STYLE_NORMAL, R.style.AppTheme_DialogOverlay); // fixes size issue + dialog corner style
        Bundle bundle = getArguments();
        if (bundle != null) {
            User userToRate = (User) bundle.getSerializable(KEY_USER);
            String tripUid = bundle.getString(KEY_TRIP_UID);
            if (userToRate != null ) {
                this.userToRate = userToRate;
                this.tripUid = tripUid;
                validBundle = true;
            }
        }
        else{
            validBundle = false;
            Logger.printLogError(SetRatingDialogFragment.class,"Invalid bundle.");
        }
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        binding = FragmentSetRatingBinding.inflate(inflater,container,false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        init();
        setListeners();
    }

    private void init(){
        if(!validBundle)
        {
            Logger.printLogFatal(SetRatingDialogFragment.class,"Invalid bundle, force back.");
            dismiss();
        }
        ratingDao = RatingDao.getInstance();
        preferenceManager = new PreferenceManager(requireContext());

        binding.profileImageView.setImageBitmap(ImageUtils.decodeImage(userToRate.getImageUri()));
        binding.usernameTextView.setText(String.format("@%s", userToRate.getUsername()));
    }

    private void setListeners(){
        binding.submitButton.setOnClickListener( unused -> onSubmitClick() );
        binding.backImageView.setOnClickListener( unused -> dismiss() );
    }

    private void onSubmitClick(){
        binding.submitButton.setClickable(false);
        int rating = (int) binding.starRating.getRating();
        String review = ""; // for future

        ratingDao.putRating(tripUid, preferenceManager.getString(KEY_USERNAME), userToRate.getUsername(), rating, review )
                .addOnSuccessListener( isSuccessful -> {
                    if(isSuccessful)
                    {
                        Toast.makeText(requireContext(),"Rating submitted!",Toast.LENGTH_SHORT).show();
                        notifySubmit();
                    }
                    else{
                        Toast.makeText(requireContext(),"Already rated!",Toast.LENGTH_SHORT).show();
                        notifySubmit();
                    }
                });
    }

    private void notifySubmit(){
        Bundle result = new Bundle();
        result.putBoolean(IS_SUCCESSFUL, true);
        result.putSerializable(KEY_USER,userToRate);
        getParentFragmentManager().setFragmentResult(REQUEST_KEY_RATE_SUBMIT, result);
        dismiss();
    }

    @Override
    public void onDismiss(@NonNull DialogInterface dialog) {
        super.onDismiss(dialog);
    }
}