
package tclib.behaviours.hfsm;

import java.awt.Point;
import java.util.ArrayList;

import org.xml.sax.Attributes;
import org.xml.sax.ContentHandler;
import org.xml.sax.Locator;
import org.xml.sax.SAXException;

import chaos.fsm.gui.MetaState;
import chaos.fsm.gui.State;
import chaos.fsm.gui.Transition;
import chaos.fsm.gui.Utils;



public class XMLParser implements ContentHandler {

	/**
	 * array of root MetaStates
	 */
	private MetaState[] meta;
	
	final static int META = 999;
	
	/**
	 * 
	 */
	private int nivel;
	
	/**
	 * Constructor del miContentHandler que recibe el nodo raiz del arbol que hay que rellenar.
	 * y crea los ArrayList.
	 * 
	 * @param meta
	 */	
	public XMLParser(MetaState m) {
		
		meta = new MetaState[META];
		meta[0] = m;
		nivel = 0;

	}

	/* (non-Javadoc)
	 * @see org.xml.sax.ContentHandler#startDocument()
	 */
	public void startDocument() throws SAXException {
	}

	/* (non-Javadoc)
	 * @see org.xml.sax.ContentHandler#endDocument()
	 */
	public void endDocument() throws SAXException {
	}

	/* (non-Javadoc)
	 * @see org.xml.sax.ContentHandler#startPrefixMapping(java.lang.String, java.lang.String)
	 */
	public void startPrefixMapping(String arg0, String arg1)
		throws SAXException {
	}

	/* (non-Javadoc)
	 * @see org.xml.sax.ContentHandler#endPrefixMapping(java.lang.String)
	 */
	public void endPrefixMapping(String arg0) throws SAXException {
	}

	/* (non-Javadoc)
	 * @see org.xml.sax.ContentHandler#startElement(java.lang.String, java.lang.String, java.lang.String, org.xml.sax.Attributes)
	 */
	public void startElement(String namespaceURI, String localname,
		String qName, Attributes atributos)
	
		throws SAXException {
		if(localname.equals("strategy")){
			Utils.stateCount = Integer.parseInt(atributos.getValue("stateCount"));
			Utils.transitionCount = Integer.parseInt(atributos.getValue("transitionCount"));
			
			meta[0].metaStateCountToName=Integer.parseInt(atributos.getValue("metaStateCountName"));
			meta[0].stateCountToName=Integer.parseInt(atributos.getValue("stateCountName"));
			meta[0].transitionCountToName=Integer.parseInt(atributos.getValue("transitionCountName"));		
		}else
		if(localname.equals("metastate")){
			MetaState ms = new MetaState(atributos.getValue("name"),Integer.parseInt(atributos.getValue("id")),new Point(Integer.parseInt(atributos.getValue("x")),Integer.parseInt(atributos.getValue("y"))));
	
			ms.metaStateCountToName=Integer.parseInt(atributos.getValue("metaStateCountName"));
			ms.stateCountToName=Integer.parseInt(atributos.getValue("stateCountName"));
			ms.transitionCountToName=Integer.parseInt(atributos.getValue("transitionCountName"));		
			
			meta[nivel].addState(ms);
			
			if(Integer.parseInt(atributos.getValue("initial"))==1)
				meta[nivel].setInitialState(ms);
			
			if(Integer.parseInt(atributos.getValue("extern"))==1){
				ms.setExtern(true);
				ms.setPathExtern(atributos.getValue("pathExtern"));
			}else
				ms.setExtern(false);
				
			nivel++;
			meta[nivel] = ms;
			
		}else
			if(localname.equals("state")){
				State s= new State(atributos.getValue("name"),Integer.parseInt(atributos.getValue("id")),new Point(Integer.parseInt(atributos.getValue("x")),Integer.parseInt(atributos.getValue("y"))));
					
				meta[nivel].addState(s);
				if(Integer.parseInt(atributos.getValue("initial"))==1)
					meta[nivel].setInitialState(s);
			}else
				if(localname.equals("transition")){
					Transition t = new Transition(null,atributos.getValue("name"),Integer.parseInt(atributos.getValue("id")),new Point(Integer.parseInt(atributos.getValue("x")),Integer.parseInt(atributos.getValue("y"))));
					t.setPriority(Integer.parseInt(atributos.getValue("priority")));
					
					String fromState = atributos.getValue("from");
					String toState = atributos.getValue("to");

					Object to = findState(toState);
					
					try{
						t.setArrivalState(to);
					}catch(Exception e){
						System.out.println("-> "+ e.getMessage());
					}
					
					Object from = findState(fromState);

					if(State.isState(from))
						((State)from).addTransition(t);
					else
						((MetaState)from).addTransition(t);
				}
				else
					if(localname.equals("privatevariable")){
						String [] vars=new String[5];
						vars[0]=atributos.getValue("name");
						vars[1]=atributos.getValue("type");
						vars[2]=atributos.getValue("numElements");
						vars[3]=atributos.getValue("initValue");
						vars[4]=atributos.getValue("msname");
						
						Utils.privVars.add(vars);
					}
	}


	/* (non-Javadoc)
	 * @see org.xml.sax.ContentHandler#endElement(java.lang.String, java.lang.String, java.lang.String)
	 */
	public void endElement(String namespaceUri, String localname, String qName)
		throws SAXException{
		if(localname.equals("metastate"))
			nivel--;
	}
	
	public Object findState(String id){
		Object o=null;
		boolean notFound=true;

		for(int i=0;i<meta[nivel].getStatesSize() && notFound;i++){
			
			o= meta[nivel].getState(i);
			
			if(!State.isState(o)){ //is MetaState
				if(((MetaState)o).getId()==(new Integer(id).intValue())){
					//System.out.println(((MetaState)o).getName());
					
					notFound=false;
				}
			}else{//is State
				if(((State)o).getId()==(new Integer(id).intValue())){
					//System.out.println(((State)o).getName());
					notFound=false;
				}
			}
		}
		
		return o;
	}
	
	/* (non-Javadoc)
	 * @see org.xml.sax.ContentHandler#characters(char[], int, int)
	 */
	public void characters(char[] cadena, int inicio, int longitud)
		throws SAXException {

	}

	/* (non-Javadoc)
	 * @see org.xml.sax.ContentHandler#ignorableWhitespace(char[], int, int)
	 */
	public void ignorableWhitespace(char[] arg0, int arg1, int arg2)
		throws SAXException {
		//System.out.println("Ignorable white space.");
	}

	/* (non-Javadoc)
	 * @see org.xml.sax.ContentHandler#processingInstruction(java.lang.String, java.lang.String)
	 */
	public void processingInstruction(String arg0, String arg1)
		throws SAXException {
	}

	/* (non-Javadoc)
	 * @see org.xml.sax.ContentHandler#skippedEntity(java.lang.String)
	 */
	public void skippedEntity(String arg0) throws SAXException {
	}

	/* (non-Javadoc)
	 * @see org.xml.sax.ContentHandler#setDocumentLocator(org.xml.sax.Locator)
	 */
	public void setDocumentLocator(Locator arg0) {
		
	}

}
