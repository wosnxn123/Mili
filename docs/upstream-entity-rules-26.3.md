# Mili 26.3：实体、交易和物品玩法

基准：Lophine `7a51f49a512f1b5465f15a905d3704754e4bed02` 的 0131–0134；Mili Minecraft 预览固定到 `5d222ff1b0f616945c7500b1dd0361513a6e7bd4`。本批只处理[剩余玩法对照](upstream-gameplay-26.3.md)中指定的 51 个去重键；原清单作为前一批的记录保留。这里未计算交给主线的 8 个通用复验项。

0340–0349 接入 42 项；0401 又接入采掘掉落入包和漏斗经验计数。51 项合计：44 项有服务端消费者、1 项已有原生实现、6 项未接入。新开关位于 `carpet/entity-gameplay`；只有启用非默认值才启用玩法。`EntityGameplayRules.registerProtocolRules` 供现有同步入口统一注册，不增加权限、脚本、协议编码、插件依赖或存档字段。来源和许可证沿用 [Carpet 随包声明](../mili-server/src/main/resources/META-INF/licenses/carpet-26.3.txt)；村民菜单按 Lophine 固定版本的 ORG `OrgVillagerMenu` 适配，保留其来源和 MIT 归属。

## 已接入

| 补丁 | 规则 | 原生路径和边界 |
| --- | --- | --- |
| 0340 | `huskSpawningInTemples`, `shulkerSpawningInEndCities`, `piglinsSpawningInBastions`, `fakePeace`, `overspawningReintroduced` | 结构注册表和原生 `NaturalSpawner`；保留生成事件和真实计数。旧式超生成只放宽已满足初始上限的批次，不取消后续上限。 |
| 0341 | `renewableSponges`, `breedableParrots`, `renewableNetheriteScrap`, `renewableElytra`, `headHunter`, `playerDropHead`, `superChargedCreeper` | `Mob.convertTo`、繁殖目标、Paper 生物和玩家死亡掉落列表。只在拥有苦力怕的线程读写单次头颅记账；非同区苦力怕不附加玩家头颅。 |
| 0342 | `missingTools`, `pickaxeMinedBedrock`, `kirinArm`, `fakePlayerPickUpController` | `Tool` 数据组件、方块破坏进度和 `ItemEntity.playerTouch`。基岩规则只改持镐的破坏进度，不改变爆炸或建造权限。假玩家逻辑作用于 Mili 的 `ServerBot`。 |
| 0343 | `infiniteTrades`, `villagerInfiniteTrade`, `easyRefreshTrades`, `quickVillagerLevelUp`, `sharedVillagerDiscounts`, `forceRestock` | 保留报价、职业、购买事件及旧交易次数；两无限交易键共享正常库存判断，不丢已有次数。保留 Mili 的虚空交易次数计数。补充快捷栏仅发生于玩家没有其他菜单时。 |
| 0344 | `superBow`, `damageEnchantmentCompatible`, `protectionEnchantmentCompatible`, `knockbackStick`, `craftableCarvedPumpkin` | 限制于规定的原版附魔对及铁砧木棍；不影响其他附魔的互斥。雕刻南瓜使用普通无序配方；剪刀只在取出结果后以真实耐久流程计算剩余物，含耐久附魔，需重启。 |
| 0345 | `experienceOrbMerge` | 合并不同价值，与普通经验注入共用原生入口；两个现存经验球合并保留可取消事件，事件后重新校验实体归属和总量。超过 `Short.MAX_VALUE` 保留原球，不截断经验。 |
| 0346 | `beaconRangeExpand`, `beaconWorldHeight`, `limitPhantomSpawn` | 信标使用原生生效间隔与 `BeaconEffectEvent`；异区玩家在自己线程上复核世界、边界、死亡及旁观状态，扩展范围不加载地块。幻翼使用区域刷怪状态，只计入实际生成。本批未实测插件在异区玩家事件中尝试直接读取信标的行为。 |
| 0347 | `experimentalMinecartSpeed`, `minecartPlaceableOnGround`, `entityPlacementIgnoreCollision`, `superLeash` | 只调整已启用原版矿车实验时的默认速度，插件单矿车限速优先。地面放置和碰撞规则保留交互权限和放置事件。拴绳仍通过原生交互事件。 |
| 0348 | `creativeFlySpeed`, `fasterMovement`, `fasterMovementController`, `persistentParrots`, `renewableDragonEgg` | 原版能力包同步创造飞行速度；使用非持久移动属性加成同步地面速度。鹦鹉在伤害被接受后才按概率从肩上释放。龙蛋仅在持有且加载的区块用龙息复制，传播事件取消时不消耗。飞行倍率和龙蛋随机刻缓存需重启。 |
| 0349 | `openVillagerInventory`, `preciseEntityPlacement` | 使用原版 3×3 菜单查看村民的八个真实槽（第九槽锁定），地域或距离校验失败不再访问库存。盔甲架、水晶和刷怪蛋以点击水平位置放置，限制在原生目标格以保留压低生成、权限和原生事件。 |

