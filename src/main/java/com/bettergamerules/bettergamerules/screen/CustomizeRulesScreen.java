package com.bettergamerules.bettergamerules.screen;

import com.bettergamerules.bettergamerules.config.ClientConfig;
import com.bettergamerules.bettergamerules.util.GameruleHelper;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * Screen for customizing which game rules appear in Simple Mode.
 * Two-column layout: left = all rules, right = selected rules.
 * Uses Java 17 compatible clamp helper.
 */
public class CustomizeRulesScreen extends Screen {

    private static final Component TITLE = Component.translatable("screen.bettergamerules.customize_title");

    private final GameruleScreen parentScreen;

    private List<String> allRuleIds = new ArrayList<>();
    private List<String> selectedRuleIds;

    private static final int COLUMN_WIDTH = 140;
    private static final int ENTRY_HEIGHT = 20;
    private static final int VISIBLE_ENTRIES = 10;

    private double leftScroll = 0;
    private double rightScroll = 0;

    private static double clamp(double val, double min, double max) {
        return Math.max(min, Math.min(max, val));
    }

    public CustomizeRulesScreen(GameruleScreen parentScreen) {
        super(TITLE);
        this.parentScreen = parentScreen;
        this.selectedRuleIds = new ArrayList<>(ClientConfig.getSimpleModeRules());
    }

    @Override
    protected void init() {
        super.init();

        this.allRuleIds = new ArrayList<>(parentScreen.getGameruleData().keySet());
        this.allRuleIds.sort(Comparator.naturalOrder());

        int btnY = this.height - 28;
        int leftColX = (this.width - COLUMN_WIDTH * 2 - 10) / 2;

        // Reset default button
        this.addRenderableWidget(Button.builder(
                Component.translatable("screen.bettergamerules.reset_default"),
                btn -> resetToDefault()
        ).pos(leftColX, btnY).size(60, 20).build());

        // Done button
        this.addRenderableWidget(Button.builder(
                CommonComponents.GUI_DONE,
                btn -> saveAndClose()
        ).pos(leftColX + COLUMN_WIDTH * 2 - 40, btnY).size(60, 20).build());
    }

    @Override
    public void render(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
        this.renderBackground(g);

        g.drawCenteredString(this.font, this.title, this.width / 2, 10, 0xFFFFFF);

        int colsStartX = (this.width - COLUMN_WIDTH * 2 - 10) / 2;
        int headerY = 30;
        g.drawCenteredString(this.font,
                Component.translatable("screen.bettergamerules.all_rules"),
                colsStartX + COLUMN_WIDTH / 2, headerY, 0xAAAAAA);
        g.drawCenteredString(this.font,
                Component.translatable("screen.bettergamerules.selected_rules"),
                colsStartX + COLUMN_WIDTH + 10 + COLUMN_WIDTH / 2, headerY, 0xAAAAAA);

        renderRuleColumn(g, colsStartX, 42, COLUMN_WIDTH, allRuleIds,
                selectedRuleIds, true, leftScroll);
        renderRuleColumn(g, colsStartX + COLUMN_WIDTH + 10, 42, COLUMN_WIDTH, selectedRuleIds,
                selectedRuleIds, false, rightScroll);

        super.render(g, mouseX, mouseY, partialTick);
    }

    private void renderRuleColumn(GuiGraphics g, int colX, int colY, int colWidth,
                                   List<String> ruleIds, List<String> selected,
                                   boolean showCheckbox, double scroll) {
        int maxScroll = Math.max(0, ruleIds.size() - VISIBLE_ENTRIES);
        scroll = clamp(scroll, 0, maxScroll);
        int startIndex = (int) scroll;

        for (int i = 0; i < Math.min(ruleIds.size() - startIndex, VISIBLE_ENTRIES); i++) {
            int idx = startIndex + i;
            String ruleId = ruleIds.get(idx);
            int entryY = colY + i * ENTRY_HEIGHT;

            boolean isSelected = selected.contains(ruleId);
            if (showCheckbox && isSelected) {
                g.fill(colX, entryY, colX + colWidth, entryY + ENTRY_HEIGHT - 1, 0x405B8731);
            }

            Component name = GameruleHelper.getDisplayName(ruleId);
            int textColor = isSelected ? 0x5B8731 : 0xCCCCCC;

            if (showCheckbox) {
                String marker = isSelected ? "✓ " : "☐ ";
                g.drawString(this.font, marker + name.getString(),
                        colX + 4, entryY + 5, textColor);
            } else {
                String label = (idx + 1) + ". " + name.getString();
                g.drawString(this.font, label, colX + 4, entryY + 5, 0xCCCCCC);
            }
        }

        if (maxScroll > 0) {
            int barH = (int)((VISIBLE_ENTRIES / (float) ruleIds.size()) * (VISIBLE_ENTRIES * ENTRY_HEIGHT));
            int barY = colY + (int)(scroll / maxScroll * (VISIBLE_ENTRIES * ENTRY_HEIGHT - barH));
            g.fill(colX + colWidth - 4, barY, colX + colWidth - 2, barY + barH, 0xFF888888);
        }
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        int colsStartX = (this.width - COLUMN_WIDTH * 2 - 10) / 2;
        int colY = 42;

        // Left column clicks
        if (mouseX >= colsStartX && mouseX <= colsStartX + COLUMN_WIDTH
                && mouseY >= colY && mouseY <= colY + VISIBLE_ENTRIES * ENTRY_HEIGHT) {
            int idx = (int) leftScroll + (int)(mouseY - colY) / ENTRY_HEIGHT;
            if (idx >= 0 && idx < allRuleIds.size()) {
                String ruleId = allRuleIds.get(idx);
                if (selectedRuleIds.contains(ruleId)) {
                    selectedRuleIds.remove(ruleId);
                } else {
                    selectedRuleIds.add(ruleId);
                }
            }
        }

        // Right column clicks
        int rightColX = colsStartX + COLUMN_WIDTH + 10;
        if (mouseX >= rightColX && mouseX <= rightColX + COLUMN_WIDTH
                && mouseY >= colY && mouseY <= colY + VISIBLE_ENTRIES * ENTRY_HEIGHT) {
            int idx = (int) rightScroll + (int)(mouseY - colY) / ENTRY_HEIGHT;
            if (idx >= 0 && idx < selectedRuleIds.size()) {
                selectedRuleIds.remove(idx);
            }
        }

        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double delta) {
        int colsStartX = (this.width - COLUMN_WIDTH * 2 - 10) / 2;
        if (mouseX < colsStartX + COLUMN_WIDTH) {
            leftScroll = clamp(leftScroll - delta, 0, Math.max(0, allRuleIds.size() - VISIBLE_ENTRIES));
        } else {
            rightScroll = clamp(rightScroll - delta, 0, Math.max(0, selectedRuleIds.size() - VISIBLE_ENTRIES));
        }
        return true;
    }

    private void resetToDefault() {
        this.selectedRuleIds = new ArrayList<>(List.of(
                "doFireTick", "keepInventory", "randomTickSpeed",
                "doDaylightCycle", "doMobSpawning", "doWeatherCycle",
                "mobGriefing", "commandBlockOutput", "doInsomnia",
                "disableRaids", "playersSleepingPercentage", "doTraderSpawning"
        ));
    }

    private void saveAndClose() {
        if (!selectedRuleIds.isEmpty()) {
            ClientConfig.setSimpleModeRules(selectedRuleIds);
        }
        this.minecraft.setScreen(new GameruleScreen());
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}
