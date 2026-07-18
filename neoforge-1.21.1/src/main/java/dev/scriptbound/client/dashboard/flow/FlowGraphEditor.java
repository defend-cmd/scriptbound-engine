package dev.scriptbound.client.dashboard.flow;

import dev.scriptbound.client.ui.UiDraw;
import dev.scriptbound.client.ui.UiRect;
import dev.scriptbound.client.ui.UiScale;
import dev.scriptbound.client.ui.UiTextField;
import dev.scriptbound.client.ui.UiTheme;
import dev.scriptbound.dashboard.DashboardSnapshot;
import dev.scriptbound.entity.NpcEntity;
import dev.scriptbound.flow.FlowGraph;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import org.lwjgl.glfw.GLFW;

import java.util.ArrayList;
import java.util.List;

public final class FlowGraphEditor
{
    private static int nodeW() { return UiScale.nodeW(); }
    private static int nodeH() { return UiScale.nodeH(); }
    private static int portR() { return UiScale.portR(); }
    private static final float MIN_SCALE = 0.45F;
    private static final float MAX_SCALE = 2.2F;

    private static final String[][] MENU = {
        {"#", "EVENTS"},
        {"npc", "NPC R-Click"},
        {"player_join", "Player Join"},
        {"player_chat", "Player Chat"},
        {"player_death", "Player Death"},
        {"player_respawn", "Player Respawn"},
        {"block_left", "Block LMB"},
        {"block_right", "Block RMB"},
        {"region_enter", "Region Enter"},
        {"region_exit", "Region Exit"},
        {"manual", "Manual Run"},
        {"#", "LOGIC"},
        {"condition", "Condition"},
        {"wait", "Wait"},
        {"delay", "Delay"},
        {"#", "BLOCKS"},
        {"block", "Block"},
        {"move_block", "Move Block"},
        {"#", "ACTIONS"},
        {"message", "Message"},
        {"dialogue", "Dialogue"},
        {"state", "Set State"},
        {"script", "Run Script"},
        {"quest", "Quest"},
        {"command", "Command"},
        {"run_trigger", "Run Trigger"},
        {"teleport", "Teleport"},
        {"sound", "Play Sound"},
        {"give_item", "Give Item"},
        {"play_film", "Play Film"},
        {"faction", "Faction Rep"},
        {"give_xp", "Give XP"},
        {"damage", "Damage/Heal"},
        {"effect", "Potion Effect"},
        {"comment", "Comment"}
    };

    private final FlowGraph graph = new FlowGraph();
    private boolean dirty;

    private float scale = 1.0F;
    private int panX = 40;
    private int panY = 40;

    private final java.util.Set<String> selectedIds = new java.util.LinkedHashSet<>();
    private String wireFromId;
    private boolean draggingNodes;
    private final java.util.Map<String, Double> dragOffX = new java.util.HashMap<>();
    private final java.util.Map<String, Double> dragOffY = new java.util.HashMap<>();

    private final java.util.List<String> undoStack = new ArrayList<>();
    private final java.util.List<String> redoStack = new ArrayList<>();

    private boolean boxSelecting = false;
    private double boxStartX, boxStartY, boxMouseX, boxMouseY;

    private int propsScroll = 0;
    private int maxPropsScroll = 0;

    private boolean contextMenuOpen = false;
    private int contextMenuX;
    private int contextMenuY;
    private UiRect contextCopyRect;
    private UiRect contextPasteRect;
    private UiRect contextDelRect;

    private boolean stateMenuOpen = false;
    private int stateMenuX;
    private int stateMenuY;
    private int stateMenuScroll = 0;
    private UiRect chooseStateRect;
    private java.util.List<String[]> stateMenuItems = new java.util.ArrayList<>();
    private static String clipboardJson = "[]";

    private boolean panning;
    private double panMouseX;
    private double panMouseY;
    private int panStartX;
    private int panStartY;

    private boolean menuOpen = false;
    private int menuX = 0;
    private int menuY = 0;
    private double menuWorldX = 0;
    private double menuWorldY = 0;
    private int menuScroll = 0;

    private final UiTextField[] fields = {
        new UiTextField(), new UiTextField(), new UiTextField(), new UiTextField(), new UiTextField()
    };
    private final UiRect[] expandRects = new UiRect[5];
    private final List<UiRect> npcRects = new ArrayList<>();
    private final List<String> npcIds = new ArrayList<>();
    private final List<String> npcMetaVals = new ArrayList<>();
    private UiRect detectRect = rect();
    private UiRect deleteNodeRect = rect();

    private UiRect canvasRect = rect();
    private UiRect propsRect = rect();

    public boolean dirty()
    {
        return this.dirty;
    }

    public void clearDirty()
    {
        this.dirty = false;
    }

    public boolean menuOpen()
    {
        return this.menuOpen;
    }

    private boolean dialogueMenuOpen = false;
    private int dialogueMenuX;
    private int dialogueMenuY;
    private int dialogueMenuScroll = 0;
    private UiRect chooseDialogueRect;
    private java.util.List<String> dialogueMenuItems = new java.util.ArrayList<>();

    public void saveSnapshot() {
        String json = this.graph.toJson();
        if (this.undoStack.isEmpty() || !this.undoStack.get(this.undoStack.size() - 1).equals(json)) {
            this.undoStack.add(json);
            if (this.undoStack.size() > 50) this.undoStack.remove(0);
            this.redoStack.clear();
        }
    }

    public void undo() {
        if (this.undoStack.size() > 1) {
            String current = this.undoStack.remove(this.undoStack.size() - 1);
            this.redoStack.add(current);
            loadSilent(this.undoStack.get(this.undoStack.size() - 1));
        }
    }

    public void redo() {
        if (!this.redoStack.isEmpty()) {
            String next = this.redoStack.remove(this.redoStack.size() - 1);
            this.undoStack.add(next);
            loadSilent(next);
        }
    }

    private void loadSilent(String json) {
        FlowGraph parsed = FlowGraph.fromJson(json);
        this.graph.nodes().clear();
        this.graph.links().clear();
        this.graph.nodes().addAll(parsed.nodes());
        this.graph.links().addAll(parsed.links());
        this.dirty = true;
    }

    public void load(String json)
    {
        FlowGraph parsed = FlowGraph.fromJson(json);
        this.graph.nodes().clear();
        this.graph.links().clear();
        this.graph.nodes().addAll(parsed.nodes());
        this.graph.links().addAll(parsed.links());
        this.dirty = false;
        this.selectedIds.clear();
        this.wireFromId = null;
        this.menuOpen = false;
        this.contextMenuOpen = false;
        this.scale = 1.0F;
        this.panX = 40;
        this.panY = 40;
        this.undoStack.clear();
        this.redoStack.clear();
        saveSnapshot();
    }

    public String toJson()
    {
        commitProps();
        return this.graph.toJson();
    }

    public void commitProps()
    {
        FlowGraph.Node node = selectedNode();

        if (node == null)
        {
            return;
        }

        String[][] defs = propDefs(node.type);

        for (int i = 0; i < defs.length && i < this.fields.length; i++)
        {
            node.props.put(defs[i][0], this.fields[i].value());
        }
    }

    public void blurAll()
    {
        commitProps();

        for (UiTextField field : this.fields)
        {
            field.setFocused(false);
        }
    }

