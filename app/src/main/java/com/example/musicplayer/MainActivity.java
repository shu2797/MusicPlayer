package com.example.musicplayer;

import android.Manifest;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
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
import android.os.Build;
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
import android.widget.SeekBar;
import android.widget.TextView;

import java.io.File;
import java.net.URI;

import javax.xml.transform.URIResolver;


public class MainActivity extends AppCompatActivity {

    FloatingActionButton listFAB, playFAB, stopFAB, shareFAB;
    SeekBar seekBar;
    TextView fileName, durationText, progressText;

    Intent myIntent;

    private MusicService ms;
    private boolean isBound;

    private NotificationManager notifManager;

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

        seekBar = findViewById(R.id.seekBar);

        fileName = findViewById(R.id.fileName);
        durationText = findViewById(R.id.durationText);
        progressText = findViewById(R.id.progressText);

        ActivityCompat.requestPermissions(MainActivity.this,new String[]{Manifest.permission.READ_EXTERNAL_STORAGE},1);

        Log.d("MusicPlayer", "Storage permission ok");

        seekBar.setEnabled(false);


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


        notifManager = getSystemService(NotificationManager.class);


        //start service
//        myIntent = new Intent(this, MusicService.class);
//        bindService(myIntent, myConnection,Context.BIND_AUTO_CREATE);
//        startService(myIntent);
//        Log.d("MusicPlayer", "service started");




        //disable play button because app is started in STOPPED state
//        playFAB.setEnabled(false);
//        playFAB.setBackgroundTintList(ColorStateList.valueOf(Color.DKGRAY));

        playFAB.hide();
        stopFAB.hide();

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

                stop();

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

    public void stop(){
        ms.stop();

        notifManager.cancel(1);

        shareFAB.hide();

        //reset textviews and progress bar
        fileName.setText("");
        durationText.setText("");
        seekBar.setProgress(0);
        seekBar.setEnabled(false);

        //disable play button
//                playFAB.setEnabled(false);
//                playFAB.setBackgroundTintList(ColorStateList.valueOf(Color.DKGRAY));
        playFAB.hide();
        playFAB.setImageResource(R.drawable.ic_action_play);
        stopFAB.hide();

        notifManager.cancel(1);
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
//            if (ms.getState() != MP3Player.MP3PlayerState.STOPPED){
//                Log.d("MusicPlayer", filename);
//                fileName.setText(filename);
//            }
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

        //notification support for android SDK 26+
        if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.O){
            NotificationChannel notificationChannel = new NotificationChannel("playback", "Playback", notifManager.IMPORTANCE_LOW);
            notifManager.createNotificationChannel(notificationChannel);
        }

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

            stop();

            ms.load(filePath);


            //extract file name from full path and set in textview
            filename=filePath.substring(filePath.lastIndexOf("/")+1);
            filename=filename.substring(0, filename.lastIndexOf("."));
            fileName.setText(filename);

            //enable play button
//            playFAB.setEnabled(true);
//            playFAB.setBackgroundTintList(ColorStateList.valueOf(0xFFD81B60));
//            playFAB.show();
//            playFAB.setImageResource(R.drawable.ic_action_pause);
//            stopFAB.show();


            //Log.d("MusicPlayer", Integer.toString(min)+":"+Integer.toString(sec));
            //durationText.setText(durationString);

            //sendNotif();

            new mTask().execute(0);

        }

    }

    private class mTask extends AsyncTask<Integer, Integer, Void> {

        boolean complete = false;

        @Override
        protected void onPreExecute(){
            Log.d("MusicPlayer", "onPreExecute");
            ms.play();
            int dur = ms.getDuration();
            int min = dur/60000;
            int sec = (dur % 60000) / 1000;

            Log.d("MusicPlayer", "Duration: " + dur);

            String durationString = String.format("%02d:%02d", min, sec);
            durationText.setText(durationString);

            shareFAB.show();

            seekBar.setMax(dur);
            seekBar.setEnabled(true);

            playFAB.show();
            playFAB.setImageResource(R.drawable.ic_action_pause);
            stopFAB.show();
            //sendNotif();
        }


        @Override
        protected Void doInBackground(Integer... values) {
            Log.d("MusicPlayer", "Progress bar AsyncTask started");

            seekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
                @Override
                public void onProgressChanged(SeekBar seekBar, int i, boolean b) {
                    if((ms.getState()!=MP3Player.MP3PlayerState.STOPPED)&&(b)){
                        ms.seekTo(i);
                    }
                }

                @Override
                public void onStartTrackingTouch(SeekBar seekBar) {

                }

                @Override
                public void onStopTrackingTouch(SeekBar seekBar) {

                }
            });

            sendNotif();

            while (ms.getProgress()/100 !=  ms.getDuration()/100){
            //while (ms.getState()!=MP3Player.MP3PlayerState.STOPPED){
                this.publishProgress(ms.getProgress());
                if(ms.getState() == MP3Player.MP3PlayerState.STOPPED){
                    break;
                }
                Log.d("MPState", ms.getState().toString());
                Log.d("MPProgress", Integer.toString(ms.getProgress()));
            }
            //ms.stop();

            Log.d("MusicPlayer", "background done");


            return null;
        }


//        @Override
//        protected void onPostExecute(Void result){
//
//            Log.d("MusicPlayer", "onPostExecute");
//
//            stop();
////            //reset textviews and progress bar
////            fileName.setText("");
////            seekBar.setProgress(0);
////            seekBar.setEnabled(false);
////
//            progressText.setText("");
////            durationText.setText("");
////
////            //disable play button
//////            playFAB.setEnabled(false);
//////            playFAB.setBackgroundTintList(ColorStateList.valueOf(Color.DKGRAY));
////            playFAB.hide();
////            playFAB.setImageResource(R.drawable.ic_action_play);
////            shareFAB.hide();
////            stopFAB.hide();
//        }



        @Override
        protected void onProgressUpdate(Integer... values){
            int progress = values[0];



            //if stopped, reset progress textview
            if ((ms.getState() == MP3Player.MP3PlayerState.STOPPED) || (ms.getProgress()/100 == ms.getDuration()/100)){
                //progressText.setText("");

                Log.d("MusicPlayer", "end");
//                //reset textviews and progress bar
//                fileName.setText("");
//                seekBar.setProgress(0);
//                seekBar.setEnabled(false);
//
                progressText.setText("");
//                durationText.setText("");
//
//                //disable play button
////            playFAB.setEnabled(false);
////            playFAB.setBackgroundTintList(ColorStateList.valueOf(Color.DKGRAY));
//                playFAB.hide();
//                playFAB.setImageResource(R.drawable.ic_action_play);
//                shareFAB.hide();
//                stopFAB.hide();


                stop();
            } else {
                seekBar.setProgress(progress);

                int minP = progress/60000;
                int secP = (progress % 60000) / 1000;

                String progressString = String.format("%02d:%02d", minP, secP);
                progressText.setText(progressString);
            }
        }
    }
}