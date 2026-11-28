package fun.bm.mili.carpet;

import ca.spottedleaf.moonrise.common.util.TickThread;
import fun.bm.mili.carpet.config.modules.GameplayCompatConfig;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.GameType;
import org.leavesmc.leaves.protocol.CarpetServerProtocol.CarpetRule;

public final class GameplayCompatRules {
    private GameplayCompatRules() {
    }

    public static boolean ownsLoaded(final ServerLevel level, final BlockPos pos) {
        return TickThread.isTickThreadFor(level, pos) && level.hasChunk(pos.getX() >> 4, pos.getZ() >> 4);
    }

    public static boolean ignoresVoidDamage(final GameType gameType) {
        String configured = GameplayCompatConfig.voidDamageIgnorePlayer;
        if (configured == null || "false".equalsIgnoreCase(configured)) return false;
        if ("true".equalsIgnoreCase(configured)) return true;
        for (String mode : configured.split(",")) {
            if (mode.trim().equalsIgnoreCase(gameType.getName())) return true;
        }
        return false;
    }

    public static float explosionResistance(net.minecraft.world.level.block.Block block, float original) {
        if (GameplayCompatConfig.blowUpEverything) return 0.0F;
        double configured = GameplayCompatConfig.effectiveWorldEaterResistance();
        if (configured < 0 || original < 17) return original;
        return protectedResistanceBlock(block) ? original : (float) configured;
    }

    private static boolean protectedResistanceBlock(net.minecraft.world.level.block.Block block) {
        return block == net.minecraft.world.level.block.Blocks.BEDROCK || block == net.minecraft.world.level.block.Blocks.ANVIL
            || block == net.minecraft.world.level.block.Blocks.CHIPPED_ANVIL || block == net.minecraft.world.level.block.Blocks.DAMAGED_ANVIL
            || block == net.minecraft.world.level.block.Blocks.END_PORTAL_FRAME || block == net.minecraft.world.level.block.Blocks.END_PORTAL
            || block == net.minecraft.world.level.block.Blocks.END_GATEWAY;
    }

    public static void announcePhantom(ServerLevel level, net.minecraft.server.level.ServerPlayer trigger, BlockPos pos) {
        var message = net.minecraft.network.chat.Component.translatableWithFallback("carpetamsaddition.rule.phantomSpawnAlert.msg",
            "Phantom spawned near %1$s at %2$s", trigger.getGameProfile().name(), pos.toShortString());
        var original = trigger.getBukkitEntity();
        for (var recipient : level.getServer().getPlayerList().getPlayers()) {
            recipient.getBukkitEntity().taskScheduler.schedule((net.minecraft.server.level.ServerPlayer current) -> {
                if (GameplayCompatConfig.phantomSpawnAlert && current.level() == level && current.getBukkitEntity().canSee(original)) {
                    current.sendSystemMessage(message);
                }
            }, null, 1L);
        }
    }

