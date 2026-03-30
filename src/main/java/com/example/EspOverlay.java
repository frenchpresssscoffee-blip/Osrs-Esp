package com.example;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.FontMetrics;
import java.awt.Graphics2D;
import java.awt.Polygon;
import java.awt.Stroke;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import javax.inject.Inject;
import net.runelite.api.Actor;
import net.runelite.api.Client;
import net.runelite.api.MenuAction;
import net.runelite.api.MenuEntry;
import net.runelite.api.Player;
import net.runelite.api.Point;
import net.runelite.api.Perspective;
import net.runelite.api.Tile;
import net.runelite.api.TileObject;
import net.runelite.api.coords.LocalPoint;
import net.runelite.client.ui.overlay.Overlay;
import net.runelite.client.ui.overlay.OverlayLayer;
import net.runelite.client.ui.overlay.OverlayPosition;

public class EspOverlay extends Overlay
{
	private static final int MARKER_DIAMETER = 10;
	private static final int MARKER_RADIUS = MARKER_DIAMETER / 2;
	private static final int MARKER_OFFSET = 12;
	private static final int EDGE_PADDING = 8;
	private static final int LABEL_PADDING_X = 4;
	private static final int LABEL_PADDING_Y = 2;
	private static final int LABEL_CORNER_RADIUS = 8;

	private final Client client;
	private final EspPlugin plugin;
	private final EspConfig config;
	private final Stroke lineStroke = new BasicStroke(2f);
	private final Stroke offscreenLineStroke = new BasicStroke(2f, BasicStroke.CAP_BUTT, BasicStroke.JOIN_MITER, 10f, new float[]{6f, 6f}, 0f);

	@Inject
	private EspOverlay(Client client, EspPlugin plugin, EspConfig config)
	{
		this.client = client;
		this.plugin = plugin;
		this.config = config;
		setPosition(OverlayPosition.DYNAMIC);
		setLayer(OverlayLayer.ABOVE_SCENE);
	}

	@Override
	public Dimension render(Graphics2D graphics)
	{
		if (!plugin.isReady())
		{
			return null;
		}

		final List<TrackedTarget> targets = plugin.getTrackedTargets();
		if (targets.isEmpty())
		{
			return null;
		}

		final Player localPlayer = client.getLocalPlayer();
		final LocalPoint playerLocal = localPlayer == null ? null : localPlayer.getLocalLocation();
		final Point playerCanvasPoint = localPlayer != null && (config.showLines() || config.showOffscreenMarkers())
			? getPlayerCanvasPoint(localPlayer, playerLocal)
			: null;
		final Actor hoveredActor = plugin.usesActorFocusVisibility() ? getHoveredActor() : null;
		final Map<Long, TrackedTarget> objectTargetsByHash = plugin.usesObjectFocusVisibility() ? buildObjectTargetsByHash(targets) : Map.of();
		final Set<Long> hoveredObjectHashes = plugin.usesObjectFocusVisibility() ? getHoveredObjectHashes(objectTargetsByHash) : Set.of();

		for (TrackedTarget target : targets)
		{
			if (!plugin.isTargetVisible(target, hoveredActor, hoveredObjectHashes, localPlayer))
			{
				continue;
			}

			renderTarget(graphics, target, playerLocal, playerCanvasPoint);
		}

		return null;
	}

	private void renderTarget(Graphics2D graphics, TrackedTarget target, LocalPoint playerLocal, Point playerCanvasPoint)
	{
		final LocalPoint targetLocal = target.getLocalLocation();
		if (targetLocal == null)
		{
			return;
		}

		final Color color = plugin.getDisplayColor(target);
		final Point anchor = target.getCanvasAnchor(client, config.markerBelowTarget());
		final ScreenPoint markerPoint = resolveMarkerPoint(playerLocal, playerCanvasPoint, targetLocal, anchor);
		final ScreenPoint linePoint = resolveLinePoint(playerLocal, playerCanvasPoint, targetLocal, anchor, markerPoint);

		if (linePoint != null && config.showLines())
		{
			renderTargetLine(graphics, playerCanvasPoint, linePoint, color);
		}

		if (markerPoint != null && config.showMarkers())
		{
			drawMarker(graphics, markerPoint, color, playerCanvasPoint);
		}

		if (!plugin.shouldShowLabel())
		{
			return;
		}

		final String label = plugin.buildLabel(target);
		if (label == null)
		{
			return;
		}

		Point textLocation = target.getCanvasTextLocation(graphics, label);
		if (textLocation == null && markerPoint != null)
		{
			textLocation = getMarkerTextLocation(graphics, markerPoint.point, label);
		}

		if (textLocation == null)
		{
			return;
		}

		textLocation = clampLabelTextLocation(graphics, textLocation, label);
		drawReadableLabel(graphics, textLocation, label, color);
	}

