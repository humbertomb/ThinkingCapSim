/*
 * (c) 2003 Juan Pedro Canovas Qui–onero
 * (c) 2004 Humberto Martinez Barbera
 */

package tclib.planning.htopol;

import tclib.utils.graphs.*;

import wucore.utils.geom.*;

public class GNodeSL extends GNode
{
	protected String				father;
	protected Point3				pos;
	protected int				type;
	
	public GNodeSL (String label, String father)
	{
		super (label);		
		
		this.father	= father;
		pos			= new Point3 ();
	}
		
	// Accessors
	public void 		setPosition (Point3 npos)		{ pos.set (npos); }
	public Point3	getPosition ()				{ return pos; }

	public void		setFather (String father)		{ this.father = father; }
	public String	getFather () 				{ return father; }
	public void		setType (int type)			{ this.type = type; }
	public int		getType ()					{ return type; }
}