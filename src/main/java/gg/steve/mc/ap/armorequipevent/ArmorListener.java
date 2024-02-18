package gg.steve.mc.ap.armorequipevent;

import gg.steve.mc.ap.ArmorPlus;
import gg.steve.mc.ap.managers.ConfigManager;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.inventory.*;
import org.bukkit.event.player.PlayerDropItemEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerItemBreakEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;

import java.util.EnumMap;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

/**
 * @author Arnah
 * @since Jul 30, 2015
 */
public class ArmorListener implements Listener {

    private final List<String> blockedMaterials;
    private final EnumMap<Material, ArmorType> armorTypeByMaterialMap = new EnumMap<>(Material.class);

    public ArmorListener(List<String> blockedMaterials) {
        this.blockedMaterials = blockedMaterials;
        for (Material material : Material.values()) {
            for (ArmorType armorType : ArmorType.values()) {
                if (armorType.getPattern().matcher(material.name()).matches()) this.armorTypeByMaterialMap.put(material, armorType);
            }
        }
    }
    //Event Priority is highest because other plugins might cancel the events before we check.


    // Ibramsou Start - Fix duplication
    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public final void inventoryClick(final InventoryClickEvent e) {
        if (!(e.getWhoClicked() instanceof Player player)) {
            return;
        }
        final ClickType click = e.getClick();
        if (click == ClickType.DROP) return;
        if (e.getClickedInventory() == null || e.getClickedInventory().getType() != InventoryType.PLAYER) return;
        final int slot = e.getSlot();
        final InventoryType.SlotType slotType = e.getSlotType();
        if (slotType != InventoryType.SlotType.ARMOR && slotType != InventoryType.SlotType.QUICKBAR && slotType != InventoryType.SlotType.CONTAINER)
            return;
        final boolean shift = click.isShiftClick();
        final boolean fillEmptyShift = shift && slotType != InventoryType.SlotType.ARMOR;
        final PlayerInventory inventory = player.getInventory();
        final ItemStack oldCursor = e.getCursor();
        final ItemStack oldCurrent = e.getCurrentItem();
        ArmorType armorType = null;
        if (fillEmptyShift) {
            if (isAirOrNull(oldCurrent)) return;
            ArmorType type = this.armorTypeByMaterialMap.get(oldCurrent.getType());
            if (isAirOrNull(inventory.getItem(type.getInventorySlot()))) {
                armorType = type;
            }
        } else {
            armorType = ArmorType.matchTypeBySlot(slot);
        }
        if (armorType == null) return;
        final ArmorType newArmorType = armorType;
        final boolean numberkey = click.equals(ClickType.NUMBER_KEY);
        final InventoryAction action = e.getAction();
        final int hotbarButton = e.getHotbarButton();
        if (action == InventoryAction.NOTHING) {
            if (e.getCursor() == null || !ConfigManager.CONFIG.get().getStringList("head-items").contains(e.getInventory().getType().toString().toLowerCase()))
                return;// Why does this get called if nothing happens??
        }

        final ArmorType oldArmorType = ArmorType.matchTypeByItem(oldCursor);
        final ItemStack clickedItem = shift && !fillEmptyShift && e.getCurrentItem() != null ? e.getCurrentItem().clone() : null;
        final int firstEmptySlot;
        if (shift && !fillEmptyShift) {
            int emptySlot = -1;
            for (int i = 9; i < 35; i++) {
                if (isAirOrNull(player.getInventory().getItem(i))) {
                    emptySlot = i;
                    break;
                }
            }
            if (emptySlot == -1) {
                for (int i = 0; i < 8; i++) {
                    if (isAirOrNull(player.getInventory().getItem(i))) {
                        emptySlot = i;
                        break;
                    }
                }
            }

            firstEmptySlot = emptySlot;
        } else {
            firstEmptySlot = -1;
        }
        Bukkit.getScheduler().scheduleSyncDelayedTask(ArmorPlus.get(), () -> {
            final PlayerInventory clickedInventory = player.getInventory();
            ItemStack newArmorPiece = newArmorType.getArmorPiece(clickedInventory);
            ItemStack oldArmorPiece = fillEmptyShift ? null : numberkey ? clickedInventory.getItem(hotbarButton) : shift ? clickedItem : player.getItemOnCursor();
            ArmorEquipEvent.EquipMethod method = ArmorEquipEvent.EquipMethod.PICK_DROP;
            if (action.equals(InventoryAction.HOTBAR_SWAP) || numberkey) {
                method = ArmorEquipEvent.EquipMethod.HOTBAR_SWAP;
            } else if (shift) {
                method = ArmorEquipEvent.EquipMethod.SHIFT_CLICK;
            }
            ArmorEquipEvent armorEquipEvent = new ArmorEquipEvent(player, method, newArmorType, oldArmorPiece, newArmorPiece);
            Bukkit.getServer().getPluginManager().callEvent(armorEquipEvent);
            if (armorEquipEvent.isCancelled()) {
                if (oldArmorType != null && oldArmorType != newArmorType && !fillEmptyShift) {
                    player.setItemOnCursor(oldCursor);
                    clickedInventory.setItem(slot, oldCurrent);
                    return;
                }
                if (fillEmptyShift) {
                    clickedInventory.setItem(slot, newArmorPiece);
                    clickedInventory.setItem(newArmorType.getInventorySlot(), null);
                } else {
                    clickedInventory.setItem(slot, oldArmorPiece);
                    if (firstEmptySlot != -1) {
                        clickedInventory.setItem(firstEmptySlot, null);
                    } else if (numberkey) {
                        clickedInventory.setItem(hotbarButton, newArmorPiece);
                    } else {
                        player.setItemOnCursor(newArmorPiece);
                    }
                }
            }
        });

    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onDrop(PlayerDropItemEvent event) {
        final ItemStack oldArmor = event.getItemDrop().getItemStack();
        final ArmorType type = ArmorType.matchTypeByItem(oldArmor);
        ArmorEquipEvent armorEquipEvent = new ArmorEquipEvent(event.getPlayer(), ArmorEquipEvent.EquipMethod.PICK_DROP, type, oldArmor, null);
        Bukkit.getServer().getPluginManager().callEvent(armorEquipEvent);
        if (armorEquipEvent.isCancelled()) {
            event.getItemDrop().remove();
            type.setItemStack(event.getPlayer().getInventory(), oldArmor);
        }
    }
    // Ibramsou end

    @EventHandler(priority = EventPriority.HIGHEST)
    public void playerInteractEvent(PlayerInteractEvent e) {
        if (e.useItemInHand().equals(Event.Result.DENY)) return;
        //
        final Action action = e.getAction();
        if (action == Action.PHYSICAL) return;
        if (action == Action.RIGHT_CLICK_AIR || action == Action.RIGHT_CLICK_BLOCK) {
            Player player = e.getPlayer();
            if (!e.useInteractedBlock().equals(Event.Result.DENY)) {
                if (e.getClickedBlock() != null && action == Action.RIGHT_CLICK_BLOCK && !player.isSneaking()) {// Having both of these checks is useless, might as well do it though.
                    // Some blocks have actions when you right click them which stops the client from equipping the armor in hand.
                    Material mat = e.getClickedBlock().getType();
                    for (String s : blockedMaterials) {
                        if (mat.name().equalsIgnoreCase(s)) return;
                    }
                }
            }
            ArmorType newArmorType = ArmorType.matchTypeByItem(e.getItem());
            if (newArmorType != null) {
                if (isHeadItem(e.getItem())) return;
                if (newArmorType.equals(ArmorType.HELMET) && isAirOrNull(e.getPlayer().getInventory().getHelmet()) || newArmorType.equals(ArmorType.CHESTPLATE) && isAirOrNull(e.getPlayer().getInventory().getChestplate()) || newArmorType.equals(ArmorType.LEGGINGS) && isAirOrNull(e.getPlayer().getInventory().getLeggings()) || newArmorType.equals(ArmorType.BOOTS) && isAirOrNull(e.getPlayer().getInventory().getBoots())) {
                    ArmorEquipEvent armorEquipEvent = new ArmorEquipEvent(e.getPlayer(), ArmorEquipEvent.EquipMethod.HOTBAR, ArmorType.matchTypeByItem(e.getItem()), null, e.getItem());
                    Bukkit.getServer().getPluginManager().callEvent(armorEquipEvent);
                    if (armorEquipEvent.isCancelled()) {
                        e.setCancelled(true);
                        player.updateInventory();
                    }
                }
            }
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void inventoryDrag(InventoryDragEvent event) {
        // getType() seems to always be even.
        // Old Cursor gives the item you are equipping
        // Raw slot is the ArmorType slot
        // Can't replace armor using this method making getCursor() useless.
        ArmorType type = ArmorType.matchTypeByItem(event.getOldCursor());
        if (event.getRawSlots().isEmpty()) return;// Idk if this will ever happen
        if (type != null && type.getSlot() == event.getRawSlots().stream().findFirst().orElse(0)) {
            ArmorEquipEvent armorEquipEvent = new ArmorEquipEvent((Player) event.getWhoClicked(), ArmorEquipEvent.EquipMethod.DRAG, type, null, event.getOldCursor());
            Bukkit.getServer().getPluginManager().callEvent(armorEquipEvent);
            if (armorEquipEvent.isCancelled()) {
                event.setResult(Event.Result.DENY);
                event.setCancelled(true);
            }
        }
    }

    @EventHandler
    public void itemBreakEvent(PlayerItemBreakEvent e) {
        ArmorType type = ArmorType.matchTypeByItem(e.getBrokenItem());
        if (type != null) {
            Player p = e.getPlayer();
            ArmorEquipEvent armorEquipEvent = new ArmorEquipEvent(p, ArmorEquipEvent.EquipMethod.BROKE, type, e.getBrokenItem(), null);
            Bukkit.getServer().getPluginManager().callEvent(armorEquipEvent);
            if (armorEquipEvent.isCancelled()) {
                ItemStack i = e.getBrokenItem().clone();
                i.setAmount(1);
                i.setDurability((short) (i.getDurability() - 1));
                if (type.equals(ArmorType.HELMET)) {
                    p.getInventory().setHelmet(i);
                } else if (type.equals(ArmorType.CHESTPLATE)) {
                    p.getInventory().setChestplate(i);
                } else if (type.equals(ArmorType.LEGGINGS)) {
                    p.getInventory().setLeggings(i);
                } else if (type.equals(ArmorType.BOOTS)) {
                    p.getInventory().setBoots(i);
                }
            }
        }
    }

    @EventHandler
    public void playerDeathEvent(PlayerDeathEvent e) {
        Player p = e.getEntity();
        if (e.getKeepInventory()) return;
        for (ItemStack i : p.getInventory().getArmorContents()) {
            if (!isAirOrNull(i)) {
                Bukkit.getServer().getPluginManager().callEvent(new ArmorEquipEvent(p, ArmorEquipEvent.EquipMethod.DEATH, ArmorType.matchTypeByItem(i), i, null));
                // No way to cancel a death event.
            }
        }
    }

    /**
     * A utility method to support versions that use null or air ItemStacks.
     */
    public static boolean isAirOrNull(ItemStack item) {
        return item == null || item.getType().equals(Material.AIR);
    }

    public static boolean isHeadItem(ItemStack item) {
        if (item == null) return false;
        String type = item.getType().name();
        if (type.endsWith("SKULL_ITEM") || type.endsWith("_SKULL") || type.endsWith("PLAYER_HEAD") || ConfigManager.CONFIG.get().getStringList("head-items").contains(type.toLowerCase()))
            return true;
        return false;
    }
}