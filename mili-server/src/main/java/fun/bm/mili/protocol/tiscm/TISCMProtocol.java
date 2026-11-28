package fun.bm.mili.protocol.tiscm;

import ca.spottedleaf.moonrise.common.util.TickThread;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufInputStream;
import io.netty.buffer.Unpooled;
import io.papermc.paper.ServerBuildInfo;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.protocol.common.ClientboundCustomPayloadPacket;
import net.minecraft.network.protocol.common.custom.DiscardedPayload;
import net.minecraft.resources.Identifier;
import net.minecraft.server.network.ServerGamePacketListenerImpl;
import org.jspecify.annotations.Nullable;

import java.io.IOException;

/** The read-only global MSPT subset of TISCM v1, independent of the Leaves dispatcher. */
public final class TISCMProtocol {
    public static final Identifier CHANNEL = Identifier.fromNamespaceAndPath("tiscm", "network/v1");
    private static final String HI = "hi";
    private static final String SUPPORTED_S2C = "supported_s2c_packets";
    private static final String SUPPORTED_C2S = "supported_c2s_packets";
    private static final String MSPT = "mspt_metrics_sample";
    private static final String PLATFORM_NAME = "platform_name";
    private static final String PLATFORM_VERSION = "platform_version";

    private static final int MAX_PAYLOAD_BYTES = 4096;
    private static final int MAX_CAPABILITIES = 32;
    private static final int MAX_CAPABILITY_BYTES = 96;
    private static final int MAX_PLATFORM_BYTES = 256;

    private static volatile Configuration configuration = new Configuration(false, false);

    private TISCMProtocol() {
    }

    /** Publish the two config flags together; every load/unload invalidates older handshakes and samples. */
    public static void configure(boolean networkEnabled, boolean metricsEnabled) {
        configuration = new Configuration(networkEnabled, networkEnabled && metricsEnabled);
    }

    public static boolean canReceive(int size) {
        return configuration.network && size > 0 && size <= MAX_PAYLOAD_BYTES;
    }

    /** Called by the existing Folia global tick hook. No player or connection access on this thread. */
    public static void broadcastMsptSample(long tickCounter, long nanosecond) {
        Configuration current = configuration;
        if (current.metrics) {
            current.latest = new Sample(nanosecond);
        }
    }

    private static final class Configuration {
        final boolean network;
        final boolean metrics;
        volatile @Nullable Sample latest;

        Configuration(boolean network, boolean metrics) {
            this.network = network;
            this.metrics = metrics;
        }
    }

    private static final class Sample {
        private final long nanos;
        private volatile @Nullable ClientboundCustomPayloadPacket encoded;

        Sample(long nanos) {
            this.nanos = nanos;
        }

        ClientboundCustomPayloadPacket packet() {
            ClientboundCustomPayloadPacket packet = this.encoded;
            if (packet == null) {
                CompoundTag data = new CompoundTag();
                data.putInt("version", 2);
                data.putLong("millisecond", this.nanos / 1_000_000L);
                data.putLong("nanosecond", this.nanos);
                data.putString("type", "tick_server_method");
                // A concurrent region may encode the same sample; both packets contain identical bytes.
                this.encoded = packet = payload(MSPT, data);
            }
            return packet;
        }
    }

    private enum Phase {
        NEW, AWAITING_SUPPORT, FINISHED, SUBSCRIBED
    }

    /** One instance per PLAY listener, read and mutated only on that player's region. */
    public static final class Session {
        private Phase phase = Phase.NEW;
        private @Nullable Configuration negotiated;
        private @Nullable Sample lastSample;
        private boolean closed;

        public void receive(ServerGamePacketListenerImpl connection, byte[] bytes) {
            Configuration current = configuration;
            if (this.closed || !current.network || bytes.length == 0 || bytes.length > MAX_PAYLOAD_BYTES
                    || !isCurrentGameConnection(connection)) {
                return;
            }
            if (this.negotiated != current) {
                this.reset();
            }
            if (this.phase != Phase.NEW && this.phase != Phase.AWAITING_SUPPORT) {
                return;
            }

            boolean greeting = this.phase == Phase.NEW;
            boolean subscribed;
            ByteBuf buf = Unpooled.wrappedBuffer(bytes);
            try {
                if (!readId(buf, greeting ? HI : SUPPORTED_S2C)) {
                    return;
                }
                ByteBufInputStream nbt = new ByteBufInputStream(buf);
                if (nbt.readUnsignedByte() != Tag.TAG_COMPOUND) {
                    return;
                }
                if (greeting) {
                    readGreeting(nbt);
                    subscribed = false;
                } else {
                    subscribed = readCapabilities(nbt);
                }
                if (nbt.available() != 0) {
                    return;
                }
            } catch (IOException | IndexOutOfBoundsException malformed) {
                return;
            } finally {
                buf.release();
            }

            if (current != configuration) {
                return;
            }
            if (greeting) {
                this.negotiated = current;
                this.phase = Phase.AWAITING_SUPPORT;
                sendGreeting(connection);
            } else {
                this.phase = subscribed && current.metrics ? Phase.SUBSCRIBED : Phase.FINISHED;
                this.lastSample = current.latest; // Do not send a sample taken before negotiation.
            }
        }

