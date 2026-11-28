package fun.bm.mili.config.modules.experiment;

import me.earthme.luminol.config.IConfigModule;
import me.earthme.luminol.config.flags.ConfigClassInfo;
import me.earthme.luminol.config.flags.ConfigInfo;
import me.earthme.luminol.config.flags.TransformedConfig;
import me.earthme.luminol.enums.EnumConfigCategory;

@ConfigClassInfo(category = EnumConfigCategory.EXPERIMENT, name = "command")
public class CommandConfig implements IConfigModule {
    @ConfigInfo(name = "allow_non_ascii_unquoted_string", comments = "允许服务端命令的无引号参数使用非 ASCII 字母和数字；不改变空格、引号或命令权限。")
    public static volatile boolean allowNonAsciiUnquotedString = false;

    @ConfigInfo(name = "tick_command_enabled", comments =
            """
                    允许使用 tick 命令""")
    public static boolean tick = false;

    @ConfigInfo(name = "function_command_enabled", comments =
            """
                    允许使用 function 命令""")
    public static boolean function = false;

    @TransformedConfig(name = "enable-waypoint", directory = {"experiment", "waypoint bar"})
    @TransformedConfig(name = "enable-waypoint", directory = {"experiment", "waypoint_bar"})
    @ConfigInfo(name = "waypoint_command_enabled", comments =
            """
                    允许使用 waypoint 命令和定位栏""")
    public static boolean waypoint = false;

    @ConfigInfo(name = "scoreboard_command_enabled", comments =
            """
                    允许使用 scoreboard 命令""")
    public static boolean scoreboard = false;

    @ConfigInfo(name = "enabled", directory = {"save_all_command"}, comments =
            """
                    允许使用 save-all 命令""")
    public static boolean saveAll = false;

    @ConfigInfo(name = "log_all_process", directory = {"save_all_command"}, comments =
            """
                    将 save-all 命令的所有过程记录到控制台""")
    public static boolean logAllProcess = false;

    @ConfigInfo(name = "save_all_command_timeout", directory = {"save_all_command"}, comments = """
            区块保存超时报告前的最大秒数。""")
    public static long saveAllTimeout = 30;
}