package xyz.alexcrea.cuanvil.anvil;

import io.delilaheve.util.ItemUtil;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class LegacyMergeCompatibilityTests {
    private final NamespacedKey first = new NamespacedKey("ecoitems", "first");
    private final NamespacedKey second = new NamespacedKey("ecoitems", "second");
    private boolean compatible(Material left, Material right) {
        return ItemUtil.INSTANCE.canMergeTypes(first, second, left, right);
    }
    @Test void differentEcoItemsWithSameBaseMaterialCanMerge() {
        assertNotEquals(first, second);
        assertTrue(compatible(Material.DIAMOND_SWORD, Material.DIAMOND_SWORD));
        assertTrue(compatible(Material.DIAMOND_CHESTPLATE, Material.DIAMOND_CHESTPLATE));
    }
    @Test void differentBaseMaterialsRemainIncompatible() {
        assertFalse(compatible(Material.DIAMOND_SWORD, Material.NETHERITE_SWORD));
        assertFalse(compatible(Material.DIAMOND_SWORD, Material.DIAMOND_AXE));
    }
    @Test void sameCustomIdentityStillMatches() {
        assertTrue(ItemUtil.INSTANCE.canMergeTypes(first, first, Material.DIAMOND_SWORD, Material.DIAMOND_SWORD));
    }
    @Test void enchantedBookStillMatchesRegardlessOfCustomIdentity() {
        assertTrue(compatible(Material.DIAMOND_SWORD, Material.ENCHANTED_BOOK));
        assertFalse(compatible(Material.ENCHANTED_BOOK, Material.DIAMOND_SWORD));
    }
    @Test void customAndVanillaWithSameMaterialMatchLegacyBinary() {
        assertTrue(ItemUtil.INSTANCE.canMergeTypes(first, Material.DIAMOND_SWORD.getKey(),
                Material.DIAMOND_SWORD, Material.DIAMOND_SWORD));
    }
    @Test void vanillaItemsKeepTheirCompatibility() {
        assertTrue(ItemUtil.INSTANCE.canMergeTypes(Material.DIAMOND_SWORD.getKey(), Material.DIAMOND_SWORD.getKey(),
                Material.DIAMOND_SWORD, Material.DIAMOND_SWORD));
    }
}