	private Actor getHoveredActor()
	{
		final MenuEntry[] entries = client.getMenuEntries();
		for (int index = entries.length - 1; index >= 0; index--)
		{
			final MenuEntry entry = entries[index];
			if (entry.getActor() != null)
			{
				return entry.getActor();
			}
		}

		return null;
	}

	private Map<Long, TrackedTarget> buildObjectTargetsByHash(List<TrackedTarget> targets)
	{
		final Map<Long, TrackedTarget> objectTargetsByHash = new HashMap<>();
		for (TrackedTarget target : targets)
		{
			if (target.getCategory() == TargetCategory.OBJECT)
			{
				objectTargetsByHash.put(target.getObjectHash(), target);
			}
		}

		return objectTargetsByHash;
	}

	private Set<Long> getHoveredObjectHashes(Map<Long, TrackedTarget> objectTargetsByHash)
	{
		final Set<Long> hashes = new HashSet<>();
		final Tile tile = client.getSelectedSceneTile();
		if (tile == null || objectTargetsByHash.isEmpty())
		{
			return hashes;
		}

		final HoveredObjectMatcher matcher = getHoveredObjectMatcher();
		if (matcher == null)
		{
			return hashes;
		}

		addHoveredObjectHash(hashes, tile.getWallObject(), matcher, objectTargetsByHash);
		addHoveredObjectHash(hashes, tile.getDecorativeObject(), matcher, objectTargetsByHash);
		addHoveredObjectHash(hashes, tile.getGroundObject(), matcher, objectTargetsByHash);

		for (TileObject object : tile.getGameObjects())
		{
			addHoveredObjectHash(hashes, object, matcher, objectTargetsByHash);
		}

		return hashes;
	}

	private HoveredObjectMatcher getHoveredObjectMatcher()
	{
		final MenuEntry[] entries = client.getMenuEntries();
		for (int index = entries.length - 1; index >= 0; index--)
		{
			final MenuEntry entry = entries[index];
			if (!isObjectMenuAction(entry.getType()))
			{
				continue;
			}

			return new HoveredObjectMatcher(entry.getIdentifier(), normalizeMenuTarget(entry.getTarget()));
		}

		return null;
	}

	private boolean isObjectMenuAction(MenuAction action)
	{
		return action == MenuAction.GAME_OBJECT_FIRST_OPTION
			|| action == MenuAction.GAME_OBJECT_SECOND_OPTION
			|| action == MenuAction.GAME_OBJECT_THIRD_OPTION
			|| action == MenuAction.GAME_OBJECT_FOURTH_OPTION
			|| action == MenuAction.GAME_OBJECT_FIFTH_OPTION
			|| action == MenuAction.EXAMINE_OBJECT
			|| action == MenuAction.ITEM_USE_ON_GAME_OBJECT
			|| action == MenuAction.WIDGET_TARGET_ON_GAME_OBJECT;
	}

	private String normalizeMenuTarget(String target)
	{
		if (target == null || target.isBlank())
		{
			return "";
		}

		final StringBuilder plainText = new StringBuilder(target.length());
		boolean insideTag = false;
		for (char character : target.toCharArray())
		{
			if (character == '<')
			{
				insideTag = true;
				continue;
			}
			if (character == '>')
			{
				insideTag = false;
				continue;
			}
			if (!insideTag)
			{
				plainText.append(character);
			}
		}

		return CatalogEntry.normalizeForSearch(plainText.toString());
	}

	private void addHoveredObjectHash(Set<Long> hashes, TileObject object, HoveredObjectMatcher matcher, Map<Long, TrackedTarget> objectTargetsByHash)
	{
		if (object == null || matcher == null)
		{
			return;
		}

		final TrackedTarget trackedTarget = objectTargetsByHash.get(object.getHash());
		if (trackedTarget != null && matcher.matches(object, trackedTarget.getNormalizedName()))
		{
			hashes.add(object.getHash());
		}
	}

