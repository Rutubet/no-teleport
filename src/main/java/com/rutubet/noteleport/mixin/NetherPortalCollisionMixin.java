package com.rutubet.noteleport.mixin;

import com.rutubet.noteleport.PortalCollision;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(BlockBehaviour.BlockStateBase.class)
public abstract class NetherPortalCollisionMixin {
	@Inject(
			method = "getCollisionShape(Lnet/minecraft/world/level/BlockGetter;Lnet/minecraft/core/BlockPos;Lnet/minecraft/world/phys/shapes/CollisionContext;)Lnet/minecraft/world/phys/shapes/VoxelShape;",
			at = @At("HEAD"),
			cancellable = true
	)
	private void noTeleport$getCollisionShape(BlockGetter level, BlockPos pos, CollisionContext context, CallbackInfoReturnable<VoxelShape> cir) {
		if (noTeleport$isNetherPortal()) {
			cir.setReturnValue(PortalCollision.fullBlockCollisionShape());
		}
	}

	@Inject(method = "hasLargeCollisionShape", at = @At("HEAD"), cancellable = true)
	private void noTeleport$hasLargeCollisionShape(CallbackInfoReturnable<Boolean> cir) {
		if (noTeleport$isNetherPortal()) {
			cir.setReturnValue(true);
		}
	}

	@Unique
	private boolean noTeleport$isNetherPortal() {
		return ((BlockState) (Object) this).is(Blocks.NETHER_PORTAL);
	}
}
