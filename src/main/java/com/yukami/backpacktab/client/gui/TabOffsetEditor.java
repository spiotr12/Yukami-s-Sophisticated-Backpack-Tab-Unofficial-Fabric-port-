package com.yukami.backpacktab.client.gui;

import com.yukami.backpacktab.client.util.ScreenAccess;

import com.yukami.backpacktab.client.gui.util.DropdownMenu;
import com.yukami.backpacktab.client.gui.util.EditorGui;
import com.yukami.backpacktab.client.gui.util.ResponsivePanel;
import com.yukami.backpacktab.config.TabConfig;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import org.lwjgl.glfw.GLFW;

import java.util.EnumMap;
import java.util.LinkedHashMap;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Stream;

public final class TabOffsetEditor {
    private TabOffsetEditor() {}
    private static boolean enabled;
    private static String screenClassName = "";
    private static TabConfig.TabPosition selectedPosition = TabConfig.TabPosition.TOP_LEFT;

    private static TabConfig.TabPosition pendingScreenPosition = TabConfig.TabPosition.TOP_LEFT;

    private static TabConfig.TabPosition pendingGlobalPosition = TabConfig.TabPosition.TOP_LEFT;
    private static boolean dragging;
    private static double lastMouseX = 0;
    private static double lastMouseY = 0;
    private static final EditorGui editorGui = new EditorGui();
    private static final List<TabConfig.TabPosition> POSITION_OPTIONS = List.of(TabConfig.TabPosition.values());

    private static final DropdownMenu<Optional<TabConfig.TabPosition>> screenPositionDropdown =
            new DropdownMenu<>(
                    Stream.concat(Stream.of(Optional.<TabConfig.TabPosition>empty()),
                            POSITION_OPTIONS.stream().map(Optional::of)).toList(),
                    Optional.empty(),
                    position -> position.map(TabOffsetEditor::positionLabel).orElse("Use global"),
                    position -> {
                        pendingScreenPosition = position.orElse(null);
                        selectedPosition = effectivePosition();
                    });
    private static final DropdownMenu<TabConfig.TabPosition> globalPositionDropdown =
            new DropdownMenu<>(
                    POSITION_OPTIONS,
                    TabConfig.TabPosition.TOP_LEFT,
                    TabOffsetEditor::positionLabel,
                    position -> {
                        pendingGlobalPosition = position;
                        if (pendingScreenPosition == null) selectedPosition = position;
                    });
    private static TabConfig.BackpackTabMode pendingBackpackMode = TabConfig.BackpackTabMode.SINGLE_WORN;
    private static final DropdownMenu<TabConfig.BackpackTabMode> backpackModeDropdown =
            new DropdownMenu<>(
                    List.of(TabConfig.BackpackTabMode.values()),
                    TabConfig.BackpackTabMode.SINGLE_WORN,
                    TabOffsetEditor::backpackModeLabel,
                    mode -> pendingBackpackMode = mode);
    private static final int DROPDOWN_HEADER_HEIGHT = 14;
    private static final int DROPDOWN_OPTION_HEIGHT = 12;
    private static final int BACKPACK_ROW_HEIGHT = 20;
    private static final int PANEL_HEIGHT = 164 + BACKPACK_ROW_HEIGHT;
    private static final int STACKED_PANEL_HEIGHT = 188 + BACKPACK_ROW_HEIGHT + 12;

    private static final int COLOR_TITLE = 0xFFFFFF;
    private static final int COLOR_VALUE = 0xFFFF55;
    private static final int COLOR_CAPTION = 0x55FFFF;
    private static final Map<TabConfig.TabPosition, TabConfig.ScreenOffset> pendingOffsets =
            new EnumMap<>(TabConfig.TabPosition.class);

    private static final int MENU_PANEL_WIDTH = 170;
    private static final int MENU_ROW_HEIGHT = 12;
    private static final int MENU_HEADER_HEIGHT = 20;
    private static final Map<String, Boolean> pendingMenus = new LinkedHashMap<>();
    private static int menuPanelX, menuPanelY, menuPanelWidth, menuPanelHeight;
    private static int menuScroll;

    private static int menuVisibleRows() {
        return Math.max(1, (menuPanelHeight - MENU_HEADER_HEIGHT - 4) / MENU_ROW_HEIGHT);
    }

