/*
 * Created on 28-jun-2005
 *
 * TODO To change the template for this generated file go to
 * Window - Preferences - Java - Code Style - Code Templates
 */
package tcapps.tcsim.simul.objects;

import java.lang.reflect.Constructor;
import java.util.Enumeration;
import java.util.Hashtable;
import java.util.Properties;
import java.util.StringTokenizer;

import tc.runtime.thread.StdThread;
import tc.shared.linda.ItemConfig;
import tc.shared.linda.ItemDebug;
import tc.shared.linda.Linda;
import tc.shared.linda.Tuple;
import tcapps.tcsim.simul.ItemPallet;
import tcapps.tcsim.simul.Simulator;
import devices.pos.Position;
import wucore.utils.geom.Point3;

/**
 * @author SergioPC
 *
 * TODO To change the template for this generated type comment go to
 * Window - Preferences - Java - Code Style - Code Templates
 */
public class SimMultiCargo extends StdThread {
	
	public static final double DISTAGVDOCK	 = 0.75;			//Distancia entre la posicion de dejada de un pallet por parte de un agv y la posicion exacta del pallet en el dock 
	public static final double DISTAGVPALLET = 1.5;
	public static final double DISTROBOTPOS  = 3.0;	
	
	Simulator	simul;
	
	public		Hashtable 	pallets = null;					//Pallets que hay en simulador 3D actualmente
	protected 	Hashtable	typepallets = null;				//Tipos de pallets que pueden aparecer en el simulador 3D
	
	
	
//	 Constructors
	public SimMultiCargo (Properties props, Linda linda, Simulator simul)
	{
		super (props, linda);
		
//		this.r_id		= robotid;
		this.simul		= simul;
		pallets			= new Hashtable();
	}
	
	// Instance methods
	protected void initialise (Properties props){
		String prop;
		StringTokenizer	st;
		PalletType	pt;
		int numtipospallets,typepallet;
		
//		System.out.println("SimMultiCargo initialise()");
		
//		Obtener los tipos de pallets que se utilizaran en la escena
		numtipospallets=Integer.parseInt (props.getProperty ("NOBJECTS","0"));
		
		typepallets = new Hashtable();
		for(int i=0;i<numtipospallets;i++){
			prop		= props.getProperty ("OBJECT"+i);
			st		= new StringTokenizer (prop,", \t");
			typepallet=Integer.parseInt(st.nextToken());
			if(!ItemPallet.strTypePallet(typepallet).equalsIgnoreCase("UNKNOWN")){
				pt=new PalletType(typepallet,st.nextToken(),st.nextToken());
				
				typepallets.put(new Integer(pt.getType()),pt);	
			}else{
				System.err.println("--[SimMultiCargo] tipo de pallet desconocido "+typepallet);
			}
		}
		
		simul.setSimMultiCargo(this);
	}
	public final void step (long ctime){}
	
	public SimObject createPallet(String idpallet,Position pos,int typepallet){
		Class			tclass;
		Constructor		cons;
		Class[]			types;
		Object[]		params;
		SimObject 		obj = null;		
		
		PalletType		pt;
		int 			idobject;

		pt=(PalletType)typepallets.get(new Integer(typepallet));
		if(pt==null){
			System.out.println("--[SimMultiCargo] Tipo de pallet desconocido "+ItemPallet.strTypePallet(typepallet));
			return null;
		}
		
		try
		{
			tclass		= Class.forName (pt.getClassName());
			types		= new Class[1];
			types[0]	= Class.forName ("java.lang.String");        
			cons		= tclass.getConstructor (types);
			params		= new Object[1];
			params[0]	= pt.getDescFile();        		
			obj			= (SimObject) cons.newInstance (params);		
		} catch (Exception e) { e.printStackTrace (); }

		obj.odesc.label = idpallet;
		obj.odesc.pos.x (pos.x());
		obj.odesc.pos.y (pos.y());
		obj.odesc.pos.z (pos.z());
		obj.odesc.a = Math.toRadians (pos.alpha());
//		so.SPEED= Double.parseDouble (st.nextToken());	
		
		
		idobject=simul.allocIcon ();
//		pallets.put(new Integer(idobject),obj);
//		OBJS[numobjects]	= obj;
//		OBJICONS[numobjects] = ;

		simul.moveIcon (idobject, obj.odesc.icon,obj.odesc.pos.x(), obj.odesc.pos.y(), obj.odesc.a);
		
//		simul.VISDATA[numobjects]	= new VisionData ();
//		simul.VISDATA.put(new Integer(idobject),new VisionData());

//		numobjects++;
		return obj;
		
	}
	
