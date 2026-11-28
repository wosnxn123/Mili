package fun.bm.mili.carpet.config.modules;

import fun.bm.mili.rust.TomlConfigData;
import java.util.Set;
import me.earthme.luminol.config.IConfigModule;
import me.earthme.luminol.config.flags.ConfigClassInfo;
import me.earthme.luminol.config.flags.ConfigInfo;
import me.earthme.luminol.config.flags.HotReloadUnsupported;
import me.earthme.luminol.enums.EnumConfigCategory;
import org.jetbrains.annotations.Nullable;

@ConfigClassInfo(category = EnumConfigCategory.ROOT, name = "gameplay", directory = {"carpet"},
    comments = "Carpet 26.3 兼容玩法。默认保留 Mili、Paper 和原版行为。")
public final class GameplayCompatConfig implements IConfigModule {
    @ConfigInfo(name = "blockDropsDirectlyEnterInventory", comments = "采掘产生的掉落物经事件放行和原生拾取延迟后尝试进入采掘者背包。仅同区域近距离生效。")
    public static volatile boolean blockDropsDirectlyEnterInventory = false;
    @HotReloadUnsupported
    @ConfigInfo(name = "miningDropPlayerControl", comments = "在上一开关已启用时改为玩家自选（ORG custom）；用 /orange ruleself 设置个人偏好，重启生效。")
    public static volatile boolean miningDropPlayerControl = false;
    @HotReloadUnsupported
    @ConfigInfo(name = "hopperXpCounters", comments = "启用朝向羊毛的漏斗统计经验；/xcounter 按当前世界和颜色查询。重启生效。")
    public static volatile boolean hopperXpCounters = false;
    @ConfigInfo(name = "maxEntityCollisions", comments = "正数限制每只生物每刻推动的目标数；0 保留 Paper 配置，不改变挤压游戏规则。")
    public static volatile int maxEntityCollisions = 0;
    @ConfigInfo(name = "blowUpEverything", comments = "将方块自身的基础爆炸抗性视为 0；爆炸事件和其他世界保护仍然适用。")
    public static volatile boolean blowUpEverything = false;
    @ConfigInfo(name = "enhancedWorldEater", comments = "0–16：替换基础抗性至少为 17 的普通方块抗性；-1 关闭。基岩、铁砧和末地传送设施除外。")
    public static volatile double enhancedWorldEater = -1;
    @ConfigInfo(name = "phantomSpawnAlert", comments = "幻翼自然生成成功后，只向同世界、可见触发玩家的接收者发送提示。")
    public static volatile boolean phantomSpawnAlert = false;
    @ConfigInfo(name = "witchGlowstoneDustDropController", comments = "女巫正常战利品结算后追加的荧石粉，-1 不干预，最多一组。")
    public static volatile int witchGlowstoneDustDropController = -1;
    @ConfigInfo(name = "witchRedstoneDustDropController", comments = "女巫正常战利品结算后追加的红石粉，-1 不干预，最多一组。")
    public static volatile int witchRedstoneDustDropController = -1;
    @ConfigInfo(name = "truePeacefulMode", comments = "生物不会选择玩家作为有效目标；不会修改世界难度或清除其他目标。")
    public static volatile boolean truePeacefulMode = false;
    @ConfigInfo(name = "flattenTriangularDistribution", comments = "将三角分布改为均匀分布，保留随机调用次数。与已有 old-feature 配置取或。")
    public static volatile boolean flattenTriangularDistribution = false;

    public static double effectiveWorldEaterResistance() {
        double value = enhancedWorldEater;
        return Double.isFinite(value) && value >= 0 && value <= 16 ? value : -1;
    }
    public static boolean useFlatDistribution() {
        return flattenTriangularDistribution || fun.bm.mili.config.modules.function.OldFeatureConfig.flattenTriangularDistribution;
    }

    @ConfigInfo(name = "cleanLogs", comments = "关闭死亡消息游戏规则时，不记录命名生物死亡日志。")
    public static volatile boolean cleanLogs = false;

    @ConfigInfo(name = "customMOTD", comments = "提供服务器 MOTD 的自定义默认内容；下划线保留 Paper 的配置值。")
    public static volatile String customMOTD = "_";

