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

public class RecordSoundService extends Service {

    private static final String TAG_PREFIX = "MZ_";
    private static String TAG = TAG_PREFIX + RecordSoundService.class.getSimpleName();
    private static final String NOTIFICATION_RECORD_SOUND_CHANNEL_ID = "RECORD_SOUND_CHANNEL";
    private static final String NOTIFICATION_RECORD_SOUND_CHANNEL_NAME = "record sound channel";

    private static final int SAMPLE_RATE = 44100;
    private static final int CHANNEL_MONO = AudioFormat.CHANNEL_IN_MONO;
    private static final int CHANNEL_STEREO = AudioFormat.CHANNEL_IN_STEREO;
    private static final int  FORMAT_PCM_16 = AudioFormat.ENCODING_PCM_16BIT;
    //private static int AUDIO_RECORD_BUFFER_SIZE = AudioRecord.getMinBufferSize(SAMPLE_RATE, CHANNEL_MONO, FORMAT_PCM_16); //3584
    //private static int AUDIO_RECORD_BUFFER_SIZE = AudioRecord.getMinBufferSize(SAMPLE_RATE, CHANNEL_STEREO, FORMAT_PCM_16); //7104
    //private static int AUDIO_RECORD_BUFFER_SIZE = AudioTrack.getMinBufferSize(SAMPLE_RATE, CHANNEL_MONO, FORMAT_PCM_16); //14144
    private static int AUDIO_RECORD_BUFFER_SIZE = AudioTrack.getMinBufferSize(SAMPLE_RATE, CHANNEL_STEREO, FORMAT_PCM_16); //14144

    private AudioRecord audioRecord;

    Thread recordStartThread;
    FileOutputStream fileOutputStream;
    DataOutputStream dataOutputStream;

    private boolean currentlyRecordingAudio = false;

    @Override
    public void onCreate(){
        super.onCreate();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId){

        Log.d(TAG, "AUDIO_RECORD_BUFFER_SIZE :" + AUDIO_RECORD_BUFFER_SIZE);
        String input = intent.getStringExtra("inputExtra");

        createNotificationChannel(NOTIFICATION_RECORD_SOUND_CHANNEL_ID, NOTIFICATION_RECORD_SOUND_CHANNEL_NAME, NotificationManager.IMPORTANCE_DEFAULT);

        Intent notificationIntent = new Intent(getBaseContext(), MainActivity.class);
        notificationIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK); //?
        PendingIntent pendingIntent = PendingIntent.getActivity(getBaseContext(), 0, notificationIntent, PendingIntent.FLAG_UPDATE_CURRENT);

        Intent broadCastIntent = new Intent(this, MyBroadcastReceiver.class);
        broadCastIntent.setAction("act_stop_record");
        broadCastIntent.putExtra("extra_notification_id", 0);
        PendingIntent stopRecordIntent = PendingIntent.getBroadcast(this, 0, broadCastIntent, 0);

        //start record action
        /*broadCastIntent.setAction("act_start_record");
        broadCastIntent.putExtra("extra_notification_id", 1);
        PendingIntent startRecordIntent = PendingIntent.getBroadcast(this, 0, broadCastIntent, 0);*/

        Notification notification = new NotificationCompat.Builder(this, NOTIFICATION_RECORD_SOUND_CHANNEL_ID)
                .setContentText("Record Sound Service")
                .setContentText(input)
                .setSmallIcon(R.drawable.ic_sleep_18dp)
                .setContentIntent(pendingIntent)
                .addAction(R.drawable.icon_stop_black_18dp, "Stop Recording", stopRecordIntent)
                .build();

        startForeground(1, notification);
        startRecordingThread();

        //tells the OS to not bother recreate the service when the phone runs out of memory(on the contrary, START_STICKY)
        return START_NOT_STICKY;

    }

    @Override
    public void onDestroy(){
        super.onDestroy();
        stopRecordingThread();
    }


    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    private void startRecordingThread() {
        currentlyRecordingAudio = true;

        int nativeOutputSampleRate = AudioTrack.getNativeOutputSampleRate(AudioManager.STREAM_SYSTEM);
        Log.d(TAG, "nativeoutputSampleRate: " + nativeOutputSampleRate);


        recordStartThread = new Thread(()->{
            try{

                android.os.Process.setThreadPriority(Process.THREAD_PRIORITY_AUDIO);
                byte[] buffer = new byte[AUDIO_RECORD_BUFFER_SIZE];

                audioRecord = new AudioRecord(MediaRecorder.AudioSource.MIC, SAMPLE_RATE, CHANNEL_STEREO, FORMAT_PCM_16, AUDIO_RECORD_BUFFER_SIZE);
                audioRecord.startRecording();

                String recordFilePath = Environment.getExternalStorageDirectory().getAbsolutePath() + "/samplesound.3pgg";
                //String recordFilePath2 = Environment.getExternalStorageDirectory().getAbsolutePath() + "/" + CommonUtil.getCurrentDateWithTime(CommonUtil.DATE_FORMAT_FILE) + ".3pgg";

                try{
                    fileOutputStream = new FileOutputStream(recordFilePath);
                    dataOutputStream = new DataOutputStream(fileOutputStream);
                } catch(IOException e){
                    e.printStackTrace();
                }

                while(currentlyRecordingAudio){
                    int readSize = audioRecord.read(buffer, 0, AUDIO_RECORD_BUFFER_SIZE);

                    try{
                        dataOutputStream.write(buffer, 0, AUDIO_RECORD_BUFFER_SIZE);
                    }catch (IOException e){
                        e.printStackTrace();
                    }

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

                    //Log.d(TAG, "DB: " + db);
                }

                audioRecord.stop();
                audioRecord.release();

                try{
                    dataOutputStream.close();
                    fileOutputStream.close();
                } catch (IOException e){
                    e.printStackTrace();
                }
            } catch (Exception e){
                Log.e(TAG, e.getMessage());
                e.printStackTrace();
            }
        });

        recordStartThread.start();
    }

    private void stopRecordingThread(){
        try{

            Log.d(TAG, "stopRecordingThread >>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>> ");
            currentlyRecordingAudio = false;
            recordStartThread.interrupt();

            String recordFilePath = Environment.getExternalStorageDirectory().getAbsolutePath() + "/samplesound.3pgg";

            try{
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
        }


        } catch(Exception e){
            e.printStackTrace();
        }
    }


    private void createNotificationChannel(String channelId, String channelName, int notificationImportance) {
        if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel notificationChannel = new NotificationChannel(channelId, channelName, notificationImportance);
            NotificationManager manager = getSystemService(NotificationManager.class);
            manager.createNotificationChannel(notificationChannel);
        }
    }
}
