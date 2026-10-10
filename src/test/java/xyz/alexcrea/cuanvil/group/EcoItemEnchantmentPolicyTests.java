package xyz.alexcrea.cuanvil.group;

import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.enchantments.Enchantment;
import org.junit.jupiter.api.Test;
import xyz.alexcrea.cuanvil.enchant.CAEnchantment;
import xyz.alexcrea.cuanvil.enchant.wrapped.CABukkitEnchantment;
import xyz.alexcrea.cuanvil.tests.SharedCustomAnvilTest;
import java.util.Map;
import static org.junit.jupiter.api.Assertions.*;

class EcoItemEnchantmentPolicyTests extends SharedCustomAnvilTest {
    private final NamespacedKey custom = new NamespacedKey("ecoitems", "valerin_fixture");
    private boolean allowed(AbstractMaterialGroup group, NamespacedKey custom, Material base, Enchantment enchant) {
        var wrapped = new CABukkitEnchantment(enchant);
        var rule = new EnchantConflictGroup("fixture", group, 0);
        rule.addEnchantment(wrapped);
        return rule.allowed(Map.of(wrapped, 1), group.enchantmentPolicyType(custom, base.getKey()));
    }
    @Test void customSwordReproducesOldRejectionAndInheritsSwordEligibility() {
        var swords = new IncludeGroup("swords").addToPolicy(Material.DIAMOND_SWORD.getKey());
        var enchant = new CABukkitEnchantment(Enchantment.SHARPNESS);
        var rule = new EnchantConflictGroup("sharpness", swords, 0);
        rule.addEnchantment(enchant);
        assertFalse(rule.allowed(Map.of(enchant, 1), custom));
        assertTrue(allowed(swords, custom, Material.DIAMOND_SWORD, Enchantment.SHARPNESS));
        assertEquals(custom, swords.enchantmentPolicyType(custom, null));
    }
    @Test void armourAndToolsRetainTheirMaterialRestrictions() {
        var armour = new IncludeGroup("armour").addToPolicy(Material.DIAMOND_CHESTPLATE.getKey());
        assertTrue(allowed(armour, custom, Material.DIAMOND_CHESTPLATE, Enchantment.PROTECTION));
        assertFalse(allowed(armour, custom, Material.DIAMOND_SWORD, Enchantment.PROTECTION));
        var tools = new IncludeGroup("tools").addToPolicy(Material.DIAMOND_PICKAXE.getKey());
        assertTrue(allowed(tools, custom, Material.DIAMOND_PICKAXE, Enchantment.EFFICIENCY));
        assertFalse(allowed(tools, custom, Material.STICK, Enchantment.EFFICIENCY));
    }
    @Test void explicitCustomInclusionWinsOverBaseRestriction() {
        var group = new IncludeGroup("custom_only").addToPolicy(custom);
        assertEquals(custom, group.enchantmentPolicyType(custom, Material.STICK.getKey()));
        assertTrue(allowed(group, custom, Material.STICK, Enchantment.SHARPNESS));
    }
    @Test void explicitCustomExclusionIsNeverBypassedByBaseMaterial() {
        var group = new ExcludeGroup("exclude_custom").addToPolicy(custom);
        assertEquals(custom, group.enchantmentPolicyType(custom, Material.DIAMOND_SWORD.getKey()));
        assertFalse(allowed(group, custom, Material.DIAMOND_SWORD, Enchantment.SHARPNESS));
    }
    @Test void baseMaterialExclusionStillRejectsCustomVersion() {
        var group = new ExcludeGroup("exclude_swords").addToPolicy(Material.DIAMOND_SWORD.getKey());
        assertFalse(allowed(group, custom, Material.DIAMOND_SWORD, Enchantment.SHARPNESS));
    }
    @Test void nestedCustomPoliciesRemainExplicit() {
        var nested = new IncludeGroup("inner").addToPolicy(custom);
        var group = new IncludeGroup("outer").addToPolicy(nested);
        assertEquals(custom, group.enchantmentPolicyType(custom, Material.STICK.getKey()));
        assertTrue(allowed(group, custom, Material.STICK, Enchantment.SHARPNESS));
    }
    @Test void vanillaAndUnrecognisedItemsDoNotChangeClassification() {
        var group = new IncludeGroup("swords").addToPolicy(Material.DIAMOND_SWORD.getKey());
        assertEquals(custom, group.enchantmentPolicyType(custom, null));
        assertEquals(Material.DIAMOND_SWORD.getKey(), group.enchantmentPolicyType(Material.DIAMOND_SWORD.getKey(), null));
        var player = server.addPlayer();
        player.setOp(false);
        assertFalse(player.hasPermission("ca.bypass.fuse"));
        assertFalse(player.hasPermission("ca.bypass.level"));
        assertTrue(allowed(group, custom, Material.DIAMOND_SWORD, Enchantment.SHARPNESS));
    }
}
