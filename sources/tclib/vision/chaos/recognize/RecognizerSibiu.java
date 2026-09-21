/**
 * Created on 08-nov-2005
 *
 * @author Humberto Martinez Barbera
 */
package tclib.vision.chaos.recognize;

import javax.swing.*;

import dasboot.data.*;
import tclib.vision.chaos.blobs.*;
import tclib.vision.chaos.channels.*;
import tclib.vision.chaos.gui.*;
import tclib.vision.chaos.recognize.RecognizedObject.RecognizedType;

public class RecognizerSibiu extends Recognizer
{
	static public final int					CHAN_RED		= 2;
	static public final int					CHAN_BLUE	= 3;
	static public final int					CHAN_GREEN	= 4;
	static public final int					CHAN_YELLOW	= 5;

	public int 								mineSxMin		= 4;	
	public int 								mineSyMin		= 4;		
	public int 								mineDensity		= 5;	
		
	public RecognizerSibiu ()
	{
		
	}

	public JPanel configPanel ()					{ return new CPRecogSibiuConfig (this); }

	public void set (JsonData data)
	{
		RecognizerSibiu other = (RecognizerSibiu) data;
		
		mineSxMin	= other.mineSxMin;
		mineSyMin	= other.mineSyMin;
		mineDensity	= other.mineDensity;
	}
	
	public void process (Blobs[] blobs, Channels channels)
	{
		objects.clear ();
				
		if (blobs == null)				return;
		
		recognizeMine (blobs[CHAN_GREEN], channels.at (CHAN_GREEN));
		recognizeMine (blobs[CHAN_YELLOW], channels.at (CHAN_YELLOW));
		recognizeMine (blobs[CHAN_BLUE], channels.at (CHAN_BLUE));
	}
		
	protected void recognizeMine (Blobs blobs, Channel channel)
	{
		for (int i = 0; i < blobs.getBlobNumber (); i++)
		{
			Blob	 blob = blobs.getBlob (i);
			if (testValidMine (blob))
				objects.add (new RecognizedObject (blob, RecognizedType.MINE, channel.color));
		}	
	}

	protected boolean testValidMine (Blob blob)
	{
		if ((blob.getSizeX () < mineSxMin) || (blob.getSizeY () < mineSyMin))
			return false;
		if (	blob.getArea () / blob.getNumPixels () > mineDensity)
			return false;
		
		return true;
	}
	
	public void loadFromFile (String filename)
	{
		try { fromJsonFile (filename); } catch (Exception e) { e.printStackTrace(); }		
	}
	
	public void saveToFile (String filename)
	{	
		try { toJsonFile (filename); } catch (Exception e) { e.printStackTrace(); }				
	}
}
