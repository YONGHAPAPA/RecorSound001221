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

import java.io.BufferedInputStream;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;

import org.apache.commons.math3.complex.Complex;
import org.jtransforms.fft.DoubleFFT_1D;



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

            //DoubleFFT_1D fft = new DoubleFFT_1D(BUFFER_SIZE);


            byte[] audioData = getByteFromSource(fileInputStream);
            Log.d(TAG, "BUFFER_SIZE: " + BUFFER_SIZE);
            Log.d(TAG, "audioData.length: " + audioData.length);

            double[] samples = readFileData(audioData); //짝수: 실수부분, 홀수: 허수부분

            double[] frequencies = frequencyAnalysis(samples);

            for(int i=0; i<frequencies.length; i++){
                Log.d(TAG, "frequencies[" + i + "]: " + frequencies[i]);
            }

            /*for(double i: samples){
                Log.d(TAG, "sample: " + i);
            }*/


           /* while(isPlayingAudio){
                Log.d(TAG, "isPlayingAudio: " + isPlayingAudio);

                try{
                    int readSize = dataInputStream.read(buffer, 0, BUFFER_SIZE);
                    //Log.d(TAG, "Play Audio readSize: " + readSize);


                    if(readSize <= 0){
                        isPlayingAudio = false;
                        stopSelf();
                        break;
                    }


                    //Min
                    Log.d(TAG, "BUFFER_SIZE: " + BUFFER_SIZE);
                    Log.d(TAG, "readSize: " + readSize);

                    for(int i=0; i<readSize; i++){
                        //Log.d(TAG, "buffer: " + buffer[i]);
                        //fft.complexForward();
                    }

                    audioTrack.write(buffer, 0, readSize);

                } catch (IOException e){
                    Log.e(TAG, e.getMessage());
                    e.printStackTrace();
                }
            }*/

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


    private double[] frequencyAnalysis(double[] samples){


        //(samples.length - blocksize)/ block_inc + 1 = 388.244

        int BLOCKSIZE = 4410;   //effectively FFT's 0.1 second of sound;
        int BLOCK_INC = BLOCKSIZE/5; //how much to slide between FFT's  //882
        int blocks = (samples.length - BLOCKSIZE)/BLOCK_INC + 1;        //388 (왜 전체 데이터 길이에서 blocksize 뺀후 계산을 하지?)

        //an array to hold the frequencies
        double[] frequencies = new double[blocks];
        double[] currSamp = new double[BLOCKSIZE];
        int finish = BLOCK_INC * (blocks - 1);                          //341334

        Log.d(TAG, "samples.length: " + samples.length);          //345960
        Log.d(TAG, "blocks: " + blocks);
        Log.d(TAG, "BLOCK_INC: " + BLOCK_INC); //882
        Log.d(TAG, "finish: " + finish); //341334

        //samples.lenght/block_inc = 392.244897
        int temp = samples.length/BLOCK_INC;
        Log.d(TAG, "samples.length/BLOCK_INC: " + samples.length/BLOCK_INC);    //392


        int i; //keeps track of the current sample size
        for(i=0; i < finish; i+=BLOCK_INC){

            //Samples 데이터중 단위블럭 만큼 별도의 임시배열로 복사
            System.arraycopy(samples, i, currSamp, 0, BLOCKSIZE);
            int freq = i/BLOCK_INC;

            //Log.d(TAG, i + " freq: " + freq);

            frequencies[freq] = maxFrequency(currSamp);

            /*for(int j=0; j<currSamp.length; j++){
                *//*if(i==171108) {
                    Log.d(TAG, samples[j+171108] + " :: " + "j >> " + currSamp[j]);
                }*//*
            }*/
        }

        //Log.d(TAG, "i: " + i);

        return frequencies;
    }


    private double maxFrequency(double[] sound){

        DoubleFFT_1D fftDo = new DoubleFFT_1D(sound.length);
        double[] fft = new double[sound.length*2];
        System.arraycopy(sound, 0, fft, 0, sound.length);
        fftDo.realForwardFull(fft);

        int max_i = -1;
        double max_fftval = -1;

        for(int i=0; i<fft.length; i+=2){
            double vlen = Math.sqrt((fft[i] * fft[i] + fft[i+1] * fft[i+1]));


            double currFreq = ((i/2.0)/fft.length) * SAMPLE_RATE * 2;
            if(currFreq != 0){
                if(max_fftval < vlen){
                    max_fftval = vlen;
                    max_i = i;
                }
            }

        }

        double dominantFreq = ((max_i/2.0)/fft.length) * SAMPLE_RATE * 2;
        return dominantFreq;
    }



    private double[] readFileData(byte[] fileData){

        double MAX_16_BIT = Short.MAX_VALUE; //32767
        byte[] currentData;
        currentData = fileData;

        try{
            int N = fileData.length;
            double[] d = new double[N/2];
            for(int i=0; i<N/2; i++){
                d[i] = ((short)(((currentData[2*i+1] & 0xFF) << 8) + (currentData[2*i] & 0xFF))) / ((double)MAX_16_BIT);
            }

            return d;

        } catch (Exception e){
            Log.e(TAG, "readBufferAsDouble: " + e.getMessage());
            e.printStackTrace();
            return null;
        }
    }

    private byte[] getByteFromSource(FileInputStream stream){

        byte[] audioBytes;
        int read;
        byte[] buff = new byte[BUFFER_SIZE];

        ByteArrayOutputStream out = new ByteArrayOutputStream();
        BufferedInputStream in = new BufferedInputStream(stream);

        try{
            while((read = in.read(buff)) > 0){
                out.write(buff, 0, read);
            }
            out.flush();
            in.close();
            audioBytes = out.toByteArray();
            return audioBytes;
        } catch (IOException e){
            e.printStackTrace();
            return null;
        }
    }
}
