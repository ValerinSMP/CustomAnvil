package xyz.alexcrea.cuanvil.anvil;

import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.Repairable;
import org.bukkit.persistence.PersistentDataType;
import org.junit.jupiter.api.Test;
import xyz.alexcrea.cuanvil.config.ConfigHolder;
import xyz.alexcrea.cuanvil.tests.SharedCustomAnvilTest;
import xyz.alexcrea.cuanvil.util.anvil.AnvilXpUtil;
import io.delilaheve.util.ConfigOptions;
import static org.junit.jupiter.api.Assertions.*;

class ValerinRepairCostTests extends SharedCustomAnvilTest {
    private ItemStack item(int penalty) {
        var item = new ItemStack(Material.DIAMOND_SWORD);
        var meta = item.getItemMeta();
        ((Repairable) meta).setRepairCost(penalty);
        item.setItemMeta(meta);
        return item;
    }
    @Test void disabledDoesNotChangeStoredPenalty() {
        ConfigHolder.DEFAULT_CONFIG.getConfig().set("reset_result_repair_cost", false);
        var result = item(127);
        var before = result.clone();
        AnvilXpUtil.INSTANCE.sanitizeResultRepairCost(result);
        assertEquals(before, result);
    }
    @Test void enabledOnlyChangesResultPenaltyAndPreservesInputAndPdc() {
        var config = ConfigHolder.DEFAULT_CONFIG.getConfig();
        config.set("reset_result_repair_cost", true);
        config.set("reset_result_repair_cost_value", 0);
        var input = item(127);
        var meta = input.getItemMeta();
        var key = new NamespacedKey("fixture", "repair_cost_merge");
        meta.getPersistentDataContainer().set(key, PersistentDataType.INTEGER, 31);
        input.setItemMeta(meta);
        var result = input.clone();
        AnvilXpUtil.INSTANCE.sanitizeResultRepairCost(result);
        assertEquals(0, ((Repairable) result.getItemMeta()).getRepairCost());
        assertEquals(127, ((Repairable) input.getItemMeta()).getRepairCost());
        assertEquals(31, result.getItemMeta().getPersistentDataContainer().get(key, PersistentDataType.INTEGER));
        var expected = input.clone();
        var expectedMeta = expected.getItemMeta();
        ((Repairable) expectedMeta).setRepairCost(0);
        expected.setItemMeta(expectedMeta);
        assertEquals(expected, result);
    }
    @Test void configuredValueAndInvalidFallbackAndNull() {
        var config = ConfigHolder.DEFAULT_CONFIG.getConfig();
        config.set("reset_result_repair_cost", true);
        for (int value : new int[]{0, 7, 1000, -1, 1001}) {
            config.set("reset_result_repair_cost_value", value);
            var result = item(63);
            AnvilXpUtil.INSTANCE.sanitizeResultRepairCost(result);
            assertEquals(value < 0 || value > 1000 ? 0 : value,
                    ((Repairable) result.getItemMeta()).getRepairCost());
        }
        assertDoesNotThrow(() -> AnvilXpUtil.INSTANCE.sanitizeResultRepairCost(null));
    }
    @Test void missingKeysKeepLegacyDefaultAndCapStillAppliesToOperation() {
        var config = ConfigHolder.DEFAULT_CONFIG.getConfig();
        config.set("reset_result_repair_cost", null);
        config.set("reset_result_repair_cost_value", null);
        assertFalse(ConfigOptions.INSTANCE.getDoResetResultRepairCost());
        assertEquals(0, ConfigOptions.INSTANCE.getResetResultRepairCostValue());
        config.set("limit_repair_cost", true);
        config.set("remove_repair_limit", false);
        config.set("limit_repair_value", 250);
        for (int raw : new int[]{249, 250, 600}) {
            assertEquals(Math.min(raw, 250), new AnvilCost(raw).filteredXpCost(false));
        }
    }
}