	private ScreenPoint resolveMarkerPoint(LocalPoint playerLocal, Point playerCanvasPoint, LocalPoint targetLocal, Point anchor)
	{
		if (!config.showMarkers())
		{
			return null;
		}

		if (anchor != null)
		{
			return new ScreenPoint(clampToViewport(offsetMarker(anchor)), false);
		}

		if (!config.showOffscreenMarkers())
		{
			return null;
		}

		final Point offscreenAnchor = getOffscreenAnchor(playerCanvasPoint, playerLocal, targetLocal);
		return offscreenAnchor == null ? null : new ScreenPoint(clampToViewport(offscreenAnchor), true);
	}

	private ScreenPoint resolveLinePoint(LocalPoint playerLocal, Point playerCanvasPoint, LocalPoint targetLocal, Point anchor, ScreenPoint markerPoint)
	{
		if (!config.showLines())
		{
			return null;
		}

		if (markerPoint != null)
		{
			return markerPoint;
		}

		if (anchor != null)
		{
			return new ScreenPoint(clampToViewport(offsetMarker(anchor)), false);
		}

		if (!config.showOffscreenMarkers())
		{
			return null;
		}

		final Point offscreenAnchor = getOffscreenAnchor(playerCanvasPoint, playerLocal, targetLocal);
		return offscreenAnchor == null ? null : new ScreenPoint(clampToViewport(offscreenAnchor), true);
	}

	private void renderTargetLine(Graphics2D graphics, Point playerCanvasPoint, ScreenPoint endPoint, Color color)
	{
		final Point start = playerCanvasPoint;
		if (start == null)
		{
			return;
		}

		drawLine(graphics, start, endPoint, color);
	}

	private Point offsetMarker(Point anchor)
	{
		final int direction = config.markerBelowTarget() ? 1 : -1;
		return new Point(anchor.getX(), anchor.getY() + (direction * MARKER_OFFSET));
	}

	private Point getOffscreenAnchor(Point playerCanvasPoint, LocalPoint playerLocal, LocalPoint targetLocal)
	{
		if (playerCanvasPoint == null || playerLocal == null)
		{
			return null;
		}

		return getViewportEdgePoint(playerCanvasPoint, playerLocal, targetLocal);
	}

	private Point getPlayerCanvasPoint(Player player, LocalPoint playerLocal)
	{
		Point playerCanvas = Perspective.localToCanvas(client, playerLocal, client.getPlane(), 0);
		if (playerCanvas == null)
		{
			playerCanvas = getViewportCenter();
		}

		if (playerCanvas == null)
		{
			return null;
		}

		playerCanvas = new Point(playerCanvas.getX(), playerCanvas.getY() + config.lineStartOffsetY());
		return clampToViewport(playerCanvas);
	}

	private Point getViewportEdgePoint(Point originCanvas, LocalPoint originLocal, LocalPoint targetLocal)
	{
		if (originCanvas == null || originLocal == null || targetLocal == null)
		{
			return null;
		}

		final int dx = targetLocal.getX() - originLocal.getX();
		final int dy = targetLocal.getY() - originLocal.getY();
		if (dx == 0 && dy == 0)
		{
			return null;
		}

		final double angle = client.getCameraYaw() * (Math.PI * 2D / 2048D);
		final double sin = Math.sin(angle);
		final double cos = Math.cos(angle);

		final double screenDx = dx * cos + dy * sin;
		final double screenDy = -(dy * cos - dx * sin);
		return projectRayToViewport(originCanvas, screenDx, screenDy);
	}

	private Point projectRayToViewport(Point origin, double directionX, double directionY)
	{
		if (origin == null || (directionX == 0D && directionY == 0D))
		{
			return null;
		}

		final int left = client.getViewportXOffset() + EDGE_PADDING;
		final int top = client.getViewportYOffset() + EDGE_PADDING;
		final int right = left + Math.max(client.getViewportWidth() - (EDGE_PADDING * 2), 1);
		final int bottom = top + Math.max(client.getViewportHeight() - (EDGE_PADDING * 2), 1);

		final double originX = clamp(origin.getX(), left, right);
		final double originY = clamp(origin.getY(), top, bottom);

		double edgeDistance = Double.POSITIVE_INFINITY;
		if (directionX > 0D)
		{
			edgeDistance = Math.min(edgeDistance, (right - originX) / directionX);
		}
		else if (directionX < 0D)
		{
			edgeDistance = Math.min(edgeDistance, (left - originX) / directionX);
		}

		if (directionY > 0D)
		{
			edgeDistance = Math.min(edgeDistance, (bottom - originY) / directionY);
		}
		else if (directionY < 0D)
		{
			edgeDistance = Math.min(edgeDistance, (top - originY) / directionY);
		}

		if (!Double.isFinite(edgeDistance) || edgeDistance <= 0D)
		{
			return null;
		}

		final int projectedX = clamp((int) Math.round(originX + (directionX * edgeDistance)), left, right);
		final int projectedY = clamp((int) Math.round(originY + (directionY * edgeDistance)), top, bottom);
		return new Point(projectedX, projectedY);
	}

