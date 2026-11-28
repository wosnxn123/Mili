package fun.bm.mili.carpet;

import ca.spottedleaf.moonrise.common.util.TickThread;
import ca.spottedleaf.moonrise.patches.chunk_system.scheduling.ChunkHolderManager;
import fun.bm.mili.carpet.config.modules.WorldR2Config;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.WeakHashMap;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ChunkLevel;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.TicketType;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.block.Blocks;
import org.leavesmc.leaves.protocol.CarpetServerProtocol.CarpetRule;

public final class WorldR2Rules {
    static final int MAX_BLOCK_LOADER_CENTERS = 64;
    // Level 30: 3x3 entity-ticking chunks. The normal outer loading ring is handled by Moonrise.
    private static final int BLOCK_LOADER_LEVEL = ChunkLevel.ENTITY_TICKING_LEVEL - 1;
    private static final WeakHashMap<ServerLevel, Set<ChunkPos>> BLOCK_LOADER_CENTERS = new WeakHashMap<>();

    private WorldR2Rules() {
    }

    public static void appendRules(final List<CarpetRule> rules) {
        rules.add(CarpetRule.of("carpet", "movableBlockEntities", WorldR2Config.movableBlockEntities));
        rules.add(CarpetRule.of("carpetamsaddition", "onlyPlayerCanCreateNetherPortal", WorldR2Config.onlyPlayerCanCreateNetherPortal));
        rules.add(CarpetRule.of("carpetamsaddition", "itemEntityCreateNetherPortalDisabled", WorldR2Config.itemEntityCreateNetherPortalDisabled));
        rules.add(CarpetRule.of("carpettisaddition", "renewableDragonHead", WorldR2Config.renewableDragonHead));
        rules.add(CarpetRule.of("carpetamsaddition", "noteBlockChunkLoader", WorldR2Config.noteBlockChunkLoader()));
        rules.add(CarpetRule.of("carpetamsaddition", "pistonBlockChunkLoader", WorldR2Config.pistonBlockChunkLoader()));
        rules.add(CarpetRule.of("carpetamsaddition", "bellBlockChunkLoader", WorldR2Config.bellBlockChunkLoader));
        rules.add(CarpetRule.of("carpetamsaddition", "blockChunkLoaderTimeController", WorldR2Config.blockChunkLoaderTimeController()));
    }

    public static boolean mayCreateNetherPortal(final Entity entity) {
        return mayCreateNetherPortal(entity instanceof Player, entity instanceof ItemEntity,
            WorldR2Config.onlyPlayerCanCreateNetherPortal, WorldR2Config.itemEntityCreateNetherPortalDisabled);
    }

    static boolean mayCreateNetherPortal(final boolean player, final boolean item, final boolean onlyPlayers, final boolean blockItems) {
        return (!onlyPlayers || player) && (!blockItems || !item);
    }

    public static void noteBlockPlayed(final ServerLevel level, final BlockPos pos) {
        final String mode = WorldR2Config.noteBlockChunkLoader();
        if (mode.equals("false") || !GameplayCompatRules.ownsLoaded(level, pos)) return;
        final var top = level.getBlockState(pos.above());
        if (mode.equals("note_block") || mode.equals("bone_block") && top.is(Blocks.BONE_BLOCK)
            || mode.equals("wither_skeleton_skull") && (top.is(Blocks.WITHER_SKELETON_SKULL) || top.is(Blocks.WITHER_SKELETON_WALL_SKULL))) {
            addBlockLoaderTicket(level, pos);
        }
    }

    public static void bellRang(final ServerLevel level, final BlockPos pos) {
        if (WorldR2Config.bellBlockChunkLoader) addBlockLoaderTicket(level, pos);
    }

    public static void pistonExtended(final ServerLevel level, final BlockPos pos, final BlockPos head) {
        final String mode = WorldR2Config.pistonBlockChunkLoader();
        if (mode.equals("false") || !GameplayCompatRules.ownsLoaded(level, pos)) return;
        if ((mode.equals("bone_block") || mode.equals("all")) && level.getBlockState(pos.above()).is(Blocks.BONE_BLOCK)
            || (mode.equals("bedrock") || mode.equals("all")) && level.getBlockState(pos.below()).is(Blocks.BEDROCK)) {
            addBlockLoaderTicket(level, head);
        }
    }

    private static void addBlockLoaderTicket(final ServerLevel level, final BlockPos pos) {
        if (!TickThread.isTickThreadFor(level, pos) || !GameplayCompatRules.ownsLoaded(level, pos)) return;
        final ChunkPos center = ChunkPos.containing(pos);
        final ChunkHolderManager manager = level.moonrise$getChunkTaskScheduler().chunkHolderManager;
        synchronized (BLOCK_LOADER_CENTERS) {
            final Set<ChunkPos> centers = BLOCK_LOADER_CENTERS.computeIfAbsent(level, ignored -> new HashSet<>());
            if (!centers.contains(center) && centers.size() >= MAX_BLOCK_LOADER_CENTERS) {
                // The holder manager owns actual ticket expiry. The bounded admission index only remembers their keys.
                centers.removeIf(chunk -> manager.getTicketsAt(chunk.x(), chunk.z()).stream()
                    .noneMatch(ticket -> ticket.getType() == TicketType.MILI_BLOCK_LOADER));
                if (centers.size() >= MAX_BLOCK_LOADER_CENTERS) return;
            }
            manager.addTicketAtLevel(TicketType.MILI_BLOCK_LOADER, center, BLOCK_LOADER_LEVEL, null);
            centers.add(center);
        }
    }
}
