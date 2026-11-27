package fun.bm.mili.regression;

import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.tree.ArgumentCommandNode;
import com.mojang.brigadier.tree.CommandNode;
import com.mojang.brigadier.tree.LiteralCommandNode;
import com.mojang.brigadier.tree.RootCommandNode;
import io.netty.buffer.ByteBufUtil;
import io.netty.buffer.Unpooled;
import it.unimi.dsi.fastutil.objects.Object2IntMap;
import java.lang.reflect.Method;
import java.util.ArrayDeque;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.protocol.game.ClientboundCommandsPacket;
import net.minecraft.resources.Identifier;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

final class CommandTreeEnumerationTestSuite {
    private static final ClientboundCommandsPacket.NodeInspector<Object> LITERALS_ONLY = new ClientboundCommandsPacket.NodeInspector<>() {
        @Override
        public Identifier suggestionId(ArgumentCommandNode<Object, ?> node) {
            throw new AssertionError("Fixture contains only literal nodes and a root");
        }

        @Override
        public boolean isExecutable(CommandNode<Object> node) {
            return false;
        }

        @Override
        public boolean isRestricted(CommandNode<Object> node) {
            return false;
        }
    };

    @Test
    void numbersChildrenBeforeRedirectAndDoesNotRenumberSharedNodes() throws ReflectiveOperationException {
        Fixture fixture = sharedRedirectTree();
        var actual = enumerate(fixture.root());
        assertEquals(original(fixture.root()), actual);
        assertEquals(fixture.order().size(), actual.size());
        for (int id = 0; id < fixture.order().size(); id++) {
            var node = fixture.order().get(id);
            assertEquals(id, actual.getInt(node), node::getName);
        }
    }

    @Test
    void encodesTheSamePacketBytesAsTheUnpatchedCommandTree() {
        // Golden payload verified with the unpatched 0125 production packet and codec.
        String expected = "09"                                           // Entry count
            + "00020102"                                                 // Root: children 1, 2
            + "090203040505616c706861"                                   // alpha: children 3, 4; redirect 5
            + "010204060462657461"                                       // beta: children 4, 6
            + "01000b616c7068612d6368696c64"                             // alpha-child
            + "01010706736861726564"                                     // shared: child 7
            + "0101080f72656469726563742d746172676574"                   // redirect-target: child 8
            + "0900040a626574612d6368696c64"                             // beta-child: redirect 4
            + "01000c7368617265642d6368696c64"                           // shared-child
            + "01000e72656469726563742d6368696c64"                       // redirect-child
            + "00";                                                      // Root ID
        var packet = new ClientboundCommandsPacket(sharedRedirectTree().root(), LITERALS_ONLY);
        var buffer = new FriendlyByteBuf(Unpooled.buffer());
        try {
            ClientboundCommandsPacket.STREAM_CODEC.encode(buffer, packet);
            assertEquals(expected, ByteBufUtil.hexDump(buffer));
        } finally {
            buffer.release();
        }
    }

    private static Fixture sharedRedirectTree() {
        RootCommandNode<Object> root = new RootCommandNode<>();
        LiteralCommandNode<Object> redirectTarget = literal("redirect-target");
        LiteralCommandNode<Object> redirectChild = literal("redirect-child");
        redirectTarget.addChild(redirectChild);
        LiteralCommandNode<Object> alpha = LiteralArgumentBuilder.<Object>literal("alpha").redirect(redirectTarget).build();
        LiteralCommandNode<Object> beta = literal("beta");
        LiteralCommandNode<Object> alphaChild = literal("alpha-child");
        LiteralCommandNode<Object> shared = literal("shared");
        LiteralCommandNode<Object> betaChild = LiteralArgumentBuilder.<Object>literal("beta-child").redirect(shared).build();
        LiteralCommandNode<Object> sharedChild = literal("shared-child");
        shared.addChild(sharedChild);
        alpha.addChild(alphaChild);
        alpha.addChild(shared);
        beta.addChild(shared);
        beta.addChild(betaChild);
        root.addChild(alpha);
        root.addChild(beta);

        return new Fixture(root, List.of(root, alpha, beta, alphaChild, shared, redirectTarget, betaChild, sharedChild, redirectChild));
    }

    private record Fixture(RootCommandNode<Object> root, List<CommandNode<Object>> order) {
    }

    @Test
    void emptyTreeContainsOnlyRootAndRetainsTheDefaultReturnValue() throws ReflectiveOperationException {
        RootCommandNode<Object> root = new RootCommandNode<>();
        var actual = enumerate(root);
        assertEquals(original(root), actual);
        assertEquals(1, actual.size());
        assertEquals(0, actual.getInt(root));
        var absent = literal("absent");
        assertFalse(actual.containsKey(absent));
        assertEquals(0, actual.defaultReturnValue());
        assertEquals(0, actual.getInt(absent));
    }

    private static LiteralCommandNode<Object> literal(String name) {
        return LiteralArgumentBuilder.<Object>literal(name).build();
    }

    @SuppressWarnings("unchecked")
    private static Object2IntMap<CommandNode<Object>> enumerate(RootCommandNode<Object> root) throws ReflectiveOperationException {
        Method method = ClientboundCommandsPacket.class.getDeclaredMethod("enumerateNodes", RootCommandNode.class);
        method.setAccessible(true);
        return (Object2IntMap<CommandNode<Object>>) method.invoke(null, root);
    }

    private static Map<CommandNode<Object>, Integer> original(RootCommandNode<Object> root) {
        Map<CommandNode<Object>, Integer> ids = new LinkedHashMap<>();
        var queue = new ArrayDeque<CommandNode<Object>>();
        queue.add(root);
        CommandNode<Object> node;
        while ((node = queue.poll()) != null) {
            if (!ids.containsKey(node)) {
                ids.put(node, ids.size());
                queue.addAll(node.getChildren());
                if (node.getRedirect() != null) {
                    queue.add(node.getRedirect());
                }
            }
        }
        return ids;
    }
}
