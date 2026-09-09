/** 
 * Model2D.java
 *
 * Description:		Generic component to store 2D information
 * @author			Humberto Martinez Barbera
 * @version			3.2
 */

package wucore.widgets;

import java.net.*;
import java.awt.*;

import wucore.utils.geom.*;
import wucore.utils.math.*;

public class Model2D extends Object
{
	public static final int				MAXVERT		= 50000;
	public static final int				MAXATTR		= 15000;
	
	// Basic shapes and entities
	public static final int				BOX			= 0;
	public static final int				LINE		= 1;
	public static final int				TEXT		= 2;
	public static final int				AXIS		= 3;
	public static final int				POINT		= 4;
	public static final int				ICON		= 5;
	public static final int				LABEL		= 6;
	public static final int				POLY		= 7;
	public static final int				CIRCLE		= 8;
	public static final int				ARC			= 9;	
	public static final int				DOT			= 10;
	public static final int				ZSOLID		= 11;
	public static final int				IMAGE		= 12;
	
	// Shape/entity modifiers
	public static final int				PLAIN		= 0;
	public static final int				FILLED		= 1;
	public static final int				FILLOUT		= 2;
	public static final int				DASHED		= 3;
	public static final int				THICK		= 4;
	public static final int				SELECT		= 5;
	public static final int				J_CENTER	= 6;
	public static final int				J_LEFT		= 7;	
	public static final int				J_RIGHT		= 8;	
	
	// Basic shape constants
	public static final double			ARROW_ANG	= 25.0 * Angles.DTOR;	// degrees
	public static final double			ARROW_SIZ	= 0.15;					// length percentage
	
	// Standard drawing objects (vertex & edges)
	public Model2DCoord[]				verts = new Model2DCoord[MAXVERT];	// Array of world and projected standard verteces (scaled and rotated)
	protected int						nvert;
	public Model2DAttr[]				attr = new Model2DAttr[MAXATTR];	// Array of line attributes
	public int							nattr;
	
	// World related parameters
	protected double[] 					mod_bor = new double[4];			// Model standard boundaries (world coordinates) - Bounding Box
	protected boolean					updated;							// Are there new objects available?
	protected boolean					growbbox = true;					// Update the bounding box?
	
	// Constructors
	public Model2D ()
	{    	
		// Set default parameters
		clearView ();
	}
	
	/* Accessor methods */
	public final double 		getMinX ()						{ return mod_bor[0]; }
	public final double 		getMinY ()						{ return mod_bor[1]; }
	public final double 		getMaxX ()						{ return mod_bor[2]; }
	public final double 		getMaxY ()						{ return mod_bor[3]; }
	public final double[] 		getBoundingBox ()				{ return mod_bor; }
	public final void			growBoundingBox (boolean grow)	{ growbbox = grow; }
	
	public final boolean 		isUpdated ()					{ return updated; }
	public final void	 		setUpdated (boolean updated)	{ this.updated = updated; }
	
	// Instance methods
	public void clearView ()
	{
		// Erase all edges and vertices
		nvert	= 0;
		nattr	= 0;
				
		// Initialise state variables
		updated		= true;
		
		// Add origin point for axes
		addAttrPoint (AXIS, addVertexNoBB (0.0, 0.0), null, DASHED, Color.RED, null);
		
		// Initialise the Bounding Box 
		if (growbbox)
		{
			mod_bor[0] = Double.MAX_VALUE;
			mod_bor[1] = Double.MAX_VALUE;
			mod_bor[2] = -Double.MAX_VALUE;
			mod_bor[3] = -Double.MAX_VALUE;
		}
	}
	
	protected final int addVertex (double x, double y) 
	{
		if (nvert >= verts.length)
		{
			Model2DCoord[] nv = new Model2DCoord[verts.length + MAXVERT];
			System.arraycopy (verts, 0, nv, 0, verts.length);
			verts = nv;
		}
		if (verts[nvert] == null)
			verts[nvert] = new Model2DCoord ();
		
		verts[nvert].x = (float) x;
		verts[nvert].y = (float) y;
		
		if (growbbox)
		{
			if (mod_bor[0] > x)			mod_bor[0] = x;
			if (mod_bor[2] < x)			mod_bor[2] = x;
			if (mod_bor[1] > y)			mod_bor[1] = y;
			if (mod_bor[3] < y)			mod_bor[3] = y;	
		}

		nvert++;       
		return nvert - 1;
	}

