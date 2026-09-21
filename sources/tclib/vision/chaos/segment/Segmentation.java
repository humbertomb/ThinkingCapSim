/*
 * Created on 25-jul-2005
 *
 * @author Humberto Martinez Barbera
 */
package tclib.vision.chaos.segment;

import java.awt.*;
import java.awt.image.*;

import tclib.vision.chaos.channels.*;

public abstract class Segmentation
{
	protected BufferedImage		input;
	protected Channels			channels;
	protected int[]				segmented;
	
	static public BufferedImage ycrcbToRgb (BufferedImage iycrcb)
	{
		int				x, y;
		BufferedImage	irgb;
		
		irgb = new BufferedImage (iycrcb.getWidth(), iycrcb.getHeight(), BufferedImage.TYPE_INT_RGB);		
		for (y = 0; y < iycrcb.getHeight(); y++)
			for (x = 0; x < iycrcb.getWidth(); x++)
				irgb.setRGB (x, y, ycrcbToRgb (iycrcb.getRGB (x,y)));
		
		return irgb;
	}

	static public BufferedImage ycrcbToHsv (BufferedImage iycrcb)
	{
		int				x, y;
		BufferedImage	ihsv;
		
		ihsv = new BufferedImage (iycrcb.getWidth(), iycrcb.getHeight(), BufferedImage.TYPE_INT_RGB);		
		for (y = 0; y < iycrcb.getHeight(); y++)
			for (x = 0; x < iycrcb.getWidth(); x++)
				ihsv.setRGB (x, y, ycrcbToHsv (iycrcb.getRGB (x,y)));
		
		return ihsv;
	}

	static public BufferedImage yuvToRgb (BufferedImage iyuv)
	{
		int				x, y;
		BufferedImage	irgb;
		
		irgb = new BufferedImage (iyuv.getWidth(), iyuv.getHeight(), BufferedImage.TYPE_INT_RGB);		
		for (y = 0; y < iyuv.getHeight(); y++)
			for (x = 0; x < iyuv.getWidth(); x++)
				irgb.setRGB (x, y, yuvToRgb (iyuv.getRGB (x,y)));
		
		return irgb;
	}

	static public BufferedImage yuvToHsv (BufferedImage iyuv)
	{
		int				x, y;
		BufferedImage	ihsv;
		
		ihsv = new BufferedImage (iyuv.getWidth(), iyuv.getHeight(), BufferedImage.TYPE_INT_RGB);		
		for (y = 0; y < iyuv.getHeight(); y++)
			for (x = 0; x < iyuv.getWidth(); x++)
				ihsv.setRGB (x, y, yuvToHsv (iyuv.getRGB (x,y)));
		
		return ihsv;
	}

	static public BufferedImage rgbToYcrcb (BufferedImage irgb)
	{
		int				x, y;
		BufferedImage	iycrcb;
		
		iycrcb = new BufferedImage (irgb.getWidth(), irgb.getHeight(), BufferedImage.TYPE_INT_RGB);		
		for (y = 0; y < irgb.getHeight(); y++)
			for (x = 0; x < irgb.getWidth(); x++)
				iycrcb.setRGB (x, y, rgbToYcrcb (irgb.getRGB (x,y)));
		
		return iycrcb;
	}

	static public BufferedImage rgbToYuv (BufferedImage irgb)
	{
		int				x, y;
		BufferedImage	iyuv;
		
		iyuv = new BufferedImage (irgb.getWidth(), irgb.getHeight(), BufferedImage.TYPE_INT_RGB);		
		for (y = 0; y < irgb.getHeight(); y++)
			for (x = 0; x < irgb.getWidth(); x++)
				iyuv.setRGB (x, y, rgbToYuv (irgb.getRGB (x,y)));
		
		return iyuv;
	}

