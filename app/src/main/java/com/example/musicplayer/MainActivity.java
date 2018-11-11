package com.example.musicplayer;

import android.Manifest;
import android.app.Notification;
import android.app.PendingIntent;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.UriMatcher;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.IBinder;
import android.support.design.widget.FloatingActionButton;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.NotificationCompat;
import android.support.v4.app.NotificationManagerCompat;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.ProgressBar;
import android.widget.TextView;

import java.io.File;
import java.net.URI;

import javax.xml.transform.URIResolver;


public class MainActivity extends AppCompatActivity {

    FloatingActionButton listFAB, playFAB, stopFAB, shareFAB;
    ProgressBar progressBar;
    TextView fileName, durationText, progressText;

    Intent myIntent;

    private MusicService ms;
    private boolean isBound;

    private NotificationManagerCompat notifManager;

    String filePath;
    String filename;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        listFAB = findViewById(R.id.listFAB);
        playFAB = findViewById(R.id.playFAB);
        stopFAB = findViewById(R.id.stopFAB);
        shareFAB = findViewById(R.id.shareFAB);
        shareFAB.hide();

        progressBar = findViewById(R.id.progressBar);

        fileName = findViewById(R.id.fileName);
        durationText = findViewById(R.id.durationText);
        progressText = findViewById(R.id.progressText);

        ActivityCompat.requestPermissions(MainActivity.this,new String[]{Manifest.permission.READ_EXTERNAL_STORAGE},1);

        Log.d("MusicPlayer", "Storage permission ok");


//        if (Intent.ACTION_VIEW.equals(getIntent().getAction()))
//        {
//            Log.d("MusicPlayer", "open from downloads");
//
//
//            Log.d("MusicPlayer", getIntent().getData().toString());
//            File f = new File(getIntent().getData().getPath());
//            String s = f.getPath();
//            Log.d("MusicPlayer", s);
//            //ms.load(Uri.(getIntent().getData()));
//            // do what you want with the file...
//        }


        notifManager = NotificationManagerCompat.from(this);


        //start service
//        myIntent = new Intent(this, MusicService.class);
//        bindService(myIntent, myConnection,Context.BIND_AUTO_CREATE);
//        startService(myIntent);
//        Log.d("MusicPlayer", "service started");


        try{
            if (ms.getState() != MP3Player.MP3PlayerState.STOPPED){
                Log.d("MusicPlayer", "sfa");
                fileName.setText(filename);
            }
        } catch (Exception e){}



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
                    sendNotif();
                }

                //if in paused state, change button to play
                else if (ms.getState() == MP3Player.MP3PlayerState.PAUSED){
                    playFAB.setImageResource(R.drawable.ic_action_pause);
                    //myMP3.play();
                    ms.play();
                    sendNotif();
                }

            }
        });

        stopFAB.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                ms.stop();

                notifManager.cancel(1);

                shareFAB.hide();

                //reset textviews and progress bar
                fileName.setText("");
                durationText.setText("");
                progressBar.setProgress(0);

                //disable play button
                playFAB.setEnabled(false);
                playFAB.setBackgroundTintList(ColorStateList.valueOf(Color.DKGRAY));
                playFAB.setImageResource(R.drawable.ic_action_play);
            }
        });

        shareFAB.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent shareIntent = new Intent(Intent.ACTION_SEND);
                shareIntent.setType("audio/*");
                Log.d("MusicPlayer", filePath);
                shareIntent.putExtra(Intent.EXTRA_STREAM, Uri.parse(filePath));
                startActivity(Intent.createChooser(shareIntent, "Share File Using"));
            }
        });
    }


    @Override
    protected void onDestroy() {
        stopService(myIntent);
        Log.d("MusicPlayer", "onDestroy");
        super.onDestroy();
        notifManager.cancel(1);
    }

    @Override
    public void onBackPressed(){
        moveTaskToBack(true);
    }

    @Override
    protected void onStart() {
        super.onStart();
        Log.d("MusicPlayer", "onStart");
        if(myIntent==null){
            Log.d("MusicPlayer", "intent null");
            myIntent = new Intent(this, MusicService.class);
            bindService(myIntent, myConnection, Context.BIND_AUTO_CREATE);
            startService(myIntent);
        }
    }

    private ServiceConnection myConnection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName componentName, IBinder service) {
            MusicService.mBinder binder = (MusicService.mBinder) service;
            ms = binder.getService();
            isBound = true;
            Log.d("MusicPlayer", "isBound = true");
            if (ms.getState() != MP3Player.MP3PlayerState.STOPPED){
                Log.d("MusicPlayer", filename);
                fileName.setText(filename);
            }
        }

        @Override
        public void onServiceDisconnected(ComponentName componentName) {
            isBound = false;
            Log.d("MusicPlayer", "isBound = false");
        }
    };



    public void sendNotif(){
        Intent intent = getIntent();
        PendingIntent pendingIntent = PendingIntent.getActivity(getApplicationContext(), (int)System.currentTimeMillis(), intent, PendingIntent.FLAG_UPDATE_CURRENT);


        Notification notif = new NotificationCompat.Builder(this, "playback")
                .setSmallIcon(R.drawable.ic_audio)
                .setContentTitle(filename)
                .setContentText(ms.getState().toString())
                .setPriority(NotificationCompat.PRIORITY_LOW)
                .setContentIntent(pendingIntent)
                .setOngoing(true)
                .build();

        notifManager.notify(1, notif);
    }




    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if ((requestCode == 0) && (resultCode == RESULT_OK)){
            //receive path
            filePath = data.getExtras().getString("filePath");

            ms.stop();

            ms.load(filePath);

            shareFAB.show();

            //extract file name from full path and set in textview
            filename=filePath.substring(filePath.lastIndexOf("/")+1);
            fileName.setText(filename);

            //enable play button
            playFAB.setEnabled(true);
            playFAB.setBackgroundTintList(ColorStateList.valueOf(0xFFD81B60));
            playFAB.setImageResource(R.drawable.ic_action_pause);


            int dur = ms.getDuration();
            Log.d("MusicPlayer", "Duration: " + dur);
            progressBar.setMax(dur);

            int min = dur/60000;
            int sec = (dur % 60000) / 1000;

            String durationString = String.format("%02d:%02d", min, sec);

            //Log.d("MusicPlayer", Integer.toString(min)+":"+Integer.toString(sec));
            durationText.setText(durationString);

            sendNotif();

            new mTask().execute(0);
        }

    }

    private class mTask extends AsyncTask<Integer, Integer, Void> {
        @Override
        protected Void doInBackground(Integer... values) {
            Log.d("MusicPlayer", "Progress bar AsyncTask started");
            int progress = ms.getProgress();
            while (progress !=  ms.getDuration()){
                progress = ms.getProgress();
                progressBar.setProgress(progress);

                this.publishProgress(progress);
            }
            return null;
        }

        @Override
        protected void onProgressUpdate(Integer... values){
            int progress = values[0];

            int minP = progress/60000;
            int secP = (progress % 60000) / 1000;

            String progressString = String.format("%02d:%02d", minP, secP);
            progressText.setText(progressString);

            //if stopped, reset progress textview
            if (progress == 0){
                progressText.setText("");
            }
        }
    }
}