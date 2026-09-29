/*
 * (c) 2026 Humberto Martinez Barbera
 */

package tc.modules;

import java.util.ArrayList;
import java.util.List;

import tc.runtime.thread.*;
import tc.shared.linda.*;
import tc.shared.world.World;

/**
 * A module that watches over what happens in the world rather than driving a
 * robot: a referee, an observer of a mission. It knows the world (from the
 * settings, WORLD, or from the configuration item every module receives), it
 * keeps the clock of what it watches -- a match, a mission -- which runs with
 * START, pauses with STOP and goes back to the start with RESET, and it says
 * what it decides ({@link #announce}), one line at a time, for whoever listens:
 * a window, the console, a log.
 *
 * What is watched, and what is decided about it, is up to the subclass
 * ({@link #supervise}), which is called every cycle while the module runs.
 *
 * <pre>
 *   WORLD       the world file, when it is not to be waited for from the configuration
 *   DURATION    how long what is watched lasts, in seconds (600); 0 for no end
 * </pre>
 */
public abstract class Supervisor extends StdThread
{
	/** How long what is watched lasts by default [s]: a match of ten minutes. */
	static public final int			DURATION	= 600;

	/** One thing the supervisor decided: when (ms into what is watched) and what. */
	static public class Decision
	{
		public final long			at;
		public final String			text;
		/** What it sounds like, for whoever plays sounds (a whistle, say); null for nothing. What it means is the subclass's. */
		public final String			cue;

		public Decision (long at, String text)
		{
			this (at, text, null);
		}

		public Decision (long at, String text, String cue)
		{
			this.at		= at;
			this.text	= text;
			this.cue	= cue;
		}

		/** When, as m:ss. */
		public String when ()				{ return clock (at); }

		public String toString ()			{ return "[" + when () + "] " + text; }
	}

	/** Whoever wants to know what the supervisor decides and how its clock goes. */
	public interface Listener
	{
		/** Something was decided. */
		public void decided (Decision d);
		/** The clock started, paused, ran out or went back to the start; the state may have changed with it. */
		public void changed ();
	}

	protected World					world;					// what is watched happens in it
	protected String				robotid;

	// The clock of what is watched
	protected long					duration;				// how long it lasts [ms]; 0 for no end
	protected long					accumulated;			// how long it had run before the last start [ms]
	protected long					since;					// when it was last started [ms of the system], 0 while paused
	protected boolean				over;					// it ran out

	protected final List<Decision>	decisions	= new ArrayList<Decision> ();
	protected final List<Listener>	listeners	= new ArrayList<Listener> ();

	public Supervisor (ModuleConfig config, Linda linda)
	{
		super (config, linda);

		robotid		= config.robot ();
	}

	protected void initialise (ModuleConfig cfg)
	{
		String		wname = cfg.get ("WORLD");

		duration	= cfg.getInt ("DURATION", DURATION) * 1000L;
		if (wname != null)
			try { world = new World (wname); }
			catch (Exception e) { System.out.println ("  [Supervisor] Cannot read the world <" + wname + ">: " + e.getMessage ()); }
	}

	/** The world comes with the configuration, unless the settings named one. */
	public void notify_config (String space, ItemConfig item)
	{
		if ((world == null) && (item.world != null))
			world	= World.fromJsonText (item.world);
	}

	public final World				world ()			{ return world; }
	public final String				robot ()			{ return robotid; }

	/* ------------------------------------------------------------------ */
	/* The clock                                                           */
	/* ------------------------------------------------------------------ */

	/** START runs the clock, STOP pauses it, RESET puts it back to the start; the rest is the module's. */
	public void notify_execution (String space, ItemExecution item)
	{
		super.notify_execution (space, item);

		if (item.operation != ItemExecution.COMMAND)		return;
		switch (item.command)
		{
		case ItemExecution.START:
		case ItemExecution.STEP:	resume ();		break;
		case ItemExecution.STOP:	pause ();		break;
		default:
		}
	}

