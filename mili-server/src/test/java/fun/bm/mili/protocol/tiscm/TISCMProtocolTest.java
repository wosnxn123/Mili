package fun.bm.mili.protocol.tiscm;

import ca.spottedleaf.moonrise.common.util.TickThread;
import io.netty.buffer.Unpooled;
import io.papermc.paper.ServerBuildInfo;
import net.minecraft.SharedConstants;
import net.minecraft.core.RegistryAccess;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.protocol.PacketUtils;
import net.minecraft.network.protocol.common.ClientboundCustomPayloadPacket;
import net.minecraft.network.protocol.common.ServerboundCustomPayloadPacket;
import net.minecraft.network.protocol.common.custom.DiscardedPayload;
import net.minecraft.server.Bootstrap;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.network.ServerCommonPacketListenerImpl;
import net.minecraft.server.network.ServerGamePacketListenerImpl;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.parallel.ResourceLock;
import org.mockito.ArgumentCaptor;

import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ResourceLock("tiscm-configuration")
class TISCMProtocolTest {
    // Literal legacy envelope: UTF-8 ID prefixed by VarInt length, then unnamed empty compound.
    private static final byte[] HI = {2, 'h', 'i', 10, 0};
    private static final String SUPPORT = "supported_s2c_packets";
    private static final String SAMPLE = "mspt_metrics_sample";

    @BeforeAll
    static void bootstrap() {
        SharedConstants.tryDetectVersion();
        Bootstrap.bootStrap();
    }

    @AfterEach
    void disable() {
        TISCMProtocol.configure(false, false);
    }

    @Test
    void nativePlayRouteAndGameCodecDeliverOnlyPostNegotiationSamples() throws ReflectiveOperationException {
        var play = new Play();
        var server = mock(MinecraftServer.class);
        var processor = mock(net.minecraft.network.PacketProcessor.class);
        when(server.packetProcessor()).thenReturn(processor);
        setField(ServerCommonPacketListenerImpl.class, play.listener, "server", server);
        setField(ServerGamePacketListenerImpl.class, play.listener, "tiscmSession", play.session);
        doCallRealMethod().when(play.listener).handleCustomPayload(any());
        var greeting = incoming(HI);
        var support = incoming(capabilities("other", SAMPLE));
        var oversized = incoming(new byte[4097]);
        try (var threads = mockStatic(TickThread.class); var dispatch = mockStatic(PacketUtils.class)) {
            threads.when(() -> TickThread.isTickThreadFor(play.player)).thenReturn(true);
            greeting.handle(play.listener);
            dispatch.verifyNoInteractions(); // Off by default.
            play.assertSends(0);

            TISCMProtocol.configure(true, true);
            oversized.handle(play.listener);
            dispatch.verifyNoInteractions();
            support.handle(play.listener); // Not a handshake yet.
            TISCMProtocol.broadcastMsptSample(1L, 10_100_000L);
            play.poll();
            play.assertSends(0);

            greeting.handle(play.listener);
            greeting.handle(play.listener); // HI is answered once.
            dispatch.verify(() -> PacketUtils.ensureRunningOnSameThread(greeting, play.listener, processor), times(2));
            play.poll();
            play.assertSends(2);

            TISCMProtocol.broadcastMsptSample(2L, 20_200_000L);
            support.handle(play.listener);
            play.poll(); // Do not leak the sample preceding the completed handshake.
            play.assertSends(2);

            TISCMProtocol.broadcastMsptSample(3L, 30_300_000L);
            TISCMProtocol.broadcastMsptSample(4L, 42_987_654L);
            support.handle(play.listener); // Duplicate must not reset the watermark.
            play.poll();
            play.poll();
            var packets = play.assertSends(3);

            CompoundTag hello = new CompoundTag();
            hello.putString("platform_name", "mili");
            hello.putString("platform_version", ServerBuildInfo.buildInfo().asString(ServerBuildInfo.StringRepresentation.VERSION_SIMPLE));
            assertWire(packets.get(0), "hello", hello);
            assertWire(packets.get(1), "supported_c2s_packets", strings("supported_c2s_packets", "hi", SUPPORT));
            CompoundTag sample = new CompoundTag();
            sample.putInt("version", 2);
            sample.putLong("millisecond", 42L);
            sample.putLong("nanosecond", 42_987_654L);
            sample.putString("type", "tick_server_method");
            assertWire(packets.get(2), SAMPLE, sample);
        }
    }

