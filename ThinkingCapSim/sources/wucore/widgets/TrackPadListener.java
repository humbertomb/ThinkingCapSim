/*
 * (c) 2002 Humberto Martinez Barbera
 */

package wucore.widgets;

public interface TrackPadListener
{
	public void updatePosition (double x, double y, double z);
	public void updateButtons (int buttons);
}