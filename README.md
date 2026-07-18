# ScriptBound Engine

ScriptBound Engine is a Minecraft map scripting mod with an in-game dashboard for flows, dialogues, NPCs, quests, states, triggers, and JavaScript.

## Versions

| Directory | Minecraft | Loader | Java |
| --- | --- | --- | --- |
| `forge-1.20.1` | 1.20.1 | Forge 47.4.10 | 17 |
| `forge-1.21.1` | 1.21.1 | Forge 52.1.0 | 21 |
| `neoforge-1.21.1` | 1.21.1 | NeoForge | 21 |

The Forge 1.20.1 directory contains the complete feature set. The 1.21.1 directories are ports and do not yet include the custom UI subsystem.

## Storage

Content is stored in the world directory under `scriptbound/`. There is no telemetry or cloud storage.

## Building

Run the Gradle wrapper from the version directory you want to build:

```powershell
cd forge-1.20.1
.\gradlew.bat build
```

Artifacts are written to that directory's `build/libs/` folder.

Blockbuster Studio support is optional at runtime. The Forge projects use its development JAR as a compile-only dependency for the form picker.