        public void sendLatestSample(ServerGamePacketListenerImpl connection) {
            if (this.negotiated == null || !isCurrentGameConnection(connection)) {
                return;
            }
            Configuration current = configuration;
            if (this.negotiated != current) {
                this.reset();
                return;
            }
            Sample latest = current.latest;
            if (this.phase == Phase.SUBSCRIBED && latest != null && latest != this.lastSample) {
                ClientboundCustomPayloadPacket packet = latest.packet();
                if (current == configuration) {
                    this.lastSample = latest;
                    connection.send(packet);
                }
            }
        }

        public void close() {
            this.closed = true;
            this.reset();
        }

        private void reset() {
            this.phase = Phase.NEW;
            this.negotiated = null;
            this.lastSample = null;
        }
    }

    private static boolean isCurrentGameConnection(ServerGamePacketListenerImpl connection) {
        var player = connection.player;
        return TickThread.isTickThreadFor(player) && player.connection == connection
                && !player.hasDisconnected() && connection.isAcceptingMessages();
    }

    private static boolean readId(ByteBuf buf, String expected) {
        // Both known IDs are ASCII and have a one-byte VarInt UTF-8 length in v1.
        if (buf.readUnsignedByte() != expected.length() || buf.readableBytes() <= expected.length()) {
            return false;
        }
        for (int i = 0; i < expected.length(); i++) {
            if (buf.readUnsignedByte() != expected.charAt(i)) {
                return false;
            }
        }
        return true;
    }

    // Parse only the small v1 grammar, not arbitrary NBT. The root accepts scalar strings for HI
    // or one flat list of strings for capabilities; nested tags, wrappers and duplicate keys are invalid.
    private static void readGreeting(ByteBufInputStream nbt) throws IOException {
        boolean name = false;
        boolean version = false;
        for (int type; (type = nbt.readUnsignedByte()) != Tag.TAG_END;) {
            if (type != Tag.TAG_STRING) {
                throw new IOException();
            }
            String field = readNbtUtf(nbt, 32);
            if (PLATFORM_NAME.equals(field) && !name) {
                name = true;
            } else if (PLATFORM_VERSION.equals(field) && !version) {
                version = true;
            } else {
                throw new IOException();
            }
            readNbtUtf(nbt, MAX_PLATFORM_BYTES); // Optional client metadata is neither retained nor logged.
        }
    }

    private static boolean readCapabilities(ByteBufInputStream nbt) throws IOException {
        if (nbt.readUnsignedByte() != Tag.TAG_LIST || !SUPPORTED_S2C.equals(readNbtUtf(nbt, 32))) {
            throw new IOException();
        }
        int type = nbt.readUnsignedByte();
        int count = nbt.readInt();
        if (count < 0 || count > MAX_CAPABILITIES || (type != Tag.TAG_STRING && (type != Tag.TAG_END || count != 0))) {
            throw new IOException();
        }
        boolean mspt = false;
        for (int i = 0; i < count; i++) {
            mspt |= MSPT.equals(readNbtUtf(nbt, MAX_CAPABILITY_BYTES));
        }
        if (nbt.readUnsignedByte() != Tag.TAG_END) {
            throw new IOException();
        }
        return mspt;
    }

    private static String readNbtUtf(ByteBufInputStream nbt, int maxBytes) throws IOException {
        nbt.mark(2);
        int bytes = nbt.readUnsignedShort();
        if (bytes > maxBytes || bytes > nbt.available()) {
            throw new IOException();
        }
        nbt.reset();
        return nbt.readUTF();
    }

    private static void sendGreeting(ServerGamePacketListenerImpl connection) {
        CompoundTag hello = new CompoundTag();
        hello.putString(PLATFORM_NAME, "mili");
        hello.putString(PLATFORM_VERSION, ServerBuildInfo.buildInfo().asString(ServerBuildInfo.StringRepresentation.VERSION_SIMPLE));
        connection.send(payload("hello", hello));

        CompoundTag supported = new CompoundTag();
        ListTag packets = new ListTag();
        packets.add(StringTag.valueOf(HI));
        packets.add(StringTag.valueOf(SUPPORTED_S2C));
        supported.put(SUPPORTED_C2S, packets);
        connection.send(payload(SUPPORTED_C2S, supported));
    }

    private static ClientboundCustomPayloadPacket payload(String id, CompoundTag data) {
        // Same body as the legacy STRING_UTF8 + COMPOUND_TAG codec, using vanilla's unknown-channel carrier.
        FriendlyByteBuf buf = new FriendlyByteBuf(Unpooled.buffer());
        try {
            ByteBufCodecs.STRING_UTF8.encode(buf, id);
            ByteBufCodecs.COMPOUND_TAG.encode(buf, data);
            byte[] bytes = new byte[buf.readableBytes()];
            buf.readBytes(bytes);
            return new ClientboundCustomPayloadPacket(new DiscardedPayload(CHANNEL, bytes));
        } finally {
            buf.release();
        }
    }
}
