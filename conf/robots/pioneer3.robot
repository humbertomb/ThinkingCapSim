#---------------------------------------------------------------------
# Mar 2004		Humberto Martinez Barbera
#
# Piooner3 AT robot 
#

#---------------------------------------------------------------------
# Sonar sensors definition
#---------------------------------------------------------------------
MAXSONAR	= 16
RANGESON	= 10.0
MINIMSON	= 0.135
CONESON		= 20
SENSIBSON	= 0.0000001
RAYSON		= 11
ERRORSON	= 0.05
MODESON		= 2
CYCLESON	= 4

sonfeat0	= 90
sonlen0		= 0.19474
sonrho0		= 41.88
sonhgt0		= 0.25
sonstep0	= 1
sonfeat1	= 50
sonlen1		= 0.21783
sonrho1		= 31.86
sonhgt1		= 0.25
sonstep1	= 2
sonfeat2	= 30
sonlen2		= 0.23409
sonrho2		= 19.98
sonhgt2		= 0.25
sonstep2	= 3
sonfeat3	= 10
sonlen3		= 0.24130
sonrho3		= 5.95
sonhgt3		= 0.25
sonstep3	= 4

sonfeat4	= -10
sonlen4		= 0.24130
sonrho4		= -5.95
sonhgt4		= 0.25
sonstep4	= 1
sonfeat5	= -30
sonlen5		= 0.23409
sonrho5		= -19.98
sonhgt5		= 0.25
sonstep5	= 2
sonfeat6	= -50
sonlen6		= 0.21783
sonrho6		= -31.86
sonhgt6		= 0.25
sonstep6	= 3
sonfeat7	= -90
sonlen7		= 0.19474
sonrho7		= -41.88
sonhgt7		= 0.25
sonstep7	= 4

sonfeat8	= -90
sonlen8		= 0.19474
sonrho8		= -138.12
sonhgt8		= 0.25
sonstep8	= 1
sonfeat9	= -130
sonlen9		= 0.21783
sonrho9		= -148.14
sonhgt9		= 0.25
sonstep9	= 2
sonfeat10	= -150
sonlen10	= 0.23409
sonrho10	= -160.02
sonhgt10	= 0.25
sonstep10	= 3
sonfeat11	= -170
sonlen11	= 0.24130
sonrho11	= -174.05
sonhgt11	= 0.25
sonstep11	= 4

sonfeat12	= 170
sonlen12	= 0.24130
sonrho12	= 174.05
sonhgt12	= 0.25
sonstep12	= 1
sonfeat13	= 150
sonlen13	= 0.23409
sonrho13	= 160.02
sonhgt13	= 0.25
sonstep13	= 2
sonfeat14	= 130
sonlen14	= 0.21783
sonrho14	= 148.14
sonhgt14	= 0.25
sonstep14	= 3
sonfeat15	= 90
sonlen15	= 0.19474
sonrho15	= 138.12
sonhgt15	= 0.25
sonstep15	= 4

#---------------------------------------------------------------------
# Virtual sensors definition
#---------------------------------------------------------------------
MAXVIRTU	= 16
MODEVIRTU	= 0
RANGEVIRTU	= 5.0
CONEVIRTU	= 17
RAYVIRTU	= 15

virtufeat0	= 90
virtulen0	= 0.19474
virturho0	= 41.88
virtufeat1	= 50
virtulen1	= 0.21783
virturho1	= 31.86
virtufeat2	= 30
virtulen2	= 0.23409
virturho2	= 19.98
virtufeat3	= 10
virtulen3	= 0.24130
virturho3	= 5.95

virtufeat4	= -10
virtulen4	= 0.24130
virturho4	= -5.95
virtufeat5	= -30
virtulen5	= 0.23409
virturho5	= -19.98
virtufeat6	= -50
virtulen6	= 0.21783
virturho6	= -31.86
virtufeat7	= -90
virtulen7	= 0.19474
virturho7	= -41.88

virtufeat8	= -90
virtulen8	= 0.19474
virturho8	= -138.12
virtufeat9	= -130
virtulen9	= 0.21783
virturho9	= -148.14
virtufeat10	= -150
virtulen10	= 0.23409
virturho10	= -160.02
virtufeat11	= -170
virtulen11	= 0.24130
virturho11	= -174.05

