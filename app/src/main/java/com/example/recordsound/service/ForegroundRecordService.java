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
import android.util.Log;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.core.app.NotificationCompat;

import com.example.recordsound.MainActivity;
import com.example.recordsound.R;
import com.example.recordsound.broadcast.MyBroadcastReceiver;
import com.example.recordsound.components.CommonUtil;

import java.io.DataOutputStream;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;

public class ForegroundRecordService extends Service {

    public static final String RECORD_SOUND_CHANNEL_ID = "RECORD_SOUND_CHANNEL";
    Thread streamThread = null;
    FileOutputStream fileOutputStream = null;
    DataOutputStream dataOutputStream = null;

    private static final String TAG_PREFIX = "MZ_";
    private static String TAG = TAG_PREFIX + ForegroundRecordService.class.getSimpleName();

    //the audio recording options
    private static final int RECORDING_RATE = 44100;
    //private static final int CHANNEL = AudioFormat.CHANNEL_IN_MONO;
    private static final int CHANNEL = AudioFormat.CHANNEL_IN_STEREO;
    private static final int FORMAT = AudioFormat.ENCODING_PCM_16BIT;


    //the audio recorder
    private AudioRecord recorder;

    //buffersize 크기가 다를경우 녹음을질 체크!
    //the minimum buffer size needed for audio recording
    //private static int BUFFER_SIZE = AudioRecord.getMinBufferSize(RECORDING_RATE, CHANNEL, FORMAT);  //7104
    private static int BUFFER_SIZE = AudioTrack.getMinBufferSize(RECORDING_RATE, CHANNEL, FORMAT); //14144

    // are we currently sending audio data
    private boolean currentlySendingAudio = false;

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

        Notification notification = new NotificationCompat.Builder(this, RECORD_SOUND_CHANNEL_ID)
                .setContentTitle("Foreground Record Service")
                .setContentText(input)
                .setSmallIcon(R.drawable.ic_sleep_18dp)
                .setContentIntent(pendingIntent)
                //.addAction(R.drawable.ic_audio_wave_26, "RECORD", startRecordIntent)
                .addAction(R.drawable.ic_stop_26, "STOP", stopRecordIntent)
                .build();

        startForeground(1, notification);
        StartRecorder();

        return START_NOT_STICKY;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();

        Log.d(TAG, "onDestory >>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>");
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
                    RECORD_SOUND_CHANNEL_ID,
                    "Foreground Record Service Channel",
                    NotificationManager.IMPORTANCE_DEFAULT
            );

            NotificationManager manager = getSystemService(NotificationManager.class);
            manager.createNotificationChannel(serviceChannel);
        }
    }



    public void StartRecorder() {
        Log.e(TAG, "Starting the audio stream");
        currentlySendingAudio = true;
        startStreaming();
    }

    public void StopRecorder(){
        Log.d(TAG, "Stoping the audio stream");
        currentlySendingAudio = false;
        streamThread.interrupt();   //stop thread


        //Record File Check - read file stream
        String recordFilePath = Environment.getExternalStorageDirectory().getAbsolutePath() + "/" + "recordsound.3gpp";

        /*try{
            Log.d(TAG, "File Check >>>>>>>>>>>>>>>>>>>>>>>> s");
            FileInputStream fis = new FileInputStream(recordFilePath);

            try{
                int i=0;
                while((i=fis.read()) != -1){
                    //Log.d(TAG, "fis: " + i);
                }
            } catch(IOException ex){
                Log.e(TAG, ex.getMessage());
            } finally {
                try{fis.close();} catch (IOException ex){Log.e(TAG, ex.getMessage());}
            }


            Log.d(TAG, "File Check >>>>>>>>>>>>>>>>>>>>>>>> e");
        } catch (FileNotFoundException ex){
            Log.e(TAG, ex.getMessage());
        }*/


        //recorder.release();

        Toast.makeText(this, recordFilePath, Toast.LENGTH_LONG).show();
    }

    private void startStreaming() {
        Log.d(TAG, "Creating the buffer of size 1: " + BUFFER_SIZE);

        //Thread streamThread = new Thread(() -> {
        streamThread = new Thread(() -> {
            try{
                int rate = AudioTrack.getNativeOutputSampleRate(AudioManager.STREAM_SYSTEM); //Sample Rate 는 아날로그 신호를 디지털로 변환 할 때, 1초당 몇개의 sample 을 추출할 것인가
                Log.d(TAG, "sampling rate: " + rate);

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
                recorder = new AudioRecord(MediaRecorder.AudioSource.MIC, rate, CHANNEL, FORMAT, BUFFER_SIZE);

                //Log.e(TAG, "AudioRecord recording....");
                recorder.startRecording();

                //String recordFilePath = Environment.getExternalStorageDirectory().getAbsolutePath() + "/" + CommonUtil.getCurrentDateWithTime(CommonUtil.DATE_FORMAT_FILE) + ".3gpp";
                String recordFilePath = Environment.getExternalStorageDirectory().getAbsolutePath() + "/" + "recordsound.3gpp";
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
                        /*for(int i=0; i<readSize; i++){
                            dataOutputStream.writeShort(buffer[i]);
                        }*/

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

        //start the thread
        streamThread.start();
    }

}
