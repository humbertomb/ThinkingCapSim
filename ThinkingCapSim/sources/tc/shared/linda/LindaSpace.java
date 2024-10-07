/*
 *  Created on 08-mar-2004
 *
 *  To change the template for this generated file go to
 *  Window - Preferences - Java - Code Generation - Code and Comments
 *  2004 Sergio Alem�n
 */
package tc.shared.linda;

import java.util.Enumeration;
import java.util.Hashtable;
import java.util.Vector;


/**
 * @author     Humberto Martinez Barbera
 *
 * To change the template for this generated type comment go to
 * Window - Preferences - Java - Code Generation - Code and Comments
 * @created    13 de julio de 2004
 */
public class LindaSpace {
	/**
	 *  Description of the Field
	 */
	protected final static int MAX_KEYS = 100;
	/**
	 *  Description of the Field
	 */
	protected final static int MAX_CONFIG = 50;

	/**
	 *  Description of the Field
	 */
	protected Hashtable content;
	/**
	 *  Description of the Field
	 */
	protected Hashtable content_config;

	protected Hashtable	ifork_connection;


	//operaciones register

	// Constructors
	/**
	 *Constructor for the LindaSpace object
	 */
	public LindaSpace() {
		content = new Hashtable(MAX_KEYS);
		content_config = new Hashtable(MAX_CONFIG);
		ifork_connection = new Hashtable();
	}


	// Instance methods
	/**
	 *  Description of the Method
	 *
	 * @param  tuple       Description of the Parameter
	 * @param  connection  Description of the Parameter
	 * @return             Description of the Return Value
	 */
	public synchronized boolean write(Tuple tuple, LindaConnection connection) {
		LindaEntry  record;
		LindaEntry  record_any;
		Hashtable   hs;
		Vector v_ler;
		Vector v_misler;
		LindaEntryRegistry ler;
		
		v_misler=new Vector();
//		if(tuple.key.equals("SYNC") && !tuple.space.equals(LindaEntryFilter.ANY)){
//			System.out.println("  [LindaSpace] tupla SYNC space="+tuple.space+" value="+tuple.value+" connection="+connection);
//		}
		if (tuple.key.equals(Tuple.CONFIG)) {
			//crear la tabla hash de las tuplas CONFIG
			hs = (Hashtable) content.get(tuple.key);
			if (hs == null) {
				content.put(tuple.key, content_config);
			}
						
			//Si es la primera escritura, crear el registro LindaEntry
			record = (LindaEntry) content_config.get(tuple.space);
			if (record == null) {
				record = new LindaEntry(tuple.space, tuple.key);
				//Obtener los LindaEntryRegistry de tuplas CONFIG(est�n en la entrada "any")
				record_any = (LindaEntry) content_config.get(LindaEntryFilter.ANY);
				if(record_any!=null){
					v_ler=record_any.listeners;
					Enumeration en=v_ler.elements();
					while(en.hasMoreElements()){
						ler=(LindaEntryRegistry)en.nextElement();
						v_misler.add(new LindaEntryRegistry(ler.filter.pattern,ler.listener));
					}
				}
				record.listeners=v_misler;
				content_config.put(tuple.space, record);
			}
		}
		else {
			record = (LindaEntry) content.get(tuple.key);
			if (record == null) {
				record = new LindaEntry(tuple.space, tuple.key);
				content.put(tuple.key, record);
			}
		}
		record.update_local(tuple);
		record.update_listeners(connection);

		if (tuple.key.equals(Tuple.LINDACTRL)) {
			process_linda(tuple);
		}
		return true;
	}


	/**
	 *  Description of the Method
	 *
	 * @param  template  Description of the Parameter
	 * @param  receiver  Description of the Parameter
	 * @return           Description of the Return Value
	 */
	public synchronized Tuple read(Tuple template, LindaEntryListener receiver) {
		LindaEntry  record  = null;

		if (template.key.equals(Tuple.CONFIG)) {
			record = (LindaEntry) content_config.get(template.space);
		}else{
			record = (LindaEntry) content.get(template.key);
		}
		if (record == null) {
			return null;
		}

		return record.update_remote(receiver);
	}


