/**
 * Created on 28-jun-2006
 *
 * @author Humberto Martinez Barbera
 * @author Scott Lenser (CMU)
 */
package tcrob.umu.soccer.gm.particle;

import tcrob.umu.soccer.gm.data.*;
import tcrob.umu.soccer.gm.*;
import wucore.utils.math.*;
import wucore.utils.math.stat.*;

public class MovementUpdater 
{
	static public final double		SIDE_OFFSET		= 40.0;
	static public final double		SIDE_STDDEV		= 40.0;
	static public final double		DIR_STDDEV		= 5.0 * Angles.DTOR;

	// The noise of the motion of every particle, so that they spread and keep up with what is seen: a part of the
	// displacement and of the turn, and some even when the robot stands (the odometry is never sure)
	static public final double		LIN_MIN			= 10.0;					// mm
	static public final double		LIN_FACTOR		= 0.15;					// of the displacement
	static public final double		ROT_MIN			= 1.0 * Angles.DTOR;	// rad
	static public final double		ROT_FACTOR		= 0.15;					// of the turn
	static public final double		ROT_PER_MM		= 0.03 * Angles.DTOR;	// rad per mm of displacement
	
	protected GaussianSampler			odometrySampler;	
	protected GaussianSampler			noiseSampler;
	
	protected double[]				noise;
	protected double[]				pos;
	
	public MovementUpdater ()
	{
		odometrySampler	= new GaussianSampler (3);
		odometrySampler.setRange (0,-Double.MAX_VALUE,+Double.MAX_VALUE);
		odometrySampler.setRange (1,-Double.MAX_VALUE,+Double.MAX_VALUE);
		odometrySampler.setRange (2,-Math.PI,+Math.PI);
		
		noiseSampler		= new GaussianSampler (3);
		noiseSampler.setRange (0,-Double.MAX_VALUE,+Double.MAX_VALUE);
		noiseSampler.setRange (1,-Double.MAX_VALUE,+Double.MAX_VALUE);
		noiseSampler.setRange (2,-Math.PI,+Math.PI);
		noiseSampler.setMeanDev (0,0.0,SIDE_STDDEV);
		noiseSampler.setMeanDev (1,0.0,SIDE_STDDEV);
		noiseSampler.setMeanDev (2,0.0,DIR_STDDEV);

		pos				= new double[3];
		noise			= new double[3];
	}
	
	/**
	 * Every particle moved as the odometry says, each with its own noise: the
	 * errors of the odometry when it has them, and at least the ones of the
	 * model of the motion (LIN_*, ROT_*). The Gaussian of each particle grows as
	 * much as the noise of the displacement.
	 */
	public void updateMotion (Odometry odo, GaussianSample[] samples) 
	{
		double		dist, sLin, sLat, sRot;
		
		dist	= Math.hypot (odo.dlin, odo.dlat);
		sLin	= Math.max (Math.abs (odo.elin), LIN_MIN + LIN_FACTOR * Math.abs (odo.dlin));
		sLat	= Math.max (Math.abs (odo.elat), LIN_MIN + LIN_FACTOR * Math.abs (odo.dlat));
		sRot	= Math.max (Math.abs (odo.erot), ROT_MIN + ROT_FACTOR * Math.abs (odo.drot) + ROT_PER_MM * dist);

		odometrySampler.setMeanDev (0, odo.dlin, sLin);
		odometrySampler.setMeanDev (1, odo.dlat, sLat);
		odometrySampler.setMeanDev (2, odo.drot, sRot);

		for (int i = 0; i < samples.length; i++)
			updateSample (samples[i], Math.hypot (sLin, sLat));
	}
		
	protected void updateSample (GaussianSample sample, double sigma)
	{
		// Add gaussian noise to the displacement	
		odometrySampler.generateSample (noise);		
		
		// Compute odometry translation
		pos[0]	= sample.g.getX () + noise[0] * Math.cos (sample.a) - noise[1] * Math.sin (sample.a); 
		pos[1]	= sample.g.getY () + noise[0] * Math.sin (sample.a) + noise[1] * Math.cos (sample.a);
		pos[2]	= Angles.radnorm_180 (sample.a + noise[2]);

		// Force the robot to be inside the field limits
		if (Localisation.placeOnField (pos, SIDE_OFFSET))
		{
			// Add Gaussian noise
			noiseSampler.generateSample (noise);		
			pos[0]	= sample.g.getX () + noise[0];
			pos[1]	= sample.g.getY () + noise[1];
			pos[2]	= Angles.radnorm_180 (sample.a + noise[2]);			
		}
		
		sample.g.addToCovariance (sigma);
		sample.g.setMean (pos[0], pos[1]);
		sample.a	 = pos[2];
	}
}
