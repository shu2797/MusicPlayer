/*This is the music service which is controlled by the Main Activity and
uses an instance of the MP3Player class.
 */


package com.example.musicplayer;

import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Binder;
import android.os.IBinder;
import android.util.Log;

public class MusicService extends Service {

    private final Binder mBind = new mBinder();

    MP3Player myMP3;



    public MusicService() {
        myMP3 = new MP3Player(); //initialization
    }

    @Override
    public IBinder onBind(Intent intent) {
        Log.d("MusicPlayer", "onBind");
        return mBind;
    }

    @Override
    public boolean onUnbind(Intent intent){
        Log.d("MusicPlayer", "onUnbind");
        return false;
    }

    public class mBinder extends Binder{
        MusicService getService(){
            return MusicService.this;
        }
    }

    //Functions for Music Player Service

    protected void load(String path){
        myMP3.load(path);
    }

    protected void load(Context context, Uri uri){
        myMP3.load(context, uri);
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

    protected void seekTo(int i) { myMP3.seekTo(i);}

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
