package com.example.recordsound;

import androidx.appcompat.app.AppCompatActivity;

import android.content.ContentResolver;
import android.content.ContentUris;
import android.content.Intent;
import android.graphics.Color;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.media.audiofx.Visualizer;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.util.Log;
import android.view.View;
import android.view.ViewDebug;
import android.widget.Button;
import android.widget.TextView;

import com.example.recordsound.renderer.RendererFactory;

import java.io.IOException;

public class DetailViewRecordItem extends AppCompatActivity implements Visualizer.OnDataCaptureListener {

    MediaPlayer mediaPlayer;
    Button playButton, stopButton, delButton;

    private Visualizer visualizer;
    private WaveformView waveFormView;
    private static final int CAPTURE_SIZE = 256;

    private String recordId;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_detail_view_record_item);

        Intent intent = getIntent();
        this.recordId = intent.getStringExtra(DisplayRecordListActivity.EXTRA_RECORD_ID);

        TextView txtView = findViewById(R.id.txtRecordId);
        txtView.setText(this.recordId);

        //mediaPlayer = new MediaPlayer();
        playButton = (Button) findViewById(R.id.btnPlaySound);
        stopButton = (Button) findViewById(R.id.btnPlayStop);
        delButton = (Button) findViewById(R.id.btn_delete);

        waveFormView = (WaveformView) findViewById(R.id.waveform_view);

        RendererFactory rendererFactory = new RendererFactory();
        waveFormView.setRenderer(rendererFactory.createSimpleWaveformRenderer(Color.GREEN, Color.DKGRAY));


        playButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                playButton.setEnabled(false);
                stopButton.setEnabled(true);

                try {
                    playAudio(Long.parseLong(recordId));
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        });

        stopButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                playButton.setEnabled(true);
                stopButton.setEnabled(false);

                mediaPlayer.stop();
                mediaPlayer.release();
                mediaPlayer = null;

                stopVisualizer();
            }
        });

        delButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                //recordId
                deleteAudio(Long.parseLong(recordId));

            }
        });


    }


    private void deleteAudio(long id){
        Log.e("1", "deleteContent > " + toString().valueOf(id));

        ContentResolver contentResolver = getContentResolver();
        Uri contentUri = ContentUris.withAppendedId(MediaStore.Audio.Media.EXTERNAL_CONTENT_URI, id);
        contentResolver.delete(contentUri, null, null);
        finish();
    }



    private void playAudio(long id) throws IOException {

        mediaPlayer = new MediaPlayer();
        Uri contentUri = ContentUris.withAppendedId(MediaStore.Audio.Media.EXTERNAL_CONTENT_URI, id);

        mediaPlayer.setAudioStreamType(AudioManager.STREAM_MUSIC);
        mediaPlayer.setDataSource(getApplicationContext(), contentUri);
        mediaPlayer.setOnPreparedListener(new MediaPlayer.OnPreparedListener() {
            @Override
            public void onPrepared(MediaPlayer mp) {

                //Log.e("onPrepared", "start >>>>> ");
                startVisualizer();
                mp.start();
            }
        });

        mediaPlayer.prepareAsync();

        mediaPlayer.setOnCompletionListener(new MediaPlayer.OnCompletionListener() {
            @Override
            public void onCompletion(MediaPlayer mp) {
                //Log.e("onComplettion", "end >>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>");
                playButton.setEnabled(true);
                stopButton.setEnabled(false);
                mediaPlayer.release();
                mediaPlayer = null;

                stopVisualizer();
            }
        });


        mediaPlayer.setOnErrorListener(new MediaPlayer.OnErrorListener() {
            @Override
            public boolean onError(MediaPlayer mp, int what, int extra) {
                //Log.e("onError", "Error Code : " + Integer.toString(what) + " /extra : " + Integer.toString(extra));
                return false;
            }
        });

        mediaPlayer.setOnBufferingUpdateListener(new MediaPlayer.OnBufferingUpdateListener() {
            @Override
            public void onBufferingUpdate(MediaPlayer mp, int percent) {
                //Log.e("setOnBufferingUpdate", "stat >>>>>>>>>>>>>>>>>>>>>>>");
                int postion = mp.getCurrentPosition();
                //Log.e("current position", Integer.toString(postion));
            }
        });
    }

    @Override
    protected void onResume() {
        super.onResume();
        //Log.e("1", "onResume >>>>>>>>>>>>>>>>>>>>>");
    }


    private void startVisualizer(){
        //Log.e("1", "startVisualizer");

        //Log.e("getMaxCaptureRate", Integer.toString(Visualizer.getMaxCaptureRate()));

        int rate = Visualizer.getMaxCaptureRate(); //milliHz
        rate = 1000;

        //Log.e("millihz", Integer.toString(millihz));

        visualizer = new Visualizer(0);
        visualizer.setDataCaptureListener(this, rate, true, false);
        visualizer.setCaptureSize(CAPTURE_SIZE);
        visualizer.setEnabled(true);
    }


    private void stopVisualizer(){

        Log.e("1", "stopVisualizer >>>>>> ");

        if(visualizer != null){
            visualizer.setEnabled(false);
            visualizer.release();
            visualizer.setDataCaptureListener(null, 0, false, false);
        }
    }



    @Override
    public void onWaveFormDataCapture(Visualizer visualizer, byte[] waveform, int samplingRate) {
        if(waveform != null){
            //Log.e("start", "waveFormDateCapture >>>>>>> ");
            waveFormView.setWaveform(waveform);
            //waveFormView.test(waveform);
        }
    }

    @Override
    public void onFftDataCapture(Visualizer visualizer, byte[] fft, int samplingRate) {
        //NO-OP

        //Log.e("1", "onFftDataCapture");
    }
}
