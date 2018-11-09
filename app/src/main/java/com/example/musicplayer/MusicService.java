package com.example.musicplayer;

import android.app.Service;
import android.content.Intent;
import android.os.Binder;
import android.os.IBinder;
import android.util.Log;

public class MusicService extends Service {

    private final Binder mBind = new mBinder();

    MP3Player myMP3;



    public MusicService() {
        myMP3 = new MP3Player();
        Log.d("MusicPlayer", myMP3.getState().toString());
    }

    @Override
    public IBinder onBind(Intent intent) {
        Log.d("MusicPlayer", "onBind");
        return mBind;
    }

    @Override
    public boolean onUnbind(Intent intent){
        Log.d("MusicPlayer", "onUnbind");
        //myMP3.stop();
        return false;
    }

    public class mBinder extends Binder{
        MusicService getService(){
            return MusicService.this;
        }
    }



    protected void load(String path){
        myMP3.load(path);
    }

    protected void play(){
        myMP3.play();
    }

    protected void pause(){
        myMP3.pause();
    }

    protected void stop(){
        myMP3.stop();
    }

    protected MP3Player.MP3PlayerState getState(){
        return myMP3.getState();
    }

    protected int getProgress(){
        return myMP3.getProgress();
    }

    protected int getDuration(){
        return myMP3.getDuration();
    }

}
