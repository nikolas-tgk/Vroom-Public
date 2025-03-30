package com.p17142.vroom.ui.application.ratings;

import static com.p17142.vroom.utilities.Constants.IS_SUCCESSFUL;
import static com.p17142.vroom.utilities.Constants.KEY_TRIP_UID;
import static com.p17142.vroom.utilities.Constants.KEY_USER;
import static com.p17142.vroom.utilities.Constants.KEY_USERNAME;
import static com.p17142.vroom.utilities.Constants.KEY_USERS;
import static com.p17142.vroom.utilities.Constants.REQUEST_KEY_RATE_SUBMIT;
import static com.p17142.vroom.utilities.Constants.REQUEST_KEY_RATING_COMPLETE;

import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.DialogFragment;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import com.p17142.vroom.R;
import com.p17142.vroom.adapters.UserRatingAdapter;
import com.p17142.vroom.data.dao.RatingDao;
import com.p17142.vroom.data.dao.UserDao;
import com.p17142.vroom.databinding.FragmentRatingUserListBinding;
import com.p17142.vroom.listeners.UserListener;
import com.p17142.vroom.models.User;
import com.p17142.vroom.utilities.Logger;
import com.p17142.vroom.utilities.PreferenceManager;

import java.util.ArrayList;
import java.util.List;


public class RatingUserListDialogFragment extends DialogFragment implements UserListener {

    private FragmentRatingUserListBinding binding;

    private boolean validBundle = false;

    private List<String> usernamesToRate = new ArrayList<>();
    private List<User> usersToRate = new ArrayList<>();
    private String tripUid = "";

    private UserDao userDao;
    private RatingDao ratingDao = RatingDao.getInstance();

    private UserRatingAdapter ratingsAdapter;
    private PreferenceManager preferenceManager;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setStyle(STYLE_NORMAL, R.style.AppTheme_DialogOverlay);
        Bundle bundle = getArguments();
        if (bundle != null) {
            List<String> usersToRate = bundle.getStringArrayList(KEY_USERS);
            String tripUid = bundle.getString(KEY_TRIP_UID);
            if (usersToRate != null && !usersToRate.isEmpty()) {
                this.usernamesToRate.addAll(usersToRate);
                this.tripUid = tripUid;
                validBundle = true;
            }
        }
        else{
            validBundle = false;
            Logger.printLogError(RatingUserListDialogFragment.class,"Invalid bundle.");
        }
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        binding = FragmentRatingUserListBinding.inflate(inflater, container, false);
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
            Logger.printLogFatal(RatingUserListDialogFragment.class,"Invalid bundle, force back.");
            this.dismiss();
        }
        preferenceManager = new PreferenceManager(requireContext());
        userDao = UserDao.getInstance();

        userDao.getUsersByUsernameList(usernamesToRate)
                .addOnSuccessListener( usersToRate -> {
                    if(!usersToRate.isEmpty())
                    {
                        this.usersToRate.addAll(usersToRate);
                        ratingsAdapter = new UserRatingAdapter(this.usersToRate,this);
                        binding.ratingsRecyclerView.setAdapter(ratingsAdapter);
                        loading(false);
                    }
                    else{
                        // error no users left to rate - this shouldn't happen
                        Toast.makeText(requireContext(),"No users left to rate.",Toast.LENGTH_SHORT).show();
                        dismiss();
                    }
                });
    }

    private void setListeners(){
        binding.closeImage.setOnClickListener( unused -> {
            this.dismiss();
        });
    }

    @Override
    public void onUserClicked(User user) {
        Bundle bundle = new Bundle();
        bundle.putSerializable(KEY_USER, user);
        bundle.putString(KEY_TRIP_UID,tripUid);

        SetRatingDialogFragment setRatingDialogFragment = new SetRatingDialogFragment();
        setRatingDialogFragment.setArguments(bundle);

        getParentFragmentManager().setFragmentResultListener(REQUEST_KEY_RATE_SUBMIT, this, (requestKey, result) -> {
            if(result.getBoolean(IS_SUCCESSFUL))
            {
                User userRated = (User) result.getSerializable(KEY_USER);
                int position = this.usersToRate.indexOf(userRated);
                if (position != -1) {
                    Logger.printLog(RatingUserListDialogFragment.class,"Removed item from recyclerview.");
                    usersToRate.remove(position);
                    ratingsAdapter.notifyItemRemoved(position);
                    ratingsAdapter.notifyItemRangeChanged(position, usersToRate.size());
                    if(usersToRate.isEmpty())
                    {
                        ratingDao.markRatingComplete(tripUid,preferenceManager.getString(KEY_USERNAME))
                                .addOnSuccessListener( isSuccessful -> {
                                    if(isSuccessful)
                                    {
                                        Bundle result2 = new Bundle();
                                        result2.putBoolean(IS_SUCCESSFUL, true);
                                        getParentFragmentManager().setFragmentResult(REQUEST_KEY_RATING_COMPLETE, result2);
                                        dismiss(); // close dialog if there are no users left to rate
                                    }
                                    else{
                                        Toast.makeText(requireContext(), "Unexpected error.", Toast.LENGTH_SHORT).show();
                                        dismiss();
                                    }
                                });
                    }
                }
            }
        });
        setRatingDialogFragment.show(getActivity().getSupportFragmentManager(), null);
    }

    private void loading(Boolean isLoading){
        if(isLoading)
        {
            binding.ratingsRecyclerView.setVisibility(View.INVISIBLE);
            binding.loadingBar.setVisibility(View.VISIBLE);
        }
        else{
            binding.loadingBar.setVisibility(View.INVISIBLE);
            binding.ratingsRecyclerView.setVisibility(View.VISIBLE);
        }
    }
}