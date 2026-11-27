package fun.bm.mili.carpet.config.modules;

import me.earthme.luminol.config.IConfigModule;
import me.earthme.luminol.config.flags.ConfigClassInfo;
import me.earthme.luminol.config.flags.ConfigInfo;
import me.earthme.luminol.config.flags.HotReloadUnsupported;
import me.earthme.luminol.enums.EnumConfigCategory;

@ConfigClassInfo(category = EnumConfigCategory.ROOT, name = "world-gameplay", directory = {"carpet"},
    comments = "Carpet 26.3 世界玩法；默认保留 Mili、Paper 和原版行为。")
public final class WorldGameplayConfig implements IConfigModule {
    @ConfigInfo(name = "desertShrubs", comments = "干旱沙漠中的幼苗生长时变成枯萎的灌木。")
    public static volatile boolean desertShrubs = false;

    @ConfigInfo(name = "fertilizableSmallFlower", comments = "骨粉可复制小花和孢子花。")
    public static volatile boolean fertilizableSmallFlower = false;

    @ConfigInfo(name = "scheduledRandomTickAllPlants", comments = "植物兼容项的计划刻额外运行一次原生随机刻。")
    public static volatile boolean scheduledRandomTickAllPlants = false;

    @ConfigInfo(name = "scheduledRandomTickBamboo", comments = "在计划刻中额外运行一次原生生长判定。")
    public static volatile boolean scheduledRandomTickBamboo = false;

    @ConfigInfo(name = "scheduledRandomTickCactus", comments = "在计划刻中额外运行一次原生生长判定。")
    public static volatile boolean scheduledRandomTickCactus = false;

    @ConfigInfo(name = "scheduledRandomTickChorusFlower", comments = "在计划刻中额外运行一次原生生长判定。")
    public static volatile boolean scheduledRandomTickChorusFlower = false;

    @ConfigInfo(name = "scheduledRandomTickStem", comments = "在计划刻中额外运行一次原生生长判定。")
    public static volatile boolean scheduledRandomTickStem = false;

    @ConfigInfo(name = "scheduledRandomTickSugarCane", comments = "在计划刻中额外运行一次原生生长判定。")
    public static volatile boolean scheduledRandomTickSugarCane = false;

    @ConfigInfo(name = "fillUpdates", comments = "填充、单方块设置与结构放置是否执行方块形状及邻居更新。")
    public static volatile boolean fillUpdates = true;

    @ConfigInfo(name = "summonNaturalLightning", comments = "召唤闪电接受后进行自然骷髅马陷阱概率判定。")
    public static volatile boolean summonNaturalLightning = false;

    @ConfigInfo(name = "failSoftBlockStateParsing", comments = "方块放置命令参数忽略未知属性及无效的非空属性值。")
    public static volatile boolean failSoftBlockStateParsing = false;

    @ConfigInfo(name = "fillCommandModeEnhance", comments = "为原版填充命令添加 softreplace 模式。")
    public static volatile String fillCommandModeEnhance = "false";

    public static String fillCommandModeEnhance() {
        if (fillCommandModeEnhance == null) return "false";
        final String mode = fillCommandModeEnhance.trim().toLowerCase(java.util.Locale.ROOT);
        return switch (mode) {
            case "false", "true" -> mode;
            default -> "false";
        };
    }

    @ConfigInfo(name = "stopCommandDoubleConfirmation", comments = "玩家停止服务器需要同一会话在 60 秒内确认。")
    public static volatile boolean stopCommandDoubleConfirmation = false;

    @ConfigInfo(name = "optimizedTNT", comments = "使用 Carpet 的预计算爆炸射线顺序。")
    public static volatile boolean optimizedTNT = false;

    @ConfigInfo(name = "tntRandomRange", comments = "启用 optimizedTNT 时固定射线随机项；-1 使用原版随机。")
    public static volatile double tntRandomRange = -1.0;

    public static double tntRandomRange() {
        return Double.isFinite(tntRandomRange) ? Math.clamp(tntRandomRange, -1.0, 1.0) : -1.0;
    }

    @ConfigInfo(name = "itemAntiExplosion", comments = "掉落物免受爆炸伤害；no_blast_wave 同时免于爆炸击退。")
    public static volatile String itemAntiExplosion = "false";

    public static String itemAntiExplosion() {
        if (itemAntiExplosion == null) return "false";
        final String mode = itemAntiExplosion.trim().toLowerCase(java.util.Locale.ROOT);
        return switch (mode) {
            case "false", "true", "no_blast_wave" -> mode;
            default -> "false";
        };
    }

    @ConfigInfo(name = "disableWindChargeEffect", comments = "玩家风弹不触发方块；不影响击退和生物风弹。")
    public static volatile boolean disableWindChargeEffect = false;

    @ConfigInfo(name = "wetExplosionReintroduced", comments = "湿润爆炸源不影响掉落物、盔甲架、悬挂实体。")
    public static volatile boolean wetExplosionReintroduced = false;

