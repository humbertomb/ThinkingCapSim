-- Behaviour: LookForBall
--
-- 20060406 Humberto Martinez
-- 20260924 Humberto Martinez

DISTANCE = 450
HEADING = 5

local ball	= chaos.getLpo(chaos.BALL_LPO)
local net1	= chaos.getLpo(chaos.NET1_LPO)
local net2	= chaos.getLpo(chaos.NET2_LPO)
local info	= chaos.getBehaviorInfo ()

if info.isNew > 0 then
     chaos.setGlobal("ALIGN_RHO",1,ball.rho)
end
DISTANCE = chaos.getGlobal("ALIGN_RHO")

-- Use direction of the most recently detected net
if net1.anchored > net2.anchored then
	tar_th  = net1.theta
else
	tar_th  = math.normdeg (net2.theta + PI)
end

obj_rho = ball.rho
obj_th  = ball.theta

	
-- Compute distance error, and limit control actions
edist	= obj_rho - DISTANCE
if edist > 300 then			
	edist = 300
elseif edist < -300 then
	edist = -300
end
	
-- Compute heading error and normalise
delta   = tar_th - obj_th;
if (delta > PI) then
	delta = delta - PI2 
elseif (delta < -PI) then
	delta =  delta + PI2 
end
	
vlin = 0
vlat = 0
vrot = 0

-- Keep distance to ball	
if edist > 100 then
	vlin = 0.75 * edist
else
	vlin = 0
end

-- Keep heading to ball
if math.abs (obj_th) < 20 then
	vrot = 0
elseif math.abs (obj_th) < 45 then
	vrot = 1.75 * obj_th
elseif obj_th >= 45 then
	vrot = 70
else
	vrot = -70
end

-- Align ball and net
if math.abs(delta) < HEADING then
	vlat = 0
else
	if delta > 0 then
		vlat = -7 * delta - 50
		vrot = vrot + 0.9 * delta

		if vlat < -200 then vlat = -200 end
		if vrot > 70 then vrot = 70 end
	else
   		vlat = -7 * delta + 50
		vrot = vrot + 0.9 * delta

		if vlat > 200 then vlat = 200 end
		if vrot < -70 then vrot = -70 end
	end
end

chaos.setNeeded(chaos.BALL_LPO,1.0)
chaos.setNeeded(chaos.NET1_LPO,1.0)
chaos.setVlin(vlin)
chaos.setVlat(vlat)
chaos.setVrot(vrot)