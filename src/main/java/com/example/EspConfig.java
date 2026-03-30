package com.example;

import java.awt.Color;
import net.runelite.api.Constants;
import net.runelite.client.config.Config;
import net.runelite.client.config.ConfigGroup;
import net.runelite.client.config.ConfigItem;
import net.runelite.client.config.ConfigSection;
import net.runelite.client.config.Range;

@ConfigGroup("npcdistance")
public interface EspConfig extends Config
{
	@ConfigSection(
		name = "General",
		description = "Shared label behavior",
		position = 0
	)
	String generalSection = "generalSection";

	@ConfigSection(
		name = "Visuals",
		description = "Shared marker and line presentation",
		position = 1
	)
	String visualsSection = "visualsSection";

	@ConfigSection(
		name = "Rare Targets",
		description = "Exact allowlists for rare or high-priority targets",
		position = 2,
		closedByDefault = true
	)
	String rareSection = "rareSection";

	@ConfigSection(
		name = "NPCs",
		description = "Regular NPC visibility rules",
		position = 3,
		closedByDefault = true
	)
	String npcsSection = "npcsSection";

	@ConfigSection(
		name = "Bosses",
		description = "Boss visibility rules and boss allowlists",
		position = 4,
		closedByDefault = true
	)
	String bossesSection = "bossesSection";

	@ConfigSection(
		name = "Players",
		description = "Other player visibility rules",
		position = 5,
		closedByDefault = true
	)
	String playersSection = "playersSection";

	@ConfigSection(
		name = "Objects",
		description = "Named interactable world object visibility rules",
		position = 6,
		closedByDefault = true
	)
	String objectsSection = "objectsSection";

	@ConfigItem(
		keyName = "showLabels",
		name = "Show distance labels",
		description = "Draw the distance text for visible targets",
		section = generalSection,
		position = 0
	)
	default boolean showLabels()
	{
		return true;
	}

	@ConfigItem(
		keyName = "showNames",
		name = "Include names",
		description = "Include the target name in the label",
		section = generalSection,
		position = 1
	)
	default boolean showNames()
	{
		return true;
	}

	@ConfigItem(
		keyName = "showCategoryTags",
		name = "Include category tags",
		description = "Prefix labels with NPC, Boss, Player, or Object",
		section = generalSection,
		position = 2
	)
	default boolean showCategoryTags()
	{
		return true;
	}

	@ConfigItem(
		keyName = "showMarkers",
		name = "Show dots",
		description = "Draw a dot marker for visible targets",
		section = visualsSection,
		position = 0
	)
	default boolean showMarkers()
	{
		return true;
	}

	@ConfigItem(
		keyName = "showLines",
		name = "Show lines",
		description = "Draw a line from your player to visible targets",
		section = visualsSection,
		position = 1
	)
	default boolean showLines()
	{
		return true;
	}

	@ConfigItem(
		keyName = "showOffscreenMarkers",
		name = "Show off-screen indicators",
		description = "Pin off-screen visible targets to the screen edge",
		section = visualsSection,
		position = 2
	)
	default boolean showOffscreenMarkers()
	{
		return true;
	}

	@ConfigItem(
		keyName = "markerBelowTarget",
		name = "Marker below target",
		description = "Place on-screen dots below the target instead of above it",
		section = visualsSection,
		position = 3
	)
	default boolean markerBelowTarget()
	{
		return true;
	}

	@Range(min = -150, max = 150)
	@ConfigItem(
		keyName = "lineStartOffsetY",
		name = "Line start offset Y",
		description = "Adjust the line origin up or down from your player's feet. Positive values move the line lower.",
		section = visualsSection,
		position = 4
	)
	default int lineStartOffsetY()
	{
		return 0;
	}

	@ConfigItem(
		keyName = "rareColor",
		name = "Rare target color",
		description = "Override color used for rare targets across every category",
		section = rareSection,
		position = 0
	)
	default Color rareColor()
	{
		return Color.MAGENTA;
	}

	@ConfigItem(
		keyName = "rareNpcNames",
		name = "Rare NPC names",
		description = "Exact NPC names that should always count as rare. Use the Target Browser to add more without typing them manually.",
		section = rareSection,
		position = 1
	)
	default String rareNpcNames()
	{
		return "";
	}

	@ConfigItem(
		keyName = "rareNpcIds",
		name = "Rare NPC IDs",
		description = "Exact NPC IDs that should always count as rare. Use the Target Browser to add more without typing them manually.",
		section = rareSection,
		position = 2
	)
	default String rareNpcIds()
	{
		return "";
	}

	@ConfigItem(
		keyName = "rareBossNames",
		name = "Rare boss names",
		description = "Exact boss names that should always count as rare. Use the Target Browser to add more without typing them manually.",
		section = rareSection,
		position = 3
	)
	default String rareBossNames()
	{
		return "";
	}

