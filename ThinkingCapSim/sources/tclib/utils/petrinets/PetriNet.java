package tclib.utils.petrinets;

import java.io.*;
import java.util.*;
import java.awt.*;

public class PetriNet
{
	Vector nodes;
	Vector transitions;
	Vector edges;
	String name;
	int stepCount;
	
	
	public PetriNet() {
		this.nodes = new Vector();
		this.transitions = new Vector();
		this.edges = new Vector();
		this.name = "PN";
		this.stepCount = 0;
	}
	
	public void setName(String name){
		this.name = name;
	}
	
	public int getIndexOf(PNNode n) {
		return nodes.indexOf(n);
	}
	
	public int getIndexOf(PNTransition t) {
		return transitions.indexOf(t);
	}
	
	public int getIndexOf(PNEdge e) {
		return edges.indexOf(e);
	}
	
	public boolean equal(PNNode n1, PNNode n2) {
		if (n1 == null || n2 == null)
			return false;
		if (nodes.indexOf(n1) == nodes.indexOf(n2))
			return true;
		return false;
	}
	
	public boolean equal(PNTransition t1, PNTransition t2) {
		if (t1 == null || t2 == null)
			return false;
		if(transitions.indexOf(t1) == transitions.indexOf(t2))
			return true;
		return false;
	}
	
	public boolean equal(PNEdge e1, PNEdge e2) {
		if (e1 == null || e2 == null)
			return false;
		if(edges.indexOf(e1) == edges.indexOf(e2))
			return true;
		return false;
	}
	
	public String getName(){
		return name;
	}
	
	public int numberOfNodes() {
		return nodes.size();
	}
	
	public int numberOfTransitions() {
		return transitions.size();
	}
	
	public int numberOfEdges() {
		return edges.size();
	}
	
	
	public PNNode getNode(int i) {
		PNNode n;
		n = (PNNode) nodes.elementAt(i);
		return n;
	}
	
	public PNTransition getTransition(int i) {
		PNTransition t;
		t = (PNTransition) transitions.elementAt(i);
		return t;
	}
	
	public PNEdge getEdge(int i) {
		PNEdge e;
		e = (PNEdge) edges.elementAt(i);
		return e;
	}
	
	public int getTransIndex(PNTransition t){
		int i;
		int ret = -1;
		
		for (i=0; i< numberOfTransitions(); i++){
			if ( (PNTransition) transitions.elementAt(i) == t) {ret = i;
			break;}
		}
		return ret;
	}
	
	public int getNodeIndex(PNNode n){
		int i;
		int ret = -1;
		
		for (i=0; i< numberOfNodes(); i++){
			if ( (PNNode) nodes.elementAt(i) == n) {ret = i;
			break;}
		}
		return ret;
	}
	
	/*
	 public node getClosestNode(int x, int y) {
	 node    n, cn;
	 double  d;
	 double  cd = Integer.MAX_VALUE;
	 if (this.numberOfNodes() == 0)
	 return null;
	 else
	 cn = this.getNode(0);
	 
	 for (int i = 0; i < this.numberOfNodes(); i++) {
	 n = this.getNode(i);
	 d = n.distance((double) x, (double) y);
	 if (d < cd) {
	 cn = n;
	 cd = d;
	 }
	 
	 }
	 return cn;
	 }
	 
	 public transition getClosestTransition(int x, int y) {
	 transition    t, ct;
	 double  d;
	 double  cd = Integer.MAX_VALUE;
	 if (this.numberOfTransitions() == 0)
	 return null;
	 else
	 ct = this.getTransition(0);
	 
	 for (int i = 0; i < this.numberOfTransitions(); i++) {
	 t = this.getTransition(i);
	 d = t.distance((double) x, (double) y);
	 if (d < cd) {
	 ct = t;
	 cd = d;
	 }
	 
	 }
	 return ct;
	 }
	 
	 public Object getClosestItem(int x, int y) {
	 Object o;
	 o = (Object) getClosestNode(x, y);
	 if (getDistanceToClosestNode(x, y) > getDistanceToClosestTransition(x, y))
	 o = (Object) getClosestTransition(x, y);
	 return o;
	 }
	 
	 */
		
