package fun.bm.mili.carpet;

import fun.bm.mili.api.carpet.CarpetApi.Rule;
import fun.bm.mili.api.carpet.CarpetRuleUpdate;
import fun.bm.mili.carpet.config.modules.GeneralCompatConfig;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.Locale;
import java.util.Set;
import java.util.function.BiPredicate;
import me.earthme.luminol.config.flags.ConfigInfo;
import me.earthme.luminol.config.flags.HotReloadUnsupported;

/** 只接收协议已经公布、原生配置直接消费的规则；不接受 TOML 路径或命令字符串。 */
final class CarpetRuleMutations {
    private CarpetRuleMutations() {}

    static CarpetRuleUpdate apply(Rule before, String requested, boolean persist, BiPredicate<String, Object> save) {
        String name = before.name();
        if (!name.matches("[A-Za-z][A-Za-z0-9]{0,79}") || requested.length() > 64) {
            throw new IllegalArgumentException("无效的规则名或值");
        }
        Field field;
        try {
            field = GeneralCompatConfig.class.getField(name);
        } catch (NoSuchFieldException e) {
            throw new IllegalArgumentException("该规则不由可写的 Carpet 配置直接管理：" + name);
        }
        ConfigInfo info = field.getAnnotation(ConfigInfo.class);
        boolean restart = field.isAnnotationPresent(HotReloadUnsupported.class);
        int modifiers = field.getModifiers();
        if (info == null || !info.name().equals(name) || info.directory().length != 0
            || !Modifier.isStatic(modifiers) || Modifier.isFinal(modifiers)
            || (!restart && !Modifier.isVolatile(modifiers))) {
            throw new IllegalArgumentException("规则不可由插件安全热更新：" + name);
        }
        Object parsed = parse(name, field.getType(), requested);
        if (restart && !persist) throw new IllegalArgumentException("仅重启生效的规则必须保存：" + name);
        try {
            Object live = field.get(null);
            // 保存失败时原生 setter 会回滚磁盘缓存；此时不允许先改运行值。
            if (persist && !save.test("carpet.general." + name, parsed)) {
                throw new IllegalStateException("无法保存此规则：" + name);
            }
            if (live.equals(parsed)) return new CarpetRuleUpdate(before, parsed.toString(), CarpetRuleUpdate.Status.UNCHANGED, persist);
            if (restart) return new CarpetRuleUpdate(before, parsed.toString(), CarpetRuleUpdate.Status.SAVED_FOR_RESTART, true);
            field.set(null, parsed);
            Rule current = new Rule(before.manager(), name, parsed.toString());
            return new CarpetRuleUpdate(current, current.value(), CarpetRuleUpdate.Status.UPDATED, persist);
        } catch (IllegalAccessException e) {
            throw new IllegalStateException("无法访问原生规则：" + name, e);
        }
    }

    private static Object parse(String name, Class<?> type, String value) {
        String v = value.trim().toLowerCase(Locale.ROOT);
        if (type == boolean.class && (v.equals("true") || v.equals("false"))) return Boolean.valueOf(v);
        if (type == int.class) {
            int max = switch (name) { case "snowMeltMinLightLevel" -> 16; case "flippinCactusSoundEffect" -> 5; default -> -1; };
            if (max >= 0) {
                int number = Integer.parseInt(v);
                if (number >= 0 && number <= max) return number;
            }
        }
        if (type == String.class && ((name.equals("thickFungusGrowth") && Set.of("false", "random", "all").contains(v))
            || (name.equals("reusableSmithingTemplate") && Set.of("false", "upgrade", "true").contains(v)))) return v;
        throw new IllegalArgumentException("不支持该规则值：" + name + "=" + value);
    }
}
