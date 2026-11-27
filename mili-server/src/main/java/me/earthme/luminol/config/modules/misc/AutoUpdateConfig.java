package me.earthme.luminol.config.modules.misc;

import me.earthme.luminol.config.IConfigModule;
import me.earthme.luminol.config.flags.ConfigClassInfo;
import me.earthme.luminol.config.flags.ConfigInfo;
import me.earthme.luminol.enums.EnumConfigCategory;

import java.util.List;

@ConfigClassInfo(
        category = EnumConfigCategory.MISC,
        name = "auto_update",
        comments = "Legacy settings only. Mili ignores all settings in this section; use Mili's misc.auto_update to opt in."
)
public class AutoUpdateConfig implements IConfigModule {
    @ConfigInfo(name = "enabled", comments = "Legacy setting; ignored by Mili.")
    public static boolean enabled = false;

    @ConfigInfo(name = "check_times", comments = "Legacy setting; ignored by Mili.")
    public static List<String> checkTimes = List.of("06:00");

    @ConfigInfo(name = "allow_prerelease", comments = "Legacy setting; Mili does not auto-update to prereleases.")
    public static boolean allowPrerelease = false;

    @ConfigInfo(name = "target_jar_path", comments = "Legacy setting; ignored by Mili.")
    public static String targetJarPath = "";
}
