/*
 * (c) 2003 Jose Antonio Marin Meseguer
 * (c) 2004 Humberto Martinez Barbera
 */

package tclib.utils.graphs;

import java.util.Vector;

public class GNode extends Object
{
	
	protected	String 		name;   		// Nombre del Node
	protected	int			index;		// Numero identificativo del Node
	
	protected	Vector  		list;  		// Lista de Adyacencia
	protected	Vector		pesos;		// Lista de Pesos
	protected	int			nList;		// Numero de elementos en la Lista
	
	
	//* Crea un Node por defecto con un nombre e &iacute;ndice identificativo por defecto */
	protected GNode()
	{
		index 		= -1;        
		name 		= new String("Node"+index);
		list 		= new Vector();
		pesos		= new Vector();
		nList 		= 0;            
	}
	
	/** Crea un Node definiendo el nombre del Node
	 @param	name	Nombre identificativo del Node
	 */    
	public GNode (String name)
	{
		this();
		this.name	= name;            
	}
	
	/** Crea un Node definiendo el nombre del Node, y una lista nodos adyacentes
	 @param	name	Nombre identificativo del Node
	 @param	list	Array con la lista de adyacencia del Node
	 */
	public GNode (String name, int[] lista)
	{
		this(name);
		addNode(lista);
	}
	
	
	// Metodos que devuelven valores de los datos
	
	
	/** Devuelve el nombre del Node
	 @return	Nombre del nodo
	 */
	public String getLabel()
	{ 
		return(name);
	}
	
	/** Devuelve el &iacute;ndice del Node
	 @return	Indice identificativo del Node
	 */
	public int index()
	{
		return(index);
	}
	
	/** Devuelve el n&uacute;mero de nodos adyacentes actual
	 @return	N&uacute;mero de nodos de la lista de adyacencia
	 */
	public int nList()
	{
		return nList;
	}
	
	
	/** Cambia el &iacute;ndice del nodo (debe ser unico para cada Node, y especificarse al añadirse al Graph)
	 @param	index	Nuevo &iacute;ndice del nodo
	 */
	public void setIndex(int index)
	{
		this.index = index;
	}
	
	
	/** Añade un Node a la lista de adyacencia (con peso 1 por defecto)
	 @param	A	Node que se le añade a la lista de adyacencia
	 */
	public boolean addNode(GNode A)
	{
		return (addNode(A.index(),1));
	}
	
	/** Añade una lista de &iacute;ndices de nodos a la lista de adyacencia (con peso 1 por defecto)
	 @param	nodos	Array de &iacute;ndices de nodos
	 */
	public void addNode(int[] nodos)
	{
		for (int i=0; i<nodos.length; i++)
			addNode(nodos[i],1);
	}
	
	/** Añade un &iacute;ndice de un nodo a la lista de adyacencia (con peso 1 por defecto)
	 @param	nodo	Indice del nodo que se añade
	 */
	public boolean addNode(int nodo)
	{
		return (addNode(nodo,1));
	}
	
	/** Añade un &iacute;ndice de un nodo a la lista de adyacencia definiendo el peso
	 @param	A		Node al que se añade
	 @param	peso 	Peso para llegar al nodo A
	 */
	public boolean addNode(GNode A,int peso)
	{
		return addNode(A.index(),peso);
	}
	
	/** Añade un arrays de &iacute;ndices de nodos a la lista de adyacencia definiendo el peso
	 @param	nodos	Array de nodos que se añaden
	 @param	pesos 	Pesos para llegar a esos nodos
	 */
	public void addNode(int[] nodos, int[] pesos)
	{
		for (int i=0; i<nodos.length; i++)
			addNode(nodos[i],pesos[i]);
	}
	
	/** Añade un &iacute;ndice de un nodo a la lista de adyacencia definiendo el peso
	 @param	nodo	Indice del nodo al que se añade
	 @param	peso 	Peso para llegar al nodo definido
	 */
	public boolean addNode(int nodo, int peso)
	{
		for(int i=0;i<nList;i++) 
			if(((Integer)list.get(i)).intValue() == nodo)	return(false);	// Comprueba si no esta ya metido
			
		list.addElement(new Integer(nodo));
		pesos.addElement(new Integer(peso));	
		nList++;
		return (true);
	}
	
	
	/** Devuelve el Indice de un nodo de la lista (-1 no hay Node)
	 @param		index 	Indice de la lista de adyacencia
	 @return		Indice del nodo contenido en la lista
	 */
	public int getList(int index)
	{
		try
		{
			return( ((Integer)list.get(index)).intValue() );
		}catch(Exception e){return -1;}
	}
	
	/** Devuelve el peso de un nodo de la lista (-1 no hay Node)
	 @param		index 	Indice de la lista de adyacencia
	 @return		Peso del nodo contenido en la lista de pesos
	 */
	public int getPeso(int index)
	{
		try
		{
			return( ((Integer)pesos.get(index)).intValue() );
		}catch(Exception e){return -1;}
	}
	
	
	/** Representa un nodo con su nombre y el &iacute;ndice identificativo
	 @return		Cadena con el nombre y el &iacute;ndice
	 */
	public String toString()
	{
		return name + "("+ index + ")";
	}
}
