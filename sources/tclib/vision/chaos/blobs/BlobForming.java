/**
 * Created on 5-oct-2005
 *
 * @author Humberto Martinez Barbera
 */
package tclib.vision.chaos.blobs;

import java.awt.*;
import java.awt.image.*;

import tclib.vision.chaos.channels.*;
import tclib.vision.chaos.segment.*;

public abstract class BlobForming
{
	protected Channels				channels;
	protected Segmentation			segment;
	protected Blobs[]				blobs;
	
	public final Blobs[]		getBlobs ()		{ return blobs; }
	
	public abstract void process (Segmentation segment);
	public abstract boolean configurable ();
	public void configureDialog () { }
	
	public void postProcess ()
	{				
		// Blob post-processing
		for (int i = 0; i < channels.getNumChannels (); i++)
			if (channels.at (i).blobbed)
			{
//				blobs[i].sortByArea ();
//				blobs[i].merge (channels.at (i).gap);
				blobs[i].sortByArea ();
				blobs[i].prune ();
			}
		
//		dumpBlobs ();
	}

	/**
	 * Draws a box for each blob
	 */
	public BufferedImage getBlobbedImage ()
	{
		BufferedImage		output;
		
		output	= new BufferedImage (segment.getWidth(), segment.getHeight(), BufferedImage.TYPE_INT_RGB);
		Graphics2D g2 = output.createGraphics ();

		// Put blobs in image
		g2.setStroke (new BasicStroke (3));
		for (int i = 0; i < channels.getNumChannels(); i++)
		{
			Channel ch = channels.getChannelID (i);
			if (!ch.blobbed) continue;

			for (int j = 0; j < blobs[i].getBlobNumber (); j++)
			{
				Blob blob = blobs[i].blob[j];
				g2.setColor (ch.color);
				g2.drawRect (blob.getXMin (), blob.getYMin (), blob.getXMax ()-blob.getXMin (), blob.getYMax ()-blob.getYMin ());
				// g2.drawString (Integer.toString (j), blob.getXMin (), blob.getYMin ());
			}
		}	
				
		return output;
	}

	public void dumpBlobs ()
	{
		int			i, j;
		Channel		ch;
		
		System.out.println ("\nDumping BLOBs");
		for (i = 0; i < channels.getNumChannels (); i++)
		{
			ch = channels.getChannelID (i);
			if (!ch.blobbed)					continue;
			if (blobs[i].getBlobNumber () < 1)	continue;
						
			System.out.println ("\tBlobs for channel#"+i+"<"+ch.name+">");
			System.out.print ("\t\t");
			for (j = 0; j < blobs[i].getBlobNumber (); j++)
				System.out.print ("b<"+j+">["+blobs[i].blob[j].getNumPixels ()+"]  ");			
			System.out.println ();
		}
		System.out.println ("---------------------------------------");
	}
}
