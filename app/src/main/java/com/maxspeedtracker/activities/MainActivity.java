package com.maxspeedtracker.activities;

import android.content.pm.PackageManager;
import android.os.Bundle;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.util.TypedValue;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.maxspeedtracker.R;
import com.maxspeedtracker.interfaces.TrackerListener;
import com.maxspeedtracker.logic.SpeedTracker;

import java.text.DecimalFormat;

public class MainActivity extends AppCompatActivity implements TrackerListener {
    private SpeedTracker speedTracker;
    private Snackbar snackbar = null;
    private static final String TAG = "MainActivity";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        speedTracker = new SpeedTracker(this, this);
        updateStateUI();
    }

    @Override
    protected void onPause() {
        speedTracker.unbindService();
        super.onPause();
    }

    @Override
    protected void onDestroy() {
        speedTracker.unbindService();
        super.onDestroy();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            // TODO
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    /**
     * Toggle the tracking state between play and pause
     */
    public void toggleTrackingState() {
        FloatingActionButton fab = (FloatingActionButton) findViewById(R.id.fab);

        // dismiss any previous snackbar
        if (snackbar != null) {
            snackbar.dismiss();
        }

        if (speedTracker.isTracking()) {
            speedTracker.pauseTracking();
        } else {
            speedTracker.startTracking();

            // show message
            snackbar = Snackbar.make(fab, "Tracking your speed", Snackbar.LENGTH_LONG);
            snackbar.setAction("Action", null).show();
        }
        updateStateUI();
    }

    /**
     * Updates the Tracking State in the UI
     */
    public void updateStateUI() {
        RelativeLayout speedLayout = (RelativeLayout) findViewById(R.id.speedLayout);
        FloatingActionButton fab = (FloatingActionButton) findViewById(R.id.fab);
        FloatingActionButton fabStop = (FloatingActionButton) findViewById(R.id.fab_stop);
        TextView trackNumberText = (TextView) findViewById(R.id.trackNumber);
        TextView maxSpeedText = (TextView) findViewById(R.id.maxSpeed);
        String trackNumberStr;

        if (speedTracker.isTracking()) {
            fab.setImageResource(android.R.drawable.ic_media_pause);
            fabStop.setVisibility(View.VISIBLE);
            speedLayout.setVisibility(View.VISIBLE);
        } else {
            fab.setImageResource(android.R.drawable.ic_media_play);
            fabStop.setVisibility(View.INVISIBLE);
            speedLayout.setVisibility(View.INVISIBLE);
        }

        if (speedTracker.isStopped()) {
            trackNumberStr = getResources().getString(R.string.no_current_tracks);
            maxSpeedText.setVisibility(View.INVISIBLE);
        } else {
            trackNumberStr = getResources().getString(R.string.track_number);
            trackNumberStr = String.format(trackNumberStr, speedTracker.getCurrentTrack());
            maxSpeedText.setVisibility(View.VISIBLE);
        }
        trackNumberText.setText(trackNumberStr);
        this.updateDataUI();
    }

    /**
     * Updates the data values in the UI
     */
    public void updateDataUI() {
        TextView currentSpeedText = (TextView) findViewById(R.id.currentSpeed);
        TextView maxSpeedText = (TextView) findViewById(R.id.maxSpeed);
        float currentSpeed = speedTracker.getCurrentSpeed();
        float maxSpeed = speedTracker.getMaxSpeed();

        // set current speed
        String currentSpeedStr = new DecimalFormat("#.#").format(currentSpeed);
        int textSizeInPixels;
        currentSpeedText.setText(currentSpeedStr);
        if (currentSpeedStr.length() < 4) {
            textSizeInPixels = getResources().getDimensionPixelSize(R.dimen.current_speed_large);
        } else if (currentSpeedStr.length() == 4) {
            textSizeInPixels = getResources().getDimensionPixelSize(R.dimen.current_speed_medium);
        } else {
            textSizeInPixels = getResources().getDimensionPixelSize(R.dimen.current_speed_small);
        }
        currentSpeedText.setTextSize(TypedValue.COMPLEX_UNIT_PX, textSizeInPixels);

        // set max speed
        if (maxSpeed > 0) {
            String strMaxSpeed = getResources().getString(R.string.max_speed);
            strMaxSpeed = String.format(strMaxSpeed, new DecimalFormat("#.#").format(maxSpeed));
            strMaxSpeed += " " + getResources().getString(R.string.kph);
            maxSpeedText.setText(strMaxSpeed);
            maxSpeedText.setVisibility(View.VISIBLE);
        } else {
            maxSpeedText.setVisibility(View.INVISIBLE);
        }
    }

    /**
     * Called when the main fab is clicked (for play or pause)
     */
    public void onFabClicked(View view) {
        // Check if the user granted the required permissions
        if (!speedTracker.permissionsGranted()) {
            speedTracker.requestPermissions();
            return;
        }

        this.toggleTrackingState();
    }

    /**
     * Called when the pause fab is clicked
     */
    public void onStopClicked(View view) {
        // dismiss any previous snackbar
        if (snackbar != null) {
            snackbar.dismiss();
        }
        
        speedTracker.stopTracking();
        this.updateStateUI();
    }

    public void onShowHistoryClicked(View view) {
        // TODO
        Toast.makeText(this, "Not Implemented Yet", Toast.LENGTH_LONG).show();
    }

    @Override
    public void onTrackerDataUpdated() {
        this.updateDataUI();
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        switch (requestCode) {
            case SpeedTracker.PERMISSIONS_REQUEST_CODE: {
                if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    Log.d(TAG, "Permissions Granted");
                    toggleTrackingState();
                } else {
                    Log.e(TAG, "Permissions Denied");
                }
            }
        }
    }
}
