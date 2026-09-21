/**
 * Created 10-dec-2018
 * 
 * @author Humberto Martinez Barbera
 */
package tcrob.umu.quaky2;

import java.awt.Color;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;

import com.google.gson.*;
import com.google.gson.stream.*;

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
			{ "RED",	-65536,		true,	true,	0,	0,	4,	ColorPrism.class,	"0,255,0,255,0,255",	"EMPTY" },
			{ "BLUE",	-16776961,	true,	true,	5,	3,	4,	ColorPrism.class,	"0,255,0,255,0,255",	"EMPTY" },
			{ "GREEN",	-16711936,	false,	false,	5,	0,	4,	ColorPrism.class,	"0,255,0,255,0,255",	"EMPTY" }
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
		
	/* ------------------------------------------------------------------ */
	/* Files (.chaos, JSON)                                                */
	/* ------------------------------------------------------------------ */

	/** Extension of the files of a vision configuration. */
	static public final String			EXTENSION		= ".chaos";
	/** Where they are kept. */
	static public final String			DIRECTORY		= "./conf/vision";

	/**
	 * Reads the configuration from a file: its channels (and the cluster of each,
	 * from its class, parameters and seeds) and the methods of the LUT, the
	 * segmentation and the blob forming. Files written by DasBoot are read too.
	 */
	public void loadFromFilename (String filename) throws IOException
	{
		SoccerVisionConfig	c;
		JsonElement			json;

		try { json = JsonParser.parseString (new String (Files.readAllBytes (Paths.get (filename)), StandardCharsets.UTF_8)); }
		catch (JsonParseException e)		{ throw new IOException ("<" + filename + "> is not a vision configuration: " + e.getMessage (), e); }
		// a file with no channels of its own is not one (the defaults would be taken for it)
		if (!json.isJsonObject () || !json.getAsJsonObject ().has ("channels"))
			throw new IOException ("<" + filename + "> is not a vision configuration: it has no channels");
		try { c = gson ().fromJson (json, SoccerVisionConfig.class); }
		catch (JsonParseException e)		{ throw new IOException ("<" + filename + "> is not a vision configuration: " + e.getMessage (), e); }
		if ((c == null) || (c.channels == null) || (c.channels.channels == null))
			throw new IOException ("<" + filename + "> has no channels");

		channels	= c.channels;
		lutmode		= mode (c.lutmode, LUTMODES);
		segmode		= mode (c.segmode, SEGMODES);
		blobmode	= mode (c.blobmode, BLOBMODES);
		recogclass	= c.recogclass;

		// the cluster of each channel, from what the file says of it
		for (int i = 0; i < channels.size (); i++)
		{
			Channel		ch = channels.at (i);

			if ((ch.clustype == null) || (ch.clustype.trim ().length () == 0))		ch.clustype = ColorPrism.class.getName ();
			if (ch.seeds == null)							ch.seeds	= "EMPTY";
			if (ch.color == null)							ch.color	= Color.GRAY;
			ch.setClusterParameters ();
		}
	}

	/** Writes the configuration to a file, with the parameters and seeds each channel has now. */
	public void saveToFilename (String filename) throws IOException
	{
		for (int i = 0; i < channels.size (); i++)
			channels.at (i).getClusterParameters ();
		Files.write (Paths.get (filename), (gson ().toJson (this) + "\n").getBytes (StandardCharsets.UTF_8));
	}

	/** A method index read from a file, kept within the ones there are. */
	static protected int mode (int m, String[] modes)
	{
		return ((m >= 0) && (m < modes.length)) ? m : 0;
	}

	/** The JSON of a configuration: indented, and a colour as its ARGB value ({"value": ...}, as DasBoot wrote it). */
	static protected Gson gson ()
	{
		return new GsonBuilder ().setPrettyPrinting ().disableHtmlEscaping ()
				.registerTypeHierarchyAdapter (Color.class, new TypeAdapter<Color> ()
				{
					public void write (JsonWriter out, Color c) throws IOException
					{
						if (c == null)		{ out.nullValue (); return; }
						out.beginObject ().name ("value").value (c.getRGB ()).endObject ();
					}

					public Color read (JsonReader in) throws IOException
					{
						int		argb = 0xFF808080;

						switch (in.peek ())
						{
						case NULL:		in.nextNull ();			return null;
						case NUMBER:	return new Color (in.nextInt (), true);
						default:
							in.beginObject ();
							while (in.hasNext ())
								if ("value".equals (in.nextName ()))		argb = in.nextInt ();
								else										in.skipValue ();
							in.endObject ();
							return new Color (argb, true);
						}
					}
				}).create ();
	}
}
