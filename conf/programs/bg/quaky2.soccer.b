/* -------------------- */
/* F U Z Z Y    S E T S */
/* -------------------- */

// Sets for ToF sensor range (m)
set CLOSE	= trapezoid {0.0, 0.0, 0.1, 0.15};				// Close 
set NEAR	= trapezoid {0.1, 0.15, 0.2, 0.25};				// Near 
set MED		= trapezoid {0.2, 0.25, 0.3, 0.4};				// Medium
set FAR		= trapezoid {0.3, 0.4, 1.3, 1.4};				// Far
set VFAR	= trapezoid {1.3, 1.4, 10.0, 10.0};				// Very Far

// Sets for vrot, the turn rate (deg/s)
set TTR		= trapezoid {-180.0, -180.0, -120.0, -110.0};	// Tight Right
set TR		= trapezoid {-120.0, -110.0, -60.0, -50.0};		// Right
set TSR		= trapezoid {-60.0, -50.0, -12.0, -7.0};		// Small Right
set TC		= trapezoid {-12.0, -7.0, 7.0, 12.0};			// Center
set TSL		= trapezoid {7.0, 12.0, 50.0, 60.0};			// Small Left
set TL		= trapezoid {50.0, 60.0, 110.0, 120.0};			// Left
set TTL		= trapezoid {110.0, 120.0, 180.0, 180.0};		// Tight Left

// Sets for vlin, the forward velocity (m/s)
set SFULL	= crisp {0.25};									// Full speed
set SMEDIUM	= crisp {0.10};									// Medium speed
set SLOW	= crisp {0.05};									// Low speed
set SNULL	= crisp {0.00};									// Halted

// Sets for heading-to-goal error (deg)
set GN		= trapezoid {-180.0, -180.0, -22.0, -18.0};		// Negative
set GMN		= trapezoid {-22.0, -18.0, -12.0, -8.0};		// Medium Negative
set GSN		= trapezoid {-12.0, -8.0, -5.0, -2.0};			// Small Negative
set GZ		= trapezoid {-5.0, -2.0, 2.0, 5.0};				// Zero
set GSP		= trapezoid {2.0, 5.0, 8.0, 12.0};				// Small Positive
set GMP		= trapezoid {8.0, 12.0, 18.0, 22.0};			// Medium Positive
set GP		= trapezoid {18.0, 22.0, 180.0, 180.0};			// Positive

// Sets for blending [0 .. 1] 
set LOW		= trapezoid {0.0, 0.0, 0.1, 0.2};				// Low
set HALFL	= trapezoid {0.1, 0.2, 0.4, 0.5};				// Medium
set HALFH	= trapezoid {0.4, 0.5, 0.7, 0.8};				// Medium
set HIGH	= trapezoid {0.7, 0.8, 1.0, 1.0};				// High

/* ----------------- */
/* V A R I A B L E S */
/* ----------------- */

// External Blackboard Variables
sensor float		group0, group1, group2, group3, group4;
sensor float		ballSeen, netSeen, fieldSeen, ballAligned, ballHold, inNet;
sensor float 		ballPhi, netPhi, pushPhi, lookaPhi;
sensor float 		lballPhi, lnetPhi;
effector float 		vlin, vlat, vrot;		// the control action: m/s, m/s and deg/s

// State and Control Variables
float 				left, leftd, front, rightd, right; 		// Group sensors

/* ----------------- */
/* C O N S T A N T S */
/* ----------------- */

float				TRUE	= 1.0;
float				FALSE	= 0.0;

/* --------------------------- */
/* I N I T I A L I Z A T I O N */
/* --------------------------- */

initialization
{
	vlin	= 0.0;
	vlat	= 0.0;
	vrot	= 0.0;
}


/* ----------- */
/* A G E N T S */
/* ----------- */

