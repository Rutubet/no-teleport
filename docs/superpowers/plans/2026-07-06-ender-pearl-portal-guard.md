# Ender Pearl Portal Guard Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Block dangerous Ender Pearl use on the client before Minecraft sends the use-item packet when the conservative predicted pearl path can intersect a Nether portal.

**Architecture:** Add a testable `EnderPearlPortalGuard` helper that models the vanilla `26.1.2` pearl launch and a conservative swept danger tube. Add a client-only Mixin on `MultiPlayerGameMode.useItem(Player, InteractionHand)` that returns `InteractionResult.FAIL` at method head when the guard says the current Ender Pearl throw is risky.

**Tech Stack:** Java 25, Fabric Loom `1.17.13`, Minecraft `26.1.2`, Fabric Loader `0.19.3`, Sponge Mixin, JUnit Jupiter.

---

## File Structure

- Create `src/main/java/com/rutubet/noteleport/EnderPearlPortalGuard.java`: Owns Ender Pearl item gating, launch velocity calculation, conservative danger-tube simulation, and loaded-world Nether portal checks.
- Create `src/test/java/com/rutubet/noteleport/EnderPearlPortalGuardTest.java`: Unit tests for launch velocity, conservative path scanning, and clear-path behavior.
- Create `src/main/java/com/rutubet/noteleport/mixin/EnderPearlUseMixin.java`: Client-only Mixin that cancels dangerous Ender Pearl use before `ServerboundUseItemPacket` is sent.
- Create `src/test/java/com/rutubet/noteleport/mixin/EnderPearlUseMixinTest.java`: Reflection test for the Mixin injection method signature.
- Modify `src/main/resources/no_teleport.client.mixins.json`: Add `EnderPearlUseMixin` to the client Mixin list.

## Task 1: Add A Tested Ender Pearl Portal Guard

**Files:**
- Create: `src/test/java/com/rutubet/noteleport/EnderPearlPortalGuardTest.java`
- Create: `src/main/java/com/rutubet/noteleport/EnderPearlPortalGuard.java`

- [ ] **Step 1: Write the failing guard tests**

Create `src/test/java/com/rutubet/noteleport/EnderPearlPortalGuardTest.java`:

```java
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
```

- [ ] **Step 2: Run the guard tests and verify they fail**

Run:

```powershell
.\gradlew.bat test --tests com.rutubet.noteleport.EnderPearlPortalGuardTest
```

Expected: compile failure containing `cannot find symbol` for `EnderPearlPortalGuard`.

- [ ] **Step 3: Implement the guard helper**

Create `src/main/java/com/rutubet/noteleport/EnderPearlPortalGuard.java`:

```java
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
```

- [ ] **Step 4: Run the guard tests and verify they pass**

Run:

```powershell
.\gradlew.bat test --tests com.rutubet.noteleport.EnderPearlPortalGuardTest
```

Expected: `BUILD SUCCESSFUL`.

- [ ] **Step 5: Commit the guard helper**

Run:

```powershell
git add src/test/java/com/rutubet/noteleport/EnderPearlPortalGuardTest.java src/main/java/com/rutubet/noteleport/EnderPearlPortalGuard.java
git commit -m "test: add ender pearl portal guard"
```

Expected: commit succeeds.

## Task 2: Add The Client Use-Item Mixin

**Files:**
- Create: `src/test/java/com/rutubet/noteleport/mixin/EnderPearlUseMixinTest.java`
- Create: `src/main/java/com/rutubet/noteleport/mixin/EnderPearlUseMixin.java`
- Modify: `src/main/resources/no_teleport.client.mixins.json`

- [ ] **Step 1: Write the failing Mixin signature test**

Create `src/test/java/com/rutubet/noteleport/mixin/EnderPearlUseMixinTest.java`:

```java
package com.rutubet.noteleport.mixin;

import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.InteractionResult;
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
```

- [ ] **Step 2: Run the Mixin test and verify it fails**

Run:

```powershell
.\gradlew.bat test --tests com.rutubet.noteleport.mixin.EnderPearlUseMixinTest
```

Expected: compile failure containing `cannot find symbol` for `EnderPearlUseMixin`.

- [ ] **Step 3: Implement the Mixin**

Create `src/main/java/com/rutubet/noteleport/mixin/EnderPearlUseMixin.java`:

```java
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
```

- [ ] **Step 4: Register the Mixin config entry**

Update `src/main/resources/no_teleport.client.mixins.json` to:

```json
{
  "required": true,
  "minVersion": "0.8",
  "compatibilityLevel": "JAVA_25",
  "package": "com.rutubet.noteleport.mixin",
  "client": [
    "NetherPortalCollisionMixin",
    "EnderPearlUseMixin"
  ]
}
```

- [ ] **Step 5: Run the Mixin test and verify it passes**

Run:

```powershell
.\gradlew.bat test --tests com.rutubet.noteleport.mixin.EnderPearlUseMixinTest
```

Expected: `BUILD SUCCESSFUL`.

- [ ] **Step 6: Verify resources process with the registered Mixin**

Run:

```powershell
.\gradlew.bat processResources
```

Expected: `BUILD SUCCESSFUL`.

Then run:

