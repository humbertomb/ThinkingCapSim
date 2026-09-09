/**
 * Created on 28-jun-2006
 *
 * @author Humberto Martinez Barbera
 * @author Scott Lenser (CMU)
 */

package wucore.utils.math.stat;

public class GaussianEvaluator 
{
	protected double[]				mean;
	protected double[]				stdDev;
	protected double 				minProb		= 0.0;
	
	public GaussianEvaluator (int numVars)
	{
		mean			= new double[numVars];
		stdDev		= new double[numVars];
	}
	
	public void setMinProb (double minProb)		{ this.minProb = minProb; }
	public double getMean (int i)					{ return mean[i]; }
	public double getStdDev (int i)				{ return stdDev[i]; }
	
	public void setMeanDev (int i, double mean_parm, double std_dev) 
	{
		mean[i]  	= mean_parm;
		stdDev[i]	= std_dev;
	}

	public void evaluate (double[] in, double[] out)
	{
		for (int i = 0; i < mean.length; i++) 
			out[i] = evaluate (i, in[i]);
	}
	
	public double evaluate (int i, double in)
	{
		double		x, out;
		
		x			= (in - mean[i]) / stdDev[i];
		out			= Math.max (Math.exp (-(x * x) / 2.0), minProb);
			
		return out;
	}
}
