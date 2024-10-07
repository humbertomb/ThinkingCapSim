#---------------------------------------------------------------------
# Apr 2002		Humberto Martinez Barbera
#
# Quaky-II robot 
#

#---------------------------------------------------------------------
# Sonar sensors definition
#---------------------------------------------------------------------
MAXSONAR	= 16
RANGESON	= 5.0
MINIMSON	= 0.135
CONESON		= 20
SENSIBSON	= 0.0000001
RAYSON		= 11
ERRORSON	= 0.05
MODESON		= 2
CYCLESON	= 2

sonfeat0	= 0
sonlen0		= 0.20774
sonrho0		= 13.5010
sonstep0	= 1
sonfeat1	= 22.5
sonlen1		= 0.22697
sonrho1		= 34.9506
sonstep1	= 2
sonfeat2	= 67.5
sonlen2		= 0.22697
sonrho2		= 55.0493
sonstep2	= 1
sonfeat3	= 90
sonlen3		= 0.20774
sonrho3		= 76.4989
sonstep3	= 2
sonfeat4	= 90
sonlen4		= 0.20774
sonrho4		= 103.5010
sonstep4	= 1
sonfeat5	= 112.5
sonlen5		= 0.22697
sonrho5		= 124.9506
sonstep5	= 2
sonfeat6	= 157.5
sonlen6		= 0.22697
sonrho6		= 145.0493
sonstep6	= 1
sonfeat7	= 180
sonlen7		= 0.20774
sonrho7		= 166.4989
sonstep7	= 2
sonfeat8	= -180
sonlen8		= 0.20774
sonrho8		= -166.4989
sonstep8	= 1
sonfeat9	= -157.5
sonlen9		= 0.22697
sonrho9		= -145.0493
sonstep9	= 2
sonfeat10	= -112.5
sonlen10	= 0.22697
sonrho10	= -124.9506
sonstep10	= 1
sonfeat11	= -90
sonlen11	= 0.20774
sonrho11	= -103.5010
sonstep11	= 2
sonfeat12	= -90
sonlen12	= 0.20774
sonrho12	= -76.4989
sonstep12	= 1
sonfeat13	= -67.5
sonlen13	= 0.22697
sonrho13	= -55.0493
sonstep13	= 2
sonfeat14	= -22.5
sonlen14	= 0.22697
sonrho14	= -34.9506
sonstep14	= 1
sonfeat15	= 0
sonlen15	= 0.20774
sonrho15	= -13.5010
sonstep15	= 2

#---------------------------------------------------------------------
# Infrared sensors definition
#---------------------------------------------------------------------
MAXIR		= 16
RANGEIR		= 0.80
MINIMIR		= 0.1
CONEIR		= 10
RAYIR		= 5
ERRORIR		= 0.05
MODEIR		= 2
CYCLEIR		= 2

irfeat0		= 0
irlen0		= 0.20774
irrho0		= 13.5010
irstep0		= 1
irfeat1		= 22.5
irlen1		= 0.22697
irrho1		= 34.9506
irstep1		= 2
irfeat2		= 67.5
irlen2		= 0.22697
irrho2		= 55.0493
irstep2		= 1
irfeat3		= 90
irlen3		= 0.20774
irrho3		= 76.4989
irstep3		= 2
irfeat4		= 90
irlen4		= 0.20774
irrho4		= 103.5010
irstep4		= 1
irfeat5		= 112.5
irlen5		= 0.22697
irrho5		= 124.9506
irstep5		= 2
irfeat6		= 157.5
irlen6		= 0.22697
irrho6		= 145.0493
irstep6		= 1
irfeat7		= 180
irlen7		= 0.20774
irrho7		= 166.4989
irstep7		= 2
irfeat8		= -180
irlen8		= 0.20774
irrho8		= -166.4989
irstep8		= 1
irfeat9		= -157.5
irlen9		= 0.22697
irrho9		= -145.0493
irstep9		= 2
irfeat10	= -112.5
irlen10		= 0.22697
irrho10		= -124.9506
irstep10	= 1
irfeat11	= -90
irlen11		= 0.20774
irrho11		= -103.5010
irstep11	= 2
irfeat12	= -90
irlen12		= 0.20774
irrho12		= -76.4989
irstep12	= 1
irfeat13	= -67.5
irlen13		= 0.22697
irrho13		= -55.0493
irstep13	= 2
irfeat14	= -22.5
irlen14		= 0.22697
irrho14		= -34.9506
irstep14	= 1
irfeat15	= 0
irlen15		= 0.20774
irrho15		= -13.5010
irstep15	= 2
	
#---------------------------------------------------------------------
# Virtual sensors definition
#---------------------------------------------------------------------
MAXVIRTU	= 16
MODEVIRTU	= 4
RANGEVIRTU	= 6.0
CONEVIRTU	= 17
RAYVIRTU	= 15

