package game;

import java.util.function.Supplier;

public class Spell {

	private String name;
	public String name() { return name; }

	private int manaCost;
	public int manaCost() { return manaCost; }

	private final Supplier<Effect> effectFactory;
	public Effect effect() { return effectFactory.get(); }

	public boolean requiresTarget() { return true; }
	
	public Spell(String name, int manaCost, Supplier<Effect> effectFactory){
		this.name = name;
		this.manaCost = manaCost;
		this.effectFactory = effectFactory;
	}
}