	protected final int addVertexNoBB (double x, double y) 
	{
		if (nvert >= verts.length)
		{
			Model2DCoord[] nv = new Model2DCoord[verts.length + MAXVERT];
			System.arraycopy (verts, 0, nv, 0, verts.length);
			verts = nv;
		}
		if (verts[nvert] == null)
			verts[nvert] = new Model2DCoord ();
		
		verts[nvert].x = (float) x;
		verts[nvert].y = (float) y;
		
		nvert++;       
		return nvert - 1;
	}

	protected final int addAttrPoint (int type, int vertex, String label, int mode, Color color, Object src) 
	{
		int				atndx;
		
		atndx = addAttr (type, label, mode, color);
		attr[atndx].attype = Model2DAttr.ATTR_POINT;
		attr[atndx].vorig = vertex;
		attr[atndx].src		= src;
		
		return atndx;
	}
		
	protected final int addAttrLine (int vorig, int vdest, int mode, Color color)
	{
		int atndx = addAttr (LINE, null, mode, color);
		attr[atndx].attype = Model2DAttr.ATTR_LINE;
		attr[atndx].vorig = vorig;
		attr[atndx].vdest = vdest;
		
		return atndx;
	}
	
	protected final int addAttrArc (int type, int vorig, int vdest, int start, int arc, int mode, Color color) 
	{
		int				atndx;
		
		atndx = addAttr (type, null, mode, color);
		
		if ((attr[atndx].vset == null) || (attr[atndx].vset.length < 2))
			attr[atndx].vset = new int[2];
		
		attr[atndx].attype = Model2DAttr.ATTR_LINE;
		attr[atndx].vorig = vorig;
		attr[atndx].vdest = vdest;
		attr[atndx].vset[0] = start;
		attr[atndx].vset[1] = arc;
		return atndx;
	}
	
	protected final int addAttr (int type, String label, int mode, Color color) 
	{
		if (nattr >= attr.length)
		{
			Model2DAttr[] nv = new Model2DAttr[attr.length + MAXATTR];
			System.arraycopy (attr, 0, nv, 0, attr.length);
			attr = nv;
		}
		if (attr[nattr] == null)		
			attr[nattr] = new Model2DAttr ();
		
		attr[nattr].color	= color;
		attr[nattr].type	= type;
		attr[nattr].mode	= mode;
		attr[nattr].label	= label;
		
		updated		= true;
		
		nattr ++;
		return nattr-1;
	}
	
	public void addRawText (Point2 pt, String text, Color color) 
	{		
		addAttrPoint (TEXT, addVertex (pt.x, pt.y), text, PLAIN, color, null);	
	}
	
	public void addRawText (double x, double y, String text, Color color) 
	{		
		addAttrPoint (TEXT, addVertex (x, y), text, PLAIN, color, null);	
	}
	
	public void addRawText (double x, double y, String text, int mode, Color color) 
	{		
		addAttrPoint (TEXT, addVertex (x, y), text, mode, color, null);	
	}
	
	public void addRawLabel (Point2 pt, String text, int mode, Color color) 
	{		
		addAttrPoint (LABEL, addVertex (pt.x, pt.y), text, mode, color, null);	
	}
	
	public void addRawLabel (double x, double y, String text, int mode, Color color) 
	{		
		addAttrPoint (LABEL, addVertex (x, y), text, mode, color, null);	
	}
	
	public void addRawPoint (Point2 pt, int mode, Color color) 
	{
		addAttrPoint (POINT, addVertex (pt.x, pt.y), null, mode, color, null);	
	}
	
	public void addRawPoint (double x, double y, int mode, Color color) 
	{	
		addAttrPoint (POINT, addVertex (x, y), null, mode, color, null);	
	}
	
	public void addRawDot (Point2 pt, int mode, Color color) 
	{
		addAttrPoint (DOT, addVertex (pt.x, pt.y), null, mode, color, null);	
	}
	
	public void addRawDot (double x, double y, int mode, Color color) 
	{	
		addAttrPoint (DOT, addVertex (x, y), null, mode, color, null);	
	}
	
