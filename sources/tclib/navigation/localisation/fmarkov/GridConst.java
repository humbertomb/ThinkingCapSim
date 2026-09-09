/*
 * (c) 2002 Alessandro Saffiotti
 * (c) 2004 David Herrero Perez
 *
 *	Fuzzy Markov 2.5 Grid 
 *
 */
 
package tclib.navigation.localisation.fmarkov;

/**
 * @author David Herrero-Perez
 * @version 1.0
 * 
 */
public class GridConst {
	// NOTE: When the feature is a line, Dist & Angle are polar coordinates (rho, theta)
	private double dist;		// Distance to feature
	private double angle;		// Angle to feature
	
	private boolean isPossible; // Flag indicating if it is possible percept the object from this cell position
	
	// Constructors
	public GridConst()
	{
		Create();
	}
	
	public GridConst(double dist, double angle, boolean isPossible)
	{
		this.dist = dist;
		this.angle = angle;
		this.isPossible = isPossible;
	}
	
	// Accessor methods
	public final double Dist() { return dist; }
	public final double Angle() { return angle; }
	public final boolean IsPossible() { return isPossible; }
	
	// Instance methods
	private final void Create()
	{
		this.dist =0.0;
		this.angle = 0.0;
		this.isPossible = false;
	}
}
