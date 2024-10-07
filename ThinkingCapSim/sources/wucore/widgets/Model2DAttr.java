/**
 * Created on 15-ago-2004
 *
 * @author Humberto Martinez Barbera
 */
package wucore.widgets;

import java.io.*;
import java.awt.*;

public class Model2DAttr implements Serializable
{
	static public final int		ATTR_POINT		= 0;
	static public final int		ATTR_LINE		= 1;
	static public final int		ATTR_POLY		= 2;
	
	// Visual artifacts
	public Color				color;				// Object color
	public int					attype;				// Object geometry type (point, line, poly, zpoly)
	public int					type;				// Object shape (box, icon, lines, etc)
	public int					mode;				// Object shape modifier (plain, filled, etc)
	public String				label	= null;		// Object label
	public Object				src		= null;		// Object reference (URL, file name, Java object)
	
	// Geometry attributes
	public int					vorig;				// Origin vertex
	public int					vdest;				// Destination vertex
	public int[]				vset	= null;		// Polygon vertex list / Arc parameter list
}
