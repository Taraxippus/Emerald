package com.taraxippus.emerald;

import java.util.Random;

public class NoiseGenerator
{
    final NoiseOctave[] octaves;

    final float[] frequencies;
    final float[] amplitudes;

    final int numberOfOctaves;

    public NoiseGenerator(int largestFeature, float persistence, long seed)
    {
        this.numberOfOctaves = (int) Math.ceil(Math.log10(largestFeature) / Math.log10(2));

        octaves = new NoiseOctave[numberOfOctaves];
        frequencies = new float[numberOfOctaves];
        amplitudes = new float[numberOfOctaves];

        final Random random = new Random(seed);

        for (int i = 0; i < numberOfOctaves; i++)
        {
            octaves[i] = new NoiseOctave(random.nextLong());

            frequencies[i] = (float) Math.pow(2, i);
            amplitudes[i] = (float) Math.pow(persistence, octaves.length - i);
        }
    }

    public float getNoise(float x)
    {
        float result = 0;

        for (int i = 0; i < numberOfOctaves; i++)
            result += octaves[i].noise(x / frequencies[i]) * amplitudes[i];

        return result;
    }
}
