
-- Lpo object position
ball_pos=0
net1_pos=1
net2_pos=2

PI05 = 1.5707963268
PI =  3.1415926536	
PI2 = 6.2831853072

DEG10   = 0.174
DEG20   = 0.33
DEG30   = 0.5
DEG40   = 0.698
DEG45   = 0.785
DEG70   = 1.222
DEG90   = PI05
DEG120 = 2.07
DEG180  = PI
--
GOOD_ANCHOR_BALL = 0.95
GOOD_ANCHOR_NET1 = 0.5
GOOD_ANCHOR_NET2 = 0.5
GOOD_ANCHOR_GS = 0.85
--
KICK_RHO = 240
KICK_THETA = DEG10
--
KLIN = 1.2
--

NOKICK = -1
FRONTHEADKICK = 0
LEFTHEADKICK = 1
RIGHTHEADKICK = 2
GRABNTURNLEFT = 3
GRABNTURNRIGHT = 4

-- Get object values
local myPos = chaos.gsGetMyPos()
local ball = chaos.getLpo(ball_pos)
local net1 = chaos.getLpo(net1_pos)
local net2 = chaos.getLpo(net2_pos)
local info = chaos.getBehaviorInfo ()

-- Initialisation
if info.isNew > 0 then
	chaos.setGlobal("KICK_DONE",0,"FALSE")
end

-- Motion control
vlin = 0
vrot = 0
if math.abs(ball.theta) < DEG10 then
	vrot = 0
elseif math.abs(ball.theta) < DEG45 then
	vrot = 60 * ball.theta
else
	vrot = 90 * ball.theta
end

if (math.abs (vrot) < 10) and ((math.abs (vrot) > 5)) then
	vrot = 10 * ball.theta / math.abs (ball.theta)
end
--if math.abs(vrot) > 75 then
--	vrot = 75 * ball.theta / math.abs (ball.theta)
--end

--if ball.rho > 600 then
--	vlin = 370
--elseif ball.rho > 350 then
--	vlin = KLIN * ball.rho - 350
--elseif ball.rho > KICK_RHO+10 then
--	vlin = 70
if ball.rho > 400 then
	vlin = 400
else
	vlin = (ball.rho - 200) * 2.0
--elseif ball.rho > KICK_RHO+10 then
--	vlin = 150
--else
--	vlin = 0
end
 
----------------------------------
-- Kick selection
reason = "NO KICK"
should_kick = false
what_kick = NOKICK
steps = 0
  
--  Calculate where the opponent's net based on the perception of their net
if (net1.anchored > GOOD_ANCHOR_NET1) and (net1.anchored >= net2.anchored-0.05) then 
	reason = "NET1"
	should_kick = true
	if math.abs (net1.theta) < DEG120 then
		what_kick = FRONTHEADKICK
		if net1.theta < -DEG30 then
			what_kick = RIGHTHEADKICK
		elseif net1.theta > DEG30 then
			what_kick = LEFTHEADKICK
		end
	else
		steps = math.ceil(math.abs(net1.theta) / DEG40)

		if(steps > 3) then
			steps = 3
		end
			
		if net1.theta < 0 then
			what_kick = GRABNTURNRIGHT
		else	
			what_kick = GRABNTURNLEFT
		end
	end
	--io.write ("doKick  (NET1):: net1.theta=",net1.theta * 57," kick=",what_kick,"\n")

--  Calculate where the opponent's net based on the perception of my net
elseif (net2.anchored > GOOD_ANCHOR_NET2) and (net2.anchored > net1.anchored) then 
	reason ="NET2"
	should_kick = true
	steps = math.ceil(math.abs((PI - math.abs(net2.theta)) / DEG40))

	if(steps > 3) then
		steps = 3
	end

	if net2.theta < 0 then
		what_kick = GRABNTURNLEFT
	else
		what_kick = GRABNTURNRIGHT
	end
	--io.write ("doKick (NET2):: net2.theta=",net2.theta * 57," kick=",what_kick," steps=",steps," (",steps*45+45,")\n")

--  Calculate where the opponent's net based on my global position
elseif (myPos.quality > GOOD_ANCHOR_GS) then
	reason ="GS"
	should_kick = true
	if math.abs (net1.theta) < DEG120 then
		what_kick = FRONTHEADKICK
		if net1.theta < -DEG30 then
			what_kick = RIGHTHEADKICK
		elseif net1.theta > DEG30 then
			what_kick = LEFTHEADKICK
		end
	else
		th = myPos.theta - PI05
		if (th < -PI) then
			th = th + PI2
		end
		steps = math.ceil(math.abs(th) / DEG40)

		if(steps > 3) then
			steps = 3
		end

		if math.abs(myPos.theta) > DEG90 then
			what_kick = GRABNTURNRIGHT
		else
			what_kick = GRABNTURNLEFT
		end
	end
	--io.write ("doKick (GS):: pos.theta=",myPos.theta * 57," kick=",what_kick," steps=",steps," (",steps*45+45,")\n")
end
        
----------------------------------
-- Kick execution
if ball.anchored < GOOD_ANCHOR_BALL then
	should_kick = false
	vlin = 0
	vrot = 0
	--io.write("doKick:: waiting for condition to kick, ball.anchored=",ball.anchored,"\n")
end
if should_kick and (ball.rho <= KICK_RHO) and (math.abs(ball.theta) <= KICK_THETA) then
--io.write("KICKING!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!\n")
	if what_kick == FRONTHEADKICK then
--		chaos.setSynchroKick("GrabFrontKick")
--		chaos.setSynchroKick("fwdHard")
		chaos.setSynchroKick("FrontHeadKick")
	elseif what_kick == LEFTHEADKICK then
		--chaos.setSynchroKick("LeftHeadKick")
		chaos.setSynchroKick("headL")
	elseif what_kick == RIGHTHEADKICK then
		--chaos.setSynchroKick("RightHeadKick")
		chaos.setSynchroKick("headR")
	elseif what_kick == GRABNTURNLEFT then
		chaos.setSynchroKick("GrabnTurnLeft", steps)
	elseif what_kick == GRABNTURNRIGHT then
		chaos.setSynchroKick("GrabnTurnRight", steps)
	else
		io.write ("doKick:: Wrong kick selected")
	end
--	io.write ("doKick:: Reason=",reason,"\n")
	chaos.setGlobal("KICK_DONE",0,"TRUE")
end

chaos.setNeeded(ball_pos, 1.0) 
if (not should_kick) and ((net1.anchored < GOOD_ANCHOR_NET1) or (net2.anchored < GOOD_ANCHOR_NET2)) then
	io.write ("doKick:: No information to kick. Searching <net1>\n")
	--io.write("net1: rho=",
	vlin = 0
	vrot = 0
	if net1.anchored < net2.anchored then
		chaos.setNeeded(net1_pos, 1.0)
		chaos.setNeeded(ball_pos, 0.0) 	-- Parece que esto va bien	
	else
		chaos.setNeeded(net2_pos, 1.0)
	end
end

chaos.setVlin(vlin)
chaos.setVrot(vrot)
chaos.setVlat(0)
