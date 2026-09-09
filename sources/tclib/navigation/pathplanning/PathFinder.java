package tclib.navigation.pathplanning;

import devices.pos.Position;
import wucore.utils.math.poly.Complex;
import wucore.utils.math.poly.Polynomial;

public class PathFinder {
	
	class CalculateSegment {
		
		final private double MAX_SLOPE	= 1000.0;
		
		Position[] segment;
		
		public CalculateSegment() {
			segment = null;
		}
		
		public void calculateSegment(double xi, double yi, double xf, double yf, double m) {
			
			int npoints_sg;
			double Lsg;
			
			Lsg = Math.sqrt( ((xf-xi)*(xf-xi)) + ((yf-yi)*(yf-yi)) );
			npoints_sg = (int) Math.round (Lsg/spacement);
			int i;
			double inc, value;
			if (Math.abs(m) > MAX_SLOPE)
			{
				segment = new Position[npoints_sg];
				
				value = yi;
				inc = (yf-yi) / ((double)(npoints_sg-1));
				for (i = 0; i < segment.length; i++)
				{
					segment[i] = new Position(xf,value,0.0);
					value += inc;
				}
			} else {
				if(Lsg < spacement)
				{
					segment = new Position[2];
					for(i = 0; i < segment.length; i++)
						segment[i] = new Position();
					
					if (xi < xf)
					{
						segment[0].x(xi);
						segment[0].y(yi+m*(segment[0].x()-xi));
						segment[1].x(xf);
						segment[1].y(yi+m*(segment[1].x()-xi));
					} else {
						segment[0].x(xf);
						segment[0].y(yi+m*(segment[0].x()-xi));
						segment[1].x(xi);
						segment[1].y(yi+m*(segment[1].x()-xi));
					}
				} else {
					segment = new Position[npoints_sg];
					
					value = xi;
					inc = (xf-xi) / ((double)(npoints_sg-1));
					for (i = 0; i < segment.length; i++)
					{
						segment[i] = new Position(value, yi+m*(value-xi), 0.0);
						value += inc;
					}
				}
			}
		}
		
		public void deleteLastPoint() {
			int i;
			Position[] segment_tmp;
			segment_tmp = new Position[segment.length - 1];
			for(i = 0; i< segment_tmp.length; i++)
				segment_tmp[i] = new Position(segment[i]);
			
			segment = new Position[segment_tmp.length];
			for(i = 0; i< segment.length; i++)
				segment[i] = new Position(segment_tmp[i]);
		}
		
		public Position[] getSegment() { return segment; }
	}
	
	class CalculateCircle {
		Position midcircle;
		
		public CalculateCircle() {
		}
		
		public Position calculateCenterCircle (double x, double y, double theta, double rv, double gamma, int orientation, int endpoint) {
			midcircle = new Position();
			
			if ( (orientation == LEFT_ORIENTATION) && (endpoint == FIRST_ENDPOINT) )
			{
				midcircle.x(x + (rv * Math.cos(theta - gamma + (0.5*Math.PI))));
				midcircle.y(y + (rv * Math.sin(theta - gamma + (0.5*Math.PI))));
			}
			
			if ( (orientation == LEFT_ORIENTATION) && (endpoint == LAST_ENDPOINT) )
			{
				midcircle.x(x - (rv * Math.cos(theta + gamma - (0.5*Math.PI))));
				midcircle.y(y - (rv * Math.sin(theta + gamma - (0.5*Math.PI))));
			}
			
			if ( (orientation == RIGHT_ORIENTATION) && (endpoint == FIRST_ENDPOINT) )
			{
				midcircle.x(x - (rv * Math.cos(theta + gamma + (0.5*Math.PI))));
				midcircle.y(y - (rv * Math.sin(theta + gamma + (0.5*Math.PI))));
			}
			
			if ( (orientation == RIGHT_ORIENTATION) && (endpoint == LAST_ENDPOINT) )
			{
				midcircle.x(x - (rv * Math.cos(theta - gamma + (0.5*Math.PI))));
				midcircle.y(y - (rv * Math.sin(theta - gamma + (0.5*Math.PI))));
			}
			
			return midcircle;
		}
		
	}
	
	class CalculateCurve {
		double length;
		double sigma0;
		Position[] piece_1;
		Position[] piece_2;
		Position[] piece_3;
		
		double rv, gamma;
		double b_lim;
		
		public CalculateCurve(double rv, double gamma, double b_lim) {
			this.rv = rv;
			this.gamma = gamma;
			this.b_lim = b_lim;
			
			sigma0 = 0.0;
			length = 0.0;
		}
		
		public double getSigma0()	{ return sigma0; }
		
		public double getLength()	{ return length; }
		public Position[] getPiece1() {	return piece_1; }
		public Position[] getPiece2() {	return piece_2; }
		public Position[] getPiece3() {	return piece_3; }
		
		public void deleteLastPoint()
		{
			int i;
			Position[] piece_3_tmp;
			piece_3_tmp = new Position[piece_3.length - 1];
			for(i = 0; i< piece_3_tmp.length; i++)
				piece_3_tmp[i] = new Position(piece_3[i]);
			
			piece_3 = new Position[piece_3_tmp.length];
			for(i = 0; i< piece_3.length; i++)
				piece_3[i] = new Position(piece_3_tmp[i]);
		}
		
		void toLeft(double x, double y, double theta, double x_omega, double y_omega, double xe, double ye) {
			
			piece_1 = null;
			piece_2 = null;
			piece_3 = null;
			length = 0;
			
			int arc_deg;
			arc_deg = 0;
			
			// Angle normalisation
			while(theta >= Math.toRadians(360))		theta -= Math.toRadians(360);
			while(theta < Math.toRadians(0))		theta += Math.toRadians(360);
			
			if(ye >= y_omega)
				length = Math.acos((xe-x_omega)/rv) - gamma - theta + 0.5*Math.PI;
			else
				length = -Math.acos((xe-x_omega)/rv) - gamma - theta + 2.5*Math.PI;
			
			while(length > (2*Math.PI))		length -= (2*Math.PI);
			while(length < 0.0)				length += (2*Math.PI);
			
			int npoints_cl;
			double d1, tmpsin, Lcl;
			double[] ifresnel;
			ifresnel = new double[2];
			
			int i;
			double Lce = 0;
			double[] deflac_cicl_1;
			
			// System.out.println(" length: " + length + " b_lim: " + b_lim);
			
			double aux_b_lim = b_lim;
			if (length < aux_b_lim)
			{
				aux_b_lim = length;
				arc_deg = 1;
				ifresnel = fresnel( Math.sqrt(aux_b_lim/Math.PI) );
				d1 = Math.cos(-0.5*length)*ifresnel[1] + Math.sin(-0.5*length)*(-ifresnel[0]);
				tmpsin = Math.sin(0.5*length + gamma);
				sigma0 = (d1*d1*Math.PI)/(rv*rv*tmpsin*tmpsin);
				Lcl = 2.0*Math.sqrt(length/sigma0);
				npoints_cl = (int) Math.round (0.5*(Lcl/spacement));
				deflac_cicl_1 = new double[npoints_cl];
				double vaux = sigma0/8.0;
				double value = 0.0;
				double inc = Lcl/ ((double)(npoints_cl-1));
				for (i = 0; i < deflac_cicl_1.length; i++)
				{
					// System.out.println(" deflac_cicl_1[" + i + "]: " + value);
					deflac_cicl_1[i] = value*value*vaux;
					value += inc;
				}
			} else {
				Lcl = 2.0*k_max/Math.abs(sigma_max);
				Lce = (Math.abs(length)/k_max)-(k_max/Math.abs(sigma_max));
				npoints_cl = (int) Math.round(0.5*(Lcl/spacement));
				deflac_cicl_1 = new double[npoints_cl];
				double vaux = sigma_max/8.0;
				double value = 0.0;
				double inc = Lcl/ ((double)(npoints_cl-1));
				for (i = 0; i < deflac_cicl_1.length; i++)
				{
					// System.out.println(" deflac_cicl_1[" + i + "]: " + value);
					deflac_cicl_1[i] = value*value*vaux;
					value += inc;
				}
			}
			
			//for (i = 0; i < deflac_cicl_1.length; i++)
			//	System.out.println(" deflac_cicl_1[" + i + "]: " + deflac_cicl_1[i]);
			
			double[] SFr1, CFr1;
			SFr1 = new double[deflac_cicl_1.length];
			CFr1 = new double[deflac_cicl_1.length];
			
			for(i = 0; i < deflac_cicl_1.length; i++)
			{
				ifresnel = fresnel( Math.sqrt((2.0*deflac_cicl_1[i])/Math.PI) );
				SFr1[i] = ifresnel[0];
				CFr1[i] = ifresnel[1];
				
				// System.out.println(" SFr1[" + i + "]: " + SFr1[i]);
				// System.out.println(" CFr1[" + i + "]: " + CFr1[i]);
			}
			
			double[] SFr1_r, CFr1_r;
			SFr1_r = new double[SFr1.length];
			CFr1_r = new double[CFr1.length];
			
			for(i = 0; i < SFr1.length; i++)
			{
				SFr1_r[i] = (SFr1[i]*Math.cos(theta)) + (CFr1[i]*Math.sin(theta));
				CFr1_r[i] = (-SFr1[i]*Math.sin(theta)) + (CFr1[i]*Math.cos(theta));
				
				// System.out.println(" SFr1_r[" + i + "]: " + SFr1_r[i]);
				// System.out.println(" CFr1_r[" + i + "]: " + CFr1_r[i]);
			}
			
			piece_1 = new Position[SFr1_r.length];
			for(i = 0; i< piece_1.length; i++)
				piece_1[i] = new Position();
			
			double daux;
			
			if (arc_deg == 0)
			{
				daux = Math.sqrt(Math.PI*aux_b_lim)/k_max;
				
				for(i = 0; i < piece_1.length; i++)
				{
					piece_1[i].x(x+(CFr1_r[i]*daux));
					piece_1[i].y(y+(SFr1_r[i]*daux));
				}
			} else {
				d1 = Math.cos(0.5*length)*CFr1[CFr1.length-1] + Math.sin(0.5*length)*SFr1[SFr1.length-1];
				tmpsin = Math.sin(0.5*length + gamma);
				sigma0 = (d1*d1*Math.PI)/(rv*rv*tmpsin*tmpsin);
				
				daux = Math.sqrt(Math.PI/sigma0);
				
				for(i = 0; i < piece_1.length; i++)
				{
					piece_1[i].x(x + (CFr1_r[i]*daux));
					piece_1[i].y(y + (SFr1_r[i]*daux));
				}
			}
			
			double b_lim_2;
			b_lim_2 = 0.5*aux_b_lim;
			
			int npoints_ce;
			
			if (arc_deg == 0)
			{
				npoints_ce = (int) Math.round( Lce/spacement );
				
				if(npoints_ce != 0) {
					double value = theta + b_lim_2 - (0.5*Math.PI);
					double inc = Math.abs(length-aux_b_lim)/ ((double)(npoints_ce-1));
					
					piece_2 = new Position[npoints_ce];
					for(i = 0; i< piece_2.length; i++)
					{
						piece_2[i] = new Position();
						piece_2[i].x(x_omega + (Math.cos(value)/k_max));
						piece_2[i].y(y_omega + (Math.sin(value)/k_max));
						value += inc;
					}
				} else {
					double value = theta - b_lim_2 + length - (0.5*Math.PI);
					piece_2 = new Position[1];
					piece_2[0] = new Position();
					piece_2[0].x(x_omega + (Math.cos(value)/k_max));
					piece_2[0].y(y_omega + (Math.sin(value)/k_max));
				}
			}
			
			// for(i = 0; i < piece_2.length; i++)
			//	System.out.println(" position2[" + i + "]: <" + piece_2[i].x() + ", " + piece_2[i].y() + ">");
			
			double[] deflac_cicl_2;
			deflac_cicl_2 = new double[deflac_cicl_1.length];
			
			for(i = 0; i < deflac_cicl_1.length; i++)
				deflac_cicl_2[i] = deflac_cicl_1[deflac_cicl_1.length - i - 1];
			
			// for(i = 0; i < deflac_cicl_2.length; i++)
			//	System.out.println(" deflac_cicl_2[" + i + "]: " + deflac_cicl_2[i]);
			
			double[] SFr2, CFr2;
			SFr2 = new double[deflac_cicl_2.length];
			CFr2 = new double[deflac_cicl_2.length];
			
			for(i = 0; i < deflac_cicl_2.length; i++)
			{
				ifresnel = fresnel( Math.sqrt((2.0*deflac_cicl_2[i])/Math.PI) );
				SFr2[i] = ifresnel[0];
				CFr2[i] = ifresnel[1];
				
				// System.out.println(" SFr2[" + i + "]: " + SFr2[i]);
				// System.out.println(" CFr2[" + i + "]: " + CFr2[i]);
			}
			
			double[] SFr2_r, CFr2_r;
			SFr2_r = new double[SFr2.length];
			CFr2_r = new double[CFr2.length];
			
			for(i = 0; i < SFr2.length; i++)
			{
				SFr2_r[i] = (SFr2[i]*Math.cos(Math.PI-(theta+length))) + (CFr2[i]*Math.sin(Math.PI-(theta+length)));
				CFr2_r[i] = (-SFr2[i]*Math.sin(Math.PI-(theta+length))) + (CFr2[i]*Math.cos(Math.PI-(theta+length)));
				
				// System.out.println(" SFr1_r[" + i + "]: " + SFr1_r[i]);
				// System.out.println(" CFr1_r[" + i + "]: " + CFr1_r[i]);
			}
			
			piece_3 = new Position[SFr2_r.length];
			for(i = 0; i< piece_3.length; i++)
				piece_3[i] = new Position();
			
			if (arc_deg == 0)
			{
				daux = Math.sqrt(Math.PI*aux_b_lim)/k_max;
				
				for(i = 0; i < piece_3.length; i++)
				{
					piece_3[i].x(xe+(CFr2_r[i]*daux));
					piece_3[i].y(ye-(SFr2_r[i]*daux));
				}
			} else {
				daux = Math.sqrt(Math.PI/sigma0);
				
				for(i = 0; i < piece_3.length; i++)
				{
					piece_3[i].x(xe+(CFr2_r[i]*daux));
					piece_3[i].y(ye-(SFr2_r[i]*daux));
				}
			}
			
			// for(i = 0; i < piece_3.length; i++)
			//	System.out.println(" position3[" + i + "]: <" + piece_3[i].x() + ", " + piece_3[i].y() + ">");
			
			if (piece_2 != null)
			{
				if (Math.abs(piece_2[0].x()-piece_1[piece_1.length-1].x()) <= th_sp)
				{
					Position[] piece_1_tmp;
					piece_1_tmp = new Position[piece_1.length - 1];
					for(i = 0; i< piece_1_tmp.length; i++)
						piece_1_tmp[i] = new Position(piece_1[i]);
					
					piece_1 = new Position[piece_1_tmp.length];
					for(i = 0; i< piece_1.length; i++)
						piece_1[i] = new Position(piece_1_tmp[i]);
				}
				
				if (Math.abs(piece_3[0].x()-piece_2[piece_2.length-1].x()) <= th_sp)
				{
					Position[] piece_2_tmp;
					piece_2_tmp = new Position[piece_2.length - 1];
					for(i = 0; i< piece_2_tmp.length; i++)
						piece_2_tmp[i] = new Position(piece_2[i]);
					
					piece_2 = new Position[piece_2_tmp.length];
					for(i = 0; i< piece_2.length; i++)
						piece_2[i] = new Position(piece_2_tmp[i]);
				}
				
			} else {
				
				if (Math.abs(piece_3[0].x()-piece_1[piece_1.length-1].x()) <= th_sp)
				{
					Position[] piece_1_tmp;
					piece_1_tmp = new Position[piece_1.length - 1];
					for(i = 0; i< piece_1_tmp.length; i++)
						piece_1_tmp[i] = new Position(piece_1[i]);
					
					piece_1 = new Position[piece_1_tmp.length];
					for(i = 0; i< piece_1.length; i++)
						piece_1[i] = new Position(piece_1_tmp[i]);
				}
			}
			
			// for(i = 0; i < piece_1.length; i++)
			//		System.out.println(" position1[" + i + "]: <" + piece_1[i].x() + ", " + piece_1[i].y() + ">");
				
		}
		
