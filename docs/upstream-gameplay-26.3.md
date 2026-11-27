# Mili 26.3：Carpet 剩余玩法对照

基准：Lophine `7a51f49a512f1b5465f15a905d3704754e4bed02` 的 Minecraft 0131–0134；本页逐项更新 [前一轮清单](upstream-carpet-26.3.md#其余规则清单未接入) 的 220 个去重键。源列是固定版本补丁中实际带有规则引用的类；不把注册、验证、归一化 helper 或翻译当成玩法。

此表统计 220 个去重键，不是整个服务器的规则总数。首批接入 90 项；后续 99 项中，86 项已接入、1 项沿用原版世界实验、12 项未接入。两批共接入 176 项。另 31 项沿用原有判断：已有等价路径、保留 Mili/Paper 行为、无玩法消费者或不直接移植。支持范围以各行说明为准。

## 接入方式与验证

- 首批与 0400–0401 在 `carpet/gameplay` 配置；世界和实体批次分别在 `carpet/world-gameplay` 与 `carpet/entity-gameplay`。旧 `carpet/general` 无新字段。不启用时保留原版及现有 Mili/Paper 配置值。碰撞缓存、月相时间线、寻路计时和新增配方需重启；配方使用现有 `mili:` 注入路径，保留同名预先存在的配方。
- 现有 Carpet 快照分别调用 `GameplayCompatRules`、`WorldGameplayRules`、`EntityGameplayRules`，按原 manager 发布。所有发布的新键都有服务端行为消费者；没有引入第三方库、Scarpet 或额外权限。
- 数值有安全边界；原版值与 Paper 世界级配置并存时保留 Paper 值。变化作用于相应的原生入口。
- 在独立临时树基于 26.3 预览按顺序重放 7 份补丁，使用缓存的 26.3 类路径和 `javac` 编译了 63 个受影响的 Minecraft 源文件及 4 个普通 Java 源文件。小型 JVM 检查验证了 90 个握手条目无重复、规范化数值、虚空模式匹配以及 `MoonPhase.index()` 的开关两侧。未运行主 Gradle 或游戏服务器；没有完成多区域或插件事件联测。
- 来源及许可证继续载于 [carpet-26.3.txt](../mili-server/src/main/resources/META-INF/licenses/carpet-26.3.txt)。补丁邮件保留固定 Lophine 版本及原作者；未移植 Scarpet 或 0135 helper。

## 逐项状态

### 0131

| 规则 | donor 消费类（含跨补丁消费） | 结果 |
| --- | --- | --- |
| `chainStone` | `PistonStructureResolver` | **0303 已接入**：见[世界规则](upstream-world-rules-26.3.md)的对应补丁、运行范围和限制。 |
| `cleanLogs` | `LivingEntity` | **0210 已接入**：关闭死亡消息游戏规则时，不记录命名生物死亡日志。 |
| `creativeFlyDrag` | `LivingEntity` | **未接入**：原因及影响见[实体与物品](upstream-entity-rules-26.3.md)。 |
| `creativeFlySpeed` | `LivingEntity` | **0348 已接入**：见[实体与物品](upstream-entity-rules-26.3.md)的对应补丁、运行范围和限制。 |
| `customMOTD` | `ServerStatus` | **0213 已接入**：提供服务器 MOTD 的自定义默认内容；下划线保留 Paper 的配置值。 |
| `desertShrubs` | `SaplingBlock` | **0300 已接入**：见[世界规则](upstream-world-rules-26.3.md)的对应补丁、运行范围和限制。 |
| `disableDamageImmunity` | `LivingEntity` | **0210 已接入**：忽略连续受伤的保护间隔；仍触发 Paper 伤害事件，不改变其他免疫。 |
| `fillUpdates` | `Commands`, `FillCommand`, `SetBlockCommand`, `StructureBlockEntity` | **0301 已接入**：见[世界规则](upstream-world-rules-26.3.md)的对应补丁、运行范围和限制。 |
| `forceloadLimit` | — | **无玩法消费者**：只找到配置、验证、归一化 helper 或协议注册，无实际强加载、活塞、铁轨或结构边界调用。 |
| `hardcodeTNTangle` | `PrimedTnt` | **0212 已接入**：TNT 点燃的水平角（弧度）；-1 随机；无初始水平速度规则优先。 |
| `huskSpawningInTemples` | `ChunkGenerator` | **0340 已接入**：见[实体与物品](upstream-entity-rules-26.3.md)的对应补丁、运行范围和限制。 |
| `lightningKillsDropsFix` | `ItemEntity` | **0210 已接入**：保护生成不超过 8 刻的掉落物不受闪电影响。 |
| `maxEntityCollisions` | `LivingEntity` | **0400 已接入**：正数调整本区域生物推动数；0 保留 Paper 配置。 |
| `mergeTNT` | `PrimedTnt` | **未接入**：原因及影响见[世界规则](upstream-world-rules-26.3.md)。 |
| `missingTools` | `Item` | **0342 已接入**：见[实体与物品](upstream-entity-rules-26.3.md)的对应补丁、运行范围和限制。 |
| `moreBlueSkulls` | `WitherBoss` | **0210 已接入**：将凋零主头发射蓝头的概率由 0.1% 提高到 10%。 |
| `movableAmethyst` | `BuddingAmethystBlock`, `BlockBehaviour` | **0214 已接入**：允许活塞搬动紫水晶母岩，使用精准采集镐挖掘可掉落；保留方块掉落事件。 |
| `movableBlockEntities` | `PistonBaseBlock`, `PistonStructureResolver` | **未接入**：原因及影响见[世界规则](upstream-world-rules-26.3.md)。 |
| `optimizedTNT` | `ServerExplosion` | **0302 已接入**：见[世界规则](upstream-world-rules-26.3.md)的对应补丁、运行范围和限制。 |
| `persistentParrots` | `ServerPlayer`, `Player` | **0348 已接入**：见[实体与物品](upstream-entity-rules-26.3.md)的对应补丁、运行范围和限制。 |
| `pickaxeMinedBedrock` | `Item` | **0342 已接入**：见[实体与物品](upstream-entity-rules-26.3.md)的对应补丁、运行范围和限制。 |
| `piglinsSpawningInBastions` | `SpawnPlacements`, `ChunkGenerator` | **0340 已接入**：见[实体与物品](upstream-entity-rules-26.3.md)的对应补丁、运行范围和限制。 |
| `pingPlayerListLimit` | `MinecraftServer` | **0213 已接入**：状态请求的样本上限；12 保留 Spigot 的服务器设置。 |
| `pushLimit` | — | **无玩法消费者**：只找到配置、验证、归一化 helper 或协议注册，无实际强加载、活塞、铁轨或结构边界调用。 |
| `railPowerLimit` | — | **无玩法消费者**：只找到配置、验证、归一化 helper 或协议注册，无实际强加载、活塞、铁轨或结构边界调用。 |
| `renewableSponges` | `Guardian` | **0341 已接入**：见[实体与物品](upstream-entity-rules-26.3.md)的对应补丁、运行范围和限制。 |
| `sculkSensorRange` | `SculkSensorBlockEntity` | **0215 已接入**：幽匿感测体的收听半径上限（格）；8 保留各变体原距离。 |
| `shulkerSpawningInEndCities` | `ChunkGenerator` | **0340 已接入**：见[实体与物品](upstream-entity-rules-26.3.md)的对应补丁、运行范围和限制。 |
| `simulationDistance` | `DedicatedPlayerList`, `PlayerList` | **已等价**：用 server.properties 的 `simulation-distance`，不注册第二个启动距离。 |
| `structureBlockLimit` | — | **无玩法消费者**：只找到配置、验证、归一化 helper 或协议注册，无实际强加载、活塞、铁轨或结构边界调用。 |
| `summonNaturalLightning` | `SummonCommand` | **0301 已接入**：见[世界规则](upstream-world-rules-26.3.md)的对应补丁、运行范围和限制。 |
| `superSecretSetting` | `Commands` | **明确不适配**：属于 Scarpet/客户端握手控制，不是独立的游戏规则。 |
| `tntRandomRange` | `ServerExplosion` | **0302 已接入**：见[世界规则](upstream-world-rules-26.3.md)的对应补丁、运行范围和限制。 |
| `updateSuppressionBlock` | `BarrierBlock` | **0305 已接入**：见[世界规则](upstream-world-rules-26.3.md)的对应补丁、运行范围和限制。 |
| `xpFromExplosions` | `BlockBehaviour`, `BlockBehaviour` (0134) | **0211 已接入**：爆炸破坏方块时也按方块挖掘逻辑掉落经验。 |

### 0132

| 规则 | donor 消费类（含跨补丁消费） | 结果 |
| --- | --- | --- |
| `bambooCollisionBoxDisabled` | `BambooStalkBlock` | **0211 已接入**：移除成熟竹子的物理碰撞，保留选择外形。重启生效。 |
| `betterCraftablePolishedBlackStoneButton` | `RecipeManager` | **0214 已接入**：深板岩可无序合成磨制黑石按钮。 重启生效。 |
| `blockChunkLoaderTimeController` | `TicketType` | **未接入**：原因及影响见[世界规则](upstream-world-rules-26.3.md)。 |
| `blowUpEverything` | `Block` | **0400 已接入**：方块基础抗性可降为零，不跳过爆炸事件。 |
| `blueSkullController` | `WitherBoss` | **0215 已接入**：凋零蓝头模式：vanilla、never 或 surely；never 优先于 moreBlueSkulls。 |
| `breedableParrots` | `Parrot` | **0341 已接入**：见[实体与物品](upstream-entity-rules-26.3.md)的对应补丁、运行范围和限制。 |
| `cakeBlockDropOnBreak` | `Block` | **0214 已接入**：完整蛋糕由正常掉落路径破坏时掉落蛋糕。 |
| `craftableCarvedPumpkin` | `RecipeManager`, `ShapelessRecipe` | **0344 已接入**：见[实体与物品](upstream-entity-rules-26.3.md)的对应补丁、运行范围和限制。 |
| `craftableElytra` | `RecipeManager` | **0214 已接入**：幻翼膜、木棍、鞍和线可合成鞘翅。 重启生效。 |
| `craftableEnchantedGoldenApples` | `RecipeManager` | **0214 已接入**：八个金块和苹果合成附魔金苹果。 重启生效。 |
| `creativeShulkerBoxDropsDisabled` | `ShulkerBoxBlock` | **0214 已接入**：创造玩家破坏有物品的潜影盒时不生成方块物品；不额外破坏方块。 |
| `cryingObsidianNetherPortal` | `PortalShape` | **0304 已接入**：见[世界规则](upstream-world-rules-26.3.md)的对应补丁、运行范围和限制。 |
| `customizedNetherPortal` | `NetherPortalBlock`, `BlockBehaviour` | **0304 已接入**：见[世界规则](upstream-world-rules-26.3.md)的对应补丁、运行范围和限制。 |
| `easyGetPitcherPod` | `Block` | **0214 已接入**：破坏瓶子草作物附送 2 到配置上限个种荚；0 或 1 不附送。 |
| `easyMaxLevelBeacon` | `BeaconBlockEntity` | **0211 已接入**：信标下方一格是基座方块时，直接视为四层。 |
| `easyMineDragonEgg` | `DragonEggBlock` | **0211 已接入**：龙蛋被点击或攻击时不传送。 |
| `easyRefreshTrades` | `Villager` | **0343 已接入**：见[实体与物品](upstream-entity-rules-26.3.md)的对应补丁、运行范围和限制。 |
| `easyWitherSkeletonSkullDrop` | `LivingEntity` | **0210 已接入**：凋零骷髅掉落额外头颅，进入原有生物死亡掉落事件。 |
| `endPortalChunkLoadDisabled` | `Entity`, `EndPortalBlock` | **明确不兼容直接移植**：这些玩法和旧区块容器/跨世界异步迁移及所有权相关，不套用 donor 的区块/实体修改。 |
| `enderDragonNoDestroyBlock` | `EnderDragon` | **0211 已接入**：末影龙不再破坏碰撞区域内的方块。 |
| `enhancedWorldEater` | `Block` | **0400 已接入**：只调整高抗性普通方块，保留基岩、铁砧及末地传送设施。 |
| `experimentalMinecartEnabled` | `GameRuleCommand`, `AbstractMinecart`, `NewMinecartBehavior`, `FeatureFlagSet` | **已有原生入口**：通过世界实验数据包启用；不修改进程级功能集。见[实体与物品](upstream-entity-rules-26.3.md)。 |
| `experimentalMinecartSpeed` | `GameRuleCommand`, `NewMinecartBehavior` | **0347 已接入**：见[实体与物品](upstream-entity-rules-26.3.md)的对应补丁、运行范围和限制。 |
| `fakePeace` | `NaturalSpawner` | **0340 已接入**：见[实体与物品](upstream-entity-rules-26.3.md)的对应补丁、运行范围和限制。 |
| `fakePlayerNoScoreboardCounter` | `Player` | **0215 已接入**：Mili 假玩家不向记分板累加统计。 |
| `fakePlayerPickUpController` | `ItemEntity` | **0342 已接入**：见[实体与物品](upstream-entity-rules-26.3.md)的对应补丁、运行范围和限制。 |
| `fasterMovement` | `Player` | **0348 已接入**：见[实体与物品](upstream-entity-rules-26.3.md)的对应补丁、运行范围和限制。 |
| `fasterMovementController` | `Player` | **0348 已接入**：见[实体与物品](upstream-entity-rules-26.3.md)的对应补丁、运行范围和限制。 |
| `fertilizableSmallFlower` | `BoneMealItem`, `FlowerBlock` | **0300 已接入**：见[世界规则](upstream-world-rules-26.3.md)的对应补丁、运行范围和限制。 |
| `foliageGenerateDisabled` | `HugeFungusFeature`, `FoliagePlacer` | **0211 已接入**：移除一般树叶和巨型菌的帽部生成。 |
| `fullMoonEveryDay` | `MoonPhase` | **0210 已接入**：使用满月的亮度和月相索引；亮度时间线在启动构建。 |
| `furnaceSmeltingTimeController` | `AbstractFurnaceBlockEntity` | **0212 已接入**：覆盖熔炉加工时长（刻）；-1 保留原时长，0 按 1 刻处理；插件仍可覆盖开始熔炼事件。 |
| `headHunter` | `ServerPlayer`, `Player` | **0341 已接入**：见[实体与物品](upstream-entity-rules-26.3.md)的对应补丁、运行范围和限制。 |
| `hopperSuctionDisabled` | `HopperBlockEntity` | **0212 已接入**：禁止漏斗和漏斗矿车主动吸入；不改变其他来源向它们推送物品。 |
| `immuneShulkerBullet` | `ShulkerBullet` | **0210 已接入**：潜影弹命中不产生伤害或漂浮；保留投射物碰撞事件。 |
| `infiniteDurability` | `ItemStack` | **0210 已接入**：耐久消耗归零。 |
| `infiniteTrades` | `MerchantOffer` | **0343 已接入**：见[实体与物品](upstream-entity-rules-26.3.md)的对应补丁、运行范围和限制。 |
| `invulnerable` | `ServerPlayer` (0131), `ServerPlayer` | **0216 已接入**：玩家免于普通伤害；虚空和绕过无敌标签的伤害照常。 |
| `ironGolemNoDropFlower` | `Entity` | **0214 已接入**：铁傀儡默认不掉落虞美人；插件仍可编辑死亡掉落。 |
| `itemAntiExplosion` | `ItemEntity`, `ServerExplosion` | **0302 已接入**：见[世界规则](upstream-world-rules-26.3.md)的对应补丁、运行范围和限制。 |
| `itemEntityCreateNetherPortalDisabled` | `NetherPortalBlock` | **未接入**：原因及影响见[世界规则](upstream-world-rules-26.3.md)。 |
| `jebSheepDropRandomColorWool` | `Sheep` | **0210 已接入**：剪名为 jeb_ 的羊时掉落 1–3 色随机羊毛。 |
| `keepWorldTickUpdate` | `ServerLevel` | **明确不兼容直接移植**：donor 修改服务器或共享/后台线程的节拍或抑制记账；需重新设计 Folia 区域所有权与生命周期，不改全局调度。 |
| `kirinArm` | `Player` | **0342 已接入**：见[实体与物品](upstream-entity-rules-26.3.md)的对应补丁、运行范围和限制。 |
| `largeBundle` | `BundleItem` | **未接入**：原因及影响见[实体与物品](upstream-entity-rules-26.3.md)。 |
| `largeShulkerBox` | `ShulkerBoxMenu`, `ShulkerBoxBlockEntity` | **未接入**：原因及影响见[实体与物品](upstream-entity-rules-26.3.md)。 |
| `maxChainUpdateDepth` | `CollectingNeighborUpdater` | **0213 已接入**：限制 CollectingNeighborUpdater 的更新链；-1 保留世界默认值。即时更新器启用时不适用。 |
| `maxPlayerBlockInteractionRange` | `AttributeCommand`, `AttributeInstance`, `RangedAttribute`, `Player` | **明确不兼容直接移植**：依赖修改客户端交互范围、协议或属性范围；不绕过 Paper 交互与所有权验证。 |
| `maxPlayerBlockInteractionRangeScope` | `AttributeInstance`, `Player` | **明确不兼容直接移植**：依赖修改客户端交互范围、协议或属性范围；不绕过 Paper 交互与所有权验证。 |
| `maxPlayerEntityInteractionRange` | `AttributeCommand`, `AttributeInstance`, `RangedAttribute`, `Player` | **明确不兼容直接移植**：依赖修改客户端交互范围、协议或属性范围；不绕过 Paper 交互与所有权验证。 |
| `maxPlayerEntityInteractionRangeScope` | `AttributeInstance`, `Player` | **明确不兼容直接移植**：依赖修改客户端交互范围、协议或属性范围；不绕过 Paper 交互与所有权验证。 |
| `meekEnderman` | `Enderman` | **已有同目标（不宣称完全等价）**：ORG `staringEndermanNotAngry` 允许插件覆盖默认凝视结果；不照搬 AMS 在事件后强制覆盖。 |
| `mitePearl` | `ThrownEnderpearl` | **0216 已接入**：末影珍珠原路径允许生成末影螨时，移除其随机概率判定。 |
| `movableBlockEntities` | `PistonBaseBlock` (0131), `PistonStructureResolver` (0131) | **未接入**：原因及影响见[世界规则](upstream-world-rules-26.3.md)。 |
| `netherWaterPlacement` | `BucketItem` | **0211 已接入**：玩家可用桶在水会蒸发的维度放水，保留原有桶事件与手部校验。 |
| `noCakeEating` | `CakeBlock` | **0211 已接入**：禁止食用蛋糕，仍可添加蜡烛。 |
| `noEnchantedGoldenAppleEating` | `ItemStack` | **0210 已接入**：禁止食用附魔金苹果。 |
| `noFamilyPlanning` | `AgeableMob` | **已有同目标（不宣称完全等价）**：TIS `breedingCooldownDisabled` 控制进正值冷却，不遮蔽存档中的成年年龄。 |
| `onlyPlayerCanCreateNetherPortal` | `NetherPortalBlock` | **未接入**：原因及影响见[世界规则](upstream-world-rules-26.3.md)。 |
| `perfectInvisibility` | `LivingEntity` | **0216 已接入**：隐形时常规生物视觉探测距离降到零。 |
| `phantomSpawnAlert` | `PhantomSpawner`, `PhantomSpawner` (0134) | **0400 已接入**：自然生成成功后，只向同世界且能看见触发玩家的接收者提示。 |
| `pointedDripstoneCollisionBoxDisabled` | `BlockBehaviour` | **0211 已接入**：移除滴水石锥的物理碰撞，保留选择外形。重启生效。 |
| `preventServerPause` | `MinecraftServer` | **明确不兼容直接移植**：静态世界/全局选项不等价于存活的 Folia 区域活动与休眠机制；不加入旧休眠调度覆写。 |
| `quickVillagerLevelUp` | `Villager` | **0343 已接入**：见[实体与物品](upstream-entity-rules-26.3.md)的对应补丁、运行范围和限制。 |
| `redstoneComponentSound` | `DaylightDetectorBlock`, `RedstoneWireBlock`, `RepeaterBlock` | **0305 已接入**：见[世界规则](upstream-world-rules-26.3.md)的对应补丁、运行范围和限制。 |
| `regeneratingDragonEgg` | `EnderDragonFight` | **已等价**：Paper 的 `ender-dragons-death-always-places-dragon-egg` 已在同一龙蛋生成事件路径。 |
| `renewableNetheriteScrap` | `Entity` | **0341 已接入**：见[实体与物品](upstream-entity-rules-26.3.md)的对应补丁、运行范围和限制。 |
| `rottenFleshBurnedIntoLeather` | `RecipeManager` | **0214 已接入**：普通熔炉把腐肉烧成皮革；耗时 50 刻，给予 0.1 经验。 重启生效。 |
| `safeFlight` | `ServerPlayer` | **0216 已接入**：玩家不受鞘翅撞墙伤害；不改变防作弊和移动校验。 |
| `safePointedDripstone` | `PointedDripstoneBlock` | **0211 已接入**：坠到滴水石锥上的玩家只承受普通摔落伤害。 |
| `scheduledRandomTickAllPlants` | `BambooStalkBlock`, `CactusBlock`, `ChorusFlowerBlock`, `GrowingPlantBlock`, `SugarCaneBlock` | **0300 已接入**：见[世界规则](upstream-world-rules-26.3.md)的对应补丁、运行范围和限制。 |
| `scheduledRandomTickBamboo` | `BambooStalkBlock` | **0300 已接入**：见[世界规则](upstream-world-rules-26.3.md)的对应补丁、运行范围和限制。 |
| `scheduledRandomTickCactus` | `CactusBlock` | **0300 已接入**：见[世界规则](upstream-world-rules-26.3.md)的对应补丁、运行范围和限制。 |
| `scheduledRandomTickChorusFlower` | `ChorusFlowerBlock` | **0300 已接入**：见[世界规则](upstream-world-rules-26.3.md)的对应补丁、运行范围和限制。 |
| `scheduledRandomTickStem` | `GrowingPlantBlock` | **0300 已接入**：见[世界规则](upstream-world-rules-26.3.md)的对应补丁、运行范围和限制。 |
| `scheduledRandomTickSugarCane` | `SugarCaneBlock` | **0300 已接入**：见[世界规则](upstream-world-rules-26.3.md)的对应补丁、运行范围和限制。 |
| `sharedVillagerDiscounts` | `GossipContainer` | **0343 已接入**：见[实体与物品](upstream-entity-rules-26.3.md)的对应补丁、运行范围和限制。 |
| `shulkerHitLevitationDisabled` | `ShulkerBullet` | **0210 已接入**：潜影弹仍可造成伤害，但不施加漂浮效果。 |
| `sneakInvisibility` | `LivingEntity` | **0216 已接入**：潜行时常规生物视觉探测距离降到零。 |
| `sneakToEatCake` | `CakeBlock` | **0211 已接入**：只有潜行的玩家可以食用蛋糕。 |
| `sneakToEditSign` | `SignBlock` | **0210 已接入**：仅在潜行时打开告示牌编辑器。 |
| `stringDupeReintroduced` | `TripWireHookBlock` | **未接入**：原因及影响见[世界规则](upstream-world-rules-26.3.md)。 |
| `strongLeash` | `Leashable`, `HappyGhast`, `FireworkRocketItem` | **已有同目标（不宣称完全等价）**：Paper 有可配置拴绳断开距离；并非 donor 的全部牵引和载人路径。 |
| `superBow` | `Enchantment` | **0344 已接入**：见[实体与物品](upstream-entity-rules-26.3.md)的对应补丁、运行范围和限制。 |
| `superLeash` | `Mob`, `AbstractVillager` | **0347 已接入**：见[实体与物品](upstream-entity-rules-26.3.md)的对应补丁、运行范围和限制。 |
| `superZombieDoctor` | `ZombieVillager` | **0210 已接入**：僵尸村民开始治疗后，不再等待转换倒计时。 |
| `tntPowerController` | `PrimedTnt` | **0212 已接入**：新点燃 TNT 的爆炸强度（0–128）；-1 为原版。 |
| `useItemCooldownDisabled` | `ItemCooldowns` | **0210 已接入**：跳过物品冷却使用判定。 |
| `witchGlowstoneDustDropController` | `LivingEntity` | **0400 已接入**：在女巫正常战利品后追加荧石粉，上限一组。 |
| `witchRedstoneDustDropController` | `LivingEntity` | **0400 已接入**：在女巫正常战利品后追加红石粉，上限一组。 |

### 0133

| 规则 | donor 消费类（含跨补丁消费） | 结果 |
| --- | --- | --- |
| `applyToolEffectsImmediately` | `Player` | **0210 已接入**：在攻击时立即同步装备属性。 |
| `beaconRangeExpand` | `BeaconBlockEntity` | **0346 已接入**：见[实体与物品](upstream-entity-rules-26.3.md)的对应补丁、运行范围和限制。 |
| `beaconWorldHeight` | `BeaconBlockEntity` | **0346 已接入**：见[实体与物品](upstream-entity-rules-26.3.md)的对应补丁、运行范围和限制。 |
| `bindingCurseInvalidation` | `EnchantmentHelper` | **0210 已接入**：忽略阻止更换盔甲的魔咒效果。 |
| `blockDropsDirectlyEnterInventory` | `ServerLevel` | **0401 已接入**：采掘掉落经过掉落及生成事件后，由采掘者所在区域按原生拾取延迟尝试收取；满包留地上，不含供体个人偏好模式。 |
| `cakeBlockDropOnBreak` | `Block` (0132) | **0214 已接入**：完整蛋糕由正常掉落路径破坏时掉落蛋糕。 |
| `canActivatesObserver` | `ObserverBlock` | **0305 已接入**：见[世界规则](upstream-world-rules-26.3.md)的对应补丁、运行范围和限制。 |
| `canMineSpawner` | `BlockItem`, `Block`, `SpawnerBlock` | **明确不适配**：不复制取消方块实体 NBT 权限的放置代码。 |
| `climbingBoat` | `Entity` | **0216 已接入**：玩家控制的船可跨越一格台阶；仍走原碰撞判定。 |
| `customPiglinBarteringTime` | `PiglinAi` | **0210 已接入**：覆盖猪灵观赏金锭的时长，-1 为原版。 |
| `damageEnchantmentCompatible` | `Enchantment` | **0344 已接入**：见[实体与物品](upstream-entity-rules-26.3.md)的对应补丁、运行范围和限制。 |
| `disableMobPeacefulDespawn` | `Mob`, `WitherBoss` | **0210 已接入**：保留已持久化的敌对生物；Paper 的实体级覆盖仍优先。 |
| `disableOpenOrWaterDetection` | `FishingHook` | **0210 已接入**：钓鱼时将水域视为开阔水域。 |
| `disableRespawnBlocksExplode` | `AbstractBedBlock`, `RespawnAnchorBlock` | **0211 已接入**：阻止床及重生锚在不兼容维度爆炸；床仍发布 Paper 失败入睡事件。 |
| `disableWindChargeEffect` | `ServerExplosion` | **0302 已接入**：见[世界规则](upstream-world-rules-26.3.md)的对应补丁、运行范围和限制。 |
| `experienceOrbMerge` | `ExperienceOrb` | **0345 已接入**：见[实体与物品](upstream-entity-rules-26.3.md)的对应补丁、运行范围和限制。 |
| `farmlandPreventStepping` | `FarmlandBlock` | **已等价（保留事件）**：现有 TIS `farmlandTrampledDisabled` 达成不踩坏耕地；不复制 ORG 的事件前提前返回。 |
| `fireworkRocketUseCooldown` | `FireworkRocketItem` | **0212 已接入**：真实玩家成功使用烟花后触发 5 刻物品冷却。 |
| `forceRestock` | `ServerPlayer` | **0343 已接入**：见[实体与物品](upstream-entity-rules-26.3.md)的对应补丁、运行范围和限制。 |
| `healthNotFullCanEat` | `Player` | **0210 已接入**：生命值低于上限 0.3 且饱和度不大于 5 时也允许进食。 |
| `infiniteTrades` | `MerchantOffer` (0132) | **0343 已接入**：见[实体与物品](upstream-entity-rules-26.3.md)的对应补丁、运行范围和限制。 |
| `kirinArm` | `Player` (0132) | **0342 已接入**：见[实体与物品](upstream-entity-rules-26.3.md)的对应补丁、运行范围和限制。 |
| `knockbackStick` | `AnvilMenu` | **0344 已接入**：见[实体与物品](upstream-entity-rules-26.3.md)的对应补丁、运行范围和限制。 |
| `limitPhantomSpawn` | `PhantomSpawner` | **0346 已接入**：见[实体与物品](upstream-entity-rules-26.3.md)的对应补丁、运行范围和限制。 |
| `maxBlockPlaceDistance` | `Player` | **明确不兼容直接移植**：依赖修改客户端交互范围、协议或属性范围；不绕过 Paper 交互与所有权验证。 |
| `maxBlockPlaceDistanceReferToEntity` | `Player` | **明确不兼容直接移植**：依赖修改客户端交互范围、协议或属性范围；不绕过 Paper 交互与所有权验证。 |
| `notDamageEnderPearl` | `ThrownEnderpearl` | **0213 已接入**：末影珍珠传送不产生自身的五点伤害；不替换传送或后续清理。 |
| `openVillagerInventory` | `Villager` | **0349 已接入**：见[实体与物品](upstream-entity-rules-26.3.md)的对应补丁、运行范围和限制。 |
| `peacefulCreeper` | `Creeper` | **0210 已接入**：苦力怕不参与常规自然敌对判定。 |
| `playerDropHead` | `Creeper` | **0341 已接入**：见[实体与物品](upstream-entity-rules-26.3.md)的对应补丁、运行范围和限制。 |
| `playerDropsNotDespawning` | `Inventory` | **0215 已接入**：由 Inventory.dropAll 创建的玩家物品栏物品不自然消失。 |
| `protectionEnchantmentCompatible` | `Enchantment` | **0344 已接入**：见[实体与物品](upstream-entity-rules-26.3.md)的对应补丁、运行范围和限制。 |
| `quickShulker` | `AbstractContainerMenu` | **未接入**：原因及影响见[实体与物品](upstream-entity-rules-26.3.md)。 |
| `recordPlayerCommand` | `Commands` | **明确不适配**：不默认加入原始玩家命令日志；命令串可能含敏感参数。 |
| `riptideIgnoreConditions` | `TridentItem` | **0210 已接入**：允许激流在无水无雨时启动，仍遵循乘客限制和 Bukkit 事件。 |
| `setAnvilExperienceConsumptionLimit` | `AnvilMenu` | **0215 已接入**：铁砧生存费用上限；-1 保留 Bukkit 的上限。 |
| `shulkerBoxStackable` | `AbstractContainerMenu` | **未接入**：原因及影响见[实体与物品](upstream-entity-rules-26.3.md)。 |
| `simpleUpdateSkipper` | `ComparatorBlock`, `RedstoneWireBlock`, `RepeaterBlock` | **0305 已接入**：见[世界规则](upstream-world-rules-26.3.md)的对应补丁、运行范围和限制。 |
| `superChargedCreeper` | `Creeper` | **0341 已接入**：见[实体与物品](upstream-entity-rules-26.3.md)的对应补丁、运行范围和限制。 |
| `totemOfUndyingInvincibleTime` | `LivingEntity` | **0216 已接入**：不死图腾另提供 40 刻 V 级抗性，保留复活和效果事件。 |
| `truePeacefulMode` | `Mob` | **0400 已接入**：生物不再把玩家当作有效目标，不改难度。 |
| `turtleEggFastHatch` | `TurtleEggBlock` | **0210 已接入**：海龟蛋每次随机刻均满足时间/概率孵化条件，仍需要沙子。 |
| `turtleEggFastMine` | `TurtleEggBlock` | **0211 已接入**：挖掘时移除整组海龟蛋。 |
| `villagerHeal` | `Villager` | **0210 已接入**：村民每 80 游戏刻治疗 1 点生命，保留复原事件。 |
| `villagerInfiniteTrade` | `Merchant`, `MerchantOffer` | **0343 已接入**：见[实体与物品](upstream-entity-rules-26.3.md)的对应补丁、运行范围和限制。 |

### 0134

| 规则 | donor 消费类（含跨补丁消费） | 结果 |
| --- | --- | --- |
| `blockEventPacketRange` | `ServerLevel` | **0213 已接入**：方块事件包的发送距离（0–256 格；64 为原版）。 |
| `cakeBlockDropOnBreak` | `Block` (0132) | **0214 已接入**：完整蛋糕由正常掉落路径破坏时掉落蛋糕。 |
| `chatMessageLengthLimitUnlocked` | `ServerboundChatCommandPacket` | **明确不适配**：donor 路径改变网络包解码的长度约束；不为服务端玩法直接放开报文限制。 |
| `chunkTickSpeed` | `ServerLevel` | **明确不兼容直接移植**：donor 修改服务器或共享/后台线程的节拍或抑制记账；需重新设计 Folia 区域所有权与生命周期，不改全局调度。 |
| `creativeInstantTame` | `RunAroundLikeCrazyGoal`, `Cat`, `Parrot`, `Wolf` | **0210 已接入**：创造玩家可立即驯服猫、鹦鹉、狼和马，保留驯服事件。 |
| `creativeNetherWaterPlacement` | `BucketItem` | **0211 已接入**：仅允许创造玩家在水会蒸发的维度用桶放水。 |
| `disableMobPeacefulDespawn` | `Mob` (0133), `WitherBoss` (0133) | **0210 已接入**：保留已持久化的敌对生物；Paper 的实体级覆盖仍优先。 |
| `dropperNoItemCost` | `DropperBlock` | **0212 已接入**：投掷器不消耗物品，保留发射、库存移动事件及本地目标检查。 |
| `elytraFireworkKeepLeashConnection` | `FireworkRocketItem` | **0216 已接入**：鞘翅烟花加速不主动解开拴绳；其他断开原因不变。 |
| `entityChunkSectionIndexXOverflowFix` | `EntitySectionStorage` | **明确不兼容直接移植**：这些玩法和旧区块容器/跨世界异步迁移及所有权相关，不套用 donor 的区块/实体修改。 |
| `entityMomentumLoss` | `Entity` | **0210 已接入**：读取存档时限制单轴速度；关闭后保留有限的高速分量。 |
| `entityPathNavigationStuckDetectionUseRealTimeReintroduced` | `PathNavigation` | **0210 已接入**：寻路卡住检测改用真实时间而非游戏刻。 |
| `entityPlacementIgnoreCollision` | `ArmorStandItem`, `BoatItem`, `EndCrystalItem`, `MinecartItem` | **0347 已接入**：见[实体与物品](upstream-entity-rules-26.3.md)的对应补丁、运行范围和限制。 |
| `entityTrackerDistance` | `EntityType` | **0302 已接入**：见[世界规则](upstream-world-rules-26.3.md)的对应补丁、运行范围和限制。 |
| `entityTrackerInterval` | `EntityType` | **0302 已接入**：见[世界规则](upstream-world-rules-26.3.md)的对应补丁、运行范围和限制。 |
| `explosionNoEntityInfluence` | `ServerExplosion` | **0213 已接入**：爆炸不伤害或击退实体；方块爆炸和事件保持原路径。 |
| `explosionPacketRange` | `ServerLevel` | **0302 已接入**：见[世界规则](upstream-world-rules-26.3.md)的对应补丁、运行范围和限制。 |
| `failSoftBlockStateParsing` | `BlockStateParser` | **0301 已接入**：见[世界规则](upstream-world-rules-26.3.md)的对应补丁、运行范围和限制。 |
| `fillCommandModeEnhance` | `FillCommand` | **0301 已接入**：见[世界规则](upstream-world-rules-26.3.md)的对应补丁、运行范围和限制。 |
| `flattenTriangularDistribution` | `RandomSource` | **0400 已接入**：复用旧配置，均匀分布保留原版两次随机调用。 |
| `fluidDestructionDisabled` | `FlowingFluid` | **0215 已接入**：流体不会替换非空气方块；保留含水方块正常路径。 |
| `gazeDisguiseEquipmentExtended` | `LivingEntity` | **0210 已接入**：头颅也作为凝视检测的伪装。 |
| `hopperSuctionDisabled` | `HopperBlockEntity` (0132) | **0212 已接入**：禁止漏斗和漏斗矿车主动吸入；不改变其他来源向它们推送物品。 |
| `hopperXpCounters` | `HopperBlockEntity` | **0401 已接入**：朝向羊毛的漏斗按世界和颜色累计经验，/xcounter 可查询；重置另需权限。不附加供体日志 HUD。 |
| `itemEntitySkipMovementDisabled` | `ItemEntity` | **0210 已接入**：掉落物不跳过静止移动刻。 |
| `keepWorldTickUpdate` | `ServerLevel` (0132) | **明确不兼容直接移植**：donor 修改服务器或共享/后台线程的节拍或抑制记账；需重新设计 Folia 区域所有权与生命周期，不改全局调度。 |
| `lightUpdates` | `StarLightInterface`, `ThreadedLevelLightEngine` | **明确不兼容直接移植**：donor 修改服务器或共享/后台线程的节拍或抑制记账；需重新设计 Folia 区域所有权与生命周期，不改全局调度。 |
| `maxChainUpdateDepth` | `CollectingNeighborUpdater` (0132) | **0213 已接入**：限制 CollectingNeighborUpdater 的更新链；-1 保留世界默认值。即时更新器启用时不适用。 |
| `minecartPlaceableOnGround` | `MinecartItem` | **0347 已接入**：见[实体与物品](upstream-entity-rules-26.3.md)的对应补丁、运行范围和限制。 |
| `moveableReinforcedDeepslate` | `PistonBaseBlock` | **0214 已接入**：允许活塞推动强化深板岩，仍服从活塞的区域限制。 |
| `naturalSpawningUse13Heightmap` | `NaturalSpawner` | **0213 已接入**：自然生成使用修正后的 1.13 风格高度采样。 |
| `naturalSpawningUse13HeightmapExtra` | `NaturalSpawner` | **0213 已接入**：使用 1.13 风格高度时，也忽略活塞、蜂蜜块和黏液块。 |
| `netherPortalMaxSize` | `PortalShape` | **0304 已接入**：见[世界规则](upstream-world-rules-26.3.md)的对应补丁、运行范围和限制。 |
| `netherWaterPlacement` | `BucketItem` (0132) | **0211 已接入**：玩家可用桶在水会蒸发的维度放水，保留原有桶事件与手部校验。 |
| `oakBalloonPercent` | `TreeGrower` | **0211 已接入**：无花条件下的橡树优先选择大橡树的百分比，非正值使用原版。 |
| `overspawningReintroduced` | `NaturalSpawner` | **0340 已接入**：见[实体与物品](upstream-entity-rules-26.3.md)的对应补丁、运行范围和限制。 |
| `phantomSpawnAlert` | `PhantomSpawner` (0132), `PhantomSpawner` | **0400 已接入**：自然生成成功后，只向同世界且能看见触发玩家的接收者提示。 |
| `poiUpdates` | `ServerLevel` | **明确不兼容直接移植**：donor 修改服务器或共享/后台线程的节拍或抑制记账；需重新设计 Folia 区域所有权与生命周期，不改全局调度。 |
| `preciseEntityPlacement` | `ArmorStandItem`, `EndCrystalItem`, `SpawnEggItem` | **0349 已接入**：见[实体与物品](upstream-entity-rules-26.3.md)的对应补丁、运行范围和限制。 |
| `redstoneDustRandomUpdateOrder` | `DefaultRedstoneWireEvaluator` | **0305 已接入**：见[世界规则](upstream-world-rules-26.3.md)的对应补丁、运行范围和限制。 |
| `redstoneDustRepeaterComparatorIgnoreUpwardsStateUpdate` | `ComparatorBlock`, `RedstoneWireBlock`, `RepeaterBlock` | **0305 已接入**：见[世界规则](upstream-world-rules-26.3.md)的对应补丁、运行范围和限制。 |
| `renewableDragonEgg` | `DragonEggBlock`, `BlockBehaviour`, `LevelChunkSection` | **0348 已接入**：见[实体与物品](upstream-entity-rules-26.3.md)的对应补丁、运行范围和限制。 |
| `renewableDragonHead` | `EnderDragon` | **未接入**：原因及影响见[实体与物品](upstream-entity-rules-26.3.md)。 |
| `renewableElytra` | `Phantom` | **0341 已接入**：见[实体与物品](upstream-entity-rules-26.3.md)的对应补丁、运行范围和限制。 |
| `repeaterHalfDelay` | `DiodeBlock` | **0213 已接入**：红石矿石上方的中继器延迟减半，比较器不受影响。 |
| `sandDupingFix` | `FallingBlockEntity` | **明确不兼容直接移植**：这些玩法和旧区块容器/跨世界异步迁移及所有权相关，不套用 donor 的区块/实体修改。 |
| `simpleUpdateSkipper` | `ComparatorBlock` (0133), `RedstoneWireBlock` (0133), `RepeaterBlock` (0133) | **0305 已接入**：见[世界规则](upstream-world-rules-26.3.md)的对应补丁、运行范围和限制。 |
| `soundSuppressionSimulator` | `SculkSensorBlockEntity` | **0305 已接入**：见[世界规则](upstream-world-rules-26.3.md)的对应补丁、运行范围和限制。 |
| `stopCommandDoubleConfirmation` | `StopCommand` | **0301 已接入**：见[世界规则](upstream-world-rules-26.3.md)的对应补丁、运行范围和限制。 |
| `strongLeash` | `Leashable` (0132), `HappyGhast` (0132), `FireworkRocketItem` (0132) | **已有同目标（不宣称完全等价）**：Paper 有可配置拴绳断开距离；并非 donor 的全部牵引和载人路径。 |
| `structureBlockDoNotPreserveFluid` | `StructureBlockEntity` | **0210 已接入**：结构方块加载时忽略水涝保留。 |
| `synchronizedLightThread` | `ThreadedLevelLightEngine` | **明确不兼容直接移植**：donor 修改服务器或共享/后台线程的节拍或抑制记账；需重新设计 Folia 区域所有权与生命周期，不改全局调度。 |
| `tileTickLimit` | `ServerLevel` | **已等价**：Paper `max-block-ticks` / `max-fluid-ticks` 可分别实现同一每区域计划刻预算，不用预设覆写世界值。 |
| `updateSkippingSimulator` | `PoweredRailBlock` | **0305 已接入**：见[世界规则](upstream-world-rules-26.3.md)的对应补丁、运行范围和限制。 |
| `vaultBlacklistDisabled` | `VaultServerData`, `VaultSharedData` | **0214 已接入**：宝库忽略并不再添加已奖励玩家；旧 Mili 选项仍只阻止更新名单。 |
| `violentNetherPortalCreation` | `PortalShape` | **0304 已接入**：见[世界规则](upstream-world-rules-26.3.md)的对应补丁、运行范围和限制。 |
| `voidDamageAmount` | `LivingEntity` | **0213 已接入**：设置虚空伤害；0 禁用，4 保留 Paper 的世界值。 |
| `voidDamageIgnorePlayer` | `LivingEntity` | **0213 已接入**：不受虚空伤害的玩家模式：false、true 或以逗号分隔的模式名。 |
| `voidRelatedAltitude` | `Entity` | **0213 已接入**：虚空判定相对世界最小高度的偏移；-64 保留 Paper 值。 |
| `wetExplosionReintroduced` | `ServerExplosion` | **0302 已接入**：见[世界规则](upstream-world-rules-26.3.md)的对应补丁、运行范围和限制。 |
| `xpFromExplosions` | `BlockBehaviour` (0131), `BlockBehaviour` | **0211 已接入**：爆炸破坏方块时也按方块挖掘逻辑掉落经验。 |
| `xpTrackingDistance` | `ExperienceOrb` | **0210 已接入**：经验球跟随范围（0–128 格；0 禁止吸引，8 为原版）。 |
| `yeetIdleMspt` | `MinecraftServer` | **明确不兼容直接移植**：donor 修改服务器或共享/后台线程的节拍或抑制记账；需重新设计 Folia 区域所有权与生命周期，不改全局调度。 |
| `zombifiedPiglinDropLootIfAngryReintroduced` | `ZombifiedPiglin` | **0210 已接入**：敌对玩家的僵尸猪灵恢复旧的击杀掉落资格。 |
