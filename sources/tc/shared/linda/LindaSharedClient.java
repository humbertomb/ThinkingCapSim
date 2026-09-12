/*
 * (c) 2026 Humberto Martinez Barbera
 */

package tc.shared.linda;

import java.util.HashMap;
import java.util.Map;

/**
 * Linda client of a module that shares the Linda server of its own process
 * (execution mode "shared"). It delegates everything to the server, but the
 * tuples delivered to the module carry the robot identifier as space instead
 * of {@link LindaEntryFilter#ANY}, as the network clients do (they stamp the
 * robot id when writing): the modules then know which robot they belong to
 * (window titles, own-vs-other-robot checks). The tuples themselves are not
 * modified, so the Linda router, which relies on the local tuples being
 * written with space ANY, keeps working.
 */
public class LindaSharedClient implements Linda
{
	protected Linda						server;
	protected String					robotid;
	protected Map<LindaListener, LindaListener>	wrappers	= new HashMap<LindaListener, LindaListener> ();

	public LindaSharedClient (Linda server, String robotid)
	{
		this.server		= server;
		this.robotid	= robotid;
	}

	public Linda	getServer ()				{ return server; }
	public String	getRobotId ()				{ return robotid; }

	public boolean	write (Tuple tuple)			{ return server.write (tuple); }
	public Tuple	read (Tuple template)		{ return server.read (template); }
	public Tuple	take (Tuple template)		{ return server.take (template); }
	public void		stop ()						{ server.stop (); }

	public void register (Tuple template, final LindaListener listener)
	{
		LindaListener	w;
		synchronized (wrappers)
		{
			w = wrappers.get (listener);
			if (w == null)
			{
				w = new LindaListener ()
				{
					public void notify (Tuple tuple)
					{
						if ((tuple != null) && ((tuple.space == null) || tuple.space.equals (LindaEntryFilter.ANY)))
							tuple = new Tuple (robotid, tuple.key, tuple.value);		// local tuple: it is ours
						listener.notify (tuple);
					}
				};
				wrappers.put (listener, w);
			}
		}
		server.register (template, w);
	}

	public void unregister (Tuple template, LindaListener listener)
	{
		LindaListener	w;
		synchronized (wrappers)		{ w = wrappers.get (listener); }
		server.unregister (template, (w != null) ? w : listener);
	}
}