	/*
	 public double getDistanceToClosestNode(int x, int y) {
	 double d = Double.MAX_VALUE;
	 node n = getClosestNode(x, y);
	 if (n != null)
	 d = n.distance(x, y);
	 return d;
	 }
	 
	 public double getDistanceToClosestTransition(int x, int y) {
	 double d = Double.MAX_VALUE;
	 transition t = getClosestTransition(x, y);
	 if (t != null)
	 d = t.distance(x, y);
	 return d;
	 }
	 
	 public double getDistanceToClosestItem(int x, int y) {
	 double dd;
	 double d = getDistanceToClosestNode(x, y);
	 dd = getDistanceToClosestTransition(x, y);
	 if (dd < d)
	 d = dd;
	 return d;
	 }
	 
	 */
	
	public void unhighlightAllTransitions() {
		for (int i = 0; i < numberOfTransitions(); i++) {
			getTransition(i).highlight = false;
		}
	}
	
	public void addNode(){
		PNNode n = new PNNode();
		nodes.addElement(n);
		/*System.out.print("NODE ");
		 System.out.print(n.name);
		 System.out.println(" created !");*/
	}
	
	public boolean addNode(int x, int y) {
		// if (getDistanceToClosestItem(x, y) < 45.0)
		//  return false;
		
		PNNode n = new PNNode();
		n.x(x);
		n.y(y);
		nodes.addElement(n);
		/*System.out.print("NODE ");
		 System.out.print(n.name);
		 System.out.println(" created !");*/
		return true;
	}
	
	public boolean addNode(PNNode n) {
		//   if (getDistanceToClosestItem(n.getX(), n.getY()) < 45.0)
		//    return false;
		nodes.addElement(n);
		//System.out.println("Node "+n.name+" created!");
		return true;
	}
	
	
	public void addTransition() {
		PNTransition t = new PNTransition();
		transitions.addElement(t);
	}
	
	public boolean addTransition(int x, int y) {
		//   if (getDistanceToClosestItem(x, y) < 45.0)
		//    return false;
		
		PNTransition t = new PNTransition();
		t.x(x);
		t.y(y);
		transitions.addElement(t);
		/*System.out.print("Created TRANSITION ");
		 System.out.print(name);
		 System.out.println(" !");*/
		return true;
	}
	
	public boolean addTransition(PNTransition t) {
		//   if (getDistanceToClosestItem(t.getX(), t.getY()) < 45.0)
		//    return false;
		transitions.addElement(t);
		//System.out.println("transition "+t.name+" created!");
		return true;
	}
	
	public PNEdge addEdge(PNEdge e) {
		edges.addElement(e);
		addEdgeProtocol(e);
		return e;
	}
	
	public PNEdge addEdge(int tFrom, int iFrom, int tTo, int iTo, Polygon points) {
		PNEdge e = new PNEdge(tFrom, iFrom, tTo, iTo, points);
		edges.addElement(e);
		addEdgeProtocol(e);
		return e;
	}
	
	public PNEdge addEdge (PNNode node, PNTransition trans)
	{
		return addEdge (PNEdge.NODE, getNodeIndex (node), PNEdge.TRANSITION, getTransIndex (trans), null);
	}
	
	public PNEdge addEdge (PNTransition trans, PNNode node)
	{
		return addEdge (PNEdge.TRANSITION, getTransIndex (trans), PNEdge.NODE, getNodeIndex (node), null);
	}
	
	public String numberToString(int n){
		switch (n) {
		case PNEdge.TRANSITION:
			return "TRANSITION";
		case PNEdge.NODE:
			return "NODE";
		case PNEdge.NOTHING:
			return "NOTHING";
		default:
			return "UNKNOWN";
		}
	}
	
