package com.yukami.backpacktab.client.tabs;

import com.yukami.backpacktab.client.gui.InceptionTabs;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.multiplayer.MultiPlayerGameMode;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.p3pp3rf1y.sophisticatedbackpacks.backpack.wrapper.BackpackWrapper;

import java.util.List;

/** Tab of a backpack stored inside another (open) backpack that has an inception upgrade. */
public final class SubBackpackTab implements InventoryTab {
    private final ItemStack backpackStack;
    private final ItemStack parentStack;
    private final List<Integer> path;
    private boolean active;
    private int tint = NO_TINT - 1;
    private Component cachedHoverText;

    /**
     * @param parentStack the backpack this one is stored in, its main color is used for the tab
     * @param path        slots to follow from the root backpack to this one
     */
    public SubBackpackTab(ItemStack backpackStack, ItemStack parentStack, List<Integer> path) {
        this.backpackStack = backpackStack.copy();
        this.parentStack = parentStack.copy();
        this.path = List.copyOf(path);
    }

    public List<Integer> getPath() {
        return path;
    }

    @Override
    public ItemStack getTabIcon() {
        return backpackStack;
    }

    @Override
    public Component getHoverText() {
        if (cachedHoverText == null) {
            cachedHoverText = backpackStack.getHoverName();
        }
        return cachedHoverText;
    }

    public ItemStack getParentStack() {
        return parentStack;
    }

    /** The tab takes the main color of the backpack it is stored in, which shows which tab it belongs to. */
    @Override
    public int getTabTint() {
        if (tint == NO_TINT - 1) {
            try {
                tint = BackpackWrapper.fromStack(parentStack).getMainColor() & 0xFFFFFF;
            } catch (Exception e) {
                tint = NO_TINT;
            }
        }
        return tint;
    }

    @Override
    public boolean requiresContainerClose() {
        // The parent backpack's menu has to stay open on the server, it is what opens the inner backpack.
        return false;
    }

    @Override
    public void open(Player player, Level world, AbstractContainerMenu handler, MultiPlayerGameMode gameMode) {
        InceptionTabs.open(path);
    }

    @Override
    public boolean matchesCurrentScreen(AbstractContainerScreen<?> screen) {
        return InceptionTabs.isCurrent(path);
    }

    @Override
    public boolean isActive() {
        return active;
    }

    @Override
    public void setActive(boolean active) {
        this.active = active;
    }
}
