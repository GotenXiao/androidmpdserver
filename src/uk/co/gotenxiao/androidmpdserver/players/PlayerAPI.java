package uk.co.gotenxiao.androidmpdserver.players;

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

    public String state();

    public String filename();
    public String artist();
    public String album();
    public int trackNo();
    public String track();
    public int trackLength();
}
