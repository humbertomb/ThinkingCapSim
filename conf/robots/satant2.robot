#---------------------------------------------------------------------
# Jun 2001		Humberto Martinez Barbera
# Dec 2002		Humberto Martinez Barbera
#
# SatAnt robot (leading human-driven car)
#

#---------------------------------------------------------------------
# Positioning devices definition
#---------------------------------------------------------------------
MAXCOMPASS		= 1
COMPASS0		= devices.drivers.compass.TCM2.TCM2|/dev/ttyS3
COMPASS0_OFFSET	= -180.0

MAXGPS			= 1
GPS0			= devices.drivers.gps.Novatel.Novatel|/dev/ttyS1

#---------------------------------------------------------------------
# Update cycle
#---------------------------------------------------------------------
# Robot control cycle (ms)
DTIME			= 1000

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
DRIVEMODEL	= tc.vrobot.models.AckermanDrive
LENGHT 			= 2.34

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

