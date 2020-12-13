package com.example.recordsound;

import android.content.Context;
import android.graphics.Canvas;
import android.util.AttributeSet;
import android.util.Log;
import android.view.View;

import com.example.recordsound.renderer.WaveformRenderer;

import java.util.Arrays;

public class WaveformView extends View {
    private byte[] waveform;
    private WaveformRenderer renderer;

    public WaveformView(Context context){
        super(context);
    }

    public WaveformView(Context context, AttributeSet attrs){
        super(context, attrs);
    }

    public WaveformView(Context context, AttributeSet attrs, int defStyleAttr){
        super(context, attrs, defStyleAttr);
    }

    public void setRenderer(WaveformRenderer renderer){
        this.renderer = renderer;
    }

    @Override
    protected void onDraw(Canvas canvas){

        //Min
        //Log.e("1", "WaveformView - onDraw >>>>>>>>>>>>>>>");
        super.onDraw(canvas);

        if(renderer != null){
            renderer.render(canvas, waveform);
        }
    }

    public void setWaveform(byte[] waveform){

        //Log.e("setWaveForm length", Integer.toString(waveform.length));
        this.waveform = Arrays.copyOf(waveform, waveform.length);
        invalidate();
    }

    public void test(byte[] waveform){
        Log.e("1", "WaveformView - test >>>>>> ");
        //this.waveform = waveform;
        invalidate();
    }


}
