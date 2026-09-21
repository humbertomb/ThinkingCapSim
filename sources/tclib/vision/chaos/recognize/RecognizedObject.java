/**
 * Created on 20-dec-2018
 *
 * @author Humberto Martinez Barbera
 */
package tclib.vision.chaos.recognize;

import java.util.*;
import java.awt.*;

import tclib.vision.chaos.blobs.*;

public class RecognizedObject 
{
	public enum RecognizedType					{ BLOB, BALL, PIPE, MINE, ROBOT_TOP };
		
	public RecognizedType			type;
	public Color					color;
	public Blob						blob;
	public double					yaw;
	public ArrayList<Blob>			blobs = new ArrayList<Blob> ();
	public ArrayList<Color>			colors = new ArrayList<Color> ();
	
	public RecognizedObject (Blob blob, RecognizedType type, Color color)
	{
		this.blob	= blob;
		this.type	= type;
		this.color	= color;
	}
	
	public RecognizedObject (Blob blob, RecognizedType type, Color color, double yaw)
	{
		this.blob	= blob;
		this.type	= type;
		this.color	= color;
		this.yaw	= yaw;
	}
}
