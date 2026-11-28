package fun.bm.mili.carpet.config.modules;

import fun.bm.mili.rust.TomlConfigData;
import me.earthme.luminol.config.IConfigModule;
import me.earthme.luminol.config.flags.ConfigClassInfo;
import me.earthme.luminol.config.flags.ConfigInfo;
import me.earthme.luminol.enums.EnumConfigCategory;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.Set;

@ConfigClassInfo(
        category = EnumConfigCategory.ROOT,
        name = "general",
        directory = {"carpet"},
        comments = """
                Carpet/AMS/TIS/Org 兼容规则，由 Mili 现有功能支持。
                此处仅暴露已有可用服务端实现对应的规则。"""
)
public class GeneralCompatConfig implements IConfigModule {
    @ConfigInfo(name = "language", comments = """
            Carpet 语言值，转发到 mili.function.language.lang。""")
    public static String language = "en_us";

    @ConfigInfo(name = "amsUpdateSuppressionCrashFix", comments = """
            将 AMS 更新抑制崩溃保护映射到 Mili 现有的崩溃修复。""")
    public static boolean amsUpdateSuppressionCrashFix = false;

    @ConfigInfo(name = "yeetUpdateSuppressionCrash", comments = """
            将 TIS 更新抑制崩溃移除映射到同一 Mili 崩溃修复。""")
    public static boolean yeetUpdateSuppressionCrash = false;

    @ConfigInfo(name = "dustTrapdoorReintroduced", comments = """
            将活板门上红石粉的行为映射到 Mili 的红石忽略向上更新选项。""")
    public static boolean dustTrapdoorReintroduced = false;

    @ConfigInfo(name = "shulkerBoxCCEReintroduced", comments = """
            仅兼容旧 TOML。26.3 不再用类型转换异常中断比较器更新。""")
    public static boolean shulkerBoxCCEReintroduced = false;

    @ConfigInfo(name = "instantBlockUpdaterReintroduced", comments = """
            启用 Mili 已携带的即时方块更新器补丁。""")
    public static boolean instantBlockUpdaterReintroduced = false;

    @ConfigInfo(name = "commandTick", comments = """
            启用 Mili 已补丁的 tick 命令支持。""")
    public static boolean commandTick = false;

    @ConfigInfo(name = "creativeNoClip", comments = """
            启用现有的创造模式飞行穿墙实现。""")
    public static boolean creativeNoClip = false;

    @ConfigInfo(name = "optimizedDragonRespawn", comments = """
            启用 Luminol 已有的优化末影龙重生实现。""")
    public static boolean optimizedDragonRespawn = false;

    @ConfigInfo(name = "antiSpamDisabled", comments = """
            禁用原版/Spigot 使用的服务端聊天和创造模式丢弃刷屏限制。""")
    public static boolean antiSpamDisabled = false;

    @ConfigInfo(name = "blockPlacementIgnoreEntity", comments = """
            允许创造模式玩家放置方块时不进行实体碰撞检查。""")
    public static boolean blockPlacementIgnoreEntity = false;

    @ConfigInfo(name = "creativeOpenContainerForcibly", comments = """
            允许创造模式玩家强制打开被阻挡的箱子、末影箱和潜影盒。""")
    public static boolean creativeOpenContainerForcibly = false;

    @ConfigInfo(name = "creativeOneHitKill", comments = """
            允许创造模式玩家一击杀死可攻击的非创造、非旁观实体。
            蹲下时该效果扩展为小范围群体攻击。""")
    public static boolean creativeOneHitKill = false;

    @ConfigInfo(name = "creativeHitRemoveEntity", comments = "创造模式命中后直接移除非玩家实体，不走死亡掉落；持剑时可横扫同一区域的目标。优先于一击击杀。")
    public static volatile boolean creativeHitRemoveEntity = false;

    @ConfigInfo(name = "observerNoDetection", comments = """
            完全禁用侦测器的检测脉冲。""")
    public static boolean observerNoDetection = false;

