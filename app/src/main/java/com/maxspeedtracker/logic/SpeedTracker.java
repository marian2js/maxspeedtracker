package com.maxspeedtracker.logic;

import android.Manifest;
import android.app.Activity;
import android.app.ActivityManager;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.os.Messenger;
import android.os.RemoteException;
import android.support.v4.app.ActivityCompat;
import android.util.Log;

import com.maxspeedtracker.data.SettingsDAO;
import com.maxspeedtracker.data.TrackerDAO;
import com.maxspeedtracker.interfaces.TrackerListener;
import com.maxspeedtracker.services.LocationService;

import java.util.ArrayList;
import java.util.HashMap;

public class SpeedTracker {
    public static final int PERMISSIONS_REQUEST_CODE = 100;
    private static final String[] PERMISSIONS_REQUEST = {
            Manifest.permission.ACCESS_FINE_LOCATION,
            Manifest.permission.ACCESS_COARSE_LOCATION
    };
    private static final String TAG = "SpeedTracker";
    private Activity activity;
    private TrackerListener trackerListener;
    private Intent locationIntent = null;
    private TrackerDAO tracker;
    private SettingsDAO settings;
    private Messenger messenger = new Messenger(new IncomingHandler());
    private boolean serviceBound = false;
    private ServiceConnection serviceConnection = new ServiceConnection() {

        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            Messenger locationMessenger = new Messenger(service);
            try {
                Message msg = Message.obtain(null, LocationService.MSG_REGISTER_CLIENT);
                msg.replyTo = messenger;
                locationMessenger.send(msg);

            } catch (RemoteException e) {
                Log.e(TAG, "RemoteException", e);
            }
        }

        @Override
        public void onServiceDisconnected(ComponentName name) {

        }
    };

    public SpeedTracker(Activity activity, TrackerListener trackerListener) {
        this(activity, trackerListener, false);
    }

    /**
     * @param activity        The target activity.
     * @param trackerListener Listener to be notified on data changes.
     * @param restoreTracking If true, attempts to restore the service.
     */
    public SpeedTracker(Activity activity, TrackerListener trackerListener, boolean restoreTracking) {
        this.activity = activity;
        this.trackerListener = trackerListener;
        tracker = new TrackerDAO(activity);
        this.settings = new SettingsDAO(activity);
        if (restoreTracking) {
            this.restoreTracker();
        } else if (this.isTracking()) {
            this.bindService();
        }
    }

    /**
     * Starts tracking if service should be running, but it isn't.
     * Binds the service if the service is running but not bound.
     */
    private void restoreTracker() {
        if (this.isTracking()) {
            if (!isLocationServiceRunning()) {
                this.startTracking();
            } else if (!serviceBound) {
                this.bindService();
            }
        }
    }

    /**
     * Starts the speed tracking
     */
    public void startTracking() {
        Log.d(TAG, "Start Speed Track");
        tracker.onStart();
        this.bindService();
        activity.startService(locationIntent);
    }

    /**
     * Pauses the speed tracking
     */
    public void pauseTracking() {
        Log.d(TAG, "Pause Speed Track");
        tracker.onPause();
        this.stopService();
    }

    /**
     * Stops the speed tracking
     */
    public void stopTracking() {
        Log.d(TAG, "Stop Speed Track");
        tracker.onStop();
        this.stopService();
    }

    public boolean isTracking() {
        return tracker.getTrackingState() == TrackerDAO.STATE_TRACKING;
    }

    public boolean isPaused() {
        return tracker.getTrackingState() == TrackerDAO.STATE_PAUSED;
    }

    public boolean isStopped() {
        return tracker.getTrackingState() == TrackerDAO.STATE_STOPPED;
    }

    public float getCurrentSpeed() {
        return this.formatSpeed(tracker.getCurrentSpeed());
    }

    public float getMaxSpeed() {
        return this.formatSpeed(tracker.getMaxSpeed());
    }

    public int getCurrentTrack() {
        return tracker.getCurrentTrack();
    }

    public ArrayList<HashMap<String, Object>> getTracks() {
        return tracker.getTracks();
    }

    public void clearTracks() {
        tracker.clearTracks();
    }

    /**
     * Binds the main activity from the service
     */
    private void bindService() {
        if (locationIntent == null) {
            locationIntent = new Intent(activity, LocationService.class);
        }
        activity.bindService(locationIntent, serviceConnection, Context.BIND_AUTO_CREATE);
        serviceBound = true;
    }

    /**
     * Unbinds the main activity from the service
     */
    public void unbindService() {
        try {
            activity.unbindService(serviceConnection);
            serviceBound = false;
        } catch (Exception e) {
        }
    }

    /**
     * Stops the service gracefully
     */
    public void stopService() {
        if (locationIntent == null) {
            locationIntent = new Intent(activity, LocationService.class);
        }
        activity.stopService(locationIntent);
        this.unbindService();
    }

    /**
     * Called when the current speed is updated
     */
    private void onSpeedChanged() {
        trackerListener.onTrackerDataUpdated();
    }

    /**
     * Format a speed represented in the corresponding unit
     */
    public float formatSpeed(float speed) {
        double multiplier;
        switch (settings.getSpeedUnits()) {
            case "kph":
                multiplier = 3.6;
                break;
            case "mph":
                multiplier = 2.23;
                break;
            default:
                multiplier = 1;
        }
        return (float) (speed * multiplier);
    }

    /**
     * Checks if the necessary permissions have been granted
     * On Android 6+ the users need to grant permissions on run time
     * On older versions permissions have been granted before the installation
     */
    public boolean permissionsGranted() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            boolean grantedFineLocation = activity.checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED;
            boolean grantedCoarseLocation = activity.checkSelfPermission(Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED;
            return grantedFineLocation && grantedCoarseLocation;
        }
        return true;
    }

    /**
     * Request location permissions
     * Only for API Level >= 23
     */
    public void requestPermissions() {
        Log.d(TAG, "Request Permissions");
        ActivityCompat.requestPermissions(activity, PERMISSIONS_REQUEST, PERMISSIONS_REQUEST_CODE);
    }

    /**
     * Checks if Location Service is running
     *
     * @link http://stackoverflow.com/a/5921190/2246938
     */
    private boolean isLocationServiceRunning() {
        ActivityManager manager = (ActivityManager) activity.getSystemService(Context.ACTIVITY_SERVICE);
        for (ActivityManager.RunningServiceInfo service : manager.getRunningServices(Integer.MAX_VALUE)) {
            if (LocationService.class.getName().equals(service.service.getClassName())) {
                return true;
            }
        }
        return false;
    }

    /**
     * Handle incoming messages from the Location Service
     */
    class IncomingHandler extends Handler {
        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case LocationService.MSG_SPEED_CHANGED:
                    onSpeedChanged();
                    break;
                default:
                    super.handleMessage(msg);
            }
        }
    }
}
