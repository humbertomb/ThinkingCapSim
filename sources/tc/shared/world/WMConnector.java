/*
 * Created on 08-feb-2004
 *
 * To change the template for this generated file go to
 * Window - Preferences - Java - Code Generation - Code and Comments
 */
package tc.shared.world;

import com.google.gson.JsonObject;

import java.util.StringTokenizer;

import wucore.utils.dxf.DXFWorldFile;
import wucore.utils.dxf.entities.LineDxf;
import wucore.utils.geom.Line2;
import wucore.utils.geom.Point3;

/**
 * @author Humberto Martinez Barbera
 *
 * To change the template for this generated type comment go to
 * Window - Preferences - Java - Code Generation - Code and Comments
 */
/** A connector between two zones (a door, a gate, an opening...): the physical opening and the path to cross it. */
/**
 * A connector between two zones (a door, a gate, an opening...): the physical
 * opening (edge) and the segment to cross it (path). Kept as DOOR_i in the files.
 */
public class WMConnector extends WMElement
{
    // 2D components
    public Line2				edge;		// Physical location of the door
    public Line2				path;		// Points for crossing the door
    
    // 2 1/2 D components
    public double				width;
    public double				height;
    
    // Visualization components
    public String				texture;					
    
    
    public WMConnector(){
    }
    
    public WMConnector(LineDxf line,double dwidth, double dheight, String dtexture) {
        texture = dtexture;
        width = dwidth;
        height = dheight;
        
        path = new Line2(line.getStart().x(),line.getStart().y(),line.getStart().z(),line.getEnd().x(),line.getEnd().y(),line.getEnd().z());
        if(line.ExtendedText.size()>0) label = line.getExtText(0);
        else									label = "DOOR_?";
        if(line.ExtendedText.size()>1){
            StringTokenizer tk = new StringTokenizer(line.getExtText(1), ", ");
            if(tk.countTokens()>=4)
                edge = new Line2(Double.parseDouble(tk.nextToken()),Double.parseDouble(tk.nextToken()),Double.parseDouble(tk.nextToken()),Double.parseDouble(tk.nextToken()));
            else
                edge = new Line2();
        }
        if(line.ExtendedText.size()>2) texture = line.getExtText(2);
        if(line.ExtendedDouble.size()>0) height = line.getExtDouble(0);
        if(line.ExtendedDouble.size()>1) width = line.getExtDouble(1);
    }
    
    public void toDxf(DXFWorldFile dxf) {
        LineDxf line = new LineDxf(new Point3(path.orig()),new Point3(path.dest()),"DOORS");
        line.addExtText(0,label);
        line.addExtText(1,edge.toRawString());
        line.addExtText(2,texture);
        line.addExtDouble(0,height);
        line.addExtDouble(1,width);
        dxf.addEntity(line);
    }
    

    /* JSON: {label, edge: {x1..z2}, path: {x1..z2} [, width, height, texture]} */

    public WMConnector (JsonObject o, double dwidth, double dheight, String dtexture)
    {
        label	= World.getString (o, "label", "door");
        edge	= World.toLine (World.getObject (o, "edge"));
        path	= o.has ("path") ? World.toLine (World.getObject (o, "path")) : new Line2 (edge.orig ().x (), edge.orig ().y (), edge.z1 (), edge.dest ().x (), edge.dest ().y (), edge.z2 ());
        width	= World.getDouble (o, "width", dwidth);
        height	= World.getDouble (o, "height", dheight);
        texture	= World.getString (o, "texture", dtexture);
    }

    public JsonObject toJson (double dwidth, double dheight, String dtexture)
    {
        JsonObject	o = new JsonObject ();
        o.addProperty ("label", label);
        o.add ("edge", World.line (edge));
        o.add ("path", World.line (path));
        if (width != dwidth)							o.addProperty ("width", World.num (width));
        if (height != dheight)							o.addProperty ("height", World.num (height));
        if ((texture != null) && !texture.equals (dtexture))	o.addProperty ("texture", texture);
        return o;
    }
}
