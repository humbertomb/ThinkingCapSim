-- Behaviour: LookForLandmark1
--
-- 20260930 Humberto Martinez

local lm1 = chaos.getLpo(chaos.LANDMARK1_LPO)
local info = chaos.getBehaviorInfo ()
local sgn = 0
local vlin = 0
local vlat = 0
local vrot = 0

if info.isNew > 0 or lm1.anchored > 0.8 then
	chaos.setGlobal("LM1_DIRECTION",math.sign (lm1.theta))
end
sgn = chaos.getGlobal("LM1_DIRECTION")

-- PID-like controller
if lm1.anchored > 0.8 and math.abs (lm1.theta) < 30 then
	vrot = 3.5 * lm1.theta
else
	vrot = 65 * sgn
end

chaos.setScanType(chaos.SCAN_FULL)
chaos.setNeeded(chaos.LANDMARK1_LPO,1.0)
chaos.setVlin(vlin)
chaos.setVlat(vlat)
chaos.setVrot(vrot)
