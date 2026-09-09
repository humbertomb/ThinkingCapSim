/*
 * Created on 08-mar-2004
 *
 * To change the template for this generated file go to
 * Window - Preferences - Java - Code Generation - Code and Comments
 */
package tc.shared.linda;

import java.util.*;

/**
 * @author Humberto Martinez Barbera
 *
 * To change the template for this generated type comment go to
 * Window - Preferences - Java - Code Generation - Code and Comments
 */
public class LindaEntry
{
	// Linda space data
	protected Tuple							tuple;
	protected Vector<LindaEntryRegistry>	listeners;
	
	// Administrative and accounting data
	protected int							rio;			// Read operations
	protected int							wio;			// Write operations
	protected int							eio;			// Event notifications
	
	// Constructors
	public LindaEntry (String space, String key)
	{
		rio			= 0;
		wio			= 0;
		eio			= 0;
		
		tuple		= new Tuple (space, key, null);
		listeners	= new Vector<LindaEntryRegistry> ();
	}
	
	// Accessors
	public final String			key ()			{ return tuple.key; }
	public final boolean		hasValue ()		{ return tuple.value != null; }
	
	// Instance methods
	public void update_local (Tuple ntuple)
	{
		wio ++;	
		
		tuple.space		= ntuple.space;
		tuple.value		= ntuple.value;
	}
	
	public Tuple update_remote (LindaEntryListener receiver)
	{
		if (tuple.value == null)			return null;
		
		if (receiver == null) 
			rio ++;
		else if (receiver.answer (tuple, receiver.getConnection ()))
			rio ++;
				
		return tuple;
	}
	
	public void update_listeners (LindaConnection connection)
	{
		LindaEntryRegistry	registry;
		
		if (tuple.value == null)			return;
		
		// Hay que hacer una copia del vector listeners, debido a con la tupla delrobot se eliminan los elementos del 
		//  vector antes de que se notifiquen la tupla a todos los listeners
		LindaEntryRegistry ler[] = (LindaEntryRegistry[]) listeners.toArray(new LindaEntryRegistry[0]);
		for(int j=0; j<ler.length;j++)
		{
			registry = ler[j];
			if (registry.filter.matches (tuple.space) && !registry.listener.matches (connection))
			{
				if (registry.listener.notify (tuple, registry.listener.getConnection ()))
				{
					registry.eio ++;
					eio ++;
				}
			}
		}
	}

	public void update_listener (LindaEntryListener listener, LindaConnection connection)
	{	
		if (tuple.value == null)			return;
				
		if (listener.notify (tuple, connection))
			eio ++;
	}
	
	public void register (String space, LindaEntryListener listener)
	{
		Enumeration			enu;
		LindaEntryRegistry	registry;
		boolean				update = true;

		enu = listeners.elements ();
		while (enu.hasMoreElements ()) 
		{
			registry = (LindaEntryRegistry) enu.nextElement ();
			if (registry.matches (space, listener.getConnection ()))
				update = false;
		}
		
		if (update)
			listeners.add (new LindaEntryRegistry (space, listener));
	}
	
	public void unregister (String space, LindaEntryListener listener) 
	{
		Enumeration			enu;
		LindaEntryRegistry	registry;
		
		enu = listeners.elements ();
		while (enu.hasMoreElements ()) 
		{
			registry = (LindaEntryRegistry) enu.nextElement ();
			if (registry.matches (space, listener.getConnection ()))
			{
				listeners.remove (registry);
				break;
			}
		}
	}
	
	public void unregister (String space) 
	{
		Enumeration			enu;
		LindaEntryRegistry	registry;
		
		enu = listeners.elements ();
		while (enu.hasMoreElements ()) 
		{
			registry = (LindaEntryRegistry) enu.nextElement ();
			if (registry.filter.matches_exact (space))
				listeners.remove (registry);
		}
	}
	
	public void unregister (LindaEntryListener con) 
	{
		Enumeration			enu;
		LindaEntryRegistry	registry;
		
		enu = listeners.elements ();
		while (enu.hasMoreElements ()) 
		{
			registry = (LindaEntryRegistry) enu.nextElement ();
			if (registry.matchesConnection(con.getConnection())){
				//System.out.println("LindaEntry unregistry("+con+") eliminando entrada "+registry.filter.toString());
				listeners.remove (registry);
			}
		}
	}
	
	public String toHTML (boolean expand)
	{
		Enumeration			enu;
		LindaEntryRegistry	registry;
		String				output;
		
		output	= "[<B>" + tuple.key + "</B>]  W=<B>" + wio + "</B>, R=<B>" + rio + "</B>, E=<B>" + eio + "</B> (L=<B>" + listeners.size () + "</B>)";
		
		enu 	= listeners.elements ();
		if (expand && enu.hasMoreElements ())
		{
			output	+= "<BR>";
			while (enu.hasMoreElements ()) 
			{
				registry= (LindaEntryRegistry) enu.nextElement ();
				output	+= "&nbsp;&nbsp;&nbsp;&nbsp;* " + registry.filter + " => " + registry.listener + " E=<B>" + registry.eio + "</B><BR>";		
			}
		}
		else
			output	+= "<BR>";
		
		return output;
	}
	
	public String toString (boolean expand)
	{
		Enumeration			enu;
		LindaEntryRegistry	registry;
		String				output;
		
		output	= "[" + tuple.key + "]  W=" + wio + ", R=" + rio + ", E=" + eio + " (L=" + listeners.size () + ")";
		
		enu 	= listeners.elements ();
		if (expand && enu.hasMoreElements ())
		{
			output	+= "\n";
			while (enu.hasMoreElements ()) 
			{
				registry= (LindaEntryRegistry) enu.nextElement ();
				output	+= "    >> " + registry.filter + " => " + registry.listener + " E=" + registry.eio + "\n";		
			}
		}
		else
			output	+= "\n";
		
		return output;
	}
	
	public String toString ()
	{
		return "[" + tuple.key + "]  W=" + wio + ", R=" + rio + ", E=" + eio;
	}
}
