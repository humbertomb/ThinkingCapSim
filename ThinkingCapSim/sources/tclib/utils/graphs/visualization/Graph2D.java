/*
 * (c) 2003 Jose Antonio Marin Meseguer
 * (c) 2004 Humberto Martinez Barbera
 */

package tclib.utils.graphs.visualization;

import java.awt.*;

import tclib.utils.graphs.*;

import wucore.utils.math.*;
import wucore.widgets.*;

/**
 * @author Humberto Martinez Barbera
 *
 * To change the template for this generated type comment go to
 * Window - Preferences - Java - Code Generation - Code and Comments
 */
public class Graph2D 
{
	static public final double			NODERADIUS	= 5.0;
	static public final double			NODEGAP		= 25.0;

	public static final double			ARROW_ANG	= 10.0 * Angles.DTOR;		// degrees
	public static final double			ARROW_SIZ	= 0.1;					// length percentage
	
	// Graph model (internal representation)
	protected int 						nnodes;
	protected G2DNode					nodes[];
	protected int						nedges;
	protected G2DEdge					edges[];

	// GUI control
	protected Model2D					model;
	protected boolean					drawwgts		= true;

	public Graph2D (Model2D model)
	{
		this.model		= model;
		
		nnodes			= 0;
		nedges			= 0;
	}
		
	// Accessors
	public void		drawWeights (boolean draw)	{ drawwgts = draw; }
	
	/* Instance methods */
	public int findNode (double x, double y)
	{
		int			i, k;
		double		min, dst;
		
		k	= -1;
		min	= Double.MAX_VALUE;
		for (i = 0; i < nnodes; i++)
		{
			dst	= Math.sqrt ((x - nodes[i].x) * (x - nodes[i].x) + (y - nodes[i].y) * (y - nodes[i].y));
			if (dst < min)
			{
				min	= dst;
				k	= i;
			}
		}
			
		return k;
	}
	
	public void fixNode (int i, boolean fix)
	{
		if ((i < 0) || (i >= nnodes))				return;
		
		nodes[i].fixed = fix;
	}
	
	public void moveNode (int i, double x, double y)
	{
		if ((i < 0) || (i >= nnodes))				return;
		
		nodes[i].x = x;
		nodes[i].y = y;
	}
	
	public void update (Graph graph)
	{
		int			i,j;
		int			factor, sign, scount;
		double		xd, yd;
		boolean		even, bsign;
		GNode			node;

		// Initialise component's canvas
		model.clearView ();	

		if (graph == null || graph.numNodes() == 0) return;
		
		// Set up internal nodes
		factor	= 1;
		sign		= 1;
		scount	= 0;
		even		= false;
		bsign	= false;
		
		xd		= 10.0;
		yd		= 10.0;
		nnodes	= graph.numNodes();
		nodes	= new G2DNode[nnodes];
		for (i = 0; i < graph.numNodes(); i++)
		{
			yd += NODEGAP*factor;
			scount++;
			if (scount == sign)
			{
				scount = 0;
				if (bsign)
					sign++;
				bsign = !bsign;				
				factor *= -1;
			}	
			if (even) factor /= 2;
			else 
			{
				factor *= 2;
				xd += NODEGAP;
			}
			even = !even;
			
			node				= graph.getNode (i);

			nodes[i]			= new G2DNode ();
			nodes[i].x		= xd;
			nodes[i].y		= yd;
			nodes[i].color	= Color.RED;
			nodes[i].label	= node.getLabel ();
		}		

		// Set up internal arcs
		nedges		= 0;
		edges		= new G2DEdge[100];
		for (i = 0; i < graph.numNodes(); i++)
		{
			node		= graph.getNode (i);
			
			// Draw arcs
			for (j = 0; j < node.nList(); j++)
			{
				edges[nedges]		= new G2DEdge ();
				edges[nedges].from	= i;
				edges[nedges].to		= node.getList (j);
				edges[nedges].len	= 3.0 * NODERADIUS;
				edges[nedges].wgt	= node.getPeso (j);
				
				nedges ++;
			}	
		}
		
		drawGraph ();
	}
	
