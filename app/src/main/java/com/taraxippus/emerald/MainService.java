package com.taraxippus.emerald;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.media.session.MediaController;
import android.media.session.MediaSession;
import android.net.Uri;
import android.os.Environment;
import android.os.IBinder;
import android.os.PowerManager;
import android.preference.PreferenceManager;
import android.text.format.DateFormat;
import android.widget.Toast;
import com.leff.midi.MidiFile;
import com.leff.midi.MidiTrack;
import com.leff.midi.event.meta.Tempo;
import com.leff.midi.event.meta.TimeSignature;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Date;
import java.util.Random;
import com.leff.midi.event.ProgramChange;

public class MainService extends Service implements MediaPlayer.OnPreparedListener, MediaPlayer.OnErrorListener, MediaPlayer.OnCompletionListener, AudioManager.OnAudioFocusChangeListener, SharedPreferences.OnSharedPreferenceChangeListener
{
	public static final String SESSION = "com.taraxippus.emerald";
	public static final int NOTIFICATION_ID = 1;

	public static final String ACTION_PLAY = "com.taraxippus.emerald.action.PLAY";
	public static final String ACTION_PAUSE = "com.taraxippus.emerald.action.PAUSE";
	public static final String ACTION_STOP = "com.taraxippus.emerald.action.STOP";
	public static final String ACTION_SKIP_NEXT = "com.taraxippus.emerald.action.SKIP_NEXT";
	public static final String ACTION_SAVE = "com.taraxippus.emerald.action.SAVE";
	
	public File FILE;
	
	public static final int TICKS_PER_BEAT = 480;
	
	public static final Random random = new Random();
	
	boolean wasPlaying = false;

	MediaPlayer player;
	MediaSession session;
	MediaController controller;

	private static boolean preparing = true;
	private NotificationManager notificationManager;
	
	@Override
	public int onStartCommand(Intent intent, int flags, int startId)
	{
		if (FILE == null)
		{
			FILE = new File(getFilesDir() + "/out.mid");
			
			if (!FILE.exists())
				createSequence();
		}
			
		if (notificationManager == null)
			notificationManager = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
			
		if (player == null)
			initMediaSession();

		if (intent != null && controller != null)
			handleIntent(intent);

		return super.onStartCommand(intent, flags, startId);
	}

	public void createSequence()
	{
		final MidiTrack trackTempo = new MidiTrack();
		final MidiTrack trackMelody = new MidiTrack();
		final MidiTrack trackChords = new MidiTrack();
		
		//trackChords.insertEvent(new ProgramChange(0, 1, ProgramChange.MidiProgram.SYNTH_BASS_1.programNumber()));
		//trackMelody.insertEvent(new ProgramChange(0, 0, ProgramChange.MidiProgram.SYNTH_BRASS_1.programNumber()));
		
        Key.KeyType keyType = Key.KeyType.values()[random.nextInt(Key.KeyType.values().length)];
        int keyRoot = random.nextInt(12);

        int bpm = 70 + random.nextInt(20);
        int repeatCount = 3 + random.nextInt(4);
        int octave = 4;
        int melodyLength = 2 + random.nextInt(3);
        int melodyRange = 6 + random.nextInt(12);
        int melodyOffset = 4 + random.nextInt(6);
        int melodyScale = 100 + random.nextInt(32);
        float melodyPersistence = 0.6F + random.nextFloat() * 0.4F;
        long melodySeed = random.nextLong();
		int chordRhythmComplexity = 1 + random.nextInt(3);
        int rhythmComplexity = 2 + random.nextInt(7);
		int rhythmScale = 1 + random.nextInt(3);
		
        Key key = new Key(keyType, keyRoot);
        NoiseGenerator generator = new NoiseGenerator(256, melodyPersistence, melodySeed);

        Rhythm rhythmChords = Rhythm.getRandom(chordRhythmComplexity, rhythmScale);
        Rhythm rhythmMelody = Rhythm.mutate(rhythmChords, rhythmComplexity);

        ChordProgression progression = ChordProgression.getRandom(key, octave);

        Melody melody = Melody.getRandom(key, progression, rhythmMelody, generator, melodyLength, melodyScale, melodyRange, melodyOffset);
        melody.addOctave(octave + random.nextInt(2));
        melody.notes = rhythmMelody.apply(melody.notes, false);

        Melody chords = progression.asMelody();
        chords.notes = rhythmChords.apply(chords.notes, false);
		
        int i;
        int ticks = 0;

        for (i = 0; i < repeatCount; ++i)
            ticks = melody.add(trackMelody, 0, ticks);

        ticks = 0;
        for (i = 0; i < repeatCount; ++i)
            ticks = chords.add(trackChords, 1, ticks);
			
		TimeSignature ts = new TimeSignature();
		ts.setTimeSignature(4, 4, TimeSignature.DEFAULT_METER, TimeSignature.DEFAULT_DIVISION);
		trackTempo.insertEvent(ts);
		
		Tempo tempo = new Tempo();
		tempo.setBpm(bpm);
		trackTempo.insertEvent(tempo);
		
		ArrayList<MidiTrack> tracks = new ArrayList<MidiTrack>();
		tracks.add(trackTempo);
		tracks.add(trackMelody);
		//tracks.add(trackChords);
		
		MidiFile midi = new MidiFile(MidiFile.DEFAULT_RESOLUTION, tracks);

		try
		{
			midi.writeToFile(FILE);
		}
		catch (IOException e)
		{
			System.err.println(e);
		}
	}
	
