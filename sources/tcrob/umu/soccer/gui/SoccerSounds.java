/*
 * (c) 2026 Humberto Martinez Barbera
 */

package tcrob.umu.soccer.gui;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.SourceDataLine;

/**
 * What a match sounds like, made up rather than read from files: the whistle
 * of a football referee -- a pea whistle, a shrill note trilled by the pea --
 * and the applause of a crowd. One whistle at a kick-off, a short one at a
 * fault, and two short and a long, then the applause, when the time is up
 * ({@link #play}). The sounds are made once, when first asked for, and played
 * one after the other on a thread of their own; where there is nothing to
 * play them on (no sound card, a headless run) nothing happens.
 */
public class SoccerSounds
{
	/** The cues a decision may carry: a kick-off, a fault, the end of the match. */
	static public final String		START	= "start";
	static public final String		FAULT	= "fault";
	static public final String		END		= "end";

	static public final float		RATE	= 44100f;
	static public final int			SHORT	= 220;					// a short blow [ms]
	static public final int			KICKOFF	= 750;					// the blow of a kick-off [ms]
	static public final int			LONG	= 1100;					// the long blow of the end [ms]
	static public final int			GAP		= 130;					// between the blows of the end [ms]
	static public final int			APPLAUSE = 4200;				// how long the crowd claps [ms]

	static private byte[]			shortBlow, kickoffBlow, longBlow, applause, silence;
	static private final List<byte[]>	queue = new ArrayList<byte[]> ();
	static private Thread			player;
	static private boolean			mute;							// no line to play on: found out once, and kept quiet after

	/** Plays what a cue says, after whatever is playing; an unknown or null cue plays nothing. */
	static public void play (String cue)
	{
		if ((cue == null) || mute)				return;
		synchronized (SoccerSounds.class)
		{
			make ();
			if (START.equals (cue))				queue.add (kickoffBlow);
			else if (FAULT.equals (cue))		queue.add (shortBlow);
			else if (END.equals (cue))
			{
				queue.add (shortBlow);	queue.add (silence);
				queue.add (shortBlow);	queue.add (silence);
				queue.add (longBlow);	queue.add (silence);
				queue.add (applause);
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

	/** Nothing is played from now on (and whatever is queued is dropped). */
	static public void mute (boolean m)
	{
		synchronized (SoccerSounds.class)		{ mute = m;	if (m) queue.clear (); }
	}

	/* ------------------------------------------------------------------ */
	/* Playing                                                             */
	/* ------------------------------------------------------------------ */

	static private void drain ()
	{
		AudioFormat		fmt = new AudioFormat (RATE, 16, 1, true, false);
		SourceDataLine	line = null;

		try
		{
			line	= AudioSystem.getSourceDataLine (fmt);
			line.open (fmt, 1 << 16);
			line.start ();
			while (true)
			{
				byte[]		next;

				synchronized (SoccerSounds.class)
				{
					if (queue.isEmpty ())		break;
					next	= queue.remove (0);
				}
				line.write (next, 0, next.length);
			}
			line.drain ();
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

	/* ------------------------------------------------------------------ */
	/* Making the sounds                                                   */
	/* ------------------------------------------------------------------ */

	static private void make ()
	{
		if (shortBlow != null)		return;
		shortBlow	= whistle (SHORT);
		kickoffBlow	= whistle (KICKOFF);
		longBlow	= whistle (LONG);
		applause	= applause (APPLAUSE);
		silence		= pcm (new double[(int) (RATE * GAP / 1000)]);
	}

	/**
	 * A pea whistle blown for so long: a shrill note (a fundamental and two
	 * partials) that the pea trills at about 40 times a second, a little breath
	 * noise under it, a quick attack and a short tail.
	 */
	static protected byte[] whistle (int ms)
	{
		int			n = (int) (RATE * ms / 1000);
		double[]	s = new double[n];
		Random		rnd = new Random (7);
		double		f0 = 2650.0, trill = 42.0;

		for (int i = 0; i < n; i++)
		{
			double	t = i / RATE;
			double	env = Math.min (1.0, t / 0.012) * Math.min (1.0, (ms / 1000.0 - t) / 0.05);
			double	pea = 0.55 + 0.45 * Math.sin (2 * Math.PI * trill * t + 0.6 * Math.sin (2 * Math.PI * 5.0 * t));	// the trill, itself a little uneven
			double	f = f0 * (1.0 + 0.012 * Math.sin (2 * Math.PI * trill * t));								// and a little pitch wobble with it
			double	tone = Math.sin (2 * Math.PI * f * t) + 0.45 * Math.sin (2 * Math.PI * 2 * f * t) + 0.18 * Math.sin (2 * Math.PI * 3 * f * t);

			s[i]	= env * (0.55 * pea * tone + 0.04 * (rnd.nextDouble () - 0.5));
		}
		return pcm (s);
	}

	/**
	 * A crowd clapping for so long: many claps, each a short burst of noise with
	 * a body of its own, falling at random, the crowd joining in over the first
	 * half second and dying away over the last second and a half.
	 */
	static protected byte[] applause (int ms)
	{
		int			n = (int) (RATE * ms / 1000);
		double[]	s = new double[n];
		Random		rnd = new Random (11);
		double		len = ms / 1000.0;
		double		t = 0.0;

		while (t < len)
		{
			double	env = Math.min (1.0, t / 0.5) * Math.min (1.0, (len - t) / 1.5);
			double	amp = env * (0.25 + 0.5 * rnd.nextDouble ());
			double	decay = 0.006 + 0.010 * rnd.nextDouble ();										// how long the clap rings [s]
			double	tone = 900.0 + 1400.0 * rnd.nextDouble ();										// the body of the clap (a big hand, a small one)
			int		at = (int) (t * RATE), k = (int) (RATE * decay * 5);
			double	ph = rnd.nextDouble () * 2 * Math.PI;

			for (int i = 0; (i < k) && (at + i < n); i++)
			{
				double	tt = i / RATE;

				s[at + i]	+= amp * Math.exp (-tt / decay) * ((rnd.nextDouble () - 0.5) * 1.2 + 0.5 * Math.sin (2 * Math.PI * tone * tt + ph));
			}
			t	+= 0.004 + 0.020 * rnd.nextDouble () / Math.max (0.15, env);						// a denser crowd while it is at full clap
		}
		// a light hall to it: the sound smeared a little
		double	y = 0.0;
		for (int i = 0; i < n; i++)		{ y = 0.6 * y + 0.4 * s[i];	s[i] = 0.5 * s[i] + 0.5 * y; }
		return pcm (s);
	}

	/** Samples (about -1..1, clipped) as 16 bit little endian PCM. */
	static protected byte[] pcm (double[] s)
	{
		byte[]	b = new byte[2 * s.length];

		for (int i = 0; i < s.length; i++)
		{
			int		v = (int) Math.round (Math.max (-1.0, Math.min (1.0, s[i])) * 32000.0);

			b[2 * i]		= (byte) (v & 0xff);
			b[2 * i + 1]	= (byte) ((v >> 8) & 0xff);
		}
		return b;
	}

	/** Blows the three of them and claps, to hear what they are like. */
	static public void main (String[] args) throws Exception
	{
		play (START);	Thread.sleep (1500);
		play (FAULT);	Thread.sleep (1000);
		play (END);
		Thread.sleep (9000);
	}
}
