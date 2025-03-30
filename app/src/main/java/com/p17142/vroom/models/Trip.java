package com.p17142.vroom.models;

import static com.p17142.vroom.utilities.Constants.STATUS_NEW_INVITE;
import static com.p17142.vroom.utilities.Constants.STATUS_NONE;
import static com.p17142.vroom.utilities.Constants.STATUS_OWNED;
import static com.p17142.vroom.utilities.Constants.STATUS_PARTICIPANT;

import android.annotation.SuppressLint;

import com.p17142.vroom.utilities.Logger;

import java.io.Serializable;
import java.text.SimpleDateFormat;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

public class Trip implements Serializable {
    private String tripUid, startLocation, endLocation, startTime, driverUsername = "";
    private String driverImageUri;
    private String currentUserStatus = STATUS_NONE;
    private int maxNumOfRiders = 1;
    private List<String> riderUsernames = new ArrayList<>();
    private List<String> invitedUsernames = new ArrayList<>();
    private List<String> ratingCompletedByUsernames = new ArrayList<>();
    private Date tripDate, dateCreated;
    private Boolean isCompleted = null;
    private Boolean isInProgress = false;

    public String getCurrentUserStatus(){
        return currentUserStatus;
    }

    public void determineSetProgress() throws Exception {
        if( isCompleted == null )
        {
            throw new Exception("First set isCompleted from database.");
        }
        if(isCompleted)
        {
            isInProgress = false;
        }
        else if(!isCompleted)
        {
            if(hasTripStartTimeDateElapsed())
            {
                isInProgress = true;
            }
            else{
                isInProgress = false;
            }
        }
    }

    public boolean isParticipant(String usernameToCheck){
        if(riderUsernames.isEmpty())
        {
            return false;
        }
        for(String participant : riderUsernames)
            if(participant.equals(usernameToCheck))
            {
                return true;
            }
        return false;
    }

    public void setRatingCompletedByUsernames(List<String> ratingCompletedByUsernames) {
        this.ratingCompletedByUsernames = ratingCompletedByUsernames;
    }

    public void determineSetCurrentUserStatus(String currentUsername)
    {
        if(currentUsername.equals(driverUsername))
        {
            currentUserStatus = STATUS_OWNED;
            return;
        }
        for (String username: invitedUsernames
             ) {
            if(currentUsername.equals(username))
            {
                currentUserStatus = STATUS_NEW_INVITE; // user has been invited to this trip
                return;
            }
        }
        for (String username: riderUsernames)
        {
            if(currentUsername.equals(username))
            {
                currentUserStatus = STATUS_PARTICIPANT; // user has accepted the invite and is a trip participant ( a trip rider )
                return;
            }
        }
        currentUserStatus = STATUS_NONE; // only executes if all above fail
    }

    public boolean hasUserFinishedRating(String raterUsername){
        return ratingCompletedByUsernames.contains(raterUsername);
    }

    public String getTripUid() {
        return tripUid;
    }

    public void setTripUid(String tripUid) {
        this.tripUid = tripUid;
    }

    public int getMaxNumOfRiders() {
        return maxNumOfRiders;
    }

    public void setMaxNumOfRiders(int maxNumOfRiders) {
        this.maxNumOfRiders = maxNumOfRiders;
    }

    public String getStartLocation() {
        return startLocation;
    }

    public void setStartLocation(String startLocation) {
        this.startLocation = startLocation;
    }

    public String getEndLocation() {
        return endLocation;
    }

    public void setEndLocation(String endLocation) {
        this.endLocation = endLocation;
    }

    public String getStartTime() {
        return startTime;
    }

    public void setStartTime(String startTime) {
        this.startTime = startTime;
    }

    public Date getTripDate() {
        return tripDate;
    }

    public void setTripDate(Date tripDate) {
        this.tripDate = tripDate;
    }

    /*public Date getDateCreated() {
        return dateCreated;
    }*/

    public void setDateCreated(Date dateCreated) {
        this.dateCreated = dateCreated;
    }

    public String getDriverUsername() {
        return driverUsername;
    }

    public void setDriverUsername(String driverUsername) {
        this.driverUsername = driverUsername;
    }

    public List<String> getRiderUsernames() {
        return riderUsernames;
    }

    public Boolean isCompleted() {
        return isCompleted;
    }

    public void setCompleted(Boolean completed) {
        isCompleted = completed;
    }

    public Boolean getInProgress() {
        return isInProgress;
    }

    public List<String> getAllParticipantUsernames(){
        List<String> participantUsernames = new ArrayList<>();
        participantUsernames.addAll(riderUsernames);
        participantUsernames.add(driverUsername);
        return participantUsernames;
    }

    public boolean areMaxRiders()
    {
        if(riderUsernames.size() == maxNumOfRiders)
        {
            return true;
        } else if (riderUsernames.size() > maxNumOfRiders) {
            Logger.printLogFatal(Trip.class,"FATAL ERROR on areMaxRiders(), trip riders are more than max.");
            return true;
        }
        return false;
    }