    @ConfigInfo(name = "disableDamageImmunity", comments = "忽略连续受伤的保护间隔；仍触发 Paper 伤害事件，不改变其他免疫。")
    public static volatile boolean disableDamageImmunity = false;

    @ConfigInfo(name = "hardcodeTNTangle", comments = "TNT 点燃的水平角（弧度）；-1 随机；无初始水平速度规则优先。")
    public static volatile double hardcodeTNTangle = -1.0;

    @ConfigInfo(name = "lightningKillsDropsFix", comments = "保护生成不超过 8 刻的掉落物不受闪电影响。")
    public static volatile boolean lightningKillsDropsFix = false;

    @ConfigInfo(name = "moreBlueSkulls", comments = "将凋零主头发射蓝头的概率由 0.1% 提高到 10%。")
    public static volatile boolean moreBlueSkulls = false;

    @ConfigInfo(name = "movableAmethyst", comments = "允许活塞搬动紫水晶母岩，使用精准采集镐挖掘可掉落；保留方块掉落事件。")
    public static volatile boolean movableAmethyst = false;

    @ConfigInfo(name = "pingPlayerListLimit", comments = "状态请求的样本上限；12 保留 Spigot 的服务器设置。")
    public static volatile int pingPlayerListLimit = 12;

    @ConfigInfo(name = "sculkSensorRange", comments = "幽匿感测体的收听半径上限（格）；8 保留各变体原距离。")
    public static volatile int sculkSensorRange = 8;

    @ConfigInfo(name = "xpFromExplosions", comments = "爆炸破坏方块时也按方块挖掘逻辑掉落经验。")
    public static volatile boolean xpFromExplosions = false;

    @HotReloadUnsupported
    @ConfigInfo(name = "bambooCollisionBoxDisabled", comments = "移除成熟竹子的物理碰撞，保留选择外形。重启生效。")
    public static volatile boolean bambooCollisionBoxDisabled = false;

    @HotReloadUnsupported
    @ConfigInfo(name = "betterCraftablePolishedBlackStoneButton", comments = "深板岩可无序合成磨制黑石按钮。 重启生效。")
    public static volatile boolean betterCraftablePolishedBlackStoneButton = false;

    @ConfigInfo(name = "blueSkullController", comments = "凋零蓝头模式：vanilla、never 或 surely；never 优先于 moreBlueSkulls。")
    public static volatile String blueSkullController = "vanilla";

    @ConfigInfo(name = "cakeBlockDropOnBreak", comments = "完整蛋糕由正常掉落路径破坏时掉落蛋糕。")
    public static volatile boolean cakeBlockDropOnBreak = false;

    @HotReloadUnsupported
    @ConfigInfo(name = "craftableElytra", comments = "幻翼膜、木棍、鞍和线可合成鞘翅。 重启生效。")
    public static volatile boolean craftableElytra = false;

    @HotReloadUnsupported
    @ConfigInfo(name = "craftableEnchantedGoldenApples", comments = "八个金块和苹果合成附魔金苹果。 重启生效。")
    public static volatile boolean craftableEnchantedGoldenApples = false;

    @ConfigInfo(name = "creativeShulkerBoxDropsDisabled", comments = "创造玩家破坏有物品的潜影盒时不生成方块物品；不额外破坏方块。")
    public static volatile boolean creativeShulkerBoxDropsDisabled = false;

    @ConfigInfo(name = "easyGetPitcherPod", comments = "破坏瓶子草作物附送 2 到配置上限个种荚；0 或 1 不附送。")
    public static volatile int easyGetPitcherPod = 0;

    @ConfigInfo(name = "easyMaxLevelBeacon", comments = "信标下方一格是基座方块时，直接视为四层。")
    public static volatile boolean easyMaxLevelBeacon = false;

    @ConfigInfo(name = "easyMineDragonEgg", comments = "龙蛋被点击或攻击时不传送。")
    public static volatile boolean easyMineDragonEgg = false;

    @ConfigInfo(name = "easyWitherSkeletonSkullDrop", comments = "凋零骷髅掉落额外头颅，进入原有生物死亡掉落事件。")
    public static volatile boolean easyWitherSkeletonSkullDrop = false;

