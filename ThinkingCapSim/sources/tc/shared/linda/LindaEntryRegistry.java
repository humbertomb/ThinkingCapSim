/*
 * Created on 10-mar-2004
 *
 * To change the template for this generated file go to
 * Window - Preferences - Java - Code Generation - Code and Comments
 */
package tc.shared.linda;

/**
 * @author Humberto Martinez Barbera
 *
 * To change the template for this generated type comment go to
 * Window - Preferences - Java - Code Generation - Code and Comments
 */
public class LindaEntryRegistry
{
	protected LindaEntryFilter				filter;
	protected LindaEntryListener			listener;
	public int								eio;
	
	// Constructors
	public LindaEntryRegistry (String pattern, LindaEntryListener listener)
	{
		this.filter		= new LindaEntryFilter (pattern);
		this.listener	= listener;
		this.eio		= 0;
	}
	
	// Instance methods
	public boolean matches (String pattern, LindaConnection connection)
	{
		return filter.matches (pattern)	&& listener.matches (connection);
	}
	
	public boolean matchesConnection (LindaConnection connection)
	{
		return listener.matches (connection);
	}
}
