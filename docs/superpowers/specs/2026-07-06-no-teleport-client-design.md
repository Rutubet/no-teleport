# No Teleport Client Mod Design

## Summary

Build a pure client-side Fabric mod for Minecraft 26.1.2 that prevents players from entering Nether portal blocks on multiplayer servers. The mod makes Nether portal blocks behave as full solid collision cubes on the client. Players experience the portal as an invisible wall while the portal still renders normally.

## Goals

- Target Minecraft `26.1.2`.
- Ship as a client-only Fabric mod.
- Work on multiplayer servers without any server-side mod.
- Prevent normal Nether portal teleportation by keeping the player's client position outside portal blocks.
- Preserve the visual appearance of Nether portals.
- Keep the first implementation minimal and easy to verify.

## Non-Goals

- Do not build a server-side mod.
- Do not require a server plugin or server configuration.
- Do not cancel dimension-change packets after the server has already teleported the player.
- Do not implement safe-position caching, rollback, or rescue behavior for players who are already inside a portal block.
- Do not add a configuration screen for the MVP.

## Version And Tooling

- Minecraft version: `26.1.2`.
- Fabric Loader: use the latest stable loader available for `26.1.2`, currently `0.19.3`.
- Fabric API: avoid it unless implementation proves it is necessary.
- Mappings: prefer a Loom setup that can build against available `26.1.2` mappings. Yarn metadata for `26.1.2` is not currently available from Fabric, so implementation should verify whether official Mojang mappings or Intermediary names are the cleanest working option.

## Behavior

When the mod is installed on the client:

1. Nether portals still appear and animate normally.
2. The local player cannot walk into Nether portal blocks during normal movement.
3. The collision should feel like hitting a full solid block.
4. The client sends normal movement updates to the server from outside the portal.
5. Since the player does not stay inside the portal block, the server should not accumulate normal portal teleport progress.

## Architecture

The mod contains only client-side behavior:

- `fabric.mod.json` declares the mod as client-side only.
- A Mixin config is loaded only on the client.
- One Mixin targets Minecraft block-state collision queries.
- The Mixin returns a full cube for client-side collision queries when the queried block state is `Blocks.NETHER_PORTAL`.

The mod does not add gameplay state, networking channels, commands, or persistent config.

## Collision Design

The Mixin should intercept the method Minecraft uses to ask a block state for its collision shape. For the MVP, it returns a full-cube voxel shape when the state is `Blocks.NETHER_PORTAL`.

Minecraft `26.1.2` registers Nether portals with `noCollision`, so implementation should also ensure portal block states are not skipped by collision traversal optimizations that check for a large collision shape before asking for the actual shape.

The implementation should not change:

- outline shape used for selection unless required by the target method layout,
- render shape,
- block state,
- portal block placement,
- portal block activation.

If the target version separates collision shape from outline/render shape, only collision shape should be changed.

## Multiplayer Boundary

Minecraft servers are authoritative for dimension changes. A client-only mod cannot guarantee cancellation after the server has already decided to teleport the player. This MVP avoids that case by preventing normal client movement into the portal block in the first place.

The expected multiplayer behavior is that the client remains outside the portal, sends normal outside-portal movement packets, and therefore does not satisfy the server's ordinary "standing in a Nether portal" condition.

## Testing Plan

Verification should cover:

- The project builds for Minecraft `26.1.2`.
- The generated jar declares itself as client-only.
- A client can launch with the mod installed.
- In a local test world, the player cannot walk into a Nether portal block.
- On a multiplayer server, the player is blocked at the portal and is not teleported through normal walking.
- Portal rendering remains unchanged.

## Risks

- The exact Nether portal block method names may differ because `26.1.2` has no Fabric Yarn entries at the time of design. Implementation must verify mappings during setup.
- Some servers or mods may teleport players by custom logic that does not depend on vanilla portal collision or vanilla portal standing time.
- If a server forcibly places or teleports the player into a portal block, this MVP intentionally does not rescue them.
