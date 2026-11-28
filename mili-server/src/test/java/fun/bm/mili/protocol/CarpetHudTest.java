package fun.bm.mili.protocol;

import ca.spottedleaf.moonrise.common.util.TickThread;
import fun.bm.mili.carpet.config.modules.GameplayCompatConfig;
import fun.bm.mili.carpet.config.modules.GeneralCompatConfig;
import java.util.List;
import net.kyori.adventure.text.Component;
import net.minecraft.SharedConstants;
import net.minecraft.network.protocol.game.ClientboundTabListPacket;
import net.minecraft.server.Bootstrap;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.network.ServerGamePacketListenerImpl;
import org.bukkit.craftbukkit.entity.CraftPlayer;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class CarpetHudTest {
    @BeforeAll static void bootstrap() {
        SharedConstants.tryDetectVersion();
        Bootstrap.bootStrap();
    }

    @Test void xpFooterUsesOwnerPermissionAndRestoresCurrentPluginText() {
        var previous = GeneralCompatConfig.defaultLoggers;
        var enabled = GameplayCompatConfig.hopperXpCounters;
        var player = mock(ServerPlayer.class);
        var bukkit = mock(CraftPlayer.class);
        var connection = mock(ServerGamePacketListenerImpl.class);
        player.connection = connection;
        when(player.getBukkitEntity()).thenReturn(bukkit);
        when(player.level()).thenReturn(mock(ServerLevel.class));
        when(bukkit.playerListHeader()).thenReturn(Component.text("header"));
        when(bukkit.playerListFooter()).thenReturn(Component.text("footer"));
        try (var region = mockStatic(TickThread.class)) {
            GameplayCompatConfig.hopperXpCounters = true;
            GeneralCompatConfig.defaultLoggers = List.of("xcounter blue");
            CarpetLoggerProtocol.refreshConfiguredDefaults();
            assertFalse(CarpetLoggerProtocol.tickHud(null, player, false));
            region.when(() -> TickThread.isTickThreadFor(player)).thenReturn(true);
            assertFalse(CarpetLoggerProtocol.tickHud(null, player, false));
            verifyNoInteractions(connection);
            when(bukkit.hasPermission("mili.commands.counter")).thenReturn(true);
            assertTrue(CarpetLoggerProtocol.tickHud(null, player, false));
            var packet = ArgumentCaptor.forClass(ClientboundTabListPacket.class);
            verify(connection).send(packet.capture());
            assertEquals(Component.text("header"), io.papermc.paper.adventure.PaperAdventure.asAdventure(packet.getValue().header()));
            var parts = packet.getValue().footer().getSiblings();
            assertEquals(Component.text("footer"), io.papermc.paper.adventure.PaperAdventure.asAdventure(parts.getFirst()));
            assertEquals("\nblue: not started", parts.stream().skip(1).map(net.minecraft.network.chat.Component::getString).collect(java.util.stream.Collectors.joining()));
            clearInvocations(connection);
            GeneralCompatConfig.defaultLoggers = List.of();
            CarpetLoggerProtocol.refreshConfiguredDefaults();
            when(bukkit.playerListFooter()).thenReturn(Component.text("new footer"));
            assertFalse(CarpetLoggerProtocol.tickHud(null, player, true));
            assertFalse(CarpetLoggerProtocol.tickHud(null, player, false));
            verify(connection).send(packet.capture());
            assertEquals(1, packet.getValue().footer().getSiblings().size());
            assertEquals(Component.text("new footer"), io.papermc.paper.adventure.PaperAdventure.asAdventure(packet.getValue().footer().getSiblings().getFirst()));
        } finally {
            GeneralCompatConfig.defaultLoggers = previous;
            GameplayCompatConfig.hopperXpCounters = enabled;
            CarpetLoggerProtocol.refreshConfiguredDefaults();
        }
    }
}
