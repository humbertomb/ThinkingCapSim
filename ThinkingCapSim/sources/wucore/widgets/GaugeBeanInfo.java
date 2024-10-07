/** 
 * GaugeBeanInfo.java
 *
 * Description:		BeanInfo for class Gauge
 * @author			Humberto Martinez Barbera
 * @version			1.1
 */

package wucore.widgets;

/**
 * GaugeBeanInfo just gives the Gauge bean its icons.
 *
 * @see Gauge
 */
public class GaugeBeanInfo extends java.beans.SimpleBeanInfo 
{
	private final Class				beanClass		= Gauge.class;

	public GaugeBeanInfo ()
	{
    }
	
    public java.awt.Image getIcon(int iconKind) 
    {
		java.awt.Image icon = null;
		switch (iconKind) 
		{
		case ICON_COLOR_16x16:
    		icon = loadImage ("icons/gauge16.gif");
			break;			
		case ICON_COLOR_32x32:
			icon = loadImage ("icons/gauge32.gif");
			break;
		default:
			break;
		}
		return icon;
	}
}

/* @(#)GaugeBeanInfo.java */
