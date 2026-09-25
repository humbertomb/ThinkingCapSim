-- Behaviour: LookForNet1
--
-- 20260925 Humberto Martinez

local net1 = chaos.getLpo(chaos.NET1_LPO)
local info = chaos.getBehaviorInfo ()
local sgn = 0
local vlin = 0
local vlat = 0
local vrot = 0

if info.isNew > 0 or net1.anchored > 0.8 then
	chaos.setGlobal("NET1_DIRECTION",math.sign (net1.theta))
end
sgn = chaos.getGlobal("NET1_DIRECTION")

-- PID-like controller
if net1.anchored > 0.8 and math.abs (net1.theta) < 30 then
	vrot = 5.5 * net1.theta
else
	vrot = 75 * sgn
end

chaos.setNeeded(chaos.BALL_LPO,1.0)
chaos.setVlin(vlin)
chaos.setVlat(vlat)
chaos.setVrot(vrot)
