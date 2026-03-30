package com.example;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.Locale;
import java.util.Set;

final class CatalogEntry
{
	private final CatalogKind kind;
	private final int id;
	private final String rawName;
	private final String displayName;
	private final String normalizedRawName;
	private final String normalizedDisplayName;
	private final String searchableText;
	private final Set<String> aliases;

	CatalogEntry(CatalogKind kind, int id, String rawName)
	{
		this(kind, id, rawName, humanize(rawName));
	}

	CatalogEntry(CatalogKind kind, int id, String rawName, String displayName)
	{
		this.kind = kind;
		this.id = id;
		this.rawName = rawName;
		this.displayName = displayName == null || displayName.isBlank() ? humanize(rawName) : displayName.strip();
		this.normalizedRawName = normalizeForSearch(rawName);
		this.normalizedDisplayName = normalizeForSearch(this.displayName);
		this.aliases = buildAliases(normalizedRawName, normalizedDisplayName);
		this.searchableText = id >= 0
			? normalizedRawName + " " + normalizedDisplayName + " " + String.join(" ", aliases) + " " + id
			: normalizedRawName + " " + normalizedDisplayName + " " + String.join(" ", aliases);
	}

	CatalogKind getKind()
	{
		return kind;
	}

	int getId()
	{
		return id;
	}

	String getRawName()
	{
		return rawName;
	}

	String getDisplayName()
	{
		return displayName;
	}

	int matchScore(String normalizedQuery, boolean numericQuery)
	{
		if (normalizedQuery == null || normalizedQuery.isEmpty())
		{
			return -1;
		}

		final String idText = Integer.toString(id);
		if ((id >= 0 && idText.equals(normalizedQuery)) || normalizedRawName.equals(normalizedQuery) || normalizedDisplayName.equals(normalizedQuery))
		{
			return 0;
		}

		if (aliases.contains(normalizedQuery))
		{
			return 0;
		}

		if (numericQuery)
		{
			return id >= 0 && idText.startsWith(normalizedQuery) ? 1 : -1;
		}

		if (normalizedRawName.startsWith(normalizedQuery) || normalizedDisplayName.startsWith(normalizedQuery) || aliasStartsWith(normalizedQuery))
		{
			return 1;
		}

		return searchableText.contains(normalizedQuery) ? 2 : -1;
	}

	boolean shouldShowRawName()
	{
		return !normalizedRawName.equals(normalizedDisplayName);
	}

	boolean hasId()
	{
		return id >= 0;
	}

	private boolean aliasStartsWith(String normalizedQuery)
	{
		for (String alias : aliases)
		{
			if (alias.startsWith(normalizedQuery))
			{
				return true;
			}
		}

		return false;
	}

	static String normalizeForSearch(String value)
	{
		if (value == null)
		{
			return "";
		}

		final StringBuilder builder = new StringBuilder(value.length() * 2);
		char previous = 0;
		boolean previousWasSpace = true;

		for (char current : value.strip().toLowerCase(Locale.ROOT).toCharArray())
		{
			final boolean currentIsLetterOrDigit = Character.isLetterOrDigit(current);
			if (!currentIsLetterOrDigit)
			{
				if (!previousWasSpace)
				{
					builder.append(' ');
					previousWasSpace = true;
				}
				previous = current;
				continue;
			}

			final boolean boundaryBetweenDigits = previous != 0
				&& Character.isLetterOrDigit(previous)
				&& Character.isDigit(previous) != Character.isDigit(current)
				&& !previousWasSpace;
			if (boundaryBetweenDigits)
			{
				builder.append(' ');
			}

			builder.append(current);
			previousWasSpace = false;
			previous = current;
		}

		final int length = builder.length();
		if (length > 0 && builder.charAt(length - 1) == ' ')
		{
			builder.setLength(length - 1);
		}

		return builder.toString();
	}

	private static String humanize(String rawName)
	{
		final String normalized = normalizeForSearch(rawName);
		if (normalized.isEmpty())
		{
			return "";
		}

		final String[] parts = normalized.split(" ");
		final StringBuilder humanized = new StringBuilder(normalized.length());
		for (String part : parts)
		{
			if (part.isEmpty())
			{
				continue;
			}

			if (humanized.length() > 0)
			{
				humanized.append(' ');
			}

			if (Character.isDigit(part.charAt(0)))
			{
				humanized.append(part);
				continue;
			}

			humanized.append(Character.toUpperCase(part.charAt(0)));
			if (part.length() > 1)
			{
				humanized.append(part.substring(1));
			}
		}

		return humanized.toString();
	}

	private static Set<String> buildAliases(String normalizedRawName, String normalizedDisplayName)
	{
		final LinkedHashSet<String> values = new LinkedHashSet<>();
		addSuffixAliases(values, normalizedRawName);
		addSuffixAliases(values, normalizedDisplayName);
		values.remove(normalizedRawName);
		values.remove(normalizedDisplayName);
		return Set.copyOf(values);
	}

	private static void addSuffixAliases(Set<String> values, String normalizedValue)
	{
		if (normalizedValue == null || normalizedValue.isBlank())
		{
			return;
		}

		final String[] parts = normalizedValue.split(" ");
		if (parts.length <= 1)
		{
			return;
		}

		final int maxAliasLength = Math.min(3, parts.length - 1);
		for (int aliasLength = 1; aliasLength <= maxAliasLength; aliasLength++)
		{
			final ArrayList<String> suffix = new ArrayList<>(aliasLength);
			for (int index = parts.length - aliasLength; index < parts.length; index++)
			{
				if (!parts[index].isBlank())
				{
					suffix.add(parts[index]);
				}
			}

			if (!suffix.isEmpty())
			{
				values.add(String.join(" ", suffix));
			}
		}
	}
}
