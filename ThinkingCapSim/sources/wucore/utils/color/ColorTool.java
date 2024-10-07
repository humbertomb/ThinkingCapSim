/**
 * Created on 29-abr-2008
 *
 * @author Humberto Martinez Barbera
 */
package wucore.utils.color;

import java.awt.*;
import java.util.*;

public class ColorTool 
{
	static public final String[]		COL_NAMES		= { "white", "black", 
															"gray", "gray_light", "gray_dark",
															"red", "red_bright", "red_dark",  
															"cyan", "cyan_bright", "cyan_dark",
															"blue", "blue_bright", "blue_dark",  
															"yellow", "yellow_bright", "yellow_dark",
															"orange", "orange_bright", "orange_dark",  
															"green", "green_bright", "green_dark",
															"magenta", "magenta_bright", "magenta_dark"
														};
	static public final WColor[]			COL_VALUES		= { WColor.WHITE, WColor.BLACK, 
															WColor.GRAY, WColor.LIGHT_GRAY, WColor.DARK_GRAY,
															WColor.RED, WColor.RED.brighter(), WColor.RED.darker(),
															WColor.CYAN, WColor.CYAN.brighter(), WColor.CYAN.darker(),
															WColor.BLUE, WColor.BLUE.brighter(), WColor.BLUE.darker(),
															WColor.YELLOW, WColor.YELLOW.brighter(), WColor.YELLOW.darker(),
															WColor.ORANGE, WColor.ORANGE.brighter(), WColor.ORANGE.darker(),
															WColor.GREEN, WColor.GREEN.brighter(), WColor.GREEN.darker(),
															WColor.MAGENTA, WColor.MAGENTA.brighter(), WColor.MAGENTA.darker()
														};
	
	static public WColor getColorFromName (String name)
	{
		int				r, g, b;
		StringTokenizer	st;

		if (name == null)			return WColor.BLACK;

		for (int i = 0; i < COL_NAMES.length; i++)
			if (COL_NAMES[i].equalsIgnoreCase (name))
				return COL_VALUES[i];

		st		= new StringTokenizer (name, ":");
		r		= Integer.parseInt (st.nextToken ());
		g		= Integer.parseInt (st.nextToken ());
		b		= Integer.parseInt (st.nextToken ());

		return new WColor (r, g, b);
	}
	
	static public String getNameFromColor (WColor color)
	{
		for (int i = 0; i < COL_VALUES.length; i++)
			if (COL_VALUES[i].equals (color))
				return COL_NAMES[i];
		
		return color.getRed() + ":" + color.getGreen() + ":" + color.getBlue();
	}
	
	static public WColor fromColorToWColor (Color color)
	{
		return new WColor (color.getRed(), color.getGreen(), color.getBlue());
	}
	
	static public Color fromWColorToColor (WColor wcolor)
	{
		return new Color (wcolor.getRed(), wcolor.getGreen(), wcolor.getBlue());
	}
}
