package com.taraxippus.emerald;

import java.util.ArrayList;
import java.util.Arrays;

public class Rhythm
{
    final Note.NoteType[] rhythm;
    final int countNotes;

    public static Rhythm getRandom(int complexity, int scale)
    {
        ArrayList<Note.NoteType> bar = new ArrayList<>();

        bar.add(Note.NoteDuration.WHOLE.getNoteType());
      
		Note.NoteType type;
		
		for (int i1, i = 0; i < scale; ++i)
			for (i1 = 0; i1 < bar.size(); ++i1)
				{
					type = bar.get(i1);
					
					if (type.duration.ordinal() < Note.NoteDuration.SIXTEENTH.ordinal())
					{
						type.duration = Note.NoteDuration.values()[type.duration.ordinal() + 1];
						bar.add(i1 + 1, new Note.NoteType(type.duration));
					}
				}
			
        int index;
        
        for (int i = 0; i < complexity; ++i)
        {
            index = MainService.random.nextInt(bar.size());

            type = bar.get(index);

            if (type.isExtended() || MainService.random.nextInt(10) == 0)
                continue;

            if (!type.isRest() && type.duration.ordinal() < Note.NoteDuration.QUARTER.ordinal() && MainService.random.nextInt(10) == 0)
            {
                type.duration = Note.NoteDuration.values()[type.duration.ordinal() + 1];
                type.extend();
                bar.add(index + 1, new Note.NoteType(Note.NoteDuration.values()[type.duration.ordinal() + 1]));
            }
            else if (!type.isConcurrent() && type.duration.ordinal() <= Note.NoteDuration.QUARTER.ordinal() && (index == bar.size() -1 && MainService.random.nextBoolean() || MainService.random.nextInt(7) == 0))
            {
                type.isRest = !type.isRest();
            }
            else if (type.duration.ordinal() < Note.NoteDuration.SIXTEENTH.ordinal())
            {
                type.duration = Note.NoteDuration.values()[type.duration.ordinal() + 1];
                bar.add(index + 1, new Note.NoteType(type.duration));
            }
        }

        return new Rhythm(bar.toArray(new Note.NoteType[bar.size()]));
    }

    public static Rhythm mutate(Rhythm rhythm, int complexity)
    {
        ArrayList<Note.NoteType> bar = new ArrayList<>();

        for (Note.NoteType type : rhythm.rhythm)
            bar.add(type.copy());
		int index;
        Note.NoteType type;

        for (int i = 0; i < complexity; ++i)
        {
            index = MainService.random.nextInt(bar.size());

            type = bar.get(index);

            if (type.isExtended() || MainService.random.nextInt(10) == 0)
                continue;

            if (!type.isRest() && type.duration.ordinal() < Note.NoteDuration.QUARTER.ordinal() && MainService.random.nextInt(10) == 0)
            {
                type.duration = Note.NoteDuration.values()[type.duration.ordinal() + 1];
                type.extend();
                bar.add(index + 1, new Note.NoteType(Note.NoteDuration.values()[type.duration.ordinal() + 1]));
            }
            else if (!type.isConcurrent() && (index == bar.size() -1 && MainService.random.nextBoolean() || MainService.random.nextInt(7) == 0))
            {
                type.isRest = !type.isRest();
            }
            else if (type.duration.ordinal() < Note.NoteDuration.SIXTEENTH.ordinal())
            {
                type.duration = Note.NoteDuration.values()[type.duration.ordinal() + 1];
                bar.add(index + 1, new Note.NoteType(type.duration));
            }
        }

        return new Rhythm(bar.toArray(new Note.NoteType[bar.size()]));
    }

    public Rhythm(Note.NoteType[] rhythm)
    {
        this.rhythm = rhythm;

        int count = 0;
        for (Note.NoteType noteType : rhythm)
            if (!noteType.isConcurrent() && !noteType.isRest())
                count++;

        this.countNotes = count;
    }

    public Note[] apply(Note[] notes, boolean copy)
    {
        int rests = 0;

        for (Note.NoteType type : rhythm)
            if (type.isRest())
                rests++;

        for (Note note : notes)
            if (note.isRest())
                rests--;

        Note[] rhythmicNotes = new Note[notes.length + rests];

        int index = 0;
        for (int i = 0; i < rhythmicNotes.length;)
        {
            if (rhythm[i % rhythm.length].isRest())
            {
                rhythmicNotes[i] = new Note(rhythm[i % rhythm.length]);
                ++i;
            }
            else if (notes[index].isRest())
                index++;
            else
            {
                rhythmicNotes[i] = copy ? notes[index++].copy().setDuration(rhythm[i % rhythm.length]) : notes[index++].setDuration(rhythm[i % rhythm.length]);
                ++i;
            }
        }

        return rhythmicNotes;
    }

    @Override
    public String toString()
    {
        return Arrays.toString(rhythm);
    }
}
