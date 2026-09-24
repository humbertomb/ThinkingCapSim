-- Behaviour: LookForBall
--
-- 20060406 Humberto Martinez
-- 20260924 Humberto Martinez

MAX_RHO = 800
MIN_RHO = 300
MIN_THETA = 5

MAX_VROT = 70
MAX_VLAT = 200

local ball	= chaos.getLpo(chaos.BALL_LPO)
local net1	= chaos.getLpo(chaos.NET1_LPO)
local net2	= chaos.getLpo(chaos.NET2_LPO)
local info	= chaos.getBehaviorInfo ()

-- Compute distance at invocation
if info.isNew > 0 then
     chaos.setGlobal("ALIGN_RHO",1,ball.rho)
end
ALIGN_RHO = chaos.getGlobal("ALIGN_RHO")

-- Use direction of the most recently detected net
if net1.anchored > net2.anchored then
	ALIGN_THETA = net1.theta
else
	ALIGN_THETA = math.normdeg (net2.theta + PI)
end

-- Compute distance error, and limit control actions
delta_rho = math.limit (ball.rho - ALIGN_RHO, -MAX_RHO, MAX_RHO)

-- Compute heading error and normalise
delta_theta = math.normdeg (ALIGN_THETA - ball.theta);

-- Keep distance to ball	
if delta_rho > MIN_RHO then
	vlin = 0.75 * delta_rho
else
	vlin = 0
end

-- Keep heading to ball
if math.abs (ball.theta) < 20 then
	vrot = 0
elseif math.abs (ball.theta) < 45 then
	vrot = math.limit (1.75 * ball.theta, -MAX_VROT, MAX_VROT)
elseif math.abs (ball.theta) >= 45 then
	vrot = MAX_VROT * math.sign (ball.theta)
end

-- Align ball and net
if math.abs(delta_theta) < MIN_THETA then
	vlat = 0
else
	vlat = math.limit (7 * delta_theta + 50*math.sign(delta_theta), -MAX_VLAT, MAX_VLAT)
	vrot = math.limit (vrot + 0.9 * delta_theta, -MAX_VROT, MAX_VROT)
end

chaos.setNeeded(chaos.BALL_LPO,1.0)
chaos.setNeeded(chaos.NET1_LPO,1.0)
chaos.setVlin(vlin)
chaos.setVlat(vlat)
chaos.setVrot(vrot)