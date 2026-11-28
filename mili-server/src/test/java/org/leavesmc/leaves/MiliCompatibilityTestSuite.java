package org.leavesmc.leaves;

import org.junit.platform.suite.api.SelectClasses;
import org.junit.platform.suite.api.Suite;
import org.leavesmc.leaves.plugin.ServerFeatureManagerTest;
import org.leavesmc.leaves.protocol.servux.litematics.LitematicaSchematicTest;

@Suite
@SelectClasses(value = {LitematicaSchematicTest.class, ServerFeatureManagerTest.class, net.minecraft.world.level.block.CactusRotationTest.class},
        names = {"net.minecraft.world.level.block.CarpetLargeBarrelTest", "net.minecraft.core.dispenser.CarpetDispenserTest",
                "fun.bm.mili.portal.PortalSearchChunkLoadsTest", "fun.bm.mili.carpet.WorldGameplayRulesTest",
                "fun.bm.mili.carpet.EntityGameplayRulesTest", "net.minecraft.world.inventory.ContainerR2Test",
                "org.leavesmc.leaves.bot.BotCombatTestSuite", "fun.bm.mili.carpet.WorldR2RulesTest",
                "net.minecraft.world.level.block.piston.MovablePistonCargoTest", "fun.bm.mili.carpet.timing.MicroTimingTest",
                "fun.bm.mili.protocol.CarpetHudTest", "org.leavesmc.leaves.protocol.CarpetHelloTest",
                "fun.bm.mili.protocol.tiscm.TISCMProtocolTest"})
public class MiliCompatibilityTestSuite {
}
