package dev.scriptbound.client.dashboard;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import dev.scriptbound.client.ui.UiDraw;
import dev.scriptbound.client.ui.UiRect;
import dev.scriptbound.client.ui.UiTextArea;
import dev.scriptbound.client.ui.UiTextField;
import dev.scriptbound.client.ui.UiTheme;
import dev.scriptbound.dashboard.DashboardSnapshot;
import dev.scriptbound.client.bbs.BbsFormBridge;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public final class DashboardFormEditor
{
    public enum ConditionType
    {
        ALWAYS,
        STATE_ABSENT,
        STATE_PRESENT,
        STATE_GTE,
        TALKED,
        QUEST_ACTIVE,
        QUEST_READY
    }

    public enum ActionType
    {
        MESSAGE,
        DIALOGUE,
        STATE,
        SCRIPT,
        TRIGGER,
        COMMAND,
        QUEST,
        PLAY_FILM
    }

    public static final class TriggerActionForm
    {
        public ConditionType condition = ConditionType.ALWAYS;
        public String conditionKey = "";
        public String conditionValue = "1";
        public String conditionNpc = "";
        public String conditionQuest = "";
        public ActionType actionType = ActionType.MESSAGE;
        public String text = "";
        public String mode = "chat";
        public String dialogue = "";
        public String script = "";
        public String trigger = "";
        public String command = "";
        public String scope = "player";
        public String stateKey = "";
        public String stateValue = "1";
        public String quest = "";
        public String questOp = "give";
        public String film = "";
    }

    public static final class DialogueLineForm
    {
        public String speaker = "";
        public String text = "";

        public String choicesJson = "";
    }

    public static final class QuestObjectiveForm
    {
        public String type = "talk";
        public String key = "";
        public String value = "1";
        public String entity = "";
        public String count = "1";
        public String npc = "";
    }

    private String category = "";
    private boolean dirty;
    private int scrollY;
    private int contentHeight;
    private int selectedActionIndex = -1;
    private int selectedLineIndex = -1;
    private int selectedObjectiveIndex = -1;

    private final List<TriggerActionForm> triggerActions = new ArrayList<>();
    private final List<DialogueLineForm> dialogueLines = new ArrayList<>();
    private final List<QuestObjectiveForm> questObjectives = new ArrayList<>();

    private final UiTextArea scriptArea = new UiTextArea().hint("JavaScript source...");
    private final UiTextField questTitle = new UiTextField().hint("Quest title");
    private final UiTextField questDescription = new UiTextField().hint("Quest description");
    private final UiTextField questReward = new UiTextField().hint("Reward trigger id");
    private final UiTextField npcName = new UiTextField().hint("Display name");
    private final UiTextField npcHealth = new UiTextField().hint("Health");
    private final UiTextField npcInteract = new UiTextField().hint("On interact trigger");
    private final UiTextField npcDeath = new UiTextField().hint("On death trigger");
    private final UiTextField npcDialogue = new UiTextField().hint("Fallback dialogue id");
    private final UiTextField npcMobId = new UiTextField().hint("BBS mob id");
    private String npcRawJson = "";
    private Object selectedForm = null;
    private final UiTextField globalTrigger = new UiTextField().hint("Trigger id");
    private final UiTextField fieldA = new UiTextField();
    private final UiTextField fieldB = new UiTextField();
    private final UiTextField fieldC = new UiTextField();
    private final UiTextField fieldD = new UiTextField();
    private final UiTextField primaryField = new UiTextField();

    private UiRect expandPrimaryRect = null;
    private UiRect expandFieldBRect = null;
    private UiRect expandQuestDescRect = null;

    public boolean dirty()
    {
        return this.dirty;
    }

    public void clearDirty()
    {
        this.dirty = false;
    }

    public void load(String category, String content)
    {
        this.category = category == null ? "" : category;
        this.dirty = false;
        this.scrollY = 0;
        this.contentHeight = 0;
        this.selectedActionIndex = -1;
        this.selectedLineIndex = -1;
        this.selectedObjectiveIndex = -1;
        this.triggerActions.clear();
        this.dialogueLines.clear();
        this.questObjectives.clear();

        switch (this.category)
        {
            case "trigger" -> loadTrigger(content);
            case "script" -> this.scriptArea.setValue(content == null ? "" : content);
            case "dialogue" -> loadDialogue(content);
            case "quest" -> loadQuest(content);
            case "npc" -> {
                this.selectedForm = null;
                loadNpc(content);
            }
            case "global" -> this.globalTrigger.setValue(content == null ? "" : content);
        }
    }

    public String buildContent()
    {
        commitEdits();
        return switch (this.category)
        {
            case "trigger" -> buildTrigger();
            case "script" -> this.scriptArea.value();
            case "dialogue" -> buildDialogue();
            case "quest" -> buildQuest();
            case "npc" -> buildNpc();
            case "global" -> this.globalTrigger.value();
            default -> "";
        };
    }

    public void render(
        GuiGraphics graphics,
        Font font,
        UiRect rect,
        DashboardSnapshot data,
        int mouseX,
        int mouseY
    )
    {
        int x = rect.x() + UiTheme.PAD;
        int baseY = rect.y() + UiTheme.PAD;
        int w = rect.w() - UiTheme.PAD * 2;
        int viewH = rect.h();

        this.contentHeight = measureContentHeight(w);

        int maxScroll = Math.max(0, this.contentHeight - viewH);
        this.scrollY = Math.min(this.scrollY, maxScroll);

        int y = baseY - this.scrollY;

        graphics.enableScissor(rect.x(), rect.y(), rect.x() + rect.w(), rect.y() + rect.h());

        switch (this.category)
        {
            case "trigger" -> renderTriggerEditor(graphics, font, x, y, w, mouseX, mouseY);
            case "script" -> {
                label(graphics, font, x, y, "Script source");
                this.scriptArea.render(graphics, font, new UiRect(x, y + 14, w, Math.max(120, viewH - 20)));
            }
            case "dialogue" -> renderDialogueEditor(graphics, font, x, y, w, mouseX, mouseY);
            case "quest" -> renderQuestEditor(graphics, font, x, y, w, mouseX, mouseY);
            case "npc" -> renderNpcEditor(graphics, font, x, y, w, mouseX, mouseY);
            case "global" -> {
                label(graphics, font, x, y, "Assigned trigger");
                this.globalTrigger.render(graphics, font, new UiRect(x, y + 14, w, 20));
                label(graphics, font, x, y + 44, "Available triggers");
                int lineY = y + 58;

                for (String trigger : data.triggers())
                {
                    graphics.drawString(font, "- " + trigger, x, lineY, UiTheme.TEXT_DIM);
                    lineY += 12;
                }
            }
            default -> graphics.drawString(font, "Select an item to edit.", x, y, UiTheme.TEXT_DIM);
        }

        graphics.disableScissor();
        UiDraw.scrollbar(graphics, rect.x() + rect.w() - 3, rect.y(), viewH, this.contentHeight, viewH, this.scrollY);
    }

    private int measureContentHeight(int w)
    {
        return switch (this.category)
        {
            case "trigger" -> {
                int listH = Math.min(100, Math.max(56, this.triggerActions.size() * UiTheme.ROW_H + 12));
                int editH = this.selectedActionIndex >= 0 ? 170 : 0;
                yield 24 + listH + editH + 12;
            }
            case "script" -> 160;
            case "dialogue" -> {
                int listH = Math.min(100, Math.max(56, this.dialogueLines.size() * UiTheme.ROW_H + 12));
                int editH = this.selectedLineIndex >= 0 ? 90 : 0;
                yield 24 + listH + editH + 12;
            }
            case "quest" -> 130 + this.questObjectives.size() * UiTheme.ROW_H + (this.selectedObjectiveIndex >= 0 ? 90 : 0);
            case "npc" -> 270;
            case "global" -> 80;
            default -> 40;
        };
    }

    public boolean mouseClicked(UiRect rect, double mouseX, double mouseY, int button, DashboardSnapshot data)
    {
        if (button != 0)
        {
            return false;
        }

        int x = rect.x() + UiTheme.PAD;
        int y = rect.y() + UiTheme.PAD - this.scrollY;
        int w = rect.w() - UiTheme.PAD * 2;

        if (this.expandPrimaryRect != null && this.expandPrimaryRect.contains(mouseX, mouseY)) {
            if (Minecraft.getInstance().screen instanceof dev.scriptbound.client.screen.DashboardScreen ds) ds.openExpandedEditor(this.primaryField);
            return true;
        }
        if (this.expandFieldBRect != null && this.expandFieldBRect.contains(mouseX, mouseY)) {
            if (Minecraft.getInstance().screen instanceof dev.scriptbound.client.screen.DashboardScreen ds) ds.openExpandedEditor(this.fieldB);
            return true;
        }
        if (this.expandQuestDescRect != null && this.expandQuestDescRect.contains(mouseX, mouseY)) {
            if (Minecraft.getInstance().screen instanceof dev.scriptbound.client.screen.DashboardScreen ds) ds.openExpandedEditor(this.questDescription);
            return true;
        }

        return switch (this.category)
        {
            case "trigger" -> clickTriggerEditor(x, y, w, mouseX, mouseY);
            case "script" -> this.scriptArea.mouseClicked(new UiRect(x, y + 14, w, Math.max(120, rect.h() - 20)), mouseX, mouseY, button);
            case "dialogue" -> clickDialogueEditor(x, y, w, mouseX, mouseY);
            case "quest" -> clickQuestEditor(x, y, w, mouseX, mouseY);
            case "npc" -> clickNpcEditor(x, y, w, mouseX, mouseY);
            case "global" -> this.globalTrigger.mouseClicked(new UiRect(x, y + 14, w, 20), mouseX, mouseY, button);
            default -> false;
        };
    }

    public boolean mouseScrolled(UiRect rect, double mouseX, double mouseY, double delta)
    {
        if (!rect.contains(mouseX, mouseY))
        {
            return false;
        }

        if ("script".equals(this.category))
        {
            int x = rect.x() + UiTheme.PAD;
            int y = rect.y() + UiTheme.PAD - this.scrollY;
            int w = rect.w() - UiTheme.PAD * 2;

            if (this.scriptArea.mouseScrolled(new UiRect(x, y + 14, w, rect.h() - 20), mouseX, mouseY, delta))
            {
                return true;
            }
        }

        int maxScroll = Math.max(0, this.contentHeight - rect.h());
        this.scrollY = Math.max(0, Math.min(maxScroll, this.scrollY - (int) Math.signum(delta) * 14));
        return true;
    }

    public boolean keyPressed(int key)
    {
        if (this.scriptArea.keyPressed(key))
        {
            this.dirty = true;
            return true;
        }

        if (this.globalTrigger.keyPressed(key))
        {
            this.dirty = true;
            return true;
        }

        for (UiTextField field : activeFields())
        {
            if (field.keyPressed(key))
            {
                this.dirty = true;
                return true;
            }
        }

        return false;
    }

    public boolean charTyped(char codePoint)
    {
        if (this.scriptArea.charTyped(codePoint))
        {
            this.dirty = true;
            return true;
        }

        if (this.globalTrigger.charTyped(codePoint))
        {
            this.dirty = true;
            return true;
        }

        for (UiTextField field : activeFields())
        {
            if (field.charTyped(codePoint))
            {
                this.dirty = true;
                return true;
            }
        }

        return false;
    }

    public void commitEdits()
    {
        switch (this.category)
        {
            case "trigger" -> applyTriggerFields();
            case "dialogue" -> applyDialogueFields();
            case "quest" -> applyObjectiveFields();
            default -> {
            }
        }
    }

    public void blurAll()
    {
        commitEdits();
        this.scriptArea.setFocused(false);
        this.globalTrigger.setFocused(false);

        for (UiTextField field : activeFields())
        {
            field.setFocused(false);
        }
    }

    private UiTextField[] activeFields()
    {
        return switch (this.category)
        {
            case "quest" -> new UiTextField[] { this.questTitle, this.questDescription, this.questReward, this.fieldA, this.fieldB, this.fieldC, this.fieldD };
            case "npc" -> this.selectedForm != null
                ? new UiTextField[] { this.npcName, this.npcHealth, this.npcInteract, this.npcDeath, this.npcDialogue }
                : new UiTextField[] { this.npcName, this.npcHealth, this.npcInteract, this.npcDeath, this.npcDialogue, this.npcMobId };
            case "trigger" -> new UiTextField[] { this.fieldA, this.fieldB, this.fieldC, this.fieldD, this.primaryField };
            case "dialogue" -> new UiTextField[] { this.fieldA, this.fieldB, this.fieldC };
            default -> new UiTextField[0];
        };
    }

    private void renderTriggerEditor(GuiGraphics graphics, Font font, int x, int y, int w, int mouseX, int mouseY)
    {
        label(graphics, font, x, y, "Actions");
        UiRect addRect = new UiRect(x + w - 72, y, 72, 18);
        UiDraw.button(graphics, font, addRect, "+ Add", "ADD", addRect.contains(mouseX, mouseY), false, true);

        int listY = y + 20;
        int listH = Math.min(100, Math.max(56, this.triggerActions.size() * UiTheme.ROW_H + 12));
        UiDraw.inset(graphics, x, listY, w, listH);

        if (this.triggerActions.isEmpty())
        {
            graphics.drawString(font, "No actions yet. Click + Add.", x + 8, listY + 8, UiTheme.TEXT_DIM);
        }

        for (int i = 0; i < this.triggerActions.size(); i++)
        {
            TriggerActionForm row = this.triggerActions.get(i);
            UiRect rowRect = new UiRect(x + 4, listY + 4 + i * UiTheme.ROW_H, w - 12, UiTheme.ROW_H - 2);
            boolean selected = i == this.selectedActionIndex;
            graphics.fill(rowRect.x(), rowRect.y(), rowRect.x() + rowRect.w(), rowRect.y() + rowRect.h(), selected ? UiTheme.ROW_SELECT : 0x22000000);
            String summary = row.condition.name().toLowerCase(Locale.ROOT) + " -> " + row.actionType.name().toLowerCase(Locale.ROOT);
            graphics.drawString(font, font.plainSubstrByWidth(summary, rowRect.w() - 40), rowRect.x() + 4, rowRect.y() + 4, UiTheme.TEXT);
            UiDraw.button(graphics, font, new UiRect(rowRect.x() + rowRect.w() - 28, rowRect.y() + 2, 24, rowRect.h() - 4), "X", null, rowRect.contains(mouseX, mouseY), false, true);
        }

        if (this.selectedActionIndex >= 0 && this.selectedActionIndex < this.triggerActions.size())
        {
            TriggerActionForm row = this.triggerActions.get(this.selectedActionIndex);
            int editY = listY + listH + 12;
            this.expandPrimaryRect = null;
            label(graphics, font, x, editY, "Condition");
            drawField(graphics, font, this.fieldA, new UiRect(x, editY + 14, w, 20), conditionLabel(row.condition));
            label(graphics, font, x, editY + 40, "Condition key / npc / quest");
            drawField(graphics, font, this.fieldB, new UiRect(x, editY + 54, w / 2 - 4, 20), row.conditionKey.isBlank() ? row.conditionNpc.isBlank() ? row.conditionQuest : row.conditionNpc : row.conditionKey);
            label(graphics, font, x + w / 2 + 4, editY + 40, "Value");
            drawField(graphics, font, this.fieldC, new UiRect(x + w / 2 + 4, editY + 54, w / 2 - 4, 20), row.conditionValue);
            label(graphics, font, x, editY + 80, "Action type");
            drawField(graphics, font, this.fieldD, new UiRect(x, editY + 94, w, 20), row.actionType.name().toLowerCase(Locale.ROOT));
            label(graphics, font, x, editY + 120, actionFieldLabel(row.actionType));
            if (row.actionType == ActionType.MESSAGE || row.actionType == ActionType.COMMAND) {
                this.expandPrimaryRect = new UiRect(x + w - 50, editY + 116, 50, 12);
                boolean h = this.expandPrimaryRect.contains(mouseX, mouseY);
                UiDraw.button(graphics, font, this.expandPrimaryRect, "Expand", null, h, false, true);
            }
            drawField(graphics, font, this.primaryField, new UiRect(x, editY + 134, w, 20), primaryActionValue(row));
        }
    }

    private void renderDialogueEditor(GuiGraphics graphics, Font font, int x, int y, int w, int mouseX, int mouseY)
    {
        label(graphics, font, x, y, "Lines");
        UiRect addRect = new UiRect(x + w - 72, y, 72, 18);
        UiDraw.button(graphics, font, addRect, "+ Add", "ADD", addRect.contains(mouseX, mouseY), false, true);

        int listY = y + 20;
        int listH = Math.min(100, Math.max(56, this.dialogueLines.size() * UiTheme.ROW_H + 12));
        UiDraw.inset(graphics, x, listY, w, listH);

        for (int i = 0; i < this.dialogueLines.size(); i++)
        {
            DialogueLineForm line = this.dialogueLines.get(i);
            UiRect rowRect = new UiRect(x + 4, listY + 4 + i * UiTheme.ROW_H, w - 12, UiTheme.ROW_H - 2);
            boolean selected = i == this.selectedLineIndex;
            graphics.fill(rowRect.x(), rowRect.y(), rowRect.x() + rowRect.w(), rowRect.y() + rowRect.h(), selected ? UiTheme.ROW_SELECT : 0x22000000);
            graphics.drawString(font, font.plainSubstrByWidth(line.speaker + ": " + line.text, rowRect.w() - 40), rowRect.x() + 4, rowRect.y() + 4, UiTheme.TEXT);
            UiDraw.button(graphics, font, new UiRect(rowRect.x() + rowRect.w() - 28, rowRect.y() + 2, 24, rowRect.h() - 4), "X", null, false, false, true);
        }

        if (this.selectedLineIndex >= 0 && this.selectedLineIndex < this.dialogueLines.size())
        {
            DialogueLineForm line = this.dialogueLines.get(this.selectedLineIndex);
            int editY = listY + listH + 12;
            this.expandFieldBRect = null;
            label(graphics, font, x, editY, "Speaker");
            drawField(graphics, font, this.fieldA, new UiRect(x, editY + 14, w, 20), line.speaker);

            this.expandFieldBRect = new UiRect(x + w - 50, editY + 36, 50, 12);
            boolean h = this.expandFieldBRect.contains(mouseX, mouseY);
            label(graphics, font, x, editY + 40, "Text");
            UiDraw.button(graphics, font, this.expandFieldBRect, "Expand", null, h, false, true);
            drawField(graphics, font, this.fieldB, new UiRect(x, editY + 54, w, 20), line.text);
            label(graphics, font, x, editY + 80, "Choices: Yes=trigger_id; No=other_trigger");
            drawField(graphics, font, this.fieldC, new UiRect(x, editY + 94, w, 20), choicesToDsl(line.choicesJson));
        }
    }

    private void renderQuestEditor(GuiGraphics graphics, Font font, int x, int y, int w, int mouseX, int mouseY)
    {
        label(graphics, font, x, y, "Title");
        drawField(graphics, font, this.questTitle, new UiRect(x, y + 14, w, 20), this.questTitle.value());

        this.expandQuestDescRect = new UiRect(x + w - 50, y + 36, 50, 12);
        boolean h = this.expandQuestDescRect.contains(mouseX, mouseY);
        label(graphics, font, x, y + 40, "Description");
        UiDraw.button(graphics, font, this.expandQuestDescRect, "Expand", null, h, false, true);
        drawField(graphics, font, this.questDescription, new UiRect(x, y + 54, w, 20), this.questDescription.value());
        label(graphics, font, x, y + 80, "Reward trigger");
        drawField(graphics, font, this.questReward, new UiRect(x, y + 94, w, 20), this.questReward.value());

        int objY = y + 124;
        label(graphics, font, x, objY, "Objectives");
        UiRect addObjRect = new UiRect(x + w - 72, objY, 72, 18);
        UiDraw.button(graphics, font, addObjRect, "+ Add", "ADD", addObjRect.contains(mouseX, mouseY), false, true);

        for (int i = 0; i < this.questObjectives.size(); i++)
        {
            QuestObjectiveForm objective = this.questObjectives.get(i);
            UiRect rowRect = new UiRect(x, objY + 18 + i * UiTheme.ROW_H, w, UiTheme.ROW_H - 2);
            boolean selected = i == this.selectedObjectiveIndex;
            graphics.fill(rowRect.x(), rowRect.y(), rowRect.x() + rowRect.w(), rowRect.y() + rowRect.h(), selected ? UiTheme.ROW_SELECT : 0x22000000);
            graphics.drawString(font, objective.type + "  " + objectiveSummary(objective), rowRect.x() + 4, rowRect.y() + 4, UiTheme.TEXT);
        }

        if (this.selectedObjectiveIndex >= 0 && this.selectedObjectiveIndex < this.questObjectives.size())
        {
            QuestObjectiveForm objective = this.questObjectives.get(this.selectedObjectiveIndex);
            int editY = objY + 18 + this.questObjectives.size() * UiTheme.ROW_H + 8;
            label(graphics, font, x, editY, "Type (talk/kill/state)");
            drawField(graphics, font, this.fieldA, new UiRect(x, editY + 14, w / 2 - 4, 20), objective.type);
            label(graphics, font, x + w / 2 + 4, editY, "NPC / entity / key");
            drawField(graphics, font, this.fieldB, new UiRect(x + w / 2 + 4, editY + 14, w / 2 - 4, 20), objectiveParam(objective));
            label(graphics, font, x, editY + 40, "Count / value");
            drawField(graphics, font, this.fieldC, new UiRect(x, editY + 54, w, 20), objectiveCountOrValue(objective));
        }
    }

    private void renderNpcEditor(GuiGraphics graphics, Font font, int x, int y, int w, int mouseX, int mouseY)
    {
        label(graphics, font, x, y, "Display name");
        drawField(graphics, font, this.npcName, new UiRect(x, y + 14, w, 20), this.npcName.value());
        label(graphics, font, x, y + 40, "Health");
        drawField(graphics, font, this.npcHealth, new UiRect(x, y + 54, 120, 20), this.npcHealth.value());
        label(graphics, font, x, y + 80, "On interact trigger");
        drawField(graphics, font, this.npcInteract, new UiRect(x, y + 94, w, 20), this.npcInteract.value());
        label(graphics, font, x, y + 120, "On death trigger");
        drawField(graphics, font, this.npcDeath, new UiRect(x, y + 134, w, 20), this.npcDeath.value());
        label(graphics, font, x, y + 160, "Fallback dialogue");
        drawField(graphics, font, this.npcDialogue, new UiRect(x, y + 174, w, 20), this.npcDialogue.value());

        int formY = y + 204;

        if (this.selectedForm != null)
        {
            label(graphics, font, x, formY, "Selected BBS Morph:");
            try {
                String name = (String) this.selectedForm.getClass().getMethod("getId").invoke(this.selectedForm);
                graphics.drawString(font, name, x, formY + 14, UiTheme.TEXT_DIM);
            } catch (Exception ignored) {}
            UiRect clearRect = new UiRect(x + w - 72, formY + 12, 72, 18);
            UiDraw.button(graphics, font, clearRect, "Clear Morph", null, clearRect.contains(mouseX, mouseY), false, true);
        }
        else
        {
            label(graphics, font, x, formY, "Manual BBS mob id (Fallback)");
            drawField(graphics, font, this.npcMobId, new UiRect(x, formY + 14, w, 20), this.npcMobId.value());
        }

        UiRect chooseRect = new UiRect(x, formY + (this.selectedForm != null ? 36 : 40), 120, 18);
        UiDraw.button(graphics, font, chooseRect, BbsFormBridge.available() ? "Choose BBS Morph" : "BBS Not Installed", null, chooseRect.contains(mouseX, mouseY), !BbsFormBridge.available(), true);
    }

    private boolean clickTriggerEditor(int x, int y, int w, double mouseX, double mouseY)
    {
        UiRect addRect = new UiRect(x + w - 72, y, 72, 18);

        if (addRect.contains(mouseX, mouseY))
        {
            this.triggerActions.add(new TriggerActionForm());
            this.selectedActionIndex = this.triggerActions.size() - 1;
            syncTriggerFields();
            this.dirty = true;
            return true;
        }

        int listY = y + 20;
        int listH = Math.min(100, Math.max(56, this.triggerActions.size() * UiTheme.ROW_H + 12));

        for (int i = 0; i < this.triggerActions.size(); i++)
        {
            UiRect rowRect = new UiRect(x + 4, listY + 4 + i * UiTheme.ROW_H, w - 12, UiTheme.ROW_H - 2);

            if (new UiRect(rowRect.x() + rowRect.w() - 28, rowRect.y() + 2, 24, rowRect.h() - 4).contains(mouseX, mouseY))
            {
                this.triggerActions.remove(i);
                this.selectedActionIndex = Math.min(this.selectedActionIndex, this.triggerActions.size() - 1);
                this.dirty = true;
                return true;
            }

            if (rowRect.contains(mouseX, mouseY))
            {
                applyTriggerFields();
                this.selectedActionIndex = i;
                syncTriggerFields();
                return true;
            }
        }

        return clickFields(x, y, w, mouseX, mouseY, this::applyTriggerFields);
    }

    private boolean clickDialogueEditor(int x, int y, int w, double mouseX, double mouseY)
    {
        if (new UiRect(x + w - 72, y, 72, 18).contains(mouseX, mouseY))
        {
            this.dialogueLines.add(new DialogueLineForm());
            this.selectedLineIndex = this.dialogueLines.size() - 1;
            syncDialogueFields();
            this.dirty = true;
            return true;
        }

        int listY = y + 20;
        int listH = Math.min(100, Math.max(56, this.dialogueLines.size() * UiTheme.ROW_H + 12));

        for (int i = 0; i < this.dialogueLines.size(); i++)
        {
            UiRect rowRect = new UiRect(x + 4, listY + 4 + i * UiTheme.ROW_H, w - 12, UiTheme.ROW_H - 2);

            if (rowRect.contains(mouseX, mouseY))
            {
                applyDialogueFields();
                this.selectedLineIndex = i;
                syncDialogueFields();
                return true;
            }
        }

        if (this.selectedLineIndex >= 0)
        {
            int editY = listY + listH + 12;
            UiRect[] rects = {
                new UiRect(x, editY + 14, w, 20),
                new UiRect(x, editY + 54, w, 20),
                new UiRect(x, editY + 94, w, 20)
            };
            UiTextField[] fields = { this.fieldA, this.fieldB, this.fieldC };

            if (clickFieldArray(rects, fields, mouseX, mouseY))
            {
                applyDialogueFields();
                return true;
            }
        }

        return false;
    }

    private boolean clickQuestEditor(int x, int y, int w, double mouseX, double mouseY)
    {
        int objY = y + 124;

        if (new UiRect(x + w - 72, objY, 72, 18).contains(mouseX, mouseY))
        {
            this.questObjectives.add(new QuestObjectiveForm());
            this.selectedObjectiveIndex = this.questObjectives.size() - 1;
            syncObjectiveFields();
            this.dirty = true;
            return true;
        }

        for (int i = 0; i < this.questObjectives.size(); i++)
        {
            UiRect rowRect = new UiRect(x, objY + 18 + i * UiTheme.ROW_H, w, UiTheme.ROW_H - 2);

            if (rowRect.contains(mouseX, mouseY))
            {
                applyObjectiveFields();
                this.selectedObjectiveIndex = i;
                syncObjectiveFields();
                return true;
            }
        }

        return clickQuestFields(x, y, w, mouseX, mouseY);
    }

    private boolean clickNpcEditor(int x, int y, int w, double mouseX, double mouseY)
    {
        int formY = y + 204;

        if (this.selectedForm != null)
        {
            if (new UiRect(x + w - 72, formY + 12, 72, 18).contains(mouseX, mouseY))
            {
                this.selectedForm = null;
                this.dirty = true;
                return true;
            }
        }

        UiRect chooseRect = new UiRect(x, formY + (this.selectedForm != null ? 36 : 40), 120, 18);
        if (chooseRect.contains(mouseX, mouseY))
        {
            if (BbsFormBridge.available()) {
                BbsFormBridge.openMorphSelector(Minecraft.getInstance().screen, this.selectedForm, (form) -> {
                    this.selectedForm = form;
                    this.dirty = true;
                });
            }
            return true;
        }

        UiRect[] rects = this.selectedForm != null
            ? new UiRect[] {
                new UiRect(x, y + 14, w, 20),
                new UiRect(x, y + 54, 120, 20),
                new UiRect(x, y + 94, w, 20),
                new UiRect(x, y + 134, w, 20),
                new UiRect(x, y + 174, w, 20)
            }
            : new UiRect[] {
                new UiRect(x, y + 14, w, 20),
                new UiRect(x, y + 54, 120, 20),
                new UiRect(x, y + 94, w, 20),
                new UiRect(x, y + 134, w, 20),
                new UiRect(x, y + 174, w, 20),
                new UiRect(x, formY + 14, w, 20)
            };
        return clickFieldArray(rects, activeFields(), mouseX, mouseY);
    }

    private boolean clickQuestFields(int x, int y, int w, double mouseX, double mouseY)
    {
        UiRect[] rects = {
            new UiRect(x, y + 14, w, 20),
            new UiRect(x, y + 54, w, 20),
            new UiRect(x, y + 94, w, 20)
        };
        UiTextField[] fields = { this.questTitle, this.questDescription, this.questReward };

        if (clickFieldArray(rects, fields, mouseX, mouseY))
        {
            return true;
        }

        int objY = y + 124;

        if (this.selectedObjectiveIndex >= 0)
        {
            UiRect[] objRects = {
                new UiRect(x, objY + 18 + this.questObjectives.size() * UiTheme.ROW_H + 22, w / 2 - 4, 20),
                new UiRect(x + w / 2 + 4, objY + 18 + this.questObjectives.size() * UiTheme.ROW_H + 22, w / 2 - 4, 20),
                new UiRect(x, objY + 18 + this.questObjectives.size() * UiTheme.ROW_H + 62, w, 20)
            };
            UiTextField[] objFields = { this.fieldA, this.fieldB, this.fieldC };

            if (clickFieldArray(objRects, objFields, mouseX, mouseY))
            {
                applyObjectiveFields();
                return true;
            }
        }

        return false;
    }

    private boolean clickFields(int x, int y, int w, double mouseX, double mouseY, Runnable apply)
    {
        if (this.selectedActionIndex < 0)
        {
            return false;
        }

        int listH = Math.min(100, Math.max(56, this.triggerActions.size() * UiTheme.ROW_H + 12));
        int editY = y + 20 + listH + 12;
        UiRect[] rects = {
            new UiRect(x, editY + 14, w, 20),
            new UiRect(x, editY + 54, w / 2 - 4, 20),
            new UiRect(x + w / 2 + 4, editY + 54, w / 2 - 4, 20),
            new UiRect(x, editY + 94, w, 20),
            new UiRect(x, editY + 134, w, 20)
        };
        UiTextField[] fields = { this.fieldA, this.fieldB, this.fieldC, this.fieldD, this.primaryField };

        if (clickFieldArray(rects, fields, mouseX, mouseY))
        {
            apply.run();
            return true;
        }

        return false;
    }

    private boolean clickFieldArray(UiRect[] rects, UiTextField[] fields, double mouseX, double mouseY)
    {
        boolean clicked = false;

        for (int i = 0; i < rects.length; i++)
        {
            if (fields[i].mouseClicked(rects[i], mouseX, mouseY, 0))
            {
                clicked = true;
            }
            else if (clicked)
            {
                fields[i].setFocused(false);
            }
        }

        if (clicked)
        {
            this.dirty = true;
        }

        return clicked;
    }

    private void loadTrigger(String content)
    {
        try
        {
            JsonObject root = JsonParser.parseString(content == null || content.isBlank() ? "{}" : content).getAsJsonObject();

        if (!root.has("actions") || !root.get("actions").isJsonArray())
        {
            return;
        }

        for (var element : root.getAsJsonArray("actions"))
        {
            if (!element.isJsonObject())
            {
                continue;
            }

            JsonObject entry = element.getAsJsonObject();
            TriggerActionForm row = new TriggerActionForm();

            if (entry.has("if") && entry.get("if").isJsonObject())
            {
                JsonObject condition = entry.getAsJsonObject("if");
                String type = condition.has("type") ? condition.get("type").getAsString().toLowerCase(Locale.ROOT) : "state";
                row.condition = switch (type)
                {
                    case "state_absent" -> ConditionType.STATE_ABSENT;
                    case "state_present" -> ConditionType.STATE_PRESENT;
                    case "talk" -> ConditionType.TALKED;
                    case "quest" -> "ready".equalsIgnoreCase(condition.has("status") ? condition.get("status").getAsString() : "")
                        ? ConditionType.QUEST_READY
                        : ConditionType.QUEST_ACTIVE;
                    case "state" -> {
                        String op = condition.has("op") ? condition.get("op").getAsString().toLowerCase(Locale.ROOT) : "gte";
                        yield switch (op)
                        {
                            case "absent" -> ConditionType.STATE_ABSENT;
                            case "present" -> ConditionType.STATE_PRESENT;
                            default -> ConditionType.STATE_GTE;
                        };
                    }
                    default -> ConditionType.STATE_GTE;
                };
                row.conditionKey = condition.has("key") ? condition.get("key").getAsString() : "";
                row.conditionValue = condition.has("value") ? condition.get("value").getAsString() : "1";
                row.conditionNpc = condition.has("npc") ? condition.get("npc").getAsString() : "";
                row.conditionQuest = condition.has("quest") ? condition.get("quest").getAsString()
                    : (condition.has("id") ? condition.get("id").getAsString() : "");
            }

            if (entry.has("type"))
            {
                row.actionType = parseActionTypeFromJson(entry.get("type").getAsString());
            }

            row.text = entry.has("text") ? entry.get("text").getAsString() : "";
            row.mode = entry.has("mode") ? entry.get("mode").getAsString() : "chat";
            row.dialogue = entry.has("dialogue") ? entry.get("dialogue").getAsString() : "";
            row.script = entry.has("script") ? entry.get("script").getAsString() : "";
            row.trigger = entry.has("trigger") ? entry.get("trigger").getAsString() : "";
            row.command = entry.has("command") ? entry.get("command").getAsString() : "";
            row.scope = entry.has("scope") ? entry.get("scope").getAsString() : "player";
            row.stateKey = entry.has("key") ? entry.get("key").getAsString() : "";
            row.stateValue = entry.has("value") ? entry.get("value").getAsString() : "1";
            row.quest = entry.has("quest") ? entry.get("quest").getAsString()
                : (entry.has("id") ? entry.get("id").getAsString() : "");
            row.questOp = entry.has("op") ? entry.get("op").getAsString() : "give";
            row.film = entry.has("film") ? entry.get("film").getAsString() : "";
            this.triggerActions.add(row);
        }

        if (!this.triggerActions.isEmpty())
        {
            this.selectedActionIndex = 0;
            syncTriggerFields();
        }
        }
        catch (Exception ignored)
        {
        }
    }

    private String buildTrigger()
    {
        JsonObject root = new JsonObject();
        JsonArray actions = new JsonArray();

        for (TriggerActionForm row : this.triggerActions)
        {
            JsonObject entry = new JsonObject();

            if (row.condition != ConditionType.ALWAYS)
            {
                entry.add("if", buildCondition(row));
            }

            entry.addProperty("type", row.actionType == ActionType.PLAY_FILM ? "play_film" : row.actionType.name().toLowerCase(Locale.ROOT));

            switch (row.actionType)
            {
                case MESSAGE -> {
                    entry.addProperty("text", row.text);
                    entry.addProperty("mode", row.mode);
                }
                case DIALOGUE -> entry.addProperty("dialogue", row.dialogue);
                case SCRIPT -> entry.addProperty("script", row.script);
                case TRIGGER -> entry.addProperty("trigger", row.trigger);
                case COMMAND -> entry.addProperty("command", row.command);
                case STATE -> {
                    entry.addProperty("scope", row.scope);
                    entry.addProperty("key", row.stateKey);
                    entry.addProperty("value", row.stateValue);
                }
                case QUEST -> {
                    entry.addProperty("op", row.questOp);
                    entry.addProperty("quest", row.quest);
                }
                case PLAY_FILM -> entry.addProperty("film", row.film);
            }

            actions.add(entry);
        }

        root.add("actions", actions);
        return root.toString();
    }

    private JsonObject buildCondition(TriggerActionForm row)
    {
        JsonObject condition = new JsonObject();

        switch (row.condition)
        {
            case STATE_ABSENT -> {
                condition.addProperty("type", "state");
                condition.addProperty("scope", "player");
                condition.addProperty("key", row.conditionKey);
                condition.addProperty("op", "absent");
            }
            case STATE_PRESENT -> {
                condition.addProperty("type", "state");
                condition.addProperty("scope", "player");
                condition.addProperty("key", row.conditionKey);
                condition.addProperty("op", "present");
            }
            case STATE_GTE -> {
                condition.addProperty("type", "state");
                condition.addProperty("scope", "player");
                condition.addProperty("key", row.conditionKey);
                condition.addProperty("op", "gte");
                condition.addProperty("value", parseDouble(row.conditionValue, 1D));
            }
            case TALKED -> {
                condition.addProperty("type", "talk");
                condition.addProperty("npc", row.conditionNpc.isBlank() ? row.conditionKey : row.conditionNpc);
            }
            case QUEST_ACTIVE -> {
                condition.addProperty("type", "quest");
                condition.addProperty("quest", row.conditionQuest);
                condition.addProperty("status", "active");
            }
            case QUEST_READY -> {
                condition.addProperty("type", "quest");
                condition.addProperty("quest", row.conditionQuest);
                condition.addProperty("status", "ready");
            }
            default -> {
            }
        }

        return condition;
    }

    private void loadDialogue(String content)
    {
        JsonObject root = JsonParser.parseString(content == null || content.isBlank() ? "{}" : content).getAsJsonObject();

        if (!root.has("lines") || !root.get("lines").isJsonArray())
        {
            return;
        }

        for (var element : root.getAsJsonArray("lines"))
        {
            if (!element.isJsonObject())
            {
                continue;
            }

            JsonObject line = element.getAsJsonObject();
            DialogueLineForm form = new DialogueLineForm();
            form.speaker = line.has("speaker") ? line.get("speaker").getAsString() : "";
            form.text = line.has("text") ? line.get("text").getAsString() : "";
            form.choicesJson = line.has("choices") && line.get("choices").isJsonArray() ? line.get("choices").toString() : "";
            this.dialogueLines.add(form);
        }

        if (!this.dialogueLines.isEmpty())
        {
            this.selectedLineIndex = 0;
            syncDialogueFields();
        }
    }

    private String buildDialogue()
    {
        JsonObject root = new JsonObject();
        JsonArray lines = new JsonArray();

        for (DialogueLineForm line : this.dialogueLines)
        {
            JsonObject entry = new JsonObject();
            entry.addProperty("speaker", line.speaker);
            entry.addProperty("text", line.text);

            if (!line.choicesJson.isBlank())
            {
                try
                {
                    entry.add("choices", JsonParser.parseString(line.choicesJson).getAsJsonArray());
                }
                catch (RuntimeException ignored)
                {
                }
            }

            lines.add(entry);
        }

        root.add("lines", lines);
        return root.toString();
    }

    private void loadQuest(String content)
    {
        JsonObject root = JsonParser.parseString(content == null || content.isBlank() ? "{}" : content).getAsJsonObject();
        this.questTitle.setValue(root.has("title") ? root.get("title").getAsString() : "");
        this.questDescription.setValue(root.has("description") ? root.get("description").getAsString() : "");
        this.questReward.setValue(root.has("reward_trigger") ? root.get("reward_trigger").getAsString() : "");

        if (root.has("objectives") && root.get("objectives").isJsonArray())
        {
            for (var element : root.getAsJsonArray("objectives"))
            {
                if (!element.isJsonObject())
                {
                    continue;
                }

                JsonObject obj = element.getAsJsonObject();
                QuestObjectiveForm form = new QuestObjectiveForm();
                form.type = obj.has("type") ? obj.get("type").getAsString() : "talk";
                form.key = obj.has("key") ? obj.get("key").getAsString() : "";
                form.value = obj.has("value") ? obj.get("value").getAsString() : "1";
                form.entity = obj.has("entity") ? obj.get("entity").getAsString() : "";
                form.count = obj.has("count") ? obj.get("count").getAsString() : "1";
                form.npc = obj.has("npc") ? obj.get("npc").getAsString() : "";
                this.questObjectives.add(form);
            }
        }

        if (!this.questObjectives.isEmpty())
        {
            this.selectedObjectiveIndex = 0;
            syncObjectiveFields();
        }
    }

    private String buildQuest()
    {
        JsonObject root = new JsonObject();
        root.addProperty("title", this.questTitle.value());
        root.addProperty("description", this.questDescription.value());
        root.addProperty("reward_trigger", this.questReward.value());
        JsonArray objectives = new JsonArray();

        for (QuestObjectiveForm objective : this.questObjectives)
        {
            JsonObject entry = new JsonObject();
            entry.addProperty("type", objective.type);

            switch (objective.type.toLowerCase(Locale.ROOT))
            {
                case "kill" -> {
                    entry.addProperty("entity", objective.entity);
                    entry.addProperty("count", parseInt(objective.count, 1));
                }
                case "state" -> {
                    entry.addProperty("key", objective.key);
                    entry.addProperty("value", parseDouble(objective.value, 1D));
                }
                default -> entry.addProperty("npc", objective.npc);
            }

            objectives.add(entry);
        }

        root.add("objectives", objectives);
        return root.toString();
    }

    private void loadNpc(String content)
    {
        JsonObject root = JsonParser.parseString(content == null || content.isBlank() ? "{}" : content).getAsJsonObject();
        this.npcRawJson = root.toString();
        this.npcName.setValue(root.has("displayName") ? root.get("displayName").getAsString() : "");
        this.npcHealth.setValue(root.has("health") ? root.get("health").getAsString() : "20");
        this.npcInteract.setValue(root.has("onInteract") ? root.get("onInteract").getAsString() : "");
        this.npcDeath.setValue(root.has("onDeath") ? root.get("onDeath").getAsString() : "");
        this.npcDialogue.setValue(root.has("dialogue") ? root.get("dialogue").getAsString() : "");

        if (root.has("form") && root.get("form").isJsonObject())
        {
            JsonObject form = root.getAsJsonObject("form");
            this.npcMobId.setValue(form.has("mobId") ? form.get("mobId").getAsString() : "minecraft:villager");

            try
            {
                if (BbsFormBridge.available()) this.selectedForm = BbsFormBridge.parseForm(form.toString());
            }
            catch (Exception ignored)
            {
            }
        }
    }

    private String buildNpc()
    {

        JsonObject root;

        try
        {
            root = JsonParser.parseString(this.npcRawJson == null || this.npcRawJson.isBlank() ? "{}" : this.npcRawJson).getAsJsonObject();
        }
        catch (RuntimeException ignored)
        {
            root = new JsonObject();
        }

        root.addProperty("displayName", this.npcName.value());
        root.addProperty("health", parseDouble(this.npcHealth.value(), 20D));

        if (!root.has("invulnerable"))
        {
            root.addProperty("invulnerable", true);
        }

        root.addProperty("onInteract", this.npcInteract.value());
        root.addProperty("onDeath", this.npcDeath.value());

        if (!this.npcDialogue.value().isBlank())
        {
            root.addProperty("dialogue", this.npcDialogue.value());
        }
        else
        {
            root.remove("dialogue");
        }

        if (this.selectedForm != null)
        {
            try
            {
                JsonObject formJsonObj = JsonParser.parseString(BbsFormBridge.serializeForm(this.selectedForm)).getAsJsonObject();
                root.add("form", formJsonObj);
                dev.scriptbound.ScriptBoundMod.LOGGER.debug("Dashboard saved BBS morph for NPC.");
            }
            catch (Exception e)
            {
                dev.scriptbound.ScriptBoundMod.LOGGER.error("[DEBUG] Failed to serialize selected BBS morph", e);
            }
        }
        else
        {
            JsonObject form = root.has("form") && root.get("form").isJsonObject() ? root.getAsJsonObject("form") : new JsonObject();

            if (!form.has("id"))
            {
                form.addProperty("id", "bbs:mob");
            }

            form.addProperty("mobId", this.npcMobId.value().isBlank() ? "minecraft:villager" : this.npcMobId.value());
            root.add("form", form);
        }

        return root.toString();
    }

    private void syncTriggerFields()
    {
        if (this.selectedActionIndex < 0 || this.selectedActionIndex >= this.triggerActions.size())
        {
            return;
        }

        TriggerActionForm row = this.triggerActions.get(this.selectedActionIndex);
        this.fieldA.setValue(conditionLabel(row.condition));
        this.fieldB.setValue(row.conditionKey.isBlank() ? row.conditionNpc.isBlank() ? row.conditionQuest : row.conditionNpc : row.conditionKey);
        this.fieldC.setValue(row.conditionValue);
        this.fieldD.setValue(row.actionType.name().toLowerCase(Locale.ROOT));
        this.primaryField.setValue(primaryActionValue(row));
    }

    private void applyTriggerFields()
    {
        if (this.selectedActionIndex < 0 || this.selectedActionIndex >= this.triggerActions.size())
        {
            return;
        }

        TriggerActionForm row = this.triggerActions.get(this.selectedActionIndex);
        row.condition = parseCondition(this.fieldA.value());
        String param = this.fieldB.value();
        row.conditionKey = param;
        row.conditionNpc = param;
        row.conditionQuest = param;
        row.conditionValue = this.fieldC.value();
        row.actionType = parseActionType(this.fieldD.value());
        applyPrimaryActionValue(row, this.primaryField.value());
        this.dirty = true;
    }

    private void syncDialogueFields()
    {
        if (this.selectedLineIndex < 0 || this.selectedLineIndex >= this.dialogueLines.size())
        {
            return;
        }

        DialogueLineForm line = this.dialogueLines.get(this.selectedLineIndex);
        this.fieldA.setValue(line.speaker);
        this.fieldB.setValue(line.text);
        this.fieldC.setValue(choicesToDsl(line.choicesJson));
    }

    private void applyDialogueFields()
    {
        if (this.selectedLineIndex < 0 || this.selectedLineIndex >= this.dialogueLines.size())
        {
            return;
        }

        DialogueLineForm line = this.dialogueLines.get(this.selectedLineIndex);
        line.speaker = this.fieldA.value();
        line.text = this.fieldB.value();
        line.choicesJson = dslToChoicesJson(this.fieldC.value());
        this.dirty = true;
    }

    private static String choicesToDsl(String choicesJson)
    {
        if (choicesJson == null || choicesJson.isBlank())
        {
            return "";
        }

        try
        {
            StringBuilder builder = new StringBuilder();

            for (var element : JsonParser.parseString(choicesJson).getAsJsonArray())
            {
                if (!element.isJsonObject())
                {
                    continue;
                }

                JsonObject choice = element.getAsJsonObject();

                if (!builder.isEmpty())
                {
                    builder.append("; ");
                }

                builder.append(choice.has("text") ? choice.get("text").getAsString() : "");

                if (choice.has("trigger") && !choice.get("trigger").getAsString().isBlank())
                {
                    builder.append('=').append(choice.get("trigger").getAsString());
                }
            }

            return builder.toString();
        }
        catch (RuntimeException ignored)
        {
            return "";
        }
    }

    private static String dslToChoicesJson(String dsl)
    {
        if (dsl == null || dsl.isBlank())
        {
            return "";
        }

        JsonArray choices = new JsonArray();

        for (String part : dsl.split(";"))
        {
            String entry = part.trim();

            if (entry.isEmpty())
            {
                continue;
            }

            int eq = entry.lastIndexOf('=');
            JsonObject choice = new JsonObject();

            if (eq > 0)
            {
                choice.addProperty("text", entry.substring(0, eq).trim());
                choice.addProperty("trigger", entry.substring(eq + 1).trim());
            }
            else
            {
                choice.addProperty("text", entry);
            }

            choices.add(choice);
        }

        return choices.isEmpty() ? "" : choices.toString();
    }

    private void syncObjectiveFields()
    {
        if (this.selectedObjectiveIndex < 0 || this.selectedObjectiveIndex >= this.questObjectives.size())
        {
            return;
        }

        QuestObjectiveForm objective = this.questObjectives.get(this.selectedObjectiveIndex);
        this.fieldA.setValue(objective.type);
        this.fieldB.setValue(objectiveParam(objective));
        this.fieldC.setValue(objectiveCountOrValue(objective));
    }

    private void applyObjectiveFields()
    {
        if (this.selectedObjectiveIndex < 0 || this.selectedObjectiveIndex >= this.questObjectives.size())
        {
            return;
        }

        QuestObjectiveForm objective = this.questObjectives.get(this.selectedObjectiveIndex);
        objective.type = this.fieldA.value().isBlank() ? "talk" : this.fieldA.value();
        String param = this.fieldB.value();

        switch (objective.type.toLowerCase(Locale.ROOT))
        {
            case "kill" -> objective.entity = param;
            case "state" -> objective.key = param;
            default -> objective.npc = param;
        }

        String countOrValue = this.fieldC.value();

        if ("state".equalsIgnoreCase(objective.type))
        {
            objective.value = countOrValue;
        }
        else
        {
            objective.count = countOrValue;
        }

        this.dirty = true;
    }

    private static void label(GuiGraphics graphics, Font font, int x, int y, String text)
    {
        graphics.drawString(font, text, x, y, UiTheme.TEXT_ACCENT);
    }

    private static void drawField(GuiGraphics graphics, Font font, UiTextField field, UiRect rect, String value)
    {
        if (!field.focused() && !field.value().equals(value))
        {
            field.setValue(value);
        }

        field.render(graphics, font, rect);
    }

    private static String conditionLabel(ConditionType type)
    {
        return type.name().toLowerCase(Locale.ROOT);
    }

    private static ConditionType parseCondition(String value)
    {
        if (value == null || value.isBlank())
        {
            return ConditionType.ALWAYS;
        }

        try
        {
            return ConditionType.valueOf(value.trim().toUpperCase(Locale.ROOT));
        }
        catch (IllegalArgumentException ignored)
        {
            return ConditionType.ALWAYS;
        }
    }

    private static ActionType parseActionTypeFromJson(String value)
    {
        if (value == null || value.isBlank())
        {
            return ActionType.MESSAGE;
        }

        return switch (value.toLowerCase(Locale.ROOT))
        {
            case "play_film" -> ActionType.PLAY_FILM;
            default -> parseActionType(value);
        };
    }

    private static ActionType parseActionType(String value)
    {
        if (value == null || value.isBlank())
        {
            return ActionType.MESSAGE;
        }

        try
        {
            return ActionType.valueOf(value.trim().toUpperCase(Locale.ROOT));
        }
        catch (IllegalArgumentException ignored)
        {
            return ActionType.MESSAGE;
        }
    }

    private static String actionFieldLabel(ActionType type)
    {
        return switch (type)
        {
            case MESSAGE -> "Message text / mode (chat|title|actionbar)";
            case DIALOGUE -> "Dialogue id";
            case SCRIPT -> "Script id";
            case TRIGGER -> "Trigger id";
            case COMMAND -> "Command";
            case STATE -> "State key / value / scope";
            case QUEST -> "Quest id / op (give|complete)";
            case PLAY_FILM -> "Film id";
        };
    }

    private static String primaryActionValue(TriggerActionForm row)
    {
        return switch (row.actionType)
        {
            case MESSAGE -> row.text + " | " + row.mode;
            case DIALOGUE -> row.dialogue;
            case SCRIPT -> row.script;
            case TRIGGER -> row.trigger;
            case COMMAND -> row.command;
            case STATE -> row.stateKey + " | " + row.stateValue + " | " + row.scope;
            case QUEST -> row.quest + " | " + row.questOp;
            case PLAY_FILM -> row.film;
        };
    }

    private static void applyPrimaryActionValue(TriggerActionForm row, String value)
    {
        String[] parts = value.split("\\|");

        switch (row.actionType)
        {
            case MESSAGE -> {
                row.text = parts[0].trim();
                row.mode = parts.length > 1 ? parts[1].trim() : "chat";
            }
            case DIALOGUE -> row.dialogue = value.trim();
            case SCRIPT -> row.script = value.trim();
            case TRIGGER -> row.trigger = value.trim();
            case COMMAND -> row.command = value.trim();
            case STATE -> {
                row.stateKey = parts[0].trim();
                row.stateValue = parts.length > 1 ? parts[1].trim() : "1";
                row.scope = parts.length > 2 ? parts[2].trim() : "player";
            }
            case QUEST -> {
                row.quest = parts[0].trim();
                row.questOp = parts.length > 1 ? parts[1].trim() : "give";
            }
            case PLAY_FILM -> row.film = value.trim();
        }
    }

    private static String objectiveSummary(QuestObjectiveForm objective)
    {
        return switch (objective.type.toLowerCase(Locale.ROOT))
        {
            case "kill" -> objective.entity + " x" + objective.count;
            case "state" -> objective.key + " >= " + objective.value;
            default -> objective.npc;
        };
    }

    private static String objectiveParam(QuestObjectiveForm objective)
    {
        return switch (objective.type.toLowerCase(Locale.ROOT))
        {
            case "kill" -> objective.entity;
            case "state" -> objective.key;
            default -> objective.npc;
        };
    }

    private static String objectiveCountOrValue(QuestObjectiveForm objective)
    {
        return "state".equalsIgnoreCase(objective.type) ? objective.value : objective.count;
    }

    private static double parseDouble(String value, double fallback)
    {
        try
        {
            return Double.parseDouble(value);
        }
        catch (NumberFormatException ignored)
        {
            return fallback;
        }
    }

    private static int parseInt(String value, int fallback)
    {
        try
        {
            return Integer.parseInt(value);
        }
        catch (NumberFormatException ignored)
        {
            return fallback;
        }
    }

    public static String categoryForTab(DashboardScreenTab tab)
    {
        return switch (tab)
        {
            case TRIGGERS -> "trigger";
            case SCRIPTS -> "script";
            case DIALOGUES -> "dialogue";
            case QUESTS -> "quest";
            case NPCS -> "npc";
            case GLOBAL -> "global";
            default -> "";
        };
    }

    public enum DashboardScreenTab
    {
        TRIGGERS,
        SCRIPTS,
        DIALOGUES,
        QUESTS,
        NPCS,
        GLOBAL
    }
}
