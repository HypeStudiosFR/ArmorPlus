package gg.steve.mc.ap.click;

import gg.steve.mc.ap.armorequipevent.ArmorEquipEvent;
import gg.steve.mc.ap.armorequipevent.ArmorType;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

public record ClickSlot(Player player, ArmorType resultArmorType, ItemStack oldItemStack, int resultSlot, int baseSlot, ArmorEquipEvent.EquipMethod method) {}
