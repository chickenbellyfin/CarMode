package com.chickenbellyfinn.carmode;

import android.app.Activity;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.os.Bundle;
import android.preference.PreferenceManager;
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


public class MainActivity extends Activity implements PowerampReceiver.PowerampListener {

    private static final String TAG = MainActivity.class.getSimpleName();

    private static final SimpleDateFormat DATE_FORMAT = new SimpleDateFormat("h:mm a");

    @BindView(R.id.musicArea) View musicArea;

    @BindView(R.id.playStatus) ImageView playStatus;
    @BindView(R.id.track) TextView track;

    @BindView(R.id.album) TextView album;
    @BindView(R.id.artist) TextView artist;
    @BindView(R.id.art) ImageView art;
    @BindView(R.id.time) TextView time;

    @BindView(R.id.gesturePreview) ImageView gesturePreview;
    @BindView(R.id.nightMode) ImageButton night;

    private PowerampReceiver poweramp;

    private GestureController gestureController;
    private NightModeController nightModeController;

    private Timer timer;

    class TimeUpdateTask extends TimerTask {
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

        poweramp = new PowerampReceiver(this, this);

        nightModeController = new NightModeController(this, night);
        nightModeController.add(time, night, track, album, artist, playStatus, gesturePreview);
        nightModeController.setListener(nightMode -> updateArt());

        gestureController = new GestureController(this, gesturePreview, poweramp);
    }

    @Override
    public void onResume() {
        super.onResume();
        timer = new Timer();
        nightModeController.onResume();
        registerReceiver(poweramp, PowerampReceiver.FILTER);
        timer.scheduleAtFixedRate(new TimeUpdateTask(), 0, 500);
    }

    @Override
    public void onPause() {
        super.onPause();
        nightModeController.onPause();
        unregisterReceiver(poweramp);
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

    private void updateArt() {
        boolean nightMode = nightModeController.isNightMode();
        if (poweramp.art != null && !nightMode) {
            art.clearColorFilter();
            art.setImageBitmap(poweramp.art);
            musicArea.setBackgroundColor(poweramp.color);
        } else {
            art.setImageResource(R.drawable.art_default);
            musicArea.setBackgroundColor(Color.BLACK);
            if (nightMode) {
                art.setColorFilter(NightModeController.nightColor);
            }
        }
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        return gestureController.onTouchEvent(event);
    }

}
