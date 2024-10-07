/**
 * Created on 27-jun-2006
 *
 * @author Humberto Martinez Barbera
 * @author Scott Lenser (CMU)
 */
package wucore.utils.math.stat;

public class Range
{
	public double			low;
	public double			high;
	
	public Range ()
	{
		low		= -Double.MAX_VALUE;
		high		= Double.MAX_VALUE;
	}
	
	public Range (int low, int high)
	{
		this.low		= low;
		this.high	= high;
	}
}
