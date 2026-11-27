package org.leavesmc.leaves;

import org.junit.platform.suite.api.SelectClasses;
import org.junit.platform.suite.api.Suite;
import org.leavesmc.leaves.plugin.ServerFeatureManagerTest;
import org.leavesmc.leaves.protocol.servux.litematics.LitematicaSchematicTest;

@Suite
@SelectClasses(value = {LitematicaSchematicTest.class, ServerFeatureManagerTest.class, net.minecraft.world.level.block.CactusRotationTest.class},
        names = {"net.minecraft.world.level.block.CarpetLargeBarrelTest", "net.minecraft.core.dispenser.CarpetDispenserTest"})
public class MiliCompatibilityTestSuite {
}
