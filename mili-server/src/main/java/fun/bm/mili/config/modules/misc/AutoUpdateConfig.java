package fun.bm.mili.config.modules.misc;

import fun.bm.mili.rust.TomlConfigData;
import me.earthme.luminol.config.IConfigModule;
import me.earthme.luminol.config.flags.ConfigClassInfo;
import me.earthme.luminol.config.flags.ConfigInfo;
import me.earthme.luminol.enums.EnumConfigCategory;
import me.earthme.luminol.utils.AutoUpdateHelper;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.Set;

@ConfigClassInfo(
        category = EnumConfigCategory.MISC,
        name = "auto_update",
        comments = "仅检查 wosnxn123/Mili 的 26.3 正式发布；验证后的启动 JAR 暂存于 auto_update/mili。"
)
public class AutoUpdateConfig implements IConfigModule {
    private static final AutoUpdateHelper UPDATER = new AutoUpdateHelper();
    private static final List<String> DEFAULT_CHECK_TIMES = List.of("06:00");

    @ConfigInfo(name = "enabled", comments = "显式允许定时检查并暂存更新。默认禁用，不继承 Luminol 设置。")
    public static boolean enabled = false;

    @ConfigInfo(name = "check_times", comments = "每天检查更新的本地时间列表（HH:mm）。")
    public static List<String> checkTimes = DEFAULT_CHECK_TIMES;

    @ConfigInfo(name = "apply_on_restart", comments = "显式允许兼容的 Riceear 启动器在重启时采用更新。不会覆盖外部或无法验证的 core.path。")
    public static boolean applyOnRestart = false;

    @ConfigInfo(name = "replace_launcher", comments = "显式允许备份并替换当前启动 JAR；须指定 target_jar_path、没有 core.path，且不能与 apply_on_restart 同时使用。")
    public static boolean replaceLauncher = false;

    @ConfigInfo(name = "target_jar_path", comments = "仅在 replace_launcher 启用时使用；必须是正在使用的完整 Riceear 启动 JAR 的路径。")
    public static String targetJarPath = "";

    @Override
    public void onLoaded(TomlConfigData configInstance, @Nullable Set<Exception> exceptions) {
        UPDATER.shutdown();
        if (enabled && (exceptions == null || exceptions.isEmpty())) {
            UPDATER.load(List.copyOf(checkTimes), applyOnRestart, replaceLauncher, targetJarPath);
        }
    }

    @Override
    public void onUnloaded(TomlConfigData configInstance) {
        UPDATER.shutdown();
        // A missing key on reload must use the defaults, not restore a previous opt-in.
        enabled = false;
        applyOnRestart = false;
        replaceLauncher = false;
        targetJarPath = "";
        checkTimes = DEFAULT_CHECK_TIMES;
    }
}
