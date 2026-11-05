package com.yukami.backpacktab.client.gui;

import net.minecraft.client.Minecraft;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.screen.v1.ScreenEvents;
import net.fabricmc.fabric.api.client.screen.v1.ScreenKeyboardEvents;
import net.fabricmc.fabric.api.client.screen.v1.ScreenMouseEvents;
import net.fabricmc.fabric.api.event.player.UseBlockCallback;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.minecraft.client.multiplayer.MultiPlayerGameMode;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.p3pp3rf1y.sophisticatedbackpacks.backpack.BackpackBlock;
import net.p3pp3rf1y.sophisticatedbackpacks.backpack.BackpackBlockEntity;
import net.p3pp3rf1y.sophisticatedbackpacks.client.gui.BackpackScreen;
import net.p3pp3rf1y.sophisticatedbackpacks.common.gui.BackpackContainer;
import net.p3pp3rf1y.sophisticatedbackpacks.util.PlayerInventoryProvider;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import com.yukami.backpacktab.YukamiBackpackTab;
import com.yukami.backpacktab.client.tabs.BackpackTab;
import com.yukami.backpacktab.client.tabs.ContainerTab;
import com.yukami.backpacktab.client.tabs.InventoryTab;
import com.yukami.backpacktab.client.tabs.PlayerTab;
import com.yukami.backpacktab.client.keybind.KeyBindings;
import com.yukami.backpacktab.client.util.CarriedItemUtil;
import com.yukami.backpacktab.config.TabConfig;

public final class TabManager {
    private TabManager() {}

    private static AbstractContainerScreen<?> currentScreen;
    private static BlockPos storedBlockPos;
    private static Block storedBlockType;
    private static final TabSwitchState tabSwitch = new TabSwitchState();
    private static final List<InventoryTab> activeTabs = new ArrayList<>();
    private static final Set<Integer> consumedMouseButtons = new HashSet<>();

    static boolean beginTabSwitch(InventoryTab target, AbstractContainerMenu menu) {
        return tabSwitch.tryBegin(target.isActive(), menu, System.nanoTime());
    }

    static void refreshTabsForCurrentScreen(Player player) {
        if (currentScreen == null) return;
        activeTabs.clear();
        activeTabs.addAll(getAvailableTabs(player, currentScreen));
        updateActiveStates(activeTabs, currentScreen);
    }

    static void replaceActiveTab(InventoryTab oldTab, InventoryTab newTab) {
        int index = activeTabs.indexOf(oldTab);
        if (index >= 0) {
            activeTabs.set(index, newTab);
        }
    }

    public static Optional<UUID> getScreenBackpackContentsUuid(AbstractContainerScreen<?> screen) {
        try {
            if (screen.getMenu() instanceof BackpackContainer backpackContainer) {
                Player player = Minecraft.getInstance().player;
                if (player != null) {
                    return backpackContainer.getBackpackContext().getBackpackWrapper(player).getContentsUuid();
                }
            }
        } catch (Exception e) {
            YukamiBackpackTab.LOGGER.debug("Failed to resolve screen backpack identity", e);
        }
        return Optional.empty();
    }

    public static Optional<UUID> getBlockBackpackContentsUuid(BlockPos pos) {
        try {
            Level world = Minecraft.getInstance().level;
            if (world != null && world.getBlockEntity(pos) instanceof BackpackBlockEntity backpackBE) {
                return backpackBE.getBackpackWrapper().getContentsUuid();
            }
        } catch (Exception e) {
            YukamiBackpackTab.LOGGER.debug("Failed to resolve block backpack identity at {}", pos, e);
        }
        return Optional.empty();
    }

    public static List<InventoryTab> getAvailableTabs(Player player, AbstractContainerScreen<?> screen) {
        List<InventoryTab> tabs = new ArrayList<>();

        List<InventoryTab> backpackTabs = TabConfig.getBackpackTabMode().usesInceptionTabs()
                ? getInceptionTabs(player, screen)
                : new ArrayList<>(getBackpackTabs(player));
        if (backpackTabs.isEmpty()) {
            return tabs;
        }

        if (screen instanceof InventoryScreen) {
            tabs.add(new PlayerTab());
            tabs.addAll(backpackTabs);
            return tabs;
        }

        if (storedBlockPos != null && !isStoredBlockValid()) {
            clearStoredBlock();
        }

        if (storedBlockPos != null) {
            tabs.add(new ContainerTab(storedBlockPos));
        } else {
            tabs.add(new PlayerTab());
        }

        tabs.addAll(backpackTabs);
        return tabs;
    }

