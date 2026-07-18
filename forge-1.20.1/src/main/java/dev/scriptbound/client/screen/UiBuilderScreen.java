package dev.scriptbound.client.screen;

import dev.scriptbound.client.ui.UiDraw;
import dev.scriptbound.client.ui.UiElementRenderer;
import dev.scriptbound.client.ui.UiRect;
import dev.scriptbound.client.ui.UiTextField;
import dev.scriptbound.client.ui.UiTheme;
import dev.scriptbound.network.NetworkHandler;
import dev.scriptbound.network.packet.SaveUiPacket;
import dev.scriptbound.ui.UiAnchor;
import dev.scriptbound.ui.UiDefinition;
import dev.scriptbound.ui.UiElement;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

import java.util.ArrayList;
import java.util.List;

public class UiBuilderScreen extends Screen
{
    private static final String[] BIND_SCOPES = { "", "player", "global" };
    private static final String[] ALIGNS = { "left", "center", "right" };

    private record TreeRow(UiElement el, int depth, List<UiElement> parent) {}

    private final String docId;
    private final UiDefinition def;
    private UiElement sel;
    private List<UiElement> selParent;
    private UiElement lastSynced;

    private boolean dragging;
    private boolean resizing;
    private double dragX, dragY;

    private final UiTextField fId = new UiTextField().hint("id");
    private final UiTextField fX = new UiTextField();
    private final UiTextField fY = new UiTextField();
    private final UiTextField fW = new UiTextField();
    private final UiTextField fH = new UiTextField();
    private final UiTextField fColor = new UiTextField().hint("#AARRGGBB");
    private final UiTextField fBorder = new UiTextField().hint("#AARRGGBB");
    private final UiTextField fText = new UiTextField().hint("text / %player.key%");
    private final UiTextField fTextColor = new UiTextField().hint("#FFFFFFFF");
    private final UiTextField fOnClick = new UiTextField().hint("trigger id");
    private final UiTextField fImage = new UiTextField().hint("texture path");
    private final UiTextField fBindKey = new UiTextField().hint("state key");
    private final UiTextField fMax = new UiTextField().hint("max");
    private final UiTextField[] fields = { fId, fX, fY, fW, fH, fColor, fBorder, fText, fTextColor, fOnClick, fImage, fBindKey, fMax };
    private final java.util.Map<UiTextField, UiRect> fieldRects = new java.util.IdentityHashMap<>();

    private UiRect frame = new UiRect(0, 0, 0, 0);
    private UiRect closeRect = new UiRect(0, 0, 0, 0);
    private final List<UiRect> toolbar = new ArrayList<>();
    private final List<UiRect> treeRowRects = new ArrayList<>();
    private final List<TreeRow> treeData = new ArrayList<>();
    private UiRect anchorBtn = new UiRect(0, 0, 0, 0);
    private UiRect alignBtn = new UiRect(0, 0, 0, 0);
    private UiRect barTextBtn = new UiRect(0, 0, 0, 0);
    private UiRect scopeBtn = new UiRect(0, 0, 0, 0);
    private UiRect deleteBtn = new UiRect(0, 0, 0, 0);
    private int propScroll;
    private int propViewTop;
    private int propViewBottom;

