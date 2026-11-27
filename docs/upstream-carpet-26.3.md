# Mili 26.3 Carpet 上游对照

基准：[LophineLabs/Lophine `7a51f49a512f1b5465f15a905d3704754e4bed02`](https://github.com/LophineLabs/Lophine/tree/7a51f49a512f1b5465f15a905d3704754e4bed02)。对照上游 Minecraft 0131–0135、API 0015、Paper-server 0025 与 Mili 0115–0118、已有普通源码、配置和协议钩子。原有状态表不能单独证明规则可用：有些标为 Removed 的规则仍有实现，也有些规则只存在配置或握手声明。

## 26.2 → 26.3：旧行为还是新增规则

旧引用是 Lophine [`ver/26.2` (`fc3415e6672e1ce2810e5096f2c2fb35cb63b300`)](https://github.com/LophineLabs/Lophine/tree/fc3415e6672e1ce2810e5096f2c2fb35cb63b300) 与 [`ver/26.2-hardfork` (`9afac8a68da08fbb7ea5adcd9c1484f5756ce787`)](https://github.com/LophineLabs/Lophine/tree/9afac8a68da08fbb7ea5adcd9c1484f5756ce787)。旧主线把 Carpet 集中在 Minecraft 0045/0046（另有 Luminol 0007/0008），hardfork 集中在 Minecraft 0127，二者都另带 Leaves 等功能补丁。比较包括规则声明、协议转发、普通 Java 和补丁内真正读取规则的代码；只列注册或翻译不视为实现。两个旧引用均不含 26.3 的 `carpet.script` 运行时。

| 26.3 分包 | 在两个旧引用可追到的规则或机制 | 确认新增或扩充的代表行为 |
| --- | --- | --- |
| 0132 AMS | `bambooModelNoOffset`（`BlockBehaviour` 偏移）、`betterCraftableBoneBlock` / `betterCraftableDispenser`（`RecipeManager` 注入）、`creativeOneHitKill`（玩家攻击）、`powerfulExpMending`（经验球修补）、`sensibleEnderman`（可拾方块白名单）、`shulkerGolem`（雕刻南瓜图案）；`preventEndSpikeRespawn` 原已通过龙复活阶段与 `EndSpikeFeature` 阻止重建，26.3 改成字符串模式。 | `extinguishedCampfire`（营火放置状态）、`easyCompost`（堆肥升层）、`undyingCoral`（珊瑚死亡刻）、`endermanTeleportRandomlyDisabled` 和 `endermanPickUpDisabled`（分别为随机传送和禁用拾取；不是原有 `sensibleEnderman` 的白名单）。 |
| 0133 ORG | `dustTrapdoorReintroduced` 和 `shulkerBoxCCEReintroduced` 的旧消费者在 Leaves 红石和 CCE 补丁；`xpNoCooldown` 的旧消费者在 `ExperienceOrb` / `Player`。这些交叉引用不构成新规则。 | `disableBatCanSpawn`、`disableWaterFreezes`、`reusableSmithingTemplate`、`disableFurnaceDropExperience`、`staringEndermanNotAngry` 有新的生成、结冰、菜单、经验或凝视判定。`farmlandPreventStepping` 与旧 TIS 耕地保护同目标但在实体交互事件前返回，不把需要保留事件的旧规则重命名为它。 |
| 0134 TIS | 两个旧 Carpet 补丁均有 `antiSpamDisabled`、`blockPlacementIgnoreEntity`、`clientSettingsLostOnRespawnFix`、`creativeNoItemCooldown`、`creativeOneHitKill`、`creativeOpenContainerForcibly`、`entityInstantDeathRemoval`、`farmlandTrampledDisabled`、`hopperNoItemCost`、`observerNoDetection`、`tntDoNotUpdate`、`tntDupingFix`、`tntIgnoreRedstoneSignal`、`totallyNoBlockUpdate`、`yeetOutOfOrderChatKick` 的实际处理；`commandTick`、`instantBlockUpdaterReintroduced`、`hopperCountersUnlimitedSpeed` 也有原路径。`tickCommandPermission`、`tntFuseDuration` 经归一化 helper，`yeetUpdateSuppressionCrash` 经共享崩溃保护 helper，不是只声明。 | `breedingCooldownDisabled`、`snowMeltMinLightLevel`、`endPortalOpenedSoundDisabled`、`toughWitherRose`、`turtleEggTrampledDisabled`、`undeadDontBurnInSunlight` 有新判定。`leaderZombieSpawnWithMaxHealthDisabled` 名称是新的，但两个旧引用都已有 `oldLeaderZombieHealth` 对 `Zombie` 的同类控制，故 Mili 复用 0181。 |
| 0131、0135 两个旧配置名 | 两个旧 Lophine 引用未找到 `renewableDeepslate` / `renewableCoral` 的实际规则消费；Mili 自己的旧 `ver/26.2`（`e31b877e88286c86994cd7725a24487d0516b68f`）保留这两个配置名但无钩子。Mili 旧引用的 `farmlandTrampledDisabled` / `preventEndSpikeRespawn` 也仅声明及同步。 | 26.3 由 0131 增加两种流体反应的深板岩形态，由 0131 + 0135 组合接入珊瑚骨粉结构生长；Mili 现连接旧键，不另建配置。 |

其余条目按未适配处理；没有仅凭旧引用查不到同名就把潜在别名认定为全新，也不把 26.3 整个分包误算为拆包或完整移植。

## 已接入

新规则默认保持原版（布尔开关默认关闭，雪层光照阈值默认 12）；新配置在 `carpet/general`，使用现有握手协议和扩展 manager ID；深板岩和珊瑚沿用 Mili 已有普通配置。没有导入上游规则框架、品牌或翻译包。以下是实现范围，不等于已进行游戏内回归测试。

| Mili 补丁 | 上游 | 已接入的规则及行为 |
| --- | --- | --- |
| 0150 | 0131 Carpet | `renewableBlackstone`（侧面或上方蓝冰；原版水和玄武岩判定优先，保留方块形成事件）、`liquidDamageDisabled`（禁用流体冲毁普通方块）、`thickFungusGrowth`（人工巨型菌粗柄 `false` / `random` / `all`，不改变自然生成判定） |
| 0151 | 0132 AMS | `extinguishedCampfire`（放置时熄灭）、`easyCompost`（合格物品默认每次加一级，保留 Paper 事件）、`undyingCoral`（所有活珊瑚变体，含切换配置前已排队的死亡刻） |
| 0152 | 0133 ORG | `disableBatCanSpawn`（生成判定）、`disableWaterFreezes`（自然结冰判定）、`reusableSmithingTemplate`（只影响模板栏；`false` / `upgrade` / `true`）、`disableFurnaceDropExperience`（默认掉落为零；仍触发原有事件，插件可覆盖） |
| 0153 | 0132 AMS、0133 ORG | `endermanTeleportRandomlyDisabled`（无目标随机传送）、`staringEndermanNotAngry`（默认不因凝视发怒；保留 Paper 事件，插件可覆盖） |
| 0154 | 0134 TIS | `turtleEggTrampledDisabled`（实体踩踏和主动破坏海龟蛋，不阻止玩家挖掘）、`undeadDontBurnInSunlight`（只阻止日照点燃） |
| 0155 | 0134 TIS | `endPortalOpenedSoundDisabled`（只静音，不改变传送门创建）、`toughWitherRose`（放宽支撑材质） |
| 0156 | 0134 TIS / 旧 0045、0127 | `farmlandTrampledDisabled` 跳过落地踩坏的状态写入，保留落地伤害、物理交互及实体改变方块事件；不干预自然退化。 |
| 0157 | 0132 AMS / 旧 0045、0127 | `preventEndSpikeRespawn` 在已有布尔模式下只跳过复活期间尖柱破坏、爆炸、重建和柱顶水晶生成；不改变自然生成、龙复活阶段或用来启动仪式的水晶。未增加 26.3 的其他字符串模式。 |
| 0158 | 0131 Carpet | 现有 `renewable-deepslate` 配置：主世界 Y<0 时，流动熔岩格接触侧面或上方的水生成圆石深板岩，熔岩向下流入水格生成深板岩；保留黑曜石、玄武岩、原有方块形成事件及取消。 |
| 0159 | 0131 + 0135 Carpet | 现有 `renewable-coral` 配置：`TRUE` 支持水下活珊瑚植物，`EXPANDED` 新增活珊瑚扇和墙珊瑚扇。沿用原版骨粉及 Paper/CraftBukkit 捕获和施肥事件。只在全部相关区块已加载且由当前 Folia 线程持有时受理生长，不强制加载、不跨区写；装饰使用有界的原版海泡菜定义。 |
| 0160 | 0132 AMS | `endermanPickUpDisabled`：关闭末影人的拾方块目标，不改变可拾白名单的配置值、已携带方块放置或其他末影人判定。 |
| 0161 | 0134 TIS | `breedingCooldownDisabled`（成年 `age=0` 不进入正值冷却）及 `snowMeltMinLightLevel`（最低方块光照 0–16，默认 12，保留 `BlockFadeEvent`）。 |
| 0181，共享钩子 | 0134 TIS | `leaderZombieSpawnWithMaxHealthDisabled`：保留 TIS 配置名和协议值，与 Mili 的 `oldLeaderZombieHealth` 在同一个生成条件取 OR，不重复修改 Zombie，也不互相覆盖配置。 |
| 0204 | 0134 TIS / 0135 `CarpetLargeBarrel` | `largeBarrel` 默认关闭，重启生效。仅 `barrel_rows=3`、同一已加载且归当前 Folia 线程所有的区块、背靠背且各 27 格的木桶组成真实 54 格组合（水平或竖直）。跨区块、缺少另一半或容量不符时退回既有单桶路径，不改变 Purpur 行数设置和旧档。菜单、漏斗和比较器共用配对判定；尚无完整服务器运行验证。 |
| 0205 | 0131 / 0135 `CarpetBlockRotator` | 主手仙人掌旋转、副手反向放置、AMS 扩展方块和音效、发射器旋转。尊重交互和发射事件的拒绝；不跨区写入、不加载区块。四项规则默认关闭（音效 0）。与双木桶同在 `4724d44` 的已验证候选包中。 |
| 0207 | 0134 / 0135 `CarpetEntityRemoval` | `creativeHitRemoveEntity` 默认关闭。在 Paper 允许攻击后直接移除非玩家、非旁观实体；不走死亡掉落，不分裂史莱姆，也不洒出载具库存。横扫不跨区域、次要目标也允许插件拒绝。没有新增实体移除命令。 |
| 0206 | 0134 TIS / 0135 `CarpetDragonBreathDispenseBehavior` | 两项默认关闭。`dispensersFireDragonBreath` 在已加载且归当前 Folia 线程所有的作用范围内生成龙息云；发射事件的向量是绝对位置，可重定向。换成其他物品时保留选中龙息，替代品按它自己的原生发射和目标规则执行；同种改写也不扣选中龙息。取消、空替代物、无效或跨区域目标、生成被拒绝时不生成云或扣除龙息；跨区不排队。`dispenserNoItemCost` 保留发射器选中物品与耐久，容器返还仍用原生库存插入或掉落路径，不影响投掷器；普通发射行为的正前方区块未加载或不归当前线程时不执行。未引入 Scarpet，也未做真实服务器事件联测。 |
| API 0013、Paper-server 0025 | API 0015、Paper-server 0025 | `Server#getCarpet()` / `fun.bm.mili.api.carpet.CarpetApi.get()` 查询不可变的已广播规则快照（manager、name、value）。同步读取不需要区域线程，也不授予区域所有权。握手的 NBT 字段不变；规则重载完整发布而非先清空再逐条填充。 |

API 只暴露 Mili 已经广播的值，不承诺底层规则能力探测，不含 Lophine 的隐藏规则元数据，也不与其插件二进制兼容。`setRule`、默认值持久化、变更事件、Scarpet 注册和插件启停撤销**未接入**。当前实现不保存插件对象或回调，因此未增加空生命周期钩子。

## 已存在，不重复覆盖

这里表示 Mili 已有运行路径或相邻实现，不表示与 Lophine 的所有模式、边缘行为和命令树一致。

| 上游 | Mili 已有路径 |
| --- | --- |
| 0131 | `commandTick`（转发到 `CommandConfig`）、`creativeNoClip`（转发到原配置/0114）、`explosionNoBlockDamage`、`fastRedstoneDust`（0117）、`interactionUpdates`、`lagFreeSpawning`、`placementRotationFix`、`tntDoNotUpdate`、`tntPrimerMomentumRemoved`、`viewDistance`、`xpNoCooldown`。 |
| 0132 | `bambooModelNoOffset`、`betterCraftableBoneBlock`、`betterCraftableDispenser`、`creativeOneHitKill`、`powerfulExpMending`、`sensibleEnderman`、`shulkerGolem`。 |
| 0133 | `dustTrapdoorReintroduced`、`shulkerBoxCCEReintroduced`、`xpNoCooldown`；`villagerVoidTrading` 与独立补丁 0185 属同一行为，勿重复复制村民交易栈。 |
| 0134 | `antiSpamDisabled`、`blockPlacementIgnoreEntity`、`clientSettingsLostOnRespawnFix`、`commandTick`、`creativeNoItemCooldown`、`creativeOneHitKill`、`creativeOpenContainerForcibly`、`dustTrapdoorReintroduced`、`entityInstantDeathRemoval`、`hopperCountersUnlimitedSpeed`、`hopperNoItemCost`、`instantBlockUpdaterReintroduced`（0106 / `RedStoneConfig`）、`observerNoDetection`、`tickCommandPermission`、`tntDoNotUpdate`、`tntDupingFix`、`tntFuseDuration`、`tntIgnoreRedstoneSignal`、`totallyNoBlockUpdate`、`yeetOutOfOrderChatKick`、`yeetUpdateSuppressionCrash`（0103 / `UpdateSuppressionCrashFixConfig`）。已有 TISCM MSPT、Carpet HUD 和现有假玩家支持保留；Mili HUD/区域性能分析不等于上游完整微时序事件流。 |

## 未接入及阻断

范围按补丁中可识别的规则和 helper 调用拆分。待适配表示没有在此提交行为，不代表所有同名配置都应添加；后续需要先查对应的 Paper、Folia、Mili 或独立补丁能力。

| 上游部分 | 阻断 / 状态 |
| --- | --- |
| 0131 Scarpet runtime、命令、应用库、事件续体 | donor 含 `carpet.script` 表达式、主机、模块和区域/实体 continuations，并连接大量 NMS 钩子。Mili 的计算器和客户端协议不能替代 runtime；未引入 `/script`、`commandScriptACE`、本地脚本自动载入、应用库下载/安装、native actors、world/command 函数或脚本事件。后续如实现本地受限脚本，需独立审计区域 ownership、能力权限、资源配额及注销语义；不开放任意远程脚本或任意命令。 |
| 0131–0134 共享规则/命令框架 | donor 的 rule registry、配置事件、权限动态刷新、客户端命令桥、跨区域 command continuations 和 bot 调度不是 Mili 同名配置的直接替换。`/carpet` 写操作、插件写入/持久化、脚本回调以及命令和诊断框架未移植。 |
| 0135 CrashReport / CrashReportCategory | `deobfuscateCrashReportStackTrace` 未移植。donor 使用 `TisStackTraces` 的 26.3 空映射重新构建堆栈帧，不做实际名称反混淆且会丢掉模块/类加载器信息；不增加只保留名字但有该副作用的开关。 |
| 0135 `VanillaLikeExperience` 与 0136 | 独立任务；本组补丁未修改该开关或末影珍珠实现。 |
| 权限/不受控命令 | `antiCheatDisabled`、`debugNbtQueryNoPermission`、`enchantCommandNoRestriction`、`instantCommandBlock`、`openGameRulePermission`、`openSeedPermission`、`openTpPermission`、`opPlayerNoCheat`、`perfPermissionLevel`、`preventAdministratorCheat` 不从 donor 直接引入；如需能力，先有明确授权模型并保留 Paper 权限。 |

### 其余规则清单（未接入）

同一规则可能出现在多个补丁。这里按 donor 补丁内引用分组，排除上表已接入、已有路径、明确阻断的命令，以及只有内部用途的函数名和常量。列在这里的都未标为完成。

#### 0131

`disableGhastFire`、`disableBlazeFire`、`noCreeperBlockBreaking`、`noGhastBlockBreaking` 已经由 MiniTweaks 0202 接入，保留爆炸事件，并分别控制地形破坏和点火。

- `chainStone`, `cleanLogs`, `creativeFlyDrag`, `creativeFlySpeed`, `customMOTD`, `desertShrubs`, `disableDamageImmunity`, `fillUpdates`
- `forceloadLimit`, `hardcodeTNTangle`, `huskSpawningInTemples`, `lightningKillsDropsFix`, `maxEntityCollisions`, `mergeTNT`, `missingTools`, `moreBlueSkulls`, `movableAmethyst`
- `movableBlockEntities`, `optimizedTNT`, `persistentParrots`, `pickaxeMinedBedrock`, `piglinsSpawningInBastions`, `pingPlayerListLimit`, `pushLimit`
- `railPowerLimit`, `renewableSponges`, `sculkSensorRange`, `shulkerSpawningInEndCities`, `simulationDistance`, `structureBlockLimit`, `summonNaturalLightning`, `superSecretSetting`
- `tntRandomRange`, `updateSuppressionBlock`, `xpFromExplosions`

#### 0132

- `bambooCollisionBoxDisabled`, `betterCraftablePolishedBlackStoneButton`, `blockChunkLoaderTimeController`, `blowUpEverything`, `blueSkullController`, `breedableParrots`, `cakeBlockDropOnBreak`, `craftableCarvedPumpkin`, `craftableElytra`
- `craftableEnchantedGoldenApples`, `creativeShulkerBoxDropsDisabled`, `cryingObsidianNetherPortal`, `customizedNetherPortal`, `easyGetPitcherPod`, `easyMaxLevelBeacon`, `easyMineDragonEgg`, `easyRefreshTrades`, `easyWitherSkeletonSkullDrop`
- `endPortalChunkLoadDisabled`, `enderDragonNoDestroyBlock`, `enhancedWorldEater`, `experimentalMinecartEnabled`, `experimentalMinecartSpeed`, `fakePeace`, `fakePlayerNoScoreboardCounter`, `fakePlayerPickUpController`
- `fasterMovement`, `fasterMovementController`, `fertilizableSmallFlower`, `foliageGenerateDisabled`, `fullMoonEveryDay`, `furnaceSmeltingTimeController`, `headHunter`, `hopperSuctionDisabled`, `immuneShulkerBullet`
- `infiniteDurability`, `infiniteTrades`, `invulnerable`, `ironGolemNoDropFlower`, `itemAntiExplosion`, `itemEntityCreateNetherPortalDisabled`, `jebSheepDropRandomColorWool`, `keepWorldTickUpdate`, `kirinArm`
- `largeBundle`, `largeShulkerBox`, `maxChainUpdateDepth`, `maxPlayerBlockInteractionRange`, `maxPlayerBlockInteractionRangeScope`, `maxPlayerEntityInteractionRange`, `maxPlayerEntityInteractionRangeScope`, `meekEnderman`, `mitePearl`
- `movableBlockEntities`, `netherWaterPlacement`, `noCakeEating`, `noEnchantedGoldenAppleEating`, `noFamilyPlanning`, `onlyPlayerCanCreateNetherPortal`, `perfectInvisibility`, `phantomSpawnAlert`, `pointedDripstoneCollisionBoxDisabled`
- `preventServerPause`, `quickVillagerLevelUp`, `redstoneComponentSound`, `regeneratingDragonEgg`, `renewableNetheriteScrap`, `rottenFleshBurnedIntoLeather`, `safeFlight`, `safePointedDripstone`, `scheduledRandomTickAllPlants`
- `scheduledRandomTickBamboo`, `scheduledRandomTickCactus`, `scheduledRandomTickChorusFlower`, `scheduledRandomTickStem`, `scheduledRandomTickSugarCane`, `sharedVillagerDiscounts`, `shulkerHitLevitationDisabled`, `sneakInvisibility`, `sneakToEatCake`
- `sneakToEditSign`, `stringDupeReintroduced`, `strongLeash`, `superBow`, `superLeash`, `superZombieDoctor`, `tntPowerController`, `useItemCooldownDisabled`, `witchGlowstoneDustDropController`
- `witchRedstoneDustDropController`

#### 0133

- `applyToolEffectsImmediately`, `beaconRangeExpand`, `beaconWorldHeight`, `bindingCurseInvalidation`, `blockDropsDirectlyEnterInventory`, `cakeBlockDropOnBreak`, `canActivatesObserver`, `canMineSpawner`, `climbingBoat`
- `customPiglinBarteringTime`, `damageEnchantmentCompatible`, `disableMobPeacefulDespawn`, `disableOpenOrWaterDetection`, `disableRespawnBlocksExplode`, `disableWindChargeEffect`, `experienceOrbMerge`, `farmlandPreventStepping`, `fireworkRocketUseCooldown`
- `forceRestock`, `healthNotFullCanEat`, `infiniteTrades`, `kirinArm`, `knockbackStick`, `limitPhantomSpawn`, `maxBlockPlaceDistance`, `maxBlockPlaceDistanceReferToEntity`, `notDamageEnderPearl`
- `openVillagerInventory`, `peacefulCreeper`, `playerDropHead`, `playerDropsNotDespawning`, `protectionEnchantmentCompatible`, `quickShulker`, `recordPlayerCommand`, `riptideIgnoreConditions`, `setAnvilExperienceConsumptionLimit`
- `shulkerBoxStackable`, `simpleUpdateSkipper`, `superChargedCreeper`, `totemOfUndyingInvincibleTime`, `truePeacefulMode`, `turtleEggFastHatch`, `turtleEggFastMine`, `villagerHeal`, `villagerInfiniteTrade`

#### 0134

- `blockEventPacketRange`, `cakeBlockDropOnBreak`, `chatMessageLengthLimitUnlocked`, `chunkTickSpeed`, `creativeInstantTame`, `creativeNetherWaterPlacement`, `disableMobPeacefulDespawn`, `dropperNoItemCost`
- `elytraFireworkKeepLeashConnection`, `entityChunkSectionIndexXOverflowFix`, `entityMomentumLoss`, `entityPathNavigationStuckDetectionUseRealTimeReintroduced`, `entityPlacementIgnoreCollision`, `entityTrackerDistance`, `entityTrackerInterval`, `explosionNoEntityInfluence`, `explosionPacketRange`
- `failSoftBlockStateParsing`, `fillCommandModeEnhance`, `flattenTriangularDistribution`, `fluidDestructionDisabled`, `gazeDisguiseEquipmentExtended`, `hopperSuctionDisabled`, `hopperXpCounters`, `itemEntitySkipMovementDisabled`, `keepWorldTickUpdate`
- `lightUpdates`, `maxChainUpdateDepth`, `minecartPlaceableOnGround`, `moveableReinforcedDeepslate`, `naturalSpawningUse13Heightmap`, `naturalSpawningUse13HeightmapExtra`, `netherPortalMaxSize`, `netherWaterPlacement`, `oakBalloonPercent`
- `overspawningReintroduced`, `phantomSpawnAlert`, `poiUpdates`, `preciseEntityPlacement`, `redstoneDustRandomUpdateOrder`, `redstoneDustRepeaterComparatorIgnoreUpwardsStateUpdate`, `renewableDragonEgg`, `renewableDragonHead`, `renewableElytra`
- `repeaterHalfDelay`, `sandDupingFix`, `simpleUpdateSkipper`, `soundSuppressionSimulator`, `stopCommandDoubleConfirmation`, `strongLeash`, `structureBlockDoNotPreserveFluid`, `synchronizedLightThread`
- `tileTickLimit`, `updateSkippingSimulator`, `vaultBlacklistDisabled`, `violentNetherPortalCreation`, `voidDamageAmount`, `voidDamageIgnorePlayer`, `voidRelatedAltitude`, `wetExplosionReintroduced`, `xpFromExplosions`
- `xpTrackingDistance`, `yeetIdleMspt`, `zombifiedPiglinDropLootIfAngryReintroduced`

另有原版、Paper 或 Mili 提供相邻行为但未做等价验证的项目，例如 0118 的部分村民交易和 Vault 变更。

## 依赖、许可和验证

- Lophine 补丁署名：Bacteriawa；原 integration 依据 Lophine 的 GPL v3 声明。保留 Fabric Carpet（gnembon，参考 `f358000b175ddbcf1dd0bc59641c715fb0545664`）、AMS Addition（`750310179368b2569dd6121a2769b2bbc7343`）、ORG Addition（fcsailboat，`c2142c213269f85fb1851263bf60f147849224a1`）和 TIS Addition（`62c3cb6fff26cd9c4e12aa616b403c184a4a6ca0`）的原有许可通知，见 [服务器随包声明](../mili-server/src/main/resources/META-INF/licenses/carpet-26.3.txt)。0159 中 0135 珊瑚 helper 保留 `LGPL-3.0-or-later` 标注和原作者/固定版本出处。donor 的 `META-INF/licenses/Scarpet-Carpet-MIT.txt` 是后续移植脚本运行时的独立许可依据，本次未引入 Scarpet 源码。
- 新规则只用 Minecraft、Paper/Folia 和 Mili 已有配置/协议，不加第三方库。`exp4j:0.4.8` 已供 Mili 聊天计算器使用，不是 Scarpet；不因 donor 构建脚本而导入额外依赖。
- 对受影响的 Minecraft 文件在临时树中按顺序复现 Paper/Folia/Mili 补丁；API 和 CraftServer 补丁另做 `git apply --check`。使用缓存的 26.3 类路径和 `javac` 局部编译；用小型 Java 检查覆盖快照不可变、规则排序、区分大小写、后续发布、握手 NBT 字段，以及新增开关默认值和雪层阈值上下界。没有运行 Gradle、没有启动游戏服务器，未验证实际世界内规则及客户端模组联机。
