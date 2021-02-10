package com.example.recordsound.components.Graph;

import android.app.Activity;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.RectF;
import android.util.AttributeSet;
import android.util.Log;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.HorizontalScrollView;

import java.util.HashMap;
import java.util.List;
import java.util.Set;

public class GraphView extends HorizontalScrollView {
    private static final String TAG_PREFIX = "MZ_";
    private static final String TAG = TAG_PREFIX + GraphView.class.getSimpleName();

    private double graphXOffset = 0.75; //X position to start plotting
    private int timeScale = 5 * 1000; //put time marker for every 5 sec
    private int maxAmplitude = 35000; //Maximum possible amplitude;
    private double defaultWaveLength = 2.6; //default sine wave length
    private int timeMarkerSize = 50;
    private boolean drawFullGraph = false;
    private GraphSurfaceView graphSurfaceView;
    private List<WaveSample> pointList;

    private Paint paint;
    private Paint markerPaint;
    private Paint timePaint;
    private Paint needlePaint;

    private int canvasColor = Color.rgb(0,0,0);
    private int markerColor = Color.argb(160, 30, 30, 30);
    private int graphColor = Color.rgb(255,255,255);
    private int timeColor = Color.rgb(250, 250, 250);
    private int needleColor = Color.rgb(250, 0, 0);


    private volatile float move = 0;



    public GraphView(Context context){
        super(context);
    }


    private class GraphSurfaceView extends SurfaceView implements SurfaceHolder.Callback, Runnable {

        private SurfaceHolder holder;
        private Context context;
        private Thread _plottingThread;

        private int height;
        private int halfHeight;
        private int width;

        private volatile int waveLength;
        private volatile boolean isRunning = false;
        private volatile boolean stop = false;
        private int widthForFullGraph = 50;
        int listMasterSize = 0;
        int redrawCount = 0;
        int freezCount = 0;
        int sleepTime = 5;
        private int deltaWidth;

        public GraphSurfaceView(Context context) {
            super(context);
            init(context);
        }

        public GraphSurfaceView(Context context, AttributeSet attrs){
            super(context, attrs);
            init(context);
        }

        public GraphSurfaceView(Context context, AttributeSet attrs, int defStyle){
            super(context, attrs, defStyle);
            init(context);
        }

        public void setWaveLength(int scale){
            waveLength = scale;
        }

        public void init(Context context){
            this.context = context;

            //setting up surface view with default dimensions
            this.setLayoutParams(new ViewGroup.LayoutParams(100, 100));

            //set wave length in mm
            waveLength = (int) (context.getResources().getDisplayMetrics().xdpi / (25.4 * defaultWaveLength));  //context.getResources().getDisplayMetrics().xdpi: 해당 디바이스의 정확한 가로밀도

            holder = getHolder();
            holder.addCallback(this);

            //paint config for waves
            paint = new Paint(Paint.ANTI_ALIAS_FLAG);
            paint.setColor(graphColor);
            paint.setStrokeWidth(1);
            paint.setStyle(Paint.Style.STROKE); //Paint.Style.STROKE: 채우지 않은 외각선

            //paint config for amplitude needle
            needlePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
            needlePaint.setColor(needleColor);
            needlePaint.setStrokeWidth(1);
            needlePaint.setStyle(Paint.Style.STROKE);

            //Paint config for time text
            timePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
            timePaint.setColor(timeColor);
            timePaint.setStrokeWidth(1);
            timePaint.setTextSize(timeMarkerSize/this.context.getResources().getDisplayMetrics().scaledDensity);    //context.getResources().getDisplayMetrics().scaledDensity: 해당 디바이스의 문자열 스케일링시 곱해지는 값

            //Paint config for right side marker
            markerPaint.setColor(markerColor);
            markerPaint.setStyle(Paint.Style.STROKE);
        }

