package fun.bm.mili.carpet.config.modules;

import fun.bm.mili.rust.TomlConfigData;
import java.util.Set;
import me.earthme.luminol.config.IConfigModule;
import me.earthme.luminol.config.flags.ConfigClassInfo;
import me.earthme.luminol.config.flags.ConfigInfo;
import me.earthme.luminol.config.flags.HotReloadUnsupported;
import me.earthme.luminol.enums.EnumConfigCategory;
import org.jetbrains.annotations.Nullable;

@ConfigClassInfo(category = EnumConfigCategory.ROOT, name = "entity-gameplay", directory = {"carpet"},
    comments = "Carpet 26.3 兼容玩法。默认保留 Mili、Paper 和原版行为。")
public final class EntityGameplayConfig implements IConfigModule {
    @HotReloadUnsupported
    @ConfigInfo(name = "creativeFlySpeed", comments = "创造模式飞行速度倍率，使用原生客户端能力包与相同服务端倍率。重启生效。")
    public static volatile double creativeFlySpeed = 1.0;

    @ConfigInfo(name = "creativeFlyDrag", comments = "创造模式飞行离地时的水平阻力（0–1，原版 0.09）；不改变飞行速度或其他模式的飞行。")
    public static volatile double creativeFlyDrag = 0.09D;

    public static double creativeFlyDrag() {
        double value = creativeFlyDrag;
        return Double.isFinite(value) && value >= 0.0D && value <= 1.0D ? value : 0.09D;
    }

    @ConfigInfo(name = "huskSpawningInTemples", comments = "沙漠神殿范围内的敌对生成表改为尸壳。")
    public static volatile boolean huskSpawningInTemples = false;

    @ConfigInfo(name = "missingTools", comments = "支持用镐的采矿速度加快挖掘玻璃类方块；非镐工具不变。")
    public static volatile boolean missingTools = false;

    @ConfigInfo(name = "persistentParrots", comments = "普通坠落不卸下鹦鹉；伤害成功后落地概率约为伤害/15，潜行维持普通卸下行为。")
    public static volatile boolean persistentParrots = false;

    @ConfigInfo(name = "pickaxeMinedBedrock", comments = "持镐可挖掘基岩，硬度按 50 处理；仍经原生破坏权限和事件。")
    public static volatile boolean pickaxeMinedBedrock = false;

    @ConfigInfo(name = "piglinsSpawningInBastions", comments = "堡垒遗迹部件生成猪灵、猪灵蛮兵和疣猪兽。")
    public static volatile boolean piglinsSpawningInBastions = false;

    @ConfigInfo(name = "renewableSponges", comments = "普通守卫者被闪电转化为远古守卫者，保留变形和生成事件。")
    public static volatile boolean renewableSponges = false;

    @ConfigInfo(name = "shulkerSpawningInEndCities", comments = "末地城部件内使用潜影贝生成表。")
    public static volatile boolean shulkerSpawningInEndCities = false;

    @ConfigInfo(name = "breedableParrots", comments = "驯服鹦鹉可用配置的物品 ID 进入原生繁殖；none 禁用。")
    public static volatile String breedableParrots = "none";

    @HotReloadUnsupported
    @ConfigInfo(name = "craftableCarvedPumpkin", comments = "用普通南瓜和剪刀无序合成雕刻南瓜；按实际取出次数消耗剪刀耐久。重启生效。")
    public static volatile boolean craftableCarvedPumpkin = false;

    @ConfigInfo(name = "easyRefreshTrades", comments = "主手交互使未交易的新手村民刷新当级报价；持绿宝石块不刷新。")
    public static volatile boolean easyRefreshTrades = false;

    @ConfigInfo(name = "experimentalMinecartSpeed", comments = "启用原版矿车改进实验后，覆盖默认速度（方块/秒）；-1 保留游戏规则，插件实体级设置优先。")
    public static volatile int experimentalMinecartSpeed = -1;

    @ConfigInfo(name = "fakePeace", comments = "禁止选定维度的自然敌对生成；接受 true 或逗号分隔维度名。")
    public static volatile String fakePeace = "false";

    @ConfigInfo(name = "fakePlayerPickUpController", comments = "Mili 假玩家捡取：false 正常、NoPickUp 禁用、MainHandOnly 主手空时才允许。")
    public static volatile String fakePlayerPickUpController = "false";

    @ConfigInfo(name = "fasterMovement", comments = "使用可同步、非持久的原版移动速度属性加成；VANILLA 关闭，Ⅰ–Ⅴ 逐级提高。")
    public static volatile String fasterMovement = "VANILLA";

    @ConfigInfo(name = "fasterMovementController", comments = "加速仅作用于指定维度：all、overworld、nether 或 end。")
    public static volatile String fasterMovementController = "all";

    @ConfigInfo(name = "headHunter", comments = "默认把玩家自身头颅加入原生死亡掉落（保留物品栏时不加）。")
    public static volatile boolean headHunter = false;

    @ConfigInfo(name = "infiniteTrades", comments = "普通交易不消耗次数；保留插件购买事件、原存档次数及 Mili 暂存虚空交易次数。")
    public static volatile boolean infiniteTrades = false;

    @ConfigInfo(name = "kirinArm", comments = "提高原生挖掘速度，保留不可破坏方块及事件。")
    public static volatile boolean kirinArm = false;

    @ConfigInfo(name = "quickVillagerLevelUp", comments = "主手交互使有报价且未满级的村民升一级，使用 Paper 原生职业升级路径。")
    public static volatile boolean quickVillagerLevelUp = false;

    @ConfigInfo(name = "renewableNetheriteScrap", comments = "成年僵尸猪灵在正常死亡掉落中额外掉落下界合金碎片的概率。")
    public static volatile double renewableNetheriteScrap = 0.0;

