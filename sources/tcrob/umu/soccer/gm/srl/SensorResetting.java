/**
 * Created on 27-jun-2006
 *
 * @author Humberto Martinez Barbera
 * @author Scott Lenser (CMU)
 */
package tcrob.umu.soccer.gm.srl;

import java.awt.*;

import tcrob.umu.soccer.gm.data.*;
import tcrob.umu.soccer.gm.*;
import wucore.utils.math.*;
import wucore.utils.math.stat.*;
import wucore.widgets.*;

public class SensorResetting implements Localisation
{
	private String ID = new String("SRL"); 

	static public final boolean[]		angle			= { false, false, true };	
	
	static public final double		MIN_PROB_SENSE 	= 0.001;
	static public final double		RESAMPLE_FACTOR	= 1.0;
	
	protected LocaleSampled			locale;
	protected LocaleSampled			newLocale;
	protected UniformSampler			fieldSampler;
	protected GaussianSampler			posSampler;
	protected MovementUpdater			move_updater;
	protected MarkerUpdater[]			lm_updater;
	protected MarkerSampler			single_lm_sampler;
	protected DualMarkerSampler		dual_lm_sampler;
	protected RandomNumberGenerator	random;
	protected double[]				params = new double[6];
	protected double[]				cum_weights;
	
	protected Gs						gs;

	protected int[]					mLastAnchored;
	protected boolean[]				mLastUpdated;
	
	public SensorResetting (int numSamples)
	{
		mLastAnchored	= new int[LocLps.LPS_SIZE];
		mLastUpdated		= new boolean[LocLps.LPS_SIZE];

		gs				= new Gs ();

		fieldSampler		= new UniformSampler (3);
		fieldSampler.setRange (0, -TOTAL_X_SIZE*0.5, TOTAL_X_SIZE*0.5);
		fieldSampler.setRange (1, -TOTAL_Y_SIZE*0.5, TOTAL_Y_SIZE*0.5);
		fieldSampler.setRange (2, -Math.PI, Math.PI);

		posSampler		= new GaussianSampler (3);
		posSampler.setRange (0, -TOTAL_X_SIZE*0.5, TOTAL_X_SIZE*0.5);
		posSampler.setRange (1, -TOTAL_Y_SIZE*0.5, TOTAL_Y_SIZE*0.5);
		posSampler.setRange (2, -Math.PI, Math.PI);
		
		random			= new RandomNumberGenerator ();
		locale			= new LocaleSampled (numSamples);
		newLocale		= new LocaleSampled (numSamples);
		move_updater		= new MovementUpdater ();
		single_lm_sampler= new MarkerSampler ();
		dual_lm_sampler	= new DualMarkerSampler ();
		cum_weights		= new double[numSamples+1];

		lm_updater	= new MarkerUpdater[LocLps.NUM_NETS + LocLps.NUM_LMS];
		for (int i = 0; i < LocLps.NUM_NETS + LocLps.NUM_LMS; i++)
			lm_updater[i] 	= new MarkerUpdater ();
		
		// Initialise position with uncertainly
		GsPosition	initPos;
		initPos			= new GsPosition ();
		initPos.x		= 0;
		initPos.y		= -1500;
		initPos.dx		= 1000;
		initPos.dy		= 100;
		initPos.theta	= (double) (90.0 * Angles.DTOR);
		initPos.dtheta	= (double) (40.0 * Angles.DTOR);
		
		initialPosition (initPos);
	}

	public Gs				getGs ()						{ return gs; }
	public LocaleSampled		getSamples ()				{ return locale; }
	public int				getSampleNumber ()			{ return locale.numSamples; }
	public boolean			getLastUpdated (int index)	{ return mLastUpdated[index]; }

	public void updateMotionOnly (Odometry odo)
	{
		for (int index = 0; index < LocLps.LPS_SIZE; index++)
			mLastUpdated[index]	= false;	

		// Update motion
		move_updater.updateMotion (odo, locale);
		
		// Localise
		updatePosition ();
	}
	
	public void updateMotionAndSensors (Odometry odo, LocLps lps)
	{
		for (int index = 0; index < LocLps.LPS_SIZE; index++)
			mLastUpdated[index]	= false;	

		// Update motion
		move_updater.updateMotion (odo, locale);
		
		// Update sensors
		updateSensors (lps);
		
		// Localise
		updatePosition ();
	}

	public void initialPosition (GsPosition pos) 
	{
		posSampler.setMeanDev (0, pos.x, pos.dx);
		posSampler.setMeanDev (1, pos.y, pos.dy);
		posSampler.setMeanDev (2, pos.theta, pos.dtheta);
		
		for (int i=0; i<locale.numSamples; i++) 
		{
			posSampler.generateSample (locale.sample[i].data);
			locale.sample[i].data[Sample.TH] = Angles.radnorm_180 (locale.sample[i].data[Sample.TH]);
			locale.sample[i].data[Sample.W] = 1.0;
		}		
	}
	
