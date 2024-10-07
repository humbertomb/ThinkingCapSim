/*
 * Seccion BLOCKS del archivo DXF.
 * La sección BLOCKS contiene una entrada para cada una de las referencias
 * a un bloque del dibujo. 
 */
package wucore.utils.dxf.sections;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.Vector;

import wucore.utils.dxf.entities.BlockDxf;

/**
 * @author Administrador
 *
 * TODO To change the template for this generated type comment go to
 * Window - Preferences - Java - Code Style - Code Templates
 */
public class BlocksSection {

public Vector blocks;
 


public BlocksSection(){
	blocks = new Vector();
}

public void add(BlockDxf block){
	for(int i = 0; i<blocks.size(); i++){
		if(((BlockDxf)blocks.get(i)).getName().equals(block.getName())) {
			//System.out.println("Bloque con el nombre "+block.name+" ya insertado");
			return;
		}
	}
	blocks.add(block);
}

public BlockDxf getBlock(String name){
	if(blocks == null) return null;
	for (int i = 0; i<blocks.size(); i++)
		if(((BlockDxf)blocks.get(i)).getName().equalsIgnoreCase(name)) 
			return (BlockDxf)blocks.get(i);
	return null;
}

public void write(PrintWriter out){
	out.println("  0\nSECTION");					// Inicio Seccion
	out.println("  2\nBLOCKS"); 					// Seccion Blocks
	for(int i = 0; i<blocks.size(); i++){
		((BlockDxf)blocks.get(i)).write(out);
	}
	out.println("  0\nENDSEC"); 					// Fin Seccion Blocks
}

public void read(BufferedReader br) throws IOException{
	String linea;
	while((linea=br.readLine())!=null){
		if(linea.equals("BLOCK")) add(new BlockDxf(br));
		else if(linea.equals("ENDSEC")) return;
	}
}

public String toString(){
	String ret = "";
	if(blocks == null) return ret;
	ret += 	"----------------------------------\n"+
			"Blocks = "+blocks.size()+"\n"+
			"----------------------------------\n";
	for(int i = 0; i<blocks.size();i++)	ret += blocks.get(i).toString()+"\n";
	
	return ret;
}

}
