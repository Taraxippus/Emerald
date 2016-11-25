package com.taraxippus.emerald;


import com.leff.midi.MidiTrack;
import com.leff.midi.event.MidiEvent;

public final class Chord
{
    final int note;
    final ChordType type;

    int octave = 0;
    int velocity = 100;

    ChordInversion inversion = ChordInversion.NONE;

    public Chord(int note, ChordType type)
    {
        this.note = note % 12;
        this.octave = note / 12;
        this.type = type;
    }

    public void add(MidiTrack track, int channel, int ticks, int[] delayStart, int[] delayEnd)
    {
        int[] notes = getNotes(this.type);
        int[] inversion = getInversion(this.inversion);

        for (int i = 0; i < notes.length; ++i)
			track.insertNote(channel, (octave + inversion[notes.length - i - 1]) * 12 + note + notes[i], velocity, ticks + (delayStart == null ? 0 : delayStart[i]), (delayEnd == null ? 0 : delayEnd[i]) - (delayStart == null ? 0 : delayStart[i]));
            
    }

    public int[] getPitch(int[] pitch)
    {
        int[] notes = getNotes(this.type);
        int[] inversion = getInversion(this.inversion);

        if (pitch == null)
            pitch = new int[notes.length];

        for (int i = 0; i < notes.length; ++i)
            pitch[i] = note + notes[i] + inversion[notes.length - i - 1];

        return pitch;
    }

    public Chord invert()
    {
        return new Chord(this.note, this.type).setInversion(ChordInversion.values()[(this.inversion.ordinal() + 1) % ChordInversion.values().length]);
    }

    public Chord setInversion(ChordInversion inversion)
    {
        this.inversion = inversion;
        return this;
    }

    public Chord setOctave(int octave)
    {
        this.octave = octave;
        return this;
    }

    public Chord addOctave(int octave)
    {
        this.octave += octave;
        return this;
    }

    public Chord copy()
    {
        Chord copy = new Chord(note, type);
        copy.octave = octave;
        copy.velocity = velocity;
        copy.inversion = inversion;

        return copy;
    }

    public Note getNote(int index)
    {
        return new Note(note + getNotes(type)[index] + octave * 12 + getInversion(inversion)[getNotes(type).length - index - 1]);
    }

    private static final int[] major = new int[] {0, 4, 7};
    private static final int[] minor = new int[] {0, 3, 7};
    private static final int[] augmented = new int[] {0, 4, 8};
    private static final int[] diminished = new int[] {0, 3, 6};

    public int[] getNotes(ChordType type)
    {
        switch (type)
        {
            case MAJOR:
                return major;
            case MINOR:
                return minor;
            case AUGMENTED:
                return augmented;
            case DIMINISHED:
                return diminished;
        }

        return major;
    }

    private static final int[] none = new int[] {0, 0, 0, 0};
    private static final int[] first = new int[] {-1, 0, 0, 0};
    private static final int[] second = new int[] {-1, -1, 0, 0};
    private static final int[] third = new int[] {-1, -1, -1, 0};

    public int[] getInversion(ChordInversion inversion)
    {
        switch (inversion)
        {
            case NONE:
                return none;
            case FIRST:
                return first;
            case SECOND:
                return second;
            case THIRD:
                return third;
        }

        return none;
    }

    public enum ChordType
    {
        MAJOR,
        MINOR,
        AUGMENTED,
        DIMINISHED,
    }

    public enum ChordInversion
    {
        NONE,
        FIRST,
        SECOND,
        THIRD,
    }
}
