# No Teleport

[简体中文](README.zh-CN.md)

No Teleport is a client-side Fabric mod for Minecraft `26.1.2` that prevents players from teleporting through Nether portals.

It makes Nether portal blocks behave like full solid blocks on the client, so walking into a portal feels like hitting an invisible wall while the portal still renders normally. It also performs a conservative Ender Pearl trajectory check before item use is sent to the server. If the predicted danger area may touch a Nether portal, the client cancels that Ender Pearl use before the server can spawn the pearl.

## Features

- Client-side only; no server mod is required.
- Nether portals still render and animate normally.
- Players cannot normally walk into Nether portal blocks.
- Ender Pearl use is checked before the use packet is sent.
- Suspicious near-portal Ender Pearl throws are conservatively blocked.
- Normal Ender Pearl use away from Nether portals remains available.

## Compatibility

| Requirement | Version |
| --- | --- |
| Minecraft | `26.1.2` |
| Fabric Loader | `>=0.19.2` |
| Java | `>=25` |
| Fabric API | Not required |

## Installation

1. Install Fabric Loader for Minecraft `26.1.2`.
2. Download or build `no-teleport-0.1.0.jar`.
3. Put the jar into the client's `mods` folder.
4. Launch the game.

The built mod jar is located at:

```text
build/libs/no-teleport-0.1.0.jar
```

## Building From Source

Windows:

```powershell
.\gradlew.bat clean build
```

Linux/macOS:

```bash
./gradlew clean build
```

The mod jar will be generated at:

```text
build/libs/no-teleport-0.1.0.jar
```

## Behavior

Portal blocking is implemented through client-side collision. The mod changes the collision shape for `Blocks.NETHER_PORTAL` to a full block, without changing the portal's rendered shape.

Ender Pearl blocking happens before the client sends the item-use packet. The mod simulates a conservative pearl path and expands it into a wide danger area. If that area may touch any loaded Nether portal block, the item use is cancelled.

## Limitations

- This is a client-side mod and cannot change server-authoritative logic.
- It does not rescue players who are already inside portal blocks.
- It does not roll back a player after the server has already decided to teleport them.
- Ender Pearl checking is conservative, so some near-portal throws that look safe may still be blocked.
- Servers with custom teleport logic, custom Ender Pearl physics, or unsynchronized portal blocks may bypass the assumptions this client-side guard relies on.

## Development

Run the test suite:

```powershell
.\gradlew.bat test
```

Build the release jar:

```powershell
.\gradlew.bat clean build
```

## License

MIT
