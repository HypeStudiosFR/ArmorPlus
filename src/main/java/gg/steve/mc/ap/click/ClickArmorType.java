package gg.steve.mc.ap.click;

import org.bukkit.event.inventory.InventoryClickEvent;

import java.util.function.Function;

public enum ClickArmorType {
    EMPTY_ARMOR_CONTENT(InventoryClickEvent::getSlot, ClickUtils::getFirstEmptySlot),
    FILL_ARMOR_CONTENT(ClickUtils::getFillingArmorSlot),
    CLICK_ARMOR_CONTENT(InventoryClickEvent::getSlot),
    HOT_BAR_KEY(InventoryClickEvent::getSlot, InventoryClickEvent::getHotbarButton);

    private final Function<InventoryClickEvent, Integer> toResultSlot;
    private final Function<InventoryClickEvent, Integer> toBaseSlot;

    ClickArmorType(Function<InventoryClickEvent, Integer> toResultSlot) {
        this(toResultSlot, InventoryClickEvent::getSlot);
    }

    ClickArmorType(Function<InventoryClickEvent, Integer> toResultSlot, Function<InventoryClickEvent, Integer> toBaseSlot) {
        this.toResultSlot = toResultSlot;
        this.toBaseSlot = toBaseSlot;
    }

    public int getResultSlot(InventoryClickEvent event) {
        return this.toResultSlot.apply(event);
    }

    public int getBaseSlot(InventoryClickEvent event) {
        return this.toBaseSlot.apply(event);
    }
}
