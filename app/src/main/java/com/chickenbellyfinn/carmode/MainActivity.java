package com.chickenbellyfinn.carmode;

import android.app.Activity;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.graphics.Point;
import android.graphics.PointF;
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

    private static final long GESTURE_HANG_TIME = 300;
    private static final int GESTURE_NONE = -1;
    private static final int GESTURE_NEXT = 0;
    private static final int GESTURE_PREV = 1;
    private static final int GESTURE_PLAY = 2;

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

    private View musicArea;
    private ImageView playStatus;

    private TextView track;
    private TextView album;
    private TextView artist;
    private ImageView art;

    private TextView time;

    private ImageView gesturePreview;

    private ImageButton night;

    private int lightMode;
    private boolean nightMode = false;

    private Timer timer;
    class TimeTask extends TimerTask {
        @Override
        public void run() {
            updateTime();
        }
    };

    //gesture stuff
    private float minDist;
    private PointF downLoc;
    private PointF curLoc;
    private boolean lastDetected = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        getWindow().setStatusBarColor(Color.BLACK);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);

        prefs = PreferenceManager.getDefaultSharedPreferences(this);
        vibrator = (Vibrator)getSystemService(VIBRATOR_SERVICE);
        sensorManager = (SensorManager)getSystemService(SENSOR_SERVICE);
        lightSensor = sensorManager.getDefaultSensor(Sensor.TYPE_LIGHT);

        controls = new PowerampController(this);
        poweramp = new PowerampReceiver(this);

        musicArea = findViewById(R.id.musicArea);
        playStatus = (ImageView)findViewById(R.id.playStatus);
        time = (TextView)findViewById(R.id.time);
        night = (ImageButton)findViewById(R.id.nightMode);

        track = (TextView)findViewById(R.id.track);
        album = (TextView)findViewById(R.id.album);
        artist = (TextView)findViewById(R.id.artist);
        art = (ImageView)findViewById(R.id.art);

        gesturePreview = (ImageView)findViewById(R.id.gesturePreview);

        night.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                vibrateClick();
                lightMode = (lightMode+1)%3;
                prefs.edit().putInt(PREF_LIGHT_MODE, lightMode).commit();

                updateLightMode();
            }
        });

        night.setImageResource(LIGHT_MODE_ICONS[lightMode]);

        Point winSize = new Point();
        getWindowManager().getDefaultDisplay().getSize(winSize);
        minDist = winSize.x*0.15f;
    }

    @Override
    public void onResume(){
        super.onResume();
        timer = new Timer();

        lightMode = prefs.getInt(PREF_LIGHT_MODE, LIGHT_MODE_AUTO);
        updateLightMode();

        registerReceiver(poweramp, PowerampReceiver.FILTER);
        sensorManager.registerListener(this, lightSensor, SensorManager.SENSOR_DELAY_GAME);
        timer.scheduleAtFixedRate(new TimeTask(), 0, 500);
    }

    @Override
    public void onPause(){
        super.onPause();
        unregisterReceiver(poweramp);
        sensorManager.unregisterListener(this);
        timer.cancel();
    }

    @Override
    public void onDestroy(){
        super.onDestroy();
    }

    @Override
    public void updated() {
        playStatus.setImageResource(poweramp.isPaused? R.drawable.ic_pause : R.drawable.ic_play );
        track.setText(poweramp.title);
        album.setText(poweramp.album);
        artist.setText(poweramp.artist);

        updateArt();
    }

    private void updateTime(){
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
               time.setText(DATE_FORMAT.format(Calendar.getInstance().getTime()));
            }
        });
    }

    private void updateLightMode(){
        if(lightMode == LIGHT_MODE_BRIGHT)setNightMode(false);
        else if(lightMode == LIGHT_MODE_DARK)setNightMode(true);
        night.setImageResource(LIGHT_MODE_ICONS[lightMode]);
    }

    private void vibrateClick(){
        vibrator.vibrate(VIBRATE_CLICK);
    }

    private void setNightMode(boolean mode){
        nightMode = mode;
        int color = nightMode?NIGHT_COLOR:Color.WHITE;
        time.setTextColor(color);
        night.setColorFilter(color);
        track.setTextColor(color);
        album.setTextColor(color);
        artist.setTextColor(color);
        playStatus.setColorFilter(color);
        gesturePreview.setColorFilter(color);
        updateArt();
    }

    private void updateArt(){
        if(poweramp.art != null && !nightMode){
            art.clearColorFilter();
            art.setImageBitmap(poweramp.art);
            musicArea.setBackgroundColor(poweramp.color);
        } else {
            art.setImageResource(R.drawable.art_default);
            musicArea.setBackgroundColor(Color.BLACK);
            if(nightMode){
                art.setColorFilter(NIGHT_COLOR);
            }
        }
    }

    @Override
    public void onSensorChanged(SensorEvent event) {
        if(lightMode != LIGHT_MODE_AUTO)return;
        boolean isDark = event.values[0] < DARK_VAULE;
        if(nightMode != isDark){
            setNightMode(isDark);
        }
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {}

    @Override
    public boolean onTouchEvent(MotionEvent event){
        int action = event.getAction();

        curLoc = new PointF(event.getX(), event.getY());

        if(action == MotionEvent.ACTION_DOWN){
            downLoc = curLoc;
            return true;
        } else if (action == MotionEvent.ACTION_MOVE){
            checkGesture();
            return true;
        } else if(action == MotionEvent.ACTION_UP){
            int result = checkGesture();
            downLoc = null;
            curLoc = null;

            timer.schedule(new TimerTask() {
                @Override
                public void run() {
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            gesturePreview.setImageResource(0);
                        }
                    });
                }
            }, result == GESTURE_NONE ? 0 : GESTURE_HANG_TIME);

            if(result == GESTURE_NEXT){
                controls.next();
            } else if (result == GESTURE_PREV){
                controls.prev();
            } else if(result == GESTURE_PLAY){
                controls.playPause();
            }

            return true;
        }

        return false;
    }

    private int checkGesture(){

        boolean detected = false;
        int gesture = GESTURE_NONE;

        if(curLoc != null && downLoc != null){
            float dx = (curLoc.x - downLoc.x);
            float dy = (curLoc.y - downLoc.y);
            float dist = (float)Math.sqrt(dx*dx + dy*dy);

            Log.d(TAG, String.format("%.2f, %.2f, %.2f", dx, dy, dist));

            float alpha = Math.max(0f, Math.min(1f, dist/minDist));
            detected = (alpha == 1f);
            gesturePreview.setImageAlpha((int)(255f*alpha));

            if(Math.abs(dx) > Math.abs(dy)) {
                if (dx > 0) {
                    gesturePreview.setImageResource(R.drawable.ic_skip_next);
                    gesture = GESTURE_NEXT;
                } else if (dx < 0) {
                    gesturePreview.setImageResource(R.drawable.ic_skip_prev);
                    gesture = GESTURE_PREV;
                }
            } else {
                if(dy > 0){
                    gesturePreview.setImageResource(poweramp.isPaused ? R.drawable.ic_play : R.drawable.ic_pause);
                    gesture = GESTURE_PLAY;
                }
            }
        }

        return detected ? gesture : GESTURE_NONE;
    }
}
