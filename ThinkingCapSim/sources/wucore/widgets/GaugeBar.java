/*
 * @(#)GaugeBar.java		1.0 Mar 25, 2004
 * 
 * (c) 2004 Denis Remondini
 *
 */
package wucore.widgets;

import javax.swing.JComponent;
import java.awt.*;

/**
 * @author Denis Remondini
 *
 */
public class GaugeBar extends JComponent {
	
	private double minimum;
	private double maximum;
	private double realValue;
	private boolean inverted;
	private Color borderColor;
	private int width = 100;
	private int height = 10;
	private int arrowX[], arrowY[];

	public GaugeBar() {
		minimum = 0;
		maximum = 100;
		realValue = 0;
		inverted = false;
		borderColor = Color.BLUE;
		this.setPreferredSize(new Dimension(width,height));
	}
	
	public double getMinimum() {
		return minimum;
	}
	
	public double getMaximum() {
		return maximum;
	}
	
	public double getValue() {
		return realValue;
	}
	
	public Color getBorderColor() {
		return borderColor;
	}
	
	public boolean getInverted() {
		return inverted;
	}
	

	public void setMinimum(double value) {
		minimum = value;
	}
	
	public void setMaximum(double value) {
		maximum = value;
	}
	
	public void setValue(double value) {
		if (value < minimum) 
			value = minimum;
		if (value > maximum)
			value = maximum;
		
		realValue = value;
		repaint();
	}
	
	public void setBorderColor(Color color) {
		borderColor = color;
	}
	
	public void setInverted(boolean inv) {
		inverted = inv;
		repaint();
	}
	
	public void paint(Graphics g) {   
		int linePos;
		g.setColor (borderColor);
		g.drawRect(0,0,getWidth()-1,getHeight()-1);
		if (inverted)
			linePos = (int) ((getWidth()-2)-((getWidth()-2)*(realValue-minimum)/(maximum-minimum)));
		else
			linePos = (int) ((getWidth()-2)*(realValue-minimum)/(maximum-minimum));
		g.drawLine(linePos,0,linePos,getHeight());
		g.drawLine(linePos+1,0,linePos+1,getHeight());
	}
}
