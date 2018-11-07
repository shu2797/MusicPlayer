package com.example.musicplayer;

import android.Manifest;
import android.content.Intent;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.media.MediaMetadataRetriever;
import android.support.design.widget.FloatingActionButton;
import android.support.v4.app.ActivityCompat;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.TextView;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;

public class MainActivity extends AppCompatActivity {

    MediaMetadataRetriever mmr;
    FloatingActionButton listFAB, playFAB, stopFAB;
    TextView fileName;

    MP3Player myMP3 = new MP3Player();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        listFAB = findViewById(R.id.listFAB);
        playFAB = findViewById(R.id.playFAB);
        stopFAB = findViewById(R.id.stopFAB);

        fileName = findViewById(R.id.fileName);

        ActivityCompat.requestPermissions(MainActivity.this,new String[]{Manifest.permission.READ_EXTERNAL_STORAGE},1);

        Log.d("MusicPlayer", "Storage permission ok");

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
                if(myMP3.getState() == MP3Player.MP3PlayerState.PLAYING){
                    playFAB.setImageResource(R.drawable.ic_action_play);
                    myMP3.pause();
                } else if (myMP3.getState() == MP3Player.MP3PlayerState.PAUSED){
                    playFAB.setImageResource(R.drawable.ic_action_pause);
                    myMP3.play();
                }

            }
        });

        stopFAB.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                myMP3.stop();

                //disable play button
                playFAB.setEnabled(false);
                playFAB.setBackgroundTintList(ColorStateList.valueOf(Color.DKGRAY));
            }
        });
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if ((requestCode == 0) && (resultCode == RESULT_OK)){
            //receive path
            String filePath = data.getExtras().getString("filePath");

            //stop previous file before loading new one
            myMP3.stop();

            //load file
            myMP3.load(filePath);


            try{
                mmr.setDataSource(filePath);
                //fileName.setText(mmr.extractMetadata(MediaMetadataRetriever.METADATA_KEY_ALBUM));
                //Log.d("MusicPlayer", mmr.extractMetadata(MediaMetadataRetriever.METADATA_KEY_TITLE));
            } catch (Exception e){
                Log.d("MusicPlayer", filePath);
                Log.d("MusicPlayer", e.toString());
            }

            Log.d("MusicPlayer", Integer.toString(myMP3.getDuration()));
            Log.d("MusicPlayer", Integer.toString(myMP3.getProgress()));

            //enable play button
            playFAB.setEnabled(true);
            playFAB.setBackgroundTintList(ColorStateList.valueOf(0xFFD81B60));
            playFAB.setImageResource(R.drawable.ic_action_pause);
        }

    }
}