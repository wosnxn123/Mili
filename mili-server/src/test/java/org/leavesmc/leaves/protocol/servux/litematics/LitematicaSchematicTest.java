package org.leavesmc.leaves.protocol.servux.litematics;

import net.minecraft.SharedConstants;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.leavesmc.leaves.protocol.servux.litematics.utils.Schema;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class LitematicaSchematicTest {
    @BeforeAll
    static void detectMinecraftVersion() {
        SharedConstants.tryDetectVersion();
    }

    @Test
    void readsSchematicWithCurrentDataVersion() {
        int dataVersion = SharedConstants.getCurrentVersion().dataVersion().version();
        CompoundTag root = schematicTag(7);
        root.putInt("MinecraftDataVersion", dataVersion);

        LitematicaSchematic schematic = LitematicaSchematic.readFromNBT(root);
        assertEquals(dataVersion, schematic.metadata().minecraftDataVersion());
        assertEquals(7, schematic.metadata().schematicVersion());
    }

    @ParameterizedTest
    @ValueSource(ints = {4325, 6000})
    void keepsExplicitDataVersion(int dataVersion) {
        CompoundTag root = schematicTag(7);
        root.putInt("MinecraftDataVersion", dataVersion);

        assertEquals(dataVersion, LitematicaSchematic.readFromNBT(root).metadata().minecraftDataVersion());
    }

    @Test
    void defaultsMissingTagToServerDataVersionWithoutWritingItBack() {
        CompoundTag root = schematicTag(7);

        LitematicaSchematic schematic = LitematicaSchematic.readFromNBT(root);
        assertEquals(SharedConstants.getCurrentVersion().dataVersion().version(), schematic.metadata().minecraftDataVersion());
        assertFalse(root.contains("MinecraftDataVersion"));
    }

    @Test
    void rejectsMalformedExplicitDataVersionInsteadOfDefaulting() {
        CompoundTag root = schematicTag(7);
        root.putString("MinecraftDataVersion", "not an integer");

        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> LitematicaSchematic.readFromNBT(root));
        assertTrue(exception.getMessage().contains("MinecraftDataVersion"));
    }

    @Test
    void readsLegacyWrapperWithoutUpgradingMinecraftData() {
        CompoundTag root = schematicTag(1);
        root.putInt("MinecraftDataVersion", 1631);
        CompoundTag tile = new CompoundTag();
        tile.putString("id", "minecraft:sign");
        tile.putString("Text1", "{\"text\":\"Legacy\"}");
        CompoundTag wrappedTile = position(0, 0, 0);
        wrappedTile.put("TileNBT", tile);
        ListTag tiles = new ListTag();
        tiles.add(wrappedTile);
        CompoundTag region = new CompoundTag();
        region.put("Position", position(0, 0, 0));
        region.put("Size", position(1, 1, 1));
        region.put("TileEntities", tiles);
        CompoundTag regions = new CompoundTag();
        regions.put("sample", region);
        root.put("Regions", regions);

        LitematicaSchematic schematic = LitematicaSchematic.readFromNBT(root);
        assertEquals(1, schematic.metadata().schematicVersion());
        assertEquals(1631, schematic.metadata().minecraftDataVersion());
        assertEquals(Schema.SCHEMA_1_13_02, schematic.metadata().schema());
        CompoundTag loadedTile = schematic.getSubRegion("sample").tileEntities().get(BlockPos.ZERO);
        assertEquals("minecraft:sign", loadedTile.getStringOr("id", ""));
        assertEquals("{\"text\":\"Legacy\"}", loadedTile.getStringOr("Text1", ""));
        assertFalse(loadedTile.contains("front_text"));
    }

    private static CompoundTag schematicTag(int version) {
        CompoundTag root = new CompoundTag();
        root.putInt("Version", version);
        return root;
    }

    private static CompoundTag position(int x, int y, int z) {
        CompoundTag position = new CompoundTag();
        position.putInt("x", x);
        position.putInt("y", y);
        position.putInt("z", z);
        return position;
    }
}
