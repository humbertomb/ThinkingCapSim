/*
 * (c) 1997-2002 Humberto Martinez
 *				 Juan Pedro Canovas Qui�onero
 */

package tc.gui.visualization;

import java.awt.*;
import java.awt.geom.*;

import tc.vrobot.*;
import tclib.utils.fusion.*;
import tc.coord.*;
import tc.modules.*;
import tc.shared.lps.lpo.*;
import tc.shared.world.*;

import devices.pos.*;
import wucore.widgets.*;
import wucore.utils.geom.*;
import wucore.utils.color.*;

public class World2D extends Object2D
{
	// Icon properties
	static public final double			T_WIDTH		= 0.25;	// Trace icon width (m)
	static public final double			T_LENGHT		= 0.5;	// Trace icon lenght (m)
	
	// LPS HUD's labels
	public static final int				H_ZONE		= 0;
	
	// World and robot features
	protected RobotList					robots;
	protected World						map;
	protected RobotDesc					rdesc;
	protected FusionDesc					fdesc;
	
	// Behaviour of the widget
	protected boolean					onplace;
	protected boolean					simulated;
	
	// Configuration of the layers to be drawn
	protected boolean					drawwarning;			// Draw the warning area
	protected boolean					drawdanger;			// Draw the bumper detection zone
	protected boolean					drawsensors;			// Draw the current values of the sensors
	protected boolean					drawartifacts;		// Draw artifacts geenrated by some algorithms
	protected boolean					drawlpos;			// Draw perceived objects (LPOs)
	protected boolean					drawlabels;			// Draw docks and wp's id label
	
	// Configuration of the widgets drawing procedures
	protected int						T_FACTOR		= 5;		// Plot a trace out of this number
	
	/* Constructors */
	protected World2D ()
	{
	}
	
	public World2D (Model2D model, String mapname)
	{
		this.initialise (model, null, null, mapname);
	}
	
	public World2D (Model2D model, RobotDesc rdesc) 
	{
		this.initialise (model, rdesc, null,  null);
	}
	
	public World2D (Model2D model, RobotDesc rdesc, World map) 
	{
		this.initialise (model, rdesc, null, null);
		this.setmap	(map);
	}
	
	public World2D (Model2D model, World map)
	{
		this.initialise (model, null, null, null);
		this.setmap (map);
	}
		
	/* Accesors */
	public final void		drawwarning (boolean b)			{ drawwarning = b; }
	public final void		drawdanger (boolean b)			{ drawdanger = b; }
	public final void		drawsensors (boolean b)			{ drawsensors = b; }
	public final void		drawartifacts (boolean b)			{ drawartifacts = b; }
	public final void		drawlpos (boolean b)				{ drawlpos = b; }
	public final void		drawlabels (boolean b)			{ drawlabels = b; }
	
	public final void		onplace (boolean b)				{ onplace = b; }
	public final void		setmap (World w)					{ map = w; }
	public final void		simulated (boolean b)				{ simulated = b; }
	
	/* Instance methods */
	protected void initialise (Model2D model, RobotDesc rdesc, FusionDesc fdesc, String mapname) 
	{
		super.initialise (model);
		
		this.rdesc			= rdesc;
		this.fdesc			= fdesc;
		this.drawwarning		= true;
		this.drawdanger		= true;
		this.drawsensors		= true;
		this.onplace			= false;
		this.simulated		= true;
		this.drawartifacts	= false;
		this.drawlpos		= false;
		this.drawlabels		= true;
		
		// Load real map if available
		if (mapname != null)
		{
			try
			{
				map		= new World (mapname);	
			} catch (Exception e)
			{
				System.out.println ("Exception creating World: " + e);
				map = null;
			}
		}	
	}
		
	public void saveWorld (String filename)
	{
		if (map == null) return;
		try
		{
			map.toFile (filename);
		} catch (Exception e)
		{
			System.out.println ("World2D: exception saving world->"+e);
		}
	}
	
	//*************************
	// 		DRAW WORLD	
	//*************************
	
	public void drawWorld ()
	{    	
		if (map == null)					return;
		
		drawStart ();
		drawMap ();
		drawObjects();
		drawPath ();
		drawDocks ();
		drawDoors ();
		drawWayPoints ();
		drawZones ();
		drawFAreas ();
		drawStripBeacons ();
		drawCilindricalBeacons ();
	}
	
