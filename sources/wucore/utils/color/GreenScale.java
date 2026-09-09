/**
 * Created 22-sep-2009
 * 
 * @author Humberto Martinez Barbera
 */
package wucore.utils.color;

import java.awt.*;

public class GreenScale implements ColorScale
{
	public Color color (double x) 
	{
	    if (x <= 0.0) return Color.black;
	    if (x >= 1.0) return Color.green;
	    return new Color (0, (int) (x * 255.0), 0);
	}
}
