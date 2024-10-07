/**
 * Created 22-sep-2009
 * 
 * @author Humberto Martinez Barbera
 */
package wucore.utils.color;

import java.awt.*;

public interface ColorScale 
{
    /**
      All color scales return a color corresponding to a double between
      0.0 and 1.0; if the double is out of bounds, the color corresponding
      to the nearest endpoint of the scale should be returned.
      */
    public Color color (double x);
}
