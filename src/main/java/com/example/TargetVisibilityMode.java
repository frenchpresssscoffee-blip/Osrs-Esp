package com.example;

public enum TargetVisibilityMode
{
	ALL("Show all"),
	FOCUSED("Focused only"),
	NAME_MATCH("Name contains"),
	RARE_ONLY("Rare only");

	private final String label;

	TargetVisibilityMode(String label)
	{
		this.label = label;
	}

	@Override
	public String toString()
	{
		return label;
	}
}
