-- Constants
--TIMEOUT=2000



-- Lpo object position
ball_pos=0

-- Some constants
--slowTime = 400
slowTime = 0

-- Program

local ball = chaos.getLpo(ball_pos)
local info = chaos.getBehaviorInfo ()


if info.isNew > 0 then
	if ball.theta > 0 then
		sgn = 1
	else
		sgn = -1
	end
    chaos.setGlobal("BALL_DIRECTION",1,sgn)
end
sgn = chaos.getGlobal("BALL_DIRECTION")


--if info.timer < slowTime then	
	--vrot = ball.theta
--else
	vrot = -90 * sgn
--end


chaos.setNeeded(ball_pos,1.0)
chaos.setVelocities(0,0,vrot)

--caguero - Add a timeout for avoid looking for the ball during many time
--jj		 - timer commented (not tested in nao)

--chaos.setGlobal("LOOK_TIMEOUT",1,0)
--if (info.timer > TIMEOUT) then                        
--       --Register in a global variable that the behabior must finish
--        chaos.setGlobal("LOOK_TIMEOUT",1,1)
--end