    public void render(GuiGraphics graphics, Font font, UiRect canvas, UiRect props, DashboardSnapshot data, int mouseX, int mouseY)
    {
        this.canvasRect = canvas;
        this.propsRect = props;

        graphics.fill(canvas.x(), canvas.y(), canvas.x() + canvas.w(), canvas.y() + canvas.h(), 0x33000000);
        graphics.renderOutline(canvas.x(), canvas.y(), canvas.w(), canvas.h(), UiTheme.BORDER);
        graphics.enableScissor(canvas.x() + 1, canvas.y() + 1, canvas.x() + canvas.w() - 1, canvas.y() + canvas.h() - 1);
        if (dev.scriptbound.client.ui.UiConfig.showGrid) {
            renderGrid(graphics);
        }
        renderLinks(graphics);

        if (this.wireFromId != null)
        {
            int[] out = port(selectedNodeById(this.wireFromId), true);

            if (out != null)
            {
                drawWire(graphics, out[0], out[1], mouseX, mouseY, UiTheme.ACCENT);
            }
        }

        renderNodes(graphics, font, mouseX, mouseY);
        graphics.disableScissor();

        if (this.boxSelecting) {
            int bx = Math.min((int) this.boxStartX, mouseX);
            int by = Math.min((int) this.boxStartY, mouseY);
            int bw = Math.abs((int) this.boxStartX - mouseX);
            int bh = Math.abs((int) this.boxStartY - mouseY);
            graphics.fill(bx, by, bx + bw, by + bh, 0x44FFFFFF);
            graphics.renderOutline(bx, by, bw, bh, 0x88FFFFFF);
        }

        renderToolbar(graphics, font);
        renderProps(graphics, font, data, mouseX, mouseY);

        if (this.menuOpen)
        {
            renderMenu(graphics, font, mouseX, mouseY);
        }

        if (this.stateMenuOpen)
        {
            renderStateMenu(graphics, font, mouseX, mouseY);
        }

        if (this.contextMenuOpen)
        {
            renderContextMenu(graphics, font, mouseX, mouseY);
        }
    }

    private void renderToolbar(GuiGraphics graphics, Font font)
    {
        graphics.drawString(font, "RMB add node  -  wheel zoom  -  drag empty to pan", this.canvasRect.x() + 6, this.canvasRect.y() + 4, UiTheme.TEXT_DIM);
        String zoom = Math.round(this.scale * 100) + "%";
        graphics.drawString(font, zoom, this.canvasRect.x() + this.canvasRect.w() - font.width(zoom) - 6, this.canvasRect.y() + 4, UiTheme.TEXT_DIM);
    }

    private void renderGrid(GuiGraphics graphics)
    {
        int step = Math.max(8, (int) (24 * this.scale));
        int ox = this.canvasRect.x() + Math.floorMod(this.panX, step);
        int oy = this.canvasRect.y() + Math.floorMod(this.panY, step);

        for (int x = ox; x < this.canvasRect.x() + this.canvasRect.w(); x += step)
        {
            graphics.fill(x, this.canvasRect.y(), x + 1, this.canvasRect.y() + this.canvasRect.h(), 0x12FFFFFF);
        }

        for (int y = oy; y < this.canvasRect.y() + this.canvasRect.h(); y += step)
        {
            graphics.fill(this.canvasRect.x(), y, this.canvasRect.x() + this.canvasRect.w(), y + 1, 0x12FFFFFF);
        }
    }

    private void renderLinks(GuiGraphics graphics)
    {
        for (FlowGraph.Link link : this.graph.links())
        {
            int[] from = port(selectedNodeById(link.from), true);
            int[] to = port(selectedNodeById(link.to), false);

            if (from != null && to != null)
            {
                drawWire(graphics, from[0], from[1], to[0], to[1], UiTheme.ACCENT_DIM);
            }
        }
    }

    private void drawWire(GuiGraphics graphics, int x1, int y1, int x2, int y2, int color)
    {
        int segments = 20;
        int prevX = x1;
        int prevY = y1;
        int dy = Math.max(20, Math.abs(y2 - y1) / 2);
        float cy1 = y1 + dy;
        float cy2 = y2 - dy;

        for (int i = 1; i <= segments; i++)
        {
            float t = i / (float) segments;
            float u = 1 - t;
            int x = Math.round(u * u * u * x1 + 3 * u * u * t * x1 + 3 * u * t * t * x2 + t * t * t * x2);
            int y = Math.round(u * u * u * y1 + 3 * u * u * t * cy1 + 3 * u * t * t * cy2 + t * t * t * y2);
            thickLine(graphics, prevX, prevY, x, y, color);
            prevX = x;
            prevY = y;
        }
    }

    private void thickLine(GuiGraphics graphics, int x1, int y1, int x2, int y2, int color)
    {
        int steps = Math.max(Math.abs(x2 - x1), Math.abs(y2 - y1));

        if (steps == 0)
        {
            graphics.fill(x1, y1, x1 + 2, y1 + 2, color);
            return;
        }

        for (int i = 0; i <= steps; i++)
        {
            int x = x1 + (x2 - x1) * i / steps;
            int y = y1 + (y2 - y1) * i / steps;
            graphics.fill(x, y, x + 2, y + 2, color);
        }
    }

    private void renderNodes(GuiGraphics graphics, Font font, int mouseX, int mouseY)
    {
        int w = Math.round(nodeW() * this.scale);
        int h = Math.round(nodeH() * this.scale);

        for (FlowGraph.Node node : this.graph.nodes())
        {
            int x = worldToScreenX(node.x);
            int y = worldToScreenY(node.y);
            boolean selected = this.selectedIds.contains(node.id);
            int accent = FlowGraph.colorFor(node.type);

            graphics.fill(x + 2, y + 2, x + w + 2, y + h + 2, 0x44000000);
            graphics.fill(x, y, x + w, y + h, selected ? UiTheme.PANEL_INSET : UiTheme.PANEL);
            graphics.fill(x, y, x + w, y + 3, accent | 0xFF000000);
            graphics.renderOutline(x, y, w, h, selected ? UiTheme.TEXT_ACCENT : UiTheme.BORDER);

            String title = FlowGraph.labelFor(node.type);
            String sub = summary(node);

            if (sub.isBlank())
            {
                int tw = font.width(title);
                graphics.drawString(font, font.plainSubstrByWidth(title, w - 6), x + Math.max(3, (w - tw) / 2), y + (h - 8) / 2 + 1, UiTheme.TEXT);
            }
            else
            {
                graphics.drawString(font, font.plainSubstrByWidth(title, w - 6), x + 4, y + 6, UiTheme.TEXT_DIM);
                graphics.drawString(font, font.plainSubstrByWidth(sub, w - 6), x + 4, y + 18, UiTheme.TEXT);
            }

            if (!"comment".equals(node.type))
            {
                if (hasInput(node))
                {
                    drawPort(graphics, x + w / 2, y, accent);
                }

                drawPort(graphics, x + w / 2, y + h, accent);
            }
        }
    }

    private void drawPort(GuiGraphics graphics, int cx, int cy, int color)
    {
        graphics.fill(cx - portR(), cy - portR(), cx + portR(), cy + portR(), 0xFF0C0E14);
        graphics.renderOutline(cx - portR(), cy - portR(), portR() * 2, portR() * 2, color);
    }

