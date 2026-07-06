package com.rutubet.noteleport.mixin;

import com.rutubet.noteleport.EnderPearlPortalGuard;
import net.minecraft.client.multiplayer.MultiPlayerGameMode;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(MultiPlayerGameMode.class)
public abstract class EnderPearlUseMixin {
	@Inject(method = "useItem", at = @At("HEAD"), cancellable = true)
	private void noTeleport$guardEnderPearlUse(Player player, InteractionHand hand, CallbackInfoReturnable<InteractionResult> cir) {
		if (EnderPearlPortalGuard.shouldBlockUse(player.level(), player, hand)) {
			cir.setReturnValue(InteractionResult.FAIL);
		}
	}
}
