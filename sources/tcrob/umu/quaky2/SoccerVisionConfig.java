/**
 * Created 10-dec-2018
 * 
 * @author Humberto Martinez Barbera
 */
package tcrob.umu.quaky2;

import tclib.vision.chaos.channels.*;

public class SoccerVisionConfig
{
	static public final String[]		LUTMODES		= { "LUTStandard", "LUTGrowing" };
	static public final String[]		SEGMODES		= { "Thresholding", "SeedRegionGrowing" };
	static public final String[]		BLOBMODES		= { "RLEBlobForming", "BlobGrowing",  };

	public Channels						channels;	

	public int							lutmode			= 0;
	public int							segmode			= 0;	
	public int							blobmode		= 0;	
	public String						recogclass;
	
	public SoccerVisionConfig ()
	{
		initChannels ();
	}

	/**
	 * The channels written in here, until they are read from a file: the colour
	 * channels of the soccer field (the "channels" of its JSON configuration).
	 */
	public void initChannels ()
	{
		// name, colour (ARGB), segmented, blobbed, threshold, gap, minpix, cluster class, parameters, seeds
		final Object[][]	CHANNELS	=
		{
			{ "BLACK",	-16777216,	false,	false,	0,	0,	4,	ColorPrism.class,	"0,255,0,255,0,255",	"EMPTY" },
			{ "WHITE",	-1,			false,	false,	0,	0,	4,	ColorPrism.class,	"0,255,0,255,0,255",	"EMPTY" },
			{ "RED",	-65536,		false,	false,	0,	0,	4,	ColorPrism.class,	"0,255,0,255,0,255",	"EMPTY" },
			{ "BLUE",	-16776961,	true,	true,	5,	3,	4,	ColorPrism.class,	"0,255,0,255,0,255",	"EMPTY" },
			{ "GREEN",	-16711936,	true,	true,	5,	0,	4,	ColorPrism.class,	"0,255,0,255,0,255",	"EMPTY" },
			{ "YELLOW",	-256,		true,	true,	5,	3,	4,	ColorPrism.class,	"0,255,0,255,0,255",	"EMPTY" },
			{ "GRAY",	-6908266,	false,	false,	0,	0,	4,	ColorPrism.class,	"0,255,0,255,0,255",	"EMPTY" },
		};

		channels	= new Channels ();
		for (int i = 0; i < CHANNELS.length; i++)
		{
			Object[]	c = CHANNELS[i];
			Channel		ch = new Channel ((String) c[0], new java.awt.Color ((Integer) c[1], true), i);

			ch.segmented	= (Boolean) c[2];
			ch.blobbed		= (Boolean) c[3];
			ch.threshold	= (Integer) c[4];
			ch.gap			= (Integer) c[5];
			ch.minpix		= (Integer) c[6];
			ch.clustype		= ((Class<?>) c[7]).getName ();
			ch.params		= (String) c[8];
			ch.seeds		= (String) c[9];
			ch.setClusterParameters ();					// the cluster from its class, parameters and seeds (as after loading)
			channels.channels.add (ch);
		}
	}
		
//	public void loadFromFile (String filename)
//	{
//		try { fromJsonFile (filename); } catch (Exception e) { e.printStackTrace(); }		
//		
//		// Update channel seeds (after loading)
//		for (Channel channel : channels.channels)
//			channel.setClusterParameters ();
//	}
//	
//	public void saveToFile (String filename)
//	{	
//		// Update channel seeds (before saving)
//		for (Channel channel : channels.channels)
//			channel.getClusterParameters ();
//
//		try { toJsonFile (filename); } catch (Exception e) { e.printStackTrace(); }				
//	}
}
