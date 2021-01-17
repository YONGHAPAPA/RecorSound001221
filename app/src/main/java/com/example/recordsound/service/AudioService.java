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
import android.os.Environment;
import android.os.IBinder;
import android.os.Process;
import android.provider.MediaStore;
import android.util.Log;

import androidx.annotation.Nullable;
import androidx.core.app.NotificationCompat;

import com.example.recordsound.MainActivity;
import com.example.recordsound.R;
import com.example.recordsound.broadcast.MyBroadcastReceiver;
import com.example.recordsound.components.CommandFormat;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;

public class AudioService extends Service {

    private static String TAG_PRX = "MZ_";
    private static String TAG = TAG_PRX + AudioService.class.getSimpleName();

    private Intent intent;
    private int flags;
    private int startId;
    private int serviceCommand = 0;



    //private static final int CHANNEL_CONFIG = AudioFormat.CHANNEL_IN_MONO;
    private static final int CHANNEL_CONFIG = AudioFormat.CHANNEL_IN_STEREO;
    private static final int AUDIO_FORMAT = AudioFormat.ENCODING_PCM_16BIT;

    private static final String NOTIFICATION_RECORD_SOUND_CHANNEL_ID = "AUDIO_SVC_CHANNEL";
    private static final String NOTIFICATION_RECORD_SOUND_CHANNEL_NAME = "Audio Service";

    private AudioRecord recorder;
    private AudioTrack audioTrack;

    //Sample Rate 는 아날로그 신호를 디지털로 변환 할 때, 1초당 몇개의 sample 을 추출할 것인가
    //private static final int SAMPLE_RATE = 44100;
    private static final int SAMPLE_RATE = AudioTrack.getNativeOutputSampleRate(AudioManager.STREAM_SYSTEM);    //48000

    //private static int BUFFER_SIZE = AudioRecord.getMinBufferSize(SAMPLE_RATE, CHANNEL_CONFIG, AUDIO_FORMAT);  //7104
    private static int BUFFER_SIZE = AudioTrack.getMinBufferSize(SAMPLE_RATE, CHANNEL_CONFIG, AUDIO_FORMAT); //14144, 15376(SampleRate:48000, stereo)

    private boolean currentlySendingAudio = false;
    private boolean isPlayingAudio = false;

    private Thread recordAudioThread;
    private Thread playbackAudioThread;
    FileOutputStream fileOutputStream;
    DataOutputStream dataOutputStream;

    String recordFilePath = Environment.getExternalStorageDirectory().getAbsolutePath() + "/" + "recordSound.3gpp";


    @Override
    public void onCreate() {
        super.onCreate();
    }


    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        this.intent = intent;
        this.flags = flags;
        this.startId = startId;


        //SAMPLE_RATE
        //BUFFER_SIZE

        Log.d(TAG, "SAMPLE_RATE: " + SAMPLE_RATE);
        Log.d(TAG, "BUFFER_SIZE: " + BUFFER_SIZE);

        serviceCommand = intent.getIntExtra("CMD", 0);

        switch(serviceCommand){
            case CommandFormat.START_RECORD_AUDIO:
                startRecordAudio(intent);
                break;
            case CommandFormat.PLAY_RECORD_AUDIO:
                startPlaybackAudio(intent);
                break;
            default:

                break;
        }

        return START_NOT_STICKY;
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public void onDestroy(){
        super.onDestroy();

        Log.d(TAG, "onDestroy >> " + serviceCommand);
        switch (serviceCommand){
            case CommandFormat.START_RECORD_AUDIO:
                Log.d(TAG, "recording stop!!!!!");
                currentlySendingAudio = false;
                recordAudioThread.interrupt();
                break;
            case CommandFormat.PLAY_RECORD_AUDIO:
                Log.d(TAG, "playback stop!!!!!!");
                playbackAudioThread.interrupt();
                break;
            default:
                break;
        }
    }


