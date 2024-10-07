#---------------------------------------------------------------------
# Sept 2000		Humberto Martinez Barbera
#
# Nomad-200 robot
#

#---------------------------------------------------------------------
# Sonar sensors definition
#---------------------------------------------------------------------
MAXSONAR = 16
RANGESON = 5.0
MINIMSON = 0.135
CONESON = 20
SENSIBSON = 0.0000001
RAYSON = 11
ERRORSON = 0.05
MODESON = 2

sonfeat0 = 0.0
sonlen0 = 0.2305
sonrho0 = 0.0
sonfeat1 = 22.5
sonlen1 = 0.2305
sonrho1 =  22.5
sonfeat2 = 45.0
sonlen2 = 0.2305
sonrho2 = 45.0
sonfeat3 = 67.5
sonlen3 = 0.2305
sonrho3 = 67.5
sonfeat4 = 90.0
sonlen4= 0.2305
sonrho4 = 90.0
sonfeat5 = 112.5
sonlen5 = 0.2305
sonrho5 = 112.5
sonfeat6 = 135.0
sonlen6 = 0.2305
sonrho6 = 135.0
sonfeat7 = 157.5
sonlen7 = 0.2305
sonrho7 = 157.5
sonfeat8 = 180.0
sonlen8 = 0.2305
sonrho8 = 180.0
sonfeat9 = -157.5
sonlen9 = 0.2305
sonrho9 = -157.5
sonfeat10 = -135.0
sonlen10 = 0.2305
sonrho10 = -135.0
sonfeat11 = -112.5
sonlen11 = 0.2305
sonrho11 = -112.5
sonfeat12 = -90.0
sonlen12 = 0.2305
sonrho12 = -90.0
sonfeat13 = -67.5
sonlen13 = 0.2305
sonrho13 = -67.5
sonfeat14 = -45.0
sonlen14 = 0.2305
sonrho14 = -45.0
sonfeat15 = -22.5
sonlen15 = 0.2305
sonrho15 = -22.5

#---------------------------------------------------------------------
# Infrared sensors definition
#---------------------------------------------------------------------
MAXIR = 16
RANGEIR = 0.45
MINIMIR = 0.1
CONEIR= 10
RAYIR = 5
ERRORIR = 0.05
MODEIR = 2

irfeat0 = 0.0
irlen0 = 0.2305
irrho0 = 0.0
irfeat1 = 22.5
irlen1 = 0.2305
irrho1 =  22.5
irfeat2 = 45.0
irlen2 = 0.2305
irrho2 = 45.0
irfeat3 = 67.5
irlen3 = 0.2305
irrho3 = 67.5
irfeat4 = 90.0
irlen4= 0.2305
irrho4 = 90.0
irfeat5 = 112.5
irlen5 = 0.2305
irrho5 = 112.5
irfeat6 = 135.0
irlen6 = 0.2305
irrho6 = 135.0
irfeat7 = 157.5
irlen7 = 0.2305
irrho7 = 157.5
irfeat8 = 180.0
irlen8 = 0.2305
irrho8 = 180.0
irfeat9 = -157.5
irlen9 = 0.2305
irrho9 = -157.5
irfeat10 = -135.0
irlen10 = 0.2305
irrho10 = -135.0
irfeat11 = -112.5
irlen11 = 0.2305
irrho11 = -112.5
irfeat12 = -90.0
irlen12 = 0.2305
irrho12 = -90.0
irfeat13 = -67.5
irlen13 = 0.2305
irrho13 = -67.5
irfeat14 = -45.0
irlen14 = 0.2305
irrho14 = -45.0
irfeat15 = -22.5
irlen15 = 0.2305
irrho15 = -22.5
	
#---------------------------------------------------------------------
# Virtual sensors definition
#---------------------------------------------------------------------
MAXVIRTU = 16
RANGEVIRTU = 5.0
CONEVIRTU = 17
RAYVIRTU = 15
#MODEVIRTU = 3
MODEVIRTU = 4
#FILTERVIRTU = ann2.filter

virtufeat0 = 0.0
virtulen0 = 0.2305
virturho0 = 0.0
virtufeat1 = 22.5
virtulen1 = 0.2305
virturho1 =  22.5
virtufeat2 = 45.0
virtulen2 = 0.2305
virturho2 = 45.0
virtufeat3 = 67.5
virtulen3 = 0.2305
virturho3 = 67.5
virtufeat4 = 90.0
virtulen4= 0.2305
virturho4 = 90.0
virtufeat5 = 112.5
virtulen5 = 0.2305
virturho5 = 112.5
virtufeat6 = 135.0
virtulen6 = 0.2305
virturho6 = 135.0
virtufeat7 = 157.5
virtulen7 = 0.2305
virturho7 = 157.5
virtufeat8 = 180.0
virtulen8 = 0.2305
virturho8 = 180.0
virtufeat9 = -157.5
virtulen9 = 0.2305
virturho9 = -157.5
virtufeat10 = -135.0
virtulen10 = 0.2305
virturho10 = -135.0
virtufeat11 = -112.5
virtulen11 = 0.2305
virturho11 = -112.5
virtufeat12 = -90.0
virtulen12 = 0.2305
virturho12 = -90.0
virtufeat13 = -67.5
virtulen13 = 0.2305
virturho13 = -67.5
virtufeat14 = -45.0
virtulen14 = 0.2305
virturho14 = -45.0
virtufeat15 = -22.5
virtulen15 = 0.2305
virturho15 = -22.5

