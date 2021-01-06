package com.example.recordsound.service;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Intent;
import android.media.AudioFormat;
import android.media.AudioManager;
import android.media.AudioRecord;
import android.media.AudioTrack;
import android.media.MediaRecorder;
import android.os.Build;
import android.os.IBinder;
import android.os.Process;
import android.util.Log;

import androidx.annotation.Nullable;
import androidx.core.app.NotificationCompat;

import com.example.recordsound.MainActivity;
import com.example.recordsound.R;
import com.example.recordsound.broadcast.MyBroadcastReceiver;

public class ForegroundRecordService extends Service {

    public static final String CHANNERL_ID = "ForegroundRecordServiceChannel";

    @Override
    public void onCreate(){
        super.onCreate();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId){

        String input = intent.getStringExtra("inputExtra");
        createNotificationChannel();

        Intent notificationIntent = new Intent(this, MainActivity.class);
        PendingIntent pendingIntent = PendingIntent.getActivity(this, 0, notificationIntent, 0);


        Intent broadCastIntent = new Intent(this, MyBroadcastReceiver.class);

        broadCastIntent.setAction("ACTION_START_RECORD");
        broadCastIntent.putExtra("EXTRA_NOTIFICATION_ID", 0);
        PendingIntent startRecordIntent = PendingIntent.getBroadcast(this, 0, broadCastIntent, 0);

        broadCastIntent.setAction("ACTION_STOP_RECORD");
        broadCastIntent.putExtra("EXTRA_NOTIFICATION_ID", 1);
        PendingIntent stopRecordIntent = PendingIntent.getBroadcast(this, 0, broadCastIntent, 0);

        Notification notification = new NotificationCompat.Builder(this, CHANNERL_ID)
                .setContentTitle("Foreground Record Service")
                .setContentText(input)
                .setSmallIcon(R.drawable.ic_sleep_18dp)
                .setContentIntent(pendingIntent)
                .addAction(R.drawable.ic_audio_wave_26, "RECORD", startRecordIntent)
                .addAction(R.drawable.ic_stop_26, "STOP", stopRecordIntent)
                .build();

        startForeground(1, notification);

        StartRecorder();

        return START_NOT_STICKY;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();

        Log.e(TAG, "onDestory >>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>");
        StopRecorder();
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent){
        return null;
    }

    private void createNotificationChannel(){
        if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.O){
            NotificationChannel serviceChannel = new NotificationChannel(
                    CHANNERL_ID,
                    "Foreground Record Service Channel",
                    NotificationManager.IMPORTANCE_DEFAULT
            );

            NotificationManager manager = getSystemService(NotificationManager.class);
            manager.createNotificationChannel(serviceChannel);
        }
    }

    private static final String TAG_PREFIX = "MZ_";
    private static String TAG = TAG_PREFIX + ForegroundRecordService.class.getSimpleName();

    //the audio recording options
    private static final int RECORDING_RATE = 44100;
    private static final int CHANNEL = AudioFormat.CHANNEL_IN_MONO;
    private static final int FORMAT = AudioFormat.ENCODING_PCM_16BIT;


    //the audio recorder
    private AudioRecord recorder;

    //the minimum buffer size needed for audio recording
    private static int BUFFER_SIZE = AudioRecord.getMinBufferSize(RECORDING_RATE, CHANNEL, FORMAT);

    // are we currently sending audio data
    private boolean currentlySendingAudio = false;
    public void StartRecorder() {
        Log.e(TAG, "Starting the audio stream");
        currentlySendingAudio = true;
        startStreaming();
    }

    public void StopRecorder(){
        Log.e(TAG, "Stoping the audio stream");
        currentlySendingAudio = false;
        recorder.release();
    }

    private void startStreaming() {
        Log.e(TAG, "Creating the buffer of size " + BUFFER_SIZE);

        Thread streamThread = new Thread(() -> {
            try{
                int rate = AudioTrack.getNativeOutputSampleRate(AudioManager.STREAM_SYSTEM);
                int bufferSize = AudioRecord.getMinBufferSize(rate, AudioFormat.CHANNEL_IN_MONO, AudioFormat.ENCODING_PCM_16BIT);
                short[] buffer = new short[bufferSize];

                android.os.Process.setThreadPriority(Process.THREAD_PRIORITY_AUDIO);
                Log.e(TAG, "Creating the AudioRecord");
                recorder = new AudioRecord(MediaRecorder.AudioSource.MIC, rate, AudioFormat.CHANNEL_IN_MONO, AudioFormat.ENCODING_PCM_16BIT, bufferSize);

                Log.e(TAG, "AudioRecord recording....");
                recorder.startRecording();

                while(currentlySendingAudio == true){
                    //read the data into the buffer
                    int readSize = recorder.read(buffer, 0, buffer.length);
                    double maxAmplitude = 0;

                    for(int i=0; i<readSize; i++){
                        if(Math.abs(buffer[i]) > maxAmplitude){
                            maxAmplitude = Math.abs(buffer[i]);
                        }
                    }

                    double db = 0;
                    if(maxAmplitude != 0){
                        db = 20.0 * Math.log10(maxAmplitude / 32767.0) + 90;
                    }

                    Log.e(TAG, "Max amplitude : " + maxAmplitude + " ; DB: " + db);
                }

                Log.e(TAG, "Audio Record finished recording");
            } catch (Exception e){
                Log.e(TAG, "Exception: " + e);
            }
        });

        //start the thread
        streamThread.start();
    }

}
