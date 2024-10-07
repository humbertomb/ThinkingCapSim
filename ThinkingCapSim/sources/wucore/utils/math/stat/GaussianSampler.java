/**
 * Created on 28-jun-2006
 *
 * @author Humberto Martinez Barbera
 * @author Scott Lenser (CMU)
 */
package wucore.utils.math.stat;

public class GaussianSampler
{
	protected Range[]				range;
	protected double[]				mean;
	protected double[]				stdDev;
	protected double[]				normLow;
	protected double[]				normHigh;
	protected boolean[]				noBounds;
	
	protected RandomNumberGenerator	random;
	
	public GaussianSampler (int numVars)
	{
		random		= new RandomNumberGenerator ();
		mean			= new double[numVars];
		stdDev		= new double[numVars];
		normLow		= new double[numVars];
		normHigh		= new double[numVars];
		noBounds		= new boolean[numVars];
		range		= new Range[numVars];
		
		for (int i = 0; i < numVars; i++)
		{
			range[i]			= new Range ();
			
			mean[i]			= 0.0;
			stdDev[i]		= 1.0;
			range[i].low 	= -Double.MAX_VALUE;
			range[i].high	= +Double.MAX_VALUE;
			normLow [i]		= -Double.MAX_VALUE;
			normHigh[i]		= +Double.MAX_VALUE;
			noBounds[i]		= true;
		}
	}
	
	public void setRange(int i, double low, double high)
	{
		range[i].low =low;
		range[i].high=high;
		
		if(range[i].low==-Double.MAX_VALUE && range[i].high==+Double.MAX_VALUE)
			noBounds[i]=true;
		else
			noBounds[i]=false;
		
		normLow [i]=(range[i].low  - mean[i]) / stdDev[i];
		normHigh[i]=(range[i].high - mean[i]) / stdDev[i];
	}
	
	public void setRange (int i, Range srange) 
	{
		setRange (i, srange.low, srange.high);
	}
	
	public void setMeanDev (int i, double mean_parm, double std_dev) 
	{
		mean[i]  =mean_parm;
		stdDev[i]=std_dev;
		
		normLow [i]=(range[i].low  - mean[i]) / stdDev[i];
		normHigh[i]=(range[i].high - mean[i]) / stdDev[i];
	}
		
	public void generateSample (double[] buf)
	{
		for(int i=0; i<mean.length; i++)
			if (noBounds[i])
				buf[i] = random.nextGaussian (mean[i], stdDev[i] * stdDev[i]);
			else
				buf[i] = random.nextGaussian (mean[i], stdDev[i] * stdDev[i], normLow[i], normHigh[i]);
	}

}
