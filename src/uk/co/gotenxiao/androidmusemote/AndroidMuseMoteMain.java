package uk.co.gotenxiao.androidmusemote;

import android.app.Activity;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.Bundle;
import android.os.IBinder;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.ToggleButton;

public class AndroidMuseMoteMain extends Activity implements OnClickListener
{
    private static final String LOG_TAG = "AndroidMuseMote";
    private ToggleButton toggleButton = null;
    private AndroidMuseMoteService mService = null;
    private boolean mIsBound = false;

    private Intent mServiceIntent = null;

    private ServiceConnection mConnection = new ServiceConnection()
    {
        public void onServiceConnected(ComponentName className, IBinder service)
        {
            mService = ((AndroidMuseMoteService.LocalBinder)service).getService();
            updateButtonChecked();
        }

        public void onServiceDisconnected(ComponentName className)
        {
            mService = null;
            updateButtonChecked();
        }
    };

    void doBindService()
    {
        bindService(mServiceIntent, mConnection, 0);
        mIsBound = true;
    }

    void doUnbindService()
    {
        if (mIsBound)
        {
            unbindService(mConnection);
            mIsBound = false;
        }
    }

    @Override
    public void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main);

        mServiceIntent = new Intent(this, AndroidMuseMoteService.class);

        toggleButton = (ToggleButton) findViewById(R.id.button);
        toggleButton.setOnClickListener(this);
    }

    @Override
    public void onDestroy()
    {
        super.onDestroy();

        if (!isRunning())
        {
            stopService(mServiceIntent);
        }
        doUnbindService();
    }

    @Override
    public void onPause()
    {
        super.onPause();
    }

    @Override
    public void onResume()
    {
        super.onResume();

        doBindService();
    }

    @Override
    protected void onSaveInstanceState(Bundle outState)
    {
    }

    @Override
    protected void onRestoreInstanceState(Bundle savedInstanceState)
    {
    }

    public boolean isRunning()
    {
        return (mService != null && mService.server != null && mService.server.running);
    }

    public void startServer()
    {
        if (isRunning())
        {
            return;
        }

        startService(mServiceIntent);
    }

    public void stopServer()
    {
        if (!isRunning())
        {
            return;
        }

        stopService(mServiceIntent);
    }

    public void toggleServer()
    {
        if (!isRunning())
        {
            startServer();
        } else
        {
            stopServer();
        }
    }

    public void onClick(View v)
    {
        toggleServer();
    }

    public void updateButtonChecked()
    {
        toggleButton.setChecked(isRunning());
    }
}
