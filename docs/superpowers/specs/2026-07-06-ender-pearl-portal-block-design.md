# Ender Pearl Portal Block Design

## Summary

Extend the pure client-side Fabric mod for Minecraft `26.1.2` so a player cannot use an Ender Pearl in a way that can reasonably enter a Nether portal. The mod should keep normal Ender Pearl use available when the throw is clearly away from Nether portals, but it should prefer false positives over missed dangerous throws.

The feature is a single-layer prevention system: predict a conservative Ender Pearl danger tube before the client sends the use-item packet. If the danger tube intersects any loaded Nether portal block, cancel the use. The feature does not add post-teleport rollback, safe-position rescue, or rubber-banding.

## Goals

- Remain a pure client-side Fabric mod.
- Work on multiplayer servers without a server mod or plugin.
- Prevent the client from sending dangerous Ender Pearl use packets near Nether portals.
- Preserve normal Ender Pearl use when the predicted throw is clearly not near a Nether portal.
- Use conservative prediction settings so near-portal throws are blocked rather than risk a missed portal entry.
- Keep the existing Nether portal collision wall behavior unchanged.

## Non-Goals

- Do not create a server-side mod.
- Do not change server projectile physics.
- Do not modify Ender Pearl entities after the server has spawned them.
- Do not implement safe-position caching, rollback, or rescue behavior after a server teleport.
- Do not try to bypass or fight server anticheat.
- Do not add an in-game configuration screen for this iteration.

## Minecraft 26.1.2 Interaction Chain

In Minecraft `26.1.2`, client item use goes through `MultiPlayerGameMode.useItem(player, hand)`. The client starts prediction, constructs a `ServerboundUseItemPacket`, performs local item-use prediction, and then sends that packet to the server.

On the server, `ServerGamePacketListenerImpl.handleUseItem(packet)` reads the player's current hand item and calls server item use logic. For Ender Pearls, `EnderpearlItem.use(level, player, hand)` spawns the real projectile only when the level is a `ServerLevel`.

The server-spawned Ender Pearl uses `Projectile.spawnProjectileFromRotation` with velocity `1.5f` and inaccuracy `1.0f`. The projectile then ticks on the server. Its throwable projectile physics apply gravity `0.03`, air inertia `0.99`, water inertia `0.8`, and movement-vector hit detection. When `ThrownEnderpearl.onHit(...)` runs on the server, the owner is teleported with server-authoritative logic.

The reliable pure-client prevention point is before `ServerboundUseItemPacket` is sent. After the packet is accepted and the server spawns a pearl, the client cannot authoritatively cancel the pearl teleport.

## Design

Add an Ender Pearl use guard at the client item-use entry point. When the player uses an item:

1. If the held item is not `Items.ENDER_PEARL`, allow the use.
2. If there is no client level or local player context, allow the use rather than breaking unrelated interactions.
3. Build a conservative predicted throw path from the local player's current eye position, rotation, and known movement.
4. Expand that path into a danger tube that accounts for server-side inaccuracy and synchronization uncertainty.
5. Check loaded blocks around each predicted segment for `Blocks.NETHER_PORTAL`.
6. If any expanded segment can intersect a Nether portal block, cancel the item use before the use packet is sent.
7. If no portal intersection is found, allow the use normally.

The guard should make no changes to other items, other packets, portal rendering, or the existing portal collision wall.

## Conservative Danger Tube

The prediction should intentionally be wider than the exact client-estimated pearl path.

Initial constants for implementation:

- Simulated ticks: `120`.
- Initial speed: `1.5`.
- Gravity per tick: `0.03`.
- Air inertia: `0.99`.
- Water inertia: use `0.8` only if the simulated position is currently in water; otherwise use `0.99`.
- Danger radius: start with `2.0` blocks around each path segment.
- Portal block expansion: treat each Nether portal block as its block AABB expanded by the danger radius.

The exact implementation may model the tube either by expanding the swept segment AABB or by scanning nearby block positions around the segment. The important behavior is conservative coverage: throws that pass close to any loaded Nether portal block should be blocked.

Because the server applies random projectile inaccuracy that the client cannot know exactly, the prediction must not be a thin centerline. The danger radius exists to cover random inaccuracy, small state desync, and ordinary latency.

## Expected Behavior

When a player tries to throw an Ender Pearl:

- If the predicted danger tube intersects a loaded Nether portal block, the client cancels the use and no pearl is thrown.
- If the throw is clearly away from loaded Nether portals, the pearl works normally.
- Throws near Nether portals may be blocked even when the centerline appears safe.
- The mod does not move the player, save a previous position, or try to undo a server teleport.

## Limitations

This is the strongest practical pure-client approach, but it is not a mathematical guarantee against every possible server condition. A client cannot know server random seeds, hidden server-side world changes, custom projectile behavior, server plugins, anticheat rewrites, or unloaded portal blocks that the client has not received.

Within normal vanilla or Fabric multiplayer behavior where the relevant portal blocks are loaded and synchronized to the client, the conservative danger tube is expected to prevent dangerous Ender Pearl throws before the server can spawn the pearl.

## Testing Plan

Verification should cover:

- Non-Ender Pearl item use is not blocked.
- Ender Pearl use far away from Nether portals is allowed.
- Ender Pearl use aimed directly at a Nether portal is blocked.
- Ender Pearl use aimed near a Nether portal is blocked by the conservative radius.
- Existing Nether portal collision wall behavior still builds and remains unchanged.
- The generated jar remains client-only.

## Risks

- Too small a danger radius can miss edge cases caused by server inaccuracy or desync.
- Too large a danger radius can block more near-portal throws than desired.
- Client-only prediction cannot account for portal blocks that are not loaded or synchronized to the client.
- Servers with custom Ender Pearl physics may not match vanilla prediction.
