-- Behaviour: LookForBall
--
-- 20060406 Humberto Martinez
-- 20260924 Humberto Martinez

local MIN_DRHO = 50
local MIN_DTHETA = 1
local MAX_VROT = 60

local MAX_VLIN = 250
local MAX_VLAT = 150

local ball	= chaos.getLpo(chaos.BALL_LPO)
local net1	= chaos.getLpo(chaos.NET1_LPO)
local net2	= chaos.getLpo(chaos.NET2_LPO)
local info	= chaos.getBehaviorInfo ()

local vlin = 0
local vlat = 0
local vrot = 0

local delta_rho, delta_theta
local align_rho, align_theta

-- Compute distance at invocation
if info.isNew > 0 then
     chaos.setGlobal("ALIGN_RHO",ball.rho)
end
align_rho = chaos.getGlobal("ALIGN_RHO")

-- Use direction of the most recently detected net
if net1.anchored > net2.anchored then
	align_theta = net1.theta
else
	align_theta = math.normdeg (net2.theta + 180)
end

-- Compute distance error, and limit control actions
delta_rho = align_rho - ball.rho

-- Compute heading error and normalise
delta_theta = math.normdeg (align_theta - ball.theta)

-- Keep distance to ball	
if math.abs (delta_rho) > MIN_DRHO then
	vlin = math.limit (-0.75 * delta_rho, -MAX_VLIN, MAX_VLIN)
else
	vlin = 0
end

-- Keep heading to ball
if math.abs (ball.theta) < 3 then
	vrot = 0
elseif math.abs (ball.theta) < 45 then
	vrot = math.limit (1.75 * ball.theta, -MAX_VROT, MAX_VROT)
elseif math.abs (ball.theta) >= 45 then
	vrot = MAX_VROT * math.sign (ball.theta)
end

-- Align ball and net
if math.abs(delta_theta) < MIN_DTHETA then
	vlat = 0
else
	vlat = -math.limit (10 * delta_theta, -MAX_VLAT, MAX_VLAT)
	vrot = math.limit (vrot - 0.9 * delta_theta, -MAX_VROT, MAX_VROT)
end

chaos.setScanType(chaos.SCAN_FULL)
chaos.setNeeded(chaos.BALL_LPO,1.0)
chaos.setNeeded(chaos.NET1_LPO,1.0)
chaos.setVlin(vlin)
chaos.setVlat(vlat)
chaos.setVrot(vrot)