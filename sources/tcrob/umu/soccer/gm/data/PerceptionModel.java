/**
 * Created on 16-jun-2006
 *
 * @author Humberto Martinez Barbera
 */

package tcrob.umu.soccer.gm.data;

public class PerceptionModel
{
	static public final int DISTANCE_BEARING			= 0;
	static public final int BEARING_ONLY				= 1;
	static public final int BEARING_WITH_THRESHOLD		= 2;

	public int model;		// perception model
	public int index;		// object id
	public double rho;		// distance to object
	public double mrho;		// distance to object for aplied the sensor model
	public double theta;		// angle to object
	public double dcore;		// uncertainly distance (core of fuzzy trapezoid)
	public double dslope;		// uncertainly distance (slope of fuzzy trapezoid)
	public double acore;		// uncertainly angle (core of fuzzy trapezoid)
	public double aslope;		// uncertainly angle (slope of fuzzy trapezoid)
}