virtufeat12	= 170
virtulen12	= 0.24130
virturho12	= 174.05
virtufeat13	= 150
virtulen13	= 0.23409
virturho13	= 160.02
virtufeat14	= 130
virtulen14	= 0.21783
virturho14	= 148.14
virtufeat15	= 90
virtulen15	= 0.19474
virturho15	= 138.12

#---------------------------------------------------------------------
# Laser Range Finder sensors definition
#---------------------------------------------------------------------
MAXLRF 			= 1
LRF0			= devices.drivers.laser.LMS200.LMS200|/dev/tty.usbserial
RANGELRF 		= 82.0
MINIMLRF 		= 0.01
CONELRF 		= 180
RAYLRF 			= 361
CYCLELRF		= 6

ERRORLRF 		= 0.05
ERRORLRFGAUSS 	= 0.0009
MODELRF 		= 2

lrffeat0 		= 0.0
lrflen0 		= 0.200
lrfrho0 		= 0.0
lrfhgt0			= 0.35
lrfstep0		= 4

#---------------------------------------------------------------------
# Virtual scanner definition
#---------------------------------------------------------------------
RAYSCAN 	= 90
RANGESCAN 	= 10
CONESCAN 	= 180

scanmode	= 0
scanfeat 	= 0.0
scanlen 	= 0.200
scanrho 	= 0.0

#---------------------------------------------------------------------
# Group sensors definition
#---------------------------------------------------------------------
MAXGROUP	= 5
CONEGROUP	= 50
RANGEGROUP	= 0.75

groupmode0	= 4
groupfeat0	= 90.0
grouplen0	= 0.10
grouprho0	= 120.0

groupmode1	= 4
groupfeat1	= 45.0
grouplen1	= 0.21
grouprho1	= 45.0

groupmode2	= 4
groupfeat2	= 0.0
grouplen2	= 0.21
grouprho2	= 0.0

groupmode3	= 4
groupfeat3	= -45.0
grouplen3	= 0.21
grouprho3	= -45.0

groupmode4	= 4
groupfeat4	= -90.0
grouplen4	= 0.10
grouprho4	= -120.0

#---------------------------------------------------------------------
# Digital inputs definition
#---------------------------------------------------------------------
MAXDSIG		= 1

dsigfeat0	= 0.0
dsiglen0	= 0.25
dsigrho0	= 0.0

#---------------------------------------------------------------------
# Bumper sensors definition
#---------------------------------------------------------------------
MAXBUMPER	= 4

bumxi0		= 0.21
bumyi0		= 0.21
bumxf0		= 0.21
bumyf0		= -0.21
bumxi1		= 0.21
bumyi1		= -0.21
bumxf1		= -0.21
bumyf1		= -0.21
bumxi2		= -0.21
bumyi2		= -0.21
bumxf2		= -0.21
bumyf2		= 0.21
bumxi3		= -0.21
bumyi3		= 0.21
bumxf3		= 0.21
bumyf3		= 0.21

#---------------------------------------------------------------------
# Positioning devices definition
#---------------------------------------------------------------------
MAXGPS		= 0
GPS0		= devices.drivers.gps.Garmin.Garmin|/dev/tty.usbserial2

#---------------------------------------------------------------------
# Robot kinematics definition
#---------------------------------------------------------------------
# Target robot speed (m/s, deg/s)
VMAX		= 1.2
RMAX		= 300.0

# Maximum speed of the driving wheels (m/s)
#MAXMOTOR 	= 1.2
MAXMOTOR 	= 1.2

# Robot control cycle (ms)
DTIME 		= 175

# Robot plant model
DRIVEMODEL	= tc.vrobot.models.DifferentialDrive
BASE		= 0.400
GEAR		= 71.0
WHEEL		= 0.1487
PULSES		= 2000

# Odometry errors for simulation [m/s, rad/s]
#ODOM_ET 	= 0.0005
#ODOM_ER 	= 0.0001
#ODOM_BIAS	= 0.0001
ODOM_ET 	= 0.0
ODOM_ER 	= 0.0
ODOM_BIAS	= 0.0

#---------------------------------------------------------------------
# Robot AROS serial port configuration
#---------------------------------------------------------------------
SERPORT		= /dev/tty.usbserial
SERBRATE	= 9600

#---------------------------------------------------------------------
# Robot icon specification
#---------------------------------------------------------------------
RADIUS		= 0.25
LINES		= 31

