/**
 * Created on 5-nov-2005
 *
 * @author Francisco Antonio Bas Esparza
 */
package tclib.vision.chaos.blobs;

import java.awt.image.*;
import java.awt.*;

import tclib.vision.chaos.channels.*;
import tclib.vision.chaos.segment.*;

public class RLEBlobForming extends BlobForming
{
	class CMVRun
	{
		int			x, y, width;			// location and width of run
		int			color;				// which color(s) this run represents
		CMVRun		parent,next;   	 	// parent run and next run in run list
		Blob			blob;				// blob it belongs (will be used and updated 
										// during the extractRegions procedure) 
	}
	
	// Blob forming data
	private CMVRun				rleMap;
	private int					numChannels = -1;
	
	// Segmentation data
	private int					width;
	private int					height;
	private int[]				segmented;
	
	/**
	 * Creates color blobs from a segmented image
	 * @param cvinput image used for the segmentation process
	 * @param segmented segmented image
	 * @pfaram chs channels used for the segmentation process
	 */
	public void process (Segmentation segment)
	{
		this.segment 	= segment;
				
		this.width		= segment.getWidth ();
		this.height		= segment.getHeight ();
		this.channels	= segment.getChannels ();
		this.segmented	= segment.getSegmented ();
		
		if ((width == 0) || (height == 0))		return;
		
		//Steps:
		// 1: Find connected regions (using LRE) in rows
		// 2: Connect components using four-connecteness so that the runs each
		//    identify the global parent of the connected region they are a part of
		// 3: Extract region information from merged RLE map (bounding box, centroid and size)
		// 4: Eliminate errors generated in the bottom up region generation
		
		//if (true)return testCMBlobForming(chs);
							
		//Step 1
		encodeRuns ();
		//Step 2
		connectComponents ();
		//Step 3
		extractRegions (channels);
		//Step 4
		// TBD
	}
	
	/**
	 * Creates a rle map from an image
	 * @param cvinput iamge ised for the segmentation
	 * @param segmented segmented image
	 */
	private void encodeRuns ()
	{
		int ch;
		int x,y,lx;
		CMVRun r;
		
		rleMap = new CMVRun();
		r = rleMap;
		for(y=0;y<height;y++)
		{
			x = 0;
			while (x<width)
			{
				ch = segmented[y*width+x];
				//System.out.println("("+x+","+y+") Channel = "+ch);
				lx = x;
				while ((x<width) && (segmented[y*width+x] == ch)) x++;
				
				if (ch!=Channels.NO_COLOR)
				{
					r.next = new CMVRun();
					r = r.next;
					r.y = y;
					r.x = lx;
					r.color = ch; 
					r.width = x - lx;
					r.parent = r;
				}
			}
		}
		//Discard first node
		rleMap = rleMap.next;
	}
	
	/**
	 * Connects the runs
	 *
	 */
	private void connectComponents ()
	{
		CMVRun r1 = rleMap;
		CMVRun r2 = rleMap;
		
		//Continue the procedure while we haven't visited all the nodes
		//in the run list
		while ((r1!=null) && (r2!=null))
		{
			//System.out.println("r1("+r1.x+","+r1.y+") r2("+r2.x+","+r2.y+")");
			if (r2.y<=r1.y) 		r2 = r2.next;
			else if (r2.y>r1.y+1) 	r1 = r1.next;
			else
			{
				if (r2.color == r1.color)
				{
					// case 1: r2.x <= r1.x < r2.x + r2.width
					// case 2: r1.x <= r2.x < r1.x + r1.width
					if ((r1.x>=r2.x && r1.x<r2.x+r2.width) ||
					    (r2.x>=r1.x && r2.x<r1.x+r1.width))
					{
						if (r2.parent == r2)
							r2.parent = r1.parent.parent;
						else
						{
							if (r2.parent.parent.y>r1.parent.y)
								r2.parent.parent = r1.parent;
							else
								r1.parent.parent = r2.parent.parent;
						}
					}
				}
				//Move to next point where values may change
				if (r2.x+r2.width<=r1.x+r1.width) r2 = r2.next;
				else r1 = r1.next;
			}
		}
		//Compress all parent paths
		for(CMVRun r=rleMap;r!=null;r=r.next)
			r.parent = r.parent.parent;
	}
	