    private static int maxMenuScroll() {
        return Math.max(0, pendingMenus.size() - menuVisibleRows());
    }

    private static boolean inMenuPanel(double x, double y) {
        return x >= menuPanelX && x < menuPanelX + menuPanelWidth && y >= menuPanelY && y < menuPanelY + menuPanelHeight;
    }

    public static boolean handleMouseScrolled(AbstractContainerScreen<?> screen, double mouseX, double mouseY, double amount) {
        if (!isEnabledFor(screen)) return false;
        double lx = mouseX - ScreenAccess.guiLeft(screen);
        double ly = mouseY - ScreenAccess.guiTop(screen);
        if (inMenuPanel(lx, ly)) {
            menuScroll = Math.max(0, Math.min(maxMenuScroll(), menuScroll - (int) Math.signum(amount)));
        }
        return true;
    }

    private static boolean handleMenuClick(double lx, double ly) {
        if (!inMenuPanel(lx, ly)) return false;
        int listTop = menuPanelY + MENU_HEADER_HEIGHT;
        if (ly < listTop) return true;
        int row = (int) ((ly - listTop) / MENU_ROW_HEIGHT);
        if (row >= menuVisibleRows()) return true;
        int index = row + menuScroll;
        if (index < pendingMenus.size()) {
            String id = pendingMenus.keySet().stream().skip(index).findFirst().orElse(null);
            if (id != null) pendingMenus.put(id, !pendingMenus.get(id));
        }
        return true;
    }

    private static String menuLabel(String id) {
        String text = id.startsWith("minecraft:") ? id.substring("minecraft:".length()) : id;
        return text.replace('_', ' ');
    }

    private static void renderMenuPanel(GuiGraphics guiGraphics, AbstractContainerScreen<?> screen, double lx, double ly) {
        var font = Minecraft.getInstance().font;
        int guiLeft = ScreenAccess.guiLeft(screen);
        int guiTop = ScreenAccess.guiTop(screen);
        int guiRight = guiLeft + ScreenAccess.xSize(screen);
        menuPanelWidth = MENU_PANEL_WIDTH;
        int absX = guiRight + 8;
        if (absX + menuPanelWidth > screen.width - 4) absX = Math.max(4, screen.width - menuPanelWidth - 4);
        menuPanelHeight = Math.min(screen.height - 16, MENU_HEADER_HEIGHT + 4 + MENU_ROW_HEIGHT * Math.max(1, pendingMenus.size()));
        menuPanelX = absX - guiLeft;
        menuPanelY = 8 - guiTop;
        menuScroll = Math.max(0, Math.min(maxMenuScroll(), menuScroll));

        editorGui.panel(guiGraphics, menuPanelX, menuPanelY, menuPanelWidth, menuPanelHeight);
        editorGui.sectionHeader(guiGraphics, "Menus with tabs", menuPanelX + 6, menuPanelY + 8, menuPanelWidth - 12, COLOR_CAPTION);

        int rows = menuVisibleRows();
        int index = 0;
        for (Map.Entry<String, Boolean> entry : pendingMenus.entrySet()) {
            if (index >= menuScroll && index < menuScroll + rows) {
                int y = menuPanelY + MENU_HEADER_HEIGHT + (index - menuScroll) * MENU_ROW_HEIGHT;
                int bx = menuPanelX + 6;
                boolean hovered = lx >= menuPanelX && lx < menuPanelX + menuPanelWidth && ly >= y && ly < y + MENU_ROW_HEIGHT;
                guiGraphics.fill(bx, y + 1, bx + 9, y + 10, 0xFFA0A0A0);
                guiGraphics.fill(bx + 1, y + 2, bx + 8, y + 9, hovered ? 0xFF565B63 : 0xFF202328);
                if (entry.getValue()) guiGraphics.fill(bx + 2, y + 3, bx + 7, y + 8, 0xFF55FF55);
                String text = fitLabel(menuLabel(entry.getKey()), menuPanelWidth - 12 - 14);
                editorGui.label(guiGraphics, text, bx + 14, y + 2, entry.getValue() ? COLOR_TITLE : 0x888888);
            }
            index++;
        }
        if (maxMenuScroll() > 0) {
            int trackTop = menuPanelY + MENU_HEADER_HEIGHT;
            int trackH = rows * MENU_ROW_HEIGHT;
            int barH = Math.max(8, trackH * rows / pendingMenus.size());
            int barY = trackTop + (trackH - barH) * menuScroll / maxMenuScroll();
            int barX = menuPanelX + menuPanelWidth - 4;
            guiGraphics.fill(barX, barY, barX + 2, barY + barH, 0xFFA0A0A0);
        }
    }

