package fun.bm.mili.carpet;

import ca.spottedleaf.moonrise.common.util.TickThread;
import fun.bm.mili.carpet.config.modules.EntityGameplayConfig;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.Container;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraft.world.entity.npc.villager.Villager;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.ContainerInput;
import net.minecraft.world.inventory.DispenserMenu;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;
import org.leavesmc.leaves.protocol.CarpetServerProtocol.CarpetRule;

public final class EntityGameplayRules {
    private EntityGameplayRules() {
    }

    public static boolean ownsLoaded(final ServerLevel level, final BlockPos pos) {
        return ownsLoaded(level, pos, 0);
    }

    public static boolean ownsLoaded(final ServerLevel level, final BlockPos pos, final int radius) {
        return TickThread.isTickThreadFor(level, pos, radius)
            && level.hasChunksAt(pos.getX() - radius, pos.getZ() - radius, pos.getX() + radius, pos.getZ() + radius);
    }

    public static int mergedExperienceValue(int first, int count, int second, int secondCount) {
        if (first <= 0 || second <= 0 || count <= 0 || secondCount <= 0) return -1;
        long total = (long) first * count + (long) second * secondCount;
        return total <= Short.MAX_VALUE ? (int) total : -1;
    }

    private static final Identifier GROUND_SPEED_ID = Identifier.fromNamespaceAndPath("mili", "carpet_ground_speed");

    static int groundSpeedLevel(final @Nullable String speed) {
        if (speed == null) return 0;
        return switch (speed) {
            case "Ⅰ" -> 1;
            case "Ⅱ" -> 2;
            case "Ⅲ" -> 3;
            case "Ⅳ" -> 4;
            case "Ⅴ" -> 5;
            default -> 0;
        };
    }

    private static final Identifier FLY_DRAG_ID = Identifier.fromNamespaceAndPath("mili", "carpet_creative_fly_drag");

    // Vanilla computes clamp(1 - (1 - 0.91F) * attribute, 0, 1). Scale the
    // existing attribute, not its base, so unrelated modifiers remain intact.
    static double flightDragMultiplier(double drag) {
        return drag == 0.09D ? 1.0D : (1.0F - (float) (1.0D - drag)) / (1.0F - 0.91F);
    }

    public static void updateMovementAttributes(final ServerPlayer player) {
        var drag = player.getAttribute(Attributes.AIR_DRAG_MODIFIER);
        if (drag != null) {
            double factor = player.isCreative() && player.getAbilities().flying && !player.onGround() && !player.isPassenger() && !player.isFallFlying()
                ? flightDragMultiplier(EntityGameplayConfig.creativeFlyDrag()) : 1.0D;
            applyFlightDrag(drag, factor);
        }
        updateGroundSpeed(player);
    }

    static void applyFlightDrag(net.minecraft.world.entity.ai.attributes.AttributeInstance drag, double factor) {
        var current = drag.getModifier(FLY_DRAG_ID);
        if (factor == 1.0D) {
            if (current != null) drag.removeModifier(FLY_DRAG_ID);
        } else if (current == null || current.amount() != factor - 1.0D) {
            drag.addOrUpdateTransientModifier(new AttributeModifier(FLY_DRAG_ID, factor - 1.0D,
                AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL));
        }
    }

    public static void updateGroundSpeed(final ServerPlayer player) {
        int bonus = groundSpeedLevel(EntityGameplayConfig.fasterMovement);
        final String scope = EntityGameplayConfig.fasterMovementController;
        var dimension = player.level().dimension();
        if (player.isSpectator() || !("all".equalsIgnoreCase(scope) || "overworld".equalsIgnoreCase(scope) && dimension == Level.OVERWORLD
            || "nether".equalsIgnoreCase(scope) && dimension == Level.NETHER || "end".equalsIgnoreCase(scope) && dimension == Level.END)) bonus = 0;
        var attribute = player.getAttribute(Attributes.MOVEMENT_SPEED);
        if (attribute == null) return;
        var current = attribute.getModifier(GROUND_SPEED_ID);
        if (bonus == 0) {
            if (current != null) attribute.removeModifier(GROUND_SPEED_ID);
            return;
        }
        double amount = bonus * 0.1;
        if (current == null || current.amount() != amount) {
            if (current != null) attribute.removeModifier(GROUND_SPEED_ID);
            attribute.addTransientModifier(new AttributeModifier(GROUND_SPEED_ID, amount, AttributeModifier.Operation.ADD_VALUE));
        }
    }

    public static Vec3 precisePlacement(final BlockPos target, final Vec3 click, final double y) {
        return new Vec3(Math.clamp(click.x, target.getX() + 0.001, target.getX() + 0.999), y,
            Math.clamp(click.z, target.getZ() + 0.001, target.getZ() + 0.999));
    }

