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
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.media.MediaMetadataRetriever;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Build;
import android.os.IBinder;
import android.provider.OpenableColumns;
import android.support.design.widget.FloatingActionButton;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.NotificationCompat;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.widget.TooltipCompat;
import android.util.Log;
import android.view.View;
import android.widget.ImageView;
import android.widget.SeekBar;
import android.widget.TextView;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.InputStream;


public class MainActivity extends AppCompatActivity {

    FloatingActionButton listFAB, browseFAB, playFAB, stopFAB, shareFAB;
    SeekBar seekBar;
    TextView titleText, artistText, albumText, durationText, progressText;
    ImageView albumArt;

    Intent myIntent;

    Uri shareURI; //URI used for sharing file

    private MusicService ms;
    private boolean isBound;

    private final static String TAG = "MusicPlayer";

    MediaMetadataRetriever mmr = new MediaMetadataRetriever(); //to retrieve song details

    private NotificationManager notifManager; //notification manager used to send notification

    String filePath; //path of selected file
    String mTitle; //name of file extracted from path and without extension

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        //Link all UI elements
        listFAB = findViewById(R.id.listFAB);
        browseFAB = findViewById(R.id.browseFAB);
        playFAB = findViewById(R.id.playFAB);
        stopFAB = findViewById(R.id.stopFAB);
        shareFAB = findViewById(R.id.shareFAB);

        seekBar = findViewById(R.id.seekBar);

        titleText = findViewById(R.id.titleText);
        artistText = findViewById(R.id.artistText);
        albumText = findViewById(R.id.albumText);
        durationText = findViewById(R.id.durationText);
        progressText = findViewById(R.id.progressText);

        albumArt = findViewById(R.id.albumArt);


        //Request permission from user to read from device storage
        ActivityCompat.requestPermissions(MainActivity.this,new String[]{Manifest.permission.READ_EXTERNAL_STORAGE},1);

        //Initial UI configuration: hide buttons since Player starts in STOPPED mode
        shareFAB.hide();
        seekBar.setEnabled(false);
        playFAB.hide();
        stopFAB.hide();
        titleText.setText("Please select a file");

