/**
 * Created on 30-jun-2006
 *
 * @author Humberto Martinez Barbera
 */
package tcrob.umu.soccer.gm.particle;

public class GaussianSample 
{
	public double				a;
	public Gaussian2D			g;

	public GaussianSample ()
	{
		g	= new Gaussian2D ();
	}
	
	public void set (GaussianSample other)
	{
		g.set(other.g);
		a	= other.a;
	}
}
