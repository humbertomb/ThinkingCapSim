-- Behaviour: GoToBall
--
-- 20060406 Humberto Martinez
-- 20260924 Humberto Martinez

-- Constants and parameters
LPOBALL = chaos.BALL_LPO
ANGLELARGE = 48.7014		-- 50 degrees
ANGLESMALL = 22.9183		-- 23 degrees
SLOWDOWN = 360

-- Implementation
vlin = 0
vrot = 0
local ball = chaos.getLpo(LPOBALL)
--io.write(" brho = ",ball.rho," btheta = ",ball.theta)

linPos = ball.rho * math.cos(math.rad(ball.theta))
latPos = - ball.rho * math.sin(math.rad(ball.theta))
thetaPos = ball.theta

chaos.setTargetPos(linPos, latPos, thetaPos)
chaos.setNeeded(LPOBALL,1.0)


