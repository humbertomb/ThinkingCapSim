package tcrob.oru.rasmus;

public class RasmusDriver
{
   	// Always Load Library
   static
   {
       System.loadLibrary ("wrapper");
   }
   	
    // JNI methods
    public native boolean connect( String adress, double odom_turn_const );
    public native void disconnect( );
    public native void initalize();
    public native void restart( );
    public native void motor ( double trans, double steer );
    public native void sonars ( double[] son, boolean[] flag );
    public native void lasers(  double[] las) ; //, boolean[] flag );
    public native void compass( );
    public native void stop( );
    public native void gps( );
    public native void talk( String message );
    public native void update( );
   // public native void sensors( float[] state, boolean[] flg );
    public native void odomentry();
    public static native void setPanTilt( int pan, int tilt );
   

    // Oridinary fields
        // Fields for the compass
    public static double direction	= 0.0;		// Compass
    public static double pitch		= 0.0;		// Compass
    public static double roll		= 0.0;		// Compass
    public static boolean cflag		= false;	// Compass
        // Fields for the GPS         
    public static double XPos		= 0.0;      // GPS
    public static double YPos		= 0.0;      // GPS
    public static double Altitude	= 0.0;  	// GPS
    public static boolean gflag		= false;	// GPS
    	// Filed for Odemntry
    public static double odom_x		= 0.0;		//Odomentry
    public static double odom_y		= 0.0;		//Odomentry
    public static double odom_a		= 0.0;		//Odomentry
        
}    
