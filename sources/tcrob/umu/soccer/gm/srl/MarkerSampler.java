/**
 * Created on 28-jun-2006
 *
 * @author Humberto Martinez Barbera
 * @author Scott Lenser (CMU)
 */
package tcrob.umu.soccer.gm.srl;

import tcrob.umu.soccer.gm.*;

import wucore.utils.math.*;
import wucore.utils.math.stat.*;

public class MarkerSampler 
{
	static public final double		CAMERA_FOV	= 56.9 * Angles.DTOR;			// rad
	static public final double		HEAD_PAN	= 70.0 * Angles.DTOR;			// rad
	static public final double		PAN_VIEW	= HEAD_PAN + CAMERA_FOV * 0.5;

	protected GaussianSampler			gaussianSampler;
	protected RandomNumberGenerator	random;
	
	protected double					markerX;			// Global X position of marker
	protected double					markerY;			// Global Y position of marker
	protected double					markerTH;		// Global heading to marker
	protected double					markerTHi;		// Inverted heading to marker
	
	public MarkerSampler ()
	{
		gaussianSampler	= new GaussianSampler (2);
		random			= new RandomNumberGenerator ();
	}
	
	public void generateSamples (MarkerUpdater updater, LocaleSampled locale, int top) 
	{
		markerX		= updater.markerX;
		markerY		= updater.markerY;
		markerTH		= Math.atan2 (markerY, markerX);
		markerTHi	= Math.atan2 (-markerY, -markerX);
		
		gaussianSampler.setMeanDev (0, updater.gaussianEvaluator.getMean (0), updater.gaussianEvaluator.getStdDev (0));
		gaussianSampler.setMeanDev (1, updater.gaussianEvaluator.getMean (1), updater.gaussianEvaluator.getStdDev (1));
		
		for (int i = 0; i < top; i++)
			generateSample (locale.sample[i].data);
	}

	public void generateSample (double[] pos)
	{
		double		x, y, theta;
		double		pan, from;
		
		do
		{		
			gaussianSampler.generateSample (pos);
			
			pan		= random.nextUniform (-PAN_VIEW, PAN_VIEW);
			from		= markerTHi + pan;
			
			x		= markerX + pos[0] * Math.cos (from);
			y		= markerY + pos[0] * Math.sin (from);
			theta	= Angles.radnorm_180 (markerTH + pan - pos[1]);
		}
		while (!Localisation.onField (x, y));
		
		pos[0] = x;
		pos[1] = y;
		pos[2] = theta;
		pos[3] = 1.0;
	}
}
