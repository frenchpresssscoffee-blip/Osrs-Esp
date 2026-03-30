package com.example;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.concurrent.ConcurrentHashMap;
import javax.inject.Inject;
import javax.inject.Singleton;
import net.runelite.client.config.ConfigManager;

@Singleton
class TargetCatalogConfigEditor
{
	private static final String CONFIG_GROUP = "npcdistance";

	@Inject
	private ConfigManager configManager;

	@Inject
	private EspConfig config;

	private final Map<String, String> sessionValues = new ConcurrentHashMap<>();

	void onConfigChanged(String key)
	{
		if (key != null && !key.isBlank())
		{
			sessionValues.remove(key);
		}
	}

	int addNpcIdsToRare(Collection<Integer> ids)
	{
		final SplitIds splitIds = splitBossIds(ids);
		int added = appendIds("rareNpcIds", splitIds.standardIds);
		if (!splitIds.bossIds.isEmpty())
		{
			added += appendIds("rareBossIds", splitIds.bossIds);
			configManager.setConfiguration(CONFIG_GROUP, "showBosses", true);
		}

		return added;
	}

	int addNpcIdsToBossAllowlist(Collection<Integer> ids)
	{
		final int added = appendIds("bossIdAllowlist", ids);
		if (added > 0)
		{
			configManager.setConfiguration(CONFIG_GROUP, "showBosses", true);
		}

		return added;
	}

	int addBossIdsToHidden(Collection<Integer> ids)
	{
		return appendIds("bossExcludeIds", ids);
	}

	int addNpcNamesToRare(Collection<String> names)
	{
		return appendNames("rareNpcNames", names);
	}

	int addNpcNamesToHidden(Collection<String> names)
	{
		return appendNames("npcExcludeNames", names);
	}

	int addBossIdsToRare(Collection<Integer> ids)
	{
		final int added = appendIds("rareBossIds", ids);
		if (added > 0)
		{
			configManager.setConfiguration(CONFIG_GROUP, "showBosses", true);
		}

		return added;
	}

	int addBossNamesToRare(Collection<String> names)
	{
		final int added = appendNames("rareBossNames", names);
		if (added > 0)
		{
			configManager.setConfiguration(CONFIG_GROUP, "showBosses", true);
		}

		return added;
	}

	int addBossNamesToAllowlist(Collection<String> names)
	{
		final int added = appendNames("bossNameAllowlist", names);
		if (added > 0)
		{
			configManager.setConfiguration(CONFIG_GROUP, "showBosses", true);
		}

		return added;
	}

	int addBossNamesToHidden(Collection<String> names)
	{
		return appendNames("bossExcludeNames", names);
	}

	int addNpcIdsToHidden(Collection<Integer> ids)
	{
		final SplitIds splitIds = splitBossIds(ids);
		return appendIds("npcExcludeIds", splitIds.standardIds) + appendIds("bossExcludeIds", splitIds.bossIds);
	}

	private SplitIds splitBossIds(Collection<Integer> ids)
	{
		if (ids == null || ids.isEmpty())
		{
			return SplitIds.empty();
		}

		final Set<Integer> knownBossIds = new HashSet<>(PresetCatalog.getAutoBossIds());
		knownBossIds.addAll(parseConfiguredIds("bossIdAllowlist"));

		final List<Integer> bossIds = new ArrayList<>();
		final List<Integer> standardIds = new ArrayList<>();
		for (Integer id : ids)
		{
			if (id == null)
			{
				continue;
			}

			if (knownBossIds.contains(id))
			{
				bossIds.add(id);
			}
			else
			{
				standardIds.add(id);
			}
		}

		return new SplitIds(standardIds, bossIds);
	}

	private Set<Integer> parseConfiguredIds(String key)
	{
		final Set<Integer> values = new HashSet<>();
		for (String current : getCurrentValues(key))
		{
			if (current == null || current.isBlank())
			{
				continue;
			}

			for (String token : current.split("[,;\\r\\n]+"))
			{
				final String trimmed = token.trim();
				if (trimmed.isEmpty())
				{
					continue;
				}

				try
				{
					values.add(Integer.parseInt(trimmed));
				}
				catch (NumberFormatException ignored)
				{
					// Ignore malformed tokens while reading manual config.
				}
			}
		}

		return values;
	}

	int addObjectIdsToRare(Collection<Integer> ids)
	{
		final int added = appendIds("rareObjectIds", ids);
		if (added > 0)
		{
			configManager.setConfiguration(CONFIG_GROUP, "showObjects", true);
		}

		return added;
	}

	int addObjectNamesToRare(Collection<String> names)
	{
		final int added = appendNames("rareObjectNames", names);
		if (added > 0)
		{
			configManager.setConfiguration(CONFIG_GROUP, "showObjects", true);
		}

		return added;
	}