    private void startRecordAudio(Intent intent){

        try{
            Log.d(TAG, "startRecordAudio!!!!!");

            String serviceTitle = intent.getStringExtra("SVC_TITLE");

            createNotificationChannel(NOTIFICATION_RECORD_SOUND_CHANNEL_ID, NOTIFICATION_RECORD_SOUND_CHANNEL_NAME, NotificationManager.IMPORTANCE_DEFAULT);

            Intent notificationIntent = new Intent(this, MainActivity.class);
            PendingIntent pendingIntent = PendingIntent.getActivity(this, 0, notificationIntent, 0);


            Intent broadCastIntent = new Intent(this, MyBroadcastReceiver.class);

            broadCastIntent.setAction("ACTION_START_RECORD");
            broadCastIntent.putExtra("EXTRA_NOTIFICATION_ID", 0);
            PendingIntent startRecordIntent = PendingIntent.getBroadcast(this, 0, broadCastIntent, 0);

            broadCastIntent.setAction("ACTION_STOP_RECORD");
            broadCastIntent.putExtra("EXTRA_NOTIFICATION_ID", 1);
            PendingIntent stopRecordIntent = PendingIntent.getBroadcast(this, 0, broadCastIntent, 0);

            Notification notification = new NotificationCompat.Builder(this, NOTIFICATION_RECORD_SOUND_CHANNEL_ID)
                    .setContentTitle("Foreground Record Service")
                    .setContentText(serviceTitle)
                    .setSmallIcon(R.drawable.ic_sleep_18dp)
                    .setContentIntent(pendingIntent)
                    //.addAction(R.drawable.ic_audio_wave_26, "RECORD", startRecordIntent)
                    .addAction(R.drawable.ic_stop_26, "STOP", stopRecordIntent)
                    .build();

            startForeground(1, notification);
            startRecordStreaming();
        } catch (Exception e){
            Log.e(TAG, e.getMessage());
            e.printStackTrace();
        }

    }

    private void startRecordStreaming(){

        currentlySendingAudio = true;

        recordAudioThread = new Thread(() -> {
            try{
//                int rate = AudioTrack.getNativeOutputSampleRate(AudioManager.STREAM_SYSTEM); //Sample Rate 는 아날로그 신호를 디지털로 변환 할 때, 1초당 몇개의 sample 을 추출할 것인가
//                Log.d(TAG, "sampling rate: " + rate);

                //Channel 값은 mono/streo 중에
                //audioformat 의 값은 PCM Data 를 받을 것이기 때문에 AudioFormat.ENCODING_PCM_16BIT
                // buffer size 는 한번에 전달 받을 audio data 의 크기
                //int bufferSize = AudioRecord.getMinBufferSize(rate, AudioFormat.CHANNEL_IN_MONO, AudioFormat.ENCODING_PCM_16BIT);
                //int bufferSize = AudioRecord.getMinBufferSize(RECORDING_RATE, AudioFormat.CHANNEL_IN_MONO, AudioFormat.ENCODING_PCM_16BIT);
                //Log.d(TAG, "Creating the buffer of size 2: " + bufferSize);

                //short[] buffer = new short[bufferSize];
                byte[] buffer = new byte[BUFFER_SIZE];

                android.os.Process.setThreadPriority(Process.THREAD_PRIORITY_AUDIO);
                //Log.e(TAG, "Creating the AudioRecord");
                recorder = new AudioRecord(MediaRecorder.AudioSource.MIC, SAMPLE_RATE, CHANNEL_CONFIG, AUDIO_FORMAT, BUFFER_SIZE);

                //Log.e(TAG, "AudioRecord recording....");
                recorder.startRecording();

                //String recordFilePath = Environment.getExternalStorageDirectory().getAbsolutePath() + "/" + CommonUtil.getCurrentDateWithTime(CommonUtil.DATE_FORMAT_FILE) + ".3gpp";
                //String recordFilePath = Environment.getExternalStorageDirectory().getAbsolutePath() + "/" + "recordsound.3gpp";
                Log.d(TAG, "recordFilePath: " + recordFilePath);

                try{
                    fileOutputStream = new FileOutputStream(recordFilePath);
                    dataOutputStream = new DataOutputStream(fileOutputStream);
                } catch (FileNotFoundException e){
                    Log.e(TAG, e.getMessage());
                }

                while(currentlySendingAudio == true){
                    //read the data into the buffer
                    int readSize = recorder.read(buffer, 0, buffer.length);
                    double maxAmplitude = 0;

                    try{
                        Log.d(TAG, "read bytes is: " + readSize);
                        dataOutputStream.write(buffer, 0, BUFFER_SIZE);
                    } catch (IOException e){
                        Log.e(TAG, e.getMessage());
                    }

                    //Min
                    //Log.d(TAG, "readSize: " + readSize);
                    for(int i=0; i<readSize; i++){
                        //Log.d(TAG, "buffer[" + i + "]: " + buffer[i] + "/ Math.abs: " + Math.abs(buffer[i]));
                        if(Math.abs(buffer[i]) > maxAmplitude){
                            maxAmplitude = Math.abs(buffer[i]);
                        }
                    }

                    double db = 0;
                    if(maxAmplitude != 0){
                        db = 20.0 * Math.log10(maxAmplitude / 32767.0) + 90;
                    }

                    //Log.e(TAG, "Max amplitude : " + maxAmplitude + " ; DB: " + db);
                }

                Log.d(TAG, "Audio Record finished recording");
                recorder.stop();
                recorder.release();

                try{
                    dataOutputStream.close();
                    fileOutputStream.close();
                } catch (IOException ex){
                    Log.e(TAG, ex.getMessage());
                }

            } catch (Exception e){
                Log.e(TAG, "Exception: " + e);
            }
        });

        recordAudioThread.start();

    }