	public void releaseMediaSession()
	{
		if (player != null)
		{
			if (!preparing && player.isPlaying())
				player.stop();

			player.reset();
			player.release();
			player = null;
			preparing = true;
		}
	}

	public void initMediaSession()
	{
		if (player != null)
			releaseMediaSession();

		try
		{
			player = new MediaPlayer();
			player.setAudioStreamType(AudioManager.STREAM_MUSIC);
			player.setDataSource(this, Uri.fromFile(FILE));
			player.setOnPreparedListener(this);
			player.setOnCompletionListener(this);
			player.setOnErrorListener(this);
			player.setWakeMode(this, PowerManager.PARTIAL_WAKE_LOCK);
			player.setLooping(PreferenceManager.getDefaultSharedPreferences(MainService.this).getBoolean("repeat", false));
		}
		catch (Exception e)
		{
			e.printStackTrace();
			return;
		}

		if (session == null)
		{
			session = new MediaSession(this, SESSION);
			controller = new MediaController(this, session.getSessionToken());

			session.setCallback(new MediaSession.Callback()
				{
					@Override
					public void onPlay() 
					{
						super.onPlay();

						if (player == null)
							initMediaSession();

						else if (!preparing && !player.isPlaying())
						{
							AudioManager audioManager = (AudioManager) getSystemService(Context.AUDIO_SERVICE);
							int result = audioManager.requestAudioFocus(MainService.this, AudioManager.STREAM_MUSIC, AudioManager.AUDIOFOCUS_GAIN);

							if (result == AudioManager.AUDIOFOCUS_REQUEST_GRANTED)
							{
								player.start();

								float volume = PreferenceManager.getDefaultSharedPreferences(MainService.this).getFloat("volume", 1);
								player.setVolume(volume, volume);

								buildNotification(R.drawable.pause, "Pause", ACTION_PAUSE);
							}
						}
					}

					@Override
					public void onPause() 
					{
						super.onPause();

						if (!preparing && player != null && player.isPlaying())
						{
							player.pause();
							((AudioManager) getSystemService(Context.AUDIO_SERVICE)).abandonAudioFocus(MainService.this);

							buildNotification(R.drawable.play, "Play", ACTION_PLAY);
						}
					}

					@Override
					public void onSkipToNext() 
					{
						super.onSkipToNext();
						
						wasPlaying = true;
						releaseMediaSession();
						createSequence();
						initMediaSession();
					}


					@Override
					public void onStop()
					{
						super.onStop();

						if (player != null && !preparing && player.isPlaying())
							player.stop();

						((AudioManager) getSystemService(Context.AUDIO_SERVICE)).abandonAudioFocus(MainService.this);

						stopForeground(true);
						stopSelf();
					}
				});

			PreferenceManager.getDefaultSharedPreferences(this).registerOnSharedPreferenceChangeListener(this);
		}

		buildNotification(R.drawable.load, "Load", ACTION_PLAY);
		player.prepareAsync();
		preparing = true;
	}

	private Notification.Action generateAction(int icon, String title, String intentAction) 
	{
		Intent intent = new Intent(this, MainService.class);
		intent.setAction(intentAction);
		PendingIntent pendingIntent = PendingIntent.getService(this, 0, intent, 0);
		return new Notification.Action.Builder(icon, title, pendingIntent).build();
	}

	private void buildNotification(final int icon, final String title, final String intentAction) 
	{
		Notification.Action action = generateAction(icon, title, intentAction);
		Notification.MediaStyle style = new Notification.MediaStyle();
		
		final PendingIntent preferenceIntent = PendingIntent.getActivity(this, 0, new Intent(this, PreferenceActivity.class), 0);
		
		Notification.Builder builder = new Notification.Builder(this)
            .setSmallIcon(R.drawable.launcher)
            .setContentTitle("Emerald")
            .setContentText("Procedural Generated Music")
            .setStyle(style)
			.setShowWhen(false);

		builder.addAction(generateAction(R.drawable.heart, "Save", ACTION_SAVE));
		builder.addAction(action);
		builder.addAction(generateAction(R.drawable.skip_next, "Skip to next", ACTION_SKIP_NEXT));
		builder.addAction(generateAction(R.drawable.stop, "Stop", ACTION_STOP));

		if (session != null)
			style.setMediaSession(session.getSessionToken());

		style.setShowActionsInCompactView(1);

		Notification notification = builder.build();

		notification.contentView.setOnClickPendingIntent(android.R.id.icon, preferenceIntent);
		notification.bigContentView.setOnClickPendingIntent(android.R.id.icon, preferenceIntent);
		
		notificationManager.notify(NOTIFICATION_ID, notification);
		startForeground(NOTIFICATION_ID, notification);
	}

