package com.example;

import com.google.inject.Provides;
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import javax.inject.Inject;
import lombok.extern.slf4j.Slf4j;
import net.runelite.api.Actor;
import net.runelite.api.Client;
import net.runelite.api.GameState;
import net.runelite.api.NPC;
import net.runelite.api.ObjectComposition;
import net.runelite.api.Player;
import net.runelite.api.Tile;
import net.runelite.api.TileObject;
import net.runelite.api.coords.LocalPoint;
import net.runelite.api.coords.WorldArea;
import net.runelite.api.coords.WorldPoint;
import net.runelite.api.events.GameTick;
import net.runelite.client.callback.ClientThread;
import net.runelite.client.config.ConfigManager;
import net.runelite.client.eventbus.Subscribe;
import net.runelite.client.events.ConfigChanged;
import net.runelite.client.plugins.Plugin;
import net.runelite.client.plugins.PluginDescriptor;
import net.runelite.client.ui.ClientToolbar;
import net.runelite.client.ui.NavigationButton;
import net.runelite.client.ui.ColorScheme;
import net.runelite.client.ui.overlay.OverlayManager;

@Slf4j
@PluginDescriptor(
	name = "ESP",
	description = "Shows distance overlays for nearby NPCs, bosses, players, and objects",
	tags = {"npc", "distance", "tiles", "overlay"}
)
public class EspPlugin extends Plugin
{
	@Inject
	private Client client;

	@Inject
	private EspConfig config;

	@Inject
	private OverlayManager overlayManager;

	@Inject
	private EspOverlay overlay;

	@Inject
	private ClientToolbar clientToolbar;

	@Inject
	private TargetCatalogPanel targetCatalogPanel;

	@Inject
	private ClientThread clientThread;

	@Inject
	private TargetCatalogConfigEditor targetCatalogConfigEditor;

	private final Set<Integer> unstableObjectIds = new HashSet<>();
	private final Map<Long, Optional<String>> objectNameCacheByHash = new HashMap<>();
	private volatile List<TrackedTarget> cachedTargets = List.of();
	private volatile List<TrackedTarget> cachedObjectTargets = List.of();
	private volatile ParsedFilters parsedFilters = ParsedFilters.empty();
	private volatile WorldPoint lastObjectCacheOrigin;
	private volatile int lastObjectCachePlane = Integer.MIN_VALUE;
	private volatile int lastObjectCacheRange = -1;
	private volatile int tickCounter;
	private volatile int lastObjectCacheTick = -1;
	private volatile boolean objectCacheDirty = true;
	private NavigationButton navigationButton;

	@Override
	protected void startUp()
	{
		unstableObjectIds.clear();
		objectNameCacheByHash.clear();
		cachedObjectTargets = List.of();
		lastObjectCacheOrigin = null;
		lastObjectCachePlane = Integer.MIN_VALUE;
		lastObjectCacheRange = -1;
		tickCounter = 0;
		lastObjectCacheTick = -1;
		objectCacheDirty = true;
		overlayManager.add(overlay);
		navigationButton = NavigationButton.builder()
			.tooltip("ESP Browser")
			.icon(createNavigationIcon())
			.priority(5)
			.panel(targetCatalogPanel)
			.build();
		clientToolbar.addNavigation(navigationButton);
		rebuildParsedFilters();
		scheduleRefreshTrackedTargets();
		log.debug("ESP started");
	}

	@Override
	protected void shutDown()
	{
		unstableObjectIds.clear();
		objectNameCacheByHash.clear();
		cachedTargets = List.of();
		cachedObjectTargets = List.of();
		lastObjectCacheOrigin = null;
		lastObjectCachePlane = Integer.MIN_VALUE;
		lastObjectCacheRange = -1;
		tickCounter = 0;
		lastObjectCacheTick = -1;
		objectCacheDirty = true;
		if (navigationButton != null)
		{
			clientToolbar.removeNavigation(navigationButton);
			navigationButton = null;
		}
		overlayManager.remove(overlay);
		log.debug("ESP stopped");
	}

