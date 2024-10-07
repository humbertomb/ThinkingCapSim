/** 
 * GaugeBeanInfo.java
 *
 * Description:		BeanInfo for class Plotter
 * @author			Humberto Martinez Barbera
 * @version			1.1
 */

package wucore.widgets;

/**
 * GaugeBeanInfo just gives the Gauge bean its icons.
 *
 * @see Gauge
 */
public class PlotterBeanInfo extends java.beans.SimpleBeanInfo 
{
	private final Class				beanClass		= Plotter.class;

	public PlotterBeanInfo ()
	{
    }
	
    public java.awt.Image getIcon(int iconKind) 
    {
		java.awt.Image icon = null;
		switch (iconKind) 
		{
		case ICON_COLOR_16x16:
    		icon = loadImage ("icons/plotter16.gif");
			break;			
		case ICON_COLOR_32x32:
			icon = loadImage ("icons/plotter32.gif");
			break;
		default:
			break;
		}
		return icon;
	}
}

/* @(#)PlotterBeanInfo.java */
