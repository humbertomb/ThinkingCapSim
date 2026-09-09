/*
 * (c) 2002 Alessandro Saffiotti
 * (c) 2004 David Herrero Perez
 *
 *	Fuzzy Markov GUI Component  
 *
 */

package tclib.navigation.localisation.fmarkov.gui;

import java.awt.*;
import java.awt.event.*;
import java.awt.geom.*;

import javax.swing.*;

import tc.shared.world.*;
import tclib.navigation.localisation.fmarkov.F2_5Cell;
import tclib.navigation.localisation.fmarkov.MK2_5FGrid;
import tclib.navigation.localisation.fmarkov.MKPos;
import wucore.utils.geom.*;

/**
 * @author David Herrero-Perez
 * @version 1.0
 * 
 */
public class MKCanvas extends JComponent {
	public class MKCanvasMouseListener extends MouseAdapter
	{
		public void mouseClicked(MouseEvent evt)
		{
			if ((evt.getModifiers() & InputEvent.BUTTON1_MASK) != 0) 
			{
				int gpos[] = getGridPosition(evt.getPoint());
//				double rpos[] = getRealPosition(evt.getPoint());
				
				F2_5Cell fcell = mkgrid.getCell(gpos[0], gpos[1]);
				
				if (fcell != null)
				{
					fcell.setHeight(F2_5Cell.FULL);
					//fcell.setCenter(Math.PI * 0.2 + fcell.getCenter());
				}
				
				//System.out.println("Window Position x:" + evt.getPoint().getX() + " y:" + evt.getPoint().getY());
				//System.out.println("  Real Position rx:" + rpos[0] + " ry:" + rpos[1]);
				//System.out.println("  Grid Position gx:" + gpos[0] + " gy:" + gpos[1]);
				
				repaint();
			}
			
			if ((evt.getModifiers() & InputEvent.BUTTON2_MASK) != 0)
			{
				//cleargrid();
				//repaint();
			}
			
			if ((evt.getModifiers() & InputEvent.BUTTON3_MASK) != 0)
			{
				int gpos[] = getGridPosition(evt.getPoint());
//				double rpos[] = getRealPosition(evt.getPoint());
				
				F2_5Cell fcell = mkgrid.getCell(gpos[0], gpos[1]);
				
				if (fcell != null)
				{
					System.out.println(fcell.toString());
					//fcell.setHeight(1.0);
				}
				
				repaint();
			}
		}
	}
	
	public class MKCanvasMouseMotionListener extends MouseMotionAdapter
	{
		public void mouseMoved(MouseEvent evt)
		{
			if (infopanel != null)
			{
				int gpos[] = getGridPosition(evt.getPoint());
				double rpos[] = getRealPosition(evt.getPoint());

				infopanel.setPointerValues(
					Integer.toString(gpos[0]),
					Integer.toString(gpos[1]),
					Double.toString((double)Math.round(rpos[0]*1000.0) / 1000.0),
					Double.toString((double)Math.round(rpos[1]*1000.0) / 1000.0));
			}
		}
		public void mouseDragged(MouseEvent evt)
		{
			// Process current position of cursor while mouse button is pressed.
			//process(evt.getPoint());
		}
	}
	
	public static final double ANGLE_LINE_LENGHT = 0.5*Math.sqrt(2);
	
	// World dimesions (m)
	double rwidth, rheight; // World dimensions width-height (m)
	double rborder; // Border dimension (m)
	double rcwidth, rcheight; // Centered world (m)
	double xcenter, ycenter; // Center of real world limits (It is the origin of global system reference)
	
	// World dimensions (grids)
	int gwidth, gheight;
	double gsize; // Size per cell (m)
	
	// Scaled world dimension
	double swidth, sheight; // Scaled world dimensions (m)
	double sborder; // Scaled border dimension (m)
	double scwidth, scheight; // Scaled centered world dimensions (m)
	
	double sgsize; // Scaled size per square (m)
	
	// Frame Size (pixels)
	int fxsize, fysize;
	