	public void drawStart () 
	{
		model.addRawCircle (map.start_x (), map.start_y (), map.G_RADIUS, Color.GREEN);
		model.addRawArrow (map.start_x (), map.start_y (), map.G_LENGHT, map.start_a(), Color.GREEN);
	}
	
	public void drawMap () 
	{
		int i;
		double x1,y1,x2,y2;
		
		if (map.walls ().n () <= 0)			return;
		
		for (i = 0; i < map.walls ().n (); i++)
		{
			x1	= map.walls ().at (i).edge.orig ().x ();
			y1 	= map.walls ().at (i).edge.orig ().y ();
			x2 	= map.walls ().at (i).edge.dest ().x ();
			y2 	= map.walls ().at (i).edge.dest ().y (); 
			model.addRawLine (x1, y1, x2, y2, Color.BLACK);
		}
		
		boundary (map.walls ().minx (), map.walls ().miny (), map.walls ().maxx (), map.walls ().maxy ());
	}
	
	public void drawObjects () 
	{
		int			i, j;
		double		x1, y1, x2, y2;
		WMObject		object;
		Line2[]		icon;
		
		for (i = 0; i < map.objects ().n (); i++)
		{
			object	= map.objects ().at (i);
			icon		= object.icon;
			for (j = 0; j < icon.length; j++)
			{
				x1	= icon[j].orig ().x ();
				y1 	= icon[j].orig ().y ();
				x2 	= icon[j].dest ().x ();
				y2 	= icon[j].dest ().y ();
				model.addRawLine (x1, y1, x2, y2, ColorTool.fromWColorToColor(object.color));
				
				//model.addRawLine (x1, y1, x2, y2, object.color);
				//model.addRawTransRotLine(new Line2(x1, y1, x2, y2),object.pos.x(),object.pos.y(),object.a, object.color);
			}
		}
	}
	
	public void drawPath () 
	{
		int i;
		boolean	 first;
		double x1,y1,x2,y2;
		
		first	= true;
		x1		= 0.0;
		y1		= 0.0;
		
		for (i = 0; i < map.path().n(); i++)
		{
			x2	= map.path().at(i).x ();
			y2 	= map.path().at(i).y ();
			
			if (!first)
				model.addRawLine (x1, y1, x2, y2, Color.CYAN);
			else
				first	= false;
			
			x1	= x2;
			y1	= y2;
		}
	}
	
	public void drawDocks () 
	{
		int i;
		double ra,x1,y1;	
		
		for (i = 0; i < map.docks().n(); i++)
		{
			x1 = map.docks().at(i).pos.x();
			y1 = map.docks().at(i).pos.y();
			ra = map.docks().at(i).getAng() - Math.PI*0.5;
			model.addRawTransRotLine (-map.D_LENGHT,0,map.D_LENGHT, 0, x1, y1, ra, Color.BLUE);
			model.addRawTransRotLine (-map.D_LENGHT,0,-map.D_LENGHT,map.D_LENGHT, x1, y1, ra, Color.BLUE);
			model.addRawTransRotLine (map.D_LENGHT,0,map.D_LENGHT,map.D_LENGHT, x1, y1, ra, Color.BLUE);
			if (drawlabels)
				model.addRawText (x1, y1, map.docks().at(i).label, Color.BLUE);				
		}
	}
	
	public void drawDoors () 
	{
		int i;
		double x1,y1,x2,y2;
		Line2 l; 
		
		for (i = 0; i < map.doors ().n (); i++)
		{
			l = map.doors ().at (i).edge;
			x1 = l.orig().x();
			y1 = l.orig().y();
			x2 = l.dest().x();
			y2 = l.dest().y();
			
			model.addRawCircle(x1, y1, 0.1, Color.BLUE);
			model.addRawCircle(x2, y2, 0.1, Color.BLUE);
			model.addRawLine (l, Color.BLUE);
			
			l = map.doors ().at (i).path;
			x1 = l.orig().x();
			y1 = l.orig().y();
			x2 = l.dest().x();
			y2 = l.dest().y();
			
			model.addRawCircle(x1, y1, 0.1, Color.BLACK);
			model.addRawCircle(x2, y2, 0.1, Color.BLACK);
			//			model.addRawLine (l, Color.BLACK);
			model.addRawText (x1,y1,map.doors ().at (i).label, Color.BLACK);			
		}
	}
	
