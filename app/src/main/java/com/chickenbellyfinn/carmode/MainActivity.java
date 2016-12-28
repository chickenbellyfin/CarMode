package com.chickenbellyfinn.carmode;

import android.app.Activity;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Bundle;
import android.os.Vibrator;
import android.preference.PreferenceManager;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.view.WindowManager;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Timer;
import java.util.TimerTask;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;


public class MainActivity extends Activity implements PowerampReceiver.PowerampListener, SensorEventListener {

    private static final String TAG = MainActivity.class.getSimpleName();

    private static final String PREF_LIGHT_MODE = "lightMode";

    private static final long VIBRATE_CLICK = 65;
    private static final SimpleDateFormat DATE_FORMAT = new SimpleDateFormat("h:mm a");
    private static final int NIGHT_COLOR = 0xffd60000;
    private static final float DARK_VAULE = 20;

    private static final int LIGHT_MODE_AUTO = 0;
    private static final int LIGHT_MODE_BRIGHT = 1;
    private static final int LIGHT_MODE_DARK = 2;

    private static final int[] LIGHT_MODE_ICONS = {
            R.drawable.ic_brightness_auto,
            R.drawable.ic_brightness_light,
            R.drawable.ic_brightness_dark,
    };

    private SharedPreferences prefs;
    private Vibrator vibrator;
    private SensorManager sensorManager;
    private Sensor lightSensor;

    private PowerampController controls;
    private PowerampReceiver poweramp;

    @BindView(R.id.musicArea) View musicArea;
    @BindView(R.id.playStatus) ImageView playStatus;

    @BindView(R.id.track) TextView track;
    @BindView(R.id.album) TextView album;
    @BindView(R.id.artist) TextView artist;
    @BindView(R.id.art) ImageView art;

    @BindView(R.id.time) TextView time;
    @BindView(R.id.gesturePreview) ImageView gesturePreview;
    @BindView(R.id.nightMode) ImageButton night;

    private GestureController gestureController;

    private int lightMode;
    private boolean nightMode = false;

    private Timer timer;

    class TimeTask extends TimerTask {
        @Override
        public void run() {
            runOnUiThread(() -> time.setText(DATE_FORMAT.format(Calendar.getInstance().getTime())));
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        ButterKnife.bind(this);
        getWindow().setStatusBarColor(Color.BLACK);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);

        prefs = PreferenceManager.getDefaultSharedPreferences(this);
        vibrator = (Vibrator) getSystemService(VIBRATOR_SERVICE);
        sensorManager = (SensorManager) getSystemService(SENSOR_SERVICE);
        lightSensor = sensorManager.getDefaultSensor(Sensor.TYPE_LIGHT);

        controls = new PowerampController(this);
        poweramp = new PowerampReceiver(this);

        night.setImageResource(LIGHT_MODE_ICONS[lightMode]);

        gestureController = new GestureController(this, gesturePreview, poweramp);
    }

    @Override
    public void onResume() {
        super.onResume();
        timer = new Timer();

        lightMode = prefs.getInt(PREF_LIGHT_MODE, LIGHT_MODE_AUTO);
        updateLightMode();

        registerReceiver(poweramp, PowerampReceiver.FILTER);
        sensorManager.registerListener(this, lightSensor, SensorManager.SENSOR_DELAY_GAME);
        timer.scheduleAtFixedRate(new TimeTask(), 0, 500);
    }

    @Override
    public void onPause() {
        super.onPause();
        unregisterReceiver(poweramp);
        sensorManager.unregisterListener(this);
        timer.cancel();
    }


    @Override
    public void onPowerampUpdate() {
        playStatus.setImageResource(poweramp.isPaused ? R.drawable.ic_pause : R.drawable.ic_play);
        track.setText(poweramp.title);
        album.setText(poweramp.album);
        artist.setText(poweramp.artist);

        updateArt();
    }

    @OnClick(R.id.nightMode)
    public void onNightModeClicked(){
        vibrateClick();
        lightMode = (lightMode + 1) % 3;
        prefs.edit().putInt(PREF_LIGHT_MODE, lightMode).commit();

        updateLightMode();
    }

    private void updateLightMode() {
        if (lightMode == LIGHT_MODE_BRIGHT) setNightMode(false);
        else if (lightMode == LIGHT_MODE_DARK) setNightMode(true);
        night.setImageResource(LIGHT_MODE_ICONS[lightMode]);
    }

    private void vibrateClick() {
        vibrator.vibrate(VIBRATE_CLICK);
    }

    private void setNightMode(boolean mode) {
        nightMode = mode;
        int color = nightMode ? NIGHT_COLOR : Color.WHITE;
        time.setTextColor(color);
        night.setColorFilter(color);
        track.setTextColor(color);
        album.setTextColor(color);
        artist.setTextColor(color);
        playStatus.setColorFilter(color);
        gesturePreview.setColorFilter(color);
        updateArt();
    }

    private void updateArt() {
        if (poweramp.art != null && !nightMode) {
            art.clearColorFilter();
            art.setImageBitmap(poweramp.art);
            musicArea.setBackgroundColor(poweramp.color);
        } else {
            art.setImageResource(R.drawable.art_default);
            musicArea.setBackgroundColor(Color.BLACK);
            if (nightMode) {
                art.setColorFilter(NIGHT_COLOR);
            }
        }
    }

    @Override
    public void onSensorChanged(SensorEvent event) {
        if (lightMode != LIGHT_MODE_AUTO) return;
        boolean isDark = event.values[0] < DARK_VAULE;
        if (nightMode != isDark) {
            setNightMode(isDark);
        }
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {}

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        return gestureController.onTouchEvent(event);
    }


}
