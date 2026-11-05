# Shulker Feed

自动从潜影盒取材料补充到物品栏，让你按投影（任意投影类 mod，包括二改投影机）连续放置方块时**不断料**。

## 功能

- 手持方块时，若背包里该方块不足一组（64 个），自动从**附近地面上的潜影盒（半径 4 格）**取出同款方块补满
- 每约 1 秒（20 tick）检查一次所有在线玩家，不打断正常游戏
- **通用适配**：不依赖任何投影 mod 的内部接口，Litematica、二改投影机、Schematica 等都能用

## 安装

1. 安装 **Fabric Loader（≥0.19.5）** 与 **Fabric API（0.161.0+，对应 MC 26.2）**
2. 将 `shulker-feed-1.0.0.jar` 放入 `.minecraft/mods/`
3. 把装材料的**潜影盒放地上**（站在它旁边 4 格内），手持要摆的方块，用投影机正常放置即可

## 构建（开发者）

需要 **JDK 25** 与网络（首次构建会下载 Minecraft / Fabric 依赖）：

```bash
./gradlew build
# 产物位于 build/libs/shulker-feed-1.0.0.jar
```

## 配置

当前版本的参数在源码 `src/main/java/com/shulkerfeed/ShulkerFeeder.java` 顶部：

| 常量 | 默认值 | 含义 |
| --- | --- | --- |
| `RADIUS` | `4` | 地面潜影盒搜索半径（格） |
| `TARGET_STACK` | `64` | 补足到的方块数量 |

## 已知限制（v1.0.0）

- 仅支持**地面上**的潜影盒；背包内揣着的潜影盒暂不支持（MC 26.2 的 NBT / Data Component 改动所致，后续版本计划支持）
- 仅在 **Minecraft 26.2 + Fabric** 环境下验证过

## 计划中的改进

- [ ] 支持背包内潜影盒直接取料（无需把箱子放地上）
- [ ] 加入配置文件（补料半径、是否仅投影时触发等）

## 许可证

本项目以 **MIT License** 开源 —— **允许商用、修改、再分发**，只需保留版权声明。详见 [LICENSE](LICENSE)。

> 说明：本 mod 为第三方非官方作品，与 Mojang Studios / Microsoft 无关。使用本项目的源代码及构建产物须遵守 Minecraft 最终用户许可协议（EULA）及 Fabric 相关许可。
