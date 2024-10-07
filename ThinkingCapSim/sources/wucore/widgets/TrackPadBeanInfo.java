/** 
 * TrackPadBeanInfo.java
 *
 * Description:		BeanInfo for class TrackPad
 * @author			Humberto Martinez Barbera
 * @version			1.1
 */

package wucore.widgets;

/**
 * TrackPadBeanInfo just gives the TrackPad bean its icons.
 *
 * @see TrackPad
 */
public class TrackPadBeanInfo extends java.beans.SimpleBeanInfo 
{
	private final Class				beanClass		= TrackPad.class;

	public TrackPadBeanInfo ()
	{
    }
	
    public java.awt.Image getIcon(int iconKind) 
    {
		java.awt.Image icon = null;
		switch (iconKind) 
		{
		case ICON_COLOR_16x16:
    		icon = loadImage ("icons/trackpad16.gif");
			break;			
		case ICON_COLOR_32x32:
			icon = loadImage ("icons/trackpad32.gif");
			break;
		default:
			break;
		}
		return icon;
	}
}

/* @(#)TrackPadBeanInfo.java */
