/*
 * (c) 2003 Jose Antonio Marin Meseguer
 * (c) 2004 Humberto Martinez Barbera
 */

package tclib.utils.graphs;

import java.util.*;
import java.io.*;

public class Graph
{
	Vector 	tabla;
	int 	nNodes;
	
	protected boolean debug	= false;

	/** Constructor por defecto */
	public Graph ()
	{
		tabla 	= new Vector();
		nNodes 	= 0;
	}

	public void setDebug (boolean deb)
	{
		debug = deb;
	}
	
	public int numNodes ()	{ return nNodes; }

	/** Une dos nodos desde A hasta B, añadiendolos en la lista del grafo en caso de que no existan
		@param	A	Node origen
		@param	B	Node destino
	*/
	public void join(GNode A, GNode B)
	{
		insNode(A);
		insNode(B);
		A.addNode(B);
	}

	/** Une dos nodos desde A hasta B con un peso determinado, añadiendolos en la lista del grafo en caso de que no existan
		@param	A		Node origen
		@param	B		Node destino
		@param	peso	Peso del arco (solo indices negativos)
	*/
	public void join(GNode A, GNode B, int peso)
	{
		insNode(A);
		insNode(B);
		A.addNode(B,peso);
	}

	/** Añade un vector de arrays al grafo, siendo el primer Node del vector el nodo origen y los demas los Nodes adyacentes.
		@param	arrayNodes	Array de nodos
	*/
	public void join(Vector arrayNodes)
	{
		if(arrayNodes.isEmpty()) return;
		
		GNode principal;
		GNode otros;
	
		int peso = 0;
		principal = (GNode)arrayNodes.get(0);
		insNode(principal);
		for(int i = 1; i<(arrayNodes.size()+1)/2 ; i++)
		{
			otros = (GNode)arrayNodes.get(i);
			peso = ((Integer)arrayNodes.get((arrayNodes.size()-1)/2+i)).intValue();
			principal.addNode(insNode(otros),peso);
		}
		modNode(principal);
	}


	/** Inserta un nodo en la lista de Nodes del grafo, en caso de que no exista.
		@param	A		Node insertado en el grafo
		@return			Indice identificativo del nodo insertado
	*/
	public int insNode(GNode A)
	{
		int index = -1;
		String name = A.getLabel();
		boolean newNode = true;
		GNode aux;
		
		for(int i=0; i<nNodes; i++)
		{
			aux = (GNode)tabla.get(i);
			if (aux.getLabel().equals(name))	
			{
				newNode = false;
				index = i;
			}	
		}

		if(newNode == true)
		{
			A.setIndex(nNodes);
			tabla.addElement(A);						// Añade el Node a la tabla
			index = nNodes;
			nNodes++;
		}
		return index;
	}

	/** Modifica (o inserta en caso de que exista) un nodo en la lista de Nodes del grafo. El &iacute;ndice identificativo no varia.
		@param	A		Node que se quiere modificar
		@return			Indice identificativo del nodo modificado
	*/
	public int modNode(GNode A)
	{
		boolean mod = false;
		String name = A.getLabel();
		GNode aux;
	
		for(int i=0; i<nNodes; i++)
		{
			aux = (GNode)tabla.get(i);
			if (aux.getLabel().equals(name))
			{
				mod = true;
				A.setIndex(aux.index());
				tabla.set(i,A);						// Modifica el Node de la tabla
				return i;
			}	
		}
		if( mod == false)	 return( insNode(A) );
		return -1;
	}


	/** Devuelve el Node del grafo a partir de su &iacute;ndice
		@param	index	Indice del nodo
		@return			Node correspondiente a ese indice (null en caso de que no exista)
	*/
	public GNode getNode(int index)
	{
		if(index<nNodes)	return((GNode)tabla.get(index));
		else				return null;
	}
 
	/** Devuelve el Node del grafo a partir de su nombre identificativo
		@param	name	Nombre del nodo
		@return			Node correspondiente a ese &iacute;ndice (null en caso de que no exista)
	*/
	public GNode getNode(String name)
	{
		for (int i=0; i<nNodes; i++)
			if( (getNode(i).getLabel()).equals(name))
				return((GNode)tabla.get(i));
		return null;
	}
	
