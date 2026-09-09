/*
 * Created on 08-dic-2004
 *
 * To change the template for this generated file go to
 * Window - Preferences - Java - Code Generation - Code and Comments
 */
package tcapps.tcsim.gui.visualization.objects;

import java.awt.*;

import javax.media.j3d.*;
import javax.vecmath.*;

import com.sun.j3d.utils.geometry.*;

import tc.shared.world.*;
import tcapps.tcsim.gui.visualization.*;

import wucore.utils.geom.*;

/**
 * @author Humberto Martinez Barbera
 *
 * To change the template for this generated type comment go to
 * Window - Preferences - Java - Code Generation - Code and Comments
 */
public class World3D extends BranchGroup
{
	// "Offset" of the position of the docks. The position indicated in the map is
	//	 the vehicle center when docked. So, this value must contain
	//  the distance between center of the vehicle and it load extreme
	protected static final double 	DOCKOFFSET = -1.2425;

	protected boolean				showFloor = false;
	protected Scene3D				scene;

	// Constructors
	public World3D (World map, Scene3D scene)
	{
		int				i;
		
		this.scene	= scene;
		
		setCapability(BranchGroup.ALLOW_CHILDREN_EXTEND);
		setCapability(BranchGroup.ALLOW_CHILDREN_READ);	
		setCapability(BranchGroup.ALLOW_CHILDREN_WRITE);

		// Add walls
		for (i = 0; i < map.walls ().n (); i++)
			addChild (createWall (map.walls ().at (i)));
		
		// Add External Objects
		for (i = 0; i < map.objects() .n (); i++)
		{
			Object3D		obj;

			obj	= createObject (map.objects ().at (i));
			if (obj != null)
				addChild (obj);
		}
		
		// Add floor
		if (showFloor)
			addChild (createFloor (map));
		
		// Add specific zone textures
		for ( i = 0;i<map.zones().n();i++)
			addChild (createZone (map.zones().at(i)));
		
		// Add docks
		for (i = 0; i < map.docks().n(); i++)
			addChild (createDock (map.docks().at(i)));
		
		// Add waypoints
		for (i = 0; i < map.wps().n(); i++)
			addChild (createWaypoint (map.wps().at(i)));
		
		// Add doors
		WMDoors			doors;
		
		doors	= map.doors ();
		for (i = 0; i < doors.n (); i++)
		{
//			WMDoor			door;
//			door		= (doors.edges ())[i];
			addChild (createDoorOrig (map.doors().at(i)));
			addChild (createDoorDest (map.doors().at(i)));
		}
	}
	
	/** Adds a new wall object to the universe */
	protected TransformGroup createWall (WMWall wall)
	{
		double				xi, yi;
		double				xf, yf;
		double				xo, yo;
		double				len, af;
		float 				wallWidth,wallHeight;
		
		Line2				line;
		Transform3D			pos;
		TransformGroup		gwall;
		Appearance 			app;		
		Box					surf;
		
		line = wall.edge;
		xi	= line.orig ().x ();
		yi	= line.orig ().y ();
		xf	= line.dest ().x ();
		yf	= line.dest ().y ();
		wallWidth = (float) wall.width;
		wallHeight = (float) wall.height;			
		len	= Math.sqrt ((xi - xf) * (xi - xf) + (yi - yf) * (yi - yf));
		af	= Math.atan2 ((yf - yi), (xf - xi));
		xo	= len / 2.0 * Math.cos (af) + wallWidth / 2.0 * Math.sin (af);
		yo	= len / 2.0 * Math.sin (af) + wallWidth / 2.0 * Math.cos (af);
		
		gwall 	= new TransformGroup ();
		pos 	= new Transform3D ();
		pos.rotZ (af);
		pos.setTranslation (new Vector3d (xi + xo, yi + yo, wallHeight/2.0));
		pos.setScale (1.0f);
		gwall.setTransform (pos);
		
		app		= scene.getCachedTexture (wall.texture, false);		
		surf		= new Box ((float) len / 2.0f, wallWidth / 2.0f, wallHeight / 2.0f, Box.GENERATE_NORMALS | Box.GENERATE_TEXTURE_COORDS, app);
		
		gwall.addChild (surf);
		return gwall;
	}
	
