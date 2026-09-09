package devices.pos;

import wucore.utils.geom.*;

/**
 * Clase para aplicar rotaciones, translaciones de puntos o lineas 2D, 
 * o realizar cambios entre coordenadas locales y globales.
 * 
 *  @author Jose Antonio Marin
 */
public class Transform2 {


    
    //			TRANSLACIONES                //
    
    /** Translacion de un punto 
     * @param pt 	Punto al que se aplica la translacion
     * @param tx 	Translacion en eje x
     * @param ty 	Translacion en eje y
     * @return		Punto transladado
     */
    static public Point2 trans(Point2 pt, double tx, double ty){
        return new Point2(pt.x()+tx, pt.y() + ty);
    }
    
    /** Translacion de una linea 
     * @param line 	Linea a la que se aplica la translacion
     * @param tx 	Translacion en eje x
     * @param ty 	Translacion en eje y
     * @return		Linea transladada
     */
    static public Line2 trans(Line2 line, double tx, double ty){
        Point2 p1 = trans(line.orig,tx,ty);
        Point2 p2 = trans(line.dest,tx,ty);
        return new Line2(p1.x(),p1.y(),p2.x(),p2.y());
    }    

    /** Translacion de varias lineas 
     * @param pts 	Puntos a los que se aplica la translacion
     * @param tx 	Translacion en eje x
     * @param ty 	Translacion en eje y
     * @return		Puntos transladados
     */
    static public Line2[] trans(Line2[] lines, double tx, double ty){
    	Line2[] ret = new Line2[lines.length];
    	for(int i = 0; i<ret.length; i++){
    		ret[i] = trans(lines[i],tx,ty);
    	}
        return ret;
    }   
    
    //			ROTACIONES                //
    
    /** Rotacion de un punto respecto al origen de coordenadas (sentido antihorario)
     * @param pt 		Punto al que se aplica la rotacion
     * @param alpha 	Angulo de rotacion
     * @return			Punto rotado
     */
    static public Point2 rot(Point2 pt, double alpha){
        //									| cos	sin		0|
        //	[x',y', w] = |pt.x pt.y 1|	* 	|-sin	cos		0|
        //									|  0	 0		1|
        //
        double x = pt.x()*Math.cos(alpha) - pt.y()*Math.sin(alpha);
        double y = pt.x()*Math.sin(alpha) + pt.y()*Math.cos(alpha);
        return new Point2(x,y);
    }
    
    /** Rotacion de varias lineas respecto al origen de coordenadas (sentido antihorario)
     * @param pt 		Punto al que se aplica la rotacion
     * @param alpha 	Angulo de rotacion
     * @return			Punto rotado
     */
    static public Line2[] rot(Line2[] lines, double alpha){
    	Line2[] ret = new Line2[lines.length];
    	for(int i = 0; i<ret.length; i++){
    		ret[i] = rot(lines[i],alpha);
    	}
        return ret;
    }
    
    /** Rotacion de una linea respecto al origen de coordenadas (sentido antihorario)
     * @param line 		Linea a la que se aplica la rotacion
     * @param alpha 	Angulo de rotacion
     * @return			Linea rotada
     */
    static public Line2 rot(Line2 line, double alpha){
        Point2 p1 = rot(line.orig,alpha);
        Point2 p2 = rot(line.dest,alpha);
        return new Line2(p1.x(),p1.y(),p2.x(),p2.y());
    }
    
    /** Rotacion de un punto respecto a otro punto (sentido antihorario)
     * @param pt 		Punto al que se aplica la rotacion
     * @param cx 		Centro de rotacion (coordenada x)
     * @param cy 		Centro de rotacion (coordenada y)
     * @param alpha 	Angulo de rotacion
     * @return			Punto rotado
     */
    static public Point2 rot(Point2 pt,double cx, double cy, double alpha){
        // Se translada el punto de rotacion al origen, se rota respecto al origen, 
        // y se vuelve a transladar al punto de rotacion
        return trans(rot(trans(pt,-cx,-cy),alpha),cx,cy);
    }
    