## 已有原生路径、仍未接入的规则

| 规则 | 证据与后续路线 |
| --- | --- |
| `experimentalMinecartEnabled` | 已有等价入口：`AbstractMinecart.useExperimentalMovement` 使用世界 `enabledFeatures().contains(FeatureFlags.MINECART_IMPROVEMENTS)`。用原版世界实验数据包启用，同时向客户端发送世界功能集；不另改进程级 `FeatureFlagSet.contains`。 |
| `creativeFlyDrag` | `ClientboundPlayerAbilitiesPacket` 只发四个能力标志、`flyingSpeed`、`walkingSpeed`；没有阻力。仅修改 `LivingEntity` 的服务端拖曳不能同步原版客户端预测。需要客户端就绪协商和同一计算，或另行明确定义仅服务端假玩家的规则。 |
| `largeBundle` | `BundleContents.Mutable` 以 `Fraction.ONE` 为装填上限，客户端也按该值预测点击和显示容量；服务端放宽而不提供客户端容量会失配。后续需同时设计持久容量及客户端协商，验证溢出物品和禁用模式。 |
| `largeShulkerBox` | `ShulkerBoxBlockEntity` 存储、漏斗侧面槽和 `ShulkerBoxMenu` 都固定为 27；`ItemContainerContents.copyInto` 按目标列表长度复制（组件本身支持 256 槽）。新版超出 27 槽的数据必须在第一处解码时保留；经本版 27 槽加载再保存后，不能从截断的存档还原。可用原版泛型 6 行菜单、成对升级物品组件和方块实体装载、并在关闭规则时拒绝静默降级。 |
| `quickShulker` | 当前 `AbstractContainerMenu.doClick` 负责活动菜单点击，快捷打开要绑定真实物品槽而不是复制一个库存。需要锁住物品来源槽，覆盖交换、快移、关闭、掉线、断线和插件取消；先验证往返无丢物/重复，再选择已支持的泛型菜单，无需新包。 |
| `shulkerBoxStackable` | 不能只让 `AbstractContainerMenu` 在一个点击分支叠放：原版物品堆叠限制来自 `MAX_STACK_SIZE`，物品放置和非菜单库存也各有路径。可为空、同组件的盒子设计明确的组件和拆分策略，并测试堆叠打开、堆叠放置、组件由插件改变/规则关闭时返还剩余物。 |
| `blockDropsDirectlyEnterInventory` | 0401 + Paper-server 0028 已接入。只收取采掘掉落事件放行且实际加入世界的物品；12 刻后在原玩家仍在线、同一世界、同区且相距不超过 8 格时通过原生拾取入口尝试一次。尊重插件延长的拾取延迟和背包容量，每次破坏至多收取 64 个掉落实体；不注册供体的个人偏好模式。 |
| `hopperXpCounters` | 0401 已接入。在朝向羊毛、未锁定的漏斗传输刻中，每次最多处理 16 个所属区域的经验球；按世界和颜色统计价值与数量的乘积。`/xcounter` 查询，`reset` 另需 `mili.commands.counter.reset`。重启清零；不含供体速率 HUD。 |
| `renewableDragonHead` | donor 0134 只打开一个候选判定；候选必须经 0131 的异步受伤续体在龙归属线程取状态、在苦力怕归属线程预约头颅，再回龙的线程处理。目标版 `EnderDragon.hurtServer`/`hurtPartServer` 和 `tickDeath` 分离且有自有死亡阶段。可在已接受伤害路径生成一次性归因记录，在苦力怕 owner 上预约，最后随龙死亡原生掉落提交；不改写龙死亡阶段或搬入 Scarpet。 |

## 验证

从固定预览提交读取 Blob，在独立临时树顺序执行 `git apply --check` 与补丁应用，并使用缓存的 26.3 类路径和 `javac` 局部编译。测试覆盖经验和溢出、点击位置仍属于原目标格、移动等级回退以及规则快照。未运行根 Gradle、真实多区域服务器、客户端联机或插件事件联测。
