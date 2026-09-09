/**
 * Created 22-sep-2009
 * 
 * @author Humberto Martinez Barbera
 */
package wucore.utils.color;

import java.awt.*;

public class RedScale implements ColorScale
{
	public Color color(double x) 
	{
	    if (x <= 0.0) return Color.black;
	    if (x >= 1.0) return Color.red;
	    return new Color ((int) (x * 255.0), 0, 0);
	}
}
