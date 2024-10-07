package tclib.utils.petrinets;

import java.util.*;
import java.awt.*;

import wucore.utils.math.*;
import wucore.widgets.*;
import wucore.utils.color.*;

public class PNEdge extends PNObject 
{
	public static final double		ARROW_ANG	= 10.0 * Angles.DTOR;		// degrees
	public static final double		ARROW_SIZ	= 0.2;					// length percentage
	
	public static final int NOTHING = 0;
	public static final int TRANSITION = 1;
	public static final int NODE = 2;
	
	// type and index the edge comes from and goes to
	private int tFrom;
	private int iFrom;
	
	private int tTo;
	private int iTo;
	
	// NEU (25.03.1997 MK)
	private boolean negated;
	private int weight;
	
	private Polygon points;
	
	protected PNEdge()
	{
		super ();
		
		color = WColor.BLACK;
		points = null;
		
		// NEU (25.03.1997 MK)
		this.negated = false;
		this.weight = 1;     // new (15.5.97 JW)
		// sehr sinnvoll, gewicht auf null zu setzen!!  	        
	}
	
	public PNEdge(int tFrom, int iFrom, int tTo, int iTo, Polygon points)
	{
		this ();
		
		this.tFrom = tFrom;
		this.iFrom = iFrom;
		this.tTo = tTo;
		this.iTo = iTo;
		
		if (points != null) {
			this.points = new Polygon();
			this.points.addPoint(0, 0);
			for (int i = 0; i < points.npoints; i++)
				this.points.addPoint(points.xpoints[i], points.ypoints[i]);
		}
		else
			this.points = null;
	}
	
	// NEU (25.03.1997 MK)
	public boolean isNegated() {
		return negated;
	}
	
	// neu (15.4.97 JW)
	public void setNegated(boolean n) {
		negated = n;
	}
	
	// NEU (25.03.1997 MK)
	public int getWeight() {
		return weight;
	}
	
	// NEU (25.03.1997 MK)
	public void setWeight(int weight) {
		this.weight = weight;
	}
	
	
	public int getTFrom() {
		return tFrom;
	}
	
	public void setTFrom(int tFrom) {
		this.tFrom = tFrom;
	}
	
	public int getTTo() {
		return tTo;
	}
	
	public void setTTo(int tTo) {
		this.tTo = tTo;
	}
	
	public int getIFrom() {
		return iFrom;
	}
	
	public void setIFrom(int iFrom) {
		this.iFrom = iFrom;
	}
	
	public int getITo() {
		return iTo;
	}
	
	public void setITo(int iTo) {
		this.iTo = iTo;
	}
	
	public Polygon getPoints() {
		return points;
	}
	
	public void setPoints(Polygon p) {
		points = p;
	}
	
	public int getNumberOfPoints() {
		if (points == null)
			return 0;
		return (points.npoints - 1);
	}
	
	public Point getPoint(int i) {
		if (points == null)
			return null;
		Point p = new Point(points.xpoints[i + 1],
				points.ypoints[i + 1]);
		return p;
	}
	
	// neu (04.05.1997 MK)
	public Point getWeightPosition(PetriNet PN) {
		Point pFrom, pTo;
		if (points == null) {
			pFrom = getXYFrom(PN);
			pTo = getXYTo(PN);
		}
		else {
			pFrom = new Point(0, 0);
			pTo = new Point(0, 0);
			pFrom.x = points.xpoints[0];
			pFrom.y = points.ypoints[0];
			pTo.x = points.xpoints[1];
			pTo.y = points.ypoints[1];
		}
		double dx = (double) (pTo.x - pFrom.x);
		double dy = (double) (pTo.y - pFrom.y);
		double length = Math.sqrt(dx * dx + dy * dy);
		int xAdd = (int) (dy * 10.0 / length);
		int yAdd = (int) (dx * 10.0 / length);
		
		Point res = new Point(0, 0);
		if (dy <= 0) {
			if (dx <= 0) {
				res.y = pFrom.y + (int) (dy / 2.0) + yAdd;
				res.x = pFrom.x + (int) (dx / 2.0) - xAdd;
			}
			else {
				res.y = pFrom.y + (int) (dy / 2.0) - yAdd;
				res.x = pFrom.x + (int) (dx / 2.0) + xAdd;
			}
		}
		else {
			if (dx <= 0) {
				res.y = pFrom.y + (int) (dy / 2.0) + yAdd;
				res.x = pFrom.x + (int) (dx / 2.0) - xAdd;
			}
			else {
				res.y = pFrom.y + (int) (dy / 2.0) - yAdd;
				res.x = pFrom.x + (int) (dx / 2.0) + xAdd;
			}
		}
		return res;
	}
	
