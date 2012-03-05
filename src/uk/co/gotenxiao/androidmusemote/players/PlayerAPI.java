package uk.co.gotenxiao.androidmusemote.players;

import android.content.Context;

public interface PlayerAPI
{
    public void play();
    public void pause();
    public void previous();
    public void next();
    public void stop();
    public void setRandom(boolean random);
    public boolean random();
    public void setRepeat(boolean repeat);
    public boolean repeat();
}
