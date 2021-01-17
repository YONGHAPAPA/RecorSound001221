package com.example.recordsound.broadcast;

import android.content.BroadcastReceiver;
import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.media.MediaRecorder;
import android.net.Uri;
import android.provider.MediaStore;
import android.util.Log;
import android.widget.Toast;

import com.example.recordsound.service.ForegroundRecordService;
import com.example.recordsound.service.PlaySoundService;

import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;

public class MyBroadcastReceiver extends BroadcastReceiver {


    private static final String TAG_PREFIX = "MZ_";
    private static String TAG = TAG_PREFIX + MyBroadcastReceiver.class.getSimpleName();


    Context context;
    File audiofile;
    MediaRecorder recorder;

    @Override
    public void onReceive(Context context, Intent intent) {

        this.context = context;
        recorder = new MediaRecorder();

        StringBuilder sb = new StringBuilder();
        sb.append("Action: " + intent.getAction() + "\n");
        sb.append("URI: " + intent.toUri(Intent.URI_INTENT_SCHEME).toString() + "\n");
        String log = sb.toString();
        Log.e(TAG, log);

        switch (intent.getAction()){
            case "ACTION_START_RECORD" :
                startRecordService(context);
                break;

            case "ACTION_STOP_RECORD" :
                stopRecordService(context);
                break;

            case "ACTION_START_PLAY" :
                break;

            case "ACTION_STOP_PLAY":
                stopPlayAudioService(context);
                break;
        }


        Toast.makeText(this.context, log, Toast.LENGTH_LONG).show();
    }

    private void startRecordService(Context context) {
        Log.e(TAG, "Start Record Service from Receiver .....................");
    }

    private void stopRecordService(Context context) {
        Log.d(TAG, "Stop Record Service from Receiver .....................");
        Intent serviceIntent = new Intent(context, ForegroundRecordService.class);
        context.stopService(serviceIntent);
        Log.d(TAG, "Complete Stop Record Service from Receiver .....................");
    }

    private void stopPlayAudioService(Context context){
        Intent serviceIntent = new Intent(context, PlaySoundService.class);
        context.stopService(serviceIntent);
        Log.d(TAG, "Stop Play Audio service!!!!");

    }


}