    public static boolean isEnabled() {
        return enabled;
    }

    public static boolean isEnabledFor(AbstractContainerScreen<?> screen) {
        return enabled && screen != null && screen.getClass().getSimpleName().equals(screenClassName);
    }

    public static void toggle(Screen screen) {
        if (enabled) {
            cancel();
            return;
        }

        if (screen instanceof AbstractContainerScreen<?> containerScreen) {
            begin(containerScreen);
        }
    }

    public static void begin(AbstractContainerScreen<?> screen) {
        screenClassName = screen.getClass().getSimpleName();

        TabConfig.TabPosition override = TabConfig.getScreenPositionOverride(screenClassName);
        pendingScreenPosition = override;
        pendingGlobalPosition = TabConfig.getTabPosition();
        selectedPosition = effectivePosition();
        dragging = false;
        editorGui.clearButtons();
        screenPositionDropdown.setSelected(Optional.ofNullable(pendingScreenPosition));
        screenPositionDropdown.close();
        globalPositionDropdown.setSelected(pendingGlobalPosition);
        globalPositionDropdown.close();
        pendingBackpackMode = TabConfig.getBackpackTabMode();
        backpackModeDropdown.setSelected(pendingBackpackMode);
        backpackModeDropdown.close();
        pendingOffsets.clear();
        pendingMenus.clear();
        for (String id : TabManager.getAllMenuIds()) {
            pendingMenus.put(id, TabConfig.isMenuEnabled(id));
        }
        menuScroll = 0;

        for (TabConfig.TabPosition position : TabConfig.TabPosition.values()) {
            TabConfig.ScreenOffset offset = TabConfig.getScreenOffset(screenClassName, position);
            pendingOffsets.put(position, new TabConfig.ScreenOffset(offset.x, offset.y));
        }

        enabled = true;
    }

    public static void cancel() {
        enabled = false;
        dragging = false;
        screenPositionDropdown.close();
        globalPositionDropdown.close();
        backpackModeDropdown.close();
        pendingOffsets.clear();
        pendingMenus.clear();
    }

    public static boolean handleKeyPressed(int keyCode) {
        if (!enabled) {
            return false;
        }

        switch (keyCode) {
            case GLFW.GLFW_KEY_TAB -> {
                cyclePosition();
                return true;
            }
            case GLFW.GLFW_KEY_ENTER, GLFW.GLFW_KEY_KP_ENTER -> {
                save();
                return true;
            }
            case GLFW.GLFW_KEY_R -> {
                resetScreenOffsets();
                return true;
            }
            case GLFW.GLFW_KEY_ESCAPE -> {
                cancel();
                return true;
            }
            case GLFW.GLFW_KEY_UP -> {
                moveSelected(0, -stepSize());
                return true;
            }
            case GLFW.GLFW_KEY_DOWN -> {
                moveSelected(0, stepSize());
                return true;
            }
            case GLFW.GLFW_KEY_LEFT -> {
                moveSelected(-stepSize(), 0);
                return true;
            }
            case GLFW.GLFW_KEY_RIGHT -> {
                moveSelected(stepSize(), 0);
                return true;
            }
            default -> {
                return false;
            }
        }
    }

    public static TabConfig.ScreenOffset getOffset(TabConfig.TabPosition position) {
        return pendingOffsets.getOrDefault(position, TabConfig.ScreenOffset.ZERO);
    }

