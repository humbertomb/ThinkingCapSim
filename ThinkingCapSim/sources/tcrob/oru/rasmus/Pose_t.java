package tcrob.oru.rasmus;
import java.lang.Math;
import tc.vrobot.*;

public class Pose_t
{
	public double X;
	public double Y;
	public double Theta;
	
	Pose_t()
	{
		X=0.0;
		Y=0.0;
		Theta = 0.0;
	}
	
	Pose_t(double x, double y, double theta)
	{
		this.X = x;
		this.Y = y;
		this.Theta = theta;
	}
	

	private void normalize()
	{
		if( Theta> Math.PI )
		{
			double diff = Theta - Math.PI; 
			Theta 		= -(Theta-diff);
		}
		if( Theta < -Math.PI )
		{	
			double diff = -(Theta+Math.PI);
			Theta 		= -(Theta+diff);
		}
	}
	public void add_to_global( Pose_t pos )
	{
		this.X 		= this.X + pos.X * Math.cos(this.Theta) - pos.Y * Math.sin(this.Theta);
		this.Y 		= this.Y + pos.X * Math.sin(this.Theta) - pos.Y * Math.cos(this.Theta); 
		this.Theta 	= this.Theta + pos.Theta;			
		this.normalize();
	}
	public void update_data( RobotData data )
	{
		data.odom_x 	= this.X;
		data.odom_y 	= this.Y;
		data.odom_a 	= this.Theta;
	}
	public void print_values(String source)
	{
		System.out.println("---"+source+"----------------------");
		System.out.println( " X = "+this.X);
		System.out.println( " Y = "+this.Y);
		System.out.println( " A = "+(180*this.Theta/Math.PI)+"  Grader");
		System.out.println();
	}
}