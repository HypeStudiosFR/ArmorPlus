package gg.steve.mc.ap.armorequipevent;

import gg.steve.mc.ap.ArmorPlus;
import gg.steve.mc.ap.click.ClickArmorType;
import gg.steve.mc.ap.click.ClickSlot;
import gg.steve.mc.ap.click.ClickUtils;
import gg.steve.mc.ap.managers.ConfigManager;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Item;
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

import java.util.*;

/**
 * @author Arnah
 * @since Jul 30, 2015
 */
public class ArmorListener implements Listener {

    private final List<String> blockedMaterials;
    private final Map<ItemStack, ClickSlot> dropMap = new HashMap<>();

    public ArmorListener(List<String> blockedMaterials) {
        this.blockedMaterials = blockedMaterials;

    }
    //Event Priority is highest because other plugins might cancel the events before we check.



    // Ibramsou Start - Recode and fix duplication issues
    @EventHandler(priority = EventPriority.HIGHEST)
    public void onClick(InventoryClickEvent event) {
        if (event.getClickedInventory() == null || !(event.getClickedInventory() instanceof PlayerInventory playerInventory)) return;
        if (!(event.getWhoClicked() instanceof Player player)) return;
        final InventoryType.SlotType slotType = event.getSlotType();
        final ClickType type = event.getClick();
        final ClickArmorType clickArmorType;
        final boolean isHotBarKey = type.equals(ClickType.NUMBER_KEY);
        if (type.isShiftClick()) {
            clickArmorType = slotType == InventoryType.SlotType.ARMOR ? ClickArmorType.EMPTY_ARMOR_CONTENT : ClickArmorType.FILL_ARMOR_CONTENT;
        } else {
            clickArmorType = isHotBarKey ? ClickArmorType.HOT_BAR_KEY : ClickArmorType.CLICK_ARMOR_CONTENT;
        }
        final int resultSlot = clickArmorType.getResultSlot(event);
        final int baseSlot = clickArmorType.getBaseSlot(event);
        final ArmorType resultArmorType = ClickUtils.fromResultSlot(resultSlot);
        if (resultArmorType == null) return;
        final ItemStack resultItem = playerInventory.getItem(resultSlot);
        final ItemStack oldItemStack = resultItem == null ? null : resultItem.clone();

        final InventoryAction action = event.getAction();
        final ArmorEquipEvent.EquipMethod method;
        if (action.equals(InventoryAction.HOTBAR_SWAP) ||  isHotBarKey) {
            method = ArmorEquipEvent.EquipMethod.HOTBAR_SWAP;
        } else if (type.isShiftClick()) {
            method = ArmorEquipEvent.EquipMethod.SHIFT_CLICK;
        } else {
            method = ArmorEquipEvent.EquipMethod.PICK_DROP;
        }
        final ClickSlot clickSlot = new ClickSlot(player, resultArmorType, oldItemStack, resultSlot, baseSlot, method);
        if (type == ClickType.DROP || type == ClickType.CONTROL_DROP) {
            if (oldItemStack == null) return;
            this.dropMap.put(oldItemStack, clickSlot);
            return;
        }

        Bukkit.getScheduler().scheduleSyncDelayedTask(ArmorPlus.get(), () -> this.compareClickFromLastArmorContents(clickSlot, false));
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onDrop(PlayerDropItemEvent event) {
        final Item item = event.getItemDrop();
        final ClickSlot clickSlot = this.dropMap.remove(item.getItemStack());
        if (clickSlot == null) return;
        final boolean cancelled = event.isCancelled();
        if (cancelled) {
            event.setCancelled(false);
        }
        if (this.compareClickFromLastArmorContents(clickSlot, cancelled)) {
            item.remove();
        }
    }

    private boolean compareClickFromLastArmorContents(ClickSlot clickSlot, boolean cancelled) {
        final ArmorType resultArmorType = clickSlot.resultArmorType();
        final Player player = clickSlot.player();
        final ItemStack oldItemStack = clickSlot.oldItemStack();
        final ArmorEquipEvent.EquipMethod method = clickSlot.method();
        if (resultArmorType == null) return false;
        final PlayerInventory inventory = player.getInventory();
        final ItemStack resultItemStack = resultArmorType.getArmorPiece(inventory);
        if (resultItemStack != null && resultItemStack.equals(oldItemStack)) return false;
        if (cancelled) {
            this.cancel(clickSlot, resultItemStack);
            return true;
        }
        final ArmorEquipEvent event = new ArmorEquipEvent(player, method, resultArmorType, oldItemStack, resultItemStack);
        Bukkit.getPluginManager().callEvent(event);
        if (event.isCancelled()) {
            this.cancel(clickSlot, resultItemStack);
            return true;
        }

        return false;
    }

    private void cancel(ClickSlot clickSlot, ItemStack resultItemStack) {
        final int resultSlot = clickSlot.resultSlot();
        final int baseSlot = clickSlot.baseSlot();
        final ItemStack oldItemStack = clickSlot.oldItemStack();
        final Player player = clickSlot.player();
        final Inventory inventory = player.getInventory();
        if (resultSlot == baseSlot) {
            player.setItemOnCursor(resultItemStack);
            inventory.setItem(resultSlot, oldItemStack);
        } else {
            inventory.setItem(resultSlot, oldItemStack);
            inventory.setItem(baseSlot, resultItemStack);
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