# ESP

`ESP` is a RuneLite external plugin project for drawing distance overlays for:

- NPCs
- bosses
- players
- interactable world objects

It supports labels, markers, lines, off-screen indicators, category filters, rare-target lists, and a searchable Target Browser backed by bundled local catalogs.

## What This Project Is

This repository is a **development project** for a RuneLite external plugin.

There are two main ways to use it:

1. **Direct dev run**
   - Use [launch-dev-plugin.bat](/C:/Users/Administrator/Desktop/example-plugin-master/launch-dev-plugin.bat)
   - Best for testing the plugin from source
   - Does not require manual Gradle installation
   - Does require a working JDK

2. **Built jar / sideloaded plugin**
   - Build the plugin jar
   - Copy it to `%USERPROFILE%\.runelite\sideloaded-plugins\`
   - Use a RuneLite path that actually supports sideloaded plugins

The direct dev launcher and the sideloaded-jar workflow are separate. Do not mix them up.

## Features

- distance labels in tiles
- optional marker dots and lines
- optional off-screen indicators
- NPC, boss, player, and object categories
- per-category colors and visibility rules
- rare target lists by exact IDs and names
- searchable Target Browser for bundled NPC, boss, and object catalogs
- object, NPC, boss, and player filtering from config

## Requirements

For the **direct dev launcher**:

- Windows
- a working JDK, preferably Java 17 or Java 21
- RuneLite installed

You do **not** need to install Gradle manually. This project includes the Gradle wrapper:

- [gradlew](/C:/Users/Administrator/Desktop/example-plugin-master/gradlew)
- [gradlew.bat](/C:/Users/Administrator/Desktop/example-plugin-master/gradlew.bat)
- [gradle/wrapper/gradle-wrapper.jar](/C:/Users/Administrator/Desktop/example-plugin-master/gradle/wrapper/gradle-wrapper.jar)

## What The RuneLite Arguments Mean

Two arguments come up a lot in this project:

### `--insecure-write-credentials`

This is used in **`RuneLite (configure)`**.

Purpose:

- helps RuneLite write local credentials to:
  - `%USERPROFILE%\.runelite\credentials.properties`
- used for the Jagex-account direct dev-run workflow

Use this when:

- you want the direct dev launcher to detect the account through locally exported credentials

### `--developer-mode`

This is for **sideloaded RuneLite plugins**.

Purpose:

- allows RuneLite to load jars from:
  - `%USERPROFILE%\.runelite\sideloaded-plugins\`

Use this when:

- you are testing a built jar from the sideloaded plugins folder

Do **not** use it for:

- the direct source-based dev launcher in this repository

### Most important distinction

- `launch-dev-plugin.bat` does **not** need `--developer-mode`
- the direct dev launcher loads the plugin from source
- `--insecure-write-credentials` is the important argument for the Jagex-account credential export flow

## Quick Start

### Fastest test

Install Java first.

Run:

```bat
launch-dev-plugin.bat
```

Exact troubleshooting order for fresh machines:

1. Run `launch-dev-plugin.bat`
2. Let it finish whatever setup or checks it needs to do
3. Close anything it opened
4. Run `gradlew.bat`
5. Let that finish
6. Run `launch-dev-plugin.bat` again

So the order is:

- install Java
- run `launch-dev-plugin.bat`
- close it
- run `gradlew.bat`
- run `launch-dev-plugin.bat` again

If the machine already has the wrapper and Java set up correctly, `launch-dev-plugin.bat` can usually call `gradlew.bat` on its own. The explicit extra step is mainly a troubleshooting/workaround path.

That launcher will:

- check Java
- try to find a working JDK
- remove broken Gradle Java overrides from `%USERPROFILE%\.gradle\gradle.properties` if they point to missing Java installs
- write a project-local `gradle.properties` with a valid `org.gradle.java.home=...` override
- write a debug log to:
  - `logs\launch-dev-plugin.log`
- start the RuneLite dev run if the environment is valid

### Environment check only

Run:

```bat
launch-dev-plugin.bat --check-only
```

That only validates and repairs the Java/Gradle setup. It does not launch the client.

## Jagex Accounts and Direct Dev Runs

This is the most confusing part of the project.

### Important distinction

The direct dev launcher does **not** inherit a live Jagex Launcher session the same way the normal Jagex-launched RuneLite client does.

What usually happens instead is:

- RuneLite is launched directly from the project
- RuneLite reads exported credentials from disk
- the direct dev run uses those saved credentials

So if the dev launcher appears to "detect the account," it is usually because RuneLite successfully exported usable credentials locally, not because the dev launcher is truly running inside the Jagex Launcher session.

### What file matters

The most important file is:

```text
%USERPROFILE%\.runelite\credentials.properties
```

For a working Jagex-account dev flow, that file needs real values for:

```text
JX_ACCESS_TOKEN=...
JX_REFRESH_TOKEN=...
```

If those values are blank, the direct dev run will usually fall back to the old login screen.

### Do not share this file

Do **not** copy or share `credentials.properties`.

Each user must generate their own credentials file on their own machine.

## Exact Jagex Account Setup Flow

If someone wants to use the direct dev launcher with a Jagex account, these are the exact steps:

1. Install RuneLite normally.
2. Open **Windows Start Menu**.
3. Search for:
   - `RuneLite (configure)`
4. Open **RuneLite (configure)**.
5. In **Client arguments**, add:

```text
--insecure-write-credentials
```

6. Save and close the config window.
7. Fully close RuneLite and the Jagex Launcher.
8. Make sure both are running at the same permission level, preferably **normal user**, not Administrator.
9. Open the **Jagex Launcher**.
10. Set **Game client = RuneLite**.
11. Launch RuneLite through the Jagex Launcher once.
12. Let RuneLite open fully.
13. Close RuneLite.
14. Open:

```text
%USERPROFILE%\.runelite
```

15. Check:

```text
credentials.properties
```

16. Confirm these are not blank:

```text
JX_ACCESS_TOKEN=...
JX_REFRESH_TOKEN=...
```

17. Then run:

```bat
launch-dev-plugin.bat
```

### If the token file exists but values are blank

If the file exists but looks like this:

```text
JX_ACCESS_TOKEN=
JX_REFRESH_TOKEN=
```

then the export step failed.

Try this:

1. Close RuneLite and the Jagex Launcher.
2. Delete:

```text
%USERPROFILE%\.runelite\credentials.properties
```

3. Repeat the Jagex Launcher flow above.
4. Re-check the file.

## If the Direct Dev Run Shows the Old Login Screen

That usually means one of these is true:

- `credentials.properties` is missing
- `JX_ACCESS_TOKEN` / `JX_REFRESH_TOKEN` are blank
- the Jagex export step did not complete
- RuneLite and Jagex Launcher ran at mismatched permission levels

First thing to check:

```text
%USERPROFILE%\.runelite\credentials.properties
```

Second thing to check:

```text
%USERPROFILE%\.runelite\logs\client.log
```

Useful lines include:

- `read ... credentials from disk`
- `No session file exists`
- `launcher version unknown`

Those lines usually mean the direct dev launcher is reading local exported credentials rather than using a live launcher session.

## Java and Gradle Troubleshooting

### Common failure

One common error is:

```text
The supplied javaHome seems to be invalid.
I cannot find the java executable.
Tried location: C:\Program Files\Java\jre1.8.0_481\bin\java.exe
```

That means Gradle is pointing at an old broken Java path.

The launcher tries to fix this automatically by:

- checking `%USERPROFILE%\.gradle\gradle.properties`
- removing invalid `org.gradle.java.home=...` lines
- writing a valid project-local `gradle.properties`

### Check Java manually

Run:

```bat
echo %JAVA_HOME%
where java
java -version
```

Recommended JDKs:

- Java 17
- Java 21

### Project-local Gradle override

The launcher may create a project-local file:

```text
gradle.properties
```

with a line like:

```text
org.gradle.java.home=C:/Program Files/Eclipse Adoptium/jdk-21.0.10.7-hotspot
```

That file is intentionally ignored by git and is safe to regenerate.

## Building the Jar

Build the plugin jar:

```powershell
.\gradlew.bat jar
```

The generated jar will be:

```text
build\libs\esp-1.0-SNAPSHOT.jar
```

## Installing the Jar Manually

Copy the built jar to:

```text
%USERPROFILE%\.runelite\sideloaded-plugins\
```

Or let Gradle copy it for you:

```powershell
.\gradlew.bat installSideloadedPlugin
```

### Important limitation

The current Jagex Launcher UI does **not** provide a reliable local-development sideload workflow for this project.

Treat this repository as a **direct dev-run project first**.

If you want a normal public installation path later, the proper route is Plugin Hub publication, not relying on Jagex Launcher sideload behavior.

## Target Browser vs Manual Filters

The plugin has two lookup paths:

### Target Browser

- searches the bundled offline NPC, boss, and object catalogs
- catalogs come from local cache-style data
- some entries use internal-style names
- some display-name aliases are missing

### Manual config fields

- match live RuneLite client data
- NPCs and players use the live in-game name and ID exposed by RuneLite
- objects use the live loaded scene object name and ID exposed by RuneLite

This means a target can still work when added manually even if it does not show up cleanly in the Target Browser.

Example:

- `Gee` is an in-game NPC
- the offline catalog may store it as `prif_gee`
- the browser now tries to match human-facing suffixes, so `Gee` can still match `prif_gee`
- but alias coverage is still imperfect
- manually adding `Gee` by name can still work because manual matching uses live client names

## Target Browser Limitations

The Target Browser is useful, but it is not a perfect live game encyclopedia.

- not every NPC, boss, or object has a clean display-name entry
- some entries are stored with internal names
- bosses may exist in multiple NPC variants
- the browser should be treated as a convenience search layer, not a final source of truth

If the browser misses something:

1. add the target manually by exact name
2. add the target manually by exact ID
3. use the sources below to find the correct ID

## Finding IDs

Useful sources:

- local OSRS MCP data
  - NPCs: `search_npctypes`
  - objects: `search_loctypes`
- OSRS Wiki NPC IDs
  - [NPC_IDs](https://oldschool.runescape.wiki/w/NPC_IDs)
- OSRS Wiki Object IDs
  - [Object_IDs](https://oldschool.runescape.wiki/w/Object_IDs)
- OSRS Wiki Special:Lookup
  - [Special:Lookup](https://oldschool.runescape.wiki/w/Special:Lookup)

Practical rule:

- if you know the exact NPC or boss name, try the manual name field first
- if you need exact matching across variants, use the ID fields
- if the Target Browser misses a target, look up the ID and add it manually

## Useful File Locations

### Project files

- launcher:
  - [launch-dev-plugin.bat](/C:/Users/Administrator/Desktop/example-plugin-master/launch-dev-plugin.bat)
- build script:
  - [build.gradle](/C:/Users/Administrator/Desktop/example-plugin-master/build.gradle)
- plugin metadata:
  - [runelite-plugin.properties](/C:/Users/Administrator/Desktop/example-plugin-master/runelite-plugin.properties)

### Local RuneLite files

- RuneLite home:
  - `%USERPROFILE%\.runelite`
- credentials:
  - `%USERPROFILE%\.runelite\credentials.properties`
- client log:
  - `%USERPROFILE%\.runelite\logs\client.log`
- sideloaded plugins:
  - `%USERPROFILE%\.runelite\sideloaded-plugins\`

### Local launcher log

- dev launcher log:
  - `logs\launch-dev-plugin.log`

## Security Notes

- do not share `credentials.properties`
- do not copy your Jagex token file to another person
- do not commit generated token files or local launcher state
- each user should generate their own tokens locally through the Jagex Launcher flow

## Summary

For most users, the correct rule is:

- **plugin testing from source**: use `launch-dev-plugin.bat`
- **Jagex account login support in the dev run**: make sure `credentials.properties` contains real Jagex tokens
- **normal public installation**: use a proper distribution path later, not the direct dev launcher