    private void startPlaybackAudio(Intent intent){

        try{
            Log.d(TAG, "startPlaybackAudio >>>>>>>>");

            //String input = intent.getStringExtra("inputExtra");
            String serviceTitle = intent.getStringExtra("SVC_TITLE");
            audioTrack = new AudioTrack(AudioManager.STREAM_MUSIC, SAMPLE_RATE, CHANNEL_CONFIG, AUDIO_FORMAT, BUFFER_SIZE, AudioTrack.MODE_STREAM);

            createNotificationChannel(NOTIFICATION_RECORD_SOUND_CHANNEL_ID, NOTIFICATION_RECORD_SOUND_CHANNEL_NAME, NotificationManager.IMPORTANCE_DEFAULT);

            Intent notificationIntent = new Intent(this, MainActivity.class);
            PendingIntent pendingIntent = PendingIntent.getActivity(this, 0, notificationIntent, 0);

            Intent broadCastIntent = new Intent(this, MyBroadcastReceiver.class);

            broadCastIntent.setAction("ACTION_START_PLAY");
            broadCastIntent.putExtra("EXTRA_NOTIFICATION_ID", 0);
            PendingIntent startPlayIntent = PendingIntent.getBroadcast(this, 0, broadCastIntent, 0);

            broadCastIntent.setAction("ACTION_STOP_PLAY");
            broadCastIntent.putExtra("EXTRA_NOTIFICATION_ID", 1);
            PendingIntent stopPlayIntent = PendingIntent.getBroadcast(this, 0, broadCastIntent, 0);

            Notification notification = new NotificationCompat.Builder(this, NOTIFICATION_RECORD_SOUND_CHANNEL_ID)
                    .setContentTitle("Foreground Record Service")
                    .setContentText(serviceTitle)
                    .setSmallIcon(R.drawable.ic_sleep_18dp)
                    .setContentIntent(pendingIntent)
                    //.addAction(R.drawable.ic_audio_wave_26, "RECORD", startRecordIntent)
                    .addAction(R.drawable.ic_stop_26, "STOP", stopPlayIntent)
                    .build();

            startForeground(1, notification);
            startPlaybackThread();

        } catch (Exception e){

        }
    }


    private void startPlaybackThread(){
        Log.d(TAG, "startPlaybackThread>>>>>>>>>>>>>");
        isPlayingAudio = true;

        playbackAudioThread = new Thread(() -> {

            byte[] buffer = new byte[BUFFER_SIZE];
            FileInputStream fileInputStream = null;

            try{
                fileInputStream = new FileInputStream(recordFilePath);
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

        playbackAudioThread.start();
    }


    private void createNotificationChannel(String channelId, String channelName, int notificationImportance) {
        if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel notificationChannel = new NotificationChannel(channelId, channelName, notificationImportance);
            NotificationManager manager = getSystemService(NotificationManager.class);
            manager.createNotificationChannel(notificationChannel);
        }
    }
}
