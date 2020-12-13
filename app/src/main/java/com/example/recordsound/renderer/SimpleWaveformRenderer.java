package com.example.recordsound.renderer;

import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Path;
import android.util.Log;

import androidx.annotation.ColorInt;

public class SimpleWaveformRenderer implements WaveformRenderer {

    private static final int Y_FACTOR = 0xFF; //255
    private static final float HALF_FACTOR = 0.5f;

    @ColorInt
    private final int backgroundColour;
    private final Paint foregroundPaint;
    private final Path waveformPath;

    static SimpleWaveformRenderer newInstance(@ColorInt int backgroundColour, @ColorInt int foregroundColour){

        Paint paint = new Paint();
        paint.setColor(foregroundColour);
        paint.setAntiAlias(true);
        paint.setStyle(Paint.Style.STROKE);
        Path waveformPath = new Path();

        return new SimpleWaveformRenderer(backgroundColour, paint, waveformPath);
    }

    SimpleWaveformRenderer(@ColorInt int backgroundColour, Paint foregroundPaint, Path waveformPath){
        this.backgroundColour = backgroundColour;
        this.foregroundPaint = foregroundPaint;
        this.waveformPath = waveformPath;
    }


    @Override
    public void render(Canvas canvas, byte[] waveform) {
        canvas.drawColor(backgroundColour);
        float width = canvas.getWidth();
        float height = canvas.getHeight();
        waveformPath.reset();

        if(waveform != null){
            renderWaveform(waveform, width, height);
        } else {
            renderBlank(width, height);
        }

        canvas.drawPath(waveformPath, foregroundPaint);
    }

    private void renderWaveform(byte[] waveform, float width, float height){
        float xIncrement = width / (float) (waveform.length);
        float yIncrement = height / Y_FACTOR;

        //Log.e("1", "width > " + Float.toString(width));
        //Log.e("1", "height > " + Float.toString(height));
        //Log.e("1", "xIncrement > " + Float.toString(xIncrement));
        //Log.e("2", "yIncrement > " + Float.toString(yIncrement));

        int halfHeight = (int) (height * HALF_FACTOR);
        waveformPath.moveTo(0, halfHeight);

        float temp = 0.0f;
        for(int i=1; i<waveform.length; i++){

//            if(!temp.equals(String.valueOf(waveform[i]))) {
//                temp = String.valueOf(waveform[i]);
//                Log.e("1", "temp/ waveform >>> " + temp + "/ " + Float.toString(waveform[i]));
//            }

            float yPostion = waveform[i] > 0 ? height - (yIncrement * waveform[i]) : -(yIncrement * waveform[i]);

            //Log.e("1", "halfHeight/ height/ yPosition/ waveform " + String.valueOf(halfHeight) + "/" + String.valueOf(height) + "/ " + Float.toString(yPostion) + "/ " + waveform[i]);
            if(temp != yPostion){
                Log.e("1", "temp/ yPostion >> " + Float.toString(temp) + "/ " + Float.toString(yPostion) + "/ " + waveform[i]);
                temp = yPostion;

            }

            waveformPath.lineTo(xIncrement * i, yPostion);
        }

        waveformPath.lineTo(width, halfHeight);
    }

    private void renderBlank(float width, float height){
        int y = (int) (height * HALF_FACTOR);
        waveformPath.moveTo(0, y);
        waveformPath.lineTo(width, y);
    }
}
