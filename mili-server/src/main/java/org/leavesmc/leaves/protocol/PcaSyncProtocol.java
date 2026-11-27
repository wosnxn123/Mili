/*
 * PCA sync: Leaves (violetc), GPL-3.0; Lophine 7a51f49a, Minecraft 0110 (Bacteriawa).
 * Mili keeps its own player-data policy and applies Paper/Folia visibility and ownership checks.
 */
package org.leavesmc.leaves.protocol;

import ca.spottedleaf.moonrise.common.util.TickThread;
import fun.bm.mili.config.modules.function.protocol.PcaSyncProtocolConfig;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.resources.Identifier;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.CompoundContainer;
import net.minecraft.world.Container;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.level.block.ChestBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.properties.ChestType;
import org.bukkit.craftbukkit.inventory.CraftInventory;
import org.jetbrains.annotations.Contract;
import org.leavesmc.leaves.bot.ServerBot;
import org.leavesmc.leaves.protocol.core.LeavesCustomPayload;
import org.leavesmc.leaves.protocol.core.LeavesProtocol;
import org.leavesmc.leaves.protocol.core.ProtocolHandler;
import org.leavesmc.leaves.protocol.core.ProtocolUtils;
import org.leavesmc.leaves.util.TagUtil;

import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

@LeavesProtocol.Register(namespace = "pca")
public class PcaSyncProtocol implements LeavesProtocol {

    public static final String PROTOCOL_ID = "pca";
    private static final Identifier ENABLE_PCA_SYNC_PROTOCOL = id("enable_pca_sync_protocol");
    private static final Identifier DISABLE_PCA_SYNC_PROTOCOL = id("disable_pca_sync_protocol");

    private static final Object WATCH_LOCK = new Object();
    private static final Map<ServerPlayer, Watch> WATCHES = new IdentityHashMap<>();
    private static final Map<Object, Set<Watch>> BY_TARGET = new IdentityHashMap<>();

    // Player/entity IDs can be reused, so index by live session and target identity.
    private record Watch(ServerPlayer viewer, ServerLevel world, Object target, Object partner, AbstractContainerMenu menu) {
        boolean menuStillOpen() {
            return this.menu == null || this.viewer.containerMenu == this.menu;
        }
    }

    @Contract("_ -> new")
    public static Identifier id(String path) {
        return Identifier.fromNamespaceAndPath(PROTOCOL_ID, path);
    }

    @ProtocolHandler.PlayerJoin
    private static void onJoin(ServerPlayer player) {
        if (PcaSyncProtocolConfig.enable) {
            ProtocolUtils.sendEmptyPacket(player, ENABLE_PCA_SYNC_PROTOCOL);
        }
    }

    @ProtocolHandler.PlayerLeave
    public static void clearPlayerWatchData(ServerPlayer player) {
        synchronized (WATCH_LOCK) {
            remove(WATCHES.get(player));
        }
    }

    @ProtocolHandler.BytebufReceiver(key = "cancel_sync_block_entity")
    private static void cancelSyncBlockEntityHandler(ServerPlayer player, FriendlyByteBuf buf) {
        synchronized (WATCH_LOCK) {
            final Watch watch = WATCHES.get(player);
            if (watch != null && watch.target instanceof BlockEntity) {
                remove(watch);
            }
        }
    }

    @ProtocolHandler.BytebufReceiver(key = "cancel_sync_entity")
    private static void cancelSyncEntityHandler(ServerPlayer player, FriendlyByteBuf buf) {
        synchronized (WATCH_LOCK) {
            final Watch watch = WATCHES.get(player);
            if (watch != null && watch.target instanceof Entity) {
                remove(watch);
            }
        }
    }

    // The Paper payload handler has already dispatched to this player's packet processor.
    // Consume only the four existing PCA request IDs; all other channels keep Paper's path.
    public static boolean handleIncoming(ServerPlayer player, Identifier channel, byte[] data) {
        if (!PROTOCOL_ID.equals(channel.getNamespace()) || !canReceive(player)) {
            return false;
        }
        final int length = switch (channel.getPath()) {
            case "sync_block_entity" -> Long.BYTES;
            case "sync_entity" -> Integer.BYTES;
            case "cancel_sync_block_entity", "cancel_sync_entity" -> 0;
            default -> -1;
        };
        if (length < 0) {
            return false;
        }
        if (data.length != length) {
            return true;
        }
        final FriendlyByteBuf input = new FriendlyByteBuf(io.netty.buffer.Unpooled.wrappedBuffer(data));
        try {
            switch (channel.getPath()) {
                case "sync_block_entity" -> syncBlockEntityHandler(player, SyncBlockEntityPayload.CODEC.decode(input));
                case "sync_entity" -> syncEntityHandler(player, SyncEntityPayload.CODEC.decode(input));
                case "cancel_sync_block_entity" -> cancelSyncBlockEntityHandler(player, input);
                case "cancel_sync_entity" -> cancelSyncEntityHandler(player, input);
                default -> throw new AssertionError(channel);
            }
        } finally {
            input.release();
        }
        return true;
    }