    /** Rotacion de una linea respecto a otro punto (sentido antihorario)
     * @param line 		Linea a la que se aplica la rotacion
     * @param cx 		Centro de rotacion (coordenada x)
     * @param cy 		Centro de rotacion (coordenada y)
     * @param alpha 	Angulo de rotacion
     * @return			Linea rotada
     */
    static public Line2 rot(Line2 line,double cx, double cy, double alpha){
        Point2 p1 = rot(line.orig,cx,cy,alpha);
        Point2 p2 = rot(line.dest,cx,cy,alpha);
        return new Line2(p1.x(),p1.y(),p2.x(),p2.y());
    }
    
    /** Rotacion de varias lineas respecto a otro punto (sentido antihorario)
     * @param line 		Lineas a la que se aplica la rotacion
     * @param cx 		Centro de rotacion (coordenada x)
     * @param cy 		Centro de rotacion (coordenada y)
     * @param alpha 	Angulo de rotacion
     * @return			Lineas rotadas
     */
    static public Line2[] rot(Line2[] lines, double cx, double cy, double alpha){
    	Line2[] ret = new Line2[lines.length];
    	for(int i = 0; i<ret.length; i++){
    		ret[i] = rot(lines[i],cx,cy,alpha);
    	}
        return ret;
    }
    
    
    //		TRANSLACIONES Y ROTACIONES
    
    
    /** Translacion de un punto seguida de una rotacion respecto al origen
     * @param pt 		Punto al que se aplica la translacion y rotacion
     * @param tx 		Translacion en eje x
     * @param ty 		Translacion en eje y
     * @param alpha 	Angulo de rotacion
     * @return			Punto transladado y rotado
     */
    static public Point2 transRot(Point2 pt, double tx, double ty, double alpha){
        return rot(trans(pt,tx,ty),alpha);
    }
    
    /** Translacion de una linea seguida de una rotacion respecto al origen
     * @param line 		Linea a la que se aplica la translacion y rotacion
     * @param tx 		Translacion en eje x
     * @param ty 		Translacion en eje y
     * @param alpha 	Angulo de rotacion
     * @return			Linea transladada y rotada
     */
    static public Line2 transRot(Line2 line, double tx, double ty, double alpha){
        return rot(trans(line,tx,ty),alpha);
    }
    
    /** Translacion de una linea seguida de una rotacion respecto al origen
     * @param line 		Linea a la que se aplica la translacion y rotacion
     * @param tx 		Translacion en eje x
     * @param ty 		Translacion en eje y
     * @param alpha 	Angulo de rotacion
     * @return			Linea transladada y rotada
     */
    static public Line2[] transRot(Line2[] lines, double tx, double ty, double alpha){
    	Line2[] ret = new Line2[lines.length];
    	for(int i = 0; i<ret.length; i++){
    		ret[i] = transRot(lines[i],tx,ty,alpha);
    	}
        return ret;
    }
    
    /** Rotacion de un punto respecto al origen seguida de una translacion 
     * @param pt 		Punto al que se aplica la translacion y rotacion
     * @param tx 		Translacion en eje x
     * @param ty 		Translacion en eje y
     * @param alpha 	Angulo de rotacion
     * @return			Punto rotado y transladado
     */
    static public Point2 rotTrans(Point2 pt, double tx, double ty, double alpha){
        return trans(rot(pt,alpha),tx,ty);
    }
    
    /** Rotacion de una linea respecto al origen seguida de una translacion 
     * @param line 		Linea a la que se aplica la translacion y rotacion
     * @param tx 		Translacion en eje x
     * @param ty 		Translacion en eje y
     * @param alpha 	Angulo de rotacion
     * @return			Linea rotada y transladada
     */
    static public Line2 rotTrans(Line2 line, double tx, double ty, double alpha){
        return trans(rot(line,alpha),tx,ty);
    }
    
    /** Rotacion de una linea respecto al origen seguida de una translacion 
     * @param line 		Linea a la que se aplica la translacion y rotacion
     * @param tx 		Translacion en eje x
     * @param ty 		Translacion en eje y
     * @param alpha 	Angulo de rotacion
     * @return			Linea rotada y transladada
     */
    static public Line2[] rotTrans(Line2[] lines, double tx, double ty, double alpha){
    	Line2[] ret = new Line2[lines.length];
    	for(int i = 0; i<ret.length; i++){
    		ret[i] = rotTrans(lines[i],tx,ty,alpha);
    	}
        return ret;
    }
    
    
    //	TRANSFORMACION COORDENADAS
    