	@Subscribe
	public void onGameTick(GameTick gameTick)
	{
		tickCounter++;
		refreshTrackedTargets();
	}

	@Subscribe
	public void onConfigChanged(ConfigChanged event)
	{
		if (!"npcdistance".equals(event.getGroup()))
		{
			return;
		}

		targetCatalogConfigEditor.onConfigChanged(event.getKey());
		rebuildParsedFilters();
		objectCacheDirty = true;
		scheduleRefreshTrackedTargets();
	}

	boolean isReady()
	{
		return client.getGameState() == GameState.LOGGED_IN && client.getLocalPlayer() != null;
	}

	List<TrackedTarget> getTrackedTargets()
	{
		return cachedTargets;
	}

	Color getDisplayColor(TrackedTarget target)
	{
		if (target != null && target.isRare())
		{
			return config.rareColor();
		}

		switch (target.getCategory())
		{
			case BOSS:
				return config.bossColor();
			case PLAYER:
				return config.playerColor();
			case OBJECT:
				return config.objectColor();
			case NPC:
			default:
				return config.npcColor();
		}
	}

	String buildLabel(TrackedTarget target)
	{
		if (target == null)
		{
			return null;
		}

		final StringBuilder label = new StringBuilder();
		if (target.isRare())
		{
			label.append("[Rare] ");
		}

		if (config.showCategoryTags())
		{
			label.append('[').append(target.getCategory().getLabel()).append("] ");
		}

		if (config.showNames() && target.getName() != null && !target.getName().isBlank())
		{
			label.append(target.getName()).append(": ");
		}

		label.append(target.getDistance()).append('t');
		return label.toString();
	}

	boolean isTargetVisible(TrackedTarget target, Actor hoveredActor, Set<Long> hoveredObjectHashes, Player localPlayer)
	{
		if (target == null)
		{
			return false;
		}

		switch (getVisibilityMode(target.getCategory()))
		{
			case FOCUSED:
				return isFocused(target, hoveredActor, hoveredObjectHashes, localPlayer);
			case NAME_MATCH:
				return matchesConfiguredNameFilter(target);
			case RARE_ONLY:
				return target.isRare();
			case ALL:
			default:
				return true;
		}
	}

	boolean shouldShowLabel()
	{
		return config.showLabels();
	}

	boolean usesActorFocusVisibility()
	{
		return config.npcVisibilityMode() == TargetVisibilityMode.FOCUSED
			|| config.bossVisibilityMode() == TargetVisibilityMode.FOCUSED
			|| config.playerVisibilityMode() == TargetVisibilityMode.FOCUSED;
	}

	boolean usesObjectFocusVisibility()
	{
		return config.objectVisibilityMode() == TargetVisibilityMode.FOCUSED;
	}

	String getObjectName(TileObject object)
	{
		if (object == null)
		{
			return null;
		}

		final Optional<String> cachedName = objectNameCacheByHash.get(object.getHash());
		if (cachedName != null)
		{
			return cachedName.orElse(null);
		}

		final int objectId = object.getId();
		try
		{
			final ObjectComposition composition = client.getObjectDefinition(objectId);
			if (composition == null)
			{
				objectNameCacheByHash.put(object.getHash(), Optional.empty());
				return null;
			}

			final ObjectComposition resolved = resolveObjectComposition(objectId, composition);
			final String name = resolved.getName();
			if (name == null || name.isBlank() || "null".equalsIgnoreCase(name) || !hasActions(resolved.getActions()))
			{
				objectNameCacheByHash.put(object.getHash(), Optional.empty());
				return null;
			}

			objectNameCacheByHash.put(object.getHash(), Optional.of(name));
			return name;
		}
		catch (RuntimeException ex)
		{
			if (unstableObjectIds.add(objectId))
			{
				if (unstableObjectIds.size() <= 20)
				{
					log.debug("Skipping unstable object definition {}", objectId);
				}
			}

			objectNameCacheByHash.put(object.getHash(), Optional.empty());
			return null;
		}
	}

