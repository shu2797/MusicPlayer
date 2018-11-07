package com.example.musicplayer;

import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.os.Environment;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import java.io.File;

public class ListActivity extends AppCompatActivity {

    MP3Player myMP3;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_list);

        myMP3 = (MP3Player)getIntent().getExtras().getSerializable("mp3");

        final ListView lv = (ListView) findViewById(R.id.listView);
        File musicDir = new File(Environment.getExternalStorageDirectory().getPath()+ "/Music/");
        File list[] = musicDir.listFiles();
        lv.setAdapter(new ArrayAdapter<File>(this,android.R.layout.simple_list_item_1, list));
        lv.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            public void onItemClick(AdapterView<?> myAdapter, View myView, int myItemInt, long mylng) {
                File selectedFromList =(File) (lv.getItemAtPosition(myItemInt));
                Log.d("MusicPlayer", selectedFromList.getAbsolutePath());
                // do something with selectedFromList...
            }
        });
    }
}