	protected void resetPosition ()
	{
		for (int i=0; i<locale.numSamples; i++) 
		{
			fieldSampler.generateSample (locale.sample[i].data);
			locale.sample[i].data[Sample.TH] = Angles.radnorm_180 (locale.sample[i].data[Sample.TH]);
			locale.sample[i].data[Sample.W] = 1.0;
		}
	}

	protected void updateSensors (LocLps lps)
	{
		LocLpo			lpo;
		int			curLandmarks;
		double		errorRho;
		double		errorTheta;		
		
		curLandmarks = 0;
		
		// Landmarks
		for (int index = LocLps.INIT_LMS; index < (LocLps.INIT_LMS + LocLps.NUM_LMS); index++)
		{
			lpo = lps.getLpo(index);
			if (lpo.last_anchored > mLastAnchored[index])
			{
				mLastAnchored[index]	= lpo.last_anchored;				
				mLastUpdated[index]	= true;					
				
				if (lpo.rho < 2000)
					errorRho		= 50 + 0.05 * lpo.rho;
				else
					errorRho		= 200 + 0.05 * lpo.rho;
				errorRho		= 50;
				errorTheta	= 2.0 * Angles.DTOR;

				lm_updater[curLandmarks].setMarkerLoc (LM_X[index-LocLps.INIT_LMS], LM_Y[index-LocLps.INIT_LMS]);
				lm_updater[curLandmarks].setMinProb (MIN_PROB_SENSE);
				lm_updater[curLandmarks].updateSamples (lpo, errorRho, errorTheta, locale);
				
				curLandmarks ++;
			}
		}
		
		// Nets
		for (int index = LocLps.INIT_NETS; index < (LocLps.INIT_NETS + LocLps.NUM_NETS); index++)
		{
			lpo = lps.getLpo(index);
			if (lpo.last_anchored > mLastAnchored[index])
			{
				mLastAnchored[index]	= lpo.last_anchored;				
				mLastUpdated[index]	= true;									
				
				if (lpo.rho < 1000)
				{
					errorRho		= 500.0;
					errorTheta	= 25.0 * Angles.DTOR;
				} 
				else if (lpo.rho < 3000)
				{
					errorRho		= 0.3 * lpo.rho;
					errorTheta	= 15.0 * Angles.DTOR;
				} 
				else
				{
					errorRho		= 1000.0;
					errorTheta	= 5.0 * Angles.DTOR;
				}
				errorRho		= 100.0;
				errorTheta	= 1.0 * Angles.DTOR;

				lm_updater[curLandmarks].setMarkerLoc (NET_X[index-LocLps.INIT_NETS], NET_Y[index-LocLps.INIT_NETS]);
				lm_updater[curLandmarks].setMinProb (MIN_PROB_SENSE);
				lm_updater[curLandmarks].updateSamples (lpo, errorRho, errorTheta, locale);
				
				curLandmarks ++;
			}
		}
		
		// Resampling
		if (curLandmarks > 0)
		{
			double		goodSamplesProb, expectedProb;
			int			newSamples;
					
			goodSamplesProb	= normalizeSamples ();
			expectedProb		= Math.pow (0.054, RESAMPLE_FACTOR * curLandmarks);
			newSamples		= (int) ((1 - goodSamplesProb / expectedProb) * locale.numSamples) + 1;
		
			if (newSamples > 0)
			{
//				System.out.println ("goodProb="+goodSamplesProb+" expected("+curLandmarks+")="+expectedProb+" new samples="+newSamples);
			
				if (curLandmarks == 1)
					single_lm_sampler.generateSamples (lm_updater[0], locale, newSamples);
				else
					dual_lm_sampler.generateSamples (lm_updater, curLandmarks, locale, newSamples);
			}
		}
	}
	
	protected void updatePosition ()
	{
		double		aMean, xMean, yMean;
		double		aVar, xVar, yVar;
		double		aCos, aSin;
		GsPosition	pos;

		// Compute particle distribution parameters (mean, variance)
		aMean	= xMean	= yMean	= 0.0;
		aVar		= xVar	= yVar	= 0.0;
		aCos		= aSin	= 0.0;
		for (int i = 0; i < locale.numSamples; i++)
		{
			xMean	+= locale.sample[i].data[0];
			xVar		+= locale.sample[i].data[0] * locale.sample[i].data[0];
			
			yMean	+= locale.sample[i].data[1];
			yVar		+= locale.sample[i].data[1] * locale.sample[i].data[1];
			
			aCos		+= Math.cos (locale.sample[i].data[2]);
			aSin		+= Math.sin (locale.sample[i].data[2]);
		}
		
		xMean	= xMean / locale.numSamples;
		xVar		= (xVar / locale.numSamples) - (xMean * xMean);

		yMean	= yMean / locale.numSamples;
		yVar		= (yVar / locale.numSamples) - (yMean * yMean);

		aCos		= aCos / locale.numSamples;
		aSin		= aSin / locale.numSamples;
		aMean	= Math.atan2 (aSin, aCos);
		aVar		= 1.0 - Functions.hypot (aSin, aCos);

		pos			= gs.getPosition ();
		pos.x		= (int) xMean;
		pos.y		= (int) yMean;
		pos.theta	= (double) aMean;
		pos.dx		= (int) Math.sqrt (xVar);
		pos.dy		= (int) Math.sqrt (yVar);
		pos.dtheta	= (double) Math.sqrt (aVar);
	}
	
