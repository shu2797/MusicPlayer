package com.example.musicplayer;

import android.Manifest;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.os.IBinder;
import android.support.design.widget.FloatingActionButton;
import android.support.v4.app.ActivityCompat;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.ProgressBar;
import android.widget.SeekBar;
import android.widget.TextView;


public class MainActivity extends AppCompatActivity {

    FloatingActionButton listFAB, playFAB, stopFAB;
    ProgressBar progressBar;
    TextView fileName, durationText, progressText;


    private MusicService ms;
    private boolean isBound;

    String filePath;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        listFAB = findViewById(R.id.listFAB);
        playFAB = findViewById(R.id.playFAB);
        stopFAB = findViewById(R.id.stopFAB);

        progressBar = findViewById(R.id.progressBar);

        fileName = findViewById(R.id.fileName);
        durationText = findViewById(R.id.durationText);
        progressText = findViewById(R.id.progressText);

        ActivityCompat.requestPermissions(MainActivity.this,new String[]{Manifest.permission.READ_EXTERNAL_STORAGE},1);

        Log.d("MusicPlayer", "Storage permission ok");


        //start service
        Intent myIntent = new Intent(this, MusicService.class);
        bindService(myIntent, myConnection,Context.BIND_AUTO_CREATE);
        startService(myIntent);
        Log.d("MusicPlayer", "service started");


        //disable play button because app is started in STOPPED state
        playFAB.setEnabled(false);
        playFAB.setBackgroundTintList(ColorStateList.valueOf(Color.DKGRAY));

        listFAB.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Log.d("MusicPlayer", "List view selected");
                Intent i = new Intent(getBaseContext(), ListActivity.class);
                startActivityForResult(i, 0);
            }
        });

        playFAB.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {

                //if in playing state, change button to pause
                if(ms.getState() == MP3Player.MP3PlayerState.PLAYING){
                    playFAB.setImageResource(R.drawable.ic_action_play);
                    //myMP3.pause();
                    ms.pause();
                }

                //if in paused state, change button to play
                else if (ms.getState() == MP3Player.MP3PlayerState.PAUSED){
                    playFAB.setImageResource(R.drawable.ic_action_pause);
                    //myMP3.play();
                    ms.play();
                }

            }
        });

        stopFAB.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                ms.stop();
                //myMP3.stop();

                //disable play button
                playFAB.setEnabled(false);
                playFAB.setBackgroundTintList(ColorStateList.valueOf(Color.DKGRAY));
                playFAB.setImageResource(R.drawable.ic_action_play);
            }
        });
    }

    private ServiceConnection myConnection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName componentName, IBinder service) {
            MusicService.mBinder binder = (MusicService.mBinder) service;
            ms = binder.getService();
            isBound = true;
            Log.d("MusicPlayer", "isBound = true");
        }

        @Override
        public void onServiceDisconnected(ComponentName componentName) {
            isBound = false;
            Log.d("MusicPlayer", "isBound = false");
        }
    };

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if ((requestCode == 0) && (resultCode == RESULT_OK)){
            //receive path
            filePath = data.getExtras().getString("filePath");

            ms.stop();

            ms.load(filePath);

            String filename=filePath.substring(filePath.lastIndexOf("/")+1);
            fileName.setText(filename);

//            //stop previous file before loading new one
//            myMP3.stop();
//
//            //load file
//            myMP3.load(filePath);
//
//            Log.d("MusicPlayer", Integer.toString(myMP3.getDuration()));
//            Log.d("MusicPlayer", Integer.toString(myMP3.getProgress()));
//
            //enable play button
            playFAB.setEnabled(true);
            playFAB.setBackgroundTintList(ColorStateList.valueOf(0xFFD81B60));
            playFAB.setImageResource(R.drawable.ic_action_pause);


            int dur = ms.getDuration();
            Log.d("MusicPlayer", "Duration: " + dur);
            progressBar.setMax(dur);

            int min = dur/60000;
            int sec = (dur - (min*60000)) / 1000;
            //Log.d("MusicPlayer", Integer.toString(min)+":"+Integer.toString(sec));
            durationText.setText(Integer.toString(min)+":"+Integer.toString(sec));
        }

    }

}