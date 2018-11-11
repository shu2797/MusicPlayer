package com.example.musicplayer;

import android.media.AudioManager;
import android.media.MediaPlayer;
import android.util.Log;
import java.io.IOException;

/**
 * Created by pszmdf on 06/11/16.
 */
public class MP3Player {

    protected MediaPlayer mediaPlayer;
    protected MP3PlayerState state;
    protected String filePath;

    public enum MP3PlayerState {
        ERROR,
        PLAYING,
        PAUSED,
        STOPPED,
        LOADED
    }

    public MP3Player() {
        this.state = MP3PlayerState.STOPPED;
    }

    public MP3PlayerState getState() {
        return this.state;
    }

    public void load(String filePath) {
        this.filePath = filePath;
        mediaPlayer = new MediaPlayer();
        mediaPlayer.setAudioStreamType(AudioManager.STREAM_MUSIC);

        try{
            mediaPlayer.setDataSource(filePath);
            mediaPlayer.prepare();
        } catch (IOException e) {
            Log.e("MusicPlayer", e.toString());
            e.printStackTrace();
            this.state = MP3PlayerState.ERROR;
            return;
        } catch (IllegalArgumentException e) {
            Log.e("MusicPlayer", e.toString());
            e.printStackTrace();
            this.state = MP3PlayerState.ERROR;
            return;
        }

        this.state = MP3PlayerState.LOADED;
        //mediaPlayer.start();
    }

    public String getFilePath() {
        return this.filePath;
    }

    public int getProgress() {
        if(mediaPlayer!=null) {
            if(this.state == MP3PlayerState.PAUSED || this.state == MP3PlayerState.PLAYING)
            return mediaPlayer.getCurrentPosition();
        }
        return 0;
    }

    public int getDuration() {
        if(mediaPlayer!=null)
            if(this.state == MP3PlayerState.PAUSED || this.state == MP3PlayerState.PLAYING)
                return mediaPlayer.getDuration();
        return 0;
    }

    public void play() {
        if((this.state == MP3PlayerState.PAUSED) || (this.state == MP3PlayerState.LOADED)) {
            mediaPlayer.start();
            this.state = MP3PlayerState.PLAYING;
        }
    }

    public void pause() {
        if(this.state == MP3PlayerState.PLAYING) {
            mediaPlayer.pause();
            state = MP3PlayerState.PAUSED;
        }
    }

    public void stop() {
        if(mediaPlayer!=null) {
            if(mediaPlayer.isPlaying())
                mediaPlayer.stop();
            state = MP3PlayerState.STOPPED;
            mediaPlayer.reset();
            mediaPlayer.release();
            mediaPlayer = null;
        }
    }

    public void seekTo(int i){
        mediaPlayer.seekTo(i);
    }
}
