package com.example.recordsound;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;


import android.Manifest;
import android.app.Activity;
import android.content.ContentResolver;
import android.content.ContentUris;
import android.content.ContentValues;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.graphics.Color;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.media.MediaRecorder;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.MediaStore;
import android.provider.Settings;
import android.util.Log;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import com.example.recordsound.components.CommandFormat;
import com.example.recordsound.components.CommonUtil;
import com.example.recordsound.components.PermissionsUtil;
import com.example.recordsound.service.AudioService;
import com.example.recordsound.service.ForegroundRecordService;
import com.example.recordsound.service.PlaySoundService;
import com.example.recordsound.service.RecordSoundService;
import com.example.recordsound.vo.PermissionVO;
import com.google.android.material.snackbar.Snackbar;

import java.io.File;
import java.io.IOException;
import java.security.Permissions;
import java.text.SimpleDateFormat;
import java.util.ArrayList;

import java.util.Calendar;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.ListIterator;
import java.util.Locale;
import java.util.Map;

public class MainActivity extends AppCompatActivity {

    private static final String TAG_PREFIX = "MZ_";
    private static final int AUDIO_PERMISSION_REQUEST_CD = 100;
    private static final int READ_EXTERNAL_STORAGE_PERMISSION_REQUEST_CD = 101;
    private static final int WRITE_EXTERNAL_STORAGE_PERMISSION_REQUEST_CD = 102;
    private static final int MODIFY_AUDIO_SETTINGS_PERMISSION_REQUEST_CD = 103;

    ArrayList<PermissionVO> permissionList = new ArrayList<>();

    MediaRecorder recorder;
    MediaPlayer mMediaPlayer = null;

    long lastMediaId;

    File audiofile = null;
    static final String TAG = TAG_PREFIX + MainActivity.class.getSimpleName();

    Button btnStartRecording,btnStopRecording,playButton,listButton, startSvcButton, stopSvcButton;

    TextView txtV1, txtV2;
    View mainLayout;

    private static String fileName = null;

    private boolean hasAudioPermission = false;
    private boolean hasAudioSettingPermission = false;
    private boolean hasReadExternalPermission = false;
    private boolean hasWriteExternalPermission = false;


    private ActivityResultLauncher<String> requestPermissionLauncher = registerForActivityResult(new ActivityResultContracts.RequestPermission(), isGranted -> {
        if(isGranted){

        } else {

        }
    });

    private ActivityResultLauncher<String[]> requestPermissionsLauncher = registerForActivityResult(new ActivityResultContracts.RequestMultiplePermissions(), permissions -> {
        Map<String, Boolean> map = permissions;

        Boolean isGranted = false;

        for(Map.Entry<String, Boolean> elem : map.entrySet()){
            Log.d(TAG, "requestPermissionsLauncher > " + elem.getKey() + " / " + elem.getValue());

            //권한처리가 안될경우 메뉴얼로 설정할수 있도록 권한 설정화면이동
            isGranted = elem.getValue();
        }
    });



    private void populatePermissionResult(String permission, Integer isGranted){

        Boolean isPermission = (isGranted != PackageManager.PERMISSION_DENIED) ? true : false;

        switch (permission){
            case Manifest.permission.RECORD_AUDIO:
                hasAudioPermission = isPermission;
                break;
            case Manifest.permission.MODIFY_AUDIO_SETTINGS:
                hasAudioSettingPermission = isPermission;
                break;
            case Manifest.permission.READ_EXTERNAL_STORAGE:
                hasReadExternalPermission = isPermission;
                break;
            case Manifest.permission.WRITE_EXTERNAL_STORAGE:
                hasWriteExternalPermission = isPermission;
                break;
            default:
                break;
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults ){
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        //Log.d(TAG, "onRequestPermissionsResult >>>>>  " + permissions[0]);

        for(Integer i=0; i<permissions.length; i++){
            populatePermissionResult(permissions[i], grantResults[i]);
        }
    }




    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_main);

