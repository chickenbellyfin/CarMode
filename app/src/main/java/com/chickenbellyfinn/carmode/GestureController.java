package com.chickenbellyfinn.carmode;

import android.app.Activity;
import android.graphics.Point;
import android.graphics.PointF;
import android.support.annotation.Dimension;
import android.util.Log;
import android.view.MotionEvent;
import android.widget.ImageView;

import java.util.Objects;
import java.util.Timer;
import java.util.TimerTask;

/**
 * Created by akshay on 12/28/16.
 */

public class GestureController {

    private static final String TAG = GestureController.class.getSimpleName();

    private static final float MIN_SWIPE_DIST = 0.15f;
    private static final float MARGIN_TOP = 0.2f;

    private static final int GESTURE_NONE = 0;
    private static final int GESTURE_LEFT = 1;
    private static final int GESTURE_RIGHT = 2;
    private static final int GESTURE_UP = 3;
    private static final int GESTURE_DOWN = 4;

    private static final long GESTURE_HANG_TIME = 300;

    private ImageView preview;

    private class RemovePreviewTask extends TimerTask {
        @Override
        public void run() {
            activity.runOnUiThread(() -> preview.setImageResource(0));
        }
    };

    private Activity activity;

    private float minDist;
    private float minY;

    private PointF currentLocation;
    private PointF downLocation;

    private Timer timer;
    private PowerampController powerampController;
    private PowerampReceiver poweramp;

    public GestureController(Activity activity, ImageView view, PowerampReceiver poweramp){
        this.activity = activity;
        this.preview = view;
        this.poweramp = poweramp;

        powerampController = new PowerampController(activity);
        timer = new Timer();

        Point winSize = new Point();
        activity.getWindowManager().getDefaultDisplay().getSize(winSize);
        minDist = winSize.x * MIN_SWIPE_DIST;
        minY = winSize.y * MARGIN_TOP;
    }

    private void gesturePreview(int gesture, double alpha){
        int resource = 0;
        switch (gesture){
            case GESTURE_LEFT:
                resource = R.drawable.ic_skip_next;
                break;
            case GESTURE_RIGHT:
                resource = R.drawable.ic_skip_prev;
                break;
            case GESTURE_UP:
                // unused
                break;
            case GESTURE_DOWN:
                resource = poweramp.isPaused ? R.drawable.ic_play : R.drawable.ic_pause;
                break;
        }

        preview.setImageResource(resource);
        preview.setImageAlpha((int)(255 * alpha));
    }

    private void gestureDetected(int gesture){
        switch (gesture){
            case GESTURE_LEFT:
                powerampController.next();
                break;
            case GESTURE_RIGHT:
                powerampController.prev();
                break;
            case GESTURE_UP:
                // unused
                break;
            case GESTURE_DOWN:
                powerampController.playPause();
                break;
        }
    }

    public boolean onTouchEvent(MotionEvent event){
        int action = event.getAction();

        currentLocation = new PointF(event.getX(), event.getY());

        if(action == MotionEvent.ACTION_DOWN && currentLocation.y > minY){
            downLocation = currentLocation;
            return true;
        } else if (downLocation != null){
            int gesture = getDirection();
            double progress = getProgress();
            boolean completed = progress == 1;
            if(action == MotionEvent.ACTION_MOVE){
                gesturePreview(gesture, progress);
                return true;

            } else if (action == MotionEvent.ACTION_UP){
                if(completed) {
                    gestureDetected(gesture);
                }

                timer.schedule(new RemovePreviewTask(), completed ? GESTURE_HANG_TIME : 0);

                downLocation = null;
                return true;
            }
        }
        return false;
    }

    private int getDirection(){
        float dx = (currentLocation.x - downLocation.x);
        float dy = (currentLocation.y - downLocation.y);

        int direction;

        if(Math.abs(dx) > Math.abs(dy)) {
            direction = (dx > 0) ? GESTURE_LEFT : GESTURE_RIGHT;
        } else {
            direction = (dy > 0) ? GESTURE_DOWN : GESTURE_UP;
        }

        return direction;
    }

    private double getProgress(){

        float dx = (currentLocation.x - downLocation.x);
        float dy = (currentLocation.y - downLocation.y);
        double dist = Math.sqrt(dx*dx + dy*dy)/minDist;

        return Math.min(1, dist);
    }
}