	public void handleIntent(Intent intent)
	{
		if (intent == null || intent.getAction() == null)
			return;

		String action = intent.getAction();

		if (action.equalsIgnoreCase(ACTION_PLAY))
			controller.getTransportControls().play();

		else if (action.equalsIgnoreCase(ACTION_PAUSE))
			controller.getTransportControls().pause();

		else if (action.equalsIgnoreCase(ACTION_STOP))
			controller.getTransportControls().stop();

		else if (action.equalsIgnoreCase(ACTION_SKIP_NEXT))
			controller.getTransportControls().skipToNext();
			
		else if (action.equalsIgnoreCase(ACTION_SAVE))
		{
			try
			{
				File dir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_MUSIC);
				dir.mkdirs();
				
				InputStream in = new FileInputStream(FILE);
				OutputStream out = new FileOutputStream(dir + "/" + DateFormat.getDateFormat(this).format(new Date()).replace('/', '-') + "_" + DateFormat.getTimeFormat(this).format(new Date()) +  ".mid");

				byte[] buf = new byte[1024];
				int len;
				while ((len = in.read(buf)) > 0) 
				{
					out.write(buf, 0, len);
				}
				in.close();
				out.close();
				
				Toast.makeText(this, "Saved MIDI to music", Toast.LENGTH_SHORT).show();
			}
			catch (IOException e)
			{
				e.printStackTrace();
				
				Toast.makeText(this, "An error occured while trying to save the file :/", Toast.LENGTH_SHORT).show();
			}
		}
			
		notificationManager.cancel(NOTIFICATION_ID + 1);
	}

	@Override
	public IBinder onBind(Intent p1)
	{
		return null;
	}

	@Override
	public void onDestroy()
	{
		releaseMediaSession();
		
		if (session != null)
			session.release();

		PreferenceManager.getDefaultSharedPreferences(this).unregisterOnSharedPreferenceChangeListener(this);

		super.onDestroy();
	}

	@Override
	public boolean onError(MediaPlayer p1, int p2, int p3)
	{
		buildNotification(R.drawable.alert, "Error", ACTION_PLAY);

		releaseMediaSession();
		
		Notification n = new Notification.Builder(MainService.this)
			.setSmallIcon(R.drawable.alert)
			.setContentText("Emerald")
			.setContentTitle("An Error Occured").build();
			
		notificationManager.notify(NOTIFICATION_ID + 1, n);
		controller.getTransportControls().play();
		
		return true;
	}

	@Override
	public void onCompletion(MediaPlayer p1)
	{
		if (PreferenceManager.getDefaultSharedPreferences(this).getBoolean("skip", true))
			controller.getTransportControls().skipToNext();
			
		else
			controller.getTransportControls().pause();
	}

	@Override
	public void onPrepared(MediaPlayer p1)
	{
		preparing = false;

		if (wasPlaying)
			controller.getTransportControls().play();
			
		else
			buildNotification(R.drawable.play, "Play", ACTION_PLAY);
	}

	@Override
	public void onAudioFocusChange(int focusChange)
	{
		if (!PreferenceManager.getDefaultSharedPreferences(this).getBoolean("audioFocus", true))
			return;

		wasPlaying = false;

		switch (focusChange)
		{
			case AudioManager.AUDIOFOCUS_GAIN_TRANSIENT_MAY_DUCK:
			case AudioManager.AUDIOFOCUS_GAIN_TRANSIENT_EXCLUSIVE:
			case AudioManager.AUDIOFOCUS_GAIN_TRANSIENT:
			case AudioManager.AUDIOFOCUS_GAIN:
				if (player == null)
					initMediaSession();

				else if (!preparing && !player.isPlaying() && wasPlaying)
					controller.getTransportControls().play();

				if (player != null)
				{
					float volume = PreferenceManager.getDefaultSharedPreferences(this).getFloat("volume", 1);
					player.setVolume(volume, volume);
				}

				break;

			case AudioManager.AUDIOFOCUS_LOSS_TRANSIENT:
			case AudioManager.AUDIOFOCUS_LOSS:
				if (!preparing && player != null)
				{
					wasPlaying = player.isPlaying();

					if (player.isPlaying())
						controller.getTransportControls().pause();
				}
				break;

			case AudioManager.AUDIOFOCUS_LOSS_TRANSIENT_CAN_DUCK:
				if (!preparing && player != null)
				{
					if (player.isPlaying())
					{
						float volume = PreferenceManager.getDefaultSharedPreferences(this).getFloat("volume", 1);
						player.setVolume(volume * 0.1F, volume * 0.1F);
					}

					wasPlaying = player.isPlaying();
				}

				break;
		}
	}

	@Override
	public void onSharedPreferenceChanged(SharedPreferences p1, String key)
	{
		if (key.equals("volume") && !preparing && player != null && player.isPlaying())
		{
			float volume = PreferenceManager.getDefaultSharedPreferences(this).getFloat("volume", 1);
			player.setVolume(volume, volume);
		}
	}
}
