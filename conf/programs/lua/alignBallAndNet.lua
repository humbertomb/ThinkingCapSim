--io.write("\nalignBallAndNet\n")

ball_pos=0
net1_pos=3
net2_pos=4

PI =  180	
PI2 = 360

DISTANCE	= 450


local ball   = chaos.getLpo(ball_pos)
local net1  = chaos.getLpo(net1_pos)
local net2  = chaos.getLpo(net2_pos)
local info = chaos.getBehaviorInfo ()


if info.isNew > 0 then
     DISTANCE = ball.rho
     chaos.setGlobal("ALIGN_RHO",1,DISTANCE)
else
     DISTANCE = chaos.getGlobal("ALIGN_RHO")
end



if net1.anchored > net2.anchored then
	tar_th  = net1.theta
else
	tar_th  = net2.theta + PI
	if (tar_th > PI) then
		tar_th = tar_th - PI2 
	elseif (tar_th < -PI) then
		tar_th =  tar_th + PI2 
	end
--	io.write ("AlignBallNet1:: using NET2 as reference. Net2=", math.floor(net2.theta*57), " Target=", math.floor(tar_th*57), "\n")
end


--io.write( "net1.anchored =",net1.anchored,", net1.theta =", net1.theta,"\n");
clin =   ball.rho * math.cos(math.rad(ball.theta))
clat =  -ball.rho * math.sin(math.rad(ball.theta))
targetRadium = 670;
sense = -1;



chaos.setNeeded(ball_pos,1.0)
--chaos.setNeeded(net1_pos,1.0)


chaos.setSurround(clin,clat,targetRadium,sense);

