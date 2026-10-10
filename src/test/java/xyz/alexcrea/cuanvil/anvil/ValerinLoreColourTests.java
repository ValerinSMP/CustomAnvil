package xyz.alexcrea.cuanvil.anvil;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.BookMeta;
import org.junit.jupiter.api.Test;
import xyz.alexcrea.cuanvil.config.ConfigHolder;
import xyz.alexcrea.cuanvil.tests.SharedCustomAnvilTest;
import xyz.alexcrea.cuanvil.util.anvil.AnvilColourUtil;
import xyz.alexcrea.cuanvil.util.anvil.AnvilLoreEditUtil;
import java.util.List;
import static org.junit.jupiter.api.Assertions.*;

class ValerinLoreColourTests extends SharedCustomAnvilTest {
    @Test void colourRequiresConfiguredPermissionsAndKeepsEscapedAmpersands() {
        var player = server.addPlayer();
        var denied = AnvilColourUtil.INSTANCE.calculatePermissions(player, true, true, false,
                false, AnvilColourUtil.ColourUseType.RENAME);
        assertNull(AnvilColourUtil.INSTANCE.handleColour("&aValerin", denied));
        player.addAttachment(plugin, "ca.color.code", true);
        var allowed = AnvilColourUtil.INSTANCE.calculatePermissions(player, true, true, false,
                false, AnvilColourUtil.ColourUseType.RENAME);
        var result = AnvilColourUtil.INSTANCE.handleColour("&aValerin && SMP", allowed);
        assertNotNull(result);
        assertEquals("Valerin & SMP", PlainTextComponentSerializer.plainText().serialize(result));
        assertTrue(net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer.legacySection().serialize(result).contains("§a"));
    }

    @Test void bookLoreAppendAndRemovalPreserveInputAndPermissionBoundary() {
        var player = server.addPlayer();
        var config = ConfigHolder.DEFAULT_CONFIG.getConfig();
        config.set("lore_edit.book_and_quil.use_permission", true);
        ItemStack original = new ItemStack(Material.DIAMOND_SWORD);
        var meta = original.getItemMeta();
        meta.lore(List.of(Component.text("Existing")));
        original.setItemMeta(meta);
        var book = (BookMeta) new ItemStack(Material.WRITABLE_BOOK).getItemMeta();
        book.setPages("&aValerin");
        assertNull(AnvilLoreEditUtil.INSTANCE.handleLoreAppendByBook(player, original, book,
                new AnvilCost()));
        player.addAttachment(plugin, "ca.lore_edit.book", true);
        var appended = AnvilLoreEditUtil.INSTANCE.handleLoreAppendByBook(player, original,
                book, new AnvilCost());
        assertNotNull(appended);
        assertEquals(2, appended.getItemMeta().lore().size());
        assertEquals(1, original.getItemMeta().lore().size());
        var removed = AnvilLoreEditUtil.INSTANCE.handleLoreRemoveByBook(player, appended,
                new AnvilCost());
        assertNotNull(removed);
        assertFalse(removed.getItemMeta().hasLore());
        assertEquals(2, appended.getItemMeta().lore().size());
    }
}
