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
	
	/**
	 * Whether a channel takes some colour of a cell of the table. A cell holds
	 * several colours (2 x 2 x 2 with 7 bits a component), and all of them are
	 * looked at, not only its first corner: a channel of a single colour (one
	 * seed: a prism of no size) would otherwise take a cell only when its colour
	 * happens to be that corner -- never, for (0, 0, 255), whose cell starts at
	 * (0, 0, 254).
	 */
	protected final boolean takes (Channel chan, int r, int g, int b)
	{
		int		r0 = r << shiftsR, g0 = g << shiftsG, b0 = b << shiftsB;

		for (int dr = 0; dr < (1 << shiftsR); dr++)
			for (int dg = 0; dg < (1 << shiftsG); dg++)
				for (int db = 0; db < (1 << shiftsB); db++)
					if (chan.insideChannel (Segmentation.rgbToHsv (Pixel.mergeComponents (r0 + dr, g0 + dg, b0 + db))))
						return true;
		return false;
	}

	/** The first segmented channel that takes some colour of a cell of the table (its index), or NO_COLOR. */
	protected final int labelOf (Channels chs, int r, int g, int b)
	{
		for (int ch = 0; ch < chs.getNumChannels (); ch++)
			if (chs.at (ch).segmented && takes (chs.at (ch), r, g, b))
				return ch;
		return NO_COLOR;
	}

	public abstract void initialise (Channels chs);
	public abstract void update (Channels chs, int ch);
	public abstract boolean configurable ();
	public void configureDialog (Channels chs) { }

}
