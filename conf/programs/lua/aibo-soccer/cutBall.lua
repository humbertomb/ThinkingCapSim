-- Behaviour: CutBall
--
-- Puts the robot between the ball and its own net (net 2), looking at the
-- ball, using only turns (vrot) and steps sideways (vlat): it never walks
-- forwards or backwards. Looking at the ball, a step sideways takes the
-- robot round the ball, at the distance it is from it, so the robot goes
-- round it (by the shorter way) until it is on the line from the ball to its
-- own net, and vrot keeps the ball straight ahead, turning beforehand by what
-- the step sideways turns the ball away.
--
-- Where its own net is, is told by the better anchored of the two nets: net 2
-- itself, or net 1, whose opposite way from the ball is the way to net 2 (the
-- nets are at the two ends of the field). With neither, or without the ball,
-- the robot only keeps looking at the ball.
--
-- 20261006 Humberto Martinez

local MIN_ANCHOR = 0.1			-- an object anchored less than this is not seen
local MIN_DARC = 15				-- on the line enough: how far from it, round the ball (mm)
local MIN_DTHETA = 2			-- looking at the ball (deg)
local MAX_TURNED = 45			-- the ball further to a side than this (deg): turn to it before stepping

local K_ARC = 2.0				-- how fast the errors are corrected (1/s)
local K_ROT = 2.0

local MAX_VLAT = 150			-- mm/s
local MAX_VROT = 60				-- deg/s

local ball	= chaos.getLpo(chaos.BALL_LPO)
local net1	= chaos.getLpo(chaos.NET1_LPO)
local net2	= chaos.getLpo(chaos.NET2_LPO)

local vlat, vrot = 0, 0
local bx, by, rho = ball.x, ball.y, ball.rho

if (ball.anchored >= MIN_ANCHOR) and (rho > 1) then
	-- Which way from the ball its own net is: towards net 2, or away from net 1,
	-- by the better anchored of the two (none: the robot stays where it is)
	local dx, dy = 0, 0
	local net = nil
	if (net2.anchored >= MIN_ANCHOR) and (net2.anchored >= net1.anchored) then
		dx, dy = net2.x - bx, net2.y - by
		net = chaos.NET2_LPO
	elseif net1.anchored >= MIN_ANCHOR then
		dx, dy = bx - net1.x, by - net1.y
		net = chaos.NET1_LPO
	end

	if (dx ~= 0) or (dy ~= 0) then
		-- Where the robot is round the ball, and where it is to be: on the side of
		-- the ball of its own net
		local phi_now	= math.atan2 (-by, -bx)
		local phi_goal	= math.atan2 (dy, dx)
		local dphi		= math.rad (math.normdeg (math.deg (phi_goal - phi_now)))
		local darc		= rho * dphi				-- how far round, along the circle (mm)

		if math.abs (darc) < MIN_DARC then darc = 0 end

		-- Round the ball, phi growing, is sideways for a robot that looks at it:
		-- only that part of it is walked (no vlin)
		vlat = K_ARC * darc * math.cos (phi_now)
		vlat = math.limit (vlat, -MAX_VLAT, MAX_VLAT)

		-- Turned too far from the ball to step round it: turn first
		if math.abs (ball.theta) > MAX_TURNED then vlat = 0 end

		chaos.setNeeded(net,1.0)
	end

	-- Looking at the ball: what the step sideways turns it away, and what it is off already
	local vff = math.deg (-bx * vlat / (rho * rho))
	local dth = ball.theta
	if math.abs (dth) < MIN_DTHETA then dth = 0 end
	vrot = math.limit (vff + K_ROT * dth, -MAX_VROT, MAX_VROT)
end

chaos.setScanType(chaos.SCAN_FULL)
chaos.setNeeded(chaos.BALL_LPO,1.0)
chaos.setVlin(0)
chaos.setVlat(vlat)
chaos.setVrot(vrot)
