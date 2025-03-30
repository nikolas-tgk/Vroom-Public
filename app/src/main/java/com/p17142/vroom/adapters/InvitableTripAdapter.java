package com.p17142.vroom.adapters;

import android.view.LayoutInflater;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.p17142.vroom.databinding.ItemContainerInvitableTripBinding;
import com.p17142.vroom.listeners.TripListener;
import com.p17142.vroom.models.Trip;
import com.p17142.vroom.utilities.ImageUtils;

import java.text.SimpleDateFormat;
import java.util.List;

public class InvitableTripAdapter extends RecyclerView.Adapter<InvitableTripAdapter.InvitableTripViewHolder> {
    private final List<Trip> trips;
    private final TripListener tripListener;
    private SimpleDateFormat dateFormat = new SimpleDateFormat("dd MMM"); // format date to e.g. 14 Jan

    public InvitableTripAdapter(List<Trip> trips, TripListener tripListener) {
        this.trips = trips;
        this.tripListener = tripListener;
    }

    @NonNull
    @Override
    public InvitableTripViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        return new InvitableTripAdapter.InvitableTripViewHolder(ItemContainerInvitableTripBinding.inflate(LayoutInflater.from(parent.getContext()),parent,false));
    }

    @Override
    public void onBindViewHolder(@NonNull InvitableTripViewHolder holder, int position) {
        holder.setData(trips.get(position));
    }

    @Override
    public int getItemCount() {
        return trips.size();
    }

    class InvitableTripViewHolder extends RecyclerView.ViewHolder{
        ItemContainerInvitableTripBinding binding;
        public InvitableTripViewHolder(ItemContainerInvitableTripBinding itemContainerInvitableTripBinding) {
            super(itemContainerInvitableTripBinding.getRoot());
            binding = itemContainerInvitableTripBinding;
        }

        void setData(Trip trip){
            binding.startLocText.setText(trip.getStartLocation());
            binding.endLocText.setText(trip.getEndLocation());
            binding.dateText.setText(dateFormat.format(trip.getTripDate()));
            String tripStartTime = trip.getStartTime();
            if(tripStartTime.charAt(0)=='0' || tripStartTime.startsWith("10") || tripStartTime.startsWith("11") || tripStartTime.startsWith("12"))
            {
                tripStartTime+=" AM";
            }
            else{
                tripStartTime+=" PM";
            }
            binding.timeText.setText(tripStartTime);
            String numOfRiders = "0";
            if(trip.getRiderUsernames() != null)
            {
                numOfRiders = String.valueOf(trip.getRiderUsernames().size()); // check for null error
            }
            binding.ridersNumtext.setText(String.format("%s/%d", numOfRiders, trip.getMaxNumOfRiders()));
            binding.imageView.setImageBitmap(ImageUtils.decodeImage(trip.getDriverImageUri()));
            //binding.usernameTextView.setText(trip.getDriverUsername());
            binding.usernameTextView.setText("You");
            binding.getRoot().setOnClickListener(v -> tripListener.onTripClicked(trip));
        }
    }
}
