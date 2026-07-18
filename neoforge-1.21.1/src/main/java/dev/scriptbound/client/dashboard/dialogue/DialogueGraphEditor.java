package dev.scriptbound.client.dashboard.dialogue;

import dev.scriptbound.client.ui.UiDraw;
import dev.scriptbound.client.ui.UiRect;
import dev.scriptbound.client.ui.UiScale;
import dev.scriptbound.client.ui.UiTextField;
import dev.scriptbound.client.ui.UiTheme;
import dev.scriptbound.dashboard.DashboardSnapshot;
import dev.scriptbound.dialogue.DialogueGraph;
import dev.scriptbound.entity.NpcEntity;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.EntityHitResult;
import org.lwjgl.glfw.GLFW;

import java.util.ArrayList;
import java.util.List;

public final class DialogueGraphEditor
{
    private static final float MIN_SCALE = 0.4F;
    private static final float MAX_SCALE = 2.2F;

    private static final String[][] MENU = {
        {"#", "DIALOGUE"},
        {"start", "Dialogue Start"},
        {"choices_pair", "Add 2 Choices"},
        {"choice_extra", "+ 1 More Choice"},
        {"#", "LOGIC"},
        {"wait", "Wait"},
        {"#", "ACTIONS"},
        {"message", "Message"},
        {"dialogue", "Next Dialogue"},
        {"run_trigger", "Run Trigger"},
        {"command", "Command"},
        {"state", "Set State"},
        {"script", "Run Script"},
        {"comment", "Comment"}
    };

    private final DialogueGraph graph = new DialogueGraph();
    private boolean dirty;

    private float scale = 1.0F;
    private int panX = 40;
    private int panY = 40;