    /**
     * Inception mode: the worn backpack and the backpacks opened from the inventory, each followed by the tabs of
     * the backpacks that are open inside it.
     */
    private static List<InventoryTab> getInceptionTabs(Player player, AbstractContainerScreen<?> screen) {
        if (screen instanceof BackpackScreen) {
            InceptionTabs.sync(screen, player);
        } else if (isScreenAllowedForTabs(screen)) {
            // The player inventory (or a container) is shown, so every open backpack is closed.
            InceptionTabs.reset();
        }

        List<BackpackTab> roots = new ArrayList<>();
        for (BackpackTab tab : getBackpackTabs(player)) {
            // Worn backpacks always have a tab, carried ones need an inception upgrade.
            if (tab.isWorn() || InceptionTabs.hasInceptionUpgrade(tab)) {
                roots.add(tab);
            }
        }
        for (BackpackTab extra : InceptionTabs.getExtraRoots(player)) {
            if (roots.stream().noneMatch(tab -> InceptionTabs.sameLocation(tab, extra))) {
                roots.add(extra);
            }
        }

        List<InventoryTab> tabs = new ArrayList<>();
        for (BackpackTab root : roots) {
            tabs.add(root);
            tabs.addAll(InceptionTabs.getDescendantTabs(root));
        }
        return tabs;
    }

    public static void updateActiveStates(List<InventoryTab> tabs, AbstractContainerScreen<?> screen) {
        tabs.forEach(tab -> tab.setActive(tab.matchesCurrentScreen(screen)));
    }

    /** Worn backpacks first (in discovery order), then the rest sorted by contents UUID (unknown UUIDs last). */
    static final Comparator<BackpackTab> BACKPACK_TAB_ORDER = Comparator
            .comparing((BackpackTab tab) -> !tab.isWorn())
            .thenComparing(tab -> tab.getContentsUuid().isEmpty())
            .thenComparing(tab -> tab.isWorn() ? null : tab.getContentsUuid().orElse(null),
                    Comparator.nullsFirst(Comparator.<UUID>naturalOrder()));

    private static List<BackpackTab> getBackpackTabs(Player player) {
        List<BackpackTab> found = new ArrayList<>();
        TabConfig.BackpackTabMode mode = TabConfig.getBackpackTabMode();
        boolean any = mode == TabConfig.BackpackTabMode.ANY || mode == TabConfig.BackpackTabMode.ANY_INCEPTION;
        try {
            PlayerInventoryProvider.get().runOnBackpacks(player, (backpack, handlerName, identifier, slot) -> {
                boolean worn = !"main".equals(handlerName);
                if (!worn && !any) {
                    return false;
                }
                found.add(new BackpackTab(backpack, handlerName, identifier, slot));
                return !any;
            });
        } catch (Exception e) {
            YukamiBackpackTab.LOGGER.debug("Failed to find backpacks", e);
            return List.of();
        }

        if (any) {
            found.sort(BACKPACK_TAB_ORDER);
        }
        return found;
    }

    public static boolean isStoredBlockValid() {
        if (storedBlockPos == null || storedBlockType == null) {
            return false;
        }

        Player player = Minecraft.getInstance().player;
        if (player == null) return false;

        Level world = player.level();
        if (world == null) return false;

        BlockState blockState = world.getBlockState(storedBlockPos);
        if (blockState.getBlock() != storedBlockType) {
            return false;
        }

        return player.canInteractWithBlock(storedBlockPos, 1.0);
    }

