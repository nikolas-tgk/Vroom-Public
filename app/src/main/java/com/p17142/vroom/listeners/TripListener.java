package com.p17142.vroom.listeners;

import com.p17142.vroom.models.Trip;

@FunctionalInterface
public interface TripListener {
    void onTripClicked(Trip trip);
}