        btnStartRecording = (Button) findViewById(R.id.button1);
        btnStopRecording = (Button) findViewById(R.id.button2);
        listButton = (Button) findViewById(R.id.btn_list);
        playButton = (Button) findViewById(R.id.btn_play);
        startSvcButton = (Button) findViewById(R.id.btnStartSvc);
        stopSvcButton = (Button) findViewById(R.id.btnStopSvc);
        txtV1 = (TextView) findViewById(R.id.textView1);
        txtV2 = (TextView) findViewById(R.id.textView2);
        mainLayout = findViewById(R.id.main_layout);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            initLayout();
        }


        /**
         * Event Button Start Recording
         */
        btnStartRecording.setOnClickListener(new View.OnClickListener(){
            @Override
            public void onClick(View view){

                //Check Permission MIC & External Storage (Read, Write)
                if(checkAudioPermission() && checkExternalStoragePermission()){
                    startRecordSoundService();
                } else {
                    openPermissionNeedDialog(MainActivity.this);
                }
            }
        });

        btnStopRecording.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                //stopRecording(view);
                stopRecordSoundService();
            }
        });

        listButton.setOnClickListener(new View.OnClickListener(){
            @Override
            public void onClick(View view){
                gotoRecordList();
            }
        });

        playButton.setOnClickListener(new View.OnClickListener(){
            @Override
            public void onClick(View view){
                //Log.d("Media", "start play audio.......");
                //Log.d("audioID", Long.toString(lastMediaId));
                /*if(Long.toString(lastMediaId) != ""){
                    try{
                        playAudio(lastMediaId);
                    } catch (IOException ex){
                        Log.e("PLAY Audio", ex.getMessage());
                    }
                }*/

                startPlayAudioService();
            }
        });


        startSvcButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {


            }
        });

        stopSvcButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                stopRecordSoundService();
            }
        });
    }


    private void openPermissionNeedDialog(Context context){
        try{

            PermissionsUtil permissionsUtil = new PermissionsUtil();
            CommonUtil.openPositiveNegativeDialog(context, context.getString(R.string.P002), context.getString(R.string.P003, ""), context.getString(R.string.C001), context.getString(R.string.C002), new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    //startIntentPermissionSetting(context);
                    permissionsUtil.startIntentPermissionSetting(context);
                }
            }, new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {

                    Toast.makeText(context, R.string.P002, Toast.LENGTH_LONG).show();
                }
            });
        } catch (Exception ex){
            Log.e(TAG, ex.getMessage());
        }
    }


    private void initLayout(){
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            Activity activity = (Activity)this;
            Window window = activity.getWindow();
            window.addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS);
            window.setStatusBarColor(Color.rgb(22, 0, 219));
        }

        //mainLayout.setBackgroundColor(Color.rgb(255, 255, 229));
    }

    private void startRecordSoundService(){
        Log.d(TAG, "startRecordSoundService");
        //Intent serviceIntent = new Intent(this, ForegroundRecordService.class);
        //Intent serviceIntent = new Intent(this, RecordSoundService.class);
        //serviceIntent.putExtra("inputExtra", "Foreground Record Service in android");

        Intent serviceIntent = new Intent(this, AudioService.class);
        serviceIntent.putExtra("CMD", CommandFormat.START_RECORD_AUDIO);
        serviceIntent.putExtra("SVC_TITLE", "Recording sound");

        //Oreo 이전 버전은 context.startService 로 이후 버전은 ContextCompat.startForegroundService 로 처리
        if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.O)
            ContextCompat.startForegroundService(this, serviceIntent);
        else
            startService(serviceIntent);

        Toast.makeText(this, R.string.msg_start_recording, Toast.LENGTH_SHORT).show();
    }

    private void startPlayAudioService(){
        Log.d(TAG, "startPlayAudioService >>> ");
        //Intent playSoundServiceIntent = new Intent(this, PlaySoundService.class);
        Intent playSoundServiceIntent = new Intent(this, AudioService.class);
        playSoundServiceIntent.putExtra("CMD", CommandFormat.PLAY_RECORD_AUDIO);
        playSoundServiceIntent.putExtra("SVC_TITLE", "start playAudio Service");

        if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.O)
            ContextCompat.startForegroundService(this, playSoundServiceIntent);
        else
            startService(playSoundServiceIntent);

        Toast.makeText(this, R.string.msg_start_playAudio, Toast.LENGTH_SHORT).show();
    }


    public void stopRecordSoundService() {
        Log.d(TAG, "Stoping the foreground-thread");
        //Intent serviceIntent = new Intent(this, ForegroundRecordService.class);
        //Intent serviceIntent = new Intent(this, RecordSoundService.class);
        Intent serviceIntent = new Intent(this, AudioService.class);
        stopService(serviceIntent);
        //getApplicationContext().stopService(serviceIntent);

        Toast.makeText(this, R.string.msg_stop_recording, Toast.LENGTH_SHORT).show();
    }



    private boolean checkAudioPermission(){

        Boolean hasAudioPermissions = true;

        try{

            ArrayList<PermissionVO> audioPermissions = new ArrayList<>();
            audioPermissions.add(new PermissionVO(AUDIO_PERMISSION_REQUEST_CD, Manifest.permission.RECORD_AUDIO, -1));
            audioPermissions.add(new PermissionVO(MODIFY_AUDIO_SETTINGS_PERMISSION_REQUEST_CD, Manifest.permission.MODIFY_AUDIO_SETTINGS, -1));

            for(PermissionVO item: audioPermissions){
                populatePermissionResult(item.getPermission(), ActivityCompat.checkSelfPermission(this, item.getPermission()));
            }

            Log.d(TAG, "this.hasAudioPermission: " + this.hasAudioPermission);
            Log.d(TAG, "this.hasAudioSettingPermission: " + this.hasAudioSettingPermission);

            if(!this.hasAudioPermission || !this.hasAudioSettingPermission){
                hasAudioPermissions = false;
                PermissionsUtil permissionsUtil = new PermissionsUtil();
                permissionsUtil.requestPermissions(this, mainLayout, audioPermissions, requestPermissionsLauncher);
            }
        } catch (Exception e){
            Log.e(TAG, e.getMessage());
        }

        return hasAudioPermissions;
    }

    private Boolean checkExternalStoragePermission(){

        Boolean hasExternalStoragePermissions = true;

        try{

            ArrayList<PermissionVO> externalStoragePermissions = new ArrayList<>();
            externalStoragePermissions.add(new PermissionVO(READ_EXTERNAL_STORAGE_PERMISSION_REQUEST_CD, Manifest.permission.READ_EXTERNAL_STORAGE, -1));
            externalStoragePermissions.add(new PermissionVO(WRITE_EXTERNAL_STORAGE_PERMISSION_REQUEST_CD, Manifest.permission.WRITE_EXTERNAL_STORAGE, -1));

            for(PermissionVO item: externalStoragePermissions){
                populatePermissionResult(item.getPermission(), ActivityCompat.checkSelfPermission(this, item.getPermission()));
            }

            Log.d(TAG, "this.hasAudioPermission: " + this.hasReadExternalPermission);
            Log.d(TAG, "this.hasAudioSettingPermission: " + this.hasWriteExternalPermission);

            if(!this.hasReadExternalPermission || !this.hasWriteExternalPermission){
                hasExternalStoragePermissions = false;
                PermissionsUtil permissionsUtil = new PermissionsUtil();
                permissionsUtil.requestPermissions(this, mainLayout, externalStoragePermissions, requestPermissionsLauncher);
            }
        } catch (Exception e){
            Log.e(TAG, e.getMessage());
        }

        return hasExternalStoragePermissions;
    }





    private void startRecord() {
        Log.d("step", "recording start .....");

        btnStartRecording.setEnabled(false);
        btnStopRecording.setEnabled(true);


        File dir = getExternalCacheDir();

        //set prefix as today date
        Date currentDate = Calendar.getInstance().getTime();
        //Log.e("1", "currentDate > " + currentDate.toString());
        SimpleDateFormat df = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
        String toDay = df.format(currentDate);

        String prefix = "[" + toDay + "]sample_";

        try{
            audiofile = File.createTempFile(prefix, ".3gp", dir);
        } catch (IOException ex){
            Log.e("runningRecord", ex.getMessage());
            return;
        }

        recorder = new MediaRecorder();
        recorder.setAudioSource(MediaRecorder.AudioSource.MIC);
        recorder.setOutputFormat(MediaRecorder.OutputFormat.THREE_GPP);
        recorder.setAudioEncoder(MediaRecorder.AudioEncoder.AMR_NB);
        recorder.setOutputFile(audiofile.getAbsolutePath());

        try{
            recorder.prepare();
        } catch (Exception ex){
            Log.e("runningRecord", "prepare recording is failed.");
        }

        recorder.start();
    }


    public void stopRecording(View view) {
        btnStartRecording.setEnabled(true);
        btnStopRecording.setEnabled(false);
        recorder.stop();
        recorder.release();


        addRecordingToMediaLibrary();
    }



    protected void addRecordingToMediaLibrary() {
        ContentValues values = new ContentValues(4);
        long current = System.currentTimeMillis();

        values.put(MediaStore.Audio.Media.TITLE, "audio" + audiofile.getName());
        values.put(MediaStore.Audio.Media.DATE_ADDED, (int) (current / 1000));
        values.put(MediaStore.Audio.Media.MIME_TYPE, "audio/3gpp");
        values.put(MediaStore.Audio.Media.DATA, audiofile.getAbsolutePath());

        ContentResolver contentResolver = getContentResolver();
        Uri base = MediaStore.Audio.Media.EXTERNAL_CONTENT_URI;
        Uri newUri = contentResolver.insert(base, values);

        String filePath = getPathFromUri(newUri);
        //Log.e("filePath", filePath);
        //txtV2.setText(filePath);
        sendBroadcast(new Intent(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE, newUri));
        Toast.makeText(this, "Added File " + newUri, Toast.LENGTH_LONG).show();

    }

    public String getPathFromUri(Uri uri){
        Cursor cursor = getContentResolver().query(uri, null, null, null, null);
        cursor.moveToNext();
        String path = cursor.getString(cursor.getColumnIndex("_data"));
        cursor.close();

        return path;

    }

    private void playAudio(long id) throws IOException {
        Uri contentUri = ContentUris.withAppendedId(MediaStore.Audio.Media.EXTERNAL_CONTENT_URI, id);

        mMediaPlayer= new MediaPlayer();
        mMediaPlayer.setAudioStreamType(AudioManager.STREAM_MUSIC);
        mMediaPlayer.setDataSource(getApplicationContext(), contentUri);

        mMediaPlayer.prepare();
        mMediaPlayer.start();
    }

    private void gotoRecordList(){
        Intent intent = new Intent(this, DisplayRecordListActivity.class);
        startActivity(intent);
    }


    private void writeSharedPermissionPreferences(String key, boolean value) {
        SharedPreferences sharedPmPref = getApplicationContext().getSharedPreferences(getString(R.string.key_pm_preference_file), Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = sharedPmPref.edit();
        editor.putBoolean(key, value);
        editor.commit();
    }

    private boolean getSharedPermissionPreferences(String key){
        SharedPreferences sharedPmPref = getApplicationContext().getSharedPreferences(getString(R.string.key_pm_preference_file), Context.MODE_PRIVATE);
        return sharedPmPref.getBoolean(key, false);
    }

}