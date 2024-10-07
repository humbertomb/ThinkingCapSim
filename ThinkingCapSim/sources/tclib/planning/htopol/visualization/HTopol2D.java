/*
 * Created on 01-oct-2004
 *
 * To change the template for this generated file go to
 * Window - Preferences - Java - Code Generation - Code and Comments
 */
package tclib.planning.htopol.visualization;

import java.awt.*;

import tclib.planning.htopol.*;
import tclib.utils.graphs.*;
import tclib.utils.graphs.visualization.*;

import tc.shared.world.*;

import wucore.utils.geom.*;
import wucore.widgets.*;

/**
 * @author Humberto Martinez Barbera
 *
 * To change the template for this generated type comment go to
 * Window - Preferences - Java - Code Generation - Code and Comments
 */
public class HTopol2D extends Graph2D
{
	static public final double			RADIUS		= 0.5;
	
	// HUD's labels
	static public final int				H_ZONE		= 0;

	public HTopol2D (Model2D model)
	{
		super (model);
		
//		// Add support for HUD objects
//		model.hud_n			= 1;
//		model.hud_x[H_ZONE]	= 15;
//		model.hud_y[H_ZONE]	= 15;
	}
		
	/* Instance methods */
	public void update (Graph graph, String zone, boolean metric)
	{
		if (!metric)
		{
			super.update (graph);
//			model.hud_label[H_ZONE] = zone;
		}
		else
			this.update (graph, zone);
	}

	public void update (Graph graph, String zone)
	{
		int			i, j;
		GNodeSL		node;
		Point3		pos;
		
		// Initialise component's canvas
		model.clearView ();	
//		model.hud_label[H_ZONE] = zone;

		if (graph == null || graph.numNodes() == 0) return;
		
		// Set up internal nodes
		nnodes	= graph.numNodes();
		nodes	= new G2DNode[nnodes];
		for (i = 0; i < graph.numNodes(); i++)
		{
			node		= (GNodeSL) graph.getNode (i);
			pos		= node.getPosition ();

			nodes[i]			= new G2DNode ();
			nodes[i].x		= pos.x ();
			nodes[i].y		= pos.y ();
			nodes[i].type	= node.getType ();
			nodes[i].color	= Color.RED;
			nodes[i].label	= node.getLabel ();
		}		

		// Set up internal arcs
		nedges		= 0;
		edges		= new G2DEdge[100];
		for (i = 0; i < graph.numNodes(); i++)
		{
			node		= (GNodeSL) graph.getNode (i);

			for (j = 0; j < node.nList(); j++)
			{
			    edges[nedges]		= new G2DEdge ();
				edges[nedges].from	= i;
				edges[nedges].to		= node.getList (j);
				edges[nedges].len	= 1.25 * RADIUS;
				edges[nedges].wgt	= node.getPeso (j);
				
				nedges ++;
			}	
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
			Color		color;
			
			switch (node.type)
			{
			case World.DOOR:			color	= Color.BLUE;		break;			
			case World.DOCK:			color	= Color.GREEN.darker();	break;				
			case World.WP:
			default:					color	= Color.RED;		
			}
			
			model.addRawCircle (node.x, node.y, RADIUS, Model2D.FILLED, color);
			model.addRawCircle (node.x, node.y, RADIUS, Color.BLACK);
			model.addRawText (node.x+RADIUS*1.3, node.y+RADIUS*1.3, node.label, color);		
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
		    xoff	= RADIUS * dx / len;
		    yoff	= RADIUS * dy / len;
		    
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