    public UiBuilderScreen(String id, String json)
    {
        super(Component.literal("UI Builder: " + id));
        this.docId = id;
        UiDefinition parsed;

        try
        {
            parsed = UiDefinition.fromJsonString(id, json);
        }
        catch (Exception e)
        {
            parsed = new UiDefinition();
            parsed.id = id;
        }

        this.def = parsed;
        this.selParent = this.def.elements;

        if (!this.def.elements.isEmpty())
        {
            this.sel = this.def.elements.get(0);
        }
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick)
    {
        this.renderBackground(graphics);
        UiDraw.panel(graphics, 0, 0, this.width, this.height);

        int pad = 8;
        int ph = UiTheme.PANEL_HEADER_H;

        graphics.drawString(this.font, "ScriptBound Engine", pad + 2, 10, UiTheme.TEXT);
        graphics.drawString(this.font, "UI Builder  ·  " + this.docId, pad + 2, 22, UiTheme.TEXT_DIM);
        UiDraw.badge(graphics, this.font, this.width - 96, 9, this.def.mode == UiDefinition.Mode.HUD ? "HUD" : "SCREEN");
        this.closeRect = new UiRect(this.width - 26, 8, 18, 18);
        UiDraw.iconButton(graphics, this.closeRect, "CLOSE", this.closeRect.contains(mouseX, mouseY), false);

        int top = 34;
        int toolbarH = 18;
        int rightW = 174;
        int rightX = this.width - rightW - pad;

        this.toolbar.clear();
        int tx = pad;
        String[] tools = { "+ Panel", "+ Label", "+ Button", "+ Bar", "+ Image",
            this.def.mode == UiDefinition.Mode.HUD ? "Mode: HUD" : "Mode: SCREEN", "Save" };

        for (int i = 0; i < tools.length; i++)
        {
            int w = i < 5 ? 50 : (i == 5 ? 82 : 52);
            UiRect r = new UiRect(tx, top, w, toolbarH);
            this.toolbar.add(r);
            UiDraw.button(graphics, this.font, r, tools[i], null, r.contains(mouseX, mouseY), i == 6, true);
            tx += w + 4;
        }

        syncSelection();
        applyFields();

        int contentY = top + toolbarH + 6;

        int canvasX = pad;
        int canvasW = rightX - pad - canvasX;
        int canvasH = this.height - contentY - pad;
        UiDraw.panel(graphics, canvasX, contentY, canvasW, canvasH);
        UiDraw.panelHeader(graphics, this.font, canvasX, contentY, canvasW, "Canvas — preview");

        this.frame = new UiRect(canvasX + 6, contentY + ph + 6, canvasW - 12, canvasH - ph - 12);
        UiDraw.inset(graphics, this.frame.x() - 1, this.frame.y() - 1, this.frame.w() + 2, this.frame.h() + 2);
        graphics.fill(this.frame.x(), this.frame.y(), this.frame.x() + this.frame.w(), this.frame.y() + this.frame.h(), 0xFF0C0C10);
        graphics.enableScissor(this.frame.x(), this.frame.y(), this.frame.x() + this.frame.w(), this.frame.y() + this.frame.h());

        for (UiElement element : this.def.elements)
        {
            UiElementRenderer.render(graphics, element, this.frame.x(), this.frame.y(), this.frame.w(), this.frame.h());
        }

        if (this.sel != null)
        {
            int[] box = absBox(this.sel);

            if (box != null)
            {
                UiDraw.outlineRounded(graphics, box[0] - 1, box[1] - 1, box[2] + 2, box[3] + 2, UiTheme.ACCENT, 0);
                graphics.fill(box[0] + box[2] - 5, box[1] + box[3] - 5, box[0] + box[2], box[1] + box[3], UiTheme.ACCENT);
            }
        }

        graphics.disableScissor();

        int elemH = 132;
        UiDraw.panel(graphics, rightX, contentY, rightW, elemH);
        UiDraw.panelHeader(graphics, this.font, rightX, contentY, rightW, "Elements");
        int listX = rightX + 6;
        int listY = contentY + ph + 4;
        int listW = rightW - 12;
        int listH = elemH - ph - 8;
        UiDraw.inset(graphics, listX, listY, listW, listH);

        this.treeData.clear();
        flatten(this.def.elements, 0);
        this.treeRowRects.clear();
        graphics.enableScissor(listX, listY, listX + listW, listY + listH);
        int ry = listY + 3;

        for (TreeRow row : this.treeData)
        {
            UiRect r = new UiRect(listX + 2, ry, listW - 4, 14);
            this.treeRowRects.add(r);

            if (row.el == this.sel)
            {
                UiDraw.fillRounded(graphics, r.x(), r.y(), r.w(), r.h(), UiTheme.ROW_SELECT, 0);
            }
            else if (r.contains(mouseX, mouseY))
            {
                UiDraw.fillRounded(graphics, r.x(), r.y(), r.w(), r.h(), UiTheme.ROW_HOVER, 0);
            }

            String name = row.el.id == null || row.el.id.isBlank() ? "(" + row.el.type + ")" : row.el.id;
            graphics.drawString(this.font, name, r.x() + 4 + row.depth * 9, r.y() + 3, row.el == this.sel ? 0xFFFFFFFF : UiTheme.TEXT, false);
            graphics.drawString(this.font, row.el.type, r.x() + r.w() - this.font.width(row.el.type) - 4, r.y() + 3, UiTheme.TEXT_DIM, false);
            ry += 15;
        }

        graphics.disableScissor();

        int propY = contentY + elemH + pad;
        int propH = this.height - propY - pad;
        UiDraw.panel(graphics, rightX, propY, rightW, propH);
        UiDraw.panelHeader(graphics, this.font, rightX, propY, rightW, "Properties");

        int px = rightX + 6;
        int pw = rightW - 12;
        int half = (pw - 4) / 2;
        this.propViewTop = propY + ph + 2;
        this.propViewBottom = propY + propH - 4;
        int viewH = this.propViewBottom - this.propViewTop;
        int contentH = this.sel != null ? 348 : 14;
        this.propScroll = Math.max(0, Math.min(this.propScroll, contentH - viewH));

        graphics.enableScissor(px - 2, this.propViewTop, rightX + rightW - 2, this.propViewBottom);
        int py = this.propViewTop - this.propScroll;

        if (this.sel != null)
        {
            label(graphics, px, py, "id"); rf(graphics, this.fId, new UiRect(px, py + 9, pw, 14)); py += 26;

            label(graphics, px, py, "x / y");
            rf(graphics, this.fX, new UiRect(px, py + 9, half, 14));
            rf(graphics, this.fY, new UiRect(px + half + 4, py + 9, half, 14)); py += 26;

            label(graphics, px, py, "w / h");
            rf(graphics, this.fW, new UiRect(px, py + 9, half, 14));
            rf(graphics, this.fH, new UiRect(px + half + 4, py + 9, half, 14)); py += 26;

            this.anchorBtn = new UiRect(px, py, pw, 16);
            UiDraw.button(graphics, this.font, this.anchorBtn, "anchor: " + this.sel.anchor.id(), null, this.anchorBtn.contains(mouseX, mouseY), false, true); py += 20;

            this.alignBtn = new UiRect(px, py, half, 16);
            UiDraw.button(graphics, this.font, this.alignBtn, "align: " + this.sel.align, null, this.alignBtn.contains(mouseX, mouseY), false, true);
            this.barTextBtn = new UiRect(px + half + 4, py, half, 16);
            UiDraw.button(graphics, this.font, this.barTextBtn, this.sel.barText ? "bar txt: on" : "bar txt: off", null, this.barTextBtn.contains(mouseX, mouseY), this.sel.barText, true); py += 22;

            label(graphics, px, py, "fill color"); rf(graphics, this.fColor, new UiRect(px, py + 9, pw, 14)); py += 26;
            label(graphics, px, py, "border color"); rf(graphics, this.fBorder, new UiRect(px, py + 9, pw, 14)); py += 26;
            label(graphics, px, py, "text"); rf(graphics, this.fText, new UiRect(px, py + 9, pw, 14)); py += 26;
            label(graphics, px, py, "text color"); rf(graphics, this.fTextColor, new UiRect(px, py + 9, pw, 14)); py += 26;
            label(graphics, px, py, "image (texture)"); rf(graphics, this.fImage, new UiRect(px, py + 9, pw, 14)); py += 26;
            label(graphics, px, py, "on click (trigger)"); rf(graphics, this.fOnClick, new UiRect(px, py + 9, pw, 14)); py += 26;

            this.scopeBtn = new UiRect(px, py, half, 16);
            UiDraw.button(graphics, this.font, this.scopeBtn, "bind: " + (this.sel.bindScope.isBlank() ? "none" : this.sel.bindScope), null, this.scopeBtn.contains(mouseX, mouseY), false, true);
            rf(graphics, this.fMax, new UiRect(px + half + 4, py, half, 16)); py += 22;

            label(graphics, px, py, "bind key"); rf(graphics, this.fBindKey, new UiRect(px, py + 9, pw, 14)); py += 26;

            this.deleteBtn = new UiRect(px, py, pw, 16);
            UiDraw.button(graphics, this.font, this.deleteBtn, "Delete element", null, this.deleteBtn.contains(mouseX, mouseY), false, true);
        }
        else
        {
            graphics.drawString(this.font, "Select or add an element.", px, py, UiTheme.TEXT_DIM, false);
        }

        graphics.disableScissor();
        UiDraw.scrollbar(graphics, rightX + rightW - 6, this.propViewTop, viewH, contentH, viewH, this.propScroll);

        super.render(graphics, mouseX, mouseY, partialTick);
    }

