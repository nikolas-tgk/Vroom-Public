package com.p17142.vroom.adapters;

import static com.p17142.vroom.utilities.Constants.STATUS_NEW_INVITE;
import static com.p17142.vroom.utilities.Constants.STATUS_OWNED;
import static com.p17142.vroom.utilities.Constants.STATUS_PARTICIPANT;

import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.p17142.vroom.databinding.ItemContainerTripModernBinding;
import com.p17142.vroom.listeners.TripListener;
import com.p17142.vroom.models.Trip;
import com.p17142.vroom.utilities.ImageUtils;

import java.text.SimpleDateFormat;
import java.util.List;

public class TripAdapter extends RecyclerView.Adapter<TripAdapter.TripViewHolder>{
    private final List<Trip> trips;
    private final TripListener tripListener;
    private SimpleDateFormat dateFormat = new SimpleDateFormat("dd MMM"); // format date to e.g. 14 Jan
    private String targetUsername = "";

    public TripAdapter(List<Trip> trips, String targetUsername, TripListener tripListener){
        this.trips = trips;
        this.tripListener = tripListener;
        this.targetUsername = targetUsername;
    }

    @NonNull
    @Override
    public TripViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        return new TripViewHolder(ItemContainerTripModernBinding.inflate(LayoutInflater.from(parent.getContext()),parent,false));
    }

    @Override
    public void onBindViewHolder(@NonNull TripViewHolder holder, int position) {
            holder.setData(trips.get(position));
    }

    @Override
    public int getItemCount() {
        return trips.size();
    }

    class TripViewHolder extends RecyclerView.ViewHolder{
        ItemContainerTripModernBinding binding;
        public TripViewHolder(ItemContainerTripModernBinding itemContainerTripModernBinding) {
            super(itemContainerTripModernBinding.getRoot());
            binding = itemContainerTripModernBinding;
        }

        void setData(Trip trip){
            binding.startLocText.setText(trip.getStartLocation());
            binding.endLocText.setText(trip.getEndLocation());
            binding.dateText.setText(dateFormat.format(trip.getTripDate()));
            binding.imageView.setImageBitmap(ImageUtils.decodeImage(trip.getDriverImageUri()));
            binding.usernameTextView.setText(trip.getDriverUsername());
            String currentUserStatus = trip.getCurrentUserStatus();
            binding.extraText.setVisibility(View.INVISIBLE);

            if(trip.getInProgress() && (trip.isParticipant(targetUsername) || trip.getDriverUsername().equals(targetUsername)))
            {
                binding.infoText.setText("In Progress");
                binding.infoText.setTextColor(Color.parseColor("#ffe234")); // must match star-yellow saved color
                binding.infoText.setVisibility(View.VISIBLE);
            }
            else if(trip.isCompleted())
            {
                binding.infoText.setText("Completed");
                binding.infoText.setTextColor(Color.parseColor("#6ce86c")); // must match completed-green saved color //TO-DO find a better way
                binding.infoText.setVisibility(View.VISIBLE);
                if((trip.isParticipant(targetUsername) || trip.getDriverUsername().equals(targetUsername)) && !trip.hasUserFinishedRating(targetUsername) && !trip.getRiderUsernames().isEmpty())
                {
                    binding.extraText.setText("Rate now");
                    binding.extraText.setVisibility(View.VISIBLE);
                }
            }
            else if(currentUserStatus.equals(STATUS_NEW_INVITE)||currentUserStatus.equals(STATUS_OWNED)||currentUserStatus.equals(STATUS_PARTICIPANT))
            {
                binding.infoText.setText(currentUserStatus);
                binding.infoText.setTextColor(Color.parseColor("#ffe234")); // must match star-yellow saved color
                binding.infoText.setVisibility(View.VISIBLE);
            }
            else{
                binding.infoText.setText("");
                binding.infoText.setVisibility(View.INVISIBLE);
            }
            binding.getRoot().setOnClickListener(v -> tripListener.onTripClicked(trip));
        }
    }
}