    private static boolean isContainerBlock(Level world, BlockPos pos) {
        if (world == null || pos == null) return false;

        BlockState blockState = world.getBlockState(pos);

        if (blockState.getBlock() instanceof BackpackBlock) {
            return true;
        }

        if (isSophisticatedStorageBlock(blockState.getBlock())) {
            return true;
        }

        try {
            return blockState.getMenuProvider(world, pos) != null
                    || world.getBlockEntity(pos) instanceof net.minecraft.world.MenuProvider;
        } catch (Exception e) {
            YukamiBackpackTab.LOGGER.debug("Failed to inspect menu provider at {}", pos, e);
            return false;
        }
    }

    /**
     * Sophisticated Storage (optional) opens its screens from a custom use handler instead of exposing a menu
     * provider, so it is detected by registry namespace to avoid a hard dependency on the mod.
     */
    public static boolean isSophisticatedStorageBlock(Block block) {
        return "sophisticatedstorage".equals(
                net.minecraft.core.registries.BuiltInRegistries.BLOCK.getKey(block).getNamespace());
    }

    private static boolean isScreenAllowedForTabs(AbstractContainerScreen<?> screen) {
        if (screen instanceof InventoryScreen) {
            return true;
        }

        if (screen instanceof BackpackScreen) {
            return true;
        }

        return storedBlockPos != null && isStoredBlockValid();
    }

    private static boolean useCurrentScreen(AbstractContainerScreen<?> screen) {
        if (screen == currentScreen) return true;
        if (screen instanceof InventoryScreen) {
            currentScreen = screen;
            return true;
        }
        if (currentScreen == null) return false;
        if (!screen.getClass().equals(currentScreen.getClass()) && screen.getMenu() != currentScreen.getMenu()) {
            return false;
        }
        currentScreen = screen;
        return true;
    }

    private static void handleInvalidBlock() {
        if (currentScreen instanceof BackpackScreen) {
            LocalPlayer player = Minecraft.getInstance().player;
            if (player != null) {
                player.closeContainer();
            }
        }
        resetState();
    }

    private static void resetState() {
        clearStoredBlock();
        currentScreen = null;
        tabSwitch.reset();
        activeTabs.clear();
        consumedMouseButtons.clear();
        CarriedItemUtil.reset();
        TabRenderer.invalidateCache();
    }

    private static void clearStoredBlock() {
        storedBlockPos = null;
        storedBlockType = null;
    }

    public static void register() {
        UseBlockCallback.EVENT.register(TabManager::onRightClickBlock);
        ScreenEvents.AFTER_INIT.register(TabManager::onScreenInit);
        ClientTickEvents.END_CLIENT_TICK.register(TabManager::onClientTick);
    }

    private static InteractionResult onRightClickBlock(Player player, Level world, InteractionHand hand, BlockHitResult hit) {
        if (world.isClientSide()) {
            BlockPos clickedPos = hit.getBlockPos();

            if (isContainerBlock(world, clickedPos)) {
                storedBlockPos = clickedPos;
                storedBlockType = world.getBlockState(clickedPos).getBlock();
            } else {
                clearStoredBlock();
            }
        }
        return InteractionResult.PASS;
    }

    private static void onScreenInit(Minecraft client, Screen screen, int scaledWidth, int scaledHeight) {
        if (!(screen instanceof AbstractContainerScreen<?> containerScreen)) return;

        ScreenMouseEvents.allowMouseClick(screen).register((s, x, y, button) -> allowMouseClick(containerScreen, x, y, button));
        ScreenMouseEvents.allowMouseRelease(screen).register((s, x, y, button) -> allowMouseRelease(containerScreen, button));
        ScreenKeyboardEvents.allowKeyPress(screen).register((s, key, scancode, modifiers) -> allowKeyPress(key, scancode));
        ScreenEvents.remove(screen).register(s -> onScreenClose(s));

        currentScreen = containerScreen;

        boolean completedSwitch = tabSwitch.complete(containerScreen.getMenu());
        if (tabSwitch.isPending()) {
            return;
        }

        if (containerScreen instanceof InventoryScreen) {
            clearStoredBlock();
        } else if (storedBlockPos != null && !isStoredBlockValid()) {
            clearStoredBlock();
        }

        Player player = Minecraft.getInstance().player;
        if (player != null) {
            // In the multi tab modes the tab list is rebuilt after a switch too, so freshly opened backpacks
            // (which only get a contents UUID once opened) are identified correctly.
            if (!completedSwitch || TabConfig.getBackpackTabMode() != TabConfig.BackpackTabMode.SINGLE_WORN) {
                activeTabs.clear();
                activeTabs.addAll(getAvailableTabs(player, containerScreen));
            }

            updateActiveStates(activeTabs, containerScreen);
        }

        if (player instanceof LocalPlayer localPlayer) {
            MultiPlayerGameMode gameMode = Minecraft.getInstance().gameMode;
            if (gameMode != null) {
                TabSwitcher.restoreCarriedItems(localPlayer, gameMode);
            }
        }
    }