	// Drawing component parameters
	double ScaleFactor; // Drawing scale
	
	// Grid obtained of dicretize the world
	MK2_5FGrid mkgrid;
	MKPos mkpos;
	
	// Information component
	InforPanel infopanel;
	
	// A priori known world
	WMWall[] wmwall;
	
	//GUI Component
	int[] xpestpos; // x coordinates of estimated robot position
	int[] ypestpos; // y coordinates of estimated robot position
	
	boolean showangle;
	
	// Constructors
	public MKCanvas(MK2_5FGrid mkgrid, double ScaleFactor)
	{
		set(mkgrid);
		set(ScaleFactor);
		
		infopanel = null;
		mkpos = null;
		
		showangle = false;
		
		initGUI();
		
		addMouseListener(new MKCanvasMouseListener());
		addMouseMotionListener(new MKCanvasMouseMotionListener());
	}
	
	private void initGUI()
	{
		xpestpos = new int[3];
		ypestpos = new int[3];
		
		int radius = (int)(2*gsize*ScaleFactor);
		
		xpestpos[0] = radius;
		ypestpos[0] = 0;
		
		xpestpos[1] = (int)(-Math.cos(Math.PI/6)*radius);
		ypestpos[1] = (int)(Math.sin(Math.PI/6)*radius);
		
		xpestpos[2] = (int)(-Math.cos(Math.PI/6)*radius);
		ypestpos[2] = (int)(-Math.sin(Math.PI/6)*radius);
	}
	
	private void set(MK2_5FGrid mkgrid)
	{
		this.mkgrid = mkgrid;
		
		gwidth = mkgrid.getGridWidth();
		gheight = mkgrid.getGridHeight();
		gsize = mkgrid.getCellSize();
		
		rwidth = mkgrid.getWidth();
		rheight = mkgrid.getHeight();
		rborder = mkgrid.getBorder();
		
		rcwidth = mkgrid.getCenterWidth();
		rcheight = mkgrid.getCenterHeight();
		xcenter = mkgrid.getXCenterWorld();
		ycenter = mkgrid.getYCenterWorld();
		
		WMWalls wmwalls = mkgrid.getWorld().walls();
		wmwall = wmwalls.edges();
	}
	
	private void set(double ScaleFactor)
	{
		this.ScaleFactor = ScaleFactor;
		
		sgsize = gsize * ScaleFactor;
		
		swidth = rwidth * ScaleFactor;
		sheight = rheight * ScaleFactor;
		sborder = rborder * ScaleFactor;
		
		scwidth = (swidth + sborder) * 0.5;
		scheight = (sheight + sborder) * 0.5;
		
		fxsize = (int) (swidth + sborder);
		fysize = (int) (sheight + sborder);
		
		setPreferredSize(new Dimension(fxsize, fysize));
	}
	
	public void setMKPos(MKPos mkpos)
	{
		this.mkpos = mkpos;
	}
	
	public void setShowAngle(boolean showangle)
	{
		this.showangle = showangle;
	}
	
	// Accessors
	public void setInfoPanel(InforPanel infopanel)
	{
		this.infopanel = infopanel;
	}

	// Transformations
	public double getGridCenterPosX(int i)
	{
		return (((double) (i - (gwidth >> 1))) + 0.5) * gsize;
	}
	public double getGridCenterPosY(int j)
	{
		return (((double) (j - (gheight >> 1))) + 0.5) * gsize;
	}
	// Return the real world coordinate with a component coordinate input value
	public double getRealX(double x)
	{
		return ((x - scwidth) / ScaleFactor) + rcwidth;
	}
	public double getRealY(double y)
	{
		return ((y - scheight) / ScaleFactor) + rcheight;
	}
	// Return the component coordinate with a real world input value
	public int getPosX(double rx)
	{
		return (int) ((rx - xcenter) * ScaleFactor + scwidth);
	}
	public int getPosY(double ry)
	{
		return (int) ((ry - ycenter) * ScaleFactor + scheight);
	}

