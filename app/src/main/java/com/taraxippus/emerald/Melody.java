package com.taraxippus.emerald;

import com.leff.midi.MidiTrack;
import java.util.Arrays;

public class Melody
{
    public static Melody getRandom(Key key, ChordProgression progression, Rhythm rhythm, NoiseGenerator generator, int melodyLength, int melodyScale, int melodyRange, int melodyOffset)
    {
        boolean multiple = MainService.random.nextInt(5) == 0;

        Note[] notes = new Note[(rhythm.countNotes * (melodyLength + 1)) * (multiple ? 2 : 1)];

        for (int i = 0; i < notes.length; ++i)
        {
            notes[i] = key.getClosest((int) (melodyOffset + melodyRange * generator.getNoise((float) i / notes.length * melodyScale)));

            if (multiple)
            {
                if (key.containsNote((notes[i].note + 5) % 12))
                    notes[++i] = notes[i - 1].copy(5);
                else
                    notes[++i] = notes[i - 1].copy(3);
            }
        }

        return new Melody(notes);
    }

    Note notes[];

    public Melody(Note[] notes)
    {
        this.notes = notes;
    }

    public void addOctave(int octave)
    {
        for (Note note : notes)
            note.addOctave(octave);
    }

    public int add(MidiTrack track, int channel, int start)
    {
        int ticks = start;

        for (int i = 0; i < notes.length; ++i)
            ticks = notes[i].add(track, channel, ticks);

        return ticks;
    }

    @Override
    public String toString()
    {
        return Arrays.toString(notes);
    }
}
