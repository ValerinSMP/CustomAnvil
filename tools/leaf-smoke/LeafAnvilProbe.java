package valerin.anvilprobe;

import com.mojang.authlib.GameProfile;
import net.minecraft.network.Connection;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.PacketFlow;
import net.minecraft.server.level.ClientInformation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.network.CommonListenerCookie;
import net.minecraft.server.network.ServerGamePacketListenerImpl;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.craftbukkit.CraftServer;
import org.bukkit.craftbukkit.CraftWorld;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.PrepareAnvilEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.Damageable;
import org.bukkit.inventory.view.AnvilView;
import org.bukkit.plugin.java.JavaPlugin;
import xyz.alexcrea.cuanvil.config.ConfigHolder;
import xyz.alexcrea.cuanvil.dependency.DependencyManager;
import xyz.alexcrea.cuanvil.dependency.plugins.EcoItemDependencyUtil;
import xyz.alexcrea.cuanvil.enchant.CAEnchantmentRegistry;
import xyz.alexcrea.cuanvil.enchant.wrapped.CAEcoEnchant;
import xyz.alexcrea.cuanvil.listener.PrepareAnvilListener;
import xyz.alexcrea.cuanvil.update.UpdateUtils;
import xyz.alexcrea.cuanvil.api.event.listener.CATreatAnvilResultEvent;
import java.nio.file.Files;
import java.nio.file.Path;
import java.net.InetSocketAddress;
import java.util.UUID;

public final class LeafAnvilProbe extends JavaPlugin implements Listener {
    private int callbacks;
    private int packets;
    private int callbackMode;
    private int callbackPenalty;
    private int forcedCost = 600;
    private boolean cancelClick;
    @EventHandler(priority = org.bukkit.event.EventPriority.LOWEST)
    public void cancelFixtureClick(org.bukkit.event.inventory.InventoryClickEvent event) {
        if (cancelClick) event.setCancelled(true);
    }
    @EventHandler public void onResult(CATreatAnvilResultEvent event) {
        callbacks++;
        if (event.getResult() != null) {
            callbackPenalty = ((org.bukkit.inventory.meta.Repairable) event.getResult().getItemMeta()).getRepairCost();
            if (callbackMode == 1) {
                var replacement = event.getResult().clone();
                var meta = replacement.getItemMeta();
                ((org.bukkit.inventory.meta.Repairable) meta).setRepairCost(511);
                meta.getPersistentDataContainer().set(new NamespacedKey("fixture", "callback"),
                        org.bukkit.persistence.PersistentDataType.STRING, "kept");
                replacement.setItemMeta(meta);
                event.setResult(replacement);
            } else if (callbackMode == 2) event.getCost().setGeneric(forcedCost);
            else if (callbackMode == 3) event.setResult(null);
        }
    }

