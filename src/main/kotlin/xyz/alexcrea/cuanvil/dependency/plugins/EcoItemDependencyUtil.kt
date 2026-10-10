package xyz.alexcrea.cuanvil.dependency.plugins

import com.willfp.ecoitems.items.EcoItem
import com.willfp.ecoitems.items.EcoItems
import com.willfp.ecoitems.items.ecoItem
import org.bukkit.Material
import org.bukkit.NamespacedKey
import org.bukkit.inventory.ItemStack

object EcoItemDependencyUtil {

    fun ecoItemNamespace(item: ItemStack): NamespacedKey? {
        val ecoi = item.ecoItem ?: return null

        return ecoi.id
    }

    fun ecoItemFromKey(key: NamespacedKey): EcoItem? {
        // EcoItems versions differ in whether the registry accepts a namespaced ID.
        // Validate the provider identity before accepting the bare-ID fallback.
        return EcoItems.getByID(key.toString())?.takeIf { it.id == key }
            ?: EcoItems.getByID(key.key)?.takeIf { it.id == key }
    }

    fun ecoItemMaterialFromKey(key: NamespacedKey): Material? {
        val ecoi = ecoItemFromKey(key) ?: return null

        return ecoi.itemStack.type
    }

    fun newEcoItemstack(key: NamespacedKey): ItemStack? {
        val ecoi = ecoItemFromKey(key) ?: return null

        return ecoi.itemStack
    }

    fun getItems(): List<NamespacedKey> {
        return EcoItems.values().map { item -> item.id }
    }

}