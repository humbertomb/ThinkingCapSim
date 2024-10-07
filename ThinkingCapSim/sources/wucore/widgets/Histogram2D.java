/*
 * @(#)Histogram2D.java		1.0 2004/01/27
 * 
 * (c) 2004 Denis Remondini
 *
 */
package wucore.widgets;

import javax.swing.JComponent;
import java.awt.*;
import java.text.DecimalFormat;
import java.lang.reflect.Array;

/**
 * This class implements a graphical component that displays an histogram.
 * 
 * @version	1.0		27 Jan 2004
 * @author Denis Remondini
 */
public class Histogram2D extends JComponent {
		
	/* Specifies if the x coordinates have to be visualized.*/
	protected boolean drawXValues;
	/* The values that identify the histogram (x and y coordinates) */
	protected double[] xValues, yValues;
	/* number of bars in the histogram */
	protected int nBars;
	/* font used to display the x coordinates */
	private Font textFont;
	/* max value that can be assumed by an histogram bar */
	protected double max = 1.0;	
	/* min value that can be assumed by an histogram bar */
	protected double min = 0.0;
	/* color of the histogram bars */
	protected Color	barColor = Color.BLUE;
	
	/**
	 * Creates an empty histogram.
	 * @param nBars the number of the histogram bars 
	 * @param barWidth the width of each histogram bar
	 * @param barHeight the heigth of the histogram bars
	 */
	public Histogram2D(int nBars,int barWidth, int barHeight) {
		if (nBars > 0)
			this.nBars = nBars;
		else 
			nBars = 1;
		xValues = new double[nBars];
		yValues = new double[nBars];
		drawXValues = false;
		textFont = new Font(null,Font.PLAIN,9);
		this.setPreferredSize (new Dimension(nBars*barWidth, barHeight));
	}
	
	/**
	 * Sets the y coordinates of the histogram
	 * @param values the y coordinates of the histogram
	 */
	public void setYValues (double[] values)
	{
		for (int i = 0; (i < nBars) && (i < Array.getLength(values)); i++)
			if (values[i] >= min)
				this.yValues[i] = values[i];
		repaint();
	}
	
	/**
	 * Sets the x coordinates of the histogram
	 * @param values the x coordinates of the histogram
	 */
	public void setXValues(double[] values) {
		for (int j = 0; (j < nBars) && (j < Array.getLength(values)); j++)
			xValues[j] = values[j];
		repaint();
	}
	
	/**
	 * Specifies if the x coordinates have to be displayed
	 * @param draw true if the x coordinates have to be displayed, false otherwise.
	 */
	public void setDrawXValues(boolean draw) {
		drawXValues = draw;
	}
	
	/**
	 * Sets the max value that can be assumed by an histogram bar. If some bars have a greater value they assumed
	 * this new max value.
	 * @param m the max value that can be assumed by an histogram bar
	 */
	public void setMax (double m)
	{
		max = m;
		for (int i = 0; i < nBars; i++)
			if (yValues[i] > max)	
				yValues[i] = max;
	}
	
	/**
	 * Sets the min value that can be assumed by an histogram bar. If some bars have a smaller value they assumed
	 * this new min value.
	 * @param m the min value that can be assumed by an histogram bar
	 */
	public void setMin (double m) {
		min = m;
		for (int i = 0; i < nBars; i++)
			if (yValues[i] < min)	
				yValues[i] = min;
	}
	
	/**
	 * Sets the font that has to be used to display the x coordinates.
	 * @param newFont the font that has to be used to display the x coordinates.
	 */
	public void setFont(Font newFont) {
		textFont = newFont;
	}
	
	/**
	 * Shows the histogram.
	 */
	public void paint(Graphics g) {   
		int barHeight;
		/* Computes the width of each bar */
		int barWidth = getWidth()/nBars;
		
		/* Computes the height of the histogram bars */
		if (drawXValues)
			barHeight = getHeight()-textFont.getSize()*2;
		else 
			barHeight = getHeight()-10;
		
		/* draws the background */
		g.setColor (this.getBackground ());
		g.fillRect (0,0,getWidth(),getHeight());
		
		/* draws the histogram bars */
		for (int i = 0; i < nBars; i++) {
			g.setColor(barColor);		
			g.fillRect(i*barWidth,barHeight-(int)(barHeight*yValues[i]/max),barWidth-1,(int)(barHeight*yValues[i]/max));
			/* displays the x coordinate */
			if (drawXValues) {
				DecimalFormat fmt = new DecimalFormat();
				fmt.setMaximumFractionDigits(1);
				fmt.setMinimumFractionDigits(1);	
				g.setFont(textFont);
				g.drawString(fmt.format(xValues[i]),i*barWidth,barHeight+textFont.getSize());
				
			}
		}
		/* draws the histogram base line */
		g.setColor(barColor);
		g.drawLine(0,barHeight,getWidth(),barHeight);		
	}
}
