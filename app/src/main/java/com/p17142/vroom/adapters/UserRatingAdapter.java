package com.p17142.vroom.adapters;

import android.view.LayoutInflater;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.p17142.vroom.databinding.ItemContainerUserToRateBinding;
import com.p17142.vroom.listeners.UserListener;
import com.p17142.vroom.models.User;
import com.p17142.vroom.utilities.ImageUtils;

import java.util.List;

public class UserRatingAdapter extends RecyclerView.Adapter<UserRatingAdapter.UserRatingsViewHolder>{

    private final List<User> users;
    private final UserListener userListener;

    public UserRatingAdapter(List<User> users, UserListener userListener) {

        this.users = users;
        this.userListener = userListener;
    }

    @NonNull
    @Override
    public UserRatingsViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        ItemContainerUserToRateBinding itemContainerUserToRateBinding = ItemContainerUserToRateBinding.inflate(
                LayoutInflater.from(parent.getContext()),
                parent,false
        );
        return new UserRatingsViewHolder(itemContainerUserToRateBinding);
    }

    @Override
    public void onBindViewHolder(@NonNull UserRatingsViewHolder holder, int position) {
        holder.setData(users.get(position));
    }

    @Override
    public int getItemCount() {
        return users.size();
    }

    class UserRatingsViewHolder extends RecyclerView.ViewHolder {
        ItemContainerUserToRateBinding binding;

        public UserRatingsViewHolder(ItemContainerUserToRateBinding itemContainerUserToRateBinding){
            super(itemContainerUserToRateBinding.getRoot());
            binding = itemContainerUserToRateBinding;
        }

        void setData(User user){
            binding.usernameText.setText(user.getUsername());
            binding.rateText.setText("Not yet rated");
            binding.profileImageView.setImageBitmap(ImageUtils.decodeImage(user.getImageUri()));
            binding.getRoot().setOnClickListener(v -> userListener.onUserClicked(user));
        }
    }

}
