package com.example.recordsound;

import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.database.Cursor;
import android.os.Bundle;
import android.provider.MediaStore;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ListView;

import java.util.ArrayList;

public class DisplayRecordListActivity extends AppCompatActivity {

    public static final String EXTRA_RECORD_ID = "com.example.recordsound.RECORD_ID";
    RecordListAdapter adapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_display_record_list);

        adapter = new RecordListAdapter();
        ListView lv = (ListView) findViewById(R.id.recordListView);
        lv.setAdapter(adapter);

        populateRecordList();

        lv.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                //Log.d("onclick", Long.toString(id));
                RecordItem item = (RecordItem) parent.getItemAtPosition(position);
                //Log.d("record Id", Long.toString(item.getId()));
                //Log.d("display Name", item.getDisplayName());
                gotoRecordDetailView(item.getId());
            }
        });
    }


    private void gotoRecordDetailView(Long id){
        Intent intent = new Intent(this, DetailViewRecordItem.class);
        intent.putExtra(EXTRA_RECORD_ID, Long.toString(id));
        startActivity(intent);
        this.finish();
    }


    private void populateRecordList(){

        Log.d("populateRecordList", "start..");

        String projection[] = new String[] {
                MediaStore.Audio.Media._ID,
                MediaStore.Audio.Media.DISPLAY_NAME,
                MediaStore.Audio.Media.DURATION,
                MediaStore.Audio.Media.SIZE,
                MediaStore.Audio.Media.DATE_ADDED
        };

        String selection = MediaStore.Audio.Media.DISPLAY_NAME + " like '%pcm'";

        Cursor cursor = getContentResolver().query(
                MediaStore.Audio.Media.EXTERNAL_CONTENT_URI,
                projection,
                selection,
                null,
                null
        );

        while(cursor.moveToNext()){
            String id = cursor.getString(cursor.getColumnIndex(MediaStore.Audio.Media._ID));
            String displayNm = cursor.getString(cursor.getColumnIndex(MediaStore.Audio.Media.DISPLAY_NAME));
            String duration = cursor.getString(cursor.getColumnIndex(MediaStore.Audio.Media.DURATION));
            String size = cursor.getString(cursor.getColumnIndex(MediaStore.Audio.Media.SIZE));
            String date = cursor.getString(cursor.getColumnIndex(MediaStore.Audio.Media.DATE_ADDED));

            //Log.d("cursor", displayNm);
            this.adapter.addItem(null, Long.parseLong(id), displayNm, date, size);
        }
    }
}
