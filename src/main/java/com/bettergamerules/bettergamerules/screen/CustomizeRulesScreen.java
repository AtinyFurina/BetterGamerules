package com.bettergamerules.bettergamerules.screen;

import com.bettergamerules.bettergamerules.config.ClientConfig;
import com.bettergamerules.bettergamerules.util.GameruleHelper;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;

import java.util.*;

/**
 * Screen for customizing which game rules appear in Simple Mode.
 * Two-column layout: left = all rules, right = selected rules.
 */
public class CustomizeRulesScreen extends Screen {

    private static final Component TITLE = Component.translatable("screen.bettergamerules.customize_title");

    private final GameruleScreen parentScreen;

    private List<String> allRuleIds = new ArrayList<>();
    private List<String> selectedRuleIds;
    private Set<String> selectedSet = new HashSet<>(); // O(1) lookup for render

    private static final int COLUMN_WIDTH = 140;
    private static final int ENTRY_HEIGHT = 20;
    private static final int VISIBLE_ENTRIES = 10;

    private int leftScroll = 0;
    private int rightScroll = 0;

    // Cached display names
    private Map<String, String> displayNameCache = Map.of();

    public CustomizeRulesScreen(GameruleScreen parentScreen) {
        super(TITLE);
        this.parentScreen = parentScreen;
        this.selectedRuleIds = new ArrayList<>(ClientConfig.getSimpleModeRules());
    }

    @Override
    protected void init() {
        this.allRuleIds = new ArrayList<>(parentScreen.getGameruleData().keySet());
        this.allRuleIds.sort(Comparator.naturalOrder());

        // Build selected set and display name cache
        this.selectedSet = new HashSet<>(this.selectedRuleIds);
        Map<String, String> names = new HashMap<>();
        for (String id : allRuleIds) {
            names.put(id, GameruleHelper.getDisplayNameString(id));
        }
        this.displayNameCache = names;

        int btnY = this.height - 28;
        int leftColX = getColsStartX();

        // Reset default button
        this.addRenderableWidget(Button.builder(
                Component.translatable("screen.bettergamerules.reset_default"),
                btn -> resetToDefault()
        ).pos(leftColX, btnY).size(60, 20).build());

        // Done button — positioned to the right of the right column
        int rightColRight = leftColX + COLUMN_WIDTH * 2 + 10;
        this.addRenderableWidget(Button.builder(
                CommonComponents.GUI_DONE,
                btn -> saveAndClose()
        ).pos(rightColRight - 60, btnY).size(60, 20).build());
    }

    private int getColsStartX() {
        return (this.width - COLUMN_WIDTH * 2 - 10) / 2;
    }

    private int getColsTop() {
        return 42;
    }

    @Override
    public void render(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
        this.renderBackground(g);

        g.drawCenteredString(this.font, this.title, this.width / 2, 10, GameruleHelper.COLOR_TEXT_WHITE);

        int colsStartX = getColsStartX();
        int headerY = 30;
        g.drawCenteredString(this.font,
                Component.translatable("screen.bettergamerules.all_rules"),
                colsStartX + COLUMN_WIDTH / 2, headerY, 0xAAAAAA);
        g.drawCenteredString(this.font,
                Component.translatable("screen.bettergamerules.selected_rules"),
                colsStartX + COLUMN_WIDTH + 10 + COLUMN_WIDTH / 2, headerY, 0xAAAAAA);

        renderRuleColumn(g, colsStartX, getColsTop(), COLUMN_WIDTH, allRuleIds,
                true, leftScroll);
        renderRuleColumn(g, colsStartX + COLUMN_WIDTH + 10, getColsTop(), COLUMN_WIDTH,
                selectedRuleIds, false, rightScroll);

        super.render(g, mouseX, mouseY, partialTick);
    }

