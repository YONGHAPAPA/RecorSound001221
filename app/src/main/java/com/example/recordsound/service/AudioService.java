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
import android.renderscript.Sampler;
import android.util.Log;

import androidx.annotation.Nullable;
import androidx.core.app.NotificationCompat;

import com.example.recordsound.MainActivity;
import com.example.recordsound.R;
import com.example.recordsound.broadcast.MyBroadcastReceiver;
import com.example.recordsound.components.CommandFormat;
import com.example.recordsound.components.FFT;

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
import ca.uol.aig.fftpack.RealDoubleFFT;

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
    private static final int SAMPLE_RATE = 44100;
    //private static final int SAMPLE_RATE = AudioTrack.getNativeOutputSampleRate(AudioManager.STREAM_SYSTEM);    //48000

    private int BUFFER_SIZE;
    private static int BUFFER_SIZE_AR = AudioRecord.getMinBufferSize(SAMPLE_RATE, CHANNEL_CONFIG, AUDIO_FORMAT);  //7104
    private static int BUFFER_SIZE_AT = AudioTrack.getMinBufferSize(SAMPLE_RATE, CHANNEL_CONFIG, AUDIO_FORMAT); //14144(SR:44100 STREO), 15376(SampleRate:48000, stereo)
    private static int NEW_BUFFER_SIZE;

    private boolean currentlySendingAudio = false;
    private boolean isPlayingAudio = false;

    private Thread recordAudioThread;
    private Thread playbackAudioThread;
    FileOutputStream fileOutputStream;
    DataOutputStream dataOutputStream;

    FFT fft;
    float[] fftRealArray = null;
    float volume = 0;

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
        Log.d(TAG, "BUFFER_SIZE_AR: " + BUFFER_SIZE_AR);
        Log.d(TAG, "BUFFER_SIZE_AT: " + BUFFER_SIZE_AT);

        //NEW_BUFFER_SIZE = getHigherP2(BUFFER_SIZE_AR);
        NEW_BUFFER_SIZE = getHigherP2(BUFFER_SIZE_AT); //16384 (power of 2)

        //BUFFER_SIZE = BUFFER_SIZE_AR;
        //BUFFER_SIZE = BUFFER_SIZE_AT;
        BUFFER_SIZE = NEW_BUFFER_SIZE;

        Log.d(TAG, "NEW_BUFFER_SIZE:" + NEW_BUFFER_SIZE);
        //Log.d(TAG, "SAMPLE_RATE: " + SAMPLE_RATE);
        Log.d(TAG, "BUFFER_SIZE: " + BUFFER_SIZE);
        
        //buffer size 8192 : 정상 (Audio Record Min Buffer size)
        //buffer size 16384 : 정상 (Audio Track Min Buffer size)


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

        //Log.d(TAG, "onDestroy >> " + serviceCommand);
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

        //currentlySendingAudio = true;

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
                //byte[] buffer = new byte[NEW_BUFFER_SIZE];

                android.os.Process.setThreadPriority(Process.THREAD_PRIORITY_AUDIO);
                //Log.e(TAG, "Creating the AudioRecord");
                recorder = new AudioRecord(MediaRecorder.AudioSource.MIC, SAMPLE_RATE, CHANNEL_CONFIG, AUDIO_FORMAT, BUFFER_SIZE);
                //recorder = new AudioRecord(MediaRecorder.AudioSource.MIC, SAMPLE_RATE, CHANNEL_CONFIG, AUDIO_FORMAT, NEW_BUFFER_SIZE);


                if(recorder != null && recorder.getState() == AudioRecord.STATE_INITIALIZED){
                    //Log.e(TAG, "AudioRecord recording....");
                    recorder.startRecording();
                    currentlySendingAudio = true;
                }


                //String recordFilePath = Environment.getExternalStorageDirectory().getAbsolutePath() + "/" + CommonUtil.getCurrentDateWithTime(CommonUtil.DATE_FORMAT_FILE) + ".3gpp";
                //String recordFilePath = Environment.getExternalStorageDirectory().getAbsolutePath() + "/" + "recordsound.3gpp";
                Log.d(TAG, "recordFilePath: " + recordFilePath);

                try{
                    fileOutputStream = new FileOutputStream(recordFilePath);
                    dataOutputStream = new DataOutputStream(fileOutputStream);
                } catch (FileNotFoundException e){
                    Log.e(TAG, e.getMessage());
                    currentlySendingAudio = false;
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
                        currentlySendingAudio = false;
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
                        db = 20.0 * Math.log10(maxAmplitude / 32767.0) + 90; //short(16bit) 타입 범위(-32768 ~ 32767)
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
            //audioTrack = new AudioTrack(AudioManager.STREAM_MUSIC, SAMPLE_RATE, CHANNEL_CONFIG, AUDIO_FORMAT, NEW_BUFFER_SIZE, AudioTrack.MODE_STREAM);



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

            //startPlaybackThreadByStream1();
            startPlaybackThreadByStream2();

        } catch (Exception e){

            Log.e(TAG, e.getMessage());

        }
    }



    private void startPlaybackThreadByStream2(){

        int TEST_SIZE = 100;
        int BLOCK_SIZE = 1024;
        int NEW_BLOCK_SIZE = 512;
        isPlayingAudio = true;




        playbackAudioThread = new Thread(()->{
            Log.d(TAG, "<<<<<<<<<<<<<<<<<<<<< start playback an audio >>>>>>>>>>>>>>>>>>");

            //byte[] buffer = new byte[BLOCK_SIZE];     //1024
            byte[] buffer = new byte[BUFFER_SIZE];  //15376
            //byte[] buffer = new byte[NEW_BUFFER_SIZE];  //4096
            //byte[] buffer = new byte[BUFFER_SIZE_AT];
            //byte[] buffer = new byte[TEST_SIZE];    //100
            //short[] shortBuffer = new short[BUFFER_SIZE];
            FileInputStream fileStream = null;
            DataInputStream dataInputStream = null;

            try{
                fileStream = new FileInputStream(recordFilePath);
                dataInputStream = new DataInputStream(fileStream);

            } catch (Exception e){
                e.printStackTrace();
            }

            try{

                //Log.d(TAG, "buffer_size: " + BUFFER_SIZE);  //15376


                audioTrack.play();

                //boolean isReadBuffer = true;


                while(isPlayingAudio){
                    //int readSize = dataInputStream.read(buffer, 0, BLOCK_SIZE);
                    int readSize = dataInputStream.read(buffer, 0, BUFFER_SIZE);


                    //int readSize = dataInputStream.read(buffer, 0, NEW_BUFFER_SIZE);
                    //int readSize = dataInputStream.read(buffer, 0, BUFFER_SIZE_AT);


                    Log.d(TAG, "readSize: " + readSize);    //1024 > 720

                    if(readSize <= 0){
                        //isReadBuffer = false;
                        isPlayingAudio = false;
                        break;
                    }



                    //byte[](8bit)-> short[](2byte, 16bit)
                    //byte.length = 1/2 * short.length
                    //double[] toTransform = convertByteToDouble(buffer);   //byte[] buffer:1024 > double[] audioData:512

                    //Log.d(TAG, "audioData length: " + toTransform.length);    //512
                    //transformer.ft(toTransform);

                    audioTrack.write(buffer,0, readSize);




                    double curr_frequency = 0;
                    double magnitude = 0;


                    //Convert byte to short type
                    short[] shortBuffer = new short[buffer.length];

                    for(int i=0; i<buffer.length;i++){
                        shortBuffer[i] = (short)buffer[i];
                    }

                    //Log.d(TAG, "toTransform length : " + toTransform.length);   //512
                    fft = new FFT(shortBuffer.length, SAMPLE_RATE);

                    float maxFreqToDraw = 2500;
                    Log.d(TAG, "fft.freqToIndex: " + fft.freqToIndex(maxFreqToDraw));



                    fftRealArray = new float[shortBuffer.length];
                    for(int i=0; i<shortBuffer.length; i++){
                        fftRealArray[i] = (float)shortBuffer[i]/Short.MAX_VALUE; //32768.0

                        Log.d(TAG, "fftRealArray[" + i +"]" + fftRealArray[i]);
                        volume += Math.abs(fftRealArray[i]);
                    }

                    volume = (float)Math.log10(volume/shortBuffer.length);

                    Log.d(TAG, "volume: " + volume);

                    //Get Volume
                    //volume





                    /*for(int i=0; i<toTransform.length; i++){

                        //Sample rate : 48000
                        //46.875Hz
                        curr_frequency = i * SAMPLE_RATE / toTransform.length;
                        magnitude = toTransform[i];

                        //Log.d(TAG, "freq: " + curr_frequency + " / " + magnitude);

                    }*/

                }



                dataInputStream.close();
                fileStream.close();

                audioTrack.stop();
                audioTrack.release();

            } catch (Exception e){
                e.printStackTrace();
                isPlayingAudio = false;
            }
        });

        playbackAudioThread.start();
    }


    int getHigherP2(int val){
        val--;
        val |= val >> 1;
        val |= val >> 2;
        val |= val >> 8;
        val |= val >> 16;
        //val |= val >> 32;
        val++;
        return (val);
    }

    private void startPlaybackThreadByStream1(){
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



//            byte[] audioData = getByteFromSource(fileInputStream);
//            Log.d(TAG, "BUFFER_SIZE: " + BUFFER_SIZE);              //15376
//            Log.d(TAG, "audioData.length: " + audioData.length);    //691920


            //Audio file 의 byte정보를 double 타입으로 변경
            //짝수: 실수부분, 홀수: 허수부분
            //double[] samples = readFileData(audioData);

            //Log.d(TAG, "samples length:" + samples.length); //345960
            /*for(int i=0; i<samples.length;i++){
                Log.d(TAG, "[" + i + "]: " + samples[i]);
            }*/

            //double[] frequencies = frequencyAnalysis(samples);

            /*for(int i=0; i<frequencies.length; i++){
                Log.d(TAG, "frequencies[" + i + "]: " + frequencies[i]);
            }*/

            /*for(double i: samples){
                Log.d(TAG, "sample: " + i);
            }*/


            while(isPlayingAudio){
                //Log.d(TAG, "isPlayingAudio: " + isPlayingAudio);

                try{
                    int readSize = dataInputStream.read(buffer, 0, BUFFER_SIZE);

                    if(readSize <= 0){
                        isPlayingAudio = false;
                        stopSelf();
                        break;
                    }

                    //Min
                    //Log.d(TAG, "BUFFER_SIZE: " + BUFFER_SIZE);  //15376
                    //Log.d(TAG, "readSize: " + readSize);        //15376

                    for(int i=0; i<readSize; i++){
                        //Log.d(TAG, "buffer: " + buffer[i]);
                        //fft.complexForward();
                    }

                    //Convert Sample Data type from byte to double
                    double[] sample = convertByteToDouble(buffer);
                    //Log.d(TAG, "sample.length: " + sample.length); //7688

                    double[] frequencyData = frequencyAnalysis(sample);

                    Log.d(TAG, "Hz: " + frequencyData[0] + "/ mangnitude:" + frequencyData[1]);




                    audioTrack.write(buffer, 0, readSize);

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




    private double[] frequencyAnalysis(double[] sample){

        double currFrequency;
        //int FFT_SIZE = sample.length * 2; //7688 ---> FFT_SIZE 는 2에 제곱승 이여야하는데 7688은 2제곱승이 아닌데...
        int FFT_SIZE = 1024;
        DoubleFFT_1D fft = new DoubleFFT_1D(FFT_SIZE);
        double[] fftData = new double[sample.length*2];
        double[] magnitude = new double[sample.length];
        
        
        //FFT Length는 2의 제곱승 체크
        int n = FFT_SIZE;
        int m = (int)(Math.log(n) / Math.log(2));
        if(n != (1<<m))
            throw new RuntimeException("FFT length must be power of 2");

        for(int i=0; i<sample.length; i++){
            fftData[2*i] = sample[i];   //sin파의 합을 넣는다고 보면되겠네...
            fftData[2*i+1] = 0;
        }

        fft.complexForward(fftData);    //fftData 에 output data overwrite 처리
        //Log.d(TAG, "fftData.length: " + fftData.length);    //15376


        //fftData 는 퓨리에변환된 실수(짝수index), 허수(홀수index) 데이터가 담겨있는 주파수에 대한 진폭정보
        for(int i=0; i<sample.length; i++){
            double re = fftData[2*i];
            double im = fftData[2*i+1];
            magnitude[i] = Math.sqrt((re*re + im*im)); //
        }


        int i_fq=0;
        double max_magnitude = -1;
        double max_i = -1;

        for(int i=0; i<sample.length; i++){
            if(magnitude[i] > max_magnitude){
                max_magnitude = magnitude[i];
                max_i = i;
           }
        }

        currFrequency = (SAMPLE_RATE * max_i) / sample.length;  //Frequency = Sample Rate * index / FFT Length

        double[] frequency = new double[2];
        frequency[0] = currFrequency;
        frequency[1] = max_magnitude;

        return frequency;
    }


    private double[] frequencyAnalysis1(double[] samples){


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



    private double[] convertByteToDouble(byte[] fileData){

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
        byte[] buff = new byte[BUFFER_SIZE];    //BUFFER_SIZE: 15376

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
