package gg.steve.mc.ap.armorequipevent;

import gg.steve.mc.ap.managers.ConfigManager;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;

import java.util.function.BiConsumer;

/**
 * @author Arnah
 * @since Jul 30, 2015
 */
public enum ArmorType {
    HELMET(5, PlayerInventory::setHelmet),
    CHESTPLATE(6, PlayerInventory::setChestplate),
    LEGGINGS(7, PlayerInventory::setLeggings),
    BOOTS(8, PlayerInventory::setBoots);

    private final int slot;
    private final BiConsumer<PlayerInventory, ItemStack> applyItemConsumer;

    ArmorType(int slot, BiConsumer<PlayerInventory, ItemStack> applyItemConsumer) {
        this.slot = slot;
        this.applyItemConsumer = applyItemConsumer;
    }

    public void setItemStack(PlayerInventory inventory, ItemStack itemStack) {
        this.applyItemConsumer.accept(inventory, itemStack);
    }

    /**
     * Attempts to match the ArmorType for the specified ItemStack.
     *
     * @param itemStack The ItemStack to parse the type of.
     * @return The parsed ArmorType, or null if not found.
     */
    public static ArmorType matchTypeByItem(final ItemStack itemStack) {
        if (itemStack == null) return null;
        String type = itemStack.getType().name();
        if (type.endsWith("_HELMET") || type.endsWith("SKULL_ITEM") || type.endsWith("_SKULL") || type.endsWith("PLAYER_HEAD") || ConfigManager.CONFIG.get().getStringList("head-items").contains(type.toLowerCase()))
            return HELMET;
        else if (type.endsWith("_CHESTPLATE") || type.endsWith("ELYTRA")) return CHESTPLATE;
        else if (type.endsWith("_LEGGINGS")) return LEGGINGS;
        else if (type.endsWith("_BOOTS")) return BOOTS;
        else return null;
    }

    public static ArmorType matchTypeBySlot(int slot) {
        return slot == 39 ? HELMET : slot == 38 ? CHESTPLATE : slot == 37 ? LEGGINGS : slot == 36 ? BOOTS : null;
    }

    public int getSlot() {
        return slot;
    }
}