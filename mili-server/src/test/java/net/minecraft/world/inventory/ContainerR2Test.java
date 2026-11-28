package net.minecraft.world.inventory;

import fun.bm.mili.carpet.ContainerR2Rules;
import fun.bm.mili.carpet.config.modules.ContainerR2Config;
import fun.bm.mili.config.modules.function.ContainerExpansionConfig;
import java.util.ArrayList;
import java.util.Collections;
import java.util.OptionalInt;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;
import ca.spottedleaf.moonrise.common.util.TickThread;
import net.minecraft.SharedConstants;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Holder;
import net.minecraft.core.RegistryAccess;
import net.minecraft.core.component.DataComponentMap;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.server.Bootstrap;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.ProblemReporter;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.EntityEquipment;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.BundleContents;
import net.minecraft.world.item.component.ItemContainerContents;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.HopperBlockEntity;
import net.minecraft.world.level.block.entity.ShulkerBoxBlockEntity;
import net.minecraft.world.level.storage.TagValueInput;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.leavesmc.leaves.protocol.CarpetServerProtocol.CarpetRule;
import org.leavesmc.leaves.util.ItemOverstackUtils;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.when;

class ContainerR2Test {
    private static final ArrayList<Holder.Reference<Item>> FIXTURES = new ArrayList<>();
    private static RegistryAccess registries;

    @BeforeAll
    static void bootstrap() {
        SharedConstants.tryDetectVersion();
        Bootstrap.bootStrap();
        registries = RegistryAccess.fromRegistryOfRegistries(BuiltInRegistries.REGISTRY);
        fixture(Items.SHULKER_BOX, 1);
        fixture(Items.DYED_SHULKER_BOX.blue(), 1);
        fixture(Items.BUNDLE, 1);
        for (Item item : java.util.List.of(Items.COBBLESTONE, Items.DIAMOND, Items.STONE, Items.GOLD_INGOT, Items.APPLE, Items.CARROT)) fixture(item, 64);
    }

    private static void fixture(final Item item, final int max) {
        final var holder = item.builtInRegistryHolder();
        try {
            holder.components();
            return;
        } catch (NullPointerException noDatapack) {
            // The focused unit test does not load a server or a level's datapacks.
        }
        FIXTURES.add(holder);
        holder.bindComponents(DataComponentMap.builder().set(DataComponents.MAX_STACK_SIZE, max)
            .set(DataComponents.ITEM_NAME, Component.literal(holder.key().identifier().toString())).build());
    }

    @AfterAll
    static void clearFixtures() {
        FIXTURES.forEach(holder -> holder.bindComponents(null));
        FIXTURES.clear();
    }

    @AfterEach
    void reset() {
        ContainerR2Config.quickShulker = false;
        ContainerR2Config.largeShulkerBox = false;
        ContainerR2Config.shulkerBoxStackable = false;
        ContainerR2Config.largeBundle = "false";
        ContainerExpansionConfig.shulkerCount = 1;
        ContainerExpansionConfig.nbtShulkerStackable = false;
    }