virtufeat0	= 0
virtulen0	= 0.20774
virturho0	= 13.5010
virtufeat1	= 22.5
virtulen1	= 0.22697
virturho1	= 34.9506
virtufeat2	= 67.5
virtulen2	= 0.22697
virturho2	= 55.0493
virtufeat3	= 90
virtulen3	= 0.20774
virturho3	= 76.4989
virtufeat4	= 90
virtulen4	= 0.20774
virturho4	= 103.5010
virtufeat5	= 112.5
virtulen5	= 0.22697
virturho5	= 124.9506
virtufeat6	= 157.5
virtulen6	= 0.22697
virturho6	= 145.0493
virtufeat7	= 180
virtulen7	= 0.20774
virturho7	= 166.4989
virtufeat8	= -180
virtulen8	= 0.20774
virturho8	= -166.4989
virtufeat9	= -157.5
virtulen9	= 0.22697
virturho9	= -145.0493
virtufeat10	= -112.5
virtulen10	= 0.22697
virturho10	= -124.9506
virtufeat11	= -90
virtulen11	= 0.20774
virturho11	= -103.5010
virtufeat12	= -90
virtulen12	= 0.20774
virturho12	= -76.4989
virtufeat13	= -67.5
virtulen13	= 0.22697
virturho13	= -55.0493
virtufeat14	= -22.5
virtulen14	= 0.22697
virturho14	= -34.9506
virtufeat15	= 0
virtulen15	= 0.20774
virturho15	= -13.5010

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
# Vision sensors definition
#---------------------------------------------------------------------
MAXVISION	= 1
CONEVIS		= 60
#ERRORVIS	= 0.05
ERRORVIS	= 0
CYCLEVIS	= 1
VISION0		= devices.drivers.vision.quaky2.Quaky2Vis|5,7000,10.0.0.1:8000

#visfeat0	= 0.0
#vislen0	= 0.15
#visrho0	= 0.0

visfeat0	= 0.0
vislen0		= 0.0
visrho0		= 0.0
vishgt0		= 0.35
visstep0	= 1

#---------------------------------------------------------------------
# LCD debug serial port
#---------------------------------------------------------------------
LCD			= /dev/ttyS0

#---------------------------------------------------------------------
# Robot kinematics definition
#---------------------------------------------------------------------
# Target robot speed (m/s, deg/s)
VMAX		= 0.42
RMAX		= 110.0

# Maximum speed of the driving wheels (m/s)
MAXMOTOR 	= 0.42

# Robot plant model
DRIVEMODEL	= tc.vrobot.models.DifferentialDrive
BASE		= 0.365
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
# Robot CAN bus configuration
#---------------------------------------------------------------------
CAN_M_SETID		= 96
CAN_M_CFGID		= 97
CAN_M_ODOMID	= 98
CAN_S_FIREID	= 160
CAN_S_SONID		= 192
CAN_S_IRID		= 208
CAN_S_BUMID		= 250

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
CAN_OCR 		= 250
CAN_CDR 		= 192
 
#OCR = 0xFA y CDR = 0xC0

#---------------------------------------------------------------------
# Robot icon specification
#---------------------------------------------------------------------
RADIUS		= 0.25
LINES		= 24

iconxi0 	= 0.202
iconyi0 	= -0.097
iconxf0 	= 0.202
iconyf0 	= 0.097
iconxi1 	= 0.202
iconyi1 	= 0.097
iconxf1 	= 0.170
iconyf1 	= 0.170
iconxi2 	= 0.170
iconyi2 	= 0.170
iconxf2 	= 0.097
iconyf2 	= 0.202
iconxi3 	= 0.097
iconyi3 	= 0.202
iconxf3 	= -0.097
iconyf3 	= 0.202
iconxi4 	= -0.097
iconyi4 	= 0.202
iconxf4 	= -0.170
iconyf4 	= 0.170
iconxi5 	= -0.170
iconyi5 	= 0.170
iconxf5 	= -0.202
iconyf5 	= 0.097
iconxi6 	= -0.202
iconyi6 	= 0.097
iconxf6 	= -0.202
iconyf6 	= -0.097
iconxi7 	= -0.202
iconyi7 	= -0.097
iconxf7 	= -0.170
iconyf7 	= -0.170
iconxi8 	= -0.170
iconyi8 	= -0.170
iconxf8 	= -0.097
iconyf8 	= -0.202
iconxi9 	= -0.097
iconyi9 	= -0.202
iconxf9 	= 0.097
iconyf9 	= -0.202
iconxi10 	= 0.097
iconyi10 	= -0.202
iconxf10 	= 0.170
iconyf10 	= -0.170 
iconxi11 	= 0.170
iconyi11 	= -0.170
iconxf11 	= 0.202
iconyf11 	= -0.097 

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

iconxi15 	= 0.202
iconyi15 	= -0.15
iconxf15 	= 0.202
iconyf15 	= 0.15
iconxi16 	= 0.202
iconyi16 	= 0.15
iconxf16 	= 0.242
iconyf16 	= 0.15
iconxi17 	= 0.242
iconyi17 	= 0.15
iconxf17 	= 0.242
iconyf17 	= 0.00
iconxi18 	= 0.242
iconyi18 	= 0.00
iconxf18 	= 0.242
iconyf18 	= -0.15
iconxi19 	= 0.242
iconyi19 	= -0.15
iconxf19 	= 0.202
iconyf19 	= -0.15
iconxi20 	= 0.242
iconyi20 	= 0.15
iconxf20 	= 0.262
iconyf20 	= 0.15
iconxi21 	= 0.262
iconyi21 	= 0.15
iconxf21 	= 0.297
iconyf21 	= 0.17
iconxi22 	= 0.242
iconyi22 	= -0.15
iconxf22 	= 0.262
iconyf22 	= -0.15
iconxi23 	= 0.262
iconyi23 	= -0.15
iconxf23 	= 0.297
iconyf23 	= -0.17

#-----------------------------------------------------------------------------------------
# Robot 3D specification
#-----------------------------------------------------------------------------------------
V3DFILE		= conf/3dmodels/quaky2.3ds
V3DCOLORR	= 0.6
V3DCOLORG	= 0.6
V3DCOLORB	= 0.0