	public void addRawIcon (double x, double y, String label, URL icon, int mode, Color color) 
	{	
		addAttrPoint (ICON, addVertex (x, y), label, mode, color, icon);	
	}
	
	public void addRawIcon (Point2 pt, String label, URL icon, int mode, Color color) 
	{	
		addAttrPoint (ICON, addVertex (pt.x, pt.y), label, mode, color, icon);	
	}
	
	public void addRawIcon (double x, double y, String label, String icon, int mode, Color color) 
	{	
		addAttrPoint (ICON, addVertex (x, y), label, mode, color, icon);	
	}
	
	public void addRawIcon (Point2 pt, String label, String icon, int mode, Color color) 
	{	
		addAttrPoint (ICON, addVertex (pt.x, pt.y), label, mode, color, icon);	
	}
	
	public void addRawAxis (double x, double y, int mode, Color color) 
	{	
		int atndx = addAttr (AXIS, null, mode, color);
		attr[atndx].attype = Model2DAttr.ATTR_LINE;
		attr[atndx].vorig = addVertexNoBB (x, y);
		attr[atndx].vdest = addVertexNoBB (x, y);
	}
	
	public void addRawIcon (double x, double y, String label, Image icon, int mode, Color color) 
	{	
		addAttrPoint (ICON, addVertex (x, y), label, mode, color, icon);	
	}
	
	public void addRawIcon (Point2 pt, String label, Image icon, int mode, Color color) 
	{	
		addAttrPoint (ICON, addVertex (pt.x, pt.y), label, mode, color, icon);	
	}
	
	public final void addRawLine (Line2 line, Color color) 
	{
		int atndx = addAttr (LINE, null, PLAIN, color);
		attr[atndx].attype = Model2DAttr.ATTR_LINE;
		attr[atndx].vorig = addVertex (line.orig.x, line.orig.y);
		attr[atndx].vdest = addVertex (line.dest.x, line.dest.y);
	}
	
	public final void addRawLine (Point2 orig, Point2 dest, Color color) 
	{	
		int atndx = addAttr (LINE, null, PLAIN, color);
		attr[atndx].attype = Model2DAttr.ATTR_LINE;
		attr[atndx].vorig = addVertex (orig.x, orig.y);
		attr[atndx].vdest = addVertex (dest.x, dest.y);
	}
	
	public final void addRawLine (double xa, double ya, double xb, double yb, Color color) 
	{	
		int atndx = addAttr (LINE, null, PLAIN, color);
		attr[atndx].attype = Model2DAttr.ATTR_LINE;
		attr[atndx].vorig = addVertex (xa, ya);
		attr[atndx].vdest = addVertex (xb, yb);
	}
	
	public final void addRawLine (Line2 line, int mode, Color color) 
	{
		int atndx = addAttr (LINE, null, mode, color);
		attr[atndx].attype = Model2DAttr.ATTR_LINE;
		attr[atndx].vorig = addVertex (line.orig.x, line.orig.y);
		attr[atndx].vdest = addVertex (line.dest.x, line.dest.y);
	}
	
	public final void addRawLine (Point2 orig, Point2 dest, int mode, Color color) 
	{	
		int atndx = addAttr (LINE, null, mode, color);
		attr[atndx].attype = Model2DAttr.ATTR_LINE;
		attr[atndx].vorig = addVertex (orig.x, orig.y);
		attr[atndx].vdest = addVertex (dest.x, dest.y);
	}
	
	public final void addRawLine (double xa, double ya, double xb, double yb, int mode, Color color) 
	{	
		int atndx = addAttr (LINE, null, mode, color);
		attr[atndx].attype = Model2DAttr.ATTR_LINE;
		attr[atndx].vorig = addVertex (xa, ya);
		attr[atndx].vdest = addVertex (xb, yb);
	}
	
	public void addRawRotLine (Line2 line, double alpha, Color color) 
	{
		addRawRotLine (line.orig.x, line.orig.y, line.dest.x, line.dest.y, alpha, PLAIN, color);
	}
	
	public void addRawRotLine (Line2 line, double alpha, int mode, Color color) 
	{
		addRawRotLine (line.orig.x, line.orig.y, line.dest.x, line.dest.y, alpha, mode, color);
	}
	