	public int delPallet(String idpallet,Position pos){
		int id,idso=-1;
		SimObject	so;
		
//		System.out.println("SimMultiCargo delPallet("+idpallet+","+pos+")");
		if(idpallet.equalsIgnoreCase(PalletType.PALLET_UNKNOWN)){
			id=getUnkownPalletInPos(new Point3(pos.x(),pos.y(),pos.z()));
			idpallet=PalletType.PALLET_UNKNOWN+id;
		}
		so=(SimObject)pallets.remove(idpallet);
		if(so!=null){
			idso=so.idsimul;
		}else{
			System.out.println("--[SimMultiCargo] no se encuentra el SimObject idpallet="+idpallet);
		}
		return idso;
	}
	private int getUnkownPalletInPos(Point3 pos){
		Enumeration enu;
		int 		id=-1;
		SimObject	so;
		
//		System.out.println("SimMultiCargo getUnknownPalletInPos("+pos+") ");
		for(enu=pallets.elements();enu.hasMoreElements();){
			so=(SimObject)enu.nextElement();
//			System.out.println("\t so.pos="+so.odesc.pos+" dist="+pos.distance(so.odesc.pos));
			if(pos.distance(so.odesc.pos)<DISTAGVPALLET){
				return so.idsimul;
			}
		}
		return id;
	}
	/** Indicates that the robot with id "robotid" has executed a pick operation. 
	 The nearest object will be marked as picked by the robot */
	public void pick_object(int robotid, double z){
		SimObject 	so;
		Position 	posrobot;
		
//		System.out.println("SimMultiCargo: pick_object("+robotid+","+z+")");
		
		posrobot=new Position(simul.MODEL[robotid].real_x,simul.MODEL[robotid].real_y,z);
		
		if((so=getNearestPalletToPos(posrobot))!=null){
			if(so instanceof SimCargo){
				((SimCargo)so).pick(simul.lastRobotData[robotid].real_x,simul.lastRobotData[robotid].real_y,z,simul.lastRobotData[robotid].real_a);
				simul.palletPicked[robotid]=((SimCargo)so).namepallet;	
			}else{
				System.out.println("--[SimMultiCargo] pick_object("+robotid+","+z+") No hay un pallet cerca del robot de tipo SimCargo so="+so.getClass()+" posrobot="+posrobot);
			}
		}else{
//			System.out.println("--[SimMultiCargo] pick_object("+robotid+","+z+") No hay un pallet cerca del robot posrobot="+posrobot);
		}
	}
	public SimObject getNearestPalletToPos(Position pos){
		Enumeration enu;
		SimObject	so=null,ret=null;
		double 		dist_min,dist_aux;
		
		dist_min=Double.MAX_VALUE;
//		System.out.println("SimMultiCargo getNearestPalletToPos("+pos+") ");
		for(enu=pallets.elements();enu.hasMoreElements();){
			so=(SimObject)enu.nextElement();
//			System.out.println("\t so.pos="+so.odesc.pos+" dist="+pos.distance(so.odesc.pos));
			dist_aux=pos.distance(so.odesc.pos);
			if(dist_aux<DISTAGVPALLET){
				if(dist_aux<dist_min){
					dist_min=dist_aux;
					ret=so;
				}
			}
		}
		return ret;
	}
	public int getNearestRobotToPos(Position pos){
		int ret=-1;
		double min=Double.MAX_VALUE,aux=0;
		
		for(int i=0;i<simul.MODEL.length;i++){
			if(simul.MODEL[i]!=null){
				aux=pos.distance(simul.MODEL[i].real_x,simul.MODEL[i].real_y);
				if(aux<DISTROBOTPOS){
					if(aux<min){
						min=aux;
						ret=i;
					}
				}
			}
		}
//		System.out.println("  [SimMultiCargo]:getNearestRobotToPos("+pos+") el mas cercano a "+aux);
		return ret;
	}
	
