package net.minecraft.world.level.block.piston;

import fun.bm.mili.carpet.config.modules.WorldR2Config;
import net.minecraft.SharedConstants;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.RegistryAccess;
import net.minecraft.network.chat.Component;
import net.minecraft.server.Bootstrap;
import net.minecraft.util.ProblemReporter;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.ChestBlock;
import net.minecraft.world.level.block.entity.BarrelBlockEntity;
import net.minecraft.world.level.block.entity.ChestBlockEntity;
import net.minecraft.world.level.block.state.properties.ChestType;
import net.minecraft.world.level.storage.TagValueInput;
import net.minecraft.world.level.storage.TagValueOutput;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class MovablePistonCargoTest {
    private static final java.util.List<net.minecraft.core.Holder.Reference<net.minecraft.world.item.Item>> ITEM_FIXTURES = new java.util.ArrayList<>();
    private static RegistryAccess registries;

    @BeforeAll
    static void bootstrap() {
        SharedConstants.tryDetectVersion();
        Bootstrap.bootStrap();
        registries = RegistryAccess.fromRegistryOfRegistries(net.minecraft.core.registries.BuiltInRegistries.REGISTRY);
        // Item defaults normally come from the server's datapacks; do not start a server for these tests.
        for (var item : java.util.List.of(Items.BARREL, Items.DIAMOND, Items.GOLD_INGOT, Items.IRON_INGOT)) {
            var holder = item.builtInRegistryHolder();
            try {
                holder.components();
            } catch (NullPointerException unbound) {
                ITEM_FIXTURES.add(holder);
                holder.bindComponents(net.minecraft.core.component.DataComponentMap.builder()
                    .set(net.minecraft.core.component.DataComponents.MAX_STACK_SIZE, 64).build());
            }
        }
    }

    @org.junit.jupiter.api.AfterAll
    static void clearFixtures() {
        ITEM_FIXTURES.forEach(holder -> holder.bindComponents(null));
        ITEM_FIXTURES.clear();
    }

    @Test
    void carriedBarrelRestoresInventoryAndNameEvenIfTheRuleWasTurnedOff() {
        var state = Blocks.BARREL.defaultBlockState();
        var movingState = Blocks.MOVING_PISTON.defaultBlockState();
        var original = new BarrelBlockEntity(BlockPos.ZERO, state);
        var namedBarrel = new ItemStack(Items.BARREL);
        namedBarrel.set(net.minecraft.core.component.DataComponents.CUSTOM_NAME, Component.literal("In transit"));
        original.applyComponentsFromItemStack(namedBarrel);
        original.setItem(26, new ItemStack(Items.DIAMOND, 7));
        var carrier = new PistonMovingBlockEntity(BlockPos.ZERO, movingState, state, Direction.EAST, true, false);
        carrier.miliSetCargo(original, null);
        var encoded = TagValueOutput.createWithoutContext(ProblemReporter.DISCARDING);
        carrier.saveWithoutMetadata(encoded);
        var restored = new PistonMovingBlockEntity(BlockPos.ZERO, movingState);
        final boolean wasEnabled = WorldR2Config.movableBlockEntities;
        try {
            WorldR2Config.movableBlockEntities = false;
            restored.loadWithComponents(TagValueInput.create(ProblemReporter.DISCARDING, registries, encoded.buildResult()));
            var cargo = restored.miliTakeCargo();
            assertNotNull(cargo);
            assertTrue(cargo.getItem(26).is(Items.DIAMOND));
            assertEquals(7, cargo.getItem(26).getCount());
            assertEquals(Component.literal("In transit"), cargo.getCustomName());
            assertNull(restored.miliTakeCargo(), "no second consumer after transfer or destruction");
        } finally {
            WorldR2Config.movableBlockEntities = wasEnabled;
        }
    }

    @Test
    void pairedChestRecordsKeepSeparateInventoriesAndReciprocalDirections() {
        var left = Blocks.CHEST.defaultBlockState().setValue(ChestBlock.TYPE, ChestType.LEFT);
        var right = left.setValue(ChestBlock.TYPE, ChestType.RIGHT);
        var connected = ChestBlock.getConnectedDirection(left);
        assertTrue(MovablePistonCargo.matchingPair(BlockPos.ZERO, left, connected, right));
        assertFalse(MovablePistonCargo.matchingPair(BlockPos.ZERO, left, connected, left));
        assertFalse(MovablePistonCargo.matchingPair(BlockPos.ZERO, left, connected, right.setValue(ChestBlock.TYPE, ChestType.SINGLE)));
        var moving = Blocks.MOVING_PISTON.defaultBlockState();
        var leftCargo = new ChestBlockEntity(BlockPos.ZERO, left);
        var rightPos = BlockPos.ZERO.relative(connected);
        var rightCargo = new ChestBlockEntity(rightPos, right);
        leftCargo.setItem(0, new ItemStack(Items.GOLD_INGOT, 3));
        rightCargo.setItem(0, new ItemStack(Items.IRON_INGOT, 5));
        var leftCarrier = new PistonMovingBlockEntity(BlockPos.ZERO, moving, left, Direction.UP, true, false);
        var rightCarrier = new PistonMovingBlockEntity(rightPos, moving, right, Direction.UP, true, false);
        leftCarrier.miliSetCargo(leftCargo, connected);
        rightCarrier.miliSetCargo(rightCargo, connected.getOpposite());
        leftCarrier = roundTrip(leftCarrier);
        rightCarrier = roundTrip(rightCarrier);
        assertEquals(connected, leftCarrier.miliCargoPartner());
        assertEquals(connected.getOpposite(), rightCarrier.miliCargoPartner());
        assertEquals(3, leftCarrier.miliTakeCargo().getItem(0).getCount());
        assertEquals(5, rightCarrier.miliTakeCargo().getItem(0).getCount());
        assertFalse(MovablePistonCargo.supports(Blocks.COMMAND_BLOCK.defaultBlockState()));
    }

    private static PistonMovingBlockEntity roundTrip(final PistonMovingBlockEntity carrier) {
        var output = TagValueOutput.createWithoutContext(ProblemReporter.DISCARDING);
        carrier.saveWithoutMetadata(output);
        var restored = new PistonMovingBlockEntity(carrier.getBlockPos(), carrier.getBlockState());
        restored.loadWithComponents(TagValueInput.create(ProblemReporter.DISCARDING, registries, output.buildResult()));
        return restored;
    }
}
