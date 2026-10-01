-- Behaviour: LookForLandmark2
--
-- 20260930 Humberto Martinez

local lm2 = chaos.getLpo(chaos.LANDMARK2_LPO)
local info = chaos.getBehaviorInfo ()
local sgn = 0
local vlin = 0
local vlat = 0
local vrot = 0

if info.isNew > 0 or lm2.anchored > 0.8 then
	chaos.setGlobal("LM2_DIRECTION",math.sign (lm2.theta))
end
sgn = chaos.getGlobal("LM2_DIRECTION")

-- PID-like controller
if lm2.anchored > 0.8 and math.abs (lm2.theta) < 30 then
	vrot = 3.5 * lm2.theta
else
	vrot = 65 * sgn
end

chaos.setScanType(chaos.SCAN_HIGH)
chaos.setNeeded(chaos.LANDMARK2_LPO,1.0)
chaos.setVlin(vlin)
chaos.setVlat(vlat)
chaos.setVrot(vrot)