	@ConfigItem(
		keyName = "rareBossIds",
		name = "Rare boss IDs",
		description = "Exact boss NPC IDs that should always count as rare. Use the Target Browser to add more without typing them manually.",
		section = rareSection,
		position = 4
	)
	default String rareBossIds()
	{
		return "";
	}

	@ConfigItem(
		keyName = "rarePlayerNames",
		name = "Rare player names",
		description = "Exact player names that should always count as rare",
		section = rareSection,
		position = 5
	)
	default String rarePlayerNames()
	{
		return "";
	}

	@ConfigItem(
		keyName = "rareObjectNames",
		name = "Rare object names",
		description = "Exact object names that should always count as rare. Use the Target Browser to add more without typing them manually.",
		section = rareSection,
		position = 6
	)
	default String rareObjectNames()
	{
		return "";
	}

	@ConfigItem(
		keyName = "rareObjectIds",
		name = "Rare object IDs",
		description = "Exact object IDs that should always count as rare. Use the Target Browser to add more without typing them manually.",
		section = rareSection,
		position = 7
	)
	default String rareObjectIds()
	{
		return "";
	}

	@ConfigItem(
		keyName = "showNpcs",
		name = "Enabled",
		description = "Track regular NPCs",
		section = npcsSection,
		position = 0
	)
	default boolean showNpcs()
	{
		return true;
	}

	@ConfigItem(
		keyName = "npcVisibilityMode",
		name = "Visibility",
		description = "How regular NPCs become visible in the overlay",
		section = npcsSection,
		position = 1
	)
	default TargetVisibilityMode npcVisibilityMode()
	{
		return TargetVisibilityMode.ALL;
	}

	@Range(min = 1, max = Constants.SCENE_SIZE)
	@ConfigItem(
		keyName = "npcMaxDistance",
		name = "Range (tiles)",
		description = "Maximum regular NPC distance. Scene-based tracking tops out at 104 tiles.",
		section = npcsSection,
		position = 2
	)
	default int npcMaxDistance()
	{
		return 20;
	}

	@ConfigItem(
		keyName = "npcNameFilter",
		name = "Name contains",
		description = "Comma or newline separated substrings used only when Visibility is Name contains",
		section = npcsSection,
		position = 3
	)
	default String npcNameFilter()
	{
		return "";
	}

	@ConfigItem(
		keyName = "npcExcludeNames",
		name = "Hide names containing",
		description = "Comma or newline separated substrings that always hide matching NPCs",
		section = npcsSection,
		position = 4
	)
	default String npcExcludeNames()
	{
		return "";
	}

	@ConfigItem(
		keyName = "npcExcludeIds",
		name = "Hide NPC IDs",
		description = "Comma or newline separated NPC IDs that always hide matching NPCs",
		section = npcsSection,
		position = 5
	)
	default String npcExcludeIds()
	{
		return "";
	}

	@ConfigItem(
		keyName = "npcColor",
		name = "Overlay color",
		description = "Color used for regular NPC dots, lines, and labels",
		section = npcsSection,
		position = 6
	)
	default Color npcColor()
	{
		return Color.CYAN;
	}

	@ConfigItem(
		keyName = "showBosses",
		name = "Enabled",
		description = "Track bosses that match the boss allowlists",
		section = bossesSection,
		position = 0
	)
	default boolean showBosses()
	{
		return true;
	}

	@ConfigItem(
		keyName = "autoDetectKnownBosses",
		name = "Auto-detect known bosses",
		description = "Use the built-in known boss baseline first, then merge your manual boss IDs and names on top.",
		section = bossesSection,
		position = 1
	)
	default boolean autoDetectKnownBosses()
	{
		return true;
	}

	@ConfigItem(
		keyName = "bossVisibilityMode",
		name = "Visibility",
		description = "How bosses become visible in the overlay",
		section = bossesSection,
		position = 2
	)
	default TargetVisibilityMode bossVisibilityMode()
	{
		return TargetVisibilityMode.ALL;
	}

	@Range(min = 1, max = Constants.SCENE_SIZE)
	@ConfigItem(
		keyName = "bossMaxDistance",
		name = "Range (tiles)",
		description = "Maximum boss distance. Scene-based tracking tops out at 104 tiles.",
		section = bossesSection,
		position = 3
	)
	default int bossMaxDistance()
	{
		return 30;
	}

	@ConfigItem(
		keyName = "bossNameFilter",
		name = "Name contains",
		description = "Comma or newline separated substrings used only when Visibility is Name contains",
		section = bossesSection,
		position = 4
	)
	default String bossNameFilter()
	{
		return "";
	}

	@ConfigItem(
		keyName = "bossExcludeNames",
		name = "Hide names containing",
		description = "Comma or newline separated substrings that always hide matching bosses",
		section = bossesSection,
		position = 5
	)
	default String bossExcludeNames()
	{
		return "";
	}

