/*
 * (c) 2002 Humberto Martinez Barbera
 */

package tclib.navigation.mapbuilding;

import tc.shared.world.*;

public interface MapBuilding
{
	public void fromWorld (World world);
	public void fromWorld (World world, String zone);
}