	static public BufferedImage rgbToHsv (BufferedImage irgb)
	{
		int				x, y;
		BufferedImage	ihsv;
		
		ihsv = new BufferedImage (irgb.getWidth(), irgb.getHeight(), BufferedImage.TYPE_INT_RGB);		
		for (y = 0; y < irgb.getHeight(); y++)
			for (x = 0; x < irgb.getWidth(); x++)
				ihsv.setRGB (x, y, rgbToHsv (irgb.getRGB (x,y)));
		
		return ihsv;
	}

	static public BufferedImage hsvToRgb (BufferedImage ihsv)
	{
		int				x, y;
		BufferedImage	irgb;
		
		irgb = new BufferedImage (ihsv.getWidth(), ihsv.getHeight(), BufferedImage.TYPE_INT_RGB);		
		for (y = 0; y < ihsv.getHeight(); y++)
			for (x = 0; x < ihsv.getWidth(); x++)
				irgb.setRGB (x, y, hsvToRgb (ihsv.getRGB (x,y)));
		
		return irgb;
	}

	static public BufferedImage hsvToYcrcb (BufferedImage ihsv)
	{
		int				x, y;
		BufferedImage	iycrcb;
		
		iycrcb = new BufferedImage (ihsv.getWidth(), ihsv.getHeight(), BufferedImage.TYPE_INT_RGB);		
		for (y = 0; y < ihsv.getHeight(); y++)
			for (x = 0; x < ihsv.getWidth(); x++)
				iycrcb.setRGB (x, y, hsvToYcrcb (ihsv.getRGB (x,y)));
		
		return iycrcb;
	}

	static public BufferedImage hsvToYuv (BufferedImage ihsv)
	{
		int				x, y;
		BufferedImage	iyuv;
		
		iyuv = new BufferedImage (ihsv.getWidth(), ihsv.getHeight(), BufferedImage.TYPE_INT_RGB);		
		for (y = 0; y < ihsv.getHeight(); y++)
			for (x = 0; x < ihsv.getWidth(); x++)
				iyuv.setRGB (x, y, hsvToYuv (ihsv.getRGB (x,y)));
		
		return iyuv;
	}

	static public BufferedImage filterComponent1 (BufferedImage input)
	{
		int				x, y;
		int				ch1;
		BufferedImage	output;
		
		output = new BufferedImage (input.getWidth(), input.getHeight(), BufferedImage.TYPE_INT_RGB);		
		for (y = 0; y < input.getHeight(); y++)
			for (x = 0; x < input.getWidth(); x++)
			{
				ch1 = Pixel.getComponent0 (input.getRGB (x,y));
				output.setRGB (x, y, Pixel.mergeComponents (ch1, ch1, ch1));
			}
		
		return output;
	}

	static public BufferedImage filterComponent2 (BufferedImage input)
	{
		int				x, y;
		int				ch2;
		BufferedImage	output;
		
		output = new BufferedImage (input.getWidth(), input.getHeight(), BufferedImage.TYPE_INT_RGB);		
		for (y = 0; y < input.getHeight(); y++)
			for (x = 0; x < input.getWidth(); x++)
			{
				ch2 = Pixel.getComponent1 (input.getRGB (x,y));
				output.setRGB (x, y, Pixel.mergeComponents (ch2, ch2, ch2));
			}
		
		return output;
	}

	static public BufferedImage filterComponent3 (BufferedImage input)
	{
		int				x, y;
		int				ch3;
		BufferedImage	output;
		
		output = new BufferedImage (input.getWidth(), input.getHeight(), BufferedImage.TYPE_INT_RGB);		
		for (y = 0; y < input.getHeight(); y++)
			for (x = 0; x < input.getWidth(); x++)
			{
				ch3 = Pixel.getComponent2 (input.getRGB (x,y));
				output.setRGB (x, y, Pixel.mergeComponents (ch3, ch3, ch3));
			}
		
		return output;
	}

