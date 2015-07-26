package com.chickenbellyfinn.carmode;

import android.content.Context;
import android.content.Intent;

import com.maxmpz.poweramp.player.PowerampAPI;

public class PowerampController {

    private Context context;

    public PowerampController(Context context){
        this.context = context;
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
        context.startService(new Intent(PowerampAPI.ACTION_API_COMMAND).setPackage(PowerampAPI.PACKAGE_NAME).putExtra(PowerampAPI.COMMAND, action));
    }

}
