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
	
	public void updateMotion (Odometry odo, GaussianSample[] samples) 
	{
		double		odoCov;
		
		odoCov	= odo.elin * odo.elin + odo.elat * odo.elat;

		odometrySampler.setMeanDev (0, odo.dlin, Math.abs (odo.elin));
		odometrySampler.setMeanDev (1, odo.dlat, Math.abs (odo.elat));
		odometrySampler.setMeanDev (2, odo.drot, Math.abs (odo.erot));

		for (int i = 0; i < samples.length; i++)
			updateSample (samples[i], odoCov);
	}
		
	protected void updateSample (GaussianSample sample, double odoCov)
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
		
		sample.g.addToCovariance (odoCov * 100.0);
		sample.g.setMean (pos[0], pos[1]);
		sample.a	 = pos[2];
	}
}