    public static boolean handleMousePressed(AbstractContainerScreen<?> screen, double mouseX, double mouseY, int button) {
        if (!isEnabledFor(screen) || button != GLFW.GLFW_MOUSE_BUTTON_LEFT) {
            return false;
        }

        double localMouseX = mouseX - ScreenAccess.guiLeft(screen);
        double localMouseY = mouseY - ScreenAccess.guiTop(screen);

        if (!screenPositionDropdown.isOpen() && !globalPositionDropdown.isOpen() && !backpackModeDropdown.isOpen()
                && handleMenuClick(localMouseX, localMouseY)) {
            return true;
        }
        if (screenPositionDropdown.isOpen()) {
            screenPositionDropdown.handleMousePressed(localMouseX, localMouseY);
            return true;
        }
        if (globalPositionDropdown.isOpen()) {
            globalPositionDropdown.handleMousePressed(localMouseX, localMouseY);
            return true;
        }
        if (backpackModeDropdown.isOpen()) {
            backpackModeDropdown.handleMousePressed(localMouseX, localMouseY);
            return true;
        }
        if (screenPositionDropdown.handleMousePressed(localMouseX, localMouseY)) {
            return true;
        }
        if (globalPositionDropdown.handleMousePressed(localMouseX, localMouseY)) {
            return true;
        }
        if (backpackModeDropdown.handleMousePressed(localMouseX, localMouseY)) {
            return true;
        }
        if (editorGui.click(localMouseX, localMouseY)) {
            return true;
        }

        TabConfig.TabPosition hoveredPosition = getHoveredPosition(screen, mouseX, mouseY);
        if (hoveredPosition == null) {
            return true;
        }

        selectedPosition = hoveredPosition;
        dragging = true;
        lastMouseX = mouseX;
        lastMouseY = mouseY;
        return true;
    }

    public static boolean handleMouseDragged(AbstractContainerScreen<?> screen, double mouseX, double mouseY, int button) {
        if (!isEnabledFor(screen) || button != GLFW.GLFW_MOUSE_BUTTON_LEFT) {
            return false;
        }

        if (!dragging) {
            return true;
        }

        int dx = (int) Math.round(mouseX - lastMouseX);
        int dy = (int) Math.round(mouseY - lastMouseY);
        if (dx != 0 || dy != 0) {
            moveSelected(dx, dy);
            lastMouseX += dx;
            lastMouseY += dy;
        }
        return true;
    }

    public static boolean handleMouseReleased(AbstractContainerScreen<?> screen, int button) {
        if (!isEnabledFor(screen) || button != GLFW.GLFW_MOUSE_BUTTON_LEFT) {
            return false;
        }

        dragging = false;
        return true;
    }

