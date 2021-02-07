package com.example.recordsound.components;

import org.jtransforms.fft.DoubleFFT_1D;

public class FrequencyScanner {

    private double[] window;

    public FrequencyScanner(){
        window = null;
    }

    public double extractFrequency(short[] sampleData, int sampleRate){

        double result = 0.0f;
        DoubleFFT_1D fft = new DoubleFFT_1D(sampleData.length);


        return result;
    }
}
