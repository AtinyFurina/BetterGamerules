package com.bettergamerules.bettergamerules.screen;

import com.bettergamerules.bettergamerules.config.ClientConfig;
import com.bettergamerules.bettergamerules.network.C2SRequestGamerulesPacket;
import com.bettergamerules.bettergamerules.network.ModNetwork;
import com.bettergamerules.bettergamerules.screen.widget.RuleNumberWidget;
import com.bettergamerules.bettergamerules.screen.widget.RuleToggleButton;
import com.bettergamerules.bettergamerules.screen.widget.SearchTextBox;
import com.bettergamerules.bettergamerules.util.GameruleHelper;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;
import net.minecraftforge.network.PacketDistributor;

import java.util.*;

/**
 * Main screen for editing game rules in-game.
 * All widgets registered via addRenderableWidget; Minecraft handles event dispatch.
 * Manual scroll by updating widget positions each frame.
 * Supports both vanilla and modded game rules.
 */
public class GameruleScreen extends Screen {

    private static final Component TITLE = Component.translatable("screen.bettergamerules.title");
    private static final int ENTRY_H = 30;
    private static final int VISIBLE = 5;

    // Panel
    private static final int PW = 300, PH = 186;
    private int px, py;

    // Tabs
    private int tab = 0;
    private Button btnSimple, btnAdv;

    // Search
    private SearchTextBox searchBox;
    private String filter = "";

    // Buttons
    private Button btnCustom, btnDone;

    // Data
    private Map<String, GameruleHelper.RuleData> allData = Map.of();

    // Per-entry widgets — created/destroyed on data/tab change (NOT on search keystroke)
    private final List<EntryWidgets> entries = new ArrayList<>();

    // Scroll
    private int scroll = 0;

    // Cached display names for performance (rebuilt when data/tab changes)
    private Map<String, String> displayNameCache = Map.of();
    private Map<String, String> descriptionCache = Map.of();

    private record EntryWidgets(String ruleId, AbstractWidget control, String type) {}

    // ===== Init =====

    public GameruleScreen() { super(TITLE); }

    @Override
    protected void init() {
        this.clearWidgets();
        this.entries.clear();

        this.px = (this.width - PW) / 2;
        this.py = (this.height - PH) / 2 + 5;

        // Tab buttons
        int ty = py - 24;
        this.btnSimple = Button.builder(
            Component.translatable("screen.bettergamerules.simple_mode"), b -> setTab(0)
        ).pos(px, ty).size(80, 20).build();
        this.btnAdv = Button.builder(
            Component.translatable("screen.bettergamerules.advanced_mode"), b -> setTab(1)
        ).pos(px + 82, ty).size(80, 20).build();
        addRenderableWidget(btnSimple);
        addRenderableWidget(btnAdv);

        // Search box
        this.searchBox = new SearchTextBox(font, px + 8, py + 6, PW - 16, 16,
            Component.translatable("screen.bettergamerules.search"));
        this.searchBox.setResponder(s -> { filter = s; /* no rebuild — filter in render */ });
        addRenderableWidget(searchBox);

        // Customize button
        this.btnCustom = Button.builder(
            Component.translatable("screen.bettergamerules.customize"),
            b -> minecraft.setScreen(new CustomizeRulesScreen(this))
        ).pos(px + 4, py + PH - 26).size(PW - 8, 20).build();
        addRenderableWidget(btnCustom);

        // Done button
        this.btnDone = Button.builder(CommonComponents.GUI_DONE, b -> onClose())
            .pos((width - 100) / 2, py + PH + 10).size(100, 20).build();
        addRenderableWidget(btnDone);

        updateUI();
        ModNetwork.CHANNEL.send(PacketDistributor.SERVER.noArg(), new C2SRequestGamerulesPacket());
    }

    // ===== Tabs =====

    private void setTab(int t) {
        tab = t; filter = ""; searchBox.setValue("");
        updateUI(); rebuildEntries();
    }

