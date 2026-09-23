-- Behaviour: GoToBall
--
-- 20060406 Humberto Martinez

--io.write("  [Beh] GoToBall  ")

-- Constants and parameters
LPOBALL = 0
ANGLELARGE = 48.7014		-- degrees, as every angle here
ANGLESMALL = 22.9183		-- degrees, as every angle here
SLOWDOWN = 360

-- Implementation
vlin = 0
vrot = 0
local ball = chaos.getLpo(LPOBALL)
if (ball.rho < SLOWDOWN) then
	-- ***************************
	-- Slow approach to ball
	vlin = 0.6 * ball.rho -100
	if (math.abs(ball.theta) < ANGLESMALL) then		-- small angle
		vrot = 0.8726646259971648 * ball.theta
	else				-- large angle
		vrot = 1.2217304763960306 * ball.theta
	end


else
	-- ***************************
	-- Fast approach to ball
	if (math.abs(ball.theta) < ANGLESMALL) then		-- small angle
		vlin = 400
		vrot = 1.5707963267948966 * ball.theta
	elseif (math.abs(ball.theta) < ANGLELARGE) then	-- medium angle
		vlin = 250
		vrot = 120 * ball.theta / math.abs(ball.theta)
	else				-- large angle
		vlin = 0
		vrot = 150 * ball.theta / math.abs(ball.theta)
	end
end
	
if (math.abs (vrot) < 10) and ((math.abs (vrot) > 5)) then
	vrot = 10 * ball.theta / math.abs (ball.theta)
end
--if math.abs(vrot) > 75 then
--	vrot = 75 * ball.theta / math.abs (ball.theta)
--end

--io.write(" brho = ",ball.rho," btheta = ",ball.theta)
--io.write(" vlin = ",vlin," vrot = ",vrot,"\n")

chaos.setNeeded(LPOBALL,1.0)
chaos.setVlin(vlin)
chaos.setVrot(vrot)
chaos.setVlat(0)

