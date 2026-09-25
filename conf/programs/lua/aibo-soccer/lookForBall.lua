-- Behaviour: LookForBall
--
-- 20060406 Humberto Martinez
-- 20260924 Humberto Martinez

local ball = chaos.getLpo(chaos.BALL_LPO)
local info = chaos.getBehaviorInfo ()

if info.isNew > 0 or ball.anchored > 0.8 then
	chaos.setGlobal("BALL_DIRECTION",1,math.sign (ball.theta))
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
chaos.setVlin(0)
chaos.setVlat(0)
chaos.setVrot(vrot)