	public void addRawRotLine (double xa, double ya, double xb, double yb, double alpha, int mode, Color color) 
	{
		double		x1, y1;
		double		x2, y2;
		double		l1, l2, r1, r2;
		
		l1	= Math.sqrt (xa * xa + ya * ya);
		r1	= Math.atan2 (ya, xa) + alpha;
		l2	= Math.sqrt (xb * xb + yb * yb);
		r2	= Math.atan2 (yb, xb) + alpha;
		
		x1 = l1 * Math.cos (r1);
		y1 = l1 * Math.sin (r1);
		x2 = l2 * Math.cos (r2);
		y2 = l2 * Math.sin (r2);	

		int atndx = addAttr (LINE, null, PLAIN, color);
		attr[atndx].attype = Model2DAttr.ATTR_LINE;
		attr[atndx].vorig = addVertex (x1, y1);
		attr[atndx].vdest = addVertex (x2, y2);
	}
	
	public void addRawTransRotLine (Line2 line, double rx, double ry, double alpha, Color color) 
	{
		addRawTransRotLine (line.orig.x, line.orig.y, line.dest.x, line.dest.y, rx, ry, alpha, color);
	}
	
	public void addRawTransRotLine (double xa, double ya, double xb, double yb, double rx, double ry, double alpha, Color color) 
	{
		double		x1, y1;
		double		x2, y2;
		double		l1, l2, r1, r2;
		
		l1	= Math.sqrt (xa * xa + ya * ya);
		r1	= Math.atan2 (ya, xa) + alpha;
		l2	= Math.sqrt (xb * xb + yb * yb);
		r2	= Math.atan2 (yb, xb) + alpha;
		
		x1 = rx + l1 * Math.cos (r1);
		y1 = ry + l1 * Math.sin (r1);
		x2 = rx + l2 * Math.cos (r2);
		y2 = ry + l2 * Math.sin (r2);	
		
		int atndx = addAttr (LINE, null, PLAIN, color);
		attr[atndx].attype = Model2DAttr.ATTR_LINE;
		attr[atndx].vorig = addVertex (x1, y1);
		attr[atndx].vdest = addVertex (x2, y2);
	}
	
	public void addRawRotSegment (double xa, double ya, double xb, double yb, double width, double alpha, Color color) 
	{
		double		x1, y1, x2, y2;
		double		x3, y3, x4, y4;
		double		l1, l2, r1, r2;
		double		da, db, a;
		
		// NOTE: (xa,ya)-(xb,yb) are the segment end points
		
		// Compute parallel lines from global coordinates
		da	= Math.sqrt (xa * xa + ya * ya);
		db	= Math.sqrt (xb * xb + yb * yb);
		if (db < da)
			a	= Math.atan2 ((ya - yb), (xa - xb));
		else
			a	= Math.atan2 ((yb - ya), (xb - xa));	
		
		x1	= xa + width * Math.cos (a - Angles.PI05);		
		y1	= ya + width * Math.sin (a - Angles.PI05);		
		x2	= xb + width * Math.cos (a - Angles.PI05);		
		y2	= yb + width * Math.sin (a - Angles.PI05);		
		
		x3	= xa + width * Math.cos (a + Angles.PI05);		
		y3	= ya + width * Math.sin (a + Angles.PI05);		
		x4	= xb + width * Math.cos (a + Angles.PI05);		
		y4	= yb + width * Math.sin (a + Angles.PI05);		
		
		// Convert segments to local coordinates and then rotate
		l1	= Math.sqrt (x1 * x1 + y1 * y1);
		r1	= Math.atan2 (y1, x1) + alpha;
		l2	= Math.sqrt (x2 * x2 + y2 * y2);
		r2	= Math.atan2 (y2, x2) + alpha;
		
		x1 = l1 * Math.cos (r1);
		y1 = l1 * Math.sin (r1);
		x2 = l2 * Math.cos (r2);
		y2 = l2 * Math.sin (r2) ;	
		
		l1	= Math.sqrt (x3 * x3 + y3 * y3);
		r1	= Math.atan2 (y3, x3) + alpha;
		l2	= Math.sqrt (x4 * x4 + y4 * y4);
		r2	= Math.atan2 (y4, x4) + alpha;
		
		x3 = l1 * Math.cos (r1);
		y3 = l1 * Math.sin (r1);
		x4 = l2 * Math.cos (r2);
		y4 = l2 * Math.sin (r2);	
		
		addAttrLine (addVertex (x1, y1), addVertex (x2, y2), PLAIN, color);
		addAttrLine (addVertex (x3, y3), addVertex (x4, y4), PLAIN, color);
		addAttrLine (addVertex (x1, y1), addVertex (x3, y3), PLAIN, color);
		addAttrLine (addVertex (x2, y2), addVertex (x4, y4), PLAIN, color);
	}
	