		void toRight(double x, double y, double theta, double x_omega, double y_omega, double xe, double ye) {
			
			piece_1 = null;
			piece_2 = null;
			piece_3 = null;
			length = 0;
			
			int arc_deg;
			arc_deg = 0;
	        
			// Angle normalisation
			while(theta >= Math.toRadians(360))		theta -= Math.toRadians(360);
			while(theta < Math.toRadians(0))		theta += Math.toRadians(360);
			
			if(ye >= y_omega)
				length = -Math.acos((xe-x_omega)/rv) - gamma + theta + 0.5*Math.PI;
			else
				length = Math.acos((xe-x_omega)/rv) - gamma + theta - 1.5*Math.PI;
			
			while(length > (2*Math.PI))		length -= (2*Math.PI);
			while(length < 0.0)				length += (2*Math.PI);
			
			int npoints_cl;
			double d1, tmpsin, Lcl;
			double[] ifresnel;
			ifresnel = new double[2];
			
			int i;
			double Lce = 0;
			double[] deflac_cicl_1;
			
			// System.out.println(" length: " + length + " b_lim: " + b_lim);
			
			double aux_b_lim = b_lim;
			if (length < aux_b_lim)
			{
				aux_b_lim = length;
				arc_deg = 1;
				ifresnel = fresnel( Math.sqrt(aux_b_lim/Math.PI) );
				
				d1 = Math.cos(-0.5*length)*ifresnel[1] + Math.sin(-0.5*length)*(-ifresnel[0]);
				tmpsin = Math.sin(0.5*length + gamma);
				sigma0 = (d1*d1*Math.PI)/(rv*rv*tmpsin*tmpsin);
				Lcl = 2.0*Math.sqrt(length/sigma0);
				npoints_cl = (int) Math.round (0.5*(Lcl/spacement));
				deflac_cicl_1 = new double[npoints_cl];
				double vaux = sigma0/8.0;
				double value = 0.0;
				double inc = Lcl / ((double)(npoints_cl-1));
				for (i = 0; i < deflac_cicl_1.length; i++)
				{
					// System.out.println(" deflac_cicl_1[" + i + "]: " + value);
					deflac_cicl_1[i] = value*value*vaux;
					value += inc;
				}
			} else {
				Lcl = 2.0*k_max/Math.abs(sigma_max);
				Lce = (Math.abs(length)/k_max)-(k_max/Math.abs(sigma_max));
				npoints_cl = (int) Math.round(0.5*(Lcl/spacement));
				deflac_cicl_1 = new double[npoints_cl];
				double vaux = sigma_max/8.0;
				double value = 0.0;
				double inc = Lcl/ ((double)(npoints_cl-1));
				for (i = 0; i < deflac_cicl_1.length; i++)
				{
					// System.out.println(" deflac_cicl_1[" + i + "]: " + value);
					deflac_cicl_1[i] = value*value*vaux;
					value += inc;
				}
			}
			
			// for (i = 0; i < deflac_cicl_1.length; i++)
			//	System.out.println(" deflac_cicl_1[" + i + "]: " + deflac_cicl_1[i]);
			
			double[] SFr1, CFr1;
			SFr1 = new double[deflac_cicl_1.length];
			CFr1 = new double[deflac_cicl_1.length];
			
			for(i = 0; i < deflac_cicl_1.length; i++)
			{
				ifresnel = fresnel( Math.sqrt((2.0*deflac_cicl_1[i])/Math.PI) );
				SFr1[i] = -ifresnel[0];
				CFr1[i] = ifresnel[1];
				
				// System.out.println(" SFr1[" + i + "]: " + SFr1[i]);
				// System.out.println(" CFr1[" + i + "]: " + CFr1[i]);
			}
			
			double[] SFr1_r, CFr1_r;
			SFr1_r = new double[SFr1.length];
			CFr1_r = new double[CFr1.length];
			
			for(i = 0; i < SFr1.length; i++)
			{
				SFr1_r[i] = (SFr1[i]*Math.cos(theta)) + (CFr1[i]*Math.sin(theta));
				CFr1_r[i] = (-SFr1[i]*Math.sin(theta)) + (CFr1[i]*Math.cos(theta));
				
				// System.out.println(" SFr1_r[" + i + "]: " + SFr1_r[i]);
				// System.out.println(" CFr1_r[" + i + "]: " + CFr1_r[i]);
			}
			
			piece_1 = new Position[SFr1_r.length];
			for(i = 0; i< piece_1.length; i++)
				piece_1[i] = new Position();
			
			double daux;
			
			if (arc_deg == 0)
			{
				daux = Math.sqrt(Math.PI*aux_b_lim)/k_max;
				
				for(i = 0; i < piece_1.length; i++)
				{
					piece_1[i].x(x+(CFr1_r[i]*daux));
					piece_1[i].y(y+(SFr1_r[i]*daux));
				}
			} else {
				d1 = Math.cos(-0.5*length)*CFr1[CFr1.length-1] + Math.sin(-0.5*length)*SFr1[SFr1.length-1];
				tmpsin = Math.sin(0.5*length + gamma);
				sigma0 = (d1*d1*Math.PI)/(rv*rv*tmpsin*tmpsin);
				
				daux = Math.sqrt(Math.PI/sigma0);
				
				for(i = 0; i < piece_1.length; i++)
				{
					piece_1[i].x(x + (CFr1_r[i]*daux));
					piece_1[i].y(y + (SFr1_r[i]*daux));
				}
			}
			
			double b_lim_2;
			b_lim_2 = 0.5*aux_b_lim;
			
			int npoints_ce;
			
			if (arc_deg == 0)
			{
				npoints_ce = (int) Math.round( Lce/spacement );
				
				if(npoints_ce != 0)
				{
					double value = theta - b_lim_2 + (0.5*Math.PI);
					double inc = Math.abs(aux_b_lim-length)/ ((double)(npoints_ce-1));
					
					piece_2 = new Position[npoints_ce];
					for(i = 0; i< piece_2.length; i++)
					{
						piece_2[i] = new Position();
						piece_2[i].x(x_omega + (Math.cos(value)/k_max));
						piece_2[i].y(y_omega + (Math.sin(value)/k_max));
						value -= inc;
					}
				} else {
					double value = theta + b_lim_2 -length + (0.5*Math.PI);
					piece_2 = new Position[1];
					piece_2[0] = new Position();
					piece_2[0].x(x_omega + (Math.cos(value)/k_max));
					piece_2[0].y(y_omega + (Math.sin(value)/k_max));
				}
			}
			
			// for(i = 0; i < piece_2.length; i++)
			//	System.out.println(" position2[" + i + "]: <" + piece_2[i].x() + ", " + piece_2[i].y() + ">");
			
			double[] deflac_cicl_2;
			deflac_cicl_2 = new double[deflac_cicl_1.length];
			
			for(i = 0; i < deflac_cicl_1.length; i++)
				deflac_cicl_2[i] = deflac_cicl_1[deflac_cicl_1.length - i - 1];
			
			// for(i = 0; i < deflac_cicl_2.length; i++)
			//	System.out.println(" deflac_cicl_2[" + i + "]: " + deflac_cicl_2[i]);
			
			double[] SFr2, CFr2;
			SFr2 = new double[deflac_cicl_2.length];
			CFr2 = new double[deflac_cicl_2.length];
			
			for(i = 0; i < deflac_cicl_2.length; i++)
			{
				ifresnel = fresnel( Math.sqrt((2.0*deflac_cicl_2[i])/Math.PI) );
				SFr2[i] = -ifresnel[0];
				CFr2[i] = ifresnel[1];
				
				// System.out.println(" SFr2[" + i + "]: " + SFr2[i]);
				// System.out.println(" CFr2[" + i + "]: " + CFr2[i]);
			}
			
			double[] SFr2_r, CFr2_r;
			SFr2_r = new double[SFr2.length];
			CFr2_r = new double[CFr2.length];
			
			for(i = 0; i < SFr2.length; i++)
			{
				SFr2_r[i] = (SFr2[i]*Math.cos(Math.PI-(theta-length))) + (CFr2[i]*Math.sin(Math.PI-(theta-length)));
				CFr2_r[i] = (-SFr2[i]*Math.sin(Math.PI-(theta-length))) + (CFr2[i]*Math.cos(Math.PI-(theta-length)));
				
				// System.out.println(" SFr1_r[" + i + "]: " + SFr1_r[i]);
				// System.out.println(" CFr1_r[" + i + "]: " + CFr1_r[i]);
			}
			
			piece_3 = new Position[SFr2_r.length];
			for(i = 0; i< piece_3.length; i++)
				piece_3[i] = new Position();
			
			if (arc_deg == 0)
			{
				daux = Math.sqrt(Math.PI*aux_b_lim)/k_max;
				
				for(i = 0; i < piece_3.length; i++)
				{
					piece_3[i].x(xe+(CFr2_r[i]*daux));
					piece_3[i].y(ye-(SFr2_r[i]*daux));
				}
			} else {
				daux = Math.sqrt(Math.PI/sigma0);
				
				for(i = 0; i < piece_3.length; i++)
				{
					piece_3[i].x(xe+(CFr2_r[i]*daux));
					piece_3[i].y(ye-(SFr2_r[i]*daux));
				}
			}
			
			// for(i = 0; i < piece_3.length; i++)
			//	System.out.println(" position3[" + i + "]: <" + piece_3[i].x() + ", " + piece_3[i].y() + ">");
			
			if (piece_2 != null)
			{
				if (Math.abs(piece_2[0].x()-piece_1[piece_1.length-1].x()) <= th_sp)
				{
					Position[] piece_1_tmp;
					piece_1_tmp = new Position[piece_1.length - 1];
					for(i = 0; i< piece_1_tmp.length; i++)
						piece_1_tmp[i] = new Position(piece_1[i]);
					
					piece_1 = new Position[piece_1_tmp.length];
					for(i = 0; i< piece_1.length; i++)
						piece_1[i] = new Position(piece_1_tmp[i]);
				}
				
				if (Math.abs(piece_3[0].x()-piece_2[piece_2.length-1].x()) <= th_sp)
				{
					Position[] piece_2_tmp;
					piece_2_tmp = new Position[piece_2.length - 1];
					for(i = 0; i< piece_2_tmp.length; i++)
						piece_2_tmp[i] = new Position(piece_2[i]);
					
					piece_2 = new Position[piece_2_tmp.length];
					for(i = 0; i< piece_2.length; i++)
						piece_2[i] = new Position(piece_2_tmp[i]);
				}
				
			} else {
				
				if (Math.abs(piece_3[0].x()-piece_1[piece_1.length-1].x()) <= th_sp)
				{
					Position[] piece_1_tmp;
					piece_1_tmp = new Position[piece_1.length - 1];
					for(i = 0; i< piece_1_tmp.length; i++)
						piece_1_tmp[i] = new Position(piece_1[i]);
					
					piece_1 = new Position[piece_1_tmp.length];
					for(i = 0; i< piece_1.length; i++)
						piece_1[i] = new Position(piece_1_tmp[i]);
				}
			}
		}
	}
	
	class PathRSR {
		
		final private double MAX_SLOPE	= 1000.0;
		
		double x_i, y_i, theta_i;
		double x_e, y_e, theta_e;
		double rv, gamma;
		double b_lim;
		
		double length;
		Position[] trajectory;
		
		public PathRSR (double x_i, double y_i, double theta_i, double x_e, double y_e, double theta_e, double rv, double gamma, double b_lim) {
			this.x_i		= x_i;
			this.y_i		= y_i;
			this.theta_i	= theta_i;
			this.x_e		= x_e;
			this.y_e		= y_e;
			this.theta_e	= theta_e;
			this.gamma		= gamma;
			this.rv			= rv;
			this.b_lim		= b_lim;
			
			length = 0;
			trajectory = null;
		}
		
		double getLength() { return length; }
		
		public Position[] getTrajectory() {	return trajectory; }
		
