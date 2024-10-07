/*
 * Created on 05-ene-2005
 *
 * TODO To change the template for this generated file go to
 * Window - Preferences - Java - Code Style - Code Templates
 */
package wucore.utils.dxf.sections;

/**
 * @author Administrador
 *
 * TODO To change the template for this generated type comment go to
 * Window - Preferences - Java - Code Style - Code Templates
 */
public class Layer {

    public String name;
    public int	color;
    public String linestile;
    
    public Layer(String name) {
        this(name,ACADColor.DEFAULT);
    }
    
    public Layer(String name,int color) {
        this(name,color,"CONTINUOUS");
    }
    
    public Layer(String name, int color, String linestile) {
        this.name = name;
        this.color = color;
        this.linestile = linestile;
    }
       

}