    @ConfigInfo(name = "bambooModelNoOffset", comments = """
            移除竹子和竹子幼苗的随机水平模型偏移。""")
    public static boolean bambooModelNoOffset = false;

    @ConfigInfo(name = "creativeNoItemCooldown", comments = """
            跳过创造模式玩家的物品冷却应用。""")
    public static boolean creativeNoItemCooldown = false;

    @ConfigInfo(name = "ctrlQCraftingFix", comments = """
            当前菜单代码中已存在的上游结果栏 Ctrl+Q 合成修复的兼容标志。""")
    public static boolean ctrlQCraftingFix = false;

    @ConfigInfo(name = "carpetAlwaysSetDefault", comments = """
            Mili 配置加载器的兼容标志，该加载器已在预加载阶段将默认值写入兼容配置。""")
    public static boolean carpetAlwaysSetDefault = false;

    @ConfigInfo(name = "placementRotationFix", comments = """
            使用玩家身体主旋转而非插值头部偏航来进行放置方向检查。""")
    public static boolean placementRotationFix = false;

    @ConfigInfo(name = "tntDoNotUpdate", comments = """
            阻止 TNT 在首次放置时检查红石信号。""")
    public static boolean tntDoNotUpdate = false;

    @ConfigInfo(name = "totallyNoBlockUpdate", comments = """
            全局抑制方块变更的邻居和形状更新。""")
    public static boolean totallyNoBlockUpdate = false;

    @ConfigInfo(name = "tiscmNetworkProtocol", comments = """
            启用 tiscm:network/v1 上的原生 Carpet TIS Addition 网络通道。""")
    public static boolean tiscmNetworkProtocol = false;

    @ConfigInfo(name = "hopperNoItemCost", comments = """
            在漏斗上方放置羊毛块时，将已传输的物品堆恢复到漏斗中。""")
    public static boolean hopperNoItemCost = false;

    @ConfigInfo(name = "explosionNoBlockDamage", comments = """
            让爆炸只伤害实体而不破坏方块。""")
    public static boolean explosionNoBlockDamage = false;

    @ConfigInfo(name = "optimizedTNTHighPriority", comments = """
            当前运行时已携带的优化服务端爆炸路径的兼容标志。""")
    public static boolean optimizedTNTHighPriority = false;

    @ConfigInfo(name = "tntPrimerMomentumRemoved", comments = """
            移除新点燃 TNT 的随机水平发射动量。""")
    public static boolean tntPrimerMomentumRemoved = false;

    @ConfigInfo(name = "tntIgnoreRedstoneSignal", comments = """
            在决定 TNT 是否自动点燃时忽略红石信号。""")
    public static boolean tntIgnoreRedstoneSignal = false;

    @ConfigInfo(name = "tntDupingFix", comments = """
            切换原版 TNT 复制装置使用的活塞不同步路径。""")
    public static boolean tntDupingFix = false;

    @ConfigInfo(name = "interactionUpdates", comments = """
            控制玩家交互的方块变更是否发出正常方块更新。
            设为 false 可抑制方块使用和破坏过程中的邻居和形状更新。""")
    public static boolean interactionUpdates = true;

    @ConfigInfo(name = "xpNoCooldown", comments = """
            允许玩家在同一 tick 内吸收多个经验球而不受拾取延迟限制。""")
    public static boolean xpNoCooldown = false;

    @ConfigInfo(name = "powerfulExpMending", comments = """
            让拾取的经验修复玩家物品栏中所有受损的附魔修复物品，而非仅限装备栏。""")
    public static boolean powerfulExpMending = false;

    @ConfigInfo(name = "clientSettingsLostOnRespawnFix", comments = """
            在重生后重新应用玩家上次已知的客户端设置。""")
    public static boolean clientSettingsLostOnRespawnFix = false;

    @ConfigInfo(name = "sensibleEnderman", comments = """
            限制末影人拾取方块仅为南瓜和西瓜。""")
    public static boolean sensibleEnderman = false;

