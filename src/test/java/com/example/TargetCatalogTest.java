package com.example;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

public class TargetCatalogTest
{
	@Test
	public void bossSearchFindsScurriusByName()
	{
		final TargetCatalog catalog = new TargetCatalog();

		final TargetCatalog.SearchResult result = catalog.search(CatalogKind.BOSS, "Scurrius", 50);

		assertFalse(result.isQueryTooShort());
		assertTrue(result.getResults().stream().anyMatch(entry -> "Scurrius".equals(entry.getDisplayName())));
	}

	@Test
	public void bossSearchSupportsMappedBossIds()
	{
		final TargetCatalog catalog = new TargetCatalog();

		final TargetCatalog.SearchResult result = catalog.search(CatalogKind.BOSS, "2215", 50);

		assertFalse(result.isQueryTooShort());
		assertTrue(result.getResults().stream().anyMatch(entry -> entry.getId() == 2215));
	}

	@Test
	public void emptyQueryStaysInIdleState()
	{
		final TargetCatalog catalog = new TargetCatalog();

		final TargetCatalog.SearchResult result = catalog.search(CatalogKind.BOSS, "", 50);

		assertTrue(result.isQueryTooShort());
		assertTrue(result.getResults().isEmpty());
	}

	@Test
	public void autoBossNamesUseSearchNormalization()
	{
		assertTrue(PresetCatalog.getAutoBossNames().contains(CatalogEntry.normalizeForSearch("K'ril Tsutsaroth")));
		assertTrue(PresetCatalog.getAutoBossDisplayNames().contains("Scurrius"));
	}

	@Test
	public void catalogEntrySuffixAliasMatchesHumanFacingQuery()
	{
		final CatalogEntry entry = new CatalogEntry(CatalogKind.NPC, 9199, "prif_gee");

		assertTrue(entry.matchScore(CatalogEntry.normalizeForSearch("Gee"), false) >= 0);
	}
}