		public void calculatePath (double ang_df, double slope) {
			
			length = 0;
			trajectory = null;
			
			// Angle normalisation
			while(theta_i >= Math.toRadians(360))	theta_i -= Math.toRadians(360);
			while(theta_i < Math.toRadians(0))		theta_i += Math.toRadians(360);
			
			while(theta_e >= Math.toRadians(360))	theta_e -= Math.toRadians(360);
			while(theta_e < Math.toRadians(0))		theta_e += Math.toRadians(360);
			
			CalculateCircle circle_centre = new CalculateCircle();
			Position omega1, omega2;
			// Calculate circle RIGHT_ORIENTATION FIRST_ENDPOINT
			omega1 = circle_centre.calculateCenterCircle (x_i, y_i, theta_i, rv, gamma, RIGHT_ORIENTATION, FIRST_ENDPOINT);
			// Calculate circle RIGHT_ORIENTATION END_ENDPOINT
			omega2 = circle_centre.calculateCenterCircle (x_e, y_e, theta_e, rv, gamma, RIGHT_ORIENTATION, LAST_ENDPOINT);
			
			// System.out.println(" omega1: <" + omega1.x() + "," + omega1.y() + "> omega2: <" + omega2.x() + "," + omega2.y() + ">");
			
			double dx, dy, path_cond;
			dx = omega2.x()-omega1.x();
			dy = omega2.y()-omega1.y();
			path_cond = Math.sqrt((dx*dx)+(dy*dy));
			
			double m_centres = 0.0;
			
			if (path_cond > (2.0*rv*Math.sin(gamma))) {
				double ang_centres;
				if ( (omega1.x() == omega2.x()) && (omega1.y() < omega2.y()) )
				{
					ang_centres = 0.5*Math.PI;
				} else if ( (omega1.x() == omega2.x()) && (omega1.y() > omega2.y()) ) {
					ang_centres = 1.5*Math.PI;
				} else {
					
					m_centres = (omega2.y()-omega1.y()) / (omega2.x()-omega1.x());
					ang_centres = Math.atan(m_centres);
					if ( (omega2.y() >= omega1.y()) && (omega2.x() < omega1.x()) ) {
						ang_centres = Math.PI - Math.abs(ang_centres);
					} else if ( (omega2.y() < omega1.y()) && (omega2.x() < omega1.x()) ) {
						ang_centres = Math.PI + Math.abs(ang_centres);
					} else if ( (omega2.y() < omega1.y()) && (omega2.x() > omega1.x()) ) {
						ang_centres = 2.0*Math.PI + ang_centres;
					}
				}
				
				double alfa1;
				alfa1 = -gamma - (0.5*Math.PI);
				
				double ang1;
				ang1 = ang_centres + alfa1;
				
				double m1;
				m1 = Math.tan(ang1);
				
				try {
					double[] root_values = new double[2];
					root_values[0] = 0.0;
					root_values[1] = 0.0;
					
					double[] coef = new double[3];
					coef[2] = (m1*m1) + 1.0;
					coef[1] = -2.0*omega1.x() *coef[2];
					coef[0] = (omega1.x()*omega1.x())*coef[2]-(rv*rv);
					
					// Create polynomial p(x) = P1_pi*x^2 + P2_pi*x + P3_pi
					Polynomial poly = new Polynomial(coef);
					
					// Calculate the roots of the polynomial
					Complex[] zeros = poly.zeros();
					
					double[] xroots;
					double[] yroots;
					
					xroots = new double[zeros.length];
					yroots = new double[zeros.length];
					
					int i;
					for (i = 0; i < zeros.length; ++i)
					{
						xroots[i] = zeros[i].re();
						yroots[i] = (xroots[i]-omega1.x())*m1+omega1.y();
					}
					
					//for(i = 0; i < xroots.length; i++)
					//	System.out.println(" xroots[" + i + "]: " + xroots[i] + " yroots[" + i + "]: " + yroots[i]);
					
					double dist_1sa, dist_1sb;
					dist_1sa = Math.sqrt( ((omega2.x()-xroots[0])*(omega2.x()-xroots[0])) + ((omega2.y()-yroots[0])*(omega2.y()-yroots[0])) );
					dist_1sb = Math.sqrt( ((omega2.x()-xroots[1])*(omega2.x()-xroots[1])) + ((omega2.y()-yroots[1])*(omega2.y()-yroots[1])) );
					
					if(dist_1sa <= dist_1sb)
					{
						root_values[0] = xroots[0];
						root_values[1] = yroots[0];
					} else {
						root_values[0] = xroots[1];
						root_values[1] = yroots[1];
					}
					
					// System.out.println(" xroot: " + root_values[0] + " yroot: " + root_values[1]);
					
					double long1a, long2a, long3a;
					double lenght1a, lenght3a;
					Position[] piece1a, piece2a, piece3a, piece4a, piece5a, piece6a, piece7a;
					
					CalculateCurve ccurve;
					ccurve = new CalculateCurve(rv, gamma, b_lim);
					
					//System.out.println(x_i + "," + y_i + "," + theta_i + "," + x_omega1 + "," + y_omega1 + "," + roots5[0] + "," + roots5[1]);
					
					ccurve.toRight(x_i, y_i, theta_i, omega1.x(), omega1.y(), root_values[0], root_values[1]);
					ccurve.deleteLastPoint();
					
					lenght1a = ccurve.getLength();
					piece1a = ccurve.getPiece1();
					piece2a = ccurve.getPiece2();
					piece3a = ccurve.getPiece3();
					
					if (lenght1a > b_lim)
						long1a = (Math.abs(lenght1a)/k_max) + (k_max/Math.abs(sigma_max));
					else
						long1a = 2.0*Math.sqrt(lenght1a/ccurve.getSigma0());
					
					double ms;
					
					double[] root_values2 = new double[2];
					double[] xroots2 = new double[2];
					double[] yroots2 = new double[2];
					
					if( (ang_centres == Math.PI) || (ang_centres == (1.5*Math.PI)) )
					{
						root_values2[0] = root_values[0];
						yroots2[0] = omega2.y() + Math.sqrt((rv*rv)-((root_values[0]-omega2.x())*(root_values[0]-omega2.x())));
						yroots2[1] = omega2.y() - Math.sqrt((rv*rv)-((root_values[0]-omega2.x())*(root_values[0]-omega2.x())));
						
						double dist_ra, dist_rb;
						dist_ra = Math.abs(root_values[1]-yroots2[0]);
						dist_rb = Math.abs(root_values[1]-yroots2[1]);
						
						if(dist_ra <= dist_rb)
						{
							root_values2[1] = yroots2[0];
						} else {
							root_values2[1] = yroots2[1];
						}
						
						ms = MAX_SLOPE;
					} else {
						
						ms = m_centres;
						
						coef[2] = (ms*ms) + 1.0;
						coef[1] = 2.0*(ms*(root_values[1]-omega2.y()-(ms*root_values[0]))-omega2.x());
						coef[0] = (omega2.x()*omega2.x()) + (root_values[1]*root_values[1]) + (omega2.y()*omega2.y()) + (ms*ms*root_values[0]*root_values[0]) - 2.0*((root_values[1]*omega2.y())+(ms*root_values[0]*root_values[1])-(ms*omega2.y()*root_values[0])) - (rv*rv);
						
						//System.out.println("coef[2]: " + coef[2] + " coef[1]: " + coef[1] + " coef[0]: " + coef[0]);
						
						// Create polynomial p(x) = P1_pi*x^2 + P2_pi*x + P3_pi
						poly = new Polynomial(coef);
						
						// Calculate the roots of the polynomial
						zeros = poly.zeros();
						
						for (i = 0; i < zeros.length; ++i)
						{
							xroots2[i] = zeros[i].re();
							yroots2[i] = root_values[1] + ms*(xroots2[i]-root_values[0]);
						}
						
						double dist_34a, dist_34b;
						double dxa, dxb, dya, dyb;
						dxa = xroots2[0]-root_values[0];
						dxb = xroots2[1]-root_values[0];
						dya = yroots2[0]-root_values[1];
						dyb = yroots2[1]-root_values[1];
						
						dist_34a = Math.sqrt( (dxa*dxa)+(dya*dya) );
						dist_34b = Math.sqrt( (dxb*dxb)+(dyb*dyb));
						
						if(dist_34a <= dist_34b)
						{
							root_values2[0] = xroots2[0];
							root_values2[1] = yroots2[0];
						} else {
							root_values2[0] = xroots2[1];
							root_values2[1] = yroots2[1];
						}
					}
					
					//System.out.println(" X3: " + root_values[0] + " Y3: " + root_values[1]);
					//System.out.println(" X4: " + root_values2[0] + " Y4: " + root_values2[1]);
					
					CalculateSegment calc_segment = new CalculateSegment();
					calc_segment.calculateSegment(root_values[0], root_values[1], root_values2[0], root_values2[1], ms);
					calc_segment.deleteLastPoint();
					//lenght2a = calc_segment.getLength();
					piece4a = calc_segment.getSegment();
					long2a = long1a + Math.sqrt( ((root_values2[0]-root_values[0])*(root_values2[0]-root_values[0])) + ((root_values2[1]-root_values[1])*(root_values2[1]-root_values[1])) );
					
					ccurve.toRight(root_values2[0], root_values2[1], theta_i-lenght1a, omega2.x(), omega2.y(), x_e, y_e);
					
					lenght3a = ccurve.getLength();
					piece5a = ccurve.getPiece1();
					piece6a = ccurve.getPiece2();
					piece7a = ccurve.getPiece3();
					
					if (lenght3a > b_lim)
						long3a = long2a + (Math.abs(lenght3a)/k_max) + (k_max/Math.abs(sigma_max));
					else
						long3a = long2a + 2.0*Math.sqrt(lenght3a/ccurve.getSigma0());
					
					length = long3a;
					
					int size_traj;
					size_traj = 0;
					if(piece1a != null) size_traj += piece1a.length;
					if(piece2a != null) size_traj += piece2a.length;
					if(piece3a != null) size_traj += piece3a.length;
					if(piece4a != null) size_traj += piece4a.length;
					if(piece5a != null) size_traj += piece5a.length;
					if(piece6a != null) size_traj += piece6a.length;
					if(piece7a != null) size_traj += piece7a.length;
					
					trajectory = new Position[size_traj];
					for(i = 0; i < trajectory.length; i++)
						trajectory[i] = new Position();
					
					int k = 0;
					if(piece1a != null)	for(i = 0; i < piece1a.length; i++)	trajectory[k++].set(piece1a[i]);
					if(piece2a != null)	for(i = 0; i < piece2a.length; i++)	trajectory[k++].set(piece2a[i]);
					if(piece3a != null)	for(i = 0; i < piece3a.length; i++)	trajectory[k++].set(piece3a[i]);
					if(piece4a != null)	for(i = 0; i < piece4a.length; i++)	trajectory[k++].set(piece4a[i]);
					if(piece5a != null)	for(i = 0; i < piece5a.length; i++)	trajectory[k++].set(piece5a[i]);
					if(piece6a != null)	for(i = 0; i < piece6a.length; i++)	trajectory[k++].set(piece6a[i]);
					if(piece7a != null)	for(i = 0; i < piece7a.length; i++)	trajectory[k++].set(piece7a[i]);
					
				} catch (Exception e) {
					e.printStackTrace();
				}
				
			} else {
				length = 0;
				trajectory = null;
			}
		}
	}
	
	class PathLSL {
		
		final private double MAX_SLOPE	= 1000.0;
		
		double x_i, y_i, theta_i;
		double x_e, y_e, theta_e;
		double rv, gamma;
		double b_lim;
		
		double length;
		Position[] trajectory;
		
		public PathLSL (double x_i, double y_i, double theta_i, double x_e, double y_e, double theta_e, double rv, double gamma, double b_lim) {
			this.x_i		= x_i;
			this.y_i		= y_i;
			this.theta_i	= theta_i;
			this.x_e		= x_e;
			this.y_e		= y_e;
			this.theta_e	= theta_e;
			this.gamma		= gamma;
			this.rv			= rv;
			this.b_lim		= b_lim;
			
			length = 0;
			trajectory = null;
		}
		
		double getLength() { return length; }
		
		public Position[] getTrajectory() {	return trajectory; }
		
		public void calculatePath (double ang_df, double slope) {
			
			length = 0;
			trajectory = null;
			
			// Angle normalisation
			while(theta_i >= Math.toRadians(360))	theta_i -= Math.toRadians(360);
			while(theta_i < Math.toRadians(0))		theta_i += Math.toRadians(360);
			
			while(theta_e >= Math.toRadians(360))	theta_e -= Math.toRadians(360);
			while(theta_e < Math.toRadians(0))		theta_e += Math.toRadians(360);
			
			CalculateCircle circle_centre = new CalculateCircle();
			Position omega1, omega2;
			// Calculate circle LEFT_ORIENTATION FIRST_ENDPOINT
			omega1 = circle_centre.calculateCenterCircle (x_i, y_i, theta_i, rv, gamma, LEFT_ORIENTATION, FIRST_ENDPOINT);
			// Calculate circle LEFT_ORIENTATION END_ENDPOINT
			omega2 = circle_centre.calculateCenterCircle (x_e, y_e, theta_e, rv, gamma, LEFT_ORIENTATION, LAST_ENDPOINT);
			
			// System.out.println(" omega1: <" + omega1.x() + "," + omega1.y() + "> omega2: <" + omega2.x() + "," + omega2.y() + ">");
			
			// Condition to calculate the path
			double dx, dy, path_cond;
			dx = omega2.x()-omega1.x();
			dy = omega2.y()-omega1.y();
			path_cond = Math.sqrt((dx*dx)+(dy*dy));
			
			double m_centres = 0.0;
			
			if (path_cond > (2.0*rv)) {
				double ang_centres;
				if ( (omega1.x() == omega2.x()) && (omega1.y() < omega2.y()) )
				{
					ang_centres = 0.5*Math.PI;
				} else if ( (omega1.x() == omega2.x()) && (omega1.y() > omega2.y()) ) {
					ang_centres = 1.5*Math.PI;
				} else {
					m_centres = (omega2.y()-omega1.y()) / (omega2.x()-omega1.x());
					ang_centres = Math.atan(m_centres);
					if ( (omega2.y() >= omega1.y()) && (omega2.x() < omega1.x()) ) {
						ang_centres = Math.PI - Math.abs(ang_centres);
					} else if ( (omega2.y() < omega1.y()) && (omega2.x() < omega1.x()) ) {
						ang_centres = Math.PI + Math.abs(ang_centres);
					} else if ( (omega2.y() < omega1.y()) && (omega2.x() > omega1.x()) ) {
						ang_centres = 2.0*Math.PI + ang_centres;
					}
				}
				
				double alfa1;
				alfa1 = gamma-(0.5*Math.PI);
				
				double ang1;
				ang1 = ang_centres + alfa1;
				
				double m1;
				m1 = Math.tan(ang1);
				
				try {
					
					double[] root_values = new double[2];
					root_values[0] = 0.0;
					root_values[1] = 0.0;
					
					double[] coef = new double[3];
					coef[2] = (m1*m1) + 1.0;
					coef[1] = -2.0*omega1.x() *coef[2];
					coef[0] = (omega1.x()*omega1.x())*coef[2]-(rv*rv);
					
					// Create polynomial p(x) = P1_pi*x^2 + P2_pi*x + P3_pi
					Polynomial poly = new Polynomial(coef);
					
					// Calculate the roots of the polynomial
					Complex[] zeros = poly.zeros();
					
					double[] xroots;
					double[] yroots;
					
					xroots = new double[zeros.length];
					yroots = new double[zeros.length];
					
					int i;
					for (i = 0; i < zeros.length; ++i)
					{
						xroots[i] = zeros[i].re();
						yroots[i] = (xroots[i]-omega1.x())*m1+omega1.y();
					}
					
					//for(i = 0; i < xroots.length; i++)
					//	System.out.println(" xroots[" + i + "]: " + xroots[i] + " yroots[" + i + "]: " + yroots[i]);
					
					double dist_1sa, dist_1sb;
					dist_1sa = Math.sqrt( ((omega2.x()-xroots[0])*(omega2.x()-xroots[0])) + ((omega2.y()-yroots[0])*(omega2.y()-yroots[0])) );
					dist_1sb = Math.sqrt( ((omega2.x()-xroots[1])*(omega2.x()-xroots[1])) + ((omega2.y()-yroots[1])*(omega2.y()-yroots[1])) );
					
					if(dist_1sa <= dist_1sb)
					{
						root_values[0] = xroots[0];
						root_values[1] = yroots[0];
					} else {
						root_values[0] = xroots[1];
						root_values[1] = yroots[1];
					}
					
					// System.out.println(" xroot: " + root_values[0] + " yroot: " + root_values[1]);
					
					double long1a, long2a, long3a;
					double lenght1a, lenght3a;
					Position[] piece1a, piece2a, piece3a, piece4a, piece5a, piece6a, piece7a;
					
					CalculateCurve ccurve;
					ccurve = new CalculateCurve(rv, gamma, b_lim);
					
					//System.out.println(x_i + "," + y_i + "," + theta_i + "," + x_omega1 + "," + y_omega1 + "," + roots5[0] + "," + roots5[1]);
					
					ccurve.toLeft (x_i, y_i, theta_i, omega1.x(), omega1.y(), root_values[0], root_values[1]);
					ccurve.deleteLastPoint();
					
					lenght1a = ccurve.getLength();
					piece1a = ccurve.getPiece1();
					piece2a = ccurve.getPiece2();
					piece3a = ccurve.getPiece3();
					
					if (lenght1a > b_lim)
						long1a = (Math.abs(lenght1a)/k_max) + (k_max/Math.abs(sigma_max));
					else
						long1a = 2.0*Math.sqrt(lenght1a/ccurve.getSigma0());
					
					double ms;
					
					double[] root_values2 = new double[2];
					double[] xroots2 = new double[2];
					double[] yroots2 = new double[2];
					
					if( (ang_centres == Math.PI) || (ang_centres == (1.5*Math.PI)) )
					{
						root_values2[0] = root_values[0];
						yroots2[0] = omega2.y() + Math.sqrt((rv*rv)-((root_values[0]-omega2.x())*(root_values[0]-omega2.x())));
						yroots2[1] = omega2.y() - Math.sqrt((rv*rv)-((root_values[0]-omega2.x())*(root_values[0]-omega2.x())));
						
						double dist_ra, dist_rb;
						dist_ra = Math.abs(root_values[1]-yroots2[0]);
						dist_rb = Math.abs(root_values[1]-yroots2[1]);
						
						if(dist_ra <= dist_rb)
						{
							root_values2[1] = yroots2[0];
						} else {
							root_values2[1] = yroots2[1];
						}
						
						ms = MAX_SLOPE;
					} else {
						
						ms = m_centres;
						
						coef[2] = (ms*ms) + 1.0;
						coef[1] = 2.0*(ms*(root_values[1]-omega2.y()-(ms*root_values[0]))-omega2.x());
						coef[0] = (omega2.x()*omega2.x()) + (root_values[1]*root_values[1]) + (omega2.y()*omega2.y()) + (ms*ms*root_values[0]*root_values[0]) - 2.0*((root_values[1]*omega2.y())+(ms*root_values[0]*root_values[1])-(ms*omega2.y()*root_values[0])) - (rv*rv);
						
						//System.out.println("coef[2]: " + coef[2] + " coef[1]: " + coef[1] + " coef[0]: " + coef[0]);
						
						// Create polynomial p(x) = P1_pi*x^2 + P2_pi*x + P3_pi
						poly = new Polynomial(coef);
						
						// Calculate the roots of the polynomial
						zeros = poly.zeros();
						
						for (i = 0; i < zeros.length; ++i)
						{
							xroots2[i] = zeros[i].re();
							yroots2[i] = root_values[1] + ms*(xroots2[i]-root_values[0]);
						}
						
						double dist_34a, dist_34b;
						double dxa, dxb, dya, dyb;
						dxa = xroots2[0]-root_values[0];
						dxb = xroots2[1]-root_values[0];
						dya = yroots2[0]-root_values[1];
						dyb = yroots2[1]-root_values[1];
						
						dist_34a = Math.sqrt( (dxa*dxa)+(dya*dya) );
						dist_34b = Math.sqrt( (dxb*dxb)+(dyb*dyb));
						
						if(dist_34a <= dist_34b)
						{
							root_values2[0] = xroots2[0];
							root_values2[1] = yroots2[0];
						} else {
							root_values2[0] = xroots2[1];
							root_values2[1] = yroots2[1];
						}
					}
					
					CalculateSegment calc_segment = new CalculateSegment();
					calc_segment.calculateSegment(root_values[0], root_values[1], root_values2[0], root_values2[1], ms);
					calc_segment.deleteLastPoint();
					//lenght2a = calc_segment.getLength();
					piece4a = calc_segment.getSegment();
					long2a = long1a + Math.sqrt( ((root_values2[0]-root_values[0])*(root_values2[0]-root_values[0])) + ((root_values2[1]-root_values[1])*(root_values2[1]-root_values[1])) );
					
					ccurve.toLeft(root_values2[0], root_values2[1], lenght1a+theta_i, omega2.x(), omega2.y(), x_e, y_e);
					
					lenght3a = ccurve.getLength();
					piece5a = ccurve.getPiece1();
					piece6a = ccurve.getPiece2();
					piece7a = ccurve.getPiece3();
					
					if (lenght3a > b_lim)
						long3a = long2a + (Math.abs(lenght3a)/k_max) + (k_max/Math.abs(sigma_max));
					else
						long3a = long2a + 2.0*Math.sqrt(lenght3a/ccurve.getSigma0());
					
					length = long3a;
					
					int size_traj;
					size_traj = 0;
					if(piece1a != null) size_traj += piece1a.length;
					if(piece2a != null) size_traj += piece2a.length;
					if(piece3a != null) size_traj += piece3a.length;
					if(piece4a != null) size_traj += piece4a.length;
					if(piece5a != null) size_traj += piece5a.length;
					if(piece6a != null) size_traj += piece6a.length;
					if(piece7a != null) size_traj += piece7a.length;
					
					trajectory = new Position[size_traj];
					for(i = 0; i < trajectory.length; i++)
						trajectory[i] = new Position();
					
					int k = 0;
					if(piece1a != null)	for(i = 0; i < piece1a.length; i++)	trajectory[k++].set(piece1a[i]);
					if(piece2a != null)	for(i = 0; i < piece2a.length; i++)	trajectory[k++].set(piece2a[i]);
					if(piece3a != null)	for(i = 0; i < piece3a.length; i++)	trajectory[k++].set(piece3a[i]);
					if(piece4a != null)	for(i = 0; i < piece4a.length; i++)	trajectory[k++].set(piece4a[i]);
					if(piece5a != null)	for(i = 0; i < piece5a.length; i++)	trajectory[k++].set(piece5a[i]);
					if(piece6a != null)	for(i = 0; i < piece6a.length; i++)	trajectory[k++].set(piece6a[i]);
					if(piece7a != null)	for(i = 0; i < piece7a.length; i++)	trajectory[k++].set(piece7a[i]);
					
					//System.out.println(" Lenght: " + lenght3a);
					//if (piece5a != null)
					//	for(i = 0; i < piece5a.length; i++)
					//		System.out.println(" piece5a[" + i + "]: <" + piece5a[i].x() + "," + piece5a[i].y() + ">");
					//System.out.println("");
					
				} catch (Exception e) {
					e.printStackTrace();
				}
				
			} else {
				length = 0;
				trajectory = null;
			}
		}
	}
	
