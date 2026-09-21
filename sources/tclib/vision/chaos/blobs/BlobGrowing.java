/**
 * Created on 05-oct-2005
 *
 * @author Humberto Martinez Barbera
 */
package tclib.vision.chaos.blobs;

import tclib.vision.chaos.channels.*;
import tclib.vision.chaos.segment.Segmentation;

public class BlobGrowing extends BlobForming
{
	// Blob forming data
	private int					numPixImage	= -1;
	private int					numChannels = -1;
	private int[]				xTable;
	private int[]				yTable;
	private boolean[]			selected;
	
	// Segmentation data
	private int					width;
	private int					height;
	private int[]				segmented;

	public void process (Segmentation segment)
	{		
		int				i;
		int				x, y;
		int				pixel;
		Channel			ch;
				
		this.segment 	= segment;
		
		this.width		= segment.getWidth ();
		this.height		= segment.getHeight ();
		this.channels	= segment.getChannels ();
		this.segmented	= segment.getSegmented ();
		
		// Perform tables initialisation
		if (height * width != numPixImage)
		{
			numPixImage		= height * width;
			xTable			= new int [numPixImage];
			yTable			= new int [numPixImage];
			selected			= new boolean [numPixImage];
		}
		
		// Perform channel initialisation
		if (channels.getNumChannels () != numChannels)
		{
			numChannels		= channels.getNumChannels ();
			blobs			= new Blobs [numChannels];		
			for (i = 0; i < numChannels; i++)
				blobs[i]		= new Blobs ();
		}

		for (i = 0; i < numPixImage; i++)
			selected[i] = false;
		for (i = 0; i < numChannels; i++)
			blobs[i].initialise ();
		for (y = 0; y < height; y++)
			for (x = 0; x < width; x++)
			{
				pixel	= (y * width) + x;
				ch		= channels.getChannelID (segmented[pixel]);
				
				// Check labelling conditions
				if (selected[pixel])				continue;
				if (ch == null)					continue;
				if (!ch.blobbed)					continue;
											
				createBlob (x, y, ch.minpix);
			}
	}

	protected void createBlob (int xo, int yo, int minpix)
	{
		int			x, y;
		int			endTable, startTable;
		int			channel;		
		int			pos, newpos;
		Blob			blob;
		
		x			= xo;
		y			= yo;
		endTable		= 0;
		startTable	= -1;
		
		pos			= (y * width) + x;			
		channel		= segmented[pos];
						
		blob = blobs[channel].getNewBlob ();		
		blob.initialise ();
		
		while (startTable != endTable)
		{
			pos = (y * width) + x;			
			blob.update (x, y);
			
			// check left cell
			newpos = pos - 1;
			if ((newpos > -1) & (newpos < numPixImage) & (x > 0))
				if ((segmented[newpos] == channel) & !selected[newpos])
				{
					selected[newpos]	= true;
					xTable[endTable] = x-1; yTable[endTable] = y;
				   	endTable++;
				}
			
			// check right cell
			newpos = pos + 1;
			if ((newpos > -1) & (newpos < numPixImage) & (x < (width - 1)))
				if ((segmented[newpos] == channel) & !selected[newpos])
				{
					selected[newpos]	= true;
					xTable[endTable] = x+1; yTable[endTable] = y;
					endTable++;
				}
			
			// check upper cell
			newpos = pos - width;
			if ((newpos > -1) & (newpos < numPixImage) & (y > 0))
				if ((segmented[newpos] == channel) & !selected[newpos])
				{
					selected[newpos]	= true;
					xTable[endTable] = x; yTable[endTable] = y-1;
			       	endTable++;
				}
			
			// check lower cell
			newpos = pos + width;
			if ((newpos > -1) & (newpos < numPixImage) & (y < (height - 1)))
				if ((segmented[newpos] == channel) & !selected[newpos])
				{
					selected[newpos]	= true;
					xTable[endTable] = x; yTable[endTable] = y+1; 
			       	endTable++;
				}
			
			startTable++;
		    x = xTable[startTable];
		    y = yTable[startTable];
		}
		
		// Do not consider a malformed blob
		if (!blob.hasHeight () || !blob.hasWidth () || (blob.getNumPixels() < minpix))
			blobs[channel].discardBlob ();
		else
			blob.computeShape ();
	}
	
	public boolean configurable ()
	{
		return false;
	}
}
