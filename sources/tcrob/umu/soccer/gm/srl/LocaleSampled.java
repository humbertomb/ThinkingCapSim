/**
 * Created on 27-jun-2006
 *
 * @author Humberto Martinez Barbera
 * @author Scott Lenser (CMU)
 */
package tcrob.umu.soccer.gm.srl;

public class LocaleSampled 
{
	public Sample[]			sample;
	public int				numSamples;
	
	public LocaleSampled (int numSamples)
	{
		this.numSamples=numSamples;
		
		sample	= new Sample[numSamples];
		for (int i = 0; i < numSamples; i++)
			sample[i]	= new Sample ();
	}
}
