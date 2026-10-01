-- IASF: exercise 1, the same controller as IasfJavaController
--
-- Run once every cycle. It reads the sensor groups of the LPS
-- (tc.getGroups, in metres) and leaves the control action in three globals:
--   vlin  [m/s]   vlat  [m/s]   vrot  [deg/s]
--
-- 20261001 Humberto Martinez

-- Read sensor values
local groups = tc.getGroups ()
local group0 = groups.group0
local group1 = groups.group1
local group2 = groups.group2
local group3 = groups.group3
local group4 = groups.group4

-- Set action values
vlin = 0.30
vlat = 0.0
vrot = 0.0

if group2 == nil then
	-- no groups in the LPS yet: stand still
	vlin = 0.0
elseif group2 < 0.5 then
	vlin = 0.0
	if group1 > group2 then vrot = 45.0 else vrot = -45.0 end
elseif group1 < 0.5 then
	vlin = 0.1
	vrot = -30.0
elseif group0 < 0.5 then
	vlin = 0.2
	vrot = -15.0
elseif group3 < 0.5 then
	vlin = 0.1
	vrot = 30.0
elseif group4 < 0.5 then
	vlin = 0.2
	vrot = 15.0
end