    public static void render(GuiGraphics guiGraphics, AbstractContainerScreen<?> screen, int mouseX, int mouseY) {
        for (TabConfig.TabPosition position : TabConfig.TabPosition.values()) {
            TabRenderer.renderTabsAtPosition(
                    guiGraphics,
                    screen,
                    mouseX,
                    mouseY,
                    position,
                    getOffset(position),
                    position == selectedPosition
            );
        }

        Minecraft minecraft = Minecraft.getInstance();
        TabConfig.ScreenOffset offset = getOffset(selectedPosition);
        int minimumWidth = minimumPanelWidth();
        int labelWidth = Math.max(minecraft.font.width("Current screen"),
                Math.max(minecraft.font.width("Global"), minecraft.font.width("Backpack tabs"))) + 8;
        int preferredWidth = minimumWidth + labelWidth;
        ResponsivePanel.Layout layout = ResponsivePanel.findLayout(screen, preferredWidth, minimumWidth, 8,
                width -> width < preferredWidth ? STACKED_PANEL_HEIGHT : PANEL_HEIGHT);
        boolean stacked = layout.width() < preferredWidth;
        int extraRow = stacked ? 12 : 0;
        int screenFieldY = 35 + extraRow;
        int globalFieldY = 55 + extraRow * 2;
        int backpackFieldY = globalFieldY + BACKPACK_ROW_HEIGHT + extraRow;
        int backpackShift = BACKPACK_ROW_HEIGHT + extraRow;
        int offsetSectionY = 80 + extraRow * 2 + backpackShift;
        int selectedY = 92 + extraRow * 2 + backpackShift;
        int valuesY = 106 + extraRow * 2 + backpackShift;
        int resetY = 122 + extraRow * 2 + backpackShift;
        int actionsY = resetY + 14 + 4;
        int panelX = layout.x() - ScreenAccess.guiLeft(screen);
        int panelY = layout.y() - ScreenAccess.guiTop(screen);
        int innerX = panelX + 6;
        int innerWidth = layout.width() - 12;
        int fieldX = innerX + (stacked ? 0 : labelWidth);
        int fieldWidth = innerWidth - (stacked ? 0 : labelWidth);
        double localMouseX = mouseX - ScreenAccess.guiLeft(screen);
        double localMouseY = mouseY - ScreenAccess.guiTop(screen);

        editorGui.clearButtons();

        guiGraphics.pose().pushPose();
        guiGraphics.pose().translate(0, 0, 300);
        editorGui.panel(guiGraphics, panelX, panelY, layout.width(), layout.height());
        editorGui.label(guiGraphics, fitLabel(screenClassName, innerWidth), innerX, panelY + 6, COLOR_TITLE);
        editorGui.sectionHeader(guiGraphics, "Position", innerX, panelY + 24, innerWidth, COLOR_CAPTION);

        editorGui.label(guiGraphics, "Current screen", innerX, panelY + screenFieldY + (stacked ? -12 : 3), COLOR_CAPTION);
        editorGui.label(guiGraphics, "Global", innerX, panelY + globalFieldY + (stacked ? -12 : 3), COLOR_CAPTION);
        editorGui.label(guiGraphics, "Backpack tabs", innerX, panelY + backpackFieldY + (stacked ? -12 : 3), COLOR_CAPTION);
        editorGui.sectionHeader(guiGraphics, "Offset", innerX, panelY + offsetSectionY, innerWidth, COLOR_CAPTION);

        editorGui.label(guiGraphics, "Selected: " + positionLabel(selectedPosition),
                innerX, panelY + selectedY, COLOR_VALUE);
        editorGui.label(guiGraphics, fitLabel("X: " + offset.x + "  Y: " + offset.y, innerWidth),
                innerX, panelY + valuesY, COLOR_VALUE);
        editorGui.button(guiGraphics, "Reset", innerX, panelY + resetY, innerWidth, 14,
                localMouseX, localMouseY, TabOffsetEditor::resetScreenOffsets);

        int halfWidth = (innerWidth - 4) / 2;
        editorGui.primaryButton(guiGraphics, "Save", innerX, panelY + actionsY, halfWidth, 16,
                localMouseX, localMouseY, TabOffsetEditor::save);
        editorGui.button(guiGraphics, "Cancel", innerX + halfWidth + 4, panelY + actionsY,
                innerWidth - halfWidth - 4, 16, localMouseX, localMouseY, TabOffsetEditor::cancel);

        int maxVisibleY = screen.height - ScreenAccess.guiTop(screen);
        screenPositionDropdown.renderHeader(guiGraphics, minecraft.font, fieldX, panelY + screenFieldY, fieldWidth,
                DROPDOWN_HEADER_HEIGHT, DROPDOWN_OPTION_HEIGHT, localMouseX, localMouseY, maxVisibleY);
        globalPositionDropdown.renderHeader(guiGraphics, minecraft.font, fieldX, panelY + globalFieldY, fieldWidth,
                DROPDOWN_HEADER_HEIGHT, DROPDOWN_OPTION_HEIGHT, localMouseX, localMouseY, maxVisibleY);

        backpackModeDropdown.renderHeader(guiGraphics, minecraft.font, fieldX, panelY + backpackFieldY, fieldWidth,
                DROPDOWN_HEADER_HEIGHT, DROPDOWN_OPTION_HEIGHT, localMouseX, localMouseY, maxVisibleY);

        renderMenuPanel(guiGraphics, screen, localMouseX, localMouseY);

        guiGraphics.pose().translate(0, 0, 10);
        screenPositionDropdown.renderPopup(guiGraphics, minecraft.font, localMouseX, localMouseY);
        globalPositionDropdown.renderPopup(guiGraphics, minecraft.font, localMouseX, localMouseY);
        backpackModeDropdown.renderPopup(guiGraphics, minecraft.font, localMouseX, localMouseY);
        guiGraphics.pose().popPose();
    }

    private static int minimumPanelWidth() {
        var font = Minecraft.getInstance().font;
        int choiceWidth = font.width("Use global");
        int selectedWidth = 0;
        for (TabConfig.TabPosition position : POSITION_OPTIONS) {
            choiceWidth = Math.max(choiceWidth, font.width(positionLabel(position)));
            selectedWidth = Math.max(selectedWidth, font.width("Selected: " + positionLabel(position)));
        }

        for (TabConfig.BackpackTabMode mode : TabConfig.BackpackTabMode.values()) {
            choiceWidth = Math.max(choiceWidth, font.width(backpackModeLabel(mode)));
        }

        int footerWidth =2 * Math.max(font.width("Save"), font.width("Cancel")) + 20;
        return Math.max(104, 12 + Math.max(choiceWidth + 18, Math.max(footerWidth, selectedWidth)));
    }