	// neu (15.4.97 JW)
	public boolean pointingTo(PetriNet p, PNTransition t){
		int ind;
		ind =p.getTransIndex(t);
		boolean ret = false;
		
		if ((getTTo() == TRANSITION) && (getITo() == ind)) ret = true;
		
		return ret;
	}
	
	public void setLastPoint(int x, int y) {
		if (points == null) {
			int[] xpoints = new int[2];
			int[] ypoints = new int[2];
			xpoints[0] = ypoints[0] = 0;
			xpoints[1] = x;
			ypoints[1] = y;
			points = new Polygon(xpoints, ypoints, 2);
		}
		else {
			points.xpoints[points.npoints - 1] = x;
			points.ypoints[points.npoints - 1] = y;
		}
	}
	
	public void clearAllPoints() {
		points = null;
	}
	
	public void clearLastPoint() {
		if (points.npoints <= 2)
			points = null;
		else {
			
			int[] newX = new int[points.npoints];
			int[] newY = new int[points.npoints];
			
			for (int i = 0; i < points.npoints - 1; i++) {
				newX[i] = points.xpoints[i];
				newY[i] = points.ypoints[i];
			}
			Polygon p = new Polygon(newX, newY, points.npoints - 1);
			points = p;
		}
	}
	
	public void addPoint(int x, int y) {
		if (points == null) {
			points = new Polygon();
			points.addPoint(0,0);
		}
		points.addPoint(x, y);
	}
	
	public double distance (double px, double py, PetriNet PN) {
		Point p1 = getXYFrom(PN);
		Point p2 = getXYTo(PN);
		if (points == null) {
			return distancePointToLine(px, py, p1.x, p1.y, p2.x, p2.y);
		}
		else if (points.npoints > 1) {
			double d1 = distancePointToLine(px, py,
					p1.x, p1.y,
					points.xpoints[1],
					points.ypoints[1]);
			double d2 = distancePointToLine(px, py,
					p2.x, p2.y,
					points.xpoints[points.npoints - 1],
					points.ypoints[points.npoints - 1]);
			double d3 = Integer.MAX_VALUE;
			if (points.npoints > 2) {
				for (int i = 1; i < points.npoints - 1; i++) {
					double d3new = distancePointToLine(px, py,
							points.xpoints[i],
							points.ypoints[i],
							points.xpoints[i+1],
							points.ypoints[i+1]);
					if (d3new < d3)
						d3 = d3new;
				}
			}
			
			if (d1 > d2)
				d1 = d2;
			
			if (d1 > d3)
				d1 = d3;
			
			return d1;
		}
		else
			return Integer.MAX_VALUE;
	}
	
	private double distancePointToLine (double px, double py, double x1, double y1, double x2, double y2) {
		double ax, ay, bx, by;
		
		ax = x1;
		ay = y1;
		
		bx = x2 - x1;
		by = y2 - y1;
		
		// length of vector b
		double lb = Math.sqrt(bx * bx + by * by);
		
		double t0 = ((px - ax) * bx + (py - ay) * by) / (lb * lb);
		
		double x0x = ax + t0 * bx;
		double x0y = ay + t0 * by;
		
		if (x0x < x1 && x0x < x2 ||
				x0x > x1 && x0x > x2 ||
				x0y < y1 && x0y < y2 ||
				x0y > y1 && x0y > y2) {
			double dH, dT;
			dH = distancePointToPoint(px, py, x1, y1);
			dT = distancePointToPoint(px, py, x2, y2);
			if (dH < dT)
				return dH;
			return dT;
		}
		
		double tmpx = (by * (px - ax)) - (bx * (py - ay));
		double tmpy = (bx * (py - ay)) - (by * (px - ax));
		
		double ltmp = Math.sqrt(tmpx * tmpx + tmpy * tmpy);
		
		double l = ltmp / lb;
		
		return l;
	}
	
	private double distancePointToPoint(double px1, double py1, double px2, double py2) {
		double dx = (px1 - px2);
		double dy = (py1 - py2);
		double dist = Math.sqrt (dx*dx + dy*dy);
		return dist;
	}
	
