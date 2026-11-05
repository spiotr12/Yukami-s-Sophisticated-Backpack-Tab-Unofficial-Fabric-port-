package com.yukami.backpacktab.client.keybind;

import com.mojang.blaze3d.platform.InputConstants;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.minecraft.client.KeyMapping;
import org.lwjgl.glfw.GLFW;

public final class KeyBindings {
    private KeyBindings() {}

    public static final String KEY_CATEGORY = "key.categories.yukamibackpacktab";
    public static final String KEY_TOGGLE_OFFSET_EDITOR = "key.yukamibackpacktab.toggle_offset_editor";

    public static final String KEY_SWITCH_TAB = "key.yukamibackpacktab.switch_tab";

    public static final String KEY_PREVIOUS_TAB = "key.yukamibackpacktab.previous_tab";

    public static KeyMapping toggleOffsetEditorKey;
    public static KeyMapping switchTabKey;
    public static KeyMapping previousTabKey;

    public static void register() {
        previousTabKey = KeyBindingHelper.registerKeyBinding(new KeyMapping(
            KEY_PREVIOUS_TAB,
            InputConstants.Type.MOUSE,
            GLFW.GLFW_MOUSE_BUTTON_4,
            KEY_CATEGORY
        ));

        switchTabKey = KeyBindingHelper.registerKeyBinding(new KeyMapping(
            KEY_SWITCH_TAB,
            InputConstants.Type.MOUSE,
            GLFW.GLFW_MOUSE_BUTTON_5,
            KEY_CATEGORY
        ));

        toggleOffsetEditorKey = KeyBindingHelper.registerKeyBinding(new KeyMapping(
            KEY_TOGGLE_OFFSET_EDITOR,
            InputConstants.Type.KEYSYM,
            GLFW.GLFW_KEY_F9,
            KEY_CATEGORY
        ));
    }
}
