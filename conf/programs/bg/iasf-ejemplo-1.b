/* -------------------- */
/* F U Z Z Y    S E T S */
/* -------------------- */

// Sets for ToF sensor range [0 .. 10] (meters)
set CLOSE	= trapezoid {0.0, 0.0, 0.2, 0.3};			// Close 
set NEAR	= trapezoid {0.2, 0.3, 0.4, 0.5};			// Near 
set MED		= trapezoid {0.4, 0.5, 0.6, 0.7};			// Medium
set FAR		= trapezoid {0.6, 0.7, 1.3, 1.4};			// Far
set VFAR	= trapezoid {1.3, 1.4, 10.0, 10.0};			// Very Far

// Sets for vrot, the turn rate (deg/s)
set TTR		= trapezoid {-150.0, -150.0, -100.0, -90.0};	// Tight Right
set TR		= trapezoid {-100.0, -90.0, -60.0, -50.0};		// Right
set TSR		= trapezoid {-60.0, -50.0, -20.0, -10.0};		// Small Right
set TC		= trapezoid {-20.0, -10.0, 10.0, 20.0};			// Center
set TSL		= trapezoid {10.0, 20.0, 50.0, 60.0};			// Small Left
set TL		= trapezoid {50.0, 60.0, 90.0, 100.0};			// Left
set TTL		= trapezoid {90.0, 100.0, 150.0, 150.0};		// Tight Left

// Sets for vlin, the forward velocity (m/s)
set SFULL	= trapezoid {0.25, 0.3, 0.5, 0.5};				// Full speed
set SMEDIUM	= trapezoid {0.15, 0.2, 0.25, 0.3};				// Medium speed
set SLOW	= trapezoid {0.0, 0.0, 0.15, 0.2};				// Stop

// Sets for blending [0 ..1 ] 
set LOW		= trapezoid {0.0, 0.0, 0.1, 0.2};			// Low
set HALFL	= trapezoid {0.1, 0.2, 0.4, 0.5};			// Medium
set HALFH	= trapezoid {0.4, 0.5, 0.7, 0.8};			// Medium
set HIGH	= trapezoid {0.7, 0.8, 1.0, 1.0};			// High

// Sets for goal differences [-PI .. PI] (radians)
set GN		= trapezoid {-3.5, -3.5, -0.87, -0.52};		// Negative
set GSN		= trapezoid {-0.87, -0.52, -0.087, 0.0};	// Small Negative
set GZ		= trapezoid {-0.087, -0.02, 0.02, 0.087};	// Zero
set GSP		= trapezoid {0.0, 0.087, 0.52, 0.87};		// Small Positive
set GP		= trapezoid {0.52, 0.87, 3.5, 3.5};			// Positive

/* ----------------- */
/* C O N S T A N T S */
/* ----------------- */


/* ----------------- */
/* V A R I A B L E S */
/* ----------------- */

// External Blackboard Variables
sensor float		group0, group1, group2, group3, group4;
sensor float 		bumper0, bumper1, bumper2, bumper3;
sensor float 		alpha, heading;
effector float 		vlin, vlat, vrot;		// the control action: m/s, m/s and deg/s

// State and Control Variables
float 				collision = 0.0;
float 				left, leftd, front, rightd, right; 	// Group sensors

/* ----------------- */
/* F U N C T I O N S */
/* ----------------- */

function min (a, b)
{
	if (a < b) return a;
	return b;
}

function max (a, b)
{
	if (a > b) return a;
	return b;
}

function limits (x, a, b)
{
	if (x < a) return a;
	if (x > b) return b;
	return x;
}

function sgn (a)
{
	if (a < 0.0) return -1.0;
	if (a > 0.0) return 1.0;
	return 0.0;
}


/* --------------------------- */
/* I N I T I A L I Z A T I O N */
/* --------------------------- */

initialization
{
	vlin = 0.0;
	vlat = 0.0;
	vrot = 0.0;
}


/* ----------- */
/* A G E N T S */
/* ----------- */

agent ReactiveControl
{
	blending	left range (0.0, 2.0), leftd range (0.0, 1.0), right range (0.0, 2.0), rightd range (0.0, 1.0), front range (0.0, 2.0), collision range (0.0, 1.0);

	common
	{
		left	= group0;
		leftd	= group1;
		front	= group2;
		rightd	= group3;
		right	= group4;
		
		collision = bumper0 + bumper1 + bumper2 + bumper3;
		if (collision > 1.0) collision = 1.0;
	}

	behaviour wander priority 1.0
	{
		fusion	vlin, vrot;

		rules
		{
			background (1.0)								vlin is SFULL;
			background (1.0)								vrot is TC;
		}
	}

	behaviour avoidL priority 1.0
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
		
		vrot = vrot * 1.5;
	}

	behaviour avoidR priority 1.0
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
		
		vrot = vrot * 1.5;
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

	blender
	{
		// OJO: no se pueden usar ORs en las reglas del blender, y SOLO conjuntos trapezoidales
		rules
		{
 			background (0.01)					wander is HIGH;
			background (0.01)					avoidR is LOW;
			background (0.01)					avoidL is LOW;
			background (0.01)					avoidF is LOW;

			// Rules for obstacle avoidance
			if (leftd is CLOSE)					avoidL is HIGH,		wander is LOW;
			if (front is CLOSE)					avoidF is HIGH,		wander is LOW;
			if (rightd is CLOSE)				avoidR is HIGH,		wander is LOW;

			if (leftd is NEAR) 					avoidL is HALFH,	wander is HALFL;
			if (front is NEAR)					avoidF is HALFH,	wander is HALFL;
			if (rightd is NEAR) 				avoidR is HALFH,	wander is HALFL;

			if (leftd is MED) 					avoidL is HALFL,	wander is HALFH;
			if (front is MED)					avoidF is HALFL,	wander is HALFH;
			if (rightd is MED) 					avoidR is HALFL,	wander is HALFH;
		}
	}
}
