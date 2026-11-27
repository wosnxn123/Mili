package fun.bm.mili.carpet;

import com.mojang.logging.LogUtils;
import fun.bm.mili.carpet.config.modules.CoreConfig;
import fun.bm.mili.carpet.config.modules.CounterCompatConfig;
import fun.bm.mili.carpet.config.modules.FakePlayerCompatConfig;
import fun.bm.mili.carpet.config.modules.GeneralCompatConfig;
import fun.bm.mili.config.modules.experiment.CommandConfig;
import fun.bm.mili.config.modules.experiment.RedStoneConfig;
import fun.bm.mili.config.modules.fixes.UpdateSuppressionCrashFixConfig;
import fun.bm.mili.config.modules.function.CreativeFlyNoClipConfig;
import fun.bm.mili.config.modules.function.FakeplayerConfig;
import fun.bm.mili.config.modules.function.LanguageConfig;
import fun.bm.mili.config.modules.function.WoolHopperCounterConfig;
import fun.bm.mili.protocol.CarpetLoggerProtocol;
import me.earthme.luminol.config.modules.optimizations.OptimizedDragonRespawnConfig;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.item.crafting.RecipeManager;
import org.leavesmc.leaves.bot.ServerBot;
import org.leavesmc.leaves.protocol.CarpetServerProtocol;

import java.util.ArrayList;
import java.util.List;

public final class CarpetCompatSync {
    // Mili start - fix: declare init as volatile to prevent check-then-act race in apply()
    private static volatile boolean init = false;
    // Mili end

    public static void apply() {
        if (CoreConfig.enabled) {
            applyGeneralRules();
            applyFakePlayerRules();
            applyCounterRules();
        }
        CarpetLoggerProtocol.refreshConfiguredDefaults(!init);
        registerProtocolRules();

        // After initial load or reload, re-inject compat recipes so that
        // toggling betterCraftableBoneBlock / betterCraftableDispenser takes effect
        // without requiring a full server restart.
        reinjectCompatRecipes();

        init = true;
    }

    private static void reinjectCompatRecipes() {
        MinecraftServer server = MinecraftServer.getServer();
        if (server == null) return;
        try {
            RecipeManager recipeManager = server.getRecipeManager();
            recipeManager.reinjectCompatRecipes();
        // Mili start - fix: catch Throwable and log instead of silently swallowing all errors
        } catch (Throwable t) {
            LogUtils.getLogger().warn("[Mili] Failed to re-inject compat recipes", t);
        }
        // Mili end
    }

    private static void applyGeneralRules() {
        LanguageConfig.lang = GeneralCompatConfig.language;
        UpdateSuppressionCrashFixConfig.enabled = GeneralCompatConfig.amsUpdateSuppressionCrashFix || GeneralCompatConfig.yeetUpdateSuppressionCrash;
        RedStoneConfig.redstoneIgnoreUpwardsUpdate = GeneralCompatConfig.dustTrapdoorReintroduced;
        RedStoneConfig.instantBlockUpdater = GeneralCompatConfig.instantBlockUpdaterReintroduced;
        CommandConfig.tick = GeneralCompatConfig.commandTick;
        CreativeFlyNoClipConfig.enabled = GeneralCompatConfig.creativeNoClip;
        OptimizedDragonRespawnConfig.optimizedRespawn = GeneralCompatConfig.optimizedDragonRespawn;
    }

    private static void applyFakePlayerRules() {
        FakeplayerConfig.enable = FakePlayerCompatConfig.commandBot || FakePlayerCompatConfig.commandPlayer;
        FakeplayerConfig.canResident = FakePlayerCompatConfig.fakePlayerResident;
        FakeplayerConfig.canOpenInventory = FakePlayerCompatConfig.openFakePlayerInventory;
        FakeplayerConfig.tickType = FakePlayerCompatConfig.fakePlayerTicksLikeRealPlayer
                ? ServerBot.TickType.NETWORK
                : ServerBot.TickType.ENTITY_LIST;
    }

    private static void applyCounterRules() {
        WoolHopperCounterConfig.enabled = CounterCompatConfig.hopperCounters;
        WoolHopperCounterConfig.unlimitedSpeed = CounterCompatConfig.hopperCountersUnlimitedSpeed;
    }

    private static List<String> sanitizeDefaultLoggers(List<String> configuredLoggers) {
        return configuredLoggers == null ? List.of() : List.copyOf(configuredLoggers);
    }