	/**
	 * Extracts regions (blobs) from the connected rle map
	 * @param chs Channels used during the segmentation process
	 */
	private void extractRegions (Channels chs)
	{
		//Perform blobs initialisation
		numChannels		= chs.getNumChannels ();
		blobs			= new Blobs [numChannels];		
		for (int i = 0; i < numChannels; i++)
		{
			blobs[i]		= new Blobs ();
			blobs[i].initialise ();
		}
		
		for(CMVRun r=rleMap;r!=null;r=r.next)
		{
			//System.out.println("Checking ("+r.x+","+r.y+") --> ("+r.parent.x+","+r.parent.y+")"+r.color);
			if (r==r.parent)
			{
				int channel = r.color;
				Channel ch = chs.getChannelID (channel);
				if (!ch.blobbed) continue;
				
				//Create a new Blob if this run is a root (i.e. self parented)
				Blob blob = blobs[channel].getNewBlob ();		
				blob.initialise ();
				r.blob = blob;
				blob.npixel = r.width;
				blob.xmin = r.x;
				blob.ymin = r.y;
				blob.xmax = r.x+r.width-1;
				blob.ymax = r.y;
				blob.m10 = range_sum(r.x,r.width);
				blob.m01 = r.y*r.width;
			}	
			else
			{
				int channel = r.color;
				Channel ch = chs.getChannelID (channel);
				if (!ch.blobbed) continue;
				// Otherwise update region stats incrementally
				Blob blob = (r.parent != null ? r.parent.blob : null);
				r.blob = blob;
				if (blob != null)		// --HMB 10/12/2018
				{
					blob.npixel += r.width;
					blob.xmin = Math.min (r.x,blob.xmin);
					blob.xmax = Math.max (r.x + r.width-1,blob.xmax);
					blob.ymax = r.y;
					blob.m10 +=range_sum(r.x,r.width);
					blob.m01 += r.y*r.width;
				}
			}
		}
		
		//Discard non usable blobs and calculate centroids from stored sums
		for (int i = 0; i < numChannels; i++)
		{
			Channel ch = chs.getChannelID (i);
			for(int j=0;j<=blobs[i].blobNumber;j++)
			{
				Blob blob = blobs[i].getBlob(j);
				
				//Discard non usable blobs
				if (blob.hasHeight () && blob.hasWidth () && (blob.getNumPixels() >= ch.minpix))
					blob.computeShape();
			}
		}
	}
	
	/**
	 * 
	 */
	public boolean configurable (){
		return false;
	}
	
//	==== Utility Functions ===========================================//
	/** sum of integers over range [x,x+w) */
	static private int range_sum (int x,int w)
	{
		return w*(2*x + w-1) / 2;
	}
	
	
//	==== Test Functions ===========================================//
	/**
	 * Draws a dot in the first pixel of every run (BLACK if the runis 
	 * a root, or PINK, if it is not)
	 */
	protected BufferedImage drawRuns(BufferedImage input)
	{
		BufferedImage output = new BufferedImage(input.getWidth(),input.getHeight(), BufferedImage.TYPE_INT_RGB);
		for(int y=0;y<output.getHeight();y++)
			for(int x=0;x<output.getWidth();x++)
				output.setRGB(x,y,input.getRGB(x,y));
		
		for(CMVRun r=rleMap;r!=null;r=r.next)
		{
			int x1 = r.x;
			int y1 = r.y;
//			int x2 = r.x+r.width-1;
//			int y2 = r.y;
			//int x2 = r.parent.x;
			//int y2 = r.parent.y;
			//System.out.println("("+x1+","+y1+") ("+x2+","+y2+") "+r.width);
			if (r.parent==r)
				output.setRGB(x1,y1,Color.BLACK.getRGB());
			else
				output.setRGB(x1,y1,Color.PINK.getRGB());
			
			//output.setRGB(x2,y2,Color.BLACK.getRGB());
			/*int pendiente;
			if (y1==y2) pendiente = 0;
			else pendiente = (x2-x1)/(y2-y1);
			for(int x=x1;x!=x2;x+=(x2-x1)/Math.abs(x2-x1))
			{
				int y = y1+ (x-x1)*pendiente;
				output.setRGB(x,y,Color.BLACK.getRGB());
			}*/
		}
		return output;
	}
}