#---------------------------------------------------------------------
# Group sensors definition
#---------------------------------------------------------------------
MAXGROUP = 5
RANGEGROUP = 0.75

groupmode0 = 1
groupfeat0 = 90.0
grouplen0 = 0.2305
grouprho0 = 90.0
groupequ0 = 3,3,0.333,4,0.333,5,0.333

groupmode1 = 2
groupfeat1 = 45.0
grouplen1 = 0.2305
grouprho1 =  45.0
groupequ1 = 3,1,0.33,2,0.33,3,0.33

groupmode2 = 0
groupfeat2 = 0.0
grouplen2 = 0.2305
grouprho2 = 0.0
groupequ2 = 3,0,1.0,1,1.0,15,1.0

groupmode3 = 2
groupfeat3 = -45.0
grouplen3 = 0.2305
grouprho3 = -45.0
groupequ3 = 3,15,0.33,14,0.33,13,0.33

groupmode4 = 1
groupfeat4 = -90.0
grouplen4= 0.2305
grouprho4 = -90.0
groupequ4 = 3,13,0.333,12,0.333,11,0.333

# Directly from the original BG code
#group[0] => left	= (virtu13 + virtu12 + virtu11) / 3.0;
#group[1] => leftd	= (0.33 * min (virtu15, SONLIMIT)) + (0.33 * min (virtu14, SONLIMIT)) + (0.33 * min (virtu13, SONLIMIT));
#group[2] => front	= (virtu0 + min (virtu1, virtu15)) / 2.0;
#group[3] => rightd	= (0.33 * min (virtu1, SONLIMIT)) + (0.33 * min (virtu2, SONLIMIT)) + (0.33 * min (virtu3, SONLIMIT));
#group[4] => right	= (virtu3 + virtu4 + virtu5) / 3.0;
		

#---------------------------------------------------------------------
# Bumper sensors definition
#---------------------------------------------------------------------
MAXBUMPER = 4

bumxi0 = 0.2400
bumyi0 = 0.1950
bumxf0 = 0.2400
bumyf0 = -0.1950
bumxi1 = 0.2400
bumyi1 = -0.1950
bumxf1 = -0.2300
bumyf1 = -0.1500
bumxi2 = -0.2300
bumyi2 = -0.1500
bumxf2 = -0.2300
bumyf2 = 0.1500
bumxi3 = -0.2300
bumyi3 = 0.1500
bumxf3 = 0.2400
bumyf3 = 0.1950

#---------------------------------------------------------------------
# Robot kinematics definition
#---------------------------------------------------------------------
#MAXMOTOR = 30
MAXMOTOR = 130
MAXSTEER = 300
MAXSPEED = 1
MAXTURN = 10
DRIVEMODEL	= tc.vrobot.models.SynchroDrive
BASE = 0.2305
GEAR = 60.0
VMAX = 0.75
RMAX = 90.0
WHEEL = 0.1487
PULSES = 500
ODOM_ET = 0.025
ODOM_ER = 0.1
ODOM_BIAS = 0.10

#---------------------------------------------------------------------
# Robot icon specification
#---------------------------------------------------------------------
RADIUS = 0.2690
LINES = 19

iconxi0 = 0.0
iconyi0 = 0.2305
iconxf0 = -0.0882
iconyf0 = 0.2129
iconxi1 = -0.0882
iconyi1 = 0.2129
iconxf1 = -0.1629
iconyf1 = 0.1629
iconxi2 = -0.1629
iconyi2 = 0.1629
iconxf2 = -0.2129
iconyf2 = 0.0882
iconxi3 = -0.2129
iconyi3 = 0.0882
iconxf3 = -0.2305
iconyf3 = 0.0
iconxi4 = -0.2305
iconyi4 = 0.0
iconxf4 = -0.2129
iconyf4 = -0.0882
iconxi5 = -0.2129
iconyi5 = -0.0882
iconxf5 = -0.1629
iconyf5 = -0.1629
iconxi6 = -0.1629
iconyi6 = -0.1629
iconxf6 = -0.0882
iconyf6 = -0.2129
iconxi7 = -0.0882
iconyi7 = -0.2129
iconxf7 = 0.0
iconyf7 = -0.2305
iconxi8 = 0.0
iconyi8 = -0.2305
iconxf8 = 0.0882
iconyf8 = -0.2129
iconxi9 = 0.0882
iconyi9 = -0.2129
iconxf9 = 0.1629
iconyf9 = -0.1629
iconxi10 = 0.1629
iconyi10 = -0.1629
iconxf10 = 0.2129
iconyf10 = -0.0882 
iconxi11 = 0.2129
iconyi11 = -0.0882
iconxf11 = 0.2305
iconyf11 = 0.0
iconxi12 = 0.0
iconyi12 = 0.2305
iconxf12 = 0.0882
iconyf12 = 0.2129 
iconxi13 = 0.0882
iconyi13 = 0.2129
iconxf13 = 0.1629
iconyf13 = 0.1629
iconxi14 = 0.1629
iconyi14 = 0.1629
iconxf14 = 0.2129
iconyf14 = 0.0882
iconxi15 = 0.2129
iconyi15 = 0.0882
iconxf15 = 0.2305
iconyf15 = 0.0

iconxi16 = 0.2305
iconyi16 = 0.0
iconxf16 = 0.1305
iconyf16 = -0.1
iconxi17 = 0.2305
iconyi17 = 0.0
iconxf17 = 0.1305
iconyf17 = 0.1
iconxi18 = 0.1305
iconyi18 = -0.1
iconxf18 = 0.1305
iconyf18 = 0.1
