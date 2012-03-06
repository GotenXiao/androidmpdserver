package uk.co.gotenxiao.androidmusemote.players;

import com.maxmpz.audioplayer.player.PowerAMPiAPI;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.util.Log;

public class PowerAMP implements PlayerAPI
{
    static final String LOG_TAG = "AndroidMuseMote";
    static final String ACTION_API_COMMAND = "com.maxmpz.audioplayer.API_COMMAND";

    private Context mContext;

    private Intent mPlayIntent;
    private Intent mPauseIntent;
    private Intent mPreviousIntent;
    private Intent mNextIntent;
    private Intent mStopIntent;

    private Intent mTrackIntent;
    private Intent mStatusIntent;
    private Intent mPlayingModeIntent;

    private BroadcastReceiver mTrackReceiver = new BroadcastReceiver()
    {
        @Override
        public void onReceive(Context context, Intent intent)
        {
            mTrackIntent = intent;
            mCurrentTrack = mTrackIntent.getBundleExtra(PowerAMPiAPI.TRACK);
            Log.d(LOG_TAG, "Received track intent: " + intent);
        }
    };

    private BroadcastReceiver mStatusReceiver = new BroadcastReceiver()
    {
        @Override
        public void onReceive(Context context, Intent intent)
        {
            mStatusIntent = intent;
            Log.d(LOG_TAG, "Received status intent: " + intent);
        }
    };

    private BroadcastReceiver mPlayingModeReceiver = new BroadcastReceiver()
    {
        @Override
        public void onReceive(Context context, Intent intent)
        {
            mPlayingModeIntent = intent;
            Log.d(LOG_TAG, "Received playing mode intent: " + intent);
        }
    };

    private Bundle mCurrentTrack;

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

        register();
    }

    public void register()
    {
        mTrackIntent = mContext.registerReceiver(mTrackReceiver, new IntentFilter(PowerAMPiAPI.ACTION_TRACK_CHANGED));
        mStatusIntent = mContext.registerReceiver(mStatusReceiver, new IntentFilter(PowerAMPiAPI.ACTION_STATUS_CHANGED));
        mPlayingModeIntent = mContext.registerReceiver(mPlayingModeReceiver, new IntentFilter(PowerAMPiAPI.ACTION_PLAYING_MODE_CHANGED));
    }

    public void unregister()
    {
        if (mTrackIntent != null)
        {
            try
            {
                mContext.unregisterReceiver(mTrackReceiver);
            } catch (Exception e) {}
        }

        if (mStatusReceiver != null)
        {
            try
            {
                mContext.unregisterReceiver(mStatusReceiver);
            } catch (Exception e) {}
        }

        if (mPlayingModeReceiver != null)
        {
            try
            {
                mContext.unregisterReceiver(mPlayingModeReceiver);
            } catch (Exception e) {}
        }
    }

    private boolean hasMetadata()
    {
        return (mTrackIntent != null && mCurrentTrack != null);
    }

    private boolean hasStatus()
    {
        return (mStatusIntent != null);
    }

    private boolean hasPlayingMode()
    {
        return (mPlayingModeIntent != null);
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
        if (hasPlayingMode())
        {
            if (mPlayingModeIntent.getIntExtra(PowerAMPiAPI.SHUFFLE, 0) != 0)
            {
                return true;
            }
        }
        return false;
    }

    public void setRepeat(boolean repeat)
    {
    }

    public boolean repeat()
    {
        if (hasPlayingMode())
        {
            if (mPlayingModeIntent.getIntExtra(PowerAMPiAPI.REPEAT, 0) != 0)
            {
                return true;
            }
        }
        return false;
    }

    public String state()
    {
        if (hasStatus())
        {
            int status = mStatusIntent.getIntExtra(PowerAMPiAPI.STATUS, -1);
            if (status == PowerAMPiAPI.Status.TRACK_PLAYING)
            {
                boolean paused = mStatusIntent.getBooleanExtra(PowerAMPiAPI.PAUSED, false);
                if (paused)
                {
                    return "pause";
                } else
                {
                    return "play";
                }
            }

        }
        return "stop";
    }

    public String filename()
    {
        if (hasMetadata())
        {
            return mCurrentTrack.getString(PowerAMPiAPI.Track.PATH);
        }
        return "";
    }

    public String artist()
    {
        if (hasMetadata())
        {
            return mCurrentTrack.getString(PowerAMPiAPI.Track.ARTIST);
        }
        return "";
    }

    public String album()
    {
        if (hasMetadata())
        {
            return mCurrentTrack.getString(PowerAMPiAPI.Track.ALBUM);
        }
        return "";
    }

    public int trackNo()
    {
        // PowerAMP API does not currently support track number.
        return 0;
    }

    public String track()
    {
        if (hasMetadata())
        {
            return mCurrentTrack.getString(PowerAMPiAPI.Track.TITLE);
        }
        return "";
    }

    public int trackLength()
    {
        if (hasMetadata())
        {
            return mCurrentTrack.getInt(PowerAMPiAPI.Track.DURATION);
        }
        return 0;
    }
}
