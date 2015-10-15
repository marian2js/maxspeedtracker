package com.maxspeedtracker.data;

import android.content.Context;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;

import com.maxspeedtracker.R;

public class SettingsDAO {
    private Context context;
    private SharedPreferences sp;

    public SettingsDAO(Context context) {
        this.context = context;
        this.sp = PreferenceManager.getDefaultSharedPreferences(context);
    }

    public String getSpeedUnits() {
        return sp.getString("speed_units", context.getString(R.string.speed_units_default));
    }

    public String getSpeedUnitsText() {
        String key = this.getSpeedUnits();
        return this.getStringByKey(key);
    }

    public boolean isHistoryEnabled() {
        return sp.getBoolean("enabled_history", true);
    }

    public int getLocationMode() {
        String defaultMode = context.getString(R.string.pref_mode_default);
        String mode = sp.getString("location_mode", defaultMode);
        return Integer.parseInt(mode);
    }

    public long getMinLocationUpdateTime() {
        String defaultTime = context.getString(R.string.pref_location_time_default);
        String time = sp.getString("location_time", defaultTime);
        return Long.parseLong(time);
    }

    public float getMinLocationUpdateDistance() {
        String defaultDistance = context.getString(R.string.pref_location_distance_default);
        String distance = sp.getString("location_distance", defaultDistance);
        return Float.parseFloat(distance);
    }

    private String getStringByKey(String key) {
        int res = context.getResources().getIdentifier(key, "string", context.getPackageName());
        return context.getString(res);
    }
}
