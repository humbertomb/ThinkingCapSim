-- Program
local ball = chaos.getLpo(chaos.BALL_LPO)
local info = chaos.getBehaviorInfo ()

if info.isNew > 0 or ball.anchored > 0.8 then
	sgn = math.sign (ball.theta)
	chaos.setGlobal("BALL_DIRECTION",1,sgn)
end
sgn = chaos.getGlobal("BALL_DIRECTION")

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
