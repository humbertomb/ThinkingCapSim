-- Behaviour: GoToPos
-- 20070402 Francisco Martin
-- 20260924 Humberto Martinez

-- Constants and parameters 
local ANGLELARGE = 50
local ANGLESMALL = 25
local SLOWDOWN = 300
local GOTTHERE = 100
local MAXVEL = 370

-- Geometrical computations
local pos = chaos.getCurrentPos()
local dest = chaos.getDesiredPos()
--local dest = chaos.getStartPos()

local vlin = 0
local vlat = 0
local vrot = 0

local net1 = chaos.getLpo(chaos.NET1_LPO)
dx = dest.x - pos.x
dy = dest.y - pos.y
dth = math.deg(math.atan2(dy,dx))
rho= math.sqrt(dx*dx+dy*dy)		-- Distance to destination position
theta = math.normdeg (dth - pos.theta)	-- Difference with the angle needed for going to destination

rdestx=-rho*math.sin(math.rad(theta))
rdesty=rho*math.cos(math.rad(theta))

if(math.abs(rdestx)>math.abs(rdesty)) then
	maxdist=math.abs(rdestx)
else
	maxdist=math.abs(rdesty)
end

rho= math.sqrt(rdestx*rdestx+rdesty*rdesty)		-- Distance to destination position

if(rho>SLOWDOWN) then	
	MAXVEL=370
elseif(rho>GOTTHERE) then
	MAXVEL=250
else
	MAXVEL=0
end

vlin = (rdesty/maxdist)*MAXVEL
vlat = (rdestx/maxdist)*MAXVEL

if (math.abs(net1.theta) < ANGLESMALL) then		-- small angle
		vrot = 0.9 * net1.theta
	else				-- large angle
		vrot = 1.2 * net1.theta
	end

if((net1.anchored < 0.8) and  (rho<GOTTHERE)) then
if (math.abs(net1.theta) < ANGLESMALL) then		-- small angle
		vrot = 0.9 * net1.theta
	else				-- large angle
		vrot = 1.2 * net1.theta
	end

--
end

chaos.trackLandMarks()
chaos.setNeeded(chaos.NET1_LPO, 1.0-net1.anchored)
chaos.setVlin(vlin)
chaos.setVrot(vrot)
chaos.setVlat(-vlat)



