/**
 * Created on 27-jun-2006
 *
 * @author Humberto Martinez Barbera
 * @author Scott Lenser (CMU)
 */
package wucore.utils.math.stat;

public class UniformSampler
{
	protected Range[]				range;
	protected int					numVars;
	protected RandomNumberGenerator	random;
	
	public UniformSampler (int numVars)
	{
		random	= new RandomNumberGenerator ();
		range	= new Range[numVars];
		for (int i = 0; i < numVars; i++)
			range[i]	= new Range ();
	}

	public void setRange (int i, double low, double high)
	{
		range[i].low 	= low;
		range[i].high	= high;
	}

	public void generateSample (double[] buf)
	{
		for(int i=0; i<numVars; i++)
			buf[i] = random.nextUniform (range[i].low, range[i].high);
	}
}