```powershell
Get-Content -Raw build\resources\main\no_teleport.client.mixins.json
```

Expected output contains both `"NetherPortalCollisionMixin"` and `"EnderPearlUseMixin"`.

- [ ] **Step 7: Commit the Mixin**

Run:

```powershell
git add src/test/java/com/rutubet/noteleport/mixin/EnderPearlUseMixinTest.java src/main/java/com/rutubet/noteleport/mixin/EnderPearlUseMixin.java src/main/resources/no_teleport.client.mixins.json
git commit -m "feat: block risky ender pearl throws"
```

Expected: commit succeeds.

## Task 3: Full Build And Jar Verification

**Files:**
- Build output: `build/libs/no-teleport-0.1.0.jar`
- Build output: `build/libs/no-teleport-0.1.0-sources.jar`

- [ ] **Step 1: Run the full test suite**

Run:

```powershell
.\gradlew.bat test
```

Expected: `BUILD SUCCESSFUL`.

- [ ] **Step 2: Compile main sources**

Run:

```powershell
.\gradlew.bat compileJava
```

Expected: `BUILD SUCCESSFUL`.

- [ ] **Step 3: Build the jar**

Run:

```powershell
.\gradlew.bat clean build
```

Expected: `BUILD SUCCESSFUL`.

- [ ] **Step 4: Confirm jar outputs exist**

Run:

```powershell
Get-ChildItem build\libs | Select-Object Name,Length
```

Expected output includes:

```text
no-teleport-0.1.0.jar
no-teleport-0.1.0-sources.jar
```

- [ ] **Step 5: Inspect jar contents**

Run:

```powershell
jar tf build\libs\no-teleport-0.1.0.jar | Select-String -Pattern "fabric.mod.json|no_teleport.client.mixins.json|EnderPearlPortalGuard|EnderPearlUseMixin|NetherPortalCollisionMixin|PortalCollision"
```

Expected output includes:

```text
fabric.mod.json
no_teleport.client.mixins.json
com/rutubet/noteleport/EnderPearlPortalGuard.class
com/rutubet/noteleport/mixin/EnderPearlUseMixin.class
com/rutubet/noteleport/mixin/NetherPortalCollisionMixin.class
com/rutubet/noteleport/PortalCollision.class
```

- [ ] **Step 6: Inspect final Mixin metadata inside the jar**

Run:

```powershell
Add-Type -AssemblyName System.IO.Compression.FileSystem
$zip = [System.IO.Compression.ZipFile]::OpenRead((Resolve-Path 'build\libs\no-teleport-0.1.0.jar'))
$entry = $zip.GetEntry('no_teleport.client.mixins.json')
$reader = [System.IO.StreamReader]::new($entry.Open())
Write-Output $reader.ReadToEnd()
$reader.Dispose()
$zip.Dispose()
```

Expected output contains:

```json
"client": [
  "NetherPortalCollisionMixin",
  "EnderPearlUseMixin"
]
```

## Task 4: Manual Gameplay Verification

**Files:**
- Runtime directory: `run/`
- Built jar: `build/libs/no-teleport-0.1.0.jar`

- [ ] **Step 1: Launch the development client**

Run:

```powershell
.\gradlew.bat runClient
```

Expected: Minecraft `26.1.2` launches with Fabric Loader and the `No Teleport` mod active.

- [ ] **Step 2: Verify normal Ender Pearl use away from portals**

In a creative test world:

```text
1. Move at least 30 blocks away from any Nether portal.
2. Throw an Ender Pearl toward open terrain.
3. Confirm the pearl throws and teleports normally.
```

Expected: normal Ender Pearl behavior.

- [ ] **Step 3: Verify direct portal throws are blocked**

In the same world:

```text
1. Build and light a Nether portal.
2. Stand several blocks away.
3. Aim directly at the portal.
4. Right-click with an Ender Pearl.
```

Expected: no Ender Pearl is thrown, and the player remains in place.

- [ ] **Step 4: Verify near-portal throws are conservatively blocked**

In the same world:

```text
1. Stand near the portal.
2. Aim so the crosshair is close to, but not directly on, the portal.
3. Right-click with an Ender Pearl.
```

Expected: near-portal throws may be blocked because the danger tube is intentionally coarse.

- [ ] **Step 5: Verify existing portal collision behavior still works**

In the same world:

```text
1. Walk directly into the Nether portal.
2. Walk into it from the side.
3. Jump while pressing into it.
```

Expected: the player is blocked like the portal is a full solid block, and portal rendering remains normal.

## Self-Review

- Spec coverage: The plan implements pure-client packet-prevention before server pearl spawn, keeps normal away-from-portal pearl use, intentionally blocks near-portal throws with a conservative danger tube, and excludes safe-position rollback or post-teleport rescue.
- Placeholder scan: No unresolved placeholders or vague implementation steps remain.
- Type consistency: The Mixin targets `MultiPlayerGameMode.useItem(Player, InteractionHand)` and returns `InteractionResult.FAIL` through `CallbackInfoReturnable<InteractionResult>`. The helper uses Minecraft `26.1.2` constants from the inspected bytecode: speed `1.5`, gravity `0.03`, air inertia `0.99`, and water inertia `0.8`.