	public void drawWayPoints () 
	{
		int i;
		double x1,y1;
		
		for (i = 0; i < map.wps().n(); i++)
		{
			x1 = map.wps().at(i).pos.x();
			y1 = map.wps().at(i).pos.y();	
			
			model.addRawCircle(x1, y1, 0.1, Color.ORANGE);
			model.addRawArrow (x1, y1, map.G_LENGHT, map.wps().at(i).pos.alpha(), Color.ORANGE);
			if (drawlabels)
				model.addRawText (x1+0.25, y1+0.25, map.wps().at(i).label, Color.ORANGE);	
		} 
	}
	
	public void drawFAreas ()
	{
		int i,j,k;
		double x1,y1,x2,y2;
		Polygon2 polygon;
		String label;
		
		for (i = 0; i < map.fareas().n(); i++)
		{
			label = map.fareas().at(i).label;
			polygon = map.fareas().at(i).polygon;
			for (j = 0; j < polygon.npoints; j++)
			{
				x1 = polygon.xpoints[j];
				y1 = polygon.ypoints[j];
				
				k=j+1;
				if(k >= polygon.npoints)
				{
					x2 = polygon.xpoints[0];
					y2 = polygon.ypoints[0];
				} else {
					x2 = polygon.xpoints[k];
					y2 = polygon.ypoints[k];
				}
				model.addRawLine (x1, y1, x2, y2, Model2D.LINE, Color.RED);
			}
			model.addRawText (polygon.getCoGX(), polygon.getCoGY(), label, Color.RED);
		}
	}
	
	public void drawZones () 
	{	
		int i;
		double x1,y1,x2,y2;
		double width,height;
		Rectangle2D	rect;
		
		for (i = 0; i < map.zones().n (); i++)
		{
			rect = map.zones().at (i).area;
			x1 = rect.getX();
			y1 = rect.getY();
			width = rect.getWidth ();
			height = rect.getHeight();
			x2 = rect.getCenterX();
			y2 = rect.getCenterY(); 
			
			model.addRawBox (x1, y1, x1+width, y1+height, Model2D.DASHED, Color.YELLOW);
			model.addRawText (x2, y2, map.zones().at (i).label, Color.YELLOW);				
		}
	}
	
	public void drawStripBeacons () 
	{
		int i;
		double x1,y1,x2,y2;
		Line2 l; 
		
		for (i = 0; i < map.beacons().n(); i++)
		{
			l = map.beacons().at(i).getLine();
			x1 = l.orig().x();
			y1 = l.orig().y();
			x2 = l.dest().x();
			y2 = l.dest().y();
			
			model.addRawLine (l, Color.RED);
			model.addRawText (x1, y1, map.beacons().at(i).label, Color.RED);
			model.addRawArrow (x2, y2, 0.2, map.beacons().at(i).getAng() + (0.5 * Math.PI), Color.RED);
		}
	}
	
	public void drawCilindricalBeacons () 
	{
		int i;
		Ellipse2 e;
		
		for (i = 0; i < map.cbeacons().n(); i++)
		{
			e = map.cbeacons().at(i).beacon;
			model.addRawCircle (e.center().x(), e.center().y(), e.vert() / 2, Color.RED);
			model.addRawText (e.center().x(), e.center().y(), map.cbeacons().at(i).label, Color.RED);
		}
	}
	
	public void drawLimits (double x1, double y1, double x2, double y2) 
	{	
		model.addRawLine (x1, y1, x2, y1, Color.BLACK);
		model.addRawLine (x2, y1, x2, y2, Color.BLACK);
		model.addRawLine (x2, y2, x1, y2, Color.BLACK);
		model.addRawLine (x1, y2, x1, y1, Color.BLACK);		
	}    
	
	//*************************
	//* 	END DRAW WORLD	
	//*************************
	
