package fun.bm.mili.carpet;

import fun.bm.mili.carpet.config.modules.ContainerR2Config;
import java.util.List;
import org.leavesmc.leaves.protocol.CarpetServerProtocol.CarpetRule;

public final class ContainerR2Rules {
    private ContainerR2Rules() {}

    public static void appendRules(final List<CarpetRule> rules) {
        rules.add(CarpetRule.of("carpetorgaddition", "quickShulker", ContainerR2Config.quickShulker));
        rules.add(CarpetRule.of("carpetorgaddition", "shulkerBoxStackable", ContainerR2Config.shulkerBoxStackable));
        rules.add(CarpetRule.of("carpetamsaddition", "largeShulkerBox", ContainerR2Config.largeShulkerBox));
        rules.add(CarpetRule.of("carpetamsaddition", "largeBundle", ContainerR2Config.largeBundleMode()));
    }
}