    @ConfigInfo(name = "enderDragonNoDestroyBlock", comments = "末影龙不再破坏碰撞区域内的方块。")
    public static volatile boolean enderDragonNoDestroyBlock = false;

    @ConfigInfo(name = "fakePlayerNoScoreboardCounter", comments = "Mili 假玩家不向记分板累加统计。")
    public static volatile boolean fakePlayerNoScoreboardCounter = false;

    @ConfigInfo(name = "foliageGenerateDisabled", comments = "移除一般树叶和巨型菌的帽部生成。")
    public static volatile boolean foliageGenerateDisabled = false;

    @HotReloadUnsupported
    @ConfigInfo(name = "fullMoonEveryDay", comments = "使用满月的亮度和月相索引；亮度时间线在启动构建。")
    public static volatile boolean fullMoonEveryDay = false;

    @ConfigInfo(name = "furnaceSmeltingTimeController", comments = "覆盖熔炉加工时长（刻）；-1 保留原时长，0 按 1 刻处理；插件仍可覆盖开始熔炼事件。")
    public static volatile int furnaceSmeltingTimeController = -1;

    @ConfigInfo(name = "hopperSuctionDisabled", comments = "禁止漏斗和漏斗矿车主动吸入；不改变其他来源向它们推送物品。")
    public static volatile boolean hopperSuctionDisabled = false;

    @ConfigInfo(name = "immuneShulkerBullet", comments = "潜影弹命中不产生伤害或漂浮；保留投射物碰撞事件。")
    public static volatile boolean immuneShulkerBullet = false;

    @ConfigInfo(name = "infiniteDurability", comments = "耐久消耗归零。")
    public static volatile boolean infiniteDurability = false;

    @ConfigInfo(name = "invulnerable", comments = "玩家免于普通伤害；虚空和绕过无敌标签的伤害照常。")
    public static volatile boolean invulnerable = false;

    @ConfigInfo(name = "ironGolemNoDropFlower", comments = "铁傀儡默认不掉落虞美人；插件仍可编辑死亡掉落。")
    public static volatile boolean ironGolemNoDropFlower = false;

    @ConfigInfo(name = "jebSheepDropRandomColorWool", comments = "剪名为 jeb_ 的羊时掉落 1–3 色随机羊毛。")
    public static volatile boolean jebSheepDropRandomColorWool = false;

    @ConfigInfo(name = "maxChainUpdateDepth", comments = "限制 CollectingNeighborUpdater 的更新链；-1 保留世界默认值。即时更新器启用时不适用。")
    public static volatile int maxChainUpdateDepth = -1;

    @ConfigInfo(name = "mitePearl", comments = "末影珍珠原路径允许生成末影螨时，移除其随机概率判定。")
    public static volatile boolean mitePearl = false;

    @ConfigInfo(name = "netherWaterPlacement", comments = "玩家可用桶在水会蒸发的维度放水，保留原有桶事件与手部校验。")
    public static volatile boolean netherWaterPlacement = false;

    @ConfigInfo(name = "noCakeEating", comments = "禁止食用蛋糕，仍可添加蜡烛。")
    public static volatile boolean noCakeEating = false;

    @ConfigInfo(name = "noEnchantedGoldenAppleEating", comments = "禁止食用附魔金苹果。")
    public static volatile boolean noEnchantedGoldenAppleEating = false;

    @ConfigInfo(name = "perfectInvisibility", comments = "隐形时常规生物视觉探测距离降到零。")
    public static volatile boolean perfectInvisibility = false;

    @HotReloadUnsupported
    @ConfigInfo(name = "pointedDripstoneCollisionBoxDisabled", comments = "移除滴水石锥的物理碰撞，保留选择外形。重启生效。")
    public static volatile boolean pointedDripstoneCollisionBoxDisabled = false;

    @HotReloadUnsupported
    @ConfigInfo(name = "rottenFleshBurnedIntoLeather", comments = "普通熔炉把腐肉烧成皮革；耗时 50 刻，给予 0.1 经验。 重启生效。")
    public static volatile boolean rottenFleshBurnedIntoLeather = false;

    @ConfigInfo(name = "safeFlight", comments = "玩家不受鞘翅撞墙伤害；不改变防作弊和移动校验。")
    public static volatile boolean safeFlight = false;