	public int indNode (String name)
	{
		for (int i=0; i<nNodes; i++)
			if( (getNode(i).getLabel()).equals(name))
				return (i);
		
		return (-1);
	}


////////////////// Metodos para Calcular distancias minimas

	/** Calcula las distancias m&iacute;nimas de un nodo Origen hacia los demas nodos sin tener en cuenta los pesos
		@param	nodoOrigen 	Indice identificativo del nodo origen de donde se calculan las distancias
		@return				Array con el valor de la distancia desde el nodo Origen (el &iacute;ndice del array se corresponde con el indice de los Nodes) 
	*/
	public int[] sinPesosMin(int nodoOrigen)
	{
		int[] mask 		= new int[nNodes];							// Array para marcar los nodos

		int ipila=0;
		int[] pilaNodes		= new int[nNodes];

		int v;
		for(int i = 0; i<nNodes ; i++) mask[i] = -1;				// Se inicializa a -1
		mask[nodoOrigen]	= 0;									// Se marca el origen
		pilaNodes[ipila++] 	= nodoOrigen;							// Mete Node inicial en cola
	
		while(ipila > 0)
		{
			v = pilaNodes[--ipila];									// Saca Node de la cola
	
			for (int i=0; i < getNode(v).nList() ; i++)
			{				
				// Añade en cola los nodos Adyacentes
				if(mask[ getNode(v).getList(i) ] == -1)
				{
					mask[ getNode(v).getList(i) ] =mask[v]+1;
					pilaNodes[ipila++]=getNode(v).getList(i);		// Mete en cola los nodos Adyacentes en caso de que no esten marcados
				}
			}
		}
		return(mask);
	}


	/** Algoritmo de Dijkstra para calcular las distancias m&iacute;nimas desde el nodoOrigen a los demas puntos con arcos ponderados
		@param	nodoOrigen 	Indice identificativo del nodo origen de donde se calculan las distancias
		@return				Devuelve un array de dos filas, donde cada columna representa un nodo. La primera fila da informacion sobre la ruta que se debe 
							seguir para obtener m&iacute;nimo costo y la segunda fila da la distancia de cada nodo respecto al nodo origenArray con el valor de la distancia desde el
		 					nodo Origen (el &iacute;ndice del array se corresponde con el &iacute;ndice de los Nodes). 
	*/
	public int[][] pathMin(int nodoOrigen)
	{
		int[] path = new int[nNodes];		// Vector del camino minimo
		int[] dist = new int[nNodes];		// Vector con la distancia del nodo origen
		int[][] result =  new int[2][];
		boolean[] colaPrio = new boolean[nNodes];
		int icola, minIndex=0;
		
		Arrays.fill(path,nodoOrigen);				// Se inicia el vector camino con el nodo Origen
		Arrays.fill(dist,Integer.MAX_VALUE);		// Distancia inicial maxima
		dist[nodoOrigen] = 0;
		for(int i=0;i<getNode(nodoOrigen).nList();i++)	dist[getNode(nodoOrigen).getList(i)] = getNode(nodoOrigen).getPeso(i);	

		Arrays.fill(colaPrio,true); //Se mete en la cola todos los elementos menos el origen
		colaPrio[nodoOrigen]=false;
		icola = nNodes-1;

		while(icola != 0)
		{
			// Se calcula el Node perteneciente a la Cola, con menor distancia del origen 
			for(int i=0,min=Integer.MAX_VALUE; i<nNodes; i++)
			{
				if(colaPrio[i] == true)
				{
					min = Math.min(dist[i],min);
					if(min == dist[i]) minIndex = i;
				}
			}

			colaPrio[minIndex]=false;		// Se quita el Node minimo de la Cola	
			icola --;

			for(int i=0;i<getNode(minIndex).nList();i++)
				if(colaPrio[ getNode(minIndex).getList(i) ] == true)
					if( dist[minIndex]+getNode(minIndex).getPeso(i) < dist[getNode(minIndex).getList(i)] )
					{
						dist[getNode(minIndex).getList(i)] = dist[minIndex]+getNode(minIndex).getPeso(i);
						path[getNode(minIndex).getList(i)] = minIndex;
					}
		}

		result[0] = path;
		result[1] = dist;
		return result;
	}