    @ConfigInfo(name = "explosionPacketRange", comments = "同一 Folia 区域内的爆炸包距离（格）。")
    public static volatile double explosionPacketRange = 64.0;

    public static double explosionPacketRange() {
        return Double.isFinite(explosionPacketRange) ? Math.clamp(explosionPacketRange, 0.0, 256.0) : 64.0;
    }

    @HotReloadUnsupported
    @ConfigInfo(name = "entityTrackerDistance", comments = "覆盖实体类型默认追踪距离（区块）；保留 Paper 最终追踪配置。")
    public static volatile int entityTrackerDistance = -1;

    public static int entityTrackerDistance() {
        return Math.clamp(entityTrackerDistance, -1, 32);
    }

    @HotReloadUnsupported
    @ConfigInfo(name = "entityTrackerInterval", comments = "覆盖具有周期更新的实体类型间隔；-1 保留类型值。")
    public static volatile int entityTrackerInterval = -1;

    public static int entityTrackerInterval() {
        return Math.clamp(entityTrackerInterval, -1, 200);
    }

    @ConfigInfo(name = "chainStone", comments = "锁链沿自身轴连接；stick_to_all 允许连接非支撑方块。")
    public static volatile String chainStone = "false";

    public static String chainStone() {
        if (chainStone == null) return "false";
        final String mode = chainStone.trim().toLowerCase(java.util.Locale.ROOT);
        return switch (mode) {
            case "false", "true", "stick_to_all" -> mode;
            default -> "false";
        };
    }

    @ConfigInfo(name = "cryingObsidianNetherPortal", comments = "允许使用哭泣的黑曜石作为点火传送门边框。")
    public static volatile boolean cryingObsidianNetherPortal = false;

    @ConfigInfo(name = "customizedNetherPortal", comments = "保留手工建造的不完整传送门；允许在正常挖掘判定下破坏传送门。")
    public static volatile boolean customizedNetherPortal = false;

    @ConfigInfo(name = "netherPortalMaxSize", comments = "手工下界门最大内径（3–64 格）；目的地搜索也识别扩大后的传送门。")
    public static volatile int netherPortalMaxSize = 21;

    public static int netherPortalMaxSize() {
        return Math.clamp(netherPortalMaxSize, 3, 64);
    }

    @ConfigInfo(name = "violentNetherPortalCreation", comments = "允许传送门点火替换无流体、无方块实体的可替换方块。")
    public static volatile String violentNetherPortalCreation = "false";

    public static String violentNetherPortalCreation() {
        if (violentNetherPortalCreation == null) return "false";
        final String mode = violentNetherPortalCreation.trim().toLowerCase(java.util.Locale.ROOT);
        return switch (mode) {
            case "false", "replaceable" -> mode;
            default -> "false";
        };
    }

    @ConfigInfo(name = "redstoneComponentSound", comments = "在成功切换阳光探测器、红石粉或中继器时播放音效。")
    public static volatile boolean redstoneComponentSound = false;

    @ConfigInfo(name = "canActivatesObserver", comments = "用打火石或火焰弹触发侦测器的原生计划刻。")
    public static volatile boolean canActivatesObserver = false;

    @ConfigInfo(name = "simpleUpdateSkipper", comments = "跳过红石粉、中继器、比较器的向上形状更新，并关闭红石粉特殊活板门连接。")
    public static volatile boolean simpleUpdateSkipper = false;

    @ConfigInfo(name = "redstoneDustRepeaterComparatorIgnoreUpwardsStateUpdate", comments = "避免向上的形状更新移除红石粉、中继器或比较器。")
    public static volatile boolean redstoneDustRepeaterComparatorIgnoreUpwardsStateUpdate = false;

    @ConfigInfo(name = "redstoneDustRandomUpdateOrder", comments = "Vanilla 红石粉的广播顺序：false、true 或 random_pos。")
    public static volatile String redstoneDustRandomUpdateOrder = "false";

    public static String redstoneDustRandomUpdateOrder() {
        if (redstoneDustRandomUpdateOrder == null) return "false";
        final String mode = redstoneDustRandomUpdateOrder.trim().toLowerCase(java.util.Locale.ROOT);
        return switch (mode) {
            case "false", "true", "random_pos" -> mode;
            default -> "false";
        };
    }

    @ConfigInfo(name = "soundSuppressionSimulator", comments = "青金石矿上方的幽匿感测体不接收振动，不抛出异常。")
    public static volatile boolean soundSuppressionSimulator = false;

    @ConfigInfo(name = "updateSkippingSimulator", comments = "深层青金石矿上方的动力铁轨跳过熄灭状态写入及自身更新。")
    public static volatile boolean updateSkippingSimulator = false;

    @ConfigInfo(name = "updateSuppressionBlock", comments = "屏障上未供电激活铁轨压缩当前区域的剩余收集更新；-1 关闭。")
    public static volatile int updateSuppressionBlock = -1;

    public static int updateSuppressionBlock() {
        return Math.clamp(updateSuppressionBlock, -1, 1000000);
    }
}
