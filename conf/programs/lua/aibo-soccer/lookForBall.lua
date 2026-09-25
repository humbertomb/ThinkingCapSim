-- Behaviour: LookForBall
--
-- 20060406 Humberto Martinez
-- 20260924 Humberto Martinez

local ball = chaos.getLpo(chaos.BALL_LPO)
local info = chaos.getBehaviorInfo ()
local sgn = 0
local vlin = 0
local vlat = 0
local vrot = 0

if info.isNew > 0 or ball.anchored > 0.8 then
	chaos.setGlobal("BALL_DIRECTION",math.sign (ball.theta))
end
sgn = chaos.getGlobal("BALL_DIRECTION")

-- PID-like controller
vrot = 0
if ball.anchored > 0.8 and math.abs (ball.theta) < 30 then
	vrot = 5.5 * ball.theta
else
	vrot = 75 * sgn
end

chaos.setNeeded(chaos.BALL_LPO,1.0)
chaos.setVlin(vlin)
chaos.setVlat(vlat)
chaos.setVrot(vrot)