	/////////////////     Metodos para recorrer el Graph		
	/** Recorre el grafo con una b&uacute;squeda en Anchura
		@param	nodoOrigen 	Indice identificativo del nodo origen de donde empieza el recorrido
		@return				Array que indica el orden seguido en la ruta de b&uacute;squeda en Anchura. El &iacute;ndice del array se corresponde
						con el indice identificativo del nodo. Cuando un elemento vale -1 significa que no ha sido recorrido
	*/
	public int[] busquedaAnch(int nodoOrigen)
	{
		int[] path 			= new int[nNodes];						// Orden seguido por el recurrido en Profundidad
		int	  ipath = 0;											// Indice de la ruta

		int[] mask 			= new int[nNodes];						// Array para marcar los nodos
		int[] colaNodes		= new int[nNodes*2];					// Cola de nodos
		int   icola=0;												// Inicio de la cola
		int   fcola=0;												// final de la cola

		int aux;

		for(int i = 0; i<nNodes ; i++) 
		{
			mask[i] = 1;											// Marcar los nodos en estado preparado
			path[i] = -1;											// Marcar ruta a -1
		}

		colaNodes[icola] =  nodoOrigen;fcola++;						// Mete el nodo origen en pila

		mask[nodoOrigen] = 2;										// y cambia a estado espera

		while(icola != fcola )
		{										// Mientras la pila no se vacia
			aux = colaNodes[icola++];								// Saca nodo de la pila 
			path[aux] = ipath++;
			mask[aux] = 3;											// Pone a estado procesado
			for (int i=0; i < getNode(aux).nList() ; i++)
			{	// Añade en cola los nodos Adyacentes
				if(mask[ getNode(aux).getList(i) ] == 1)
				{
					mask[ getNode(aux).getList(i) ] = 2;			// Cambia a modo espera
					colaNodes[fcola++]=getNode(aux).getList(i);		// Mete en cola los nodos Adyacentes
				}
			}
		}
		return path;
	}

		
	/** Recorre el grafo con una b&uacute;squeda en Profundidad
		@param	nodoOrigen 	Indice identificativo del nodo origen de donde empieza el recorrido
		@return				Array que indica el orden seguido en la ruta de busqueda en Profundidad. El &iacute;ndice del array se corresponde
							con el &iacute;ndice identificativo del nodo. Cuando un elemento vale -1 significa que no ha sido recorrido
	*/
	public int[] busquedaProf(int nodoOrigen)
	{
		int[] path 			= new int[nNodes];						// Orden seguido por el recurrido en Profundidad
		int	  ipath = 0;											// Indice de la ruta

		int[] mask 			= new int[nNodes];						// Array para marcar los nodos
		int[] pilaNodes		= new int[nNodes];						// Pila de nodos
		int   ipila=0;												// Indice de la pila
		int   aux;

		for(int i = 0; i<nNodes ; i++) 
		{
			mask[i] = 1;											// Marcar los nodos en estado preparado
			path[i] = -1;											// Marcar ruta a -1
		}
	
		pilaNodes[ipila++] =  nodoOrigen;							// Mete el nodo origen en pila
		mask[nodoOrigen] = 2;										// y cambia a estado espera

		while(ipila > 0)
		{											// Mientras la pila no se vacia
			aux = pilaNodes[--ipila];								// Saca nodo de la pila 
			path[aux] = ipath++;
			mask[aux] = 3;											// Pone a estado procesado
			for (int i=getNode(aux).nList()-1; i >= 0 ; i--)
			{	// Añade en cola los nodos Adyacentes
				if(mask[ getNode(aux).getList(i) ] == 1)
				{
					mask[ getNode(aux).getList(i) ] = 2;			// Cambia a modo espera
					pilaNodes[ipila++]=getNode(aux).getList(i);		// Mete en cola los nodos Adyacentes
				}
			}
		}
		return path;
	}

