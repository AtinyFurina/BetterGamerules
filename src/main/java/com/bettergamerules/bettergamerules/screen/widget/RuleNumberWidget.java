package com.bettergamerules.bettergamerules.screen.widget;

import com.bettergamerules.bettergamerules.network.C2SSyncGamerulePacket;
import com.bettergamerules.bettergamerules.network.ModNetwork;
import com.bettergamerules.bettergamerules.util.GameruleHelper;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.network.chat.Component;
import net.minecraftforge.network.PacketDistributor;
import org.lwjgl.glfw.GLFW;

import java.util.regex.Pattern;

/**
 * A combined slider + text input widget for integer game rules.
 * Supports both vanilla and modded integer gamerules.
 *
 * Two input modes, always visible and bidirectionally synced:
 *   1. Slider (left)  — Quick/drag adjustment for rough values
 *   2. Input box (right) — Type exact value for precision
 *
 * Debounce strategy:
 *   - Slider: updates input box live during drag, sends packet on mouse RELEASE and click
 *   - Input box: updates slider live during typing, sends packet on ENTER key
 *
 * Uses shared clamp helpers from GameruleHelper for Java 17 compat.
 */
public class RuleNumberWidget extends AbstractWidget {

    public static final int TOTAL_WIDTH = 116;
    public static final int WIDGET_HEIGHT = 18;

    private final String ruleId;
    private int value;
    private final int minValue;
    private final int maxValue;
    private final Font font;

    // Layout
    private final int inputWidth;
    private final int sliderWidth;
    private static final int GAP = 4;
    private static final int EDITBOX_DX = 3;
    private static final int EDITBOX_DY = 5;

    // Colors (using shared palette from GameruleHelper)
    private static final int GRAY_TRACK = GameruleHelper.COLOR_GRAY;
    private static final int GREEN_TRACK = GameruleHelper.COLOR_GREEN;
    private static final int WHITE_HANDLE = 0xFFFFFFFF;
    private static final int INPUT_BG = GameruleHelper.COLOR_PANEL_BG;
    private static final int INPUT_BORDER = GameruleHelper.COLOR_GRAY;
    private static final int FOCUS_BORDER = GameruleHelper.COLOR_GREEN;

    // Sub-widgets
    private final EditBox inputBox;
    private boolean inputFocused = false;
    private boolean dragging = false;

    // Cached display value to avoid per-frame String.valueOf + font.width
    private String cachedValueStr;
    private int cachedValue = Integer.MIN_VALUE;

    // Precompiled regex for input filtering
    private static final Pattern DIGITS_PATTERN = Pattern.compile("-?\\d*");

    public RuleNumberWidget(int x, int y, String ruleId, int initialValue,
                            int min, int max, Font font) {
        super(x, y, TOTAL_WIDTH, WIDGET_HEIGHT, Component.empty());
        this.ruleId = ruleId;
        this.minValue = min;
        this.maxValue = max;
        this.font = font;

        // Dynamic input width based on value magnitude
        this.inputWidth = GameruleHelper.getInputWidth(ruleId, initialValue);
        this.sliderWidth = TOTAL_WIDTH - GAP - this.inputWidth;
        this.value = GameruleHelper.clamp(initialValue, min, max);
        this.cachedValueStr = String.valueOf(this.value);
        this.cachedValue = this.value;

        // Create the text input box (accounting for DX inset)
        this.inputBox = new EditBox(
                font,
                x + this.sliderWidth + GAP + EDITBOX_DX, y + EDITBOX_DY,
                this.inputWidth - EDITBOX_DX, WIDGET_HEIGHT,
                Component.empty()
        );
        this.inputBox.setValue(this.cachedValueStr);
        this.inputBox.setMaxLength(10);
        this.inputBox.setFilter(text -> text == null || text.isEmpty()
                || DIGITS_PATTERN.matcher(text).matches());
        this.inputBox.setResponder(this::onInputChanged);
        this.inputBox.setBordered(false);
        this.inputBox.setTextColor(GameruleHelper.COLOR_TEXT_DIM);
    }

    // ========== Slider Rendering ==========

    @Override
    protected void renderWidget(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
        // 1. Slider track — 6px thick, centered
        int trackY = getY() + 6;
        int trackH = 6;
        g.fill(getX(), trackY, getX() + this.sliderWidth, trackY + trackH, GRAY_TRACK);

        // 2. Slider progress (green fill)
        float progress = (maxValue > minValue)
                ? (float)(value - minValue) / (maxValue - minValue)
                : 0f;
        int fillWidth = (int)(this.sliderWidth * progress);
        if (fillWidth > 0) {
            g.fill(getX(), trackY, getX() + fillWidth, trackY + trackH, GREEN_TRACK);
        }

        // 3. Slider handle — 10px, centered on track
        int handleSize = 10;
        int handleX = getX() + fillWidth - handleSize / 2;
        handleX = GameruleHelper.clamp(handleX, getX(), getX() + this.sliderWidth - handleSize);
        g.fill(handleX, getY() + 4, handleX + handleSize, getY() + 4 + handleSize, WHITE_HANDLE);

        // 4. Input box area
        int inputX = getX() + this.sliderWidth + GAP;
        g.fill(inputX, getY(), inputX + this.inputWidth, getY() + WIDGET_HEIGHT, INPUT_BG);
        int borderColor = inputFocused ? FOCUS_BORDER : INPUT_BORDER;
        g.renderOutline(inputX, getY(), this.inputWidth, WIDGET_HEIGHT, borderColor);

        if (inputFocused) {
            this.inputBox.render(g, mouseX, mouseY, partialTick);
        } else {
            // Update cached display string if value changed
            if (this.value != this.cachedValue) {
                this.cachedValueStr = String.valueOf(this.value);
                this.cachedValue = this.value;
            }
            int textW = font.width(this.cachedValueStr);
            int cx = inputX + this.inputWidth / 2;
            int cy = getY() + (WIDGET_HEIGHT - 8) / 2;
            g.drawString(font, this.cachedValueStr, cx - textW / 2, cy, GameruleHelper.COLOR_TEXT_DIM);
        }
    }