    @ConfigInfo(name = "safePointedDripstone", comments = "坠到滴水石锥上的玩家只承受普通摔落伤害。")
    public static volatile boolean safePointedDripstone = false;

    @ConfigInfo(name = "shulkerHitLevitationDisabled", comments = "潜影弹仍可造成伤害，但不施加漂浮效果。")
    public static volatile boolean shulkerHitLevitationDisabled = false;

    @ConfigInfo(name = "sneakInvisibility", comments = "潜行时常规生物视觉探测距离降到零。")
    public static volatile boolean sneakInvisibility = false;

    @ConfigInfo(name = "sneakToEatCake", comments = "只有潜行的玩家可以食用蛋糕。")
    public static volatile boolean sneakToEatCake = false;

    @ConfigInfo(name = "sneakToEditSign", comments = "仅在潜行时打开告示牌编辑器。")
    public static volatile boolean sneakToEditSign = false;

    @ConfigInfo(name = "superZombieDoctor", comments = "僵尸村民开始治疗后，不再等待转换倒计时。")
    public static volatile boolean superZombieDoctor = false;

    @ConfigInfo(name = "tntPowerController", comments = "新点燃 TNT 的爆炸强度（0–128）；-1 为原版。")
    public static volatile double tntPowerController = -1.0;

    @ConfigInfo(name = "useItemCooldownDisabled", comments = "跳过物品冷却使用判定。")
    public static volatile boolean useItemCooldownDisabled = false;

    @ConfigInfo(name = "applyToolEffectsImmediately", comments = "在攻击时立即同步装备属性。")
    public static volatile boolean applyToolEffectsImmediately = false;

    @ConfigInfo(name = "bindingCurseInvalidation", comments = "忽略阻止更换盔甲的魔咒效果。")
    public static volatile boolean bindingCurseInvalidation = false;

    @ConfigInfo(name = "climbingBoat", comments = "玩家控制的船可跨越一格台阶；仍走原碰撞判定。")
    public static volatile boolean climbingBoat = false;

    @ConfigInfo(name = "customPiglinBarteringTime", comments = "覆盖猪灵观赏金锭的时长，-1 为原版。")
    public static volatile long customPiglinBarteringTime = -1L;

    @ConfigInfo(name = "disableMobPeacefulDespawn", comments = "保留已持久化的敌对生物；Paper 的实体级覆盖仍优先。")
    public static volatile boolean disableMobPeacefulDespawn = false;

    @ConfigInfo(name = "disableOpenOrWaterDetection", comments = "钓鱼时将水域视为开阔水域。")
    public static volatile boolean disableOpenOrWaterDetection = false;

    @ConfigInfo(name = "disableRespawnBlocksExplode", comments = "阻止床及重生锚在不兼容维度爆炸；床仍发布 Paper 失败入睡事件。")
    public static volatile boolean disableRespawnBlocksExplode = false;

    @ConfigInfo(name = "fireworkRocketUseCooldown", comments = "真实玩家成功使用烟花后触发 5 刻物品冷却。")
    public static volatile boolean fireworkRocketUseCooldown = false;

    @ConfigInfo(name = "healthNotFullCanEat", comments = "生命值低于上限 0.3 且饱和度不大于 5 时也允许进食。")
    public static volatile boolean healthNotFullCanEat = false;

    @ConfigInfo(name = "notDamageEnderPearl", comments = "末影珍珠传送不产生自身的五点伤害；不替换传送或后续清理。")
    public static volatile boolean notDamageEnderPearl = false;

    @ConfigInfo(name = "peacefulCreeper", comments = "苦力怕不参与常规自然敌对判定。")
    public static volatile boolean peacefulCreeper = false;

    @ConfigInfo(name = "playerDropsNotDespawning", comments = "由 Inventory.dropAll 创建的玩家物品栏物品不自然消失。")
    public static volatile boolean playerDropsNotDespawning = false;

    @ConfigInfo(name = "riptideIgnoreConditions", comments = "允许激流在无水无雨时启动，仍遵循乘客限制和 Bukkit 事件。")
    public static volatile boolean riptideIgnoreConditions = false;

