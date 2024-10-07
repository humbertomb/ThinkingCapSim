#---------------------------------------------------------------------
# Jan 2003		
#
# Rasmus robot 
#

#---------------------------------------------------------------------
# Camera operation and settings
#---------------------------------------------------------------------
MAXCAMERA		= 1
CAMERA0			= mimics.devices.camera.CCD400E.CCD400E|/dev/ttyS3
CAMERA0_SERVER	= 50000

#---------------------------------------------------------------------
# Group sensors definition
#---------------------------------------------------------------------
MAXGROUP		= 5
CONEGROUP		= 50
RANGEGROUP		= 0.75

groupmode0		= 4
groupfeat0		= 90.0
grouplen0		= 0.396610
grouprho0		= 56.30993

groupmode1		= 4
groupfeat1		= 45.0
grouplen1		= 0.615873
grouprho1		= 32.39984

groupmode2		= 4
grouplen2		= 0.520
grouprho2		= 0.0

groupmode3		= 4
groupfeat3		= -45.0
grouplen3		= 0.615873
grouprho3		= -32.39984

groupmode4		= 4
groupfeat4		= -90.0
grouplen4		= 0.396610
grouprho4		= -56.30993

#---------------------------------------------------------------------
# Laser Range Finder sensors definition
#---------------------------------------------------------------------
MAXLRF 			= 1
#LRF0			= mimics.devices.laser.LMS200.LMS200|/dev/ttyS0
RANGELRF 		= 82
MINIMLRF 		= 0.01
CONELRF 		= 180
RAYLRF 			= 181
CYCLELRF		= 6

ERRORLRF 		= 0.05
ERRORLRFGAUSS 	= 0.0009
MODELRF 		= 2

lrffeat0 		= 0.0
lrflen0 		= 0.35
lrfrho0 		= 0.0
lrfstep0		= 4

#---------------------------------------------------------------------
# Virtual scanner definition
#---------------------------------------------------------------------
RAYSCAN 		= 90
RANGESCAN 		= 30
CONESCAN 		= 180

scanmode		= 0
scanfeat 		= 0.0
scanlen 		= 0.35
scanrho 		= 0.0

#---------------------------------------------------------------------
# Sonar sensors definition
#---------------------------------------------------------------------
MAXSONAR	= 17
RANGESON	= 10.0
MINIMSON	= 0.135
CONESON		= 20
SENSIBSON	= 0.0000001
RAYSON		= 11
ERRORSON	= 0.05
MODESON		= 2
CYCLESON	= 1

sonfeat0	= 180
sonlen0	= 0.34438
sonrho0	= 154.17901
sonstep0	= 1
sonfeat1	= 90
sonlen1	= 0.33144
sonrho1	= 146.07020
sonstep1	= 1
sonfeat2	= 90
sonlen2	= 0.19780
sonrho2	= 69.274444
sonstep2	= 1
sonfeat3	= 75
sonlen3	= 0.21468
sonrho3	= 56.97613
sonstep3	= 1
sonfeat4	= 60
sonlen4	= 0.24905
sonrho4	= 46.95254
sonstep4	= 1
sonfeat5	= 45
sonlen5	= 0.28901
sonrho5	= 37.26640
sonstep5	= 1
sonfeat6	= 30
sonlen6	= 0.31885
sonrho6	= 19.79888
sonstep6	= 1
sonfeat7	= 15
sonlen7	= 0.30992
sonrho7	= 10.22217
sonstep7	= 1
sonfeat8	= 0
sonlen8	= 0.31300
sonrho8	= 0
sonstep8	= 1
sonfeat9	= -15
sonlen9	= 0.30922
sonrho9	= -10.22217
sonstep9	= 1
sonfeat10	= -30
sonlen10	= 0.31885
sonrho10	= -19.79888
sonstep10	= 1
sonfeat11	= -180
sonlen11	= 0.34438
sonrho11	= -154.17901
sonstep11	= 1
sonfeat12	= -90
sonlen12	= 0.33144
sonrho12	= -146.07020
sonstep12	= 1
sonfeat13	= -90
sonlen13	= 0.19780
sonrho13	= -69.27444
sonstep13	= 1
sonfeat14	= -75
sonlen14	= 0.21468
sonrho14	= -56.97613
sonstep14	= 1
sonfeat15	= -60
sonlen15	= 0.24905
sonrho15	= -46.95254
sonstep15	= 1
sonfeat16	= -45
sonlen16	= 0.28901
sonrho16	= -37.26640
sonstep16	= 1
	