	public void addEdgeProtocol(PNEdge e){
		/*System.out.print("Created edge from ");
		 System.out.print(numberToString(e.getTFrom()));
		 System.out.print(" ");
		 if (e.getTFrom() == edge.TRANSITION)
		 System.out.print(getTransition(e.getIFrom()).name);
		 else if (e.getTFrom() == edge.NODE)
		 System.out.print(getNode(e.getIFrom()).name);
		 System.out.print(" to ");
		 System.out.print(numberToString(e.getTTo()));
		 System.out.print(" ");
		 if (e.getTTo() == edge.TRANSITION)
		 System.out.print(getTransition(e.getITo()).name);
		 else if (e.getTTo() == edge.NODE)
		 System.out.print(getNode(e.getTTo()).name);
		 System.out.println(" ! ");*/
	}
	
	
	public void removeNode(PNNode n){
		int index = nodes.indexOf(n);
		int i;
		int k = 0;
		int todelete[] = new int[nodes.size()];
		
		
		this.nodes.removeElement(n);
		
		/*System.out.print("NODE ");
		 System.out.print(n.name);
		 System.out.println(" removed !");*/
		
		for (i = 0; i < edges.size(); i++) {
			PNEdge e = (PNEdge) edges.elementAt(i);
			if (e.adjust(this, index, PNEdge.NODE))
			{todelete[k] = i; k++;}
		}
		
		for (i = k; i > 0; i--) {
			PNEdge d = (PNEdge) edges.elementAt(todelete[i-1]);
			edges.removeElement(d);
		}
	}
	
	public void removeTransition(PNTransition t){
		int i;
		int k = 0;
		int todelete[] = new int[edges.size()];
		int index = transitions.indexOf(t);
		
		
		/*System.out.print("TRANSITION ");
		 System.out.print(t.name);
		 System.out.println(" removed ! (==> deleting of connected edges)");*/
		
		for (i = 0; i < edges.size(); i++) {
			PNEdge e = (PNEdge) edges.elementAt(i);
			if (e.adjust(this, index, PNEdge.TRANSITION))
			{todelete[k] = i; k++;}
		}
		
		for (i = k; i > 0; i--) {
			PNEdge d = (PNEdge) edges.elementAt(todelete[i-1]);
			edges.removeElement(d);
		}
		this.transitions.removeElement(t);
	}
	
	public boolean removeEdge(PNEdge e){
		removeEdgeProtocol(e);
		edges.removeElement(e);
		return true;
	}
	
	
	public void removeEdgeProtocol(PNEdge e){
		/*System.out.print("Removed edge from ");
		 System.out.print(numberToString(e.getTFrom()));
		 System.out.print(" ");
		 if (e.getTFrom() == edge.TRANSITION) System.out.print(getTransition(e.getIFrom()).name);
		 else if (e.getTFrom() == edge.NODE) System.out.print(getNode(e.getIFrom()).name);
		 System.out.print(" to ");
		 System.out.print(numberToString(e.getTTo()));
		 System.out.print(" ");
		 if (e.getTTo() == edge.TRANSITION) System.out.print(getTransition(e.getITo()).name);
		 else if (e.getTTo() == edge.NODE) System.out.print(getNode(e.getITo()).name);
		 System.out.println(" ! ");*/
	}
	
	public boolean isDead(boolean priorEnabled){
		boolean isdead = true;
		int i;
		for (i=0; i<transitions.size(); i++){
			isdead = ! (((PNTransition) transitions.elementAt(i)).canFire(this, priorEnabled));
			if (isdead == false) break;
		}
		return isdead;
	}
	
	public synchronized Object clone() {
		PetriNet p = new PetriNet();
		p.setName(name);
		p.setStepCount(stepCount);
		for (int i = 0; i < nodes.size(); i++) {
			PNNode n	= new PNNode ();
			n.set ((PNNode) nodes.elementAt(i));
			p.addNode(n);
		}
		for (int i = 0; i < transitions.size(); i++)
		{
			PNTransition t = new PNTransition ();
			t.set ((PNTransition) transitions.elementAt(i));
			p.addTransition(t);
		}
		for (int i = 0; i < edges.size(); i++)
		{
			PNEdge e = new PNEdge ();
			e.set ((PNEdge)edges.elementAt(i));
			p.addEdge(e);
		}
		return p;
	}
	