    /** Called from the container screen mixin, after labels were rendered (matrix translated to the GUI origin). */
    public static void onScreenRender(AbstractContainerScreen<?> containerScreen, GuiGraphics guiGraphics, int mouseX, int mouseY) {
        if (TabOffsetEditor.isEnabledFor(containerScreen)) {
            TabRenderer.renderTabs(guiGraphics, containerScreen, mouseX, mouseY);
            return;
        }

        if (TabConfig.isScreenBlacklisted(containerScreen.getClass().getSimpleName())) {
            return;
        }

        if (!isScreenAllowedForTabs(containerScreen)) {
            return;
        }

        if (!useCurrentScreen(containerScreen)) return;

        if (storedBlockPos != null && !isStoredBlockValid()) {
            handleInvalidBlock();
            return;
        }

        TabRenderer.renderTabs(guiGraphics, containerScreen, mouseX, mouseY);
    }

    /** Called from the container screen mixin at the start of every render; emulates mouse dragging for the offset editor. */
    public static void onScreenFrame(AbstractContainerScreen<?> containerScreen, int mouseX, int mouseY) {
        if (TabOffsetEditor.isEnabledFor(containerScreen)) {
            TabOffsetEditor.handleMouseDragged(containerScreen, mouseX, mouseY, org.lwjgl.glfw.GLFW.GLFW_MOUSE_BUTTON_LEFT);
        }
    }

    /** @return true when tooltips of the screen should be suppressed. */
    public static boolean shouldSuppressTooltip(Screen screen) {
        return screen instanceof AbstractContainerScreen<?> containerScreen && TabOffsetEditor.isEnabledFor(containerScreen);
    }

    private static boolean allowMouseClick(AbstractContainerScreen<?> containerScreen, double mouseX, double mouseY, int button) {
        consumedMouseButtons.remove(button);

        if (tabSwitch.isPending()) {
            consumedMouseButtons.add(button);
            return false;
        }

        if (TabOffsetEditor.isEnabledFor(containerScreen)) {
            TabOffsetEditor.handleMousePressed(containerScreen, mouseX, mouseY, button);
            consumedMouseButtons.add(button);
            return false;
        }

        if (!useCurrentScreen(containerScreen)) return true;

        if (TabConfig.isScreenBlacklisted(containerScreen.getClass().getSimpleName())) {
            return true;
        }

        if (!isScreenAllowedForTabs(containerScreen)) {
            return true;
        }

        // Next/previous tab keybindings may be bound to mouse buttons.
        if (KeyBindings.switchTabKey != null && KeyBindings.switchTabKey.matchesMouse(button)) {
            consumedMouseButtons.add(button);
            switchToNextTab(containerScreen);
            return false;
        }
        if (KeyBindings.previousTabKey != null && KeyBindings.previousTabKey.matchesMouse(button)) {
            consumedMouseButtons.add(button);
            switchToPreviousTab(containerScreen);
            return false;
        }

        if (TabRenderer.handleTabClick(mouseX, mouseY, button, containerScreen)) {
            consumedMouseButtons.add(button);
            return false;
        }
        return true;
    }

    /**
     * Switches to the next tab (cycling). Tabs are laid out away from the screen edge they are anchored to, so
     * "next" is always the tab further from the edge: right of the active tab on the left side, left of it on the right.
     */
    public static boolean switchToNextTab(AbstractContainerScreen<?> containerScreen) {
        return switchTab(containerScreen, 1);
    }

