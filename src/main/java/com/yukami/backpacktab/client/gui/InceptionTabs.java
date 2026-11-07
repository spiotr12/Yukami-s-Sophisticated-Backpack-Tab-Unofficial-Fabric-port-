package com.yukami.backpacktab.client.gui;

import com.yukami.backpacktab.YukamiBackpackTab;
import com.yukami.backpacktab.client.tabs.BackpackTab;
import com.yukami.backpacktab.client.tabs.InventoryTab;
import com.yukami.backpacktab.client.tabs.SubBackpackTab;
import com.yukami.backpacktab.mixin.BackpackContextItemAccessor;
import com.yukami.backpacktab.mixin.BackpackContextSubAccessor;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.p3pp3rf1y.sophisticatedbackpacks.backpack.BackpackItem;
import net.p3pp3rf1y.sophisticatedbackpacks.backpack.wrapper.BackpackWrapper;
import net.p3pp3rf1y.sophisticatedbackpacks.backpack.wrapper.IBackpackWrapper;
import net.p3pp3rf1y.sophisticatedbackpacks.common.gui.BackpackContainer;
import net.p3pp3rf1y.sophisticatedbackpacks.common.gui.BackpackContext;
import net.p3pp3rf1y.sophisticatedbackpacks.network.BackpackOpenPayload;
import net.p3pp3rf1y.sophisticatedbackpacks.upgrades.inception.InceptionUpgradeItem;
import net.p3pp3rf1y.sophisticatedcore.upgrades.UpgradeType;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

/**
 * State of the "inception upgrade only" tab mode.
 * <p>
 * The worn backpack always has a tab. A backpack opened from the inventory gets a tab (a "root") that lives until the
 * player goes back to the inventory. While a backpack with an inception upgrade is open, tabs for the backpacks stored
 * inside it are added after its own tab (see {@link InceptionTree} for which ones are shown).
 * <p>
 * Everything is derived from the backpack menu that is open on the client, so it stays correct no matter how the
 * backpack was opened (tab click, keybinding, Sophisticated Backpacks' own controls).
 */
public final class InceptionTabs {
    private InceptionTabs() {}

    /**
     * Deepest level an inner backpack can be opened from. Sophisticated Backpacks only knows sub backpack contexts of
     * a root item backpack, opening deeper levels would open a sibling of the wrong backpack. The tree itself is
     * recursive, raise this if Sophisticated Backpacks ever supports deeper nesting.
     */
    static final int MAX_DEPTH = 1;

    private record RootKey(String handlerName, String identifier, int slot) {
        static RootKey of(BackpackTab tab) {
            return new RootKey(tab.getHandlerName(), tab.getIdentifier(), tab.getSlot());
        }
    }

    private static final List<BackpackTab> extraRoots = new ArrayList<>();
    private static InceptionTree<InventoryTab> tree;
    private static RootKey treeKey;
    private static Optional<UUID> treeRootUuid = Optional.empty();
    /** Backpacks (by contents UUID) that were seen open, and whether they have an inception upgrade. */
    private static final Map<UUID, Boolean> knownInception = new HashMap<>();
    /** Bumped whenever the set of tabs or the selected tab changes. */
    private static int version;

    /** Forgets all open backpacks, i.e. the player is back in the inventory or closed the screen. */
    public static void reset() {
        if (tree != null || !extraRoots.isEmpty()) {
            version++;
        }
        tree = null;
        treeKey = null;
        treeRootUuid = Optional.empty();
        extraRoots.clear();
    }

    private static void clearTree() {
        if (tree != null) {
            version++;
        }
        tree = null;
        treeKey = null;
        treeRootUuid = Optional.empty();
    }

    /**
     * Updates the state from the backpack screen that is currently shown.
     *
     * @return whether the tabs have to be rebuilt
     */
    public static boolean sync(AbstractContainerScreen<?> screen, Player player) {
        int before = version;
        try {
            doSync(screen, player);
        } catch (Exception e) {
            YukamiBackpackTab.LOGGER.debug("Failed to read open backpack for inception tabs", e);
            clearTree();
        }
        return version != before;
    }

