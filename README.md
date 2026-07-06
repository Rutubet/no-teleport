# No Teleport

## 中文

No Teleport 是一个纯客户端 Fabric Mod，用于在 Minecraft `26.1.2` 中阻止玩家通过地狱门传送。

它会在客户端把地狱门方块当作完整实体方块处理，让玩家像撞到空气墙一样无法正常走进地狱门。同时，它会在投掷末影珍珠前进行保守轨迹预测：如果预测出的危险轨迹范围可能接触到地狱门，客户端会取消这次末影珍珠使用，避免服务端生成末影珍珠实体。

### 功能

- 纯客户端 Mod，不需要服务端安装。
- 地狱门仍正常渲染和动画显示。
- 玩家无法通过普通移动走进地狱门。
- 末影珍珠投掷前会进行地狱门风险检测。
- 靠近地狱门的可疑末影珍珠投掷会被保守拦截。
- 远离地狱门的正常末影珍珠使用不受影响。

### 兼容性

- Minecraft: `26.1.2`
- Fabric Loader: `>=0.19.2`
- Java: `>=25`
- Fabric API: 不需要

### 安装

1. 安装支持 Minecraft `26.1.2` 的 Fabric Loader。
2. 将 `no-teleport-0.1.0.jar` 放入客户端的 `mods` 文件夹。
3. 启动游戏。

构建产物位于：

```text
build/libs/no-teleport-0.1.0.jar
```

### 从源码构建

在项目根目录运行：

```powershell
.\gradlew.bat clean build
```

Linux/macOS:

```bash
./gradlew clean build
```

构建完成后，Mod jar 会生成到：

```text
build/libs/no-teleport-0.1.0.jar
```

### 行为说明

地狱门阻挡通过客户端碰撞实现。客户端会把 `Blocks.NETHER_PORTAL` 的碰撞形状改为整格方块，但不会改变地狱门的渲染形状。

末影珍珠阻挡发生在客户端发送使用物品包之前。Mod 会模拟一段保守的末影珍珠轨迹，并把轨迹扩展成较粗的危险范围。只要这个范围可能接触到已加载的地狱门方块，本次使用就会被取消。

### 限制

- 这是纯客户端 Mod，不能改变服务端权威逻辑。
- 不会救援已经进入地狱门方块内的玩家。
- 不会在服务端已经决定传送后强制回退玩家。
- 末影珍珠风险检测是保守的，靠近地狱门时可能误拦一些看似安全的投掷。
- 如果服务器使用自定义传送逻辑、自定义末影珍珠物理，或客户端未加载相关地狱门方块，Mod 不能保证完全覆盖这些情况。

## English

No Teleport is a client-side Fabric mod for Minecraft `26.1.2` that prevents players from teleporting through Nether portals.

It makes Nether portal blocks behave like full solid blocks on the client, so walking into a portal feels like hitting an invisible wall while the portal still renders normally. It also performs a conservative Ender Pearl trajectory check before item use is sent to the server. If the predicted danger area may touch a Nether portal, the client cancels that Ender Pearl use before the server can spawn the pearl.

### Features

- Client-side only; no server mod is required.
- Nether portals still render and animate normally.
- Players cannot normally walk into Nether portal blocks.
- Ender Pearl use is checked before the use packet is sent.
- Suspicious near-portal Ender Pearl throws are conservatively blocked.
- Normal Ender Pearl use away from Nether portals remains available.

### Compatibility

- Minecraft: `26.1.2`
- Fabric Loader: `>=0.19.2`
- Java: `>=25`
- Fabric API: not required

### Installation

1. Install Fabric Loader for Minecraft `26.1.2`.
2. Put `no-teleport-0.1.0.jar` into the client's `mods` folder.
3. Launch the game.

The built mod jar is located at:

```text
build/libs/no-teleport-0.1.0.jar
```

### Building From Source

From the project root, run:

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

### Behavior

Portal blocking is implemented through client-side collision. The mod changes the collision shape for `Blocks.NETHER_PORTAL` to a full block, without changing the portal's rendered shape.

Ender Pearl blocking happens before the client sends the item-use packet. The mod simulates a conservative pearl path and expands it into a wide danger area. If that area may touch any loaded Nether portal block, the item use is cancelled.

### Limitations

- This is a client-side mod and cannot change server-authoritative logic.
- It does not rescue players who are already inside portal blocks.
- It does not roll back a player after the server has already decided to teleport them.
- Ender Pearl checking is conservative, so some near-portal throws that look safe may still be blocked.
- Servers with custom teleport logic, custom Ender Pearl physics, or unsynchronized portal blocks may bypass the assumptions this client-side guard relies on.