    @ProtocolHandler.PayloadReceiver(payload = SyncBlockEntityPayload.class)
    private static void syncBlockEntityHandler(ServerPlayer player, SyncBlockEntityPayload payload) {
        if (canReceive(player)) {
            subscribeBlock(player, payload.pos);
        }
    }

    private static void subscribeBlock(ServerPlayer player, BlockPos pos) {
        clearPlayerWatchData(player);
        final ServerLevel world = player.level();
        if (!canReachBlock(player, world, pos)) {
            return;
        }
        final BlockEntity block = loadedBlockEntity(world, pos);
        if (!canReceiveBlock(player, world, block)) {
            return;
        }
        BlockEntity partner = null;
        final var state = block.getBlockState();
        if (state.getBlock() instanceof ChestBlock && state.getValue(ChestBlock.TYPE) != ChestType.SINGLE) {
            final BlockPos adjacent = pos.relative(ChestBlock.getConnectedDirection(state));
            if (canReachBlock(player, world, adjacent)) {
                final BlockEntity other = loadedBlockEntity(world, adjacent);
                if (canReceiveBlock(player, world, other)) {
                    partner = other;
                }
            }
        }
        final Watch watch = new Watch(player, world, block, partner, player.containerMenu);
        if (subscribe(watch)) {
            sendBlock(watch, block);
            if (partner != null) {
                sendBlock(watch, partner);
            }
        }
    }

    @ProtocolHandler.PayloadReceiver(payload = SyncEntityPayload.class)
    private static void syncEntityHandler(ServerPlayer player, SyncEntityPayload payload) {
        if (!canReceive(player)) {
            return;
        }
        clearPlayerWatchData(player);
        final ServerLevel world = player.level();
        final Entity entity = world.getEntity(payload.entityId);
        if (entity != null && canReceiveEntity(player, world, entity)) {
            final Watch watch = new Watch(player, world, entity, null, entity instanceof Container ? player.containerMenu : null);
            if (subscribe(watch)) {
                sendEntity(watch, entity);
            }
        }
    }

    private static boolean canReceive(ServerPlayer player) {
        return PcaSyncProtocolConfig.enable && TickThread.isTickThreadFor(player) && !player.hasDisconnected()
            && player.level().getServer().getPlayerList().getPlayer(player.getUUID()) == player;
    }

    private static boolean canReachBlock(ServerPlayer player, ServerLevel world, BlockPos pos) {
        return TickThread.isTickThreadFor(world, pos)
            && player.isWithinBlockInteractionRange(pos, 1.0)
            && world.getWorldBorder().isWithinBounds(pos)
            && world.moonrise$getPlayerChunkLoader().isChunkSent(player, pos.getX() >> 4, pos.getZ() >> 4);
    }

    private static BlockEntity loadedBlockEntity(ServerLevel world, BlockPos pos) {
        return world.lithium$getLoadedExistingBlockEntity(pos);
    }

    private static boolean canReceiveBlock(ServerPlayer player, ServerLevel world, BlockEntity block) {
        return block instanceof Container container && block.getLevel() == world && !block.isRemoved()
            && canReachBlock(player, world, block.getBlockPos()) && hasOpenInventory(player, container);
    }

    private static boolean hasOpenInventory(ServerPlayer player, Container target) {
        // The actual Paper menu (including both halves of a chest) is proof of access. Merely
        // asking for a nearby position or ID would bypass lock, protection and inventory-open events.
        if (player.containerMenu == player.inventoryMenu
            || !(player.containerMenu.getBukkitView().getTopInventory() instanceof CraftInventory inventory)) {
            return false;
        }
        final Container opened = inventory.getInventory();
        return opened == target || opened instanceof CompoundContainer pair && pair.contains(target);
    }

