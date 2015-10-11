package com.maxspeedtracker.data;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.SharedPreferences;

import java.util.ArrayList;
import java.util.HashMap;

public class TrackerDAO {
    public static final int STATE_STOPPED = 0;
    public static final int STATE_PAUSED = 1;
    public static final int STATE_TRACKING = 2;

    public static final String SP_KEY = "tracks";
    public static final String TRACKING_STATE_KEY = "tracking_state";
    public static final String CURRENT_TRACK_KEY = "current_track";
    public static final String CURRENT_SPEED_KEY = "current_speed";
    public static final String MAX_SPEED_KEY = "max_speed_";
    private SharedPreferences sp;
    private SharedPreferences.Editor editor;

    @SuppressLint("CommitPrefEdits")
    public TrackerDAO(Context context) {
        sp = context.getSharedPreferences(SP_KEY, Context.MODE_PRIVATE);
        editor = sp.edit();
    }

    public float getCurrentSpeed() {
        return sp.getFloat(CURRENT_SPEED_KEY, 0);
    }

    public void setCurrentSpeed(float speed) {
        this.setCurrentSpeed(speed, true);
    }

    public void setCurrentSpeed(float speed, boolean apply) {
        editor.putFloat(CURRENT_SPEED_KEY, speed);
        this.setMaxSpeed(speed, false);
        if (apply) {
            editor.apply();
        }
    }

    public float getMaxSpeed() {
        int currentTrack = this.getCurrentTrack();
        return this.getMaxSpeed(currentTrack);
    }

    public float getMaxSpeed(int trackId) {
        return sp.getFloat(MAX_SPEED_KEY + trackId, 0);
    }

    private void setMaxSpeed(float currentSpeed, boolean apply) {
        int currentTrack = this.getCurrentTrack();
        float maxSpeed = this.getMaxSpeed(currentTrack);
        if (currentSpeed > maxSpeed) {
            editor.putFloat(MAX_SPEED_KEY + currentTrack, currentSpeed);
        }
        if (apply) {
            editor.apply();
        }
    }

    public int getCurrentTrack() {
        return sp.getInt(CURRENT_TRACK_KEY, 1);
    }

    private void setCurrentTrack(int track, boolean apply) {
        editor.putInt(CURRENT_TRACK_KEY, track);
        if (apply) {
            editor.apply();
        }
    }

    public int getTrackingState() {
        return sp.getInt(TRACKING_STATE_KEY, STATE_STOPPED);
    }

    private void setTrackingState(int state) {
        this.setTrackingState(state, true);
    }

    private void setTrackingState(int state, boolean apply) {
        editor.putInt(TRACKING_STATE_KEY, state);
        if (apply) {
            editor.apply();
        }
    }

    public void onStart() {
        this.setTrackingState(TrackerDAO.STATE_TRACKING);
    }

    public void onPause() {
        this.setCurrentSpeed(0, false);
        this.setTrackingState(TrackerDAO.STATE_PAUSED);
    }

    public void onStop() {
        int currentTrack = this.getCurrentTrack();
        this.setCurrentSpeed(0, false);
        this.setCurrentTrack(++currentTrack, false);
        this.setTrackingState(TrackerDAO.STATE_STOPPED);
    }

    /**
     * Returns a list of tracks stored, including the current one
     */
    public ArrayList<HashMap<String, Object>> getTracks() {
        ArrayList<HashMap<String, Object>> tracks = new ArrayList<>();
        int currentTrack = this.getCurrentTrack();

        // If there is a current track, include it in the list
        if (this.getTrackingState() != STATE_STOPPED) {
            currentTrack++;
        }

        for (int i = 1; i < currentTrack; i++) {
            HashMap<String, Object> track = new HashMap<>();
            float maxSpeed = this.getMaxSpeed(i);
            track.put(CURRENT_TRACK_KEY, i);
            track.put(MAX_SPEED_KEY, maxSpeed);
            tracks.add(track);
        }
        return tracks;
    }

    /**
     * Clears all the tracks except the current one
     */
    public void clearTracks() {
        float currentMaxSpeed = this.getMaxSpeed();
        int trackingState = this.getTrackingState();
        editor.clear();
        editor.apply();

        // Restore the current state and max speed
        if (trackingState != STATE_STOPPED) {
            this.setMaxSpeed(currentMaxSpeed, false);
        }
        this.setTrackingState(trackingState);
    }

}
