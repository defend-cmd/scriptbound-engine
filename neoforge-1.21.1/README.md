# ScriptBound Engine

ScriptBound Engine is a NeoForge mod for building scripted Minecraft maps from an in-game dashboard.

This port targets Minecraft 1.21.1 and Java 21.

## Features

- Node-based flow and dialogue editors
- Triggers, conditions, global state, and player state
- NPC blueprints, interaction and death triggers, factions, and patrols
- Quests and quest journal
- JavaScript execution through Rhino
- Trigger and region blocks
- Optional Blockbuster Studio integration

## Data

Project data is stored in the world directory under `scriptbound/`. The mod does not use telemetry or cloud storage.

Back up the world before updating the mod or editing project files manually.

## Build

```powershell
.\gradlew.bat build
```

Build artifacts are written to `build/libs/`.