    private static boolean canReceiveEntity(ServerPlayer viewer, ServerLevel world, Entity entity) {
        if (!TickThread.isTickThreadFor(entity) || entity.level() != world || entity.isRemoved()) {
            return false;
        }
        if (entity != viewer && (!viewer.isWithinEntityInteractionRange(entity, 1.0)
            || !viewer.getBukkitEntity().canSee(entity.getBukkitEntity())
            || entity.generation || !entity.getBukkitEntity().isTrackedBy(viewer.getBukkitEntity()))) {
            return false;
        }
        if (entity instanceof Player) {
            final boolean isOp = world.getServer().getPlayerList().isOp(viewer.nameAndId());
            final var mode = PcaSyncProtocolConfig.syncPlayerEntity;
            return mode != null && switch (mode) {
                case NOBODY -> false;
                case BOT -> entity instanceof ServerBot;
                case OPS -> entity instanceof ServerBot || isOp;
                case OPS_AND_SELF -> entity instanceof ServerBot || isOp || entity == viewer;
                case EVERYONE -> true;
            };
        }
        // For non-players the protocol carries full serialized entity data, not a vanilla spawn packet.
        // Non-operators may inspect a container entity only through a menu they actually opened.
        return entity instanceof Container container ? hasOpenInventory(viewer, container)
            : viewer.permissions().hasPermission(net.minecraft.server.permissions.Permissions.COMMANDS_GAMEMASTER)
                && viewer.getBukkitEntity().hasPermission("minecraft.command.data");
    }

    private static boolean subscribe(Watch watch) {
        synchronized (WATCH_LOCK) {
            if (!PcaSyncProtocolConfig.enable) {
                return false;
            }
            remove(WATCHES.get(watch.viewer));
            WATCHES.put(watch.viewer, watch);
            addTarget(watch.target, watch);
            if (watch.partner != null) {
                addTarget(watch.partner, watch);
            }
            return true;
        }
    }

    private static void addTarget(Object target, Watch watch) {
        BY_TARGET.computeIfAbsent(target, key -> Collections.newSetFromMap(new IdentityHashMap<>())).add(watch);
    }

    // WATCH_LOCK is held only for index mutations and copies, never for world access or serialization.
    private static void remove(Watch watch) {
        if (watch == null || WATCHES.get(watch.viewer) != watch) {
            return;
        }
        WATCHES.remove(watch.viewer);
        removeTarget(watch.target, watch);
        if (watch.partner != null) {
            removeTarget(watch.partner, watch);
        }
    }

    private static void removeTarget(Object target, Watch watch) {
        final Set<Watch> watchers = BY_TARGET.get(target);
        if (watchers != null && watchers.remove(watch) && watchers.isEmpty()) {
            BY_TARGET.remove(target);
        }
    }

    private static List<Watch> watching(Object target) {
        synchronized (WATCH_LOCK) {
            final Set<Watch> watchers = BY_TARGET.get(target);
            return watchers == null ? List.of() : List.copyOf(watchers);
        }
    }

    private static boolean isCurrent(Watch watch) {
        synchronized (WATCH_LOCK) {
            return PcaSyncProtocolConfig.enable && WATCHES.get(watch.viewer) == watch;
        }
    }

    public static boolean syncEntityToClient(Entity entity) {
        if (!PcaSyncProtocolConfig.enable || !TickThread.isTickThreadFor(entity)
            || !(entity.level() instanceof ServerLevel world)) {
            return false;
        }
        boolean sent = false;
        for (final Watch watch : watching(entity)) {
            final ServerPlayer viewer = watch.viewer;
            if (!TickThread.isTickThreadFor(viewer)) {
                continue;
            }
            if (watch.world != world || !canReceive(viewer) || viewer.level() != world || !watch.menuStillOpen() || !canReceiveEntity(viewer, world, entity)) {
                synchronized (WATCH_LOCK) { remove(watch); }
            } else {
                sent |= sendEntity(watch, entity);
            }
        }
        return sent;
    }

    public static boolean syncBlockEntityToClient(BlockEntity block) {
        if (!PcaSyncProtocolConfig.enable || !(block.getLevel() instanceof ServerLevel world)
            || !TickThread.isTickThreadFor(world, block.getBlockPos())) {
            return false;
        }
        boolean sent = false;
        for (final Watch watch : watching(block)) {
            final ServerPlayer viewer = watch.viewer;
            if (!TickThread.isTickThreadFor(viewer)) {
                continue;
            }
            if (watch.world != world || !canReceive(viewer) || viewer.level() != world || !watch.menuStillOpen() || !canReceiveBlock(viewer, world, block)) {
                synchronized (WATCH_LOCK) { remove(watch); }
            } else {
                sent |= sendBlock(watch, block);
            }
        }
        return sent;
    }

    private static boolean sendEntity(Watch watch, Entity entity) {
        if (!isCurrent(watch)) {
            return false;
        }
        final CompoundTag tag = entity instanceof Container ? inventoryTag(TagUtil.saveEntity(entity)) : TagUtil.saveEntity(entity);
        // Passenger and root-vehicle snapshots can disclose entities the viewer cannot inspect.
        tag.remove("Passengers");
        tag.remove("RootVehicle");
        tag.remove("BukkitValues");
        if (!isCurrent(watch)) {
            return false;
        }
        ProtocolUtils.sendPayloadPacket(watch.viewer, new UpdateEntityPayload(watch.world.dimension().identifier(), entity.getId(), tag));
        return true;
    }