    public static InteractionResult openVillagerInventory(final ServerPlayer player, final Villager villager) {
        if (!TickThread.isTickThreadFor(player) || !TickThread.isTickThreadFor(villager)) return InteractionResult.FAIL;
        return player.openMenu(new SimpleMenuProvider((id, inventory, ignored) -> new VillagerInventoryMenu(id, inventory, villager),
            villager.getName())).isPresent() ? InteractionResult.SUCCESS : InteractionResult.FAIL;
    }

    static void registerProtocolRules(final List<CarpetRule> rules) {
        rules.add(CarpetRule.of("carpet", "creativeFlyDrag", Double.toString(EntityGameplayConfig.creativeFlyDrag())));
        rules.add(CarpetRule.of("carpet", "creativeFlySpeed", Double.toString(EntityGameplayConfig.creativeFlySpeed())));
        rules.add(CarpetRule.of("carpet", "huskSpawningInTemples", EntityGameplayConfig.huskSpawningInTemples));
        rules.add(CarpetRule.of("carpet", "missingTools", EntityGameplayConfig.missingTools));
        rules.add(CarpetRule.of("carpet", "persistentParrots", EntityGameplayConfig.persistentParrots));
        rules.add(CarpetRule.of("carpet", "pickaxeMinedBedrock", EntityGameplayConfig.pickaxeMinedBedrock));
        rules.add(CarpetRule.of("carpet", "piglinsSpawningInBastions", EntityGameplayConfig.piglinsSpawningInBastions));
        rules.add(CarpetRule.of("carpet", "renewableSponges", EntityGameplayConfig.renewableSponges));
        rules.add(CarpetRule.of("carpet", "shulkerSpawningInEndCities", EntityGameplayConfig.shulkerSpawningInEndCities));
        rules.add(CarpetRule.of("carpetamsaddition", "breedableParrots", EntityGameplayConfig.breedableParrots));
        rules.add(CarpetRule.of("carpetamsaddition", "craftableCarvedPumpkin", EntityGameplayConfig.craftableCarvedPumpkin));
        rules.add(CarpetRule.of("carpetamsaddition", "easyRefreshTrades", EntityGameplayConfig.easyRefreshTrades));
        rules.add(CarpetRule.of("carpetamsaddition", "experimentalMinecartSpeed", EntityGameplayConfig.experimentalMinecartSpeed()));
        rules.add(CarpetRule.of("carpetamsaddition", "fakePeace", EntityGameplayConfig.fakePeace));
        rules.add(CarpetRule.of("carpetamsaddition", "fakePlayerPickUpController", EntityGameplayConfig.fakePlayerPickUpController));
        rules.add(CarpetRule.of("carpetamsaddition", "fasterMovement", EntityGameplayConfig.fasterMovement));
        rules.add(CarpetRule.of("carpetamsaddition", "fasterMovementController", EntityGameplayConfig.fasterMovementController));
        rules.add(CarpetRule.of("carpetamsaddition", "headHunter", EntityGameplayConfig.headHunter));
        rules.add(CarpetRule.of("carpetamsaddition", "infiniteTrades", EntityGameplayConfig.infiniteTrades));
        rules.add(CarpetRule.of("carpetamsaddition", "kirinArm", EntityGameplayConfig.kirinArm));
        rules.add(CarpetRule.of("carpetamsaddition", "quickVillagerLevelUp", EntityGameplayConfig.quickVillagerLevelUp));
        rules.add(CarpetRule.of("carpetamsaddition", "renewableNetheriteScrap", Double.toString(EntityGameplayConfig.renewableNetheriteScrap())));
        rules.add(CarpetRule.of("carpetamsaddition", "sharedVillagerDiscounts", EntityGameplayConfig.sharedVillagerDiscounts));
        rules.add(CarpetRule.of("carpetamsaddition", "superBow", EntityGameplayConfig.superBow));
        rules.add(CarpetRule.of("carpetamsaddition", "superLeash", EntityGameplayConfig.superLeash));
        rules.add(CarpetRule.of("carpetorgaddition", "beaconRangeExpand", EntityGameplayConfig.beaconRangeExpand()));
        rules.add(CarpetRule.of("carpetorgaddition", "beaconWorldHeight", EntityGameplayConfig.beaconWorldHeight));
        rules.add(CarpetRule.of("carpetorgaddition", "damageEnchantmentCompatible", EntityGameplayConfig.damageEnchantmentCompatible));
        rules.add(CarpetRule.of("carpetorgaddition", "experienceOrbMerge", EntityGameplayConfig.experienceOrbMerge));
        rules.add(CarpetRule.of("carpetorgaddition", "forceRestock", EntityGameplayConfig.forceRestock));
        rules.add(CarpetRule.of("carpetorgaddition", "knockbackStick", EntityGameplayConfig.knockbackStick));
        rules.add(CarpetRule.of("carpetorgaddition", "limitPhantomSpawn", EntityGameplayConfig.limitPhantomSpawn));
        rules.add(CarpetRule.of("carpetorgaddition", "openVillagerInventory", EntityGameplayConfig.openVillagerInventory));
        rules.add(CarpetRule.of("carpetorgaddition", "playerDropHead", EntityGameplayConfig.playerDropHead));
        rules.add(CarpetRule.of("carpetorgaddition", "protectionEnchantmentCompatible", EntityGameplayConfig.protectionEnchantmentCompatible));
        rules.add(CarpetRule.of("carpetorgaddition", "superChargedCreeper", EntityGameplayConfig.superChargedCreeper));
        rules.add(CarpetRule.of("carpetorgaddition", "villagerInfiniteTrade", EntityGameplayConfig.villagerInfiniteTrade));
        rules.add(CarpetRule.of("carpettisaddition", "entityPlacementIgnoreCollision", EntityGameplayConfig.entityPlacementIgnoreCollision));
        rules.add(CarpetRule.of("carpettisaddition", "minecartPlaceableOnGround", EntityGameplayConfig.minecartPlaceableOnGround));
        rules.add(CarpetRule.of("carpettisaddition", "overspawningReintroduced", EntityGameplayConfig.overspawningReintroduced));
        rules.add(CarpetRule.of("carpettisaddition", "preciseEntityPlacement", EntityGameplayConfig.preciseEntityPlacement));
        rules.add(CarpetRule.of("carpettisaddition", "renewableDragonEgg", EntityGameplayConfig.renewableDragonEgg));
        rules.add(CarpetRule.of("carpettisaddition", "renewableElytra", Double.toString(EntityGameplayConfig.renewableElytra())));
    }

