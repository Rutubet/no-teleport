package com.rutubet.noteleport.mixin;

import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import org.junit.jupiter.api.Test;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.lang.reflect.Method;
import java.lang.reflect.Modifier;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class EnderPearlUseMixinTest {
	@Test
	void mixinDeclaresTheUseItemInjectionSignature() throws NoSuchMethodException {
		Method method = EnderPearlUseMixin.class.getDeclaredMethod(
				"noTeleport$guardEnderPearlUse",
				Player.class,
				InteractionHand.class,
				CallbackInfoReturnable.class
		);

		assertEquals(void.class, method.getReturnType());
		assertTrue(Modifier.isPrivate(method.getModifiers()));
	}
}