	private Point getViewportCenter()
	{
		final int width = client.getViewportWidth();
		final int height = client.getViewportHeight();
		if (width <= 0 || height <= 0)
		{
			return null;
		}

		return new Point(
			client.getViewportXOffset() + (width / 2),
			client.getViewportYOffset() + (height / 2)
		);
	}

	private Point clampToViewport(Point point)
	{
		if (point == null)
		{
			return null;
		}

		final int left = client.getViewportXOffset() + EDGE_PADDING;
		final int top = client.getViewportYOffset() + EDGE_PADDING;
		final int right = left + Math.max(client.getViewportWidth() - (EDGE_PADDING * 2), 1);
		final int bottom = top + Math.max(client.getViewportHeight() - (EDGE_PADDING * 2), 1);

		return new Point(
			clamp(point.getX(), left, right),
			clamp(point.getY(), top, bottom)
		);
	}

	private int clamp(int value, int min, int max)
	{
		return Math.max(min, Math.min(max, value));
	}

	private void drawMarker(Graphics2D graphics, ScreenPoint center, Color fill, Point origin)
	{
		if (center == null || center.point == null)
		{
			return;
		}

		final int x = center.point.getX() - MARKER_RADIUS;
		final int y = center.point.getY() - MARKER_RADIUS;
		if (!center.offscreen)
		{
			graphics.setColor(new Color(fill.getRed(), fill.getGreen(), fill.getBlue(), 200));
			graphics.fillOval(x, y, MARKER_DIAMETER, MARKER_DIAMETER);
			graphics.setColor(Color.BLACK);
			graphics.drawOval(x, y, MARKER_DIAMETER, MARKER_DIAMETER);
			return;
		}

		final Polygon arrow = createOffscreenArrow(center.point, origin == null ? getViewportCenter() : origin);
		graphics.setColor(new Color(0, 0, 0, 120));
		graphics.translate(1, 1);
		graphics.fillPolygon(arrow);
		graphics.translate(-1, -1);
		graphics.setColor(new Color(fill.getRed(), fill.getGreen(), fill.getBlue(), 210));
		graphics.fillPolygon(arrow);
		graphics.setColor(Color.WHITE);
		graphics.drawPolygon(arrow);
	}

	private Polygon createOffscreenArrow(Point markerCenter, Point origin)
	{
		final double angle = origin == null
			? 0D
			: Math.atan2(markerCenter.getY() - origin.getY(), markerCenter.getX() - origin.getX());
		final int length = 16;
		final int width = 12;
		final int tail = 6;

		final int[] baseX = new int[]{length / 2, -length / 2, -length / 2 + tail};
		final int[] baseY = new int[]{0, -width / 2, width / 2};
		final Polygon polygon = new Polygon();
		for (int index = 0; index < baseX.length; index++)
		{
			final double rotatedX = (baseX[index] * Math.cos(angle)) - (baseY[index] * Math.sin(angle));
			final double rotatedY = (baseX[index] * Math.sin(angle)) + (baseY[index] * Math.cos(angle));
			polygon.addPoint(
				markerCenter.getX() + (int) Math.round(rotatedX),
				markerCenter.getY() + (int) Math.round(rotatedY)
			);
		}

		return polygon;
	}

	private void drawLine(Graphics2D graphics, Point start, ScreenPoint end, Color color)
	{
		final Stroke originalStroke = graphics.getStroke();
		graphics.setColor(new Color(color.getRed(), color.getGreen(), color.getBlue(), end.offscreen ? 150 : 180));
		graphics.setStroke(end.offscreen ? offscreenLineStroke : lineStroke);
		graphics.drawLine(start.getX(), start.getY(), end.point.getX(), end.point.getY());
		graphics.setStroke(originalStroke);
	}

