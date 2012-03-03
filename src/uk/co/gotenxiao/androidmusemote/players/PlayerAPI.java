package uk.co.gotenxiao.androidmusemote.players;

import android.content.Context;

public interface PlayerAPI
{
    public void play(Context context);
    public void pause(Context context);
    public void previous(Context context);
    public void next(Context context);
    public void stop(Context context);
    public void random(Context context, boolean random);
    public void repeat(Context context, boolean repeat);
}