	private void refreshTrackedTargets()
	{
		if (!isReady())
		{
			cachedTargets = List.of();
			cachedObjectTargets = List.of();
			return;
		}

		final ParsedFilters filters = parsedFilters;
		final List<TrackedTarget> refreshed = new ArrayList<>();

		collectNpcTargets(refreshed, filters);
		collectPlayerTargets(refreshed, filters);
		refreshed.addAll(getOrRefreshObjectTargets(filters));

		refreshed.sort(
			Comparator.comparingInt(TrackedTarget::getDistance)
				.thenComparingInt(target -> target.getCategory().ordinal())
				.thenComparing(target -> safeName(target.getName()), String.CASE_INSENSITIVE_ORDER)
		);

		cachedTargets = List.copyOf(refreshed);
	}

	private void scheduleRefreshTrackedTargets()
	{
		clientThread.invokeLater(this::refreshTrackedTargets);
	}

	private List<TrackedTarget> getOrRefreshObjectTargets(ParsedFilters filters)
	{
		if (!config.showObjects())
		{
			cachedObjectTargets = List.of();
			lastObjectCacheOrigin = null;
			lastObjectCachePlane = Integer.MIN_VALUE;
			lastObjectCacheRange = -1;
			lastObjectCacheTick = -1;
			objectCacheDirty = true;
			return List.of();
		}

		final Player localPlayer = client.getLocalPlayer();
		final WorldPoint currentOrigin = localPlayer == null ? null : localPlayer.getWorldLocation();
		final int currentPlane = client.getPlane();
		final int currentRange = config.objectMaxDistance();
		if (!objectCacheDirty
			&& currentOrigin != null
			&& currentOrigin.equals(lastObjectCacheOrigin)
			&& currentPlane == lastObjectCachePlane
			&& currentRange == lastObjectCacheRange
			&& tickCounter - lastObjectCacheTick < 5)
		{
			return cachedObjectTargets;
		}

		final List<TrackedTarget> refreshedObjects = new ArrayList<>();
		collectObjectTargets(refreshedObjects, filters);
		cachedObjectTargets = List.copyOf(refreshedObjects);
		lastObjectCacheOrigin = currentOrigin;
		lastObjectCachePlane = currentPlane;
		lastObjectCacheRange = currentRange;
		lastObjectCacheTick = tickCounter;
		objectCacheDirty = false;
		return cachedObjectTargets;
	}

	private void collectNpcTargets(List<TrackedTarget> refreshed, ParsedFilters filters)
	{
		for (NPC npc : client.getNpcs())
		{
			final TargetCategory category = getNpcCategory(npc, filters);
			if (category == null)
			{
				continue;
			}

			final String normalizedName = normalize(npc.getName());
			if (isExcluded(category, npc.getId(), normalizedName, filters))
			{
				continue;
			}

			final int distance = getDistanceTo(npc.getWorldArea());
			if (distance == Integer.MAX_VALUE || distance > getMaxDistance(category))
			{
				continue;
			}

			final boolean rare = category == TargetCategory.BOSS ? isRareBoss(npc, filters) : isRareNpc(npc, filters);
			refreshed.add(TrackedTarget.forNpc(category, npc, distance, rare));
		}
	}

	private void collectPlayerTargets(List<TrackedTarget> refreshed, ParsedFilters filters)
	{
		if (!config.showPlayers())
		{
			return;
		}

		for (Player player : client.getPlayers())
		{
			if (player == null || player == client.getLocalPlayer() || player.getName() == null)
			{
				continue;
			}

			final String normalizedName = normalize(player.getName());
			if (isExcluded(TargetCategory.PLAYER, -1, normalizedName, filters))
			{
				continue;
			}

			final int distance = getDistanceTo(player.getWorldArea());
			if (distance == Integer.MAX_VALUE || distance > config.playerMaxDistance())
			{
				continue;
			}

			refreshed.add(TrackedTarget.forPlayer(player, distance, isRarePlayer(player, filters)));
		}
	}