	public void addRawPolarLine (double xa, double ya, double len, double phi, int mode, Color color) 
	{
		double		xx, yy;
		
		xx	= xa + len * Math.cos (phi);
		yy	= ya + len * Math.sin (phi);		
		int atndx = addAttr (LINE, null, PLAIN, color);
		attr[atndx].attype = Model2DAttr.ATTR_LINE;
		attr[atndx].vorig = addVertex (xa, ya);
		attr[atndx].vdest = addVertex (xx, yy);
	}
	
	public void addRawBox (double xa, double ya, double xb, double yb, Color color) 
	{
		int atndx = addAttr (BOX, null, PLAIN, color);
		attr[atndx].attype = Model2DAttr.ATTR_LINE;
		attr[atndx].vorig = addVertex (xa, ya);
		attr[atndx].vdest = addVertex (xb, yb);
	}
	
	public void addRawBox (double xa, double ya, double xb, double yb, int mode, Color color) 
	{
		int atndx = addAttr (BOX, null, mode, color);
		attr[atndx].attype = Model2DAttr.ATTR_LINE;
		attr[atndx].vorig = addVertex (xa, ya);
		attr[atndx].vdest = addVertex (xb, yb);
	}
	
	// Gira (alpha grados) un rectangulo con limites max y min (xa,ya,xb,yb) respecto a su centro
	public void addRawRotBox (double xa, double ya, double xb, double yb, double alpha, int mode, Color color) 
	{
		double		xi, yi, xf, yf;
		double		x, y;
		double		w, h, l;
		double		p1, p2, p3, p4;
		double		x1, y1, x2, y2;
		double		x3, y3, x4, y4;
		
		if (xa > xb)		{ xi = xb; xf = xa; }
		else				{ xi = xa; xf = xb; }
		if (ya > yb)		{ yi = yb; yf = ya; }
		else				{ yi = ya; yf = yb; }
		
		w	= (xf - xi) * 0.5;
		h	= (yf - yi) * 0.5;
		l	= Math.sqrt (w * w + h * h);
		x	= xi + w;
		y	= yi + h;
		
		p1	= Math.atan2 (xf-x, yf-y) + alpha;
		x1	= x + l * Math.cos (p1);
		y1	= y + l * Math.sin (p1);

		p2	= Math.atan2 (xf-x, yi-y) + alpha;
		x2	= x + l * Math.cos (p2);
		y2	= y + l * Math.sin (p2);

		p3	= Math.atan2 (xi-x, yf-y) + alpha;
		x3	= x + l * Math.cos (p3);
		y3	= y + l * Math.sin (p3);

		p4	= Math.atan2 (xi-x, yi-y) + alpha;
		x4	= x + l * Math.cos (p4);
		y4	= y + l * Math.sin (p4);

		addAttrLine (addVertex (x1, y1), addVertex (x2, y2), PLAIN, color);
		addAttrLine (addVertex (x3, y3), addVertex (x4, y4), PLAIN, color);
		addAttrLine (addVertex (x1, y1), addVertex (x3, y3), PLAIN, color);
		addAttrLine (addVertex (x2, y2), addVertex (x4, y4), PLAIN, color);
	}
	