    public void addRider(String username){
        if(riderUsernames.size() != maxNumOfRiders)
        {
            riderUsernames.add(username); // no checks here
        }
    }

    public void setRiderUsernames(List<String> riderUsernames) {
        this.riderUsernames = riderUsernames;
    }

    public void setInvitedUsernames(List<String> invitedUsernames) {
        this.invitedUsernames = invitedUsernames;
    }

    public void removeInvite(String username){
        for (String invitedUsername: invitedUsernames
             ) {
            if(username.equals(invitedUsername))
            {
                invitedUsernames.remove(username);
                return;
            }
        }
        Logger.printLogError(Trip.class," Tried to remove an invite that doesn't exist.");
    }

    public void removeRider(String username){
        for (String riderUsername: riderUsernames)
        {
            if(username.equals(riderUsername))
            {
                riderUsernames.remove(username);
                return;
            }
        }
        Logger.printLogError(Trip.class," Tried to remove a rider that does not participate in trip.");
    }

    public String getDriverImageUri() {
        return driverImageUri;
    }

    public void setDriverImageUri(String driverImageUri) {
        this.driverImageUri = driverImageUri;
    }

    public boolean isRider(String username){
        for (String riderUsername : riderUsernames) {
            if(username.equals(riderUsername))
            {
                return true;
            }
        }
        return  false;
    }

    public boolean isInvited(String username){
        for (String invitedUsername: invitedUsernames) {
            if(username.equals(invitedUsername))
            {
                return true;
            }
        }
        return false;
    }

    public String getMaxNumOfRidersToString(){
        return String.valueOf(maxNumOfRiders);
    }

    public String getCurrentNumOfRidersToString(){
        return String.valueOf(riderUsernames.size());
    }

    public String getStartTimeDisplayValue(){
        if(startTime.charAt(0)=='0' || startTime.startsWith("10") || startTime.startsWith("11") || startTime.startsWith("12"))
        {
            return startTime+=" AM";
        }
        return startTime;
    }

    public String getTripDateDisplayValueddMMM(){
        @SuppressLint("SimpleDateFormat") SimpleDateFormat dateFormat = new SimpleDateFormat("dd MMM");
        return dateFormat.format(tripDate);
    }

    public String getRidersToMaxRidersDisplayValue(){
        return  String.format("%s/%s", getCurrentNumOfRidersToString(), getMaxNumOfRidersToString());
    }

    public boolean hasTripStartTimeDateElapsed() {
        try {
            ZoneId zoneId = ZoneId.systemDefault(); // might be error prone if abused
            LocalDate tripLocalDate = LocalDate.from(tripDate.toInstant().atZone(zoneId));
            LocalDate nowLocalDate = LocalDate.from(new Date().toInstant().atZone(zoneId));
            if (tripLocalDate.isBefore(nowLocalDate)) {
                return true;
            } else if (tripLocalDate.isAfter(nowLocalDate)) {
                return false;
            } else {
                // same day, determine using time
                Instant nowInstant = Instant.now();
                int nowHour = nowInstant.atZone(zoneId).getHour();
                int nowMinute = nowInstant.atZone(zoneId).getMinute();
                String[] splitTime = this.startTime.split(":"); // if time format changes, this will crash with null
                int tripHour = Integer.parseInt(splitTime[0]);
                int tripMinute = Integer.parseInt(splitTime[1]);
                if (tripHour < nowHour) {
                    return true;
                } else if (tripHour > nowHour) {
                    return false;
                } else { // same hour, go to minutes
                    if (tripMinute > nowMinute) {
                        // trip is almost starting but has not
                        return false;
                    } else if (tripMinute < nowMinute) {
                        // trip just started a few minutes ago
                        return true;
                    } else {
                        // trip started exactly now
                        return true;
                    }
                }
            }
        } catch (Exception e) {
            Logger.printLogFatal(Trip.class,"Trip hasTripStartTimeDateElapsed unexpected error, did time format change? Error: "+e);
            // return true on error
            return  true;
        }
    }

    public boolean isBetweenEpochDates(long startEpoch, long endEpoch){
        // does not take account of time stored in epochs!

        Date tripDateToCompare = tripDate;

        LocalDate localDateTripDate = tripDateToCompare.toInstant()
                .atZone(ZoneId.systemDefault())
                .toLocalDate();

        // convert epoch millis to LocalDate (ignoring time)
        LocalDate startDate = Instant.ofEpochMilli(startEpoch)
                .atZone(ZoneId.systemDefault())
                .toLocalDate();
        LocalDate endDate = Instant.ofEpochMilli(endEpoch)
                .atZone(ZoneId.systemDefault())
                .toLocalDate();

        if(localDateTripDate.isBefore(endDate) && localDateTripDate.isAfter(startDate) || localDateTripDate.isEqual(startDate) || localDateTripDate.isEqual(endDate) )
        {
            return true;
        }
        return false;
    }
}
