package com.chickenbellyfinn.carmode;

import android.app.Activity;
import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.preference.PreferenceManager;
import android.view.View;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by akshay on 12/28/16.
 */

public class NightModeController implements SensorEventListener {

    public interface NightModeListener {
        public void onNightModeChanged(boolean isNightMode);
    }

    private static final String PREF_LIGHT_MODE = "lightMode";

    public static final int defaultColor = Color.WHITE;
    public static final int nightColor = 0xffd60000;

    private static final float SENSOR_DARK_VALUE = 20;

    private static final int MODE_AUTO = 0;
    private static final int MODE_BRIGHT = 1;
    private static final int MODE_DARK = 2;

    private static final int[] LIGHT_MODE_ICONS = {
            R.drawable.ic_brightness_auto,
            R.drawable.ic_brightness_light,
            R.drawable.ic_brightness_dark,
    };

    private int lightMode;
    private boolean isLowLight = false;

    private Activity activity;
    private ImageButton modeButton;

    private NightModeListener listener;
    private SharedPreferences prefs;
    private SensorManager sensorManager;
    private Sensor lightSensor;

    private List<View> views = new ArrayList<>();

    public NightModeController(Activity activity, ImageButton modeButton){
        this.activity = activity;
        this.modeButton = modeButton;
        prefs = PreferenceManager.getDefaultSharedPreferences(activity);
        modeButton.setOnClickListener(view -> onNightModeClicked());

        lightMode = prefs.getInt(PREF_LIGHT_MODE, MODE_AUTO);
        updateModeButton();

        sensorManager = (SensorManager) activity.getSystemService(Context.SENSOR_SERVICE);
        lightSensor = sensorManager.getDefaultSensor(Sensor.TYPE_LIGHT);
    }

    public void setListener(NightModeListener listener){
        this.listener = listener;
    }

    public void add(View... views){
        for(View v:views){
            this.views.add(v);
        }
    }

    public boolean isNightMode(){
        if(lightMode == MODE_AUTO){
            return isLowLight;
        }

        return lightMode == MODE_DARK;
    }

    private void updateTheme(){
        int color = isNightMode() ? nightColor : defaultColor;
        for(View view:views){
            if(view instanceof TextView){
                ((TextView)view).setTextColor(color);
            } else if (view instanceof ImageView){
                ((ImageView)view).setColorFilter(color);
            }
        }

        if(listener != null){
            listener.onNightModeChanged(isNightMode());
        }
    }

    private void updateModeButton(){
        modeButton.setImageResource(LIGHT_MODE_ICONS[lightMode]);
    }

    private void onNightModeClicked(){
        lightMode = (lightMode + 1) % 3;
        prefs.edit().putInt(PREF_LIGHT_MODE, lightMode).commit();
        updateModeButton();
        updateTheme();
    }

    public void onResume(){
        sensorManager.registerListener(this, lightSensor, SensorManager.SENSOR_DELAY_GAME);

        lightMode = prefs.getInt(PREF_LIGHT_MODE, MODE_AUTO);
        updateModeButton();
        updateTheme();
    }

    public void onPause(){
        sensorManager.unregisterListener(this);
    }

    @Override
    public void onSensorChanged(SensorEvent sensorEvent) {
        if(lightMode == MODE_AUTO) {
            float value = sensorEvent.values[0];
            isLowLight = value < SENSOR_DARK_VALUE;
            updateTheme();
        }

    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int i) {}
}
