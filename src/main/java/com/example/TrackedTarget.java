package com.example;

import java.awt.Graphics2D;
import java.awt.Polygon;
import net.runelite.api.Actor;
import net.runelite.api.Client;
import net.runelite.api.NPC;
import net.runelite.api.Perspective;
import net.runelite.api.Player;
import net.runelite.api.Point;
import net.runelite.api.TileObject;
import net.runelite.api.coords.LocalPoint;

final class TrackedTarget
{
	private static final int OBJECT_OVERLAY_HEIGHT = 40;

	private final TargetCategory category;
	private final String name;
	private final String normalizedName;
	private final int id;
	private final int distance;
	private final NPC npc;
	private final Player player;
	private final TileObject object;
	private final long objectHash;
	private final int overlayHeight;
	private final boolean rare;

	private TrackedTarget(TargetCategory category, String name, int id, int distance, NPC npc, Player player, TileObject object, long objectHash, int overlayHeight, boolean rare)
	{
		this.category = category;
		this.name = name;
		this.normalizedName = normalize(name);
		this.id = id;
		this.distance = distance;
		this.npc = npc;
		this.player = player;
		this.object = object;
		this.objectHash = objectHash;
		this.overlayHeight = overlayHeight;
		this.rare = rare;
	}

	static TrackedTarget forNpc(TargetCategory category, NPC npc, int distance, boolean rare)
	{
		return new TrackedTarget(category, npc.getName(), npc.getId(), distance, npc, null, null, 0L, npc.getLogicalHeight(), rare);
	}

	static TrackedTarget forPlayer(Player player, int distance, boolean rare)
	{
		return new TrackedTarget(TargetCategory.PLAYER, player.getName(), -1, distance, null, player, null, 0L, player.getLogicalHeight(), rare);
	}

	static TrackedTarget forObject(String name, TileObject object, int distance, boolean rare)
	{
		return new TrackedTarget(TargetCategory.OBJECT, name, object.getId(), distance, null, null, object, object.getHash(), OBJECT_OVERLAY_HEIGHT, rare);
	}

	TargetCategory getCategory()
	{
		return category;
	}

	String getName()
	{
		return name;
	}

	String getNormalizedName()
	{
		return normalizedName;
	}

	int getId()
	{
		return id;
	}

	int getDistance()
	{
		return distance;
	}

	boolean isRare()
	{
		return rare;
	}

	Actor getActor()
	{
		return npc != null ? npc : player;
	}

	TileObject getObject()
	{
		return object;
	}

	long getObjectHash()
	{
		return objectHash;
	}

	LocalPoint getLocalLocation()
	{
		try
		{
			if (object != null)
			{
				return object.getLocalLocation();
			}

			final Actor actor = getActor();
			return actor == null ? null : actor.getLocalLocation();
		}
		catch (RuntimeException ex)
		{
			return null;
		}
	}

	boolean matchesHovered(Actor hoveredActor, java.util.Set<Long> hoveredObjectHashes)
	{
		final Actor actor = getActor();
		if (actor != null)
		{
			return actor == hoveredActor;
		}

		return hoveredObjectHashes.contains(objectHash);
	}

	boolean matchesInteracting(Player localPlayer)
	{
		if (localPlayer == null)
		{
			return false;
		}

		final Actor actor = getActor();
		if (actor == null)
		{
			return false;
		}

		return localPlayer.getInteracting() == actor || actor.getInteracting() == localPlayer;
	}

	Point getCanvasAnchor(Client client, boolean markerBelowTarget)
	{
		final int height = markerBelowTarget ? 0 : overlayHeight;
		try
		{
			Point anchor = null;
			if (object != null)
			{
				anchor = object.getCanvasLocation(height);
			}
			else
			{
				final LocalPoint localPoint = getLocalLocation();
				if (localPoint != null)
				{
					anchor = Perspective.localToCanvas(client, localPoint, client.getPlane(), height);
				}
			}

			if (anchor != null)
			{
				return anchor;
			}

			return getCanvasTileCenter(client);
		}
		catch (RuntimeException ex)
		{
			return null;
		}
	}

	Point getCanvasTextLocation(Graphics2D graphics, String label)
	{
		try
		{
			if (npc != null)
			{
				return npc.getCanvasTextLocation(graphics, label, npc.getLogicalHeight() + OBJECT_OVERLAY_HEIGHT);
			}

			if (player != null)
			{
				return player.getCanvasTextLocation(graphics, label, player.getLogicalHeight() + OBJECT_OVERLAY_HEIGHT);
			}

			if (object != null)
			{
				return object.getCanvasTextLocation(graphics, label, OBJECT_OVERLAY_HEIGHT);
			}

			return null;
		}
		catch (RuntimeException ex)
		{
			return null;
		}
	}

	private static String normalize(String value)
	{
		return CatalogEntry.normalizeForSearch(value);
	}

	private Point getCanvasTileCenter(Client client)
	{
		final LocalPoint localPoint = getLocalLocation();
		if (localPoint == null)
		{
			return null;
		}

		final Polygon polygon = Perspective.getCanvasTilePoly(client, localPoint);
		if (polygon == null || polygon.npoints <= 0)
		{
			return null;
		}

		int totalX = 0;
		int totalY = 0;
		for (int index = 0; index < polygon.npoints; index++)
		{
			totalX += polygon.xpoints[index];
			totalY += polygon.ypoints[index];
		}

		return new Point(totalX / polygon.npoints, totalY / polygon.npoints);
	}
}
