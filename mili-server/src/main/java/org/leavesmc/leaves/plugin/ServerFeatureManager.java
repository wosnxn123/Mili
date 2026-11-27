/*
 * This file is licensed under the MIT license.
 * Origin : Leaves (https://github.com/LeavesMC/Leaves)
 */

package org.leavesmc.leaves.plugin;

import java.util.HashSet;
import java.util.Set;

import static org.leavesmc.leaves.plugin.Features.*;

public class ServerFeatureManager implements FeatureManager {
    public static ServerFeatureManager INSTANCE = new ServerFeatureManager();
    private final Set<String> availableFeatures = new HashSet<>();

    ServerFeatureManager() {
        availableFeatures.addAll(Set.of(
                FAKEPLAYER,
                PHOTOGRAPHER
        ));
        // Match the pinned Riceear launcher's ENABLE_LEAVES_PLUGIN predicate.
        if (Boolean.getBoolean("leavesclip.enable.mixin") || Boolean.getBoolean("riceear.enable.mixin")) {
            availableFeatures.add(MIXIN);
        }
    }

    @Override
    public Set<String> getAvailableFeatures() {
        return availableFeatures;
    }

    @Override
    public boolean isFeatureAvailable(String feature) {
        return availableFeatures.contains(feature);
    }
}
