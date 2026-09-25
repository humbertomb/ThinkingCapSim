-- Behaviour: LookForNet2
--
-- 20260925 Humberto Martinez

local net2 = chaos.getLpo(chaos.NET2_LPO)
local info = chaos.getBehaviorInfo ()
local sgn = 0
local vlin = 0
local vlat = 0
local vrot = 0

if info.isNew > 0 or net2.anchored > 0.8 then
	chaos.setGlobal("NET2_DIRECTION",math.sign (net2.theta))
end
sgn = chaos.getGlobal("NET2_DIRECTION")

-- PID-like controller
if net2.anchored > 0.8 and math.abs (net2.theta) < 30 then
	vrot = 5.5 * net2.theta
else
	vrot = 75 * sgn
end

chaos.setNeeded(chaos.BALL_LPO,1.0)
chaos.setVlin(vlin)
chaos.setVlat(vlat)
chaos.setVrot(vrot)