	private void collectObjectTargets(List<TrackedTarget> refreshed, ParsedFilters filters)
	{
		if (!config.showObjects())
		{
			return;
		}

		final Tile[][][] tiles = client.getScene().getTiles();
		final int plane = client.getPlane();
		if (tiles == null || plane < 0 || plane >= tiles.length)
		{
			return;
		}

		final Player localPlayer = client.getLocalPlayer();
		final LocalPoint playerLocal = localPlayer == null ? null : localPlayer.getLocalLocation();
		if (playerLocal == null || !playerLocal.isInScene())
		{
			return;
		}

		final Tile[][] planeTiles = tiles[plane];
		if (planeTiles == null || planeTiles.length == 0)
		{
			return;
		}

		final int range = Math.max(1, config.objectMaxDistance()) + 1;
		final int minSceneX = Math.max(0, playerLocal.getSceneX() - range);
		final int maxSceneX = Math.min(planeTiles.length - 1, playerLocal.getSceneX() + range);
		final int minSceneY = Math.max(0, playerLocal.getSceneY() - range);
		final Set<Long> seen = new HashSet<>();
		for (int sceneX = minSceneX; sceneX <= maxSceneX; sceneX++)
		{
			final Tile[] row = planeTiles[sceneX];
			if (row == null || row.length == 0)
			{
				continue;
			}

			final int maxSceneY = Math.min(row.length - 1, playerLocal.getSceneY() + range);
			for (int sceneY = minSceneY; sceneY <= maxSceneY; sceneY++)
			{
				final Tile tile = row[sceneY];
				if (tile == null)
				{
					continue;
				}

				// Skip far tiles before resolving any object definitions.
				final int tileDistance = getDistanceTo(tile.getWorldLocation());
				if (tileDistance == Integer.MAX_VALUE || tileDistance > config.objectMaxDistance())
				{
					continue;
				}

				collectTileObject(refreshed, seen, tile.getWallObject(), filters);
				collectTileObject(refreshed, seen, tile.getDecorativeObject(), filters);
				collectTileObject(refreshed, seen, tile.getGroundObject(), filters);

				for (TileObject object : tile.getGameObjects())
				{
					collectTileObject(refreshed, seen, object, filters);
				}
			}
		}

		objectNameCacheByHash.keySet().retainAll(seen);
	}

	private void collectTileObject(List<TrackedTarget> refreshed, Set<Long> seen, TileObject object, ParsedFilters filters)
	{
		if (object == null || !seen.add(object.getHash()))
		{
			return;
		}

		final String name = getObjectName(object);
		if (name == null)
		{
			return;
		}

		if (isExcluded(TargetCategory.OBJECT, object.getId(), normalize(name), filters))
		{
			return;
		}

		final int distance = getDistanceTo(object.getWorldLocation());
		if (distance == Integer.MAX_VALUE || distance > config.objectMaxDistance())
		{
			return;
		}

		refreshed.add(TrackedTarget.forObject(name, object, distance, isRareObject(object, name, filters)));
	}

	private ObjectComposition resolveObjectComposition(int objectId, ObjectComposition composition)
	{
		if (unstableObjectIds.contains(objectId))
		{
			return composition;
		}

		try
		{
			final ObjectComposition impostor = composition.getImpostor();
			return impostor != null ? impostor : composition;
		}
		catch (RuntimeException ex)
		{
			if (unstableObjectIds.add(objectId))
			{
				if (unstableObjectIds.size() <= 20)
				{
					log.debug("Falling back to base object definition for {}", objectId);
				}
			}

			return composition;
		}
	}

	private TargetCategory getNpcCategory(NPC npc, ParsedFilters filters)
	{
		if (npc == null || npc.getName() == null || npc.isDead())
		{
			return null;
		}

		if (isBoss(npc, filters))
		{
			return config.showBosses() ? TargetCategory.BOSS : null;
		}

		return config.showNpcs() ? TargetCategory.NPC : null;
	}

	private boolean isBoss(NPC npc, ParsedFilters filters)
	{
		if (npc == null)
		{
			return false;
		}

		if (filters.bossIds.contains(npc.getId()))
		{
			return true;
		}

		return filters.bossNames.contains(normalize(npc.getName()));
	}

	private boolean isRareNpc(NPC npc, ParsedFilters filters)
	{
		return npc != null
			&& (filters.rareNpcIds.contains(npc.getId()) || filters.rareNpcNames.contains(normalize(npc.getName())));
	}

