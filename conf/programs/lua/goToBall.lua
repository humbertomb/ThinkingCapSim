-- Behaviour: GoToBall
--
-- 20060406 Humberto Martinez

--io.write("  [Beh] GoToBall  ")

--io.write("GoToBall\n")

-- Constants and parameters
LPOBALL = 0
ANGLELARGE = 0.85		-- 50-60 grados
ANGLESMALL = 0.40		-- 40 grados
SLOWDOWN = 360

-- Implementation
vlin = 0
vrot = 0
local ball = chaos.getLpo(LPOBALL)
--io.write(" brho = ",ball.rho," btheta = ",ball.theta)

linPos = ball.rho * math.cos(ball.theta)
latPos = - ball.rho * math.sin(ball.theta)
thetaPos = ball.theta

chaos.setTargetPos(linPos, latPos, thetaPos)
chaos.setNeeded(LPOBALL,1.0)


