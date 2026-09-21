/**
 * Created on 08-nov-2005
 *
 * @author Humberto Martinez Barbera
 */
package tcrob.umu.quaky2.gui.images;

import java.awt.image.*;

import wucore.utils.math.*;

public class BufferedImageDrawing
{
	static public final double		ARROW_SIZE = 0.25;
	static public final double		ARROW_ANGLE = 20.0*Angles.DTOR;
	
	private BufferedImage			image;
	
	public BufferedImageDrawing ()
	{
	}
	
	public void updateImage (BufferedImage image)
	{
		this.image	= image;
	}
	
	public void drawBox (int xmin, int ymin, int xmax, int ymax, int color)
	{
		int			x, y;
		
		xmin = Math.max (xmin ,0);
		xmax = Math.min (xmax, image.getWidth()-1);
		ymin = Math.max (ymin ,0);
		ymax = Math.min (ymax, image.getHeight()-1);
		
		for (x = xmin; x <= xmax; x++)
		{
			image.setRGB (x, ymin, color);
			image.setRGB (x, ymax, color);
		}
		for (y = ymin; y <= ymax; y++)
		{
			image.setRGB (xmin, y, color);
			image.setRGB (xmax, y, color);
		}
	}
	
	public void drawCross (int x, int y, int color)
	{
		if ((x > 0) && (x < image.getWidth ()-1) && (y > 0) && (y < image.getHeight()-1))
		{
			image.setRGB (x, y, color);	
			image.setRGB (x-1, y, color);	
			image.setRGB (x+1, y, color);	
			image.setRGB (x, y-1, color);	
			image.setRGB (x, y+1, color);	
		}
	}
	
	public void drawPoint (int x, int y, int color)
	{
		image.setRGB (x, y, color);	
	}
	
	public void drawCircle (int x, int y, int radius, int color)
	{
		int			xx, yy;
		double		alpha;
		double		step;
		
		step		= Math.PI / (double) (radius * 5);
		for (alpha = 0; alpha <= Math.PI*0.5; alpha += step)
		{
			xx	= (int) Math.round (radius * Math.cos (alpha));
			yy	= (int) Math.round (radius * Math.sin (alpha));
			
			if ((x+xx >= 0) && (x+xx < image.getWidth ()))
			{
				if ((y+yy >= 0) && (y+yy < image.getHeight()))
					image.setRGB (x+xx, y+yy, color);
				if ((y-yy >= 0) && (y-yy < image.getHeight()))
					image.setRGB (x+xx, y-yy, color);
			}
			if ((x-xx >= 0) && (x-xx < image.getWidth ()))
			{
				if ((y+yy >= 0) && (y+yy < image.getHeight()))
					image.setRGB (x-xx, y+yy, color);
				if ((y-yy >= 0) && (y-yy < image.getHeight()))
					image.setRGB (x-xx, y-yy, color);
			}
		}
	}
	
	public void drawArc (int x, int y, int radius, double theta1, double theta2, int color)
	{
		int			xx, yy;
		double		alpha;
		double		step;
		
		step		= Math.PI / (double) (radius * 5);
		for (alpha = theta1; alpha <= theta2; alpha += step)
		{
			xx	= (int) Math.round (radius * Math.cos (alpha));
			yy	= (int) Math.round (radius * Math.sin (alpha));
			
			if ((x+xx >= 0) && (x+xx < image.getWidth ())
					&& (y+yy >= 0) && (y+yy < image.getHeight()))
				image.setRGB (x+xx, y+yy, color);
		}
	}
	
