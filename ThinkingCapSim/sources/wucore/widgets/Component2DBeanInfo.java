/** 
 * Component2DBeanInfo.java
 *
 * Description:		BeanInfo for class Component2D
 * @author			Humberto Martinez Barbera
 * @version			3.0
 */

package wucore.widgets;

/**
 * Component2DBeanInfo just gives the Component2D bean its icons.
 *
 * @see Component2D
 */
public class Component2DBeanInfo extends java.beans.SimpleBeanInfo 
{
	private final Class				beanClass		= Component2D.class;

	public Component2DBeanInfo ()
	{
    }
	
    public java.awt.Image getIcon(int iconKind) 
    {
		java.awt.Image icon = null;
		switch (iconKind) 
		{
		case ICON_COLOR_16x16:
    		icon = loadImage ("icons/comp2d16.gif");
			break;			
		case ICON_COLOR_32x32:
			icon = loadImage ("icons/comp2d32.gif");
			break;
		default:
			break;
		}
		return icon;
	}
}

/* @(#)Component2DBeanInfo.java */
