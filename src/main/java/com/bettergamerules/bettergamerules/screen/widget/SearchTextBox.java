package com.bettergamerules.bettergamerules.screen.widget;

import com.bettergamerules.bettergamerules.util.GameruleHelper;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.network.chat.Component;

/**
 * A styled text box for searching game rules in Advanced Mode.
 * Wraps Minecraft's EditBox with custom styling.
 */
public class SearchTextBox extends EditBox {

    private static final int MAX_SEARCH_LENGTH = 50;

    public SearchTextBox(Font font, int x, int y, int width, int height, Component hint) {
        super(font, x, y, width, height, hint);

        this.setBordered(true);
        this.setMaxLength(MAX_SEARCH_LENGTH);
        this.setTextColor(GameruleHelper.COLOR_TEXT_DIM);
    }
}
