package com.yukami.backpacktab.client.keybind;

import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.fabricmc.fabric.api.client.screen.v1.ScreenEvents;
import net.fabricmc.fabric.api.client.screen.v1.ScreenKeyboardEvents;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.player.LocalPlayer;

import com.yukami.backpacktab.client.gui.TabOffsetEditor;

public final class KeyInputHandler {
    private KeyInputHandler() {}

    public static void register() {
        ScreenEvents.BEFORE_INIT.register((client, screen, scaledWidth, scaledHeight) ->
                ScreenKeyboardEvents.allowKeyPress(screen).register(KeyInputHandler::allowKeyPress));
    }

    /** @return false to cancel the key press. */
    private static boolean allowKeyPress(Screen screen, int keyCode, int scanCode, int modifiers) {
        Minecraft minecraft = Minecraft.getInstance();
        LocalPlayer player = minecraft.player;

        if (player == null || minecraft.screen == null) {
            return true;
        }

        if (TabOffsetEditor.handleKeyPressed(keyCode)) {
            return false;
        }

        if (KeyBindings.toggleOffsetEditorKey != null &&
            keyCode == KeyBindingHelper.getBoundKeyOf(KeyBindings.toggleOffsetEditorKey).getValue() &&
            minecraft.screen instanceof AbstractContainerScreen<?>) {
            TabOffsetEditor.toggle(minecraft.screen);
            return false;
        }
        return true;
    }
}
