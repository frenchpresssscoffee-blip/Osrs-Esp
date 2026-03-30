package com.example;

enum CatalogKind
{
	NPC("NPCs", "NPC", "npc-catalog.txt"),
	BOSS("Bosses", "Boss", "npc-catalog.txt"),
	OBJECT("Objects", "Object", "object-catalog.txt");

	private final String pluralLabel;
	private final String singularLabel;
	private final String resourceName;

	CatalogKind(String pluralLabel, String singularLabel, String resourceName)
	{
		this.pluralLabel = pluralLabel;
		this.singularLabel = singularLabel;
		this.resourceName = resourceName;
	}

	String getPluralLabel()
	{
		return pluralLabel;
	}

	String getSingularLabel()
	{
		return singularLabel;
	}

	String getResourceName()
	{
		return resourceName;
	}

	@Override
	public String toString()
	{
		return pluralLabel;
	}
}
