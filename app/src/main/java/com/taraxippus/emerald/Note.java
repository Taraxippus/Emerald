package com.taraxippus.emerald;

import com.leff.midi.MidiTrack;
import com.leff.midi.event.MidiEvent;

public class Note
{
    public static final String[] names = new String[] {"C", "C#", "D", "D#", "E", "F", "F#", "G", "G#", "A", "A#", "B"};

    final int note;
    NoteType duration;
    int octave;

    int velocity = 100;

    public Note(int note)
    {
        this(note, NoteDuration.QUARTER.getNoteType());
    }

    public Note(NoteType type)
    {
        this(-1000, type.toRest());
    }

    public Note(int note, NoteType type)
    {
        this(note % 12, note / 12, type);
    }

    public Note(int note, int octave, NoteType duration)
    {
        this.note = note;
        this.octave = octave;
        this.duration = duration;
    }

    public int add(MidiTrack track, int channel, int ticks)
    {
        if (!isRest())
            track.insertNote(channel, getPitch(), velocity, ticks, duration.getRealDuration());
            
        return ticks + duration.getDuration();
    }

    public boolean isRest()
    {
        return duration.isRest() || note < 0 || octave < 0;
    }

    public int getPitch()
    {
        return octave * 12 + note;
    }

    public Note setDuration(NoteType duration)
    {
        this.duration = duration;
        return this;
    }

    public Note setOctave(int octave)
    {
        this.octave = octave;
        return this;
    }

    public Note addOctave(int octave)
    {
        this.octave += octave;
        return this;
    }

    public Note copy()
    {
        return copy(0);
    }

    public Note copy(int offset)
    {
        Note copy = new Note(note + offset + octave * 12, duration);
        copy.velocity = velocity;

        return copy;
    }

    @Override
    public String toString()
    {
        return note < 0 ? duration.toString() : names[note] + "" + octave  + " " + duration.toString();
    }

    public enum NoteDuration
    {
        WHOLE,
        HALF,
        QUARTER,
        EIGHTH,
        SIXTEENTH;

        public NoteType getNoteType()
        {
            return new NoteType(this);
        }
    }

    public static class NoteType
    {
        NoteDuration duration;

        boolean isRest = false;
        boolean isConcurrent = false;
        boolean isExtended = false;

        public NoteType(NoteDuration duration) { this.duration = duration; }

        public int getDuration()
        {
            if (isConcurrent)
                return 0;

            return getRealDuration();
        }

        public int getRealDuration()
        {
            if (isExtended)
            {
                switch (duration)
                {
                    case WHOLE:
                        return MainService.TICKS_PER_BEAT * 6;
                    case HALF:
                        return MainService.TICKS_PER_BEAT * 3;
                    case QUARTER:
                        return MainService.TICKS_PER_BEAT / 2 * 3;
                    case EIGHTH:
                        return MainService.TICKS_PER_BEAT / 4 * 3;
                    case SIXTEENTH:
                        return MainService.TICKS_PER_BEAT / 8 * 6;
                    default:
                        return 0;
                }
            }

            switch (duration)
            {
                case WHOLE:
                    return MainService.TICKS_PER_BEAT * 4;
                case HALF:
                    return MainService.TICKS_PER_BEAT * 2;
                case QUARTER:
                    return MainService.TICKS_PER_BEAT;
                case EIGHTH:
                    return MainService.TICKS_PER_BEAT / 2;
                case SIXTEENTH:
                    return MainService.TICKS_PER_BEAT / 4;
                default:
                    return 0;
            }
        }

        public NoteType toRest()
        {
            this.isRest = true;
            this.isConcurrent = false;
            return this;
        }

        public boolean isRest()
        {
            return this.isRest;
        }

        public NoteType toConcurrent()
        {
            this.isRest = false;
            this.isConcurrent = true;
            return this;
        }

        public boolean isConcurrent()
        {
            return this.isConcurrent;
        }

        public NoteType extend()
        {
            this.isExtended = true;
            return this;
        }

        public boolean isExtended()
        {
            return this.isExtended;
        }

        public NoteType copy()
        {
            NoteType type = new NoteType(duration);
            type.isRest = isRest;
            type.isConcurrent = isConcurrent;
            type.isExtended = isExtended;
            return type;
        }

        @Override
        public String toString()
        {
            StringBuilder sb = new StringBuilder();

            if (isConcurrent())
                sb.append("(");
            else if (isRest())
                sb.append("[");

            switch (duration)
            {
                case WHOLE:
                    sb.append("1");
                    break;
                case HALF:
                    sb.append("2");
                    break;
                case QUARTER:
                    sb.append("4");
                    break;
                case EIGHTH:
                    sb.append("8");
                    break;
                case SIXTEENTH:
                    sb.append("16");
                    break;
            }

            if (isExtended())
                sb.append(".");

            if (isConcurrent())
                sb.append(")");
            else if (isRest())
                sb.append("]");

            return sb.toString();
        }
    }
}
