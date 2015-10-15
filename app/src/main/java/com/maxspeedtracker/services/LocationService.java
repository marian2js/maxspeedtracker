package com.maxspeedtracker.services;

import android.app.Service;
import android.content.Intent;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.os.Messenger;
import android.os.RemoteException;
import android.support.annotation.Nullable;
import android.util.Log;

import com.maxspeedtracker.data.SettingsDAO;
import com.maxspeedtracker.data.TrackerDAO;

import java.util.ArrayList;

public class LocationService extends Service implements LocationListener {
    public static final int MSG_REGISTER_CLIENT = 1;
    public static final int MSG_SPEED_CHANGED = 2;
    private static final String TAG = "LocationService";
    private static long minUpdateTime = 1000 * 5;
    private static float minUpdateDistance = 1;
    private static int mode = 2;
    private LocationManager locationManager;
    private TrackerDAO tracker;
    private ArrayList<Messenger> clients = new ArrayList<>();
    private Messenger messenger = new Messenger(new IncomingHandler());
    private int lastMode;

    public static void setMode(int m) {
        mode = m;
    }

    public static void setMinUpdateTime(long time) {
        minUpdateTime = time * 1000;
    }

    public static void setMinUpdateDistance(float distance) {
        minUpdateDistance = distance;
    }

    @Override
    public void onCreate() {
        SettingsDAO settings = new SettingsDAO(this);
        locationManager = (LocationManager) this.getSystemService(LOCATION_SERVICE);
        tracker = new TrackerDAO(this);
        setMode(settings.getLocationMode());
        setMinUpdateTime(settings.getMinLocationUpdateTime());
        setMinUpdateDistance(settings.getMinLocationUpdateDistance());
        lastMode = mode;
        this.startListeningLocation();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        this.stopListeningLocation();
    }

    private void startListeningLocation() throws SecurityException {
        Log.d(TAG, "Start Listening Location");
        if (mode == 2 || mode == 0) {
            locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, minUpdateTime, minUpdateDistance, this);
        }
        if (mode == 2 || mode == 1) {
            locationManager.requestLocationUpdates(LocationManager.NETWORK_PROVIDER, minUpdateTime, minUpdateDistance, this);
        }
    }

    private void stopListeningLocation() throws SecurityException {
        Log.d(TAG, "Stop Listening Location");
        locationManager.removeUpdates(this);
    }

    /**
     * Send a message to every client subscribed
     */
    private void sendMessage(int key) {
        for (int i = 0; i < clients.size(); i++) {
            try {
                clients.get(i).send(Message.obtain(null, key));
            } catch (RemoteException e) {
                Log.e(TAG, "RemoteException", e);
                clients.remove(i);
            }
        }
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return messenger.getBinder();
    }

    @Override
    public void onLocationChanged(Location location) {
        // If the location mode changes, restart the location manager
        if (lastMode != mode) {
            this.stopListeningLocation();
            this.startListeningLocation();
        }

        Log.d(TAG, "Current Speed " + location.getSpeed() + " m/s");
        tracker.setCurrentSpeed(location.getSpeed());
        sendMessage(MSG_SPEED_CHANGED);
    }

    @Override
    public void onStatusChanged(String provider, int status, Bundle extras) {

    }

    @Override
    public void onProviderEnabled(String provider) {

    }

    @Override
    public void onProviderDisabled(String provider) {

    }

    /**
     * Handle messages from clients
     */
    class IncomingHandler extends Handler {
        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case MSG_REGISTER_CLIENT:
                    clients.add(msg.replyTo);
                    break;
                default:
                    super.handleMessage(msg);
            }
        }
    }
}