	class PathLSR {
		double x_i, y_i, theta_i;
		double x_e, y_e, theta_e;
		double rv, gamma;
		double b_lim;
		
		double length;
		Position[] trajectory;
		
		public PathLSR (double x_i, double y_i, double theta_i, double x_e, double y_e, double theta_e, double rv, double gamma, double b_lim) {
			this.x_i		= x_i;
			this.y_i		= y_i;
			this.theta_i	= theta_i;
			this.x_e		= x_e;
			this.y_e		= y_e;
			this.theta_e	= theta_e;
			this.gamma		= gamma;
			this.rv			= rv;
			this.b_lim		= b_lim;
			
			length = 0;
			trajectory = null;
		}
		
		double getLength() { return length; }
		
		public Position[] getTrajectory() {	return trajectory; }
		
		public void calculatePath (double ang_df, double slope) {
			length = 0;
			trajectory = null;
			
			// Angle normalisation
			while(theta_i >= Math.toRadians(360))	theta_i -= Math.toRadians(360);
			while(theta_i < Math.toRadians(0))		theta_i += Math.toRadians(360);
			
			while(theta_e >= Math.toRadians(360))	theta_e -= Math.toRadians(360);
			while(theta_e < Math.toRadians(0))		theta_e += Math.toRadians(360);
			
			CalculateCircle circle_centre = new CalculateCircle();
			Position omega1, omega2;
			// Calculate circle LEFT_ORIENTATION FIRST_ENDPOINT
			omega1 = circle_centre.calculateCenterCircle (x_i, y_i, theta_i, rv, gamma, LEFT_ORIENTATION, FIRST_ENDPOINT);
			// Calculate circle RIGHT_ORIENTATION END_ENDPOINT
			omega2 = circle_centre.calculateCenterCircle (x_e, y_e, theta_e, rv, gamma, RIGHT_ORIENTATION, LAST_ENDPOINT);
			
			// System.out.println(" omega1: <" + omega1.x() + "," + omega1.y() + "> omega2: <" + omega2.x() + "," + omega2.y() + ">");
			
			// Condition to calculate the path
			double dx, dy, path_cond;
			dx = omega2.x()-omega1.x();
			dy = omega2.y()-omega1.y();
			path_cond = Math.sqrt((dx*dx)+(dy*dy));
			
			if (path_cond > (2.0*rv)) {
				double ang_centres;
				if ( (omega1.x() == omega2.x()) && (omega1.y() < omega2.y()) )
				{
					ang_centres = 0.5*Math.PI;
				} else if ( (omega1.x() == omega2.x()) && (omega1.y() > omega2.y()) ) {
					ang_centres = 1.5*Math.PI;
				} else {
					double m_centres;
					m_centres = (omega2.y()-omega1.y()) / (omega2.x()-omega1.x());
					ang_centres = Math.atan(m_centres);
					if ( (omega2.y() >= omega1.y()) && (omega2.x() < omega1.x()) ) {
						ang_centres = Math.PI - Math.abs(ang_centres);
					} else if ( (omega2.y() < omega1.y()) && (omega2.x() < omega1.x()) ) {
						ang_centres = Math.PI + Math.abs(ang_centres);
					} else if ( (omega2.y() < omega1.y()) && (omega2.x() > omega1.x()) ) {
						ang_centres = 2.0*Math.PI + ang_centres;
					}
				}
				
				double alfa2, alfa1;
				alfa2 = Math.asin((2.0*rv*Math.cos(gamma))/path_cond);
				alfa1 = alfa2 + gamma - (0.5*Math.PI);
				
				double ang1;
				ang1 = ang_centres + alfa1;
				
				double m1;
				m1 = Math.tan(ang1);
				
				try {
					double[] root_values = new double[2];
					root_values[0] = 0.0;
					root_values[1] = 0.0;
					
					double[] coef = new double[3];
					coef[2] = (m1*m1) + 1.0;
					coef[1] = -2.0*omega1.x() *coef[2];
					coef[0] = (omega1.x()*omega1.x())*coef[2]-(rv*rv);
					
					// Create polynomial p(x) = P1_pi*x^2 + P2_pi*x + P3_pi
					Polynomial poly = new Polynomial(coef);
					
					// Calculate the roots of the polynomial
					Complex[] zeros = poly.zeros();
					
					double[] xroots;
					double[] yroots;
					
					xroots = new double[zeros.length];
					yroots = new double[zeros.length];
					
					int i;
					for (i = 0; i < zeros.length; ++i)
					{
						xroots[i] = zeros[i].re();
						yroots[i] = (xroots[i]-omega1.x())*m1+omega1.y();
					}
					
					//for(i = 0; i < xroots.length; i++)
					//	System.out.println(" xroots[" + i + "]: " + xroots[i] + " yroots[" + i + "]: " + yroots[i]);
					
					double dist_1sa, dist_1sb;
					dist_1sa = Math.sqrt( ((omega2.x()-xroots[0])*(omega2.x()-xroots[0])) + ((omega2.y()-yroots[0])*(omega2.y()-yroots[0])) );
					dist_1sb = Math.sqrt( ((omega2.x()-xroots[1])*(omega2.x()-xroots[1])) + ((omega2.y()-yroots[1])*(omega2.y()-yroots[1])) );
					
					if(dist_1sa <= dist_1sb)
					{
						root_values[0] = xroots[0];
						root_values[1] = yroots[0];
					} else {
						root_values[0] = xroots[1];
						root_values[1] = yroots[1];
					}
					
					// System.out.println(" xroot: " + root_values[0] + " yroot: " + root_values[1]);
					
					double long1a, long2a, long3a;
					double lenght1a, lenght3a;
					Position[] piece1a, piece2a, piece3a, piece4a, piece5a, piece6a, piece7a;
					
					CalculateCurve ccurve;
					ccurve = new CalculateCurve(rv, gamma, b_lim);
					
					//System.out.println(x_i + "," + y_i + "," + theta_i + "," + x_omega1 + "," + y_omega1 + "," + roots5[0] + "," + roots5[1]);
					
					ccurve.toLeft (x_i, y_i, theta_i, omega1.x(), omega1.y(), root_values[0], root_values[1]);
					ccurve.deleteLastPoint();
					
					lenght1a = ccurve.getLength();
					piece1a = ccurve.getPiece1();
					piece2a = ccurve.getPiece2();
					piece3a = ccurve.getPiece3();
					
					if (lenght1a > b_lim)
						long1a = (Math.abs(lenght1a)/k_max) + (k_max/Math.abs(sigma_max));
					else
						long1a = 2.0*Math.sqrt(lenght1a/ccurve.getSigma0());
					
					double[] root_values2 = new double[2];
					root_values2[0] = 0.0;
					root_values2[1] = 0.0;
					
					double ms;
					ms = Math.tan(theta_i+lenght1a);
					
					coef[2] = (ms*ms) + 1.0;
					coef[1] = 2.0*(ms*(root_values[1]-omega2.y()-(ms*root_values[0]))-omega2.x());
					coef[0] = (omega2.x()*omega2.x()) + (root_values[1]*root_values[1]) + (omega2.y()*omega2.y()) + (ms*ms*root_values[0]*root_values[0]) - 2.0*((root_values[1]*omega2.y())+(ms*root_values[0]*root_values[1])-(ms*omega2.y()*root_values[0])) - (rv*rv);
					
					//System.out.println("coef[2]: " + coef[2] + " coef[1]: " + coef[1] + " coef[0]: " + coef[0]);
					
					// Create polynomial p(x) = P1_pi*x^2 + P2_pi*x + P3_pi
					poly = new Polynomial(coef);
					
					// Calculate the roots of the polynomial
					zeros = poly.zeros();
					
					double[] xroots2 = new double[zeros.length];
					double[] yroots2 = new double[zeros.length];
					
					for (i = 0; i < zeros.length; ++i)
					{
						xroots2[i] = zeros[i].re();
						yroots2[i] = root_values[1] + ms*(xroots2[i]-root_values[0]);
					}
					
					//for(i = 0; i < zeros.length; ++i)
					//{
					//	System.out.println(" xroots2[" + i + "]: " + xroots2[i]);
					//	System.out.println(" yroots2[" + i + "]: " + yroots2[i]);
					//}
					
					double dist_34a, dist_34b;
					double dxa, dxb, dya, dyb;
					dxa = xroots2[0]-root_values[0];
					dxb = xroots2[1]-root_values[0];
					dya = yroots2[0]-root_values[1];
					dyb = yroots2[1]-root_values[1];
					
					dist_34a = Math.sqrt( (dxa*dxa)+(dya*dya) );
					dist_34b = Math.sqrt( (dxb*dxb)+(dyb*dyb));
					
					if(dist_34a <= dist_34b)
					{
						root_values2[0] = xroots2[0];
						root_values2[1] = yroots2[0];
					} else {
						root_values2[0] = xroots2[1];
						root_values2[1] = yroots2[1];
					}
					
					//System.out.println(" X3: " + root_values[0] + " Y3: " + root_values[1]);
					//System.out.println(" X4: " + root_values2[0] + " Y4: " + root_values2[1]);
					
					CalculateSegment calc_segment = new CalculateSegment();
					calc_segment.calculateSegment(root_values[0], root_values[1], root_values2[0], root_values2[1], ms);
					calc_segment.deleteLastPoint();
					//lenght2a = calc_segment.getLength();
					piece4a = calc_segment.getSegment();
					long2a = long1a + Math.sqrt( ((root_values2[0]-root_values[0])*(root_values2[0]-root_values[0])) + ((root_values2[1]-root_values[1])*(root_values2[1]-root_values[1])) );
					
					ccurve.toRight(root_values2[0], root_values2[1], theta_i+lenght1a, omega2.x(), omega2.y(), x_e, y_e);
					
					lenght3a = ccurve.getLength();
					piece5a = ccurve.getPiece1();
					piece6a = ccurve.getPiece2();
					piece7a = ccurve.getPiece3();
					
					/*
					System.out.println(" Lenght: " + lenght3a);
					if (piece7a != null)
						for(i = 0; i < piece7a.length; i++)
							System.out.println(" piece7a[" + i + "]: <" + piece7a[i].x() + "," + piece7a[i].y() + ">");
					System.out.println("");
					*/
					
					if (lenght3a > b_lim)
						long3a = long2a + (Math.abs(lenght3a)/k_max) + (k_max/Math.abs(sigma_max));
					else
						long3a = long2a + 2.0*Math.sqrt(lenght3a/ccurve.getSigma0());
					
					length = long3a;
					
					int size_traj;
					size_traj = 0;
					if(piece1a != null) size_traj += piece1a.length;
					if(piece2a != null) size_traj += piece2a.length;
					if(piece3a != null) size_traj += piece3a.length;
					if(piece4a != null) size_traj += piece4a.length;
					if(piece5a != null) size_traj += piece5a.length;
					if(piece6a != null) size_traj += piece6a.length;
					if(piece7a != null) size_traj += piece7a.length;
					
					trajectory = new Position[size_traj];
					for(i = 0; i < trajectory.length; i++)
						trajectory[i] = new Position();
					
					int k = 0;
					if(piece1a != null)	for(i = 0; i < piece1a.length; i++)	trajectory[k++].set(piece1a[i]);
					if(piece2a != null)	for(i = 0; i < piece2a.length; i++)	trajectory[k++].set(piece2a[i]);
					if(piece3a != null)	for(i = 0; i < piece3a.length; i++)	trajectory[k++].set(piece3a[i]);
					if(piece4a != null)	for(i = 0; i < piece4a.length; i++)	trajectory[k++].set(piece4a[i]);
					if(piece5a != null)	for(i = 0; i < piece5a.length; i++)	trajectory[k++].set(piece5a[i]);
					if(piece6a != null)	for(i = 0; i < piece6a.length; i++)	trajectory[k++].set(piece6a[i]);
					if(piece7a != null)	for(i = 0; i < piece7a.length; i++)	trajectory[k++].set(piece7a[i]);
					
					//System.out.println(" Lenght: " + lenght3a);
					//if (piece5a != null)
					//	for(i = 0; i < piece5a.length; i++)
					//		System.out.println(" piece5a[" + i + "]: <" + piece5a[i].x() + "," + piece5a[i].y() + ">");
					//System.out.println("");
					
				} catch (Exception e) {
					e.printStackTrace();
				}
				
			} else {
				length = 0;
				trajectory = null;
			}
		}
	}
	
	class PathRSL {
		
		double x_i, y_i, theta_i;
		double x_e, y_e, theta_e;
		double rv, gamma;
		double b_lim;
		
		double length;
		Position[] trajectory;
		
		public PathRSL (double x_i, double y_i, double theta_i, double x_e, double y_e, double theta_e, double rv, double gamma, double b_lim) {
			this.x_i		= x_i;
			this.y_i		= y_i;
			this.theta_i	= theta_i;
			this.x_e		= x_e;
			this.y_e		= y_e;
			this.theta_e	= theta_e;
			this.gamma		= gamma;
			this.rv			= rv;
			this.b_lim		= b_lim;
			
			length = 0;
			trajectory = null;
		}
		
