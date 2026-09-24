-- Behaviour: GoToGlobalPositionFacing
-- 20070402 Francisco mart’n

-- Constants and parameters
NET1LPO = chaos.NET1_LPO
ANGLELARGE = 48.7014		-- degrees, as every angle here
ANGLESMALL = 22.9183		-- degrees, as every angle here
SLOWDOWN = 300
GOTTHERE = 100

PI05 = 90
PI =  180	
PI2 = 360

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
dth = math.deg(math.atan2(dy,dx))
rho= math.sqrt(dx*dx+dy*dy)		-- Distance to destination position
theta = dth - pos.theta		-- Difference with the angle needed for going to destination

-- Normalize angle between PI and -PI
if (theta > PI) then
	theta = theta - PI2 
elseif (theta < -PI) then
	theta =  theta + PI2 
end	

rdestx=-rho*math.sin(math.rad(theta))
rdesty=rho*math.cos(math.rad(theta))


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
		vrot = 0.8726646259971648 * net1.theta
	else				-- large angle
		vrot = 1.2217304763960306 * net1.theta
	end

--io.write(" ------------------------vlin = ",vlin," vlat = ",vlat,"\n")
--vrot = 40
if((net1.anchored < 0.8) and  (rho<GOTTHERE)) then
if (math.abs(net1.theta) < ANGLESMALL) then		-- small angle
		vrot = 0.8726646259971648 * net1.theta
	else				-- large angle
		vrot = 1.2217304763960306 * net1.theta
	end

--
end

chaos.trackLandMarks()
chaos.setNeeded(NET1LPO, 1.0-net1.anchored)
chaos.setVlin(vlin)
chaos.setVrot(vrot)
chaos.setVlat(-vlat)



