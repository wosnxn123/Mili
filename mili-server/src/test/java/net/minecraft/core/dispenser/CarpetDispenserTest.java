package net.minecraft.core.dispenser;

import java.util.concurrent.atomic.AtomicInteger;
import net.minecraft.SharedConstants;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.component.DataComponentMap;
import net.minecraft.core.component.DataComponents;
import net.minecraft.server.Bootstrap;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.DispenserBlockEntity;
import net.minecraft.world.phys.Vec3;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CarpetDispenserTest {
    @BeforeAll
    static void bootstrap() {
        SharedConstants.tryDetectVersion();
        Bootstrap.bootStrap();
    }

    @Test
    void noCostPreservesSelectedSlotButUsesNativeRemainderInsertion() {
        var state = Blocks.DISPENSER.defaultBlockState();
        var inventory = new DispenserBlockEntity(BlockPos.ZERO, state);
        // The insertion path does not use a level while there is an available slot.
        var source = new BlockSource(null, BlockPos.ZERO, state, inventory);
        var behavior = new DefaultDispenseItemBehavior();
        var input = stack(Items.GLASS_BOTTLE, 1);
        inventory.setItem(0, input);

        CarpetDispenserRules.dispenseNoCost(source, (pointer, working) -> {
            assertSame(Items.GLASS_BOTTLE, working.getItem());
            return behavior.consumeWithRemainder(pointer, working, stack(Items.HONEY_BOTTLE, 1));
        }, input);
        assertSame(input, inventory.getItem(0));
        assertEquals(1, inventory.getItem(0).getCount());
        assertSame(Items.HONEY_BOTTLE, inventory.getItem(1).getItem());
        assertEquals(1, inventory.getItem(1).getCount());

        // With no active scope, vanilla replaces a one-count input with the remainder.
        var ordinaryInput = input.copy();
        assertSame(Items.HONEY_BOTTLE, behavior.consumeWithRemainder(source, ordinaryInput, stack(Items.HONEY_BOTTLE, 1)).getItem());
        assertTrue(ordinaryInput.isEmpty());
        assertEquals(1, inventory.getItem(1).getCount());
    }

    @Test
    void rejectsInvalidAndNonOwnedCloudFootprints() {
        var inspected = new AtomicInteger();
        assertFalse(CarpetDragonBreathDispenseBehavior.canSpawnAt(new Vec3(Double.NaN, 64, 8), pos -> {
            inspected.incrementAndGet();
            return true;
        }));
        assertEquals(0, inspected.get());
        assertTrue(CarpetDragonBreathDispenseBehavior.canSpawnAt(new Vec3(8.5, 64, 8.5), pos -> (pos.getX() >> 4) == 0));
        assertFalse(CarpetDragonBreathDispenseBehavior.canSpawnAt(new Vec3(15.5, 64, 8.5), pos -> (pos.getX() >> 4) == 0));
        assertFalse(CarpetDragonBreathDispenseBehavior.canSpawnAt(new Vec3(8.5, 64, 8.5), pos -> false));
    }

    @Test
    void consumesOnlyAfterAcceptedCloudAndUnchangedSelectedSlot() {
        var cloudInput = stack(Items.DRAGON_BREATH, 3);
        assertSame(cloudInput, CarpetDragonBreathDispenseBehavior.finishCloud(cloudInput, true, () -> false, () -> true));
        assertEquals(3, cloudInput.getCount());
        CarpetDragonBreathDispenseBehavior.finishCloud(cloudInput, true, () -> true, () -> false);
        assertEquals(3, cloudInput.getCount());
        CarpetDragonBreathDispenseBehavior.finishCloud(cloudInput, false, () -> true, () -> true);
        assertEquals(3, cloudInput.getCount());
        CarpetDragonBreathDispenseBehavior.finishCloud(cloudInput, true, () -> true, () -> true);
        assertEquals(2, cloudInput.getCount());
    }

    private static ItemStack stack(final Item item, final int count) {
        var components = DataComponentMap.builder().set(DataComponents.MAX_STACK_SIZE, 64).build();
        return new ItemStack(Holder.direct(item, components), count);
    }
}
