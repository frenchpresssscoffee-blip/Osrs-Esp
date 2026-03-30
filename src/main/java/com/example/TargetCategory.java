package com.example;

enum TargetCategory
{
	NPC("NPC"),
	BOSS("Boss"),
	PLAYER("Player"),
	OBJECT("Object");

	private final String label;

	TargetCategory(String label)
	{
		this.label = label;
	}

	String getLabel()
	{
		return label;
	}
}