	public void drawRobot (String name, RobotDesc rdesc, RobotData data, Color color, Position goal)
	{
		int				i, j;
		double			xi, yi, xf, yf;
		double			rx, ry, ra;
		double			x1, y1, x2, y2;
		double			cone, delta, k;
		
		if ((data == null) || (rdesc == null))				return;
		
		// Set robot's real location
		if (onplace)
		{
			rx	= 0.0;
			ry	= 0.0;
			ra	= 0.0;
		}
		else if (!simulated)
		{
			rx	= data.odom_x;
			ry	= data.odom_y;
			ra	= data.odom_a;
		}
		else
		{		
			rx	= data.real_x;
			ry	= data.real_y;
			ra	= data.real_a;
		}
		
		// Set zone HUD data values
//		if(map != null)
//			model.hud_label[H_ZONE] = "Zone: " + map.zones().inZone (rx, ry);
//		
		// Draw the real robot
		if (rdesc.icon == null)
			model.addRawCircle (rx, ry, rdesc.RADIUS, color);
		else
			for (i = 0; i < rdesc.icon.length; i++)
				model.addRawTransRotLine (rdesc.icon[i], rx, ry, ra, color);
		if (name != null)
			model.addRawText (rx + rdesc.RADIUS, ry - rdesc.RADIUS, name, color);				
		
		// Draw the location quality label
		if ((rdesc.MAXLSB > 0) && data.beacon[0].isValid ())
			model.addRawText (rx + rdesc.RADIUS, ry + rdesc.RADIUS, new Integer (data.beacon[0].getNumber ()).toString (), color);				
		
		
		// Draw the warning area
		if (drawwarning)
		{
			model.addRawCircle (rx, ry, rdesc.RADIUS, Color.ORANGE);
			model.addRawArrow (rx, ry, rdesc.RADIUS, ra, Color.ORANGE);	
		}
		
		// Draw the danger area
		if (drawdanger)
			for (i = 0; i < rdesc.MAXBUMPER; i++)
				model.addRawTransRotLine (rdesc.bumfeat[i], rx, ry, ra, Color.ORANGE.darker());
		
		if (drawsensors)
		{
			// Draw sonar sensors
			cone = rdesc.CONESON / 2.0;		
			for (i = 0; i < rdesc.MAXSONAR; i++)
			{ 
				if (!data.sonars_flg[i])					continue;
				
				xi	= rdesc.sonfeat[i].x ();
				yi 	= rdesc.sonfeat[i].y ();
				x1 	= rdesc.sonfeat[i].x () + data.sonars[i] * Math.cos ((rdesc.sonfeat[i].alpha () - cone));
				y1 	= rdesc.sonfeat[i].y () + data.sonars[i] * Math.sin ((rdesc.sonfeat[i].alpha () - cone)); 
				x2 	= rdesc.sonfeat[i].x () + data.sonars[i] * Math.cos ((rdesc.sonfeat[i].alpha () + cone));
				y2 	= rdesc.sonfeat[i].y () + data.sonars[i] * Math.sin ((rdesc.sonfeat[i].alpha () + cone)); 
				
				model.addRawTransRotLine (new Line2 (xi, yi, x1, y1), rx, ry, ra, Color.YELLOW);
				model.addRawTransRotLine (new Line2 (xi, yi, x2, y2), rx, ry, ra, Color.YELLOW);
				model.addRawTransRotLine (new Line2 (x1, y1, x2, y2), rx, ry, ra, Color.YELLOW);
			}
			
			// Draw infrared sensors
			cone = rdesc.CONEIR / 2.0;		
			for (i = 0; i < rdesc.MAXIR; i++)
			{ 
				if (!data.irs_flg[i])						continue;
				
				xi	= rdesc.irfeat[i].x ();
				yi 	= rdesc.irfeat[i].y ();
				x1 	= rdesc.irfeat[i].x () + data.irs[i] * Math.cos ((rdesc.irfeat[i].alpha () - cone));
				y1 	= rdesc.irfeat[i].y () + data.irs[i] * Math.sin ((rdesc.irfeat[i].alpha () - cone)); 
				x2 	= rdesc.irfeat[i].x () + data.irs[i] * Math.cos ((rdesc.irfeat[i].alpha () + cone));
				y2 	= rdesc.irfeat[i].y () + data.irs[i] * Math.sin ((rdesc.irfeat[i].alpha () + cone)); 
				
				model.addRawTransRotLine (new Line2 (xi, yi, x1, y1), rx, ry, ra, Color.MAGENTA);
				model.addRawTransRotLine (new Line2 (xi, yi, x2, y2), rx, ry, ra, Color.MAGENTA);
				model.addRawTransRotLine (new Line2 (x1, y1, x2, y2), rx, ry, ra, Color.MAGENTA);
			}
			
			// Draw laser sensors
			cone	= rdesc.CONELRF / 2.0;		
			delta	= rdesc.CONELRF / (rdesc.RAYLRF - 1.0);
			for (i = 0; i < rdesc.MAXLRF; i++)
			{ 		
				if (!data.lrfs_flg[i])						continue;
				
				xi	= rdesc.lrffeat[i].x ();
				yi 	= rdesc.lrffeat[i].y ();
				k	= -cone;
				for (j = 0; j < rdesc.RAYLRF; j++)
				{
					xf 	= rdesc.lrffeat[i].x () + data.lrfs[i][j] * Math.cos ((rdesc.lrffeat[i].alpha () + k));
					yf 	= rdesc.lrffeat[i].y () + data.lrfs[i][j] * Math.sin ((rdesc.lrffeat[i].alpha () + k)); 
					
					model.addRawTransRotLine (new Line2 (xi, yi, xf, yf), rx, ry, ra, Color.BLUE);
					
					k	+= delta;			
					xi	= xf;
					yi	= yf;
				}
				xf	= rdesc.lrffeat[i].x ();
				yf 	= rdesc.lrffeat[i].y ();
				model.addRawTransRotLine (new Line2 (xi, yi, xf, yf), rx, ry, ra, Color.BLUE);
			}			
		}
		
		// Draw goal place
		if (goal != null)
		{
			model.addRawCircle (goal.x (), goal.y (), map.G_RADIUS, color);
			model.addRawArrow (goal.x (), goal.y (), map.G_LENGHT, goal.alpha (), color);
		}
	}
	