    @ConfigInfo(name = "entityInstantDeathRemoval", comments = """
            移除死亡生物实体被清除前的正常 20 刻延迟。""")
    public static boolean entityInstantDeathRemoval = false;

    @ConfigInfo(name = "farmlandTrampledDisabled", comments = """
            阻止耕地在实体落地时变为泥土。""")
    public static volatile boolean farmlandTrampledDisabled = false;

    @ConfigInfo(name = "shulkerGolem", comments = """
            允许在潜影盒上方放置雕刻南瓜来召唤潜影贝。""")
    public static boolean shulkerGolem = false;

    @ConfigInfo(name = "preventEndSpikeRespawn", comments = """
            在末影龙重生时跳过黑曜石柱的重新生成。""")
    public static volatile boolean preventEndSpikeRespawn = false;

    @ConfigInfo(name = "yeetOutOfOrderChatKick", comments = """
            忽略乱序安全聊天链检查，而非使聊天会话失效。""")
    public static boolean yeetOutOfOrderChatKick = false;

    @ConfigInfo(name = "betterCraftableBoneBlock", comments = """
            添加 AMS 的替代骨块合成配方：9 个骨头产出 3 个骨块。""")
    public static boolean betterCraftableBoneBlock = false;

    @ConfigInfo(name = "betterCraftableDispenser", comments = """
            添加 AMS 的替代发射器合成配方：使用投掷器合成。""")
    public static boolean betterCraftableDispenser = false;

    @ConfigInfo(name = "viewDistance", comments = """
            用 Carpet 兼容值覆盖专用服务器的启动视距。""")
    public static int viewDistance = 12;

    @ConfigInfo(name = "tickCommandPermission", comments = """
            覆盖 /tick 命令的权限等级。
            接受 0..4 范围的值，其中 2 对应旧版 Carpet 行为，3 保持原版。""")
    public static int tickCommandPermission = 3;

    @ConfigInfo(name = "tickFreezeCommandToggleable", comments = """
            使 /tick freeze 在服务器已冻结时再次执行可切换回运行状态。""")
    public static boolean tickFreezeCommandToggleable = false;

    @ConfigInfo(name = "syncServerMsptMetricsData", comments = """
            通过原生 TISCM 协议通道广播实时 MSPT 采样数据。""")
    public static boolean syncServerMsptMetricsData = false;

    @ConfigInfo(name = "simpleInGameCalculator", comments = """
            将以 = 开头的聊天消息作为简单计算器表达式求值并私信回复。""")
    public static boolean simpleInGameCalculator = false;

    @ConfigInfo(name = "microTiming", comments = """
            允许有权限的玩家短时观察附近已加载方块的更新顺序；重载配置会清除观察会话。""")
    public static volatile boolean microTiming = false;

    @ConfigInfo(name = "fastRedstoneDust", comments = """
            通过 Alternate Current 快速更新后端路由红石粉更新。""")
    public static boolean fastRedstoneDust = false;

    @ConfigInfo(name = "lagFreeSpawning", comments = """
            使用轻量级碰撞和预处理生物生成路径进行自然生成检查。""")
    public static boolean lagFreeSpawning = false;

    @ConfigInfo(name = "optimizedFastEntityMovement", comments = """
            Moonrise/Paper 始终启用的快速实体移动碰撞管线的兼容标志。""")
    public static boolean optimizedFastEntityMovement = false;

    @ConfigInfo(name = "optimizedHardHitBoxEntityCollision", comments = """
            Moonrise/Paper 始终启用的硬碰撞箱实体碰撞优化的兼容标志。""")
    public static boolean optimizedHardHitBoxEntityCollision = false;

    @ConfigInfo(name = "tntFuseDuration", comments = """
            覆盖默认的已点燃 TNT 引信时长（单位：刻）。
            接受 0..32767 范围的值。""")
    public static int tntFuseDuration = 80;

    @ConfigInfo(name = "defaultLoggers", comments = """
            Carpet 风格的玩家默认日志订阅。
            示例：["tps", "mob_caps", "counter white"]""")
    public static List<String> defaultLoggers = List.of();