    @Test
    void worldShulkerUsesVanillaScreenAndPersistsHopperAccessibleTail(@org.junit.jupiter.api.io.TempDir java.nio.file.Path dir) throws java.io.IOException {
        var vanilla = box();
        assertEquals(27, vanilla.getContainerSize());
        final var inventory = new Inventory(null, new EntityEquipment());
        assertSame(MenuType.SHULKER_BOX, new ShulkerBoxMenu(1, inventory, vanilla).getType());
        ContainerR2Config.largeShulkerBox = true;
        var expanded = box();
        for (int i = 0; i < 53; ++i) expanded.setItem(i, new ItemStack(Items.COBBLESTONE, 64));
        assertTrue(HopperBlockEntity.addItem(null, expanded, new ItemStack(Items.DIAMOND, 7), Direction.UP).isEmpty());
        assertEquals(53, expanded.getSlotsForFace(Direction.DOWN)[53]);
        assertTrue(expanded.canTakeItemThroughFace(53, expanded.getItem(53), Direction.DOWN));
        assertEquals(7, expanded.getItem(53).getCount());
        assertFalse(HopperBlockEntity.addItem(null, expanded, new ItemStack(Items.SHULKER_BOX), Direction.UP).isEmpty());
        final var menu = new ShulkerBoxMenu(1, inventory, expanded);
        assertSame(MenuType.GENERIC_9x6, menu.getType());
        assertEquals(90, menu.slots.size());

        final var file = dir.resolve("shulker.nbt");
        net.minecraft.nbt.NbtIo.writeCompressed(expanded.saveWithoutMetadata(registries), file);
        ContainerR2Config.largeShulkerBox = false;
        var loaded = box();
        loaded.loadWithComponents(TagValueInput.create(ProblemReporter.DISCARDING, registries,
            net.minecraft.nbt.NbtIo.readCompressed(file, net.minecraft.nbt.NbtAccounter.create(1024 * 1024))));
        assertEquals(54, loaded.getContainerSize());
        assertEquals(7, loaded.getItem(53).getCount());
        final var packed = new ItemStack(Items.SHULKER_BOX);
        packed.set(DataComponents.CONTAINER, loaded.collectComponents().get(DataComponents.CONTAINER));
        var placed = box();
        placed.applyComponentsFromItemStack(packed);
        assertEquals(54, placed.getContainerSize());
        assertSame(Items.DIAMOND, placed.getItem(53).getItem());
    }

    @Test
    void quickTransferConservesCountsAndDoesNotNestAnotherShulker() {
        var box = new ItemStack(Items.SHULKER_BOX);
        var items = new ArrayList<ItemStack>(Collections.nCopies(54, ItemStack.EMPTY));
        items.set(0, new ItemStack(Items.STONE, 63));
        items.set(53, new ItemStack(Items.GOLD_INGOT, 3));
        box.set(DataComponents.CONTAINER, ItemContainerContents.fromItems(items));
        var source = new ItemStack(Items.STONE, 64);
        assertEquals(64, PortableContainerMenu.transferInto(box, source));
        assertTrue(source.isEmpty());
        var result = box.get(DataComponents.CONTAINER).itemCopies().toList();
        assertEquals(64, result.get(0).getCount());
        assertEquals(63, result.get(1).getCount());
        assertEquals(3, result.get(53).getCount());
        assertEquals(54, PortableContainerMenu.shulkerSlots(box));
        assertEquals(0, PortableContainerMenu.transferInto(box, new ItemStack(Items.DYED_SHULKER_BOX.blue())));
        assertEquals(3, box.get(DataComponents.CONTAINER).itemCopies().skip(53).findFirst().orElseThrow().getCount());
    }