	public void drawRobot (String name, RobotDesc rdesc, FusionDesc fdesc, MonitorData data, LPO[] lpos, Color color, Position goal)
	{
		int				i;
		double			xi, yi, xf, yf;
		double			rx, ry, ra;
		double			cone, delta, k;
		double			dr0, anc;
		
		if ((data == null) || (rdesc == null) || (fdesc == null))				return;
		
		// Set robot's location
		rx	= data.cur.x ();
		ry	= data.cur.y ();
		ra	= data.cur.alpha  ();
		
		// Set zone HUD data values
//		if(map != null)
//			model.hud_label[H_ZONE] = "Zone: " + map.zones().inZone (rx, ry);
		
		// Draw the real robot
		if (rdesc.icon == null)
			model.addRawCircle (rx, ry, rdesc.RADIUS, color);
		else
			for (i = 0; i < rdesc.icon.length; i++)
				model.addRawTransRotLine (rdesc.icon[i], rx, ry, ra, color);
		if (name != null)
			model.addRawText (rx + rdesc.RADIUS, ry - rdesc.RADIUS, name, color);				
		
		// Draw the location quality label
		if (data.qlty >= 0.0)
			model.addRawText (rx + rdesc.RADIUS, ry + rdesc.RADIUS, new Integer ((int) Math.round (data.qlty)).toString (), color);				
			
		// Draw a pallet when agv carry it
		if(data.pal_switch==1){
			model.addRawTransRotLine(new Line2(- 0.1 , 	0.5 , - 0.1 ,- 0.5),rx,ry,ra,Color.BLACK);
			model.addRawTransRotLine(new Line2(- 0.1 , 	0.5 , - 1.1 ,  0.5),rx,ry,ra,Color.BLACK);
			model.addRawTransRotLine(new Line2(- 1.1 , 	0.5 , - 1.1 ,- 0.5),rx,ry,ra,Color.BLACK);
			model.addRawTransRotLine(new Line2(- 1.1 ,- 0.5 , - 0.1 ,- 0.5),rx,ry,ra,Color.BLACK);
			
			model.addRawTransRotLine(new Line2(- 0.1 ,  0.5 , - 1.1 ,- 0.5),rx,ry,ra,Color.BLACK);
			model.addRawTransRotLine(new Line2(- 0.1 ,- 0.5 , - 1.1 ,  0.5),rx,ry,ra,Color.BLACK);
		}
		// Draw the warning area
		if (drawwarning)
		{
			model.addRawCircle (rx, ry, rdesc.RADIUS, Color.ORANGE);
			model.addRawArrow (rx, ry, rdesc.RADIUS, ra, Color.ORANGE);	
		}
		
		// Draw the danger area
		if (drawdanger)
			for (i = 0; i < rdesc.MAXBUMPER; i++)
				model.addRawTransRotLine (rdesc.bumfeat[i], rx, ry, ra, Color.ORANGE.darker());
		
		// Draw fusion based sensors
		if (drawsensors)
		{
			// Draw current virtual sensors
			for (i = 0; i < fdesc.MAXVIRTU; i++)
			{ 
				if (!data.virtuals_flg[i])						continue;
				
				xf 	= rx + fdesc.virtufeat[i].rho () * Math.cos (fdesc.virtufeat[i].phi () + ra);
				yf 	= ry + fdesc.virtufeat[i].rho () * Math.sin (fdesc.virtufeat[i].phi () + ra); 
				dr0	= fdesc.virtufeat[i].alpha ();
				
				model.addRawArrow (xf, yf, data.virtuals[i], ra+dr0, Color.CYAN.brighter());
			}
			
			// Draw current group sensors
			for (i = 0; i < fdesc.MAXGROUP; i++)
			{ 
				if (!data.groups_flg[i])						continue;
				
				xf 	= rx + fdesc.groupfeat[i].rho () * Math.cos (fdesc.groupfeat[i].phi () + ra);
				yf 	= ry + fdesc.groupfeat[i].rho () * Math.sin (fdesc.groupfeat[i].phi () + ra); 
				dr0	= fdesc.groupfeat[i].alpha ();
				
				model.addRawArrow (xf, yf, data.groups[i], ra+dr0, Color.MAGENTA);
			}
			
			// Draw current digital inputs
			for (i = 0; i < fdesc.MAXDSIG; i++)
			{ 
				if (!data.dsignals_flg[i])						continue;
				
				xf 	= rx + fdesc.dsigfeat[i].rho () * Math.cos (fdesc.dsigfeat[i].phi () + ra);
				yf 	= ry + fdesc.dsigfeat[i].rho () * Math.sin (fdesc.dsigfeat[i].phi () + ra); 
				dr0	= fdesc.dsigfeat[i].alpha ();
				
				if (data.dsignals[i])
				{
					model.addRawCircle (xf, yf, 0.05, Color.RED.brighter());
					model.addRawCircle (xf, yf, 0.04, Color.RED.brighter());
					model.addRawCircle (xf, yf, 0.03, Color.RED.brighter());
					model.addRawCircle (xf, yf, 0.02, Color.RED.brighter());
				}
				else
					model.addRawCircle (xf, yf, 0.05, Color.MAGENTA.brighter());
			}
			
			// Draw virtual scanner sensors
			if (data.scans_flg)
			{
				cone	= fdesc.CONESCAN / 2.0;		
				delta	= fdesc.CONESCAN / (fdesc.RAYSCAN - 1.0);
				
				xi	= fdesc.scanfeat.x ();
				yi 	= fdesc.scanfeat.y ();
				k	= -cone;
				for (i = 0; i < fdesc.RAYSCAN; i++)
				{
					xf 	= fdesc.scanfeat.x () + data.scans[i] * Math.cos ((fdesc.scanfeat.alpha () + k));
					yf 	= fdesc.scanfeat.y () + data.scans[i] * Math.sin ((fdesc.scanfeat.alpha () + k)); 
					
					model.addRawTransRotLine (new Line2 (xi, yi, xf, yf), rx, ry, ra, Color.BLUE);
					
					k	+= delta;			
					xi	= xf;
					yi	= yf;
				}
				xf	= fdesc.scanfeat.x ();
				yf 	= fdesc.scanfeat.y ();
				model.addRawTransRotLine (new Line2 (xi, yi, xf, yf), rx, ry, ra, Color.BLUE);
			}	
		}
		
		// Draw perceived objects (LPOs)
		if (drawlpos && (lpos != null))
		{
			for (i = 0; i < lpos.length; i++)
			{
				if (!lpos[i].active ())			continue;
				
				dr0	= lpos[i].phi () + ra;
				xi	= rx + lpos[i].rho () * Math.cos (dr0);
				yi	= ry + lpos[i].rho () * Math.sin (dr0);
				
				anc	= Math.round (lpos[i].anchor () * 100.0) / 100.0;
				
				//model.addRawCircle (xi, yi, 0.10, lpos[i].color ());
				//model.addRawCircle (xi, yi, 0.09, lpos[i].color ());
				//model.addRawCircle (xi, yi, 0.08, lpos[i].color ());
				//model.addRawText (xi, yi, lpos[i].label () + "-" + Double.toString (anc), lpos[i].color ());
				
				model.addRawCircle (xi, yi, 0.10, ColorTool.fromWColorToColor(lpos[i].color ()));
				model.addRawCircle (xi, yi, 0.09, ColorTool.fromWColorToColor(lpos[i].color ()));
				model.addRawCircle (xi, yi, 0.08, ColorTool.fromWColorToColor(lpos[i].color ()));
				model.addRawText (xi, yi, lpos[i].label () + "-" + Double.toString (anc), ColorTool.fromWColorToColor(lpos[i].color ()));
				
				/*
				 switch (lpos[i].type ())
				 {
				 case LPO.BALL:
				 model.addRawCircle (xi, yi, 0.10, (Color) lpos[i].color ());
				 model.addRawCircle (xi, yi, 0.09, (Color) lpos[i].color ());
				 model.addRawCircle (xi, yi, 0.08, (Color) lpos[i].color ());
				 model.addRawText (xi, yi, Double.toString (anc), ((Color) lpos[i].color ()));
				 break;
				 
				 case LPO.NET:
				 model.addRawTransRotLine (-0.2, 0, 0.2, 0, xi, yi, dr0, (Color) lpos[i].color ());
				 model.addRawTransRotLine (-0.2, 0, -0.2, 0.2, xi, yi, dr0, (Color) lpos[i].color ());
				 model.addRawTransRotLine (0.2, 0, 0.2, 0.2, xi, yi, dr0, (Color) lpos[i].color ());
				 model.addRawText (xi, yi, lpos[i].label () + ": " + anc, ((Color) lpos[i].color ()));
				 break;
				 
				 default:
				 }
				 */
			}
		}
		
		// Draw goal place
		if (goal != null)
		{
			model.addRawCircle (goal.x (), goal.y (), map.G_RADIUS, color);
			model.addRawArrow (goal.x (), goal.y (), map.G_LENGHT, goal.alpha (), color);
		}
	}
	
