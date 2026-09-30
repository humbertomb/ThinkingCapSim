package tcrob.umu.soccer.gm.data;


import wucore.utils.math.*;
import wucore.utils.math.jama.Matrix;

public class LocLpo
{	
	public final static int			BALL			= 0;
	public final static int			LANDMARK		= 1;
	public final static int			NET				= 2;
	public final static int			MATE			= 3;
	public final static int			OPPONENT		= 4;

	private int						id;
	private int						type;
	private int						rho;
	private float					theta;
	private float					anchored;
	private int						last_anchored;
	
	private EKFLpos					lpos;
	
	public LocLpo ()
	{
		Matrix S0 = new Matrix(2,1);
		
		set ((short)0,(short)0,0,0,0);
		
		S0.set(0, 0, 0);
		S0.set(1, 0, 0);
		lpos = new EKFLpos(S0, 0.3, 0.3, 0.3, 1000.0, Angles.DTOR * 7.0);
	}
	
	public LocLpo (int id, int type)
	{
		set (id,type,0,0,0);
	}
	
	public void set (int id,int type,int rho,float theta,float anchored)
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
	
	public int getRho()					{ return this.rho;}
	public float getTheta()				{ return this.theta;}
	public float getAnchored()	{ 
		return this.anchored;
		}
	
	public Matrix getP() { return lpos.getP(); };
	
	public int getLastAnchored ()			{ return last_anchored; }
}
