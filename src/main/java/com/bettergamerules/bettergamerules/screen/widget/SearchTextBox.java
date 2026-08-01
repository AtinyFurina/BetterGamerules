package com.bettergamerules.bettergamerules.screen.widget;

import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.network.chat.Component;

/**
 * A styled text box for searching game rules in Advanced Mode.
 * Wraps Minecraft's EditBox with custom styling.
 *
 * Note: In 1.20.1, EditBox does NOT have setHintColor() or setBgdColor().
 * Text colors are set via setTextColor() and setTextColorUneditable().
 * Background is handled by setBordered() which draws a dark border/bg.
 */
public class SearchTextBox extends EditBox {

    public SearchTextBox(Font font, int x, int y, int width, int height, Component hint) {
        super(font, x, y, width, height, hint);

        // Style configuration — using only methods that exist in 1.20.1 EditBox
        this.setBordered(true);
        this.setMaxLength(50);
        this.setHint(hint);
        this.setTextColor(0xE0E0E0);
    }
}