		/*
	 public Point getXYFrom(pn PN) {
	 int xF, yF, xT, yT;
	 Point p;
	 switch(tFrom) {
	 case TRANSITION:
	 transition t = PN.getTransition(iFrom);
	 p = new Point(t.getX(), t.getY());
	 return p;
	 case NODE:
	 node n = PN.getNode(iFrom);
	 xF = n.getX();
	 yF = n.getY();
	 break;
	 default:
	 p = new Point(0, 0);
	 return p;
	 }
	 if (points == null) {
	 switch(tTo) {
	 case NOTHING:
	 p = new Point(xF, yF);
	 return p;
	 case TRANSITION:
	 transition t = PN.getTransition(iTo);
	 xT = t.getX();
	 yT = t.getY();
	 break;
	 case NODE:
	 node n = PN.getNode(iTo);
	 xT = n.getX();
	 yT = n.getY();
	 break;
	 default:
	 xT = yT = 0;
	 }
	 }
	 else {
	 xT = points.xpoints[1];
	 yT = points.ypoints[1];
	 }
	 double dx = (double) (xT - xF);
	 double dy = (double) (yT - yF);
	 double length = Math.sqrt(dx * dx + dy * dy);
	 int xAdd = (int) Math.round((double) (node.getRadius() + 1) * dx / length);
	 int yAdd = (int) Math.round((double) (node.getRadius() + 1) * dy / length);
	 p = new Point(xF + xAdd, yF + yAdd);
	 return p;
	 }
	 
	 	 */
	