    @ConfigInfo(name = "renewableBlackstone", comments = "熔岩在上方或水平相邻有蓝冰时形成黑石；原版水和玄武岩优先。")
    public static volatile boolean renewableBlackstone = false;

    @ConfigInfo(name = "liquidDamageDisabled", comments = "禁止水和熔岩冲毁普通方块；仍允许流入空气、液体和可含液体的方块。")
    public static volatile boolean liquidDamageDisabled = false;

    @ConfigInfo(name = "thickFungusGrowth", comments = "人工种植巨型菌的粗柄模式：false（原版）、random（6%）或 all（总是）。")
    public static volatile String thickFungusGrowth = "false";

    @ConfigInfo(name = "extinguishedCampfire", comments = "放置营火时不点燃（不影响放置之后手动点燃）。")
    public static volatile boolean extinguishedCampfire = false;

    @ConfigInfo(name = "easyCompost", comments = "可堆肥物品默认每次增加一级，仍触发 Paper 堆肥事件。")
    public static volatile boolean easyCompost = false;

    @ConfigInfo(name = "undyingCoral", comments = "珊瑚块、珊瑚、珊瑚扇和墙上的珊瑚扇离水后不死亡。")
    public static volatile boolean undyingCoral = false;

    @ConfigInfo(name = "endermanTeleportRandomlyDisabled", comments = "禁用末影人的无指定目标随机传送，不禁用带目标的传送。")
    public static volatile boolean endermanTeleportRandomlyDisabled = false;

    @ConfigInfo(name = "disableBatCanSpawn", comments = "禁用蝙蝠的自然生成判定，不影响命令等直接生成。")
    public static volatile boolean disableBatCanSpawn = false;

    @ConfigInfo(name = "disableWaterFreezes", comments = "禁用生物群系的自然结冰判定，不影响冰霜行者。")
    public static volatile boolean disableWaterFreezes = false;

    @ConfigInfo(name = "reusableSmithingTemplate", comments = "false（原版）、upgrade（只保留下界合金升级模板）或 true（所有锻造模板均可重复使用）。")
    public static volatile String reusableSmithingTemplate = "false";

    @ConfigInfo(name = "disableFurnaceDropExperience", comments = "禁止熔炉默认掉落经验；插件仍可通过原有事件自定义掉落。")
    public static volatile boolean disableFurnaceDropExperience = false;

    @ConfigInfo(name = "staringEndermanNotAngry", comments = "末影人默认不会因被凝视而发怒；插件仍可通过 Paper 事件覆盖。")
    public static volatile boolean staringEndermanNotAngry = false;

    @ConfigInfo(name = "turtleEggTrampledDisabled", comments = "阻止实体踩碎海龟蛋，包括僵尸主动破坏海龟蛋；不影响玩家正常挖掘。")
    public static volatile boolean turtleEggTrampledDisabled = false;

    @ConfigInfo(name = "undeadDontBurnInSunlight", comments = "禁用亡灵生物因日照起火；不影响其他来源点燃。")
    public static volatile boolean undeadDontBurnInSunlight = false;

    @ConfigInfo(name = "leaderZombieSpawnWithMaxHealthDisabled", comments = "首领僵尸生成时不自动补满额外生命值；保留最大生命属性加成。")
    public static volatile boolean leaderZombieSpawnWithMaxHealthDisabled = false;

    @ConfigInfo(name = "endPortalOpenedSoundDisabled", comments = "禁用打开末地传送门时的广播音效；不影响生成传送门。")
    public static volatile boolean endPortalOpenedSoundDisabled = false;

    @ConfigInfo(name = "toughWitherRose", comments = "允许凋零玫瑰在任意支撑材质上存活。")
    public static volatile boolean toughWitherRose = false;

    @ConfigInfo(name = "endermanPickUpDisabled", comments = "禁止末影人主动拾取方块，不影响已持有方块的放置。")
    public static volatile boolean endermanPickUpDisabled = false;

