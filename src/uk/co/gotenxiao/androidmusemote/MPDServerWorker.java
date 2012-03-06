package uk.co.gotenxiao.androidmusemote;

import java.io.InputStreamReader;
import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.Socket;
import java.util.ArrayDeque;

import android.content.Intent;
import android.content.Context;
import android.text.TextUtils.SimpleStringSplitter;
import android.util.Log;

import uk.co.gotenxiao.androidmusemote.players.PlayerAPI;
import uk.co.gotenxiao.androidmusemote.players.PowerAMP;

import java.util.ArrayList;

public class MPDServerWorker extends Thread
{
    static final String LOG_TAG = "AndroidMuseMote";

    static final int ACK_ERROR_NOT_LIST = 1;
    static final int ACK_ERROR_ARG = 2;
    static final int ACK_ERROR_PASSWORD = 3;
    static final int ACK_ERROR_PERMISSION = 4;
    static final int ACK_ERROR_UNKNOWN = 5;
    static final int ACK_ERROR_NO_EXIST = 50;
    static final int ACK_ERROR_PLAYLIST_MAX = 51;
    static final int ACK_ERROR_SYSTEM = 52;
    static final int ACK_ERROR_PLAYLIST_LOAD = 53;
    static final int ACK_ERROR_UPDATE_ALREADY = 54;
    static final int ACK_ERROR_PLAYER_SYNC = 55;
    static final int ACK_ERROR_EXIST = 56;

    static final String PROTO_MPD_ACK = "ACK [%d@%d] {%s} %s";
    static final String PROTO_MPD_ALBUM = "Album";
    static final String PROTO_MPD_ARTIST = "Artist";
    static final String PROTO_MPD_AUDIO = "audio";
    static final String PROTO_MPD_BITRATE = "bitrate";
    static final String PROTO_MPD_CLOSE = "close";
    static final String PROTO_MPD_COMMAND_LIST_BEGIN = "command_list_begin";
    static final String PROTO_MPD_COMMAND_LIST_END = "command_list_end";
    static final String PROTO_MPD_COMMAND_LIST_OK_BEGIN = "command_list_ok_begin";
    static final String PROTO_MPD_COMMANDS = "commands";
    static final String PROTO_MPD_CONSUME = "consume";
    static final String PROTO_MPD_CROSSFADE = "crossfade";
    static final String PROTO_MPD_CURRENTSONG = "currentsong";
    static final String PROTO_MPD_DATE = "Date";
    static final String PROTO_MPD_DELIMITER = ": ";
    static final String PROTO_MPD_ELAPSED = "elapsed";
    static final String PROTO_MPD_FILE = "file";
    static final String PROTO_MPD_GENRE = "Genre";
    static final String PROTO_MPD_HANDSHAKE = "OK MPD 0.16.0";
    static final String PROTO_MPD_ID = "Id";
    static final String PROTO_MPD_KILL = "kill";
    static final String PROTO_MPD_LAST_MODIFIED = "Last-Modified";
    static final String PROTO_MPD_LIST_OK = "list_OK";
    static final String PROTO_MPD_MIXRAMPDB = "mixrampdb";
    static final String PROTO_MPD_MIXRAMPDELAY = "mixrampdelay";
    static final String PROTO_MPD_NAME = "Name";
    static final String PROTO_MPD_NEXT = "next";
    static final String PROTO_MPD_NEXTSONGID = "nextsongid";
    static final String PROTO_MPD_NEXTSONG = "nextsong";
    static final String PROTO_MPD_NOTCOMMANDS = "notcommands";
    static final String PROTO_MPD_OK = "OK";
    static final String PROTO_MPD_PAUSE = "pause";
    static final String PROTO_MPD_PING = "ping";
    static final String PROTO_MPD_PLAYLIST_LENGTH = "playlistlength";
    static final String PROTO_MPD_PLAYLIST = "playlist";
    static final String PROTO_MPD_PLAY = "play";
    static final String PROTO_MPD_POS = "Pos";
    static final String PROTO_MPD_PREVIOUS = "previous";
    static final String PROTO_MPD_RANDOM = "random";
    static final String PROTO_MPD_REPEAT = "repeat";
    static final String PROTO_MPD_SETVOL = "setvol";
    static final String PROTO_MPD_SINGLE = "single";
    static final String PROTO_MPD_SONGID = "songid";
    static final String PROTO_MPD_STATE = "state";
    static final String PROTO_MPD_STATE_STOP = "stop";
    static final String PROTO_MPD_STATUS = "status";
    static final String PROTO_MPD_STOP = "stop";
    static final String PROTO_MPD_TITLE = "Title";
    static final String PROTO_MPD_TRACK_LENGTH = "Time";
    static final String PROTO_MPD_TRACK = "Track";
    static final String PROTO_MPD_VOLUME = "volume";
    static final String PROTO_MPD_XFADE = "xfade";

