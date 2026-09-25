-- Behaviour: GoToBall
--
-- 20060406 Humberto Martinez
-- 20260924 Humberto Martinez

local ANGLE_LARGE = 50
local ANGLE_SMALL = 25

local RHO_MIN = 100
local RHO_SLOWDOWN = 600

local vlin = 0
local vlat = 0
local vrot = 0

-- PID-like controllers
local ball = chaos.getLpo(chaos.BALL_LPO)
if (ball.rho < RHO_SLOWDOWN) then
	-- ***************************
	-- Slow approach to ball
	if ball.rho > RHO_MIN then
		vlin = 0.6 * ball.rho + 50
	end
	if (math.abs(ball.theta) < ANGLE_SMALL) then	
		vrot = 0.9 * ball.theta
	else			
		vrot = 1.23 * ball.theta
	end
else
	-- ***************************
	-- Fast approach to ball
	if (math.abs(ball.theta) < ANGLE_SMALL) then
		vlin = 400
		vrot = 1.6 * ball.theta
	elseif (math.abs(ball.theta) < ANGLE_LARGE) then
		vlin = 250
		vrot = 120 * math.sign(ball.theta)
	else
		vlin = 0
		vrot = 150 * math.sign(ball.theta)
	end
end
	
chaos.setNeeded(chaos.BALL_LPO,1.0)
chaos.setVlin(vlin)
chaos.setVrot(vrot)
chaos.setVlat(vlat)