    @Test
    void malformedUnknownAndOutOfOrderInputsDoNotAdvanceNegotiation() {
        TISCMProtocol.configure(true, true);
        var play = new Play();
        try (var threads = mockStatic(TickThread.class)) {
            threads.when(() -> TickThread.isTickThreadFor(play.player)).thenReturn(true);
            CompoundTag nested = new CompoundTag();
            nested.put("platform_name", new CompoundTag());
            CompoundTag longPlatform = new CompoundTag();
            longPlatform.putString("platform_name", "x".repeat(257));
            for (byte[] bad : List.of(new byte[0], new byte[4097], new byte[]{(byte) 0x82, 0, 'h', 'i', 10, 0},
                    legacyBody("HI", new CompoundTag()), legacyBody("speed_test", new CompoundTag()),
                    Arrays.copyOf(HI, HI.length - 1), Arrays.copyOf(HI, HI.length + 1),
                    legacyBody("hi", nested), legacyBody("hi", longPlatform), capabilities(SAMPLE))) {
                play.receive(bad);
            }
            play.assertSends(0);

            CompoundTag metadata = new CompoundTag();
            metadata.putString("platform_name", "TIS\0客户端");
            metadata.putString("platform_version", "fixture");
            play.receive(legacyBody("hi", metadata));
            play.assertSends(2);

            String[] tooMany = new String[33];
            Arrays.fill(tooMany, SAMPLE);
            CompoundTag wrongType = new CompoundTag();
            wrongType.putString(SUPPORT, SAMPLE);
            CompoundTag extra = strings(SUPPORT, SAMPLE);
            extra.putString("extra", "ignored by the old parser, disallowed here");
            byte[] capability = capabilities(SAMPLE);
            byte[] negativeCount = capability.clone();
            Arrays.fill(negativeCount, negativeCount.length - SAMPLE.length() - 7,
                    negativeCount.length - SAMPLE.length() - 3, (byte) -1);
            for (byte[] bad : List.of(legacyBody(SUPPORT, wrongType), legacyBody(SUPPORT, extra),
                    capabilities(tooMany), capabilities(SAMPLE, "x".repeat(97)), negativeCount,
                    Arrays.copyOf(capability, capability.length - 1), Arrays.copyOf(capability, capability.length + 1))) {
                play.receive(bad);
                TISCMProtocol.broadcastMsptSample(1L, 23_456_789L);
                play.poll();
                play.assertSends(2);
            }
            play.receive(capability);
            TISCMProtocol.broadcastMsptSample(2L, 30_765_432L);
            play.poll();
            play.assertSends(3);
        }
    }

    @Test
    void onlyExplicitCapabilityAndBothConfigFlagsCanSubscribe() {
        var play = new Play();
        try (var threads = mockStatic(TickThread.class)) {
            threads.when(() -> TickThread.isTickThreadFor(play.player)).thenReturn(true);
            TISCMProtocol.configure(false, true);
            play.receive(HI);
            play.assertSends(0);

            TISCMProtocol.configure(true, false);
            play.receive(HI);
            play.receive(capabilities(SAMPLE));
            TISCMProtocol.broadcastMsptSample(1L, 10L);
            play.poll();
            play.assertSends(2);

            TISCMProtocol.configure(true, true);
            play.receive(HI);
            play.receive(capabilities("speed_test"));
            play.receive(capabilities(SAMPLE)); // Valid first advertisement was final.
            TISCMProtocol.broadcastMsptSample(2L, 20L);
            play.poll();
            play.assertSends(4);

            TISCMProtocol.configure(true, true);
            play.receive(HI);
            play.receive(capabilities());
            play.receive(capabilities(SAMPLE));
            TISCMProtocol.broadcastMsptSample(3L, 30L);
            play.poll();
            play.assertSends(6);
        }
    }

