package com.yukami.backpacktab;

import com.mojang.logging.LogUtils;
import com.yukami.backpacktab.client.gui.TabManager;
import com.yukami.backpacktab.client.keybind.KeyBindings;
import com.yukami.backpacktab.client.keybind.KeyInputHandler;
import com.yukami.backpacktab.config.TabConfig;
import net.fabricmc.api.ClientModInitializer;
import org.slf4j.Logger;

public class YukamiBackpackTab implements ClientModInitializer {
    public static final String MODID = "yukamibackpacktab";
    public static final Logger LOGGER = LogUtils.getLogger();

    @Override
    public void onInitializeClient() {
        TabConfig.loadClientConfig();
        TabConfig.initializeOffsets();

        KeyBindings.register();
        KeyInputHandler.register();
        TabManager.register();

        LOGGER.info("Yukami Backpack Tab mod initialized");
    }
}
