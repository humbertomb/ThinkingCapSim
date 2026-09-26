-- Behaviour: FaceBall
--
-- 20260926 Humberto Martinez

local ball = chaos.getLpo(chaos.BALL_LPO)

chaos.setScanType(chaos.SCAN_LOW)
chaos.setNeeded(chaos.BALL_LPO,1.0)
chaos.setVelocities(0, 0, 5.5 * ball.theta)