    private void flatten(List<UiElement> list, int depth)
    {
        for (UiElement e : list)
        {
            this.treeData.add(new TreeRow(e, depth, list));
            flatten(e.children, depth + 1);
        }
    }

    private void label(GuiGraphics graphics, int x, int y, String text)
    {
        graphics.drawString(this.font, text, x, y, UiTheme.TEXT_DIM, false);
    }

    private void rf(GuiGraphics graphics, UiTextField field, UiRect rect)
    {
        this.fieldRects.put(field, rect);
        field.render(graphics, this.font, rect);
    }

    private void syncSelection()
    {
        if (this.sel == this.lastSynced)
        {
            return;
        }

        this.lastSynced = this.sel;

        if (this.sel == null)
        {
            return;
        }

        this.fId.setValue(this.sel.id);
        this.fX.setValue(String.valueOf(this.sel.x));
        this.fY.setValue(String.valueOf(this.sel.y));
        this.fW.setValue(String.valueOf(this.sel.w));
        this.fH.setValue(String.valueOf(this.sel.h));
        this.fColor.setValue(UiElement.colorString(this.sel.color));
        this.fBorder.setValue(UiElement.colorString(this.sel.border));
        this.fText.setValue(this.sel.text);
        this.fTextColor.setValue(UiElement.colorString(this.sel.textColor));
        this.fImage.setValue(this.sel.image);
        this.fOnClick.setValue(this.sel.onClick);
        this.fBindKey.setValue(this.sel.bindKey);
        this.fMax.setValue(String.valueOf(this.sel.max));
    }

