# Чеклист тестирования — v1.2.1

Перед тестом: собери мод (`gradlew build`), зайди в мир, открой дашборд
(`/sbe dashboard`).

## 1. Привязка NPC (главный фикс)

1. Заспавни NPC: `/sbe npc spawn guard`.
2. В дашборде → Flows → создай флоу: нода **NPC R-Click** → **Message**.
3. В ноде NPC R-Click впиши `npcId = guard` (или через Detect, см. ниже). Сохрани флоу.
4. Кликни ПКМ по уже заспавненному NPC.
   - **Ожидается:** сообщение из ноды Message приходит сразу, без переспавна NPC.
5. Поменяй текст Message, сохрани, кликни ещё раз — текст обновился.

## 2. Detect

1. Наведи прицел на NPC, открой дашборд → Flows → выбери ноду NPC R-Click →
   кнопка **Detect aimed NPC**.
   - **Ожидается:** npcId, Display name, mobId заполнились сами.
2. Отойди (прицел в пустоту, NPC в радиусе 12 блоков) — Detect всё равно
   берёт ближайшего NPC.
3. Поставь Trigger Block, наведи на него прицел, в ноде **Block L/R-Click**
   нажми **Detect aimed block**.
   - **Ожидается:** X/Y/Z заполнились координатами блока.
4. То же с Region Block и нодой **Region Enter/Exit**.

## 3. События (фаза 10)

1. Флоу: **Player Death** → Message «ты умер». Сохрани. Умри.
   - **Ожидается:** сообщение при смерти.
2. Флоу: **Player Respawn** → Message. Возродись — сообщение пришло.

## 4. Condition-нода (фаза 10)

1. **NPC R-Click** → **Condition** (`source=state, key=test, op=>=, value=1`)
   → Message.
   - Без стейта: клик по NPC — сообщения нет.
   - `/sbe state set @p test 1` → клик — сообщение есть.
2. `source=quest, key=<квест>, op=active` — сообщение только при взятом квесте.
3. `source=talked, key=<npcId>` — сообщение только после первого разговора.
4. `source=faction, key=<фракция>, op=friendly` — см. п.5.

## 5. Фракции (фазы 11/14)

1. Флоу: NPC R-Click → **Faction Rep** (`faction=guards, op=add, value=10`).
   Кликай по NPC — репутация растёт (`faction.guards` в стейтах).
2. Проверь файл `scriptbound/factions/guards.json` — создался с порогами.
3. В JSON NPC добавь `"faction": "guards"`. Поставь себе репутацию -50:
   флоу с Faction Rep `op=set, value=-50`.
   - **Ожидается:** NPC пишет «refuses to talk to you» и не запускает триггер.
4. Подними репутацию выше порога `hostile` — NPC снова разговаривает.
5. `"onHostileInteract": "<trigger_id>"` в JSON NPC — вместо отказа
   запускается указанный триггер.

## 6. Диалоги — нодовый редактор (v1.2.1)

1. Dashboard → **Dialogues** → открой `d1` (или создай новый через +).
   - **Ожидается:** большой канвас слева, список справа (как во Flows).
2. ПКМ на канвасе → **Dialogue Start** (если нет стартовой ноды).
   - Заполни Speaker, Opening text. **Detect aimed NPC** подставляет NPC.
3. ПКМ → **Add 2 Choices** — появляются **две** ноды Choice, провода к Start.
   - Одиночную Choice из меню добавить нельзя.
4. В каждой Choice впиши текст («Да» / «Нет»). Проведи от Choice к **Run Trigger**
   (или Message). В Action укажи trigger id.
5. Сохрани (иконка дискеты). Открой диалог в игре через NPC/триггер.
   - **Ожидается:** текст из Start, две кнопки, выбор запускает нужный триггер.
6. ПКМ → **+ 1 More Choice** (когда уже 2 choice) — третий вариант.
7. Старый диалог `guard_first` открывается как граф (конвертация из lines).

## 6b. GUI scale

1. Поставь GUI Scale **3** или **4** в настройках Minecraft.
2. Открой Dashboard → Dialogues и Flows.
   - **Ожидается:** интерфейс помещается, ноды и панели меньше, канвас шире.

## 7. Script API (фаза 12)

Создай скрипт и дёрни его триггером:

```js
function main(c) {
    c.giveItem("minecraft:diamond", 3);
    c.playSound("minecraft:entity.player.levelup", 1.0);
    c.actionbar("Привет!");
    c.addFaction("guards", 5);
    c.send("Репутация: " + c.getFaction("guards") + " (" + c.getFactionAttitude("guards") + ")");
    c.giveQuest("first_quest");
    c.delayTrigger(60, "some_trigger"); // через 3 секунды
}
```

- **Ожидается:** алмазы, звук, актионбар, репутация +5, квест выдан,
  через 3 сек сработал `some_trigger`.

## 8. Патруль NPC (фаза 13)

1. `/sbe npc spawn guard`.
2. Встань в точку A: `/sbe npc patrol add guard`. Точка B: ещё раз.
   - **Ожидается:** NPC начинает ходить между точками (скорость 0.25 по умолчанию,
     настраивается `"speed"` в JSON).
3. Перезайди в мир — патруль сохранился.
4. `/sbe npc patrol clear guard` — NPC остановился.

## 9. Радиус региона из ноды (фаза 13)

1. Поставь Region Block. В ноде Region Enter впиши координаты (или Detect)
   и `Radius = 10`. Сохрани флоу.
2. Зайди в радиус 10 блоков — событие сработало.

## 10. Регрессия (быстро прогнать)

- Delay-нода всё ещё работает (пауза в тиках).
- Teleport / Sound / Give Item ноды работают.
- Старые флоу открываются и сохраняются без потерь.
- Демо-контент в новом мире создаётся без ошибок.
- Диалоги/квесты/NPC сохраняются из дашборда как раньше.
