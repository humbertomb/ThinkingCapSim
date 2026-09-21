/**
 	@author Humberto Martinez Barbera
 */

package tclib.vision.chaos.channels;

import java.awt.*;

public class Channel
{
	static protected int 		q			= 0;
	
	public int					id;
	public String				name;
	public Color				color;
	
	public boolean				segmented;
	public boolean				blobbed;
	public int					threshold	= 0;
	public int					gap			= 0;
	public int					minpix		= 0;
	
	// loadable parameters
	public String				clustype;
	public transient String		params;		// the limits of the cluster: made from its seeds, so not kept in the files
	public String				seeds;
	
	// temporary items
	private transient Cluster	cluster;
	private transient Pixel		pix;
	private transient int		seq 			= 0;

	public Channel ()
	{		
		this.pix		= new Pixel (0);
		
		resetCluster ();
	}
	
	public Channel (String name, Color color, int id)
	{
		this ();
		
		this.name	= name;
		this.id		= id;
		this.color	= color;
	}
			
	public final int getSequence ()			{ return seq; }
	public final Cluster getCluster ()		{ return cluster; }
	
	public void resetCluster ()
	{
		cluster = new ColorPrism ();
	}

	public void getClusterParameters ()
	{
		clustype		= cluster.getClass ().getName ();	
		params		= cluster.paramRawData ();
		seeds		= cluster.seedsRawData ();
	}

	public void setClusterParameters ()
	{
		seq = (q ++);

		try 
		{
			Class<?>		sclass;
			
			sclass	= Class.forName (clustype);
			cluster	= (Cluster) sclass.getDeclaredConstructor().newInstance ();
			cluster.initialise (params, seeds);
		} catch (Exception e) { e.printStackTrace (); }
	}
	
	public void addSeed (int value)
	{
		cluster.include (new Pixel (value));

		if (cluster.isModified ())
			seq = (q ++);	
	}
	
	public void removeSeed (int value)
	{
		cluster.exclude (new Pixel (value));
		
		if (cluster.isModified ())	
			seq = (q ++);
	}
 
	public boolean insideChannel (int value)
	{
		pix.set (value);
		return cluster.inside (pix);
	}
	 
	public boolean isSeed (int value)
	{
		pix.set (value);
		return cluster.getSeeds ().contains (pix);
	}
}