    private void applyFields()
    {
        if (this.sel == null)
        {
            return;
        }

        this.sel.id = this.fId.value();
        if (!this.fX.focused() || isNumber(this.fX.value())) this.sel.x = parseInt(this.fX.value(), this.sel.x);
        if (!this.fY.focused() || isNumber(this.fY.value())) this.sel.y = parseInt(this.fY.value(), this.sel.y);
        if (!this.fW.focused() || isNumber(this.fW.value())) this.sel.w = Math.max(2, parseInt(this.fW.value(), this.sel.w));
        if (!this.fH.focused() || isNumber(this.fH.value())) this.sel.h = Math.max(2, parseInt(this.fH.value(), this.sel.h));
        this.sel.color = UiElement.parseColor(this.fColor.value(), this.sel.color);
        this.sel.border = UiElement.parseColor(this.fBorder.value(), this.sel.border);
        this.sel.text = this.fText.value();
        this.sel.textColor = UiElement.parseColor(this.fTextColor.value(), this.sel.textColor);
        this.sel.image = this.fImage.value();
        this.sel.onClick = this.fOnClick.value();
        this.sel.bindKey = this.fBindKey.value();
        this.sel.max = parseDouble(this.fMax.value(), this.sel.max);
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button)
    {
        if (this.closeRect.contains(mouseX, mouseY))
        {
            this.minecraft.setScreen(null);
            return true;
        }

        for (int i = 0; i < this.toolbar.size(); i++)
        {
            if (this.toolbar.get(i).contains(mouseX, mouseY))
            {
                handleToolbar(i);
                return true;
            }
        }

        for (int i = 0; i < this.treeRowRects.size(); i++)
        {
            if (this.treeRowRects.get(i).contains(mouseX, mouseY))
            {
                TreeRow row = this.treeData.get(i);
                this.sel = row.el;
                this.selParent = row.parent;
                return true;
            }
        }

        boolean inProps = mouseY >= this.propViewTop && mouseY <= this.propViewBottom;

        if (inProps && this.sel != null)
        {
            if (this.anchorBtn.contains(mouseX, mouseY))
            {
                UiAnchor[] all = UiAnchor.values();
                this.sel.anchor = all[(this.sel.anchor.ordinal() + 1) % all.length];
                return true;
            }
            if (this.alignBtn.contains(mouseX, mouseY))
            {
                int idx = 0;
                for (int i = 0; i < ALIGNS.length; i++) if (ALIGNS[i].equals(this.sel.align)) idx = i;
                this.sel.align = ALIGNS[(idx + 1) % ALIGNS.length];
                return true;
            }
            if (this.barTextBtn.contains(mouseX, mouseY))
            {
                this.sel.barText = !this.sel.barText;
                return true;
            }
            if (this.scopeBtn.contains(mouseX, mouseY))
            {
                cycleScope();
                return true;
            }
            if (this.deleteBtn.contains(mouseX, mouseY))
            {
                this.selParent.remove(this.sel);
                this.sel = null;
                this.lastSynced = null;
                return true;
            }
        }

        if (inProps)
        {
            boolean focusedAny = false;
            for (UiTextField f : this.fields)
            {
                UiRect r = this.fieldRects.get(f);

                if (r != null && f.mouseClicked(r, mouseX, mouseY, button))
                {
                    focusedAny = true;
                }
                else
                {
                    f.setFocused(false);
                }
            }
            if (focusedAny)
            {
                return true;
            }
        }

        if (this.frame.contains(mouseX, mouseY))
        {
            UiElement hit = hitAt(this.def.elements, this.frame.x(), this.frame.y(), this.frame.w(), this.frame.h(), mouseX, mouseY);

            if (hit != null)
            {
                this.sel = hit;
                this.selParent = this.hitParentList;
                int[] box = absBox(hit);
                this.resizing = box != null && mouseX >= box[0] + box[2] - 6 && mouseY >= box[1] + box[3] - 6;
                this.dragging = !this.resizing;
                this.dragX = mouseX;
                this.dragY = mouseY;
                return true;
            }

            this.sel = null;
            return true;
        }

        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseDragged(double mouseX, double mouseY, int button, double dx, double dy)
    {
        if (this.sel != null && (this.dragging || this.resizing))
        {
            int ddx = (int) Math.round(mouseX - this.dragX);
            int ddy = (int) Math.round(mouseY - this.dragY);

            if (this.dragging)
            {
                this.sel.x += ddx;
                this.sel.y += ddy;
            }
            else
            {
                this.sel.w = Math.max(2, this.sel.w + ddx);
                this.sel.h = Math.max(2, this.sel.h + ddy);
            }

            this.dragX = mouseX;
            this.dragY = mouseY;
            this.lastSynced = null;
            return true;
        }

        return super.mouseDragged(mouseX, mouseY, button, dx, dy);
    }

    @Override
    public boolean mouseReleased(double mouseX, double mouseY, int button)
    {
        if (this.dragging && this.sel != null)
        {
            reparentOnDrop(mouseX, mouseY);
        }

        this.dragging = false;
        this.resizing = false;
        return super.mouseReleased(mouseX, mouseY, button);
    }

    private void reparentOnDrop(double mouseX, double mouseY)
    {
        if (!this.frame.contains(mouseX, mouseY))
        {
            return;
        }

        UiElement targetPanel = panelAt(this.def.elements, this.frame.x(), this.frame.y(), this.frame.w(), this.frame.h(), mouseX, mouseY, this.sel);
        List<UiElement> targetList = targetPanel != null ? targetPanel.children : this.def.elements;

        if (targetList == this.selParent)
        {
            return;
        }

        int[] selAbs = absBox(this.sel);
        int[] parAbs = targetPanel != null ? absBox(targetPanel) : new int[] { this.frame.x(), this.frame.y(), this.frame.w(), this.frame.h() };

        if (selAbs == null || parAbs == null)
        {
            return;
        }

        this.selParent.remove(this.sel);
        this.sel.anchor = UiAnchor.TOP_LEFT;
        this.sel.x = selAbs[0] - parAbs[0];
        this.sel.y = selAbs[1] - parAbs[1];
        targetList.add(this.sel);
        this.selParent = targetList;
        this.lastSynced = null;
    }

    private List<UiElement> hitParentList;

    private UiElement hitAt(List<UiElement> list, int px, int py, int pw, int ph, double mx, double my)
    {
        for (int i = list.size() - 1; i >= 0; i--)
        {
            UiElement e = list.get(i);

            if (!e.visible)
            {
                continue;
            }

            int ex = e.resolveX(px, pw);
            int ey = e.resolveY(py, ph);

            UiElement child = hitAt(e.children, ex, ey, e.w, e.h, mx, my);

            if (child != null)
            {
                return child;
            }

            if (mx >= ex && mx < ex + e.w && my >= ey && my < ey + e.h)
            {
                this.hitParentList = list;
                return e;
            }
        }

        return null;
    }

    private UiElement panelAt(List<UiElement> list, int px, int py, int pw, int ph, double mx, double my, UiElement exclude)
    {
        for (int i = list.size() - 1; i >= 0; i--)
        {
            UiElement e = list.get(i);

            if (e == exclude || !e.visible)
            {
                continue;
            }

            int ex = e.resolveX(px, pw);
            int ey = e.resolveY(py, ph);

            UiElement deeper = panelAt(e.children, ex, ey, e.w, e.h, mx, my, exclude);

            if (deeper != null)
            {
                return deeper;
            }

            if ("panel".equals(e.type) && mx >= ex && mx < ex + e.w && my >= ey && my < ey + e.h)
            {
                return e;
            }
        }

        return null;
    }

    private int[] absBox(UiElement target)
    {
        return absBox(this.def.elements, this.frame.x(), this.frame.y(), this.frame.w(), this.frame.h(), target);
    }

    private int[] absBox(List<UiElement> list, int px, int py, int pw, int ph, UiElement target)
    {
        for (UiElement e : list)
        {
            int ex = e.resolveX(px, pw);
            int ey = e.resolveY(py, ph);

            if (e == target)
            {
                return new int[] { ex, ey, e.w, e.h };
            }

            int[] r = absBox(e.children, ex, ey, e.w, e.h, target);

            if (r != null)
            {
                return r;
            }
        }

        return null;
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double delta)
    {
        if (mouseY >= this.propViewTop && mouseY <= this.propViewBottom && mouseX >= this.width - 180)
        {
            this.propScroll = Math.max(0, this.propScroll - (int) (delta * 16));
            return true;
        }

        return super.mouseScrolled(mouseX, mouseY, delta);
    }

    @Override
    public boolean keyPressed(int key, int scan, int mods)
    {
        for (UiTextField f : this.fields)
        {
            if (f.focused() && f.keyPressed(key))
            {
                return true;
            }
        }

        return super.keyPressed(key, scan, mods);
    }

    @Override
    public boolean charTyped(char c, int mods)
    {
        for (UiTextField f : this.fields)
        {
            if (f.focused() && f.charTyped(c))
            {
                return true;
            }
        }

        return super.charTyped(c, mods);
    }

    private void handleToolbar(int i)
    {
        switch (i)
        {
            case 0 -> addElement("panel");
            case 1 -> addElement("label");
            case 2 -> addElement("button");
            case 3 -> addElement("bar");
            case 4 -> addElement("image");
            case 5 -> this.def.mode = this.def.mode == UiDefinition.Mode.HUD ? UiDefinition.Mode.SCREEN : UiDefinition.Mode.HUD;
            case 6 -> save();
        }
    }

    private void addElement(String type)
    {
        UiElement e = new UiElement();
        e.type = type;
        e.anchor = UiAnchor.TOP_LEFT;
        e.x = 12;
        e.y = 12;

        switch (type)
        {
            case "label" -> { e.text = "Label"; e.w = 60; e.h = 10; e.color = 0; }
            case "button" -> { e.text = "Button"; e.w = 70; e.h = 18; e.color = 0xFF333333; e.border = 0xFF555555; }
            case "bar" -> { e.w = 100; e.h = 8; e.color = 0xFF33CC55; e.border = 0xFF000000; e.max = 20; }
            case "image" -> { e.w = 32; e.h = 32; e.color = 0; e.image = "minecraft:textures/item/apple.png"; }
            default -> { e.w = 120; e.h = 40; e.color = 0xB0101018; e.border = 0xFF3A6EA5; }
        }

        List<UiElement> target = (this.sel != null && "panel".equals(this.sel.type)) ? this.sel.children : this.def.elements;
        e.id = type + countAll();
        target.add(e);
        this.sel = e;
        this.selParent = target;
        this.lastSynced = null;
    }

    private int countAll()
    {
        this.treeData.clear();
        flatten(this.def.elements, 0);
        return this.treeData.size() + 1;
    }

    private void cycleScope()
    {
        String current = this.sel.bindScope == null ? "" : this.sel.bindScope;
        int idx = 0;

        for (int i = 0; i < BIND_SCOPES.length; i++)
        {
            if (BIND_SCOPES[i].equals(current)) idx = i;
        }

        this.sel.bindScope = BIND_SCOPES[(idx + 1) % BIND_SCOPES.length];
    }

    private void save()
    {
        applyFields();
        NetworkHandler.CHANNEL.sendToServer(new SaveUiPacket(this.docId, this.def.toJson().toString()));
    }

    @Override
    public boolean isPauseScreen()
    {
        return false;
    }

    private static boolean isNumber(String s)
    {
        if (s == null || s.isBlank() || s.equals("-")) return false;
        try { Integer.parseInt(s.trim()); return true; } catch (NumberFormatException e) { return false; }
    }

    private static int parseInt(String s, int fallback)
    {
        try { return Integer.parseInt(s.trim()); } catch (Exception e) { return fallback; }
    }

    private static double parseDouble(String s, double fallback)
    {
        try { return Double.parseDouble(s.trim()); } catch (Exception e) { return fallback; }
    }
}