    @ConfigInfo(name = "setAnvilExperienceConsumptionLimit", comments = "铁砧生存费用上限；-1 保留 Bukkit 的上限。")
    public static volatile int setAnvilExperienceConsumptionLimit = -1;

    @ConfigInfo(name = "totemOfUndyingInvincibleTime", comments = "不死图腾另提供 40 刻 V 级抗性，保留复活和效果事件。")
    public static volatile boolean totemOfUndyingInvincibleTime = false;

    @ConfigInfo(name = "turtleEggFastHatch", comments = "海龟蛋每次随机刻均满足时间/概率孵化条件，仍需要沙子。")
    public static volatile boolean turtleEggFastHatch = false;

    @ConfigInfo(name = "turtleEggFastMine", comments = "挖掘时移除整组海龟蛋。")
    public static volatile boolean turtleEggFastMine = false;

    @ConfigInfo(name = "villagerHeal", comments = "村民每 80 游戏刻治疗 1 点生命，保留复原事件。")
    public static volatile boolean villagerHeal = false;

    @ConfigInfo(name = "blockEventPacketRange", comments = "方块事件包的发送距离（0–256 格；64 为原版）。")
    public static volatile double blockEventPacketRange = 64.0D;

    @ConfigInfo(name = "creativeInstantTame", comments = "创造玩家可立即驯服猫、鹦鹉、狼和马，保留驯服事件。")
    public static volatile boolean creativeInstantTame = false;

    @ConfigInfo(name = "creativeNetherWaterPlacement", comments = "仅允许创造玩家在水会蒸发的维度用桶放水。")
    public static volatile boolean creativeNetherWaterPlacement = false;

    @ConfigInfo(name = "dropperNoItemCost", comments = "投掷器不消耗物品，保留发射、库存移动事件及本地目标检查。")
    public static volatile boolean dropperNoItemCost = false;

    @ConfigInfo(name = "elytraFireworkKeepLeashConnection", comments = "鞘翅烟花加速不主动解开拴绳；其他断开原因不变。")
    public static volatile boolean elytraFireworkKeepLeashConnection = false;

    @ConfigInfo(name = "entityMomentumLoss", comments = "读取存档时限制单轴速度；关闭后保留有限的高速分量。")
    public static volatile boolean entityMomentumLoss = true;

    @HotReloadUnsupported
    @ConfigInfo(name = "entityPathNavigationStuckDetectionUseRealTimeReintroduced", comments = "寻路卡住检测改用真实时间而非游戏刻。")
    public static volatile boolean entityPathNavigationStuckDetectionUseRealTimeReintroduced = false;

    @ConfigInfo(name = "explosionNoEntityInfluence", comments = "爆炸不伤害或击退实体；方块爆炸和事件保持原路径。")
    public static volatile boolean explosionNoEntityInfluence = false;

    @ConfigInfo(name = "fluidDestructionDisabled", comments = "流体不会替换非空气方块；保留含水方块正常路径。")
    public static volatile boolean fluidDestructionDisabled = false;

    @ConfigInfo(name = "gazeDisguiseEquipmentExtended", comments = "头颅也作为凝视检测的伪装。")
    public static volatile boolean gazeDisguiseEquipmentExtended = false;

    @ConfigInfo(name = "itemEntitySkipMovementDisabled", comments = "掉落物不跳过静止移动刻。")
    public static volatile boolean itemEntitySkipMovementDisabled = false;

    @ConfigInfo(name = "moveableReinforcedDeepslate", comments = "允许活塞推动强化深板岩，仍服从活塞的区域限制。")
    public static volatile boolean moveableReinforcedDeepslate = false;

    @ConfigInfo(name = "naturalSpawningUse13Heightmap", comments = "自然生成使用修正后的 1.13 风格高度采样。")
    public static volatile boolean naturalSpawningUse13Heightmap = false;

    @ConfigInfo(name = "naturalSpawningUse13HeightmapExtra", comments = "使用 1.13 风格高度时，也忽略活塞、蜂蜜块和黏液块。")
    public static volatile boolean naturalSpawningUse13HeightmapExtra = false;

    @ConfigInfo(name = "oakBalloonPercent", comments = "无花条件下的橡树优先选择大橡树的百分比，非正值使用原版。")
    public static volatile int oakBalloonPercent = -1;