	public void drawLine (int m_x0, int m_y0, int m_x1, int m_y1, int color)
	{
		int			x, y;
		boolean		m_slopeLT1;
		int			m_slopeSign;
		int			m_twoDeltaY = 0;
		int			m_twoDeltaX = 0;
		int			m_twoDYDX = 0;
		int			m_twoDXDY = 0;
		int			m_param;
		
		// Find slope of line
		int deltaY = m_y1 - m_y0, deltaX = m_x1 - m_x0;
		// Check sign of slope
		m_slopeSign = 1;
		// Check if slope < 1
		m_slopeLT1 = true;
		if (Math.abs(deltaY) > Math.abs(deltaX)) {
			m_slopeLT1 = false;
		}
		if (m_slopeLT1) {
			// Ensure m_x0 is smallest x value if slope < 1
			if (m_x1 < m_x0) {
				int temp = m_x0;  m_x0 = m_x1;  m_x1 = temp;
				temp = m_y0;  m_y0 = m_y1;  m_y1 = temp;
				deltaX = -deltaX;
				deltaY = -deltaY;
			}
			if (deltaY < 0) {
				m_slopeSign = -1;
				deltaY = -deltaY;
			}
			m_twoDeltaY = 2 * deltaY;
			m_twoDYDX = 2 * (deltaY - deltaX);
			m_param = m_twoDeltaY - deltaX;
		} else {
			// Ensure m_y0 is smallest y value if slope >= 1
			if (m_y1 < m_y0) {
				int temp = m_x0;  m_x0 = m_x1;  m_x1 = temp;
				temp = m_y0;  m_y0 = m_y1;  m_y1 = temp;
				deltaX = -deltaX;
				deltaY = -deltaY;
			}
			if (deltaX < 0) {
				m_slopeSign = -1;
				deltaX = -deltaX;
			}
			m_twoDeltaX = 2 * deltaX;
			m_twoDXDY = 2 * (deltaX - deltaY);
			m_param = m_twoDeltaX - deltaY;
		}
		
		// Implements algorithm in Chapter 1 for slope < 1
		if (m_slopeLT1) {
			x = m_x0 + 1;
			y = m_y0;
			while (x < m_x1) {
				if (m_param < 0) {
					m_param += m_twoDeltaY;
				} else {
					m_param += m_twoDYDX;
					y += m_slopeSign;
				}
				if ((x >= 0) && (x < image.getWidth ()) && (y >= 0) && (y < image.getHeight()))
					image.setRGB (x, y, color);
				x++;
			}
			// Implements algorithm for slope >= 1
		} else {
			x = m_x0;
			y = m_y0 + 1;
			while (y < m_y1) {
				if (m_param < 0) {
					m_param += m_twoDeltaX;
				} else {
					m_param += m_twoDXDY;
					x += m_slopeSign;
				}
				if ((x >= 0) && (x < image.getWidth ()) && (y >= 0) && (y < image.getHeight()))
					image.setRGB (x, y, color);
				y++;
			}
		}
	}

	public void drawArrow (int m_x0, int m_y0, int m_x1, int m_y1, int color)
	{
		int				x2, y2;
		double			dx, dy;
		double			len, ang;
		
		dx = (m_x1 - m_x0);
		dy = (m_y1 - m_y0);
		
		len = ARROW_SIZE * Math.sqrt (dx*dx + dy*dy);
		ang = Math.atan2 (dy, dx);
		
		drawLine (m_x0, m_y0, m_x1, m_y1, color);
		
		x2 = m_x1 + (int) Math.round (len * Math.cos (Math.PI + ang + ARROW_ANGLE));
		y2 = m_y1 + (int) Math.round (len * Math.sin (Math.PI + ang + ARROW_ANGLE));
		drawLine (m_x1, m_y1, x2, y2, color);
		
		x2 = m_x1 + (int) Math.round (len * Math.cos (Math.PI + ang - ARROW_ANGLE));
		y2 = m_y1 + (int) Math.round (len * Math.sin (Math.PI + ang - ARROW_ANGLE));
		drawLine (m_x1, m_y1, x2, y2, color);
	}
	
	public void drawSector (int x, int y, int rhmin, double thmin, int rhmax, double thmax, int color)
	{
		int		x0, y0, x1, y1;
		
		drawArc (x, y, rhmin, thmin, thmax, color);
		drawArc (x, y, rhmax, thmin, thmax, color);
		
		x0 = (int) (x + rhmin * Math.cos (thmin));
		y0 = (int) (y + rhmin * Math.sin (thmin));
		x1 = (int) (x + rhmax * Math.cos (thmin));
		y1 = (int) (y + rhmax * Math.sin (thmin));
		drawLine (x0, y0, x1, y1, color);
		
		x0 = (int) (x + rhmin * Math.cos (thmax));
		y0 = (int) (y + rhmin * Math.sin (thmax));
		x1 = (int) (x + rhmax * Math.cos (thmax));
		y1 = (int) (y + rhmax * Math.sin (thmax));
		drawLine (x0, y0, x1, y1, color);
	}
}
