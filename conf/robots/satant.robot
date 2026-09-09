#---------------------------------------------------------------------
# Jun 2001		Humberto Martinez Barbera
# Dec 2002		Humberto Martinez Barbera
#
# SatAnt robot (sort of autonomous car)
#

#---------------------------------------------------------------------
# Positioning devices definition
#---------------------------------------------------------------------
MAXENCS			= 4
CAPTORS			= devices.drivers.captors.UDP.UDPCaptors|10005

MAXCOMPASS		= 1
COMPASS0		= devices.drivers.compass.TCM2.TCM2|/dev/ttyS3
COMPASS0_OFFSET	= -180.0

MAXGPS			= 1
GPS0			= devices.drivers.gps.Novatel.Novatel|/dev/ttyS1

MAXRADAR		= 1
RADAR0			= devices.drivers.radar.Fujitsu.Fujitsu|/dev/ttyS0
RAYRAD			= 16

#---------------------------------------------------------------------
#Tracking sensors definition
#---------------------------------------------------------------------
MAXTRACKER		= 1
RANGETRK 		= 140
MINIMTRK		= 5
CONETRK 		= 16
OBJTRK 			= 8

trkfeat0 		= 0.0
trklen0 		= 1.60
trkrho0 		= 15.0

#---------------------------------------------------------------------
# Group sensors definition
#---------------------------------------------------------------------
MAXGROUP		= 1
RANGEGROUP		= 50.00
CONEGROUP		= 16.0

groupmode0		= 6
groupfeat0		= 0.0
grouplen0		= 1.6
grouprho0		= 15.0


#---------------------------------------------------------------------
# CAN bus identifiers and parameters
#---------------------------------------------------------------------
CAN_ADDRESS		= mimics4.inf.um.es
CAN_PORT 		= 10001
REC_PORT 		= 10005

#---------------------------------------------------------------------
# Actuators ranges and parameters
#---------------------------------------------------------------------
KMOTOR 			= 70
KSTEER 			= 150
KMSPD 			= 30

#---------------------------------------------------------------------
# Braking times
#---------------------------------------------------------------------
BRAKE20_T1		= 1000
BRAKE20_T2 		= 2000
BRAKE20_T3 		= 1500

BRAKE30_T1 		= 1000
BRAKE30_T2 		= 1000
BRAKE30_T3 		= 1500

BRAKE40_T1 		= 1000
BRAKE40_T2 		= 500
BRAKE40_T3 		= 1500

#---------------------------------------------------------------------
# Update cycle
#---------------------------------------------------------------------
# Robot control cycle (ms)
DTIME			= 100

#---------------------------------------------------------------------
# Robot kinematics definition
#---------------------------------------------------------------------
# Target robot speed (m/s, deg/s)
VMAX			= 40.0
RMAX			= 30.0

# Kynematics constraints. Maximum speed and angle of the driving wheel (m/s, deg)
MAXMOTOR 		= 40.0
MAXSTEER 		= 45.0

# Robot plant model
DRIVEMODEL		= tc.vrobot.models.AckermanDrive
LENGHT 			= 2.34

# Odometry errors for simulation [m/s, rad/s]
ODOM_ET 		= 0.0
ODOM_ER 		= 0.0
ODOM_BIAS		= 0.0

#---------------------------------------------------------------------
# Robot icon specification
#---------------------------------------------------------------------
RADIUS 			= 0.1
LINES 			= 23

iconxi0 		= 1.5
iconyi0 		= -0.5
iconxf0 		= 1.5
iconyf0 		= 0.5
iconxi1 		= 1.5
iconyi1 		= 0.5
iconxf1 		= 1.2
iconyf1 		= 1.0
iconxi2 		= 1.2
iconyi2 		= 1.0
iconxf2 		= 0.5
iconyf2 		= 1.0
iconxi3 		= 0.5
iconyi3 		= 1.0
iconxf3 		= 0.5
iconyf3 		= 0.5
iconxi4 		= 0.5
iconyi4 		= 0.5
iconxf4 		= -1.0
iconyf4 		= 0.6
iconxi5 		= -1.0
iconyi5 		= 0.6
iconxf5 		= -1.0
iconyf5 		= 1.0
iconxi6 		= -1.0
iconyi6 		= 1.0
iconxf6 		= -2.0
iconyf6 		= 1.0
iconxi7 		= -2.0
iconyi7 		= 1.0
iconxf7 		= -2.0
iconyf7 		= -1.0
iconxi8 		= -2.0
iconyi8 		= -1.0
iconxf8 		= -1.0
iconyf8 		= -1.0
iconxi9 		= -1.0
iconyi9 		= -1.0
iconxf9 		= -1.0
iconyf9 		= -0.6
iconxi10 		= -1.0
iconyi10 		= -0.6
iconxf10 		= 0.5
iconyf10 		= -0.5
iconxi11 		= 0.5
iconyi11 		= -0.5
iconxf11 		= 0.5
iconyf11 		= -1.0
iconxi12 		= 0.5
iconyi12 		= -1.0
iconxf12 		= 1.2
iconyf12 		= -1.0
iconxi13 		= 1.2
iconyi13 		= -1.0
iconxf13 		= 1.5
iconyf13 		= -0.5

iconxi14 		= 0.4
iconyi14 		= -0.2
iconxf14 		= 0.4
iconyf14 		= 0.2
iconxi15 		= 0.4
iconyi15 		= 0.2
iconxf15 		= 0.0
iconyf15 		= 0.4
iconxi16 		= 0.0
iconyi16 		= 0.4
iconxf16 		= -0.8
iconyf16 		= 0.4
iconxi17 		= -0.8
iconyi17 		= 0.4
iconxf17 		= -0.8
iconyf17 		= -0.4
iconxi18 		= -0.8
iconyi18 		= -0.4
iconxf18 		= 0.0
iconyf18 		= -0.4
iconxi19 		= 0.0
iconyi19 		= -0.4
iconxf19 		= 0.4
iconyf19 		= -0.2

iconxi20 		= 1.5
iconyi20 		= 0.0
iconxf20 		= 1.3
iconyf20 		= 0.2
iconxi21 		= 1.3
iconyi21 		= 0.2
iconxf21 		= 1.3
iconyf21 		= -0.2
iconxi22 		= 1.3
iconyi22 		= -0.2
iconxf22 		= 1.5
iconyf22 		= 0.0

