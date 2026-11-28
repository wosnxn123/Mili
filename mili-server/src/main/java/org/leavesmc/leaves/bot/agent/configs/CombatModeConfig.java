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

import fun.bm.mili.config.modules.function.FakeplayerConfig;
import net.minecraft.nbt.CompoundTag;
import org.jetbrains.annotations.NotNull;
import org.leavesmc.leaves.command.CommandContext;
import org.leavesmc.leaves.command.arguments.EnumArgumentType;

import java.util.Locale;

public class CombatModeConfig extends AbstractBotConfig<CombatModeConfig.CombatMode, CombatModeConfig> {

    public enum CombatMode {
        /** No combat AI. */
        NONE,
        /** Hold position, rotate and attack in melee range. */
        GUARD,
        /** Walk towards distant mobs to lure them, then attack. */
        LURE
    }

    private volatile CombatMode value;

    public CombatModeConfig() {
        super("combat_mode", EnumArgumentType.fromEnum(CombatMode.class), CombatModeConfig::new);
        this.value = FakeplayerConfig.botCombatMode;
    }

    @Override
    public CombatMode getValue() {
        return value;
    }

    @Override
    public void setValue(CombatMode value) throws IllegalArgumentException {
        this.value = value;
    }

    @Override
    public CombatMode loadFromCommand(CommandContext context) {
        return context.getArgument(getName(), CombatMode.class);
    }

    @Override
    public @NotNull CompoundTag save(@NotNull CompoundTag nbt) {
        super.save(nbt);
        nbt.putString(getName(), this.getValue().toString().toLowerCase(Locale.ROOT));
        return nbt;
    }

    @Override
    public void load(@NotNull CompoundTag nbt) {
        String raw = nbt.getStringOr(getName(), FakeplayerConfig.botCombatMode.name());
        CombatMode parsed;
        try {
            parsed = CombatMode.valueOf(raw.toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException e) {
            parsed = CombatMode.NONE;
        }
        this.setValue(parsed);
    }
}