    private static String fitLabel(String text, int width) {
        var font = Minecraft.getInstance().font;
        return font.width(text) <= width ? text : font.plainSubstrByWidth(text, width - font.width("...")) + "...";
    }

    private static TabConfig.TabPosition effectivePosition() {
        return pendingScreenPosition != null ? pendingScreenPosition : pendingGlobalPosition;
    }

    private static String positionLabel(TabConfig.TabPosition position) {
        return switch (position) {
            case TOP_LEFT -> "Top left";
            case TOP_RIGHT -> "Top right";
            case BOTTOM_LEFT -> "Bottom left";
            case BOTTOM_RIGHT -> "Bottom right";
        };
    }

    private static String backpackModeLabel(TabConfig.BackpackTabMode mode) {
        return switch (mode) {
            case SINGLE_WORN -> "Single worn backpack";
            case ANY -> "Any backpack";
            case INCEPTION_ONLY -> "Inception upgrade only";
            case ANY_INCEPTION -> "Any backpack with inception";
        };
    }

    private static void cyclePosition() {
        TabConfig.TabPosition[] positions = TabConfig.TabPosition.values();
        selectedPosition = positions[(selectedPosition.ordinal() + 1) % positions.length];
    }

    private static int stepSize() {
        return Screen.hasShiftDown() ? 5 : 1;
    }

    private static void moveSelected(int dx, int dy) {
        TabConfig.ScreenOffset current = getOffset(selectedPosition);
        pendingOffsets.put(selectedPosition, new TabConfig.ScreenOffset(current.x + dx, current.y + dy));
    }

    private static void resetScreenOffsets() {
        pendingOffsets.put(selectedPosition, TabConfig.ScreenOffset.ZERO);
    }

    private static TabConfig.TabPosition getHoveredPosition(AbstractContainerScreen<?> screen, double mouseX, double mouseY) {
        double localMouseX = mouseX - ScreenAccess.guiLeft(screen);
        double localMouseY = mouseY - ScreenAccess.guiTop(screen);

        var tabs = TabManager.getActiveTabsView();
        for (TabConfig.TabPosition position : TabConfig.TabPosition.values()) {
            for (int i = 0; i < tabs.size(); i++) {
                boolean active = tabs.get(i).isActive();
                TabBounds bounds = TabRenderer.getTabBounds(i, position, screen, getOffset(position), active);
                if (bounds.contains(localMouseX, localMouseY)) {
                    return position;
                }
            }
        }
        return null;
    }

    private static void save() {
        for (Map.Entry<TabConfig.TabPosition, TabConfig.ScreenOffset> entry : pendingOffsets.entrySet()) {
            TabConfig.setScreenOffset(screenClassName, entry.getKey(), entry.getValue());
        }

        if (pendingScreenPosition == null) {
            TabConfig.removeScreenPositionOverride(screenClassName);
        } else {
            TabConfig.setScreenPositionOverride(screenClassName, pendingScreenPosition);
        }

        if (pendingGlobalPosition != TabConfig.getTabPosition()) {
            TabConfig.setGlobalTabPosition(pendingGlobalPosition);
        }

        boolean modeChanged = pendingBackpackMode != TabConfig.getBackpackTabMode();
        if (modeChanged) {
            TabConfig.setBackpackTabMode(pendingBackpackMode);
        }

        TabConfig.setMenuEnabled(new HashMap<>(pendingMenus));
        TabConfig.saveOffsets();
        TabRenderer.invalidateCache();
        cancel();
        if (modeChanged && Minecraft.getInstance().player != null) {
            TabManager.refreshTabsForCurrentScreen(Minecraft.getInstance().player);
        }
        sendMessage("Saved offsets & position for " + screenClassName);
    }

    private static void sendMessage(String message) {
        if (Minecraft.getInstance().player != null) {
            Minecraft.getInstance().player.sendSystemMessage(Component.literal(message));
        }
    }
}