    private static void registerProtocolRules() {
        List<CarpetServerProtocol.CarpetRule> rules = new ArrayList<>();

        rules.add(CarpetServerProtocol.CarpetRule.of("carpet", "language", GeneralCompatConfig.language));
        rules.add(CarpetServerProtocol.CarpetRule.of("carpetamsaddition", "amsUpdateSuppressionCrashFix", GeneralCompatConfig.amsUpdateSuppressionCrashFix));
        rules.add(CarpetServerProtocol.CarpetRule.of("carpettisaddition", "yeetUpdateSuppressionCrash", GeneralCompatConfig.yeetUpdateSuppressionCrash));
        rules.add(CarpetServerProtocol.CarpetRule.of("carpettisaddition", "dustTrapdoorReintroduced", GeneralCompatConfig.dustTrapdoorReintroduced));
        rules.add(CarpetServerProtocol.CarpetRule.of("carpettisaddition", "instantBlockUpdaterReintroduced", GeneralCompatConfig.instantBlockUpdaterReintroduced));
        rules.add(CarpetServerProtocol.CarpetRule.of("carpet", "commandTick", GeneralCompatConfig.commandTick));
        rules.add(CarpetServerProtocol.CarpetRule.of("carpet", "creativeNoClip", GeneralCompatConfig.creativeNoClip));
        rules.add(CarpetServerProtocol.CarpetRule.of("carpettisaddition", "optimizedDragonRespawn", GeneralCompatConfig.optimizedDragonRespawn));
        rules.add(CarpetServerProtocol.CarpetRule.of("carpettisaddition", "antiSpamDisabled", GeneralCompatConfig.antiSpamDisabled));
        rules.add(CarpetServerProtocol.CarpetRule.of("carpettisaddition", "blockPlacementIgnoreEntity", GeneralCompatConfig.blockPlacementIgnoreEntity));
        rules.add(CarpetServerProtocol.CarpetRule.of("carpettisaddition", "creativeOpenContainerForcibly", GeneralCompatConfig.creativeOpenContainerForcibly));
        rules.add(CarpetServerProtocol.CarpetRule.of("carpetamsaddition", "creativeOneHitKill", GeneralCompatConfig.creativeOneHitKill));
        rules.add(CarpetServerProtocol.CarpetRule.of("carpettisaddition", "creativeHitRemoveEntity", GeneralCompatConfig.creativeHitRemoveEntity));
        rules.add(CarpetServerProtocol.CarpetRule.of("carpettisaddition", "observerNoDetection", GeneralCompatConfig.observerNoDetection));
        rules.add(CarpetServerProtocol.CarpetRule.of("carpetamsaddition", "bambooModelNoOffset", GeneralCompatConfig.bambooModelNoOffset));
        rules.add(CarpetServerProtocol.CarpetRule.of("carpettisaddition", "creativeNoItemCooldown", GeneralCompatConfig.creativeNoItemCooldown));
        rules.add(CarpetServerProtocol.CarpetRule.of("carpet", "ctrlQCraftingFix", GeneralCompatConfig.ctrlQCraftingFix));
        rules.add(CarpetServerProtocol.CarpetRule.of("carpetamsaddition", "carpetAlwaysSetDefault", GeneralCompatConfig.carpetAlwaysSetDefault));
        rules.add(CarpetServerProtocol.CarpetRule.of("carpet", "placementRotationFix", GeneralCompatConfig.placementRotationFix));
        rules.add(CarpetServerProtocol.CarpetRule.of("carpet", "tntDoNotUpdate", GeneralCompatConfig.tntDoNotUpdate));
        rules.add(CarpetServerProtocol.CarpetRule.of("carpettisaddition", "totallyNoBlockUpdate", GeneralCompatConfig.totallyNoBlockUpdate));
        rules.add(CarpetServerProtocol.CarpetRule.of("carpettisaddition", "tiscmNetworkProtocol", GeneralCompatConfig.tiscmNetworkProtocol));
        rules.add(CarpetServerProtocol.CarpetRule.of("carpetorgaddition", "hopperNoItemCost", GeneralCompatConfig.hopperNoItemCost));
        rules.add(CarpetServerProtocol.CarpetRule.of("carpet", "explosionNoBlockDamage", GeneralCompatConfig.explosionNoBlockDamage));
        rules.add(CarpetServerProtocol.CarpetRule.of("carpettisaddition", "optimizedTNTHighPriority", GeneralCompatConfig.optimizedTNTHighPriority));
        rules.add(CarpetServerProtocol.CarpetRule.of("carpet", "tntPrimerMomentumRemoved", GeneralCompatConfig.tntPrimerMomentumRemoved));
        rules.add(CarpetServerProtocol.CarpetRule.of("carpettisaddition", "tntIgnoreRedstoneSignal", GeneralCompatConfig.tntIgnoreRedstoneSignal));
        rules.add(CarpetServerProtocol.CarpetRule.of("carpettisaddition", "tntDupingFix", GeneralCompatConfig.tntDupingFix));
        rules.add(CarpetServerProtocol.CarpetRule.of("carpet", "interactionUpdates", GeneralCompatConfig.interactionUpdates));
        rules.add(CarpetServerProtocol.CarpetRule.of("carpet", "xpNoCooldown", GeneralCompatConfig.xpNoCooldown));
        rules.add(CarpetServerProtocol.CarpetRule.of("carpetamsaddition", "powerfulExpMending", GeneralCompatConfig.powerfulExpMending));
        rules.add(CarpetServerProtocol.CarpetRule.of("carpettisaddition", "clientSettingsLostOnRespawnFix", GeneralCompatConfig.clientSettingsLostOnRespawnFix));
        rules.add(CarpetServerProtocol.CarpetRule.of("carpetamsaddition", "sensibleEnderman", GeneralCompatConfig.sensibleEnderman));
        rules.add(CarpetServerProtocol.CarpetRule.of("carpettisaddition", "entityInstantDeathRemoval", GeneralCompatConfig.entityInstantDeathRemoval));
        rules.add(CarpetServerProtocol.CarpetRule.of("carpettisaddition", "farmlandTrampledDisabled", GeneralCompatConfig.farmlandTrampledDisabled));
        rules.add(CarpetServerProtocol.CarpetRule.of("carpetamsaddition", "shulkerGolem", GeneralCompatConfig.shulkerGolem));
        rules.add(CarpetServerProtocol.CarpetRule.of("carpetamsaddition", "preventEndSpikeRespawn", GeneralCompatConfig.preventEndSpikeRespawn));
        rules.add(CarpetServerProtocol.CarpetRule.of("carpettisaddition", "yeetOutOfOrderChatKick", GeneralCompatConfig.yeetOutOfOrderChatKick));
        rules.add(CarpetServerProtocol.CarpetRule.of("carpetamsaddition", "betterCraftableBoneBlock", GeneralCompatConfig.betterCraftableBoneBlock));
        rules.add(CarpetServerProtocol.CarpetRule.of("carpetamsaddition", "betterCraftableDispenser", GeneralCompatConfig.betterCraftableDispenser));
        rules.add(CarpetServerProtocol.CarpetRule.of("carpet", "viewDistance", GeneralCompatConfig.viewDistance));
        rules.add(CarpetServerProtocol.CarpetRule.of("carpettisaddition", "tickCommandPermission", GeneralCompatConfig.normalizedTickCommandPermission()));
        rules.add(CarpetServerProtocol.CarpetRule.of("carpettisaddition", "tickFreezeCommandToggleable", GeneralCompatConfig.tickFreezeCommandToggleable));
        rules.add(CarpetServerProtocol.CarpetRule.of("carpettisaddition", "syncServerMsptMetricsData", GeneralCompatConfig.syncServerMsptMetricsData));
        rules.add(CarpetServerProtocol.CarpetRule.of("carpetorgaddition", "simpleInGameCalculator", GeneralCompatConfig.simpleInGameCalculator));
        rules.add(CarpetServerProtocol.CarpetRule.of("carpettisaddition", "microTiming", GeneralCompatConfig.microTiming));
        rules.add(CarpetServerProtocol.CarpetRule.of("carpet", "fastRedstoneDust", GeneralCompatConfig.fastRedstoneDust));
        rules.add(CarpetServerProtocol.CarpetRule.of("carpet", "lagFreeSpawning", GeneralCompatConfig.lagFreeSpawning));
        rules.add(CarpetServerProtocol.CarpetRule.of("carpettisaddition", "optimizedFastEntityMovement", GeneralCompatConfig.optimizedFastEntityMovement));
        rules.add(CarpetServerProtocol.CarpetRule.of("carpettisaddition", "optimizedHardHitBoxEntityCollision", GeneralCompatConfig.optimizedHardHitBoxEntityCollision));
        rules.add(CarpetServerProtocol.CarpetRule.of("carpettisaddition", "tntFuseDuration", GeneralCompatConfig.normalizedTntFuseDuration()));
        rules.add(CarpetServerProtocol.CarpetRule.of("carpet", "defaultLoggers", CarpetLoggerProtocol.serializeConfiguredDefaults(sanitizeDefaultLoggers(GeneralCompatConfig.defaultLoggers))));

        rules.add(CarpetServerProtocol.CarpetRule.of("mili", "commandBot", FakePlayerCompatConfig.commandBot));
        rules.add(CarpetServerProtocol.CarpetRule.of("carpet", "commandPlayer", FakePlayerCompatConfig.commandPlayer));
        rules.add(CarpetServerProtocol.CarpetRule.of("mili", "fakePlayerResident", FakePlayerCompatConfig.fakePlayerResident));
        rules.add(CarpetServerProtocol.CarpetRule.of("mili", "openFakePlayerInventory", FakePlayerCompatConfig.openFakePlayerInventory));
        rules.add(CarpetServerProtocol.CarpetRule.of("carpettisaddition", "fakePlayerTicksLikeRealPlayer", FakePlayerCompatConfig.fakePlayerTicksLikeRealPlayer));
        rules.add(CarpetServerProtocol.CarpetRule.of("carpetamsaddition", "fakePlayerDefaultSurvivalMode", FakePlayerCompatConfig.fakePlayerDefaultSurvivalMode));
        rules.add(CarpetServerProtocol.CarpetRule.of("carpetamsaddition", "fakePlayerInteractLikeClient", FakePlayerCompatConfig.fakePlayerInteractLikeClient));
        rules.add(CarpetServerProtocol.CarpetRule.of("mili", "fakePlayerAutoReplaceTool", FakePlayerCompatConfig.fakePlayerAutoReplaceTool));
        rules.add(CarpetServerProtocol.CarpetRule.of("mili", "fakePlayerAutoReplenishment", FakePlayerCompatConfig.fakePlayerAutoReplenishment));
        rules.add(CarpetServerProtocol.CarpetRule.of("carpetamsaddition", "fakePlayerAutoReplenishmentFormShulkerBox", FakePlayerCompatConfig.fakePlayerAutoReplenishmentFormShulkerBox));
        rules.add(CarpetServerProtocol.CarpetRule.of("mili", "fakePlayerAutoFish", FakePlayerCompatConfig.fakePlayerAutoFish));
        rules.add(CarpetServerProtocol.CarpetRule.of("mili", "fakePlayerReloadAction", FakePlayerCompatConfig.fakePlayerReloadAction));

        rules.add(CarpetServerProtocol.CarpetRule.of("carpet", "hopperCounters", CounterCompatConfig.hopperCounters));
        rules.add(CarpetServerProtocol.CarpetRule.of("carpettisaddition", "hopperCountersUnlimitedSpeed", CounterCompatConfig.hopperCountersUnlimitedSpeed));

        rules.add(CarpetServerProtocol.CarpetRule.of("carpet", "renewableBlackstone", GeneralCompatConfig.renewableBlackstone));
        rules.add(CarpetServerProtocol.CarpetRule.of("carpet", "liquidDamageDisabled", GeneralCompatConfig.liquidDamageDisabled));
        rules.add(CarpetServerProtocol.CarpetRule.of("carpet", "thickFungusGrowth", GeneralCompatConfig.thickFungusGrowth));
        rules.add(CarpetServerProtocol.CarpetRule.of("carpetamsaddition", "extinguishedCampfire", GeneralCompatConfig.extinguishedCampfire));
        rules.add(CarpetServerProtocol.CarpetRule.of("carpetamsaddition", "easyCompost", GeneralCompatConfig.easyCompost));
        rules.add(CarpetServerProtocol.CarpetRule.of("carpetamsaddition", "undyingCoral", GeneralCompatConfig.undyingCoral));
        rules.add(CarpetServerProtocol.CarpetRule.of("carpetamsaddition", "endermanTeleportRandomlyDisabled", GeneralCompatConfig.endermanTeleportRandomlyDisabled));
        rules.add(CarpetServerProtocol.CarpetRule.of("carpetorgaddition", "disableBatCanSpawn", GeneralCompatConfig.disableBatCanSpawn));
        rules.add(CarpetServerProtocol.CarpetRule.of("carpetorgaddition", "disableWaterFreezes", GeneralCompatConfig.disableWaterFreezes));
        rules.add(CarpetServerProtocol.CarpetRule.of("carpetorgaddition", "reusableSmithingTemplate", GeneralCompatConfig.reusableSmithingTemplate));
        rules.add(CarpetServerProtocol.CarpetRule.of("carpetorgaddition", "disableFurnaceDropExperience", GeneralCompatConfig.disableFurnaceDropExperience));
        rules.add(CarpetServerProtocol.CarpetRule.of("carpetorgaddition", "staringEndermanNotAngry", GeneralCompatConfig.staringEndermanNotAngry));
        rules.add(CarpetServerProtocol.CarpetRule.of("carpettisaddition", "turtleEggTrampledDisabled", GeneralCompatConfig.turtleEggTrampledDisabled));
        rules.add(CarpetServerProtocol.CarpetRule.of("carpettisaddition", "undeadDontBurnInSunlight", GeneralCompatConfig.undeadDontBurnInSunlight));
        rules.add(CarpetServerProtocol.CarpetRule.of("carpettisaddition", "leaderZombieSpawnWithMaxHealthDisabled", GeneralCompatConfig.leaderZombieSpawnWithMaxHealthDisabled));
        rules.add(CarpetServerProtocol.CarpetRule.of("carpettisaddition", "endPortalOpenedSoundDisabled", GeneralCompatConfig.endPortalOpenedSoundDisabled));
        rules.add(CarpetServerProtocol.CarpetRule.of("carpettisaddition", "toughWitherRose", GeneralCompatConfig.toughWitherRose));

        rules.add(CarpetServerProtocol.CarpetRule.of("carpetamsaddition", "endermanPickUpDisabled", GeneralCompatConfig.endermanPickUpDisabled));
        rules.add(CarpetServerProtocol.CarpetRule.of("carpettisaddition", "breedingCooldownDisabled", GeneralCompatConfig.breedingCooldownDisabled));
        rules.add(CarpetServerProtocol.CarpetRule.of("carpettisaddition", "snowMeltMinLightLevel", GeneralCompatConfig.normalizedSnowMeltMinLightLevel()));

        rules.add(CarpetServerProtocol.CarpetRule.of("carpet", "flippinCactus", GeneralCompatConfig.flippinCactus));
        rules.add(CarpetServerProtocol.CarpetRule.of("carpetamsaddition", "flippinCactusExtras", GeneralCompatConfig.flippinCactusExtras));
        rules.add(CarpetServerProtocol.CarpetRule.of("carpetamsaddition", "flippinCactusSoundEffect", GeneralCompatConfig.flippinCactusSoundEffect));
        rules.add(CarpetServerProtocol.CarpetRule.of("carpet", "rotatorBlock", GeneralCompatConfig.rotatorBlock));
        rules.add(CarpetServerProtocol.CarpetRule.of("carpettisaddition", "dispensersFireDragonBreath", GeneralCompatConfig.dispensersFireDragonBreath));
        rules.add(CarpetServerProtocol.CarpetRule.of("carpettisaddition", "dispenserNoItemCost", GeneralCompatConfig.dispenserNoItemCost));
        rules.add(CarpetServerProtocol.CarpetRule.of("carpet", "noCreeperBlockBreaking", GeneralCompatConfig.noCreeperBlockBreaking));
        rules.add(CarpetServerProtocol.CarpetRule.of("carpet", "noGhastBlockBreaking", GeneralCompatConfig.noGhastBlockBreaking));
        rules.add(CarpetServerProtocol.CarpetRule.of("carpet", "disableGhastFire", GeneralCompatConfig.disableGhastFire));
        rules.add(CarpetServerProtocol.CarpetRule.of("carpet", "disableBlazeFire", GeneralCompatConfig.disableBlazeFire));
        rules.add(CarpetServerProtocol.CarpetRule.of("carpettisaddition", "largeBarrel", GeneralCompatConfig.largeBarrel));
        GameplayCompatRules.registerProtocolRules(rules);
        WorldGameplayRules.registerProtocolRules(rules);
        EntityGameplayRules.registerProtocolRules(rules);
        CarpetServerProtocol.CarpetRules.replace(rules);
    }
}