	public int getWindowPosX(double x)
	{
		return (int) (scwidth + x * ScaleFactor);
	}
	public int getWindowPosY(double y)
	{
		return (int) (scheight + y * ScaleFactor);
	}
	// Return the grid index with a component coordinate input
	public int getGridX(double x)
	{
		return (int) (((x - scwidth) / sgsize) + (double) (gwidth >> 1));
	}
	public int getGridY(double y)
	{
		return (int) (((y - scheight) / sgsize) + (double) (gheight >> 1));
	}
	
	protected double[] getRealPosition(Point point)
	{
		double aux[] = new double[2];
		
		aux[0] = getRealX(point.getX());
		aux[1] = getRealY(point.getY());
		
		return aux;
	}
	
	protected int[] getGridPosition(Point point)
	{
		int aux[] = new int[2];
		
		aux[0] = getGridX(point.getX());
		aux[1] = getGridY(point.getY());
		
		//System.out.println("    Grid gx: " + aux[0] + " rx: " + getGridRealPosX(aux[0]) + " gy: " + aux[1] + " ry: " + getGridRealPosY(aux[1]));
		
		return aux;
	}
	
	// This method is called whenever the contents needs to be painted
	public void paint(Graphics g)
	{
		Graphics2D g2d = (Graphics2D) g;
		
		drawGrid(g2d); // Paint the grid
		drawWorld(g2d); // Paint the world
		drawPos(g2d); // Paint the estimated position
	}

	private void drawWorld(Graphics2D g2d)
	{
		Line2 line2;
		int x1, y1, x2, y2;

		for (int i = 0; i < wmwall.length; i++)
		{
			line2 = wmwall[i].edge;
			
			x1 = getPosX(line2.orig().x());
			y1 = getPosY(line2.orig().y());
			x2 = getPosX(line2.dest().x());
			y2 = getPosY(line2.dest().y());
			
			g2d.setPaint(Color.GREEN);
			g2d.drawLine(x1, y1, x2, y2);
		}
	}

	private void drawGrid(Graphics2D g2d)
	{
		int gx, gy;
		double heightvalue;
		
		int x0, y0, x1, y1;
		double th, ax, ay;
		
		int size;
		int graylevel;
		
		size = (int) sgsize;
		
		for (int i = 0; i < gwidth; i++)
			for (int j = 0; j < gheight; j++) 
			{
				gx = getWindowPosX(getGridCenterPosX(i)) - (size >> 1);
				gy = getWindowPosY(getGridCenterPosY(j)) - (size >> 1);
				
				heightvalue = 1.0 - mkgrid.getCell(i, j).getHeight();
				graylevel = (int) (heightvalue * 255.0);
				g2d.setPaint(new Color(graylevel, graylevel, graylevel));
				g2d.fill(new Rectangle2D.Double(gx, gy, size, size));
				
				if (showangle)
				{
					th = mkgrid.getCell(i, j).getCenter();
					ax = gsize * ANGLE_LINE_LENGHT * Math.cos(th);
					ay = gsize * ANGLE_LINE_LENGHT * Math.sin(th);
					
					x0 = gx + (size >> 1);
					y0 = gy + (size >> 1);
					x1 = x0 + (int)(ax * ScaleFactor);
					y1 = y0 + (int)(ay * ScaleFactor);
					
					g2d.setPaint(Color.RED);
					g2d.drawLine(x0, y0, x1, y1);
				}
			}
	}
	
	private void drawPos(Graphics2D g2d)
	{
		if(mkpos == null) return;
		
		AffineTransform tx = new AffineTransform();
	    //tx.scale(scalex, scaley);
	    //tx.shear(shiftx, shifty);
	    tx.translate(getPosX(mkpos.getX()), getPosY(mkpos.getY()));
	    tx.rotate(mkpos.getTheta());
	    
		g2d.setPaint(Color.MAGENTA);
		g2d.draw(tx.createTransformedShape(new Polygon(xpestpos, ypestpos, 3)));
		//g2d.fill(tx.createTransformedShape(new Polygon(xpestpos, ypestpos, 3)));
	}
}