    @Test
    void wrongOwnerRetiredConnectionAndDisableDoNotSend() {
        TISCMProtocol.configure(true, true);
        var play = new Play();
        try (var threads = mockStatic(TickThread.class)) {
            play.receive(HI);
            play.assertSends(0);
            threads.when(() -> TickThread.isTickThreadFor(play.player)).thenReturn(true);
            play.player.connection = null;
            play.receive(HI);
            play.assertSends(0);
            play.player.connection = play.listener;
            when(play.listener.isAcceptingMessages()).thenReturn(false);
            play.receive(HI);
            play.assertSends(0);
            when(play.listener.isAcceptingMessages()).thenReturn(true);
            play.receive(HI);
            play.receive(capabilities(SAMPLE));
            TISCMProtocol.broadcastMsptSample(1L, 12_345_678L);

            threads.when(() -> TickThread.isTickThreadFor(play.player)).thenReturn(false);
            play.poll();
            play.assertSends(2);
            threads.when(() -> TickThread.isTickThreadFor(play.player)).thenReturn(true);
            when(play.player.hasDisconnected()).thenReturn(true);
            play.poll();
            play.assertSends(2);
            when(play.player.hasDisconnected()).thenReturn(false);

            TISCMProtocol.configure(false, false);
            play.poll();
            TISCMProtocol.configure(true, true);
            play.receive(capabilities(SAMPLE));
            TISCMProtocol.broadcastMsptSample(2L, 21_000_000L);
            play.poll();
            play.assertSends(2);
            play.receive(HI);
            play.receive(capabilities(SAMPLE));
            play.session.close();
            play.receive(HI);
            play.receive(capabilities(SAMPLE));
            TISCMProtocol.broadcastMsptSample(3L, 31_000_000L);
            play.poll();
            play.assertSends(4);
        }
    }

    private static final class Play {
        final ServerPlayer player = mock(ServerPlayer.class);
        final ServerGamePacketListenerImpl listener = mock(ServerGamePacketListenerImpl.class);
        final TISCMProtocol.Session session = new TISCMProtocol.Session();

        Play() {
            this.listener.player = this.player;
            this.player.connection = this.listener;
            when(this.listener.isAcceptingMessages()).thenReturn(true);
        }

        void receive(byte[] body) {
            this.session.receive(this.listener, body);
        }

        void poll() {
            this.session.sendLatestSample(this.listener);
        }

        List<ClientboundCustomPayloadPacket> assertSends(int count) {
            var packets = ArgumentCaptor.forClass(ClientboundCustomPayloadPacket.class);
            verify(this.listener, times(count)).send(packets.capture());
            return packets.getAllValues();
        }
    }

    private static void setField(Class<?> owner, Object target, String name, Object value) throws ReflectiveOperationException {
        var field = owner.getDeclaredField(name);
        field.setAccessible(true);
        field.set(target, value);
    }

    private static byte[] capabilities(String... ids) {
        return legacyBody(SUPPORT, strings(SUPPORT, ids));
    }

    private static CompoundTag strings(String key, String... values) {
        var data = new CompoundTag();
        var list = new ListTag();
        for (String value : values) {
            list.add(StringTag.valueOf(value));
        }
        data.put(key, list);
        return data;
    }

    private static byte[] legacyBody(String id, CompoundTag data) {
        var buf = new FriendlyByteBuf(Unpooled.buffer());
        try {
            ByteBufCodecs.STRING_UTF8.encode(buf, id);
            ByteBufCodecs.COMPOUND_TAG.encode(buf, data);
            byte[] bytes = new byte[buf.readableBytes()];
            buf.readBytes(bytes);
            return bytes;
        } finally {
            buf.release();
        }
    }

    private static ServerboundCustomPayloadPacket incoming(byte[] body) {
        var buf = new FriendlyByteBuf(Unpooled.buffer());
        try {
            buf.writeUtf("tiscm:network/v1");
            buf.writeBytes(body);
            var packet = ServerboundCustomPayloadPacket.STREAM_CODEC.decode(buf);
            assertEquals(0, buf.readableBytes());
            return packet;
        } finally {
            buf.release();
        }
    }

    private static void assertWire(ClientboundCustomPayloadPacket packet, String id, CompoundTag data) {
        assertInstanceOf(DiscardedPayload.class, packet.payload());
        var buf = new RegistryFriendlyByteBuf(Unpooled.buffer(), RegistryAccess.EMPTY);
        try {
            ClientboundCustomPayloadPacket.GAMEPLAY_STREAM_CODEC.encode(buf, packet);
            assertEquals("tiscm:network/v1", buf.readUtf());
            assertEquals(id, ByteBufCodecs.STRING_UTF8.decode(buf));
            assertEquals(data, ByteBufCodecs.COMPOUND_TAG.decode(buf));
            assertEquals(0, buf.readableBytes());
        } finally {
            buf.release();
        }
    }
}