    @ConfigInfo(name = "sharedVillagerDiscounts", comments = "当前村民共享正向治愈折扣，保留玩家其他声望。")
    public static volatile boolean sharedVillagerDiscounts = false;

    @ConfigInfo(name = "superBow", comments = "弓的无限和经验修补可以共存；不改变其他附魔或适用物品。")
    public static volatile boolean superBow = false;

    @ConfigInfo(name = "superLeash", comments = "放宽生物和村民的拴绳判定；实际拴绳仍通过原生交互和插件事件。")
    public static volatile boolean superLeash = false;

    @ConfigInfo(name = "beaconRangeExpand", comments = "调整信标水平半径；效果和事件由受益玩家归属线程执行。")
    public static volatile int beaconRangeExpand = 0;

    @ConfigInfo(name = "beaconWorldHeight", comments = "规则启用后信标按世界高度覆盖其水平范围；仍保留 Paper 信标效果事件。")
    public static volatile boolean beaconWorldHeight = false;

    @ConfigInfo(name = "damageEnchantmentCompatible", comments = "同组原版伤害附魔之间解除互斥；保留其他数据包互斥。")
    public static volatile boolean damageEnchantmentCompatible = false;

    @ConfigInfo(name = "experienceOrbMerge", comments = "邻近经验球跨数值合并，合并后经验总量不变且保留 Paper 可取消合并事件。")
    public static volatile boolean experienceOrbMerge = false;

    @ConfigInfo(name = "forceRestock", comments = "没有其他菜单时，从主物品栏为已有物品的快捷栏补齐同物品和组件的堆叠。")
    public static volatile boolean forceRestock = false;

    @ConfigInfo(name = "knockbackStick", comments = "允许铁砧为木棍应用击退，仍须经验和合法附魔。")
    public static volatile boolean knockbackStick = false;

    @ConfigInfo(name = "limitPhantomSpawn", comments = "幻翼群遵守当前区域刷怪上限，只有原生生成事件未取消时才计数。")
    public static volatile boolean limitPhantomSpawn = false;

    @ConfigInfo(name = "openVillagerInventory", comments = "潜行主手交互打开村民实际八个库存槽，使用原生菜单和插件事件。")
    public static volatile boolean openVillagerInventory = false;

    @ConfigInfo(name = "playerDropHead", comments = "带电苦力怕击杀时在原生玩家死亡事件中提供玩家头颅。")
    public static volatile boolean playerDropHead = false;

    @ConfigInfo(name = "protectionEnchantmentCompatible", comments = "四种原版保护附魔之间解除互斥。")
    public static volatile boolean protectionEnchantmentCompatible = false;

    @ConfigInfo(name = "superChargedCreeper", comments = "带电苦力怕不受单次头颅掉落上限；保留原掉落逻辑。")
    public static volatile boolean superChargedCreeper = false;

    @ConfigInfo(name = "villagerInfiniteTrade", comments = "与 AMS 无限交易共享同一报价路径，不重置原存档。")
    public static volatile boolean villagerInfiniteTrade = false;

    @ConfigInfo(name = "entityPlacementIgnoreCollision", comments = "船、矿车、盔甲架与末地水晶可忽略放置碰撞；不放开点击权限、底座、实体放置或生成事件。")
    public static volatile boolean entityPlacementIgnoreCollision = false;

    @ConfigInfo(name = "minecartPlaceableOnGround", comments = "在没有铁轨的承载方块上面放置矿车；归属、出生空间和 EntityPlaceEvent 仍生效。")
    public static volatile boolean minecartPlaceableOnGround = false;

    @ConfigInfo(name = "overspawningReintroduced", comments = "允许一次已通过上限检查的生成批次超过剩余额度；保留实际计数。")
    public static volatile boolean overspawningReintroduced = false;

    @ConfigInfo(name = "preciseEntityPlacement", comments = "盔甲架、水晶与刷怪蛋使用点击的水平位置（限制在原生目标格）；垂直位置、NBT 权限和生成事件保持原生。")
    public static volatile boolean preciseEntityPlacement = false;

    @HotReloadUnsupported
    @ConfigInfo(name = "renewableDragonEgg", comments = "龙蛋随机刻用附近的龙息云复制；只使用当前持有的已加载区块，传播取消时不消耗龙息。重启生效。")
    public static volatile boolean renewableDragonEgg = false;

    @ConfigInfo(name = "renewableElytra", comments = "由潜影贝击杀的幻翼在正常死亡掉落中掉落鞘翅的概率。")
    public static volatile double renewableElytra = 0.0;

    public static double creativeFlySpeed() {
        double value = creativeFlySpeed;
        return Double.isFinite(value) ? Math.clamp(value, 0.1, 4) : 1.0;
    }

    public static int experimentalMinecartSpeed() {
        int value = experimentalMinecartSpeed;
        return Math.clamp(value, -1, 1000);
    }

    public static double renewableNetheriteScrap() {
        double value = renewableNetheriteScrap;
        return Double.isFinite(value) ? Math.clamp(value, 0, 1) : 0.0;
    }

    public static int beaconRangeExpand() {
        int value = beaconRangeExpand;
        return Math.clamp(value, -1024, 1024);
    }

    public static double renewableElytra() {
        double value = renewableElytra;
        return Double.isFinite(value) ? Math.clamp(value, 0, 1) : 0.0;
    }

    @Override
    public void onLoaded(final TomlConfigData configInstance, final @Nullable Set<Exception> exceptions) {
    }

    @Override
    public void onUnloaded(final TomlConfigData configInstance) {
    }
}
