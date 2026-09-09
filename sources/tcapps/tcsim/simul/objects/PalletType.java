/*
 * Created on 01-jul-2005
 *
 * TODO To change the template for this generated file go to
 * Window - Preferences - Java - Code Style - Code Templates
 */
package tcapps.tcsim.simul.objects;

import tcapps.tcsim.simul.ItemPallet;

/**
 * @author SergioPC
 *
 * TODO To change the template for this generated type comment go to
 * Window - Preferences - Java - Code Style - Code Templates
 */
public class PalletType {

	public static final String PALLET_UNKNOWN = "UNKNOWN";
	public static final int EMPTY	= ItemPallet.FREE_WITH_BOX;
	public static final int FULL	= ItemPallet.LOADED;
	
	protected int type;
	protected String classname;
	protected String descfile;
	
	public PalletType(){
		type=-1;
	}
	public PalletType(int type,String classname,String descfile){
		this.type=type;
		this.classname=classname;
		this.descfile=descfile;
	}
	
	public int getType(){
		return type;
	}
	public String getClassName(){
		return classname;
	}
	public String getDescFile(){
		return descfile;
	}
	
	public void setType(int type){
		this.type=type;
	}
	public void setClassName(String classname){
		this.classname=classname;
	}
	public void setDescFile(String descfile){
		this.descfile=descfile;
	}
	
}