	static public BufferedImage quantize (BufferedImage input)
	{
		int				x, y;
		int				ch1, ch2, ch3;
		int				bits = 5;
		int				sh1, sh2, sh3;
		BufferedImage	output;
		
		sh1	= 8 - bits;
		sh2	= 8 - bits;
		sh3	= 8 - bits;
		
		output = new BufferedImage (input.getWidth(), input.getHeight(), BufferedImage.TYPE_INT_RGB);		
		for (y = 0; y < input.getHeight(); y++)
			for (x = 0; x < input.getWidth(); x++)
			{
				ch1 = (Pixel.getComponent0 (input.getRGB (x,y)) >> sh1) << sh1;
				ch2 = (Pixel.getComponent1 (input.getRGB (x,y)) >> sh2) << sh2;
				ch3 = (Pixel.getComponent2 (input.getRGB (x,y)) >> sh3) << sh3;
				output.setRGB (x, y, Pixel.mergeComponents (ch1, ch2, ch3));
			}
		
		return output;
	}

	/*
Y = ( (  66 * R + 129 * G +  25 * B + 128) >> 8) +  16
U = ( ( -38 * R -  74 * G + 112 * B + 128) >> 8) + 128
V = ( ( 112 * R -  94 * G -  18 * B + 128) >> 8) + 128


These formulas produce 8-bit results using coefficients that require no more than 8 bits of (unsigned) precision. Intermediate results require up to 16 bits of precision.
Converting 8-bit YUV to RGB888
The following coefficients are used in conversion process:
C = Y - 16
D = U - 128
E = V - 128
Using the previous coefficients and noting that clip() denotes clipping a value to the range of 0 to 255, the following formulas provide the conversion from YUV to RGB:
R = clip(( 298 * C           + 409 * E + 128) >> 8)
G = clip(( 298 * C - 100 * D - 208 * E + 128) >> 8)
B = clip(( 298 * C + 516 * D           + 128) >> 8)

	 */
	static public int rgbToHsv (int r, int g, int b)
	{
		return rgbToHsv (Pixel.mergeComponents (r, g, b));
	}
	
	static public int rgbToHsv__ (int rgb)
	{
		double		r, g, b;
		double 		u, x, f;
		int			h, s, v;
		int			i;
		
		r	= Pixel.getR (rgb) / 255.0;
		g	= Pixel.getG (rgb) / 255.0;
		b	= Pixel.getB (rgb) / 255.0;

//		 RGB are each on [0, 1]. S and V are returned on [0, 1] and H is
//		 returned on [0, 6]. Exception: H is returned UNDEFINED if S==0.

		x	= Math.min (Math.min (r, g), b);
		u	= Math.max (Math.max (r, g), b);
		v	= (int) (u * 255.0);

		if (u == x)
			return Pixel.mergeComponents (0, 0, v);
		
		f = (r == x) ? g - b : ((g == x) ? b - r : r - g);
		i = (r == x) ? 3 : ((g == x) ? 5 : 1);
		
		h	= (int) ((i - f /(u - x)) / 6.0 * 255.0);
		s	= (int) (((u - x)/u) * 255.0);

		if (h < 0)		h += 255;
		if (h > 255)		h -= 255;
		
		// Clip the resulting values
        h	= Math.max (Math.min (h, 255), 0);
        s	= Math.max (Math.min (s, 255), 0);

		return Pixel.mergeComponents (h, s, v);
	}