	@ConfigItem(
		keyName = "bossExcludeIds",
		name = "Hide boss IDs",
		description = "Comma or newline separated NPC IDs that always hide matching bosses",
		section = bossesSection,
		position = 6
	)
	default String bossExcludeIds()
	{
		return "";
	}

	@ConfigItem(
		keyName = "bossNameAllowlist",
		name = "Boss names",
		description = "Comma or newline separated exact boss names that should be treated as bosses. When auto-detect is on, these merge with the built-in boss baseline.",
		section = bossesSection,
		position = 7
	)
	default String bossNameAllowlist()
	{
		return "";
	}

	@ConfigItem(
		keyName = "bossIdAllowlist",
		name = "Boss IDs",
		description = "Comma or newline separated NPC IDs that should be treated as bosses. When auto-detect is on, these merge with the built-in boss baseline.",
		section = bossesSection,
		position = 8
	)
	default String bossIdAllowlist()
	{
		return "";
	}

	@ConfigItem(
		keyName = "bossColor",
		name = "Overlay color",
		description = "Color used for boss dots, lines, and labels",
		section = bossesSection,
		position = 9
	)
	default Color bossColor()
	{
		return Color.RED;
	}

	@ConfigItem(
		keyName = "showPlayers",
		name = "Enabled",
		description = "Track other players",
		section = playersSection,
		position = 0
	)
	default boolean showPlayers()
	{
		return false;
	}

	@ConfigItem(
		keyName = "playerVisibilityMode",
		name = "Visibility",
		description = "How other players become visible in the overlay",
		section = playersSection,
		position = 1
	)
	default TargetVisibilityMode playerVisibilityMode()
	{
		return TargetVisibilityMode.ALL;
	}

	@Range(min = 1, max = Constants.SCENE_SIZE)
	@ConfigItem(
		keyName = "playerMaxDistance",
		name = "Range (tiles)",
		description = "Maximum player distance. Scene-based tracking tops out at 104 tiles.",
		section = playersSection,
		position = 2
	)
	default int playerMaxDistance()
	{
		return 20;
	}

	@ConfigItem(
		keyName = "playerNameFilter",
		name = "Name contains",
		description = "Comma or newline separated substrings used only when Visibility is Name contains",
		section = playersSection,
		position = 3
	)
	default String playerNameFilter()
	{
		return "";
	}

	@ConfigItem(
		keyName = "playerExcludeNames",
		name = "Hide names containing",
		description = "Comma or newline separated substrings that always hide matching players",
		section = playersSection,
		position = 4
	)
	default String playerExcludeNames()
	{
		return "";
	}

	@ConfigItem(
		keyName = "playerColor",
		name = "Overlay color",
		description = "Color used for player dots, lines, and labels",
		section = playersSection,
		position = 5
	)
	default Color playerColor()
	{
		return Color.ORANGE;
	}

	@ConfigItem(
		keyName = "showObjects",
		name = "Enabled",
		description = "Track named interactable world objects",
		section = objectsSection,
		position = 0
	)
	default boolean showObjects()
	{
		return false;
	}

	@ConfigItem(
		keyName = "objectVisibilityMode",
		name = "Visibility",
		description = "How named objects become visible in the overlay",
		section = objectsSection,
		position = 1
	)
	default TargetVisibilityMode objectVisibilityMode()
	{
		return TargetVisibilityMode.ALL;
	}

	@Range(min = 1, max = Constants.SCENE_SIZE)
	@ConfigItem(
		keyName = "objectMaxDistance",
		name = "Range (tiles)",
		description = "Maximum object distance. Scene-based tracking tops out at 104 tiles.",
		section = objectsSection,
		position = 2
	)
	default int objectMaxDistance()
	{
		return 15;
	}

	@ConfigItem(
		keyName = "objectNameFilter",
		name = "Name contains",
		description = "Comma or newline separated substrings used only when Visibility is Name contains",
		section = objectsSection,
		position = 3
	)
	default String objectNameFilter()
	{
		return "";
	}

	@ConfigItem(
		keyName = "objectExcludeNames",
		name = "Hide names containing",
		description = "Comma or newline separated substrings that always hide matching objects",
		section = objectsSection,
		position = 4
	)
	default String objectExcludeNames()
	{
		return "";
	}

	@ConfigItem(
		keyName = "objectExcludeIds",
		name = "Hide object IDs",
		description = "Comma or newline separated object IDs that always hide matching objects",
		section = objectsSection,
		position = 5
	)
	default String objectExcludeIds()
	{
		return "";
	}

	@ConfigItem(
		keyName = "objectColor",
		name = "Overlay color",
		description = "Color used for object dots, lines, and labels",
		section = objectsSection,
		position = 6
	)
	default Color objectColor()
	{
		return Color.GREEN;
	}
}
