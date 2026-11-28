package org.leavesmc.leaves.protocol;

import ca.spottedleaf.moonrise.common.util.TickThread;
import io.netty.buffer.Unpooled;
import net.minecraft.SharedConstants;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.Bootstrap;
import net.minecraft.server.level.ServerPlayer;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.leavesmc.leaves.protocol.core.ProtocolUtils;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class CarpetHelloTest {
    @BeforeAll static void bootstrap() {
        SharedConstants.tryDetectVersion();
        Bootstrap.bootStrap();
    }

    static byte[] response(String key) {
        var nbt = new CompoundTag();
        nbt.putString(key, "fixture");
        var buf = new FriendlyByteBuf(Unpooled.buffer());
        try {
            buf.writeNbt(nbt);
            var bytes = new byte[buf.readableBytes()];
            buf.readBytes(bytes);
            return bytes;
        } finally { buf.release(); }
    }

    @Test void nativeNbtHelloCanOnlyRequestOneRuleSnapshotForTheInvitedSession() {
        var owner = mock(ServerPlayer.class);
        var channel = CarpetServerProtocol.id("hello");
        var wire = response("420");
        try (var threads = mockStatic(TickThread.class);
             var router = mockStatic(ProtocolUtils.class);
             var rules = mockStatic(CarpetServerProtocol.CarpetRules.class)) {
            rules.when(CarpetServerProtocol.CarpetRules::hasRules).thenReturn(true);
            threads.when(() -> TickThread.isTickThreadFor(owner)).thenReturn(true);
            assertFalse(CarpetServerProtocol.handleIncomingHello(owner, channel, wire));
            CarpetServerProtocol.onPlayerJoin(owner);
            assertFalse(CarpetServerProtocol.handleIncomingHello(owner, CarpetServerProtocol.id("other"), wire));
            assertFalse(CarpetServerProtocol.handleIncomingHello(owner, channel, response("other")));
            threads.when(() -> TickThread.isTickThreadFor(owner)).thenReturn(false);
            assertFalse(CarpetServerProtocol.handleIncomingHello(owner, channel, wire));
            threads.when(() -> TickThread.isTickThreadFor(owner)).thenReturn(true);
            assertTrue(CarpetServerProtocol.handleIncomingHello(owner, channel, wire));
            assertTrue(CarpetServerProtocol.handleIncomingHello(owner, channel, wire));
            rules.verify(() -> CarpetServerProtocol.CarpetRules.write(any()), times(1));
            router.verify(() -> ProtocolUtils.sendPayloadPacket(eq(owner), any()), times(2));
            CarpetServerProtocol.onPlayerLeave(owner);
            assertFalse(CarpetServerProtocol.handleIncomingHello(owner, channel, wire));
            CarpetServerProtocol.onPlayerJoin(owner);
            CarpetServerProtocol.onPlayerLeave(owner);
            assertFalse(CarpetServerProtocol.handleIncomingHello(owner, channel, wire));
        } finally { CarpetServerProtocol.onPlayerLeave(owner); }
    }
}