        //Setting tooltips for all buttons so that user can long-press on buttons to understand what they do
        TooltipCompat.setTooltipText(listFAB, "Browse from list in Music folder");
        TooltipCompat.setTooltipText(browseFAB, "Browse from storage");
        TooltipCompat.setTooltipText(shareFAB, "Share");


        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            notifManager = getSystemService(NotificationManager.class); //assign notification manager to system
        }

        //List view button
        listFAB.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Log.d(TAG, "List view selected");
                Intent i = new Intent(getBaseContext(), ListActivity.class);
                startActivityForResult(i, 0);
            }
        });

        //Browse from storage button
        browseFAB.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent nintent = new Intent(Intent.ACTION_GET_CONTENT);
                nintent.setType("audio/*");
                startActivityForResult(nintent, 10);
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
                Log.d(TAG, "Sharing");
                shareIntent.putExtra(Intent.EXTRA_STREAM, shareURI);
                startActivity(Intent.createChooser(shareIntent, "Share File Using"));
            }
        });
        Log.d(TAG, "onCreate");
    }

    //Stopping function to stop playback and update UI
    public void stop(){
        ms.stop();

        notifManager.cancel(1); //clear notification

        //reset UI
        titleText.setText("Please select a file");
        artistText.setText("");
        albumText.setText("");
        durationText.setText("");
        albumArt.setImageDrawable(getResources().getDrawable(R.mipmap.ic_album_foreground));
        seekBar.setProgress(0);
        seekBar.setEnabled(false);

        //Hide UI buttons
        playFAB.hide();
        playFAB.setImageResource(R.drawable.ic_action_play);
        stopFAB.hide();
        shareFAB.hide();

        Log.d(TAG, "Stopped");
    }

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
                .setContentTitle(mTitle)
                .setContentText(mmr.extractMetadata(MediaMetadataRetriever.METADATA_KEY_ARTIST))
                .setPriority(NotificationCompat.PRIORITY_LOW)
                .setContentIntent(pendingIntent)
                .setOngoing(true)
                .build();

        notifManager.notify(1, notif); //send notification

        Log.d(TAG, "Send notification");
    }

    public void renderUI(){
        //get duration
        int dur = ms.getDuration();
        int min = dur/60000;
        int sec = (dur % 60000) / 1000;


        //format duration into MM:SS for  UI
        String durationString = String.format("%02d:%02d", min, sec);
        durationText.setText(durationString);

        //display song details
        String nTitle = mmr.extractMetadata(MediaMetadataRetriever.METADATA_KEY_TITLE);
        mTitle = (nTitle == null)? mTitle : nTitle; //if file contains no title, use file name
        titleText.setText(mTitle);
        artistText.setText(mmr.extractMetadata(MediaMetadataRetriever.METADATA_KEY_ARTIST));
        albumText.setText(mmr.extractMetadata(MediaMetadataRetriever.METADATA_KEY_ALBUM));

        //set album art
        byte[] artBytes =  mmr.getEmbeddedPicture();
        if(artBytes!=null)
        {
            //     InputStream is = new ByteArrayInputStream(mmr.getEmbeddedPicture());
            Bitmap bm = BitmapFactory.decodeByteArray(artBytes, 0, artBytes.length);
            albumArt.setImageBitmap(bm);
        }
        else
        {
            albumArt.setImageDrawable(getResources().getDrawable(R.mipmap.ic_album_foreground));
        }

        //Enable UI buttons and initialise seek bar
        seekBar.setMax(dur);
        seekBar.setEnabled(true);
        playFAB.show();
        playFAB.setImageResource(R.drawable.ic_action_pause);
        stopFAB.show();
        shareFAB.show();

        Log.d(TAG, "Playing: " + mTitle);
        Log.d(TAG, "Duration: " + dur);
        Log.d(TAG, "UI loaded");

        //send notification
        sendNotif();
    }

    @Override
    protected void onDestroy() {
        stopService(myIntent); //stop service before destroying activity
        Log.d(TAG, "onDestroy");
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
        Log.d(TAG, "Starting service");
        if(myIntent==null){
            myIntent = new Intent(this, MusicService.class);
            bindService(myIntent, myConnection, Context.BIND_AUTO_CREATE);
            startService(myIntent);
            Log.d(TAG, "Service started");
        }
    }

    private ServiceConnection myConnection = new ServiceConnection() {
        //Bind Service
        @Override
        public void onServiceConnected(ComponentName componentName, IBinder service) {
            MusicService.mBinder binder = (MusicService.mBinder) service;
            ms = binder.getService();
            isBound = true;
            Log.d(TAG, "isBound = true");
        }

        @Override
        public void onServiceDisconnected(ComponentName componentName) {
            isBound = false;
            Log.d(TAG, "isBound = false");
        }
    };

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        //After selecting music file from list
        if ((requestCode == 0) && (resultCode == RESULT_OK)){
            //receive file path
            filePath = data.getExtras().getString("filePath");

            stop(); //stop current playback before loading new file

            ms.load(filePath); //load file


            //extract file name from full path and set in textview
            mTitle=filePath.substring(filePath.lastIndexOf("/")+1);
            mTitle=mTitle.substring(0, mTitle.lastIndexOf("."));

            mmr.setDataSource(filePath); //set source for MediaMetadataRetriever

            //URI used for sharing file
            shareURI = Uri.parse(filePath);

            new mTask().execute(0); //execute AsyncTask to start playback and continuously update UI

        }

        //After selecting music file after browsing folder
        if ((requestCode == 10) && (resultCode == RESULT_OK)){
            stop();

            ms.load(getApplicationContext(), data.getData());

            mmr.setDataSource(getApplicationContext(), data.getData()); //set source for MediaMetadataRetriever

            //URI used for sharing file
            shareURI = data.getData();

            //get file name from URI
            mTitle = getFileName(data.getData());
            mTitle=mTitle.substring(0, mTitle.lastIndexOf("."));

            new mTask().execute(0); //execute AsyncTask to start playback and continuously update UI
        }


    }

    //function to extract file name from URI
    public String getFileName(Uri uri) {
        String result = null;
        if (uri.getScheme().equals("content")) {
            Cursor cursor = getContentResolver().query(uri, null, null, null, null);
            try {
                if (cursor != null && cursor.moveToFirst()) {
                    result = cursor.getString(cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME));
                }
            } finally {
                cursor.close();
            }
        }
        if (result == null) {
            result = uri.getPath();
            int cut = result.lastIndexOf('/');
            if (cut != -1) {
                result = result.substring(cut + 1);
            }
        }
        return result;
    }


    private class mTask extends AsyncTask<Integer, Integer, Void> {

        //Update UI before starting playback
        @Override
        protected void onPreExecute(){
            Log.d(TAG, "onPreExecute");

            //start playback
            ms.play();

            renderUI(); //display music details
        }


        @Override
        protected Void doInBackground(Integer... values) {
            Log.d(TAG, "Seek bar AsyncTask started");

            //seek bar listener
            seekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
                @Override
                public void onProgressChanged(SeekBar seekBar, int i, boolean b) {
                    //When user wants to change position at which music is being played
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


            //Loop until music playback is completed
            while (ms.getProgress()/100 !=  ms.getDuration()/100){ //Needed to divide by 100 for system to be able to catch a point where progress = duration
                this.publishProgress(ms.getProgress());

                //if stop button is pressed, break from loop
                if(ms.getState() == MP3Player.MP3PlayerState.STOPPED){
                    break;
                }

                //for debugging
                Log.d("MPState", ms.getState().toString());
                Log.d("MPProgress", Integer.toString(ms.getProgress()));
            }

            Log.d(TAG, "AsyncTask over");

            return null;
        }

        //when progress of playback is changed
        @Override
        protected void onProgressUpdate(Integer... values){
            int progress = values[0];

            //if stopped or music playback is complete
            if ((ms.getState() == MP3Player.MP3PlayerState.STOPPED) || (ms.getProgress()/100 == ms.getDuration()/100)){

                Log.d(TAG, "Reset UI");

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