        /*
          시간흐름에 대한 진폭을 계산 그래프처리
        * */
        private void processAmplitude(){
            //wave의 부드러운 이동을 위해 sleep time 및 redraw 회수 계산
            if(pointList.size() != listMasterSize){
                //최종 프레임에서 신규 sample 데이터가 들어오면
                 listMasterSize = pointList.size();
                 freezCount = -1;
                 redrawCount = 0;
            } else {
                //마지막 프레임에서 samples 수만큼 좌측으로 이동
                redrawCount++;
                if(redrawCount > waveLength){ //moved too much left still no new sample
                    freezCount++;

                    if(freezCount > 0){
                        sleepTime = sleepTime + 1;
                    } else if(freezCount < 0){
                        sleepTime = sleepTime - 1;

                        if(sleepTime < 0){
                            sleepTime = 0;
                        }
                    }

                    redrawCount = waveLength;
                }
            }

            //Wave 처리용 Path
            Path graphPath = new Path();

            //time marker position용 hashmap
            HashMap<Integer, String> timeMap = new HashMap<>();

            //wave path의 시작점 초기화
            int x = (int) (width * graphXOffset); // canvas width(getWidth()) * 0.75
            int listSize = pointList.size() - 1;

            //Marker용 path
            Path markerPath = new Path();
            markerPaint.setStrokeWidth((float)(width - (width * graphXOffset)));
            markerPath.moveTo(x + (width/8), 0);
            markerPath.lineTo(x + (width/8), height);

            //Needle용 path
            Path needlPath = new Path();

            /*
            * draw sine waves for last 'n' no of samples.
            * 'n' is calculated from no x - direction pixels available in surface view from width*3/4 to 0-wavelength.
            * each sample will be drawn as a sine wave with wavelength as width
            *
            * waveLength < scale size < setWaveLength(int scale)
            * timeScale: 5*1000 sec, time marker 5sec.
            * x: waveform start x-point
            * */
            for(int i = listSize-1; x >= 0 - waveLength; x = x - waveLength){
                if(i >= 0){
                    if(i == 0){
                        timeMap.put(x - redrawCount, "00:00"); //최초 시간 00.00 으로 입력
                    } else {
                        long currentSampleTime = pointList.get(i).getTime();
                        long lastSampleTime = pointList.get(i-1).getTime();


                        //if current sampled time passes timeScale sec, put a time maker.
                        if(lastSampleTime % timeScale > currentSampleTime % timeScale){
                            //timeMap.put(x - redrawCount, formatTime(currentSampleTime));
                        }
                    }

                    int amplitude = (int) pointList.get(i).getAmplitude();
                    drawAmplitude(amplitude, x, graphPath, needlPath);
                }

                i--;
            }

            renderAmplitude(timeMap, graphPath, markerPath, needlPath);
        }


        /*
        * Draw sine wave path for current sample at x position with amplitude and needle path to show current amplitude
        * */
        private Path drawAmplitude(int amplitude, int x, Path graphPath, Path needlePath){

            /* calculate no y pixels for sine wave magnitude from amplitude */
            amplitude = halfHeight * amplitude / maxAmplitude;

            /* if current sample is the latest then move needle to show current amplitude*/
            if(x == (int)(width * graphXOffset)){
                needlePath.moveTo((float)(width*graphXOffset), halfHeight - amplitude);
                needlePath.lineTo(width, halfHeight - amplitude);
            }

            if(amplitude > 0){
                /*below code can be customized to support more graph types*/
                /* draw a sine wave from x-redrawCount to x-redrawCount+waveLength with positive magnitude at halfHeight-amplitude and negative at halfHeight+amplitude*/
                RectF oval = new RectF();
                oval.set(x-redrawCount, halfHeight-amplitude, x-redrawCount+(waveLength/2), halfHeight+amplitude);
                graphPath.addArc(oval, 180, 180);
                oval.set(x-redrawCount + (waveLength/2), halfHeight-amplitude, x-redrawCount+(waveLength), halfHeight+amplitude);
                graphPath.addArc(oval, 0, 180);
            } else {
                /*draw simple line to represent 0*/
                graphPath.moveTo(x-redrawCount, halfHeight);
                graphPath.lineTo(x-redrawCount+waveLength, halfHeight);
            }

            return graphPath;
        }