    private static void doSync(AbstractContainerScreen<?> screen, Player player) {
        if (!(screen.getMenu() instanceof BackpackContainer menu)) {
            return;
        }

        BackpackContext context = menu.getBackpackContext();
        if (!(context instanceof BackpackContext.Item) || context instanceof BackpackContext.AnotherPlayer) {
            // Placed backpacks and other players' backpacks are not part of the tab bar.
            reset();
            return;
        }

        boolean sub = context instanceof BackpackContext.ItemSubBackpack;
        BackpackContextItemAccessor accessor = (BackpackContextItemAccessor) context;
        RootKey key = new RootKey(accessor.yukamibackpacktab$getHandlerName(),
                accessor.yukamibackpacktab$getIdentifier(), context.getBackpackSlotIndex());

        IBackpackWrapper currentWrapper = context.getBackpackWrapper(player);
        ItemStack currentStack = currentWrapper.getBackpack();
        ItemStack rootStack = currentStack;
        Optional<UUID> rootUuid = currentWrapper.getContentsUuid();
        if (sub) {
            Optional<IBackpackWrapper> parent = context.getParentBackpackWrapper(player)
                    .filter(IBackpackWrapper.class::isInstance).map(IBackpackWrapper.class::cast);
            if (parent.isEmpty()) {
                clearTree();
                return;
            }
            rootStack = parent.get().getBackpack();
            rootUuid = parent.get().getContentsUuid();
        }
        if (rootStack.isEmpty() || currentStack.isEmpty()) {
            return;
        }

        boolean currentHasInception = hasInceptionUpgrade(menu);
        rememberInception(currentWrapper.getContentsUuid(), currentHasInception);
        if (sub) {
            rememberInception(rootUuid, true);
        }

        if (!key.equals(treeKey)) {
            BackpackTab rootTab = new BackpackTab(rootStack, key.handlerName(), key.identifier(), key.slot());
            tree = new InceptionTree<>(rootTab);
            treeKey = key;
            version++;
            if (rootTab.isWorn()) {
                // Worn backpacks always have a tab, it is rebuilt from the player's equipment.
            } else if (extraRoots.stream().noneMatch(tab -> RootKey.of(tab).equals(key))) {
                extraRoots.add(rootTab);
            }
        }
        treeRootUuid = rootUuid;

        InceptionTree.Node<InventoryTab> node = tree.root();
        if (sub) {
            int slot = ((BackpackContextSubAccessor) context).yukamibackpacktab$getSubBackpackSlotIndex();
            node = tree.child(tree.root(), slot);
            if (node == null) {
                node = tree.addChild(tree.root(), slot, new SubBackpackTab(currentStack, rootStack, List.of(slot)));
                version++;
            }
        }
        if (tree.current() != node) {
            tree.setCurrent(node);
            version++;
        }

        List<InceptionTree.Entry<InventoryTab>> children = List.of();
        if (node.depth() < MAX_DEPTH && currentHasInception) {
            children = collectChildren(menu, node.path(), currentStack);
        }
        if (tree.setChildren(node, children, InceptionTabs::sameTab)) {
            version++;
        }
    }

    private static void rememberInception(Optional<UUID> uuid, boolean hasInception) {
        if (uuid.isPresent() && !Boolean.valueOf(hasInception).equals(knownInception.put(uuid.get(), hasInception))) {
            version++;
        }
    }

    /**
     * Whether a carried backpack has an inception upgrade. The client only knows the upgrades of backpacks whose
     * contents it received, so backpacks that were seen open are remembered.
     */
    public static boolean hasInceptionUpgrade(BackpackTab tab) {
        Optional<UUID> uuid = tab.getContentsUuid();
        if (uuid.isPresent() && knownInception.containsKey(uuid.get())) {
            return knownInception.get(uuid.get());
        }
        try {
            // Reflection: UpgradeHandler extends a Porting Lib class that is not on the compile classpath.
            IBackpackWrapper wrapper = BackpackWrapper.fromStack(tab.getTabIcon());
            Object upgradeHandler = wrapper.getClass().getMethod("getUpgradeHandler").invoke(wrapper);
            return upgradeHandler != null && Boolean.TRUE.equals(upgradeHandler.getClass()
                    .getMethod("hasUpgrade", UpgradeType.class).invoke(upgradeHandler, InceptionUpgradeItem.TYPE));
        } catch (ReflectiveOperationException | RuntimeException e) {
            YukamiBackpackTab.LOGGER.debug("Failed to inspect upgrades of a backpack", e);
            return false;
        }
    }