# Robot contour
iconxi0 	= 0.255
iconyi0 	= -0.07
iconxf0 	= 0.255
iconyf0 	= 0.070
iconxi1 	= 0.255
iconyi1 	= 0.07
iconxf1 	= 0.180
iconyf1 	= 0.190
iconxi2 	= 0.180
iconyi2 	= 0.190
iconxf2 	= 0.0
iconyf2 	= 0.190
iconxi3 	= 0.0
iconyi3 	= 0.190
iconxf3 	= 0.0
iconyf3 	= 0.160
iconxi4 	= 0.0
iconyi4 	= 0.160
iconxf4 	= -0.180
iconyf4 	= 0.160
iconxi5 	= -0.180
iconyi5 	= 0.160
iconxf5 	= -0.255
iconyf5 	= 0.07
iconxi6 	= -0.255
iconyi6 	= 0.07
iconxf6 	= -0.255
iconyf6 	= -0.07
iconxi7 	= -0.255
iconyi7 	= -0.07
iconxf7 	= -0.180
iconyf7 	= -0.160
iconxi8 	= -0.180
iconyi8 	= -0.160
iconxf8 	= 0.0
iconyf8 	= -0.160
iconxi9 	= 0.0
iconyi9 	= -0.160
iconxf9 	= 0.0
iconyf9 	= -0.190
iconxi10 	= 0.0
iconyi10 	= -0.190
iconxf10 	= 0.180
iconyf10 	= -0.190
iconxi11 	= 0.180
iconyi11 	= -0.190
iconxf11 	= 0.255
iconyf11 	= -0.07

# Guidance triangle
iconxi12 	= 0.23
iconyi12 	= 0.00
iconxf12 	= 0.13
iconyf12 	= -0.10
iconxi13 	= 0.23
iconyi13 	= 0.00
iconxf13 	= 0.13
iconyf13 	= 0.10
iconxi14 	= 0.13
iconyi14 	= -0.10
iconxf14 	= 0.13
iconyf14 	= 0.10

# Front-Left wheel
iconxi15 	= 0.210
iconyi15 	= 0.150
iconxf15 	= 0.230
iconyf15 	= 0.150
iconxi16 	= 0.230
iconyi16 	= 0.150
iconxf16 	= 0.230
iconyf16 	= 0.250
iconxi17 	= 0.230
iconyi17 	= 0.250
iconxf17 	= 0.025
iconyf17 	= 0.250
iconxi18 	= 0.025
iconyi18 	= 0.250
iconxf18 	= 0.025
iconyf18 	= 0.190

# Front-Right wheel
iconxi19 	= 0.210
iconyi19 	= -0.150
iconxf19 	= 0.230
iconyf19 	= -0.150
iconxi20 	= 0.230
iconyi20 	= -0.150
iconxf20 	= 0.230
iconyf20 	= -0.250
iconxi21 	= 0.230
iconyi21 	= -0.250
iconxf21 	= 0.025
iconyf21 	= -0.250
iconxi22 	= 0.025
iconyi22 	= -0.250
iconxf22 	= 0.025
iconyf22 	= -0.190

# Rear-Left wheel
iconxi23 	= -0.200
iconyi23 	= 0.150
iconxf23 	= -0.230
iconyf23 	= 0.150
iconxi24 	= -0.230
iconyi24 	= 0.150
iconxf24 	= -0.230
iconyf24 	= 0.250
iconxi25 	= -0.230
iconyi25 	= 0.250
iconxf25 	= -0.025
iconyf25 	= 0.250
iconxi26 	= -0.025
iconyi26 	= 0.250
iconxf26 	= -0.025
iconyf26 	= 0.160

# Rear-Right wheel
iconxi27 	= -0.200
iconyi27 	= -0.150
iconxf27 	= -0.230
iconyf27 	= -0.150
iconxi28 	= -0.230
iconyi28 	= -0.150
iconxf28 	= -0.230
iconyf28 	= -0.250
iconxi29 	= -0.230
iconyi29 	= -0.250
iconxf29 	= -0.025
iconyf29 	= -0.250
iconxi30 	= -0.025
iconyi30 	= -0.250
iconxf30 	= -0.025
iconyf30 	= -0.160

#-----------------------------------------------------------------------------------------
# Robot 3D specification
#-----------------------------------------------------------------------------------------
V3DFILE		= conf/3dmodels/pioneer3.3ds
V3DCOLORR	= 0.6
V3DCOLORG	= 0.6
V3DCOLORB	= 0.0

