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

/**
 * A combined slider + text input widget for integer game rules.
 *
 * Two input modes, always visible and bidirectionally synced:
 *   1. Slider (left)  — Quick/drag adjustment for rough values
 *   2. Input box (right) — Type exact value for precision
 *
 * Layout: [ ═══════●═══════ ] [  123  ]
 *          ─── slider 80px ──   ─input 32px─
 *
 * Debounce strategy:
 *   - Slider: updates input box live during drag, sends packet on mouse RELEASE
 *   - Input box: updates slider live during typing, sends packet on ENTER key
 *
 * Uses min/max/clamp helpers instead of Java 21's Math.clamp for Java 17 compat.
 */
public class RuleNumberWidget extends AbstractWidget {

    private final String ruleId;
    private int value;
    private final int minValue;
    private final int maxValue;
    private final Font font;

    // Layout
    private final int inputWidth;
    private final int sliderWidth;
    private final int totalWidth;
    private static final int GAP = 4;
    private static final int WIDGET_HEIGHT = 18;
    private static final int EDITBOX_DX = 3;
    private static final int EDITBOX_DY = 5;

    // Colors
    private static final int GRAY_TRACK = 0xFF555555;
    private static final int GREEN_TRACK = 0xFF5B8731;
    private static final int WHITE_HANDLE = 0xFFFFFFFF;
    private static final int INPUT_BG = 0xCC000000;
    private static final int INPUT_BORDER = 0xFF555555;
    private static final int FOCUS_BORDER = 0xFF5B8731;

    // Sub-widgets
    private final EditBox inputBox;
    private boolean inputFocused = false;
    private boolean dragging = false;

    // ===== Java 17 compatible clamp helpers =====
    private static int clamp(int val, int min, int max) {
        return Math.max(min, Math.min(max, val));
    }
    private static float clamp(float val, float min, float max) {
        return Math.max(min, Math.min(max, val));
    }
    private static double clamp(double val, double min, double max) {
        return Math.max(min, Math.min(max, val));
    }

    public RuleNumberWidget(int x, int y, String ruleId, int initialValue,
                            int min, int max, Font font) {
        super(x, y, 116, WIDGET_HEIGHT, Component.empty()); // fixed total width
        this.ruleId = ruleId;
        this.inputWidth = GameruleHelper.getInputWidth(ruleId);
        this.sliderWidth = 116 - GAP - this.inputWidth;
        this.totalWidth = 116;
        this.value = clamp(initialValue, min, max);
        this.minValue = min;
        this.maxValue = max;
        this.font = font;

        // Create the text input box with dynamic width
        this.inputBox = new EditBox(
                font,
                x + this.sliderWidth + GAP + EDITBOX_DX, y + EDITBOX_DY,
                this.inputWidth, WIDGET_HEIGHT,
                Component.empty()
        );
        this.inputBox.setValue(String.valueOf(this.value));
        this.inputBox.setMaxLength(10);
        // Only allow digits and an optional leading minus sign
        this.inputBox.setFilter(text -> text == null || text.isEmpty() || text.matches("-?\\d*"));
        // Sync slider position as user types
        this.inputBox.setResponder(this::onInputChanged);
        this.inputBox.setBordered(false);
        this.inputBox.setTextColor(0xE0E0E0);
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
        handleX = clamp(handleX, getX(), getX() + this.sliderWidth - handleSize);
        g.fill(handleX, getY() + 4, handleX + handleSize, getY() + 4 + handleSize, WHITE_HANDLE);

        // 4. Input box area
        int inputX = getX() + this.sliderWidth + GAP;
        g.fill(inputX, getY(), inputX + this.inputWidth, getY() + WIDGET_HEIGHT, INPUT_BG);
        int borderColor = inputFocused ? FOCUS_BORDER : INPUT_BORDER;
        g.renderOutline(inputX, getY(), this.inputWidth, WIDGET_HEIGHT, borderColor);

        if (inputFocused) {
            // Show EditBox for typing
            this.inputBox.render(g, mouseX, mouseY, partialTick);
        } else {
            // Show centered number text
            String text = String.valueOf(this.value);
            int textW = font.width(text);
            int cx = inputX + this.inputWidth / 2;
            int cy = getY() + (WIDGET_HEIGHT - 8) / 2;
            g.drawString(font, text, cx - textW / 2, cy, 0xE0E0E0);
        }
    }

    // ========== Slider Interaction (Fuzzy Adjustment) ==========

    @Override
    public void onClick(double mouseX, double mouseY) {
        if (mouseX >= getX() && mouseX < getX() + this.sliderWidth) {
            updateValueFromSlider((int) mouseX);
        } else {
            this.inputBox.setFocused(true);
            this.inputBox.mouseClicked(mouseX, mouseY, 0);
            this.inputFocused = true;
        }
    }

    // Made public so SimpleModeTab/AdvancedModeTab can delegate drag events
    @Override
    public void onDrag(double mouseX, double mouseY, double dragX, double dragY) {
        updateValueFromSlider((int) mouseX);
        this.dragging = true;
    }

    @Override
    public void onRelease(double mouseX, double mouseY) {
        super.onRelease(mouseX, mouseY);
        if (this.dragging) {
            this.dragging = false;
            sendUpdatePacket();
        }
    }

    private void updateValueFromSlider(int mouseX) {
        float progress = (float)(mouseX - getX()) / this.sliderWidth;
        progress = clamp(progress, 0f, 1f);
        int newValue = minValue + Math.round(progress * (maxValue - minValue));
        newValue = clamp(newValue, minValue, maxValue);

        if (newValue != this.value) {
            this.value = newValue;
            this.inputBox.setValue(String.valueOf(newValue));
        }
    }

    // ========== Input Box Interaction (Precise Adjustment) ==========

    private void onInputChanged(String text) {
        if (text == null || text.isEmpty()) return;
        try {
            int newValue = Integer.parseInt(text);
            this.value = clamp(newValue, minValue, maxValue);
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
            this.value = clamp(parsed, minValue, maxValue);
            this.inputBox.setValue(String.valueOf(this.value));
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
        this.value = clamp(newValue, minValue, maxValue);
        this.inputBox.setValue(String.valueOf(this.value));
    }

    @Override
    protected void updateWidgetNarration(NarrationElementOutput narration) {
        this.defaultButtonNarrationText(narration);
    }
}
