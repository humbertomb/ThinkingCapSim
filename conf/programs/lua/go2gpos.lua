-- Behaviour: GoToGlobalPositionFacing
-- 20070402 Francisco mart’n

-- Constants and parameters
NET1LPO = 1
ANGLELARGE = 0.85		-- 50-60 grados
ANGLESMALL = 0.40		-- 40 grados
SLOWDOWN = 300
GOTTHERE = 100

PI05 = 1.5707963268
PI =  3.1415926536	
PI2 = 6.2831853072

MAXVEL = 370

-- Geometrical computations
local pos = chaos.gsGetMyPos()
local dest = chaos.getDesiredPos()

--dest.x = 0
--dest.y = 0
--io.write("---------------------------------------------------\n")

local net1 = chaos.getLpo(NET1LPO)
dx = dest.x - pos.x
dy = dest.y - pos.y
dth = math.atan2(dy,dx)
rho= math.sqrt(dx*dx+dy*dy)		-- Distance to destination position
theta = dth - pos.theta		-- Difference with the angle needed for going to destination

-- Normalize angle between PI and -PI
if (theta > PI) then
	theta = theta - PI2 
elseif (theta < -PI) then
	theta =  theta + PI2 
end	

rdestx=-rho*math.sin(theta)
rdesty=rho*math.cos(theta)


--io.write(" px = ",pos.x," py = ",pos.y)


--io.write("relative pos:   x=", rdestx,"   y=   ", rdesty, "\n")

if(math.abs(rdestx)>math.abs(rdesty)) then
	maxdist=math.abs(rdestx)
else
	maxdist=math.abs(rdesty)
end

rho= math.sqrt(rdestx*rdestx+rdesty*rdesty)		-- Distance to destination position

--io.write(" rho = ",rho)

if(rho>SLOWDOWN) then	
	MAXVEL=370
elseif(rho>GOTTHERE) then
	MAXVEL=250
else
	MAXVEL=0
end
--io.write(" MAXVEL =  ",MAXVEL,"\n")

vlin = (rdesty/maxdist)*MAXVEL
vlat = (rdestx/maxdist)*MAXVEL

if (math.abs(net1.theta) < ANGLESMALL) then		-- small angle
		vrot = 50 * net1.theta
	else				-- large angle
		vrot = 70 * net1.theta
	end

--io.write(" ------------------------vlin = ",vlin," vlat = ",vlat,"\n")
--vrot = 40
if((net1.anchored < 0.8) and  (rho<GOTTHERE)) then
if (math.abs(net1.theta) < ANGLESMALL) then		-- small angle
		vrot = 50 * net1.theta
	else				-- large angle
		vrot = 70 * net1.theta
	end

--
end

chaos.trackLandMarks()
chaos.setNeeded(NET1LPO, 1.0-net1.anchored)
chaos.setVlin(vlin)
chaos.setVrot(vrot)
chaos.setVlat(-vlat)