    /** Switches to the previous tab (cycling), i.e. the tab closer to the anchored screen edge. */
    public static boolean switchToPreviousTab(AbstractContainerScreen<?> containerScreen) {
        return switchTab(containerScreen, -1);
    }

    /** Switches tab by the given direction (cycling), mirroring the conditions of a tab click. */
    private static boolean switchTab(AbstractContainerScreen<?> containerScreen, int direction) {
        if (tabSwitch.isPending() || TabOffsetEditor.isEnabled()) return false;
        if (!useCurrentScreen(containerScreen)) return false;
        if (TabConfig.isScreenBlacklisted(containerScreen.getClass().getSimpleName())) return false;
        if (!isScreenAllowedForTabs(containerScreen)) return false;

        Minecraft minecraft = Minecraft.getInstance();
        Player player = minecraft.player;
        if (player == null || activeTabs.size() < 2) return false;

        int activeIndex = 0;
        for (int i = 0; i < activeTabs.size(); i++) {
            if (activeTabs.get(i).isActive()) {
                activeIndex = i;
                break;
            }
        }

        for (int step = 1; step < activeTabs.size(); step++) {
            InventoryTab target = activeTabs.get(Math.floorMod(activeIndex + step * direction, activeTabs.size()));
            if (!target.isActive() && !target.matchesCurrentScreen(containerScreen)) {
                TabSwitcher.switchToTab(target, player, minecraft.gameMode);
                return true;
            }
        }
        return false;
    }

    private static boolean allowMouseRelease(AbstractContainerScreen<?> containerScreen, int button) {
        // Always end an offset-editor drag, even if the press was consumed (it always is in editor mode).
        if (TabOffsetEditor.isEnabledFor(containerScreen)) {
            TabOffsetEditor.handleMouseReleased(containerScreen, button);
        }

        if (consumedMouseButtons.remove(button)) {
            return false;
        }

        if (tabSwitch.isPending()) {
            return false;
        }

        if (TabOffsetEditor.isEnabledFor(containerScreen)) {
            return false;
        }
        return true;
    }

    private static boolean allowKeyPress(int keyCode, int scanCode) {
        if (!tabSwitch.isPending()) return true;

        Minecraft minecraft = Minecraft.getInstance();

        return keyCode == org.lwjgl.glfw.GLFW.GLFW_KEY_ESCAPE ||
                minecraft.options.keyInventory.matches(keyCode, scanCode);
    }

    private static void onScreenClose(Screen screen) {
        if (screen instanceof AbstractContainerScreen<?> containerScreen && TabOffsetEditor.isEnabledFor(containerScreen)) {
            TabOffsetEditor.cancel();
        }

        if (screen == currentScreen) {
            if (!tabSwitch.isPending()) {
                resetState();
            }
            currentScreen = null;
        }
    }

    private static void onClientTick(Minecraft minecraft) {
        if (!tabSwitch.isPending()) {
            tickInception(minecraft);
            return;
        }

        if (minecraft.player == null || !(minecraft.screen instanceof AbstractContainerScreen<?>)) {
            resetState();
        } else if (tabSwitch.hasTimedOut(System.nanoTime())) {
            resetState();
            minecraft.player.clientSideCloseContainer();
            minecraft.player.sendSystemMessage(net.minecraft.network.chat.Component.literal(
                    "Tab switch timed out. Please reopen your inventory."));
        }
    }

    /** Keeps the inception tabs in line with the open backpack, whose contents can change while it is shown. */
    private static void tickInception(Minecraft minecraft) {
        if (!TabConfig.getBackpackTabMode().usesInceptionTabs()
                || minecraft.player == null
                || !(minecraft.screen instanceof AbstractContainerScreen<?>)) {
            // Closing the screen closes the open backpacks.
            InceptionTabs.reset();
            return;
        }

        if (minecraft.screen instanceof BackpackScreen backpackScreen && backpackScreen == currentScreen
                && InceptionTabs.sync(backpackScreen, minecraft.player)) {
            refreshTabsForCurrentScreen(minecraft.player);
        }
    }

    public static List<InventoryTab> getActiveTabsView() {
        return activeTabs;
    }

    public static BlockPos getStoredBlockPos() {
        return storedBlockPos;
    }
}
