/*
 * Based on Lophine's VanillaLikeExperienceConfig by Bacteriawa (GPL-3.0).
 * Source: LophineLabs/Lophine at 7a51f49a512f1b5465f15a905d3704754e4bed02.
 */
package fun.bm.mili.config.modules.fixes;

import me.earthme.luminol.config.IConfigModule;
import me.earthme.luminol.config.flags.ConfigClassInfo;
import me.earthme.luminol.config.flags.ConfigInfo;
import me.earthme.luminol.enums.EnumConfigCategory;

@ConfigClassInfo(category = EnumConfigCategory.FIXES, name = "vanilla-like-experience")
public final class VanillaLikeExperienceConfig implements IConfigModule {
    @ConfigInfo(name = "enabled", comments = "让实体挤压遵循游戏规则，取消 Spigot 的 TNT 刻数上限，恢复原版活塞缩回与怪物计数行为。活塞可破坏部分原本受 Paper 保护的方块。")
    public static volatile boolean enabled = false;

    @ConfigInfo(name = "persistent-ender-pearls", comments = "在玩家数据中保存和恢复现代末影珍珠；不覆盖 Paper 的 legacy-ender-pearl-behavior。请在启动前配置。")
    public static volatile boolean persistentEnderPearls = false;
}
