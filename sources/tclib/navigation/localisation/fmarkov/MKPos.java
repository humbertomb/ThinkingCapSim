/*
 * (c) 2002 Alessandro Saffiotti
 * (c) 2004 David Herrero Perez
 *
 * Result position of Fuzzy Markov Localisation Process
 * The position is modelled as a position with orientation and its uncertainly
 *
 */
 
package tclib.navigation.localisation.fmarkov;

/**
 * @author David Herrero-Perez
 * @version 1.0
 * 
 */
public class MKPos {
	public static final int START_XPOS = 40;
	public static final int START_YPOS = 50;
	
	public static final double START_ANGLE = 0.0;
	//public static final double START_ANGLE = Math.PI / 2;
	//public static final double START_ANGLE = Math.PI;
	//public static final double START_ANGLE = -Math.PI / 2;
	
	public static final int START_XWIDTH = 30;
	public static final int START_YHEIGHT = 30;
	public static final double START_ANGLEWIDTH = Math.toRadians(60.0);
	
	private double x, y;
	private double theta;
	private double dx, dy;
	private double dtheta;
	
	// Constructors
	public MKPos()
	{
		set(START_XPOS, START_YPOS, START_ANGLE, START_XWIDTH, START_YHEIGHT, START_ANGLEWIDTH);
	}
	
	public MKPos(double x, double y, double theta, double dx, double dy, double dtheta)
	{
		set(x, y, theta, dx, dy, dtheta);
	}
	
	// Accessors
	public void set(double x, double y, double theta, double dx, double dy, double dtheta)
	{
		this.x = x;
		this.y = y;
		this.theta = theta;
		this.dx = dx;
		this.dy = dy;
		this.dtheta = dtheta;
	}
	
	public double getX()
	{
		return x;
	}
	
	public double getY()
	{
		return y;
	}
	
	public double getTheta()
	{
		return theta;
	}
	
	public double getDX()
	{
		return dx;
	}
	
	public double getDY()
	{
		return dy;
	}
	
	public double getDTheta()
	{
		return dtheta;
	}
	
	public void setX(double x)
	{
		this.x = x;
	}
	
	public void setY(double y)
	{
		this.y = y;
	}
	
	public String toString()
	{
		return "[ Position x: " + x + ", y: " + y + ", theta: " + Math.toDegrees(theta) + "\n , dx: " + dx + ", dy: " + dy + ", dtheta: " + Math.toDegrees(dtheta) + "]";
	}
}
