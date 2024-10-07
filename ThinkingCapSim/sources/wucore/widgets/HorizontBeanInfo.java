/** 
 * HorizontBeanInfo.java
 *
 * Description:		BeanInfo for class Horizont
 * @author			Humberto Martinez Barbera
 * @version			1.1
 */

package wucore.widgets;

/**
 * HorizontBeanInfo just gives the Horizont bean its icons.
 *
 * @see Horizont
 */
public class HorizontBeanInfo extends GaugeBeanInfo 
{
	private final Class beanClass = Horizont.class;
	 
    public java.awt.Image getIcon(int iconKind) {
		java.awt.Image icon = null;
		switch (iconKind) {
		case ICON_COLOR_16x16:
    		icon = loadImage("icons/horizont16.gif");
			break;			
		case ICON_COLOR_32x32:
			icon = loadImage("icons/horizont32.gif");
			break;
		default:
			break;
		}
		return icon;
	}
}

/* @(#)HorizontBeanInfo.java */
