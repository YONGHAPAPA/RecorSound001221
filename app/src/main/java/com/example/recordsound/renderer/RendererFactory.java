package com.example.recordsound.renderer;

import androidx.annotation.ColorInt;

public class RendererFactory {
    public WaveformRenderer createSimpleWaveformRenderer(@ColorInt int foreground, @ColorInt int background) {
        return SimpleWaveformRenderer.newInstance(background, foreground);
    }
}
