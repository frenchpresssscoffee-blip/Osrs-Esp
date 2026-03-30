package com.example;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import javax.inject.Singleton;
import lombok.extern.slf4j.Slf4j;

@Singleton
@Slf4j
class TargetCatalog
{
	private static final int MIN_TEXT_QUERY_LENGTH = 2;
	private static final int UNKNOWN_TOTAL_ENTRIES = -1;

	private volatile List<CatalogEntry> npcEntries;
	private volatile List<CatalogEntry> bossEntries;
	private volatile List<CatalogEntry> objectEntries;

	SearchResult search(CatalogKind kind, String query, int limit)
	{
		final String normalizedQuery = CatalogEntry.normalizeForSearch(query);
		if (normalizedQuery.isEmpty())
		{
			return SearchResult.empty(UNKNOWN_TOTAL_ENTRIES, true);
		}

		final boolean numericQuery = normalizedQuery.chars().allMatch(Character::isDigit);
		if (!numericQuery && normalizedQuery.length() < MIN_TEXT_QUERY_LENGTH)
		{
			return SearchResult.empty(UNKNOWN_TOTAL_ENTRIES, true);
		}

		final List<CatalogEntry> entries = getEntries(kind);

		final List<ScoredEntry> matches = new ArrayList<>();
		for (CatalogEntry entry : entries)
		{
			final int score = entry.matchScore(normalizedQuery, numericQuery);
			if (score >= 0)
			{
				matches.add(new ScoredEntry(entry, score));
			}
		}

		matches.sort(
			Comparator.comparingInt(ScoredEntry::getScore)
				.thenComparingInt(scoredEntry -> scoredEntry.entry.getId())
				.thenComparing(scoredEntry -> scoredEntry.entry.getDisplayName(), String.CASE_INSENSITIVE_ORDER)
		);

		final List<CatalogEntry> visibleResults = new ArrayList<>(Math.min(limit, matches.size()));
		for (int index = 0; index < matches.size() && index < limit; index++)
		{
			visibleResults.add(matches.get(index).entry);
		}

		return new SearchResult(List.copyOf(visibleResults), entries.size(), matches.size(), false);
	}

	private List<CatalogEntry> getEntries(CatalogKind kind)
	{
		switch (kind)
		{
			case BOSS:
				return getOrLoadBossEntries();
			case OBJECT:
				return getOrLoadObjectEntries();
			case NPC:
			default:
				return getOrLoadNpcEntries();
		}
	}

	private List<CatalogEntry> getOrLoadNpcEntries()
	{
		List<CatalogEntry> entries = npcEntries;
		if (entries == null)
		{
			synchronized (this)
			{
				entries = npcEntries;
				if (entries == null)
				{
					entries = loadEntries(CatalogKind.NPC);
					npcEntries = entries;
				}
			}
		}

		return entries;
	}

	private List<CatalogEntry> getOrLoadObjectEntries()
	{
		List<CatalogEntry> entries = objectEntries;
		if (entries == null)
		{
			synchronized (this)
			{
				entries = objectEntries;
				if (entries == null)
				{
					entries = loadEntries(CatalogKind.OBJECT);
					objectEntries = entries;
				}
			}
		}

		return entries;
	}

	private List<CatalogEntry> getOrLoadBossEntries()
	{
		List<CatalogEntry> entries = bossEntries;
		if (entries == null)
		{
			synchronized (this)
			{
				entries = bossEntries;
				if (entries == null)
				{
					entries = buildBossEntries();
					bossEntries = entries;
				}
			}
		}

		return entries;
	}

	private List<CatalogEntry> loadEntries(CatalogKind kind)
	{
		final List<CatalogEntry> entries = new ArrayList<>();
		try (InputStream stream = TargetCatalog.class.getClassLoader().getResourceAsStream(kind.getResourceName()))
		{
			if (stream == null)
			{
				log.warn("Catalog resource {} was not found", kind.getResourceName());
				return List.of();
			}

			try (BufferedReader reader = new BufferedReader(new InputStreamReader(stream, StandardCharsets.UTF_8)))
			{
				String line;
				while ((line = reader.readLine()) != null)
				{
					final CatalogEntry entry = parseEntry(kind, line);
					if (entry != null)
					{
						entries.add(entry);
					}
				}
			}
		}
		catch (IOException ex)
		{
			log.warn("Unable to load {}", kind.getResourceName(), ex);
			return List.of();
		}

		return List.copyOf(entries);
	}

