/**
 * Created on 26-jun-2006
 *
 * @author Humberto  Martinez Barbera
 */
package tcrob.umu.soccer.gm;

import tcrob.umu.soccer.gm.data.*;
import wucore.widgets.*;

public interface Localisation
{
	public Gs getGs ();
	public WorldModel getWorldModel ();
	public boolean getLastUpdated (int index);

	public void updateMotionOnly (Odometry odo);
	public void updateMotionAndSensors (Odometry odo, Lps lps);
	public void drawElements (Model2D model);
	
	public void setGT(GsPosition pos);
	
	public String getId();
	public void setId(String newid);
}