    private void renderProps(GuiGraphics graphics, Font font, DashboardSnapshot data, int mouseX, int mouseY)
    {
        UiDraw.panel(graphics, this.propsRect.x(), this.propsRect.y(), this.propsRect.w(), this.propsRect.h());
        UiDraw.panelHeader(graphics, font, this.propsRect.x(), this.propsRect.y(), this.propsRect.w(), "Node");

        this.npcRects.clear();
        this.npcIds.clear();
        this.npcMetaVals.clear();
        this.deleteNodeRect = rect();

        int x = this.propsRect.x() + 8;
        int y = this.propsRect.y() + UiTheme.PANEL_HEADER_H + 8 - this.propsScroll;
        int w = this.propsRect.w() - 16;

        if (this.selectedIds.size() != 1)
        {
            graphics.drawString(font, "Select exactly one node", x, y + this.propsScroll, UiTheme.TEXT_DIM);
            return;
        }

        FlowGraph.Node node = selectedNode();

        if (node == null)
        {
            return;
        }

        graphics.drawString(font, FlowGraph.labelFor(node.type), x, y, UiTheme.TEXT_ACCENT);
        y += 16;

        int clipBottom = this.propsRect.y() + this.propsRect.h() - 30;
        graphics.enableScissor(this.propsRect.x() + 1, this.propsRect.y() + UiTheme.PANEL_HEADER_H, this.propsRect.x() + this.propsRect.w() - 1, clipBottom);

        String[][] defs = propDefs(node.type);

        for (int i = 0; i < defs.length && i < this.fields.length; i++)
        {
            graphics.drawString(font, defs[i][1], x, y, UiTheme.TEXT_DIM);

            if ((node.type.equals("state") || node.type.equals("condition")) && i == 1) {
                int btnW = 80;
                int btnX = x + w - btnW;
                this.chooseStateRect = new UiRect(btnX, y - 4, btnW, 12);
                boolean hovered = this.chooseStateRect.contains(mouseX, mouseY);
                UiDraw.button(graphics, font, this.chooseStateRect, "< Choose >", null, hovered, false, true);
            } else if (i == 1) {
                this.chooseStateRect = null;
            }

            if (node.type.equals("dialogue") && i == 0) {
                int btnW = 80;
                int btnX = x + w - btnW;
                this.chooseDialogueRect = new UiRect(btnX, y - 4, btnW, 12);
                boolean hovered = this.chooseDialogueRect.contains(mouseX, mouseY);
                UiDraw.button(graphics, font, this.chooseDialogueRect, "< Choose >", null, hovered, false, true);
            } else if (i == 0) {
                this.chooseDialogueRect = null;
            }

            this.expandRects[i] = null;
            if (defs[i][0].equals("text") || defs[i][0].equals("command") || defs[i][0].equals("script") || defs[i][0].equals("comment")) {
                int btnW = 50;
                int btnX = x + w - btnW;
                this.expandRects[i] = new UiRect(btnX, y - 4, btnW, 12);
                boolean hovered = this.expandRects[i].contains(mouseX, mouseY);
                UiDraw.button(graphics, font, this.expandRects[i], "Expand", null, hovered, false, true);
            }

            this.fields[i].render(graphics, font, new UiRect(x, y + 10, w, 18));
            y += 34;
        }

        this.detectRect = rect();

        if (supportsDetect(node.type))
        {
            this.detectRect = new UiRect(x, y, w, 18);
            String label = switch (node.type)
            {
                case "npc" -> "Detect aimed NPC";
                case "move_block" -> "Detect target block";
                default -> "Detect aimed block";
            };
            UiDraw.button(graphics, font, this.detectRect, label, "SEARCH", this.detectRect.contains(mouseX, mouseY), false, true);
            y += 24;
        }

        if ("npc".equals(node.type))
        {
            graphics.drawString(font, "Detected NPCs (click to use):", x, y, UiTheme.TEXT_ACCENT);
            y += 12;

            java.util.Map<String, String> meta = data == null ? java.util.Map.of() : data.npcMeta();

            if (meta.isEmpty())
            {
                graphics.drawString(font, "none in world - type id above", x, y, UiTheme.TEXT_DIM);
                y += 14;
            }

            for (java.util.Map.Entry<String, String> entry : meta.entrySet())
            {
                String npc = entry.getKey();
                String label = entry.getValue().contains("\u0001") ? entry.getValue().substring(0, entry.getValue().indexOf('\u0001')) : npc;
                UiRect row = new UiRect(x, y, w, 16);
                boolean hovered = row.contains(mouseX, mouseY);
                boolean chosen = npc.equals(node.props.get("npcId"));
                graphics.fill(row.x(), row.y(), row.x() + row.w(), row.y() + row.h(), chosen ? UiTheme.ROW_SELECT : (hovered ? UiTheme.ROW_HOVER : 0x22000000));
                graphics.drawString(font, font.plainSubstrByWidth(label + "  (" + npc + ")", w - 8), row.x() + 6, row.y() + 4, UiTheme.TEXT);
                this.npcRects.add(row);
                this.npcIds.add(npc);
                this.npcMetaVals.add(entry.getValue());
                y += 18;
            }

            y += 4;
        }

        graphics.disableScissor();

        this.deleteNodeRect = new UiRect(x, this.propsRect.y() + this.propsRect.h() - 26, w, 18);
        UiDraw.button(graphics, font, this.deleteNodeRect, "DELETE NODE", "x", this.deleteNodeRect.contains(mouseX, mouseY), false, true);

        int contentH = y - (this.propsRect.y() + UiTheme.PANEL_HEADER_H + 8 - this.propsScroll);
        this.maxPropsScroll = Math.max(0, contentH - (this.propsRect.h() - 50));
    }

    private void renderMenu(GuiGraphics graphics, Font font, int mouseX, int mouseY)
    {
        int w = 132;
        int itemH = 14;
        int maxItems = Math.max(5, (this.canvasRect.h() - 20) / itemH);
        int visibleItems = Math.min(MENU.length, maxItems);
        int totalH = visibleItems * itemH + 6;

        int x = Math.min(this.menuX, this.canvasRect.x() + this.canvasRect.w() - w - 2);
        int y = Math.min(this.menuY, this.canvasRect.y() + this.canvasRect.h() - totalH - 2);
        y = Math.max(this.canvasRect.y() + 2, y);

        int r = dev.scriptbound.client.ui.UiConfig.roundedStyle * 2;
        dev.scriptbound.client.ui.UiDraw.fillRounded(graphics, x - 2, y - 2, w + 4, totalH + 4, 0xF0080A12, r);
        dev.scriptbound.client.ui.UiDraw.outlineRounded(graphics, x - 2, y - 2, w + 4, totalH + 4, UiTheme.ACCENT, r);

        int iy = y + 3;

        for (int i = this.menuScroll; i < this.menuScroll + visibleItems && i < MENU.length; i++)
        {
            String[] entry = MENU[i];
            if ("#".equals(entry[0]))
            {
                graphics.drawString(font, entry[1], x + 4, iy + 3, UiTheme.TEXT_DIM);
            }
            else
            {
                UiRect row = new UiRect(x, iy, w, itemH);
                boolean hovered = row.contains(mouseX, mouseY);

                if (hovered)
                {
                    graphics.fill(row.x(), row.y(), row.x() + row.w(), row.y() + row.h(), UiTheme.ROW_HOVER);
                }

                graphics.fill(x + 4, iy + 4, x + 10, iy + 10, FlowGraph.colorFor(entry[0]));
                graphics.drawString(font, entry[1], x + 14, iy + 3, UiTheme.TEXT);
            }

            iy += itemH;
        }

        if (this.menuScroll > 0) graphics.fill(x + w - 4, y + 2, x + w, y + 6, UiTheme.BORDER_LIGHT);
        if (this.menuScroll + visibleItems < MENU.length) graphics.fill(x + w - 4, y + totalH - 6, x + w, y + totalH - 2, UiTheme.BORDER_LIGHT);
    }