		double getLength() { return length; }
		
		public Position[] getTrajectory() {	return trajectory; }
		
		public void calculatePath (double ang_df, double slope) {
			length = 0;
			trajectory = null;
			
			// Angle normalisation
			while(theta_i >= Math.toRadians(360))	theta_i -= Math.toRadians(360);
			while(theta_i < Math.toRadians(0))		theta_i += Math.toRadians(360);
			
			while(theta_e >= Math.toRadians(360))	theta_e -= Math.toRadians(360);
			while(theta_e < Math.toRadians(0))		theta_e += Math.toRadians(360);
			
			CalculateCircle circle_centre = new CalculateCircle();
			Position omega1, omega2;
			// Calculate circle RIGHT_ORIENTATION FIRST_ENDPOINT
			omega1 = circle_centre.calculateCenterCircle (x_i, y_i, theta_i, rv, gamma, RIGHT_ORIENTATION, FIRST_ENDPOINT);
			// Calculate circle LEFT_ORIENTATION END_ENDPOINT
			omega2 = circle_centre.calculateCenterCircle (x_e, y_e, theta_e, rv, gamma, LEFT_ORIENTATION, LAST_ENDPOINT);
			
			// System.out.println(" omega1: <" + omega1.x() + "," + omega1.y() + "> omega2: <" + omega2.x() + "," + omega2.y() + ">");
			
			// Condition to calculate the path
			double dx, dy, path_cond;
			dx = omega2.x()-omega1.x();
			dy = omega2.y()-omega1.y();
			path_cond = Math.sqrt((dx*dx)+(dy*dy));
			
			if (path_cond > (2.0*rv)) {
				double ang_centres;
				if ( (omega1.x() == omega2.x()) && (omega1.y() < omega2.y()) )
				{
					ang_centres = 0.5*Math.PI;
				} else if ( (omega1.x() == omega2.x()) && (omega1.y() > omega2.y()) ) {
					ang_centres = 1.5*Math.PI;
				} else {
					double m_centres;
					m_centres = (omega2.y()-omega1.y()) / (omega2.x()-omega1.x());
					ang_centres = Math.atan(m_centres);
					if ( (omega2.y() >= omega1.y()) && (omega2.x() < omega1.x()) ) {
						ang_centres = Math.PI - Math.abs(ang_centres);
					} else if ( (omega2.y() < omega1.y()) && (omega2.x() < omega1.x()) ) {
						ang_centres = Math.PI + Math.abs(ang_centres);
					} else if ( (omega2.y() < omega1.y()) && (omega2.x() > omega1.x()) ) {
						ang_centres = 2.0*Math.PI + ang_centres;
					}
				}
				
				double alfa2, alfa1;
				alfa2 = -Math.asin((2.0*rv*Math.cos(gamma))/path_cond);
				alfa1 = -(-alfa2 + gamma - (0.5*Math.PI));
				
				double ang1;
				ang1 = ang_centres + alfa1;
				
				double m1;
				m1 = Math.tan(ang1);
				
				try {
					
					double[] root_values = new double[2];
					root_values[0] = 0.0;
					root_values[1] = 0.0;
					
					double[] coef = new double[3];
					coef[2] = (m1*m1) + 1.0;
					coef[1] = -2.0*omega1.x() *coef[2];
					coef[0] = (omega1.x()*omega1.x())*coef[2]-(rv*rv);
					
					// Create polynomial p(x) = P1_pi*x^2 + P2_pi*x + P3_pi
					Polynomial poly = new Polynomial(coef);
					
					// Calculate the roots of the polynomial
					Complex[] zeros = poly.zeros();
					
					double[] xroots;
					double[] yroots;
					
					xroots = new double[zeros.length];
					yroots = new double[zeros.length];
					
					int i;
					for (i = 0; i < zeros.length; ++i)
					{
						xroots[i] = zeros[i].re();
						yroots[i] = (xroots[i]-omega1.x())*m1+omega1.y();
					}
					
					//for(i = 0; i < xroots.length; i++)
					//	System.out.println(" xroots[" + i + "]: " + xroots[i] + " yroots[" + i + "]: " + yroots[i]);
					
					double dist_1sa, dist_1sb;
					dist_1sa = Math.sqrt( ((omega2.x()-xroots[0])*(omega2.x()-xroots[0])) + ((omega2.y()-yroots[0])*(omega2.y()-yroots[0])) );
					dist_1sb = Math.sqrt( ((omega2.x()-xroots[1])*(omega2.x()-xroots[1])) + ((omega2.y()-yroots[1])*(omega2.y()-yroots[1])) );
					
					if(dist_1sa <= dist_1sb)
					{
						root_values[0] = xroots[0];
						root_values[1] = yroots[0];
					} else {
						root_values[0] = xroots[1];
						root_values[1] = yroots[1];
					}
					
					// System.out.println(" xroot: " + root_values[0] + " yroot: " + root_values[1]);
					
					double long1a, long2a, long3a;
					double lenght1a, lenght3a;
					Position[] piece1a, piece2a, piece3a, piece4a, piece5a, piece6a, piece7a;
					
					CalculateCurve ccurve;
					ccurve = new CalculateCurve(rv, gamma, b_lim);
					
					//System.out.println(x_i + "," + y_i + "," + theta_i + "," + x_omega1 + "," + y_omega1 + "," + roots5[0] + "," + roots5[1]);
					
					ccurve.toRight(x_i, y_i, theta_i, omega1.x(), omega1.y(), root_values[0], root_values[1]);
					ccurve.deleteLastPoint();
					
					lenght1a = ccurve.getLength();
					piece1a = ccurve.getPiece1();
					piece2a = ccurve.getPiece2();
					piece3a = ccurve.getPiece3();
					
					if (lenght1a > b_lim)
						long1a = (Math.abs(lenght1a)/k_max) + (k_max/Math.abs(sigma_max));
					else
						long1a = 2.0*Math.sqrt(lenght1a/ccurve.getSigma0());
					
					double[] root_values2 = new double[2];
					root_values2[0] = 0.0;
					root_values2[1] = 0.0;
					
					double ms;
					ms = Math.tan(theta_i-lenght1a);
					
					coef[2] = (ms*ms) + 1.0;
					coef[1] = 2.0*(ms*(root_values[1]-omega2.y()-(ms*root_values[0]))-omega2.x());
					coef[0] = (omega2.x()*omega2.x()) + (root_values[1]*root_values[1]) + (omega2.y()*omega2.y()) + (ms*ms*root_values[0]*root_values[0]) - 2.0*((root_values[1]*omega2.y())+(ms*root_values[0]*root_values[1])-(ms*omega2.y()*root_values[0])) - (rv*rv);
					
					//System.out.println("coef[2]: " + coef[2] + " coef[1]: " + coef[1] + " coef[0]: " + coef[0]);
					
					// Create polynomial p(x) = P1_pi*x^2 + P2_pi*x + P3_pi
					poly = new Polynomial(coef);
					
					// Calculate the roots of the polynomial
					zeros = poly.zeros();
					
					double[] xroots2 = new double[zeros.length];
					double[] yroots2 = new double[zeros.length];
					
					for (i = 0; i < zeros.length; ++i)
					{
						xroots2[i] = zeros[i].re();
						yroots2[i] = root_values[1] + ms*(xroots2[i]-root_values[0]);
					}
					
					//for(i = 0; i < zeros.length; ++i)
					//{
					//	System.out.println(" xroots2[" + i + "]: " + xroots2[i]);
					//	System.out.println(" yroots2[" + i + "]: " + yroots2[i]);
					//}
					
					double dist_34a, dist_34b;
					double dxa, dxb, dya, dyb;
					dxa = xroots2[0]-root_values[0];
					dxb = xroots2[1]-root_values[0];
					dya = yroots2[0]-root_values[1];
					dyb = yroots2[1]-root_values[1];
					
					dist_34a = Math.sqrt( (dxa*dxa)+(dya*dya) );
					dist_34b = Math.sqrt( (dxb*dxb)+(dyb*dyb));
					
					if(dist_34a <= dist_34b)
					{
						root_values2[0] = xroots2[0];
						root_values2[1] = yroots2[0];
					} else {
						root_values2[0] = xroots2[1];
						root_values2[1] = yroots2[1];
					}
					
					//System.out.println(" X3: " + root_values[0] + " Y3: " + root_values[1]);
					//System.out.println(" X4: " + root_values2[0] + " Y4: " + root_values2[1]);
					
					CalculateSegment calc_segment = new CalculateSegment();
					calc_segment.calculateSegment(root_values[0], root_values[1], root_values2[0], root_values2[1], ms);
					calc_segment.deleteLastPoint();
					//lenght2a = calc_segment.getLength();
					piece4a = calc_segment.getSegment();
					long2a = long1a + Math.sqrt( ((root_values2[0]-root_values[0])*(root_values2[0]-root_values[0])) + ((root_values2[1]-root_values[1])*(root_values2[1]-root_values[1])) );
					
					ccurve.toLeft(root_values2[0], root_values2[1], theta_i-lenght1a, omega2.x(), omega2.y(), x_e, y_e);
					
					lenght3a = ccurve.getLength();
					piece5a = ccurve.getPiece1();
					piece6a = ccurve.getPiece2();
					piece7a = ccurve.getPiece3();
					
					if (lenght3a > b_lim)
						long3a = long2a + (Math.abs(lenght3a)/k_max) + (k_max/Math.abs(sigma_max));
					else
						long3a = long2a + 2.0*Math.sqrt(lenght3a/ccurve.getSigma0());
					
					length = long3a;
					
					int size_traj;
					size_traj = 0;
					if(piece1a != null) size_traj += piece1a.length;
					if(piece2a != null) size_traj += piece2a.length;
					if(piece3a != null) size_traj += piece3a.length;
					if(piece4a != null) size_traj += piece4a.length;
					if(piece5a != null) size_traj += piece5a.length;
					if(piece6a != null) size_traj += piece6a.length;
					if(piece7a != null) size_traj += piece7a.length;
					
					trajectory = new Position[size_traj];
					for(i = 0; i < trajectory.length; i++)
						trajectory[i] = new Position();
					
					int k = 0;
					if(piece1a != null)	for(i = 0; i < piece1a.length; i++)	trajectory[k++].set(piece1a[i]);
					if(piece2a != null)	for(i = 0; i < piece2a.length; i++)	trajectory[k++].set(piece2a[i]);
					if(piece3a != null)	for(i = 0; i < piece3a.length; i++)	trajectory[k++].set(piece3a[i]);
					if(piece4a != null)	for(i = 0; i < piece4a.length; i++)	trajectory[k++].set(piece4a[i]);
					if(piece5a != null)	for(i = 0; i < piece5a.length; i++)	trajectory[k++].set(piece5a[i]);
					if(piece6a != null)	for(i = 0; i < piece6a.length; i++)	trajectory[k++].set(piece6a[i]);
					if(piece7a != null)	for(i = 0; i < piece7a.length; i++)	trajectory[k++].set(piece7a[i]);
					
					//System.out.println(" Lenght: " + lenght3a);
					//if (piece5a != null)
					//	for(i = 0; i < piece5a.length; i++)
					//		System.out.println(" piece5a[" + i + "]: <" + piece5a[i].x() + "," + piece5a[i].y() + ">");
					//System.out.println("");
					
				} catch (Exception e) {
					e.printStackTrace();
				}
				
			} else {
				length = 0;
				trajectory = null;
			}
		}
	}
	
	class PathRLR {
		
		double x_i, y_i, theta_i;
		double x_e, y_e, theta_e;
		double rv, gamma;
		double b_lim;
		
		double length;
		Position[] trajectory;
		
		public PathRLR (double x_i, double y_i, double theta_i, double x_e, double y_e, double theta_e, double rv, double gamma, double b_lim) {
			this.x_i		= x_i;
			this.y_i		= y_i;
			this.theta_i	= theta_i;
			this.x_e		= x_e;
			this.y_e		= y_e;
			this.theta_e	= theta_e;
			this.gamma		= gamma;
			this.rv			= rv;
			this.b_lim		= b_lim;
			
			length = 0;
			trajectory = null;
		}
		
		double getLength() { return length; }
		
		public Position[] getTrajectory() {	return trajectory; }
		