    @Override public void onEnable() {
        Bukkit.getPluginManager().registerEvents(this, this);
        Bukkit.getScheduler().runTaskLater(this, this::check, 40);
    }
    private void require(boolean ok, String check) {
        if (!ok) throw new AssertionError(check);
        getLogger().info("PASS " + check);
    }
    private void check() {
        try {
            require(Bukkit.getPluginManager().isPluginEnabled("CustomAnvil"), "plugin-enabled");
            getLogger().info("VERSION Minecraft=" + Bukkit.getMinecraftVersion()
                    + " Bukkit=" + Bukkit.getBukkitVersion());
            require(UpdateUtils.currentMinecraftVersion().toString().equals("26.2.0"), "native-version-26.2");
            require(DependencyManager.INSTANCE.getPacketManager().getClass().getSimpleName().equals("PaperPacketManager"),
                    "native-packet-manager-no-ProtocolLib");
            require(!DependencyManager.INSTANCE.getExternGuiTester().isInTest(), "native-GUI-detector");

            var config = ConfigHolder.DEFAULT_CONFIG.getConfig();
            require(config.getInt("limit_repair_value") == 250, "legacy-limit-preserved");
            require(config.getInt("item_rename_cost") == 1, "legacy-rename-cost-preserved");
            require(!config.getBoolean("allow_color_code"), "legacy-colour-toggle-preserved");
            require(config.getString("valerin_fixture").equals("preserve"), "unknown-config-key-preserved");

            var server = ((CraftServer) Bukkit.getServer()).getServer();
            var world = (CraftWorld) Bukkit.getWorlds().getFirst();
            var profile = new GameProfile(UUID.randomUUID(), "AnvilProbe");
            var handle = new ServerPlayer(server, world.getHandle(), profile, ClientInformation.createDefault());
            var connection = new Connection(PacketFlow.SERVERBOUND);
            connection.address = new InetSocketAddress("127.0.0.1", 0);
            // Capture packets without a network client or world/player persistence.
            handle.connection = new ServerGamePacketListenerImpl(server, connection, handle,
                    CommonListenerCookie.createInitial(profile, false)) {
                @Override public void send(Packet<?> packet) { packets++; }
            };
            Player player = handle.getBukkitEntity();
            player.addAttachment(this, "ca.affected", true);
            player.setLevel(100);
            DependencyManager.INSTANCE.getPacketManager().setInstantBuild(player, true);
            DependencyManager.INSTANCE.getPacketManager().setInstantBuild(player, false);
            require(packets == 2, "native-abilities-packets");
            AnvilView view = (AnvilView) player.openAnvil(world.getSpawnLocation(), true);
            require(view != null, "native-anvil-view");
            require(!DependencyManager.INSTANCE.getExternGuiTester().testIfGui(view), "canonical-anvil-accepted");
            var listener = new PrepareAnvilListener();

            var left = new ItemStack(Material.DIAMOND_SWORD);
            left.addUnsafeEnchantment(Enchantment.SHARPNESS, 4);
            var right = left.clone();
            var fused = listener.getResult(view, player, left, right);
            require(!fused.isEmpty() && fused.getItem().getEnchantmentLevel(Enchantment.SHARPNESS) == 5,
                    "sharpness-IV-fusion-to-V");
            require(left.getEnchantmentLevel(Enchantment.SHARPNESS) == 4, "fusion-input-preserved");
            view.getTopInventory().setItem(0, left);
            view.getTopInventory().setItem(1, right);
            var event = new PrepareAnvilEvent(view, null);
            Bukkit.getPluginManager().callEvent(event);
            require(event.getResult() != null && event.getResult().getEnchantmentLevel(Enchantment.SHARPNESS) == 5,
                    "prepare-event-result");
            require(callbacks > 0, "result-callback-retained");
            require(view.getRepairCost() > 0, "native-repair-cost");

            var damaged = new ItemStack(Material.DIAMOND_SWORD);
            var meta = (Damageable) damaged.getItemMeta();
            meta.setDamage(700);
            damaged.setItemMeta(meta);
            var repaired = listener.getResult(view, player, damaged, new ItemStack(Material.DIAMOND, 4));
            require(!repaired.isEmpty() && ((Damageable) repaired.getItem().getItemMeta()).getDamage() < 700,
                    "material-repair");
            var forbiddenBook = new ItemStack(Material.ENCHANTED_BOOK);
            var bookMeta = (org.bukkit.inventory.meta.EnchantmentStorageMeta) forbiddenBook.getItemMeta();
            bookMeta.addStoredEnchant(Enchantment.PROTECTION, 4, true);
            forbiddenBook.setItemMeta(bookMeta);
            var restricted = listener.getResult(view, player, new ItemStack(Material.DIAMOND_SWORD), forbiddenBook);
            require(restricted.isEmpty() || restricted.getItem().getEnchantmentLevel(Enchantment.PROTECTION) == 0,
                    "armour-enchant-restriction");

            var renameInput = new ItemStack(Material.DIAMOND_SWORD);
            view.getTopInventory().clear();
            view.getTopInventory().setItem(0, renameInput);
            var menu = (net.minecraft.world.inventory.AnvilMenu)
                    ((org.bukkit.craftbukkit.inventory.view.CraftAnvilView) view).getHandle();
            menu.setItemName("&aValerin");
            var plain = listener.getResult(view, player, renameInput, null);
            require(!plain.isEmpty() && net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer
                    .plainText().serialize(plain.getItem().getItemMeta().customName()).equals("&aValerin"),
                    "native-rename-with-colour-disabled");
            config.set("allow_color_code", true);
            player.addAttachment(this, "ca.color.code", true);
            var coloured = listener.getResult(view, player, renameInput, null);
            require(!coloured.isEmpty() && net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer
                    .legacySection().serialize(coloured.getItem().getItemMeta().customName()).contains("§a"),
                    "native-coloured-rename");
            require(!renameInput.getItemMeta().hasCustomName(), "rename-input-preserved");
            config.set("allow_color_code", false);

            var loreInput = new ItemStack(Material.DIAMOND_SWORD);
            var loreMeta = loreInput.getItemMeta();
            loreMeta.lore(java.util.List.of(net.kyori.adventure.text.Component.text("Existing")));
            loreInput.setItemMeta(loreMeta);
            var book = (org.bukkit.inventory.meta.BookMeta)
                    new ItemStack(Material.WRITABLE_BOOK).getItemMeta();
            book.setPages("&aValerin");
            player.addAttachment(this, "ca.lore_edit.book", true);
            var appended = xyz.alexcrea.cuanvil.util.anvil.AnvilLoreEditUtil.INSTANCE
                    .handleLoreAppendByBook(player, loreInput, book, new xyz.alexcrea.cuanvil.anvil.AnvilCost());
            require(appended != null && appended.getItemMeta().lore().size() == 2,
                    "native-book-lore-append");
            require(loreInput.getItemMeta().lore().size() == 1, "lore-input-preserved");
            var removed = xyz.alexcrea.cuanvil.util.anvil.AnvilLoreEditUtil.INSTANCE
                    .handleLoreRemoveByBook(player, appended, new xyz.alexcrea.cuanvil.anvil.AnvilCost());
            require(removed != null && !removed.getItemMeta().hasLore(), "native-book-lore-remove");

            checkExtraction(player, view);

            if (Boolean.getBoolean("valerin.probe.eco")) {
                require(Bukkit.getPluginManager().isPluginEnabled("eco"), "eco-enabled");
                require(Bukkit.getPluginManager().isPluginEnabled("EcoEnchants"), "EcoEnchants-enabled");
                require(Bukkit.getPluginManager().isPluginEnabled("EcoItems"), "EcoItems-enabled");
                int ecoCount = 0;
                for (var enchant : CAEnchantmentRegistry.getInstance().values()) {
                    if (enchant instanceof CAEcoEnchant eco) {
                        require(eco.fromKey() != null, "eco-enchant-lookup");
                        eco.isItemConflict(java.util.Map.of(), Material.DIAMOND_SWORD.getKey(),
                                new ItemStack(Material.DIAMOND_SWORD), new ItemStack(Material.DIAMOND_SWORD));
                        var vanilla = new xyz.alexcrea.cuanvil.enchant.wrapped.CABukkitEnchantment(
                                Enchantment.SHARPNESS, xyz.alexcrea.cuanvil.enchant.EnchantmentRarity.COMMON);
                        eco.isEnchantConflict(java.util.Map.of(vanilla, 1), Material.DIAMOND_SWORD.getKey());
                        ecoCount++;
                    }
                }
                require(ecoCount > 0, "eco-enchant-wrappers-registered:" + ecoCount);
                require(DependencyManager.INSTANCE.getHasEcoItem(), "eco-item-hook");
                var keys = EcoItemDependencyUtil.INSTANCE.getItems();
                require(!keys.isEmpty(), "eco-items-registered");
                for (NamespacedKey key : keys) {
                    var item = EcoItemDependencyUtil.INSTANCE.newEcoItemstack(key);
                    require(item != null, "eco-item-lookup:" + key);
                    require(item.getType() == EcoItemDependencyUtil.INSTANCE.ecoItemMaterialFromKey(key),
                            "eco-item-repair-material:" + key);
                    require(key.equals(EcoItemDependencyUtil.INSTANCE.ecoItemNamespace(item)),
                            "eco-item-namespace-roundtrip:" + key);
                }
                require(EcoItemDependencyUtil.INSTANCE.newEcoItemstack(
                        new NamespacedKey("valerin_fixture", keys.getFirst().getKey())) == null,
                        "foreign-namespace-rejected");
            }
            player.closeInventory();
            Files.writeString(Path.of("probe-result.txt"), "PASS\ncallbacks=" + callbacks + "\npackets=" + packets + "\n");
        } catch (Throwable failure) {
            getLogger().log(java.util.logging.Level.SEVERE, "PROBE FAILED", failure);
            try { Files.writeString(Path.of("probe-result.txt"), "FAIL\n" + failure); }
            catch (Exception ignored) {}
        }
    }
    private ItemStack penalty(ItemStack item, int value) {
        var meta = item.getItemMeta();
        ((org.bukkit.inventory.meta.Repairable) meta).setRepairCost(value);
        item.setItemMeta(meta);
        return item;
    }
    private boolean empty(ItemStack item) { return item == null || item.getType().isAir() || item.getAmount() == 0; }
    private int stored(ItemStack item) {
        return ((org.bukkit.inventory.meta.Repairable) item.getItemMeta()).getRepairCost();
    }
    private ItemStack prepare(Player player, AnvilView view, ItemStack left, ItemStack right, String name) {
        player.setItemOnCursor(null);
        view.getTopInventory().clear();
        view.setItem(0, left);
        view.setItem(1, right);
        var menu = (net.minecraft.world.inventory.AnvilMenu)
                ((org.bukkit.craftbukkit.inventory.view.CraftAnvilView) view).getHandle();
        menu.setItemName(name);
        var event = new PrepareAnvilEvent(view, null);
        Bukkit.getPluginManager().callEvent(event);
        view.setItem(2, event.getResult());
        return event.getResult();
    }
    private void click(AnvilView view, boolean shift) {
        var event = new org.bukkit.event.inventory.InventoryClickEvent(view,
                org.bukkit.event.inventory.InventoryType.SlotType.RESULT, 2,
                shift ? org.bukkit.event.inventory.ClickType.SHIFT_LEFT : org.bukkit.event.inventory.ClickType.LEFT,
                shift ? org.bukkit.event.inventory.InventoryAction.MOVE_TO_OTHER_INVENTORY
                      : org.bukkit.event.inventory.InventoryAction.PICKUP_ALL);
        Bukkit.getPluginManager().callEvent(event);
    }
    private void checkExtraction(Player player, AnvilView view) {
        var config = ConfigHolder.DEFAULT_CONFIG.getConfig();
        config.set("reset_result_repair_cost", true);
        config.set("reset_result_repair_cost_value", 0);
        player.setLevel(1000);
        var left = penalty(new ItemStack(Material.DIAMOND_SWORD), 127);
        left.addUnsafeEnchantment(Enchantment.SHARPNESS, 4);
        var right = new ItemStack(Material.DIAMOND_SWORD);
        right.addUnsafeEnchantment(Enchantment.SHARPNESS, 4);
        callbackMode = 1;
        var preview = prepare(player, view, left, right, "");
        require(preview != null && stored(preview) == 0, "preview-reset-after-callback");
        require(callbackPenalty > 0 && stored(left) == 127, "callback-sees-prior-penalty-input-preserved");
        int cost = view.getRepairCost();
        int level = player.getLevel();
        click(view, false);
        var received = player.getItemOnCursor();
        require(received.getType() == Material.DIAMOND_SWORD && stored(received) == 0,
                "merge-extracted-result-reset");
        require(received.getEnchantmentLevel(Enchantment.SHARPNESS) == 5,
                "merge-extracted-enchant-preserved");
        require("kept".equals(received.getItemMeta().getPersistentDataContainer().get(
                new NamespacedKey("fixture", "callback"), org.bukkit.persistence.PersistentDataType.STRING)),
                "callback-replacement-pdc-preserved");
        require(player.getLevel() == level - cost && cost > 0 && cost <= 250,
                "merge-xp-actually-charged");
        getLogger().info("MERGE slots left=" + view.getItem(0) + " right=" + view.getItem(1));
        require(empty(view.getItem(0)) && empty(view.getItem(1)), "merge-inputs-consumed");
        callbackMode = 0;
        for (int i = 0; i < 12; i++) {
            var next = received.clone();
            prepare(player, view, next, null, "Valerin cycle " + i);
            level = player.getLevel(); cost = view.getRepairCost();
            click(view, false);
            received = player.getItemOnCursor();
            require(received.getType() == Material.DIAMOND_SWORD && stored(received) == 0,
                    "repeated-work-reset:" + i);
            require(player.getLevel() == level - cost && cost > 0, "rename-xp-consumed:" + i);
        }
        for (int i = 0; i < 12; i++) {
            var next = received.clone();
            var damageMeta = (Damageable) next.getItemMeta(); damageMeta.setDamage(50); next.setItemMeta(damageMeta);
            var sacrifice = received.clone();
            prepare(player, view, next, sacrifice, "");
            level = player.getLevel(); cost = view.getRepairCost(); click(view, false);
            received = player.getItemOnCursor();
            require(received.getType() == Material.DIAMOND_SWORD && stored(received) == 0
                    && ((Damageable) received.getItemMeta()).getDamage() == 0, "repeated-fusion-reset:" + i);
            require(player.getLevel() == level - cost && empty(view.getItem(0)) && empty(view.getItem(1)),
                    "repeated-fusion-xp-and-material:" + i);
        }
        callbackMode = 2;
        for (int raw : new int[]{249, 250}) {
            forcedCost = raw - 1;
            player.setLevel(300);
            prepare(player, view, new ItemStack(Material.DIAMOND_SWORD), null, "Boundary " + raw);
            cost = view.getRepairCost(); click(view, false);
            require(cost == raw && player.getLevel() == 300 - cost,
                    "boundary-actual-xp:" + raw);
        }
        forcedCost = 600;
        player.setLevel(300);
        prepare(player, view, new ItemStack(Material.DIAMOND_SWORD), null, "Capped");
        require(view.getRepairCost() == 250 && view.getMaximumRepairCost() == 251, "cap-preview-250");
        click(view, false);
        require(player.getLevel() == 50 && stored(player.getItemOnCursor()) == 0, "cap-extraction-charges-250");
        player.setLevel(249);
        prepare(player, view, new ItemStack(Material.DIAMOND_SWORD), null, "Cannot afford");
        click(view, false);
        require(player.getItemOnCursor().getType() == Material.AIR && player.getLevel() == 249
                && !empty(view.getItem(0)), "insufficient-xp-no-consumption");
        callbackMode = 0;
        player.setLevel(1000);
        var damaged = penalty(new ItemStack(Material.DIAMOND_SWORD), 63);
        var meta = (Damageable) damaged.getItemMeta(); meta.setDamage(700); damaged.setItemMeta(meta);
        prepare(player, view, damaged, new ItemStack(Material.DIAMOND, 8), "");
        cost = view.getRepairCost(); level = player.getLevel();
        click(view, false);
        received = player.getItemOnCursor();
        require(received.getType() == Material.DIAMOND_SWORD && stored(received) == 0,
                "material-extraction-reset-closes-legacy-hole");
        require(((Damageable) received.getItemMeta()).getDamage() == 0, "material-extracted-damage-repaired");
        require(empty(view.getItem(0)) && view.getItem(1).getAmount() == 6,
                "material-extraction-consumes-two-diamonds");
        require(player.getLevel() == level - cost, "material-xp-consumed");
        callbackMode = 3;
        prepare(player, view, new ItemStack(Material.DIAMOND_SWORD), null, "Cancelled");
        level = player.getLevel(); click(view, false);
        require(player.getItemOnCursor().getType() == Material.AIR && player.getLevel() == level
                && !empty(view.getItem(0)), "null-callback-no-consumption");
        callbackMode = 0;
        prepare(player, view, new ItemStack(Material.DIAMOND_SWORD), null, "Event cancelled");
        cancelClick = true; level = player.getLevel(); click(view, false); cancelClick = false;
        require(player.getLevel() == level && !empty(view.getItem(0))
                && player.getItemOnCursor().getType() == Material.AIR, "cancelled-click-no-consumption");
        config.set("reset_result_repair_cost", false);
        prepare(player, view, penalty(new ItemStack(Material.DIAMOND_SWORD), 7), null, "Disabled");
        click(view, false);
        require(stored(player.getItemOnCursor()) == 7, "disabled-reset-preserves-penalty");
        config.set("reset_result_repair_cost", true);
        config.set("reset_result_repair_cost_value", 9);
        player.getInventory().clear();
        prepare(player, view, new ItemStack(Material.DIAMOND_SWORD), null, "Shift");
        click(view, true);
        ItemStack shifted = null;
        for (var item : player.getInventory().getStorageContents())
            if (item != null && item.getType() == Material.DIAMOND_SWORD) shifted = item;
        require(shifted != null && stored(shifted) == 9, "shift-extraction-configured-value");
        config.set("reset_result_repair_cost_value", 0);
        player.getInventory().clear();
        for (int i = 0; i < 36; i++) player.getInventory().setItem(i, new ItemStack(Material.STONE, 64));
        prepare(player, view, new ItemStack(Material.DIAMOND_SWORD), null, "Full inventory");
        level = player.getLevel(); click(view, true);
        require(!empty(view.getItem(0)) && player.getLevel() == level, "full-inventory-no-consumption");
        player.getInventory().clear();
        player.setGameMode(org.bukkit.GameMode.CREATIVE);
        player.setLevel(0);
        prepare(player, view, penalty(new ItemStack(Material.DIAMOND_SWORD), 127), null, "Creative");
        click(view, false);
        require(stored(player.getItemOnCursor()) == 0 && player.getLevel() == 0, "creative-reset-without-xp-charge");
        player.setGameMode(org.bukkit.GameMode.SURVIVAL);
        player.setLevel(1000);
        config.set("lore_edit.book_and_quil.append.enabled", true);
        player.addAttachment(this, "ca.lore_edit.book", true);
        var loreBook = new ItemStack(Material.WRITABLE_BOOK);
        var bookMeta = (org.bukkit.inventory.meta.BookMeta) loreBook.getItemMeta();
        bookMeta.setPages("Valerin lore"); loreBook.setItemMeta(bookMeta);
        prepare(player, view, penalty(new ItemStack(Material.DIAMOND_SWORD), 63), loreBook, "");
        cost = view.getRepairCost(); level = player.getLevel(); click(view, false);
        received = player.getItemOnCursor();
        require(received.getType() == Material.DIAMOND_SWORD && stored(received) == 0
                && received.getItemMeta().hasLore(), "lore-extraction-reset-and-content");
        require(player.getLevel() == level - cost && empty(view.getItem(0)), "lore-cost-and-input-consumed");
        var builder = new xyz.alexcrea.cuanvil.api.AnvilRecipeBuilder("valerin_reset_fixture")
                .setLeftItem(new ItemStack(Material.COBBLESTONE))
                .setRightItem(new ItemStack(Material.DIRT))
                .setResultItem(penalty(new ItemStack(Material.DIAMOND_SWORD), 127))
                .setLevelCostPerCraft(3);
        require(xyz.alexcrea.cuanvil.api.CustomAnvilRecipeApi.addRecipe(builder), "fixture-recipe-registered");
        prepare(player, view, new ItemStack(Material.COBBLESTONE), new ItemStack(Material.DIRT), "");
        level = player.getLevel(); click(view, false);
        received = player.getItemOnCursor();
        require(received.getType() == Material.DIAMOND_SWORD && stored(received) == 0,
                "custom-recipe-extraction-reset");
        require(player.getLevel() == level - 3 && empty(view.getItem(0)) && empty(view.getItem(1)),
                "custom-recipe-xp-and-inputs-consumed");
    }

}