    @Test
    void heldOpenHonorsRejectedMenuAndInvalidatesOnSourceReplacement() {
        ContainerR2Config.quickShulker = true;
        final ServerPlayer owner = mock(ServerPlayer.class);
        final ServerPlayer other = mock(ServerPlayer.class);
        final Inventory inventory = new Inventory(owner, new EntityEquipment());
        final ItemStack source = new ItemStack(Items.SHULKER_BOX);
        source.set(DataComponents.CONTAINER, ItemContainerContents.fromItems(java.util.List.of(new ItemStack(Items.STONE, 3))));
        inventory.setItem(0, source);
        when(owner.getInventory()).thenReturn(inventory);
        when(owner.getItemInHand(InteractionHand.MAIN_HAND)).thenAnswer(invocation -> inventory.getItem(0));
        when(owner.isAlive()).thenReturn(true);
        final AtomicBoolean cancel = new AtomicBoolean(true);
        final AtomicReference<PortableContainerMenu> candidate = new AtomicReference<>();
        doAnswer(invocation -> {
            final MenuProvider provider = invocation.getArgument(0);
            final PortableContainerMenu menu = (PortableContainerMenu) provider.createMenu(1, inventory, owner);
            candidate.set(menu);
            assertEquals(3, menu.getSlot(0).getItem().getCount());
            menu.getSlot(0).set(new ItemStack(Items.GOLD_INGOT));
            assertSame(Items.STONE, menu.getSlot(0).getItem().getItem());
            if (cancel.get()) return OptionalInt.empty();
            owner.containerMenu = menu;
            return OptionalInt.of(1);
        }).when(owner).openMenu(any());

        try (var region = mockStatic(TickThread.class)) {
            region.when(() -> TickThread.isTickThreadFor(owner)).thenReturn(true);
            assertTrue(PortableContainerMenu.tryUse(owner, InteractionHand.MAIN_HAND, false));
            assertFalse(candidate.get().stillValid(owner));
            assertTrue(candidate.get().getSlot(0).getItem().isEmpty());
            assertEquals(3, source.get(DataComponents.CONTAINER).itemCopies().findFirst().orElseThrow().getCount());
            cancel.set(false);
            assertTrue(PortableContainerMenu.tryUse(owner, InteractionHand.MAIN_HAND, false));
            final var menu = candidate.get();
            assertTrue(menu.stillValid(owner));
            assertFalse(menu.stillValid(other));
            assertFalse(menu.allowInput(owner, 0, 0, ContainerInput.SWAP));
            menu.getSlot(0).set(new ItemStack(Items.GOLD_INGOT, 4));
            assertEquals(4, source.get(DataComponents.CONTAINER).itemCopies().findFirst().orElseThrow().getCount());
            when(owner.hasDisconnected()).thenReturn(true);
            assertFalse(menu.stillValid(owner));
            menu.getSlot(0).set(ItemStack.EMPTY);
            assertEquals(4, source.get(DataComponents.CONTAINER).itemCopies().findFirst().orElseThrow().getCount());
            when(owner.hasDisconnected()).thenReturn(false);
            inventory.setItem(0, source.copy());
            assertFalse(menu.stillValid(owner));
            menu.getSlot(0).set(ItemStack.EMPTY);
            assertEquals(4, inventory.getItem(0).get(DataComponents.CONTAINER).itemCopies().findFirst().orElseThrow().getCount());
            inventory.setItem(0, source);
            menu.removed(owner);
            assertFalse(menu.stillValid(owner));
            menu.getSlot(0).set(ItemStack.EMPTY);
            assertEquals(4, source.get(DataComponents.CONTAINER).itemCopies().findFirst().orElseThrow().getCount());
        }
    }

    @Test
    void directQuickClickCanInsertAndExtractButNotUseAnExternalSlot() {
        ContainerR2Config.quickShulker = true;
        final ServerPlayer owner = mock(ServerPlayer.class);
        final Inventory inventory = new Inventory(owner, new EntityEquipment());
        when(owner.getInventory()).thenReturn(inventory);
        when(owner.isAlive()).thenReturn(true);
        final var menu = new ShulkerBoxMenu(1, inventory, new SimpleContainer(27));
        owner.containerMenu = menu;
        final Slot owned = menu.slots.stream().filter(slot -> slot.container == inventory && slot.getContainerSlot() == 0).findFirst().orElseThrow();
        final ItemStack box = new ItemStack(Items.SHULKER_BOX);
        menu.setCarried(box);
        inventory.setItem(0, new ItemStack(Items.STONE, 8));
        try (var region = mockStatic(TickThread.class)) {
            region.when(() -> TickThread.isTickThreadFor(owner)).thenReturn(true);
            assertFalse(PortableContainerMenu.tryInventoryClick(menu, owner, menu.getSlot(0)));
            menu.clicked(owned.index, 1, ContainerInput.PICKUP, owner);
            assertTrue(inventory.getItem(0).isEmpty());
            assertEquals(8, box.get(DataComponents.CONTAINER).itemCopies().findFirst().orElseThrow().getCount());
            menu.clicked(owned.index, 1, ContainerInput.PICKUP, owner);
            assertEquals(8, inventory.getItem(0).getCount());
            assertTrue(box.get(DataComponents.CONTAINER).itemCopies().findFirst().isEmpty());
        }
    }

