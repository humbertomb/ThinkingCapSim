#---------------------------------------------------------------------
#
# i-Fork v1.0b
# OMG 808 AGV robot specification
#

#---------------------------------------------------------------------
# Laser Range Finder sensors definition
#---------------------------------------------------------------------
MAXLRF 			= 1
LRF0				= devices.drivers.laser.PLS.PLS|/dev/ttyS0
RANGELRF 		= 82.0
MINIMLRF 		= 0.01
CONELRF 			= 180
RAYLRF 			= 361
CYCLELRF			= 6

ERRORLRF 		= 0.05
ERRORLRFGAUSS 	= 0.0009
MODELRF 			= 2

lrffeat0 		= 0.0
lrflen0 			= 1.314
lrfrho0 			= 0.0
lrfhgt0			= 0.15
lrfstep0			= 4

#---------------------------------------------------------------------
# Laser Beacon Scanner sensors definition
#---------------------------------------------------------------------
MAXLSB 			= 1
LSB0				= devices.drivers.beacon.nav200.NAV200|/dev/ttyS1
LSB0_OFFSET		= 180.0
RANGELSB 		= 30.0
MINIMLSB 		= 0.01
CONELSB 			= 360.0
REFLSB			= 10.0
BEACLSB			= 20
RAYLSB 			= 361
CYCLELSB			= 6

ERRORLSB 		= 0.05
ERRORLSBGAUSS 	= 0.5
MODELSB 			= 2

lsbfeat0 		= 6
lsblen0 			= 0.623
lsbrho0 			= -5.0
lsbstep0			= 1

MAXLAYER			= 5
LAYER_0			= ZoneA
LAYER_1			= ZoneB
LAYER_2			= ZoneC
LAYER_3			= ZoneD
LAYER_4			= RoomA
INITLAYER		= 0

#---------------------------------------------------------------------
# Virtual scanner definition
#---------------------------------------------------------------------
RAYSCAN 			= 90
RANGESCAN 		= 10
CONESCAN 		= 180

scanmode			= 0
scanfeat 		= 0.0
scanlen 			= 1.135
scanrho 			= 0.0

#---------------------------------------------------------------------
# Group sensors definition
#---------------------------------------------------------------------
MAXGROUP			= 5
RANGEGROUP		= 4.00
CONEGROUP		= 45.0

groupmode0		= 6
groupfeat0		= 90.0
grouplen0		= 1.0
grouprho0		= 0.0
groupbase0		= 0.9
grouprng0		= 2.0

groupmode1		= 4
groupfeat1		= 45.0
grouplen1		= 1.2
grouprho1		= 0.0

groupmode2		= 6
groupfeat2		= 0.0
grouplen2		= 1.8
grouprho2		= 0.0
groupbase2		= 0.75

groupmode3		= 4
groupfeat3		= -45.0
grouplen3		= 1.2
grouprho3		= 0.0

groupmode4		= 6
groupfeat4		= -90.0
grouplen4		= 1.0
grouprho4		= 0.0
groupbase4		= 0.9
grouprng4		= 2.0

#---------------------------------------------------------------------
# Bumper sensors definition
#---------------------------------------------------------------------
MAXBUMPER		= 4

bumxi0			= -0.24
bumyi0			= 0.64
bumxf0 			= -0.24
bumyf0 			= -0.64
bumxi1 			= -0.24
bumyi1 			= -0.64
bumxf1 			= 1.95
bumyf1 			= -0.64
bumxi2 			= 1.95
bumyi2 			= -0.64
bumxf2 			= 1.95
bumyf2 			= 0.64
bumxi3 			= 1.95
bumyi3 			= 0.64
bumxf3 			= -0.24
bumyf3 			= 0.64

#---------------------------------------------------------------------
# Robot kinematics definition
#---------------------------------------------------------------------
# Maximum robot linear and angular velocities (m/s, deg/s)
VMAX 			= 2.5
RMAX 			= 72.0

# Kynematics constraints. Maximum speed and angle of the driving wheel (m/s, deg)
MAXMOTOR 		= 2.5
MAXSTEER 		= 60.0

# Dynamics constraints. Maximum turning speed of the driving wheel (deg/s)
SAMAX			= 42.0

# Acceleration/decceleration constraints (m/s2)
LAMAX			= 0.5
LDMAX			= 0.2

# Robot control cycle (ms)
DTIME 			= 115

# Robot plant model
DRIVEMODEL		= tc.vrobot.models.TricycleDrive
LENGHT 			= 1.004
BASE				= 0.0
RWHEEL			= 0.0

# Odometry errors for simulation [m/s, rad/s]
ODOM_ET 			= 0.002
ODOM_ER 			= 0.005
ODOM_BIAS		= 0.001

#---------------------------------------------------------------------
# Robot CAN bus configuration
#---------------------------------------------------------------------
CAN_DEBUGID		= 111
CAN_SECURITYID	= 200
CAN_BRAKEID		= 300
CAN_ACTIVEID		= 400
CAN_MOTID		= 800
CAN_ODOM_MOTID	= 1000
CAN_ODOM_POSID	= 1200
CAN_FORKID		= 1400
CAN_HORNID		= 1600
CAN_LIGHTSONID	= 1800
CAN_LIGHTSOFFID	= 2000

