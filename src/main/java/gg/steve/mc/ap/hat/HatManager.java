package gg.steve.mc.ap.hat;

import gg.steve.mc.ap.armorequipevent.ArmorType;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class HatManager {

    private final Map<UUID, ItemStack> headItemMap = new HashMap<>();

    public void addHat(Player player, ArmorType type, ItemStack newArmorPiece) {
        if (type != ArmorType.HELMET) return;
        this.headItemMap.put(player.getUniqueId(), newArmorPiece);

    }

    public void removeHat(Player player, ArmorType type) {
        if (type != ArmorType.HELMET) return;
        this.headItemMap.remove(player.getUniqueId());
    }
}
