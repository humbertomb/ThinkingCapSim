DEG10   = 0.174
DEG30   = 0.5

local ball = chaos.getLpo(0)

vrot = 0
if math.abs(ball.theta) < DEG10 then
	vrot = 0
elseif math.abs(ball.theta) < DEG30 then
	vrot = 30 * ball.theta
else
	vrot = 70 * ball.theta
end

if (math.abs (vrot) < 10) and ((math.abs (vrot) > 5)) then
	vrot = 10 * ball.theta / math.abs (ball.theta)
end
if math.abs(vrot) > 75 then
	vrot = 75 * ball.theta / math.abs (ball.theta)
end

chaos.setNeeded(0, 1.0)
chaos.setVrot(vrot)
chaos.setVlin(0)
chaos.setVlat(0)
