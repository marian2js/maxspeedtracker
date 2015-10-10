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
import android.widget.Toast;

import com.maxspeedtracker.data.TrackerDAO;

import java.util.ArrayList;

public class LocationService extends Service implements LocationListener {
    private LocationManager locationManager;
    private TrackerDAO tracker;
    private ArrayList<Messenger> clients = new ArrayList<>();
    private Messenger messenger = new Messenger(new IncomingHandler());
    public static final int MSG_REGISTER_CLIENT = 1;
    public static final int MSG_SPEED_CHANGED = 2;
    private static final long MIN_UPDATE_TIME = 1000 * 5;
    private static final float MIN_UPDATE_DISTANCE = 1;
    private static final String TAG = "LocationService";

    @Override
    public void onCreate() {
        locationManager = (LocationManager) this.getSystemService(LOCATION_SERVICE);
        tracker = new TrackerDAO(this);
        this.startListeningLocation();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        this.stopListeningLocation();
    }

    private void startListeningLocation() throws SecurityException {
        Log.d(TAG, "Start Listening Location");
        locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, MIN_UPDATE_TIME, MIN_UPDATE_DISTANCE, this);
        locationManager.requestLocationUpdates(LocationManager.NETWORK_PROVIDER, MIN_UPDATE_TIME, MIN_UPDATE_DISTANCE, this);
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

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return messenger.getBinder();
    }

    @Override
    public void onLocationChanged(Location location) {
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
}
