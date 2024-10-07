/**
 * Created on 04-dic-2006
 *
 * A convenience class that loads Icons for users and provides caching
 * mechanisms.
 * <p>
 *
 * @author Justin Couch
 * @author Humberto Martinez Barbera
 */
package wucore.utils.image;

import java.awt.*;
import java.awt.geom.AffineTransform;
import java.awt.image.*;
import java.io.*;
import java.lang.ref.*;
import java.net.*;
import java.util.*;
import javax.imageio.*;
import javax.swing.*;

import wucore.utils.math.Angles;

public class ImageTools 
{
	/** The default size of the map */
	private static final int DEFAULT_SIZE = 10;

	/** The image toolkit used to load images with */
	private static Toolkit toolkit;

	/**
	 * A hashmap of the loaded image instances. Weak so that we can discard
	 * them if if needed because we're running out of memory.
	 */
	private static HashMap loadedImages;

	/**
	 * A hashmap of the loaded icon instances. Weak so that we can discard
	 * them if if needed because we're running out of memory.
	 */
	private static HashMap loadedIcons;

	/**
	 * Static initialiser to get all the bits set up as needed.
	 */
	static
	{
		toolkit = Toolkit.getDefaultToolkit();
		loadedImages = new HashMap (DEFAULT_SIZE);
		loadedIcons = new HashMap (DEFAULT_SIZE);
	}

	/**
	 * Load an icon for the named image file. Looks in the classpath for the
	 * image so the path provided must be fully qualified relative to the
	 * classpath.
	 *
	 * @param name The path to load the icon for. If not found,
	 *   no image is loaded.
	 * @return An icon for the named path.
	 */
	public static Icon loadIcon (String name)
	{
		// Check the map for an instance first
		Icon ret_val = null;

		WeakReference ref = (WeakReference)loadedIcons.get(name);
		if(ref != null)
		{
			ret_val = (Icon)ref.get();
			if(ret_val == null)
				loadedIcons.remove(name);
		}

		if(ret_val == null)
		{
			Image img = loadImage(name);

			if(img != null)
			{
				ret_val = new ImageIcon(img, name);
				loadedIcons.put(name, new WeakReference(ret_val));
			}
		}

		return ret_val;
	}

	/**
	 * Load an image for the named image file. Looks in the classpath for the
	 * image so the path provided must be fully qualified relative to the
	 * classpath.
	 *
	 * @param name The path to load the icon for. If not found,
	 *   no image is loaded.
	 * @return An image for the named path.
	 */
	public static Image loadImage (String name)
	{
		// Check the map for an instance first
		Image ret_val = null;

		WeakReference ref = (WeakReference) loadedImages.get(name);
		if(ref != null)
		{
			ret_val = (Image)ref.get();
			if(ret_val == null)
				loadedImages.remove (name);
		}

		if(ret_val == null)
		{
			URL url = ClassLoader.getSystemResource(name);

			if(url != null)
			{
				ret_val = toolkit.createImage(url);
				loadedImages.put(name, new WeakReference(ret_val));
			}
		}

		return ret_val;
	}

	static public Image[] loadRotatedImage (String fname)
	{
		BufferedImage 	icon;
		Image[]			image;

		try { icon = ImageIO.read (ClassLoader.getSystemResource (fname)); } catch (Exception e) { e.printStackTrace();  return null; }

		image = new Image[361];
		for (int i = 0; i < 361; i++)
			image[i] = rotateImage (icon, i*Angles.DTOR);

		return image;
	}

	static protected final BufferedImage rotateImage (BufferedImage image, double angle) 
	{
		double sin = Math.abs(Math.sin(angle)), cos = Math.abs(Math.cos(angle));
		int w = image.getWidth(), h = image.getHeight();
		int neww = (int)Math.floor(w*cos+h*sin), newh = (int)Math.floor(h*cos+w*sin);

		BufferedImage source = new BufferedImage (w, h, BufferedImage.TYPE_INT_ARGB);
		Graphics2D g = (Graphics2D) source.getGraphics();
		g.drawImage (image, 0, 0, null);

		AffineTransform xform = new AffineTransform();
		xform.translate ((neww-w)/2, (newh-h)/2);
		xform.rotate (angle, w/2.0, h/2.0);

		BufferedImageOp bio = new AffineTransformOp (xform, AffineTransformOp.TYPE_BILINEAR);
		return bio.filter (source, null);
	}

	static public Cursor loadCursor (String filename)
	{
		Cursor		cursor = Cursor.getDefaultCursor ();
		Image		img = loadImage (filename);

		if (img != null)
		{
			Point center = new Point();
			center.x = img.getWidth(null) / 2;
			center.y = img.getHeight(null) / 2;

			cursor = toolkit.createCustomCursor (img, center , filename);
		}

		return cursor;
	}
	
	static public void saveImage (String filename, String type, String output)
	{
		BufferedImage image = toBufferedImage (loadImage (filename));
		try { ImageIO.write (image, type, new File (output)); } catch (Exception e) { e.printStackTrace(); }
	}
	
	static public boolean hasAlpha (Image image) 
	{
		// If buffered image, the color model is readily available
		if (image instanceof BufferedImage) 
		{
			BufferedImage bimage = (BufferedImage)image;
			return bimage.getColorModel().hasAlpha();
		}

		// Use a pixel grabber to retrieve the image's color model;
		// grabbing a single pixel is usually sufficient
		PixelGrabber pg = new PixelGrabber(image, 0, 0, 1, 1, false);
		try { pg.grabPixels(); } catch (InterruptedException e) { e.printStackTrace(); }

		// Get the image's color model
		ColorModel cm = pg.getColorModel();
		return cm.hasAlpha();
	}

	static public BufferedImage toBufferedImage (Image image) 
	{
		if (image instanceof BufferedImage)
			return (BufferedImage) image;

		// This code ensures that all the pixels in the image are loaded
		image = new ImageIcon(image).getImage();

		// Determine if the image has transparent pixels; for this method's
		// implementation, see e661 Determining If an Image Has Transparent Pixels
		boolean hasAlpha = hasAlpha (image);

		// Create a buffered image with a format that's compatible with the screen
		BufferedImage bimage = null;
		GraphicsEnvironment ge = GraphicsEnvironment.getLocalGraphicsEnvironment();
		try 
		{
			// Determine the type of transparency of the new buffered image
			int transparency = Transparency.OPAQUE;
			if (hasAlpha)
				transparency = Transparency.BITMASK;

			// Create the buffered image
			GraphicsDevice gs = ge.getDefaultScreenDevice();
			GraphicsConfiguration gc = gs.getDefaultConfiguration();
			bimage = gc.createCompatibleImage(image.getWidth(null), image.getHeight(null), transparency);
		} catch (HeadlessException e) {  e.printStackTrace(); }

		if (bimage == null) 
		{
			// Create a buffered image using the default color model
			int type = BufferedImage.TYPE_INT_RGB;
			if (hasAlpha)
				type = BufferedImage.TYPE_INT_ARGB;
			bimage = new BufferedImage(image.getWidth(null), image.getHeight(null), type);
		}

		// Copy image to buffered image
		Graphics g = bimage.createGraphics();
		g.drawImage (image, 0, 0, null);
		g.dispose ();

		return bimage;
	}
}
