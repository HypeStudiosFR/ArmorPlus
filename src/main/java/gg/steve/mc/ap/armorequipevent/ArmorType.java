package gg.steve.mc.ap.armorequipevent;

import gg.steve.mc.ap.managers.ConfigManager;
import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;

import java.util.EnumMap;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.regex.Pattern;

/**
 * @author Arnah
 * @since Jul 30, 2015
 */
public enum ArmorType {
    HELMET(5, 39, Pattern.compile("^[A-Z]+_HELMET$"), PlayerInventory::setHelmet, PlayerInventory::getHelmet),
    CHESTPLATE(6, 38, Pattern.compile("^[A-Z]+_CHESTPLATE$"), PlayerInventory::setChestplate, PlayerInventory::getChestplate),
    LEGGINGS(7, 37, Pattern.compile("^[A-Z]+_LEGGINGS$"), PlayerInventory::setLeggings, PlayerInventory::getLeggings),
    BOOTS(8, 36, Pattern.compile("^[A-Z]+_BOOTS$"), PlayerInventory::setBoots, PlayerInventory::getBoots);

    private final int slot;
    private final BiConsumer<PlayerInventory, ItemStack> applyItemConsumer;
    private final Function<PlayerInventory, ItemStack> toArmorItem;
    private final int inventorySlot;
    private final Pattern pattern;

    ArmorType(int slot, int inventorySlot, Pattern pattern, BiConsumer<PlayerInventory, ItemStack> applyItemConsumer, Function<PlayerInventory, ItemStack> toArmorItem) {
        this.slot = slot;
        this.inventorySlot = inventorySlot;
        this.pattern = pattern;
        this.applyItemConsumer = applyItemConsumer;
        this.toArmorItem = toArmorItem;
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

    public ItemStack getArmorPiece(PlayerInventory inventory) {
        return this.toArmorItem.apply(inventory);
    }

    public int getSlot() {
        return slot;
    }

    public Pattern getPattern() {
        return pattern;
    }

    public int getInventorySlot() {
        return inventorySlot;
    }
}