    @ConfigInfo(name = "breedingCooldownDisabled", comments = "禁止成年生物进入繁殖冷却，不影响幼年生长。")
    public static volatile boolean breedingCooldownDisabled = false;

    @ConfigInfo(name = "snowMeltMinLightLevel", comments = "雪层融化所需最低方块光照。12 为原版，16 禁用光照融化。")
    public static volatile int snowMeltMinLightLevel = 12;

    @ConfigInfo(name = "flippinCactus", comments = "主手仙人掌右键旋转支持的方块，副手仙人掌反转放置方向。")
    public static volatile boolean flippinCactus = false;

    @ConfigInfo(name = "flippinCactusExtras", comments = "仙人掌也可旋转木桶、合成器、雕纹书架和置物架。")
    public static volatile boolean flippinCactusExtras = false;

    @ConfigInfo(name = "flippinCactusSoundEffect", comments = "旋转音效：0 关闭，1–5 为 AMS 音效。")
    public static volatile int flippinCactusSoundEffect = 0;

    @ConfigInfo(name = "rotatorBlock", comments = "装有仙人掌的发射器旋转前方方块，不消耗仙人掌。")
    public static volatile boolean rotatorBlock = false;

    @ConfigInfo(name = "dispensersFireDragonBreath", comments = "发射器可释放龙息云；只在目标范围的区块均已加载且由当前线程持有时生成。")
    public static volatile boolean dispensersFireDragonBreath = false;

    @ConfigInfo(name = "dispenserNoItemCost", comments = "发射器保留被选取的物品及其耐久，但仍按原有行为返还容器物品；不影响投掷器。")
    public static volatile boolean dispenserNoItemCost = false;

    @ConfigInfo(name = "noCreeperBlockBreaking", comments = "苦力怕的爆炸不破坏方块，保留爆炸事件和实体伤害。")
    public static volatile boolean noCreeperBlockBreaking = false;

    @ConfigInfo(name = "noGhastBlockBreaking", comments = "恶魂大火球爆炸不破坏方块。")
    public static volatile boolean noGhastBlockBreaking = false;

    @ConfigInfo(name = "disableGhastFire", comments = "大火球爆炸不点燃方块。")
    public static volatile boolean disableGhastFire = false;

    @ConfigInfo(name = "disableBlazeFire", comments = "阻止生物发射的小火球点燃方块，不改发射器火球。")
    public static volatile boolean disableBlazeFire = false;

    @me.earthme.luminol.config.flags.HotReloadUnsupported
    @ConfigInfo(name = "largeBarrel", comments = "启用同一区块内、背靠背且各有 27 格的木桶配对。仅 barrel_rows 为 3 时有效；修改此开关需重启。")
    public static volatile boolean largeBarrel = false;

    public static int normalizedSnowMeltMinLightLevel() {
        return Math.clamp(snowMeltMinLightLevel, 0, 16);
    }

    public static int normalizedTntFuseDuration() {
        return Math.clamp(tntFuseDuration, 0, Short.MAX_VALUE);
    }

    public static int normalizedTickCommandPermission() {
        return Math.clamp(tickCommandPermission, 0, 4);
    }

    public static boolean mergedUpdateSuppressionCrashEnabled() {
        return amsUpdateSuppressionCrashFix || yeetUpdateSuppressionCrash;
    }

    @Override
    public void onLoaded(TomlConfigData configInstance, @Nullable Set<Exception> exs) {
        fun.bm.mili.carpet.timing.MicroTiming.resetWatches();
        fun.bm.mili.protocol.tiscm.TISCMProtocol.configure(tiscmNetworkProtocol, syncServerMsptMetricsData);
    }

    @Override
    public void onUnloaded(TomlConfigData configInstance) {
        fun.bm.mili.carpet.timing.MicroTiming.resetWatches();
        fun.bm.mili.protocol.tiscm.TISCMProtocol.configure(false, false);
    }
}