# CAN parameters (1000, 500, 250 and 125 kbps)
# --------------
CAN_DEV			= /dev/can1
CAN_BRATE		= 500

# Parameters for debugging
# ------------------------
DEBUG			= false
DEBUG_CAN		= false

# Addittional configuration
# -------------------------
CAN_SINGLE_FILTER = false
CAN_OCR 			= 250
CAN_CDR 			= 192

#OCR = 0xFA y CDR = 0xC0

#---------------------------------------------------------------------
# Robot icon specification
#---------------------------------------------------------------------
RADIUS                  = 1.0
LINES                   = 30

# iFork drawing

iconxi0 = -1.1199
iconyi0 = 0.2718
iconxf0 = -1.1199
iconyf0 = 0.2182
 
iconxi1 = -1.1199
iconyi1 = 0.2718
iconxf1 = -0.9207
iconyf1 = 0.295
 
iconxi2 = -0.9207
iconyi2 = 0.195
iconxf2 = -1.1199
iconyf2 = 0.2182
 
iconxi3 = -0.9207
iconyi3 = -0.295
iconxf3 = -1.1199
iconyf3 = -0.2718
 
iconxi4 = -1.1199
iconyi4 = -0.2182
iconxf4 = -0.9207
iconyf4 = -0.195
 
iconxi5 = -1.1199
iconyi5 = -0.2182
iconxf5 = -1.1199
iconyf5 = -0.2718
 
iconxi6 = 1.375
iconyi6 = 0.0
iconxf6 = 1.3643
iconyf6 = -0.0775
 
iconxi7 = 1.3643
iconyi7 = 0.0775
iconxf7 = 1.375
iconyf7 = 0.0
                                                                                
iconxi8 = -0.086
iconyi8 = 0.295
iconxf8 = -0.086
iconyf8 = 0.49
 
iconxi9 = -0.0885
iconyi9 = -0.195
iconxf9 = -0.0885
iconyf9 = 0.195
 
iconxi10 = 0.468
iconyi10 = 0.5
iconxf10 = 1.0932
iconyf10 = 0.5
 
iconxi11 = 1.0932
iconyi11 = 0.5
iconxf11 = 1.219
iconyf11 = 0.3742
 
iconxi12 = 1.219
iconyi12 = 0.3742
iconxf12 = 1.219
iconyf12 = -0.3742
 
iconxi13 = 1.0932
iconyi13 = -0.5
iconxf13 = 0.468
iconyf13 = -0.5
 
iconxi14 = 1.219
iconyi14 = -0.3742
iconxf14 = 1.0932
iconyf14 = -0.5
 
iconxi15 = 0.143
iconyi15 = 0.375
iconxf15 = 0.143
iconyf15 = 0.49
 
iconxi16 = 0.143
iconyi16 = -0.49
iconxf16 = 0.143
iconyf16 = -0.375
 
iconxi17 = -0.086
iconyi17 = 0.49
iconxf17 = 0.143
iconyf17 = 0.49
 
iconxi18 = 0.143
iconyi18 = -0.49
iconxf18 = -0.086
iconyf18 = -0.49
 
iconxi19 = 0.468
iconyi19 = 0.375
iconxf19 = 0.143
iconyf19 = 0.375
 
iconxi20 = 0.468
iconyi20 = 0.375
iconxf20 = 0.468
iconyf20 = 0.5
 
iconxi21 = 0.468
iconyi21 = -0.5
iconxf21 = 0.468
iconyf21 = -0.3752
 
iconxi22 = 0.468
iconyi22 = -0.3752
iconxf22 = 0.143
iconyf22 = -0.375
 
iconxi23 = -0.086
iconyi23 = -0.295
iconxf23 = -0.086
iconyf23 = -0.49
 
iconxi24 = -0.086
iconyi24 = 0.295
iconxf24 = -0.9207
iconyf24 = 0.295
 
iconxi25 = -0.9207
iconyi25 = 0.195
iconxf25 = -0.0885
iconyf25 = 0.195
 
iconxi26 = -0.0885
iconyi26 = -0.195
iconxf26 = -0.9207
iconyf26 = -0.195
 
iconxi27 = -0.9207
iconyi27 = -0.295
iconxf27 = -0.086
iconyf27 = -0.295
 
iconxi28 = 1.3643
iconyi28 = 0.0775
iconxf28 = 1.219
iconyf28 = 0.0775
 
iconxi29 = 1.219
iconyi29 = -0.0775
iconxf29 = 1.3643
iconyf29 = -0.0775

#-----------------------------------------------------------------------------------------
# Robot 3D specification
#-----------------------------------------------------------------------------------------
V3DFILE = conf/3dmodels/ifork.3ds
V3DLIFT = conf/3dmodels/ifork.lift.3ds

