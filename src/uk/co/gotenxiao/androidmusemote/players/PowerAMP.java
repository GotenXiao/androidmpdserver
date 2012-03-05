package uk.co.gotenxiao.androidmusemote.players;

import com.maxmpz.audioplayer.player.PowerAMPiAPI;

import android.content.Context;
import android.content.Intent;

public class PowerAMP implements PlayerAPI
{
    static final String ACTION_API_COMMAND = "com.maxmpz.audioplayer.API_COMMAND";

    private Context mContext;

    private Intent mPlayIntent;
    private Intent mPauseIntent;
    private Intent mPreviousIntent;
    private Intent mNextIntent;
    private Intent mStopIntent;

    public PowerAMP(Context context)
    {
        mContext = context;

        mPlayIntent = (
            new Intent(PowerAMPiAPI.ACTION_API_COMMAND)
            .putExtra(PowerAMPiAPI.COMMAND, PowerAMPiAPI.Commands.TOGGLE_PLAY_PAUSE)
        );
        mPauseIntent = (
            new Intent(PowerAMPiAPI.ACTION_API_COMMAND)
            .putExtra(PowerAMPiAPI.COMMAND, PowerAMPiAPI.Commands.PAUSE)
        );
        mPreviousIntent = (
            new Intent(PowerAMPiAPI.ACTION_API_COMMAND)
            .putExtra(PowerAMPiAPI.COMMAND, PowerAMPiAPI.Commands.PREVIOUS)
        );
        mNextIntent = (
            new Intent(PowerAMPiAPI.ACTION_API_COMMAND)
            .putExtra(PowerAMPiAPI.COMMAND, PowerAMPiAPI.Commands.NEXT)
        );
        mStopIntent = (
            new Intent(PowerAMPiAPI.ACTION_API_COMMAND)
            .putExtra(PowerAMPiAPI.COMMAND, PowerAMPiAPI.Commands.STOP)
        );
    }

    public void play()
    {
        mContext.startService(mPlayIntent);
    }

    public void pause()
    {
        mContext.startService(mPauseIntent);
    }

    public void previous()
    {
        mContext.startService(mPreviousIntent);
    }

    public void next()
    {
        mContext.startService(mNextIntent);
    }

    public void stop()
    {
        mContext.startService(mStopIntent);
    }

    public void setRandom(boolean random)
    {
    }

    public boolean random()
    {
        return false;
    }

    public void setRepeat(boolean repeat)
    {
    }

    public boolean repeat()
    {
        return false;
    }
}