        /*
        * draw all the path on SurfaceView canvas
        * */
        private void renderAmplitude(HashMap<Integer, String> timeMap, Path tempPath, Path markerPath, Path needlePath){
            Canvas tempCanvas = null;

            if(holder.getSurface().isValid()){  //SurfaceView available
                try{
                    tempCanvas = holder.lockCanvas();
                    synchronized (holder){
                        if(tempCanvas != null){
                            /*Clean SurfaceView with plain canvas color*/
                            tempCanvas.drawColor(canvasColor);

                            /*draw time text*/
                            Set<Integer> keys = timeMap.keySet();
                            for(int key: keys){
                                tempCanvas.drawText(timeMap.get(key), key, 20, timePaint);
                            }

                            /*draw sine waves, maker and needle*/
                            tempCanvas.drawPath(tempPath, paint);

                            if(markerPath != null){
                                tempCanvas.drawPath(markerPath, markerPaint);
                            }

                            if(needlePath != null){
                                tempCanvas.drawPath(needlePath, needlePaint);
                            }
                        }
                    }

                } finally {
                    if(tempCanvas != null){
                        holder.unlockCanvasAndPost(tempCanvas);
                    }
                }
            }

            try{
                /*sleep the thread to reduce CPU usage and avoid same wave redrawn*/
                Thread.sleep(sleepTime);
            } catch (InterruptedException e){
                e.printStackTrace();
            }
        }

        @Override
        public void surfaceCreated(SurfaceHolder surfaceHolder){
            Log.d(TAG, "Created Surface");

            /*configure width for current mode*/
            this.setLayoutParams(new LayoutParams(GraphView.this.getWidth(), GraphView.this.getHeight()));

            /*continue plotting on app switches between foreground and background*/
            if(isRunning && !_plottingThread.isAlive()){
                //startPlotting();
            }
        }

        @Override
        public void surfaceChanged(SurfaceHolder surfaceHolder, int i, int i1, int i2){
            Log.d(TAG, "Changed Surface");

            /*Redraw full graph if needed*/
            if(drawFullGraph){
                drawFullGraph();
            }

            /*reset will get current rendered dimensions*/
            reset();
        }

        @Override
        public void surfaceDestroyed(SurfaceHolder surfaceHolder){
            Log.d(TAG, "Destroyed Surface");

            /*stop the plotting if app goes to background*/
            this.stop = true;
            if(_plottingThread != null){
                _plottingThread.interrupt();
            }
        }

        /*reset the surface view with plain canvas color and get current rendered dimensions*/
        public void reset(){
            height = getHeight();
            halfHeight = height/(2);
            width = getWidth();

            Canvas tempCanvas = null;

            if(holder.getSurface().isValid()){
                try{
                    tempCanvas = holder.lockCanvas();
                    synchronized (holder){
                        if(tempCanvas != null){
                            tempCanvas.drawColor(canvasColor);
                        }
                    }
                } finally {
                    if(tempCanvas != null){
                        holder.unlockCanvasAndPost(tempCanvas);
                    }
                }
            }
        }

        /*set master list that holds the samples*/
        public void setMasterList(List<WaveSample> list){
            pointList = list;
        }


        /*calculate no of pixels needed in x direction to display all available samples in the point list and set it as surface view's width */
        /*Will trigger surface change after new dimensions*/
        public void showFullGraph(){
            if(pointList == null){
                return;
            }

            if(pointList.size() == 0){
                return;
            }

            drawFullGraph = true;
            reset();
            this.stop = true;
            isRunning = false;
            if(_plottingThread != null){
                _plottingThread.interrupt();
            }

            widthForFullGraph = pointList.size() * waveLength + 50;
            drawFullGraph();
        }

        /*
        * same as processAmplitude function
        * */
        private void drawFullGraph(){
            new Thread(new Runnable() {
                @Override
                public void run() {
                    deltaWidth = width - widthForFullGraph;

                    if(move > 0){
                        move = 0;
                    }

                    if(deltaWidth < 0){
                        if(move < deltaWidth){
                            move = deltaWidth;
                        }
                    } else {
                        move = 0;
                    }

                    int sampleNo;
                    int x = 0;

                    if(widthForFullGraph < width){
                        sampleNo = pointList.size();
                    } else {
                        sampleNo = (int)(width + waveLength + Math.abs(move)) / waveLength;
                    }

                    if(sampleNo > pointList.size()){
                        sampleNo = pointList.size();
                    }


                    HashMap<Integer, String> timeMap1 = new HashMap<>();
                    Path tempPath1 = new Path();

                    for(int i= (int)(Math.abs(move) / waveLength); i <= sampleNo - 1; i++){
                        if(i==0){
                            timeMap1.put(x, "00:00");
                        } else {
                            long currentSampleTime = pointList.get(i).getTime();
                            long lastSampleTime = pointList.get(i-1).getTime();

                            if(lastSampleTime % timeScale > currentSampleTime % timeScale) {

                            }
                        }
                    }



                }
            }).start();
        }
    }
}


