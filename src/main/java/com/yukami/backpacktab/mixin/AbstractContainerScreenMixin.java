package com.yukami.backpacktab.mixin;

import com.yukami.backpacktab.client.gui.TabManager;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(AbstractContainerScreen.class)
public abstract class AbstractContainerScreenMixin {
    @Inject(method = "render", at = @At("HEAD"))
    private void yukamibackpacktab$onRenderStart(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick, CallbackInfo ci) {
        TabManager.onScreenFrame((AbstractContainerScreen<?>) (Object) this, mouseX, mouseY);
    }

    @Inject(
            method = "render",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/client/gui/screens/inventory/AbstractContainerScreen;renderLabels(Lnet/minecraft/client/gui/GuiGraphics;II)V",
                    shift = At.Shift.AFTER
            )
    )
    private void yukamibackpacktab$afterRenderLabels(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick, CallbackInfo ci) {
        TabManager.onScreenRender((AbstractContainerScreen<?>) (Object) this, guiGraphics, mouseX, mouseY);
    }

    @Inject(method = "renderTooltip", at = @At("HEAD"), cancellable = true)
    private void yukamibackpacktab$suppressTooltip(GuiGraphics guiGraphics, int x, int y, CallbackInfo ci) {
        if (TabManager.shouldSuppressTooltip((AbstractContainerScreen<?>) (Object) this)) {
            ci.cancel();
        }
    }
}
