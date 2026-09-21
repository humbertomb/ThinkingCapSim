/**
 * Created 10-dec-2018
 * 
 * @author Humberto Martinez Barbera
 */
package tclib.vision.chaos;

import dasboot.data.*;
import tclib.vision.chaos.channels.*;

public class ChaosPamConfig extends JsonData
{
	static public final String[]			LUTMODES			= { "LUTStandard", "LUTGrowing" };
	static public final String[]			SEGMODES			= { "Thresholding", "SeedRegionGrowing" };
	static public final String[]			BLOBMODES		= { "RLEBlobForming", "BlobGrowing",  };

	public Channels						channels;	

	public int							lutmode			= 0;
	public int							segmode			= 0;	
	public int							blobmode			= 0;
	
	public String						recogclass;
	
	public ChaosPamConfig ()
	{
		channels		= new Channels ();	
	}
	
	public void set (JsonData data)
	{
		ChaosPamConfig other = (ChaosPamConfig) data;

		channels		= other.channels;		

		lutmode		= other.lutmode;
		segmode		= other.segmode;
		blobmode		= other.blobmode;
		
		recogclass	= other.recogclass;		
	}
	
	public void loadFromFile (String filename)
	{
		try { fromJsonFile (filename); } catch (Exception e) { e.printStackTrace(); }		
		
		// Update channel seeds (after loading)
		for (Channel channel : channels.channels)
			channel.setClusterParameters ();
	}
	
	public void saveToFile (String filename)
	{	
		// Update channel seeds (before saving)
		for (Channel channel : channels.channels)
			channel.getClusterParameters ();

		try { toJsonFile (filename); } catch (Exception e) { e.printStackTrace(); }				
	}
}
