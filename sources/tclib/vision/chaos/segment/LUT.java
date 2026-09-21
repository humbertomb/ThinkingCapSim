/**
 * Created on 15-may-2006
 *
 * @author Humberto Martinez Barbera
 */
package tclib.vision.chaos.segment;

import tclib.vision.chaos.channels.*;

public abstract class LUT 
{
	static protected int 		BITS_R		= 7;
	static protected int 		BITS_G		= 7;
	static protected int 		BITS_B		= 7;
	
	static protected int			NO_COLOR		= -1;

	protected int[][][]			lut;
	protected int				shiftsR, sizeR;
	protected int				shiftsG, sizeG;
	protected int				shiftsB, sizeB;

	public final int lookup (int rgb)
	{
		int				r, g, b;

		r	= Pixel.getR (rgb);
		g	= Pixel.getG (rgb);
		b	= Pixel.getB (rgb);

		return lut[r>>shiftsR][g>>shiftsG][b>>shiftsB];
	}
	
	public final int lookup (int r, int g, int b)
	{
		return lut[r>>shiftsR][g>>shiftsG][b>>shiftsB];
	}
	
	public abstract void initialise (Channels chs);
	public abstract void update (Channels chs, int ch);
	public abstract boolean configurable ();
	public void configureDialog (Channels chs) { }

}
