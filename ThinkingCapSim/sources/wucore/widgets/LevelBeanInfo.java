/** 
 * LevelBeanInfo.java
 *
 * Description:		BeanInfo for class Level
 * @author			Humberto Martinez Barbera
 * @version			1.1
 */

package wucore.widgets;

/**
 * LevelBeanInfo just gives the Level bean its icons.
 *
 * @see Level
 */
public class LevelBeanInfo extends GaugeBeanInfo 
{
	private final Class beanClass = Level.class;	 

    public java.awt.Image getIcon(int iconKind) {
		java.awt.Image icon = null;
		switch (iconKind) {
		case ICON_COLOR_16x16:
    		icon = loadImage("icons/level16.gif");
			break;			
		case ICON_COLOR_32x32:
			icon = loadImage("icons/level32.gif");
			break;
		default:
			break;
		}
		return icon;
	}
}

/* @(#)LevelBeanInfo.java */
