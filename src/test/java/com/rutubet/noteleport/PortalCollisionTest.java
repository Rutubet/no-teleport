package com.rutubet.noteleport;

import net.minecraft.world.phys.shapes.Shapes;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertSame;

class PortalCollisionTest {
	@Test
	void portalCollisionShapeIsTheFullBlockShape() {
		assertSame(Shapes.block(), PortalCollision.fullBlockCollisionShape());
	}
}
