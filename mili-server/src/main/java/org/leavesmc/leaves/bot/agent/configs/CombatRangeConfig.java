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

import com.mojang.brigadier.arguments.DoubleArgumentType;
import fun.bm.mili.config.modules.function.FakeplayerConfig;
import net.minecraft.nbt.CompoundTag;
import org.jetbrains.annotations.NotNull;
import org.leavesmc.leaves.command.CommandContext;

public class CombatRangeConfig extends AbstractBotConfig<Double, CombatRangeConfig> {
    private volatile double value;

    public CombatRangeConfig() {
        super("combat_range", DoubleArgumentType.doubleArg(0.5D, 32.0D), CombatRangeConfig::new);
        this.setValue(FakeplayerConfig.botCombatRange);
    }

    @Override
    public Double getValue() {
        return value;
    }

    @Override
    public void setValue(Double value) throws IllegalArgumentException {
        this.value = Double.isFinite(value) ? Math.clamp(value, 0.5D, 32.0D) : 3.0D;
    }

    @Override
    public Double loadFromCommand(CommandContext context) {
        // DoubleArgumentType yields a Double, so ask for Double.class here. Requesting
        // Float.class would make the underlying bridge cast a Double to Float and blow up.
        return context.getArgument(getName(), Double.class);
    }

    @Override
    public @NotNull CompoundTag save(@NotNull CompoundTag nbt) {
        super.save(nbt);
        nbt.putDouble(getName(), this.getValue());
        return nbt;
    }

    @Override
    public void load(@NotNull CompoundTag nbt) {
        this.setValue(nbt.getDouble(getName()).orElse(FakeplayerConfig.botCombatRange));
    }
}