	int addObjectIdsToHidden(Collection<Integer> ids)
	{
		final int added = appendIds("objectExcludeIds", ids);
		if (added > 0)
		{
			configManager.setConfiguration(CONFIG_GROUP, "showObjects", true);
		}

		return added;
	}

	int addObjectNamesToHidden(Collection<String> names)
	{
		final int added = appendNames("objectExcludeNames", names);
		if (added > 0)
		{
			configManager.setConfiguration(CONFIG_GROUP, "showObjects", true);
		}

		return added;
	}

	private int appendIds(String key, Collection<Integer> ids)
	{
		if (ids == null || ids.isEmpty())
		{
			return 0;
		}

		final List<String> invalidTokens = new ArrayList<>();
		final LinkedHashSet<Integer> merged = new LinkedHashSet<>();
		for (String current : getCurrentValues(key))
		{
			if (current == null || current.isBlank())
			{
				continue;
			}

			for (String token : current.split("[,;\\r\\n]+"))
			{
				final String trimmed = token.trim();
				if (trimmed.isEmpty())
				{
					continue;
				}

				try
				{
					merged.add(Integer.parseInt(trimmed));
				}
				catch (NumberFormatException ignored)
				{
					if (!invalidTokens.contains(trimmed))
					{
						invalidTokens.add(trimmed);
					}
				}
			}
		}

		final int before = merged.size();
		for (Integer id : ids)
		{
			if (id != null)
			{
				merged.add(id);
			}
		}

		if (merged.size() == before)
		{
			return 0;
		}

		final List<String> mergedTokens = new ArrayList<>(invalidTokens);
		merged.stream().map(String::valueOf).forEach(mergedTokens::add);

		final String mergedValue = String.join(", ", mergedTokens);
		sessionValues.put(key, mergedValue);
		configManager.setConfiguration(CONFIG_GROUP, key, mergedValue);
		return merged.size() - before;
	}

	private int appendNames(String key, Collection<String> names)
	{
		if (names == null || names.isEmpty())
		{
			return 0;
		}

		final TreeSet<String> merged = new TreeSet<>(String.CASE_INSENSITIVE_ORDER);
		for (String current : getCurrentValues(key))
		{
			if (current == null || current.isBlank())
			{
				continue;
			}

			for (String token : current.split("[,;\\r\\n]+"))
			{
				final String trimmed = token.trim();
				if (!trimmed.isEmpty())
				{
					merged.add(trimmed);
				}
			}
		}

		final int before = merged.size();
		for (String name : names)
		{
			if (name != null && !name.isBlank())
			{
				merged.add(name.strip());
			}
		}

		if (merged.size() == before)
		{
			return 0;
		}

		final String mergedValue = String.join(", ", merged);
		sessionValues.put(key, mergedValue);
		configManager.setConfiguration(CONFIG_GROUP, key, mergedValue);
		return merged.size() - before;
	}

	private List<String> getCurrentValues(String key)
	{
		final LinkedHashSet<String> values = new LinkedHashSet<>();
		addValue(values, sessionValues.get(key));
		addValue(values, readLiveConfigValue(key));
		addValue(values, configManager.getConfiguration(CONFIG_GROUP, key));
		return new ArrayList<>(values);
	}

	private void addValue(Set<String> values, String value)
	{
		if (value != null && !value.isBlank())
		{
			values.add(value);
		}
	}

	private String readLiveConfigValue(String key)
	{
		if (config == null)
		{
			return null;
		}

		switch (key)
		{
			case "rareNpcIds":
				return config.rareNpcIds();
			case "bossIdAllowlist":
				return config.bossIdAllowlist();
			case "bossExcludeIds":
				return config.bossExcludeIds();
			case "rareBossIds":
				return config.rareBossIds();
			case "objectExcludeIds":
				return config.objectExcludeIds();
			case "rareObjectIds":
				return config.rareObjectIds();
			case "npcExcludeIds":
				return config.npcExcludeIds();
			case "rareNpcNames":
				return config.rareNpcNames();
			case "rareBossNames":
				return config.rareBossNames();
			case "bossNameAllowlist":
				return config.bossNameAllowlist();
			case "bossExcludeNames":
				return config.bossExcludeNames();
			case "rareObjectNames":
				return config.rareObjectNames();
			case "npcExcludeNames":
				return config.npcExcludeNames();
			case "objectExcludeNames":
				return config.objectExcludeNames();
			default:
				return null;
		}
	}

	private static final class SplitIds
	{
		private final List<Integer> standardIds;
		private final List<Integer> bossIds;

		private SplitIds(List<Integer> standardIds, List<Integer> bossIds)
		{
			this.standardIds = standardIds;
			this.bossIds = bossIds;
		}

		private static SplitIds empty()
		{
			return new SplitIds(List.of(), List.of());
		}
	}
}
