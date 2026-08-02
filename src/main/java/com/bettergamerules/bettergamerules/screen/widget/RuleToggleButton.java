package com.bettergamerules.bettergamerules.screen.widget;

import com.bettergamerules.bettergamerules.network.C2SSyncGamerulePacket;
import com.bettergamerules.bettergamerules.network.ModNetwork;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.network.chat.Component;
import net.minecraftforge.network.PacketDistributor;

/**
 * Vanilla-style toggle button for boolean game rules.
 * Renders as a small rectangular button with text and colored indicator.
 */
public class RuleToggleButton extends AbstractWidget {

    private final String ruleId;
    private boolean value;

    private static final int W = 56;
    private static final int H = 16;

    public RuleToggleButton(int x, int y, String ruleId, boolean initialValue) {
        super(x, y, W, H, Component.empty());
        this.ruleId = ruleId;
        this.value = initialValue;
    }

    @Override
    public void onClick(double mx, double my) {
        this.value = !this.value;
        ModNetwork.CHANNEL.send(PacketDistributor.SERVER.noArg(),
            new C2SSyncGamerulePacket(ruleId, Boolean.toString(value), "boolean"));
    }

    @Override
    protected void renderWidget(GuiGraphics g, int mx, int my, float pt) {
        boolean hovered = isMouseOver(mx, my);

        // Background — vanilla button style with darker/lighter states
        int bg;
        if (value) {
            bg = hovered ? 0xFF6B9F3F : 0xFF5B8731; // green (on), brighter when hovered
        } else {
            bg = hovered ? 0xFF777777 : 0xFF555555; // gray (off), brighter when hovered
        }
        g.fill(getX(), getY(), getX() + W, getY() + H, bg);

        // Subtle 1px border (top lighter, bottom darker — vanilla style)
        g.fill(getX(), getY(), getX() + W, getY() + 1, 0x40FFFFFF);
        g.fill(getX(), getY() + H - 1, getX() + W, getY() + H, 0x40000000);

        // Text — centered, localized
        String key = value ? "gamerule.bettergamerules.toggle.on" : "gamerule.bettergamerules.toggle.off";
        Component text = Component.translatable(key);
        var font = Minecraft.getInstance().font;
        int pw = font.width(text);
        int tx = getX() + (W - pw) / 2;
        int ty = getY() + (H - 8) / 2;
        g.drawString(font, text, tx, ty, 0xFFFFFF);
    }

    @Override
    protected void updateWidgetNarration(NarrationElementOutput n) {
        this.defaultButtonNarrationText(n);
    }

    public boolean getValue() { return value; }
    public void setValue(boolean v) { this.value = v; }
}
