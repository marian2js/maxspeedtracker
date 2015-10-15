package com.maxspeedtracker.activities;

import android.app.ListActivity;
import android.os.Bundle;
import android.support.design.widget.Snackbar;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.TextView;

import com.maxspeedtracker.R;
import com.maxspeedtracker.data.SettingsDAO;
import com.maxspeedtracker.data.TrackerDAO;
import com.maxspeedtracker.interfaces.TrackerListener;
import com.maxspeedtracker.logic.SpeedTracker;

import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.HashMap;

public class HistoryListActivity extends ListActivity implements TrackerListener {
    private static final String TAG = "HistoryListActivity";
    private SpeedTracker speedTracker;
    private SettingsDAO settings;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_list_history);
        speedTracker = new SpeedTracker(this, this);
        settings = new SettingsDAO(this);
        this.createList();
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

    /**
     * Populates the ListView with the history of tracks
     */
    private void createList() {
        TextView noTracksTextView = (TextView) findViewById(R.id.noCurrentTracks);
        Button clearHistory = (Button) findViewById(R.id.clearHistory);
        final ArrayList<HashMap<String, Object>> tracks = speedTracker.getTracks();
        final int currentTrack = speedTracker.getCurrentTrack();
        noTracksTextView.setVisibility(tracks.isEmpty() ? View.VISIBLE : View.INVISIBLE);
        if (tracks.isEmpty() || (tracks.size() == 1 && !speedTracker.isStopped())) {
            clearHistory.setVisibility(View.INVISIBLE);
        } else {
            clearHistory.setVisibility(View.VISIBLE);
        }

        ArrayAdapter<HashMap<String, Object>> adapter = new ArrayAdapter<HashMap<String, Object>>(
                this, android.R.layout.simple_list_item_2, android.R.id.text1, tracks) {
            @Override
            public View getView(int position, View convertView, ViewGroup parent) {
                View view = super.getView(position, convertView, parent);
                TextView textView1 = (TextView) view.findViewById(android.R.id.text1);
                TextView textView2 = (TextView) view.findViewById(android.R.id.text2);

                int trackNumber = (int) tracks.get(position).get(TrackerDAO.CURRENT_TRACK_KEY);
                float maxSpeed = (float) tracks.get(position).get(TrackerDAO.MAX_SPEED_KEY);
                maxSpeed = speedTracker.formatSpeed(maxSpeed);

                String text1 = getResources().getString(R.string.track_number);
                String text2 = getResources().getString(R.string.max_speed);
                text1 = String.format(text1, trackNumber);
                text2 = String.format(text2, new DecimalFormat("#.#").format(maxSpeed));
                text2 += " " + settings.getSpeedUnitsText();

                // If the track is not stopped, show the current state
                if (trackNumber == currentTrack) {
                    if (speedTracker.isTracking()) {
                        text1 += " (" + getResources().getString(R.string.status_tracking) + ")";
                    } else if (speedTracker.isPaused()) {
                        text1 += " (" + getResources().getString(R.string.status_paused) + ")";
                    }
                }

                textView1.setText(text1);
                textView2.setText(text2);
                return view;
            }
        };

        setListAdapter(adapter);
    }

    public void onClearHistoryClicked(View view) {
        speedTracker.clearTracks();
        Snackbar.make(view, getResources().getString(R.string.history_cleared), Snackbar.LENGTH_LONG).show();
        Log.d(TAG, "History cleared");
        this.createList();
    }

    @Override
    public void onTrackerDataUpdated() {
        this.createList();
    }
}
