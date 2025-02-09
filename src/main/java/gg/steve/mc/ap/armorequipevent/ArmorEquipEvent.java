package gg.steve.mc.ap.armorequipevent;

import gg.steve.mc.ap.ArmorPlus;
import gg.steve.mc.ap.managers.ConfigManager;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.event.Cancellable;
import org.bukkit.event.HandlerList;
import org.bukkit.event.player.PlayerEvent;
import org.bukkit.inventory.ItemStack;

import java.time.Duration;
import java.time.temporal.ChronoUnit;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * @author Arnah
 * @since Jul 30, 2015
 */
public final class ArmorEquipEvent extends PlayerEvent implements Cancellable {

    public static final Map<UUID, Long> EVENT_COOLDOWN_MAP = new HashMap<>();

    public static boolean hasCoolDown(ArmorEquipEvent event) {
        if (event.getNewArmorPiece() == null || event.getOldArmorPiece() == null) return false;
        if (!ConfigManager.CONFIG.get().getBoolean("click-cooldown.enabled")) {
            return false;
        }
        final long coolDownTime = ConfigManager.CONFIG.get().getLong("click-cooldown.time-in-millis");
        final String coolDownMessage = ConfigManager.CONFIG.get().getString("click-cooldown.message");
        final UUID uuid = event.player.getUniqueId();
        final long currentTimeStamp = System.currentTimeMillis();
        final Long lastTimeStamp = EVENT_COOLDOWN_MAP.remove(uuid);
        if (lastTimeStamp != null && currentTimeStamp - lastTimeStamp <= coolDownTime) {
            long timeLeft = Math.max(1, Duration.of(currentTimeStamp - lastTimeStamp, ChronoUnit.MILLIS).toSeconds());
            if (coolDownMessage != null) event.player.sendMessage(ChatColor.translateAlternateColorCodes('&', coolDownMessage.replace("%time%", String.valueOf(timeLeft))));
            EVENT_COOLDOWN_MAP.put(uuid, currentTimeStamp);
            event.setCancelled(true);
            return true;
        }


        Bukkit.getScheduler().scheduleSyncDelayedTask(ArmorPlus.get(), () -> EVENT_COOLDOWN_MAP.put(uuid, currentTimeStamp));
        return false;
    }

    private static final HandlerList handlers = new HandlerList();
    private boolean cancel = false;
    private final EquipMethod equipType;
    private final ArmorType type;
    private ItemStack oldArmorPiece, newArmorPiece;

    /**
     * @param player The player who put on / removed the armor.
     * @param type The ArmorType of the armor added
     * @param oldArmorPiece The ItemStack of the armor removed.
     * @param newArmorPiece The ItemStack of the armor added.
     */
    public ArmorEquipEvent(final Player player, final EquipMethod equipType, final ArmorType type, final ItemStack oldArmorPiece, final ItemStack newArmorPiece){
        super(player);
        this.equipType = equipType;
        this.type = type;
        this.oldArmorPiece = oldArmorPiece;
        this.newArmorPiece = newArmorPiece;
    }

    public static HandlerList getHandlerList() {
        return handlers;
    }
    /**
     * Gets a list of handlers handling this event.
     *
     * @return A list of handlers handling this event.
     */
    @Override
    public final HandlerList getHandlers(){
        return handlers;
    }

    /**
     * Sets if this event should be cancelled.
     *
     * @param cancel If this event should be cancelled.
     */
    public final void setCancelled(final boolean cancel){
        this.cancel = cancel;
    }

    /**
     * Gets if this event is cancelled.
     *
     * @return If this event is cancelled
     */
    public final boolean isCancelled(){
        return cancel;
    }

    public final ArmorType getType(){
        return type;
    }

    /**
     * Returns the last equipped armor piece, could be a piece of armor, or null
     */
    public final ItemStack getOldArmorPiece(){
        return oldArmorPiece;
    }

    public final void setOldArmorPiece(final ItemStack oldArmorPiece){
        this.oldArmorPiece = oldArmorPiece;
    }

    /**
     * Returns the newly equipped armor, could be a piece of armor, or null
     */
    public final ItemStack getNewArmorPiece(){
        return newArmorPiece;
    }

    public final void setNewArmorPiece(final ItemStack newArmorPiece){
        this.newArmorPiece = newArmorPiece;
    }

    /**
     * Gets the method used to either equip or unequip an armor piece.
     */
    public EquipMethod getMethod(){
        return equipType;
    }

    public enum EquipMethod{// These have got to be the worst documentations ever.
        /**
         * When you shift click an armor piece to equip or unequip
         */
        SHIFT_CLICK,
        /**
         * When you drag and drop the item to equip or unequip
         */
        DRAG,
        /**
         * When you manually equip or unequip the item. Use to be DRAG
         */
        PICK_DROP,
        /**
         * When you right click an armor piece in the hotbar without the inventory open to equip.
         */
        HOTBAR,
        /**
         * When you press the hotbar slot number while hovering over the armor slot to equip or unequip
         */
        HOTBAR_SWAP,
        /**
         * When in range of a dispenser that shoots an armor piece to equip.<br>
         * Requires the spigot version to have {@link org.bukkit.event.block.BlockDispenseArmorEvent} implemented.
         */
        DISPENSER,
        /**
         * When an armor piece is removed due to it losing all durability.
         */
        BROKE,
        /**
         * When you die causing all armor to unequip
         */
        DEATH,
        ;
    }
}
