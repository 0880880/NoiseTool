package com.noisetool;

import imgui.type.ImFloat;
import imgui.type.ImInt;
import lombok.NoArgsConstructor;

@NoArgsConstructor
public class NoiseLayer {

    public NoiseType noiseType = NoiseType.Perlin;
    public ImFloat frequency = new ImFloat(.1f);
    public ImFloat amplitude = new ImFloat(1);
    public ImFloat fractalGain = new ImFloat(1);
    public ImInt fractalOctaves = new ImInt(1);
    public ImFloat fractalLacunarity = new ImFloat(2);

    public NoiseLayer(float frequency, float amplitude) {
        this.frequency.set(frequency);
        this.amplitude.set(amplitude);
    }

}
