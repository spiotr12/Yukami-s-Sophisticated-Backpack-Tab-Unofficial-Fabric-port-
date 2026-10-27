package com.yukami.backpacktab.mixin;

import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(AbstractContainerScreen.class)
public interface AbstractContainerScreenAccessor {
    @Accessor("leftPos")
    int yukamibackpacktab$getLeftPos();

    @Accessor("topPos")
    int yukamibackpacktab$getTopPos();

    @Accessor("imageWidth")
    int yukamibackpacktab$getImageWidth();

    @Accessor("imageHeight")
    int yukamibackpacktab$getImageHeight();
}