		public void calculatePath () {
			length = 0;
			trajectory = null;
			
			// Angle normalisation
			while(theta_i >= Math.toRadians(360))	theta_i -= Math.toRadians(360);
			while(theta_i < Math.toRadians(0))		theta_i += Math.toRadians(360);
			
			while(theta_e >= Math.toRadians(360))	theta_e -= Math.toRadians(360);
			while(theta_e < Math.toRadians(0))		theta_e += Math.toRadians(360);
			
			// Calculate circle RIGHT_ORIENTATION
			CalculateCircle circle_centre = new CalculateCircle();
			Position omega1, omega2;
			omega1 = circle_centre.calculateCenterCircle (x_i, y_i, theta_i, rv, gamma, RIGHT_ORIENTATION, FIRST_ENDPOINT);
			omega2 = circle_centre.calculateCenterCircle (x_e, y_e, theta_e, rv, gamma, RIGHT_ORIENTATION, LAST_ENDPOINT);
			
			double x_omega1, y_omega1, x_omega2, y_omega2;
			x_omega1 = omega1.x();
			y_omega1 = omega1.y();
			x_omega2 = omega2.x();
			y_omega2 = omega2.y();
			
			// System.out.println(" omega1: <" + omega1.x() + "," + omega1.y() + "> omega2: <" + omega2.x() + "," + omega2.y() + ">");
			
			// Condition to calculate the path
			double dx, dy, path_cond;
			dx = x_omega2 - x_omega1;
			dy = y_omega2 - y_omega1;
			path_cond = Math.sqrt((dx*dx)+(dy*dy));
			
			// System.out.println(" path_cond: " + path_cond + " 4rv: " + (4.0*rv));
			
			if (path_cond < (4.0*rv))
			{
				if ((y_omega2 - y_omega1) == 0.0)
					y_omega2 = y_omega2 * (1+10e-6);
				
				// Calculate the polynomial
				double coef_a, coef_b, coef_c;
				coef_a = 2.0 * dx;
				coef_b = 2.0 * dy;
				coef_c = (x_omega1*x_omega1)+(y_omega1*y_omega1)-(x_omega2*x_omega2)-(y_omega2*y_omega2);
				
				// System.out.println(" coef_a: " + coef_a + " coef_b: " + coef_b + " coef_c: " + coef_c);
				
				double P1_pi, P2_pi, P3_pi;
				P1_pi = ((coef_a*coef_a)/(coef_b*coef_b)) + 1.0;
				P2_pi = 2.0 * ( ((coef_a/coef_b)*((coef_c/coef_b) + y_omega1)) - x_omega1 );
				P3_pi = (x_omega1*x_omega1) + ((coef_c/coef_b)*(coef_c/coef_b)) + (2.0*y_omega1*(coef_c/coef_b)) + (y_omega1*y_omega1) - (4.0*rv*rv);
				
				// System.out.println(" P1: " + P1_pi + " P2_pi: " + P2_pi + " P3_pi: " + P3_pi);
				
				try {
					double[] poly_coef = new double[3];
					poly_coef[2] = P1_pi;
					poly_coef[1] = P2_pi;
					poly_coef[0] = P3_pi;
					
					// Create polynomial p(x) = P1_pi*x^2 + P2_pi*x + P3_pi
					Polynomial poly = new Polynomial(poly_coef);
					
					// Calculate the roots of the polynomial
					Complex[] zeros = poly.zeros();
					
					double[] xroots;
					double[] yroots;
					xroots = new double[zeros.length];
					yroots = new double[zeros.length];
					
					int i;
					for (i = 0; i < zeros.length; ++i)
					{
						xroots[i] = zeros[i].re(); // x_omegai
						yroots[i] = -((xroots[i] * coef_a) + coef_c) / coef_b; // y_omegai
					}
					
					//for (i = 0; i < xroots.length; ++i)
					//{
					//	System.out.println(" xroots[" + i + "]: " + xroots[i]);
					//	System.out.println(" yroots[" + i + "]: " + yroots[i]);
					//}
					
					// System.out.println(" x_omega1: " + x_omega1 + " y_omega1: " + y_omega1 + " x_omega2: " + x_omega2 + " y_omega2: " + y_omega2);
					
					// Choice the central root of intermediate circle
					double m2i1, m2i2, m1i1, m1i2;
					m2i1 = (y_omega1-yroots[0])/(x_omega1-xroots[0]);
					m2i2 = (y_omega1-yroots[1])/(x_omega1-xroots[1]);
					m1i1 = (y_omega2-yroots[0])/(x_omega2-xroots[0]);
					m1i2 = (y_omega2-yroots[1])/(x_omega2-xroots[1]);
					
					//System.out.println(" m2i1: " + m2i1);
					//System.out.println(" m2i2: " + m2i2);
					//System.out.println(" m1i1: " + m1i1);
					//System.out.println(" m1i2: " + m1i2);
					
					double[] roots3;
					roots3 = calculateRoots (m2i1, x_omega1, y_omega1, xroots[0], yroots[0], rv);
					
					double[] roots4;
					roots4 = calculateRoots (m2i2, x_omega1, y_omega1, xroots[1], yroots[1], rv);
					
					double[] roots5;
					roots5 = calculateRoots (m1i1, x_omega2, y_omega2, xroots[0], yroots[0], rv);
					
					double[] roots6;
					roots6 = calculateRoots (m1i2, x_omega2, y_omega2, xroots[1], yroots[1], rv);
					
					//for (i = 0; i < roots6.length; i++)
					//	System.out.println(" roots3[" + i + "]: " + roots3[i]);
					
					double lenghta, lenghtb;
					
					double long1a, long2a, long3a;
					double lenght1a, lenght2a, lenght3a;
					Position[] piece1a, piece2a, piece3a, piece4a, piece5a, piece6a, piece7a, piece8a, piece9a;
					
					CalculateCurve ccurve;
					ccurve = new CalculateCurve(rv, gamma, b_lim);
					
					ccurve.toRight(x_i, y_i, theta_i, x_omega1, y_omega1, roots3[0], roots3[1]);
					ccurve.deleteLastPoint();
					
					lenght1a = ccurve.getLength();
					piece1a = ccurve.getPiece1();
					piece2a = ccurve.getPiece2();
					piece3a = ccurve.getPiece3();
					
					if (lenght1a > b_lim)
						long1a = (Math.abs(lenght1a)/k_max) + (k_max/Math.abs(sigma_max));
					else
						long1a = 2.0*Math.sqrt(lenght1a/ccurve.getSigma0());
					
					ccurve.toLeft(roots3[0], roots3[1], theta_i-lenght1a, xroots[0], yroots[0], roots5[0], roots5[1]);
					ccurve.deleteLastPoint();
					
					lenght2a = ccurve.getLength();
					piece4a = ccurve.getPiece1();
					piece5a = ccurve.getPiece2();
					piece6a = ccurve.getPiece3();
					
					//System.out.println(" lenght1a: " + lenght1a);
					//if (piece3a != null)
					//	for(i = 0; i < piece3a.length; i++)
					//		System.out.println(" piece3a[" + i + "]: <" + piece3a[i].x() + "," + piece3a[i].y() + ">");
					//System.out.println("");
					
					if (lenght2a > b_lim)
						long2a = long1a + (Math.abs(lenght2a)/k_max) + (k_max/Math.abs(sigma_max));
					else
						long2a = long1a + 2.0*Math.sqrt(lenght2a/ccurve.getSigma0());
					
					ccurve.toRight(roots5[0], roots5[1], theta_i-lenght1a+lenght2a, x_omega2, y_omega2, x_e, y_e);
					
					lenght3a = ccurve.getLength();
					piece7a = ccurve.getPiece1();
					piece8a = ccurve.getPiece2();
					piece9a = ccurve.getPiece3();
					
					if (lenght3a > b_lim)
						long3a = long2a + (Math.abs(lenght3a)/k_max) + (k_max/Math.abs(sigma_max));
					else
						long3a = long2a + 2.0*Math.sqrt(lenght3a/ccurve.getSigma0());
					
					lenghta = long3a;
					
					double long1b, long2b, long3b;
					double lenght1b, lenght2b, lenght3b;
					Position[] piece1b, piece2b, piece3b, piece4b, piece5b, piece6b, piece7b, piece8b, piece9b;
					
					ccurve.toRight(x_i, y_i, theta_i, x_omega1, y_omega1, roots4[0], roots4[1]);
					ccurve.deleteLastPoint();
					
					lenght1b = ccurve.getLength();
					piece1b = ccurve.getPiece1();
					piece2b = ccurve.getPiece2();
					piece3b = ccurve.getPiece3();
					
					if (lenght1b > b_lim)
						long1b = (Math.abs(lenght1b)/k_max) + (k_max/Math.abs(sigma_max));
					else
						long1b = 2.0*Math.sqrt(lenght1b/ccurve.getSigma0());
					
					ccurve.toLeft(roots4[0], roots4[1], theta_i-lenght1b, xroots[1], yroots[1], roots6[0], roots6[1]);
					ccurve.deleteLastPoint();
					
					lenght2b = ccurve.getLength();
					piece4b = ccurve.getPiece1();
					piece5b = ccurve.getPiece2();
					piece6b = ccurve.getPiece3();
					
					if (lenght2b > b_lim)
						long2b = long1b + (Math.abs(lenght2b)/k_max) + (k_max/Math.abs(sigma_max));
					else
						long2b = long1b + 2.0*Math.sqrt(lenght2b/ccurve.getSigma0());
					
					// System.out.println(x_omega2 + "," + y_omega2);
					
					ccurve.toRight(roots6[0], roots6[1], theta_i-lenght1b+lenght2b, x_omega2, y_omega2, x_e, y_e);
					
					lenght3b = ccurve.getLength();
					piece7b = ccurve.getPiece1();
					piece8b = ccurve.getPiece2();
					piece9b = ccurve.getPiece3();
					
					if (lenght3b > b_lim)
						long3b = long2b + (Math.abs(lenght3b)/k_max) + (k_max/Math.abs(sigma_max));
					else
						long3b = long2b + 2.0*Math.sqrt(lenght3b/ccurve.getSigma0());
					
					lenghtb = long3b;
					
					//System.out.println(" lenght3b: " + lenght3b);
					//if (piece9b != null)
					//	for(i = 0; i < piece9b.length; i++)
					//		System.out.println(" piece9b[" + i + "]: <" + piece9b[i].x() + "," + piece9b[i].y() + ">");
					//System.out.println("");
					
					//System.out.println(" lenghta: " + lenghta + " lenghtb: " + lenghtb);
					
					int k;
					int size_traj;
					if (lenghta >= lenghtb)
					{
						length = lenghtb;
						
						size_traj = 0;
						if(piece1b != null) size_traj += piece1b.length;
						if(piece2b != null) size_traj += piece2b.length;
						if(piece3b != null) size_traj += piece3b.length;
						if(piece4b != null) size_traj += piece4b.length;
						if(piece5b != null) size_traj += piece5b.length;
						if(piece6b != null) size_traj += piece6b.length;
						if(piece7b != null) size_traj += piece7b.length;
						if(piece8b != null) size_traj += piece8b.length;
						if(piece9b != null) size_traj += piece9b.length;
						
						trajectory = new Position[size_traj];
						for(i = 0; i < trajectory.length; i++)
							trajectory[i] = new Position();
						
						k=0;
						if(piece1b != null) for(i = 0; i < piece1b.length; i++)	trajectory[k++].set(piece1b[i]);
						if(piece2b != null) for(i = 0; i < piece2b.length; i++)	trajectory[k++].set(piece2b[i]);
						if(piece3b != null) for(i = 0; i < piece3b.length; i++)	trajectory[k++].set(piece3b[i]);
						if(piece4b != null) for(i = 0; i < piece4b.length; i++)	trajectory[k++].set(piece4b[i]);
						if(piece5b != null) for(i = 0; i < piece5b.length; i++)	trajectory[k++].set(piece5b[i]);
						if(piece6b != null) for(i = 0; i < piece6b.length; i++)	trajectory[k++].set(piece6b[i]);
						if(piece7b != null) for(i = 0; i < piece7b.length; i++)	trajectory[k++].set(piece7b[i]);
						if(piece8b != null) for(i = 0; i < piece8b.length; i++)	trajectory[k++].set(piece8b[i]);
						if(piece9b != null) for(i = 0; i < piece9b.length; i++)	trajectory[k++].set(piece9b[i]);						
					} else {
						length = lenghta;
						
						size_traj = 0;
						if(piece1a != null) size_traj += piece1a.length;
						if(piece2a != null) size_traj += piece2a.length;
						if(piece3a != null) size_traj += piece3a.length;
						if(piece4a != null) size_traj += piece4a.length;
						if(piece5a != null) size_traj += piece5a.length;
						if(piece6a != null) size_traj += piece6a.length;
						if(piece7a != null) size_traj += piece7a.length;
						if(piece8a != null) size_traj += piece8a.length;
						if(piece9a != null) size_traj += piece9a.length;
						
						trajectory = new Position[size_traj];
						for(i = 0; i < trajectory.length; i++)
							trajectory[i] = new Position();
						
						k=0;
						if(piece1a != null)	for(i = 0; i < piece1a.length; i++)	trajectory[k++].set(piece1a[i]);
						if(piece2a != null)	for(i = 0; i < piece2a.length; i++)	trajectory[k++].set(piece2a[i]);
						if(piece3a != null)	for(i = 0; i < piece3a.length; i++)	trajectory[k++].set(piece3a[i]);
						if(piece4a != null)	for(i = 0; i < piece4a.length; i++)	trajectory[k++].set(piece4a[i]);
						if(piece5a != null)	for(i = 0; i < piece5a.length; i++)	trajectory[k++].set(piece5a[i]);
						if(piece6a != null)	for(i = 0; i < piece6a.length; i++)	trajectory[k++].set(piece6a[i]);
						if(piece7a != null)	for(i = 0; i < piece7a.length; i++)	trajectory[k++].set(piece7a[i]);
						if(piece8a != null)	for(i = 0; i < piece8a.length; i++)	trajectory[k++].set(piece8a[i]);
						if(piece9a != null)	for(i = 0; i < piece9a.length; i++)	trajectory[k++].set(piece9a[i]);
					}
				} catch (Exception e) {
					e.printStackTrace();
				}
				
			} else {
				length = 0;
				trajectory = null;
			}
			
		}
		
		double[] calculateRoots (double cvalue, double xomega, double yomega, double xroot, double yroot, double rv)
		{
			double[] root_values = new double[2];
			root_values[0] = 0.0;
			root_values[1] = 0.0;
			
			try {
				double[] coef = new double[3];
				coef[2] = (cvalue*cvalue) + 1.0;
				coef[1] = -2.0*xomega*coef[2];
				coef[0] = (xomega*xomega)*coef[2]-(rv*rv);
				
				int i;
				//for(i = 0; i < coef.length; i++)
				//	System.out.println("coef[" + i + "]: " + coef[i]);
				
				// Create polynomial p(x) = P1_pi*x^2 + P2_pi*x + P3_pi
				Polynomial poly = new Polynomial(coef);
				
				// Calculate the roots of the polynomial
				Complex[] zeros = poly.zeros();
				
				double[] xroots;
				double[] yroots;
				xroots = new double[zeros.length];
				yroots = new double[zeros.length];
				
				for (i = 0; i < zeros.length; ++i)
				{
					xroots[i] = zeros[i].re();
					yroots[i] = (xroots[i]-xomega)*cvalue+yomega;
				}
				
				//for(i = 0; i < zeros.length; i++)
				//{
				//	System.out.println("xroots[" + i + "]: " + xroots[i]);
				//	System.out.println("yroots[" + i + "]: " + yroots[i]);
				//}
				
				double dist_m2i1a, dist_m2i1b;
				dist_m2i1a = Math.sqrt( ((xroot-xroots[0])*(xroot-xroots[0])) + ((yroot-yroots[0])*(yroot-yroots[0])) );
				dist_m2i1b = Math.sqrt( ((xroot-xroots[1])*(xroot-xroots[1])) + ((yroot-yroots[1])*(yroot-yroots[1])) );
				
				// System.out.println(" dist_m2i1a: " + dist_m2i1a + " dist_m2i1b: " + dist_m2i1b);
				
				if(dist_m2i1b <= dist_m2i1a)
				{
					root_values[0] = xroots[1];
					root_values[1] = yroots[1];
				} else {
					root_values[0] = xroots[0];
					root_values[1] = yroots[0];
				}
				
			} catch (Exception e) {
				e.printStackTrace();
			}
			
			return root_values;
		}
	}
	
	class PathLRL {
		
		double x_i, y_i, theta_i;
		double x_e, y_e, theta_e;
		double rv, gamma;
		double b_lim;
		
		double length;
		Position[] trajectory;
		
		public PathLRL (double x_i, double y_i, double theta_i, double x_e, double y_e, double theta_e, double rv, double gamma, double b_lim) {
			this.x_i		= x_i;
			this.y_i		= y_i;
			this.theta_i	= theta_i;
			this.x_e		= x_e;
			this.y_e		= y_e;
			this.theta_e	= theta_e;
			this.gamma		= gamma;
			this.rv			= rv;
			this.b_lim		= b_lim;
			
			length = 0;
			trajectory = null;
		}
		
		double getLength() { return length; }
		
		public Position[] getTrajectory() {	return trajectory; }
		
