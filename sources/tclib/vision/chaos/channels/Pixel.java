/**
 * Created on 16-nov-2005
 *
 * @author Humberto Martinez Barbera
 */
package tclib.vision.chaos.channels;

public class Pixel 
{
	public int co1;
	public int co2;
	public int co3;
	
	static public int byteToInt (byte value)
	{
		if (value >= 0)
			return (int) value;
		else
			return 256 + (int) value;
	}

	static public int mergeComponents (int c1, int c2, int c3)
	{
		return c3 + (c2 << 8) + (c1 << 16);
	}	

	static public final int getH (int hsv)					{ return byteToInt ((byte) (hsv >> 16)); }
	static public final int getS (int hsv)					{ return byteToInt ((byte) (hsv >> 8)); }
	static public final int getV (int hsv)					{ return byteToInt ((byte) hsv); }

	static public final int getR (int rgb)					{ return byteToInt ((byte) (rgb >> 16)); }
	static public final int getG (int rgb)					{ return byteToInt ((byte) (rgb >> 8)); }
	static public final int getB (int rgb)					{ return byteToInt ((byte) rgb); }

	static public final int getY (int yuv)					{ return byteToInt ((byte) (yuv >> 16)); }
	static public final int getCr (int yuv)				{ return byteToInt ((byte) (yuv >> 8)); }
	static public final int getCb (int yuv)				{ return byteToInt ((byte) yuv); }

	static public final int getComponent0 (int value)		{ return byteToInt ((byte) (value >> 16)); }
	static public final int getComponent1 (int value)		{ return byteToInt ((byte) (value >> 8)); }
	static public final int getComponent2 (int value)		{ return byteToInt ((byte) value); }
	
	public Pixel (int co1, int co2, int co3)
	{
		this.co1		= co1;
		this.co2		= co2;
		this.co3		= co3;
	}

	public Pixel (int value)
	{
		set (value);
	}

	public void set (int value)
	{
		co1		= getComponent0 (value);
		co2		= getComponent1 (value);
		co3		= getComponent2 (value);
	}

	public int mergeComponents ()
	{
		return mergeComponents (co1, co2, co3);
	}	

	public int hashCode ()
	{
		return mergeComponents (co1, co2, co3);
	}
	
	public boolean equals (Object object)
	{
		if (object instanceof Pixel)
		{
			Pixel		other;
			
			other	= (Pixel) object;
			return (co1 == other.co1) && (co2 == other.co2) && (co3 == other.co3);
		}
		
		return false;
	}

	public String toString ()
	{
		return "["+co1+", "+co2+", "+co3+"]";
	}
}