	private boolean isRareBoss(NPC npc, ParsedFilters filters)
	{
		return npc != null
			&& (filters.rareBossIds.contains(npc.getId()) || filters.rareBossNames.contains(normalize(npc.getName())));
	}

	private boolean isRarePlayer(Player player, ParsedFilters filters)
	{
		return player != null && filters.rarePlayerNames.contains(normalize(player.getName()));
	}

	private boolean isRareObject(TileObject object, String name, ParsedFilters filters)
	{
		return object != null
			&& (filters.rareObjectIds.contains(object.getId()) || filters.rareObjectNames.contains(normalize(name)));
	}

	private boolean isExcluded(TargetCategory category, int id, String normalizedName, ParsedFilters filters)
	{
		if (id >= 0 && filters.getExcludedIds(category).contains(id))
		{
			return true;
		}

		return matchesNameFilter(normalizedName, filters.getExcludedNames(category));
	}

	private int getMaxDistance(TargetCategory category)
	{
		switch (category)
		{
			case BOSS:
				return config.bossMaxDistance();
			case PLAYER:
				return config.playerMaxDistance();
			case OBJECT:
				return config.objectMaxDistance();
			case NPC:
			default:
				return config.npcMaxDistance();
		}
	}

	private TargetVisibilityMode getVisibilityMode(TargetCategory category)
	{
		switch (category)
		{
			case BOSS:
				return config.bossVisibilityMode();
			case PLAYER:
				return config.playerVisibilityMode();
			case OBJECT:
				return config.objectVisibilityMode();
			case NPC:
			default:
				return config.npcVisibilityMode();
		}
	}

	private void rebuildParsedFilters()
	{
		final Set<String> bossNames = config.autoDetectKnownBosses()
			? mergeStringSets(PresetCatalog.getAutoBossNames(), parseExactTokens(config.bossNameAllowlist()))
			: parseExactTokens(config.bossNameAllowlist());
		final Set<Integer> bossIds = config.autoDetectKnownBosses()
			? mergeIntegerSets(PresetCatalog.getAutoBossIds(), parseIntegerTokens(config.bossIdAllowlist()))
			: parseIntegerTokens(config.bossIdAllowlist());

		parsedFilters = new ParsedFilters(
			bossNames,
			bossIds,
			parseFilterTokens(config.npcNameFilter()),
			parseFilterTokens(config.bossNameFilter()),
			parseFilterTokens(config.playerNameFilter()),
			parseFilterTokens(config.objectNameFilter()),
			parseFilterTokens(config.npcExcludeNames()),
			parseIntegerTokens(config.npcExcludeIds()),
			parseFilterTokens(config.bossExcludeNames()),
			parseIntegerTokens(config.bossExcludeIds()),
			parseFilterTokens(config.playerExcludeNames()),
			parseFilterTokens(config.objectExcludeNames()),
			parseIntegerTokens(config.objectExcludeIds()),
			parseExactTokens(config.rareNpcNames()),
			parseIntegerTokens(config.rareNpcIds()),
			parseExactTokens(config.rareBossNames()),
			parseIntegerTokens(config.rareBossIds()),
			parseExactTokens(config.rarePlayerNames()),
			parseExactTokens(config.rareObjectNames()),
			parseIntegerTokens(config.rareObjectIds())
		);
	}

	private boolean matchesConfiguredNameFilter(TrackedTarget target)
	{
		if (target == null)
		{
			return false;
		}

		final List<String> nameFilters = parsedFilters.getNameFilters(target.getCategory());
		return !nameFilters.isEmpty() && matchesNameFilter(target.getNormalizedName(), nameFilters);
	}

	private boolean isFocused(TrackedTarget target, Actor hoveredActor, Set<Long> hoveredObjectHashes, Player localPlayer)
	{
		return target != null
			&& (target.matchesHovered(hoveredActor, hoveredObjectHashes) || target.matchesInteracting(localPlayer));
	}