    @ConfigInfo(name = "repeaterHalfDelay", comments = "红石矿石上方的中继器延迟减半，比较器不受影响。")
    public static volatile boolean repeaterHalfDelay = false;

    @ConfigInfo(name = "structureBlockDoNotPreserveFluid", comments = "结构方块加载时忽略水涝保留。")
    public static volatile boolean structureBlockDoNotPreserveFluid = false;

    @ConfigInfo(name = "vaultBlacklistDisabled", comments = "宝库忽略并不再添加已奖励玩家；旧 Mili 选项仍只阻止更新名单。")
    public static volatile boolean vaultBlacklistDisabled = false;

    @ConfigInfo(name = "voidDamageAmount", comments = "设置虚空伤害；0 禁用，4 保留 Paper 的世界值。")
    public static volatile double voidDamageAmount = 4.0;

    @ConfigInfo(name = "voidDamageIgnorePlayer", comments = "不受虚空伤害的玩家模式：false、true 或以逗号分隔的模式名。")
    public static volatile String voidDamageIgnorePlayer = "false";

    @ConfigInfo(name = "voidRelatedAltitude", comments = "虚空判定相对世界最小高度的偏移；-64 保留 Paper 值。")
    public static volatile double voidRelatedAltitude = -64.0;

    @ConfigInfo(name = "xpTrackingDistance", comments = "经验球跟随范围（0–128 格；0 禁止吸引，8 为原版）。")
    public static volatile double xpTrackingDistance = 8.0;

    @ConfigInfo(name = "zombifiedPiglinDropLootIfAngryReintroduced", comments = "敌对玩家的僵尸猪灵恢复旧的击杀掉落资格。")
    public static volatile boolean zombifiedPiglinDropLootIfAngryReintroduced = false;

    public static double hardcodeTNTangle() {
        double value = hardcodeTNTangle;
        return Double.isFinite(value) ? Math.clamp(value, -1, 6.283185307179586) : -1.0;
    }

    public static int pingPlayerListLimit() {
        int value = pingPlayerListLimit;
        return Math.clamp(value, 0, 1024);
    }

    public static int sculkSensorRange() {
        int value = sculkSensorRange;
        return Math.clamp(value, 1, 64);
    }

    public static int easyGetPitcherPod() {
        int value = easyGetPitcherPod;
        return Math.clamp(value, 0, 64);
    }

    public static int furnaceSmeltingTimeController() {
        int value = furnaceSmeltingTimeController;
        return Math.clamp(value, -1, 32767);
    }

    public static int maxChainUpdateDepth() {
        int value = maxChainUpdateDepth;
        return Math.clamp(value, -1, 10000000);
    }

    public static double tntPowerController() {
        double value = tntPowerController;
        return Double.isFinite(value) ? Math.clamp(value, -1, 128) : -1.0;
    }

    public static long customPiglinBarteringTime() {
        long value = customPiglinBarteringTime;
        return Math.clamp(value, -1L, 1200000L);
    }

    public static int setAnvilExperienceConsumptionLimit() {
        int value = setAnvilExperienceConsumptionLimit;
        return Math.clamp(value, -1, 32767);
    }

    public static double blockEventPacketRange() {
        double value = blockEventPacketRange;
        return Double.isFinite(value) ? Math.clamp(value, 0, 256) : 64.0D;
    }

    public static int oakBalloonPercent() {
        int value = oakBalloonPercent;
        return Math.clamp(value, -1, 100);
    }

    public static double voidDamageAmount() {
        double value = voidDamageAmount;
        return Double.isFinite(value) ? Math.clamp(value, 0, 1000000) : 4.0;
    }

    public static double voidRelatedAltitude() {
        double value = voidRelatedAltitude;
        return Double.isFinite(value) ? Math.clamp(value, -4096, 0) : -64.0;
    }

    public static double xpTrackingDistance() {
        double value = xpTrackingDistance;
        return Double.isFinite(value) ? Math.clamp(value, 0, 128) : 8.0;
    }

    @Override
    public void onLoaded(final TomlConfigData configInstance, final @Nullable Set<Exception> exceptions) {
    }

    @Override
    public void onUnloaded(final TomlConfigData configInstance) {
    }
}