    private static boolean sendBlock(Watch watch, BlockEntity block) {
        if (!isCurrent(watch)) {
            return false;
        }
        final CompoundTag tag = inventoryTag(block.saveWithoutMetadata(watch.world.registryAccess()));
        if (!isCurrent(watch)) {
            return false;
        }
        ProtocolUtils.sendPayloadPacket(watch.viewer, new UpdateBlockEntityPayload(watch.world.dimension().identifier(), block.getBlockPos(), tag));
        return true;
    }

    private static CompoundTag inventoryTag(CompoundTag saved) {
        // Saved stacks can contain server-only data components filtered by Paper's normal codecs.
        // The inventory preview needs only the slot, item ID and count, not raw item components.
        final var items = new net.minecraft.nbt.ListTag();
        for (final var entry : saved.getListOrEmpty("Items")) {
            if (entry instanceof CompoundTag stack) {
                final CompoundTag visible = new CompoundTag();
                for (final String field : List.of("Slot", "id", "count")) {
                    final var value = stack.get(field);
                    if (value != null) {
                        visible.put(field, value);
                    }
                }
                items.add(visible);
            }
        }
        final CompoundTag tag = new CompoundTag();
        tag.put("Items", items);
        return tag;
    }

    public static void onConfigLoaded() {
        final boolean enable = PcaSyncProtocolConfig.enable;
        synchronized (WATCH_LOCK) {
            WATCHES.clear();
            BY_TARGET.clear();
        }
        final MinecraftServer server = MinecraftServer.getServer();
        if (server == null || server.getPlayerList() == null) {
            return;
        }
        for (ServerPlayer player : server.getPlayerList().getPlayers()) {
            player.getBukkitEntity().taskScheduler.schedule(current -> {
                if (current == player && !player.hasDisconnected() && PcaSyncProtocolConfig.enable == enable) {
                    ProtocolUtils.sendEmptyPacket(player, enable ? ENABLE_PCA_SYNC_PROTOCOL : DISABLE_PCA_SYNC_PROTOCOL);
                }
            }, null, 1L);
        }
    }

    @Override
    public boolean isActive() {
        return PcaSyncProtocolConfig.enable;
    }

    public record UpdateEntityPayload(Identifier dimension, int entityId, CompoundTag tag) implements LeavesCustomPayload {

        @ID
        public static final Identifier UPDATE_ENTITY = PcaSyncProtocol.id("update_entity");

        @Codec
        public static final StreamCodec<FriendlyByteBuf, UpdateEntityPayload> CODEC = StreamCodec.composite(
            Identifier.STREAM_CODEC,
            UpdateEntityPayload::dimension,
            ByteBufCodecs.INT,
            UpdateEntityPayload::entityId,
            ByteBufCodecs.COMPOUND_TAG,
            UpdateEntityPayload::tag,
            UpdateEntityPayload::new
        );
    }

    public record UpdateBlockEntityPayload(Identifier dimension, BlockPos blockPos, CompoundTag tag) implements LeavesCustomPayload {

        @ID
        private static final Identifier UPDATE_BLOCK_ENTITY = PcaSyncProtocol.id("update_block_entity");

        @Codec
        private static final StreamCodec<FriendlyByteBuf, UpdateBlockEntityPayload> CODEC = StreamCodec.composite(
            Identifier.STREAM_CODEC,
            UpdateBlockEntityPayload::dimension,
            BlockPos.STREAM_CODEC,
            UpdateBlockEntityPayload::blockPos,
            ByteBufCodecs.COMPOUND_TAG,
            UpdateBlockEntityPayload::tag,
            UpdateBlockEntityPayload::new
        );
    }

    public record SyncBlockEntityPayload(BlockPos pos) implements LeavesCustomPayload {
        @ID
        public static final Identifier SYNC_BLOCK_ENTITY = PcaSyncProtocol.id("sync_block_entity");

        @Codec
        public static final StreamCodec<FriendlyByteBuf, SyncBlockEntityPayload> CODEC = StreamCodec.composite(
            BlockPos.STREAM_CODEC, SyncBlockEntityPayload::pos, SyncBlockEntityPayload::new
        );
    }

    public record SyncEntityPayload(int entityId) implements LeavesCustomPayload {
        @ID
        public static final Identifier SYNC_ENTITY = PcaSyncProtocol.id("sync_entity");

        @Codec
        public static final StreamCodec<FriendlyByteBuf, SyncEntityPayload> CODEC = StreamCodec.composite(
            ByteBufCodecs.INT, SyncEntityPayload::entityId, SyncEntityPayload::new
        );
    }
}