	public void setStepCount(int s) {
		stepCount = s;
	}
	
	public int getStepCount(){
		return stepCount;          // Fuer die Bildschirmausgabe abzuaendern !!!
	}
	
	public Vector getAllConnectedTrans(PNTransition t){
		int i,j;
		int ind = 0;
		
		PNEdge e,f;
		
		Vector connected = new Vector();
		
		for (i=0; i < numberOfEdges(); i++){
			e = (PNEdge) edges.elementAt(i);
			if ((e.getTTo() == PNEdge.TRANSITION) && (e.getITo() == getTransIndex(t)))
			{ind = e.getIFrom();
			for (j=0; j< numberOfEdges(); j++){
				f = getEdge(j);
				if (( f.getTFrom() == PNEdge.NODE) && (f.getIFrom() == ind)){
					if (! connected.contains(getTransition(f.getITo())))
						connected.addElement(getTransition(f.getITo()));
				}
			}
			}
		}
		return connected;
	}
	
	public Vector getAllConnectedFireableTrans(PNTransition t, boolean prior) {
		Vector vec = getAllConnectedTrans(t);
		for (int i = 0; i < vec.size(); i++) {
			if (! ((PNTransition)vec.elementAt(i)).canFire(this, prior)) {
				vec.removeElementAt(i);
			}
		}
		return vec;
	}
	
	
	public boolean connectedWith(PNNode n, PNTransition t){
		int i;
		PNEdge e;
		boolean ret = false;
		
		for (i=0; i<numberOfEdges(); i++){
			e = (PNEdge) edges.elementAt(i);
			if (e.pointingTo(this,t)){
				if (getNodeIndex(n) == e.getIFrom()) ret = true;
			}
		}
		return ret;
	}
	
	public int getAllWeights(Vector v){
		int i,j;
		PNEdge e;
		PNTransition t = new PNTransition();
		int weights = 0;
		
		for (i=0; i< v.size(); i++){
			t = (PNTransition) v.elementAt(i);
			for (j=0; j< numberOfEdges(); j++){
				e = (PNEdge) getEdge(j);
				if (e.pointingTo(this,t)) weights = weights + e.getWeight();
			}
		}
		return weights;
	}
	
	public int getConnectedItems(Vector v){
		int i,j;
		int items = 0;
		PNTransition t = new PNTransition();
		PNNode n = new PNNode();
		for (i=0; i< v.size(); i++){
			t = (PNTransition) v.elementAt(i);
			for (j=0; j<numberOfNodes(); j++){
				n = (PNNode) nodes.elementAt(j);
				if (connectedWith(n,t)) items = items + n.getTokens();
			}
		}
		return items;
	}
	
	
	public Vector multipleCanFire(Vector v, boolean priorEnabled){     //NEW 22.5.97 AH
		int i,j,size;
		PNTransition s,t;
		boolean b;
		Vector w = new Vector();
		Vector z = new Vector();
		
		PetriNet q = (PetriNet) this.clone();
		
		size = v.size();
		
		for (i=0; i < size; i++){
			t = (PNTransition) v.elementAt(i);
			for (j=0; j < q.numberOfTransitions(); j++){
				s = q.getTransition(j);
				if (s.equals(t)) z.addElement(s);
			}
		}
		
		size = z.size();
		
		for (i=0; i < size; i++){
			t = (PNTransition) z.elementAt(i);
			// System.out.println("3");               //***
			b = t.fire(q, false); // System.out.println(b);
			if (b) {  // System.out.println(t.getName());
				w.addElement(t);
			}
		}
		v.removeAllElements();
		
		for (i=0; i < w.size(); i++){
			t = (PNTransition) w.elementAt(i);
			for (j=0; j < numberOfTransitions(); j++){
				s = getTransition(j);
				if (s.equals(t)) v.addElement(s);
			}
		}
		
		return v;
	}
	