    private void renderStateMenu(GuiGraphics graphics, Font font, int mouseX, int mouseY) {
        int w = 150;
        int itemH = 14;
        int maxItems = Math.max(5, (this.canvasRect.h() - 20) / itemH);
        int visibleItems = Math.min(this.stateMenuItems.size(), maxItems);
        int totalH = visibleItems * itemH + 6;

        int x = Math.min(this.stateMenuX, this.canvasRect.x() + this.canvasRect.w() - w - 2);
        int y = Math.min(this.stateMenuY, this.canvasRect.y() + this.canvasRect.h() - totalH - 2);
        y = Math.max(this.canvasRect.y() + 2, y);

        int r = dev.scriptbound.client.ui.UiConfig.roundedStyle * 2;
        dev.scriptbound.client.ui.UiDraw.fillRounded(graphics, x - 2, y - 2, w + 4, totalH + 4, 0xF0080A12, r);
        dev.scriptbound.client.ui.UiDraw.outlineRounded(graphics, x - 2, y - 2, w + 4, totalH + 4, UiTheme.ACCENT, r);

        int iy = y + 3;

        for (int i = this.stateMenuScroll; i < this.stateMenuScroll + visibleItems && i < this.stateMenuItems.size(); i++)
        {
            String[] entry = this.stateMenuItems.get(i);
            if ("#".equals(entry[0]))
            {
                graphics.drawString(font, entry[1], x + 4, iy + 3, UiTheme.TEXT_DIM);
            }
            else
            {
                UiRect row = new UiRect(x, iy, w, itemH);
                boolean hovered = row.contains(mouseX, mouseY);
                if (hovered) graphics.fill(row.x(), row.y(), row.x() + row.w(), row.y() + row.h(), UiTheme.ROW_HOVER);
                graphics.drawString(font, entry[0], x + 10, iy + 3, UiTheme.TEXT);
            }
            iy += itemH;
        }

        dev.scriptbound.client.ui.UiDraw.scrollbar(graphics, x + w - 4, y + 2, totalH - 4, this.stateMenuItems.size(), visibleItems, this.stateMenuScroll);
    }

    private void openContextMenu(int x, int y) {
        this.contextMenuOpen = true;
        this.contextMenuX = x;
        this.contextMenuY = y;
        this.contextCopyRect = new UiRect(x, y + 4, 100, 14);
        this.contextPasteRect = new UiRect(x, y + 18, 100, 14);
        this.contextDelRect = new UiRect(x, y + 32, 100, 14);
    }

    private void renderContextMenu(GuiGraphics graphics, Font font, int mouseX, int mouseY) {
        int r = dev.scriptbound.client.ui.UiConfig.roundedStyle * 2;
        int x = (int) this.contextMenuX;
        int y = (int) this.contextMenuY;
        dev.scriptbound.client.ui.UiDraw.fillRounded(graphics, x - 2, y - 2, 104, 52, 0xF0080A12, r);
        dev.scriptbound.client.ui.UiDraw.outlineRounded(graphics, x - 2, y - 2, 104, 52, UiTheme.ACCENT, r);

        boolean hC = this.contextCopyRect.contains(mouseX, mouseY);
        boolean hP = this.contextPasteRect.contains(mouseX, mouseY);
        boolean hD = this.contextDelRect.contains(mouseX, mouseY);

        if (hC) graphics.fill(this.contextCopyRect.x(), this.contextCopyRect.y(), this.contextCopyRect.x() + this.contextCopyRect.w(), this.contextCopyRect.y() + this.contextCopyRect.h(), UiTheme.ROW_HOVER);
        if (hP) graphics.fill(this.contextPasteRect.x(), this.contextPasteRect.y(), this.contextPasteRect.x() + this.contextPasteRect.w(), this.contextPasteRect.y() + this.contextPasteRect.h(), UiTheme.ROW_HOVER);
        if (hD) graphics.fill(this.contextDelRect.x(), this.contextDelRect.y(), this.contextDelRect.x() + this.contextDelRect.w(), this.contextDelRect.y() + this.contextDelRect.h(), UiTheme.ROW_HOVER);

        graphics.drawString(font, "Copy", x + 4, y + 7, UiTheme.TEXT);
        graphics.drawString(font, "Paste", x + 4, y + 21, UiTheme.TEXT);
        graphics.drawString(font, "Delete", x + 4, y + 35, 0xFFFF5555);
    }

    public boolean mouseClicked(UiRect canvas, UiRect props, double mouseX, double mouseY, int button, DashboardSnapshot data)
    {
        this.canvasRect = canvas;
        this.propsRect = props;

        if (this.contextMenuOpen) {
            if (this.contextCopyRect.contains(mouseX, mouseY)) { copySelection(); this.contextMenuOpen = false; return true; }
            if (this.contextPasteRect.contains(mouseX, mouseY)) { pasteClipboard(mouseX, mouseY); this.contextMenuOpen = false; return true; }
            if (this.contextDelRect.contains(mouseX, mouseY)) { deleteSelection(); this.contextMenuOpen = false; return true; }
            this.contextMenuOpen = false;

        }

        if (this.stateMenuOpen) {
            int w = 150;
            int itemH = 14;
            int maxItems = Math.max(5, (this.canvasRect.h() - 20) / itemH);
            int visibleItems = Math.min(this.stateMenuItems.size(), maxItems);
            int totalH = visibleItems * itemH + 6;

            int x = Math.min(this.stateMenuX, this.canvasRect.x() + this.canvasRect.w() - w - 2);
            int y = Math.min(this.stateMenuY, this.canvasRect.y() + this.canvasRect.h() - totalH - 2);
            y = Math.max(this.canvasRect.y() + 2, y);

            if (new UiRect(x - 2, y - 2, w + 4, totalH + 4).contains(mouseX, mouseY)) {
                int iy = y + 3;
                for (int i = this.stateMenuScroll; i < this.stateMenuScroll + visibleItems && i < this.stateMenuItems.size(); i++) {
                    String[] entry = this.stateMenuItems.get(i);
                    if (!"#".equals(entry[0])) {
                        if (new UiRect(x, iy, w, itemH).contains(mouseX, mouseY)) {
                            FlowGraph.Node node = selectedNode();
                            if (node != null && this.fields.length > 1) {
                                this.fields[1].setValue(entry[0]);
                                commitProps();

                                if (node.type.equals("state")) {
                                    node.props.put("scope", entry[2]);
                                }
                                syncProps();
                                this.dirty = true;
                            }
                            this.stateMenuOpen = false;
                            return true;
                        }
                    }
                    iy += itemH;
                }
                return true;
            }
            this.stateMenuOpen = false;
            return true;
        }

        if (this.dialogueMenuOpen) {
            int w = 150;
            int itemH = 14;
            int maxItems = Math.max(5, (this.canvasRect.h() - 20) / itemH);
            int visibleItems = Math.min(this.dialogueMenuItems.size(), maxItems);
            int totalH = visibleItems * itemH + 6;

            int x = Math.min(this.dialogueMenuX, this.canvasRect.x() + this.canvasRect.w() - w - 2);
            int y = Math.min(this.dialogueMenuY, this.canvasRect.y() + this.canvasRect.h() - totalH - 2);
            y = Math.max(this.canvasRect.y() + 2, y);

            if (new UiRect(x - 2, y - 2, w + 4, totalH + 4).contains(mouseX, mouseY)) {
                int iy = y + 3;
                for (int i = this.dialogueMenuScroll; i < this.dialogueMenuScroll + visibleItems && i < this.dialogueMenuItems.size(); i++) {
                    String entry = this.dialogueMenuItems.get(i);
                    if (!"#".equals(entry)) {
                        if (new UiRect(x, iy, w, itemH).contains(mouseX, mouseY)) {
                            FlowGraph.Node node = selectedNode();
                            if (node != null && this.fields.length > 0) {
                                this.fields[0].setValue(entry);
                                commitProps();
                                syncProps();
                                this.dirty = true;
                            }
                            this.dialogueMenuOpen = false;
                            return true;
                        }
                    }
                    iy += itemH;
                }
                return true;
            }
            this.dialogueMenuOpen = false;
            return true;
        }

        if (this.menuOpen)
        {
            return handleMenuClick(mouseX, mouseY);
        }

        if (button == 1 && canvas.contains(mouseX, mouseY))
        {
            if (selectNodeUnderCursor(mouseX, mouseY)) {
                openContextMenu((int) mouseX, (int) mouseY);
            } else {
                openMenu((int) mouseX, (int) mouseY);
            }
            return true;
        }

        if (props.contains(mouseX, mouseY))
        {
            return handlePropsClick(mouseX, mouseY, button, data);
        }

        if (button == 0 && canvas.contains(mouseX, mouseY))
        {
            return handleCanvasClick(mouseX, mouseY);
        }

        return false;
    }

