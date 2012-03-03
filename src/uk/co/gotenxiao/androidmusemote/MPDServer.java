package uk.co.gotenxiao.androidmusemote;

import java.io.IOException;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketException;
import java.util.ArrayList;

import android.content.Context;
import android.util.Log;

public class MPDServer extends Thread
{
    static final String LOG_TAG = "AndroidMuseMote";

    private ServerSocket mServer = null;
    private Context mContext = null;
    volatile boolean running = true;

    private String mAddress = "0.0.0.0";
    private int mPort = 6600;

    private ArrayList<MPDServerWorker> mWorkers = null;

    public MPDServer(Context ctx)
    {
        mContext = ctx;
        mWorkers = new ArrayList<MPDServerWorker>();
    }

    public MPDServer(Context ctx, String address, int port)
    {
        mContext = ctx;
        mAddress = address;
        mPort = port;
        mWorkers = new ArrayList<MPDServerWorker>();
    }

    public void run()
    {
        try
        {
            Log.d(LOG_TAG, String.format("Listening on %s:%d", mAddress, mPort));
            mServer = new ServerSocket(mPort, 10, InetAddress.getByName(mAddress));

            while (running)
            {
                Socket client = mServer.accept();
                
                Log.d(LOG_TAG, String.format("New connection from %s:%d", client.getInetAddress(), client.getPort()));
                addWorker(client);
            }
        } catch (SocketException e)
        {
            mServer = null;
        } catch (Exception e)
        {
            Log.e(LOG_TAG, "Exception raised!", e);
        } finally
        {
            close();
        }
    }

    public void close()
    {
        for (MPDServerWorker worker : mWorkers)
        {
            worker.close();
        }

        try
        {
            if (mServer != null)
            {
                mServer.close();
            }
        }
        catch (IOException e)
        {
        }
        finally
        {
            mServer = null;
        }
        running = false;
    }

    public void addWorker(Socket client)
    {
        MPDServerWorker worker = new MPDServerWorker(this, mContext, client);
        mWorkers.add(worker);
        worker.start();
    }

    /*
     * This should always be called from the worker via worker.close()
     */
    public void removeWorker(MPDServerWorker worker)
    {
        mWorkers.remove(worker);
    }
}