    private String selectedId;
    private String wireFromId;
    private String dragNodeId;
    private double dragOffWorldX;
    private double dragOffWorldY;
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
        new UiTextField(), new UiTextField(), new UiTextField(), new UiTextField()
    };
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

    public void load(String json)
    {
        DialogueGraph parsed = DialogueGraph.fromJson(json);
        this.graph.nodes().clear();
        this.graph.links().clear();
        this.graph.nodes().addAll(parsed.nodes());
        this.graph.links().addAll(parsed.links());
        this.dirty = false;
        this.selectedId = null;
        this.wireFromId = null;
        this.menuOpen = false;
        this.scale = Math.min(1.0F, UiScale.factor() + 0.15F);
        this.panX = 40;
        this.panY = 40;
    }

    public String toJson()
    {
        commitProps();
        return this.graph.toJson();
    }

    public void commitProps()
    {
        DialogueGraph.Node node = selectedNode();

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

        graphics.fill(this.canvasRect.x(), this.canvasRect.y(), this.canvasRect.x() + canvas.w(), this.canvasRect.y() + canvas.h(), 0x33000000);
        graphics.renderOutline(this.canvasRect.x(), this.canvasRect.y(), canvas.w(), canvas.h(), UiTheme.BORDER);
        graphics.enableScissor(this.canvasRect.x() + 1, this.canvasRect.y() + 1, this.canvasRect.x() + canvas.w() - 1, this.canvasRect.y() + canvas.h() - 1);
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
        renderToolbar(graphics, font);
        renderProps(graphics, font, data, mouseX, mouseY);

        if (this.menuOpen)
        {
            renderMenu(graphics, font, mouseX, mouseY);
        }
    }

    private void renderToolbar(GuiGraphics graphics, Font font)
    {
        graphics.drawString(font, "Start -> Choices (min 2) -> Action  |  RMB menu  |  wheel zoom", this.canvasRect.x() + 6, this.canvasRect.y() + 4, UiTheme.TEXT_DIM);
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
        for (DialogueGraph.Link link : this.graph.links())
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
        int w = Math.round(UiScale.nodeW() * this.scale);
        int h = Math.round(UiScale.nodeH() * this.scale);
        int portR = UiScale.portR();

        for (DialogueGraph.Node node : this.graph.nodes())
        {
            int x = worldToScreenX(node.x);
            int y = worldToScreenY(node.y);
            boolean selected = node.id.equals(this.selectedId);
            int accent = DialogueGraph.colorFor(node.type);

            graphics.fill(x + 2, y + 2, x + w + 2, y + h + 2, 0x44000000);
            graphics.fill(x, y, x + w, y + h, selected ? UiTheme.PANEL_INSET : UiTheme.PANEL);
            graphics.fill(x, y, x + w, y + 3, accent | 0xFF000000);
            graphics.renderOutline(x, y, w, h, selected ? UiTheme.TEXT_ACCENT : UiTheme.BORDER);

            String title = DialogueGraph.labelFor(node.type);
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
                if (DialogueGraph.hasInput(node.type))
                {
                    drawPort(graphics, x + w / 2, y, accent, portR);
                }

                if (DialogueGraph.hasOutput(node.type))
                {
                    drawPort(graphics, x + w / 2, y + h, accent, portR);
                }
            }
        }
    }

    private void drawPort(GuiGraphics graphics, int cx, int cy, int color, int portR)
    {
        graphics.fill(cx - portR, cy - portR, cx + portR, cy + portR, 0xFF0C0E14);
        graphics.renderOutline(cx - portR, cy - portR, portR * 2, portR * 2, color);
    }

    private void renderProps(GuiGraphics graphics, Font font, DashboardSnapshot data, int mouseX, int mouseY)
    {
        UiDraw.panel(graphics, this.propsRect.x(), this.propsRect.y(), this.propsRect.w(), this.propsRect.h());
        UiDraw.panelHeader(graphics, font, this.propsRect.x(), this.propsRect.y(), this.propsRect.w(), "Node");
        this.detectRect = rect();
        this.deleteNodeRect = rect();

        int x = this.propsRect.x() + 6;
        int y = this.propsRect.y() + UiScale.panelHeaderH() + 6;
        int w = this.propsRect.w() - 12;
        DialogueGraph.Node node = selectedNode();

        if (node == null)
        {
            graphics.drawString(font, "Select a node, or RMB", x, y, UiTheme.TEXT_DIM);
            graphics.drawString(font, "the canvas to add nodes.", x, y + 12, UiTheme.TEXT_DIM);
            graphics.drawString(font, "Wire: Start -> Choices -> Action", x, y + 28, UiTheme.TEXT_DIM);
            return;
        }

        graphics.drawString(font, DialogueGraph.labelFor(node.type), x, y, UiTheme.TEXT_ACCENT);
        y += 14;

        int clipBottom = this.propsRect.y() + this.propsRect.h() - 28;
        graphics.enableScissor(this.propsRect.x() + 1, this.propsRect.y() + UiScale.panelHeaderH(), this.propsRect.x() + this.propsRect.w() - 1, clipBottom);

        String[][] defs = propDefs(node.type);
        int fieldH = Math.max(16, UiScale.px(18));
        int rowH = Math.max(28, UiScale.px(34));

        for (int i = 0; i < defs.length && i < this.fields.length; i++)
        {
            graphics.drawString(font, defs[i][1], x, y, UiTheme.TEXT_DIM);
            this.fields[i].render(graphics, font, new UiRect(x, y + 10, w, fieldH));
            y += rowH;
        }

        if ("start".equals(node.type))
        {
            this.detectRect = new UiRect(x, y, w, fieldH);
            UiDraw.button(graphics, font, this.detectRect, "Detect aimed NPC", "REFRESH", this.detectRect.contains(mouseX, mouseY), false, true);
            y += rowH - 8;
        }

        graphics.disableScissor();

        this.deleteNodeRect = new UiRect(x, this.propsRect.y() + this.propsRect.h() - 24, w, fieldH);
        UiDraw.button(graphics, font, this.deleteNodeRect, "Delete node", "CLOSE", this.deleteNodeRect.contains(mouseX, mouseY), false, true);
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
                boolean disabled = isMenuDisabled(entry[0]);
                UiRect row = new UiRect(x, iy, w, itemH);
                boolean hovered = !disabled && row.contains(mouseX, mouseY);

                if (hovered)
                {
                    graphics.fill(row.x(), row.y(), row.x() + row.w(), row.y() + row.h(), UiTheme.ROW_HOVER);
                }

                graphics.fill(x + 4, iy + 4, x + 10, iy + 10, disabled ? 0xFF555555 : DialogueGraph.colorFor(menuTypeToNode(entry[0])));
                graphics.drawString(font, entry[1], x + 14, iy + 3, disabled ? UiTheme.TEXT_DIM : UiTheme.TEXT);
            }

            iy += itemH;
        }

        if (this.menuScroll > 0) graphics.fill(x + w - 4, y + 2, x + w, y + 6, UiTheme.BORDER_LIGHT);
        if (this.menuScroll + visibleItems < MENU.length) graphics.fill(x + w - 4, y + totalH - 6, x + w, y + totalH - 2, UiTheme.BORDER_LIGHT);
    }

    private boolean isMenuDisabled(String menuId)
    {
        return switch (menuId)
        {
            case "choice_extra" -> this.graph.countChoices() < 2;
            default -> false;
        };
    }

    private static String menuTypeToNode(String menuId)
    {
        return switch (menuId)
        {
            case "choices_pair", "choice_extra" -> "choice";
            default -> menuId;
        };
    }

    public boolean mouseClicked(UiRect canvas, UiRect props, double mouseX, double mouseY, int button, DashboardSnapshot data)
    {
        this.canvasRect = canvas;
        this.propsRect = props;

        if (this.menuOpen)
        {
            return handleMenuClick(mouseX, mouseY);
        }

        if (button == 1 && canvas.contains(mouseX, mouseY))
        {
            openMenu((int) mouseX, (int) mouseY);
            return true;
        }

        if (props.contains(mouseX, mouseY))
        {
            return handlePropsClick(mouseX, mouseY, button);
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
            if (!"#".equals(entry[0]) && !isMenuDisabled(entry[0]) && new UiRect(x, iy, w, itemH).contains(mouseX, mouseY))
            {
                dev.scriptbound.ScriptBoundMod.LOGGER.info("[DEBUG] button clicked: " + entry[1]);
                if (!addFromMenu(entry[0]))
                {
                    this.menuOpen = false;
                    return true;
                }

                commitProps();
                syncProps();
                this.dirty = true;
                this.menuOpen = false;
                dev.scriptbound.ScriptBoundMod.LOGGER.info("[DEBUG] render refresh called");
                return true;
            }

            iy += itemH;
        }

        this.menuOpen = false;
        return true;
    }

    private boolean addFromMenu(String menuId)
    {
        int wx = (int) this.menuWorldX;
        int wy = (int) this.menuWorldY;
        dev.scriptbound.ScriptBoundMod.LOGGER.info("[DEBUG] node creation requested: " + menuId);
        int before = this.graph.nodes().size();

        return switch (menuId)
        {
            case "start" -> {
                DialogueGraph.Node existing = findStartNode();

                if (existing != null)
                {

                    this.selectedId = existing.id;
                    centerOnNode(existing);
                    syncProps();
                    yield false;
                }

                DialogueGraph.Node node = this.graph.addNode("start", wx - UiScale.nodeW() / 2, wy - UiScale.nodeH() / 2);
                dev.scriptbound.ScriptBoundMod.LOGGER.info("[DEBUG] node added to dialogue graph! type: start, id: " + node.id);
                dev.scriptbound.ScriptBoundMod.LOGGER.info("[DEBUG] node count before: " + before + " after: " + this.graph.nodes().size());
                this.selectedId = node.id;
                yield true;
            }
            case "choices_pair" -> {
                DialogueGraph.Node c1 = this.graph.addNode("choice", wx - 70, wy);
                c1.props.put("text", "Option 1");
                DialogueGraph.Node c2 = this.graph.addNode("choice", wx + 20, wy);
                c2.props.put("text", "Option 2");
                DialogueGraph.Node start = findStartNode();

                if (start != null)
                {
                    this.graph.connect(start.id, c1.id);
                    this.graph.connect(start.id, c2.id);
                }

                dev.scriptbound.ScriptBoundMod.LOGGER.info("[DEBUG] 2 nodes added to dialogue graph! type: choice, ids: " + c1.id + ", " + c2.id);
                dev.scriptbound.ScriptBoundMod.LOGGER.info("[DEBUG] node count before: " + before + " after: " + this.graph.nodes().size());
                this.selectedId = c1.id;
                yield true;
            }
            case "choice_extra" -> {
                if (this.graph.countChoices() < 2)
                {
                    yield false;
                }

                DialogueGraph.Node c = this.graph.addNode("choice", wx, wy);
                c.props.put("text", "Option " + (this.graph.countChoices()));
                dev.scriptbound.ScriptBoundMod.LOGGER.info("[DEBUG] node added to dialogue graph! type: choice, id: " + c.id);
                dev.scriptbound.ScriptBoundMod.LOGGER.info("[DEBUG] node count before: " + before + " after: " + this.graph.nodes().size());
                this.selectedId = c.id;
                yield true;
            }
            default -> {
                DialogueGraph.Node node = this.graph.addNode(menuId, wx - UiScale.nodeW() / 2, wy - UiScale.nodeH() / 2);
                dev.scriptbound.ScriptBoundMod.LOGGER.info("[DEBUG] node added to dialogue graph! type: " + menuId + ", id: " + node.id);
                dev.scriptbound.ScriptBoundMod.LOGGER.info("[DEBUG] node count before: " + before + " after: " + this.graph.nodes().size());
                this.selectedId = node.id;
                yield true;
            }
        };
    }

    private boolean hasStartNode()
    {
        return findStartNode() != null;
    }

    private DialogueGraph.Node findStartNode()
    {
        for (DialogueGraph.Node node : this.graph.nodes())
        {
            if ("start".equals(node.type))
            {
                return node;
            }
        }

        return null;
    }

    private boolean handlePropsClick(double mouseX, double mouseY, int button)
    {
        DialogueGraph.Node node = selectedNode();

        if (node == null)
        {
            return true;
        }

        if (this.deleteNodeRect.contains(mouseX, mouseY))
        {
            commitProps();
            this.graph.removeNode(node.id);
            this.selectedId = null;
            this.dirty = true;
            return true;
        }

        if (this.detectRect.contains(mouseX, mouseY) && "start".equals(node.type))
        {
            detectNpcIntoStart(node);
            return true;
        }

        String[][] defs = propDefs(node.type);
        int x = this.propsRect.x() + 6;
        int y = this.propsRect.y() + UiScale.panelHeaderH() + 6 + 14;
        int w = this.propsRect.w() - 12;
        int fieldH = Math.max(16, UiScale.px(18));
        int rowH = Math.max(28, UiScale.px(34));

        for (int i = 0; i < defs.length && i < this.fields.length; i++)
        {
            this.fields[i].mouseClicked(new UiRect(x, y + 10, w, fieldH), mouseX, mouseY, button);
            y += rowH;
        }

        return true;
    }

    private void detectNpcIntoStart(DialogueGraph.Node node)
    {
        Minecraft mc = Minecraft.getInstance();
        NpcEntity npc = findTargetNpc(mc);

        if (npc == null || npc.getBlueprintId().isBlank())
        {
            return;
        }

        node.props.put("npcId", npc.getBlueprintId());

        if (npc.hasCustomName())
        {
            node.props.put("speaker", npc.getCustomName().getString());
        }

        syncProps();
        this.dirty = true;
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

    private boolean handleCanvasClick(double mouseX, double mouseY)
    {
        int w = Math.round(UiScale.nodeW() * this.scale);
        int h = Math.round(UiScale.nodeH() * this.scale);
        int portR = UiScale.portR() + 2;

        for (int i = this.graph.nodes().size() - 1; i >= 0; i--)
        {
            DialogueGraph.Node node = this.graph.nodes().get(i);
            int x = worldToScreenX(node.x);
            int y = worldToScreenY(node.y);

            if (!"comment".equals(node.type))
            {
                if (DialogueGraph.hasOutput(node.type) && portHit(mouseX, mouseY, x + w / 2, y + h, portR))
                {
                    this.wireFromId = node.id;
                    return true;
                }

                if (this.wireFromId != null && DialogueGraph.hasInput(node.type) && portHit(mouseX, mouseY, x + w / 2, y, portR))
                {
                    this.graph.connect(this.wireFromId, node.id);
                    this.wireFromId = null;
                    this.dirty = true;
                    return true;
                }
            }

            if (new UiRect(x, y, w, h).contains(mouseX, mouseY))
            {
                commitProps();
                this.selectedId = node.id;
                syncProps();
                this.dragNodeId = node.id;
                this.dragOffWorldX = screenToWorldX(mouseX) - node.x;
                this.dragOffWorldY = screenToWorldY(mouseY) - node.y;
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
        this.selectedId = null;
        this.panning = true;
        this.panMouseX = mouseX;
        this.panMouseY = mouseY;
        this.panStartX = this.panX;
        this.panStartY = this.panY;
        return true;
    }

    public void mouseReleased(double mouseX, double mouseY, int button)
    {
        this.dragNodeId = null;
        this.panning = false;
    }

    public void mouseDragged(double mouseX, double mouseY, int button)
    {
        if (this.dragNodeId != null)
        {
            DialogueGraph.Node node = selectedNodeById(this.dragNodeId);

            if (node != null)
            {
                node.x = (int) Math.round(screenToWorldX(mouseX) - this.dragOffWorldX);
                node.y = (int) Math.round(screenToWorldY(mouseY) - this.dragOffWorldY);
                this.dirty = true;
            }

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

    public boolean keyPressed(int key)
    {
        if (this.menuOpen && key == GLFW.GLFW_KEY_ESCAPE)
        {
            this.menuOpen = false;
            return true;
        }

        for (UiTextField field : this.fields)
        {
            if (field.focused() && field.keyPressed(key))
            {
                commitProps();
                this.dirty = true;
                return true;
            }
        }

        if (key == GLFW.GLFW_KEY_DELETE && this.selectedId != null)
        {
            this.graph.removeNode(this.selectedId);
            this.selectedId = null;
            this.dirty = true;
            return true;
        }

        return false;
    }

    public boolean charTyped(char codePoint)
    {
        for (UiTextField field : this.fields)
        {
            if (field.focused() && field.charTyped(codePoint))
            {
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
        DialogueGraph.Node node = selectedNode();

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

    private DialogueGraph.Node selectedNode()
    {
        return selectedNodeById(this.selectedId);
    }

    private DialogueGraph.Node selectedNodeById(String id)
    {
        if (id == null)
        {
            return null;
        }

        return this.graph.findNode(id);
    }

    private int[] port(DialogueGraph.Node node, boolean output)
    {
        if (node == null)
        {
            return null;
        }

        int w = Math.round(UiScale.nodeW() * this.scale);
        int h = Math.round(UiScale.nodeH() * this.scale);
        int x = worldToScreenX(node.x);
        int y = worldToScreenY(node.y);

        if (output && DialogueGraph.hasOutput(node.type))
        {
            return new int[] { x + w / 2, y + h };
        }

        if (!output && DialogueGraph.hasInput(node.type))
        {
            return new int[] { x + w / 2, y };
        }

        return null;
    }

    private static boolean portHit(double mouseX, double mouseY, int cx, int cy, int portR)
    {
        return Math.abs(mouseX - cx) <= portR + 4 && Math.abs(mouseY - cy) <= portR + 4;
    }

    private void centerOnNode(DialogueGraph.Node node)
    {
        this.panX = (int) Math.round(this.canvasRect.w() / 2.0 - node.x * this.scale);
        this.panY = (int) Math.round(this.canvasRect.h() / 2.0 - node.y * this.scale);
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

    private static String[][] propDefs(String type)
    {
        return switch (type)
        {
            case "start" -> new String[][] { {"npcId", "NPC id (optional)"}, {"speaker", "Speaker name"}, {"text", "Opening text"} };
            case "choice" -> new String[][] { {"text", "Choice label"} };
            case "message" -> new String[][] { {"text", "Message"}, {"mode", "Mode (chat/actionbar)"} };
            case "run_trigger" -> new String[][] { {"trigger", "Trigger id"} };
            case "dialogue" -> new String[][] { {"dialogue", "Dialogue id to open"} };
            case "wait" -> new String[][] { {"ticks", "Wait ticks (20 = 1s)"} };
            case "command" -> new String[][] { {"command", "Command"} };
            case "state" -> new String[][] { {"scope", "Scope (player/global)"}, {"key", "State key"}, {"value", "Value"} };
            case "script" -> new String[][] { {"script", "Script id"} };
            case "comment" -> new String[][] { {"text", "Note"} };
            default -> new String[][] { {"text", "Value"} };
        };
    }

    private static String summary(DialogueGraph.Node node)
    {
        return switch (node.type)
        {
            case "start" -> node.props.getOrDefault("text", "");
            case "choice" -> node.props.getOrDefault("text", "");
            case "message" -> node.props.getOrDefault("text", "");
            case "run_trigger" -> node.props.getOrDefault("trigger", "");
            case "dialogue" -> node.props.getOrDefault("dialogue", "");
            case "wait" -> node.props.getOrDefault("ticks", "20") + " ticks";
            case "command" -> node.props.getOrDefault("command", "");
            case "script" -> node.props.getOrDefault("script", "");
            default -> "";
        };
    }

    private static UiRect rect()
    {
        return new UiRect(0, 0, 0, 0);
    }
}
