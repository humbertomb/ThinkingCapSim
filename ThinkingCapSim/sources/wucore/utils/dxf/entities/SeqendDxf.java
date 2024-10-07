/**
 * Title: SeqendDxf
 * Description: Entidad SEQEND.
 * Esta entidad marca el final del vértice de una polilínea 
 * @author Jose Antonio Marin
 * @version 1.0
 */

package wucore.utils.dxf.entities;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.PrintWriter;

public class SeqendDxf extends Entity{

	public SeqendDxf(){
		super();
	}
	
	//	 Read dxf line
	public void read(BufferedReader br) throws IOException{
		String line;
		while( !(line = br.readLine()).endsWith(" 0") ){
			if(line.endsWith(" 5"))	setID(br.readLine());
			else if(line.endsWith(" 8"))  setLayer(br.readLine());
			else line = br.readLine();
		}
	}
	
	public void write(PrintWriter out){
		calculateID();
		out.println("  0\nSEQEND");							// Tipo entidad
		out.println("  5\n" + id); 							// Identificador
		out.println("  8\n" + layer);						// Capa
		writeExt(out);		
	}
		
	
	public String toString()	{		return "SEQEND(LAYER: '"+getLayer()+"' ID: '"+getID()+"')";}
}