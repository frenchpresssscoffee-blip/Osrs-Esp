# Development Launch Guide

## Fast Test

Use the direct dev launcher:

```bat
launch-dev-plugin.bat
```

That builds the project and runs RuneLite directly for testing.
It does not use the generated jar and it does not require `--developer-mode`.

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
