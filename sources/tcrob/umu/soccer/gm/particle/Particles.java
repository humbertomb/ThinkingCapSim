/**
 * Created on 26-jun-2006
 *
 * @author Humberto Martinez Barbera
 */

package tcrob.umu.soccer.gm.particle;

import java.awt.*;

import tcrob.umu.soccer.gm.data.*;
import tcrob.umu.soccer.gm.*;
import wucore.utils.math.*;
import wucore.utils.math.stat.*;
import wucore.widgets.*;

public class Particles implements Localisation
{
	private String ID = new String("PART"); 

	static public final double	EPS_WEIGHT		= 1E-250;

	protected int					nSamples;
	protected GaussianSample[]		samples;
	protected GaussianSample[]		newSamples;
	protected GaussianSampler			posSampler;
	protected MovementUpdater			motion;
	protected double[]				bufpos;
	protected RandomNumberGenerator	random;
	
	private Gaussian2D				gtmp;
	private double[]					w;

	protected WorldModel				wm;
	protected Gs						gs;

	protected int[]					mLastAnchored;
	protected boolean[]				mLastUpdated;
		
	public Particles (String name, int nSamples)
	{
		this.nSamples = nSamples;
		
		mLastAnchored	= new int[LocLps.LPS_SIZE];
		mLastUpdated		= new boolean[LocLps.LPS_SIZE];

		wm			= new WorldModel (name);
		gs			= new Gs ();
				
		samples		= new GaussianSample[nSamples];
		newSamples	= new GaussianSample[nSamples];
		gtmp			= new Gaussian2D ();
		w			= new double[nSamples];

		for (int i = 0; i < nSamples; i++)
		{
			samples[i]		= new GaussianSample ();
			newSamples[i]	= new GaussianSample ();
		}		
		
		random		= new RandomNumberGenerator ();
		motion		= new MovementUpdater (wm);
		bufpos		= new double[3];
		posSampler	= new GaussianSampler (3);
		posSampler.setRange (0, -wm.getTotalXSize()*0.5, wm.getTotalXSize()*0.5);
		posSampler.setRange (1, -wm.getTotalYSize()*0.5, wm.getTotalYSize()*0.5);
		posSampler.setRange (2, -Math.PI, Math.PI);
		

		// Initialise position with uncertainly
		GsPosition	initPos;
		initPos			= new GsPosition ();
		initPos.x		= 0;
		initPos.y		= -1500;
		initPos.dx		= 100;
		initPos.dy		= 100;
		initPos.theta	= (float) (90.0 * Angles.DTOR);
		initPos.dtheta	= (float) (40.0 * Angles.DTOR);
		
		initialPosition (initPos);
	}

	public Gs				getGs ()						{ return gs; }
	public WorldModel		getWorldModel ()				{ return wm; }
	public GaussianSample[]	getParticles ()				{ return samples; }
	public int				getParticleNumber ()			{ return nSamples; }
	public boolean			getLastUpdated (int index)	{ return mLastUpdated[index]; }

	public void initialPosition (GsPosition pos)
	{
		posSampler.setMeanDev (0, pos.x, pos.dx);
		posSampler.setMeanDev (1, pos.y, pos.dy);
		posSampler.setMeanDev (2, pos.theta, pos.dtheta);
		
		for (int i=0; i < nSamples; i++) 
		{
			posSampler.generateSample (bufpos);
			samples[i].g.clear ();
			samples[i].g.setMean (bufpos[0], bufpos[1]);
			samples[i].g.setCovariance (pos.dx * pos.dx, pos.dy * pos.dy, 0.0);
			samples[i].a = Angles.radnorm_180 (bufpos[2]);
		}		
		
		updatePosition ();
	}
	
	public void updateMotionOnly (Odometry odo)
	{
		// Update motion
		motion.updateMotion (odo, samples);
		
		// Localise
		updatePosition ();
	}
	
	public void updateMotionAndSensors (Odometry odo, LocLps lps)
	{
		// Update motion
		motion.updateMotion (odo, samples);
		
		// Update sensors
		updateSensors (lps);
		
		// Localise
		updatePosition ();
	}

