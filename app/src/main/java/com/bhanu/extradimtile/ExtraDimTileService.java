package com.bhanu.extradimtile;

import android.content.Context;
import android.content.SharedPreferences;
import android.database.ContentObserver;
import android.net.Uri;
import android.os.Handler;
import android.os.Looper;
import android.provider.Settings;
import android.service.quicksettings.Tile;
import android.service.quicksettings.TileService;
import android.util.Log;
import android.widget.Toast;

import java.lang.reflect.Method;

public class ExtraDimTileService extends TileService {

    private static final String TAG = "ExtraDimTile";
    private static final String SETTING_KEY = "reduce_bright_colors_activated";
    private static final String PREF_NAME = "extra_dim_prefs";
    private static final String PREF_KEY_STATE = "last_state";

    private ContentObserver mObserver;

    @Override
    public void onStartListening() {
        super.onStartListening();
        registerObserver();
        updateTileState();
    }

    @Override
    public void onStopListening() {
        super.onStopListening();
        unregisterObserver();
    }

    @Override
    public void onClick() {
        super.onClick();

        int currentState = getCurrentState();
        int newState = (currentState == 1) ? 0 : 1;

        // Save cached state BEFORE writing setting so any immediate ContentObserver callback sees the new state
        saveCachedState(newState);

        boolean success = false;

        // 1. Try ColorDisplayManager via reflection (SystemApi for Android 12+)
        try {
            Object cdm = getSystemService("color_display");
            if (cdm != null) {
                Method setMethod = cdm.getClass().getMethod("setReduceBrightColorsActivated", boolean.class);
                setMethod.invoke(cdm, newState == 1);
                Log.d(TAG, "ColorDisplayManager.setReduceBrightColorsActivated succeeded: " + (newState == 1));
                success = true;
            }
        } catch (Throwable t) {
            Log.d(TAG, "ColorDisplayManager set failed, falling back to Secure Settings: " + t.getMessage());
        }

        // 2. Direct Secure Settings putInt
        try {
            Settings.Secure.putInt(getContentResolver(), SETTING_KEY, newState);
            Log.d(TAG, "Settings.Secure.putInt succeeded with: " + newState);
            success = true;
        } catch (SecurityException e) {
            Log.e(TAG, "Missing WRITE_SECURE_SETTINGS permission", e);
            if (!success) {
                Toast.makeText(this, "Grant WRITE_SECURE_SETTINGS via ADB/su first!", Toast.LENGTH_LONG).show();
            }
        } catch (Exception e) {
            Log.e(TAG, "Error writing secure setting", e);
        }

        updateTileState();
    }

    private int getCurrentState() {
        // 1. Try ColorDisplayManager reflection
        try {
            Object cdm = getSystemService("color_display");
            if (cdm != null) {
                Method isMethod = cdm.getClass().getMethod("isReduceBrightColorsActivated");
                boolean activated = (Boolean) isMethod.invoke(cdm);
                int state = activated ? 1 : 0;
                saveCachedState(state);
                return state;
            }
        } catch (Throwable t) {
            // ignore
        }

        // 2. Try Settings.Secure.getInt
        try {
            int state = Settings.Secure.getInt(getContentResolver(), SETTING_KEY);
            saveCachedState(state);
            return state;
        } catch (Throwable t) {
            // 3. Fallback to cached state
            return getCachedState();
        }
    }

    private void updateTileState() {
        int state = getCurrentState();
        Tile tile = getQsTile();
        if (tile != null) {
            tile.setState(state == 1 ? Tile.STATE_ACTIVE : Tile.STATE_INACTIVE);
            tile.setIcon(android.graphics.drawable.Icon.createWithResource(this, R.drawable.ic_dim));
            tile.setLabel("Extra Dim");
            tile.updateTile();
        }
    }

    private void registerObserver() {
        if (mObserver == null) {
            mObserver = new ContentObserver(new Handler(Looper.getMainLooper())) {
                @Override
                public void onChange(boolean selfChange, Uri uri) {
                    updateTileState();
                }
            };
            try {
                Uri uri = Settings.Secure.getUriFor(SETTING_KEY);
                getContentResolver().registerContentObserver(uri, false, mObserver);
            } catch (Exception e) {
                Log.w(TAG, "Failed to register ContentObserver", e);
            }
        }
    }

    private void unregisterObserver() {
        if (mObserver != null) {
            try {
                getContentResolver().unregisterContentObserver(mObserver);
            } catch (Exception e) {
                Log.w(TAG, "Failed to unregister ContentObserver", e);
            }
            mObserver = null;
        }
    }

    private int getCachedState() {
        SharedPreferences prefs = getSharedPreferences(PREF_NAME, MODE_PRIVATE);
        return prefs.getInt(PREF_KEY_STATE, 0);
    }

    private void saveCachedState(int state) {
        SharedPreferences prefs = getSharedPreferences(PREF_NAME, MODE_PRIVATE);
        prefs.edit().putInt(PREF_KEY_STATE, state).apply();
    }
}