	private Point getMarkerTextLocation(Graphics2D graphics, Point markerPoint, String label)
	{
		if (markerPoint == null)
		{
			return null;
		}

		final FontMetrics metrics = graphics.getFontMetrics();
		final int textWidth = metrics.stringWidth(label);
		final int left = client.getViewportXOffset() + EDGE_PADDING;
		final int top = client.getViewportYOffset() + EDGE_PADDING + metrics.getAscent();
		final int right = client.getViewportXOffset() + client.getViewportWidth() - EDGE_PADDING - textWidth - (LABEL_PADDING_X * 2);
		final int bottom = client.getViewportYOffset() + client.getViewportHeight() - EDGE_PADDING;

		final int textX = clamp(markerPoint.getX() + MARKER_RADIUS + 6, left, Math.max(left, right));
		final int textY = clamp(markerPoint.getY() - MARKER_RADIUS - 4, top, bottom);
		return new Point(textX, textY);
	}

	private Point clampLabelTextLocation(Graphics2D graphics, Point textLocation, String label)
	{
		if (textLocation == null || label == null || label.isBlank())
		{
			return textLocation;
		}

		final FontMetrics metrics = graphics.getFontMetrics();
		final int textWidth = metrics.stringWidth(label);
		final int textHeight = metrics.getHeight();
		final int left = client.getViewportXOffset() + EDGE_PADDING + LABEL_PADDING_X;
		final int top = client.getViewportYOffset() + EDGE_PADDING + metrics.getAscent() + LABEL_PADDING_Y;
		final int right = client.getViewportXOffset() + client.getViewportWidth() - EDGE_PADDING - textWidth - LABEL_PADDING_X;
		final int bottom = client.getViewportYOffset() + client.getViewportHeight() - EDGE_PADDING - (textHeight - metrics.getAscent()) - LABEL_PADDING_Y;

		return new Point(
			clamp(textLocation.getX(), left, Math.max(left, right)),
			clamp(textLocation.getY(), top, Math.max(top, bottom))
		);
	}

	private void drawReadableLabel(Graphics2D graphics, Point textLocation, String label, Color color)
	{
		if (textLocation == null || label == null || label.isBlank())
		{
			return;
		}

		final FontMetrics metrics = graphics.getFontMetrics();
		final int textWidth = metrics.stringWidth(label);
		final int textHeight = metrics.getHeight();
		final int boxX = textLocation.getX() - LABEL_PADDING_X;
		final int boxY = textLocation.getY() - metrics.getAscent() - LABEL_PADDING_Y;
		final int boxWidth = textWidth + (LABEL_PADDING_X * 2);
		final int boxHeight = textHeight + (LABEL_PADDING_Y * 2) - 1;

		graphics.setColor(new Color(0, 0, 0, 170));
		graphics.fillRoundRect(boxX, boxY, boxWidth, boxHeight, LABEL_CORNER_RADIUS, LABEL_CORNER_RADIUS);
		graphics.setColor(new Color(color.getRed(), color.getGreen(), color.getBlue(), 200));
		graphics.drawRoundRect(boxX, boxY, boxWidth, boxHeight, LABEL_CORNER_RADIUS, LABEL_CORNER_RADIUS);
		graphics.setColor(Color.BLACK);
		graphics.drawString(label, textLocation.getX() + 1, textLocation.getY() + 1);
		graphics.setColor(color);
		graphics.drawString(label, textLocation.getX(), textLocation.getY());
	}

	private static final class ScreenPoint
	{
		private final Point point;
		private final boolean offscreen;

		private ScreenPoint(Point point, boolean offscreen)
		{
			this.point = point;
			this.offscreen = offscreen;
		}
	}

	private static final class HoveredObjectMatcher
	{
		private final int id;
		private final String normalizedName;

		private HoveredObjectMatcher(int id, String normalizedName)
		{
			this.id = id;
			this.normalizedName = normalizedName == null ? "" : normalizedName;
		}

		private boolean matches(TileObject object, String normalizedObjectName)
		{
			if (object == null || object.getId() != id)
			{
				return false;
			}

			return normalizedName.isEmpty() || normalizedName.equals(normalizedObjectName);
		}
	}
}
