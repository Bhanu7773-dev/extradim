package com.bhanu.extradimtile;

import android.app.Activity;
import android.content.ComponentName;
import android.content.Intent;
import android.content.SharedPreferences;
import android.database.ContentObserver;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.provider.Settings;
import android.service.quicksettings.TileService;
import android.util.Log;
import android.view.View;
import android.widget.SeekBar;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;

public class SettingsActivity extends Activity {

    private static final String TAG = "ExtraDimSettings";
    private static final String KEY_ACTIVATED = "reduce_bright_colors_activated";
    private static final String KEY_LEVEL = "reduce_bright_colors_level";
    private static final String PREF_NAME = "extra_dim_prefs";
    private static final String PREF_KEY_STATE = "last_state";
    private static final String PREF_KEY_LEVEL = "last_level";

    private Switch switchExtraDim;
    private SeekBar seekBarIntensity;
    private TextView txtIntensityPercent;
    private ContentObserver mContentObserver;
    private boolean isUpdatingUI = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_settings);

        switchExtraDim = findViewById(R.id.switchExtraDim);
        seekBarIntensity = findViewById(R.id.seekBarIntensity);
        txtIntensityPercent = findViewById(R.id.txtIntensityPercent);
        View btnTelegram = findViewById(R.id.btnTelegram);
        View btnClose = findViewById(R.id.btnClose);

        // Load initial values
        refreshUIFromSettings();

        // Extra Dim On/Off Switch Toggle
        switchExtraDim.setOnCheckedChangeListener((buttonView, isChecked) -> {
            if (isUpdatingUI) return;

            int newState = isChecked ? 1 : 0;
            saveCachedState(newState);

            try {
                Settings.Secure.putInt(getContentResolver(), KEY_ACTIVATED, newState);
                Log.d(TAG, "Successfully set Extra Dim activated to " + newState);
            } catch (SecurityException e) {
                Log.e(TAG, "Missing WRITE_SECURE_SETTINGS permission", e);
                Toast.makeText(this, "Grant WRITE_SECURE_SETTINGS via ADB first!", Toast.LENGTH_LONG).show();
            } catch (Exception e) {
                Log.e(TAG, "Error writing setting", e);
            }

            // Sync Tile state
            try {
                TileService.requestListeningState(this, new ComponentName(this, ExtraDimTileService.class));
            } catch (Exception ignored) {}
        });

        // Intensity SeekBar (0 - 90% Safety Cap)
        seekBarIntensity.setMax(90);
        seekBarIntensity.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                int cappedProgress = Math.min(90, progress);
                txtIntensityPercent.setText(cappedProgress + "%");
                if (fromUser) {
                    saveCachedLevel(cappedProgress);
                    try {
                        Settings.Secure.putInt(getContentResolver(), KEY_LEVEL, cappedProgress);
                    } catch (SecurityException e) {
                        Log.e(TAG, "Missing WRITE_SECURE_SETTINGS permission", e);
                    } catch (Exception e) {
                        Log.e(TAG, "Error writing intensity level", e);
                    }
                }
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {}

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {}
        });

        // Developer Telegram link
        btnTelegram.setOnClickListener(v -> {
            try {
                Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse("https://t.me/darkdevil7773"));
                startActivity(intent);
            } catch (Exception e) {
                Toast.makeText(this, "Telegram: @darkdevil7773", Toast.LENGTH_SHORT).show();
            }
        });

        // Done / Close button
        btnClose.setOnClickListener(v -> finish());

        // Register ContentObserver to keep UI live-synced
        registerObserver();
    }

    @Override
    public boolean onKeyDown(int keyCode, android.view.KeyEvent event) {
        // Emergency Volume Up recovery to reset to safe 50%
        if (keyCode == android.view.KeyEvent.KEYCODE_VOLUME_UP) {
            saveCachedLevel(50);
            try {
                Settings.Secure.putInt(getContentResolver(), KEY_LEVEL, 50);
            } catch (Exception ignored) {}
            refreshUIFromSettings();
            Toast.makeText(this, "Safety Reset: Intensity set to 50%", Toast.LENGTH_SHORT).show();
            return true;
        }
        return super.onKeyDown(keyCode, event);
    }

    private void refreshUIFromSettings() {
        int activated = getCachedState();
        int level = Math.min(90, getCachedLevel());

        try {
            activated = Settings.Secure.getInt(getContentResolver(), KEY_ACTIVATED);
            saveCachedState(activated);
        } catch (Throwable ignored) {}

        try {
            level = Math.min(90, Settings.Secure.getInt(getContentResolver(), KEY_LEVEL));
            saveCachedLevel(level);
        } catch (Throwable ignored) {}

        isUpdatingUI = true;
        switchExtraDim.setChecked(activated == 1);
        seekBarIntensity.setProgress(level);
        txtIntensityPercent.setText(level + "%");
        isUpdatingUI = false;
    }

    private void registerObserver() {
        mContentObserver = new ContentObserver(new Handler(Looper.getMainLooper())) {
            @Override
            public void onChange(boolean selfChange) {
                refreshUIFromSettings();
            }
        };
        try {
            getContentResolver().registerContentObserver(
                Settings.Secure.getUriFor(KEY_ACTIVATED), false, mContentObserver);
            getContentResolver().registerContentObserver(
                Settings.Secure.getUriFor(KEY_LEVEL), false, mContentObserver);
        } catch (Exception e) {
            Log.w(TAG, "Failed to register content observer", e);
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

    private int getCachedLevel() {
        SharedPreferences prefs = getSharedPreferences(PREF_NAME, MODE_PRIVATE);
        return prefs.getInt(PREF_KEY_LEVEL, 80);
    }

    private void saveCachedLevel(int level) {
        SharedPreferences prefs = getSharedPreferences(PREF_NAME, MODE_PRIVATE);
        prefs.edit().putInt(PREF_KEY_LEVEL, level).apply();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (mContentObserver != null) {
            try {
                getContentResolver().unregisterContentObserver(mContentObserver);
            } catch (Exception ignored) {}
        }
    }
}