	/** Adds a floor object to the universe */
	protected TransformGroup createFloor (World map)
	{
		double				xo, yo;
		Appearance			app;
		Box					surf;
		TransformGroup 		floortg;
		Transform3D 			floort;
		
		xo	= (map.walls ().maxx () - map.walls ().minx ()) / 2.0;
		yo	= (map.walls ().maxy () - map.walls ().miny ()) / 2.0;
		
		floort 	= new Transform3D ();
		floort.setTranslation (new Vector3d(map.walls ().minx()+xo,map.walls ().miny()+yo,-0.01f));
		floortg	= new TransformGroup (floort);
		
		app		= scene.getCachedTexture (map.zones ().defaultTexture(), true);		
		surf = new Box((float)Math.abs(map.walls ().maxx()-map.walls ().minx())/2.0f,(float)Math.abs(map.walls ().maxy()-map.walls ().miny())/2.0f,0.01f,Box.GENERATE_NORMALS | Box.GENERATE_TEXTURE_COORDS, app);
		
		floortg.addChild (surf);
		return floortg;
	}	
	
	/** Adds a new zone object to the universe */
	protected TransformGroup createZone (WMZone zone)
	{
		double				xo, yo;
		Appearance			app;
		Box					surf;
		TransformGroup 		zonetg;
		Transform3D 			zonet;
		
		xo		= (zone.area.getMaxX()-zone.area.getMinX()) / 2.0;
		yo		= (zone.area.getMaxY()-zone.area.getMinY()) /2.0;
		zonetg	= new TransformGroup();
		zonet	= new Transform3D();
		zonet.setTranslation(new Vector3d(zone.area.getMinX()+xo,zone.area.getMinY()+yo,0.0f));
		zonetg.setTransform(zonet);
		
		app		= scene.getCachedTexture (zone.texture, true);		
		surf		= new Box((float)Math.abs(zone.area.getMaxX()-zone.area.getMinX())/2.0f,(float)Math.abs(zone.area.getMaxY()-zone.area.getMinY())/2.0f,0.01f,Box.GENERATE_NORMALS | Box.GENERATE_TEXTURE_COORDS, app);
		
		zonetg.addChild(surf);
		return zonetg;
	}
	
	/** Adds a new dock object to the universe */
	protected TransformGroup createDock (WMDock dock)
	{
		Box					leftBox, rightBox, centerBox;
		Color3f				color;
		Text2D				label;
		Material				mat;
		Appearance			app;
		TransformGroup		lt, rt, ct, dockt;
		Transform3D			lt3d, rt3d, ct3d, dockt3d;
		
		color	= new Color3f (1.0f,1.0f,0.0f);
		mat		= new Material();
		mat.setAmbientColor(color);
		mat.setDiffuseColor(color);
		app		= new Appearance();
		app.setMaterial(mat);
		
		leftBox = new Box(0.5f,0.05f,0.01f,app);	
		rightBox = new Box(0.5f,0.05f,0.01f,app);		
		centerBox = new Box(0.05f,0.5f,0.01f,app);	
		lt3d = new Transform3D();
		lt3d.setTranslation(new Vector3d(0.0,0.55,0.0));
		lt = new TransformGroup(lt3d);
		rt3d = new Transform3D();
		rt3d.setTranslation(new Vector3d(0.0,-0.55,0.0));
		rt = new TransformGroup(rt3d);
		ct3d = new Transform3D();
		ct3d.setTranslation(new Vector3d(-0.45,0.0,0.0));
		ct = new TransformGroup(ct3d);
		label	= new Text2D (dock.label, color, "Application", 100, Font.PLAIN);
		
		dockt3d = new Transform3D();
		dockt3d.setTranslation(new Vector3d(dock.pos.x ()+(DOCKOFFSET+0.5)*Math.cos(dock.getAng()), dock.pos.y ()+(DOCKOFFSET+0.5)*Math.sin(dock.getAng()), dock.pos.z ()+0.05));	
		Transform3D aux = new Transform3D();
		aux.rotZ(dock.getAng());
		dockt3d.mul(aux);
		
		dockt = new TransformGroup(dockt3d);
		dockt.addChild(lt);
		dockt.addChild(rt);
		dockt.addChild(ct);
		dockt.addChild(label);
		
		lt.addChild(leftBox);
		rt.addChild(rightBox);
		ct.addChild(centerBox);
		
		return dockt;
	}	
	