    private boolean handleMenuClick(double mouseX, double mouseY)
    {
        int w = 132;
        int itemH = 14;
        int maxItems = Math.max(5, (this.canvasRect.h() - 20) / itemH);
        int visibleItems = Math.min(MENU.length, maxItems);
        int totalH = visibleItems * itemH + 6;

        int x = Math.min(this.menuX, this.canvasRect.x() + this.canvasRect.w() - w - 2);
        int y = Math.min(this.menuY, this.canvasRect.y() + this.canvasRect.h() - totalH - 2);
        y = Math.max(this.canvasRect.y() + 2, y);

        int iy = y + 3;

        for (int i = this.menuScroll; i < this.menuScroll + visibleItems && i < MENU.length; i++)
        {
            String[] entry = MENU[i];
            if (!"#".equals(entry[0]))
            {
                if (new UiRect(x, iy, w, itemH).contains(mouseX, mouseY))
                {
                    commitProps();
                    FlowGraph.Node node = this.graph.addNode(entry[0], (int) this.menuWorldX - nodeW() / 2, (int) this.menuWorldY - nodeH() / 2);
                    this.selectedIds.clear();
                    this.selectedIds.add(node.id);
                    syncProps();
                    this.dirty = true;
                    this.menuOpen = false;
                    saveSnapshot();
                    return true;
                }
            }
            iy += itemH;
        }

        this.menuOpen = false;
        return true;
    }

    private boolean handlePropsClick(double mouseX, double mouseY, int button, DashboardSnapshot data)
    {
        FlowGraph.Node node = selectedNode();

        if (node == null)
        {
            return true;
        }

        if (this.chooseStateRect != null && this.chooseStateRect.contains(mouseX, mouseY) && data != null) {
            this.stateMenuOpen = true;
            this.stateMenuX = (int) mouseX;
            this.stateMenuY = (int) mouseY;
            this.stateMenuScroll = 0;
            this.stateMenuItems.clear();

            boolean globals = "global".equals(node.props.getOrDefault("scope", "player"));
            java.util.Map<String, String> states = globals ? data.globalStates() : data.playerStates();

            for (String key : states.keySet()) {
                this.stateMenuItems.add(new String[] { key, states.get(key), globals ? "global" : "player" });
            }
            if (this.stateMenuItems.isEmpty()) {
                this.stateMenuItems.add(new String[] { "#", "(No states found)", "" });
            }
            return true;
        }

        if (this.chooseDialogueRect != null && this.chooseDialogueRect.contains(mouseX, mouseY) && data != null) {
            this.dialogueMenuOpen = true;
            this.dialogueMenuX = (int) mouseX;
            this.dialogueMenuY = (int) mouseY;
            this.dialogueMenuScroll = 0;
            this.dialogueMenuItems.clear();

            this.dialogueMenuItems.addAll(data.dialogues());

            if (this.dialogueMenuItems.isEmpty()) {
                this.dialogueMenuItems.add("#");
            }
            return true;
        }

        if (this.deleteNodeRect.contains(mouseX, mouseY))
        {
            commitProps();
            this.graph.removeNode(node.id);
            this.selectedIds.clear();
            this.dirty = true;
            return true;
        }

        if (this.detectRect.contains(mouseX, mouseY) && supportsDetect(node.type))
        {
            detectIntoNode(node);
            return true;
        }

        for (int i = 0; i < this.npcRects.size(); i++)
        {
            if (this.npcRects.get(i).contains(mouseX, mouseY))
            {
                String npc = this.npcIds.get(i);
                node.props.put("npcId", npc);
                String meta = i < this.npcMetaVals.size() ? this.npcMetaVals.get(i) : "";

                if (meta.contains("\u0001"))
                {
                    String name = meta.substring(0, meta.indexOf('\u0001'));
                    String mob = meta.substring(meta.indexOf('\u0001') + 1);

                    if (!name.isBlank())
                    {
                        node.props.put("displayName", name);
                    }

                    if (!mob.isBlank())
                    {
                        node.props.put("mobId", mob);
                    }
                }
                else if (node.props.getOrDefault("displayName", "").isBlank())
                {
                    node.props.put("displayName", titleCase(npc));
                }

                syncProps();
                this.dirty = true;
                return true;
            }
        }

        String[][] defs = propDefs(node.type);
        int x = this.propsRect.x() + 8;
        int y = this.propsRect.y() + UiTheme.PANEL_HEADER_H + 8 + 16 - this.propsScroll;
        int w = this.propsRect.w() - 16;
        boolean any = false;

        dev.scriptbound.ScriptBoundMod.LOGGER.info("[DEBUG] handlePropsClick for node type: " + node.type + " (scroll: " + this.propsScroll + ")");

        for (int i = 0; i < defs.length && i < this.fields.length; i++)
        {
            if (this.expandRects[i] != null && this.expandRects[i].contains(mouseX, mouseY)) {
                if (Minecraft.getInstance().screen instanceof dev.scriptbound.client.screen.DashboardScreen ds) {
                    ds.openExpandedEditor(this.fields[i]);
                }
                return true;
            }
            UiRect fieldHitbox = new UiRect(x, y + 10, w, 18);
            boolean focused = this.fields[i].mouseClicked(fieldHitbox, mouseX, mouseY, button);
            if (focused) dev.scriptbound.ScriptBoundMod.LOGGER.info("[DEBUG] field " + i + " clicked/focused! (hitbox: " + fieldHitbox.x() + ", " + fieldHitbox.y() + ")");
            any = any || focused;
            y += 34;
        }

        return true;
    }

    private boolean selectNodeUnderCursor(double mouseX, double mouseY) {
        int w = Math.round(nodeW() * this.scale);
        int h = Math.round(nodeH() * this.scale);
        for (int i = this.graph.nodes().size() - 1; i >= 0; i--) {
            FlowGraph.Node node = this.graph.nodes().get(i);
            int x = worldToScreenX(node.x);
            int y = worldToScreenY(node.y);
            if (new UiRect(x, y, w, h).contains(mouseX, mouseY)) {
                if (!this.selectedIds.contains(node.id)) {
                    this.selectedIds.clear();
                    this.selectedIds.add(node.id);
                    syncProps();
                }
                return true;
            }
        }
        return false;
    }