		public void calculatePath () {
			length = 0;
			trajectory = null;
			
			// Angle normalisation
			while(theta_i >= Math.toRadians(360))	theta_i -= Math.toRadians(360);
			while(theta_i < Math.toRadians(0))		theta_i += Math.toRadians(360);
			
			while(theta_e >= Math.toRadians(360))	theta_e -= Math.toRadians(360);
			while(theta_e < Math.toRadians(0))		theta_e += Math.toRadians(360);
			
			//System.out.println(" x_i: " + x_i + " y_i: " + y_i + " theta_i: " + theta_i);
			//System.out.println(" rv: " + rv + " gamma: " + gamma);
			
			//System.out.println(" x_e: " + x_e + " y_e: " + y_e + " theta_e: " + theta_e);
			
			// Calculate circle LEFT_ORIENTATION
			CalculateCircle circle_centre = new CalculateCircle();
			Position omega1, omega2;
			omega1 = circle_centre.calculateCenterCircle (x_i, y_i, theta_i, rv, gamma, LEFT_ORIENTATION, FIRST_ENDPOINT);
			omega2 = circle_centre.calculateCenterCircle (x_e, y_e, theta_e, rv, gamma, LEFT_ORIENTATION, LAST_ENDPOINT);
			
			double x_omega1, y_omega1, x_omega2, y_omega2;
			x_omega1 = omega1.x();
			y_omega1 = omega1.y();
			x_omega2 = omega2.x();
			y_omega2 = omega2.y();
			
			//System.out.println(" omega1: <" + omega1.x() + "," + omega1.y() + "> omega2: <" + omega2.x() + "," + omega2.y() + ">");
			
			// Condition to calculate the path
			double dx, dy, path_cond;
			dx = x_omega2 - x_omega1;
			dy = y_omega2 - y_omega1;
			path_cond = Math.sqrt((dx*dx)+(dy*dy));
			
			// System.out.println(" path_cond: " + path_cond + "4rv: " + (4.0*rv));
			
			if (path_cond < (4.0*rv))
			{
				if ((y_omega2 - y_omega1) == 0.0)
					y_omega2 = y_omega2 * (1+10e-6);
				
				// Calculate the polynomial
				double coef_a, coef_b, coef_c;
				coef_a = 2.0 * dx;
				coef_b = 2.0 * dy;
				coef_c = (x_omega1*x_omega1)+(y_omega1*y_omega1)-(x_omega2*x_omega2)-(y_omega2*y_omega2);
				
				// System.out.println(" coef_a: " + coef_a + " coef_b: " + coef_b + " coef_c: " + coef_c);
				
				double P1_pi, P2_pi, P3_pi;
				P1_pi = ((coef_a*coef_a)/(coef_b*coef_b)) + 1.0;
				P2_pi = 2.0 * ( ((coef_a/coef_b)*((coef_c/coef_b) + y_omega1)) - x_omega1 );
				P3_pi = (x_omega1*x_omega1) + ((coef_c/coef_b)*(coef_c/coef_b)) + (2.0*y_omega1*(coef_c/coef_b)) + (y_omega1*y_omega1) - (4.0*rv*rv);
				
				// System.out.println(" P1: " + P1_pi + " P2_pi: " + P2_pi + " P3_pi: " + P3_pi);
				
				try {
					double[] poly_coef = new double[3];
					poly_coef[2] = P1_pi;
					poly_coef[1] = P2_pi;
					poly_coef[0] = P3_pi;
					
					// Create polynomial p(x) = P1_pi*x^2 + P2_pi*x + P3_pi
					Polynomial poly = new Polynomial(poly_coef);
					
					// Calculate the roots of the polynomial
					Complex[] zeros = poly.zeros();
					
					double[] xroots;
					double[] yroots;
					xroots = new double[zeros.length];
					yroots = new double[zeros.length];
					
					int i;
					for (i = 0; i < zeros.length; ++i)
					{
						xroots[i] = zeros[i].re(); // x_omegai
						yroots[i] = -((xroots[i] * coef_a) + coef_c) / coef_b; // y_omegai
					}
					
					//for (i = 0; i < xroots.length; ++i)
					//{
					//	System.out.println(" xroots[" + i + "]: " + xroots[i]);
					//	System.out.println(" yroots[" + i + "]: " + yroots[i]);
					//}
					
					// System.out.println(" x_omega1: " + x_omega1 + " y_omega1: " + y_omega1 + " x_omega2: " + x_omega2 + " y_omega2: " + y_omega2);
					
					// Choice the central root of intermediate circle
					double m2i1, m2i2, m1i1, m1i2;
					m2i1 = (y_omega2-yroots[0])/(x_omega2-xroots[0]);
					m2i2 = (y_omega2-yroots[1])/(x_omega2-xroots[1]);
					m1i1 = (y_omega1-yroots[0])/(x_omega1-xroots[0]);
					m1i2 = (y_omega1-yroots[1])/(x_omega1-xroots[1]);
					
					//System.out.println(" m2i1: " + m2i1);
					//System.out.println(" m2i2: " + m2i2);
					//System.out.println(" m1i1: " + m1i1);
					//System.out.println(" m1i2: " + m1i2);
					
					double[] roots3;
					roots3 = calculateRoots (m2i1, x_omega2, y_omega2, xroots[0], yroots[0], rv);
					
					double[] roots4;
					roots4 = calculateRoots (m2i2, x_omega2, y_omega2, xroots[1], yroots[1], rv);
					
					double[] roots5;
					roots5 = calculateRoots (m1i1, x_omega1, y_omega1, xroots[0], yroots[0], rv);
					
					double[] roots6;
					roots6 = calculateRoots (m1i2, x_omega1, y_omega1, xroots[1], yroots[1], rv);
					
					// for (i = 0; i < roots3.length; i++)
					//	System.out.println(" roots6[" + i + "]: " + roots6[i]);
					
					double lenghta, lenghtb;
					
					double long1a, long2a, long3a;
					double lenght1a, lenght2a, lenght3a;
					Position[] piece1a, piece2a, piece3a, piece4a, piece5a, piece6a, piece7a, piece8a, piece9a;
					
					CalculateCurve ccurve;
					ccurve = new CalculateCurve(rv, gamma, b_lim);
					
					//System.out.println(x_i + "," + y_i + "," + theta_i + "," + x_omega1 + "," + y_omega1 + "," + roots5[0] + "," + roots5[1]);
					
					ccurve.toLeft(x_i, y_i, theta_i, x_omega1, y_omega1, roots5[0], roots5[1]);
					ccurve.deleteLastPoint();
					
					lenght1a = ccurve.getLength();
					piece1a = ccurve.getPiece1();
					piece2a = ccurve.getPiece2();
					piece3a = ccurve.getPiece3();
					
					if (lenght1a > b_lim)
						long1a = (Math.abs(lenght1a)/k_max) + (k_max/Math.abs(sigma_max));
					else
						long1a = 2.0*Math.sqrt(lenght1a/ccurve.getSigma0());
					
					ccurve.toRight(roots5[0], roots5[1], theta_i+lenght1a, xroots[0], yroots[0], roots3[0], roots3[1]);
					ccurve.deleteLastPoint();
					
					lenght2a = ccurve.getLength();
					piece4a = ccurve.getPiece1();
					piece5a = ccurve.getPiece2();
					piece6a = ccurve.getPiece3();
					
					if (lenght2a > b_lim)
						long2a = long1a + (Math.abs(lenght2a)/k_max) + (k_max/Math.abs(sigma_max));
					else
						long2a = long1a + 2.0*Math.sqrt(lenght2a/ccurve.getSigma0());
					
					ccurve.toLeft(roots3[0], roots3[1], theta_i+lenght1a-lenght2a, x_omega2, y_omega2, x_e, y_e);
					
					lenght3a = ccurve.getLength();
					piece7a = ccurve.getPiece1();
					piece8a = ccurve.getPiece2();
					piece9a = ccurve.getPiece3();
					
					if (lenght3a > b_lim)
						long3a = long2a + (Math.abs(lenght3a)/k_max) + (k_max/Math.abs(sigma_max));
					else
						long3a = long2a + 2.0*Math.sqrt(lenght3a/ccurve.getSigma0());
					
					lenghta = long3a;
					
					double long1b, long2b, long3b;
					double lenght1b, lenght2b, lenght3b;
					Position[] piece1b, piece2b, piece3b, piece4b, piece5b, piece6b, piece7b, piece8b, piece9b;
					
					//System.out.println("ccurve.toLeft 3");
					
					//System.out.println(" x_i: " + x_i + " y_i: " + y_i + " theta_i: " + theta_i);
					//System.out.println(" x_omega1: " + x_omega1 + " y_omega1: " + y_omega1);
					//System.out.println(" roots6[0]: " + roots6[0] + " roots6[1]: " + roots6[1]);
					
					ccurve.toLeft(x_i, y_i, theta_i, x_omega1, y_omega1, roots6[0], roots6[1]);
					ccurve.deleteLastPoint();
					
					lenght1b = ccurve.getLength();
					piece1b = ccurve.getPiece1();
					piece2b = ccurve.getPiece2();
					piece3b = ccurve.getPiece3();
					
					if (lenght1b > b_lim)
						long1b = (Math.abs(lenght1b)/k_max) + (k_max/Math.abs(sigma_max));
					else
						long1b = 2.0*Math.sqrt(lenght1b/ccurve.getSigma0());
					
					ccurve.toRight(roots6[0], roots6[1], theta_i+lenght1b, xroots[1], yroots[1], roots4[0], roots4[1]);
					ccurve.deleteLastPoint();
					
					lenght2b = ccurve.getLength();
					piece4b = ccurve.getPiece1();
					piece5b = ccurve.getPiece2();
					piece6b = ccurve.getPiece3();
					
					if (lenght2b > b_lim)
						long2b = long1b + (Math.abs(lenght2b)/k_max) + (k_max/Math.abs(sigma_max));
					else
						long2b = long1b + 2.0*Math.sqrt(lenght2b/ccurve.getSigma0());
					
					ccurve.toLeft(roots4[0], roots4[1], theta_i+lenght1b-lenght2b, x_omega2, y_omega2, x_e, y_e);
					
					lenght3b = ccurve.getLength();
					piece7b = ccurve.getPiece1();
					piece8b = ccurve.getPiece2();
					piece9b = ccurve.getPiece3();
					
					//if (piece1a != null)
					//	for(i = 0; i < piece1a.length; i++)
					//		System.out.println(" piece1a[" + i + "]: <" + piece1a[i].x() + "," + piece1a[i].y() + ">");
					//System.out.println("");
					
					if (lenght3b > b_lim)
						long3b = long2b + (Math.abs(lenght3b)/k_max) + (k_max/Math.abs(sigma_max));
					else
						long3b = long2b + 2.0*Math.sqrt(lenght3b/ccurve.getSigma0());
					
					lenghtb = long3b;
					
					//System.out.println(" lenghta: " + lenghta + " lenghtb: " + lenghtb);
					
					int k;
					int size_traj;
					if (lenghta >= lenghtb)
					{
						length = lenghtb;
						
						size_traj = 0;
						if(piece1b != null) size_traj += piece1b.length;
						if(piece2b != null) size_traj += piece2b.length;
						if(piece3b != null) size_traj += piece3b.length;
						if(piece4b != null) size_traj += piece4b.length;
						if(piece5b != null) size_traj += piece5b.length;
						if(piece6b != null) size_traj += piece6b.length;
						if(piece7b != null) size_traj += piece7b.length;
						if(piece8b != null) size_traj += piece8b.length;
						if(piece9b != null) size_traj += piece9b.length;
						
					//System.out.println("CASE B: " + size_traj);
						
						trajectory = new Position[size_traj];
						for(i = 0; i < trajectory.length; i++)
							trajectory[i] = new Position();
						
						k=0;
						if(piece1b != null) for(i = 0; i < piece1b.length; i++)	trajectory[k++].set(piece1b[i]);
						if(piece2b != null) for(i = 0; i < piece2b.length; i++)	trajectory[k++].set(piece2b[i]);
						if(piece3b != null) for(i = 0; i < piece3b.length; i++)	trajectory[k++].set(piece3b[i]);
						if(piece4b != null) for(i = 0; i < piece4b.length; i++)	trajectory[k++].set(piece4b[i]);
						if(piece5b != null) for(i = 0; i < piece5b.length; i++)	trajectory[k++].set(piece5b[i]);
						if(piece6b != null) for(i = 0; i < piece6b.length; i++)	trajectory[k++].set(piece6b[i]);
						if(piece7b != null) for(i = 0; i < piece7b.length; i++)	trajectory[k++].set(piece7b[i]);
						if(piece8b != null) for(i = 0; i < piece8b.length; i++)	trajectory[k++].set(piece8b[i]);
						if(piece9b != null) for(i = 0; i < piece9b.length; i++)	trajectory[k++].set(piece9b[i]);						
					} else {
						length = lenghta;
						
						size_traj = 0;
						if(piece1a != null) size_traj += piece1a.length;
						if(piece2a != null) size_traj += piece2a.length;
						if(piece3a != null) size_traj += piece3a.length;
						if(piece4a != null) size_traj += piece4a.length;
						if(piece5a != null) size_traj += piece5a.length;
						if(piece6a != null) size_traj += piece6a.length;
						if(piece7a != null) size_traj += piece7a.length;
						if(piece8a != null) size_traj += piece8a.length;
						if(piece9a != null) size_traj += piece9a.length;
						
					//System.out.println("CASE A: " + size_traj);
						
						trajectory = new Position[size_traj];
						for(i = 0; i < trajectory.length; i++)
							trajectory[i] = new Position();
						
						k=0;
						if(piece1a != null)	for(i = 0; i < piece1a.length; i++)	trajectory[k++].set(piece1a[i]);
						if(piece2a != null)	for(i = 0; i < piece2a.length; i++)	trajectory[k++].set(piece2a[i]);
						if(piece3a != null)	for(i = 0; i < piece3a.length; i++)	trajectory[k++].set(piece3a[i]);
						if(piece4a != null)	for(i = 0; i < piece4a.length; i++)	trajectory[k++].set(piece4a[i]);
						if(piece5a != null)	for(i = 0; i < piece5a.length; i++)	trajectory[k++].set(piece5a[i]);
						if(piece6a != null)	for(i = 0; i < piece6a.length; i++)	trajectory[k++].set(piece6a[i]);
						if(piece7a != null)	for(i = 0; i < piece7a.length; i++)	trajectory[k++].set(piece7a[i]);
						if(piece8a != null)	for(i = 0; i < piece8a.length; i++)	trajectory[k++].set(piece8a[i]);
						if(piece9a != null)	for(i = 0; i < piece9a.length; i++)	trajectory[k++].set(piece9a[i]);
					}
				} catch (Exception e) {
					e.printStackTrace();
				}
				
			} else {
				length = 0;
				trajectory = null;
			}
			
			//System.out.println(" xomega: " + omega1[0] + " yomega: " + omega1[1]);
			//System.out.println(" x: " + x_i + " y: " + y_i + " theta: " + theta_i + " rv: " + rv + " gamma: " + gamma);
		}
		
		double[] calculateRoots (double cvalue, double xomega, double yomega, double xroot, double yroot, double rv)
		{
			double[] root_values = new double[2];
			root_values[0] = 0.0;
			root_values[1] = 0.0;
			
			try {
				double[] coef = new double[3];
				coef[2] = (cvalue*cvalue) + 1.0;
				coef[1] = -2.0*xomega*coef[2];
				coef[0] = (xomega*xomega)*coef[2]-(rv*rv);
				
				int i;
				//for(i = 0; i < coef.length; i++)
				//	System.out.println("coef[" + i + "]: " + coef[i]);
				
				// Create polynomial p(x) = P1_pi*x^2 + P2_pi*x + P3_pi
				Polynomial poly = new Polynomial(coef);
				
				// Calculate the roots of the polynomial
				Complex[] zeros = poly.zeros();
				
				double[] xroots;
				double[] yroots;
				xroots = new double[zeros.length];
				yroots = new double[zeros.length];
				
				for (i = 0; i < zeros.length; ++i)
				{
					xroots[i] = zeros[i].re();
					yroots[i] = (xroots[i]-xomega)*cvalue+yomega;
				}
				
				//for(i = 0; i < zeros.length; i++)
				//{
				//	System.out.println("xroots[" + i + "]: " + xroots[i]);
				//	System.out.println("yroots[" + i + "]: " + yroots[i]);
				//}
				
				double dist_m2i1a, dist_m2i1b;
				dist_m2i1a = Math.sqrt( ((xroot-xroots[0])*(xroot-xroots[0])) + ((yroot-yroots[0])*(yroot-yroots[0])) );
				dist_m2i1b = Math.sqrt( ((xroot-xroots[1])*(xroot-xroots[1])) + ((yroot-yroots[1])*(yroot-yroots[1])) );
				
				// System.out.println(" dist_m2i1a: " + dist_m2i1a + " dist_m2i1b: " + dist_m2i1b);
				
				if(dist_m2i1b <= dist_m2i1a)
				{
					root_values[0] = xroots[1];
					root_values[1] = yroots[1];
				} else {
					root_values[0] = xroots[0];
					root_values[1] = yroots[0];
				}
				
			} catch (Exception e) {
				e.printStackTrace();
			}
			
			return root_values;
		}
	}
	
	// Circle options
	final private int RIGHT_ORIENTATION	= 0;
	final private int LEFT_ORIENTATION	= 1;
	final private int FIRST_ENDPOINT	= 0;
	final private int LAST_ENDPOINT		= 1;
	
	// Tools for calculating curves
	CalculateCurve ccurve;
	CalculateSegment csegment;
	
	// Tools for calculating the paths
	PathLRL plrl;
	PathRLR prlr;
	PathRSL prsl;
	PathLSR plsr;
	PathLSL plsl;
	PathRSR prsr;
	
	// Path
	Position[] path;
	double length;
	
	// Problem  parameters
	double x_i,y_i,theta_i;		// initial point
	double x_e,y_e,theta_e;		// end point
	double k_max;				// curvature radius inverse : tan(phi)=L/R -> k_max=tan(phi_max)/L
	double sigma_max;			// curvature derivative : (dk/dt=dk/ds*ds/dt) sigma_max=(dk/dt)max/(ds/dt)max
	
	double spacement;				// threshold
	double th_sp;				// threshold
	
	public PathFinder() {
		reset();
	}
	
	public void reset() {
		path = null;
		length = 0;
	}
	
	public Position[] getPath()	{	return path;	}
	public double getLength()	{	return length;	}
	