	/** Adds a new waypoint object to the universe */
	protected TransformGroup createWaypoint (WMWaypoint wp)
	{
		Sphere				point;
		Color3f				color;
		Text2D				label;
		Material				mat;
		Appearance			app;
		TransformGroup		lb, node;
		Transform3D			lb3d, trans;
		
		color	= new Color3f (1.0f,1.0f,0.0f);
		mat		= new Material();
		mat.setAmbientColor(color);
		mat.setDiffuseColor(color);
		app		= new Appearance();
		app.setMaterial(mat);
		
		point	= new Sphere (0.10f, app);
		
		label	= new Text2D (wp.label, color, "Application", 100, Font.PLAIN);
		lb3d		= new Transform3D();
		lb3d.setTranslation(new Vector3d(-0.45,0.45,0.0));
		lb	 	= new TransformGroup(lb3d);
		lb.addChild (label);
		
		trans = new Transform3D();
		trans.setTranslation(new Vector3d (wp.pos.x (), wp.pos.y (), wp.pos.z ()+0.05));	
		Transform3D aux = new Transform3D();
		aux.rotZ(wp.getAng());
		trans.mul(aux);
		
		node = new TransformGroup (trans);
		node.addChild (point);
		node.addChild (lb);
		
		return node;
	}	
	
	protected TransformGroup createDoorOrig (WMDoor door){
	    return createDoor(door.label,new Point3(door.edge.orig()));
	}
	protected TransformGroup createDoorDest (WMDoor door){
	    return createDoor(door.label,new Point3(door.edge.orig()));
	}
	
	/** Adds a new door object to the universe */
	protected TransformGroup createDoor (String name, Point3 pos)
	{
		Sphere				point;
		Color3f				color;
		Text2D				label;
		Material				mat;
		Appearance			app;
		TransformGroup		lb, node;
		Transform3D			lb3d, trans;
		
		color	= new Color3f (0.0f,0.0f,1.0f);
		mat		= new Material();
		mat.setAmbientColor(color);
		mat.setDiffuseColor(color);
		app		= new Appearance();
		app.setMaterial(mat);
		
		point	= new Sphere (0.10f, app);
		
		label	= new Text2D (name, color, "Application", 100, Font.PLAIN);
		lb3d		= new Transform3D();
		lb3d.setTranslation(new Vector3d(-0.45,0.45,0.0));
		lb	 	= new TransformGroup(lb3d);
		lb.addChild (label);
		
		trans = new Transform3D();
		trans.setTranslation(new Vector3d (pos.x (), pos.y (), pos.z ()+0.05));	
		
		node = new TransformGroup (trans);
		node.addChild (point);
		node.addChild (lb);
		
		return node;
	}	
	
	/** Adds a new 3D object to the universe */
	protected Object3D createObject (WMObject object)
	{
		Color		color = null;
		
		if (object.shape == null)				return null;
		
		// Check if default coloring must be overriden
		if (object.usecolor)
			color 	= wucore.utils.color.ColorTool.fromWColorToColor(object.color);
			//color 	= object.color;
		
		return new Object3D (scene.getCachedObject (object.shape, color), object.pos, object.a);		
	}
}