    private boolean handleCanvasClick(double mouseX, double mouseY)
    {
        int w = Math.round(nodeW() * this.scale);
        int h = Math.round(nodeH() * this.scale);

        for (int i = this.graph.nodes().size() - 1; i >= 0; i--)
        {
            FlowGraph.Node node = this.graph.nodes().get(i);
            int x = worldToScreenX(node.x);
            int y = worldToScreenY(node.y);

            if (!"comment".equals(node.type))
            {
                if (portHit(mouseX, mouseY, x + w / 2, y + h))
                {
                    this.wireFromId = node.id;
                    return true;
                }

                if (this.wireFromId != null && hasInput(node) && portHit(mouseX, mouseY, x + w / 2, y))
                {
                    this.graph.connect(this.wireFromId, node.id);
                    this.wireFromId = null;
                    this.dirty = true;
                    saveSnapshot();
                    return true;
                }
            }

            if (new UiRect(x, y, w, h).contains(mouseX, mouseY))
            {
                commitProps();
                if (net.minecraft.client.gui.screens.Screen.hasShiftDown()) {
                    if (this.selectedIds.contains(node.id)) this.selectedIds.remove(node.id);
                    else this.selectedIds.add(node.id);
                } else {
                    if (!this.selectedIds.contains(node.id)) {
                        this.selectedIds.clear();
                        this.selectedIds.add(node.id);
                    }
                }
                syncProps();

                this.draggingNodes = true;
                this.dragOffX.clear();
                this.dragOffY.clear();
                for (String id : this.selectedIds) {
                    FlowGraph.Node sn = selectedNodeById(id);
                    if (sn != null) {
                        this.dragOffX.put(id, screenToWorldX(mouseX) - sn.x);
                        this.dragOffY.put(id, screenToWorldY(mouseY) - sn.y);
                    }
                }

                this.wireFromId = null;
                return true;
            }
        }

        if (this.wireFromId != null)
        {
            this.wireFromId = null;
            return true;
        }

        commitProps();
        if (!net.minecraft.client.gui.screens.Screen.hasShiftDown()) {
            this.selectedIds.clear();
            this.panning = true;
            this.panMouseX = mouseX;
            this.panMouseY = mouseY;
            this.panStartX = this.panX;
            this.panStartY = this.panY;
        } else {
            this.boxSelecting = true;
            this.boxStartX = mouseX;
            this.boxStartY = mouseY;
            this.boxMouseX = mouseX;
            this.boxMouseY = mouseY;
        }
        return true;
    }

    public void mouseReleased(double mouseX, double mouseY, int button)
    {
        if (this.draggingNodes) {
            this.draggingNodes = false;
            saveSnapshot();
        }

        if (this.boxSelecting) {
            this.boxSelecting = false;
            double minWorldX = Math.min(screenToWorldX(this.boxStartX), screenToWorldX(mouseX));
            double maxWorldX = Math.max(screenToWorldX(this.boxStartX), screenToWorldX(mouseX));
            double minWorldY = Math.min(screenToWorldY(this.boxStartY), screenToWorldY(mouseY));
            double maxWorldY = Math.max(screenToWorldY(this.boxStartY), screenToWorldY(mouseY));

            for (FlowGraph.Node node : this.graph.nodes()) {
                if (node.x + nodeW() >= minWorldX && node.x <= maxWorldX && node.y + nodeH() >= minWorldY && node.y <= maxWorldY) {
                    this.selectedIds.add(node.id);
                }
            }
            syncProps();
        }

        this.panning = false;
    }

    public void mouseDragged(double mouseX, double mouseY, int button)
    {
        if (this.draggingNodes)
        {
            for (String id : this.selectedIds) {
                FlowGraph.Node node = selectedNodeById(id);
                if (node != null && this.dragOffX.containsKey(id)) {
                    node.x = (int) Math.round(screenToWorldX(mouseX) - this.dragOffX.get(id));
                    node.y = (int) Math.round(screenToWorldY(mouseY) - this.dragOffY.get(id));
                    this.dirty = true;
                }
            }
            return;
        }

        if (this.boxSelecting)
        {
            this.boxMouseX = mouseX;
            this.boxMouseY = mouseY;
            return;
        }

        if (this.panning)
        {
            this.panX = this.panStartX + (int) (mouseX - this.panMouseX);
            this.panY = this.panStartY + (int) (mouseY - this.panMouseY);
        }
    }

    public boolean mouseScrolled(double mouseX, double mouseY, double delta)
    {
        if (this.contextMenuOpen) return true;

        if (this.stateMenuOpen) {
            int itemH = 14;
            int maxItems = Math.max(5, (this.canvasRect.h() - 20) / itemH);
            if (this.stateMenuItems.size() > maxItems) {
                this.stateMenuScroll -= (int) Math.signum(delta) * 3;
                this.stateMenuScroll = Math.max(0, Math.min(this.stateMenuScroll, this.stateMenuItems.size() - maxItems));
            }
            return true;
        }

        if (this.dialogueMenuOpen) {
            int itemH = 14;
            int maxItems = Math.max(5, (this.canvasRect.h() - 20) / itemH);
            if (this.dialogueMenuItems.size() > maxItems) {
                this.dialogueMenuScroll -= (int) Math.signum(delta) * 3;
                this.dialogueMenuScroll = Math.max(0, Math.min(this.dialogueMenuScroll, this.dialogueMenuItems.size() - maxItems));
            }
            return true;
        }

        if (this.menuOpen)
        {
            int itemH = 14;
            int maxItems = Math.max(5, (this.canvasRect.h() - 20) / itemH);
            if (MENU.length > maxItems) {
                this.menuScroll -= (int) Math.signum(delta) * 3;
                this.menuScroll = Math.max(0, Math.min(this.menuScroll, MENU.length - maxItems));
            }
            return true;
        }

        if (this.propsRect.contains(mouseX, mouseY) && this.maxPropsScroll > 0) {
            this.propsScroll -= (int) (Math.signum(delta) * 24);
            this.propsScroll = Math.max(0, Math.min(this.propsScroll, this.maxPropsScroll));
            return true;
        }

        if (!this.canvasRect.contains(mouseX, mouseY))
        {
            return false;
        }

        double worldX = screenToWorldX(mouseX);
        double worldY = screenToWorldY(mouseY);
        float factor = delta > 0 ? 1.1F : 1 / 1.1F;
        this.scale = Math.max(MIN_SCALE, Math.min(MAX_SCALE, this.scale * factor));
        this.panX = (int) Math.round(mouseX - this.canvasRect.x() - worldX * this.scale);
        this.panY = (int) Math.round(mouseY - this.canvasRect.y() - worldY * this.scale);
        return true;
    }

    private void copySelection() {
        if (this.selectedIds.isEmpty()) return;
        com.google.gson.JsonArray arr = new com.google.gson.JsonArray();
        for (String id : this.selectedIds) {
            FlowGraph.Node n = selectedNodeById(id);
            if (n != null) {
                com.google.gson.JsonObject obj = new com.google.gson.JsonObject();
                obj.addProperty("id", n.id);
                obj.addProperty("type", n.type);
                obj.addProperty("x", n.x);
                obj.addProperty("y", n.y);
                com.google.gson.JsonObject props = new com.google.gson.JsonObject();
                for (java.util.Map.Entry<String, String> entry : n.props.entrySet()) {
                    props.addProperty(entry.getKey(), entry.getValue());
                }
                obj.add("props", props);
                com.google.gson.JsonArray nodeLinks = new com.google.gson.JsonArray();
                for (FlowGraph.Link l : this.graph.links()) {
                    if (l.from.equals(n.id) && this.selectedIds.contains(l.to)) {
                        nodeLinks.add(l.to);
                    }
                }
                obj.add("outputs", nodeLinks);
                arr.add(obj);
            }
        }
        clipboardJson = arr.toString();
    }

