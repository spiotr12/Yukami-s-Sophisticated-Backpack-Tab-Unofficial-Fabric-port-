package com.yukami.backpacktab.mixin;

import net.p3pp3rf1y.sophisticatedbackpacks.common.gui.BackpackContext;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

/** Exposes the slot (inside the parent backpack) of an opened inner backpack. */
@Mixin(BackpackContext.ItemSubBackpack.class)
public interface BackpackContextSubAccessor {
    @Accessor("subBackpackSlotIndex")
    int yukamibackpacktab$getSubBackpackSlotIndex();
}
