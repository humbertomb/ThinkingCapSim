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

public class DualMarkerSampler 
{
	protected GaussianSampler			gaussianSampler;
	protected GaussianEvaluator		gaussianEvaluator;
	protected RandomNumberGenerator	random;
	
	public double					markerX0;
	public double					markerY0;
	public double					markerX1;
	public double					markerY1;
	public double					markerRho;
	public double					markerTheta;
	
	private boolean					use_next = false;
	private double					next_x;
	private double					next_y;
	private double					next_theta;
	
	public DualMarkerSampler ()
	{
		random				= new RandomNumberGenerator ();
		gaussianSampler		= new GaussianSampler (3);
		gaussianEvaluator	= new GaussianEvaluator(1);
	}
	
	public void generateSamples (MarkerUpdater[] updater, int numMarkers, LocaleSampled locale, int top) 
	{
		markerX0		= updater[0].markerX;
		markerY0		= updater[0].markerY;
		markerX1		= updater[1].markerX;
		markerY1		= updater[1].markerY;
		markerRho	= Functions.hypot (markerY1 -markerY0, markerX1-markerX0);
		markerTheta	= Math.atan2 (markerY1-markerY0, markerX1-markerX0);

		gaussianSampler.setMeanDev (0, updater[0].gaussianEvaluator.getMean (0), updater[0].gaussianEvaluator.getStdDev (0));
		gaussianSampler.setMeanDev (1, updater[0].gaussianEvaluator.getMean (1), updater[0].gaussianEvaluator.getStdDev (1));
		gaussianSampler.setMeanDev (2, updater[1].gaussianEvaluator.getMean (0), updater[1].gaussianEvaluator.getStdDev (0));
		gaussianEvaluator.setMeanDev (0, updater[1].gaussianEvaluator.getMean (1), updater[1].gaussianEvaluator.getStdDev (1));
		
		for (int i = 0; i < top; i++)
		{
			boolean		accept_sample;
			double[]		sample;

			sample = locale.sample[i].data;		
			accept_sample = true;
			do 
			{
				generateSample (sample);
				sample[Sample.W] = 1.0;
				
				if (numMarkers > 2) 
				{
					for (int j = 1; j < numMarkers; j++)
						updater[j].updateSample (sample);	
					accept_sample = (Math.max (sample[Sample.W], 0.2) >= random.nextUniform (0.0, 1.0));
				}
			}
			while (!accept_sample);		
			sample[Sample.W] = 1.0;
		}
	}

	public void generateSample (double[] pos)
	{
		boolean		accept_sample;		
		double		x, y , theta;
		double		x0, y0, theta0, x1, y1, theta1;
		double		allo_angle0, allo_angle1;
		double		rho0, rho1;
		boolean		onField0, onField1;
		double		prob;
		
		do 
		{
			accept_sample = false;
			
			if (use_next) 
			{
				use_next	= false;
				x		= next_x;
				y		= next_y;
				theta0	= next_theta;
				
				// fill in vars needed below
				allo_angle0=Math.atan2(y-markerY0,x-markerX0);
			}
			else 
			{
				gaussianSampler.generateSample (pos);
				
				rho0		= pos[0];
				theta0	= pos[1];
				rho1		= pos[2];
				
				// check if circles intersect
				if (markerRho > rho0 + rho1)
					continue;
				
				// angle between the line from marker0 to marker1 and the line from marker0 to sample point
				double spread_angle;
				double z;
				
				z = (rho1*rho1 - markerRho*markerRho - rho0*rho0) / (-2.0 * markerRho * rho0);
				z = Math.max (Math.min (z, 1.0), -1.0);
				spread_angle = Math.acos (z);
				
				allo_angle0 = markerTheta - spread_angle + Math.PI;
				allo_angle1 = markerTheta + spread_angle + Math.PI;
				
				x0		= markerX0 - rho0 * Math.cos (allo_angle0);
				y0		= markerY0 - rho0 * Math.sin (allo_angle0);					
				onField0	= Localisation.onField (x0, y0);
				
				x1		= markerX0 - rho0 * Math.cos (allo_angle1);
				y1		= markerY0 - rho0 * Math.sin (allo_angle1);					
				onField1	= Localisation.onField (x1, y1);

				if (onField0 && !onField1)
				{
					x		= x0;
					y		= y0;
				}
				else if (onField1 && !onField0)
				{
					x		= x1;
					y		= y1;
					allo_angle0 = allo_angle1;
				}
				else if (!onField0 && !onField1)
					continue;
				else					// both on field
				{
					next_theta	= theta0;
					use_next		= true;					
					if (random.nextUniform (0.0, 1.0) > 0.5)		// not fair if some distances get 2 points on field and some don't
					{
						x		= x0;
						y		= y0;
						next_x	= x1;
						next_y	= y1;
					}
					else
					{
						x		= x1;
						y		= y1;
						next_x	= x0;
						next_y	= y0;
						allo_angle0 = allo_angle1;
					}
				}
			}
			
			allo_angle1	= Math.atan2 (markerY1-y,markerX1-x);			
			theta		= allo_angle0 - theta0;			
			theta1		= Angles.radnorm_180 (allo_angle1 - theta);
			
			while (theta1 >= gaussianEvaluator.getMean(0) + Math.PI)
				theta1 -= 2 * Math.PI;
			while (theta1 <  gaussianEvaluator.getMean(0) - Math.PI)
				theta1 += 2 * Math.PI;
			
			gaussianEvaluator.setMinProb (0.1);
			prob		= gaussianEvaluator.evaluate (0, theta1);		
			if (random.nextUniform (0.0, 1.0) > prob)
				continue;
					
			pos[0] = x;
			pos[1] = y;
			pos[2] = Angles.radnorm_180 (theta);

			accept_sample = true;
		}
		while (!accept_sample);
	}
}
