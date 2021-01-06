package com.example.recordsound;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;


import android.Manifest;
import android.content.ContentResolver;
import android.content.ContentUris;
import android.content.ContentValues;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.media.MediaRecorder;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.provider.Settings;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import com.example.recordsound.components.PermissionsUtil;
import com.example.recordsound.service.ForegroundRecordService;
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


    //private String [] permissions = {Manifest.permission.RECORD_AUDIO, Manifest.permission.READ_EXTERNAL_STORAGE, Manifest.permission.WRITE_EXTERNAL_STORAGE, Manifest.permission.MODIFY_AUDIO_SETTINGS};
    //private HashMap<Integer, String[]> permissionsMap = new HashMap<>();
    ArrayList<PermissionVO> permissionList = new ArrayList<>();

    MediaRecorder recorder;
    MediaPlayer mMediaPlayer = null;

    long lastMediaId;

    File audiofile = null;
    static final String TAG = TAG_PREFIX + MainActivity.class.getSimpleName();


    Button startButton,stopButton,playButton,listButton, startSvcButton, stopSvcButton;
    TextView txtV1, txtV2;
    View mainLayout;

    private static String fileName = null;

    //Requesting permission to Record audio
    private boolean permissionToRecordAccepted = false;
    private boolean permissionToReadExternalStorage = false;
    private boolean permissionToWriteExternalStorage = false;
    private boolean permissionToModifyAudioSettings = false;


    private boolean hasAudioPermission = false;
    private boolean hasAudioSettingPermission = false;
    private boolean hasReadExternalPermission = false;
    private boolean hasWriteExternalPermission = false;

    //private String [] permissions = {Manifest.permission.RECORD_AUDIO, Manifest.permission.READ_EXTERNAL_STORAGE, Manifest.permission.WRITE_EXTERNAL_STORAGE, Manifest.permission.MODIFY_AUDIO_SETTINGS};
    //private String [] sharedPmPrefKeys = {getString(R.string.key_pm_audio), getString(R.string.key_pm_read_external_storage), getString(R.string.key_pm_write_external_storage)};


    private void initPermissions(){
        //Init Permission variable;
        permissionList.add(new PermissionVO(AUDIO_PERMISSION_REQUEST_CD, Manifest.permission.RECORD_AUDIO, -1));
        permissionList.add(new PermissionVO(MODIFY_AUDIO_SETTINGS_PERMISSION_REQUEST_CD, Manifest.permission.MODIFY_AUDIO_SETTINGS, -1));
        permissionList.add(new PermissionVO(READ_EXTERNAL_STORAGE_PERMISSION_REQUEST_CD, Manifest.permission.READ_EXTERNAL_STORAGE, -1));
        permissionList.add(new PermissionVO(WRITE_EXTERNAL_STORAGE_PERMISSION_REQUEST_CD, Manifest.permission.WRITE_EXTERNAL_STORAGE, -1));
    }





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

        Log.d(TAG, "onRequestPermissionsResult >>>>>  " + permissions[0]);

        for(Integer i=0; i<permissions.length; i++){
            populatePermissionResult(permissions[i], grantResults[i]);
        }
    }




    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        //initPermissions();
        setContentView(R.layout.activity_main);

        startButton = (Button) findViewById(R.id.button1);
        stopButton = (Button) findViewById(R.id.button2);
        listButton = (Button) findViewById(R.id.btn_list);
        playButton = (Button) findViewById(R.id.btn_play);
        startSvcButton = (Button) findViewById(R.id.btnStartSvc);
        stopSvcButton = (Button) findViewById(R.id.btnStopSvc);

        txtV1 = (TextView) findViewById(R.id.textView1);
        txtV2 = (TextView) findViewById(R.id.textView2);

        mainLayout = findViewById(R.id.main_layout);


        startButton.setOnClickListener(new View.OnClickListener(){
            @Override
            public void onClick(View view){

                //Check Permission MIC & External Storage (Read, Write)
                if(checkAudioPermission() && checkExternalStoragePermission()){
                    //startRecordAudio();
                }
            }
        });

        stopButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                stopRecording(view);
            }
        });

        listButton.setOnClickListener(new View.OnClickListener(){
            @Override
            public void onClick(View view){
                //loadRecordingMedia();
                gotoRecordList();
            }
        });

        playButton.setOnClickListener(new View.OnClickListener(){
            @Override
            public void onClick(View view){
                //Log.d("Media", "start play audio.......");
                //Log.d("audioID", Long.toString(lastMediaId));
                if(Long.toString(lastMediaId) != ""){
                    try{
                        playAudio(lastMediaId);
                    } catch (IOException ex){
                        Log.e("PLAY Audio", ex.getMessage());
                    }
                }
            }
        });


        startSvcButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                if(checkAudioPermission() && checkExternalStoragePermission()){
                    startRecordSoundService();
                } else {

                }
            }
        });

        stopSvcButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                stopService();
            }
        });
    }

    private void startRecordSoundService(){
        Log.e(TAG, "startRecordSoundService");
        Intent serviceIntent = new Intent(this, ForegroundRecordService.class);
        serviceIntent.putExtra("inputExtra", "Foreground Record Service in android");

        ContextCompat.startForegroundService(this, serviceIntent);
        //startService(serviceIntent);
    }

    public void startService() {
        /*Intent serviceIntent = new Intent(this, MyForegroundService.class);
        *//*serviceIntent.putExtra("inputExtra", "Foreground Service Example in Android");
        ContextCompat.startForegroundService(this, serviceIntent);*//*
        startService(serviceIntent);*/

        Log.e(TAG, "Starting the foreground-thread");
        Intent serviceIntent = new Intent(this, ForegroundRecordService.class);
        serviceIntent.putExtra("inputExtra", "Foreground Record Service Example in android");
        //ContextCompat.startForegroundService(this, serviceIntent);
        startService(serviceIntent);
    }

    public void stopService() {
//        Intent serviceIntent = new Intent(this, MyForegroundService.class);
//        stopService(serviceIntent);

        Log.e(TAG, "Stoping the foreground-thread");
        Intent serviceIntent = new Intent(this, ForegroundRecordService.class);
        stopService(serviceIntent);
        //getApplicationContext().stopService(serviceIntent);
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

    public void startRecordAudio(){
        //Check Record Audio & Write external storage Permission
        if(checkAllPermissionForRecording()){
            //Log.d("complete", "check All Permission^^");
            startRecord();
        }
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


    private boolean checkAllPermissionForRecording(){
//        checkPermission(this, Manifest.permission.RECORD_AUDIO, this.REQUEST_RECORD_AUDIO_PERMISSION);
//        checkPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE, this.REQUEST_READ_EXTERNAL_STORAGE_PERMISSION);
//        checkPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE, this.REQUEST_WRITE_EXTERNAL_STORAGE_PERMISSION);
//        checkPermission(this, Manifest.permission.MODIFY_AUDIO_SETTINGS, this.REQUEST_MODIFY_AUDIO_SETTINGS_PERMISSION);

//        Log.d("startRecordAudio", "permissionToRecordAccepted > " + Boolean.toString(permissionToRecordAccepted));
//        Log.d("startRecordAudio", "permissionToReadExternalStorage > " + Boolean.toString(permissionToReadExternalStorage));
//        Log.d("startRecordAudio", "permissionToWriteExternalStorage > " + Boolean.toString(permissionToWriteExternalStorage));

        if(!permissionToRecordAccepted) {
            openDialogForAskPermission(getString(R.string.dlg_need_audio_permission));
            return false;
        }

        if(!permissionToReadExternalStorage) {
            openDialogForAskPermission(getString(R.string.dlg_need_read_ex_storage_permission));
            return false;
        }

        if(!permissionToWriteExternalStorage) {
            openDialogForAskPermission(getString(R.string.dlg_need_write_ex_storage_permission));
            return false;
        }

        if(!permissionToModifyAudioSettings) {
            openDialogForAskPermission(getString(R.string.dlg_need_modify_audio_settings_permission));
            return false;
        }

        return true;
    }

    //Check Permission & Reqeust permission if not have
    private void checkPermission(Context ctx, String permission, int requestCode){
        try{
            //Log.e("checkPermission", "start >>>>>>>>>>>>>>>>>");
            if(ActivityCompat.checkSelfPermission(ctx, permission) != PackageManager.PERMISSION_GRANTED){
                if(ActivityCompat.shouldShowRequestPermissionRationale(this, permission)){
                    //show additional rationale to user if the permission was not granted.

                    Log.d(TAG, "checkPermission >>> before Snackbar open....");
                    Snackbar.make(mainLayout, R.string.audio_access_required, Snackbar.LENGTH_INDEFINITE).setAction(R.string.ok, new View.OnClickListener(){
                        @Override
                        public void onClick(View view){
                            ActivityCompat.requestPermissions(MainActivity.this, new String[]{permission}, requestCode);
                        }
                    }).show();
                } else {

                    //이미 사용자가 권한부여 UI를 항상 안보이기로 처리한경우
                    Snackbar.make(mainLayout, R.string.audio_access_unavailable, Snackbar.LENGTH_SHORT).show();
                    ActivityCompat.requestPermissions(MainActivity.this, new String[]{permission}, requestCode);
                }
            } else {
                /*switch (requestCode){
                    case REQUEST_RECORD_AUDIO_PERMISSION:
                        permissionToRecordAccepted = true;
                        break;
                    case REQUEST_WRITE_EXTERNAL_STORAGE_PERMISSION:
                        permissionToWriteExternalStorage = true;
                        break;
                    case REQUEST_READ_EXTERNAL_STORAGE_PERMISSION:
                        permissionToReadExternalStorage = true;
                        break;
                    case REQUEST_MODIFY_AUDIO_SETTINGS_PERMISSION:
                        permissionToModifyAudioSettings = true;
                    default:
                        break;
                }*/
            }

            /*switch(requestCode){
                case REQUEST_RECORD_AUDIO_PERMISSION:
                    //Log.e("permission", "permissionToRecordAccepted: " + Boolean.toString(permissionToRecordAccepted));
                    break;
                case REQUEST_WRITE_EXTERNAL_STORAGE_PERMISSION:
                    //Log.e("permission", "permissionToWriteExternalStorage: " + Boolean.toString(permissionToWriteExternalStorage));
                    break;
                case REQUEST_READ_EXTERNAL_STORAGE_PERMISSION:
                    //Log.e("permission", "permissionToReadExternalStorage: " + Boolean.toString(permissionToReadExternalStorage));
                    break;
                case REQUEST_MODIFY_AUDIO_SETTINGS_PERMISSION:
                    //Log.e("permission", "permissionToModifyAudioSettings: " + Boolean.toString(permissionToModifyAudioSettings));
                default:
            }*/
            //Log.e("checkPermission", "end >>>>>>>>>>>>>>>>>");
        } catch(Exception e){
            Log.e("checkPermission", e.getMessage());
        }
    }


    private void openDialogForAskPermission(String dialogMessage){
        AlertDialog.Builder alertDialogBuilder = new AlertDialog.Builder(this);
        alertDialogBuilder.setTitle(getString(R.string.dlg_com_need_permission));
        alertDialogBuilder.setMessage(dialogMessage);

        alertDialogBuilder.setPositiveButton(getString(R.string.btn_open_setting), new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                Intent intent = new Intent();
                intent.setAction(Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
                Uri uri = Uri.fromParts("package", MainActivity.this.getPackageName(), null);
                intent.setData(uri);
                MainActivity.this.startActivity(intent);
            }
        });

        alertDialogBuilder.setNegativeButton(getString(R.string.btn_cancel), new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                Log.d("onClick", "cancel");
            }
        });

        AlertDialog dialog = alertDialogBuilder.create();
        dialog.show();
    }

    private void startRecord() {
        Log.d("step", "recording start .....");

        startButton.setEnabled(false);
        stopButton.setEnabled(true);


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
        startButton.setEnabled(true);
        stopButton.setEnabled(false);
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

        appendLog(txtV1, "External Content URI", newUri.toString());
        appendLog(txtV1, "filePath", filePath);

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

    public void loadRecordingMedia(){
        ArrayList audio = new ArrayList();

        String[] projection = new String[] {
                MediaStore.Audio.Media._ID,
                MediaStore.Audio.Media.DISPLAY_NAME,
                MediaStore.Audio.Media.DURATION,
                MediaStore.Audio.Media.SIZE, MediaStore.Audio.Media.DATE_ADDED
        };

        String selection = MediaStore.Audio.Media.DISPLAY_NAME + " like '%3gp'";

        Cursor cursor = getContentResolver().query(
                MediaStore.Audio.Media.EXTERNAL_CONTENT_URI,
                //new String[]{MediaStore.Audio.Media.DISPLAY_NAME},
                projection,
                selection,
                null,
                null);


        while(cursor.moveToNext()){
            String id = cursor.getString(cursor.getColumnIndex(MediaStore.Audio.Media._ID));
            String display_name = cursor.getString(cursor.getColumnIndex(MediaStore.Audio.Media.DISPLAY_NAME));
            //String content_type = cursor.getString(cursor.getColumnIndex(MediaStore.Audio.Media.CONTENT_TYPE));
            String duration = cursor.getString(cursor.getColumnIndex(MediaStore.Audio.Media.DURATION));
            String size = cursor.getString(cursor.getColumnIndex(MediaStore.Audio.Media.SIZE));
            String dtAdded = cursor.getString(cursor.getColumnIndex(MediaStore.Audio.Media.DATE_ADDED));

            //appendLog(txtV1, "media", name);

            //String media_info = id.concat(display_name.concat(duration.concat(size)));
            String media_info = id + "/ " + display_name + "/ " + duration + "/ " + size + "/ " + dtAdded + "/r/n";

            Log.e("media_info", media_info);
            lastMediaId = Long.parseLong(id);
            txtV2.append(media_info);
        }
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


    public void appendLog(TextView tv, String key, String msg){
        String txtVCnt = tv.getText().toString();
        String newCnt = "[" + key + "]" + msg;
        txtVCnt = txtVCnt + "\r\n" + newCnt + "\r\n";
        tv.setText(txtVCnt);

        //Min
    }


}