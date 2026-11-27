# Mili 26.3：Carpet 世界规则第二批

以 Lophine `7a51f49a512f1b5465f15a905d3704754e4bed02` 的 Minecraft 0131–0134 和 Mili 预览 `5d222ff1b0f616945c7500b1dd0361513a6e7bd4` 为基准。按 [剩余玩法对照](upstream-gameplay-26.3.md#逐项状态)「待适配」原因分出的 40 个键，34 个接入原生消费者（其中若干规则只启用安全子集），6 个没有注册。补丁邮件保留供体署名；总来源见 [Carpet 许可证说明](../mili-server/src/main/resources/META-INF/licenses/carpet-26.3.txt)。不带入 Scarpet、全局命令执行包装或额外的权限。

独立配置模块是 `carpet/world-gameplay`，默认保留现有行为。独立的握手入口是 `WorldGameplayRules.registerProtocolRules(List<CarpetRule>)`，由主线在现有快照入口统一调用；此批没有改现有配置或共享注册。类型级实体追踪两项需重启，让新建 tracker 从同一份配置取得距离和间隔。

## 已接入

| Mili | 规则及适用范围 |
| --- | --- |
| 0300 植物 | `desertShrubs` 在带有沙漠神殿标签的干旱生物群系、幼苗进入实际生长时通过 Paper 方块生长事件变成枯萎灌木。`fertilizableSmallFlower` 让小花和孢子花经正常骨粉入口掉落一个花物品，保留物品生成事件。`scheduledRandomTickAllPlants` 与 `scheduledRandomTickBamboo`、`scheduledRandomTickCactus`、`scheduledRandomTickChorusFlower`、`scheduledRandomTickStem`、`scheduledRandomTickSugarCane` 在既有计划刻运行相应的原生随机刻；要求生长足迹已加载且同属当前区域，不另设周期、不强载区块、不替代 Paper 生长/扩散事件。 |
| 0301 命令 | `fillUpdates` 只影响原生 `/fill`、`/setblock` 与结构方块的形状/邻居更新，不对任意命令或插件建立线程局部抑制作用域。`fillCommandModeEnhance` 的 `softreplace` 模式保留可兼容且未显式覆盖的属性，并可叠加原生 `BlockPredicate`；仍检查原生 `/fill` 权限、边界、体积以及加载和所有权。`failSoftBlockStateParsing` 仅给放置型命令参数忽略未知属性和无效的非空值，仍拒绝损坏的语法，不更改 Bukkit `BlockData`、NBT 校验和方块匹配谓词。`summonNaturalLightning` 在接受 `LightningStrikeEvent(Cause.COMMAND)` 后，按本地难度、世界规则和 Paper 的雷击骷髅马概率尝试创建陷阱。`stopCommandDoubleConfirmation` 的 60 秒窗口按同一在线会话一次性消费，普通与强制停止仍检查原有权限，控制台不需要确认。 |
| 0302 爆炸和追踪 | `optimizedTNT` 将 Carpet 射线顺序和中心抗性快排接入现有 Moonrise 缓存路径，不覆盖特殊伤害计算器；`tntRandomRange` 仅在该兼容路径中固定随机强度项。`itemAntiExplosion` 提供爆炸伤害免疫，`no_blast_wave` 额外禁用爆炸位移；`wetExplosionReintroduced` 只在爆炸源可由当前线程安全读取且确实在水里时保护掉落物、盔甲架及悬挂物。`disableWindChargeEffect` 只阻止玩家风弹触发方块，仍保留击退。`explosionPacketRange` 只调 Folia 本区域广播半径，不跨区枚举玩家。`entityTrackerDistance`、`entityTrackerInterval` 覆盖类型默认值，不把原不追踪或不周期更新的类型变成周期追踪；Paper/Moonrise 下游距离、可见性、区块发送状态和纵向限制不变。 |
| 0303 活塞 | `chainStone` 在原生结构求解器加入沿轴连接，包含供体的支撑面与末地烛判定和 `stick_to_all` 模式。求解未加载或跨区的位置直接失败；仍使用原有活塞事件、移动限制和实体更新。并不赋予活塞搬运带数据方块的能力。 |
| 0304 传送门 | `cryingObsidianNetherPortal` 扩展点火门框，`customizedNetherPortal` 允许保留不完整的传送门并按普通挖掘路径破坏它。`netherPortalMaxSize` 支持 3–64 格；原门/出口的矩形识别与 Folia EMPTY 搜索区块余量一起调整，自动创建仍使用原生退路与 `PortalCreateEvent`，手工创建必须在已加载、当前线程拥有的平面上。`violentNetherPortalCreation` 仅开放 `replaceable`：不覆盖门框、方块实体或带流体的非原生候选；不提供 donor 可覆盖任意非黑曜石方块的 `all`。 |
| 0305 红石/更新器 | `redstoneComponentSound` 为正常的探测器/中继器/红石粉状态切换加音效；`canActivatesObserver` 用正常方块交互、物品使用和原生延迟刻触发。`simpleUpdateSkipper` 与 `redstoneDustRepeaterComparatorIgnoreUpwardsStateUpdate` 适配共用形状处理、二极管支撑和 Alternate Current 的红石线移除判断，保留既有活板门选项。`redstoneDustRandomUpdateOrder` 只改变 donor 对应的 Vanilla evaluator 通知顺序（`true` 或 `random_pos`）；不强制切换 Alternate Current、Eigencraft 或实验引擎。`updateSuppressionBlock` 限制当前区域 CollectingNeighborUpdater 的余量，收集链结束即清除；激活铁轨状态有 Paper 红石事件，InstantUpdater 不改。`soundSuppressionSimulator` 使青金石矿上方的幽匿感测体不接收振动；`updateSkippingSimulator` 使深层青金石矿上方的动力铁轨保持通电并跳过自己的传播；两者都不通过 donor 的异常或全链清理打断其他位置和插件。 |

## 未注册的 6 项

| 键 | 已确认的阻断 |
| --- | --- |
| `stringDupeReintroduced` | 保留 Paper 对已移除绊线的检查，不把旧快照写回世界。 |
| `movableBlockEntities` | 供体在 `PistonBaseBlock` 收集真实 BlockEntity，拆除源再交给动态活塞方块。Mili `PistonMovingBlockEntity#finalTick`、常规完成、强制覆盖/清除及 `MovingPistonBlock#getDrops` 各有独立路径；没有统一的数据载荷回收点。单纯绕过 `hasBlockEntity` 会存量丢失或复制；配对大桶还需要两端一起搬运、区域归属确认和停服恢复。未开放命令方块或容器推送。 |
| `mergeTNT` | 供体按静止坐标、相同引信合并并 `discard` 另一实体，不能维持两者独立的 Paper 实体生命周期、火焰/强度和所有者、爆炸预处理与方块事件归属。即便附加 `count` 存档字段，重启后仍不能找回被移除的每根 TNT 身份。此批不以聚合替代独立实体。 |
| `blockChunkLoaderTimeController` | 供体参数仅定义票据期限；真实票来自 `AmsBlockChunkLoaders#note`、`#piston` 和 `#bell`，各有独立加载器设置、触发块和范围策略，Mili 没有这些独立消费者。新增一个注册常量会是假规则；让普通音符盒/活塞/钟按既有默认值强加载则不是同一配置，且会引入未审计的区块与世界常驻开销。 |
| `itemEntityCreateNetherPortalDisabled`、`onlyPlayerCanCreateNetherPortal` | 供体的判断位于 `NetherPortalBlock` 同步创建路径；Folia 主路径已在 `Entity#portalToAsync` 分离并转换原实体，再异步查找，找不到会二次搜/建，`null` 又进入坐标传送退路。简单禁止写门既会让实体无门跨维度，也不能直接把已有目的地副本跨线程放回。需要在提交变维前做预查询、或新增可取消的待传送回源协议，并保留乘客树、关服回收、原/目的区域票及已有事件顺序。 |

## 验证边界

固定预览的隔离源文件切片按编号重放；Java 25 局部编译及 JVM 测试不使用根 Gradle。测试检查握手清单无重复、停止确认会话隔离与过期、门尺寸/模式边界、红石通知集合不增删。未运行实际多区域服务器、插件保护、红石机器或持久化迁移联测，不将局部结果视为发布包验证。
