/**
 * Created 22-sep-2009
 * 
 * @author Humberto Martinez Barbera
 */
package wucore.utils.color;

import java.awt.*;

public class RainbowScale implements ColorScale
{
	public Color color (double x) 
	{
		int r, g, b;

		if (x<.25) 
		{
			r=255;
			g=(int)(1020.0*x);
			b=0;
		}
		else if (x<0.5) 
		{
			r=(int)(510.0-1020.0*x);
			g=255;
			b=0;
		}
		else if (x<.75)
		{
			r=0;
			g=(int)(765.0-1020.0*x);
			b=(int)(1020.0*x-510.0);
		}
		else 
		{
			r=(int)(1020.0*x-765.0);
			g=0;
			b=255;
		}

		if (r<0) r=0;
		if (g<0) g=0;
		if (b<0) b=0;
		if (r>255) r=255;
		if (g>255) g=255;
		if (b>255) b=255;

		return new Color (r,g,b);
	}
}
