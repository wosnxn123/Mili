package net.minecraft.world.level.block;

import java.util.Map;
import java.util.Objects;
import net.minecraft.SharedConstants;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Holder;
import net.minecraft.core.component.DataComponentMap;
import net.minecraft.core.component.DataComponents;
import net.minecraft.server.Bootstrap;
import net.minecraft.world.entity.EntityEquipment;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.entity.BarrelBlockEntity;
import net.minecraft.world.level.block.entity.HopperBlockEntity;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CarpetLargeBarrelTest {
    @BeforeAll
    static void bootstrap() {
        SharedConstants.tryDetectVersion();
        Bootstrap.bootStrap();
    }

    @Test
    void bothEntrancesUseTheSame54SlotsAndMenu() {
        var first = barrel(new BlockPos(3, 64, 5), Direction.WEST);
        var second = barrel(new BlockPos(4, 64, 5), Direction.EAST);
        var barrels = Map.of(first.getBlockPos(), first, second.getBlockPos(), second);
        var pair = Objects.requireNonNull(CarpetLargeBarrel.pair(first, 3, barrels::get));
        var reverse = Objects.requireNonNull(CarpetLargeBarrel.pair(second, 3, barrels::get));
        assertEquals(54, pair.getContainerSize());
        assertSame(pair.container1, reverse.container1);
        assertSame(pair.container2, reverse.container2);

        var menu = pair.createMenu(1, new Inventory(null, new EntityEquipment()));
        assertSame(MenuType.GENERIC_9x6, menu.getType());
        assertEquals(90, menu.slots.size());
        pair.setItem(53, stack(Items.DIAMOND, 2));
        assertEquals(2, second.getItem(26).getCount());
        assertSame(second.getItem(26), reverse.getItem(53));
    }

    @Test
    void hopperAndComparatorUseTheContentsOfBothHalves() {
        var first = barrel(new BlockPos(6, 64, 5), Direction.DOWN);
        var second = barrel(new BlockPos(6, 65, 5), Direction.UP);
        var barrels = Map.of(first.getBlockPos(), first, second.getBlockPos(), second);
        var pair = Objects.requireNonNull(CarpetLargeBarrel.pair(first, 3, barrels::get));
        var reverse = Objects.requireNonNull(CarpetLargeBarrel.pair(second, 3, barrels::get));
        for (int i = 0; i < 27; i++) {
            first.setItem(i, stack(Items.COBBLESTONE, 64));
        }
        int signal = AbstractContainerMenu.getRedstoneSignalFromContainer(pair);
        assertTrue(signal > 0 && signal < 15);
        assertEquals(signal, AbstractContainerMenu.getRedstoneSignalFromContainer(reverse));
        assertTrue(HopperBlockEntity.addItem(null, reverse, stack(Items.DIAMOND, 1), Direction.DOWN).isEmpty());
        assertEquals(1, second.getItem(0).getCount());
        assertSame(Items.DIAMOND, pair.removeItem(27, 1).getItem());
        assertTrue(second.getItem(0).isEmpty());
        assertEquals(27, first.getContainerSize());
        assertEquals(27, second.getContainerSize());
    }

    @Test
    void invalidPairsFallBackWithoutReadingAcrossChunkBoundaries() {
        var first = barrel(new BlockPos(3, 64, 5), Direction.WEST);
        var second = barrel(new BlockPos(4, 64, 5), Direction.EAST);
        assertNull(CarpetLargeBarrel.pair(first, 6, p -> second));
        assertNull(CarpetLargeBarrel.pair(first, 3, p -> null));
        assertNull(CarpetLargeBarrel.pair(first, 3, p -> barrel(p, Direction.NORTH)));
        var expanded = new BarrelBlockEntity(second.getBlockPos(), second.getBlockState()) {
            @Override
            public int getContainerSize() {
                return 54;
            }
        };
        assertNull(CarpetLargeBarrel.pair(first, 3, p -> expanded));
        var edge = barrel(new BlockPos(15, 64, 5), Direction.WEST);
        boolean[] lookedUp = {false};
        assertNull(CarpetLargeBarrel.pair(edge, 3, p -> {
            lookedUp[0] = true;
            return barrel(p, Direction.EAST);
        }));
        assertFalse(lookedUp[0]);
    }

    private static ItemStack stack(final Item item, final int count) {
        // No world/datapack bootstrap: attach just the stack-size component these operations use.
        var components = DataComponentMap.builder().set(DataComponents.MAX_STACK_SIZE, 64).build();
        return new ItemStack(Holder.direct(item, components), count);
    }

    private static BarrelBlockEntity barrel(final BlockPos pos, final Direction facing) {
        return new BarrelBlockEntity(pos, Blocks.BARREL.defaultBlockState().setValue(BarrelBlock.FACING, facing));
    }
}
