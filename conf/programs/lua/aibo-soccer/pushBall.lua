-- Behaviour: GoToBall
--
-- 20260925 Humberto Martinez

local ball = chaos.getLpo(chaos.BALL_LPO)

-- PID-like controllers
local vlin = 0.75 * ball.rho + 100
local vrot = 0.9 * ball.theta
local vlat = 0
	
chaos.setNeeded(chaos.BALL_LPO,1.0)
chaos.setVlin(vlin)
chaos.setVrot(vrot)
chaos.setVlat(vlat)

