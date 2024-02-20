package gg.steve.mc.ap.click;

import gg.steve.mc.ap.armorequipevent.ArmorType;
import org.bukkit.Material;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;

public class ClickUtils {

    public static ArmorType fromResultSlot(int slot) {
        return slot == ArmorType.HELMET.getInventorySlot() ? ArmorType.HELMET :
                slot == ArmorType.CHESTPLATE.getInventorySlot() ? ArmorType.CHESTPLATE :
                        slot == ArmorType.LEGGINGS.getInventorySlot() ? ArmorType.LEGGINGS :
                                slot == ArmorType.BOOTS.getInventorySlot() ? ArmorType.BOOTS : null;
    }

    public static int getFillingArmorSlot(InventoryClickEvent event) {
        final PlayerInventory inventory = getPlayerInventory(event);
        final ItemStack currentItem = event.getCurrentItem();
        if (isEmpty(currentItem)) return -1;
        if (isNotEmpty(inventory.getHelmet()) && isNotEmpty(inventory.getChestplate()) && isNotEmpty(inventory.getLeggings()) && isNotEmpty(inventory.getBoots()))
            return -1;
        final ArmorType armorType = ArmorType.TYPES_BY_MATERIALS.get(currentItem.getType());
        if (armorType == null || armorType.isInvalid(armorType.getArmorPiece(inventory))) return -1;
        return armorType.getInventorySlot();
    }

    public static int getFirstEmptySlot(InventoryClickEvent event) {
        final PlayerInventory inventory = getPlayerInventory(event);
        int emptySlot = -1;
        for (int i = 9; i < 35; i++) {
            if (isEmpty(inventory.getItem(i))) {
                emptySlot = i;
                break;
            }
        }
        if (emptySlot == -1) {
            for (int i = 0; i < 8; i++) {
                if (isEmpty(inventory.getItem(i))) {
                    emptySlot = i;
                    break;
                }
            }
        }

        return emptySlot;
    }

    private static PlayerInventory getPlayerInventory(InventoryClickEvent event) {
        if (!(event.getClickedInventory() instanceof PlayerInventory inventory)) throw new IllegalStateException("Clicked inventory should be a player inventory");
        return inventory;
    }

    public static boolean isNotEmpty(ItemStack itemStack) {
        return !isEmpty(itemStack);
    }

    public static boolean isEmpty(ItemStack itemStack) {
        return itemStack == null || itemStack.getType() == Material.AIR;
    }
}
