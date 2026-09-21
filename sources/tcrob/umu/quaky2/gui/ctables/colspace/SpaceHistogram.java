/**
 * Created on 25-nov-2005
 *
 * @author Humberto Martinez Barbera
 */
package tcrob.umu.quaky2.gui.ctables.colspace;

import java.awt.*;
import java.awt.image.*;

import tclib.vision.chaos.channels.*;

public class SpaceHistogram 
{
	static public final int		COMP0	 = 0;
	static public final int		COMP1	 = 1;
	static public final int		COMP2	 = 2;
	
	static public final int		SIZE	 = 256;

	protected Channels			chs;
	protected int				bkg;
	
	public SpaceHistogram (Channels chs, Color color)
	{
		this.chs		= chs;
		this.bkg		= color.getRGB ();
	}

//	public void histogram (ptolemy.plot.Histogram plot, BufferedImage iproj, int band)
//	{
//		int				i;
//		int				max;
//		int[]			nBins = { SIZE, SIZE, SIZE };	
//		int[]			bin;
//		double[]			lows = { 0.0,0.0,0.0 };			// lowest value for each band 	
//		double[]			highs = { SIZE, SIZE, SIZE };		// highest value for each band
//		javax.media.jai.Histogram		hist;
//		
//		Integer xperiod = Integer.valueOf (1);
//		Integer yperiod = Integer.valueOf (1);
//		RenderedOp rop = HistogramDescriptor.create (iproj, null, xperiod, yperiod, nBins, lows, highs, null);
//		hist = (javax.media.jai.Histogram) rop.getProperty ("histogram");
//		
//		plot.clear (false);
////		plot.setBars (1.0, 0.0);
////		plot.setBinWidth (1.0);
//		bin = hist.getBins (band);
//		max = 0;
//		for (i = 0; i < hist.getNumBins (band); i++)
//		{
////			plot.addPoint (0, i, bin[i], false);
//			plot.addPoint (0, bin[i]);
//			if (bin[i] > max)
//				max = bin[i];
//		}
////		plot.setYLog (true);
////		plot.setXRange (0, SIZE);
////		plot.setXRange (0, hist.getNumBins (band));
////		plot.setYRange (0, max);
//		plot.fillPlot ();
//
////		System.out.println("Media de la banda "+ i+ " "+hist.getMean()[i]);
////		System.out.println("Entropia "+hist.getEntropy()[i]);
//	}
	
	public void histogram(ptolemy.plot.Histogram plot, BufferedImage iproj, int band)
	{
	    int width = iproj.getWidth();
	    int height = iproj.getHeight();
	    int[] bins = new int[SIZE]; 
	    int max = 0;

	    Raster raster = iproj.getRaster();
	    for (int y = 0; y < height; y++) 
	        for (int x = 0; x < width; x++)
	        {
	            int sample = raster.getSample(x, y, band);
	            if (sample >= 0 && sample < SIZE)
	                bins[sample]++;
	        }

	    plot.clear(false);
	    for (int i = 0; i < SIZE; i++)
	    {
	        plot.addPoint(0, bins[i]);
	        if (bins[i] > max)
	            max = bins[i];
	    }
	    plot.fillPlot();
	}
}
