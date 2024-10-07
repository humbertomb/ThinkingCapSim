/** 
 * CompassBeanInfo.java
 *
 * Description:		BeanInfo for class Compass
 * @author			Humberto Martinez Barbera
 * @version			1.1
 */

package wucore.widgets;

/**
 * CompassBeanInfo just gives the Compass bean its icons.
 *
 * @see Compass
 */
public class CompassBeanInfo extends GaugeBeanInfo 
{
	private final Class beanClass = Compass.class;	 

    public java.awt.Image getIcon(int iconKind) {
		java.awt.Image icon = null;
		switch (iconKind) {
		case ICON_COLOR_16x16:
    		icon = loadImage("icons/compass16.gif");
			break;			
		case ICON_COLOR_32x32:
			icon = loadImage("icons/compass32.gif");
			break;
		default:
			break;
		}
		return icon;
	}
}

/* @(#)CompassBeanInfo.java */
