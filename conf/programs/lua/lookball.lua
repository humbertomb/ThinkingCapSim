-- Constants
TIMEOUT=2000

-- Lpo object position
ball_pos=0

-- Some constants
slowTime = 400

-- Program

local ball = chaos.getLpo(ball_pos)
local info = chaos.getBehaviorInfo ()

if info.isNew > 0 then
      sgn = ball.theta / math.abs (ball.theta)
      chaos.setGlobal("BALL_DIRECTION",1,sgn)
end
sgn = chaos.getGlobal("BALL_DIRECTION")

vrot = 0
if info.timer < slowTime then
	vrot = 50 * sgn
else
	vrot = 75 * sgn
end

chaos.setNeeded(ball_pos,1.0)
chaos.setVlin(0)
chaos.setVrot(vrot)
chaos.setVlat(0)

--caguero - Add a timeout for avoid looking for the ball during many time

chaos.setGlobal("LOOK_TIMEOUT",1,0)
if (info.timer > TIMEOUT) then                        
        --Register in a global variable that the behabior must finish
        chaos.setGlobal("LOOK_TIMEOUT",1,1)
end