	/*
	 * Calculate Fresnel sin and cos integrals
	 * 
	 *         /x                                /x
	 * fs(x) = |   sin(pi/2*x^2) dx      fc(x) = |   cos(pi/2*x^2) dx
	 *         /0                                /0
	 * 
	 *  d fs                        d fc
	 *  ----- =  sin(pi/2*x^2)      ----- =  cos(pi/2*x^2)
	 *   d x                         d x
	 * 
	 * for |x| < 1.8 uses a powers series about x=0 accuracy is better than 5e-16
	 * for |x| > 1.8 uses a minimax rational approximation based on the auxilliary functions f and g accuracy is better than 1e-9
	 * 
	 * The accuracy can be checked by numerically calculating dfs/dx and dfc/dx. For all x this is better than 2e-8
	 * To test the accuracy call with no arguments
	 * Approximations and 20 digit accurate results were generated with maple 6
	 * Definitions for the Fresnel integrals and auxialliary functions f and g are taken from Handbook of Mathematical Functions Abramowitz and Stegun (9th printing 1970)
	 * uses power series for x<xc=1.8                   accurate to machine precision
	 * uses minimax rational approximation for x>xc     accurate to about 1e-9
	 * 
	 * Released under the GPL
	 * jnm11@amtp.cam.ac.uk
	 * J. N. McElwaine
	 * Department of Applied Mathematcis and Theoretical Physics
	 * University of Cambridge
	 * Silver Street
	 * UK
	 * Version 1.0 October 2001
	 */
	private double[] fresnel (double x)
	{
		double[] ifresnel;
		ifresnel = new double[2];
		
		// Cut point for switching between power series and rational approximation
		double xc;
		xc = 1.8;
		
		// Store the sign of the value
		double sign;
		if(x >= 0)
			sign = 1.0;
		else
			sign = -1.0;
		
		// Absolute value
		x = Math.abs(x);
		
		// switching functions
		double f1, f2;
		if(x <= xc)
		{
			f1 = x;
			f2 = 0.0;
		} else {
			f1 = 0.0;
			f2 = x;
		}
		
		double s, ss, is;
		s = (Math.PI/2.0)*f1*f1;
		ss = s*s;
		is = Math.sqrt(2.0/Math.PI)/f2;
		
		double fs, fc, ff, fg;
		fs = 0.0;
		fc = 0.0;
		ff = 0.0;
		fg = 0.0;
		
		// Approximations for x < 1.6 accurate to eps
		if(x <= xc)
		{
			fs = f1 * s * ((1.0/3.0) + (-0.23809523809523809524e-1 + (0.75757575757575757576e-3 + (-0.13227513227513227513e-4 + (0.14503852223150468764e-6 + (-0.10892221037148573380e-8 + (0.59477940136376350368e-11 + (-0.24668270102644569277e-13 + (0.80327350124157736091e-16 + (-0.21078551914421358249e-18 + (0.45518467589282002862e-21 + (-0.82301492992142213568e-24 + (0.12641078988989163522e-26 + (-0.16697617934173720270e-29 + 0.19169428621097825308e-32 * ss) * ss) * ss) * ss) * ss) * ss) * ss) * ss) * ss) * ss) * ss) * ss) * ss) * ss);
			fc = f1 * (1.0 + (-0.1 + (0.46296296296296296296e-2 + (-0.10683760683760683761e-3 + (0.14589169000933706816e-5 + (-0.13122532963802805073e-7 + (0.83507027951472395917e-10 + (-0.39554295164585257634e-12 + (0.14483264643598137265e-14 + (-0.42214072888070882330e-17 + (0.10025164934907719167e-19 + (-0.19770647538779051748e-22 + (0.32892603491757517328e-25 + (-0.46784835155184857737e-28 + 0.57541916439821717722e-31 * ss) * ss) * ss) * ss) * ss) * ss) * ss) * ss) * ss) * ss) * ss) * ss) * ss) * ss);
		} else {
			ff = Math.sqrt(2.0/Math.PI) * is * (0.36634901025764670680 + (0.84695666222194589182 + (-3.7301273487349839902 + (10.520237918558456228 + (-10.472617402497801126 + 4.4169634834911107719 * is) * is) * is) * is) * is) / (0.73269802661202980832 + (1.6939102288852613619 + (-7.4599994789665215344 + (21.032436583848862358 + (-20.269535575486282590 + 8.9995024877628789836 * is) * is) * is) * is) * is);
			fg = Math.sqrt(2.0/Math.PI) * is * is * is * (0.10935692320079194659 + (0.72025533055541994934 + (-2.5630322339412334317 + (7.2404268469720856874 + (-7.0473933910697823445 + 2.9315207450903789503 * is) * is) * is) * is) * is) / (0.43742772185339366619 + (2.8810049959848098312 + (-10.250672312139277005 + (28.912791022417600679 + (-25.740131167525284201 + 15.145134363709932380 * is) * is) * is) * is) * is);
		}
		
		double cx, sx;
		s = (Math.PI/2.0) * f2 * f2;
		cx = Math.cos(s);
		sx = Math.sin(s);
		
		if(x > xc)
		{
			fs = 0.5 - (ff * cx) - (fg * sx);
			fc = 0.5 + (ff * sx) - (fg * cx);
		}
		
		fs = fs * sign;
		fc = fc * sign;
		
		ifresnel[0] = fs;
		ifresnel[1] = fc;
		
		return ifresnel;
	}
	
	public static void main(String args[]) {
		PathFinder pfinder = new PathFinder();
		
		//System.out.println("angle1: " + (Math.toRadians(-136.8)+Math.PI));
		//System.out.println("angle2: " + (Math.toRadians(90.0)+Math.PI));
		
		//Position posi = new Position(59.94, 56.37, Math.toRadians(-136.8));
		//Position pose = new Position(58.34, 52.7, Math.toRadians(90.0));
		
		Position posi = new Position(59.94, 56.37, Math.toRadians(-136.8)+Math.PI);
		Position pose = new Position(58.34, 52.7, Math.toRadians(90.0)+Math.PI);
		
		//Position posi = new Position(-3.0, -2.0, Math.toRadians(-30.0)+Math.PI);
		//Position pose = new Position(2.0, 2.0, Math.toRadians(90.0)+Math.PI);
		
		//Position posi = new Position (3.5, 4.5, 60.0*Angles.DTOR+Math.PI);
		//Position pose = new Position (5.5, 6.0, -70.0*Angles.DTOR+Math.PI);
		
		pfinder.calculatePath(posi, pose, 0.8357, 0.366, 0.1, 0.1, 10.0, 0.0);
		//pfinder.calculatePath(10.0, -1.0, Math.toRadians(0.0), 3.0, -5.0, Math.toRadians(180.0), 0.8, 0.3, 0.01, 0.1);
		//pfinder.calculatePath( 3.0, 5.0, Math.PI, 15.0, 18.0, 0.0, 0.8, 0.3, 0.1, 0.1);
		//pfinder.calculatePath(3.0, 5.0 , Math.PI, 0.0, 7.0 ,-Math.PI,	0.8, 0.3, 0.1, 0.1);
	}
	
	public void calculatePath (Position pos_i, Position pos_e, double k_max, double sigma_max, double spacement, double th_sp, double extension, double post_extension)
	{
		this.calculatePath(pos_i.x(), pos_i.y(), pos_i.alpha(), pos_e.x(), pos_e.y(), pos_e.alpha(), k_max, sigma_max, spacement, th_sp, extension, post_extension);
	}
	
	public void calculatePath (double x_i, double y_i, double theta_i, double x_e, double y_e, double theta_e, double k_max, double sigma_max, double spacement, double th_sp, double pre_extension, double post_extension)
	{
		// Initialize parameters
		this.x_i = x_i;
		this.y_i = y_i;
		this.theta_i = theta_i;
		this.x_e = x_e;
		this.y_e = y_e;
		this.theta_e = theta_e;
		this.k_max = k_max;
		this.sigma_max = sigma_max;
		
		this.spacement = spacement;
		this.th_sp = th_sp;
		
		//System.out.println(" x_i: " + x_i + " y_i: " + y_i + " theta_i: " + Math.toDegrees(theta_i));
		//System.out.println(" x_e: " + x_e + " y_e: " + y_e + " theta_e: " + Math.toDegrees(theta_e));
		//System.out.println(" k_max: " + k_max + " sigma_max: " + sigma_max);
		
		// Rv and gamma computation
		double b_lim;
		b_lim = (k_max*k_max) / sigma_max;
		
		double[] Fr1;
		Fr1 = fresnel(Math.sqrt(b_lim / Math.PI));
		
		double x1_p, y1_p;
		x1_p = (Math.sqrt(Math.PI * b_lim) / k_max) * Fr1[1];
		y1_p = (Math.sqrt(Math.PI * b_lim) / k_max) * Fr1[0];
		
		double x_omega1p, y_omega1p;
		x_omega1p = x1_p - (Math.sin(b_lim / 2.0) / k_max);
		y_omega1p = y1_p + (Math.cos(b_lim / 2.0) / k_max);
		
		double rv, gamma;
		rv = Math.sqrt( (x_omega1p*x_omega1p) + (y_omega1p*y_omega1p) );
		gamma = Math.atan( x_omega1p / y_omega1p );
		
		double xaux_e, yaux_e;
		
		xaux_e = x_e - pre_extension*Math.cos(theta_e);
		yaux_e = y_e - pre_extension*Math.sin(theta_e);
		
		//System.out.println("b_lim: " + b_lim + " rv: " + rv + " gamma: " + gamma);
		
		// Degenerated cases
		double slope = 0.0;
		double ang_df;
		if ( (xaux_e == x_i) && (y_i < yaux_e) )
		{
			ang_df = 0.5*Math.PI;
		} else if( (xaux_e == x_i) && (y_i > yaux_e) ) {
			ang_df = 1.5*Math.PI;
		} else {
			slope = (yaux_e-y_i)/(xaux_e-x_i);
			ang_df = Math.atan(slope);
			if( (yaux_e >= y_i) && (xaux_e < x_i))
			{
				ang_df = Math.PI - Math.abs(ang_df);
			} else if ( (yaux_e < y_i) && (xaux_e < x_i) ) {
				ang_df = Math.PI + ang_df;
			} else if ( (yaux_e < y_i) && (xaux_e > x_i) ) {
				ang_df = 2*Math.PI + ang_df;
			}
		}
		
		// Calculate possible paths
		double theta_i_norm;
		theta_i_norm = theta_i;
		
		// Angle normalisation
		while(theta_i_norm >= Math.toRadians(360))	theta_i_norm -= Math.toRadians(360);
		while(theta_i_norm < Math.toRadians(0))		theta_i_norm += Math.toRadians(360);
		
		if ((theta_i == theta_e) && (theta_i_norm == ang_df))
		{
			Position[] path_aux;
			
			CalculateSegment cs = new CalculateSegment();
			cs.calculateSegment(x_i,y_i,xaux_e,yaux_e,slope);
			path_aux = cs.getSegment();
			
			path = new Position[path_aux.length + 1];
			for (int i = 0; i < (path.length-1); i++)
			{
				path[i] = new Position(path_aux[i]);
			}
			path[path.length-1] = new Position(xaux_e, yaux_e, 0.0);
			
			length = Math.sqrt( ((xaux_e-x_i)*(xaux_e-x_i)) + ((yaux_e-y_i)*(yaux_e-y_i)) );
			
		} else {
			
			// Calculate possible paths
			Position[] lrl_trajectory, rlr_trajectory, rsl_trajectory, lsr_trajectory, lsl_trajectory, rsr_trajectory;
			
			plrl = new PathLRL(x_i, y_i, theta_i, xaux_e, yaux_e, theta_e, rv, gamma, b_lim);
			plrl.calculatePath ();
			lrl_trajectory = plrl.getTrajectory();
			
			prlr = new PathRLR(x_i, y_i, theta_i, xaux_e, yaux_e, theta_e, rv, gamma, b_lim);
			prlr.calculatePath ();
			rlr_trajectory = prlr.getTrajectory();
			
			prsl = new PathRSL(x_i, y_i, theta_i, xaux_e, yaux_e, theta_e, rv, gamma, b_lim);
			prsl.calculatePath(ang_df, slope);
			rsl_trajectory = prsl.getTrajectory();
			
			plsr = new PathLSR(x_i, y_i, theta_i, xaux_e, yaux_e, theta_e, rv, gamma, b_lim);
			plsr.calculatePath(ang_df, slope);
			lsr_trajectory = plsr.getTrajectory();
			
			plsl = new PathLSL(x_i, y_i, theta_i, xaux_e, yaux_e, theta_e, rv, gamma, b_lim);
			plsl.calculatePath(ang_df, slope);
			lsl_trajectory = plsl.getTrajectory();
			
			prsr = new PathRSR(x_i, y_i, theta_i, xaux_e, yaux_e, theta_e, rv, gamma, b_lim);
			prsr.calculatePath(ang_df, slope);
			rsr_trajectory = prsr.getTrajectory();
			
			//System.out.println(" long: " + plrl.getLength());
			//int i;
			//if(lrl_trajectory != null)
			//	for(i = 0; i < lrl_trajectory.length; i++)
			//		System.out.println(" lrl_trajectory[" + i + "]: <" + lrl_trajectory[i].x() + "," + lrl_trajectory[i].y() + ">");
			//System.out.println("");
			
			// Choice the path with minimum length
			double aux = Double.MAX_VALUE;
			if((plrl.getLength() < aux) && (plrl.getTrajectory() != null))	// LRL
			{
				aux = plrl.getLength();
				path = plrl.getTrajectory();
				
				// System.out.println(" LRL: aux: " + aux);
			}
			if((prlr.getLength() < aux) && (prlr.getTrajectory() != null))	// RLR
			{
				aux = prlr.getLength();
				path = prlr.getTrajectory();
				
				// System.out.println(" RLR: aux: " + aux);
			}
			if((prsl.getLength() < aux) && (prsl.getTrajectory() != null))	// RSL
			{
				aux = prsl.getLength();
				path = prsl.getTrajectory();
				
				// System.out.println(" RSL: aux: " + aux);
			}
			if((plsr.getLength() < aux) && (plsr.getTrajectory() != null))	// LSR
			{
				aux = plsr.getLength();
				path = plsr.getTrajectory();
				
				// System.out.println(" LSR: aux: " + aux);
			}
			if((plsl.getLength() < aux) && (plsl.getTrajectory() != null))	// LSL
			{
				aux = plsl.getLength();
				path = plsl.getTrajectory();
				
				// System.out.println(" LSL: aux: " + aux);
			}
			if((prsr.getLength() < aux) && (prsr.getTrajectory() != null))	// RSR
			{
				aux = prsr.getLength();
				path = prsr.getTrajectory();
				
				// System.out.println(" RSR: aux: " + aux);
			}
			
			length = aux;
			
			// Pre Path extension
			Position[] pextension;
			CalculateSegment calc_segment = new CalculateSegment();
			calc_segment.calculateSegment(xaux_e, yaux_e, x_e, y_e, Math.tan(theta_e));
			pextension = calc_segment.getSegment();
			
			int i,j;
			Position[] path_tmp;
			path_tmp = new Position[path.length - 1 + pextension.length];
			
			for(i = 0; i< (path.length - 1); i++)
				path_tmp[i] = new Position(path[i]);
			
			for(j = 0; j< pextension.length; i++, j++)
				path_tmp[i] = new Position(pextension[j]);
			
			//System.out.println(" length: " + length);
			//System.out.println(" size: path: " + path.length + " extension: " + pextension.length + " path_tmp.length: " + path_tmp.length);
			
			path = new Position[path_tmp.length];
			for(i = 0; i< path_tmp.length; i++)
				path[i] = new Position(path_tmp[i]);
			
			//System.out.println(" Path: length: " + length + " npoints: " + path.length);
			
			// Post Path extension
			double x_f, y_f;
			x_f = x_e + post_extension*Math.cos(theta_e);
			y_f = y_e + post_extension*Math.sin(theta_e);
			
			calc_segment.calculateSegment(x_e, y_e, x_f, y_f, Math.tan(theta_e));
			pextension = calc_segment.getSegment();
			
			path_tmp = new Position[path.length - 1 + pextension.length];
			
			for(i = 0; i< (path.length - 1); i++)
				path_tmp[i] = new Position(path[i]);
			
			for(j = 0; j< pextension.length; i++, j++)
				path_tmp[i] = new Position(pextension[j]);
			
			path = new Position[path_tmp.length];
			for(i = 0; i< path_tmp.length; i++)
				path[i] = new Position(path_tmp[i]);
		}
	}
}
