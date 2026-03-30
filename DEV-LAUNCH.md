# Development Launch Guide

## Fast Test

Install Java first.

Use the direct dev launcher:

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

If Gradle is already prepared and Java is already configured, `launch-dev-plugin.bat` can usually call `gradlew.bat` on its own. The explicit extra step is mainly a troubleshooting/workaround path.

## What The RuneLite Arguments Mean

### `--insecure-write-credentials`

Put this in **`RuneLite (configure)`** when using a Jagex account and you want RuneLite to export usable local credentials for the direct dev launcher.

It affects:

- `%USERPROFILE%\.runelite\credentials.properties`

### `--developer-mode`

This is for RuneLite sideloaded plugins in:

- `%USERPROFILE%\.runelite\sideloaded-plugins\`

It is **not** required for `launch-dev-plugin.bat`.

### Short version

- direct dev launcher from source:
  - `--developer-mode` not required
- Jagex-account credential export:
  - `--insecure-write-credentials` matters
- sideloaded jar testing:
  - `--developer-mode` matters

That builds the project and runs RuneLite directly for testing.
It does not use the generated jar and it does not require `--developer-mode`.
It also repairs the common broken-Java setup, writes a debug log to `logs\launch-dev-plugin.log`, and will try to install Temurin 21 with `winget` if no working JDK is found.

You can also run:

```powershell
.\gradlew.bat run
```

## Generate The Plugin Jar

Build the jar:

```powershell
.\gradlew.bat jar
```

The generated file will be:

```text
build\libs\esp-1.0-SNAPSHOT.jar
```

This jar path is separate from the direct dev launcher above.

## Put The Jar In The Right Place

Copy that jar to:

```text
%USERPROFILE%\.runelite\sideloaded-plugins\
```

If you want Gradle to do that copy for you automatically, run:

```powershell
.\gradlew.bat installSideloadedPlugin
```

## File Locations

| Location | Path |
|----------|------|
| Your plugin jar | `%USERPROFILE%\.runelite\sideloaded-plugins\` |
| RuneLite settings | `%USERPROFILE%\.runelite\settings.properties` |
| Client logs | `%USERPROFILE%\.runelite\logs\` |
| Jagex Launcher | `%ProgramFiles(x86)%\Jagex Launcher\` or `%ProgramFiles%\Jagex Launcher\` |

## Jagex Launcher

The Jagex Launcher route is not documented here as a supported local dev workflow. The current launcher UI does not reliably expose the older sideload/developer-argument flow that these plugin guides used to depend on.

For this project, use the direct dev run instead.


**Plugin not loading?**
- Check logs for errors
- Use `launch-dev-plugin.bat` for testing
- If you generated a jar manually, make sure it is in `%USERPROFILE%\.runelite\sideloaded-plugins\`

**Jagex Launcher not found?**
- Jagex Launcher is not part of the supported local dev path for this project
