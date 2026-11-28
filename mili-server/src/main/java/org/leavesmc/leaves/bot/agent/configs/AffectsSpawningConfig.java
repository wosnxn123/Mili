/*
 * This file is part of Leaves (https://github.com/LeavesMC/Leaves)
 *
 * Leaves is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Leaves is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with Leaves. If not, see <https://www.gnu.org/licenses/>.
 */

package org.leavesmc.leaves.bot.agent.configs;

import com.mojang.brigadier.arguments.BoolArgumentType;
import fun.bm.mili.config.modules.function.FakeplayerConfig;
import net.minecraft.nbt.CompoundTag;
import org.jetbrains.annotations.NotNull;
import org.leavesmc.leaves.bot.ServerBot;
import org.leavesmc.leaves.command.CommandContext;

public class AffectsSpawningConfig extends AbstractBotConfig<Boolean, AffectsSpawningConfig> {
    private volatile boolean value;

    public AffectsSpawningConfig() {
        super("affects_spawning", BoolArgumentType.bool(), AffectsSpawningConfig::new);
        this.value = FakeplayerConfig.botAffectsSpawning;
    }

    @Override
    public Boolean getValue() {
        return this.bot == null ? this.value : this.bot.affectsSpawning;
    }

    @Override
    public AbstractBotConfig<Boolean, AffectsSpawningConfig> setBot(ServerBot bot) {
        super.setBot(bot);
        bot.affectsSpawning = this.value;
        return this;
    }

    @Override
    public void setValue(Boolean value) {
        this.value = value;
        if (this.bot != null) {
            this.bot.affectsSpawning = value;
        }
    }

    @Override
    public Boolean loadFromCommand(CommandContext context) {
        return context.getBoolean(getName());
    }

    @Override
    public @NotNull CompoundTag save(@NotNull CompoundTag nbt) {
        super.save(nbt);
        nbt.putBoolean(getName(), this.getValue());
        return nbt;
    }

    @Override
    public void load(@NotNull CompoundTag nbt) {
        this.setValue(nbt.getBooleanOr(getName(), FakeplayerConfig.botAffectsSpawning));
    }
}