    private MPDServer mServer = null;
    private Context mContext = null;
    private Socket mSocket = null;
    private BufferedReader mBufRecv = null;
    private DataOutputStream mBufSend = null;
    public volatile boolean running = true;

    private SimpleStringSplitter mSplitter = new SimpleStringSplitter(' '); 

    private String mCommand = null;
    private ArrayDeque<String> mCommandStack = null;
    // Are we currently processing the command stack?
    private boolean mProcessingCommandStack = false;
    // Are we currently processing a command_list_ok_begin stack?
    private boolean mCommandListOK = false;
    private int mCommandStackIndex = 0;

    private PlayerAPI mPlayerAPI = null;

    public MPDServerWorker(MPDServer server, Context context, Socket socket)
    {
        mServer = server;
        mContext = context;
        mSocket = socket;
        mCommandStack = new ArrayDeque<String>();
        mPlayerAPI = new PowerAMP(mContext);
    }

    public void close()
    {
        Log.d(LOG_TAG, "Worker closing");
        try
        {
            if (mSocket != null && !mSocket.isClosed())
            {
                Log.d(LOG_TAG, "Closing client socket");
                mSocket.close();
            }
        }
        catch (IOException e)
        {
        }
        finally
        {
            running = false;
            mSocket = null;
            mServer.removeWorker(this);
        }
    }

    private void sendServerStop()
    {
        mContext.stopService(new Intent(mContext, AndroidMuseMoteService.class));
    }

    private void error(int error_num, int command_num, String command, String message)
    {
        String msg = String.format(PROTO_MPD_ACK, error_num, command_num, command, message);
        send(msg);
    }

    private void error(int error_num, String command, String message)
    {
        int currentIdx = mCommandStack.size();
        if (mProcessingCommandStack)
        {
            currentIdx = mCommandStackIndex;
        }

        error(error_num, currentIdx, command, message);
    }

    private void send(String message)
    {
        try
        {
            message += "\n";
            Log.d(LOG_TAG, String.format("Sending message: %s", message));
            mBufSend.writeBytes(message);
        }
        catch (IOException e)
        {
            Log.e(LOG_TAG, "Failed to send message", e);
        }
    }

    private void send(String message, boolean flush)
    {
        send(message);
        if (flush)
        {
            try
            {
                mBufSend.flush();
            } catch (IOException e)
            {
                Log.e(LOG_TAG, "Exception raised flushing send buffer!", e);
            }
        }
    }

    private void sendField(String fieldName, int value)
    {
        send(String.format("%s%s%d", fieldName, PROTO_MPD_DELIMITER, value));
    }

    private void sendField(String fieldName, String value)
    {
        send(String.format("%s%s%s", fieldName, PROTO_MPD_DELIMITER, value));
    }

    private void sendField(String fieldName, boolean value)
    {
        send(String.format("%s%s%d", fieldName, PROTO_MPD_DELIMITER, value ? 1 : 0));
    }

    private void ok(boolean flush)
    {
        if (mCommandListOK)
        {
            send(PROTO_MPD_LIST_OK);
            return;
        }

        if (mProcessingCommandStack)
        {
            return;
        }

        send(PROTO_MPD_OK, flush);
    }

    private void ok()
    {
        ok(false);
    }

    private void handleStatus()
    {
        int volume = 0;
        int single = 0;
        int consume = 0;
        int playlist = 0;
        int playlist_length = 0;
        int crossfade = 0;
        int songid = 0;

        sendField(PROTO_MPD_VOLUME, volume);
        sendField(PROTO_MPD_REPEAT, mPlayerAPI.repeat());
        sendField(PROTO_MPD_RANDOM, mPlayerAPI.random());
        sendField(PROTO_MPD_SINGLE, single);
        sendField(PROTO_MPD_CONSUME, consume);
        sendField(PROTO_MPD_PLAYLIST, playlist);
        sendField(PROTO_MPD_PLAYLIST_LENGTH, playlist_length);
        sendField(PROTO_MPD_CROSSFADE, crossfade);
        sendField(PROTO_MPD_STATE, mPlayerAPI.state());
        sendField(PROTO_MPD_SONGID, songid);
        ok();
    }

    private void handleCurrentSong()
    {
        sendField(PROTO_MPD_FILE, mPlayerAPI.filename());
        sendField(PROTO_MPD_TRACK_LENGTH, mPlayerAPI.trackLength());
        sendField(PROTO_MPD_ARTIST, mPlayerAPI.artist());
        sendField(PROTO_MPD_TITLE, mPlayerAPI.track());
        sendField(PROTO_MPD_ALBUM, mPlayerAPI.album());
        sendField(PROTO_MPD_TRACK, mPlayerAPI.trackNo());
        sendField(PROTO_MPD_POS, 0);
        sendField(PROTO_MPD_ID, 0);
        ok();
    }

