/*Main Activity contains initial music player UI with play/pause buttons, stop button, list and share button.
Notifications, seekbar, and other UI control and updates are done from here.
 */




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
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Build;
import android.os.IBinder;
import android.support.design.widget.FloatingActionButton;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.NotificationCompat;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.SeekBar;
import android.widget.TextView;



public class MainActivity extends AppCompatActivity {

    FloatingActionButton listFAB, playFAB, stopFAB, shareFAB;
    SeekBar seekBar;
    TextView fileName, durationText, progressText;

    Intent myIntent;

    private MusicService ms;
    private boolean isBound;

    private NotificationManager notifManager; //notification manager used to send notification

    String filePath; //path of selected file
    String filename; //name of file extracted from path and without extension

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        //Link all UI elements
        listFAB = findViewById(R.id.listFAB);
        playFAB = findViewById(R.id.playFAB);
        stopFAB = findViewById(R.id.stopFAB);
        shareFAB = findViewById(R.id.shareFAB);

        seekBar = findViewById(R.id.seekBar);

        fileName = findViewById(R.id.fileName);
        durationText = findViewById(R.id.durationText);
        progressText = findViewById(R.id.progressText);


        //Request permission from user to read from device storage
        ActivityCompat.requestPermissions(MainActivity.this,new String[]{Manifest.permission.READ_EXTERNAL_STORAGE},1);

        //Initial UI configuration: hide buttons since Player starts in STOPPED mode
        shareFAB.hide();
        seekBar.setEnabled(false);
        playFAB.hide();
        stopFAB.hide();

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            notifManager = getSystemService(NotificationManager.class); //assign notification manager to system
        }

        //List view button
        listFAB.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Log.d("MusicPlayer", "List view selected");
                Intent i = new Intent(getBaseContext(), ListActivity.class);
                startActivityForResult(i, 0);
            }
        });

        //Play/Pause button
        playFAB.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {

                //if in playing state, change button to pause
                if(ms.getState() == MP3Player.MP3PlayerState.PLAYING){
                    playFAB.setImageResource(R.drawable.ic_action_play);
                    //myMP3.pause();
                    ms.pause();
                    sendNotif(); //update notification
                }

                //if in paused state, change button to play
                else if (ms.getState() == MP3Player.MP3PlayerState.PAUSED){
                    playFAB.setImageResource(R.drawable.ic_action_pause);
                    //myMP3.play();
                    ms.play();
                    sendNotif(); //update notification
                }

            }
        });

        //Stop button
        stopFAB.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                stop(); //public function created so it can be accessed from ASyncTask as well
            }
        });

        //Share button
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

    //Stopping function to stop playback and update UI
    public void stop(){
        ms.stop();

        notifManager.cancel(1); //clear notification



        //reset textviews and disable seekbar bar
        fileName.setText("");
        durationText.setText("");
        seekBar.setProgress(0);
        seekBar.setEnabled(false);

        //Hide UI buttons
        playFAB.hide();
        playFAB.setImageResource(R.drawable.ic_action_play);
        stopFAB.hide();
        shareFAB.hide();
    }

    @Override
    protected void onDestroy() {
        stopService(myIntent); //stop service before destroying activity
        Log.d("MusicPlayer", "onDestroy");
        super.onDestroy();
        notifManager.cancel(1); //clear notification before destroying activity
    }

    @Override
    public void onBackPressed(){
        moveTaskToBack(true); //disable activity from being destroyed when app is minimised
    }

    @Override
    protected void onStart() {
        super.onStart();

        //Start music service
        Log.d("MusicPlayer", "Starting service");
        if(myIntent==null){
            Log.d("MusicPlayer", "intent null");
            myIntent = new Intent(this, MusicService.class);
            bindService(myIntent, myConnection, Context.BIND_AUTO_CREATE);
            startService(myIntent);
            Log.d("MusicPlayer", "Service started");
        }
    }

    private ServiceConnection myConnection = new ServiceConnection() {
        //Bind Service
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

    //function to send notification
    public void sendNotif(){
        Intent intent = getIntent();
        PendingIntent pendingIntent = PendingIntent.getActivity(getApplicationContext(), (int)System.currentTimeMillis(), intent, PendingIntent.FLAG_UPDATE_CURRENT);

        //notification channel created to support android SDK 26+
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

        notifManager.notify(1, notif); //send notification
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        //After selecting music file
        if ((requestCode == 0) && (resultCode == RESULT_OK)){
            //receive file path
            filePath = data.getExtras().getString("filePath");

            stop(); //stop current playback before loading new file

            ms.load(filePath); //load file


            //extract file name from full path and set in textview
            filename=filePath.substring(filePath.lastIndexOf("/")+1);
            filename=filename.substring(0, filename.lastIndexOf("."));
            fileName.setText(filename);

            new mTask().execute(0); //execute AsyncTask to start playback and continuously update UI

        }

    }

    private class mTask extends AsyncTask<Integer, Integer, Void> {

        //Update UI before starting playback
        @Override
        protected void onPreExecute(){
            Log.d("MusicPlayer", "onPreExecute");

            //get duration
            int dur = ms.getDuration();
            int min = dur/60000;
            int sec = (dur % 60000) / 1000;

            Log.d("MusicPlayer", "Duration: " + dur);

            //format duration into MM:SS for  UI
            String durationString = String.format("%02d:%02d", min, sec);
            durationText.setText(durationString);

            //Enable UI buttons and initialise seek bar
            seekBar.setMax(dur);
            seekBar.setEnabled(true);

            playFAB.show();
            playFAB.setImageResource(R.drawable.ic_action_pause);
            stopFAB.show();
            shareFAB.show();

            //start playback and send notification
            ms.play();
            sendNotif();
        }


        @Override
        protected Void doInBackground(Integer... values) {
            Log.d("MusicPlayer", "Seek bar AsyncTask started");

            //seek bar listener
            seekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
                @Override
                public void onProgressChanged(SeekBar seekBar, int i, boolean b) {
                    //When user wants to change position at which music is being played
                    if((ms.getState()!=MP3Player.MP3PlayerState.STOPPED)&&(b)){
                        ms.seekTo(i);
                    }
                }
            });


            //Loop until music playback is completed
            while (ms.getProgress()/100 !=  ms.getDuration()/100){ //Needed to divide by 100 for system to be able to catch a point where progress = duration
                this.publishProgress(ms.getProgress());

                //if stop button is pressed, break from loop
                if(ms.getState() == MP3Player.MP3PlayerState.STOPPED){
                    break;
                }

                Log.d("MPState", ms.getState().toString());
                Log.d("MPProgress", Integer.toString(ms.getProgress()));
            }

            Log.d("MusicPlayer", "Loop exited");

            return null;
        }

        //when progress of playback is changed
        @Override
        protected void onProgressUpdate(Integer... values){
            int progress = values[0];

            //if stopped or music playback is complete
            if ((ms.getState() == MP3Player.MP3PlayerState.STOPPED) || (ms.getProgress()/100 == ms.getDuration()/100)){

                Log.d("MusicPlayer", "Reset UI");

                //reset progress textview and call stop() function to reset UI
                progressText.setText("");
                stop();
            } else { //if still playing
                seekBar.setProgress(progress); //update progress of seek bar

                //format progress into MM:SS for  UI and update
                int minP = progress/60000;
                int secP = (progress % 60000) / 1000;
                String progressString = String.format("%02d:%02d", minP, secP);
                progressText.setText(progressString);
            }
        }
    }
}