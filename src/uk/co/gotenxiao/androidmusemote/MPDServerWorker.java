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
    static final String PROTO_MPD_CLOSE = "close";
    static final String PROTO_MPD_COMMAND_LIST_BEGIN = "command_list_begin";
    static final String PROTO_MPD_COMMAND_LIST_END = "command_list_end";
    static final String PROTO_MPD_COMMAND_LIST_OK_BEGIN = "command_list_ok_begin";
    static final String PROTO_MPD_COMMANDS = "commands";
    static final String PROTO_MPD_CONSUME = "consume";
    static final String PROTO_MPD_CROSSFADE = "crossfade";
    static final String PROTO_MPD_CURRENTSONG = "currentsong";
    static final String PROTO_MPD_FILE = "file";
    static final String PROTO_MPD_HANDSHAKE = "OK MPD 0.16.0";
    static final String PROTO_MPD_ID = "Id";
    static final String PROTO_MPD_KILL = "kill";
    static final String PROTO_MPD_LIST_OK = "list_OK";
    static final String PROTO_MPD_NAME = "Name";
    static final String PROTO_MPD_NEXT = "next";
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
    static final String PROTO_MPD_VOLUME = "volume";

    private MPDServer mServer = null;
    private Context mContext = null;
    private Socket mSocket = null;
    private BufferedReader mBufRecv = null;
    private DataOutputStream mBufSend = null;
    public volatile boolean running = true;

    private SimpleStringSplitter mSplitter = new SimpleStringSplitter(' '); 

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
        mPlayerAPI = new PowerAMP();
    }

    public void close()
    {
        try
        {
            if (mSocket != null && !mSocket.isClosed())
            {
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

    private void error(int error_num, int command_num, String command, String message)
    {
        String msg = String.format(PROTO_MPD_ACK, error_num, command_num, command, message);
        send(msg);
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

    private void ok()
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

        send(PROTO_MPD_OK);
    }

    private void handleStatus()
    {
        int volume = 0;
        int repeat = 0;
        int random = 0;
        int single = 0;
        int consume = 0;
        int playlist = 0;
        int playlist_length = 0;
        int crossfade = 0;
        String state = "stop";
        int songid = 0;

        send(String.format("%s: %d", PROTO_MPD_VOLUME, volume));
        send(String.format("%s: %d", PROTO_MPD_REPEAT, repeat));
        send(String.format("%s: %d", PROTO_MPD_RANDOM, random));
        send(String.format("%s: %d", PROTO_MPD_SINGLE, single));
        send(String.format("%s: %d", PROTO_MPD_CONSUME, consume));
        send(String.format("%s: %d", PROTO_MPD_PLAYLIST, playlist));
        send(String.format("%s: %d", PROTO_MPD_PLAYLIST_LENGTH, playlist_length));
        send(String.format("%s: %d", PROTO_MPD_CROSSFADE, crossfade));
        send(String.format("%s: %s", PROTO_MPD_STATE, state));
        send(String.format("%s: %d", PROTO_MPD_SONGID, songid));
        ok();
    }

    private void handleCurrentSong()
    {
        String file = "";
        String title = "";
        String name = "";
        int pos = 0;
        int id = 0;

        send(String.format("%s: %s", PROTO_MPD_FILE, file));
        send(String.format("%s: %s", PROTO_MPD_TITLE, title));
        send(String.format("%s: %s", PROTO_MPD_NAME, name));
        send(String.format("%s: %d", PROTO_MPD_POS, pos));
        send(String.format("%s: %d", PROTO_MPD_ID, id));
        ok();
    }

    private boolean handleCommand(String line)
    {
        if (!mProcessingCommandStack)
        {
            Log.d(LOG_TAG, String.format("Received command: %s", line));
        }

        String command = "";
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
            command = mSplitter.next();
        } else
        {
            int currentIdx = mCommandStack.size();
            if (mProcessingCommandStack)
            {
                currentIdx = mCommandStackIndex;
            }

            error(ACK_ERROR_UNKNOWN, currentIdx, "", String.format("unknown command \"%s\"", line));
            return false;
        }

        if (command.equals(PROTO_MPD_PING))
        {
            send(PROTO_MPD_OK, true);
            return true;
        } else if (command.equals(PROTO_MPD_CLOSE))
        {
            send(PROTO_MPD_OK, true);
            close();
            return true;
        } else if (command.equals(PROTO_MPD_KILL))
        {
            send(PROTO_MPD_OK, true);
            mServer.close();
            return true;
        } else if (command.equals(PROTO_MPD_COMMAND_LIST_OK_BEGIN))
        {
            Log.d(LOG_TAG, PROTO_MPD_COMMAND_LIST_OK_BEGIN);

            mCommandStack.add(line);
            return true;
        } else if (command.equals(PROTO_MPD_COMMAND_LIST_BEGIN))
        {
            Log.d(LOG_TAG, PROTO_MPD_COMMAND_LIST_BEGIN);

            if (mProcessingCommandStack)
            {
                return false;
            }
            mCommandStack.add(line);
            return true;
        } else if (command.equals(PROTO_MPD_COMMAND_LIST_END))
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
                    error(ACK_ERROR_UNKNOWN, mCommandStackIndex, command, "");
                }
            }

            mCommandStack.clear();
            mCommandListOK = false;
            mCommandStackIndex = 0;
            ok();
        } else if (command.equals(PROTO_MPD_STATUS))
        {
            handleStatus();
        } else if (command.equals(PROTO_MPD_CURRENTSONG))
        {
            handleCurrentSong();
        } else if (command.equals(PROTO_MPD_PLAY))
        {
            mPlayerAPI.play(mContext);
            ok();
        } else if (command.equals(PROTO_MPD_PAUSE))
        {
            mPlayerAPI.pause(mContext);
            ok();
        } else if (command.equals(PROTO_MPD_NEXT))
        {
            mPlayerAPI.next(mContext);
            ok();
        } else if (command.equals(PROTO_MPD_PREVIOUS))
        {
            mPlayerAPI.previous(mContext);
            ok();
        } else if (command.equals(PROTO_MPD_STOP))
        {
            mPlayerAPI.stop(mContext);
            ok();
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