	private List<CatalogEntry> buildBossEntries()
	{
		final Set<Integer> bossIds = PresetCatalog.getAutoBossIds();
		final Set<String> bossNames = PresetCatalog.getAutoBossNames();
		final Set<String> bossDisplayNames = PresetCatalog.getAutoBossDisplayNames();
		final LinkedHashMap<String, CatalogEntry> bosses = new LinkedHashMap<>();
		for (CatalogEntry npcEntry : getOrLoadNpcEntries())
		{
			if (bossIds.contains(npcEntry.getId())
				|| bossNames.contains(CatalogEntry.normalizeForSearch(npcEntry.getRawName()))
				|| bossNames.contains(CatalogEntry.normalizeForSearch(npcEntry.getDisplayName())))
			{
				bosses.put(bossKey(npcEntry), new CatalogEntry(CatalogKind.BOSS, npcEntry.getId(), npcEntry.getRawName(), npcEntry.getDisplayName()));
			}
		}

		for (String bossDisplayName : bossDisplayNames)
		{
			final String normalizedBossName = CatalogEntry.normalizeForSearch(bossDisplayName);
			bosses.putIfAbsent("name:" + normalizedBossName, new CatalogEntry(CatalogKind.BOSS, -1, bossDisplayName, bossDisplayName));
		}

		final List<CatalogEntry> entries = new ArrayList<>(bosses.values());
		entries.sort(
			Comparator.comparing(CatalogEntry::getDisplayName, String.CASE_INSENSITIVE_ORDER)
				.thenComparingInt(CatalogEntry::getId)
		);
		return List.copyOf(entries);
	}

	private String bossKey(CatalogEntry entry)
	{
		return entry.hasId()
			? "id:" + entry.getId()
			: "name:" + CatalogEntry.normalizeForSearch(entry.getDisplayName());
	}


	private CatalogEntry parseEntry(CatalogKind kind, String line)
	{
		if (line == null || line.isBlank())
		{
			return null;
		}

		final int separator = line.indexOf('\t');
		if (separator <= 0 || separator >= line.length() - 1)
		{
			return null;
		}

		try
		{
			final int id = Integer.parseInt(line.substring(0, separator).trim());
			final String rawName = line.substring(separator + 1).trim();
			if (rawName.isEmpty())
			{
				return null;
			}

			return new CatalogEntry(kind, id, rawName);
		}
		catch (NumberFormatException ignored)
		{
			return null;
		}
	}

	static final class SearchResult
	{
		private final List<CatalogEntry> results;
		private final int totalEntries;
		private final int totalMatches;
		private final boolean queryTooShort;

		private SearchResult(List<CatalogEntry> results, int totalEntries, int totalMatches, boolean queryTooShort)
		{
			this.results = results;
			this.totalEntries = totalEntries;
			this.totalMatches = totalMatches;
			this.queryTooShort = queryTooShort;
		}

		private static SearchResult empty(int totalEntries, boolean queryTooShort)
		{
			return new SearchResult(List.of(), totalEntries, 0, queryTooShort);
		}

		List<CatalogEntry> getResults()
		{
			return results;
		}

		int getTotalEntries()
		{
			return totalEntries;
		}

		int getTotalMatches()
		{
			return totalMatches;
		}

		boolean isQueryTooShort()
		{
			return queryTooShort;
		}
	}

	private static final class ScoredEntry
	{
		private final CatalogEntry entry;
		private final int score;

		private ScoredEntry(CatalogEntry entry, int score)
		{
			this.entry = entry;
			this.score = score;
		}

		private int getScore()
		{
			return score;
		}
	}
}