	public void relax ()
	{
		int			i, j;
		
		for (i = 0 ; i < nedges ; i++)
		{
			G2DEdge e = edges[i];
			double vx = nodes[e.to].x - nodes[e.from].x;
			double vy = nodes[e.to].y - nodes[e.from].y;
			double len = Math.sqrt(vx * vx + vy * vy);
			double f = (edges[i].len - len) / (len * 5.0);
			double dx = f * vx;
			double dy = f * vy;
			
			nodes[e.to].dx += dx;
			nodes[e.to].dy += dy;
			nodes[e.from].dx += -dx;
			nodes[e.from].dy += -dy;
		}
		
		for (i = 0 ; i < nnodes ; i++)
		{
			G2DNode		n1 = nodes[i];
			double		dx = 0;
			double		dy = 0;
			
			for (j = 0 ; j < nnodes ; j++) 
			{
				if (i == j) 			continue;
				
				G2DNode n2 = nodes[j];
				double vx = n1.x - n2.x;
				double vy = n1.y - n2.y;
				double len = vx * vx + vy * vy;

				if (len != 0.0)
				{
					dx	+= vx / len;
					dy	+= vy / len;
				}
			}
			double dlen = dx * dx + dy * dy;
			if (dlen > 0) 
			{
				dlen = Math.sqrt(dlen) / 2;
				n1.dx += dx / dlen;
				n1.dy += dy / dlen;
			}
		}
		
		for (i = 0 ; i < nnodes ; i++)
		{
			G2DNode		n = nodes[i];
			
			if (!n.fixed)
			{
				n.x	+= n.dx;
				n.y	+= n.dy;
				
				if (n.x < model.getMinX ())
					n.x = model.getMinX ();
				else if (n.x > model.getMaxX ())
					n.x = model.getMaxX ();
				
				if (n.y < model.getMinY ())
					n.y = model.getMinY ();
				else if (n.y > model.getMaxY ())
					n.y = model.getMaxY ();
			}
			n.dx /= 2;
			n.dy /= 2;
		}
		
		drawGraph ();
	}
	
	public void drawGraph ()
	{
		int			i;

		// Initialise component's canvas
		model.clearView ();	

		// Draw nodes
		for (i = 0; i < nnodes; i++)
		{
			G2DNode			node = nodes[i];
			
			model.addRawCircle (node.x, node.y, NODERADIUS, Model2D.FILLED, node.color);
			model.addRawCircle (node.x, node.y, NODERADIUS, Color.BLACK);
			model.addRawText (node.x+NODERADIUS*1.3, node.y,node.label, node.color);		
		}
		
		// Draw arcs
		for (i = 0 ; i < nedges ; i++) 
		{
			double		xo, yo, xd, yd;
			double		dx, dy, xoff, yoff, len;
			double		rho, phi;
		    G2DEdge		e = edges[i];
		    
		    dx	= nodes[e.to].x - nodes[e.from].x;
		    dy	= nodes[e.to].y - nodes[e.from].y;
		    len	= Math.sqrt (dx * dx + dy * dy);		    
		    xoff	= NODERADIUS * dx / len;
		    yoff	= NODERADIUS * dy / len;
		    
		    xo	= nodes[e.from].x + xoff;
		    yo	= nodes[e.from].y + yoff;	    
		    xd	= nodes[e.to].x - xoff;
		    yd	= nodes[e.to].y - yoff;		    
		    
			rho	= Math.sqrt (Math.pow (xd-xo, 2.0) + Math.pow (yd-yo, 2.0));
			phi	= Math.atan2 (yd-yo, xd-xo);

			model.addRawArrow (xo, yo, rho, phi, ARROW_SIZ, ARROW_ANG, Model2D.PLAIN, Color.BLACK);

			if (drawwgts)
			{
				xo	+= (xd - xo) * 0.8;
				yo	+= (yd - yo) * 0.8;				
				model.addRawText (xo, yo, String.valueOf(e.wgt), Color.CYAN);
			}
		}
	}
}
