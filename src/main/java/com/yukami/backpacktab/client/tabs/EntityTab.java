package com.yukami.backpacktab.client.tabs;

import com.yukami.backpacktab.YukamiBackpackTab;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.minecraft.client.multiplayer.MultiPlayerGameMode;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.SpawnEggItem;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.Vec3;
import net.p3pp3rf1y.sophisticatedbackpacks.client.gui.BackpackScreen;

/** Tab that re-opens the screen of an entity (e.g. a merchant) by interacting with it again. */
public final class EntityTab implements InventoryTab {
    private final int entityId;
    private boolean active;
    private ItemStack cachedIcon;
    private Component cachedHoverText;

    public EntityTab(int entityId) {
        this.entityId = entityId;
    }

    private Entity resolve(Level level) {
        return level == null ? null : level.getEntity(entityId);
    }

    @Override
    public ItemStack getTabIcon() {
        if (cachedIcon == null) {
            Entity entity = resolve(Minecraft.getInstance().level);
            Item egg = entity == null ? null : SpawnEggItem.byId(entity.getType());
            cachedIcon = new ItemStack(egg != null ? egg : Items.EMERALD);
        }
        return cachedIcon;
    }

    @Override
    public Component getHoverText() {
        if (cachedHoverText == null) {
            Entity entity = resolve(Minecraft.getInstance().level);
            cachedHoverText = entity != null ? entity.getDisplayName() : Component.literal("Entity");
        }
        return cachedHoverText;
    }

    @Override
    public void open(Player player, Level world, AbstractContainerMenu handler, MultiPlayerGameMode gameMode) {
        if (!(player instanceof LocalPlayer localPlayer) || gameMode == null) return;
        try {
            Entity entity = resolve(world);
            if (entity == null || !entity.isAlive()) {
                Minecraft.getInstance().setScreen(null);
                return;
            }
            Vec3 hit = entity.position().add(0, entity.getBbHeight() / 2, 0);
            InteractionResult result = gameMode.interactAt(localPlayer, entity, new EntityHitResult(entity, hit), InteractionHand.MAIN_HAND);
            if (!result.consumesAction()) {
                gameMode.interact(localPlayer, entity, InteractionHand.MAIN_HAND);
            }
        } catch (Exception e) {
            YukamiBackpackTab.LOGGER.debug("Failed to open entity tab {}", entityId, e);
            Minecraft.getInstance().setScreen(null);
        }
    }

    @Override
    public boolean matchesCurrentScreen(AbstractContainerScreen<?> screen) {
        return !(screen instanceof BackpackScreen) && !(screen instanceof InventoryScreen);
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