	private boolean matchesNameFilter(String normalizedName, List<String> tokens)
	{
		if (normalizedName == null || normalizedName.isEmpty() || tokens.isEmpty())
		{
			return false;
		}

		for (String token : tokens)
		{
			if (normalizedName.contains(token))
			{
				return true;
			}
		}

		return false;
	}

	private Set<String> parseExactTokens(String raw)
	{
		return Collections.unmodifiableSet(new HashSet<>(parseFilterTokens(raw)));
	}

	private Set<Integer> parseIntegerTokens(String raw)
	{
		final Set<Integer> values = new HashSet<>();
		for (String token : parseFilterTokens(raw))
		{
			try
			{
				values.add(Integer.parseInt(token));
			}
			catch (NumberFormatException ignored)
			{
				// Ignore malformed IDs so one bad token does not break the full rule set.
			}
		}

		return Collections.unmodifiableSet(values);
	}

	private Set<String> mergeStringSets(Set<String> base, Set<String> extra)
	{
		if (base.isEmpty())
		{
			return extra;
		}

		if (extra.isEmpty())
		{
			return base;
		}

		final Set<String> merged = new HashSet<>(base);
		merged.addAll(extra);
		return Collections.unmodifiableSet(merged);
	}

	private Set<Integer> mergeIntegerSets(Set<Integer> base, Set<Integer> extra)
	{
		if (base.isEmpty())
		{
			return extra;
		}

		if (extra.isEmpty())
		{
			return base;
		}

		final Set<Integer> merged = new HashSet<>(base);
		merged.addAll(extra);
		return Collections.unmodifiableSet(merged);
	}

	private List<String> parseFilterTokens(String raw)
	{
		final List<String> tokens = new ArrayList<>();
		if (raw == null || raw.isBlank())
		{
			return tokens;
		}

		for (String token : raw.split("[,;\\r\\n]+"))
		{
			final String normalized = normalize(token);
			if (!normalized.isEmpty())
			{
				tokens.add(normalized);
			}
		}

		return List.copyOf(tokens);
	}

	private String normalize(String value)
	{
		return CatalogEntry.normalizeForSearch(value);
	}

	private String safeName(String value)
	{
		return value == null ? "" : value;
	}

	private boolean hasActions(String[] actions)
	{
		if (actions == null)
		{
			return false;
		}

		for (String action : actions)
		{
			if (action != null && !action.isBlank())
			{
				return true;
			}
		}

		return false;
	}

	private int getDistanceTo(WorldArea targetArea)
	{
		final Player localPlayer = client.getLocalPlayer();
		if (localPlayer == null)
		{
			return Integer.MAX_VALUE;
		}

		final WorldArea playerArea = localPlayer.getWorldArea();
		if (playerArea == null || targetArea == null)
		{
			return Integer.MAX_VALUE;
		}

		return playerArea.distanceTo(targetArea);
	}

	private int getDistanceTo(WorldPoint targetPoint)
	{
		final Player localPlayer = client.getLocalPlayer();
		if (localPlayer == null)
		{
			return Integer.MAX_VALUE;
		}

		final WorldArea playerArea = localPlayer.getWorldArea();
		if (playerArea == null || targetPoint == null || targetPoint.getPlane() != client.getPlane())
		{
			return Integer.MAX_VALUE;
		}

		return playerArea.distanceTo(targetPoint);
	}

	@Provides
	EspConfig provideConfig(ConfigManager configManager)
	{
		return configManager.getConfig(EspConfig.class);
	}

	private BufferedImage createNavigationIcon()
	{
		final BufferedImage image = new BufferedImage(16, 16, BufferedImage.TYPE_INT_ARGB);
		final Graphics2D graphics = image.createGraphics();
		try
		{
			graphics.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
			graphics.setColor(ColorScheme.BRAND_ORANGE);
			graphics.setStroke(new BasicStroke(2f));
			graphics.drawOval(2, 2, 8, 8);
			graphics.drawLine(9, 9, 13, 13);
			graphics.setColor(new Color(255, 255, 255, 190));
			graphics.fillOval(9, 2, 3, 3);
		}
		finally
		{
			graphics.dispose();
		}

		return image;
	}