    private void pasteClipboard(double screenX, double screenY) {
        if (clipboardJson == null || clipboardJson.equals("[]")) return;
        commitProps();
        com.google.gson.JsonArray arr = com.google.gson.JsonParser.parseString(clipboardJson).getAsJsonArray();
        this.selectedIds.clear();

        double minX = Double.MAX_VALUE, minY = Double.MAX_VALUE;
        for (com.google.gson.JsonElement el : arr) {
            com.google.gson.JsonObject obj = el.getAsJsonObject();
            int nx = obj.has("x") ? obj.get("x").getAsInt() : 0;
            int ny = obj.has("y") ? obj.get("y").getAsInt() : 0;
            if (nx < minX) minX = nx;
            if (ny < minY) minY = ny;
        }

        java.util.Map<String, String> idMap = new java.util.HashMap<>();

        double targetWorldX = screenToWorldX(screenX);
        double targetWorldY = screenToWorldY(screenY);

        for (com.google.gson.JsonElement el : arr) {
            com.google.gson.JsonObject obj = el.getAsJsonObject();
            String type = obj.has("type") ? obj.get("type").getAsString() : "message";
            int oldX = obj.has("x") ? obj.get("x").getAsInt() : 0;
            int oldY = obj.has("y") ? obj.get("y").getAsInt() : 0;
            String oldId = obj.get("id").getAsString();

            FlowGraph.Node n = this.graph.addNode(type, (int)(targetWorldX + (oldX - minX)), (int)(targetWorldY + (oldY - minY)));

            if (obj.has("props") && obj.get("props").isJsonObject()) {
                for (String key : obj.getAsJsonObject("props").keySet()) {
                    n.props.put(key, obj.getAsJsonObject("props").get(key).getAsString());
                }
            }

            idMap.put(oldId, n.id);
            this.selectedIds.add(n.id);
        }

        for (com.google.gson.JsonElement el : arr) {
            com.google.gson.JsonObject obj = el.getAsJsonObject();
            String oldId = obj.get("id").getAsString();
            if (obj.has("outputs")) {
                for (com.google.gson.JsonElement out : obj.getAsJsonArray("outputs")) {
                    String toOldId = out.getAsString();
                    if (idMap.containsKey(toOldId)) {
                        this.graph.connect(idMap.get(oldId), idMap.get(toOldId));
                    }
                }
            }
        }
        syncProps();
        this.dirty = true;
        saveSnapshot();
    }

    private void deleteSelection() {
        if (this.selectedIds.isEmpty()) return;
        commitProps();
        for (String id : this.selectedIds) this.graph.removeNode(id);
        this.selectedIds.clear();
        this.dirty = true;
        saveSnapshot();
    }

    public boolean keyPressed(int key)
    {
        if (key == GLFW.GLFW_KEY_ESCAPE)
        {
            if (this.stateMenuOpen) { this.stateMenuOpen = false; return true; }
            if (this.dialogueMenuOpen) { this.dialogueMenuOpen = false; return true; }
            if (this.menuOpen) { this.menuOpen = false; return true; }
            if (this.contextMenuOpen) { this.contextMenuOpen = false; return true; }
            if (this.boxSelecting) { this.boxSelecting = false; return true; }
            if (this.draggingNodes) { this.draggingNodes = false; syncProps(); return true; }
            if (this.wireFromId != null) { this.wireFromId = null; return true; }

            blurAll();
            return false;
        }

        for (UiTextField field : this.fields)
        {
            if (field.keyPressed(key))
            {
                commitProps();
                this.dirty = true;
                return true;
            }
        }

        if (net.minecraft.client.gui.screens.Screen.hasControlDown()) {
            if (key == GLFW.GLFW_KEY_Z) {
                undo();
                return true;
            }
            if (key == GLFW.GLFW_KEY_Y) {
                redo();
                return true;
            }
            if (key == GLFW.GLFW_KEY_C && !this.selectedIds.isEmpty()) {
                copySelection();
                return true;
            }
            if (key == GLFW.GLFW_KEY_V && !clipboardJson.equals("[]")) {
                pasteClipboard(this.menuWorldX * this.scale + this.panX + this.canvasRect.x(), this.menuWorldY * this.scale + this.panY + this.canvasRect.y());
                return true;
            }
        }

        if (key == GLFW.GLFW_KEY_DELETE && !this.selectedIds.isEmpty())
        {
            deleteSelection();
            return true;
        }

        return false;
    }

    public boolean charTyped(char codePoint)
    {
        for (int i = 0; i < this.fields.length; i++)
        {
            UiTextField field = this.fields[i];
            if (field.focused()) {
                dev.scriptbound.ScriptBoundMod.LOGGER.info("[DEBUG] charTyped in field " + i + ": " + codePoint);
            }
            if (field.charTyped(codePoint))
            {
                dev.scriptbound.ScriptBoundMod.LOGGER.info("[DEBUG] field " + i + " accepted char, committing props...");
                commitProps();
                this.dirty = true;
                return true;
            }
        }

        return false;
    }

    private void openMenu(int x, int y)
    {
        this.menuOpen = true;
        this.menuX = x;
        this.menuY = y;
        this.menuWorldX = (x - this.canvasRect.x() - this.panX) / this.scale;
        this.menuWorldY = (y - this.canvasRect.y() - this.panY) / this.scale;
        this.menuScroll = 0;
    }

    private void syncProps()
    {
        FlowGraph.Node node = selectedNode();

        for (UiTextField field : this.fields)
        {
            field.setValue("");
            field.setFocused(false);
        }

        if (node == null)
        {
            return;
        }

        String[][] defs = propDefs(node.type);

        for (int i = 0; i < defs.length && i < this.fields.length; i++)
        {
            this.fields[i].setValue(node.props.getOrDefault(defs[i][0], ""));
        }
    }

    private FlowGraph.Node selectedNode()
    {
        if (this.selectedIds.size() != 1) return null;
        return selectedNodeById(this.selectedIds.iterator().next());
    }

    private FlowGraph.Node selectedNodeById(String id)
    {
        return id == null ? null : this.graph.findNode(id);
    }

    private boolean hasInput(FlowGraph.Node node)
    {
        return node != null && !FlowGraph.isEvent(node.type) && !"comment".equals(node.type);
    }

    private int[] port(FlowGraph.Node node, boolean output)
    {
        if (node == null)
        {
            return null;
        }

        if (!output && !hasInput(node))
        {
            return null;
        }

        int w = Math.round(nodeW() * this.scale);
        int h = Math.round(nodeH() * this.scale);
        int x = worldToScreenX(node.x) + w / 2;
        int y = worldToScreenY(node.y) + (output ? h : 0);
        return new int[] { x, y };
    }

    private boolean portHit(double mx, double my, int px, int py)
    {
        return Math.abs(mx - px) <= portR() + 3 && Math.abs(my - py) <= portR() + 3;
    }

    private int worldToScreenX(int worldX)
    {
        return this.canvasRect.x() + this.panX + Math.round(worldX * this.scale);
    }

    private int worldToScreenY(int worldY)
    {
        return this.canvasRect.y() + this.panY + Math.round(worldY * this.scale);
    }

    private double screenToWorldX(double screenX)
    {
        return (screenX - this.canvasRect.x() - this.panX) / this.scale;
    }

    private double screenToWorldY(double screenY)
    {
        return (screenY - this.canvasRect.y() - this.panY) / this.scale;
    }

    private static UiRect rect()
    {
        return new UiRect(0, 0, 0, 0);
    }

