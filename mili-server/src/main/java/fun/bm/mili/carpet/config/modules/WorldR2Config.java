package fun.bm.mili.carpet.config.modules;

import me.earthme.luminol.config.IConfigModule;
import me.earthme.luminol.config.flags.ConfigClassInfo;
import me.earthme.luminol.config.flags.ConfigInfo;
import me.earthme.luminol.config.flags.HotReloadUnsupported;
import me.earthme.luminol.enums.EnumConfigCategory;

@ConfigClassInfo(category = EnumConfigCategory.ROOT, name = "world-r2", directory = {"carpet"},
    comments = "Carpet 26.3 第二轮世界规则；默认保留现有行为。")
public final class WorldR2Config implements IConfigModule {
    @ConfigInfo(name = "movableBlockEntities", comments = "允许活塞搬运无查看者的箱子、陷阱箱和木桶；配对必须在同一区块内。")
    public static volatile boolean movableBlockEntities = false;

    @ConfigInfo(name = "onlyPlayerCanCreateNetherPortal", comments = "非玩家只能使用目标维度已有的下界传送门。")
    public static volatile boolean onlyPlayerCanCreateNetherPortal = false;

    @ConfigInfo(name = "itemEntityCreateNetherPortalDisabled", comments = "掉落物只能使用目标维度已有的下界传送门。")
    public static volatile boolean itemEntityCreateNetherPortalDisabled = false;

    @ConfigInfo(name = "renewableDragonHead", comments = "充能苦力怕爆炸实际杀死末影龙时，可使用一次头颅掉落额度。")
    public static volatile boolean renewableDragonHead = false;

    @ConfigInfo(name = "noteBlockChunkLoader", comments = "音符盒加载模式：false、note_block、bone_block、wither_skeleton_skull。")
    public static volatile String noteBlockChunkLoader = "false";

    @ConfigInfo(name = "pistonBlockChunkLoader", comments = "活塞成功伸出时加载头部所处区块：false、bone_block、bedrock、all。")
    public static volatile String pistonBlockChunkLoader = "false";

    @ConfigInfo(name = "bellBlockChunkLoader", comments = "成功敲响钟时临时加载周围区域。")
    public static volatile boolean bellBlockChunkLoader = false;

    @HotReloadUnsupported
    @ConfigInfo(name = "blockChunkLoaderTimeController", comments = "方块加载票的期限（1–300 刻）；修改后重启。")
    public static volatile int blockChunkLoaderTimeController = 300;

    public static int blockChunkLoaderTimeController() {
        return Math.clamp(blockChunkLoaderTimeController, 1, 300);
    }

    public static String noteBlockChunkLoader() {
        final String mode = noteBlockChunkLoader;
        if (mode == null) return "false";
        return switch (mode) {
            case "note_block", "bone_block", "wither_skeleton_skull" -> mode;
            default -> "false";
        };
    }

    public static String pistonBlockChunkLoader() {
        final String mode = pistonBlockChunkLoader;
        if (mode == null) return "false";
        return switch (mode) {
            case "bone_block", "bedrock", "all" -> mode;
            default -> "false";
        };
    }
}
