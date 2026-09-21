/**
 * Created 10-dec-2018
 * 
 * @author Humberto Martinez Barbera
 */
package tclib.vision.chaos;

import java.awt.image.*;

import tclib.vision.chaos.blobs.*;
import tclib.vision.chaos.recognize.*;
import tclib.vision.chaos.segment.*;

public class ChaosPam 
{
	public ChaosPamConfig				config;
	
	public LUT							lut;
	public Segmentation					segment;
	public BlobForming					blobbing;
	public Recognizer					recognizer;
	
	private String						fileconf;
	private String						filerecog;

	public ChaosPam (String fileconf, String filerecog) 
	{
		this.fileconf	= fileconf;
		this.filerecog	= filerecog;
		
		config = new ChaosPamConfig ();		
		
		loadConf ();
	}

	public void loadConf ()
	{
		config.loadFromFile (fileconf);
		
		instanceLUT ();
		instanceSegment ();
		instanceBlob ();
		instanceRecog ();
	}

	public void saveConf ()
	{
		config.saveToFile (fileconf);
		recognizer.saveToFile (filerecog);
	}
	
	public void instanceLUT ()
	{
		Class<?>		sclass;
		String		pack;
		
		try 
		{
			pack		= LUT.class.getPackage().getName();
			sclass	= Class.forName (pack + "." + ChaosPamConfig.LUTMODES[config.lutmode]);
			lut		= (LUT) sclass.getDeclaredConstructor().newInstance();
			lut.initialise (config.channels);
		} catch (Exception ex) { ex.printStackTrace (); }		
	}

	public void instanceSegment ()
	{
		Class<?>		sclass;
		String		pack;
		
		try 
		{
			pack			= Segmentation.class.getPackage().getName();
			sclass		= Class.forName(pack + "." + ChaosPamConfig.SEGMODES[config.segmode]);
			segment 		= (Segmentation) sclass.getDeclaredConstructor().newInstance();
		} catch (Exception ex) { ex.printStackTrace (); }
	}

	public void instanceBlob ()
	{
		Class<?>		sclass;
		String		pack;
		
		try 
		{
			pack			= BlobForming.class.getPackage().getName();
			sclass		= Class.forName(pack + "." + ChaosPamConfig.BLOBMODES[config.blobmode]);
			blobbing 	= (BlobForming) sclass.getDeclaredConstructor().newInstance();
		} catch (Exception ex) { ex.printStackTrace (); }
	}

	public void instanceRecog ()
	{
		Class<?>		sclass;
		
		try 
		{
			sclass		= Class.forName (config.recogclass);
			recognizer 	= (Recognizer) sclass.getDeclaredConstructor().newInstance();
			recognizer.loadFromFile (filerecog);
		} catch (Exception ex) { ex.printStackTrace (); }
	}

	public BufferedImage recognition (BufferedImage image)
	{
		return recognizer.getRecognizedImage (image);
	}
	
	public void updateImage (BufferedImage image)
	{
		segment.process (image, lut, config.channels);
		blobbing.process (segment);
		blobbing.postProcess ();
		recognizer.process (blobbing.getBlobs (), config.channels);
	}
}
