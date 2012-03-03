package uk.co.gotenxiao.androidmusemote.players;

import com.maxmpz.audioplayer.player.PowerAMPiAPI;

import android.content.Context;
import android.content.Intent;

public class PowerAMP implements PlayerAPI
{
    static final String ACTION_API_COMMAND = "com.maxmpz.audioplayer.API_COMMAND";

    public void play(Context context)
    {
        context.startService(
            new Intent(PowerAMPiAPI.ACTION_API_COMMAND)
            .putExtra(PowerAMPiAPI.COMMAND, PowerAMPiAPI.Commands.TOGGLE_PLAY_PAUSE)
        );
    }

    public void pause(Context context)
    {
        context.startService(
            new Intent(PowerAMPiAPI.ACTION_API_COMMAND)
            .putExtra(PowerAMPiAPI.COMMAND, PowerAMPiAPI.Commands.PAUSE)
        );
    }

    public void previous(Context context)
    {
        context.startService(
            new Intent(PowerAMPiAPI.ACTION_API_COMMAND)
            .putExtra(PowerAMPiAPI.COMMAND, PowerAMPiAPI.Commands.PREVIOUS)
        );
    }

    public void next(Context context)
    {
        context.startService(
            new Intent(PowerAMPiAPI.ACTION_API_COMMAND)
            .putExtra(PowerAMPiAPI.COMMAND, PowerAMPiAPI.Commands.NEXT)
        );
    }

    public void stop(Context context)
    {
        context.startService(
            new Intent(PowerAMPiAPI.ACTION_API_COMMAND)
            .putExtra(PowerAMPiAPI.COMMAND, PowerAMPiAPI.Commands.STOP)
        );
    }

    public void random(Context context, boolean random)
    {
    }

    public void repeat(Context context, boolean repeat)
    {
    }
}