	static public int rgbToHsv (int rgb)
	{
		int			r, g, b;
		int			h, s, v;
		int			mn, mx;
		int			maxVal, delta;
	
		r	= Pixel.getR (rgb);
		g	= Pixel.getG (rgb);
		b	= Pixel.getB (rgb);
		mn	= mx	 = r;
		maxVal = 0;
		
		if (g > mx) { mx = g; maxVal = 1; }
		if (b > mx) { mx = b;	 maxVal = 2; }
		if (g < mn) { mn = g; }
		if (b < mn) { mn = b; }
		
		if (mx != 0)
		{
			delta = mx - mn;		
			s = (delta << 10) / mx;						// s = (delta * 1024) / mx
			if (s == 0)
				return Pixel.mergeComponents (0, 0, mx);
		}
		else
			return Pixel.mergeComponents (0, 0, mx);
		
		// This seems to be faster than a switch. Is it?
  		if (maxVal == 0)
			h = ((g - b) << 10) / delta;					// ((g - b) * 1024) / delta)
		else if (maxVal == 2)
			h = 4096 + (((r - g) << 10) / delta);			// (4 * 1024) + (((r - g) * 1024) / delta)
		else // maxVal == 1
			h = 2048 + (((b - r) << 10) / delta);			// (2 * 1024) + (((b - r) * 1024) / delta)

		if (h < 0)
			h += 6144;									// h += (6 * 1024)
			
		h	= (h * 43520) >> 20;							// (h * (42.5 * 1024)) / 1024 / 1024
		s	= (((s << 8) - s) >> 10);						// (s * 255) / 1024	->	((s * 255) >> 10)
		v	= mx;
		return Pixel.mergeComponents (h, s, v);
	}

	static public int hsvToRgb (int hsv)
	{
		int			r, g, b;
		int			h, s, v;
		double		dr, dg, db;
		double		dh, ds, dv;
		int			vari;
		double		var1, var2, var3;
	
		h	= Pixel.getH (hsv);
		s	= Pixel.getS (hsv);
		v	= Pixel.getV (hsv);
		
		if (s == 0)
			return Pixel.mergeComponents (v, v, v);
		else
		{
			dh	= h  * 6.0 / 255.0;
			ds	= s / 255.0;
			dv	= v / 255.0;
			
		   if (dh == 6.0 ) dh = 0.0;      //H must be < 1
		   
			vari = (int) Math.floor (dh);
		   var1 = dv * ( 1 - ds );
		   var2 = dv * ( 1 - ds * ( dh - vari ) );
		   var3 = dv * ( 1 - ds * ( 1 - ( dh - vari ) ) );

		   if      ( vari == 0 ) { dr = dv     ; dg = var3 ; db = var1; }
		   else if ( vari == 1 ) { dr = var2 ; dg = dv     ; db = var1; }
		   else if ( vari == 2 ) { dr = var1 ; dg = dv     ; db = var3; }
		   else if ( vari == 3 ) { dr = var1 ; dg = var2 ; db = dv;     }
		   else if ( vari == 4 ) { dr = var3 ; dg = var1 ; db = dv;     }
		   else                   { dr = dv     ; dg = var1 ; db = var2; }

		   r = (int) (dr * 255.0);
		   g = (int) (dg * 255.0);
		   b = (int) (db * 255.0);
		}
		
		return Pixel.mergeComponents (r, g, b);
	}

	static public int hsvToYuv (int hsv)
	{
		return rgbToYuv (hsvToRgb (hsv));
	}
	
	static public int hsvToYcrcb (int hsv)
	{
		return rgbToYcrcb (hsvToRgb (hsv));
	}
	
	static public int yuvToRgb (int y, int u, int v)
	{
		return yuvToRgb (Pixel.mergeComponents (y, u, v));
	}
	
	static public int yuvToRgb (int yuv)
	{
		int			y, u, v;
		int			r, g, b;
		
		y	= Pixel.getY (yuv);								//    0 <=  y <= 255
		u	= (Pixel.getCr (yuv) - 128) << 1;					// -255 <= cr <= 255
		v	= (Pixel.getCb (yuv) - 128) << 1;					// -255 <= cb <= 255
		                 
	    r	= y + u;
	    g	= (100*y - 51*u - 19*v) / 100;	
	    b	= y + v;

        // Clip the resulting values
        r	= Math.max (Math.min (r, 255), 0);
        g	= Math.max (Math.min (g, 255), 0);
        b	= Math.max (Math.min (b, 255), 0);
       
		return Pixel.mergeComponents (r, g, b);
	}

