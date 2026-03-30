package com.example;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.lang.reflect.Field;
import java.util.List;
import net.runelite.client.config.ConfigManager;
import org.junit.Test;

public class TargetCatalogConfigEditorTest
{
	@Test
	public void addNpcIdsToHiddenRoutesBossIdsToBossExcludes() throws Exception
	{
		final ConfigManager configManager = mock(ConfigManager.class);
		final EspConfig config = mock(EspConfig.class);
		when(configManager.getConfiguration("npcdistance", "npcExcludeIds")).thenReturn("123, foo");
		when(configManager.getConfiguration("npcdistance", "bossExcludeIds")).thenReturn("");
		when(configManager.getConfiguration("npcdistance", "bossIdAllowlist")).thenReturn("");
		when(config.npcExcludeIds()).thenReturn("123, foo");
		when(config.bossExcludeIds()).thenReturn("");
		when(config.bossIdAllowlist()).thenReturn("");

		final TargetCatalogConfigEditor editor = new TargetCatalogConfigEditor();
		setField(editor, "configManager", configManager);
		setField(editor, "config", config);

		final int added = editor.addNpcIdsToHidden(List.of(2215, 456));

		assertEquals(2, added);
		verify(configManager).setConfiguration("npcdistance", "npcExcludeIds", "foo, 123, 456");
		verify(configManager).setConfiguration("npcdistance", "bossExcludeIds", "2215");
	}

	@Test
	public void addBossNamesToRareEnablesBossTracking() throws Exception
	{
		final ConfigManager configManager = mock(ConfigManager.class);
		final EspConfig config = mock(EspConfig.class);
		when(configManager.getConfiguration("npcdistance", "rareBossNames")).thenReturn("");
		when(config.rareBossNames()).thenReturn("");

		final TargetCatalogConfigEditor editor = new TargetCatalogConfigEditor();
		setField(editor, "configManager", configManager);
		setField(editor, "config", config);

		final int added = editor.addBossNamesToRare(List.of("Scurrius"));

		assertEquals(1, added);
		verify(configManager).setConfiguration("npcdistance", "rareBossNames", "Scurrius");
		verify(configManager).setConfiguration("npcdistance", "showBosses", true);
	}

	@Test
	public void addNpcIdsToRareRoutesBossIdsToRareBossIds() throws Exception
	{
		final ConfigManager configManager = mock(ConfigManager.class);
		final EspConfig config = mock(EspConfig.class);
		when(configManager.getConfiguration("npcdistance", "rareNpcIds")).thenReturn("");
		when(configManager.getConfiguration("npcdistance", "rareBossIds")).thenReturn("");
		when(configManager.getConfiguration("npcdistance", "bossIdAllowlist")).thenReturn("");
		when(config.rareNpcIds()).thenReturn("");
		when(config.rareBossIds()).thenReturn("");
		when(config.bossIdAllowlist()).thenReturn("");

		final TargetCatalogConfigEditor editor = new TargetCatalogConfigEditor();
		setField(editor, "configManager", configManager);
		setField(editor, "config", config);

		final int added = editor.addNpcIdsToRare(List.of(2215, 456));

		assertEquals(2, added);
		verify(configManager).setConfiguration("npcdistance", "rareNpcIds", "456");
		verify(configManager).setConfiguration("npcdistance", "rareBossIds", "2215");
		verify(configManager).setConfiguration("npcdistance", "showBosses", true);
	}

	@Test
	public void addNpcNamesToHiddenAppendsNames() throws Exception
	{
		final ConfigManager configManager = mock(ConfigManager.class);
		final EspConfig config = mock(EspConfig.class);
		when(configManager.getConfiguration("npcdistance", "npcExcludeNames")).thenReturn("");
		when(config.npcExcludeNames()).thenReturn("Goblin");

		final TargetCatalogConfigEditor editor = new TargetCatalogConfigEditor();
		setField(editor, "configManager", configManager);
		setField(editor, "config", config);

		final int added = editor.addNpcNamesToHidden(List.of("Cow"));

		assertEquals(1, added);
		verify(configManager).setConfiguration("npcdistance", "npcExcludeNames", "Cow, Goblin");
	}

	@Test
	public void addObjectIdsAndNamesAppendToBothObjectFields() throws Exception
	{
		final ConfigManager configManager = mock(ConfigManager.class);
		final EspConfig config = mock(EspConfig.class);
		when(configManager.getConfiguration("npcdistance", "rareObjectIds")).thenReturn("");
		when(configManager.getConfiguration("npcdistance", "rareObjectNames")).thenReturn("");
		when(config.objectExcludeIds()).thenReturn("");
		when(config.objectExcludeNames()).thenReturn("");
		when(config.rareObjectIds()).thenReturn("100");
		when(config.rareObjectNames()).thenReturn("Bank booth");

		final TargetCatalogConfigEditor editor = new TargetCatalogConfigEditor();
		setField(editor, "configManager", configManager);
		setField(editor, "config", config);

		final int addedIds = editor.addObjectIdsToRare(List.of(200));
		final int addedNames = editor.addObjectNamesToRare(List.of("Yew tree"));

		assertEquals(1, addedIds);
		assertEquals(1, addedNames);
		verify(configManager).setConfiguration("npcdistance", "rareObjectIds", "100, 200");
		verify(configManager).setConfiguration("npcdistance", "rareObjectNames", "Bank booth, Yew tree");
		verify(configManager, atLeastOnce()).setConfiguration("npcdistance", "showObjects", true);
	}

	@Test
	public void addBossIdsToRareAppendsToLiveConfigValue() throws Exception
	{
		final ConfigManager configManager = mock(ConfigManager.class);
		final EspConfig config = mock(EspConfig.class);
		when(configManager.getConfiguration("npcdistance", "rareBossIds")).thenReturn("");
		when(config.rareBossIds()).thenReturn("9050");

		final TargetCatalogConfigEditor editor = new TargetCatalogConfigEditor();
		setField(editor, "configManager", configManager);
		setField(editor, "config", config);

		final int added = editor.addBossIdsToRare(List.of(12080));

		assertEquals(1, added);
		verify(configManager).setConfiguration("npcdistance", "rareBossIds", "9050, 12080");
		verify(configManager).setConfiguration("npcdistance", "showBosses", true);
	}

	@Test
	public void configChangeInvalidatesSessionValue() throws Exception
	{
		final ConfigManager configManager = mock(ConfigManager.class);
		final EspConfig config = mock(EspConfig.class);
		when(configManager.getConfiguration("npcdistance", "rareBossIds")).thenReturn("");
		when(config.rareBossIds()).thenReturn("");

		final TargetCatalogConfigEditor editor = new TargetCatalogConfigEditor();
		setField(editor, "configManager", configManager);
		setField(editor, "config", config);

		editor.addBossIdsToRare(List.of(9050));
		editor.onConfigChanged("rareBossIds");
		when(config.rareBossIds()).thenReturn("12080");

		final int added = editor.addBossIdsToRare(List.of(13669));

		assertEquals(1, added);
		verify(configManager).setConfiguration("npcdistance", "rareBossIds", "12080, 13669");
	}

	private void setField(Object target, String fieldName, Object value) throws Exception
	{
		final Field field = target.getClass().getDeclaredField(fieldName);
		field.setAccessible(true);
		field.set(target, value);
	}
}