#---------------------------------------------------------------------
# Virtual sensors definition
#---------------------------------------------------------------------
MAXVIRTU	= 17
MODEVIRTU	= 0
RANGEVIRTU	= 10.0
CONEVIRTU	= 20

virtufeat0	= 180
virtulen0	= 0.34438
virturho0	= 154.17901
virtufeat1	= 90
virtulen1	= 0.33144
virturho1	= 146.07020
virtufeat2	= 90
virtulen2	= 0.19780
virturho2	= 69.274444
virtufeat3	= 75
virtulen3	= 0.21468
virturho3	= 56.97613
virtufeat4	= 60
virtulen4	= 0.24905
virturho4	= 46.95254
virtufeat5	= 45
virtulen5	= 0.28901
virturho5	= 37.26640
virtufeat6	= 30
virtulen6	= 0.31885
virturho6	= 19.79888
virtufeat7	= 15
virtulen7	= 0.30992
virturho7	= 10.22217
virtufeat8	= 0
virtulen8	= 0.31300
virturho8	= 0
virtufeat9	= -15
virtulen9	= 0.30922
virturho9	= -10.22217
virtufeat10	= -30
virtulen10	= 0.31885
virturho10	= -19.79888
virtufeat11	= -180
virtulen11	= 0.34438
virturho11	= -154.17901
virtufeat12	= -90
virtulen12	= 0.33144
virturho12	= -146.07020
virtufeat13	= -90
virtulen13	= 0.19780
virturho13	= -69.27444
virtufeat14	= -75
virtulen14	= 0.21468
virturho14	= -56.97613
virtufeat15	= -60
virtulen15	= 0.24905
virturho15	= -46.95254
virtufeat16	= -45
virtulen16	= 0.28901
virturho16	= -37.26640

#---------------------------------------------------------------------
# Control and sensors cycles timing
#---------------------------------------------------------------------
# Robot control cycle (ms)
DTIME		= 300

#---------------------------------------------------------------------
# Robot kinematics definition
#---------------------------------------------------------------------
# Target robot speed (m/s, deg/s)
VMAX		= 1.0
RMAX		= 60.0

# Maximum speed of the driving wheels (m/s)
MAXMOTOR 	= 1.0

# Robot plant model
DRIVEMODEL	= tc.vrobot.models.DifferentialDrive
BASE		= 0.60

# Odometry errors for simulation [m/s, rad/s]
ODOM_ET 	= 0.0005
ODOM_ER 	= 0.0001
ODOM_BIAS	= 0.0001

#---------------------------------------------------------------------
# Robot icon specification
#---------------------------------------------------------------------
RADIUS		= 0.55
LINES		= 29

# Främre linje av Robot
iconxi0 	= 0.51
iconyi0 	= -0.1925 
iconxf0 	= 0.51
iconyf0 	= 0.1925

# Andra linjen framifrån  
iconxi1 	= 0.33 
iconyi1 	= -0.1925
iconxf1 	= 0.33
iconyf1 	= 0.1925

# Tredje linjen framifrån av Roboten
iconxi2	= 0.23 
iconyi2 	= -0.15
iconxf2 	= 0.23
iconyf2 	= 0.15

# Andra linjen bakifrån
iconxi3 	= -0.33 
iconyi3	= -0.1925
iconxf3 	= -0.33
iconyf3 	= 0.1925

# Bakre linje av robot
iconxi4 	= -0.39
iconyi4	= -0.1925
iconxf4	= -0.39
iconyf4 	= 0.1925