		public Point getXYFrom(PetriNet PN) {
		return getXYFrom(PN, 0.0);
	}
	
	
	// NEU (geringer Unterschied zu 'getXYFrom(pn PN)') (25.03.1997 MK)
	// changed (26.06.1997 MK)
	public Point getXYFrom(PetriNet PN, double distance) {
		int xF, yF, xT, yT;
		int radius = 0;
		Point p;
		switch(tFrom) {
		case TRANSITION:
			PNTransition t = PN.getTransition(iFrom);
			if (t.getOrientation() == PNTransition.ORIENTATION_ALL) {
				distance += 20.0;   
			}
			xF = t.x();
			yF = t.y();
			break;
		case NODE:
			PNNode n = PN.getNode(iFrom);
			xF = n.x();
			yF = n.y();
			radius = n.getRadius();
			break;
		default:
			p = new Point(0, 0);
		return p;
		}
		if (points == null) {
			switch(tTo) {
			case NOTHING:
				p = new Point(xF, yF);
				return p;
			case TRANSITION:
				PNTransition t = PN.getTransition(iTo);
				xT = t.x();
				yT = t.y();
				break;
			case NODE:
				PNNode n = PN.getNode(iTo);
				xT = n.x();
				yT = n.y();
				radius = n.getRadius ();
				break;
			default:
				xT = yT = 0;
			}
		}
		else {
			xT = points.xpoints[1];
			yT = points.ypoints[1];
		}
		double dx = (double) (xT - xF);
		double dy = (double) (yT - yF);
		double length = Math.sqrt(dx * dx + dy * dy);
		int xAdd, yAdd;
		xAdd = yAdd = 0;
		if (tFrom == TRANSITION) {
			xAdd = (int) Math.round((1 + distance) * dx / length);
			yAdd = (int) Math.round((1 + distance) * dy / length);
		}
		else if (tFrom == NODE) {
			xAdd = (int) Math.round((double) (radius + 1 + distance) * dx / length);
			yAdd = (int) Math.round((double) (radius + 1 + distance) * dy / length);
		}
		p = new Point(xF + xAdd, yF + yAdd);
		return p;
	}
	
	
	/*    public Point getXYTo(pn PN) {
	 int xF, yF, xT, yT;
	 Point p;
	 switch(tTo) {
	 case NOTHING:
	 p = new Point(points.xpoints[points.npoints-1],
	 points.ypoints[points.npoints-1]);
	 return p;
	 case TRANSITION:
	 transition t = PN.getTransition(iTo);
	 p = new Point(t.getX(), t.getY());
	 return p;
	 case NODE:
	 node n = PN.getNode(iTo);
	 xT = n.getX();
	 yT = n.getY();
	 break;
	 default:
	 p = new Point(0, 0);
	 return p;
	 }
	 if (points == null) {
	 switch(tFrom) {
	 case TRANSITION:
	 transition t = PN.getTransition(iFrom);
	 xF = t.getX();
	 yF = t.getY();
	 break;
	 case NODE:
	 node n = PN.getNode(iFrom);
	 xF = n.getX();
	 yF = n.getY();
	 break;
	 default:
	 xF = yF = 0;
	 }
	 }
	 else {
	 xF = points.xpoints[points.npoints - 1];
	 yF = points.ypoints[points.npoints - 1];
	 }
	 double dx = (double) (xT - xF);
	 double dy = (double) (yT - yF);
	 double length = Math.sqrt(dx * dx + dy * dy);
	 int xAdd = (int) Math.round((double) (node.getRadius() + 1) * dx / length);
	 int yAdd = (int) Math.round((double) (node.getRadius() + 1) * dy / length);
	 p = new Point(xT - xAdd, yT - yAdd);
	 return p;
	 }
	 	 */
		public Point getXYTo(PetriNet PN) {
		return getXYTo(PN, 0.0);
	}
	
	
	// neu (27.04.1997 MK)
	// changed (26.06.1997 MK)
	public Point getXYTo(PetriNet PN, double distance) {
		int xF, yF, xT, yT;
		int radius = 0;
		Point p;
		switch(tTo) {
		case TRANSITION:
			PNTransition t = PN.getTransition(iTo);
			if (t.getOrientation() == PNTransition.ORIENTATION_ALL) {
				distance += 20.0;   
			}
			xT = t.x();
			yT = t.y();
			break;
		case NODE:
			PNNode n = PN.getNode(iTo);
			xT = n.x();
			yT = n.y();
			radius = n.getRadius();
			break;
		case NOTHING:
			xT = points.xpoints[points.npoints-1];
			yT = points.ypoints[points.npoints-1];
			p = new Point(xT, yT);
			return p;
			//               break;
		default:
			p = new Point(0, 0);
		return p;
		}
		if (points == null) {
			switch(tFrom) {
			case NOTHING:
				p = new Point(xT, yT);
				return p;
			case TRANSITION:
				PNTransition t = PN.getTransition(iFrom);
				xF = t.x();
				yF = t.y();
				break;
			case NODE:
				PNNode n = PN.getNode(iFrom);
				xF = n.x();
				yF = n.y();
				radius = n.getRadius ();
				break;
			default:
				xF = yF = 0;
			}
		}
		else {
			xF = points.xpoints[points.npoints - 1];
			yF = points.ypoints[points.npoints - 1];
		}
		double dx = (double) (xT - xF);
		double dy = (double) (yT - yF);
		double length = Math.sqrt(dx * dx + dy * dy);
		int xAdd, yAdd;
		xAdd = yAdd = 0;
		if (tTo == TRANSITION) {
			xAdd = (int) Math.round ((1 + distance) * dx / length);
			yAdd = (int) Math.round ((1 + distance) * dy / length);
			
		}
		else {
			xAdd = (int) Math.round((double) (radius + 1 + distance) * dx / length);
			yAdd = (int) Math.round((double) (radius + 1 + distance) * dy / length);
		}
		p = new Point(xT - xAdd, yT - yAdd);
		return p;
	}
	
	
	public boolean adjust(PetriNet PN, int index, int type) {
		boolean ret = false;
		
		if (type == tFrom) {
			if (index == iFrom)
				ret = true;
			else if (iFrom > index)
				iFrom--;
		}
		if (type == tTo) {
			if (index == iTo)
				ret = true;
			else if (iTo > index)
				iTo--;
		}
		return ret;
	}
	
	public void set (PNEdge e) 
	{
		super.set (e);
		
		Polygon copyPoly = null;
		if (points != null) {
			copyPoly = new Polygon();
			for (int i = 1; i < e.points.npoints; i++) {
				copyPoly.addPoint(e.points.xpoints[i], e.points.ypoints[i]);
			}
		}
		
		setPoints (copyPoly);
		setNegated(e.negated);
		setWeight(e.weight);
	}
		
