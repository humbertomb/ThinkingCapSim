-- Lpo object position
ball_pos=chaos.BALL_LPO

-- Some constants
slowTime = 400

-- Program
local ball = chaos.getLpo(ball_pos)
local info = chaos.getBehaviorInfo ()

if info.isNew > 0 or ball.anchored > 0.8 then
	sgn = math.sign (ball.theta)
	chaos.setGlobal("BALL_DIRECTION",1,sgn)
end
sgn = chaos.getGlobal("BALL_DIRECTION")

vrot = 0
if ball.anchored > 0.8 and math.abs (ball.theta) < math.radians(30) then
	vrot = 5.5 * math.deg (ball.theta)
else
	vrot = 75 * sgn
end

chaos.setNeeded(ball_pos,1.0)
chaos.setVlin(0)
chaos.setVlat(0)
chaos.setVrot(vrot)
