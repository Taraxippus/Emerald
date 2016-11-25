package com.taraxippus.emerald;

import java.util.ArrayList;

public final class ChordProgression
{
    public static ChordProgression getRandom(Key key, int octave)
    {
        ArrayList<Integer> chordList = new ArrayList<>();
        chordList.add(1);
        chordList.add(2 + MainService.random.nextInt(5));

        int i;

        while (true)
        {
            i = chordList.get(chordList.size() - 1);

            if (i == 3)
                chordList.add(6);

            else if (i == 6)
            {
                if (chordList.contains(2))
                    chordList.add(4);
                else if (chordList.contains(4))
                    chordList.add(2);
                else
                    chordList.add(MainService.random.nextBoolean() ? 2 : 4);
            }
            else if (i == 2)
                chordList.add(5);

            else if (i == 4)
                chordList.add(7);

            else if (i == 5)
                break;

            else if (i == 7)
            {
                if (MainService.random.nextBoolean() && !chordList.contains(3) && !chordList.contains(6))
                    chordList.add(3);

                break;
            }
        }

        int[] progression = new int[chordList.size()];
        for (i = 0; i < progression.length; ++i)
            progression[i] = chordList.get(i);

        int[] repeats = new int[progression.length * (1 + MainService.random.nextInt(2))];
        int average = 1 + MainService.random.nextInt(4);
        int max = 2 + MainService.random.nextInt(3);

        for (i = 0; i < repeats.length; ++i)
            repeats[i] = Math.max(1, average + (int) (MainService.random.nextFloat() * max * 2) - max);

        return new ChordProgression(key, progression, repeats, octave);
    }

    public final int[] progression;
    public final Chord[] chords;
    public final int[] repeats;
    public final int octave;

    public ChordProgression(Key key, int[] progression, int[] repeats, int octave)
    {
        this.progression = progression;
        this.chords = new Chord[progression.length];

        for (int i = 0; i < progression.length; ++i)
            this.chords[i] = key.chords[progression[i] - 1].copy().addOctave(octave);

        this.repeats = repeats;
        this.octave = octave;
    }

    public Melody asMelody()
    {
        int count = 0;

        int i, i1, i2;
        for (i = 0; i < chords.length; ++i)
            count += repeats[i % repeats.length];

        Note[] notes = new Note[count * 3];

        count = 0;

         for (i = 0; i < chords.length; ++i)
            for (i1 = 0; i1 < repeats[i % repeats.length]; ++i1)
                for (i2 = 0; i2 < 3; i2++)
                    notes[count++] = chords[i].getNote(i2);

        return new Melody(notes);
    }

    @Override
    public String toString()
    {
        StringBuilder sb = new StringBuilder();

        for (int i = 0; i < progression.length; ++i)
        {
            if (i != 0)
                sb.append(" -> ");

            sb.append(repeats[i % repeats.length]).append(" x ").append(printChord(i));
        }

        sb.append("   | Octave: ").append(octave);

        return sb.toString();
    }

    public String printChord(int index)
    {
        String s;

        if (progression[index] == 1)
            s = "i";
        else if (progression[index] == 2)
            s = "ii";
        else if (progression[index] == 3)
            s = "iii";
        else if (progression[index] == 4)
            s = "iv";
        else if (progression[index] == 5)
            s = "v";
        else if (progression[index] == 6)
            s = "vi";
        else
            s = "vii";

        if (chords[index].type == Chord.ChordType.MAJOR)
            s = s.toUpperCase();
        else if (chords[index].type == Chord.ChordType.DIMINISHED)
            s = s + "°";

        return s;
    }
}
