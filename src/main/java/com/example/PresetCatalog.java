package com.example;

import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import lombok.extern.slf4j.Slf4j;
import net.runelite.client.hiscore.HiscoreSkill;
import net.runelite.client.hiscore.HiscoreSkillType;

@Slf4j
final class PresetCatalog
{
	private static final String AUTO_BOSS_RESOURCE_NAME = "boss-ids.txt";
	private static final Set<String> NON_NPC_BOSS_ENTRIES = Set.of(
		"Barrows Chests",
		"Chambers of Xeric",
		"Chambers of Xeric: Challenge Mode",
		"Lunar Chests",
		"The Gauntlet",
		"The Corrupted Gauntlet",
		"Theatre of Blood",
		"Theatre of Blood: Hard Mode",
		"Tombs of Amascut",
		"Tombs of Amascut: Expert Mode"
	);

	static Set<Integer> getAutoBossIds()
	{
		return AutoBossHolder.AUTO_BOSS_IDS;
	}

	static Set<String> getAutoBossNames()
	{
		return AutoBossHolder.AUTO_BOSS_NAMES;
	}

	static Set<String> getAutoBossDisplayNames()
	{
		return AutoBossHolder.AUTO_BOSS_DISPLAY_NAMES;
	}

	private static Set<Integer> loadAutoBossIds()
	{
		try (InputStream stream = PresetCatalog.class.getClassLoader().getResourceAsStream(AUTO_BOSS_RESOURCE_NAME))
		{
			if (stream == null)
			{
				log.warn("Auto boss data resource {} was not found", AUTO_BOSS_RESOURCE_NAME);
				return Set.of();
			}

			final Set<Integer> values = new HashSet<>();
			final String raw = new String(stream.readAllBytes(), java.nio.charset.StandardCharsets.UTF_8);
			for (String token : raw.split("[,;\\r\\n]+"))
			{
				final String normalized = token == null ? "" : token.strip();
				if (normalized.isEmpty())
				{
					continue;
				}

				try
				{
					values.add(Integer.parseInt(normalized));
				}
				catch (NumberFormatException ignored)
				{
					// Ignore malformed IDs in the boss resource so one bad line does not break loading.
				}
			}

			return Collections.unmodifiableSet(values);
		}
		catch (IOException ex)
		{
			log.warn("Unable to load auto boss data from {}", AUTO_BOSS_RESOURCE_NAME, ex);
			return Set.of();
		}
	}

	private static Set<String> loadAutoBossNames()
	{
		final Set<String> names = new HashSet<>();
		for (String displayName : loadAutoBossDisplayNames())
		{
			names.add(CatalogEntry.normalizeForSearch(displayName));
		}
		return Collections.unmodifiableSet(names);
	}

	private static Set<String> loadAutoBossDisplayNames()
	{
		final Set<String> names = new HashSet<>();
		Arrays.stream(HiscoreSkill.values())
			.filter(skill -> skill.getType() == HiscoreSkillType.BOSS)
			.map(HiscoreSkill::getName)
			.filter(name -> name != null && !name.isBlank())
			.filter(name -> !NON_NPC_BOSS_ENTRIES.contains(name))
			.forEach(names::add);
		return Collections.unmodifiableSet(names);
	}

	private static final class AutoBossHolder
	{
		private static final Set<Integer> AUTO_BOSS_IDS = loadAutoBossIds();
		private static final Set<String> AUTO_BOSS_DISPLAY_NAMES = loadAutoBossDisplayNames();
		private static final Set<String> AUTO_BOSS_NAMES = loadAutoBossNames();
	}
}
