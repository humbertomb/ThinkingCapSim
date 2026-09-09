/*
 * Created on 17-oct-2004
 *
 * To change the template for this generated file go to
 * Window - Preferences - Java - Code Generation - Code and Comments
 */
package tclib.utils.petrinets.visualization;

import tclib.utils.petrinets.*;

import wucore.widgets.*;

/**
 * @author Humberto Martinez Barbera
 *
 * To change the template for this generated type comment go to
 * Window - Preferences - Java - Code Generation - Code and Comments
 */
public class PetriNet2D 
{	
	// GUI control
	protected Model2D					model;
	
	public PetriNet2D (Model2D model)
	{
		this.model		= model;
	}
		
	/* Instance methods */	
	public void update (PetriNet pn) 
	{
		int			i;
		
		model.clearView();
		
		if (pn == null)			return;
		
		// Draw PN nodes
		for (i = 0; i < pn.numberOfNodes(); i++) 
			pn.getNode (i).draw (model, pn);		

		// Draw PN transitions
		for (i = 0; i < pn.numberOfTransitions(); i++)
			pn.getTransition (i).draw (model, pn);

		// Draw PN edges
		for (i = 0; i < pn.numberOfEdges(); i++) 
			pn.getEdge (i).draw (model, pn);
	}
	
	public void relax (PetriNet pn)
	{
		int			i, j;
		
		for (i = 0 ; i < pn.numberOfEdges() ; i++)
		{
			PNObject		from, to;
			PNEdge		e = pn.getEdge (i);
			
			if (e.getTFrom() == PNEdge.NODE)
			{
				from		= pn.getNode (e.getIFrom ());
				to		= pn.getTransition (e.getITo ());
			}
			else
			{
				from		= pn.getTransition (e.getIFrom ());
				to		= pn.getNode (e.getITo ());
			}
			
			double vx = to.x () - from.x ();
			double vy = to.y () - from.y ();
			double len = Math.sqrt(vx * vx + vy * vy);
//			double f = (edges[i].len - len) / (len * 5.0);
			double f = (50.0 - len) / (len * 3.0); // 5.0);
			double dx = f * vx;
			double dy = f * vy;
			
			to.dx (to.dx () + dx);
			to.dy (to.dy () + dy);
			from.dy (from.dx () - dx);
			from.dy (from.dy () - dy);
		}
		
		PNObject	[]		nodes;
		
		nodes	= new PNObject[pn.numberOfNodes () + pn.numberOfTransitions ()];
		for (i = 0; i < pn.numberOfNodes (); i++)
			nodes[i]		= pn.getNode (i);
		for (j = 0; j < pn.numberOfTransitions (); j++)
			nodes[i+j]	= pn.getTransition (j);
		
		for (i = 0 ; i < nodes.length ; i++)
		{
			PNObject		n1 = nodes[i];
			double		dx = 0;
			double		dy = 0;
			
			for (j = 0 ; j < nodes.length ; j++) 
			{
				if (i == j) 			continue;
				
				PNObject n2 = nodes[j];
				double vx = n1.x () - n2.x ();
				double vy = n1.y () - n2.y ();
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
				n1.dx (n1.dx () + dx / dlen);
				n1.dy (n1.dy () + dy / dlen);
			}
		}
		
		for (i = 0 ; i < nodes.length ; i++)
		{
			PNObject		n = nodes[i]; 
			
			n.x (n.x () + n.dx ());
			n.y	(n.y () + n.dy ());
			
			if (n.x () < model.getMinX ())
				n.x (model.getMinX ());
			else if (n.x () > model.getMaxX ())
				n.x (model.getMaxX ());
			
			if (n.y () < model.getMinY ())
				n.y (model.getMinY ());
			else if (n.y () > model.getMaxY ())
				n.y (model.getMaxY ());

			n.dx (n.dx () / 2);
			n.dy (n.dy () / 2);
		}
		
		update (pn);
	}
}
