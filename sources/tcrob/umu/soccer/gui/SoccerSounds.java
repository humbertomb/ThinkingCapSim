/*
 * (c) 2026 Humberto Martinez Barbera
 */

package tcrob.umu.soccer.gui;

import java.io.BufferedInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.SourceDataLine;

/**
 * What a match sounds like: the whistle of the referee and the applause of the
 * crowd, read from the WAV files of <code>resources/sounds</code> (on the
 * class path, or under the working directory) and played one after the other
 * on a thread of their own. One long blow at a kick-off; a short one at a
 * fault; a blow and a couple of seconds of applause at a goal; two short blows
 * and a long one, then the applause, when the time is up ({@link #play}). A
 * sound may be played for only so long, fading out where it is cut. Where
 * there is nothing to play on (no sound card, a headless run), or the files
 * are not there, it says so once and keeps quiet.
 *
 * <pre>
 *   resources/sounds/whistle_short.wav   a short blow of a referee's whistle
 *   resources/sounds/whistle_long.wav    a long one
 *   resources/sounds/applause.wav        a crowd clapping, a few seconds of it
 * </pre>
 */
public class SoccerSounds
{
	/** The cues a decision may carry: a kick-off, a fault, a goal, the end of the match. */
	static public final String		START	= "start";
	static public final String		FAULT	= "fault";
	static public final String		GOAL	= "goal";
	static public final String		END		= "end";

	/** Where the files are: as a resource of the class path, or under the working directory. */
	static public final String		FOLDER	= "resources/sounds";
//	static public final String		WHISTLE_SHORT	= "whistle_short.wav";
//	static public final String		WHISTLE_LONG	= "whistle_long.wav";
//	static public final String		APPLAUSE		= "applause.wav";
	static public final String		WHISTLE_SHORT	= "whistle_short_toot.wav";
	static public final String		WHISTLE_LONG	= "wistle_short_blow.wav";
	static public final String		APPLAUSE		= "crowd_cheer.wav";

	static public final int			GAP				= 150;		// between the blows of the end [ms]
	static public final int			GOAL_APPLAUSE	= 2500;		// how long the crowd claps a goal [ms]
	static public final int			END_APPLAUSE	= 4500;		// and the end of the match [ms]
	static public final int			FADE			= 200;		// the fade out where a sound is cut short [ms]

	/** A sound as read: its samples (signed 16 bit little endian PCM) and their format. */
	static protected class Sound
	{
		AudioFormat					format;
		byte[]						pcm;
	}

	/** One thing to play: a sound, for so long (0: all of it); or a silence, when the sound is null. */
	static private class Item
	{
		Sound						sound;
		int							ms;

		Item (Sound sound, int ms)	{ this.sound = sound;	this.ms = ms; }
	}

	static private final Map<String, Sound>	sounds = new HashMap<String, Sound> ();
	static private final List<Item>	queue = new ArrayList<Item> ();
	static private Thread			player;
	static private boolean			mute;							// nothing to play on, or no files: found out once, and quiet after

	/** Plays what a cue says, after whatever is playing; an unknown or null cue plays nothing. */
	static public void play (String cue)
	{
		if ((cue == null) || mute)				return;
		synchronized (SoccerSounds.class)
		{
			Sound	shortBlow = sound (WHISTLE_SHORT), longBlow = sound (WHISTLE_LONG), applause = sound (APPLAUSE);

			if (mute)							return;
			if (START.equals (cue))				queue.add (new Item (longBlow, 0));
			else if (FAULT.equals (cue))		queue.add (new Item (shortBlow, 0));
			else if (GOAL.equals (cue))			{ queue.add (new Item (longBlow, 0));	queue.add (new Item (applause, GOAL_APPLAUSE)); }
			else if (END.equals (cue))
			{
				queue.add (new Item (shortBlow, 0));	queue.add (new Item (null, GAP));
				queue.add (new Item (shortBlow, 0));	queue.add (new Item (null, GAP));
				queue.add (new Item (longBlow, 0));		queue.add (new Item (null, GAP));
				queue.add (new Item (applause, END_APPLAUSE));
			}
			else								return;
			if ((player == null) || !player.isAlive ())
			{
				player	= new Thread (new Runnable () { public void run () { drain (); } }, "SoccerSounds");
				player.setDaemon (true);
				player.start ();
			}
		}
	}

	/** Nothing is played from now on (and whatever is queued is dropped); false lets it play again. */
	static public void mute (boolean m)
	{
		synchronized (SoccerSounds.class)		{ mute = m;	if (m) queue.clear (); }
	}

	/* ------------------------------------------------------------------ */
	/* Reading the files                                                   */
	/* ------------------------------------------------------------------ */