    private void updateUI() {
        boolean s = tab == 0;
        btnSimple.active = !s; btnAdv.active = s;
        searchBox.visible = !s;
        btnCustom.visible = s;
    }

    // ===== Data =====

    public void updateGamerules(Map<String, GameruleHelper.RuleData> data) {
        allData = new LinkedHashMap<>(data);
        rebuildDisplayCache();
        rebuildEntries();
    }

    public Map<String, GameruleHelper.RuleData> getGameruleData() { return allData; }

    /**
     * Pre-compute display names and descriptions to avoid per-frame translation lookups.
     */
    private void rebuildDisplayCache() {
        Map<String, String> names = new HashMap<>();
        Map<String, String> descs = new HashMap<>();
        for (String id : allData.keySet()) {
            names.put(id, GameruleHelper.getDisplayNameString(id));
            descs.put(id, GameruleHelper.getDescriptionString(id));
        }
        this.displayNameCache = names;
        this.descriptionCache = descs;
    }

    /**
     * Rebuild entry widgets. Called on data change or tab switch only —
     * search filtering happens in render() via visibility toggle.
     */
    private void rebuildEntries() {
        if (allData.isEmpty()) return;

        // Remove old entry widgets
        for (EntryWidgets ew : entries) {
            removeWidget(ew.control);
        }
        entries.clear();

        // Build display order
        List<String> ids;
        if (tab == 0) {
            ids = new ArrayList<>(ClientConfig.getSimpleModeRules());
        } else {
            ids = new ArrayList<>(allData.keySet());
            ids.sort(Comparator.naturalOrder());
        }

        // Create widgets for each entry
        for (String id : ids) {
            GameruleHelper.RuleData d = allData.get(id);
            if (d == null) continue;

            // FIXED: search filter — skip non-matching entries entirely instead of
            // setting w.visible = false (which render would override back to true)
            if (tab == 1 && !filter.isEmpty()) {
                String lf = filter.toLowerCase();
                String displayName = displayNameCache.getOrDefault(id, id);
                if (!id.toLowerCase().contains(lf)
                        && !displayName.toLowerCase().contains(lf)) {
                    continue;
                }
            }

            AbstractWidget w;
            boolean isBool = GameruleHelper.isBooleanRule(d);
            if (isBool) {
                w = new RuleToggleButton(0, 0, id, Boolean.parseBoolean(d.value()));
            } else {
                int currentVal = GameruleHelper.parseIntegerValue(d.value(), 0);
                int[] range = GameruleHelper.getRuleRange(id, currentVal);
                w = new RuleNumberWidget(0, 0, id, currentVal, range[0], range[1], font);
            }

            addRenderableWidget(w);
            entries.add(new EntryWidgets(id, w, isBool ? "bool" : "int"));
        }

        scroll = 0;
    }

    // ===== Render =====