	// static methods
	
	// new 18.5.97 jw
	public static void subVectorOfVector(Vector sub,Vector vec) {
		for (int i = 0; i < sub.size(); i++) {
			vec.removeElement(sub.elementAt(i));
		}
	}

	// Implemented to override fromFile from class Graph
	public void fromFile (String name)
	{
		try
		{
			FileInputStream finput = new FileInputStream (name);
			Properties props = new Properties ();
			props.load (finput);			
			fromProps (props);
		}
		catch (Exception e) { e.printStackTrace (); }	
	}
	
	public void fromProps (Properties props)
	{
		int 				i, num;
		String 			prop;
		StringTokenizer	st;		
		
		// Read in PN nodes
		num = Integer.parseInt (props.getProperty ("NODES", "0"));
		for (i = 0; i < num; i++)
		{
			PNNode			n;
			
			prop		= props.getProperty ("NODE_" + i);
			st		= new StringTokenizer (prop,", \t");

			n		= new PNNode ();
			n.fromPropString (st);
			addNode (n);
		}
		
		// Read in PN transitions
		num = Integer.parseInt (props.getProperty ("TRANS", "0"));
		for (i = 0; i < num; i++)
		{
			PNTransition		t;
			
			prop		= props.getProperty ("TRAN_" + i);
			st		= new StringTokenizer (prop,", \t");

			t		= new PNTransition ();
			t.fromPropString (st);
			addTransition (t);
		}
		
		// Read in PN edges
		num = Integer.parseInt (props.getProperty ("EDGES", "0"));
		for (i = 0; i < num; i++)
		{
			PNEdge			e;
			
			prop		= props.getProperty ("EDGE_" + i);
			st		= new StringTokenizer (prop,", \t");

			e		= new PNEdge ();
			e.fromPropString (st);
			addEdge (e);
		}
	}

	public void toFile (String name)
	{
		int			i;	
		PrintWriter	out;
			
		try
		{
			out = new PrintWriter(new FileOutputStream(name));

			out.println("# ==============================");
			out.println("# NODES");
			out.println("# ==============================");
			out.println ("NODES = " + nodes.size ());		
			out.println();	
			for (i = 0; i < nodes.size (); i++) 
				out.println("NODE_" + i + " = " + ((PNNode) nodes.elementAt (i)).toPropString ());
			out.println();

			out.println("# ==============================");
			out.println("# TRANSITIONS");
			out.println("# ==============================");
			out.println ("TRANS = " + transitions.size ());		
			out.println();	
			for (i = 0; i < transitions.size (); i++) 
				out.println("TRAN_" + i + " = " + ((PNTransition) transitions.elementAt (i)).toPropString ());
			out.println();

			out.println("# ==============================");
			out.println("# EDGES");
			out.println("# ==============================");
			out.println ("EDGES = " + edges.size ());		
			out.println();	
			for (i = 0; i < edges.size (); i++) 
				out.println("EDGE_" + i + " = " + ((PNEdge) edges.elementAt (i)).toPropString ());
			out.println();

			out.println();
			out.close();
		}
		catch (Exception e) { e.printStackTrace (); }	
	}

	public Properties toProps ()
	{
		Properties	props;
		int			i;	

		props	= new Properties ();
		
		props.setProperty ("NODES", new Integer (nodes.size ()).toString ());		
		for (i = 0; i < nodes.size (); i++) 
			props.setProperty ("NODE_" + i, ((PNNode) nodes.elementAt (i)).toPropString ());
		
		props.setProperty  ("TRANS", new Integer (transitions.size ()).toString ());		
		for (i = 0; i < transitions.size (); i++) 
			props.setProperty ("TRAN_" + i, ((PNTransition) transitions.elementAt (i)).toPropString ());

		props.setProperty  ("EDGES", new Integer (edges.size ()).toString ());		
		for (i = 0; i < edges.size (); i++) 
			props.setProperty ("EDGE_" + i, ((PNEdge) edges.elementAt (i)).toPropString ());

		return props;
	}
}
