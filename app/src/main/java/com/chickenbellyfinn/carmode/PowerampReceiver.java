package com.chickenbellyfinn.carmode;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.support.v7.graphics.Palette;

import com.maxmpz.poweramp.player.PowerampAPI;

public class PowerampReceiver extends BroadcastReceiver {

    private final static String TAG = PowerampReceiver.class.getSimpleName();

    public final static IntentFilter FILTER = new IntentFilter(){{
        addAction(PowerampAPI.ACTION_STATUS_CHANGED);
        addAction(PowerampAPI.ACTION_TRACK_CHANGED);
        addAction(PowerampAPI.ACTION_AA_CHANGED);
    }};

    interface PowerampListener {
        public void onPowerampUpdate();
    }

    private Context context;
    private PowerampListener listener;

    public boolean isPaused = false;

    public String title;
    public String album;
    public String artist;
    public Bitmap art;
    public int color;

    public PowerampReceiver(Context context, PowerampListener listener){
        this.context = context;
        this.listener = listener;
    }

    @Override
    public void onReceive(Context context, Intent intent) {
        String action = intent.getAction();

        switch (action){
            case PowerampAPI.ACTION_STATUS_CHANGED:
                onStatusChanged(intent);
                break;
            case PowerampAPI.ACTION_TRACK_CHANGED:
                onTrackChanged(intent);
                break;
            case PowerampAPI.ACTION_AA_CHANGED:
                onAlbumArtChanged(context, intent);
                break;
        }

        if(listener != null){
            listener.onPowerampUpdate();
        }
    }

    private void onStatusChanged(Intent intent){
        int status = intent.getIntExtra(PowerampAPI.STATUS, -1);
        boolean playingEnded = (status == PowerampAPI.Status.PLAYING_ENDED);
        boolean paused = intent.getBooleanExtra(PowerampAPI.PAUSED, false);
        isPaused = ( paused || playingEnded);
    }

    private void onTrackChanged(Intent intent){
        Bundle trackBundle = intent.getBundleExtra(PowerampAPI.TRACK);
        title = trackBundle.getString(PowerampAPI.Track.TITLE);
        album = trackBundle.getString(PowerampAPI.Track.ALBUM);
        artist = trackBundle.getString(PowerampAPI.Track.ARTIST);
    }

    private void onAlbumArtChanged(Context context, Intent intent){
        String artPath = intent.getStringExtra(PowerampAPI.ALBUM_ART_PATH);
        Bitmap newArt = null;
        if(artPath != null && !artPath.isEmpty()){
            try {
                newArt = MediaStore.Images.Media.getBitmap(context.getContentResolver(), Uri.parse(artPath));
            } catch (Exception e){}
        } else if (intent.hasExtra(PowerampAPI.ALBUM_ART_BITMAP)){
            newArt = intent.getParcelableExtra(PowerampAPI.ALBUM_ART_BITMAP);
        }

        if(art != null){
            art.recycle();
        }
        art = newArt;

        if(art != null) {
            color = Palette.generate(art).getDarkVibrantColor(Color.BLACK);
        } else {
            color = Color.BLACK;
        }
    }

    public void prev(){
        command(PowerampAPI.Commands.PREVIOUS);
    }

    public void next(){
        command(PowerampAPI.Commands.NEXT);
    }

    public void playPause(){
        command(PowerampAPI.Commands.TOGGLE_PLAY_PAUSE);
    }

    private void command(int action){
        Intent apiCommandIntent = new Intent(PowerampAPI.ACTION_API_COMMAND)
                .setPackage(PowerampAPI.PACKAGE_NAME)
                .putExtra(PowerampAPI.COMMAND, action);
        context.startService(apiCommandIntent);
    }

}