    private void renderRuleColumn(GuiGraphics g, int colX, int colY, int colWidth,
                                   List<String> ruleIds, boolean showCheckbox, int scroll) {
        int maxScroll = Math.max(0, ruleIds.size() - VISIBLE_ENTRIES);
        scroll = GameruleHelper.clamp(scroll, 0, maxScroll);
        int startIndex = scroll;

        for (int i = 0; i < Math.min(ruleIds.size() - startIndex, VISIBLE_ENTRIES); i++) {
            int idx = startIndex + i;
            String ruleId = ruleIds.get(idx);
            int entryY = colY + i * ENTRY_HEIGHT;

            boolean isSelected = showCheckbox && selectedSet.contains(ruleId);
            if (isSelected) {
                g.fill(colX, entryY, colX + colWidth, entryY + ENTRY_HEIGHT - 1, 0x405B8731);
            }

            String name = displayNameCache.getOrDefault(ruleId, ruleId);
            int textColor = isSelected ? 0x5B8731 : 0xCCCCCC;

            if (showCheckbox) {
                String marker = isSelected ? "✓ " : "☐ ";
                g.drawString(this.font, marker + name, colX + 4, entryY + 5, textColor);
            } else {
                String label = (idx + 1) + ". " + name;
                g.drawString(this.font, label, colX + 4, entryY + 5, 0xCCCCCC);
            }
        }

        // Scrollbar using shared helper
        GameruleScreen.drawScrollbar(g, colX + colWidth - 4, colY,
                VISIBLE_ENTRIES * ENTRY_HEIGHT, ruleIds.size(), VISIBLE_ENTRIES,
                ENTRY_HEIGHT, scroll, maxScroll);
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        int colsStartX = getColsStartX();
        int colY = getColsTop();

        // Left column clicks
        if (mouseX >= colsStartX && mouseX <= colsStartX + COLUMN_WIDTH
                && mouseY >= colY && mouseY <= colY + VISIBLE_ENTRIES * ENTRY_HEIGHT) {
            int idx = leftScroll + (int)(mouseY - colY) / ENTRY_HEIGHT;
            if (idx >= 0 && idx < allRuleIds.size()) {
                String ruleId = allRuleIds.get(idx);
                if (selectedSet.contains(ruleId)) {
                    selectedSet.remove(ruleId);
                    selectedRuleIds.remove(ruleId);
                } else {
                    selectedSet.add(ruleId);
                    selectedRuleIds.add(ruleId);
                }
            }
        }

        // Right column clicks
        int rightColX = colsStartX + COLUMN_WIDTH + 10;
        if (mouseX >= rightColX && mouseX <= rightColX + COLUMN_WIDTH
                && mouseY >= colY && mouseY <= colY + VISIBLE_ENTRIES * ENTRY_HEIGHT) {
            int idx = rightScroll + (int)(mouseY - colY) / ENTRY_HEIGHT;
            if (idx >= 0 && idx < selectedRuleIds.size()) {
                String removed = selectedRuleIds.remove(idx);
                selectedSet.remove(removed);
            }
        }

        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double delta) {
        int colsStartX = getColsStartX();
        if (mouseX < colsStartX + COLUMN_WIDTH) {
            leftScroll = GameruleHelper.clamp(leftScroll - (int) delta, 0,
                    Math.max(0, allRuleIds.size() - VISIBLE_ENTRIES));
            return true;
        } else if (mouseX >= colsStartX + COLUMN_WIDTH + 10
                && mouseX < colsStartX + COLUMN_WIDTH * 2 + 10) {
            rightScroll = GameruleHelper.clamp(rightScroll - (int) delta, 0,
                    Math.max(0, selectedRuleIds.size() - VISIBLE_ENTRIES));
            return true;
        }
        return false;
    }

    private void resetToDefault() {
        this.selectedRuleIds = ClientConfig.getDefaultRules();
        this.selectedSet = new HashSet<>(this.selectedRuleIds);
    }

    private void saveAndClose() {
        if (!selectedRuleIds.isEmpty()) {
            ClientConfig.setSimpleModeRules(selectedRuleIds);
        }
        // FIXED: use parent screen to preserve fetched data, avoid extra network request
        this.minecraft.setScreen(parentScreen);
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}
