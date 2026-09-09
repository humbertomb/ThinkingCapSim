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
public class LindaEntryFilter
{
	static public final String			ANY 		= "any";
	
	protected String					pattern;
	
	// Constructors
	public LindaEntryFilter (String pattern)
	{
		if (pattern != null)
			this.pattern		= pattern;	
		else
			this.pattern		= ANY;
	}
	
	// Instance methods
	public boolean matches (LindaEntryFilter other)
	{
		return matches (other.pattern);
	}
	
	public boolean matches (String filter)
	{
		if (filter == null)
			return true;
		
		if (pattern.equals (ANY) || filter.equals (ANY))
			return true;
		
		return pattern.equals (filter);
	}
	
	public boolean matches_exact (String filter)
	{
		if (filter == null)
			return false;
		
		return pattern.equals (filter);
	}
	
	public String toString ()
	{
		return pattern;
	}
}