	/**
	 *  Description of the Method
	 *
	 * @param  template  Description of the Parameter
	 * @param  receiver  Description of the Parameter
	 * @return           Description of the Return Value
	 */
	public synchronized Tuple take(Tuple template, LindaEntryListener receiver) {
		LindaEntry  record  = null;
		Tuple       tuple   = null;
		Tuple 		returntuple = null;

		if (template.key.equals(Tuple.CONFIG)) {
			//System.out.println("LindaSpace:take OPERACION DE TAKE SOBRE CONFIG");
			record = (LindaEntry) content_config.get(template.space);
		}else{
			record = (LindaEntry) content.get(template.key);
		}
		if (record == null) {
			return null;
		}
		tuple = record.update_remote(receiver);
		if(tuple==null){
			return null;
		}
		if(tuple.value==null){
			return null;
		}
		returntuple=new Tuple(tuple.space,tuple.key,tuple.value);
		record.update_local(new Tuple(template.space, template.key, null));
		return returntuple;
	}


	/**
	 *  Description of the Method
	 *
	 * @param  template  Description of the Parameter
	 * @param  listener  Description of the Parameter
	 */
	public synchronized void register(Tuple template, LindaEntryListener listener) {
		LindaEntry   record  = null;
		Hashtable    hs;
		Enumeration  enu    = null;

		if(!template.space.equals(LindaEntryFilter.ANY)){
			ifork_connection.put(template.space,listener);
		}
		// Check for the corresponding record (if non existing, create it)
		//Si la tupla es de tipo CONFIG
		if (template.key.equals(Tuple.CONFIG)) {
			hs = (Hashtable) content.get(template.key);
			if (hs == null) {
				content.put(template.key, content_config);
			}

			//Si la tupla es de tipo "any", se registra en todos los robot
			if (template.space.equals(LindaEntryFilter.ANY)) {
				record = (LindaEntry) content_config.get(template.space);
				if (record == null) {
					record = new LindaEntry(template.space, template.key);
					content_config.put(template.space, record);
				}
				enu = content_config.keys();
				while (enu.hasMoreElements()) {

					record = (LindaEntry) content_config.get((String) enu.nextElement());
					//Update listeners information
					record.register(template.space, listener);
					// Send current data to the listener
					if (record.hasValue()) {
						record.update_listener(listener, listener.getConnection());
					}
				}
			}
			else {
				record = (LindaEntry) content_config.get(template.space);
				if (record == null) {
					record = new LindaEntry(template.space, template.key);
					content_config.put(template.space, record);
				}
				//Update listeners information
				record.register(template.space, listener);
				// Send current data to the listener
				if (record.hasValue()) {
					record.update_listener(listener, listener.getConnection());
				}
			}
		}
		else {
			record = (LindaEntry) content.get(template.key);
			if (record == null) {
				record = new LindaEntry(template.space, template.key);
				content.put(template.key, record);
			}
			//Update listeners information
			record.register(template.space, listener);
			// Send current data to the listener
			if (record.hasValue()) {
				record.update_listener(listener, listener.getConnection());
			}
		}

	}


	/**
	 *  Description of the Method
	 *
	 * @param  template  Description of the Parameter
	 * @param  listener  Description of the Parameter
	 */
	public synchronized void unregister(Tuple template, LindaEntryListener listener) {
		LindaEntry  record;
		Enumeration enu;
		
		if (template.key.equals(Tuple.CONFIG)) {
			if (template.space.equals(LindaEntryFilter.ANY)) {
				enu = content_config.keys();
				while (enu.hasMoreElements()) {
					record = (LindaEntry) content_config.get((String) enu.nextElement());
					record.unregister(template.space, listener);
				}
			}else{
				record = (LindaEntry) content_config.get(template.space);	
				if (record != null) {
					record.unregister(template.space, listener);
				}
			}
		}else{
			record = (LindaEntry) content.get(template.key);
			if (record != null) {
				record.unregister(template.space, listener);
			}			
		}

	}

	public synchronized void unregister(LindaEntryListener listener) {
		LindaEntry  record;
		Enumeration enu,enu1;
		
		String key,key1;
		
		enu = content.keys();
		while (enu.hasMoreElements()) {
			key = (String) enu.nextElement();
			if (key.equals(Tuple.CONFIG)) {
				enu1 = content_config.keys();
				while (enu1.hasMoreElements()) {
					key1 = (String) enu1.nextElement();
					record = (LindaEntry) content_config.get(key1);
//					System.out.println("LindaSpace unregister key="+key+" key1="+key1+" "+record.key());
					record.unregister(listener);
				}
			}
			else {
				record = (LindaEntry) content.get(key);
//				System.out.println("LindaSpace unregister key="+key+" "+record.key());
				record.unregister(listener);
			}
		}
	}

