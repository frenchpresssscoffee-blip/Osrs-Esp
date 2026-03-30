# ESP

`ESP` is a RuneLite external plugin that renders distance overlays for nearby NPCs, bosses, players, and interactable world objects.

## Features

- distance labels in tiles
- optional marker dots and lines
- NPC, boss, player, and object categories
- per-category colors and visibility rules
- a searchable Target Browser for bundled NPC, boss, and object catalogs
- manual rare and exclusion lists for exact names and IDs

## Running The Plugin

### Fast test

```bat
launch-dev-plugin.bat
```

That runs the plugin directly from the project for testing.
It does not use the generated jar and it does not require `--developer-mode`.

### Generate the jar

```powershell
.\gradlew.bat jar
```

The generated jar will be:

```text
build\libs\esp-1.0-SNAPSHOT.jar
```

Put that jar in:

```text
%USERPROFILE%\.runelite\sideloaded-plugins\
```

### Let Gradle copy it for you

```powershell
.\gradlew.bat installSideloadedPlugin
```

That generates the jar and copies it into:

`%USERPROFILE%\.runelite\sideloaded-plugins`

That is a separate path from `launch-dev-plugin.bat`.

RuneLite only loads sideloaded plugins when it is launched with:

```text
--developer-mode
```

## Jagex Launcher Note

The current Jagex Launcher UI does not expose a reliable local-development path for sideloaded RuneLite plugins. This project should be treated as a direct dev-run project, not a Jagex Launcher sideload workflow.

If you want a normal release install without dev launchers, the correct path is Plugin Hub publication rather than local sideloading through Jagex Launcher.

## Target Browser vs Manual Filters

The plugin has two different lookup paths:

- `Target Browser`
  - searches the bundled offline NPC, boss, and object catalogs
  - these catalogs are based on local cache-style data
  - some entries use internal-style names instead of the clean in-game display name
  - some display-name aliases may be missing
- manual config fields
  - match the live RuneLite client data
  - NPCs and players use the live in-game name and ID exposed by RuneLite
  - objects use the live loaded scene object name and ID exposed by RuneLite

This means a target can still work when added manually even if it does not show up cleanly in the Target Browser search.

Example:

- `Gee` is an NPC in-game
- the browser now tries to match human-facing suffixes, so queries like `Gee` can still match internal names such as `prif_gee`
- but not every offline entry has a perfect alias set
- but manually adding `Gee` to the rare NPC names field can still work because the overlay matches the live RuneLite NPC name

## Target Browser Limitations

The Target Browser is useful, but it is not a perfect live game encyclopedia.

- not every NPC, boss, or object has a clean display-name entry in the bundled catalog
- some entries are stored with internal names such as `prif_gee`
- bosses can also exist as multiple NPC variants
- the browser is best treated as a convenience search layer, not the final source of truth

If the browser misses something:

1. add the target manually by exact name
2. add the target manually by exact ID
3. use the sources below to look up the correct ID

## Finding IDs

Good sources for IDs:

- local OSRS MCP data
  - NPCs: `search_npctypes`
  - objects: `search_loctypes`
  - this is the best local source for quick lookups while developing
- OSRS Wiki NPC IDs
  - [https://oldschool.runescape.wiki/w/NPC_IDs](https://oldschool.runescape.wiki/w/NPC_IDs)
- OSRS Wiki Object IDs
  - [https://oldschool.runescape.wiki/w/Object_IDs](https://oldschool.runescape.wiki/w/Object_IDs)
- OSRS Wiki Special:Lookup
  - [https://oldschool.runescape.wiki/w/Special:Lookup](https://oldschool.runescape.wiki/w/Special:Lookup)
  - useful when you already have an ID and want to confirm the NPC or object page quickly

Practical rule:

- if you know the exact NPC or boss name, try the manual name field first
- if you need exact matching across variants, use the ID fields
- if the Target Browser misses a target, look up the ID from OSRS MCP or the wiki and add it manually