    // ========== Slider Interaction ==========

    @Override
    public void onClick(double mouseX, double mouseY) {
        if (mouseX >= getX() && mouseX < getX() + this.sliderWidth) {
            // Clear input focus when clicking slider
            if (this.inputFocused) {
                this.inputFocused = false;
                this.inputBox.setFocused(false);
            }
            updateValueFromSlider((int) mouseX);
            sendUpdatePacket(); // FIXED: click on slider now syncs immediately
        } else {
            this.inputBox.setFocused(true);
            this.inputFocused = true;
        }
    }

    @Override
    public void onDrag(double mouseX, double mouseY, double dragX, double dragY) {
        if (mouseX >= getX() && mouseX < getX() + this.sliderWidth) {
            updateValueFromSlider((int) mouseX);
            this.dragging = true;
        }
    }

    @Override
    public void onRelease(double mouseX, double mouseY) {
        super.onRelease(mouseX, mouseY);
        if (this.dragging) {
            this.dragging = false;
            sendUpdatePacket();
        }
    }

    // FIXED: clear inputFocused when widget loses focus
    @Override
    public void setFocused(boolean focused) {
        super.setFocused(focused);
        if (!focused && this.inputFocused) {
            this.inputFocused = false;
            this.inputBox.setFocused(false);
            this.inputBox.setValue(String.valueOf(this.value));
        }
    }

    private void updateValueFromSlider(int mouseX) {
        float progress = (float)(mouseX - getX()) / this.sliderWidth;
        progress = GameruleHelper.clamp(progress, 0f, 1f);
        int newValue = minValue + Math.round(progress * (maxValue - minValue));
        newValue = GameruleHelper.clamp(newValue, minValue, maxValue);

        if (newValue != this.value) {
            this.value = newValue;
            this.cachedValueStr = String.valueOf(newValue);
            this.cachedValue = newValue;
            this.inputBox.setValue(this.cachedValueStr);
        }
    }

    // ========== Input Box Interaction ==========

    private void onInputChanged(String text) {
        if (text == null || text.isEmpty()) return;
        try {
            int newValue = Integer.parseInt(text);
            this.value = GameruleHelper.clamp(newValue, minValue, maxValue);
            this.cachedValueStr = String.valueOf(this.value);
            this.cachedValue = this.value;
        } catch (NumberFormatException e) {
            // Intermediate state during typing (e.g. user typed "-")
        }
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (this.inputFocused) {
            if (keyCode == GLFW.GLFW_KEY_ENTER || keyCode == GLFW.GLFW_KEY_KP_ENTER) {
                this.inputBox.setFocused(false);
                this.inputFocused = false;
                clampAndSend();
                return true;
            }
            if (keyCode == GLFW.GLFW_KEY_ESCAPE || keyCode == GLFW.GLFW_KEY_TAB) {
                this.inputBox.setFocused(false);
                this.inputFocused = false;
                this.inputBox.setValue(String.valueOf(this.value));
                return true;
            }
            return this.inputBox.keyPressed(keyCode, scanCode, modifiers);
        }
        return false;
    }

    @Override
    public boolean charTyped(char codePoint, int modifiers) {
        if (this.inputFocused) {
            return this.inputBox.charTyped(codePoint, modifiers);
        }
        return false;
    }

    private void clampAndSend() {
        String text = this.inputBox.getValue();
        if (text.isEmpty()) {
            this.inputBox.setValue(String.valueOf(this.value));
            return;
        }
        try {
            int parsed = Integer.parseInt(text);
            this.value = GameruleHelper.clamp(parsed, minValue, maxValue);
            this.cachedValueStr = String.valueOf(this.value);
            this.cachedValue = this.value;
            this.inputBox.setValue(this.cachedValueStr);
            sendUpdatePacket();
        } catch (NumberFormatException e) {
            this.inputBox.setValue(String.valueOf(this.value));
        }
    }

    private void sendUpdatePacket() {
        ModNetwork.CHANNEL.send(
                PacketDistributor.SERVER.noArg(),
                new C2SSyncGamerulePacket(ruleId, Integer.toString(value), "integer")
        );
    }

    // ========== Position — keep internal EditBox in sync ==========

    @Override
    public void setX(int x) {
        super.setX(x);
        this.inputBox.setX(x + this.sliderWidth + GAP + EDITBOX_DX);
    }

    @Override
    public void setY(int y) {
        super.setY(y);
        this.inputBox.setY(y + EDITBOX_DY);
    }

    // ========== Public API ==========

    public int getValue() {
        return value;
    }

    public void setValue(int newValue) {
        this.value = GameruleHelper.clamp(newValue, minValue, maxValue);
        this.cachedValueStr = String.valueOf(this.value);
        this.cachedValue = this.value;
        this.inputBox.setValue(this.cachedValueStr);
    }

    @Override
    protected void updateWidgetNarration(NarrationElementOutput narration) {
        this.defaultButtonNarrationText(narration);
    }
}
