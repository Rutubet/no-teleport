package com.rutubet.noteleport;

import net.minecraft.core.BlockPos;
import net.minecraft.tags.FluidTags;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.Vec3;

public final class EnderPearlPortalGuard {
	static final int SIMULATION_TICKS = 120;
	static final double INITIAL_SPEED = 1.5D;
	static final double GRAVITY = 0.03D;
	static final double AIR_INERTIA = 0.99D;
	static final double WATER_INERTIA = 0.8D;
	static final double DANGER_RADIUS = 2.0D;

	private EnderPearlPortalGuard() {
	}

	public static boolean shouldBlockUse(Level level, Player player, InteractionHand hand) {
		if (level == null || player == null) {
			return false;
		}

		ItemStack stack = player.getItemInHand(hand);
		if (!stack.is(Items.ENDER_PEARL)) {
			return false;
		}

		Vec3 start = player.getEyePosition();
		Vec3 velocity = velocityFromRotation(player.getXRot(), player.getYRot(), player.getKnownMovement(), player.onGround());
		return pathMayTouchPortal(
				start,
				velocity,
				pos -> level.getBlockState(pos).is(Blocks.NETHER_PORTAL),
				pos -> level.getFluidState(pos).is(FluidTags.WATER)
		);
	}

	static Vec3 velocityFromRotation(float xRot, float yRot, Vec3 knownMovement, boolean onGround) {
		double x = -Mth.sin((double) (yRot * 0.017453292F)) * Mth.cos((double) (xRot * 0.017453292F));
		double y = -Mth.sin((double) (xRot * 0.017453292F));
		double z = Mth.cos((double) (yRot * 0.017453292F)) * Mth.cos((double) (xRot * 0.017453292F));
		Vec3 velocity = new Vec3(x, y, z).normalize().scale(INITIAL_SPEED);
		return velocity.add(knownMovement.x, onGround ? 0.0D : knownMovement.y, knownMovement.z);
	}

	static boolean pathMayTouchPortal(Vec3 start, Vec3 velocity, BlockPositionPredicate portalPredicate, BlockPositionPredicate waterPredicate) {
		Vec3 position = start;
		Vec3 movement = velocity;

		for (int tick = 0; tick < SIMULATION_TICKS; tick++) {
			double inertia = waterPredicate.test(BlockPos.containing(position)) ? WATER_INERTIA : AIR_INERTIA;
			movement = movement.add(0.0D, -GRAVITY, 0.0D).scale(inertia);
			Vec3 nextPosition = position.add(movement);

			if (segmentTubeTouchesPortal(position, nextPosition, DANGER_RADIUS, portalPredicate)) {
				return true;
			}

			position = nextPosition;
		}

		return false;
	}

	static boolean segmentTubeTouchesPortal(Vec3 from, Vec3 to, double radius, BlockPositionPredicate portalPredicate) {
		int minX = Mth.floor(Math.min(from.x, to.x) - radius);
		int minY = Mth.floor(Math.min(from.y, to.y) - radius);
		int minZ = Mth.floor(Math.min(from.z, to.z) - radius);
		int maxX = Mth.floor(Math.max(from.x, to.x) + radius);
		int maxY = Mth.floor(Math.max(from.y, to.y) + radius);
		int maxZ = Mth.floor(Math.max(from.z, to.z) + radius);

		for (BlockPos pos : BlockPos.betweenClosed(minX, minY, minZ, maxX, maxY, maxZ)) {
			if (portalPredicate.test(pos)) {
				return true;
			}
		}

		return false;
	}

	@FunctionalInterface
	interface BlockPositionPredicate {
		boolean test(BlockPos pos);
	}
}
