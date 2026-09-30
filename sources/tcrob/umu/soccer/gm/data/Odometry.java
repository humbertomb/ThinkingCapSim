/**
 * Created on 16-jun-2006
 *
 * @author Humberto Martinez Barbera
 */
package tcrob.umu.soccer.gm.data;

import java.util.*;

import wucore.utils.math.*;

public class Odometry
{
	public float dlin;			// mm
	public float dlat;			// mm
	public float drot;			// rad
	public float elin;
	public float elat;
	public float erot;
	
	public void reset ()
	{
		dlin			= 0.0f;
		dlat			= 0.0f;
		drot			= 0.0f;
		elin		= 0.0f;
		elat		= 0.0f;
		erot		= 0.0f;
	}
	
	public void fromVelocity (Velocity vel, OdometryErrors odomodel, float dt)
	{
		int indexlin = (int)(Math.abs(Math.ceil ((float) vel.vlin / Velocity.VxMaxForward * odomodel.nsets)));
		int indexlat = (int)(Math.abs(Math.ceil ((float) vel.vlat / Velocity.VyMaxLeft * odomodel.nsets)));
		int indexrot = (int)(Math.abs(Math.ceil (vel.vrot / Velocity.VthMaxLeft * odomodel.nsets)));

		// System.out.println ("Index lin="+indexlin+", lat="+indexlat+", rot="+indexrot);
		
		float slippage_factor_lin = odomodel.errors[indexlin].dlin;
		float slippage_factor_lat = odomodel.errors[indexlat].dlat;
		float slippage_factor_rot = odomodel.errors[indexrot].drot;
		
		dlin		= (float) vel.vlin * slippage_factor_lin * dt;
		dlat		= (float) vel.vlat * slippage_factor_lat * dt;
		drot		= vel.vrot * (float) Angles.DTOR * slippage_factor_rot * dt;	
		
		// Not correct, errors should be added proportionally to the displacement.
		elin	= odomodel.errors[indexlin].elin * dt;
		elat	= odomodel.errors[indexlat].elat * dt;
		erot	= odomodel.errors[indexrot].erot * dt;
	}

	public void translate (Odometry odo)
	{
		double rho, phi;
		
		// Update position
		rho		= Math.sqrt (odo.dlin * odo.dlin + odo.dlat * odo.dlat);
		phi		= Math.atan2 (odo.dlat, odo.dlin) + 0.5f * odo.drot;
		
		dlin		= dlin + (float) (rho * Math.cos (drot + phi));
		dlat		= dlat + (float) (rho * Math.sin (drot + phi));
		drot		= drot + odo.drot;
		drot		= (float) Angles.radnorm_180 (drot);
		
		// Update uncertainty
		rho		= Math.sqrt (odo.elin * odo.elin + odo.elat * odo.elat);
		phi		= Math.atan2 (odo.elat, odo.elin) + 0.5 * odo.erot;
		
		elin	= elin + (float) (rho * Math.cos (erot + phi));
		elat	= elat + (float) (rho * Math.sin (erot + phi));
		erot	= erot + odo.erot;
		erot	= (float) Angles.radnorm_180 (erot);
	}
	
	public void fromLog (StringTokenizer st)
	{
		dlin			= Float.parseFloat (st.nextToken ());
		dlat			= Float.parseFloat (st.nextToken ());
		drot			= Float.parseFloat (st.nextToken ());
		elin		= Float.parseFloat (st.nextToken ());
		elat		= Float.parseFloat (st.nextToken ());
		erot		= Float.parseFloat (st.nextToken ());
	}
	
	public String toString ()
	{
		return "[" + dlin + ", " + dlat + ", " + (int) (drot * Angles.RTOD) + "]";
	}
}