    private static String[][] propDefs(String type)
    {
        return switch (type)
        {
            case "npc" -> new String[][] { {"npcId", "NPC id"}, {"displayName", "Display name"}, {"mobId", "Mob id"} };
            case "message" -> new String[][] { {"text", "Text"}, {"subtitle", "Subtitle"}, {"mode", "Mode (chat/title/actionbar)"} };
            case "dialogue" -> new String[][] { {"dialogue", "Dialogue id"} };
            case "state" -> new String[][] { {"scope", "Scope (player/global)"}, {"key", "Key"}, {"value", "Value"} };
            case "script" -> new String[][] { {"script", "Script id"} };
            case "quest" -> new String[][] { {"op", "Op (give/complete)"}, {"quest", "Quest id"} };
            case "command" -> new String[][] { {"command", "Command"} };
            case "run_trigger" -> new String[][] { {"trigger", "Trigger id"} };
            case "condition" -> new String[][] { {"source", "Check (state/quest/talked)"}, {"key", "Key / quest id / npc id"}, {"op", "Op (>= == exists) or quest status"}, {"value", "Value"} };
            case "delay", "wait" -> new String[][] { {"ticks", "Wait ticks (20 = 1s)"} };
            case "block" -> new String[][] { {"x", "Block X"}, {"y", "Block Y"}, {"z", "Block Z"} };
            case "move_block" -> new String[][] {
                {"toX", "Target X"}, {"toY", "Target Y"}, {"toZ", "Target Z"},
                {"speed", "Speed (slow/normal/fast)"}, {"easing", "Easing (smooth/linear/snappy)"}
            };
            case "teleport" -> new String[][] { {"x", "X"}, {"y", "Y"}, {"z", "Z"} };
            case "sound" -> new String[][] { {"sound", "Sound id"}, {"volume", "Volume"} };
            case "give_item" -> new String[][] { {"item", "Item id"}, {"count", "Count"} };
            case "play_film" -> new String[][] { {"film", "BBS film id"} };
            case "faction" -> new String[][] { {"faction", "Faction id"}, {"op", "Op (add/set)"}, {"value", "Value"} };
            case "give_xp" -> new String[][] { {"amount", "Amount"}, {"levels", "Is Levels (true/false)"} };
            case "damage" -> new String[][] { {"amount", "Damage (negative to heal)"} };
            case "effect" -> new String[][] { {"effect", "Effect ID"}, {"duration", "Duration (ticks)"}, {"amplifier", "Amplifier"}, {"particles", "Show Particles (true/false)"} };
            case "comment" -> new String[][] { {"text", "Comment"} };
            case "block_left", "block_right" -> new String[][] { {"x", "Block X"}, {"y", "Block Y"}, {"z", "Block Z"} };
            case "region_enter", "region_exit" -> new String[][] { {"x", "Region block X"}, {"y", "Region block Y"}, {"z", "Region block Z"}, {"radius", "Radius (optional)"} };
            default -> new String[0][];
        };
    }

    private static String summary(FlowGraph.Node node)
    {
        return switch (node.type)
        {
            case "npc" -> node.props.getOrDefault("displayName", node.props.getOrDefault("npcId", ""));
            case "message", "comment" -> node.props.getOrDefault("text", "");
            case "dialogue" -> node.props.getOrDefault("dialogue", "");
            case "state" -> node.props.getOrDefault("key", "");
            case "script" -> node.props.getOrDefault("script", "");
            case "quest" -> node.props.getOrDefault("op", "give") + " " + node.props.getOrDefault("quest", "");
            case "command" -> node.props.getOrDefault("command", "");
            case "run_trigger" -> node.props.getOrDefault("trigger", "");
            case "condition" -> node.props.getOrDefault("key", "") + " " + node.props.getOrDefault("op", ">=") + " " + node.props.getOrDefault("value", "1");
            case "delay", "wait" -> node.props.getOrDefault("ticks", "20") + " ticks";
            case "block" -> posSummary(node, "block");
            case "move_block" -> node.props.getOrDefault("speed", "normal") + " -> " + node.props.getOrDefault("toX", "?") + "," + node.props.getOrDefault("toY", "?") + "," + node.props.getOrDefault("toZ", "?");
            case "teleport" -> node.props.getOrDefault("x", "0") + " " + node.props.getOrDefault("y", "64") + " " + node.props.getOrDefault("z", "0");
            case "sound" -> node.props.getOrDefault("sound", "");
            case "give_item" -> node.props.getOrDefault("item", "") + " x" + node.props.getOrDefault("count", "1");
            case "play_film" -> node.props.getOrDefault("film", "");
            case "faction" -> node.props.getOrDefault("op", "add") + " " + node.props.getOrDefault("value", "1") + " " + node.props.getOrDefault("faction", "");
            case "give_xp" -> node.props.getOrDefault("amount", "1") + ("true".equals(node.props.getOrDefault("levels", "false")) ? " levels" : " xp");
            case "damage" -> node.props.getOrDefault("amount", "1.0") + " dmg";
            case "effect" -> node.props.getOrDefault("effect", "");
            case "player_join" -> "on join";
            case "player_chat" -> "on chat";
            case "player_death" -> "on death";
            case "player_respawn" -> "on respawn";
            case "block_left" -> posSummary(node, "LMB block");
            case "block_right" -> posSummary(node, "RMB block");
            case "region_enter" -> posSummary(node, "enter region");
            case "region_exit" -> posSummary(node, "exit region");
            case "manual" -> "manual run";
            default -> "";
        };
    }

    private static boolean supportsDetect(String type)
    {
        return switch (type)
        {
            case "npc", "block", "block_left", "block_right", "region_enter", "region_exit", "move_block" -> true;
            default -> false;
        };
    }

    private void detectIntoNode(FlowGraph.Node node)
    {
        Minecraft mc = Minecraft.getInstance();

        if ("npc".equals(node.type))
        {
            NpcEntity npc = findTargetNpc(mc);

            if (npc == null || npc.getBlueprintId().isBlank())
            {
                return;
            }

            node.props.put("npcId", npc.getBlueprintId());

            if (npc.hasCustomName())
            {
                node.props.put("displayName", npc.getCustomName().getString());
            }

            String form = npc.getFormJson();

            if (form != null && !form.isBlank())
            {
                try
                {
                    com.google.gson.JsonObject formObj = com.google.gson.JsonParser.parseString(form).getAsJsonObject();

                    if (formObj.has("mobId"))
                    {
                        node.props.put("mobId", formObj.get("mobId").getAsString());
                    }
                }
                catch (RuntimeException ignored)
                {
                }
            }

            syncProps();
            this.dirty = true;
            return;
        }

        if (mc.hitResult instanceof BlockHitResult blockHit && mc.hitResult.getType() == HitResult.Type.BLOCK)
        {
            BlockPos pos = blockHit.getBlockPos();

            if ("move_block".equals(node.type))
            {
                node.props.put("toX", String.valueOf(pos.getX()));
                node.props.put("toY", String.valueOf(pos.getY()));
                node.props.put("toZ", String.valueOf(pos.getZ()));
            }
            else
            {
                node.props.put("x", String.valueOf(pos.getX()));
                node.props.put("y", String.valueOf(pos.getY()));
                node.props.put("z", String.valueOf(pos.getZ()));
            }

            syncProps();
            this.dirty = true;
        }
    }

    private static NpcEntity findTargetNpc(Minecraft mc)
    {
        if (mc.hitResult instanceof EntityHitResult entityHit && entityHit.getEntity() instanceof NpcEntity npc)
        {
            return npc;
        }

        if (mc.player == null || mc.level == null)
        {
            return null;
        }

        NpcEntity nearest = null;
        double best = 12 * 12;

        for (Entity entity : mc.level.entitiesForRendering())
        {
            if (entity instanceof NpcEntity npc)
            {
                double distance = npc.distanceToSqr(mc.player);

                if (distance < best)
                {
                    best = distance;
                    nearest = npc;
                }
            }
        }

        return nearest;
    }

    private static String posSummary(FlowGraph.Node node, String fallback)
    {
        String x = node.props.getOrDefault("x", "");
        String y = node.props.getOrDefault("y", "");
        String z = node.props.getOrDefault("z", "");

        if (x.isBlank() || y.isBlank() || z.isBlank())
        {
            return fallback;
        }

        return x + " " + y + " " + z;
    }

    private static String titleCase(String id)
    {
        if (id == null || id.isBlank())
        {
            return "";
        }

        String[] parts = id.replace('_', ' ').split(" ");
        StringBuilder builder = new StringBuilder();

        for (String part : parts)
        {
            if (part.isBlank())
            {
                continue;
            }

            if (!builder.isEmpty())
            {
                builder.append(' ');
            }

            builder.append(Character.toUpperCase(part.charAt(0)));

            if (part.length() > 1)
            {
                builder.append(part.substring(1));
            }
        }

        return builder.toString();
    }
}
