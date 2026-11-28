package fun.bm.mili.carpet.config.modules;

import java.util.Locale;
import me.earthme.luminol.config.IConfigModule;
import me.earthme.luminol.config.flags.CommandSuggestions;
import me.earthme.luminol.config.flags.ConfigClassInfo;
import me.earthme.luminol.config.flags.ConfigInfo;
import me.earthme.luminol.enums.EnumConfigCategory;

@ConfigClassInfo(category = EnumConfigCategory.ROOT, name = "container-r2", directory = {"carpet"},
    comments = "Carpet 容器玩法；默认使用原版行为，不自动启用。")
public final class ContainerR2Config implements IConfigModule {
    @ConfigInfo(name = "quickShulker", comments = "空气中使用手上的单个潜影盒，或右击自己物品栏中的单个潜影盒；支持右键直接存取。")
    public static volatile boolean quickShulker = false;

    @ConfigInfo(name = "largeShulkerBox", comments = "启用 54 格潜影盒；已扩容的方块和有后排物品的盒子在关闭后仍保留物品。")
    public static volatile boolean largeShulkerBox = false;

    @ConfigInfo(name = "shulkerBoxStackable", comments = "同组件的空潜影盒可在玩家库存和掉落物中堆叠到 64；Mili 原有同组件非空盒规则独立。")
    public static volatile boolean shulkerBoxStackable = false;

    @CommandSuggestions(suggest = {"false", "9x3", "9x6"})
    @ConfigInfo(name = "largeBundle", comments = "手持收纳袋使用独立容器菜单：false、9x3、9x6。不迁移原版收纳袋物品。")
    public static volatile String largeBundle = "false";

    public static String largeBundleMode() {
        final String value = largeBundle;
        if (value == null) return "false";
        return switch (value.trim().toLowerCase(Locale.ROOT)) {
            case "9x3" -> "9x3";
            case "9x6" -> "9x6";
            default -> "false";
        };
    }

    public static int bundleSlots() {
        return switch (largeBundleMode()) {
            case "9x3" -> 27;
            case "9x6" -> 54;
            default -> 0;
        };
    }
}