    static void registerProtocolRules(final List<CarpetRule> rules) {
        rules.add(CarpetRule.of("carpetorgaddition", "blockDropsDirectlyEnterInventory", CollectionGameplayRules.miningDropMode()));
        rules.add(CarpetRule.of("carpettisaddition", "hopperXpCounters", GameplayCompatConfig.hopperXpCounters));
        rules.add(CarpetRule.of("carpet", "maxEntityCollisions", Math.max(0, GameplayCompatConfig.maxEntityCollisions)));
        rules.add(CarpetRule.of("carpetamsaddition", "blowUpEverything", GameplayCompatConfig.blowUpEverything));
        rules.add(CarpetRule.of("carpetamsaddition", "enhancedWorldEater", Double.toString(GameplayCompatConfig.effectiveWorldEaterResistance())));
        rules.add(CarpetRule.of("carpetamsaddition", "phantomSpawnAlert", GameplayCompatConfig.phantomSpawnAlert));
        rules.add(CarpetRule.of("carpetamsaddition", "witchGlowstoneDustDropController", Math.clamp(GameplayCompatConfig.witchGlowstoneDustDropController, -1, 64)));
        rules.add(CarpetRule.of("carpetamsaddition", "witchRedstoneDustDropController", Math.clamp(GameplayCompatConfig.witchRedstoneDustDropController, -1, 64)));
        rules.add(CarpetRule.of("carpetorgaddition", "truePeacefulMode", GameplayCompatConfig.truePeacefulMode));
        rules.add(CarpetRule.of("carpettisaddition", "flattenTriangularDistribution", GameplayCompatConfig.useFlatDistribution()));
        rules.add(CarpetRule.of("carpet", "cleanLogs", GameplayCompatConfig.cleanLogs));
        rules.add(CarpetRule.of("carpet", "customMOTD", GameplayCompatConfig.customMOTD));
        rules.add(CarpetRule.of("carpet", "disableDamageImmunity", GameplayCompatConfig.disableDamageImmunity));
        rules.add(CarpetRule.of("carpet", "hardcodeTNTangle", Double.toString(GameplayCompatConfig.hardcodeTNTangle())));
        rules.add(CarpetRule.of("carpet", "lightningKillsDropsFix", GameplayCompatConfig.lightningKillsDropsFix));
        rules.add(CarpetRule.of("carpet", "moreBlueSkulls", GameplayCompatConfig.moreBlueSkulls));
        rules.add(CarpetRule.of("carpet", "movableAmethyst", GameplayCompatConfig.movableAmethyst));
        rules.add(CarpetRule.of("carpet", "pingPlayerListLimit", GameplayCompatConfig.pingPlayerListLimit()));
        rules.add(CarpetRule.of("carpet", "sculkSensorRange", GameplayCompatConfig.sculkSensorRange()));
        rules.add(CarpetRule.of("carpet", "xpFromExplosions", GameplayCompatConfig.xpFromExplosions));
        rules.add(CarpetRule.of("carpetamsaddition", "bambooCollisionBoxDisabled", GameplayCompatConfig.bambooCollisionBoxDisabled));
        rules.add(CarpetRule.of("carpetamsaddition", "betterCraftablePolishedBlackStoneButton", GameplayCompatConfig.betterCraftablePolishedBlackStoneButton));
        rules.add(CarpetRule.of("carpetamsaddition", "blueSkullController", GameplayCompatConfig.blueSkullController));
        rules.add(CarpetRule.of("carpetamsaddition", "cakeBlockDropOnBreak", GameplayCompatConfig.cakeBlockDropOnBreak));
        rules.add(CarpetRule.of("carpetamsaddition", "craftableElytra", GameplayCompatConfig.craftableElytra));
        rules.add(CarpetRule.of("carpetamsaddition", "craftableEnchantedGoldenApples", GameplayCompatConfig.craftableEnchantedGoldenApples));
        rules.add(CarpetRule.of("carpetamsaddition", "creativeShulkerBoxDropsDisabled", GameplayCompatConfig.creativeShulkerBoxDropsDisabled));
        rules.add(CarpetRule.of("carpetamsaddition", "easyGetPitcherPod", GameplayCompatConfig.easyGetPitcherPod()));
        rules.add(CarpetRule.of("carpetamsaddition", "easyMaxLevelBeacon", GameplayCompatConfig.easyMaxLevelBeacon));
        rules.add(CarpetRule.of("carpetamsaddition", "easyMineDragonEgg", GameplayCompatConfig.easyMineDragonEgg));
        rules.add(CarpetRule.of("carpetamsaddition", "easyWitherSkeletonSkullDrop", GameplayCompatConfig.easyWitherSkeletonSkullDrop));
        rules.add(CarpetRule.of("carpetamsaddition", "enderDragonNoDestroyBlock", GameplayCompatConfig.enderDragonNoDestroyBlock));
        rules.add(CarpetRule.of("carpetamsaddition", "fakePlayerNoScoreboardCounter", GameplayCompatConfig.fakePlayerNoScoreboardCounter));
        rules.add(CarpetRule.of("carpetamsaddition", "foliageGenerateDisabled", GameplayCompatConfig.foliageGenerateDisabled));
        rules.add(CarpetRule.of("carpetamsaddition", "fullMoonEveryDay", GameplayCompatConfig.fullMoonEveryDay));
        rules.add(CarpetRule.of("carpetamsaddition", "furnaceSmeltingTimeController", GameplayCompatConfig.furnaceSmeltingTimeController()));
        rules.add(CarpetRule.of("carpetamsaddition", "hopperSuctionDisabled", GameplayCompatConfig.hopperSuctionDisabled));
        rules.add(CarpetRule.of("carpetamsaddition", "immuneShulkerBullet", GameplayCompatConfig.immuneShulkerBullet));
        rules.add(CarpetRule.of("carpetamsaddition", "infiniteDurability", GameplayCompatConfig.infiniteDurability));
        rules.add(CarpetRule.of("carpetamsaddition", "invulnerable", GameplayCompatConfig.invulnerable));
        rules.add(CarpetRule.of("carpetamsaddition", "ironGolemNoDropFlower", GameplayCompatConfig.ironGolemNoDropFlower));
        rules.add(CarpetRule.of("carpetamsaddition", "jebSheepDropRandomColorWool", GameplayCompatConfig.jebSheepDropRandomColorWool));
        rules.add(CarpetRule.of("carpetamsaddition", "maxChainUpdateDepth", GameplayCompatConfig.maxChainUpdateDepth()));
        rules.add(CarpetRule.of("carpetamsaddition", "mitePearl", GameplayCompatConfig.mitePearl));
        rules.add(CarpetRule.of("carpetamsaddition", "netherWaterPlacement", GameplayCompatConfig.netherWaterPlacement));
        rules.add(CarpetRule.of("carpetamsaddition", "noCakeEating", GameplayCompatConfig.noCakeEating));
        rules.add(CarpetRule.of("carpetamsaddition", "noEnchantedGoldenAppleEating", GameplayCompatConfig.noEnchantedGoldenAppleEating));
        rules.add(CarpetRule.of("carpetamsaddition", "perfectInvisibility", GameplayCompatConfig.perfectInvisibility));
        rules.add(CarpetRule.of("carpetamsaddition", "pointedDripstoneCollisionBoxDisabled", GameplayCompatConfig.pointedDripstoneCollisionBoxDisabled));
        rules.add(CarpetRule.of("carpetamsaddition", "rottenFleshBurnedIntoLeather", GameplayCompatConfig.rottenFleshBurnedIntoLeather));
        rules.add(CarpetRule.of("carpetamsaddition", "safeFlight", GameplayCompatConfig.safeFlight));
        rules.add(CarpetRule.of("carpetamsaddition", "safePointedDripstone", GameplayCompatConfig.safePointedDripstone));
        rules.add(CarpetRule.of("carpetamsaddition", "shulkerHitLevitationDisabled", GameplayCompatConfig.shulkerHitLevitationDisabled));
        rules.add(CarpetRule.of("carpetamsaddition", "sneakInvisibility", GameplayCompatConfig.sneakInvisibility));
        rules.add(CarpetRule.of("carpetamsaddition", "sneakToEatCake", GameplayCompatConfig.sneakToEatCake));
        rules.add(CarpetRule.of("carpetamsaddition", "sneakToEditSign", GameplayCompatConfig.sneakToEditSign));
        rules.add(CarpetRule.of("carpetamsaddition", "superZombieDoctor", GameplayCompatConfig.superZombieDoctor));
        rules.add(CarpetRule.of("carpetamsaddition", "tntPowerController", Double.toString(GameplayCompatConfig.tntPowerController())));
        rules.add(CarpetRule.of("carpetamsaddition", "useItemCooldownDisabled", GameplayCompatConfig.useItemCooldownDisabled));
        rules.add(CarpetRule.of("carpetorgaddition", "applyToolEffectsImmediately", GameplayCompatConfig.applyToolEffectsImmediately));
        rules.add(CarpetRule.of("carpetorgaddition", "bindingCurseInvalidation", GameplayCompatConfig.bindingCurseInvalidation));
        rules.add(CarpetRule.of("carpetorgaddition", "climbingBoat", GameplayCompatConfig.climbingBoat));
        rules.add(CarpetRule.of("carpetorgaddition", "customPiglinBarteringTime", GameplayCompatConfig.customPiglinBarteringTime()));
        rules.add(CarpetRule.of("carpetorgaddition", "disableMobPeacefulDespawn", GameplayCompatConfig.disableMobPeacefulDespawn));
        rules.add(CarpetRule.of("carpetorgaddition", "disableOpenOrWaterDetection", GameplayCompatConfig.disableOpenOrWaterDetection));
        rules.add(CarpetRule.of("carpetorgaddition", "disableRespawnBlocksExplode", GameplayCompatConfig.disableRespawnBlocksExplode));
        rules.add(CarpetRule.of("carpetorgaddition", "fireworkRocketUseCooldown", GameplayCompatConfig.fireworkRocketUseCooldown));
        rules.add(CarpetRule.of("carpetorgaddition", "healthNotFullCanEat", GameplayCompatConfig.healthNotFullCanEat));
        rules.add(CarpetRule.of("carpetorgaddition", "notDamageEnderPearl", GameplayCompatConfig.notDamageEnderPearl));
        rules.add(CarpetRule.of("carpetorgaddition", "peacefulCreeper", GameplayCompatConfig.peacefulCreeper));
        rules.add(CarpetRule.of("carpetorgaddition", "playerDropsNotDespawning", GameplayCompatConfig.playerDropsNotDespawning));
        rules.add(CarpetRule.of("carpetorgaddition", "riptideIgnoreConditions", GameplayCompatConfig.riptideIgnoreConditions));
        rules.add(CarpetRule.of("carpetorgaddition", "setAnvilExperienceConsumptionLimit", GameplayCompatConfig.setAnvilExperienceConsumptionLimit()));
        rules.add(CarpetRule.of("carpetorgaddition", "totemOfUndyingInvincibleTime", GameplayCompatConfig.totemOfUndyingInvincibleTime));
        rules.add(CarpetRule.of("carpetorgaddition", "turtleEggFastHatch", GameplayCompatConfig.turtleEggFastHatch));
        rules.add(CarpetRule.of("carpetorgaddition", "turtleEggFastMine", GameplayCompatConfig.turtleEggFastMine));
        rules.add(CarpetRule.of("carpetorgaddition", "villagerHeal", GameplayCompatConfig.villagerHeal));
        rules.add(CarpetRule.of("carpettisaddition", "blockEventPacketRange", Double.toString(GameplayCompatConfig.blockEventPacketRange())));
        rules.add(CarpetRule.of("carpettisaddition", "creativeInstantTame", GameplayCompatConfig.creativeInstantTame));
        rules.add(CarpetRule.of("carpettisaddition", "creativeNetherWaterPlacement", GameplayCompatConfig.creativeNetherWaterPlacement));
        rules.add(CarpetRule.of("carpettisaddition", "dropperNoItemCost", GameplayCompatConfig.dropperNoItemCost));
        rules.add(CarpetRule.of("carpettisaddition", "elytraFireworkKeepLeashConnection", GameplayCompatConfig.elytraFireworkKeepLeashConnection));
        rules.add(CarpetRule.of("carpettisaddition", "entityMomentumLoss", GameplayCompatConfig.entityMomentumLoss));
        rules.add(CarpetRule.of("carpettisaddition", "entityPathNavigationStuckDetectionUseRealTimeReintroduced", GameplayCompatConfig.entityPathNavigationStuckDetectionUseRealTimeReintroduced));
        rules.add(CarpetRule.of("carpettisaddition", "explosionNoEntityInfluence", GameplayCompatConfig.explosionNoEntityInfluence));
        rules.add(CarpetRule.of("carpettisaddition", "fluidDestructionDisabled", GameplayCompatConfig.fluidDestructionDisabled));
        rules.add(CarpetRule.of("carpettisaddition", "gazeDisguiseEquipmentExtended", GameplayCompatConfig.gazeDisguiseEquipmentExtended));
        rules.add(CarpetRule.of("carpettisaddition", "itemEntitySkipMovementDisabled", GameplayCompatConfig.itemEntitySkipMovementDisabled));
        rules.add(CarpetRule.of("carpettisaddition", "moveableReinforcedDeepslate", GameplayCompatConfig.moveableReinforcedDeepslate));
        rules.add(CarpetRule.of("carpettisaddition", "naturalSpawningUse13Heightmap", GameplayCompatConfig.naturalSpawningUse13Heightmap));
        rules.add(CarpetRule.of("carpettisaddition", "naturalSpawningUse13HeightmapExtra", GameplayCompatConfig.naturalSpawningUse13HeightmapExtra));
        rules.add(CarpetRule.of("carpettisaddition", "oakBalloonPercent", GameplayCompatConfig.oakBalloonPercent()));
        rules.add(CarpetRule.of("carpettisaddition", "repeaterHalfDelay", GameplayCompatConfig.repeaterHalfDelay));
        rules.add(CarpetRule.of("carpettisaddition", "structureBlockDoNotPreserveFluid", GameplayCompatConfig.structureBlockDoNotPreserveFluid));
        rules.add(CarpetRule.of("carpettisaddition", "vaultBlacklistDisabled", GameplayCompatConfig.vaultBlacklistDisabled));
        rules.add(CarpetRule.of("carpettisaddition", "voidDamageAmount", Double.toString(GameplayCompatConfig.voidDamageAmount())));
        rules.add(CarpetRule.of("carpettisaddition", "voidDamageIgnorePlayer", GameplayCompatConfig.voidDamageIgnorePlayer));
        rules.add(CarpetRule.of("carpettisaddition", "voidRelatedAltitude", Double.toString(GameplayCompatConfig.voidRelatedAltitude())));
        rules.add(CarpetRule.of("carpettisaddition", "xpTrackingDistance", Double.toString(GameplayCompatConfig.xpTrackingDistance())));
        rules.add(CarpetRule.of("carpettisaddition", "zombifiedPiglinDropLootIfAngryReintroduced", GameplayCompatConfig.zombifiedPiglinDropLootIfAngryReintroduced));
    }
}
