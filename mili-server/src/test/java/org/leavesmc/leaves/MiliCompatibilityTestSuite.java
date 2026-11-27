package org.leavesmc.leaves;

import org.junit.platform.suite.api.SelectClasses;
import org.junit.platform.suite.api.Suite;
import org.leavesmc.leaves.plugin.ServerFeatureManagerTest;
import org.leavesmc.leaves.protocol.servux.litematics.LitematicaSchematicTest;

@Suite
@SelectClasses({LitematicaSchematicTest.class, ServerFeatureManagerTest.class})
public class MiliCompatibilityTestSuite {
}
