-- Behaviour: AlignBallAndNet1
--
-- Puts the robot behind the ball, looking at it, with the ball between the
-- robot and net 1 and as far from the ball as it was when the behaviour began
-- (ALIGN_RHO). The robot goes around the ball as on a clock: vlin takes it
-- nearer or further (it always looks at the ball, so forward is towards it),
-- vlat takes it round, by the shorter way, and vrot keeps the ball straight
-- ahead, turning beforehand by what the robot's own walking turns the ball
-- away. Going round the ball looking at it, the robot never walks into it.
--
-- 20060406 Humberto Martinez
-- 20260924 Humberto Martinez

local MIN_BALL_RHO = 250		-- what ALIGN_RHO may be (mm): nearer, the robot touches the ball (about 240)
local MAX_BALL_RHO = 350
local ORBIT_RHO = 300			-- going round the ball, no nearer than this (mm): the legs are not to touch it

local MIN_DRHO = 20				-- near enough (mm)
local MIN_DARC = 8 				-- round enough: how far from the line, along the way round (mm)
local MIN_DTHETA = 2			-- looking at the ball (deg)

local K_RHO = 1.5				-- how fast the errors are corrected (1/s)
local K_ARC = 2.0
local K_ROT = 2.0

local MAX_VLIN = 250			-- mm/s
local MAX_VLAT = 150			-- mm/s
local MAX_VROT = 60				-- deg/s

local ball	= chaos.getLpo(chaos.BALL_LPO)
local net1	= chaos.getLpo(chaos.NET1_LPO)
local net2	= chaos.getLpo(chaos.NET2_LPO)
local info	= chaos.getBehaviorInfo ()

local vlin, vlat, vrot = 0, 0, 0

-- The distance to keep: the one to the ball when the behaviour began, or
-- when the ball is first seen, if it was not then (0: not yet)
if info.isNew > 0 then
	chaos.setGlobal("ALIGN_RHO", 0)
end
local align_rho = chaos.getGlobal("ALIGN_RHO") or 0
if (align_rho == 0) and (ball.anchored > 0.5) and (ball.rho > 0) then
	align_rho = math.limit (ball.rho, MIN_BALL_RHO, MAX_BALL_RHO)
	chaos.setGlobal("ALIGN_RHO", align_rho)
end

local bx, by, rho = ball.x, ball.y, ball.rho

if (align_rho > 0) and (rho > 1) then
	-- Which way from the ball net 1 is: towards net 1, or away from net 2,
	-- whichever was seen last (no net: the robot stays on the side it is)
	local dx, dy = bx, by
	if (net1.anchored > 0) and (net1.anchored >= net2.anchored) then
		dx, dy = net1.x - bx, net1.y - by
	elseif net2.anchored > 0 then
		dx, dy = bx - net2.x, by - net2.y
	end

	-- Where the robot is around the ball, and where it is to be: on the other
	-- side of the ball from net 1
	local phi_now	= math.atan2 (-by, -bx)
	local phi_goal	= math.atan2 (-dy, -dx)
	local dphi		= math.rad (math.normdeg (math.deg (phi_goal - phi_now)))

	-- How far: going round, a little further off, not to touch the ball
	local rho_goal = align_rho
	if align_rho < ORBIT_RHO then
		rho_goal = align_rho + (ORBIT_RHO - align_rho) * math.min (1, math.abs (math.deg (dphi)) / 45)
	end

	local drho	= rho - rho_goal				-- > 0: too far
	local darc	= rho * dphi					-- how far round, along the circle (mm)

	if math.abs (drho) < MIN_DRHO then drho = 0 end
	if math.abs (darc) < MIN_DARC then darc = 0 end

	-- Towards the ball and round it, whichever way the robot is turned
	local ux, uy = bx / rho, by / rho			-- towards the ball
	local tx, ty = -math.sin (phi_now), math.cos (phi_now)		-- round it, phi growing
	local vr = K_RHO * drho
	local vt = K_ARC * darc

	vlin = vr * ux + vt * tx
	vlat = vr * uy + vt * ty

	-- No faster than the robot goes, keeping the way it goes
	local s = math.max (1, math.abs (vlin) / MAX_VLIN, math.abs (vlat) / MAX_VLAT)
	vlin = vlin / s
	vlat = vlat / s

	-- Looking at the ball: what the walk turns it away, and what it is off already
	local vff = math.deg ((by * vlin - bx * vlat) / (rho * rho))
	local dth = ball.theta
	if math.abs (dth) < MIN_DTHETA then dth = 0 end
	vrot = math.limit (vff + K_ROT * dth, -MAX_VROT, MAX_VROT)

	-- Turned too far from the ball to walk at it: turn first
	if math.abs (ball.theta) > 60 then
		vlin = 0
		vlat = 0
	end
end

chaos.setScanType(chaos.SCAN_FULL)
chaos.setNeeded(chaos.BALL_LPO,1.0)
chaos.setNeeded(chaos.NET1_LPO,1.0)
chaos.setVlin(vlin)
chaos.setVlat(vlat)
chaos.setVrot(vrot)
