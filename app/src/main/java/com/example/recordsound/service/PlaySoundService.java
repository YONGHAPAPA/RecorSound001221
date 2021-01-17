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
import android.os.Build;
import android.os.Environment;
import android.os.IBinder;
import android.util.Log;

import androidx.annotation.Nullable;
import androidx.core.app.NotificationCompat;

import com.example.recordsound.MainActivity;
import com.example.recordsound.R;
import com.example.recordsound.broadcast.MyBroadcastReceiver;

import java.io.DataInputStream;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;

public class PlaySoundService extends Service {

    private static final String TAG_PREFIX = "MZ_";
    private static final String TAG = TAG_PREFIX + PlaySoundService.class.getSimpleName();
    public static final String PLAY_AUDIO_CHANNEL_ID = "PLAY_AUDIO_CHANNEL";

    //the audio recording options
    private static final int RECORDING_RATE = 44100;
    //private static final int CHANNEL = AudioFormat.CHANNEL_IN_MONO; // MONO로 처리하면 오류발생함. 이유는?
    private static final int CHANNEL_MONO = AudioFormat.CHANNEL_IN_MONO;
    private static final int CHANNEL_STEREO = AudioFormat.CHANNEL_IN_STEREO;
    private static final int FORMAT = AudioFormat.ENCODING_PCM_16BIT;

    //buffersize 크기가 다를경우 녹음을질 체크!
    private static int BUFFER_SIZE = AudioTrack.getMinBufferSize(RECORDING_RATE, CHANNEL_STEREO, FORMAT); //14144
    //private static int BUFFER_SIZE = AudioTrack.getMinBufferSize(RECORDING_RATE, CHANNEL_MONO, FORMAT); //-2
    //private static int BUFFER_SIZE = AudioRecord.getMinBufferSize(RECORDING_RATE, CHANNEL_MONO, FORMAT);  //3584

    AudioTrack audioTrack = null;
    Thread playAudioThread = null;
    private boolean isPlayingAudio = false;


    @Override
    public void onCreate(){
        super.onCreate();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId){


        //BUFFER_SIZE
        Log.d(TAG, "BUFFER_SIZE: " + BUFFER_SIZE);

        String input = intent.getStringExtra("inputExtra");
        audioTrack = new AudioTrack(AudioManager.STREAM_MUSIC, RECORDING_RATE, CHANNEL_STEREO, FORMAT, BUFFER_SIZE, AudioTrack.MODE_STREAM);

        createNotificationChannel();

        Intent notificationIntent = new Intent(this, MainActivity.class);
        PendingIntent pendingIntent = PendingIntent.getActivity(this, 0, notificationIntent, 0);

        Intent broadCastIntent = new Intent(this, MyBroadcastReceiver.class);

        broadCastIntent.setAction("ACTION_START_PLAY");
        broadCastIntent.putExtra("EXTRA_NOTIFICATION_ID", 0);
        PendingIntent startPlayIntent = PendingIntent.getBroadcast(this, 0, broadCastIntent, 0);

        broadCastIntent.setAction("ACTION_STOP_PLAY");
        broadCastIntent.putExtra("EXTRA_NOTIFICATION_ID", 1);
        PendingIntent stopPlayIntent = PendingIntent.getBroadcast(this, 0, broadCastIntent, 0);

        Notification notification = new NotificationCompat.Builder(this, PLAY_AUDIO_CHANNEL_ID)
                .setContentTitle("Foreground Record Service")
                .setContentText(input)
                .setSmallIcon(R.drawable.ic_sleep_18dp)
                .setContentIntent(pendingIntent)
                //.addAction(R.drawable.ic_audio_wave_26, "RECORD", startRecordIntent)
                .addAction(R.drawable.ic_stop_26, "STOP", stopPlayIntent)
                .build();

        startForeground(1, notification);

        startAudioPlay();

        return START_NOT_STICKY;
    }



    @Override
    public void onDestroy(){
        super.onDestroy();
        stopAudioPlay();
    }


    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    private void startAudioPlay(){
        Log.d(TAG, "startAudioPlay>>>>>>>>>>>>>");
        isPlayingAudio = true;

        playAudioThread = new Thread(() -> {

            byte[] buffer = new byte[BUFFER_SIZE];
            FileInputStream fileInputStream = null;

            try{
                String filePath = Environment.getExternalStorageDirectory().getAbsolutePath() + "/recordsound.3gpp";
                //String filePath = Environment.getExternalStorageDirectory().getAbsolutePath() + "/samplesound.3pgg";
                fileInputStream = new FileInputStream(filePath);

            } catch (FileNotFoundException e){
                Log.e(TAG, e.getMessage());
                e.printStackTrace();
            }

            DataInputStream dataInputStream = new DataInputStream(fileInputStream);
            audioTrack.play();

            while(isPlayingAudio){
                //Log.d(TAG, "isPlayingAudio: " + isPlayingAudio);

                try{
                    int result = dataInputStream.read(buffer, 0, BUFFER_SIZE);
                    Log.d(TAG, "Play Audio result: " + result);

                    if(result <= 0){
                        isPlayingAudio = false;
                        stopSelf();
                        break;
                    }

                    audioTrack.write(buffer, 0, result);

                } catch (IOException e){
                    Log.e(TAG, e.getMessage());
                    e.printStackTrace();
                }
            }

            audioTrack.stop();
            audioTrack.release();
            audioTrack = null;

            try{
                dataInputStream.close();
                fileInputStream.close();
            } catch (IOException e){
                Log.e(TAG, e.getMessage());
                e.printStackTrace();
            }
        });

        playAudioThread.start();
    }

    private void stopAudioPlay(){
        isPlayingAudio = false;
        playAudioThread.interrupt();
    }

    private void createNotificationChannel(){
        if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.O){
            NotificationChannel serviceChannel = new NotificationChannel(
                    PLAY_AUDIO_CHANNEL_ID,
                    "Play Audio Service Channel",
                    NotificationManager.IMPORTANCE_DEFAULT
            );

            NotificationManager manager = getSystemService(NotificationManager.class);
            manager.createNotificationChannel(serviceChannel);
        }
    }
}
