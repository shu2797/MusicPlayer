package com.example.musicplayer;

import android.Manifest;
import android.content.Intent;
import android.graphics.Color;
import android.support.design.widget.FloatingActionButton;
import android.support.v4.app.ActivityCompat;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;

import java.io.Serializable;

public class MainActivity extends AppCompatActivity {

    FloatingActionButton listFAB;

    MP3Player myMP3 = new MP3Player();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        listFAB = findViewById(R.id.listFAB);

        ActivityCompat.requestPermissions(MainActivity.this,new String[]{Manifest.permission.READ_EXTERNAL_STORAGE},1);

        Log.d("MusicPlayer", "Storage permission ok");

        listFAB.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Log.d("MusicPlayer", "List view selected");
                Intent i = new Intent(getBaseContext(), ListActivity.class);
                i.putExtra("mp3", (Serializable)myMP3);
                startActivity(i);
            }
        });
    }



}