	protected void updateSensors (LocLps lps)
	{
		LocLpo			lpo;
		double		errorDepth;
		double		errorAzimuthal;
		boolean		resample = false;
		
		for (int index = 0; index < LocLps.LPS_SIZE; index++)
			mLastUpdated[index]	= false;	

		// Landmarks
		for (int index = LocLps.INIT_LMS; index < (LocLps.INIT_LMS + LocLps.NUM_LMS); index++)
		{
			lpo = lps.getLpo(index);
			if (lpo.getLastAnchored() > mLastAnchored[index])
			{
				mLastAnchored[index]	= lpo.getLastAnchored();				
				mLastUpdated[index]	= true;				
				resample				= true;	
				
				errorDepth		= 0.3 * lpo.getRho();
				errorAzimuthal	= 5.0 * Angles.DTOR;
						
				addLandmark (lpo, errorAzimuthal, errorDepth, wm.getLM (index-LocLps.INIT_LMS));
			}
		}
		
		// Nets
		for (int index = LocLps.INIT_NETS; index < (LocLps.INIT_NETS + LocLps.NUM_NETS); index++)
		{
			lpo = lps.getLpo(index);
			if (lpo.getLastAnchored() > mLastAnchored[index])
			{
				mLastAnchored[index]	= lpo.getLastAnchored();				
				mLastUpdated[index]	= true;									
				resample				= true;
				
				if (lpo.getRho() < 3000)
				{
					errorDepth		= 0.3 * lpo.getRho();
					errorAzimuthal	= 25.0 * Angles.DTOR;
				} else
				{
					errorDepth		= 1500.0;
					errorAzimuthal	= 5.0 * Angles.DTOR;
				}

				addLandmark (lpo, errorAzimuthal, errorDepth, wm.getNet (index-LocLps.INIT_NETS));
			}
		}
		
		if (resample)
			resampleParticles ();
	}
	
	private void addLandmark (LocLpo lpo, double covx, double covy, ObjectModel landmark)
	{
		double		rho, theta;
		double		xpos, ypos;
		
		rho		= lpo.getRho ();
		theta	= lpo.getTheta ();
		xpos		= rho * Math.cos (theta);
		ypos		= rho * Math.sin (theta);

		for (int i = 0; i < nSamples; i++)
		{
			gtmp.clear ();
			gtmp.setMean (xpos, ypos);
			gtmp.setCovarianceAxis (Math.sqrt (covx), Math.sqrt (covy), theta);		
			gtmp.invertMean ();
			gtmp.rotate (samples[i].a);
			gtmp.translate (landmark.getPosX (), landmark.getPosY ());

//			System.out.println ("lm<"+gtmp+"> part("+i+")<"+g[i]+">");
			samples[i].g.multiply (gtmp);
		}
	}

	private void resampleParticles ()
	{
		double		wSum;
	    
		// Compute particle weights
		wSum = 0;		
		for (int i = 0; i < nSamples; i++)
		{
			double			wi;
			
			wi	= EPS_WEIGHT;
//			if (g[i].getLogAmplitude () > Math.log (EPS_WEIGHT))
//				wi = Math.exp (g[i].getLogAmplitude ());
			if (samples[i].g.getLogAmplitude () > 0.0)
				wi = samples[i].g.getLogAmplitude ();
			
			// Calculate cumulative distribution (w[i] should be nonnegative)
			wSum += wi;			
			w[i] = wSum;
		}
		
		// Normalize samples
		for (int i = 0; i < nSamples; i++)
		{
			// Uniform random variate
			double r = random.nextUniform (0.0, wSum);
			
			// Binary search to find corresponding index
			int iLow = 0;
			int iHigh = nSamples-1;
			
			while (iHigh > iLow)
			{
				int iMid = (iLow+iHigh)/2;				
				if (r < w[iMid])
					iHigh = iMid;
				else
					iLow = iMid+1;
			}
			newSamples[i].set (samples[iLow]);
		}
		
		// Exchange particle data sets
		GaussianSample[] tmp;
		
		tmp			= samples;
		samples		= newSamples;
		newSamples	= tmp;
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
		for (int i = 0; i < nSamples; i++)
		{
			xMean	+= samples[i].g.getX ();
			xVar		+= samples[i].g.getX () * samples[i].g.getX ();
			
			yMean	+= samples[i].g.getY ();
			yVar		+= samples[i].g.getY () * samples[i].g.getY ();
			
			aCos		+= Math.cos (samples[i].a);
			aSin		+= Math.sin (samples[i].a);
		}
		
		xMean	= xMean / nSamples;
		xVar		= (xVar / nSamples) - (xMean * xMean);

		yMean	= yMean / nSamples;
		yVar		= (yVar / nSamples) - (yMean * yMean);

		aCos		= aCos / nSamples;
		aSin		= aSin / nSamples;
		aMean	= Math.atan2 (aSin, aCos);
		aVar		= 1.0 - Functions.hypot (aSin, aCos);

		pos			= gs.getPosition ();
		pos.x		= (int) xMean;
		pos.y		= (int) yMean;
		pos.theta	= (float) aMean;
		pos.dx		= (int) Math.sqrt (xVar);
		pos.dy		= (int) Math.sqrt (yVar);
		pos.dtheta	= (float) Math.sqrt (aVar);
	}
	
	public void drawElements (Model2D model)
	{
		for (int i = 0; i < nSamples; i++)
			model.addRawArrow (samples[i].g.getX (), samples[i].g.getY(), 350.0, samples[i].a, Color.GREEN.darker ());
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