    private boolean handleCommand(String line)
    {
        if (!mProcessingCommandStack)
        {
            Log.d(LOG_TAG, String.format("Received command: %s", line));
        }

        mCommand = "";
        line = line.trim();

        if (mCommandStack.size() > 0 && !mProcessingCommandStack)
        {
            mCommandStack.add(line);
            if (!line.equals(PROTO_MPD_COMMAND_LIST_END))
            {
                Log.d(LOG_TAG, "Command queued");
                // Don't process any commands until we received the command list end
                return true;
            }
        }

        mSplitter.setString(line);
        if (mSplitter.hasNext())
        {
            mCommand = mSplitter.next();
        } else
        {
            error(ACK_ERROR_UNKNOWN, "", String.format("unknown command \"%s\"", line));
            return false;
        }

        if (mCommand.equals(PROTO_MPD_PING))
        {
            ok(true);
            return true;
        } else if (mCommand.equals(PROTO_MPD_CLOSE))
        {
            close();
            return true;
        } else if (mCommand.equals(PROTO_MPD_KILL))
        {
            sendServerStop();
            return true;
        } else if (mCommand.equals(PROTO_MPD_COMMAND_LIST_OK_BEGIN))
        {
            Log.d(LOG_TAG, PROTO_MPD_COMMAND_LIST_OK_BEGIN);

            mCommandStack.add(line);
            return true;
        } else if (mCommand.equals(PROTO_MPD_COMMAND_LIST_BEGIN))
        {
            Log.d(LOG_TAG, PROTO_MPD_COMMAND_LIST_BEGIN);

            if (mProcessingCommandStack)
            {
                return false;
            }
            mCommandStack.add(line);
            return true;
        } else if (mCommand.equals(PROTO_MPD_COMMAND_LIST_END))
        {
            Log.d(LOG_TAG, "Processing queued commands");

            mProcessingCommandStack = true;
            mCommandStack.removeLast();

            String startCommand = mCommandStack.removeFirst();
            mCommandListOK = startCommand.equals(PROTO_MPD_COMMAND_LIST_OK_BEGIN);

            boolean result = false;
            boolean any_failed = false;
            mCommandStackIndex = 1; // This starts at 1 to account for the missing command_list_begin

            for (String list_command : mCommandStack)
            {
                result = handleCommand(list_command);
                if (!mCommandListOK && !result)
                {
                    mCommandStack.clear();
                    any_failed = true;
                    break;
                }

                mCommandStackIndex += 1;
            }

            mProcessingCommandStack = false;

            if (!mCommandListOK)
            {
                if (any_failed)
                {
                    error(ACK_ERROR_UNKNOWN, mCommandStackIndex, mCommand, "");
                }
            }

            mCommandStack.clear();
            mCommandListOK = false;
            mCommandStackIndex = 0;
            ok();
        } else if (mCommand.equals(PROTO_MPD_STATUS))
        {
            handleStatus();
        } else if (mCommand.equals(PROTO_MPD_CURRENTSONG))
        {
            handleCurrentSong();
        } else if (mCommand.equals(PROTO_MPD_PLAY))
        {
            mPlayerAPI.play();
            ok();
        } else if (mCommand.equals(PROTO_MPD_PAUSE))
        {
            mPlayerAPI.pause();
            ok();
        } else if (mCommand.equals(PROTO_MPD_NEXT))
        {
            mPlayerAPI.next();
            ok();
        } else if (mCommand.equals(PROTO_MPD_PREVIOUS))
        {
            mPlayerAPI.previous();
            ok();
        } else if (mCommand.equals(PROTO_MPD_STOP))
        {
            mPlayerAPI.stop();
            ok();
        } else
        {
            error(ACK_ERROR_UNKNOWN, mCommand, String.format("unknown command \"%s\"", line));
        }

        return false;
    }

    public void run()
    {
        try
        {
            mBufRecv = new BufferedReader(
                new InputStreamReader(
                    mSocket.getInputStream()
                )
            );
            mBufSend = new DataOutputStream(
                mSocket.getOutputStream()
            );

            send(PROTO_MPD_HANDSHAKE);

            while (running)
            {
                String line = mBufRecv.readLine();
                if (line != null)
                {
                    handleCommand(line);
                } else
                {
                    break;
                }
            }
        } catch (Exception e)
        {
            Log.e(LOG_TAG, "Exception raised!", e);
        } finally
        {
            close();
        }
    }
}