	public Vector calcPath (int orig, int dest)
	{
		int i;
		int[][] dijsktra;
		LinkedList pathnodes;
		Vector	ret_nodes;
		
		if (debug)
			System.out.println ("Graph.calc_path: from "+getNode (orig).getLabel()+" to "+getNode (dest).getLabel());
		
		dijsktra = pathMin (orig);		
		
		if (debug)
		{
			System.out.print ("\t");
			for (i=0; i < numNodes (); i++)
			{
				System.out.print (getNode (i).getLabel()+"\t");
			}
			System.out.println ();
		
			System.out.print ("path:\t");
			for (i=0; i < dijsktra[0].length; i++)
			{
				System.out.print (dijsktra[0][i]+"\t");	
			} 
			System.out.println ();
			System.out.print ("dist:\t");
			for (i=0; i < dijsktra[1].length; i++)
			{
				System.out.print (dijsktra[1][i]+"\t");	
			}	 
			System.out.println ();
		}
			
		i = dest;
		pathnodes = new LinkedList ();
		while (dijsktra[1][i] != 0)
		{
			pathnodes.addFirst (new Integer(i));
			i = dijsktra[0][i];
		}
		pathnodes.addFirst (new Integer(i));
					
		if (debug)
		{
			System.out.print ("Camino: {");
			for (i = 0; i < pathnodes.size (); i++)
				System.out.print (((Integer)pathnodes.get(i)).intValue()+", ");
			System.out.println ("\b\b}");	
		}
		
		ret_nodes = new Vector(pathnodes.size ());
		
		for (i = 0; i < pathnodes.size (); i++)
			ret_nodes.add(getNode (((Integer)pathnodes.get(i)).intValue()));
		
		if (debug)
		{
			System.out.print ("Camino: {");
			for (i = 0; i < ret_nodes.size(); i++)	
			{
				System.out.print (((GNode)ret_nodes.get(i)).getLabel()+", ");
			}
			System.out.println ("\b\b}");
		}
			
		return (ret_nodes);			
	}

	/** Carga un grafo desde fichero. El formato consta de filas con el nombre del nodo seguido por los nodos adyacentes, y los pesos.
		@param	name		Nombre del fichero a cargar
	*/
	protected void fromFile(String name)
	{
		Vector arrayNodes = new Vector();
		StreamTokenizer st;
		
		try
		{
			BufferedReader br = new BufferedReader(new FileReader(name));
			st = new StreamTokenizer(br);
			st.eolIsSignificant(true);
			while(st.nextToken() != StreamTokenizer.TT_EOF)
			{
				if(st.ttype == StreamTokenizer.TT_WORD )
				{
						GNode A = new GNode(st.sval); 					// Se crea un nodo
						arrayNodes.addElement(A);
						System.out.println("Añadido nodo "+A.getLabel());
				}
				if(st.ttype == StreamTokenizer.TT_NUMBER )
				{
					arrayNodes.addElement(new Integer(new Double(st.nval).intValue()));
					System.out.println("Añadido peso "+st.nval);
				}
				
				if(st.ttype == StreamTokenizer.TT_EOL)
				{
					join(arrayNodes);
					arrayNodes.removeAllElements();
					System.out.println("--------------");
				}
			}
		}catch(FileNotFoundException e)
		{	
			System.out.println("Archivo no encontrado");
			System.exit(0);
			return;
		}
		catch(IOException e)
		{
			System.out.println("Error al abrir el archivo");
			System.exit(1);
			return;
		}
	}

	public void print()
	{
		try
		{
			for(int i=0;i<nNodes;i++)
			{
				System.out.print( getNode(i) + " = { " );
				for(int j=0; j<getNode(i).nList() ; j++)
					System.out.print(getNode(getNode(i).getList(j)) + " ");
				//		System.out.print( ((Node)tabla.get(((Node)tabla.get(i)).getNode(j))).name() + "("+((Node)tabla.get(i)).getNode(j) + ")");	
				System.out.print("}\n");
			}
		}catch(Exception e){e.printStackTrace();}

		int[] busq = sinPesosMin(0);
		for(int i=0;i<nNodes;i++) 	System.out.println(getNode(i)+" = "+busq[i]);
	}
}