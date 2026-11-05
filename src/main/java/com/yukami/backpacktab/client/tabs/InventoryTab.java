package com.yukami.backpacktab.client.tabs;

import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.multiplayer.MultiPlayerGameMode;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

public interface InventoryTab {
    ItemStack getTabIcon();

    Component getHoverText();

    void open(Player player, Level world, AbstractContainerMenu handler, MultiPlayerGameMode gameMode);

    boolean matchesCurrentScreen(AbstractContainerScreen<?> screen);

    boolean isActive();

    void setActive(boolean active);

    /** Value of {@link #getTabTint()} for tabs that keep the default look. */
    int NO_TINT = -1;

    /** @return an RGB color the tab background is tinted with, or {@link #NO_TINT}. */
    default int getTabTint() {
        return NO_TINT;
    }

    /**
     * @return whether the currently open container has to be closed before this tab is opened. Tabs that are opened
     * through the open container itself must not close it first.
     */
    default boolean requiresContainerClose() {
        return true;
    }
}