	// Gira (alpha grados) un rectangulo con sus limites max y min (xa,ya,xb,yb) respecto al origen y lo desplaza a la posicion tx,ty
	public void addRawRotTransBox (double xa, double ya, double xb, double yb, double tx, double ty, double alpha, int mode, Color color) 
	{
	    Point2 p1 = rotTrans(new Point2(xa, ya), tx, ty, alpha);
	    Point2 p2 = rotTrans(new Point2(xa, yb), tx, ty, alpha);
	    Point2 p3 = rotTrans(new Point2(xb, ya), tx, ty, alpha);
	    Point2 p4 = rotTrans(new Point2(xb, yb), tx, ty, alpha);
	    
		addAttrLine (addVertex (p1.x, p1.y), addVertex (p2.x, p2.y), PLAIN, color);
		addAttrLine (addVertex (p1.x, p1.y), addVertex (p3.x, p3.y), PLAIN, color);
		addAttrLine (addVertex (p2.x, p2.y), addVertex (p4.x, p4.y), PLAIN, color);
		addAttrLine (addVertex (p3.x, p3.y), addVertex (p4.x, p4.y), PLAIN, color);
	}
	
	public void addRawCircle (Point2 pt, double radius, Color color) 
	{
		addRawCircle (pt.x, pt.y, radius, PLAIN, color);
	}
	
	public void addRawCircle (double xa, double ya, double radius, Color color) 
	{
		addRawCircle (xa, ya, radius, PLAIN, color);
	}
	
	public void addRawCircle (double xa, double ya, double radius, int mode, Color color) 
	{
		double		x, y;
		double		xx, yy;

		x	= xa - radius;
		y	= ya - radius;
		xx	= xa + radius;
		yy	= ya + radius;
			
		int atndx = addAttr (CIRCLE, null, PLAIN, color);
		attr[atndx].attype = Model2DAttr.ATTR_LINE;
		attr[atndx].vorig = addVertex (x, y);
		attr[atndx].vdest = addVertex (xx, yy);
	}
	
	public void addRawArc (double xa, double ya, double radius, double sarc, double earc, int mode, Color color) 
	{
		double		x, y;
		double		xx, yy;
		int			start;
		int			arc;

		x	= xa - radius;
		y	= ya - radius;
		xx	= xa + radius;
		yy	= ya + radius;
		
		start	= (int) Math.round (sarc * Angles.RTOD);
		arc		= (int) Math.round ((earc - sarc) * Angles.RTOD);
			
		addAttrArc (LINE, addVertex (x, y), addVertex (xx, yy), start, arc, mode, color);
	}
	
	public void addRawEllipse (double xa, double ya, double ra, double rb, Color color) 
	{
		double		a, step;
		double		x, y;
		double		xx, yy;
		boolean		first;
		
		first	= true;
		step	= 360.0 / 64.0;
		xx		= 0.0;
		yy		= 0.0;
		for (a = 0.0; a <= 360.0; a += step)
		{
			x = xa + ra * Math.cos (a * Angles.DTOR);
			y = ya + rb * Math.sin (a * Angles.DTOR);
			if (!first)
			{
				int atndx = addAttr (LINE, null, PLAIN, color);
				attr[atndx].attype = Model2DAttr.ATTR_LINE;
				attr[atndx].vorig = addVertex (x, y);
				attr[atndx].vdest = addVertex (xx, yy);

			}
			else
				first = false;
			xx = x;
			yy = y;
		}
	}
	
	public void addRawRotEllipse (double xa, double ya, double ra, double rb, double alpha, Color color) 
	{
		double		x, y;
		double		xx, yy;
		double		mx, my;
		double		a, step;
		boolean		first;
				
		first	= true;
		step	= 360.0 / 64.0;
		xx		= 0.0;
		yy		= 0.0;
		for (a = 0.0; a <= 360.0; a += step)
		{
	        mx = 0.5 * ra * Math.cos (a * Angles.DTOR);
	        my = 0.5 * rb * Math.sin (a * Angles.DTOR);
	        x = xa + mx * Math.cos (alpha) - my * Math.sin (alpha);
	        y = ya + mx * Math.sin (alpha) + my * Math.cos (alpha);
			if (!first)
			{
				int atndx = addAttr (LINE, null, PLAIN, color);
				attr[atndx].attype = Model2DAttr.ATTR_LINE;
				attr[atndx].vorig = addVertex (x, y);
				attr[atndx].vdest = addVertex (xx, yy);
			}
			else
				first = false;
			xx = x;
			yy = y;
		}
	}
		
	public void addRawArrow (Point2 pt1, Point2 pt2, Color color) 
	{
		addRawArrow (pt1.x, pt1.y, pt1.distance (pt2), pt1.angle (pt2), ARROW_SIZ, ARROW_ANG, PLAIN, color);
	}
	
