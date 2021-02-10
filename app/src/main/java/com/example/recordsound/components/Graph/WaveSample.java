package com.example.recordsound.components.Graph;

public class WaveSample {
    private long time;
    private long amplitude;

    public WaveSample(long time, int amplitude){
        this.time = time;
        this.amplitude = amplitude;
    }

    public long getTime(){
        return time;
    }

    public void setTime(long time){
        this.time = time;
    }

    public long getAmplitude(){
        return amplitude;
    }

    public void setAmplitude(int amplitude){
        this.amplitude = amplitude;
    }
}
