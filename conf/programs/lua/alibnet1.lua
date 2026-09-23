
ball_pos=0
net1_pos=1
net2_pos=2

PI05 = 90
PI =  180	
PI2 = 360

DEG05   = 4.5837
DEG10   = 9.9695
DEG20   = 20.0535
DEG30   = 28.6479
DEG40   = 39.9925
DEG45   = 44.9772
DEG90   = PI05
DEG180  = PI

DISTANCE	= 450
HEADING	= DEG05

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

obj_rho = ball.rho
obj_th  = ball.theta
if net1.anchored > net2.anchored then
	tar_th  = net1.theta
--	io.write ("AlignBallNet1:: using NET1 as reference. Net1=", math.floor(net1.theta*57), " Target=", math.floor(tar_th*57), "\n")
else
	tar_th  = net2.theta + PI
	if (tar_th > PI) then
		tar_th = tar_th - PI2 
	elseif (tar_th < -PI) then
		tar_th =  tar_th + PI2 
	end
--	io.write ("AlignBallNet1:: using NET2 as reference. Net2=", math.floor(net2.theta*57), " Target=", math.floor(tar_th*57), "\n")
end
	
-- Compute distance error, and limit control actions
edist	= obj_rho - DISTANCE
if edist > 300 then			
	edist = 300
elseif edist < -300 then
	edist = -300
end
	
-- Compute heading error and normalise
delta   = tar_th - obj_th;
if (delta > PI) then
	delta = delta - PI2 
elseif (delta < -PI) then
	delta =  delta + PI2 
end
--io.write ("AlignBallNet1:: Delta=", math.floor(delta*57), " EDist=", edist, " Ball=", math.floor(obj_th*57), "\n")
	
vlin = 0
vlat = 0
vrot = 0

-- Keep distance to ball	
if edist > 100 then
	vlin = 0.75 * edist
else
	vlin = 0
end

-- Keep heading to ball
if math.abs (obj_th) < DEG20 then
	vrot = 0
elseif math.abs (obj_th) < DEG45 then
	vrot = 1.7453292519943295 * obj_th
elseif obj_th >= DEG45 then
	vrot = 70
else
	vrot = -70
end

-- Align ball and net
if math.abs(delta) < HEADING then
	vlat = 0
else
	if delta > 0 then
		vlat = -6.981317007977318 * delta - 50
		vrot = vrot + 0.8726646259971648 * delta

		if vlat < -200 then vlat = -200 end
		if vrot > 70 then vrot = 70 end
	else
   		vlat = -6.981317007977318 * delta + 50
		vrot = vrot + 0.8726646259971648 * delta

		if vlat > 200 then vlat = 200 end
		if vrot < -70 then vrot = -70 end
	end
end

				
chaos.setNeeded(ball_pos,1.0)
chaos.setNeeded(net1_pos,1.0)

--io.write ("AlignBallNet1:: VLin=", math.floor(vlin), " VLat=", math.floor(vlat), " VRot=", math.floor(vrot), "\n")
if vlin == 0 then
	chaos.setVlin(0)
	chaos.setVrot(vrot)
	chaos.setVlat(vlat)
else
	if vrot == 0 then
		chaos.setVlin(vlin)
		chaos.setVrot(0)
		chaos.setVlat(0)
	else
		chaos.setVlin(0)
		chaos.setVrot(vrot)
		chaos.setVlat(0)
	end
end

	chaos.setVlin(0)
	chaos.setVrot(vrot)
	chaos.setVlat(vlat)