    @Test
    void bundleVisibleWriteKeepsLegacyAndHiddenContentsSeparate() {
        var bundle = new ItemStack(Items.BUNDLE);
        var vanilla = BundleContents.EMPTY.asMutable();
        assertEquals(5, vanilla.tryInsert(new ItemStack(Items.APPLE, 5)));
        BundleContents legacy = vanilla.toImmutable();
        bundle.set(DataComponents.BUNDLE_CONTENTS, legacy);
        var full = new ArrayList<ItemStack>(Collections.nCopies(54, ItemStack.EMPTY));
        full.set(53, new ItemStack(Items.DIAMOND, 7));
        bundle.set(DataComponents.CONTAINER, ItemContainerContents.fromItems(full));
        ContainerR2Config.largeBundle = "9x3";
        var visible = new ArrayList<ItemStack>(Collections.nCopies(ContainerR2Config.bundleSlots(), ItemStack.EMPTY));
        visible.set(26, new ItemStack(Items.CARROT, 2));
        bundle.set(DataComponents.CONTAINER, PortableContainerMenu.replaceVisible(bundle.get(DataComponents.CONTAINER), visible));
        var stored = bundle.get(DataComponents.CONTAINER).itemCopies().toList();
        assertEquals(2, stored.get(26).getCount());
        assertEquals(7, stored.get(53).getCount());
        assertEquals(5, bundle.get(DataComponents.BUNDLE_CONTENTS).itemCopies().findFirst().orElseThrow().getCount());
        assertEquals(0, BundleContents.EMPTY.asMutable().tryInsert(bundle));
        ContainerR2Config.largeBundle = " 9X6 ";
        assertEquals(54, ContainerR2Config.bundleSlots());
    }

    @Test
    void orgEmptyStackingWorksInInventoryAndEntityMergeAndInteroperatesWithLegacy() {
        final var empty = new ItemStack(Items.SHULKER_BOX, 12);
        final var filled = new ItemStack(Items.SHULKER_BOX);
        filled.set(DataComponents.CONTAINER, ItemContainerContents.fromItems(java.util.List.of(new ItemStack(Items.DIAMOND))));
        assertEquals(1, ItemOverstackUtils.getItemStackMaxCount(empty));
        ContainerR2Config.shulkerBoxStackable = true;
        assertEquals(64, ItemOverstackUtils.getItemStackMaxCount(empty));
        assertEquals(1, ItemOverstackUtils.getItemStackMaxCount(filled));
        assertFalse(ItemEntity.areMergable(empty, new ItemStack(Items.DYED_SHULKER_BOX.blue())));
        assertTrue(ItemEntity.areMergable(empty, new ItemStack(Items.SHULKER_BOX)));
        var inventory = new Inventory(null, new EntityEquipment());
        inventory.setItem(0, empty.copy());
        final var input = new ItemStack(Items.SHULKER_BOX, 5);
        assertTrue(inventory.add(input));
        assertTrue(input.isEmpty());
        assertEquals(17, inventory.getItem(0).getCount());
        ItemStack projected = ItemOverstackUtils.encodeMaxStackSize(empty);
        assertEquals(1, empty.getMaxStackSize());
        assertEquals(64, projected.getMaxStackSize());
        assertTrue(ItemStack.matches(empty, ItemOverstackUtils.decodeMaxStackSize(projected)));
        ContainerExpansionConfig.shulkerCount = 16;
        ContainerExpansionConfig.nbtShulkerStackable = true;
        assertEquals(16, ItemOverstackUtils.getItemStackMaxCount(filled));
        ContainerR2Config.shulkerBoxStackable = false;
        ContainerExpansionConfig.shulkerCount = 1;
        assertEquals(12, ItemOverstackUtils.getNetworkMaxCount(empty));
        assertFalse(ItemEntity.areMergable(empty, new ItemStack(Items.SHULKER_BOX)));
    }

    @Test
    void reportUsesPinnedIdentifiersAndNormalizesInvalidMode() {
        ContainerR2Config.largeBundle = "unsupported";
        final var rules = new ArrayList<CarpetRule>();
        ContainerR2Rules.appendRules(rules);
        assertEquals(java.util.List.of(
            CarpetRule.of("carpetorgaddition", "quickShulker", false),
            CarpetRule.of("carpetorgaddition", "shulkerBoxStackable", false),
            CarpetRule.of("carpetamsaddition", "largeShulkerBox", false),
            CarpetRule.of("carpetamsaddition", "largeBundle", "false")), rules);
    }

    private static ShulkerBoxBlockEntity box() {
        return new ShulkerBoxBlockEntity(BlockPos.ZERO, Blocks.SHULKER_BOX.defaultBlockState());
    }
}