	static public int yuvToHsv (int yuv)
	{
		return rgbToHsv (yuvToRgb (yuv));
	}
	
	static public int rgbToYuv (int rgb)
	{
		int			y, u, v;
		int			r, g, b;
		
  		r	= Pixel.getR (rgb);
		g	= Pixel.getG (rgb);
		b	= Pixel.getB (rgb);
			
		y	= (int) (0.257 * r + 0.504 * g + 0.098 * b) + 16;
		u	= (int) (-0.148 * r - 0.291 * g + 0.439 * b) + 128;
		v	= (int) (0.439 * r - 0.368 * g - 0.071 * b) + 128;
		
		// Clip the resulting values
        y	= Math.max (Math.min (y, 255), 0);
        u	= Math.max (Math.min (u, 255), 0);
        v	= Math.max (Math.min (v, 255), 0);

		return Pixel.mergeComponents (y, u, v);
	}

	static public int ycrcbToRgb (int ycrcb)
	{
		int			y, cr, cb;
		int			r, g, b;
		
  		y	= Pixel.getY (ycrcb);	
		cr	= Pixel.getCr (ycrcb) - 128;	
		cb	= Pixel.getCb (ycrcb) - 128;
		               
        r	= y + (1371 * cr) / 1000;
        g	= y - (698 * cr + 336 * cb) / 1000;
        b	= y + (1732 * cb) / 1000;

        // Clip the resulting values
        r	= Math.max (Math.min (r, 255), 0);
        g	= Math.max (Math.min (g, 255), 0);
        b	= Math.max (Math.min (b, 255), 0);
       
		return Pixel.mergeComponents (r, g, b);
	}

	static public int ycrcbToHsv (int ycrcb)
	{
		return rgbToHsv (ycrcbToRgb (ycrcb));
	}
	
	static public int rgbToYcrcb (int rgb)
	{
		int			y, u, v;
		int			r, g, b;
		
  		r	= Pixel.getR (rgb);
		g	= Pixel.getG (rgb);
		b	= Pixel.getB (rgb);
			
		y	= (int) (0.299 * r + 0.587 * g + 0.144 * b) + 16;
		u	= (int) (0.511 * r -0.428 * g -0.083 * b) + 128;
		v	= (int) (-0.172 * r -0.339 * g +0.511 * b) + 128;

		// Clip the resulting values
        y	= Math.max (Math.min (y, 255), 0);
        u	= Math.max (Math.min (u, 255), 0);
        v	= Math.max (Math.min (v, 255), 0);

		return Pixel.mergeComponents (y, u, v);
	}

	public final Channels	getChannels ()	{ return channels; }
	public final int[]		getSegmented ()	{ return segmented; }
	public final int			getWidth ()		{ return (input != null ? input.getWidth () : 0); }
	public final int			getHeight ()		{ return (input != null ? input.getHeight () : 0); }
	
	public abstract void process (BufferedImage input, LUT lut, Channels chs);
	public abstract boolean configurable ();
	public void configureDialog () { }
	
	public BufferedImage getSegmentedImage ()
	{		
		int				xx, yy;
		int				pixel;
		BufferedImage	output;
				
		if (input == null)			return null;
		
		output	= new BufferedImage(input.getWidth(), input.getHeight(), BufferedImage.TYPE_INT_RGB);
		for (yy = 0; yy < input.getHeight(); yy++)
			for (xx = 0; xx < input.getWidth(); xx++)
			{
				pixel = (yy * input.getWidth()) + xx;		
				if (segmented[pixel] == Channels.NO_COLOR)
					output.setRGB (xx, yy, Color.GRAY.getRGB());
				else
					output.setRGB (xx, yy, channels.getChannelID (segmented[pixel]).color.getRGB ());
			}
				
		return output;
	}
}
