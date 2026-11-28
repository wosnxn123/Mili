package org.leavesmc.leaves.protocol;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;
import org.leavesmc.leaves.protocol.core.LeavesCustomPayload;
import org.leavesmc.leaves.protocol.core.LeavesProtocol;
import org.leavesmc.leaves.protocol.core.ProtocolHandler;
import org.leavesmc.leaves.protocol.core.ProtocolUtils;

import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

@LeavesProtocol.Register(namespace = "carpet")
public class CarpetServerProtocol implements LeavesProtocol {
    public static final String PROTOCOL_ID = "carpet";
    public static final String VERSION = ProtocolUtils.buildProtocolVersion(PROTOCOL_ID);

    private static final String HI = "69";
    private static final String HELLO = "420";
    private static final java.util.Set<ServerPlayer> awaitingHello = java.util.Collections.synchronizedSet(
            java.util.Collections.newSetFromMap(new java.util.WeakHashMap<>()));
    private static final java.util.Set<ServerPlayer> subscribers = java.util.Collections.synchronizedSet(
            java.util.Collections.newSetFromMap(new java.util.WeakHashMap<>()));

    @ProtocolHandler.PlayerLeave
    public static void onPlayerLeave(ServerPlayer player) {
        awaitingHello.remove(player);
        subscribers.remove(player);
    }

    public static void refreshSubscribers() {
        List<ServerPlayer> online;
        synchronized (subscribers) { online = List.copyOf(subscribers); }
        for (ServerPlayer player : online) {
            player.getBukkitEntity().taskScheduler.schedule((ServerPlayer current) -> {
                if (current != player || !subscribers.contains(current) || current.hasDisconnected()) return;
                CompoundTag data = new CompoundTag();
                CarpetRules.write(data);
                ProtocolUtils.sendPayloadPacket(current, new CarpetPayload(data));
            }, null, 1L);
        }
    }

    @Contract("_ -> new")
    public static Identifier id(String path) {
        return Identifier.fromNamespaceAndPath(PROTOCOL_ID, path);
    }

    @ProtocolHandler.PlayerJoin
    public static void onPlayerJoin(ServerPlayer player) {
        CompoundTag data = new CompoundTag();
        data.putString(HI, VERSION);
        ProtocolUtils.sendPayloadPacket(player, new CarpetPayload(data));
        awaitingHello.add(player);
    }

    /** 只消费这个连接收到邀请后的规则显示应答。其他消息仍走 Paper 的插件通道。 */
    public static boolean handleIncomingHello(ServerPlayer player, Identifier channel, byte[] bytes) {
        if (!CarpetPayload.HELLO_ID.equals(channel) || !CarpetRules.hasRules()
                || !ca.spottedleaf.moonrise.common.util.TickThread.isTickThreadFor(player) || player.hasDisconnected()
                || bytes.length == 0 || bytes.length > 256) return false;
        if (!awaitingHello.contains(player) && !subscribers.contains(player)) return false;
        var input = new java.io.DataInputStream(new java.io.ByteArrayInputStream(bytes));
        try {
            if (!(net.minecraft.nbt.NbtIo.readAnyTag(input, new net.minecraft.nbt.NbtAccounter(2048, 2)) instanceof CompoundTag hello)
                    || input.available() != 0 || hello.size() != 1
                    || hello.getString(HELLO).filter(version -> !version.isBlank() && version.length() <= 96).isEmpty()) return false;
        } catch (java.io.IOException | RuntimeException malformed) {
            return false;
        }
        if (awaitingHello.remove(player) && subscribers.add(player)) {
            var snapshot = new CompoundTag();
            CarpetRules.write(snapshot);
            ProtocolUtils.sendPayloadPacket(player, new CarpetPayload(snapshot));
        }
        return true;
    }

    @Override
    public boolean isActive() {
        return CarpetRules.hasRules();
    }

    public static class CarpetRules {

        private static volatile Map<String, CarpetRule> rules = Map.of();

        public static void write(@NotNull CompoundTag tag) {
            CompoundTag rulesNbt = new CompoundTag();
            rules.values().forEach(rule -> rule.writeNBT(rulesNbt));

            tag.put("Rules", rulesNbt);
        }

        public static List<CarpetRule> snapshot() {
            return List.copyOf(rules.values());
        }

        public static void register(CarpetRule rule) {
            register(rule, null);
        }

        public static void register(CarpetRule rule, String pluginName) {
            Map<String, CarpetRule> previous, next;
            synchronized (CarpetRules.class) {
                previous = rules;
                Map<String, CarpetRule> updated = new HashMap<>(previous);
                updated.put(rule.name(), rule);
                rules = next = Map.copyOf(updated);
            }
            fun.bm.mili.carpet.MiliCarpetApi.published(previous, next, pluginName);
        }

        public static void replace(Collection<CarpetRule> replacement) {
            Map<String, CarpetRule> updated = new HashMap<>();
            for (CarpetRule rule : replacement) updated.put(rule.name(), rule);
            Map<String, CarpetRule> next = Map.copyOf(updated), previous;
            synchronized (CarpetRules.class) {
                previous = rules;
                rules = next;
            }
            fun.bm.mili.carpet.MiliCarpetApi.published(previous, next, null);
        }

        public static boolean hasRules() {
            return !rules.isEmpty();
        }
    }

    public record CarpetRule(String identifier, String name, String value) {

        @NotNull
        @Contract("_, _, _ -> new")
        public static CarpetRule of(String identifier, String name, Enum<?> value) {
            return new CarpetRule(identifier, name, value.name().toLowerCase(Locale.ROOT));
        }

        @NotNull
        @Contract("_, _, _ -> new")
        public static CarpetRule of(String identifier, String name, boolean value) {
            return new CarpetRule(identifier, name, Boolean.toString(value));
        }

        @NotNull
        @Contract("_, _, _ -> new")
        public static CarpetRule of(String identifier, String name, int value) {
            return new CarpetRule(identifier, name, Integer.toString(value));
        }

        @NotNull
        @Contract("_, _, _ -> new")
        public static CarpetRule of(String identifier, String name, long value) {
            return new CarpetRule(identifier, name, Long.toString(value));
        }

        @NotNull
        @Contract("_, _, _ -> new")
        public static CarpetRule of(String identifier, String name, String value) {
            return new CarpetRule(identifier, name, value);
        }

        public void writeNBT(@NotNull CompoundTag rules) {
            CompoundTag rule = new CompoundTag();
            String key = name;

            while (rules.contains(key)) {
                key = key + "2";
            }

            rule.putString("Value", value);
            rule.putString("Manager", identifier);
            rule.putString("Rule", name);
            rules.put(key, rule);
        }
    }

    public record CarpetPayload(CompoundTag nbt) implements LeavesCustomPayload {
        @ID
        private static final Identifier HELLO_ID = CarpetServerProtocol.id("hello");

        @Codec
        private static final StreamCodec<FriendlyByteBuf, CarpetPayload> CODEC = StreamCodec.composite(
                ByteBufCodecs.COMPOUND_TAG, CarpetPayload::nbt, CarpetPayload::new
        );
    }
}
