package fun.bm.mili.config.modules.function.protocol;

import me.earthme.luminol.config.IConfigModule;
import me.earthme.luminol.config.flags.ConfigClassInfo;
import me.earthme.luminol.config.flags.ConfigInfo;
import me.earthme.luminol.enums.EnumConfigCategory;

@ConfigClassInfo(category = EnumConfigCategory.FUNCTION, name = "pca", directory = {"protocol"})
public class PcaSyncProtocolConfig implements IConfigModule {
    @ConfigInfo(name = "pca-sync-protocol", comments = """
            启用 PCA 同步协议""")
    public static volatile boolean enable = false;

    @ConfigInfo(name = "pca-sync-player-entity", comments = """
            谁可以通过 PCA 同步玩家实体数据""")
    public static volatile PcaPlayerEntityType syncPlayerEntity = PcaPlayerEntityType.OPS;

    @Override
    public void onLoaded(fun.bm.mili.rust.TomlConfigData configInstance, @org.jetbrains.annotations.Nullable java.util.Set<Exception> exceptions) {
        org.leavesmc.leaves.protocol.PcaSyncProtocol.onConfigLoaded();
    }

    public enum PcaPlayerEntityType {
        NOBODY, BOT, OPS, OPS_AND_SELF, EVERYONE
    }
}
