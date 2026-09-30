package tcrob.umu.soccer.gm.data;

import java.util.*;

import wucore.utils.math.*;
import wucore.utils.math.jama.Matrix;

public class Lpo
{	
	public final static int			BALL			= 0;
	public final static int			LANDMARK		= 1;
	public final static int			NET				= 2;
	public final static int			MATE			= 3;
	public final static int			OPPONENT		= 4;

	static public final float			DriftBall	= 0.015f;	// drift in ball position
	static public final float			DriftDog		= 0.1f;		// drift in any dog's position
	static public final float			DriftPOG		= 0.1f;		// drift for POG
	static public final float			DriftWalk	= 0.05f;		// extra drift if we are walking
	static public final float			DriftTurn	= 0.02f;		// still extra drift if we rotated
	static public final float			DriftLM		= 0.01f;		// still extra drift if we rotated

	private short					id;
	private short					type;
	private int						rho;
	private float					theta;
	private float					anchored;
	private int						last_anchored;
	private boolean					do_clamping;
	
	private EKFLpos					lpos;
	
	public Lpo ()
	{
		Matrix S0 = new Matrix(2,1);
		
		set ((short)0,(short)0,0,0,0);
		
		S0.set(0, 0, 0);
		S0.set(1, 0, 0);
		lpos = new EKFLpos(S0, 0.3, 0.3, 0.3, 1000.0, Angles.DTOR * 7.0);
	}
	
	public Lpo (short id, short type)
	{
		set (id,type,0,0,0);
	}
	
	public void set (short id,short type,int rho,float theta,float anchored)
	{
		Matrix S0 = new Matrix(2,1);
		this.id = id;
		this.type = type;
		this.rho = rho;
		this.theta = theta;
		this.anchored = anchored;
		
		S0.set(0, 0, rho);
		S0.set(1, 0, theta);
		lpos = new EKFLpos(S0, 0.3, 0.3, 0.3, 1000.0, Angles.DTOR * 7.0);

		last_anchored = 0;
	}
	
	public int getId()					{ return this.id;}
	public short getType()				{ return this.type;}
	public int getRho()					{ return this.rho;}
	public float getTheta()				{ return this.theta;}
//	public int getRho()					{ return lpos.getRho();}//return this.rho;}
//	public float getTheta()				{ return lpos.getTheta();}//return this.theta;}
	public void setRho(int Rho)			{ this.rho = Rho; }
	public void setTheta(float Theta)	{ this.theta = Theta; }
//	public float getQuality()			{ return (float) lpos.getQuality();}
	public float getAnchored()	{ 
		return this.anchored;
		//return (float) lpos.getQuality();
		}
	
	public Matrix getP() { return lpos.getP(); };
	
	public int getLastAnchored ()			{ return last_anchored; }
	public boolean needClamping ()		{ return do_clamping; }
	public void doClamping (boolean clp)	{ do_clamping = clp; }
	
	public void evaporate (double evap)	{ anchored *= evap; }

	public void fromLog (StringTokenizer st, short id)
	{
		this.id			= id;
		rho				= Integer.parseInt (st.nextToken ());
		theta			= Float.parseFloat (st.nextToken ());
		anchored			= Float.parseFloat (st.nextToken ());
		last_anchored	= Integer.parseInt (st.nextToken ());
		
		lpos.correct(rho, theta);
	}

	public void clamp (Odometry odo, double dt)
	{
		float		drift;
		double		rho_aux, theta_aux;
		double		x, y;
		
		rho_aux = (double) rho;
		theta_aux = (double) theta;
			
		// Compute overall decrease in anchoring
		switch (type)
		{
			case BALL:
				drift = 1.0f - DriftBall;
				break;
				
			case MATE:
			case OPPONENT:
				drift = 1.0f - DriftDog;
				break;
				
			case NET:
			case LANDMARK:
				drift = 1.0f-DriftLM;
				break;
				
			default:
				drift = 1.0f;
		}
		
		if (odo.dlin != 0.0)	drift *= (1.0f - DriftWalk);
		if (odo.drot != 0.0)	drift *= (1.0f - DriftTurn);
		
		// only consider time since last update when caculating amount to drift
		drift = 1.0f - ((1.0f - drift)*(float)dt);
		evaporate (drift);
		
		// Introduce half rotation
		theta_aux -= (odo.drot * 0.5);
		
		// Perform translation in Cartesian coordinates
		x = rho_aux * Math.cos(theta_aux) - odo.dlin;
		y = rho_aux * Math.sin(theta_aux) - odo.dlat;
		
		// Back to polar coordinates
		rho_aux = (int) Math.sqrt(x*x + y*y);
		
		if (rho_aux != 0.0)
			theta_aux = Math.atan2(y,x);
		
		// Second half rotation
		theta_aux = Angles.radnorm_180 (theta_aux - odo.drot * 0.5);
		
		// Store the values
		rho = (int)rho_aux;
		theta = (float)theta_aux;
		
		lpos.predict(odo);
		
	}

	public void anchor (int rho, float theta, float reliability, int time)
	{
		this.rho		= rho;
		this.theta	= theta;
		
		if (reliability > anchored)
			anchored = reliability;
				
		last_anchored = time;
		do_clamping = false;
		
		lpos.correct(rho, theta);
	}

	public String toString()
	{
		return ("ID = "+this.id+" Type = "+this.type+" Rho = "+this.rho+" Theta = "+this.theta+ " Anchored = "+this.anchored);
	}
}
