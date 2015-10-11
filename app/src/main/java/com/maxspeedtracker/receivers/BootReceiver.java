package com.maxspeedtracker.receivers;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

import com.maxspeedtracker.data.TrackerDAO;
import com.maxspeedtracker.services.LocationService;

public class BootReceiver extends BroadcastReceiver {

    @Override
    public void onReceive(Context context, Intent intent) {
        TrackerDAO tracker = new TrackerDAO(context);

        // Run service if tracking was active
        if (tracker.getTrackingState() == TrackerDAO.STATE_TRACKING) {
            Intent locationIntent = new Intent(context, LocationService.class);
            context.startService(locationIntent);
        }
    }
}
