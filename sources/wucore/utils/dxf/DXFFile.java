/**
 * Title: Line
 * Description: Clase para generacion y lectura de entidades DXF version ACAD12
 * @author Jose Antonio Marin
 * @version 1.0
 */

package wucore.utils.dxf;

import java.io.BufferedReader;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.Vector;

import wucore.utils.dxf.entities.BlockDxf;
import wucore.utils.dxf.entities.CircleDxf;
import wucore.utils.dxf.entities.Entity;
import wucore.utils.dxf.entities.InsertDxf;
import wucore.utils.dxf.entities.TextDxf;
import wucore.utils.dxf.entities.LineDxf;
import wucore.utils.dxf.sections.BlocksSection;
import wucore.utils.dxf.sections.EntitiesSection;
import wucore.utils.dxf.sections.HeaderSection;
import wucore.utils.dxf.sections.Layer;
import wucore.utils.dxf.sections.TablesSection;
import wucore.utils.geom.Point3;


public class DXFFile{
		
	protected PrintWriter out;
	protected HeaderSection HEADER;
	protected BlocksSection BLOCKS;
	protected EntitiesSection ENTITIES;
	protected TablesSection TABLES;
		
	public DXFFile(){
		HEADER = new HeaderSection();
		BLOCKS = new BlocksSection();
		ENTITIES = new EntitiesSection();
		TABLES = new TablesSection();
	}
	
	
	// Carga entidades de un fichero DXF
	public void load(String file) throws IOException {
		String linea;
		BufferedReader br = new BufferedReader(new FileReader(file));
		
		while((linea=br.readLine())!=null){
			if(linea.endsWith(" 0")){
				linea=br.readLine();
				if(linea.endsWith("SECTION")){
					linea=br.readLine();	// Deberia ser un 2
					linea=br.readLine();	// Nombre de la seccion
					if		(linea.endsWith("BLOCKS")){BLOCKS.read(br);}
					else if (linea.endsWith("ENTITIES")){ENTITIES.read(br);}
				}
			}
		}
		
	}

	public void createDxf(String file) throws IOException{
		HEADER.EXTMAX.set(Entity.LIMMAX);
		HEADER.EXTMIN.set(Entity.LIMMIN);
		HEADER.LIMMAX.set(Entity.LIMMAX);
		HEADER.LIMMIN.set(Entity.LIMMIN);
		out = new PrintWriter(new FileOutputStream(file));
		HEADER.write(out);
		TABLES.write(out);
		BLOCKS.write(out);
		ENTITIES.write(out);
		out.println("  0\nEOF"); 					// Fin Archivo
		out.close();
	}
	
	public void addEntity(Entity ent){
		ENTITIES.add(ent);
	}
	public void addBlock(BlockDxf block){
		BLOCKS.add(block);
	}
	public void insertBlock(InsertDxf insert){
		ENTITIES.add(insert);
	}
	
	public Vector getEntities(){
		return ENTITIES.entities;
	}
	public Vector getBlocks(){
		return BLOCKS.blocks;
	}
	public BlockDxf getBlocks(String name){
		return BLOCKS.getBlock(name);
	}
	
	public void addLayer(Layer layer){
	    TABLES.addLayer(layer);
	}
	
	public LineDxf[] getLines(){
	    return ENTITIES.getLines();
	}
	
	public String toString() {
		if(ENTITIES != null && BLOCKS != null){
			return "BLOQUES DEFINIDOS\n"+
					BLOCKS.toString()+"\n"+
					"ENTIDADES\n"+
					ENTITIES.toString();
		}
		else return super.toString();
	}
	
	
	public static void main(String[] args){
		DXFFile dxf = new DXFFile();
		boolean load = true;
		boolean write = false;
		
		if(write){
			dxf.addEntity(new LineDxf(new Point3(0,0,0),new Point3(100,100,0)));
			dxf.addEntity(new CircleDxf(new Point3(500,200,0),50));
			dxf.addEntity(new TextDxf("Prueba",new Point3(10,10,0),0.2));
			
			LineDxf line = new LineDxf(new Point3(0,0,0),new Point3(100,100,0));
			line.addExtInt(23);line.addExtInt(634);
			line.addExtDouble(0.35);line.addExtDouble(4353.4);
			line.addExtText("Esto es una");line.addExtText("propiedad extendida");
			dxf.addEntity(line);
			
			BlockDxf block = new BlockDxf("Bloque");
			
			block.add(new LineDxf(new Point3(0,0,0),new Point3(0,10,0)));
			block.add(new CircleDxf(new Point3(0,10,0),10));
			block.add(new TextDxf("Prueba",new Point3(10,10,0),0.2));
			dxf.addBlock(block);
			
			dxf.insertBlock(new InsertDxf(new Point3(20,20,0),block.getName()));
			dxf.insertBlock(new InsertDxf(new Point3(50,20,0),block.getName()));
			
			try {
				dxf.createDxf("prueba1.dxf");
				dxf = new DXFFile();
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
		if(load){
		try {
			dxf.load("probando1.dxf");
		} catch (IOException e) {
			e.printStackTrace();
		}
		System.out.println(dxf);
		}
		
	}
	
}



