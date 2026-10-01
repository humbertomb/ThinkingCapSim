package tcrob.umu.soccer.gm.data;


public class LocLpo
{	
	public final static int			BALL			= 0;
	public final static int			LANDMARK		= 1;
	public final static int			NET				= 2;
	public final static int			MATE			= 3;
	public final static int			OPPONENT		= 4;

	public int						id;
	public int						type;
	public double					rho;
	public double					theta;
	public double					anchored;
	
	public int						last_anchored;
	
	public LocLpo ()
	{
		set (0, 0, 0, 0, 0);
	}
	
	public LocLpo (int id, int type)
	{
		set (id,type,0,0,0);
	}
	
	public void set (int id,int type,double rho,double theta,double anchored)
	{
		this.id = id;
		this.type = type;
		this.rho = rho;
		this.theta = theta;
		this.anchored = anchored;

		last_anchored = 0;
	}
}
