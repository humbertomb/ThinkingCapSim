/**
 * Title: GeoPos
 * Description: Clase para realizar transformaciones entre coordenadas LaLong y UTM
 * Copyright: Copyright (c) 2001
 * Company: Grupo ANTS - Proyecto MIMICS
 * @author Juan Pedro Cánovas Quiñonero
 * @version 1.0
 */

package devices.pos;

public class Ellipsoid extends Object
{
	public String			ellipsoidName;
	public double			equatorialRadius;
	public double			eccentricitySquared;

	public Ellipsoid (String name, double radius, double eccsq)
	{
		ellipsoidName		= name;
		equatorialRadius	= radius;
		eccentricitySquared	= eccsq;
	}

	/**
	 * Returns a string representation of the object.
	 * @return String representation
	 */
	public String toString () {
	return "Ellipsoid[name=" + ellipsoidName + ", radius=" + equatorialRadius + ", eccsq=" + eccentricitySquared + "]";
	}
}
