package fun.bm.mili.carpet.config.modules;

import java.util.List;
import me.earthme.luminol.config.IConfigModule;
import me.earthme.luminol.config.flags.ConfigClassInfo;
import me.earthme.luminol.config.flags.ConfigInfo;
import me.earthme.luminol.enums.EnumConfigCategory;

@ConfigClassInfo(category = EnumConfigCategory.ROOT, name = "api", directory = {"carpet"})
public final class PluginRuleAccessConfig implements IConfigModule {
    @ConfigInfo(name = "allowed_rule_writers", comments = "可通过 Carpet API 改规则的已安装插件名称，区分大小写，不支持通配符。默认没有插件可写。")
    public static volatile List<String> allowedRuleWriters = List.of();
}