	private double normalizeSamples () 
	{
		double			wSum = 0.0;
		LocaleSampled	tmp;
			
		for (int i = 0; i < locale.numSamples; i++) 
		{
			cum_weights[i] = wSum;
			wSum += locale.sample[i].data[Sample.W];
		}
		cum_weights[locale.numSamples] = wSum;
		
		for (int i = 0; i < newLocale.numSamples; i++)
		{
			// Uniform random variate
			double r = random.nextUniform (0.0, wSum);
							
			// Binary search to find corresponding index
			int iLow = 0;
			int iHigh = locale.numSamples-1;			
			while (iHigh > iLow)
			{
				int iMid = (iLow+iHigh)/2;				
				if (r < cum_weights[iMid])
					iHigh = iMid;
				else
					iLow = iMid+1;
			}
						
			for (int j = 0; j < 3; j++)
				newLocale.sample[i].data[j] = locale.sample[iLow].data[j];
			newLocale.sample[i].data[Sample.W] = 1.0;
		}
		
		tmp			= locale;
		locale		= newLocale;
		newLocale	= tmp;
		
		return wSum / locale.numSamples;
	}
	
	private void addNoise (int num_noise_samples) 
	{
		if(num_noise_samples>locale.numSamples)
			num_noise_samples=locale.numSamples;
		
		int start=(int)random.nextUniform (0.0,locale.numSamples-num_noise_samples);
		start=locale.numSamples-num_noise_samples;
		
		for(int i=start; i<num_noise_samples+start; i++) 
		{
			fieldSampler.generateSample (locale.sample[i].data);
			locale.sample[i].data[Sample.W]=1.0;
		}
	}
	
	private void addInterp(int num_interp_samples) 
	{
		if(num_interp_samples>locale.numSamples-1)
			num_interp_samples=locale.numSamples-1;
		
		int start;//=(int)Random.uniform(0.0,locale.numSamples-num_interp_samples);
		start=0;
		int end=start+num_interp_samples;
		
		int i;
		for(i=start; i<end; i++) 
		{
			int samp1_idx=(int)random.nextUniform(0.0,locale.numSamples-num_interp_samples);
			if(samp1_idx>=start) samp1_idx+=num_interp_samples;
			int samp2_idx=(int)random.nextUniform(0.0,locale.numSamples-num_interp_samples);
			if(samp2_idx>=start) samp2_idx+=num_interp_samples;
			
			double[] samp=locale.sample[i].data;
			double[] samp1=locale.sample[samp1_idx].data;
			double[] samp2=locale.sample[samp2_idx].data;
			
			for(int var_idx=0; var_idx<3; var_idx++) {
				double factor=random.nextUniform(0.0,1.0);
				if(angle[var_idx]) {
					double angle1=samp1[var_idx];
					double angle2=samp2[var_idx];
					
					if(Math.abs(angle2-angle1)>Math.PI) {
						if(angle2 > angle1)
							angle2-=2*Math.PI;
						else
							angle1-=2*Math.PI;
					}
					
					double angle=angle1*factor + angle2*(1.0-factor);
						
					samp[var_idx]=Angles.radnorm_180 (angle);
				}
				else
					samp[var_idx]=samp1[var_idx]*factor + samp2[var_idx]*(1.0-factor);
			}
			
			samp[Sample.W]=1.0;
		}
	}
	
	public void drawElements (Model2D model)
	{
		Sample			sample;
		
		for (int i = 0; i < locale.numSamples; i++)
		{
			sample	= locale.sample[i];
			model.addRawArrow (sample.data[Sample.X], sample.data[Sample.Y], 350.0, sample.data[Sample.TH], Color.MAGENTA);
		}
	}

	public void printLastAnchored ()
	{
		System.out.print ("mLastAnchored=[");
		for (int i = 0; i < mLastAnchored.length; i++)
			System.out.print (mLastAnchored[i]+ " ");
		System.out.println ("]");
	}
	
	public void printLastUpdated ()
	{
		System.out.print ("mLastUpdated=[");
		for (int i = 0; i < mLastUpdated.length; i++)
			System.out.print (mLastUpdated[i]+ " ");
		System.out.println ("]");
	}

	public void setGT(GsPosition pos) {
		// TODO Auto-generated method stub
		
	}
	
	public String getId() {
		return ID;
	}

	public void setId(String newid) {
		ID = new String(newid);
	}
}