	/**
	 *  Description of the Method
	 *
	 * @param  tuple  Description of the Parameter
	 */
	protected void process_linda(Tuple tuple) {
		ItemLindaCtrl  lctrl;
		LindaEntryListener lel;

		lctrl = (ItemLindaCtrl) tuple.value;

		switch (lctrl.cmd) {
				case ItemLindaCtrl.DELETE:
					Enumeration    enu;
					Enumeration    enu1;
					String         key;
					LindaEntry     entry;

					if (!tuple.space.equals(LindaEntryFilter.ANY)) {
						enu = content.keys();
						while (enu.hasMoreElements()) {
							key = (String) enu.nextElement();
							if(key.equals(Tuple.CONFIG)){
								enu1 = content_config.keys();
								while (enu1.hasMoreElements()) {
									key = (String) enu1.nextElement();
									if(key.equals(tuple.space)){
										content_config.remove(key);	
									}else{
										entry = (LindaEntry) content_config.get(key);
										entry.unregister(tuple.space);
										lel=(LindaEntryListener)ifork_connection.get(tuple.space);
										if(lel!=null)
											entry.unregister(LindaEntryFilter.ANY,lel);
									}
								}
							}else{
								entry = (LindaEntry) content.get(key);
								entry.unregister(tuple.space);
								lel=(LindaEntryListener)ifork_connection.get(tuple.space);
								if(lel!=null)
									entry.unregister(LindaEntryFilter.ANY,lel);
								
							}
							
						}
					}
					break;
				case ItemLindaCtrl.DUMPREG:
					System.out.println(toString(true));
					break;
				case ItemLindaCtrl.DUMPSPC:
					System.out.println(toString(false));
					break;
				case ItemLindaCtrl.TIMEOUT:
				default:
		}
	}


	/**
	 *  Description of the Method
	 *
	 * @param  expand  Description of the Parameter
	 * @return         Description of the Return Value
	 */
	public String toHTML(boolean expand) {
		Enumeration  enu;
		Enumeration  enu1;
		LindaEntry   record;
		String       key;
		String       key1;
		String       output;

		output = "<HTML>";
		enu = content.keys();
		while (enu.hasMoreElements()) {
			key = (String) enu.nextElement();
			if (key.equals(Tuple.CONFIG)) {
				enu1 = content_config.keys();
				while (enu1.hasMoreElements()) {
					key1 = (String) enu1.nextElement();
					record = (LindaEntry) content_config.get(key1);
					output += record.toHTML(expand);
				}
			}
			else {
				record = (LindaEntry) content.get(key);
				output += record.toHTML(expand);
			}
		}
		
		for(enu=ifork_connection.keys();enu.hasMoreElements();){
			String ifork=(String)enu.nextElement();
			output+="<br>"+ifork+" -> "+ifork_connection.get(ifork);
		}
		return output + "</HTML>";
	}


	/**
	 *  Description of the Method
	 *
	 * @param  expand  Description of the Parameter
	 * @return         Description of the Return Value
	 */
	public String toString(boolean expand) {
		Enumeration  enu;
		Enumeration  enu1;
		LindaEntry   record;
		String       key;
		String       key1;
		String       output;

		output = "--------------------------------------------\n";
		enu = content.keys();
		while (enu.hasMoreElements()) {
			key = (String) enu.nextElement();
			if (key.equals(Tuple.CONFIG)) {
				enu1 = content_config.keys();
				while (enu1.hasMoreElements()) {
					key1 = (String) enu1.nextElement();
					record = (LindaEntry) content_config.get(key1);
					output += record.toString(expand);

				}
			}
			else {
				record = (LindaEntry) content.get(key);

				output += record.toString(expand);
			}
		}
		for(enu=ifork_connection.keys();enu.hasMoreElements();){
			String ifork=(String)enu.nextElement();
			output+="\n"+ifork+" -> "+ifork_connection.get(ifork);
		}
		output += "--------------------------------------------\n\n";

		return output;
	}
}