	/** The clock runs, from where it was. */
	protected void resume ()
	{
		if (over || (since != 0))					return;
		since	= System.currentTimeMillis ();
		changed ();
	}

	/** The clock stands still. */
	protected void pause ()
	{
		if (since == 0)								return;
		accumulated	+= System.currentTimeMillis () - since;
		since		= 0;
		changed ();
	}

	/**
	 * Back to the start: the clock at zero and standing, nothing decided yet.
	 * Whoever keeps more ({@link #restart}) forgets it too.
	 */
	protected void reset ()
	{
		super.reset ();
		accumulated	= 0;
		since		= 0;
		over		= false;
		synchronized (decisions)		{ decisions.clear (); }
		restart ();
		changed ();
	}

	/** How long what is watched has been running [ms]. */
	public long elapsed ()
	{
		long	e = accumulated + ((since != 0) ? (System.currentTimeMillis () - since) : 0);

		return (duration > 0) ? Math.min (e, duration) : e;
	}

	/** How long is left [ms], or -1 when there is no end. */
	public long remaining ()
	{
		return (duration > 0) ? Math.max (0, duration - elapsed ()) : -1;
	}

	public final long				duration ()			{ return duration; }
	/** Whether the clock is running. */
	public final boolean			ticking ()			{ return since != 0; }
	/** Whether it ran out. */
	public final boolean			isOver ()			{ return over; }

	/** A time as m:ss (or h:mm:ss past the hour). */
	static public String clock (long ms)
	{
		long	s = Math.max (0, ms) / 1000;
		long	m = s / 60;

		s	%= 60;
		if (m >= 60)			return String.format ("%d:%02d:%02d", m / 60, m % 60, s);
		return String.format ("%d:%02d", m, s);
	}

	/* ------------------------------------------------------------------ */
	/* Running                                                             */
	/* ------------------------------------------------------------------ */

	public void step (long ctime)
	{
		// the clock runs out once, and what is watched is over from then on
		if (!over && (duration > 0) && (elapsed () >= duration))
		{
			over	= true;
			pause ();
			timeUp ();
		}
		if (world == null)							return;
		supervise (ctime);
	}

	/** One look at what is watched: what happened since the last one, and what is decided about it. */
	protected abstract void supervise (long ctime);

	/** The clock ran out: what is watched is over. Says so, unless the subclass has more to say. */
	protected void timeUp ()						{ announce ("Time is up"); }

	/** Starts over what the subclass keeps (a score, a count), after RESET. */
	protected void restart ()						{ }

	/* ------------------------------------------------------------------ */
	/* Saying it                                                           */
	/* ------------------------------------------------------------------ */

	/** Something was decided: it is kept, said on the console and given to whoever listens. */
	protected Decision announce (String text)
	{
		return announce (text, null);
	}

	/** The same, with what it sounds like (see {@link Decision#cue}). */
	protected Decision announce (String text, String cue)
	{
		Decision	d = new Decision (elapsed (), text, cue);

		synchronized (decisions)		{ decisions.add (d); }
		System.out.println ("  [" + ((tdesc != null) ? tdesc.preffix : "Supervisor") + "] " + d);
		for (Listener l : listeners ())	l.decided (d);
		return d;
	}

	/** What has been decided so far, oldest first. */
	public List<Decision> decisions ()
	{
		synchronized (decisions)		{ return new ArrayList<Decision> (decisions); }
	}

	public void addListener (Listener l)			{ synchronized (listeners) { if (!listeners.contains (l)) listeners.add (l); } }
	public void removeListener (Listener l)			{ synchronized (listeners) { listeners.remove (l); } }

	protected List<Listener> listeners ()
	{
		synchronized (listeners)		{ return new ArrayList<Listener> (listeners); }
	}

	/** The clock or the state changed: whoever listens is told. */
	protected void changed ()
	{
		for (Listener l : listeners ())	l.changed ();
	}
}
