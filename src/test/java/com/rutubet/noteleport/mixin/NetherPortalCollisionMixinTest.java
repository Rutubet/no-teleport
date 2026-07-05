package com.rutubet.noteleport.mixin;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.phys.shapes.CollisionContext;
import org.junit.jupiter.api.Test;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.lang.reflect.Method;
import java.lang.reflect.Modifier;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class NetherPortalCollisionMixinTest {
	@Test
	void mixinDeclaresThePortalCollisionInjectionSignature() throws NoSuchMethodException {
		Method method = NetherPortalCollisionMixin.class.getDeclaredMethod(
				"noTeleport$getCollisionShape",
				BlockGetter.class,
				BlockPos.class,
				CollisionContext.class,
				CallbackInfoReturnable.class
		);

		assertEquals(void.class, method.getReturnType());
		assertTrue(Modifier.isPrivate(method.getModifiers()));
	}

	@Test
	void mixinDeclaresTheLargeCollisionShapeInjectionSignature() throws NoSuchMethodException {
		Method method = NetherPortalCollisionMixin.class.getDeclaredMethod(
				"noTeleport$hasLargeCollisionShape",
				CallbackInfoReturnable.class
		);

		assertEquals(void.class, method.getReturnType());
		assertTrue(Modifier.isPrivate(method.getModifiers()));
	}
}
