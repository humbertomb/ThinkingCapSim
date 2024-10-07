/*
 * Created on 08-mar-2004
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
public interface LindaEntryListener
{
	public boolean notify (Tuple tuple, LindaConnection connection);
	public boolean answer (Tuple tuple, LindaConnection connection);
	public boolean matches (LindaConnection connection);
	public LindaConnection getConnection ();
}
