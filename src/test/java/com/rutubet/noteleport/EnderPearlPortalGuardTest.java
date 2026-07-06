package com.rutubet.noteleport;

import net.minecraft.core.BlockPos;
import net.minecraft.world.phys.Vec3;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class EnderPearlPortalGuardTest {
	private static final double EPSILON = 1.0E-6D;

	@Test
	void velocityFromRotationMatchesForwardPearlSpeed() {
		Vec3 velocity = EnderPearlPortalGuard.velocityFromRotation(0.0F, 0.0F, Vec3.ZERO, true);

		assertEquals(0.0D, velocity.x, EPSILON);
		assertEquals(0.0D, velocity.y, EPSILON);
		assertEquals(1.5D, velocity.z, EPSILON);
	}

	@Test
	void velocityFromRotationAddsHorizontalKnownMovement() {
		Vec3 velocity = EnderPearlPortalGuard.velocityFromRotation(0.0F, 0.0F, new Vec3(0.25D, 0.5D, -0.25D), true);

		assertEquals(0.25D, velocity.x, EPSILON);
		assertEquals(0.0D, velocity.y, EPSILON);
		assertEquals(1.25D, velocity.z, EPSILON);
	}

	@Test
	void velocityFromRotationKeepsAirborneVerticalKnownMovement() {
		Vec3 velocity = EnderPearlPortalGuard.velocityFromRotation(0.0F, 0.0F, new Vec3(0.0D, 0.5D, 0.0D), false);

		assertEquals(0.5D, velocity.y, EPSILON);
	}

	@Test
	void dangerTubeBlocksPortalNearThePredictedSegment() {
		BlockPos nearPortal = new BlockPos(2, 2, 0);

		boolean blocked = EnderPearlPortalGuard.pathMayTouchPortal(
				new Vec3(0.0D, 0.0D, 0.0D),
				new Vec3(1.0D, 0.0D, 0.0D),
				nearPortal::equals,
				pos -> false
		);

		assertTrue(blocked);
	}

	@Test
	void dangerTubeAllowsClearThrowWhenNoPortalIsFound() {
		boolean blocked = EnderPearlPortalGuard.pathMayTouchPortal(
				new Vec3(0.0D, 0.0D, 0.0D),
				new Vec3(1.0D, 0.0D, 0.0D),
				pos -> false,
				pos -> false
		);

		assertFalse(blocked);
	}

	@Test
	void segmentScanCoversFastMovementBetweenTickPositions() {
		BlockPos portalBetweenPositions = new BlockPos(4, 0, 0);

		boolean blocked = EnderPearlPortalGuard.segmentTubeTouchesPortal(
				new Vec3(0.0D, 0.0D, 0.0D),
				new Vec3(8.0D, 0.0D, 0.0D),
				0.5D,
				portalBetweenPositions::equals
		);

		assertTrue(blocked);
	}
}