    public static boolean sameLocation(BackpackTab a, BackpackTab b) {
        return RootKey.of(a).equals(RootKey.of(b));
    }

    private static boolean sameTab(InventoryTab a, InventoryTab b) {
        if (!ItemStack.matches(a.getTabIcon(), b.getTabIcon())) return false;
        // The tab color comes from the parent, so a recolored parent has to rebuild its children.
        return !(a instanceof SubBackpackTab subA && b instanceof SubBackpackTab subB)
                || ItemStack.matches(subA.getParentStack(), subB.getParentStack());
    }

    private static boolean hasInceptionUpgrade(BackpackContainer menu) {
        for (Slot slot : menu.upgradeSlots) {
            if (slot.getItem().getItem() instanceof InceptionUpgradeItem) {
                return true;
            }
        }
        return false;
    }

    private static List<InceptionTree.Entry<InventoryTab>> collectChildren(BackpackContainer menu, List<Integer> parentPath, ItemStack parentStack) {
        List<InceptionTree.Entry<InventoryTab>> children = new ArrayList<>();
        for (int i = 0; i < menu.slots.size(); i++) {
            if (!menu.isStorageInventorySlot(i)) continue;
            ItemStack stack = menu.getSlot(i).getItem();
            if (!(stack.getItem() instanceof BackpackItem)) continue;

            List<Integer> path = new ArrayList<>(parentPath);
            path.add(i);
            children.add(new InceptionTree.Entry<>(i, new SubBackpackTab(stack, parentStack, path)));
        }
        return children;
    }

    /** Extra root tabs (backpacks opened from the inventory), without the ones that are gone. */
    public static List<BackpackTab> getExtraRoots(Player player) {
        for (int i = 0; i < extraRoots.size(); i++) {
            BackpackTab tab = extraRoots.get(i);
            Optional<BackpackTab> resolved = tab.resolveCurrentLocation(player);
            if (resolved.isEmpty()) {
                extraRoots.remove(i--);
                version++;
            } else if (resolved.get() != tab) {
                extraRoots.set(i, resolved.get());
                version++;
            }
        }
        return List.copyOf(extraRoots);
    }

    /** Tabs of the open backpacks below the given root tab, in tab order. */
    public static List<InventoryTab> getDescendantTabs(BackpackTab rootTab) {
        if (tree == null || !RootKey.of(rootTab).equals(treeKey)) {
            return List.of();
        }
        return tree.visibleDescendants().stream().map(InceptionTree.Node::value).toList();
    }

    public static boolean hasOpenBackpack() {
        return tree != null;
    }

    /** @return whether the root backpack of the given tab is the one that is shown. */
    public static boolean isRootCurrent(BackpackTab rootTab) {
        if (tree == null || tree.current() != tree.root()) {
            return false;
        }
        if (RootKey.of(rootTab).equals(treeKey)) {
            return true;
        }
        // The same backpack may be addressed through another slot description.
        return treeRootUuid.isPresent() && treeRootUuid.equals(rootTab.getContentsUuid());
    }

    public static boolean isCurrent(List<Integer> path) {
        return tree != null && tree.isCurrent(path);
    }

    /** Opens the inner backpack at the given path, going through the open parent backpack. */
    public static void open(List<Integer> path) {
        if (tree == null || path.isEmpty()) return;

        List<Integer> currentPath = tree.current().path();
        int parentDepth = path.size() - 1;
        // Only parents on the way to the current backpack are reachable (those are the ones with visible children).
        if (parentDepth > currentPath.size() || !currentPath.subList(0, parentDepth).equals(path.subList(0, parentDepth))) {
            return;
        }

        try {
            // The server opens the parent for slot -1 and the inner backpack of the open one for any other slot. Both
            // packets are handled back to back, so going up and then down needs no waiting for the screen.
            for (int level = currentPath.size(); level > parentDepth; level--) {
                ClientPlayNetworking.send(new BackpackOpenPayload(-1));
            }
            ClientPlayNetworking.send(new BackpackOpenPayload(path.get(path.size() - 1)));
        } catch (Exception e) {
            YukamiBackpackTab.LOGGER.debug("Failed to open inner backpack tab {}", path, e);
        }
    }
}