	public void update (RobotList rlist)
	{
		int i;
		
		if (rlist == null)			return;
		
		// Initialise component's model
		model.clearView ();
		
		drawWorld ();
		
		for (i = 0; i < rlist.size (); i++)
			drawRobot (rlist.getName (i), rlist.getDesc (i), rlist.getFusion (i), rlist.getMData (i), rlist.getLPOs (i), rlist.getColor (i), rlist.getGoal (i));
		
		// Set clipping region to the boundaries of the LPS
		if (!autoscale)
			model.setBB (MINX_BNDRY, MINY_BNDRY, MAXX_BNDRY, MAXY_BNDRY);
	}
	
	public void update (String id, RobotList rlist)
	{
		if (rlist == null)			return;
		
		// Initialise component's model
		model.clearView ();
		
		drawWorld ();
		
		drawRobot (id, rlist.getDesc (id), rlist.getFusion (id), rlist.getMData (id), rlist.getLPOs (id), rlist.getColor (id), rlist.getGoal (id));
		
		// Set clipping region to the boundaries of the LPS
		if (!autoscale)
			model.setBB (MINX_BNDRY, MINY_BNDRY, MAXX_BNDRY, MAXY_BNDRY);
	}
	
	public void update (RobotData data)
	{
		if (data == null)			return;
		
		// Initialise component's model
		model.clearView ();		
		
		// Draw world map
		drawWorld ();
		
		// Draw robot
		drawRobot (null, rdesc, data, Color.RED, null);
		
		// Set clipping region to the boundaries of the LPS
		if (!autoscale)
			model.setBB (MINX_BNDRY, MINY_BNDRY, MAXX_BNDRY, MAXY_BNDRY);
	}
	
	public void resetModel ()
	{
		model.clearView ();		
	} 
}