	public void addRawArrow (double xa, double ya, double len, double phi, Color color) 
	{
		addRawArrow (xa, ya, len, phi, ARROW_SIZ, ARROW_ANG, PLAIN, color);
	}
	
	public void addRawArrow (double xa, double ya, double len, double phi, int mode, Color color) 
	{
		addRawArrow (xa, ya, len, phi, ARROW_SIZ, ARROW_ANG, mode, color);
	}
	
	public void addRawArrow (double xa, double ya, double len, double phi, double size, double angle, int mode, Color color) 
	{
		double		xx, yy;
		double		xxi, yyi;
		
		xx	= xa + len * Math.cos (phi);
		yy	= ya + len * Math.sin (phi);		
		addAttrLine (addVertex (xa, ya), addVertex (xx, yy), mode, color);
		
		xxi	= xx + len * size * Math.cos (phi + angle + Math.PI);
		yyi	= yy + len * size * Math.sin (phi + angle + Math.PI);		
		addAttrLine (addVertex (xx, yy), addVertex (xxi, yyi), mode, color);
		
		xxi	= xx + len * size * Math.cos (phi - angle + Math.PI);
		yyi	= yy + len * size * Math.sin (phi - angle + Math.PI);		
		addAttrLine (addVertex (xx, yy), addVertex (xxi, yyi), mode, color);
	}
	
	public void addRawTransRotArrow (double xa, double ya, double len, double phi, double rx, double ry, double alpha, Color color) 
	{
		addRawTransRotArrow (xa, ya, len, phi, rx, ry, alpha, ARROW_SIZ, ARROW_ANG, color);
	}
	
	public void addRawTransRotArrow (double xa, double ya, double len, double phi, double rx, double ry, double alpha, double size, double angle, Color color) 
	{
		double		x, y;
		double		xx, yy;
		double		xxi, yyi;	    	
		double		ll, rr;
		
		ll	= Math.sqrt (xa * xa + ya * ya);
		rr	= Math.atan2 (ya, xa) + alpha;
		
		x	= rx + ll * Math.cos (rr);
		y	= ry + ll * Math.sin (rr);
		
		xx	= x + len * Math.cos (alpha + phi);
		yy	= y + len * Math.sin (alpha + phi);
		addAttrLine (addVertex (x, y), addVertex (xx, yy), PLAIN, color);
		
		xxi	= xx + len * size * Math.cos (alpha + phi + angle + Math.PI);
		yyi	= yy + len * size * Math.sin (alpha + phi + angle + Math.PI);		
		addAttrLine (addVertex (xx, yy), addVertex (xxi, yyi), PLAIN, color);
		
		xxi	= xx + len * size * Math.cos (alpha + phi - angle + Math.PI);
		yyi	= yy + len * size * Math.sin (alpha + phi - angle + Math.PI);		
		addAttrLine (addVertex (xx, yy), addVertex (xxi, yyi), PLAIN, color);
	}
	
	public int addRawPoly (double[] x, double[] y, int mode, Color color) 
	{	
		int			i;
		int[]		vtx;
		int			atndx;
		
		vtx	= new int[x.length];
		for (i = 0; i < x.length; i++)
			vtx[i]	= addVertex (x[i], y[i]);
		
		atndx = addAttr (POLY, null, mode, color);
		attr[atndx].attype = Model2DAttr.ATTR_POLY;
		attr[atndx].vset = vtx;
		
		return atndx;
	}
	
	public void setBB (double minx, double miny, double maxx, double maxy) 
	{
		mod_bor[0] 	= minx;
		mod_bor[1] 	= miny;
		mod_bor[2] 	= maxx;
		mod_bor[3] 	= maxy;
		
		updated		= true;
	}	
	
    static public Point2 rotTrans (Point2 pt, double tx, double ty, double alpha){
        //									| cos	sin		0|
        //	[x',y', w] = |pt.x pt.y 1|	* 	|-sin	cos		0|
        //									|  0	 0		1|
        //
        double x = pt.x()*Math.cos(alpha) - pt.y()*Math.sin(alpha);
        double y = pt.x()*Math.sin(alpha) + pt.y()*Math.cos(alpha);
        return new Point2(x+tx,y+ty);
    }
}