    /** Native villager menu adapted from Lophine 7a51f49a OrgVillagerMenu (Carpet Org Addition; MIT). */
    private static final class VillagerInventoryMenu extends DispenserMenu {
        private final Villager villager;

        private VillagerInventoryMenu(int id, Inventory inventory, Villager villager) {
            super(id, inventory, new VillagerContainer(villager));
            this.villager = villager;
        }

        @Override
        protected void add3x3GridSlots(Container container, int left, int top) {
            for (int i = 0; i < 9; i++) {
                Slot slot = i == 8 ? new Slot(container, i, left + i % 3 * 18, top + i / 3 * 18) {
                    @Override
                    public boolean mayPlace(ItemStack stack) {
                        return false;
                    }

                    @Override
                    public boolean mayPickup(Player player) {
                        return false;
                    }

                    @Override
                    public boolean isActive() {
                        return false;
                    }
                } : new Slot(container, i, left + i % 3 * 18, top + i / 3 * 18);
                this.addSlot(slot);
            }
        }

        @Override
        public boolean stillValid(Player player) {
            return TickThread.isTickThreadFor(player) && TickThread.isTickThreadFor(this.villager)
                    && this.villager.isAlive() && !this.villager.isRemoved() && player.distanceTo(this.villager) < 8.0F;
        }

        @Override
        public void clicked(int index, int button, ContainerInput input, Player player) {
            if (index == 8 || !this.stillValid(player)) return;
            super.clicked(index, button, input, player);
        }

        @Override
        public boolean canDragTo(Slot slot) {
            return slot.index != 8 && super.canDragTo(slot);
        }

        @Override
        public boolean canTakeItemForPickAll(ItemStack stack, Slot slot) {
            return slot.index != 8 && super.canTakeItemForPickAll(stack, slot);
        }

        private static final class VillagerContainer extends SimpleContainer {
            private final Villager villager;

            VillagerContainer(Villager villager) {
                super(9);
                this.villager = villager;
            }

            private boolean owned(int slot) {
                return slot >= 0 && slot < 8 && TickThread.isTickThreadFor(this.villager);
            }

            @Override
            public ItemStack getItem(int slot) {
                return owned(slot) ? villager.getInventory().getItem(slot) : ItemStack.EMPTY;
            }

            @Override
            public ItemStack removeItem(int slot, int amount) {
                return owned(slot) ? villager.getInventory().removeItem(slot, amount) : ItemStack.EMPTY;
            }

            @Override
            public ItemStack removeItemNoUpdate(int slot) {
                return owned(slot) ? villager.getInventory().removeItemNoUpdate(slot) : ItemStack.EMPTY;
            }

            @Override
            public void setItem(int slot, ItemStack stack) {
                if (owned(slot)) villager.getInventory().setItem(slot, stack);
            }

            @Override
            public void setChanged() {
                if (TickThread.isTickThreadFor(villager)) villager.getInventory().setChanged();
            }

            @Override
            public java.util.List<ItemStack> getContents() {
                var view = new java.util.ArrayList<ItemStack>(9);
                for (int i = 0; i < 9; i++) view.add(this.getItem(i));
                return view;
            }

            @Override
            public boolean isEmpty() {
                return TickThread.isTickThreadFor(villager) && villager.getInventory().isEmpty();
            }

            @Override
            public boolean stillValid(Player player) {
                return TickThread.isTickThreadFor(villager) && villager.isAlive() && !villager.isRemoved() && player.distanceTo(villager) < 8.0F;
            }
        }
    }
}