# Vänstra linje i Robot
iconxi5 	= -0.39
iconyi5 	= 0.1925
iconxf5 	= 0.51
iconyf5 	= 0.1925

# Andra linjen från vänster
iconxi6 	= -0.33 
iconyi6 	= 0.15
iconxf6 	= 0.23
iconyf6 	= 0.15

# Högra linje i Robot
iconxi7 	= -0.39
iconyi7 	= -0.1925
iconxf7 	= 0.51
iconyf7 	= -0.1925

# Andra linjen från höger
iconxi8 	= -0.33 
iconyi8 	= -0.15
iconxf8 	= 0.23
iconyf8 	= -0.15

# Höger laser linje 
iconxi9 	= 0.33
iconyi9 	= -0.08
iconxf9 	= 0.51
iconyf9 	= -0.08

# Vänster laser linje
iconxi10 	= 0.33
iconyi10 	= 0.08
iconxf10 	= 0.51
iconyf10 	= 0.08

# Högra sneda linjen
iconxi11 	= 0.33 
iconyi11 	= -0.1925
iconxf11 	= 0.23
iconyf11 	= -0.15

# Vänstra sneda linjen
iconxi12 	= 0.33 
iconyi12 	= 0.1925
iconxf12 	= 0.23
iconyf12 	= 0.15

# Höger bakhjul Yttre linje
iconxi13 	= -0.35
iconyi13 	= -0.32
iconxf13 	= -0.03
iconyf13 	= -0.32

# Höger bakhjul Innre linje
iconxi14 	= -0.35
iconyi14 	= -0.22
iconxf14 	= -0.03
iconyf14 	= -0.22

# Höger bakhjul främre linje
iconxi15 	= -0.03 
iconyi15 	= -0.32
iconxf15 	= -0.03
iconyf15 	= -0.22

# Höger bakhjul bakre linje
iconxi16 	= -0.35 
iconyi16 	= -0.32
iconxf16 	= -0.35
iconyf16 	= -0.22

# Vänster bakhjul Yttre linje
iconxi17 	= -0.35 
iconyi17 	= 0.32
iconxf17 	= -0.03
iconyf17 	= 0.32

# Vänster bakhjul Innre linje
iconxi18 	= -0.35 
iconyi18 	= 0.22
iconxf18 	= -0.03
iconyf18 	= 0.22


# Vänster bakhjul bakre linje
iconxi19 	= -0.35 
iconyi19 	= 0.32
iconxf19 	= -0.35
iconyf19 	= 0.22


# Vänster bakhjul främre linje
iconxi20 	= -0.03
iconyi20 	= 0.32
iconxf20 	= -0.03
iconyf20 	= 0.22


# Höger framhjul Yttre linje
iconxi21 	= 0.03 
iconyi21 	= -0.32
iconxf21 	= 0.35
iconyf21 	= -0.32

# Höger framhjul Innre linje
iconxi22 	= 0.03 
iconyi22 	= -0.22
iconxf22 	= 0.35
iconyf22 	= -0.22

# Höger framhjul bakre linje
iconxi23 	= 0.03 
iconyi23 	= -0.32
iconxf23 	= 0.03
iconyf23 	= -0.22

# Höger framhjul främre linje 
iconxi24 	= 0.35 
iconyi24 	= -0.32
iconxf24 	= 0.35
iconyf24 	= -0.22

# Vänster framhjul Yttre linje
iconxi25 	= 0.03 
iconyi25 	= 0.32
iconxf25 	= 0.35
iconyf25 	= 0.32

# Vänster framhjul Innre linje
iconxi26 	= 0.03 
iconyi26 	= 0.22
iconxf26 	= 0.35
iconyf26 	= 0.22

# Vänster framhjul främre linje
iconxi27 	= 0.35
iconyi27 	= 0.32
iconxf27 	= 0.35
iconyf27 	= 0.22

# Vänster framhjul bakre linje
iconxi28 	= 0.03
iconyi28 	= 0.32
iconxf28 	= 0.03
iconyf28 	= 0.22








 




