	/** Indicates that the robot with id "robotid" has executed a drop operation.
	 The object picked by the robot will be marked as "not picked" and its position
	 will be its current position */
	public void drop_object(int robotid, double z){
		if (simul.palletPicked[robotid]!=null){
//			System.out.println("SimMultiCargo: drop_object("+robotid+","+z+")");
			((SimCargo) pallets.get(simul.palletPicked[robotid])).drop(z);
			simul.palletPicked[robotid]=null;
		}
	}
	public void startSimul(){
		ItemDebug ditem;
		Tuple	dtuple;
		
		ditem	= new ItemDebug ();
		dtuple	= new Tuple (Tuple.DEBUG, ditem);

		ditem.command (ItemDebug.START, System.currentTimeMillis ());
		linda.write (dtuple);
	}
	public void notify_config (String space, ItemConfig item){}
	public void notify_pallet (String space, ItemPallet item){
		int 		id;
		SimObject	so;
		String 		idpallet;
		int 		robotid;
		
//		System.out.println("  [SimMultiCargo] Recibido tuple PALLETCTRL space="+space+" "+item);
		switch (item.action) {
		case ItemPallet.ADD:

				so=createPallet(item.idpallet,positionAgvToPositionDock(item.position),item.typepallet);
				if(so!=null){
					id=simul.addPallet(so,positionAgvToPositionDock(item.position));
			
					so.idsimul=id;
					idpallet=item.idpallet;
					if(item.idpallet.equalsIgnoreCase(PalletType.PALLET_UNKNOWN)){
						idpallet=PalletType.PALLET_UNKNOWN+id;
					}
					((SimCargo)so).setNamePallet(idpallet);
//					System.out.println("SimMultiCargo nuevo pallet "+idpallet+" id="+so.idsimul);
					pallets.put(idpallet,so);
//					Si el pallet se asigna a un agv, hacer que el robot lo coja
					if(item.destiny==ItemPallet.AGV){
						robotid=getNearestRobotToPos(item.position);
						if(robotid!=-1){
//							System.out.println("SimRobot:notify_pallet space="+space+" itemmotion="+item.toString());
							pick_object(robotid,simul.MODEL[robotid].real_a);
						}
//						else{
//							System.out.println("--[SimMultiCargo]3 No hay un robot cerca de la posicion "+item.position);
//						}
					}
				}else{
					System.out.println("--SimMultiCargo so==null");
				}
			break;
		case ItemPallet.DEL:
//				Si el pallet esta asignado a un agv, hacer que el robot lo suelte
				robotid=getNearestRobotToPos(item.position);
				if(robotid!=-1){
					drop_object(robotid,simul.lastRobotData[robotid].fork);
				}
//				else{
//					System.out.println("--[SimMultiCargo]3 No hay un robot cerca de la posicion "+item.position);
//				}

				if((id=delPallet(item.idpallet,positionAgvToPositionDock(item.position)))!=-1){
//					System.out.println("SimMulticargo eliminar pallet "+item.idpallet+" id="+id);
					simul.delPallet(id);
				}
			break;
		case ItemPallet.MOVE:
				so=(SimObject)pallets.get(item.idpallet);
				so.odesc.pos=new Point3(item.position.x(),item.position.y(),item.position.z());
//				Si el pallet se asigna a un agv, hacer que el robot lo coja
				if(item.destiny==ItemPallet.AGV){
					robotid=getNearestRobotToPos(item.position);
					if(robotid!=-1){
						pick_object(robotid,simul.MODEL[robotid].real_a);
					}
//					else{
//						System.out.println("--[SimMultiCargo]2 No hay un robot cerca de la posicion "+item.position);
//					}
				}
		break;
		case ItemPallet.MOVEANDCHANGETYPE:
//			Quitar el pallet viejo
			if((id=delPallet(item.idpallet,item.position))!=-1){
//				System.out.println("SimMulticargo eliminar pallet "+item.idpallet+" id="+id);
				simul.delPallet(id);
			}
//			Ponerlo en la nueva posicion y con el nuevo tipo(cargado, vacio....)
			so=createPallet(item.idpallet,positionAgvToPositionDock(item.position),item.typepallet);
			if(so!=null){
				id=simul.addPallet(so,positionAgvToPositionDock(item.position));
			
				so.idsimul=id;
				idpallet=item.idpallet;
				if(item.idpallet.equalsIgnoreCase(PalletType.PALLET_UNKNOWN)){
					idpallet=PalletType.PALLET_UNKNOWN+id;
				}
				((SimCargo)so).setNamePallet(idpallet);
//				System.out.println("SimMultiCargo nuevo pallet "+idpallet+" id="+so.idsimul);
				pallets.put(idpallet,so);
			}else{
				System.out.println("--SimMultiCargo so==null");
			}
//			Si el pallet se asigna a un agv, hacer que el robot lo coja
			if(item.destiny==ItemPallet.AGV){
				robotid=getNearestRobotToPos(item.position);
				if(robotid!=-1){
					pick_object(robotid,simul.MODEL[robotid].real_a);
				}
//				else{
//					System.out.println("--[SimMultiCargo]3 No hay un robot cerca de la posicion "+item.position);
//				}
			}
			break;

		case ItemPallet.UNKNOWN_ACTION:
				
			break;
		}
	}
	
	private Position positionAgvToPositionDock(Position pos){
		if(pos.alpha()>Math.PI || pos.alpha()<-Math.PI){
			System.out.println("--[SimMultiCargo] angulo de giro "+pos.alpha()+" incorrecto pos="+pos);
			return pos;
		}
		pos.x(pos.x()-SimMultiCargo.DISTAGVDOCK*Math.cos(pos.alpha()));
		pos.y(pos.y()-SimMultiCargo.DISTAGVDOCK*Math.sin(pos.alpha()));
		return pos;
	}
	
	
}
