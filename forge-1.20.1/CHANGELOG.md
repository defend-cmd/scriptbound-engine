# ScriptBound Engine — Changelog

## 1.5.3

- Fixed BBS morph rendering for NPCs on all supported loaders.

## 1.5.2

- Blockbuster Studio is no longer required to run ScriptBound. Form and morph integration remains available when BBS is installed.

## 1.2.1 — Диалоги на нодах + GUI scale

### Диалоги — нодовый редактор (как Flows)
- Вкладка **Dialogues** переведена на граф-редактор: большой канвас слева,
  список + свойства ноды справа.
- **Dialogue Start** — NPC (Detect), имя спикера, открывающий текст.
- **Add 2 Choices** — сразу две ноды выбора (одна нода выбора недоступна).
  Если есть Start — автоподключение проводами.
- **+ 1 More Choice** — третий вариант (только когда уже ≥2 choice-нод).
- **Action-ноды** (Message, Run Trigger, Command, State, Script) — цепочка
  `Start → Choice → Action`; при сохранении компилируется в `lines[]` + триггеры.
- Старые диалоги `{lines, choices}` автоматически конвертируются в граф при открытии.

### GUI scale 3–4
- `UiScale` уменьшает отступы, панели и ноды на масштабе 3–4.
- Граф-вкладки (Flows, Dialogues) занимают максимум экрана.

### Исправления
- Поля текста в форме диалогов не принимали ввод (не были в `activeFields`).

## 1.2.0 — Фазы 10–14

### Исправления
- **NPC-нода во флоу теперь реально работает.** Раньше NPC читал триггер,
  записанный в момент спавна, поэтому привязка флоу к уже заспавненному NPC
  игнорировалась. Теперь при каждом клике ПКМ по NPC блюпринт перечитывается
  с диска — правки из дашборда применяются мгновенно, без переспавна.
- Сохранение NPC из дашборда больше не стирает поля, которые форма не
  редактирует (faction, patrol, speed, onHostileInteract и т.д.).

### Detect (автозаполнение из мира)
- Кнопка **Detect aimed NPC** в ноде "NPC R-Click": наведи прицел на NPC,
  открой дашборд, нажми — npcId, имя и mobId заполнятся сами.
  Если прицел мимо — берётся ближайший NPC в радиусе 12 блоков.
- Кнопка **Detect aimed block** в нодах Trigger Block L/R-Click и
  Region Enter/Exit: координаты X/Y/Z берутся из блока под прицелом.

### Фаза 10 — события
- Новые событийные ноды: **Player Death**, **Player Respawn**.
- Condition-нода получила поле `source`: `state` (по умолчанию),
  `quest` (key = id квеста, op = active/ready/absent),
  `talked` (key = id NPC), `faction` (key = id фракции, op = friendly/neutral/hostile).

### Фаза 11 — фракции и репутация
- Файлы фракций: `scriptbound/factions/<id>.json`
  (`title`, `defaultScore`, `friendly`, `hostile`). Создаются автоматически
  при первом обращении.
- Репутация игрока хранится в стейтах как `faction.<id>`.
- Новый экшен `faction` (add/set репутации) + чекер `faction` (attitude).

### Фаза 12 — Script API и choices
- Новые методы `c.*` в скриптах: `giveItem`, `playSound`, `actionbar`,
  `removeState`, `hasState`, `getFaction`, `addFaction`, `setFaction`,
  `getFactionAttitude`, `giveQuest`, `completeQuest`, `spawnNpc`,
  `dialogue`, `delayTrigger(ticks, triggerId)`.
- В редакторе диалогов появилось поле **Choices**: формат
  `Да=trigger_id; Нет=другой_trigger`. Раньше choices можно было задать
  только руками в JSON.

### Фаза 13 — патруль NPC и регионы
- NPC умеют патрулировать: `"patrol": [[x,y,z], ...]` и `"speed": 0.25`
  в JSON NPC, либо команды **/sbe npc patrol add <id>** (добавить точку
  на месте игрока) и **/sbe npc patrol clear <id>**. Применяется к живым
  NPC сразу.
- У нод Region Enter/Exit появилось поле **Radius** — при сохранении флоу
  радиус региона выставляется автоматически.

### Фаза 14 — события + фракции
- Нода **Faction Rep** во флоу-редакторе (add/set репутации).
- NPC с полем `"faction"` отказывается говорить с враждебным игроком;
  опционально `"onHostileInteract"` — триггер вместо отказа.
- Глобальные события `player_death` / `player_respawn` доступны и в
  настройках, и как событийные ноды.

## 1.1.0 — Mappet-логика во флоу

- **Condition-нода**: ветвление цепочки по стейту.
- **Delay-нода**: пауза в тиках (серверный планировщик, суб-триггеры).
- Новые экшен-ноды: **Teleport**, **Sound**, **Give Item**, **Play Film**.
- Автопривязка нод Trigger Block / Region по координатам X/Y/Z:
  компилятор сам прописывает триггеры в блок-энтити.
- Исправлено: редактор диалогов терял `choices[]` при сохранении.
- Исправлено: компиляция флоу перезаписывала весь JSON NPC (теперь merge).

## 1.0.9 — Редизайн дашборда (BBS CML стиль)

- Полный редизайн флоу-редактора: вертикальные ноды с портами сверху/снизу,
  безье-провода, панорамирование, зум к курсору, ПКМ-меню создания нод,
  панель свойств справа.
- Единый стиль всех вкладок: плавающие панели, верхний тулбар,
  полупрозрачный фон, BBS-иконки.
- Список NPC в дашборде объединяет файлы и живых NPC из мира (npcMeta).
- Клик по обнаруженному NPC в панели свойств подставляет id/имя/моба.

## 1.0.x — Базовый движок

- Триггеры (JSON-цепочки экшенов): message, command, state, dialogue,
  quest, npc и др. + условия (state/talk/quest).
- Стейты: player/global скоупы, команды /sbe state.
- Диалоги с choices, квесты с целями (talk/kill/state), NPC-блюпринты
  с BBS-формами.
- Trigger Block и Region Block (enter/exit, радиус).
- Глобальные события player_join / player_chat.
- Скрипты на JS (Nashorn) с контекстом `c`.
- Дашборд (/sbe dashboard): вкладки Triggers, Scripts, Dialogues, Quests,
  NPCs, Globals, Flows; синхронизация с сервером, демо-контент.
