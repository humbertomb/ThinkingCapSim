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
	static public final double	MIN_STDDEV		= 10.0;			// the least error of what is seen (mm)

	// Where the particles start: spread round INIT_X, INIT_Y, INIT_THETA (mm, mm, rad), by default with these deviations
	// (wide enough for the particles to cover where the robot may be, so they settle on it at the first sightings)
	static public final double	INIT_X			= 0.0;
	static public final double	INIT_Y			= -1500.0;
	static public final double	INIT_THETA		= 90.0 * Angles.DTOR;
	static public final double	INIT_DXY		= 500.0;
	static public final double	INIT_DTHETA		= 40.0 * Angles.DTOR;

	protected int					nSamples;
	protected GaussianSample[]		samples;
	protected GaussianSample[]		newSamples;
	protected GaussianSampler			posSampler;
	protected MovementUpdater			motion;
	protected double[]				bufpos;
	protected RandomNumberGenerator	random;
	
	private Gaussian2D				gtmp;
	private double[]					w;
	private double[]					lw;				// the log of the weight of each particle, from what is seen on this update

	protected Gs						gs;

	protected int[]					mLastAnchored;
	protected boolean[]				mLastUpdated;
		
	public Particles (int nSamples)
	{
		this (nSamples, INIT_DXY, INIT_DTHETA);
	}

	/** With the particles spread round where they start with these deviations: of the position (mm) and of the heading (rad). */
	public Particles (int nSamples, double spread, double spreadAngle)
	{
		this.nSamples = nSamples;
		
		mLastAnchored	= new int[LocLps.LPS_SIZE];
		mLastUpdated		= new boolean[LocLps.LPS_SIZE];

		gs			= new Gs ();
				
		samples		= new GaussianSample[nSamples];
		newSamples	= new GaussianSample[nSamples];
		gtmp			= new Gaussian2D ();
		w			= new double[nSamples];
		lw			= new double[nSamples];

		for (int i = 0; i < nSamples; i++)
		{
			samples[i]		= new GaussianSample ();
			newSamples[i]	= new GaussianSample ();
		}		
		
		random		= new RandomNumberGenerator ();
		motion		= new MovementUpdater ();
		bufpos		= new double[3];
		posSampler	= new GaussianSampler (3);
		posSampler.setRange (0, -TOTAL_X_SIZE*0.5, TOTAL_X_SIZE*0.5);
		posSampler.setRange (1, -TOTAL_Y_SIZE*0.5, TOTAL_Y_SIZE*0.5);
		posSampler.setRange (2, -Math.PI, Math.PI);
		

		// Initialise position with uncertainly
		GsPosition	initPos;
		initPos			= new GsPosition ();
		initPos.x		= (int) INIT_X;
		initPos.y		= (int) INIT_Y;
		initPos.dx		= (int) spread;
		initPos.dy		= (int) spread;
		initPos.theta	= INIT_THETA;
		initPos.dtheta	= spreadAngle;
		
		initialPosition (initPos);
	}

	public Gs				getGs ()						{ return gs; }
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
		java.util.Arrays.fill (lw, 0.0);

		// Landmarks
		for (int index = LocLps.INIT_LMS; index < (LocLps.INIT_LMS + LocLps.NUM_LMS); index++)
		{
			lpo = lps.getLpo(index);
			if (lpo.last_anchored > mLastAnchored[index])
			{
				mLastAnchored[index]	= lpo.last_anchored;				
				mLastUpdated[index]	= true;				
				resample				= true;	
				
				errorDepth		= 0.3 * lpo.rho;
				errorAzimuthal	= 5.0 * Angles.DTOR;
						
				addLandmark (lpo, errorAzimuthal, errorDepth, LM_X[index-LocLps.INIT_LMS], LM_Y[index-LocLps.INIT_LMS]);
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
				resample				= true;
				
				if (lpo.rho < 3000)
				{
					errorDepth		= 0.3 * lpo.rho;
					errorAzimuthal	= 25.0 * Angles.DTOR;
				} else
				{
					errorDepth		= 1500.0;
					errorAzimuthal	= 5.0 * Angles.DTOR;
				}

				addLandmark (lpo, errorAzimuthal, errorDepth, NET_X[index-LocLps.INIT_NETS], NET_Y[index-LocLps.INIT_NETS]);
			}
		}
		
		if (resample)
			resampleParticles ();
	}
	
	/**
	 * A mark (landmark or net) at (lmx, lmy) seen at (rho, theta) from the robot,
	 * with an error of the bearing (rad) and of the distance (mm): for each
	 * particle, where the robot would be with its heading -- a Gaussian with
	 * the error of the distance along the line of sight and the one of the bearing
	 * across it --, how well that agrees with where the particle has it (its
	 * weight) and the two put together (where it has it from now on).
	 */
	private void addLandmark (LocLpo lpo, double errAzimuth, double errDepth, double lmx, double lmy)
	{
		double		rho, theta;
		double		xpos, ypos;
		double		sDepth, sAcross;
		
		rho		= lpo.rho;
		theta	= lpo.theta;
		xpos		= rho * Math.cos (theta);
		ypos		= rho * Math.sin (theta);
		sDepth	= Math.max (MIN_STDDEV, errDepth);						// along the line of sight
		sAcross	= Math.max (MIN_STDDEV, rho * errAzimuth);				// across it

		for (int i = 0; i < nSamples; i++)
		{
			gtmp.clear ();
			gtmp.setMean (xpos, ypos);
			gtmp.setCovarianceAxis (sDepth, sAcross, theta);
			gtmp.invertMean ();
			gtmp.rotate (samples[i].a);
			gtmp.translate (lmx, lmy);

			lw[i]	+= samples[i].g.logOverlap (gtmp);
			samples[i].g.multiply (gtmp);
		}
	}

	/**
	 * The particles drawn again in proportion to their weights (the likelihood of
	 * what was seen for each, {@link #lw}), systematically: one random offset and
	 * evenly spaced from it, which keeps more of them than drawing each at random.
	 */
	private void resampleParticles ()
	{
		double		wSum, lwMax;
		double		step, r;
		int			j;

		// the weights, relative to the best one (they are logs, and may be far below zero)
		lwMax	= Double.NEGATIVE_INFINITY;
		for (int i = 0; i < nSamples; i++)
			if (lw[i] > lwMax)		lwMax = lw[i];
		if (Double.isInfinite (lwMax))			return;					// nothing to tell them apart

		wSum = 0;		
		for (int i = 0; i < nSamples; i++)
		{
			wSum	+= Math.max (EPS_WEIGHT, Math.exp (lw[i] - lwMax));
			w[i]	= wSum;												// the cumulative distribution
		}
		
		step	= wSum / nSamples;
		r		= random.nextUniform (0.0, step);
		for (int i = 0, k = 0; i < nSamples; i++, r += step)
		{
			for (j = k; (j < nSamples - 1) && (w[j] < r); j++)	;
			k	= j;
			newSamples[i].set (samples[j]);
			newSamples[i].g.setLogAmplitude (0.0);
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
		pos.theta	= (double) aMean;
		pos.dx		= (int) Math.sqrt (xVar);
		pos.dy		= (int) Math.sqrt (yVar);
		pos.dtheta	= (double) Math.sqrt (aVar);
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