    @Override
    public void render(GuiGraphics g, int mx, int my, float pt) {
        renderBackground(g);

        // Panel
        g.fill(px, py, px + PW, py + PH, GameruleHelper.COLOR_PANEL_BG);
        g.renderOutline(px, py, PW, PH, GameruleHelper.COLOR_PANEL_BORDER);

        // List area depends on mode
        int listTop = tab == 0 ? py + 2 : py + 24;
        int listBottom = tab == 0 ? py + PH - 30 : py + PH - 4;
        int listH = listBottom - listTop;

        int maxScroll = Math.max(0, entries.size() - VISIBLE);
        scroll = GameruleHelper.clamp(scroll, 0, maxScroll);

        // Render each entry
        for (int i = 0; i < entries.size(); i++) {
            EntryWidgets ew = entries.get(i);
            AbstractWidget w = ew.control;

            int cardY = listTop + i * ENTRY_H - scroll * ENTRY_H;
            boolean inView = cardY + ENTRY_H > listTop && cardY < listBottom;
            if (!inView) { w.visible = false; continue; }

            w.visible = true;

            // Card background — clipped
            int bg = (i % 2 == 0) ? 0x28FFFFFF : 0x18FFFFFF;
            int bgTop = Math.max(cardY, listTop);
            int bgBottom = Math.min(cardY + ENTRY_H - 1, listBottom);
            if (bgBottom > bgTop) {
                g.fill(px + 4, bgTop, px + PW - 4, bgBottom, bg);
            }

            // Rule name + "?" — only if inside list bounds
            if (cardY + 11 >= listTop && cardY + 11 <= listBottom) {
                // Use cached display name
                String nameStr = displayNameCache.getOrDefault(ew.ruleId, ew.ruleId);
                int nameX = px + 10;
                int nameY = cardY + 11;
                int maxNameW = ew.type.equals("bool") ? 206 : 146;
                if (font.width(nameStr) > maxNameW) {
                    nameStr = font.plainSubstrByWidth(nameStr, maxNameW - 10) + "...";
                }
                g.drawString(font, nameStr, nameX, nameY, GameruleHelper.COLOR_TEXT_WHITE);
                int nameW = font.width(nameStr);

                // Help "?" button
                int qmX = nameX + nameW + 4;
                int qmY = cardY + 10;
                int qmS = 10;
                boolean hoverQM = mx >= qmX && mx <= qmX + qmS && my >= qmY && my <= qmY + qmS;
                int qmBg = hoverQM ? GameruleHelper.COLOR_GRAY_HOVER : GameruleHelper.COLOR_GRAY;
                g.fill(qmX, qmY, qmX + qmS, qmY + qmS, qmBg);
                g.drawCenteredString(font, "?", qmX + qmS / 2, qmY + 1, GameruleHelper.COLOR_TEXT_WHITE);

                // Tooltip — rendered after super.render to avoid overdraw
                if (hoverQM) {
                    String desc = descriptionCache.getOrDefault(ew.ruleId, "");
                    g.renderTooltip(font, Component.literal(desc), mx, my);
                }
            }

            // Control widget — positioned using widget's own public dimensions
            int wY = cardY + (ENTRY_H - (ew.type.equals("bool")
                    ? RuleToggleButton.H : RuleNumberWidget.WIDGET_HEIGHT)) / 2;
            if (ew.type.equals("bool")) {
                w.setX(px + PW - RuleToggleButton.W - 8);
            } else {
                w.setX(px + PW - RuleNumberWidget.TOTAL_WIDTH - 8);
            }
            w.setY(wY);

            boolean widgetInView = wY >= listTop && wY + w.getHeight() <= listBottom;
            if (!widgetInView) { w.visible = false; }
        }

        // Scrollbar — extracted shared logic
        drawScrollbar(g, px + PW - 4, listTop, listH, entries.size(), VISIBLE, ENTRY_H,
                scroll, maxScroll);

        // Renders ALL widgets (buttons, search, entry controls) — Minecraft handles events
        super.render(g, mx, my, pt);
    }

    /**
     * Draw a vertical scrollbar. Shared pattern with CustomizeRulesScreen.
     */
    static void drawScrollbar(GuiGraphics g, int barX, int listTop, int listH,
                               int itemCount, int visibleCount, int entryH,
                               int scroll, int maxScroll) {
        if (maxScroll <= 0) return;
        int barW = 2;
        int totalScrollH = itemCount * entryH;
        int barH = Math.max(10, (visibleCount * listH) / totalScrollH);
        int barY = listTop + (int)((double) scroll / maxScroll * (listH - barH));
        g.fill(barX, barY, barX + barW, barY + barH, 0xFF888888);
    }

    // ===== Scroll =====

    @Override
    public boolean mouseScrolled(double mx, double my, double dy) {
        int maxScroll = Math.max(0, entries.size() - VISIBLE);
        scroll = GameruleHelper.clamp(scroll - (int) dy, 0, maxScroll);
        return true;
    }

    @Override
    public boolean isPauseScreen() { return false; }
}
