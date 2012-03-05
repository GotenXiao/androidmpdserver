package uk.co.gotenxiao.androidmusemote;

import android.app.Notification;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.os.Binder;
import android.os.IBinder;
import android.util.Log;

public class AndroidMuseMoteService extends Service
{
    private static final String LOG_TAG = "AndroidMuseMote";
    private int NOTIFICATION = R.string.server_running;

    MPDServer server = null;

    @Override
    public void onCreate()
    {
        Log.d(LOG_TAG, "Service starting");
    }

    @Override
    public void onDestroy()
    {
        Log.d(LOG_TAG, "Service stopping");
        stopServer();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startid)
    {
        startServer();
        return START_STICKY;
    }

    @Override
    public IBinder onBind(Intent intent)
    {
        return mBinder;
    }

    public class LocalBinder extends Binder
    {
        AndroidMuseMoteService getService()
        {
            return AndroidMuseMoteService.this;
        }
    }
    private final IBinder mBinder = new LocalBinder();

    private void showNotification()
    {
        CharSequence text = getText(NOTIFICATION);
        Notification notification = new Notification(R.drawable.ic_stat_mpd_server, text, 0);
        notification.flags |= Notification.FLAG_ONGOING_EVENT;
        PendingIntent contentIntent = PendingIntent.getActivity(this, 0, new Intent(this, AndroidMuseMoteMain.class), 0);
        notification.setLatestEventInfo(this, text, "", contentIntent);
        startForeground(NOTIFICATION, notification);
    }

    public void startServer()
    {
        showNotification();
        if (server != null && server.running)
        {
            return;
        }
        server = new MPDServer(this);
        server.start();
    }

    public void stopServer()
    {
        if (server == null)
        {
            return;
        }
        server.close();
        server = null;
        stopForeground(true);
    }
}
