/*
 * Created on 25-mar-2004
 *
 * To change the template for this generated file go to
 * Window - Preferences - Java - Code Generation - Code and Comments
 */
package tc.shared.linda.net;

/**
 * @author Humberto Martinez Barbera
 *
 * To change the template for this generated type comment go to
 * Window - Preferences - Java - Code Generation - Code and Comments
 */
public interface LindaNetProcessor
{
	public static final int DELETE = 1;
	
	public void process (LindaNet linda, LindaNetPacket packet);
	public void manage(int command,LindaNetListener listener,String robotid);
	
}