    /** Transformacion de coordenadas Globales a coordenadas Locales 
     * @param xg 		Posicion global (eje x)
     * @param yg 		Posicion global (eje y)
     * @param alpha_g 	Posicion global (angulo)
     * @param xo 		Origen del sistema local (eje x)
     * @param yo 		Origen del sistema local (eje y)
     * @param alpha_o 	Origen del sistema local (angulo)
     * @return			Posicion respecto al sistema local
     */
    static public Position toLocal(double xg, double yg, double alpha_g, double xo, double yo, double alpha_o){
        // Translacion(-xl,-yl) + Rotacion(-alphal)
        double xl = (xg-xo)*Math.cos(-alpha_o) - (yg-yo)*Math.sin(-alpha_o);
        double yl = (xg-xo)*Math.sin(-alpha_o) + (yg-yo)*Math.cos(-alpha_o);
        double al = alpha_g-alpha_o;
        return new Position(xl,yl,al);
    }
    
    /** Transformacion de coordenadas Globales a coordenadas Locales 
     * @param pg 		Posicion global
     * @param po 		Origen del sistema local
     * @return			Posicion respecto al sistema local
     */
    static public Position toLocal(Position pg, Position po){
        return toLocal(pg.x(),pg.y(),pg.alpha(),po.x(),po.y(),po.alpha());
    }
    
    /** Transformacion de coordenadas Globales a coordenadas Locales 
     * @param line 		Linea en coordenadas globales
     * @param po 		Origen del sistema local
     * @return			Linea respecto al sistema local
     */
    static public Line2 toLocal(Line2 line, Position po){
        return transRot(line, -po.x(),-po.y(),-po.alpha());
    }
    
    /** Transformacion de coordenadas Globales a coordenadas Locales 
     * @param line 		Linea en coordenadas globales
     * @param po 		Origen del sistema local
     * @return			Linea respecto al sistema local
     */
    static public Line2[] toLocal(Line2[] line, Position po){
        return transRot(line, -po.x(),-po.y(),-po.alpha());
    }
    
    /** Transformacion de coordenadas Locales a coordenadas Globales
     * @param xl 		Posicion respecto el sistema local (eje x)
     * @param yl 		Posicion respecto el sistema local (eje y)
     * @param alpha_l 	Posicion respecto el sistema local (angulo)
     * @param xo 		Origen del sistema local (eje x)
     * @param yo 		Origen del sistema local (eje y)
     * @param alpha_o 	Origen del sistema local (angulo)
     * @return			Posicion respecto al sistema global
     */
    static public Position toGlobal(double xl, double yl, double alpha_l, double xo, double yo, double alpha_o){
        //      Rotacion(alpha_o) + Translacion(xo, yo)
        double xg = xl*Math.cos(alpha_o) - yl*Math.sin(alpha_o) + xo;
        double yg = xl*Math.sin(alpha_o) + yl*Math.cos(alpha_o) + yo;
        double ag = alpha_l+alpha_o;
        return new Position(xg,yg,ag);
    }

    /** Transformacion de coordenadas Locales a coordenadas Globales 
     * @param pl 		Posicion respecto al sistema local
     * @param orig 		Origen del sistema local
     * @return			Posicion respecto al sistema global
     */
    static public Position toGlobal(Position pl, Position orig){
        return toGlobal(pl.x(),pl.y(),pl.alpha(),orig.x(),orig.y(),orig.alpha());
    }
    
    
    /** Transformacion de coordenadas Locales a coordenadas Globales 
     * @param lines		Linea respecto al sistema local
     * @param orig 		Origen del sistema local
     * @return			Linea pasada al sistema global
     */
    static public Line2 toGlobal(Line2 line, Position orig){
        return rotTrans(line,orig.x(),orig.y(),orig.alpha());
    }
    
    /** Transformacion de coordenadas Locales a coordenadas Globales 
     * @param lines		Lineas respecto al sistema local
     * @param orig 		Origen del sistema local
     * @return			Lineas pasadas al sistema global
     */
    static public Line2[] toGlobal(Line2[] lines, Position orig){
        return rotTrans(lines,orig.x(),orig.y(),orig.alpha());
    }
    
}
