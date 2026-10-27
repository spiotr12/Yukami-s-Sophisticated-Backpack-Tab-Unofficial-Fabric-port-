package com.yukami.backpacktab.client.util;

import com.yukami.backpacktab.mixin.AbstractContainerScreenAccessor;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;

public final class ScreenAccess {
    private ScreenAccess() {}

    public static int guiLeft(AbstractContainerScreen<?> screen) {
        return ((AbstractContainerScreenAccessor) screen).yukamibackpacktab$getLeftPos();
    }

    public static int guiTop(AbstractContainerScreen<?> screen) {
        return ((AbstractContainerScreenAccessor) screen).yukamibackpacktab$getTopPos();
    }

    public static int xSize(AbstractContainerScreen<?> screen) {
        return ((AbstractContainerScreenAccessor) screen).yukamibackpacktab$getImageWidth();
    }

    public static int ySize(AbstractContainerScreen<?> screen) {
        return ((AbstractContainerScreenAccessor) screen).yukamibackpacktab$getImageHeight();
    }
}