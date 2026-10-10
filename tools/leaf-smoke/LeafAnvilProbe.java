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
    @EventHandler public void onResult(CATreatAnvilResultEvent event) { callbacks++; }

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
}
