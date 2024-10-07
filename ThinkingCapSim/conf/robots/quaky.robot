#---------------------------------------------------------------------# Jun 2000		Humberto Martinez Barbera## Quaky-Ant robot ##---------------------------------------------------------------------# Sonar sensors definition#---------------------------------------------------------------------MAXSONAR = 10RANGESON = 5.0MINIMSON = 0.135CONESON = 20SENSIBSON = 0.0000001#SENSIBSON = 0.001RAYSON = 11ERRORSON = 0.05MODESON = 2#FILTERSON = lagarra2.sonar.filtersonfeat0 = 90.0sonlen0 = 0.253783sonrho0 = 55.8403sonfeat1 = 60.0sonlen1 = 0.272617sonrho1 =  44.1826sonfeat2 = 30.0sonlen2 = 0.280011sonrho2 = 29.9987sonfeat3 = 0.0sonlen3 = 0.268200sonrho3 = 11.8336sonfeat4 = 0.0sonlen4= 0.268200sonrho4 = -11.8336sonfeat5 = -30.0sonlen5 = 0.280011sonrho5 = -29.9987sonfeat6 = -60.0sonlen6 = 0.272617sonrho6 = -44.1826sonfeat7 = -90.0sonlen7 = 0.253783sonrho7 = -55.8403sonfeat8 = -90.0sonlen8 = 0.237828sonrho8 = -140.1173sonfeat9 = 90.0sonlen9 = 0.237828sonrho9 = 140.1173#---------------------------------------------------------------------# Infrared sensors definition#---------------------------------------------------------------------MAXIR = 7#RANGEIR = 0.45RANGEIR = 1.20MINIMIR = 0.1CONEIR= 10RAYIR = 5ERRORIR = 0.05MODEIR = 2#FILTERIR = lagarra2.ir.filterirfeat0 = 60.0irlen0 = 0.272617irrho0 =  44.1826irfeat1 = 30.0irlen1 = 0.280011irrho1 = 29.9987irfeat2 = 0.0irlen2 = 0.268200irrho2 = 11.8336irfeat3 = 0.0irlen3= 0.262500irrho3 = 0.0irfeat4 = 0.0irlen4= 0.268200irrho4 = -11.8336irfeat5 = -30.0irlen5 = 0.280011irrho5 = -29.9987irfeat6 = -60.0irlen6 = 0.272617irrho6 = -44.1826	#---------------------------------------------------------------------# Virtual sensors definition#---------------------------------------------------------------------MAXVIRTU = 10MODEVIRTU = 4RANGEVIRTU = 5.0CONEVIRTU = 17RAYVIRTU = 15#MODEVIRTU = 3FILTERVIRTU = anfis5.filtervirtumode0 = 0virtufeat0 = 90.0virtulen0 = 0.253783virturho0 = 55.8403virtufeat1 = 60.0virtulen1 = 0.272617virturho1 =  44.1826virtufeat2 = 30.0virtulen2 = 0.280011virturho2 = 29.9987virtufeat3 = 0.0virtulen3 = 0.268200virturho3 = 11.8336virtufeat4 = 0.0virtulen4= 0.268200virturho4 = -11.8336virtufeat5 = -30.0virtulen5 = 0.280011virturho5 = -29.9987virtufeat6 = -60.0virtulen6 = 0.272617virturho6 = -44.1826virtumode7 = 0virtufeat7 = -90.0virtulen7 = 0.253783virturho7 = -55.8403virtumode8 = 0virtufeat8 = -90.0virtulen8 = 0.237828virturho8 = -140.1173virtumode9 = 0virtufeat9 = 90.0virtulen9 = 0.237828virturho9 = 140.1173#---------------------------------------------------------------------# Group sensors definition#---------------------------------------------------------------------MAXGROUP = 5RANGEGROUP = 0.75groupmode0 = 4groupfeat0 = 90.0grouplen0 = 0.2200grouprho0 = 120.0groupmode1 = 4groupfeat1 = 45.0grouplen1 = 0.2800grouprho1 =  45.0groupmode2 = 4groupfeat2 = 0.0grouplen2 = 0.2625grouprho2 = 0.0groupmode3 = 4groupfeat3 = -45.0grouplen3 = 0.2800grouprho3 = -45.0groupmode4 = 4groupfeat4 = -90.0grouplen4 = 0.2200grouprho4 = -120.0#---------------------------------------------------------------------# Bumper sensors definition#---------------------------------------------------------------------MAXBUMPER = 4bumxi0 = 0.2400bumyi0 = 0.1950bumxf0 = 0.2400bumyf0 = -0.1950bumxi1 = 0.2400bumyi1 = -0.1950bumxf1 = -0.2300bumyf1 = -0.1500bumxi2 = -0.2300bumyi2 = -0.1500bumxf2 = -0.2300bumyf2 = 0.1500bumxi3 = -0.2300bumyi3 = 0.1500bumxf3 = 0.2400bumyf3 = 0.1950#---------------------------------------------------------------------# Robot kinematics definition#---------------------------------------------------------------------MAXMOTOR = 100MAXSPEED = 1MAXTURN = 10DRIVEMODEL	= tc.vrobot.models.DifferentialDriveBASE = 0.3625GEAR = 60.0#VMAX = 0.4705VMAX = 0.2RMAX = 130.0WHEEL = 0.1487PULSES = 500#ODOM_ET = 0.0#ODOM_ER = 0.0ODOM_ET = 0.025ODOM_ER = 0.1ODOM_BIAS = 0.10#---------------------------------------------------------------------# Robot icon specification#---------------------------------------------------------------------RADIUS = 0.30LINES = 11iconxi0 = -0.2325iconyi0 = 0.1525iconxf0 = 0.0925iconyf0 = 0.1525iconxi1 = -0.2325iconyi1 = -0.1525iconxf1 = 0.0925iconyf1 = -0.1525iconxi2 = -0.2325iconyi2 = 0.1525iconxf2 = -0.2325iconyf2 = -0.1525iconxi3 = 0.0925iconyi3 = -0.2100iconxf3 = 0.0925iconyf3 = 0.2100iconxi4 = 0.2625iconyi4 = -0.1150iconxf4 = 0.2625iconyf4 = 0.1150iconxi5 = 0.0925iconyi5 = -0.2100iconxf5 = 0.1625iconyf5 = -0.2100iconxi6 = 0.0925iconyi6 = 0.2100iconxf6 = 0.1625iconyf6 = 0.2100iconxi7 = 0.1625iconyi7 = -0.2100iconxf7 = 0.2255iconyf7 = -0.1800iconxi8 = 0.1625iconyi8 = 0.2100iconxf8 = 0.2255iconyf8 = 0.1800iconxi9 = 0.2255iconyi9 = -0.1800iconxf9 = 0.2625iconyf9 = -0.1150iconxi10 = 0.2255iconyi10 = 0.1800iconxf10 = 0.2625iconyf10 = 0.1150 