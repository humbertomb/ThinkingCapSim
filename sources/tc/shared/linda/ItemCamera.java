/* 
	Title:			Thinking Cap 
	Author:			Humberto Martinez Barbera
	Description:	BGA Architecture Support.
*/

package tc.shared.linda;

import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;

import javax.imageio.ImageIO;

/**
 * A frame of one of the cameras of a robot: the image as it was taken, and
 * which camera of the robot took it.
 *
 * On a real robot the image comes from the camera itself; on a simulated one it
 * is rendered out of the 3D model of the world, from where the camera is
 * mounted. Either way what travels is the picture, not what is in it: making
 * something of it is the work of whoever perceives.
 *
 * A {@link BufferedImage} is not serializable, so the image itself is
 * transient and goes over a connection encoded as {@value #FORMAT} -- lossless,
 * so what is read back is pixel for pixel what was taken.
 */
public class ItemCamera extends Item implements Serializable
{
	private static final long		serialVersionUID = 1L;

	/** How the image is encoded to go over a connection. */
	static public final String		FORMAT		= "png";

	public transient BufferedImage	image;					// the frame itself (null: none taken yet)
	public int						device;					// which camera of the robot took it

	// Constructors
	public ItemCamera () 
	{
		this.set (null, 0, 0);
	}	
	
	public ItemCamera (BufferedImage image) 
	{
		this.set (image, 0, 0);
	}	
	
	public ItemCamera (BufferedImage image, int device, long tstamp) 
	{
		this.set (image, device, tstamp);
	}	
	
	// Initialisers
	public void set (BufferedImage image, int device, long tstamp)
	{
		set (tstamp);
		
		this.image	= image;
		this.device	= device;
	}

	public int	width ()		{ return (image != null) ? image.getWidth () : 0; }
	public int	height ()		{ return (image != null) ? image.getHeight () : 0; }

	/**
	 * A copy of its own, image included: whoever writes one of these goes on
	 * drawing on its own buffer, so a reader given the same one would see it
	 * change underneath.
	 */
	public Item dup ()
	{
		ItemCamera		item = new ItemCamera (copy (image), device, timestamp.longValue ());
		
		return item;
	}

	/** A copy of an image, of the same kind and size. */
	static public BufferedImage copy (BufferedImage src)
	{
		BufferedImage	dst;
		
		if (src == null)		return null;
		dst		= new BufferedImage (src.getWidth (), src.getHeight (), src.getType ());
		dst.getGraphics ().drawImage (src, 0, 0, null);
		return dst;
	}
	
	// Serialisation of what a BufferedImage cannot do by itself
	private void writeObject (ObjectOutputStream out) throws IOException
	{
		ByteArrayOutputStream	bytes;
		
		out.defaultWriteObject ();
		if (image == null)		{ out.writeInt (0); return; }
		
		bytes	= new ByteArrayOutputStream ();
		ImageIO.write (image, FORMAT, bytes);
		out.writeInt (bytes.size ());
		out.write (bytes.toByteArray ());
	}
	
	private void readObject (ObjectInputStream in) throws IOException, ClassNotFoundException
	{
		int			n;
		byte[]		bytes;
		
		in.defaultReadObject ();
		n		= in.readInt ();
		if (n <= 0)		{ image = null; return; }
		
		bytes	= new byte[n];
		in.readFully (bytes);
		image	= ImageIO.read (new ByteArrayInputStream (bytes));
	}
	
	public String toString ()
	{
		return "ItemCamera[dev="+device+" "+width ()+"x"+height ()+"]";
	}	
}