	private static final class ParsedFilters
	{
		private final Set<String> bossNames;
		private final Set<Integer> bossIds;
		private final List<String> npcNameFilters;
		private final List<String> bossNameFilters;
		private final List<String> playerNameFilters;
		private final List<String> objectNameFilters;
		private final List<String> npcExcludedNames;
		private final Set<Integer> npcExcludedIds;
		private final List<String> bossExcludedNames;
		private final Set<Integer> bossExcludedIds;
		private final List<String> playerExcludedNames;
		private final List<String> objectExcludedNames;
		private final Set<Integer> objectExcludedIds;
		private final Set<String> rareNpcNames;
		private final Set<Integer> rareNpcIds;
		private final Set<String> rareBossNames;
		private final Set<Integer> rareBossIds;
		private final Set<String> rarePlayerNames;
		private final Set<String> rareObjectNames;
		private final Set<Integer> rareObjectIds;

		private ParsedFilters(
			Set<String> bossNames,
			Set<Integer> bossIds,
			List<String> npcNameFilters,
			List<String> bossNameFilters,
			List<String> playerNameFilters,
			List<String> objectNameFilters,
			List<String> npcExcludedNames,
			Set<Integer> npcExcludedIds,
			List<String> bossExcludedNames,
			Set<Integer> bossExcludedIds,
			List<String> playerExcludedNames,
			List<String> objectExcludedNames,
			Set<Integer> objectExcludedIds,
			Set<String> rareNpcNames,
			Set<Integer> rareNpcIds,
			Set<String> rareBossNames,
			Set<Integer> rareBossIds,
			Set<String> rarePlayerNames,
			Set<String> rareObjectNames,
			Set<Integer> rareObjectIds)
		{
			this.bossNames = bossNames;
			this.bossIds = bossIds;
			this.npcNameFilters = npcNameFilters;
			this.bossNameFilters = bossNameFilters;
			this.playerNameFilters = playerNameFilters;
			this.objectNameFilters = objectNameFilters;
			this.npcExcludedNames = npcExcludedNames;
			this.npcExcludedIds = npcExcludedIds;
			this.bossExcludedNames = bossExcludedNames;
			this.bossExcludedIds = bossExcludedIds;
			this.playerExcludedNames = playerExcludedNames;
			this.objectExcludedNames = objectExcludedNames;
			this.objectExcludedIds = objectExcludedIds;
			this.rareNpcNames = rareNpcNames;
			this.rareNpcIds = rareNpcIds;
			this.rareBossNames = rareBossNames;
			this.rareBossIds = rareBossIds;
			this.rarePlayerNames = rarePlayerNames;
			this.rareObjectNames = rareObjectNames;
			this.rareObjectIds = rareObjectIds;
		}

		private static ParsedFilters empty()
		{
			return new ParsedFilters(
				Set.of(), Set.of(),
				List.of(), List.of(), List.of(), List.of(),
				List.of(), Set.of(),
				List.of(), Set.of(),
				List.of(),
				List.of(), Set.of(),
				Set.of(), Set.of(), Set.of(), Set.of(), Set.of(), Set.of(), Set.of()
			);
		}

		private List<String> getNameFilters(TargetCategory category)
		{
			switch (category)
			{
				case BOSS:
					return bossNameFilters;
				case PLAYER:
					return playerNameFilters;
				case OBJECT:
					return objectNameFilters;
				case NPC:
				default:
					return npcNameFilters;
			}
		}

		private List<String> getExcludedNames(TargetCategory category)
		{
			switch (category)
			{
				case BOSS:
					return bossExcludedNames;
				case PLAYER:
					return playerExcludedNames;
				case OBJECT:
					return objectExcludedNames;
				case NPC:
				default:
					return npcExcludedNames;
			}
		}

		private Set<Integer> getExcludedIds(TargetCategory category)
		{
			switch (category)
			{
				case BOSS:
					return bossExcludedIds;
				case OBJECT:
					return objectExcludedIds;
				case PLAYER:
					return Set.of();
				case NPC:
				default:
					return npcExcludedIds;
			}
		}
	}
}
