package xyz.alexcrea.cuanvil.update;

import org.junit.jupiter.api.Test;
import xyz.alexcrea.cuanvil.config.ConfigHolder;
import xyz.alexcrea.cuanvil.tests.SharedCustomAnvilTest;
import xyz.alexcrea.cuanvil.update.minecraft.Update_1_20_5;
import static org.junit.jupiter.api.Assertions.*;

class LegacyMigrationTests extends SharedCustomAnvilTest {
    @Test void customSweepingLimitsAndCostsSurviveLegacyMigration() {
        var config = ConfigHolder.DEFAULT_CONFIG.getConfig();
        config.set("enchant_limits.minecraft:sweeping_edge", null);
        config.set("enchant_limits.sweeping_edge", null);
        config.set("enchant_limits.minecraft:sweeping", 9);
        config.set("enchant_values.minecraft:sweeping_edge", null);
        config.set("enchant_values.sweeping_edge", null);
        config.set("enchant_values.minecraft:sweeping.item", 7);
        config.set("enchant_values.minecraft:sweeping.book", 3);
        var update = new UpdateHandler.UpdatedConfigList();
        Update_1_20_5.updateName(update);
        update.cleanup(null);
        config = ConfigHolder.DEFAULT_CONFIG.getConfig();
        assertEquals(9, config.getInt("enchant_limits.minecraft:sweeping_edge"));
        assertEquals(7, config.getInt("enchant_values.minecraft:sweeping_edge.item"));
        assertEquals(3, config.getInt("enchant_values.minecraft:sweeping_edge.book"));
        assertFalse(config.contains("enchant_limits.minecraft:sweeping", true));
    }

    @Test void existingModernSettingsTakePrecedence() {
        var config = ConfigHolder.DEFAULT_CONFIG.getConfig();
        config.set("enchant_limits.minecraft:sweeping", 9);
        config.set("enchant_limits.minecraft:sweeping_edge", 12);
        config.set("enchant_values.minecraft:sweeping.item", 7);
        config.set("enchant_values.minecraft:sweeping_edge.item", 15);
        var update = new UpdateHandler.UpdatedConfigList();
        Update_1_20_5.updateName(update);
        update.cleanup(null);
        config = ConfigHolder.DEFAULT_CONFIG.getConfig();
        assertEquals(12, config.getInt("enchant_limits.minecraft:sweeping_edge"));
        assertEquals(15, config.getInt("enchant_values.minecraft:sweeping_edge.item"));
    }
    @Test void bareLegacyAliasSurvivesImplicitNamespacedDefaults() {
        var config = ConfigHolder.DEFAULT_CONFIG.getConfig();
        config.set("enchant_limits.minecraft:sweeping", null);
        config.set("enchant_limits.minecraft:sweeping_edge", null);
        config.set("enchant_limits.sweeping_edge", null);
        config.set("enchant_limits.sweeping", 8);
        config.set("enchant_values.minecraft:sweeping", null);
        config.set("enchant_values.minecraft:sweeping_edge", null);
        config.set("enchant_values.sweeping_edge", null);
        config.set("enchant_values.sweeping.item", 4);
        config.set("enchant_values.sweeping.book", 2);
        var update = new UpdateHandler.UpdatedConfigList();
        Update_1_20_5.updateName(update);
        update.cleanup(null);
        config = ConfigHolder.DEFAULT_CONFIG.getConfig();
        assertEquals(8, config.getInt("enchant_limits.minecraft:sweeping_edge"));
        assertEquals(4, config.getInt("enchant_values.minecraft:sweeping_edge.item"));
        assertEquals(2, config.getInt("enchant_values.minecraft:sweeping_edge.book"));
    }

}
