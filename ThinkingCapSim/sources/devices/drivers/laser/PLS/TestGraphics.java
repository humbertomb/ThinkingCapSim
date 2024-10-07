package devices.drivers.laser.PLS;

import devices.drivers.laser.*;
import java.awt.*;            // Para Graphics, etc.
import java.awt.event.*;      // Para Eventos, etc.
import java.awt.geom.*;       // Para Ellipse2D, etc.
import javax.swing.*;         // Para JPanel, etc.

public class TestGraphics extends JPanel {
  
	public int winx = 800;
	public int winy = 500;
	public Laser l;
	public double x1,x2,y1,y2;
	public double k2 = 50;
	public double k1;
	public boolean firstime = true;
    final static BasicStroke stroke = new BasicStroke(2.0f);
    final static BasicStroke wideStroke = new BasicStroke(8.0f);

    final static float dash1[] = {10.0f};
    final static BasicStroke dashed = new BasicStroke(1.0f, 
                                                      BasicStroke.CAP_BUTT, 
                                                      BasicStroke.JOIN_MITER, 
                                                      10.0f, dash1, 0.0f);

	public long st;
	
	
  public void paintComponent( Graphics g ) {
    Line2D line = new Line2D.Double(0,winy,winx,winy);
    
    double [] datos;
    datos = null;
    clear( g );
    Graphics2D g2d = (Graphics2D)g;
    
    if(firstime == true){
    System.out.println("Conectando con el Laser");
    LaserInit();
    firstime = false;
    }
    
    try{
		st = System.currentTimeMillis();
		datos = l.getLaserData ();
		st = System.currentTimeMillis()-st;
		//Thread.currentThread().sleep (500);
		//l.sendCommand((byte)0x31);
		//Thread.currentThread().sleep (50);
		//l.sendCommand((byte)0x31);
		//l.RestartPort();

	}catch (Exception e){
		e.printStackTrace ();
		System.out.println ("Exception: "+e);
		System.exit (0);
	}
	
	g2d.setPaint( Color.blue );
	g2d.setStroke(dashed);

	g2d.draw( new Line2D.Double(0,winy,winx,winy) );
	g2d.draw( new Line2D.Double(0.5*winx,winy,0.5*winx + 10*k2*Math.cos(Math.PI/2), winy-10*k2*Math.sin(Math.PI/2) ));
	g2d.draw( new Line2D.Double(0.5*winx,winy,0.5*winx + 10*k2*Math.cos(Math.PI/4), winy-10*k2*Math.sin(Math.PI/4) ));
	g2d.draw( new Line2D.Double(0.5*winx,winy,0.5*winx + 10*k2*Math.cos(3*Math.PI/4), winy-10*k2*Math.sin(3*Math.PI/4) ));

	
	for(int i = 1;i<=10;i++)
	g2d.draw(new Arc2D.Double(0.5*winx-k2*i, winy-k2*i, k2*2*i, k2*2*i, 0, 180, Arc2D.OPEN));

	g2d.setPaint( Color.red );
	g2d.setStroke(stroke);

	
	if(datos != null){
		k1 = Math.PI/datos.length;
        System.out.println("Datos recibidos ("+st+"ms)");
    	line.setLine(0.5*winx,winy,0.5*winx+k2*datos[0],winy);
    	g2d.draw( line );
    	for(int i=0; i<(datos.length-1); i++){
    		x1 = k2*datos[i]*Math.cos(i*k1);
    		y1 = k2*datos[i]*Math.sin(i*k1);
    		x2 = k2*datos[i+1]*Math.cos((i+1)*k1);
    		y2 = k2*datos[i+1]*Math.sin((i+1)*k1);
    		line.setLine(0.5*winx+x1,winy-y1,0.5*winx+x2,winy-y2);
    		g2d.draw( line );
   		}

    	line.setLine(0.5*winx,winy,0.5*winx-k2*datos[datos.length-1],winy);
    	g2d.draw( line );
   		
    }else{
    	System.out.println("Error al recibir datos");
    	return;
    }
   
   
   }
   

  
  protected void LaserInit(){
	String LASER_PLS 	=	"devices.drivers.laser.PLS.PLS|COM2";
		try
		{
			l = Laser.getLaser (LASER_PLS);
			if (l == null) System.out.println ("l es null");
			else{
				System.out.println ("l="+l);

   				//l.sendCommand((byte)0x31);
   				//l.sendCommand((byte)0x20,(byte)0x24);
   				
			}
		}catch (Exception e)
		{
			e.printStackTrace ();
			System.out.println ("Exception: "+e);
			System.exit (0);
		}
		
	}


  
  
  // Este método permite borrar la imagen que se crea en el buffer
  // oculto, porque por defecto se utiliza el doble buffer
  protected void clear( Graphics g) {
    super.paintComponent( g );
    }



  // Método principal de la clase
  public static void main( String[] args ) {
    JFrame ventana = new JFrame( "TEST para el LASER PLS" );
    ventana.setSize( 800,600 );
    // Receptor de eventos de cierre de la ventana
    ventana.addWindowListener( new WindowAdapter() {
      public void windowClosing( WindowEvent evt ) {
        System.exit( 0 );
        }
      } );

    Container ejemplo = new TestGraphics();    
    ventana.setContentPane( ejemplo );
    ventana.setVisible( true );
    while(true){	
    	ventana.repaint();
    }
    }
  }