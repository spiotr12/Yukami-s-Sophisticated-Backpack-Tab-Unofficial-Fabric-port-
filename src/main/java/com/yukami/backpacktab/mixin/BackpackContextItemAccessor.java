package com.yukami.backpacktab.mixin;

import net.p3pp3rf1y.sophisticatedbackpacks.common.gui.BackpackContext;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

/** Exposes where an item backpack context points to (the fields are protected in Sophisticated Backpacks). */
@Mixin(BackpackContext.Item.class)
public interface BackpackContextItemAccessor {
    @Accessor("handlerName")
    String yukamibackpacktab$getHandlerName();

    @Accessor("identifier")
    String yukamibackpacktab$getIdentifier();
}
