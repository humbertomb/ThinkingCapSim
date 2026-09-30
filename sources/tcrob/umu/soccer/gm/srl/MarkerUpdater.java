/**
 * Created on 28-jun-2006
 *
 * @author Humberto Martinez Barbera
 * @author Scott Lenser (CMU)
 */

package tcrob.umu.soccer.gm.srl;

import tcrob.umu.soccer.gm.data.*;

import wucore.utils.math.*;
import wucore.utils.math.stat.*;

public class MarkerUpdater 
{
	protected GaussianEvaluator		gaussianEvaluator;
	protected double					markerX;
	protected double					markerY;	


	public MarkerUpdater() 
	{
		gaussianEvaluator = new GaussianEvaluator (2);
	}
	
	public void setMinProb (double min_prob) 
	{
		gaussianEvaluator.setMinProb (min_prob);
	}
	
	public void setMarkerLoc (double x, double y) 
	{
		markerX = x;
		markerY = y;
	}
	
	public void updateSamples (LocLpo lpo, double rhoStdDev, double thetaStdDev, LocaleSampled locale) 
	{
		gaussianEvaluator.setMeanDev (0, lpo.rho, rhoStdDev);
		gaussianEvaluator.setMeanDev (1, lpo.theta, thetaStdDev);
		
		for (int i = 0; i < locale.numSamples; i++)
			updateSample (locale.sample[i].data);
	}
	
	protected void updateSample (double[] pos)
	{
		double		dx, dy;
		double		rho, theta;

		dx		= markerX - pos[0];
		dy		= markerY - pos[1];	
		rho		= Functions.hypot (dx, dy);
		theta	= Angles.radnorm_180 (Math.atan2 (dy, dx) - pos[2]);

		while (theta >= gaussianEvaluator.getMean(1) + Math.PI)
			theta -= 2 * Math.PI;
		while (theta <  gaussianEvaluator.getMean(1) - Math.PI)
			theta += 2 * Math.PI;
		
		// Update particle's weight
		pos[3]	= pos[3] * gaussianEvaluator.evaluate (0, rho) * gaussianEvaluator.evaluate (1, theta);
	}
}
