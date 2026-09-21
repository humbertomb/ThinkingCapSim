/**
 * Created on 30-jan-2019
 *
 * @author Humberto Martinez Barbera
 */
package tclib.vision.chaos.recognize;

import java.util.*;
import java.awt.*;
import java.awt.image.*;

import tclib.vision.chaos.blobs.*;
import tclib.vision.chaos.channels.*;

public abstract class Recognizer
{
	protected transient ArrayList<RecognizedObject>	objects = new ArrayList<RecognizedObject> ();	
	
	public ArrayList<RecognizedObject> getRecgnizedObjects ()				{ return objects; }
	
	public abstract void process (Blobs[] blobs, Channels channels);
//	public abstract void loadFromFile (String filename);
//	public abstract void saveToFile (String filename);
//	public abstract JPanel configPanel ();

	public BufferedImage getRecognizedImage (BufferedImage input)
	{
		BufferedImage		output;
		
		output	= new BufferedImage (input.getWidth(), input.getHeight(), BufferedImage.TYPE_INT_RGB);
		output.setData (input.getData ());
		Graphics2D g2 = output.createGraphics ();

		g2.setStroke (new BasicStroke (8));

		for (RecognizedObject obj : objects)
		{
			int diam = (int) Math.sqrt (obj.blob.getArea ());
			g2.setColor (obj.color);
			g2.drawOval (obj.blob.getX ()-diam/2, obj.blob.getY ()-diam/2, diam, diam);
			
			Iterator<Color> iter = obj.colors.iterator ();			
			for (Blob blob : obj.blobs)
			{
				g2.setColor (iter.next ());
				g2.fillRect (blob.getXMin (), blob.getYMin (), blob.getXMax ()-blob.getXMin (), blob.getYMax ()-blob.getYMin ());
			}
		}
				
		return output;
	}
}
