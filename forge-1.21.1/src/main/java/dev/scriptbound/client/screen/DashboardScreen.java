package dev.scriptbound.client.screen;

import dev.scriptbound.ScriptBoundConstants;
import dev.scriptbound.client.dashboard.DashboardFormEditor;
import dev.scriptbound.client.dashboard.dialogue.DialogueGraphEditor;
import dev.scriptbound.client.dashboard.flow.FlowGraphEditor;
import dev.scriptbound.client.ui.UiDraw;
import dev.scriptbound.client.ui.UiScale;
import dev.scriptbound.client.ui.UiRect;
import dev.scriptbound.client.ui.UiTextField;
import dev.scriptbound.client.ui.UiTheme;
import dev.scriptbound.dashboard.DashboardService;
import dev.scriptbound.dashboard.DashboardSnapshot;
import dev.scriptbound.network.NetworkHandler;
import dev.scriptbound.network.packet.DashboardActionPacket;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import org.lwjgl.glfw.GLFW;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class DashboardScreen extends Screen
{
    private enum Tab
    {
        FLOWS("FILTER", "Flows"),
        STATES("SERVER", "States"),
        TRIGGERS("BLOCK", "Triggers"),
        SCRIPTS("CODE", "Scripts"),
        DIALOGUES("EDIT", "Dialogues"),
        QUESTS("FAVORITE", "Quests"),
        NPCS("POSE", "NPCs"),
        SETTINGS("GEAR", "Settings");

        private final String icon;
        private final String title;

        Tab(String icon, String title)
        {
            this.icon = icon;
            this.title = title;
        }
    }

    private DashboardSnapshot data;
    private Tab activeTab = Tab.FLOWS;
    private boolean playerStates;
    private String selectedId;
    private int listScroll;
    private int maxListScroll;
    private long lastClickTime;
    private String lastClickId;

    private final DashboardFormEditor editor = new DashboardFormEditor();
    private final FlowGraphEditor flowEditor = new FlowGraphEditor();
    private final DialogueGraphEditor dialogueEditor = new DialogueGraphEditor();

    private final UiTextField searchField = new UiTextField().hint("Search...");
    private final UiTextField valueField = new UiTextField().hint("Value...");
    private final UiTextField newIdField = new UiTextField().hint("New id...");
    private final UiTextField renameField = new UiTextField().hint("New name...");

    private UiRect searchRect;
    private UiRect valueRect;
    private UiRect listRect;
    private UiRect detailRect;
    private UiRect reloadRect;
    private UiRect runRect;
    private UiRect refreshRect;
    private UiRect scopeRect;
    private UiRect setRect;
    private UiRect createRect;
    private UiRect saveRect;
    private UiRect deleteRect;
    private UiRect newIdRect;
    private UiRect closeRect;
    private UiRect editorRect;
    private UiRect flowPropsRect;
    private UiRect flowNewRect;
    private UiRect flowSaveRect;
    private UiRect flowDelRect;
    private UiRect themeToggleRect = rect();
    private UiRect alphaToggleRect = rect();
    private UiRect gridToggleRect = rect();
    private UiRect compactToggleRect = rect();
    private UiRect cornersToggleRect = rect();
    private UiRect effectsToggleRect = rect();
    private UiRect testNotificationRect = rect();

    private boolean themeDropdownOpen = false;
    private UiRect themeDropdownRect = rect();
    private int themeDropdownScroll = 0;
    private final String[] THEMES = {
        "dark_legacy", "light_legacy", "winter_legacy", "forest_legacy", "ocean_legacy", "mountain_legacy", "spring_legacy",
        "minimal", "legacy+", "classic+", "sakura+", "winter+", "autumn+", "forest+", "ocean+", "space+", "cyber+", "terminal+", "liminal+", "void+", "fire+", "storm+", "ender+", "nether+", "amethyst+", "redstone+", "minecraft+"
    };

    private boolean draggingAlpha = false;
    private int preDragAlpha = 255;
    private boolean alphaWarningOpen = false;
    private int pendingAlpha = 255;
    private UiRect alphaConfirmRect = rect();
    private UiRect alphaCancelRect = rect();
    private final UiRect[] tabRects = new UiRect[Tab.values().length];

    private boolean listContextMenuOpen = false;
    private String listContextId = null;
    private int listContextX = 0;
    private int listContextY = 0;
    private UiRect listContextDuplicateRect = rect();
    private UiRect listContextDeleteRect = rect();
    private UiRect listContextRenameRect = rect();

    private boolean renameModalOpen = false;
    private UiRect renameConfirmRect = rect();
    private UiRect renameCancelRect = rect();

    private boolean expandedModalOpen = false;
    private dev.scriptbound.client.ui.UiTextField expandedTarget = null;
    private final dev.scriptbound.client.ui.UiTextArea expandedEditor = new dev.scriptbound.client.ui.UiTextArea();
    private UiRect expandedConfirmRect = rect();
    private UiRect expandedCancelRect = rect();

    public void openExpandedEditor(dev.scriptbound.client.ui.UiTextField target) {
        this.expandedTarget = target;
        this.expandedEditor.setValue(target.value());
        this.expandedModalOpen = true;
        this.expandedEditor.setFocused(true);
    }

    private static UiRect rect() { return new UiRect(0, 0, 0, 0); }

    public DashboardScreen(DashboardSnapshot data)
    {
        super(Component.literal("ScriptBound Engine"));
        this.data = data;
    }

    public void updateData(DashboardSnapshot snapshot)
    {
        this.data = snapshot;
        this.listScroll = 0;
    }

    public void loadDocument(String category, String id, String content)
    {
        if (this.selectedId == null || !this.selectedId.equals(id))
        {
            return;
        }

        if (!categoryForTab().equals(category) && !category.isBlank())
        {
            return;
        }

        if ("flow".equals(category))
        {
            this.flowEditor.load(content);
            this.flowEditor.clearDirty();
            return;
        }

        if ("dialogue".equals(category))
        {
            this.dialogueEditor.load(content);
            this.dialogueEditor.clearDirty();
            return;
        }

        this.editor.load(category.isBlank() ? categoryForTab() : category, content);
        this.editor.clearDirty();
    }

    private void layout()
    {
        int pad = UiScale.pad();
        int sidebarW = UiScale.sidebarW();
        int cx = sidebarW + pad;
        int cw = this.width - cx - pad;
        int top = UiScale.headerH();

        int toolbarY = top + 2;
        int btnW = UiScale.px(72);
        int btnH = UiScale.px(22);
        this.reloadRect = new UiRect(cx, toolbarY, btnW, btnH);
        this.runRect = new UiRect(cx + btnW + 4, toolbarY, btnW, btnH);
        this.refreshRect = new UiRect(cx + (btnW + 4) * 2, toolbarY, btnW, btnH);
        this.scopeRect = new UiRect(cx + (btnW + 4) * 3, toolbarY, UiScale.px(96), btnH);
        this.newIdRect = new UiRect(cx + (btnW + 4) * 3 + UiScale.px(100), toolbarY, UiScale.px(140), btnH);
        this.createRect = new UiRect(cx + (btnW + 4) * 3 + UiScale.px(244), toolbarY, UiScale.px(64), btnH);

        int panelsTop = toolbarY + btnH + 6;
        int bottom = this.height - UiScale.px(8);

        int listW = (int) (cw * 0.30F);
        int detailX = cx + listW + pad;
        int detailW = this.width - detailX - pad;

        this.listRect = new UiRect(cx, panelsTop, listW, bottom - panelsTop);
        this.detailRect = new UiRect(detailX, panelsTop, detailW, bottom - panelsTop);

        this.searchRect = new UiRect(this.listRect.x() + 4, this.listRect.y() + UiScale.panelHeaderH() + 4, this.listRect.w() - 8, UiScale.px(20));

        this.saveRect = new UiRect(this.detailRect.x() + this.detailRect.w() - UiScale.px(148), this.detailRect.y() + 2, UiScale.px(68), UiScale.px(18));
        this.deleteRect = new UiRect(this.detailRect.x() + this.detailRect.w() - UiScale.px(76), this.detailRect.y() + 2, UiScale.px(68), UiScale.px(18));
        this.setRect = new UiRect(this.detailRect.x() + this.detailRect.w() - UiScale.px(72), this.detailRect.y() + this.detailRect.h() - UiScale.px(30), UiScale.px(64), btnH);
        this.valueRect = new UiRect(this.detailRect.x() + pad, this.detailRect.y() + this.detailRect.h() - UiScale.px(30), this.detailRect.w() - pad - UiScale.px(80), btnH);
        this.closeRect = new UiRect(this.width - UiScale.px(28), UiScale.px(8), UiScale.px(20), UiScale.px(20));
        int editorH = this.detailRect.h() - UiScale.panelHeaderH() - 6;
        this.editorRect = new UiRect(
            this.detailRect.x() + 4,
            this.detailRect.y() + UiScale.panelHeaderH() + 2,
            this.detailRect.w() - 8,
            Math.max(UiScale.px(100), editorH)
        );

        if (usesGraphLayout())
        {
            int fbottom = this.height - UiScale.px(8);
            int rightW = Math.max(UiScale.px(180), (int) (cw * 0.22F));
            int rightX = this.width - rightW - pad;
            int hH = UiScale.panelHeaderH();

            this.flowNewRect = new UiRect(rightX + rightW - UiScale.px(66), top + 3, UiScale.px(18), UiScale.px(16));
            this.flowSaveRect = new UiRect(rightX + rightW - UiScale.px(44), top + 3, UiScale.px(18), UiScale.px(16));
            this.flowDelRect = new UiRect(rightX + rightW - UiScale.px(22), top + 3, UiScale.px(18), UiScale.px(16));
            this.searchRect = new UiRect(rightX + 4, top + hH + 4, rightW - 8, UiScale.px(18));

            int listH = Math.max(UiScale.px(90), (fbottom - top) * 38 / 100);
            this.listRect = new UiRect(rightX, top, rightW, listH);
            this.flowPropsRect = new UiRect(rightX, top + listH + 6, rightW, fbottom - (top + listH + 6));

            int canvasX = cx;
            int canvasW = rightX - pad - cx;
            this.editorRect = new UiRect(canvasX, top, Math.max(UiScale.px(100), canvasW), fbottom - top);
        }

        for (int i = 0; i < Tab.values().length; i++)
        {
            this.tabRects[i] = new UiRect(0, UiScale.px(44) + i * UiScale.px(30), sidebarW, UiScale.px(28));
        }
    }

    private boolean usesGraphLayout()
    {
        return this.activeTab == Tab.FLOWS || this.activeTab == Tab.DIALOGUES;
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick)
    {
        layout();

        super.renderBackground(graphics, mouseX, mouseY, partialTick);
        dev.scriptbound.client.ui.UiDraw.panel(graphics, 0, 0, this.width, this.height);

        renderContent(graphics, mouseX, mouseY);
        renderSidebar(graphics, mouseX, mouseY);

        if (this.themeDropdownOpen) {
            int x = this.themeToggleRect.x();
            int y = this.themeToggleRect.y() + this.themeToggleRect.h() + 2;
            int w = this.themeToggleRect.w() + 12;
            int maxH = Math.min(this.height - y - 10, 160);
            int visibleRows = Math.max(1, maxH / 16);
            int h = visibleRows * 16;
            this.themeDropdownRect = new UiRect(x, y, w, h);

            graphics.pose().pushPose();
            graphics.pose().translate(0, 0, 400);
            int r = dev.scriptbound.client.ui.UiConfig.roundedStyle * 2;
            dev.scriptbound.client.ui.UiDraw.fillRounded(graphics, x + 4, y + 4, w, h, 0x44000000, r);
            dev.scriptbound.client.ui.UiDraw.fillRounded(graphics, x, y, w, h, 0xFF121212, r);
            dev.scriptbound.client.ui.UiDraw.outlineRounded(graphics, x, y, w, h, UiTheme.BORDER, r);

            graphics.enableScissor(x, y, x + w, y + h);
            for (int i = 0; i < visibleRows; i++) {
                int index = i + this.themeDropdownScroll;
                if (index >= THEMES.length) break;

                UiRect row = new UiRect(x, y + i * 16, w - 8, 16);
                if (row.contains(mouseX, mouseY)) {
                    graphics.fill(row.x(), row.y(), row.x() + row.w(), row.y() + row.h(), UiTheme.ROW_HOVER);
                }
                String tName = THEMES[index].substring(0, 1).toUpperCase() + THEMES[index].substring(1);
                if (tName.endsWith("_legacy")) tName = tName.replace("_legacy", " LEGACY");
                graphics.drawString(this.font, tName, x + 6, y + i * 16 + 4, UiTheme.TEXT);
            }
            graphics.disableScissor();

            dev.scriptbound.client.ui.UiDraw.scrollbar(graphics, x + w - 6, y + 2, h - 4, THEMES.length, visibleRows, this.themeDropdownScroll);
            graphics.pose().popPose();
        }

        if (this.listContextMenuOpen) {
            int x = this.listContextX;
            int y = this.listContextY;
            int w = 100;
            int h = 46;
            int r = dev.scriptbound.client.ui.UiConfig.roundedStyle * 2;

            this.listContextRenameRect = new UiRect(x, y + 2, w, 14);
            this.listContextDuplicateRect = new UiRect(x, y + 16, w, 14);
            this.listContextDeleteRect = new UiRect(x, y + 30, w, 14);

            graphics.pose().pushPose();
            graphics.pose().translate(0, 0, 400);
            dev.scriptbound.client.ui.UiDraw.fillRounded(graphics, x + 2, y + 2, w + 4, h + 4, 0x44000000, r);
            dev.scriptbound.client.ui.UiDraw.fillRounded(graphics, x - 2, y - 2, w + 4, h + 4, 0xF0080A12, r);
            dev.scriptbound.client.ui.UiDraw.outlineRounded(graphics, x - 2, y - 2, w + 4, h + 4, UiTheme.ACCENT, r);

            if (this.listContextRenameRect.contains(mouseX, mouseY)) graphics.fill(x, y + 2, x + w, y + 16, UiTheme.ROW_HOVER);
            if (this.listContextDuplicateRect.contains(mouseX, mouseY)) graphics.fill(x, y + 16, x + w, y + 30, UiTheme.ROW_HOVER);
            if (this.listContextDeleteRect.contains(mouseX, mouseY)) graphics.fill(x, y + 30, x + w, y + 44, UiTheme.ROW_HOVER);

            graphics.drawString(this.font, "Rename", x + 4, y + 5, UiTheme.TEXT);
            graphics.drawString(this.font, "Duplicate", x + 4, y + 19, UiTheme.TEXT);
            graphics.drawString(this.font, "Delete", x + 4, y + 33, 0xFFFF5555);
            graphics.pose().popPose();
        }

        if (this.renameModalOpen) {
            graphics.pose().pushPose();
            graphics.pose().translate(0, 0, 400);

            int cx = this.width / 2;
            int cy = this.height / 2;
            int pw = UiScale.px(240);
            int ph = UiScale.px(80);
            int px = cx - pw / 2;
            int py = cy - ph / 2;

            int r = dev.scriptbound.client.ui.UiConfig.roundedStyle * 2;
            dev.scriptbound.client.ui.UiDraw.fillRounded(graphics, px + 4, py + 4, pw, ph, 0x44000000, r);
            dev.scriptbound.client.ui.UiDraw.fillRounded(graphics, px, py, pw, ph, 0xFF121212, r);
            dev.scriptbound.client.ui.UiDraw.outlineRounded(graphics, px, py, pw, ph, UiTheme.ACCENT, r);

            graphics.drawCenteredString(this.font, "Rename: " + this.listContextId, cx, py + UiScale.px(12), UiTheme.TEXT);

            int bw = UiScale.px(80);
            int bh = UiScale.px(20);
            int bx1 = cx - bw - 4;
            int bx2 = cx + 4;
            int by = py + ph - bh - UiScale.px(10);

            this.renameConfirmRect = new UiRect(bx1, by, bw, bh);
            this.renameCancelRect = new UiRect(bx2, by, bw, bh);

            UiRect fieldRect = new UiRect(px + UiScale.px(20), py + UiScale.px(30), pw - UiScale.px(40), UiScale.px(18));
            this.renameField.render(graphics, this.font, fieldRect);

            dev.scriptbound.client.ui.UiDraw.button(graphics, this.font, this.renameConfirmRect, "Confirm", null, this.renameConfirmRect.contains(mouseX, mouseY), false, !this.renameField.value().isBlank());
            dev.scriptbound.client.ui.UiDraw.button(graphics, this.font, this.renameCancelRect, "Cancel", null, this.renameCancelRect.contains(mouseX, mouseY), false, true);

            graphics.pose().popPose();
        }

        if (this.expandedModalOpen) {
            graphics.pose().pushPose();
            graphics.pose().translate(0, 0, 400);

            int cx = this.width / 2;
            int cy = this.height / 2;
            int pw = UiScale.px(400);
            int ph = UiScale.px(240);
            int px = cx - pw / 2;
            int py = cy - ph / 2;

            int r = dev.scriptbound.client.ui.UiConfig.roundedStyle * 2;
            dev.scriptbound.client.ui.UiDraw.fillRounded(graphics, px + 4, py + 4, pw, ph, 0x44000000, r);
            dev.scriptbound.client.ui.UiDraw.fillRounded(graphics, px, py, pw, ph, 0xFF121212, r);
            dev.scriptbound.client.ui.UiDraw.outlineRounded(graphics, px, py, pw, ph, UiTheme.ACCENT, r);

            graphics.drawCenteredString(this.font, "Expanded Editor", cx, py + UiScale.px(12), UiTheme.TEXT);

            int bw = UiScale.px(80);
            int bh = UiScale.px(20);
            int by = py + ph - bh - UiScale.px(12);
            int bx1 = cx - bw - UiScale.px(4);
            int bx2 = cx + UiScale.px(4);

            this.expandedConfirmRect = new UiRect(bx1, by, bw, bh);
            this.expandedCancelRect = new UiRect(bx2, by, bw, bh);

            dev.scriptbound.client.ui.UiRect areaRect = new dev.scriptbound.client.ui.UiRect(px + UiScale.px(20), py + UiScale.px(30), pw - UiScale.px(40), ph - UiScale.px(70));
            this.expandedEditor.render(graphics, this.font, areaRect);

            dev.scriptbound.client.ui.UiDraw.button(graphics, this.font, this.expandedConfirmRect, "Apply", null, this.expandedConfirmRect.contains(mouseX, mouseY), false, true);
            dev.scriptbound.client.ui.UiDraw.button(graphics, this.font, this.expandedCancelRect, "Cancel", null, this.expandedCancelRect.contains(mouseX, mouseY), false, true);
            graphics.pose().popPose();
        }

        if (this.alphaWarningOpen) {
            graphics.pose().pushPose();
            graphics.pose().translate(0, 0, 400);

            int cx = this.width / 2;
            int cy = this.height / 2;
            int pw = UiScale.px(240);
            int ph = UiScale.px(100);
            int px = cx - pw / 2;
            int py = cy - ph / 2;

            graphics.fill(0, 0, this.width, this.height, 0x88000000);

            int r = dev.scriptbound.client.ui.UiConfig.roundedStyle * 2;
            dev.scriptbound.client.ui.UiDraw.fillRounded(graphics, px + 4, py + 4, pw, ph, 0x44000000, r);
            dev.scriptbound.client.ui.UiDraw.fillRounded(graphics, px, py, pw, ph, dev.scriptbound.client.ui.UiTheme.BG_OVERLAY, r);
            dev.scriptbound.client.ui.UiDraw.outlineRounded(graphics, px, py, pw, ph, dev.scriptbound.client.ui.UiTheme.BORDER, r);

            graphics.drawCenteredString(this.font, "Warning: Low UI Transparency", cx, py + 16, dev.scriptbound.client.ui.UiTheme.TEXT_ACCENT);
            graphics.drawCenteredString(this.font, "The dashboard may become hard to see.", cx, py + 34, dev.scriptbound.client.ui.UiTheme.TEXT_DIM);

            this.alphaConfirmRect = new UiRect(cx - UiScale.px(95), py + ph - 30, UiScale.px(90), 20);
            this.alphaCancelRect = new UiRect(cx + UiScale.px(5), py + ph - 30, UiScale.px(90), 20);

            dev.scriptbound.client.ui.UiDraw.button(graphics, this.font, this.alphaConfirmRect, "I understand", null, this.alphaConfirmRect.contains(mouseX, mouseY), false, true);
            dev.scriptbound.client.ui.UiDraw.button(graphics, this.font, this.alphaCancelRect, "No, revert it", null, this.alphaCancelRect.contains(mouseX, mouseY), false, true);

            graphics.pose().popPose();
        }

        dev.scriptbound.client.ui.UiNotificationManager.render(graphics, this.font, this.width, mouseX, mouseY);
    }

    private void renderContent(GuiGraphics graphics, int mouseX, int mouseY)
    {
        if (usesGraphLayout())
        {
            graphics.fill(0, 0, this.width, this.height, dev.scriptbound.client.ui.UiTheme.BG_OVERLAY);
            graphics.fill(0, 0, dev.scriptbound.client.ui.UiScale.sidebarW(), this.height, dev.scriptbound.client.ui.UiTheme.SIDEBAR);
            renderHeader(graphics, mouseX, mouseY);

            if (this.activeTab == Tab.FLOWS)
            {
                renderFlowsScreen(graphics, mouseX, mouseY);
            }
            else
            {
                renderDialoguesScreen(graphics, mouseX, mouseY);
            }

            this.searchField.render(graphics, this.font, this.searchRect);
            return;
        }

        graphics.fill(0, 0, this.width, this.height, dev.scriptbound.client.ui.UiTheme.BG_OVERLAY);
        graphics.fill(0, 0, dev.scriptbound.client.ui.UiScale.sidebarW(), this.height, dev.scriptbound.client.ui.UiTheme.SIDEBAR);

        renderHeader(graphics, mouseX, mouseY);

        renderListPanel(graphics, mouseX, mouseY);
        renderDetailPanel(graphics, mouseX, mouseY);
        renderFooter(graphics, mouseX, mouseY);

        this.searchField.render(graphics, this.font, this.searchRect);

        if (isEditableTab() || this.activeTab == Tab.STATES)
        {
            this.newIdField.render(graphics, this.font, this.newIdRect);
        }

        if (showStateEditor())
        {
            this.valueField.render(graphics, this.font, this.valueRect);
        }
    }

    private void renderHeader(GuiGraphics graphics, int mouseX, int mouseY)
    {
        int x = dev.scriptbound.client.ui.UiScale.sidebarW() + dev.scriptbound.client.ui.UiScale.pad();
        graphics.drawString(this.font, "ScriptBound Engine", x, 12, dev.scriptbound.client.ui.UiTheme.TEXT);
        graphics.drawString(this.font, this.activeTab.title, x, 24, dev.scriptbound.client.ui.UiTheme.TEXT_DIM);
        dev.scriptbound.client.ui.UiDraw.badge(graphics, this.font, this.width - 88, 10, "v" + dev.scriptbound.ScriptBoundConstants.VERSION);
        dev.scriptbound.client.ui.UiDraw.iconButton(graphics, this.closeRect, "CLOSE", this.closeRect.contains(mouseX, mouseY), false);
    }

    private void renderSidebar(GuiGraphics graphics, int mouseX, int mouseY)
    {
        for (int i = 0; i < Tab.values().length; i++)
        {
            Tab tab = Tab.values()[i];
            dev.scriptbound.client.ui.UiRect rect = this.tabRects[i];
            dev.scriptbound.client.ui.UiDraw.iconButton(graphics, rect, tab.icon, rect.contains(mouseX, mouseY), tab == this.activeTab);
        }
    }

    private void renderListPanel(GuiGraphics graphics, int mouseX, int mouseY)
    {
        dev.scriptbound.client.ui.UiDraw.panel(graphics, this.listRect.x(), this.listRect.y(), this.listRect.w(), this.listRect.h());
        String header = this.activeTab == Tab.STATES
            ? (this.playerStates ? "Player states" : "Server states")
            : this.activeTab.title;
        dev.scriptbound.client.ui.UiDraw.panelHeader(graphics, this.font, this.listRect.x(), this.listRect.y(), this.listRect.w(), header);

        int innerX = this.listRect.x() + 4;
        int innerY = this.searchRect.y() + this.searchRect.h() + 4;
        int innerW = this.listRect.w() - 12;
        int innerH = this.listRect.y() + this.listRect.h() - innerY - (isEditableTab() ? 24 : 8);

        dev.scriptbound.client.ui.UiDraw.inset(graphics, innerX, innerY, innerW, innerH);

        java.util.List<String> entries = visibleEntries();
        int visibleRows = Math.max(1, innerH / dev.scriptbound.client.ui.UiTheme.ROW_H);
        this.maxListScroll = Math.max(0, entries.size() - visibleRows);

        if (entries.isEmpty())
        {
            graphics.drawString(this.font, "No entries yet. Create one below.", innerX + 8, innerY + 8, dev.scriptbound.client.ui.UiTheme.TEXT_DIM);
            return;
        }

        graphics.enableScissor(innerX, innerY, innerX + innerW, innerY + innerH);

        for (int i = 0; i < visibleRows; i++)
        {
            int index = i + this.listScroll;

            if (index >= entries.size())
            {
                break;
            }

            String entry = entries.get(index);
            int rowY = innerY + i * dev.scriptbound.client.ui.UiTheme.ROW_H;
            boolean selected = entry.equals(this.selectedId);
            dev.scriptbound.client.ui.UiRect row = new dev.scriptbound.client.ui.UiRect(innerX + 2, rowY, innerW - 6, dev.scriptbound.client.ui.UiTheme.ROW_H);
            boolean hovered = row.contains(mouseX, mouseY);

            if (selected)
            {
                graphics.fill(row.x(), row.y(), row.x() + row.w(), row.y() + row.h(), dev.scriptbound.client.ui.UiTheme.ROW_SELECT);
            }
            else if (hovered)
            {
                graphics.fill(row.x(), row.y(), row.x() + row.w(), row.y() + row.h(), dev.scriptbound.client.ui.UiTheme.ROW_HOVER);
            }

            int color = selected ? 0xFFFFFFFF : dev.scriptbound.client.ui.UiTheme.TEXT;
            graphics.drawString(this.font, this.font.plainSubstrByWidth(formatListLabel(entry), innerW - 16), row.x() + 6, row.y() + 5, color);
        }

        graphics.disableScissor();
        dev.scriptbound.client.ui.UiDraw.scrollbar(graphics, innerX + innerW - 2, innerY, innerH, entries.size(), visibleRows, this.listScroll);
    }

    private void renderDetailPanel(GuiGraphics graphics, int mouseX, int mouseY)
    {
        dev.scriptbound.client.ui.UiDraw.panel(graphics, this.detailRect.x(), this.detailRect.y(), this.detailRect.w(), this.detailRect.h());
        String header = this.selectedId == null ? "Properties" : this.selectedId + (isDirty() ? " *" : "");
        dev.scriptbound.client.ui.UiDraw.panelHeader(graphics, this.font, this.detailRect.x(), this.detailRect.y(), this.detailRect.w(), header);

        if (isEditableTab() && this.selectedId != null)
        {
            dev.scriptbound.client.ui.UiDraw.button(graphics, this.font, this.saveRect, "Save", "SAVE", this.saveRect.contains(mouseX, mouseY), false, true);
            if (this.activeTab != Tab.SETTINGS)
            {
                dev.scriptbound.client.ui.UiDraw.button(graphics, this.font, this.deleteRect, "Delete", "CLOSE", this.deleteRect.contains(mouseX, mouseY), false, true);
            }
        }

        int x = this.detailRect.x() + dev.scriptbound.client.ui.UiTheme.PAD;
        int y = this.detailRect.y() + dev.scriptbound.client.ui.UiTheme.PANEL_HEADER_H + dev.scriptbound.client.ui.UiTheme.PAD;
        int maxW = this.detailRect.w() - dev.scriptbound.client.ui.UiTheme.PAD * 2;
        int maxH = this.detailRect.h() - dev.scriptbound.client.ui.UiTheme.PANEL_HEADER_H - (showStateEditor() ? 50 : dev.scriptbound.client.ui.UiTheme.PAD);

        if (this.selectedId == null)
        {
            graphics.drawString(this.font, "Select an item from the list.", x, y, dev.scriptbound.client.ui.UiTheme.TEXT_DIM);

            if (this.activeTab == Tab.SETTINGS)
            {
                renderSettingsPanel(graphics, mouseX, mouseY, x, y, maxW);
            }

            return;
        }

        if (isEditableTab())
        {
            this.editor.render(graphics, this.font, this.editorRect, this.data, mouseX, mouseY);
            return;
        }

        if (this.activeTab == Tab.STATES)
        {
            java.util.Map<String, String> states = this.playerStates ? this.data.playerStates() : this.data.globalStates();
            graphics.drawString(this.font, "Value: " + states.getOrDefault(this.selectedId, ""), x, y, dev.scriptbound.client.ui.UiTheme.TEXT);
            graphics.drawString(this.font, "Scope: " + (this.playerStates ? "@p" : "~"), x, y + 14, dev.scriptbound.client.ui.UiTheme.TEXT_DIM);
            dev.scriptbound.client.ui.UiDraw.button(graphics, this.font, this.setRect, "Set", null, this.setRect.contains(mouseX, mouseY), false, true);
        }
    }

    private void renderSettingsPanel(GuiGraphics graphics, int mouseX, int mouseY, int x, int y, int maxW)
    {
        y += 20;
        graphics.drawString(this.font, "ScriptBound Settings", x, y, UiTheme.TEXT_ACCENT);
        int lineY = y + 16;

        for (String event : DashboardService.GLOBAL_TRIGGER_EVENTS)
        {
            String assigned = this.data.globalTriggers().getOrDefault(event, "(none)");
            graphics.drawString(this.font, event + "  ->  " + assigned, x, lineY, UiTheme.TEXT);
            lineY += 12;
        }

        lineY += 16;
        graphics.drawString(this.font, "UI Settings", x, lineY, UiTheme.TEXT_ACCENT);
        lineY += 16;

        this.themeToggleRect = new UiRect(x, lineY, 120, 16);
        String themeName = dev.scriptbound.client.ui.UiConfig.theme.substring(0, 1).toUpperCase() + dev.scriptbound.client.ui.UiConfig.theme.substring(1);
        if (themeName.endsWith("_legacy")) themeName = themeName.replace("_legacy", " LEGACY");
        dev.scriptbound.client.ui.UiDraw.button(graphics, this.font, this.themeToggleRect, "Theme: " + themeName, null, this.themeToggleRect.contains(mouseX, mouseY), this.themeDropdownOpen, true);
        lineY += 20;

        this.alphaToggleRect = new UiRect(x, lineY, 120, 16);
        int handleW = (int) ((dev.scriptbound.client.ui.UiConfig.panelAlpha / 255.0) * 120);
        int r = dev.scriptbound.client.ui.UiConfig.roundedStyle * 2;
        dev.scriptbound.client.ui.UiDraw.fillRounded(graphics, x, lineY, 120, 16, 0xFF222222, r);
        if (handleW > 0) {
            dev.scriptbound.client.ui.UiDraw.fillRounded(graphics, x, lineY, handleW, 16, UiTheme.ACCENT_DIM, r);
        }
        dev.scriptbound.client.ui.UiDraw.outlineRounded(graphics, x, lineY, 120, 16, UiTheme.BORDER, r);

        String alphaStr = "Panel Alpha: " + Math.round((dev.scriptbound.client.ui.UiConfig.panelAlpha / 255f) * 100) + "%";
        int strW = this.font.width(alphaStr);
        graphics.drawString(this.font, alphaStr, x + (120 - strW) / 2, lineY + 4, UiTheme.TEXT);
        lineY += 20;

        this.cornersToggleRect = new UiRect(x, lineY, 120, 16);
        String cornerText = dev.scriptbound.client.ui.UiConfig.roundedStyle == 0 ? "Sharp" : (dev.scriptbound.client.ui.UiConfig.roundedStyle == 1 ? "Slightly Rounded" : "Fully Rounded");
        dev.scriptbound.client.ui.UiDraw.button(graphics, this.font, this.cornersToggleRect, "Corners: " + cornerText, null, this.cornersToggleRect.contains(mouseX, mouseY), dev.scriptbound.client.ui.UiConfig.roundedStyle > 0, true);
        lineY += 20;

        this.gridToggleRect = new UiRect(x, lineY, 120, 16);
        dev.scriptbound.client.ui.UiDraw.button(graphics, this.font, this.gridToggleRect, "Node Grid: " + (dev.scriptbound.client.ui.UiConfig.showGrid ? "ON" : "OFF"), null, this.gridToggleRect.contains(mouseX, mouseY), dev.scriptbound.client.ui.UiConfig.showGrid, true);
        lineY += 20;

        this.compactToggleRect = new UiRect(x, lineY, 120, 16);
        dev.scriptbound.client.ui.UiDraw.button(graphics, this.font, this.compactToggleRect, "Compact UI: " + (dev.scriptbound.client.ui.UiConfig.compactMode ? "ON" : "OFF"), null, this.compactToggleRect.contains(mouseX, mouseY), dev.scriptbound.client.ui.UiConfig.compactMode, true);
        lineY += 20;

        this.effectsToggleRect = new UiRect(x, lineY, 120, 16);
        dev.scriptbound.client.ui.UiDraw.button(graphics, this.font, this.effectsToggleRect, "Visual Effects: " + (dev.scriptbound.client.ui.UiConfig.visualEffects ? "ON" : "OFF"), null, this.effectsToggleRect.contains(mouseX, mouseY), dev.scriptbound.client.ui.UiConfig.visualEffects, true);
        lineY += 20;

        this.testNotificationRect = new UiRect(x, lineY, 120, 16);
        dev.scriptbound.client.ui.UiDraw.button(graphics, this.font, this.testNotificationRect, "Test Notification", null, this.testNotificationRect.contains(mouseX, mouseY), false, true);
    }

    private void updateAlphaFromMouse(double mouseX) {
        int val = (int) (((mouseX - this.alphaToggleRect.x()) / 120.0) * 255);
        int clamped = Math.max(0, Math.min(255, val));

        if (clamped < 70) {
            this.draggingAlpha = false;
            this.pendingAlpha = clamped;
            dev.scriptbound.client.ui.UiConfig.panelAlpha = 70;
            this.alphaWarningOpen = true;
        } else {
            dev.scriptbound.client.ui.UiConfig.panelAlpha = clamped;
        }
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button)
    {
        if (dev.scriptbound.client.ui.UiNotificationManager.mouseClicked(mouseX, mouseY)) {
            return true;
        }

        if (this.renameModalOpen) {
            if (this.renameConfirmRect.contains(mouseX, mouseY) && !this.renameField.value().isBlank()) {
                String newName = sanitizeId(this.renameField.value());
                if (!newName.isBlank() && !newName.equals(this.listContextId)) {
                    dev.scriptbound.network.NetworkHandler.sendToServer(new dev.scriptbound.network.packet.DashboardActionPacket("rename_doc", categoryForTab() + "|" + this.listContextId, newName));
                    if (this.listContextId.equals(this.selectedId)) {
                        this.selectedId = newName;
                    }
                }
                this.renameModalOpen = false;
                return true;
            }
            if (this.renameCancelRect.contains(mouseX, mouseY)) {
                this.renameModalOpen = false;
                return true;
            }

            dev.scriptbound.client.ui.UiRect fieldRect = new dev.scriptbound.client.ui.UiRect(this.width / 2 - dev.scriptbound.client.ui.UiScale.px(120) + dev.scriptbound.client.ui.UiScale.px(20), this.height / 2 - dev.scriptbound.client.ui.UiScale.px(40) + dev.scriptbound.client.ui.UiScale.px(30), dev.scriptbound.client.ui.UiScale.px(240) - dev.scriptbound.client.ui.UiScale.px(40), dev.scriptbound.client.ui.UiScale.px(18));
            if (fieldRect.contains(mouseX, mouseY)) {
                this.renameField.setFocused(true);
            } else {
                this.renameField.setFocused(false);
            }
            return true;
        }

        if (this.expandedModalOpen) {
            if (this.expandedConfirmRect.contains(mouseX, mouseY)) {
                if (this.expandedTarget != null) {
                    this.expandedTarget.setValue(this.expandedEditor.value());
                }
                this.expandedModalOpen = false;
                return true;
            }
            if (this.expandedCancelRect.contains(mouseX, mouseY)) {
                this.expandedModalOpen = false;
                return true;
            }

            int pw = UiScale.px(400);
            int ph = UiScale.px(240);
            int px = (this.width / 2) - pw / 2;
            int py = (this.height / 2) - ph / 2;
            dev.scriptbound.client.ui.UiRect areaRect = new dev.scriptbound.client.ui.UiRect(px + UiScale.px(20), py + UiScale.px(30), pw - UiScale.px(40), ph - UiScale.px(70));
            this.expandedEditor.mouseClicked(areaRect, mouseX, mouseY, button);
            return true;
        }

        if (button == 0)
        {
            if (this.closeRect.contains(mouseX, mouseY))
            {
                this.onClose();
                return true;
            }

            for (int i = 0; i < Tab.values().length; i++)
            {
                if (this.tabRects[i].contains(mouseX, mouseY))
                {
                    commitCurrentEdits();
                    this.activeTab = Tab.values()[i];
                    this.selectedId = null;
                    this.listScroll = 0;
                    this.editor.load("", "");
                    this.searchField.setValue("");
                    return true;
                }
            }
        }

        if (this.listContextMenuOpen) {
            if (this.listContextRenameRect.contains(mouseX, mouseY)) {
                this.renameField.setValue(this.listContextId);
                this.renameField.setFocused(true);
                this.renameModalOpen = true;
                this.listContextMenuOpen = false;
                return true;
            }
            if (this.listContextDuplicateRect.contains(mouseX, mouseY)) {
                dev.scriptbound.network.NetworkHandler.sendToServer(new DashboardActionPacket("duplicate_doc", categoryForTab() + "|" + this.listContextId));
                this.listContextMenuOpen = false;
                return true;
            }
            if (this.listContextDeleteRect.contains(mouseX, mouseY)) {
                dev.scriptbound.network.NetworkHandler.sendToServer(new DashboardActionPacket("delete_doc", categoryForTab() + "|" + this.listContextId));
                if (this.listContextId.equals(this.selectedId)) {
                    this.selectedId = null;
                }
                this.listContextMenuOpen = false;
                return true;
            }
            this.listContextMenuOpen = false;

        }

        if (usesGraphLayout())
        {
            return handleGraphTabClick(mouseX, mouseY, button);
        }

        if (this.alphaWarningOpen) {
            if (this.alphaConfirmRect.contains(mouseX, mouseY)) {
                this.alphaWarningOpen = false;
                dev.scriptbound.client.ui.UiConfig.panelAlpha = this.pendingAlpha;
                dev.scriptbound.client.ui.UiConfig.save();
                return true;
            }
            if (this.alphaCancelRect.contains(mouseX, mouseY)) {
                this.alphaWarningOpen = false;
                dev.scriptbound.client.ui.UiConfig.panelAlpha = this.preDragAlpha;
                dev.scriptbound.client.ui.UiConfig.save();
                return true;
            }
            return true;
        }

        if (this.themeDropdownOpen) {
            if (this.themeDropdownRect.contains(mouseX, mouseY)) {
                int index = (int) (mouseY - this.themeDropdownRect.y()) / 16 + this.themeDropdownScroll;
                if (index >= 0 && index < THEMES.length) {
                    dev.scriptbound.client.ui.UiConfig.theme = THEMES[index];
                    dev.scriptbound.client.ui.UiConfig.save();
                }
            }
            this.themeDropdownOpen = false;
            return true;
        }

        if (this.activeTab == Tab.SETTINGS)
        {
            if (this.themeToggleRect.contains(mouseX, mouseY))
            {
                this.themeDropdownOpen = !this.themeDropdownOpen;
                return true;
            }
            if (this.alphaToggleRect.contains(mouseX, mouseY))
            {
                this.draggingAlpha = true;
                this.preDragAlpha = dev.scriptbound.client.ui.UiConfig.panelAlpha;
                updateAlphaFromMouse(mouseX);
                return true;
            }
            if (this.cornersToggleRect.contains(mouseX, mouseY))
            {
                dev.scriptbound.client.ui.UiConfig.roundedStyle = (dev.scriptbound.client.ui.UiConfig.roundedStyle + 1) % 3;
                dev.scriptbound.client.ui.UiConfig.save();
                return true;
            }
            if (this.gridToggleRect.contains(mouseX, mouseY))
            {
                dev.scriptbound.client.ui.UiConfig.showGrid = !dev.scriptbound.client.ui.UiConfig.showGrid;
                dev.scriptbound.client.ui.UiConfig.save();
                return true;
            }
            if (this.compactToggleRect.contains(mouseX, mouseY))
            {
                dev.scriptbound.client.ui.UiConfig.compactMode = !dev.scriptbound.client.ui.UiConfig.compactMode;
                dev.scriptbound.client.ui.UiConfig.save();
                return true;
            }
            if (this.effectsToggleRect.contains(mouseX, mouseY))
            {
                dev.scriptbound.client.ui.UiConfig.visualEffects = !dev.scriptbound.client.ui.UiConfig.visualEffects;
                dev.scriptbound.client.ui.UiConfig.save();
                return true;
            }
            if (this.testNotificationRect.contains(mouseX, mouseY)) {
                dev.scriptbound.client.ui.UiNotificationManager.push(dev.scriptbound.client.ui.UiNotificationManager.TYPE_SUCCESS, "This is a test notification!");
                return true;
            }
        }

        if (this.searchField.mouseClicked(this.searchRect, mouseX, mouseY, button))
        {
            blurEditors();
            this.searchField.setFocused(true);
            this.listScroll = 0;
            return true;
        }

        if ((isEditableTab() || this.activeTab == Tab.STATES) && this.newIdField.mouseClicked(this.newIdRect, mouseX, mouseY, button))
        {
            blurEditors();
            this.newIdField.setFocused(true);
            return true;
        }

        if (showStateEditor() && this.valueField.mouseClicked(this.valueRect, mouseX, mouseY, button))
        {
            blurEditors();
            this.valueField.setFocused(true);
            return true;
        }

        if (this.reloadRect.contains(mouseX, mouseY))
        {
            dev.scriptbound.network.NetworkHandler.sendToServer(new DashboardActionPacket("reload", ""));
            return true;
        }

        if (this.runRect.contains(mouseX, mouseY) && this.selectedId != null)
        {
            runSelected();
            return true;
        }

        if (this.refreshRect.contains(mouseX, mouseY))
        {
            dev.scriptbound.network.NetworkHandler.sendToServer(new DashboardActionPacket("refresh", ""));
            requestDocumentLoad();
            return true;
        }

        if (this.activeTab == Tab.STATES && this.scopeRect.contains(mouseX, mouseY))
        {
            this.playerStates = !this.playerStates;
            this.selectedId = null;
            return true;
        }

        if (this.saveRect.contains(mouseX, mouseY))
        {
            saveSelected();
            return true;
        }

        if (this.deleteRect.contains(mouseX, mouseY))
        {
            deleteSelected();
            return true;
        }

        if (this.createRect.contains(mouseX, mouseY))
        {
            createNew();
            return true;
        }

        if (showStateEditor() && this.setRect.contains(mouseX, mouseY))
        {
            setSelectedState();
            return true;
        }

        if (isEditableTab() && !usesGraphLayout() && this.editorRect.contains(mouseX, mouseY)
            && this.editor.mouseClicked(this.editorRect, mouseX, mouseY, button, this.data))
        {
            return true;
        }

        if (pickListEntry(mouseX, mouseY))
        {
            return true;
        }

        blurEditors();
        return super.mouseClicked(mouseX, mouseY, button);
    }

    private void renderFlowsScreen(GuiGraphics graphics, int mouseX, int mouseY)
    {
        renderGraphCanvas(graphics, mouseX, mouseY, "Pick a flow on the right, or press + to create one.", this.flowEditor);
        renderGraphList(graphics, mouseX, mouseY, "Flows");
    }

    private void renderDialoguesScreen(GuiGraphics graphics, int mouseX, int mouseY)
    {
        renderGraphCanvas(graphics, mouseX, mouseY, "Pick a dialogue on the right, or press + to create one.", this.dialogueEditor);
        renderGraphList(graphics, mouseX, mouseY, "Dialogues");
    }

    private void renderGraphCanvas(GuiGraphics graphics, int mouseX, int mouseY, String emptyHint, Object editor)
    {
        boolean hasSelection = this.selectedId != null;

        if (hasSelection)
        {
            if (editor instanceof dev.scriptbound.client.dashboard.flow.FlowGraphEditor flow)
            {
                flow.render(graphics, this.font, this.editorRect, this.flowPropsRect, this.data, mouseX, mouseY);
            }
            else if (editor instanceof dev.scriptbound.client.dashboard.dialogue.DialogueGraphEditor dialogue)
            {
                dialogue.render(graphics, this.font, this.editorRect, this.flowPropsRect, this.data, mouseX, mouseY);
            }
        }
        else
        {
            graphics.fill(this.editorRect.x(), this.editorRect.y(), this.editorRect.x() + this.editorRect.w(), this.editorRect.y() + this.editorRect.h(), 0x33000000);
            graphics.renderOutline(this.editorRect.x(), this.editorRect.y(), this.editorRect.w(), this.editorRect.h(), dev.scriptbound.client.ui.UiTheme.BORDER);
            graphics.drawString(this.font, emptyHint, this.editorRect.x() + (this.editorRect.w() - this.font.width(emptyHint)) / 2, this.editorRect.y() + this.editorRect.h() / 2, dev.scriptbound.client.ui.UiTheme.TEXT_DIM);
            dev.scriptbound.client.ui.UiDraw.panel(graphics, this.flowPropsRect.x(), this.flowPropsRect.y(), this.flowPropsRect.w(), this.flowPropsRect.h());
            dev.scriptbound.client.ui.UiDraw.panelHeader(graphics, this.font, this.flowPropsRect.x(), this.flowPropsRect.y(), this.flowPropsRect.w(), "Node");
        }
    }

    private void renderGraphList(GuiGraphics graphics, int mouseX, int mouseY, String header)
    {
        dev.scriptbound.client.ui.UiDraw.panel(graphics, this.listRect.x(), this.listRect.y(), this.listRect.w(), this.listRect.h());
        dev.scriptbound.client.ui.UiDraw.panelHeader(graphics, this.font, this.listRect.x(), this.listRect.y(), this.listRect.w(), header);

        dev.scriptbound.client.ui.UiDraw.iconButton(graphics, this.flowNewRect, "ADD", this.flowNewRect.contains(mouseX, mouseY), false);
        dev.scriptbound.client.ui.UiDraw.iconButton(graphics, this.flowSaveRect, "SAVE", this.flowSaveRect.contains(mouseX, mouseY), false);
        dev.scriptbound.client.ui.UiDraw.iconButton(graphics, this.flowDelRect, "CLOSE", this.flowDelRect.contains(mouseX, mouseY), false);

        dev.scriptbound.client.ui.UiRect content = flowListContentRect();
        dev.scriptbound.client.ui.UiDraw.inset(graphics, content.x(), content.y(), content.w(), content.h());

        java.util.List<String> entries = visibleEntries();
        int rowH = dev.scriptbound.client.ui.UiScale.rowH();
        int visibleRows = Math.max(1, content.h() / rowH);
        this.maxListScroll = Math.max(0, entries.size() - visibleRows);

        if (entries.isEmpty())
        {
            graphics.drawString(this.font, "No entries yet. Press +", content.x() + 8, content.y() + 8, dev.scriptbound.client.ui.UiTheme.TEXT_DIM);
            return;
        }

        graphics.enableScissor(content.x(), content.y(), content.x() + content.w(), content.y() + content.h());

        for (int i = 0; i < visibleRows; i++)
        {
            int index = i + this.listScroll;

            if (index >= entries.size())
            {
                break;
            }

            String entry = entries.get(index);
            int rowY = content.y() + i * rowH;
            boolean selected = entry.equals(this.selectedId);
            dev.scriptbound.client.ui.UiRect row = new dev.scriptbound.client.ui.UiRect(content.x() + 2, rowY, content.w() - 6, rowH);
            boolean hovered = row.contains(mouseX, mouseY);

            if (selected)
            {
                graphics.fill(row.x(), row.y(), row.x() + row.w(), row.y() + row.h(), dev.scriptbound.client.ui.UiTheme.ROW_SELECT);
            }
            else if (hovered)
            {
                graphics.fill(row.x(), row.y(), row.x() + row.w(), row.y() + row.h(), dev.scriptbound.client.ui.UiTheme.ROW_HOVER);
            }

            int color = selected ? 0xFFFFFFFF : dev.scriptbound.client.ui.UiTheme.TEXT;
            graphics.drawString(this.font, this.font.plainSubstrByWidth(entry, content.w() - 16), row.x() + 6, row.y() + 5, color);
        }

        graphics.disableScissor();
        dev.scriptbound.client.ui.UiDraw.scrollbar(graphics, content.x() + content.w() - 2, content.y(), content.h(), entries.size(), visibleRows, this.listScroll);
    }

    private dev.scriptbound.client.ui.UiRect flowListContentRect()
    {
        int x = this.listRect.x() + 4;
        int y = this.searchRect.y() + this.searchRect.h() + 4;
        int w = this.listRect.w() - 12;
        int h = this.listRect.y() + this.listRect.h() - 6 - y;
        return new dev.scriptbound.client.ui.UiRect(x, y, w, Math.max(dev.scriptbound.client.ui.UiTheme.ROW_H, h));
    }

    private void renderFooter(GuiGraphics graphics, int mouseX, int mouseY)
    {
        dev.scriptbound.client.ui.UiDraw.button(graphics, this.font, this.reloadRect, "Reload", "REFRESH", this.reloadRect.contains(mouseX, mouseY), false, true);
        boolean canRun = this.activeTab == Tab.TRIGGERS || this.activeTab == Tab.SCRIPTS || this.activeTab == Tab.FLOWS;
        String runLabel = this.activeTab == Tab.SCRIPTS ? "Run script" : "Run trigger";
        dev.scriptbound.client.ui.UiDraw.button(graphics, this.font, this.runRect, runLabel, "PLAY", this.runRect.contains(mouseX, mouseY), false, canRun && this.selectedId != null);
        dev.scriptbound.client.ui.UiDraw.button(graphics, this.font, this.refreshRect, "Refresh", "REFRESH", this.refreshRect.contains(mouseX, mouseY), false, true);

        if (this.activeTab == Tab.STATES)
        {
            String scopeLabel = this.playerStates ? "Player states" : "Server states";
            dev.scriptbound.client.ui.UiDraw.button(graphics, this.font, this.scopeRect, scopeLabel, "SERVER", this.scopeRect.contains(mouseX, mouseY), this.playerStates, true);
        }

        if (isEditableTab())
        {
            dev.scriptbound.client.ui.UiDraw.button(graphics, this.font, this.createRect, "New", "ADD", this.createRect.contains(mouseX, mouseY), false, true);
        }
    }

    private boolean showStateEditor()
    {
        return this.activeTab == Tab.STATES && this.selectedId != null;
    }

    private boolean isDirty()
    {
        if (this.activeTab == Tab.FLOWS)
        {
            return this.flowEditor.dirty();
        }

        if (this.activeTab == Tab.DIALOGUES)
        {
            return this.dialogueEditor.dirty();
        }

        return this.editor.dirty();
    }

    private boolean isEditableTab()
    {
        return this.activeTab != Tab.STATES;
    }

    private String categoryForTab()
    {
        return switch (this.activeTab)
        {
            case STATES -> this.playerStates ? "player_state" : "global_state";
            case FLOWS -> "flow";
            case TRIGGERS -> "trigger";
            case SCRIPTS -> "script";
            case DIALOGUES -> "dialogue";
            case QUESTS -> "quest";
            case NPCS -> "npc";
            case SETTINGS -> "global_trigger";
        };
    }

    private java.util.List<String> visibleEntries()
    {
        java.util.List<String> entries = new java.util.ArrayList<>();
        String filter = this.searchField.value().toLowerCase(java.util.Locale.ROOT);

        switch (this.activeTab)
        {
            case STATES -> entries.addAll((this.playerStates ? this.data.playerStates() : this.data.globalStates()).keySet());
            case FLOWS -> entries.addAll(this.data.flows());
            case TRIGGERS -> entries.addAll(this.data.triggers());
            case SCRIPTS -> entries.addAll(this.data.scripts());
            case DIALOGUES -> entries.addAll(this.data.dialogues());
            case QUESTS -> entries.addAll(this.data.quests());
            case NPCS -> {
                entries.addAll(this.data.npcs());

                for (String id : this.data.npcMeta().keySet())
                {
                    if (!entries.contains(id))
                    {
                        entries.add(id);
                    }
                }
            }
            case SETTINGS -> entries.addAll(dev.scriptbound.dashboard.DashboardService.GLOBAL_TRIGGER_EVENTS);
        }

        if (!filter.isBlank())
        {
            entries.removeIf(entry -> !entry.toLowerCase(java.util.Locale.ROOT).contains(filter));
        }

        return entries;
    }

    private String formatListLabel(String entry)
    {
        return switch (this.activeTab)
        {
            case STATES -> {
                java.util.Map<String, String> states = this.playerStates ? this.data.playerStates() : this.data.globalStates();
                yield entry + "  =  " + states.getOrDefault(entry, "");
            }
            case TRIGGERS -> entry + "  (" + this.data.triggerActionCounts().getOrDefault(entry, 0) + " actions)";
            case FLOWS -> entry + "  (visual flow)";
            case DIALOGUES -> entry + "  (node graph)";
            case SETTINGS -> entry + "  ->  " + this.data.globalTriggers().getOrDefault(entry, "(none)");
            default -> entry;
        };
    }

    private void requestDocumentLoad()
    {
        if (!isEditableTab() || this.selectedId == null)
        {
            return;
        }

        dev.scriptbound.network.NetworkHandler.sendToServer(new DashboardActionPacket(
            "load_doc",
            categoryForTab() + "|" + this.selectedId
        ));
    }

    private void saveSelected()
    {
        if (!isEditableTab() || this.selectedId == null)
        {
            return;
        }

        String payload = switch (this.activeTab)
        {
            case FLOWS -> this.flowEditor.toJson();
            case DIALOGUES -> this.dialogueEditor.toJson();
            default -> this.editor.buildContent();
        };

        dev.scriptbound.network.NetworkHandler.sendToServer(new DashboardActionPacket(
            "save_doc",
            categoryForTab() + "|" + this.selectedId,
            payload
        ));

        if (this.activeTab == Tab.FLOWS)
        {
            this.flowEditor.clearDirty();
        }
        else if (this.activeTab == Tab.DIALOGUES)
        {
            this.dialogueEditor.clearDirty();
        }
        else
        {
            this.editor.clearDirty();
        }
    }

    private void createNew()
    {
        if (!isEditableTab() && this.activeTab != Tab.STATES)
        {
            return;
        }

        String id = this.newIdField.value().trim();

        if (id.isBlank())
        {
            return;
        }

        if (this.activeTab == Tab.STATES)
        {
            String scope = this.playerStates ? "player" : "global";
            dev.scriptbound.network.NetworkHandler.sendToServer(new DashboardActionPacket(
                "set_state",
                scope + "|" + id + "|" + ""
            ));
            this.selectedId = id;
            this.newIdField.setValue("");
            dev.scriptbound.network.NetworkHandler.sendToServer(new DashboardActionPacket("refresh", ""));
            return;
        }

        dev.scriptbound.network.NetworkHandler.sendToServer(new DashboardActionPacket(
            "create_doc",
            categoryForTab() + "|" + id
        ));
        this.selectedId = id;
        this.newIdField.setValue("");
        requestDocumentLoad();
    }

    private void deleteSelected()
    {
        if (!isEditableTab() || this.selectedId == null || this.activeTab == Tab.SETTINGS)
        {
            return;
        }

        dev.scriptbound.network.NetworkHandler.sendToServer(new DashboardActionPacket(
            "delete_doc",
            categoryForTab() + "|" + this.selectedId
        ));
        this.selectedId = null;
        this.editor.load("", "");
        this.flowEditor.load("{}");
    }

    private void syncValueField()
    {
        if (showStateEditor())
        {
            java.util.Map<String, String> states = this.playerStates ? this.data.playerStates() : this.data.globalStates();
            this.valueField.setValue(states.getOrDefault(this.selectedId, ""));
        }
    }

    private void setSelectedState()
    {
        if (!showStateEditor())
        {
            return;
        }

        String scope = this.playerStates ? "player" : "global";
        dev.scriptbound.network.NetworkHandler.sendToServer(new DashboardActionPacket(
            "set_state",
            scope + "|" + this.selectedId + "|" + this.valueField.value()
        ));
    }

    private void runSelected()
    {
        if (this.selectedId == null || this.selectedId.isBlank())
        {
            return;
        }

        String action = this.activeTab == Tab.SCRIPTS ? "run_script" : "run_trigger";
        dev.scriptbound.network.NetworkHandler.sendToServer(new DashboardActionPacket(action, this.selectedId));
    }

    private boolean handleGraphTabClick(double mouseX, double mouseY, int button)
    {
        boolean menuOpen = this.activeTab == Tab.FLOWS ? this.flowEditor.menuOpen() : this.dialogueEditor.menuOpen();

        if (menuOpen)
        {
            routeGraphEditorClick(mouseX, mouseY, button);
            return true;
        }

        if (button == 0)
        {
            if (this.closeRect.contains(mouseX, mouseY))
            {
                this.onClose();
                return true;
            }

            for (int i = 0; i < Tab.values().length; i++)
            {
                if (this.tabRects[i].contains(mouseX, mouseY))
                {
                    commitCurrentEdits();
                    this.activeTab = Tab.values()[i];
                    this.selectedId = null;
                    this.listScroll = 0;
                    this.editor.load("", "");
                    this.flowEditor.load("{}");
                    this.dialogueEditor.load("{}");
                    return true;
                }
            }

            if (this.searchField.mouseClicked(this.searchRect, mouseX, mouseY, button))
            {
                blurGraphEditors();
                this.listScroll = 0;
                return true;
            }

            if (this.flowNewRect.contains(mouseX, mouseY))
            {
                createNewGraphDoc();
                return true;
            }

            if (this.flowSaveRect.contains(mouseX, mouseY))
            {
                saveSelected();
                return true;
            }

            if (this.flowDelRect.contains(mouseX, mouseY))
            {
                deleteSelected();
                return true;
            }

            if (pickGraphListEntry(mouseX, mouseY, button))
            {
                return true;
            }
        }
        else if (button == 1)
        {
            if (pickGraphListEntry(mouseX, mouseY, button))
            {
                return true;
            }
        }

        if (this.selectedId != null
            && (this.editorRect.contains(mouseX, mouseY) || this.flowPropsRect.contains(mouseX, mouseY))
            && routeGraphEditorClick(mouseX, mouseY, button))
        {
            return true;
        }

        this.searchField.setFocused(false);
        blurGraphEditors();
        return true;
    }

    private boolean routeGraphEditorClick(double mouseX, double mouseY, int button)
    {
        if (this.activeTab == Tab.FLOWS)
        {
            return this.flowEditor.mouseClicked(this.editorRect, this.flowPropsRect, mouseX, mouseY, button, this.data);
        }

        return this.dialogueEditor.mouseClicked(this.editorRect, this.flowPropsRect, mouseX, mouseY, button, this.data);
    }

    private boolean pickGraphListEntry(double mouseX, double mouseY, int button)
    {
        UiRect content = flowListContentRect();

        if (!content.contains(mouseX, mouseY))
        {
            return false;
        }

        List<String> entries = visibleEntries();
        int row = ((int) mouseY - content.y()) / UiScale.rowH() + this.listScroll;

        if (row < 0 || row >= entries.size())
        {
            return false;
        }

        if (button == 1) {
            this.listContextMenuOpen = true;
            this.listContextX = (int) mouseX;
            this.listContextY = (int) mouseY;
            this.listContextId = entries.get(row);
            return true;
        }

        commitCurrentEdits();
        this.selectedId = entries.get(row);
        resetGraphEditorForTab();
        requestDocumentLoad();
        return true;
    }

    private void createNewGraphDoc()
    {
        String base = sanitizeId(this.searchField.value());
        String category = categoryForTab();
        String id = base.isBlank() ? nextGraphId() : base;

        dev.scriptbound.network.NetworkHandler.sendToServer(new DashboardActionPacket("create_doc", category + "|" + id));
        commitCurrentEdits();
        this.selectedId = id;
        this.searchField.setValue("");
        this.searchField.setFocused(false);
        resetGraphEditorForTab();
        requestDocumentLoad();
    }

    private void resetGraphEditorForTab()
    {
        if (this.activeTab == Tab.FLOWS)
        {
            this.flowEditor.load("{}");
        }
        else if (this.activeTab == Tab.DIALOGUES)
        {
            this.dialogueEditor.load("{}");
        }
    }

    private String nextGraphId()
    {
        List<String> pool = this.activeTab == Tab.FLOWS ? this.data.flows() : this.data.dialogues();
        String prefix = this.activeTab == Tab.FLOWS ? "flow" : "dialogue";
        int n = 1;

        while (pool.contains(prefix + n))
        {
            n++;
        }

        return prefix + n;
    }

    private static String sanitizeId(String value)
    {
        if (value == null)
        {
            return "";
        }

        return value.trim().toLowerCase(Locale.ROOT).replaceAll("[^a-z0-9_]+", "_").replaceAll("^_+|_+$", "");
    }

    private void blurEditors()
    {
        this.searchField.setFocused(false);
        this.valueField.setFocused(false);
        this.newIdField.setFocused(false);
        this.editor.blurAll();
        blurGraphEditors();
    }

    private void blurGraphEditors()
    {
        this.flowEditor.blurAll();
        this.dialogueEditor.blurAll();
    }

    private void commitCurrentEdits()
    {
        if (this.activeTab == Tab.FLOWS)
        {
            this.flowEditor.commitProps();
        }
        else if (this.activeTab == Tab.DIALOGUES)
        {
            this.dialogueEditor.commitProps();
        }
        else
        {
            this.editor.commitEdits();
        }
    }

    private boolean pickListEntry(double mouseX, double mouseY)
    {
        int innerX = this.listRect.x() + 4;
        int innerY = this.searchRect.y() + this.searchRect.h() + 4;
        int innerW = this.listRect.w() - 12;
        int innerH = this.listRect.y() + this.listRect.h() - innerY - (isEditableTab() ? 24 : 8);

        if (!new UiRect(innerX, innerY, innerW, innerH).contains(mouseX, mouseY))
        {
            return false;
        }

        List<String> entries = visibleEntries();
        int row = ((int) mouseY - innerY) / UiTheme.ROW_H + this.listScroll;

        if (row < 0 || row >= entries.size())
        {
            return false;
        }

        String entry = entries.get(row);
        long now = System.currentTimeMillis();

        if (entry.equals(this.lastClickId) && now - this.lastClickTime < 350L
            && (this.activeTab == Tab.TRIGGERS || this.activeTab == Tab.SCRIPTS))
        {
            this.selectedId = entry;
            runSelected();
        }

        this.lastClickId = entry;
        this.lastClickTime = now;
        commitCurrentEdits();
        this.selectedId = entry;
        syncValueField();

        if (isEditableTab())
        {
            if (usesGraphLayout())
            {
                resetGraphEditorForTab();
            }
            else
            {
                this.editor.load(categoryForTab(), "{}");
            }
        }

        requestDocumentLoad();
        return true;
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double delta)
    {
        layout();

        if (this.themeDropdownOpen && this.themeDropdownRect.contains(mouseX, mouseY)) {
            int visibleRows = Math.max(1, this.themeDropdownRect.h() / 16);
            int maxScroll = Math.max(0, THEMES.length - visibleRows);
            this.themeDropdownScroll = Math.max(0, Math.min(maxScroll, this.themeDropdownScroll - (int) Math.signum(delta)));
            return true;
        }

        if (this.listRect.contains(mouseX, mouseY))
        {
            this.listScroll = Math.max(0, Math.min(this.maxListScroll, this.listScroll - (int) Math.signum(delta)));
            return true;
        }

        if (isEditableTab() && this.editorRect.contains(mouseX, mouseY))
        {
            if (this.activeTab == Tab.FLOWS && this.flowEditor.mouseScrolled(mouseX, mouseY, delta))
            {
                return true;
            }

            if (this.activeTab == Tab.DIALOGUES && this.dialogueEditor.mouseScrolled(mouseX, mouseY, delta))
            {
                return true;
            }

            if (!usesGraphLayout() && this.editor.mouseScrolled(this.editorRect, mouseX, mouseY, delta))
            {
                return true;
            }
        }

        return super.mouseScrolled(mouseX, mouseY, scrollX, delta);
    }

    @Override
    public boolean keyPressed(int key, int scanCode, int modifiers)
    {
        if (key == GLFW.GLFW_KEY_ESCAPE)
        {
            if (this.renameModalOpen) {
                this.renameModalOpen = false;
                return true;
            }
            if (this.expandedModalOpen) {
                this.expandedModalOpen = false;
                return true;
            }
            if (this.listContextMenuOpen) {
                this.listContextMenuOpen = false;
                return true;
            }
            this.onClose();
            return true;
        }

        if (key == GLFW.GLFW_KEY_S && (modifiers & GLFW.GLFW_MOD_CONTROL) != 0)
        {
            saveSelected();
            return true;
        }

        if (this.searchField.keyPressed(key) || this.valueField.keyPressed(key) || this.newIdField.keyPressed(key) || this.renameField.keyPressed(key))
        {
            if (key == org.lwjgl.glfw.GLFW.GLFW_KEY_ENTER || key == org.lwjgl.glfw.GLFW.GLFW_KEY_KP_ENTER)
            {
                if (this.valueField.focused())
                {
                    setSelectedState();
                }
                else if (this.newIdField.focused())
                {
                    createNew();
                }
                else if (this.renameField.focused() && this.renameModalOpen && !this.renameField.value().isBlank())
                {
                    String newName = sanitizeId(this.renameField.value());
                    if (!newName.isBlank() && !newName.equals(this.listContextId)) {
                        dev.scriptbound.network.NetworkHandler.sendToServer(new DashboardActionPacket("rename_doc", categoryForTab() + "|" + this.listContextId, newName));
                        if (this.listContextId.equals(this.selectedId)) {
                            this.selectedId = newName;
                        }
                    }
                    this.renameModalOpen = false;
                }
            }

            return true;
        }

        if (this.expandedModalOpen) {
            this.expandedEditor.keyPressed(key);
            return true;
        }

        if (this.activeTab == Tab.FLOWS && this.flowEditor.keyPressed(key))
        {
            return true;
        }

        if (this.activeTab == Tab.DIALOGUES && this.dialogueEditor.keyPressed(key))
        {
            return true;
        }

        if (!usesGraphLayout() && this.editor.keyPressed(key))
        {
            return true;
        }

        return super.keyPressed(key, scanCode, modifiers);
    }

    @Override
    public boolean charTyped(char codePoint, int modifiers)
    {
        if (this.searchField.charTyped(codePoint))
        {
            this.listScroll = 0;
            return true;
        }

        if (this.valueField.charTyped(codePoint) || this.newIdField.charTyped(codePoint) || this.renameField.charTyped(codePoint))
        {
            return true;
        }

        if (this.expandedModalOpen) {
            this.expandedEditor.charTyped(codePoint);
            return true;
        }

        if (this.activeTab == Tab.FLOWS && this.flowEditor.charTyped(codePoint))
        {
            return true;
        }

        if (this.activeTab == Tab.DIALOGUES && this.dialogueEditor.charTyped(codePoint))
        {
            return true;
        }

        if (!usesGraphLayout() && this.editor.charTyped(codePoint))
        {
            return true;
        }

        return super.charTyped(codePoint, modifiers);
    }

    @Override
    public boolean mouseReleased(double mouseX, double mouseY, int button)
    {
        if (this.draggingAlpha) {
            this.draggingAlpha = false;
            dev.scriptbound.client.ui.UiConfig.save();
            return true;
        }
        this.flowEditor.mouseReleased(mouseX, mouseY, button);
        this.dialogueEditor.mouseReleased(mouseX, mouseY, button);
        return super.mouseReleased(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseDragged(double mouseX, double mouseY, int button, double dragX, double dragY)
    {
        if (this.draggingAlpha) {
            updateAlphaFromMouse(mouseX);
            return true;
        }
        this.flowEditor.mouseDragged(mouseX, mouseY, button);
        this.dialogueEditor.mouseDragged(mouseX, mouseY, button);
        return super.mouseDragged(mouseX, mouseY, button, dragX, dragY);
    }

    private final dev.scriptbound.client.ui.DashboardEffectSystem effectSystem = new dev.scriptbound.client.ui.DashboardEffectSystem();

    @Override
    public boolean isPauseScreen()
    {
        return false;
    }

    @Override
    public void renderBackground(GuiGraphics graphics, int mouseX, int mouseY, float partialTick)
    {
        this.effectSystem.updateAndRender(graphics, this.width, this.height);
    }
}