	/** A sound by its file name, read the first time and kept; null (and everything muted) when it cannot be. */
	static protected Sound sound (String name)
	{
		Sound	s = sounds.get (name);

		if (s != null)							return s;
		try
		{
			InputStream		in = open (name);

			if (in == null)						throw new java.io.FileNotFoundException (FOLDER + "/" + name + " (class path or working directory)");

			AudioInputStream	ais = AudioSystem.getAudioInputStream (new BufferedInputStream (in));
			AudioFormat			base = ais.getFormat ();
			AudioFormat			fmt = new AudioFormat (AudioFormat.Encoding.PCM_SIGNED, base.getSampleRate (), 16, base.getChannels (),
													   2 * base.getChannels (), base.getSampleRate (), false);

			if (!fmt.matches (base))			ais = AudioSystem.getAudioInputStream (fmt, ais);		// whatever it was written as, 16 bit PCM

			ByteArrayOutputStream	out = new ByteArrayOutputStream ();
			byte[]					buf = new byte[1 << 14];
			int						n;

			while ((n = ais.read (buf)) > 0)	out.write (buf, 0, n);
			ais.close ();
			s			= new Sound ();
			s.format	= fmt;
			s.pcm		= out.toByteArray ();
			sounds.put (name, s);
			return s;
		}
		catch (Throwable e)
		{
			System.out.println ("  [SoccerSounds] No sound: cannot read " + name + ": " + e);
			mute	= true;
			queue.clear ();
			return null;
		}
	}

	/** The file, from the class path first (resources/sounds/... next to the classes) and then from the working directory. */
	static protected InputStream open (String name) throws Exception
	{
		InputStream		in = SoccerSounds.class.getResourceAsStream ("/" + FOLDER + "/" + name);

		if (in != null)							return in;
		for (String dir : new String[] { FOLDER, "sources/" + FOLDER, "./conf/" + FOLDER })
		{
			File	f = new File (dir, name);

			if (f.isFile ())					return new FileInputStream (f);
		}
		return null;
	}

	/* ------------------------------------------------------------------ */
	/* Playing                                                             */
	/* ------------------------------------------------------------------ */

	static private void drain ()
	{
		SourceDataLine	line = null;
		AudioFormat		fmt = null;

		try
		{
			while (true)
			{
				Item		next;

				synchronized (SoccerSounds.class)
				{
					if (queue.isEmpty ())		break;
					next	= queue.remove (0);
				}
				if (next.sound == null)								// a silence, in the format of whatever was last played
				{
					if (fmt == null)			{ Thread.sleep (next.ms);	continue; }
					line.write (new byte[frames (fmt, next.ms) * fmt.getFrameSize ()], 0, frames (fmt, next.ms) * fmt.getFrameSize ());
					continue;
				}
				if ((line == null) || !next.sound.format.matches (fmt))	// another format: another line
				{
					if (line != null)			{ line.drain ();	line.close (); }
					fmt		= next.sound.format;
					line	= AudioSystem.getSourceDataLine (fmt);
					line.open (fmt, 1 << 16);
					line.start ();
				}

				byte[]		pcm = cut (next.sound, next.ms);

				line.write (pcm, 0, pcm.length);
			}
			if (line != null)					line.drain ();
		}
		catch (Throwable e)
		{
			System.out.println ("  [SoccerSounds] No sound: " + e);
			synchronized (SoccerSounds.class)	{ mute = true;	queue.clear (); }
		}
		finally
		{
			if (line != null)		{ try { line.stop (); line.close (); } catch (Throwable e) { } }
		}
	}

	static private int frames (AudioFormat fmt, int ms)
	{
		return (int) (fmt.getSampleRate () * ms / 1000.0);
	}

	/** The first so many ms of a sound, faded out at the cut; all of it for 0 or more than it has. */
	static protected byte[] cut (Sound s, int ms)
	{
		int		size = s.format.getFrameSize (), total = s.pcm.length / size;
		int		n = (ms <= 0) ? total : Math.min (total, frames (s.format, ms));

		if (n >= total)							return s.pcm;

		byte[]	out = new byte[n * size];
		int		fade = Math.min (n, frames (s.format, FADE)), ch = s.format.getChannels ();

		System.arraycopy (s.pcm, 0, out, 0, out.length);
		for (int i = n - fade; i < n; i++)
		{
			double	g = (n - 1 - i) / (double) fade;

			for (int c = 0; c < ch; c++)
			{
				int		k = (i * ch + c) * 2;
				int		v = (short) ((out[k] & 0xff) | (out[k + 1] << 8));

				v		= (int) Math.round (v * g);
				out[k]		= (byte) (v & 0xff);
				out[k + 1]	= (byte) ((v >> 8) & 0xff);
			}
		}
		return out;
	}

	/** Blows them all and claps, to hear what they are like. */
	static public void main (String[] args) throws Exception
	{
		play (START);	Thread.sleep (2000);
		play (FAULT);	Thread.sleep (1000);
		play (GOAL);	Thread.sleep (5000);
		play (END);
		Thread.sleep (9000);
	}
}
