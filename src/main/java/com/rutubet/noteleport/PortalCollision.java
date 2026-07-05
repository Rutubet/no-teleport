package com.rutubet.noteleport;

import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

public final class PortalCollision {
	private PortalCollision() {
	}

	public static VoxelShape fullBlockCollisionShape() {
		return Shapes.block();
	}
}
