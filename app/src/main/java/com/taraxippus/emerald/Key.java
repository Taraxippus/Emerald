package com.taraxippus.emerald;

public final class Key
{
    final KeyType type;
    final int root;

    final Note[] notes = new Note[7];
    final Chord[] chords = new Chord[7];

    static final int[] interval_major = new int[] { 0, 2, 4, 5, 7, 9, 11 };
    static final int[] interval_minor = new int[] { 0, 2, 3, 5, 7, 8, 10 };

    static final Chord.ChordType[] chords_major = new Chord.ChordType[] {Chord.ChordType.MAJOR, Chord.ChordType.MINOR,
            Chord.ChordType.MINOR, Chord.ChordType.MAJOR, Chord.ChordType.MAJOR, Chord.ChordType.MINOR,
            Chord.ChordType.DIMINISHED };

    static final Chord.ChordType[] chords_minor = new Chord.ChordType[] {Chord.ChordType.MINOR, Chord.ChordType.DIMINISHED,
            Chord.ChordType.MAJOR, Chord.ChordType.MINOR, Chord.ChordType.MINOR, Chord.ChordType.MAJOR,
            Chord.ChordType.MAJOR };

    public Key(KeyType type, int root)
    {
        this.type = type;
        this.root = root;

        if (type == KeyType.MAJOR)
            for (int i = 0; i < this.notes.length; ++i)
            {
                this.notes[i] = new Note(root + interval_major[i]);
                this.chords[i] = new Chord(root + interval_major[i], chords_major[i]);
            }

        else if (type == KeyType.MINOR)
            for (int i = 0; i < this.notes.length; ++i)
            {
                this.notes[i] = new Note(root + interval_minor[i]);
                this.chords[i] = new Chord(root + interval_minor[i], chords_minor[i]);
            }
    }

    public Note getClosest(int note)
    {
        int closestValue = 1000;
        int closestIndex = 0;

        for (int i = 0; i < notes.length; ++i)
            if (closestValue > Math.min(Math.abs(note % 12 - notes[i].note), Math.abs(note % 12 + 12 - notes[i].note)))
            {
                closestValue = Math.min(Math.abs(note % 12 - notes[i].note), Math.abs(note % 12 + 12 - notes[i].note));
                closestIndex = i;
            }

        Note closest = notes[closestIndex].copy();

        while (Math.abs(closest.getPitch() - note) > Math.abs(closest.getPitch() + 12 - note))
            closest.addOctave(1);

        while (Math.abs(closest.getPitch() - note) > Math.abs(closest.getPitch() - 12 - note))
            closest.addOctave(-1);

        return closest;
    }

    public boolean containsNote(int note)
    {
        for (int i = 0; i < this.notes.length; ++i)
            if (this.notes[i].note == note)
                return true;

        return false;
    }

    public enum KeyType
    {
        MAJOR,
        MINOR
    }
}