	public void draw (Model2D model, PetriNet PN)
	{
		Polygon points = getPoints();
		//
		if (getTFrom() == getTTo())
		{
			if (getIFrom() == getITo() && points != null) {
				if (points.npoints <= 2)
					return;
			}
		}
		// edges that will not be drawn
		if (getTFrom() == PNEdge.NOTHING)
			return;
		if (getTTo() == PNEdge.NOTHING && points == null)
			return;
		
		Point pFrom, pTo;
		pFrom = getXYFrom(PN);
		pTo = getXYTo(PN);
		
		// if edge is negative, the edge is drawn further away from the transition,
		// so it doesnt stick out of the oval at the end of the edge
		if (isNegated() == true) {
			if (getTFrom() == PNEdge.TRANSITION) {
				pFrom = getXYFrom(PN, 6.0);
				model.addRawCircle (pFrom.x, pFrom.y, 3, Model2D.FILLED, ColorTool.fromWColorToColor(color));
			}
			else if (getTTo() == PNEdge.TRANSITION) {
				pTo = getXYTo(PN, 6.0);
				model.addRawCircle (pTo.x, pTo.y, 3, Model2D.FILLED, ColorTool.fromWColorToColor(color));
			}
		}
		
		// if edge is build out of more than one line draw, draw all lines except
		// the last one (which will be an arrow or a just line again)
		if (points != null) {
			points.xpoints[0] = pFrom.x;
			points.ypoints[0] = pFrom.y;
			//			g.drawPolygon(points);
			
			// x and y position for the last line are adjusted
			pFrom.x = points.xpoints[points.npoints-1];
			pFrom.y = points.ypoints[points.npoints-1];
		}
		
		// draw arrow or line (the line already got the oval at the transition)
		if (isNegated() == false)
		{
			double		rho, phi;
			
			rho	= Math.sqrt (Math.pow (pTo.x-pFrom.x, 2.0) + Math.pow (pTo.y-pFrom.y, 2.0));
			phi	= Math.atan2 (pTo.y-pFrom.y, pTo.x-pFrom.x);
			model.addRawArrow (pFrom.x, pFrom.y, rho, phi, ARROW_SIZ, ARROW_ANG, Model2D.PLAIN, ColorTool.fromWColorToColor(color));
		}
		else
			model.addRawLine (pFrom.x, pFrom.y, pTo.x, pTo.y, ColorTool.fromWColorToColor(color));
		
		// the weight of the edge will be drawn if the weight of the edge
		// is higher than 1 and not negated
		if (getWeight() > 1 && isNegated() == false)
		{
			Point weightPos = getWeightPosition(PN);
			model.addRawText (weightPos.x, weightPos.y, new Integer (getWeight ()).toString (), ColorTool.fromWColorToColor(color));
		}
	}
	
	public void fromPropString (StringTokenizer st)
	{
		super.fromPropString (st);
		
		iFrom	= Integer.parseInt (st.nextToken());
		tFrom	= Integer.parseInt (st.nextToken());
		iTo		= Integer.parseInt (st.nextToken());
		tTo		= Integer.parseInt (st.nextToken());
		negated	= new Boolean (st.nextToken()).booleanValue ();		
		weight	= Integer.parseInt (st.nextToken());
		//		weight	= Double.parseDouble (st.nextToken());
	}
	
	public String toPropString ()
	{
		return super.toPropString () + ", " + iFrom + ", " + tFrom + ", " + iTo + ", " + tTo + ", " + negated + ", " + weight;
	}
	
	private void drawArrow(Graphics g, Point p1, Point p2) {
		//     System.out.println("drawArrow from (" + p1.x + ", " + p1.y + ") to (" + p2.x + ", " + p2.y + ")");
		drawArrow(g, p1.x, p1.y, p2.x, p2.y);
	}
	
	private void drawArrow(Graphics g, int xS, int yS, int xE, int yE) {
		// variables to store the x, y koordinates of the polygon
		// forming the head of the arrow.
		int xP[] = new int[3];
		int yP[] = new int[3];
		
		if (xE < 0.0)
			xP[0] = (int) (xE - 1.0);
		else
			xP[0] = (int) (xE + 1.0);
		
		if (yE < 0.0)
			yP[0] = (int) (yE - 1.0);
		else
			yP[0] = (int) (yE + 1.0);
		
		double  dx = xS - xE;
		double  dy = yS - yE;
		//       System.out.println(xS + " - " + xE + " = " + dx);
		//       System.out.println(yS + " - " + yE + " = " + dy);
		double  length = Math.sqrt(dx * dx + dy * dy);
		
		double  xAdd = 9.0 * dx / length;
		double  yAdd = 9.0 * dy / length;
		
		xP[1] = (int) Math.round (xE + xAdd - (yAdd / 3.0));
		yP[1] = (int) Math.round (yE + yAdd + (xAdd / 3.0));
		xP[2] = (int) Math.round (xE + xAdd + (yAdd / 3.0));
		yP[2] = (int) Math.round (yE + yAdd - (xAdd / 3.0));
		
		//      System.out.println(xS + " " + yS + ", " + xE + " " + yE + ", " + xAdd + " " + yAdd);
		
		g.drawLine(xS, yS, xP[0], yP[0]);
		g.fillPolygon(xP, yP, 3);
	}
}