agent ReactiveControl
{
	blending	left range (0.0, 2.0), leftd range (0.0, 1.0), right range (0.0, 2.0), rightd range (0.0, 1.0), front range (0.0, 2.0);

	common
	{
		left	= group0;
		leftd	= group1;
		front	= group2;
		rightd	= group3;
		right	= group4;		

/*
		left	= 3.0;
		leftd	= 3.0;
		front	= 3.0;
		rightd	= 3.0;
		right	= 3.0;		
*/		
	}

	behaviour avoidL priority 1.5
	{
		fusion	vlin, vrot;

		rules
		{
			background (0.01)								vlin is SFULL;
			background (0.01)								vrot is TC;

			// Left and Front sensors
			if ((leftd is CLOSE) && (front is CLOSE))		vrot is TTR;
			if ((leftd is CLOSE) && (front is NEAR))		vrot is TTR;
			if ((leftd is CLOSE) && (front is MED))			vrot is TTR;
			if ((leftd is CLOSE) && (front is FAR))			vrot is TTR;
			if ((leftd is CLOSE) && (front is VFAR))		vrot is TTR;

			if ((leftd is NEAR) && (front is CLOSE))		vrot is TR;
			if ((leftd is NEAR) && (front is NEAR))			vrot is TR;
			if ((leftd is NEAR) && (front is MED))			vrot is TR;
			if ((leftd is NEAR) && (front is FAR))			vrot is TSR;
			if ((leftd is NEAR) && (front is VFAR))			vrot is TSR;

			if ((leftd is MED) && (front is CLOSE))			vrot is TR;
			if ((leftd is MED) && (front is NEAR))			vrot is TR;
			if ((leftd is MED) && (front is MED))			vrot is TSR;
			if ((leftd is MED) && (front is FAR))			vrot is TC;
			if ((leftd is MED) && (front is VFAR))			vrot is TC;

			// Speed controller
			if ((leftd is CLOSE) || (front is CLOSE))		vlin is SLOW;
			if ((leftd is NEAR) || (front is NEAR))			vlin is SLOW;
			if ((leftd is MED) || (front is MED))			vlin is SMEDIUM;
		}
	}

	behaviour avoidR priority 1.5
	{
		fusion	vlin, vrot;

		rules
		{
			background (0.01)								vlin is SFULL;
			background (0.01)								vrot is TC;

			// Right and Front sensors
			if ((rightd is CLOSE) && (front is CLOSE))		vrot is TTL;
			if ((rightd is CLOSE) && (front is NEAR))		vrot is TTL;
			if ((rightd is CLOSE) && (front is MED))		vrot is TTL;
			if ((rightd is CLOSE) && (front is FAR))		vrot is TTL;
			if ((rightd is CLOSE) && (front is VFAR))		vrot is TTL;

			if ((rightd is NEAR) && (front is CLOSE))		vrot is TL;
			if ((rightd is NEAR) && (front is NEAR))		vrot is TL;
			if ((rightd is NEAR) && (front is MED))			vrot is TL;
			if ((rightd is NEAR) && (front is FAR))			vrot is TSL;
			if ((rightd is NEAR) && (front is VFAR))		vrot is TSL;

			if ((rightd is MED) && (front is CLOSE))		vrot is TL;
			if ((rightd is MED) && (front is NEAR))			vrot is TL;
			if ((rightd is MED) && (front is MED))			vrot is TSL;
			if ((rightd is MED) && (front is FAR))			vrot is TC;
			if ((rightd is MED) && (front is VFAR))			vrot is TC;

			// Speed controller
			if ((rightd is CLOSE) || (front is CLOSE))		vlin is SLOW;
			if ((rightd is NEAR) || (front is NEAR))		vlin is SLOW;
			if ((rightd is MED) || (front is MED))			vlin is SMEDIUM;
		}
	}

	behaviour avoidF priority 1.0
	{
		fusion	vlin, vrot;

		rules
		{
			background (0.01)													vrot is TC;
			background (0.01)													vlin is SFULL;

			// Right, Left and Front sensors
			if ((rightd is CLOSE) && (front is CLOSE) && !(leftd is CLOSE))		vrot is TTL;
			if ((left is CLOSE) && (front is CLOSE) && !(right is CLOSE))		vrot is TTR;
			
			if ((rightd is NEAR) && (front is CLOSE) && !(leftd is NEAR))		vrot is TTL;
			if ((left is NEAR) && (front is CLOSE) && !(right is NEAR))			vrot is TTR;
			
			if ((rightd is CLOSE) && (front is NEAR) && !(leftd is CLOSE))		vrot is TL;
			if ((left is CLOSE) && (front is NEAR) && !(right is CLOSE))		vrot is TR;
			
			if ((rightd is NEAR) && (front is NEAR) && !(leftd is NEAR))		vrot is TL;
			if ((left is NEAR) && (front is NEAR) && !(right is NEAR))			vrot is TR;
			
			if ((front is CLOSE) || (front is NEAR))							vlin is SLOW;
		}
	}

	behaviour goToBall priority 0.3
	{
		fusion		vlin, vrot;
			
		rules
		{
			if (ballPhi is GP)		vrot is TTL, vlin is SNULL;
			if (ballPhi is GMP)		vrot is TL, vlin is SLOW;
			if (ballPhi is GSP)		vrot is TSL, vlin is SMEDIUM;
			if (ballPhi is GZ)		vrot is TC, vlin is SFULL;
			if (ballPhi is GSN)		vrot is TSR, vlin is SMEDIUM;
			if (ballPhi is GMN)		vrot is TR, vlin is SLOW;
			if (ballPhi is GN)		vrot is TTR, vlin is SNULL;
		}
	}
	
	// pushes the ball to the net: it aims beyond the ball, along the line to the
	// net, so that it drives through the ball instead of at it
	behaviour goToNet priority 0.3
	{
		fusion		vlin, vrot;
			
		rules
		{
			if (pushPhi is GP)		vrot is TTL, vlin is SNULL;
			if (pushPhi is GMP)		vrot is TL, vlin is SLOW;
			if (pushPhi is GSP)		vrot is TSL, vlin is SMEDIUM;
			if (pushPhi is GZ)		vrot is TC, vlin is SFULL;
			if (pushPhi is GSN)		vrot is TSR, vlin is SMEDIUM;
			if (pushPhi is GMN)		vrot is TR, vlin is SLOW;
			if (pushPhi is GN)		vrot is TTR, vlin is SNULL;
		}
	}
	
	behaviour goToPos priority 0.3
	{
		fusion		vlin, vrot;
			
		rules
		{
			if (lookaPhi is GP)		vrot is TTL, vlin is SNULL;
			if (lookaPhi is GMP)	vrot is TL, vlin is SLOW;
			if (lookaPhi is GSP)	vrot is TSL, vlin is SMEDIUM;
			if (lookaPhi is GZ)		vrot is TC, vlin is SFULL;
			if (lookaPhi is GSN)	vrot is TSR, vlin is SMEDIUM;
			if (lookaPhi is GMN)	vrot is TR, vlin is SLOW;
			if (lookaPhi is GN)		vrot is TTR, vlin is SNULL;
		}
	}
	
	behaviour searchBall
	{
		fusion		vlin, vrot;
		
		vrot = 50.0;
		if (lballPhi < 0.0)
			vrot = -50.0;
			
		vlin	= 0.0;
	}

	// turns on the spot, to look at the whole field
	behaviour lookAround
	{
		fusion		vlin, vrot;
		
		vrot	= 60.0;
		vlin	= 0.0;
	}

	behaviour searchNet
	{
		fusion		vlin, vrot;
		
		vrot = 50.0;
		if (lnetPhi < 0.0)
			vrot = -50.0;
			
		vlin	= 0.0;
	}

	blender
	{
		searchBall	= 0.0;
		searchNet	= 0.0;
		lookAround	= 0.0;
		avoidR		= 0.0;
		avoidF		= 0.0;
		avoidL		= 0.0;
		goToBall	= 0.0;
		goToNet		= 0.0;
		goToPos		= 0.0;

		fsm start INIT_SEARCH_BALL
		{
			state INIT_SEARCH_BALL:
			{
				if (ballSeen > 0.6)			shift INIT_SEARCH_NET;
				
				searchBall	= 1.0;
			}
			// it turns around until it has seen the ball and both nets, so that it knows
			// which way the field, and so the net it plays on, looks
			state INIT_SEARCH_NET:
			{
				if ((netSeen == TRUE) && (fieldSeen == TRUE))		shift ALIGN;
				
				lookAround	= 1.0;
			}
			state ALIGN:
			{
				if (ballAligned == TRUE)		shift CATCH;
				if (ballSeen < 0.4)				shift ALIGN_SEARCH_BALL;			

				rules
				{
					background (0.01)			avoidR is LOW;
					background (0.01)			avoidL is LOW;
					background (0.01)			avoidF is LOW;
					background (0.01)			goToPos is HIGH;

					if (leftd is CLOSE)			avoidL is HIGH, 	goToPos is LOW;
					if (rightd is CLOSE)		avoidR is HIGH, 	goToPos is LOW;

					if (leftd is NEAR) 			avoidL is HALFH, 	goToPos is HALFL;
					if (rightd is NEAR) 		avoidR is HALFH, 	goToPos is HALFL;

					if (leftd is MED) 			avoidL is HALFL, 	goToPos is HALFH;
					if (rightd is MED) 			avoidR is HALFL,	goToPos is HALFH;
				}
			}
			state ALIGN_SEARCH_BALL:
			{
				if (ballSeen > 0.6)				shift ALIGN;
				
				searchBall	= 1.0;
			}
			state CATCH:
			{
				if (ballHold == TRUE)			shift PUSH;
				if (ballSeen < 0.2)				shift CATCH_SEARCH_BALL;			

				rules
				{
					background (0.01)			avoidR is LOW;
					background (0.01)			avoidL is LOW;
					background (0.01)			avoidF is LOW;
					background (0.01)			goToBall is HIGH;

					if (leftd is CLOSE)			avoidL is HIGH, 	goToBall is LOW;
					if (rightd is CLOSE)		avoidR is HIGH, 	goToBall is LOW;

					if (leftd is NEAR) 			avoidL is HALFH, 	goToBall is HALFL;
					if (rightd is NEAR) 		avoidR is HALFH, 	goToBall is HALFL;

					if (leftd is MED) 			avoidL is HALFL, 	goToBall is HALFH;
					if (rightd is MED) 			avoidR is HALFL,	goToBall is HALFH;
				}
			}
			state CATCH_SEARCH_BALL:
			{
				if (ballSeen > 0.6)				shift CATCH;
				
				searchBall	= 1.0;
			}
			state PUSH:
			{
				if (ballAligned == FALSE)		shift ALIGN;
				if (ballSeen < 0.2)				shift PUSH_SEARCH_BALL;			
				if (netSeen == FALSE)			shift PUSH_SEARCH_NET;			
				if (inNet == TRUE)				shift SCORED;

				rules
				{
					background (0.01)			avoidR is LOW;
					background (0.01)			avoidL is LOW;
					background (0.01)			avoidF is LOW;
					background (0.01)			goToNet is HIGH;

					if (leftd is CLOSE)			avoidL is HIGH, 	goToNet is LOW;
					if (rightd is CLOSE)		avoidR is HIGH, 	goToNet is LOW;

					if (leftd is NEAR) 			avoidL is HALFH, 	goToNet is HALFL;
					if (rightd is NEAR) 		avoidR is HALFH, 	goToNet is HALFL;

					if (leftd is MED) 			avoidL is HALFL, 	goToNet is HALFH;
					if (rightd is MED) 			avoidR is HALFL,	goToNet is HALFH;
				}
			}
			state PUSH_SEARCH_BALL:
			{
				if (ballSeen > 0.6)				shift PUSH;
				
				searchBall	= 1.0;
			}
			state PUSH_SEARCH_NET:
			{
				if (netSeen == TRUE)			shift PUSH;
				
				searchNet	= 1.0;
			}
			state SCORED:
			{
				vlin	= 0.0;
				vrot	= 0.0;
				
//				halt;
			}
		}
	}
}
